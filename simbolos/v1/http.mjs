import {encodeMicro,readMicro,microLevel,microLimit} from './micro.mjs';
import {maxTextBytes} from './text.mjs';
import {render,readImage,limits} from './codec.mjs';
import {toPNG,fromPNG,readBounded} from './png.mjs';
import {findCorners} from './finders.mjs';
export const servicePath='/api/simbolos/v1';
export async function handle(request){
 const headers={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Methods':'GET,POST,OPTIONS','Access-Control-Allow-Headers':'Content-Type','Access-Control-Expose-Headers':'X-UrySoft-Level,X-UrySoft-Corrected,X-UrySoft-Kind','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'};
 function json(value,status){return Response.json(value,{status,headers})}
 const u=new URL(request.url),action=u.pathname.slice(servicePath.length).replace(/^\//,'');
 if(request.method==='OPTIONS')return new Response(null,{status:204,headers});
 if(!action&&request.method==='GET')return json({version:1,limits:[...limits.slice(1),microLimit],encode:'POST encode?level=3',decode:'POST decode',documentation:'https://github.com/UrySoft/urysoft/tree/main/simbolos'},200);
 if(!['encode','decode'].includes(action))return json({error:'Ruta desconocida'},404);
 if(request.method!=='POST')return json({error:'Se requiere POST'},405);
 const input=request.headers.get('content-type')?.split(';')[0];
 const text=action==='encode'&&input==='text/plain';
 if(!text&&input!==(action==='encode'?'application/octet-stream':'image/png'))return json({error:'Tipo de contenido incompatible'},415);
 try{
  if(action==='encode'){
   const level=Number(u.searchParams.get('level')??3);if(!Number.isInteger(level)||(!limits[level]&&level!==microLevel))return json({error:'Nivel entre 1 y 5'},400);
   const limit=level===microLevel?microLimit:text?maxTextBytes:limits[level];
   const data=request.body?await readBounded(request.body,limit):new Uint8Array();
   if(text)new TextDecoder('utf-8',{fatal:true}).decode(data);
   const axes=['columns','rows'].filter(key=>u.searchParams.has(key));
   if(axes.length>1)return json({error:'Fija solo filas o columnas.'},400);
   const axis=axes[0]||'auto',value=Number(u.searchParams.get(axis));
   if(axes.length&&(!Number.isInteger(value)||value<1||value>60))return json({error:'Dimensión entre 1 y 60.'},400);
   const marks=u.searchParams.get('marks')??'1';
   if(!['0','1'].includes(marks))return json({error:'marks debe ser 0 o 1.'},400);
   if(level===microLevel&&(axes.length||marks!=='1'))return json({error:'El nivel 5 usa una imagen cuadrada de tamaño fijo con su marca de lectura.'},400);
   const png=level===microLevel?await encodeMicro(data,text):await toPNG(render(data,level,text,axis,value,marks==='1'));
   return new Response(png,{headers:{...headers,'Content-Type':'image/png'}});
  }
  const img=await fromPNG(await readBounded(request.body,12000000));
  const corners=u.searchParams.get('corners');let points;
  if(corners){try{points=JSON.parse(corners)}catch{return json({error:'corners debe ser una matriz de cuatro puntos'},400)}}
  let result=readMicro(img);
  if(!result){
   try{result=readImage(img,points)}catch(e){if(corners)throw e;for(const compact of [false,true]){const auto=findCorners(img,compact);if(auto){try{result=readImage(img,auto);break}catch{/* Try the other finder geometry. */}}}if(!result)throw e}
  }
  return new Response(result.data,{headers:{...headers,'Content-Type':'application/octet-stream','X-UrySoft-Kind':result.kind,'X-UrySoft-Level':String(result.level),'X-UrySoft-Corrected':String(result.corrected)}});
 }catch(e){return json({error:e.message||'Solicitud inválida'},e.message==='Tamaño excesivo'?413:422)}
}
