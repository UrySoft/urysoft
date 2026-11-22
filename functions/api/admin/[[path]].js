import {json,authorize,sameOrigin,readBody,batch} from '../../../server/admin-common.js';
import {analyticsSummary} from '../../../server/site-analytics.js';
import {operationsSummary} from '../../../server/admin-operations.js';
import {SEO_REPORT} from '../../../server/seo-report.js';
const APPS=['lumen','foldery','widgery','nexomesh','minairo','teclairc'];
const fields='c.*, (SELECT COUNT(*) FROM comment_reports r WHERE r.comment_id=c.id AND r.status=\'open\') AS reports';
export async function onRequest({request,env,params}){
 if(!env.COMMENTS_DB)return json({error:'Administración no disponible'},503);
 if(request.method!=='GET'&&!sameOrigin(request))return json({error:'Origen no permitido'},403);
 if(!await authorize(request,env))return json({error:'Acceso no autorizado o sesión caducada'},401);
 const db=env.COMMENTS_DB,url=new URL(request.url),route=(params.path||[]).join('/');
 try{
  if(route==='analytics'&&request.method==='GET')return json(await analyticsSummary(db,url));
  if(route==='operations'&&request.method==='GET')return json(await operationsSummary(env));
  if(route==='audit'&&request.method==='GET'){
   const before=Number(url.searchParams.get('before'))||Number.MAX_SAFE_INTEGER;
   const items=(await db.prepare('SELECT a.id,a.comment_id,a.action,a.created_at,c.app,c.name FROM moderation_audit a LEFT JOIN comments c ON c.id=a.comment_id WHERE a.id<? ORDER BY a.id DESC LIMIT 26').bind(before).all()).results;
   return json({items:items.slice(0,25),next:items.length>25?items[24].id:null});
  }
  if(route==='seo'&&request.method==='GET')return json(SEO_REPORT);
  if(route==='summary'&&request.method==='GET'){
   return json({comments:(await db.prepare("SELECT status,moderation_state,COUNT(*) AS total FROM comments GROUP BY status,moderation_state").all()).results,reports:(await db.prepare("SELECT COUNT(*) AS total FROM comment_reports WHERE status='open'").first()).total});
  }
  if(route==='comments'&&request.method==='GET'){
   const clauses=['1=1'],values=[],s=url.searchParams;
   if(APPS.includes(s.get('app'))){clauses.push('c.app=?');values.push(s.get('app'))}
   const state=s.get('status')||'pending';
   if(['rejected','trash'].includes(state)){clauses.push('c.moderation_state=?');values.push(state)}
   else{clauses.push("c.moderation_state='normal'");if(['pending','approved','hidden'].includes(state)){clauses.push('c.status=?');values.push(state)}}
   if(['review','idea','bug'].includes(s.get('kind'))){clauses.push('c.kind=?');values.push(s.get('kind'))}
   const q=(s.get('q')||'').slice(0,100);if(q){clauses.push('(c.name LIKE ? ESCAPE \'\\\' OR c.message LIKE ? ESCAPE \'\\\' OR c.reply LIKE ? ESCAPE \'\\\')');const term='%'+q.replace(/[\\%_]/g,'\\$&')+'%';values.push(term,term,term)}
   if(s.get('reported')==='1')clauses.push("EXISTS(SELECT 1 FROM comment_reports r WHERE r.comment_id=c.id AND r.status='open')");
   const before=Number(s.get('before'));if(Number.isSafeInteger(before)&&before>0){clauses.push('c.id<?');values.push(before)}
   const rows=(await db.prepare('SELECT '+fields+' FROM comments c WHERE '+clauses.join(' AND ')+' ORDER BY c.id DESC LIMIT 26').bind(...values).all()).results;
   const more=rows.length>25;return json({items:rows.slice(0,25),next:more?rows[24].id:null});
  }
  if(route==='comment'&&request.method==='GET'){
   const id=Number(url.searchParams.get('id'));if(!Number.isSafeInteger(id)||id<1)return json({error:'Identificador no válido'},400);
   const comment=await db.prepare('SELECT '+fields+' FROM comments c WHERE c.id=?').bind(id).first();if(!comment)return json({error:'Comentario no encontrado'},404);
   return json({comment,original:await db.prepare('SELECT before_json FROM moderation_audit WHERE comment_id=? AND action NOT LIKE \'report:%\' ORDER BY id LIMIT 1').bind(id).first(),reports:(await db.prepare('SELECT * FROM comment_reports WHERE comment_id=? ORDER BY id DESC').bind(id).all()).results,history:(await db.prepare('SELECT * FROM moderation_audit WHERE comment_id=? ORDER BY id DESC LIMIT 100').bind(id).all()).results});
  }
  if(route==='comments'&&request.method==='PATCH'){
   const b=await readBody(request);if(!Number.isSafeInteger(b.id)||b.id<1)return json({error:'Identificador no válido'},400);
   const old=await db.prepare('SELECT * FROM comments WHERE id=?').bind(b.id).first();if(!old)return json({error:'Comentario no encontrado'},404);
   if(b.version!==old.updated_at)return json({error:'Este comentario ha cambiado. Actualiza antes de guardar.'},409);
   let next={...old};const action=b.action||'edit';
   if(action==='trash'){next.moderation_state='trash';next.status='hidden'}
   else if(action==='restore'){next.moderation_state='normal';next.status='pending'}
   else if(action==='reject'){next.moderation_state='rejected';next.status='hidden'}
   else if(action==='approve'){next.moderation_state='normal';next.status='approved'}
   else if(action==='hide'){next.moderation_state='normal';next.status='hidden'}
   else if(action!=='edit')return json({error:'Acción no válida'},400);
   if(action==='edit'){
    if(typeof b.name!=='string'||b.name.trim().length<2||b.name.length>60||typeof b.message!=='string'||b.message.trim().length<10||b.message.length>3000||typeof b.reply!=='string'||b.reply.length>3000||!['review','idea','bug'].includes(b.kind)||!Number.isInteger(b.rating)||b.rating<0||b.rating>5)return json({error:'Revisa el nombre, mensaje, respuesta y valoración'},400);
    Object.assign(next,{name:b.name.trim(),message:b.message.trim(),reply:b.reply.trim(),kind:b.kind,rating:b.rating});
    if(old.message!==next.message||old.name!==next.name)next.edited_at=new Date().toISOString();
   }
   if(b.note!==undefined){if(typeof b.note!=='string'||b.note.length>2000)return json({error:'Nota demasiado larga'},400);next.moderation_note=b.note.trim()}
   next.updated_at=new Date(Math.max(Date.now(),Date.parse(old.updated_at)+1)).toISOString();
   // Both writes are atomic. A unique update timestamp prevents stale parallel saves.
   const update=db.prepare('UPDATE comments SET name=?,message=?,kind=?,rating=?,reply=?,status=?,moderation_state=?,moderation_note=?,edited_at=?,updated_at=? WHERE id=? AND updated_at=?').bind(next.name,next.message,next.kind,next.rating,next.reply,next.status,next.moderation_state,next.moderation_note,next.edited_at,next.updated_at,next.id,old.updated_at);
   const audit=db.prepare('INSERT INTO moderation_audit(comment_id,action,before_json,after_json,created_at) SELECT ?,?,?,?,? WHERE changes()=1').bind(next.id,action,JSON.stringify(old),JSON.stringify(next),next.updated_at);
   const result=await batch(db,[update,audit]);if(!result[0].meta.changes)return json({error:'El comentario ha cambiado; vuelve a cargarlo'},409);
   return json({ok:true,comment:next});
  }
  if(route==='comments'&&request.method==='DELETE'){
   const b=await readBody(request);if(!Number.isSafeInteger(b.id)||b.confirmation!=='ELIMINAR')return json({error:'Confirma la eliminación permanente'},400);
   const old=await db.prepare('SELECT * FROM comments WHERE id=?').bind(b.id).first();if(!old)return json({error:'Comentario no encontrado'},404);
   if(old.moderation_state!=='trash'||old.updated_at!==b.version)return json({error:'Actualiza el comentario y envíalo a la papelera antes de eliminarlo'},409);
   // Conditional deletes prevent a stale purge from removing a restored comment.
   const eligible="EXISTS(SELECT 1 FROM comments WHERE id=? AND moderation_state='trash' AND updated_at=?)";
   const result=await batch(db,[db.prepare('DELETE FROM comment_reports WHERE comment_id=? AND '+eligible).bind(b.id,b.id,b.version),db.prepare('DELETE FROM moderation_audit WHERE comment_id=? AND '+eligible).bind(b.id,b.id,b.version),db.prepare("DELETE FROM comments WHERE id=? AND moderation_state='trash' AND updated_at=?").bind(b.id,b.version)]);
   if(!result[2].meta.changes)return json({error:'El comentario ha cambiado'},409);return json({ok:true});
  }
  if(route==='reports'&&request.method==='GET'){
   const status=['open','resolved','dismissed'].includes(url.searchParams.get('status'))?url.searchParams.get('status'):'open';
   const before=Number(url.searchParams.get('before'))||Number.MAX_SAFE_INTEGER;
   const items=(await db.prepare('SELECT r.*,c.app,c.name,c.message,c.status AS comment_status,c.moderation_state FROM comment_reports r JOIN comments c ON c.id=r.comment_id WHERE r.status=? AND r.id<? ORDER BY r.id DESC LIMIT 26').bind(status,before).all()).results;return json({items:items.slice(0,25),next:items.length>25?items[24].id:null});
  }
  if(route==='reports'&&request.method==='PATCH'){
   const b=await readBody(request);if(!Number.isSafeInteger(b.id)||!['resolved','dismissed','open'].includes(b.status))return json({error:'Datos no válidos'},400);
   const old=await db.prepare('SELECT * FROM comment_reports WHERE id=?').bind(b.id).first();if(!old)return json({error:'Denuncia no encontrada'},404);
   const now=new Date().toISOString();await batch(db,[db.prepare('UPDATE comment_reports SET status=?,updated_at=? WHERE id=?').bind(b.status,now,b.id),db.prepare('INSERT INTO moderation_audit(comment_id,action,before_json,after_json,created_at) VALUES(?,?,?,?,?)').bind(old.comment_id,'report:'+b.status,JSON.stringify(old),JSON.stringify({...old,status:b.status}),now)]);return json({ok:true});
  }
  return json({error:'No encontrado'},404);
 }catch(e){return json({error:e.status?'Datos no válidos':'No se ha podido completar la operación'},e.status||500)}
}
