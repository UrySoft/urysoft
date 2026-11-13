const APPS = new Set(['lumen','nexomesh','foldery','widgery','minairo']);
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
const digest=async s=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(s)))).map(x=>x.toString(16).padStart(2,'0')).join('');
export async function onRequest({request,env,params}){
  if(!env.COMMENTS_DB || !env.COMMENTS_ADMIN_KEY) return json({error:'Comentarios todavía no disponibles.'},503);
  const url=new URL(request.url), path=(params.path||[]).join('/');
  if(request.method!=='GET' && request.headers.get('Origin')!==url.origin) return json({error:'Origen no permitido'},403);
  try {
    if(path==='admin'){
      const supplied=request.headers.get('Authorization')||'';
      if(await digest(supplied)!==await digest('Bearer '+env.COMMENTS_ADMIN_KEY)) return json({error:'Acceso no autorizado'},401);
      if(request.method==='GET') return json((await env.COMMENTS_DB.prepare('SELECT id,app,name,kind,message,rating,status,reply,created_at FROM comments ORDER BY created_at DESC LIMIT 200').all()).results);
      if(request.method==='PATCH'){
        const body=await request.json();
        if(!Number.isInteger(body.id)||!['pending','approved','hidden'].includes(body.status)||typeof body.reply!=='string'||body.reply.length>3000) return json({error:'Datos no válidos'},400);
        await env.COMMENTS_DB.prepare('UPDATE comments SET status=?,reply=?,updated_at=? WHERE id=?').bind(body.status,body.reply.trim(),new Date().toISOString(),body.id).run();
        return json({ok:true});
      }
      return json({error:'Método no permitido'},405);
    }
    if(path) return json({error:'No encontrado'},404);
    const app=url.searchParams.get('app');
    if(!APPS.has(app)) return json({error:'Aplicación no válida'},400);
    if(request.method==='GET') return json((await env.COMMENTS_DB.prepare("SELECT id,name,kind,message,rating,reply,created_at FROM comments WHERE app=? AND status='approved' ORDER BY created_at DESC LIMIT 100").bind(app).all()).results);
    if(request.method!=='POST') return json({error:'Método no permitido'},405);
    if(!request.headers.get('Content-Type')?.startsWith('application/json')) return json({error:'Formato no válido'},415);
    const raw=await request.text();if(raw.length>10000) return json({error:'Mensaje demasiado largo'},413);
    const b=JSON.parse(raw);
    if(b.website) return json({error:'Solicitud no válida'},400);
    if(typeof b.name!=='string'||b.name.trim().length<2||b.name.length>60||typeof b.message!=='string'||b.message.trim().length<10||b.message.length>3000||!['review','idea','bug'].includes(b.kind)||!Number.isInteger(b.rating)||b.rating<0||b.rating>5||b.consent!==true) return json({error:'Revisa los campos'},400);
    const bucket=await digest(env.COMMENTS_ADMIN_KEY+':'+(request.headers.get('CF-Connecting-IP')||'unknown')+':'+Math.floor(Date.now()/3600000));
    const rate=await env.COMMENTS_DB.prepare('INSERT INTO comment_limits (bucket,count,expires) VALUES (?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<5 RETURNING count').bind(bucket,Date.now()+7200000).first();
    if(!rate) return json({error:'Has alcanzado el límite. Inténtalo más tarde.'},429);
    await env.COMMENTS_DB.prepare('DELETE FROM comment_limits WHERE expires < ?').bind(Date.now()).run();
    const result=await env.COMMENTS_DB.prepare("INSERT INTO comments (app,name,kind,message,rating,status,reply,created_at,updated_at) VALUES (?,?,?,?,?,'pending','',?,?)").bind(app,b.name.trim(),b.kind,b.message.trim(),b.rating,new Date().toISOString(),new Date().toISOString()).run();
    return json({ok:true,id:result.meta.last_row_id,status:'pending'},201);
  }catch(e){return json({error:e instanceof SyntaxError?'Datos no válidos':'No se ha podido completar la solicitud'},e instanceof SyntaxError?400:500)}
}
