export const metrics=[['pages','Páginas vistas','#6be4d1','page'],['visits','Entradas estimadas','#8cadff','visit'],['downloads','Clics de descarga','#ffce72','download'],['engaged','Páginas con 30 s','#e9a1ff','engaged'],['clicks','Otros clics','#ff9e9e','click']];
export function aggregate(daily,unit='day'){
 const groups=new Map();
 for(const row of daily){
  const d=new Date(row.day+'T00:00:00Z');let key=row.day;
  if(unit==='month')key=row.day.slice(0,7)+'-01';
  if(unit==='week'){d.setUTCDate(d.getUTCDate()-(d.getUTCDay()+6)%7);key=d.toISOString().slice(0,10)}
  if(!groups.has(key))groups.set(key,{day:key,from:row.day,to:row.day,available:false,partial:false,...Object.fromEntries(metrics.map(([k])=>[k,0]))});
  const group=groups.get(key);group.to=row.day;group.partial ||= !row.available;group.available ||= row.available;
  for(const [key] of metrics)group[key]+=Number(row[key]||0);
 }
 return [...groups.values()].map(g=>g.available?g:{...g,...Object.fromEntries(metrics.map(([k])=>[k,null]))});
}
const svgEl=(tag,attrs={})=>{const e=document.createElementNS('http://www.w3.org/2000/svg',tag);for(const [k,v] of Object.entries(attrs))e.setAttribute(k,v);return e};
export function renderChart(container,data,selected){
 container.replaceChildren();
 if(!data.some(r=>r.available)){const p=document.createElement('p');p.className='empty';p.textContent='No hay medición disponible para estas fechas.';container.append(p);return}
 if(!selected.length){const p=document.createElement('p');p.className='empty';p.textContent='Selecciona al menos un indicador.';container.append(p);return}
 const visible=metrics.filter(([k])=>selected.includes(k)),max=Math.max(1,...data.flatMap(r=>visible.map(([k])=>r[k]||0))),W=1000,H=300,L=65,R=22,T=18,B=46;
 const svg=svgEl('svg',{viewBox:`0 0 ${W} ${H}`,role:'img','aria-label':'Gráfica temporal: '+visible.map(m=>m[1]).join(', ')});
 const title=svgEl('title');title.textContent='Evolución de recuentos. Los valores exactos también están disponibles en la tabla.';svg.append(title);
 const x=i=>L+(data.length===1?(W-L-R)/2:i*(W-L-R)/(data.length-1)),y=v=>H-B-v/max*(H-T-B);
 for(let i=0;i<=4;i++){const value=max*i/4;svg.append(svgEl('line',{x1:L,x2:W-R,y1:y(value),y2:y(value),stroke:'#2c3d58'}));const t=svgEl('text',{x:L-10,y:y(value)+4,'text-anchor':'end',fill:'#acbbd0','font-size':12});t.textContent=new Intl.NumberFormat('es',{maximumFractionDigits:1}).format(value);svg.append(t)}
 for(const [key,label,color] of visible){
  let path='';for(let i=0;i<data.length;i++){if(data[i][key]===null){path+=' ';continue}path+=(i===0||data[i-1][key]===null?'M':'L')+x(i)+','+y(data[i][key])+' '}
  svg.append(svgEl('path',{d:path,fill:'none',stroke:color,'stroke-width':2.5,'stroke-linejoin':'round'}));
  data.forEach((r,i)=>{if(r[key]===null)return;const dot=svgEl('circle',{cx:x(i),cy:y(r[key]),r:data.length>90?2:3.5,fill:color});const t=svgEl('title');t.textContent=r.from+' → '+r.to+' · '+label+': '+r[key];dot.append(t);svg.append(dot)})
 }
 const tooltip=document.createElement('p');tooltip.className='chart-tooltip';tooltip.setAttribute('aria-live','polite');tooltip.textContent='Selecciona una fecha para ver sus valores.';
 const highlight=svgEl('line',{x1:0,x2:0,y1:T,y2:H-B,stroke:'#fff','stroke-dasharray':'4 4',visibility:'hidden'});svg.append(highlight);
 const show=i=>{highlight.setAttribute('x1',x(i));highlight.setAttribute('x2',x(i));highlight.setAttribute('visibility','visible');const r=data[i];tooltip.textContent=r.from+' → '+r.to+': '+(r.available?visible.map(([k,n])=>n+': '+r[k]).join(' · ')+(r.partial?' · Medición parcial':''):'Sin medición')};
 let active=Math.max(0,data.findIndex(r=>r.available));svg.setAttribute('tabindex','0');svg.setAttribute('aria-describedby',container.id+'-help');tooltip.id=container.id+'-help';svg.onkeydown=e=>{if(['ArrowLeft','ArrowRight','Home','End'].includes(e.key)){e.preventDefault();active=e.key==='Home'?0:e.key==='End'?data.length-1:Math.max(0,Math.min(data.length-1,active+(e.key==='ArrowRight'?1:-1)));show(active)}};
 svg.onpointermove=e=>{const rect=svg.getBoundingClientRect(),px=(e.clientX-rect.left)/rect.width*W;active=data.length===1?0:Math.max(0,Math.min(data.length-1,Math.round((px-L)/(W-L-R)*(data.length-1))));show(active)};svg.onfocus=()=>show(active);
 const indexes=[...new Set([0,Math.floor((data.length-1)/4),Math.floor((data.length-1)/2),Math.floor((data.length-1)*3/4),data.length-1])];
 for(const i of indexes){const t=svgEl('text',{x:x(i),y:H-12,'text-anchor':'middle',fill:'#acbbd0','font-size':12});t.textContent=data[i].day.slice(5);svg.append(t)}
 container.append(svg,tooltip);
}
