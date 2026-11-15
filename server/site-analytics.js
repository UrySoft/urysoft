export const validPath=p=>typeof p==='string'&&p.length<180&&/^\/(?:$|apps\/(?:lumen|foldery|widgery|nexomesh|minairo)\/$|ayuda\/instalar-android\/$|calaix\/(?:[a-z0-9-]+\/)?$|legal\/(?:[a-z0-9.-]+\/){1,4}$)/.test(p);
const host=s=>{try{const u=new URL(s);return ['http:','https:'].includes(u.protocol)?u.hostname.toLowerCase().replace(/^www\./,'').slice(0,100):''}catch{return ''}};
export function normalizeEvent(b,request){
 if(!b||!validPath(b.path)||!['page','click','download','engaged'].includes(b.event)||typeof b.id!=='string'||!/^[-a-f0-9]{36}$/.test(b.id))return null;
 const ua=request.headers.get('User-Agent')||'';if(/bot|crawler|spider|headless|preview|facebookexternalhit/i.test(ua)||request.cf?.botManagement?.verifiedBot)return null;
 const own=host(request.url),ref=host(b.referrer),internal=ref===own||['urysoft.com','urysoft.pages.dev'].includes(ref);
 let target='';if(b.event==='download'){if(!['lumen','foldery','widgery','nexomesh'].includes(b.target))return null;target=b.target}
 if(b.event==='click'){
  try{const u=new URL(b.target,request.url);target=host(u.href)===own?(validPath(u.pathname)?u.pathname:'otra-página'):host(u.href);if(!target)return null}catch{return null}
 }
 const country=/^[A-Z]{2}$/.test(request.cf?.country||'')?request.cf.country:'XX';
 const device=/iPad|Tablet/i.test(ua)?'Tableta':/Mobile|Android|iPhone/i.test(ua)?'Móvil':'Escritorio';
 const browser=/Edg\//.test(ua)?'Edge':/Firefox\//.test(ua)?'Firefox':/Chrome\//.test(ua)?'Chrome':/Safari\//.test(ua)?'Safari':'Otro';
 const language=['es','ca','en'].includes(b.language)?b.language:'otro';
 const source=['google','bing','facebook','linkedin','instagram','youtube','newsletter'].includes(String(b.source).toLowerCase())?String(b.source).toLowerCase():'';
 return {id:b.id,day:new Date().toISOString().slice(0,10),path:b.path,event:b.event,target,country,referrer:internal?'Interno':ref||'Directo / desconocido',device,browser,language,source,entry:b.event==='page'&&!internal};
}
export async function saveEvent(db,e){
 // A duplicate event must never count twice, even on concurrent delivery.
 const expires=Date.now()+86400000;e.expires=expires;
 const marker=db.prepare('INSERT OR IGNORE INTO analytics_dedupe(id,expires) VALUES(?,?)').bind(e.id,expires);
 // Use changes() inside each count statement against the immediately preceding unique marker.
 const count=db.prepare('INSERT INTO analytics_counts(day,path,event,target,country,referrer,device,browser,language,source,hits) SELECT ?,?,?,?,?,?,?,?,?,?,1 WHERE changes()=1 ON CONFLICT(day,path,event,target,country,referrer,device,browser,language,source) DO UPDATE SET hits=hits+1').bind(e.day,e.path,e.event,e.target,e.country,e.referrer,e.device,e.browser,e.language,e.source);
 const statements=[marker,count];
 if(e.entry)statements.push(db.prepare("INSERT INTO analytics_counts(day,path,event,target,country,referrer,device,browser,language,source,hits) SELECT ?,?,'visit','',?,?,?,?,?,?,1 WHERE changes()=1 ON CONFLICT(day,path,event,target,country,referrer,device,browser,language,source) DO UPDATE SET hits=hits+1").bind(e.day,e.path,e.country,e.referrer,e.device,e.browser,e.language,e.source));
 statements.push(db.prepare('DELETE FROM analytics_dedupe WHERE expires<?').bind(Date.now()),db.prepare('DELETE FROM analytics_counts WHERE day<?').bind(new Date(Date.now()-400*86400000).toISOString().slice(0,10)),db.prepare('DELETE FROM comment_limits WHERE expires<?').bind(Date.now()));
 await db.batch(statements);
}
export async function analyticsSummary(db,url){
 const date=s=>/^\d{4}-\d{2}-\d{2}$/.test(s||'')&&Number.isFinite(Date.parse(s))?s:null;
 const end=date(url.searchParams.get('to'))||new Date().toISOString().slice(0,10);
 const start=date(url.searchParams.get('from'))||new Date(Date.now()-29*86400000).toISOString().slice(0,10);
 if(start>end||Date.parse(end)-Date.parse(start)>399*86400000)throw {status:400};
 const path=url.searchParams.get('path'),country=url.searchParams.get('country');let where='day>=? AND day<=?',args=[start,end];
 if(path&&validPath(path)){where+=' AND path=?';args.push(path)}if(country&&/^[A-Z]{2}$/.test(country)){where+=' AND country=?';args.push(country)}
 const all=async(sql,...rest)=>(await db.prepare(sql).bind(...args,...rest).all()).results;
 const group=async(field,event='page')=>all('SELECT '+field+' AS label,SUM(hits) AS value FROM analytics_counts WHERE '+where+' AND event=? GROUP BY '+field+' ORDER BY value DESC LIMIT 50',event);
 const [totals,daily,pages,countries,referrers,devices,browsers,languages,sources,downloads,clicks,meta]=await Promise.all([
  all('SELECT event,SUM(hits) AS value FROM analytics_counts WHERE '+where+' GROUP BY event'),
  all("SELECT day,SUM(CASE WHEN event='page' THEN hits ELSE 0 END) AS pages,SUM(CASE WHEN event='visit' THEN hits ELSE 0 END) AS visits,SUM(CASE WHEN event='download' THEN hits ELSE 0 END) AS downloads FROM analytics_counts WHERE "+where+' GROUP BY day ORDER BY day'),
  group('path'),group('country'),group('referrer','visit'),group('device'),group('browser'),group('language'),group('source','visit'),group('target','download'),group('target','click'),db.prepare("SELECT value FROM admin_meta WHERE key='analytics_started'").first()
 ]);
 return {from:start,to:end,started:meta?.value,totals:Object.fromEntries(totals.map(t=>[t.event,t.value])),daily,pages,countries,referrers,devices,browsers,languages,sources,downloads,clicks};
}
