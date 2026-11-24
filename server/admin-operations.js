import {liveProfile as foldery} from './foldery-live-profile.js';
import {liveProfile as widgery} from './widgery-live-profile.js';
import {liveProfile as minairo} from './minairo-live-profile.js';
import {liveProfile as storagey} from './storagey-live-profile.js';
import {liveProfile as teclairc} from './teclairc-live-profile.js';

export async function operationsSummary(env){
 const services=[];
 try{
  const latest=await env.COMMENTS_DB.prepare('SELECT MAX(day) AS day,SUM(hits) AS hits FROM analytics_counts').first();
  services.push({name:'Estadísticas y comunidad',ok:true,detail:'Base de datos accesible. Último día con actividad: '+(latest?.day||'sin eventos')+'.'});
 }catch{services.push({name:'Estadísticas y comunidad',ok:false,detail:'No se han podido consultar los contadores. Revisa tablas y conexión.'})}
 for(const [name,profile] of [['Foldery',foldery],['Widgery',widgery],['Minairó',minairo],['Storagey',storagey],['TeclaIRC',teclairc]]){
  const configured=profile(env);
  if(!configured){services.push({name:name+' Premium',ok:false,detail:'Configuración de producción incompleta o aislamiento no válido. Revisa el servicio en el proveedor.'});continue}
  try{
   const counts=(await configured.PREMIUM_DB.prepare('SELECT status,COUNT(*) AS total FROM premium_checkouts GROUP BY status').all()).results;
   const paid=await configured.PREMIUM_DB.prepare('SELECT COUNT(*) AS total FROM premium_checkouts WHERE paid_until>?').bind(Date.now()).first();
   services.push({name:name+' Premium',ok:true,detail:'Configuración presente y tabla de suscripciones accesible.',counts,entitlements:paid.total});
  }catch{services.push({name:name+' Premium',ok:false,detail:'Configuración presente; no se ha podido consultar la tabla de suscripciones.'})}
 }
 return {checkedAt:new Date().toISOString(),services};
}
