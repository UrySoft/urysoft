import {t} from './i18n.js?v=4.0.0';
import {encodeCompact,robustWave,TYPES} from './optics.js?v=4.0.0';
// Codec 4 is a control envelope, never text. The original sender and full
// transfer identifier bind the acknowledgement to one completed transmission.
export function makeAck({sender,target,transfer}){
 if(!Number.isInteger(target)||target<1||target>65535||!Number.isInteger(sender)||sender<1||sender>65535||!Number.isInteger(transfer)||transfer<0||transfer>0xffffffff)throw Error(t('Confirmación no válida.'));
 const payload=Uint8Array.of(target>>8,target&255),frame=encodeCompact({sender,transfer,codec:4,payload}),waves=robustWave(frame,{period:400});
 return{frame,waves,duration:waves.reduce((n,w)=>n+w.duration,0)};
}
export class AckWait{
 constructor({sender,transfer,now=()=>performance.now(),timeout=210000}){Object.assign(this,{sender,transfer,now});this.until=now()+timeout;this.active=true;}
 remaining(){return this.active?Math.max(0,this.until-this.now()):0;}
 accept(frame){
  if(!this.active||!this.remaining()||frame.type!==TYPES.ACK||frame.codec!==4||frame.channel!==7||frame.seq!==0||frame.total!==1||frame.payload.length!==2||frame.sender===this.sender||frame.transfer!==this.transfer||((frame.payload[0]<<8)|frame.payload[1])!==this.sender)return false;
  this.active=false;return true;
 }
 cancel(){this.active=false;}
}
