import {json,sameOrigin,readBody,rateLimit} from '../../../server/admin-common.js';
export async function onRequest({request,env}){
 if(request.method!=='POST')return json({error:'Método no permitido'},405);
 if(!sameOrigin(request))return json({error:'Origen no permitido'},403);
 if(!env.COMMENTS_DB||!env.COMMENTS_ADMIN_KEY)return json({error:'No disponible'},503);
 try{
  const b=await readBody(request,3000);
  if(!Number.isSafeInteger(b.id)||b.id<1||!['spam','abuse','privacy','other'].includes(b.reason)||typeof b.details!=='string'||b.details.length>1000||b.website)return json({error:'Datos no válidos'},400);
  if(!await rateLimit(request,env,'reports',5))return json({error:'Demasiadas denuncias. Inténtalo más tarde.'},429);
  const c=await env.COMMENTS_DB.prepare("SELECT id FROM comments WHERE id=? AND status='approved' AND moderation_state='normal'").bind(b.id).first();
  if(!c)return json({error:'Comentario no disponible'},404);
  const now=new Date().toISOString();await env.COMMENTS_DB.prepare("INSERT INTO comment_reports(comment_id,reason,details,status,created_at,updated_at) VALUES(?,?,?,'open',?,?)").bind(b.id,b.reason,b.details.trim(),now,now).run();
  return json({ok:true},201);
 }catch(e){return json({error:'No se ha podido enviar la denuncia'},e.status||500)}
}
