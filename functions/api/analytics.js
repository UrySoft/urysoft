import {json,sameOrigin,readBody,rateLimit} from '../../server/admin-common.js';
import {normalizeEvent,saveEvent} from '../../server/site-analytics.js';
export async function onRequest({request,env}){
 if(request.method!=='POST')return json({error:'Método no permitido'},405);
 if(!sameOrigin(request))return json({error:'Origen no permitido'},403);
 if(!env.COMMENTS_DB||!env.COMMENTS_ADMIN_KEY)return json({error:'No disponible'},503);
 if(request.headers.get('DNT')==='1'||request.headers.get('Sec-GPC')==='1')return new Response(null,{status:204});
 try{const b=await readBody(request,1800),e=normalizeEvent(b,request);if(!e)return new Response(null,{status:204});if(!await rateLimit(request,env,'analytics',120,60000))return json({error:'Límite de frecuencia'},429);await saveEvent(env.COMMENTS_DB,e);return new Response(null,{status:204})}catch(e){return json({error:'No se ha podido registrar'},e.status||500)}
}
