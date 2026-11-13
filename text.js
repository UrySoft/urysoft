import {t} from './i18n.js?v=4.0.0';
import {packQuick} from './quick.js?v=4.0.0';
import {packDictionary,packLz,unpack} from './compression.js?v=4.0.0';
import {TYPES,MAX_PAYLOAD,encodeFrame,encodeCompact,robustWave} from './optics.js?v=4.0.0';
export const MAX_CHARACTERS=160,MAX_TEXT_BYTES=640;
const encoder=new TextEncoder(),decoder=new TextDecoder('utf-8',{fatal:true});
export function makeTransmission(text,{sender,transfer,period=400,channel=7,optimize=true}={}){
 text=String(text).trim();if(!text)throw Error(t('Escribe o dicta un mensaje.'));if(text.length>MAX_CHARACTERS)throw Error(t`Máximo ${MAX_CHARACTERS} caracteres.`);
 const bytes=encoder.encode(text);if(bytes.length>MAX_TEXT_BYTES)throw Error(t('El mensaje es demasiado largo.'));const total=Math.ceil(bytes.length/MAX_PAYLOAD),frames=[];
 for(let seq=0;seq<total;seq++)frames.push(encodeFrame({type:TYPES.TEXT,sender,transfer,channel,seq,total,payload:bytes.slice(seq*MAX_PAYLOAD,(seq+1)*MAX_PAYLOAD)}));
 let format=t('Texto clásico');if(optimize&&channel===7){const variants=[[0,bytes,t('Texto compacto')],[1,packDictionary(bytes),'Diccionario'],[2,packLz(bytes),t('Compresión')]];const quick=packQuick(bytes);if(quick)variants.push([3,quick,t('Frase rápida')]);let best=frames,bestTime=frames.flatMap(f=>robustWave(f,{period})).reduce((n,w)=>n+w.duration,0);for(const[codec,payload,label]of variants){if(payload.length>MAX_PAYLOAD)continue;const frame=encodeCompact({sender,transfer,codec,payload}),time=robustWave(frame,{period}).reduce((n,w)=>n+w.duration,0);if(time<bestTime){best=[frame];bestTime=time;format=label;}}frames.splice(0,frames.length,...best);}
 const waves=frames.flatMap(frame=>robustWave(frame,{period}));return{text,frames,waves,format,duration:waves.reduce((n,w)=>n+w.duration,0)};
}
export class TextInbox{
 constructor({onMessage=()=>{},onPartial=()=>{},channel=7}={}){Object.assign(this,{onMessage,onPartial,channel});this.pending=new Map();this.complete=new Set();}
 accept(frame){
  if(frame.type!==TYPES.TEXT||frame.channel!==this.channel)return false;
  if(frame.total<1||frame.total>4||frame.seq>=frame.total||frame.payload.length>MAX_PAYLOAD)throw Error(t('Mensaje no válido.'));
  const key=`${frame.sender}:${frame.transfer}:${frame.channel}`;if(this.complete.has(key))return false;
  let item=this.pending.get(key);if(!item){if(this.pending.size>=16)this.pending.delete(this.pending.keys().next().value);item={total:frame.total,chunks:new Map()};this.pending.set(key,item);}
  if(item.total!==frame.total)throw Error(t('La transmisión cambió de formato.'));
  const old=item.chunks.get(frame.seq);if(old&&(!old.every((v,i)=>v===frame.payload[i])||old.length!==frame.payload.length))throw Error(t('Un fragmento no coincide.'));
  item.chunks.set(frame.seq,frame.payload.slice());this.onPartial(item.chunks.size,item.total);
  if(item.chunks.size!==item.total)return false;
  const length=[...item.chunks.values()].reduce((n,a)=>n+a.length,0);if(length>MAX_TEXT_BYTES)throw Error(t('Mensaje demasiado largo.'));const joined=new Uint8Array(length);let at=0;
  for(let i=0;i<item.total;i++){joined.set(item.chunks.get(i),at);at+=item.chunks.get(i).length;}
  const text=decoder.decode(unpack(joined,frame.codec??0));if(!text.trim()||text.length>MAX_CHARACTERS)throw Error(t('Texto recibido no válido.'));
  this.pending.delete(key);this.complete.add(key);if(this.complete.size>64)this.complete.delete(this.complete.values().next().value);this.onMessage(text,key);return true;
 }
}
