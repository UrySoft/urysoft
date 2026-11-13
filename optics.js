import {t} from './i18n.js?v=4.0.0';
// Monochrome text transport. Compatible with verified LUMEN 2.2 text frames.
import {LuminanceGate} from './signal.js?v=4.0.0';
export const WHITE=[255,255,255],BLACK=[0,0,0];
export const TYPES=Object.freeze({TEXT:1,ACK:2});
export const HEADER=18, MAX_PAYLOAD=192;
export const PERIODS=[80,120,150,200,240,400,600];
export function crc16(bytes){let c=0xffff;for(const v of bytes){c^=v<<8;for(let j=0;j<8;j++)c=(c&0x8000)?((c<<1)^0x1021)&65535:(c<<1)&65535;}return c;}
export function encodeFrame({type,channel=7,sender,transfer,seq=0,total=1,payload=new Uint8Array()}){
 if(payload.length>MAX_PAYLOAD||type!==TYPES.TEXT||seq<0||seq>=total||total<1||total>16)throw Error(t('Paquete no válido'));
 const out=new Uint8Array(HEADER+payload.length+2),v=new DataView(out.buffer);
 out[0]=0x4c;out[1]=0x4d;out[2]=1;out[3]=type;v.setUint16(4,sender);v.setUint16(6,channel);v.setUint32(8,transfer);v.setUint16(12,seq);v.setUint16(14,total);v.setUint16(16,payload.length);out.set(payload,HEADER);v.setUint16(out.length-2,crc16(out.subarray(0,-2)));return out;
}
export function encodeCompact({sender,transfer,codec=0,payload}){if(payload.length>MAX_PAYLOAD||![0,1,2,3,4].includes(codec))throw Error(t('Paquete compacto no válido'));const out=new Uint8Array(12+payload.length),v=new DataView(out.buffer);out.set([0x4c,0x43,codec]);v.setUint16(3,sender);v.setUint32(5,transfer);out[9]=payload.length;out.set(payload,10);v.setUint16(out.length-2,crc16(out.subarray(0,-2)));return out;}
export function decodeFrame(bytes){
 if(bytes[0]===0x4c&&bytes[1]===0x43){if(bytes.length<12||bytes[2]>4||bytes[9]>MAX_PAYLOAD||bytes.length!==12+bytes[9])throw Error(t('Paquete compacto no válido'));const v=new DataView(bytes.buffer,bytes.byteOffset,bytes.byteLength);if(crc16(bytes.subarray(0,-2))!==v.getUint16(bytes.length-2))throw Error(t('Error de integridad'));return{type:bytes[2]===4?TYPES.ACK:TYPES.TEXT,sender:v.getUint16(3),transfer:v.getUint32(5),channel:7,seq:0,total:1,codec:bytes[2],payload:bytes.slice(10,-2)};}
 if(bytes.length<HEADER+2)throw Error(t('Paquete incompleto'));const v=new DataView(bytes.buffer,bytes.byteOffset,bytes.byteLength);
 if(bytes[0]!==0x4c||bytes[1]!==0x4d||bytes[2]!==1||bytes[3]!==TYPES.TEXT)throw Error(t('Cabecera no válida'));
 const n=v.getUint16(16),total=v.getUint16(14),seq=v.getUint16(12);
 if(n>MAX_PAYLOAD||bytes.length!==HEADER+n+2||!total||total>16||seq>=total)throw Error(t('Longitud no válida'));
 if(crc16(bytes.subarray(0,-2))!==v.getUint16(bytes.length-2))throw Error(t('Error de integridad'));
 return {type:bytes[3],sender:v.getUint16(4),channel:v.getUint16(6),transfer:v.getUint32(8),seq,total,payload:bytes.slice(HEADER,-2)};
}
export function bytesToSymbols(bytes){const bits=[];for(const b of bytes)for(let k=7;k>=0;k--)bits.push((b>>k)&1);return bits;}
export function symbolsToBytes(bits){const out=new Uint8Array(Math.floor(bits.length/8));for(let i=0;i<out.length;i++)for(let k=0;k<8;k++)out[i]=(out[i]<<1)|bits[i*8+k];return out;}
export function robustWave(bytes,{period=400,beacon=false}={}){
 const waves=[],add=(rgb,ticks,tag)=>waves.push({rgb,duration:period*ticks,tag});
 add(BLACK,4,'guard');add(WHITE,8,'start');add(BLACK,3,'guard');
 for(let i=0;i<4;i++){add(WHITE,1,'calibration');add(BLACK,1,'calibration');}
 add(WHITE,beacon?6:4,'sync');add(BLACK,3,'guard');if(beacon)return waves;
 let light=true;for(const bit of bytesToSymbols(bytes)){add(light?WHITE:BLACK,bit?2:1,'data');light=!light;}
 add(light?WHITE:BLACK,4,'end');add(BLACK,4,'end');return waves;
}
export const frameWave=robustWave;
export function estimatedMs(length,{period=400}={}){return(38+(HEADER+2+length)*8*1.5)*period;}
export class PulseDecoder {
 constructor({onFrame=()=>{},onError=()=>{},onState=()=>{},onBeacon=()=>{}}={}){Object.assign(this,{onFrame,onError,onState,onBeacon});this.reset();}
 reset(){this.level=false;this.edgeAt=undefined;this.lastTime=undefined;this.lastClock=null;this.resetPacket();}
 resetPacket(){this.phase='idle';this.period=200;this.cal=[];this.calEdges=[];this.bits=[];this.expected=0;this.dataTick=0;this.fallDelay=0;this.onState('listening');}
 feed(time,lit,edgeTime=time){
  if(this.edgeAt===undefined){this.edgeAt=time;this.level=lit;this.lastTime=time;return;}
  if(this.phase!=='idle'&&this.phase!=='data'&&time-this.edgeAt>Math.max(1400,(this.phase==='sync'?this.period*8:this.marker*1.15)))this.fail(t('Sincronización incompleta: repite la prueba de luz'));
  if(this.phase==='data'&&this.lastTime!==undefined&&time-this.lastTime>Math.max(650,this.period*2.8)){this.fail(t('Se han perdido demasiados fotogramas'));}
  this.lastTime=time;
  if(lit!==this.level){const end=Math.max(this.edgeAt,Math.min(time,edgeTime));const segment={lit:this.level,start:this.edgeAt,end,duration:end-this.edgeAt};this.segment(segment);this.level=lit;this.edgeAt=end;}
  else if(this.phase==='data'&&time-this.edgeAt>this.period*3.2+350)this.fail(t('Se ha interrumpido la señal óptica'));
 }
 segment(p){
  if(this.phase==='idle'){if(p.lit&&p.duration>=440&&p.duration<20000){this.marker=p.duration;this.phase='markerGap';this.onState('calibrating');}return;}
  if(this.phase==='markerGap'){if(p.lit||p.duration<this.marker*.12||p.duration>this.marker*.9){this.resetPacket();return;}this.phase='calibration';this.cal=[];this.calEdges=[p.end];return;}
  if(this.phase==='calibration'){
   if(p.lit!==(this.cal.length%2===0)||p.duration<35||p.duration>this.marker*.5){this.resetPacket();return;}
   this.cal.push(p.duration);this.calEdges.push(p.end);if(this.cal.length===8){
    const measured=this.cal.reduce((s,t)=>s+t,0)/this.cal.length;
    if(this.cal.some(t=>t<measured*.25||t>measured*2)){this.resetPacket();return;}
    this.period=PERIODS.reduce((best,t)=>Math.abs(t-measured)<Math.abs(best-measured)?t:best,200);
    // The transmitter schedules a shared, fixed clock. Fit rising and falling
    // edges separately: real cameras can elongate white and shorten black.
    const rising=[],falling=[];this.calEdges.forEach((end,k)=>(k%2?falling:rising).push(end-k*this.period));
    const mean=a=>a.reduce((s,t)=>s+t,0)/a.length;
    this.clockOrigin=mean(rising);this.fallDelay=mean(falling)-this.clockOrigin;
    if(Math.abs(this.fallDelay)>this.period*.85){this.resetPacket();return;}
    this.lastClock={period:this.period,fallDelay:this.fallDelay};this.phase='sync';
   }return;
  }
  if(this.phase==='sync'){
   if(!p.lit||p.duration-this.fallDelay<this.period*2.8||p.duration-this.fallDelay>this.period*7.5){this.resetPacket();return;}
   if(p.duration-this.fallDelay>this.period*5){const period=this.period;this.resetPacket();this.onBeacon({period});return;}
   this.phase='syncGap';return;
  }
  if(this.phase==='syncGap'){
   if(p.lit||p.duration+this.fallDelay<this.period*1.8||p.duration+this.fallDelay>this.period*4.3){this.resetPacket();return;}
   this.phase='data';this.bits=[];this.expected=0;this.dataTick=0;this.dataOrigin=this.clockOrigin+15*this.period;this.onState('receiving');return;
  }
  if(this.phase==='data'){
   const tick=Math.round((p.end-this.dataOrigin-(p.lit?this.fallDelay:0))/this.period),delta=tick-this.dataTick;
   if(delta!==1&&delta!==2){this.fail(t('Se han perdido pulsos: usa un ritmo más lento'));return;}
   this.dataTick=tick;this.bits.push(delta-1);
   if(this.bits.length%8===0){const bytes=symbolsToBytes(this.bits,'mono');if(bytes.length===10&&bytes[0]===0x4c&&bytes[1]===0x43){if(bytes[2]>4||bytes[9]>MAX_PAYLOAD){this.fail(t('Cabecera compacta no válida'));return;}this.expected=12+bytes[9];}if(bytes.length===HEADER&&!this.expected){const n=(bytes[16]<<8)|bytes[17];if(bytes[0]!==0x4c||bytes[1]!==0x4d||n>MAX_PAYLOAD){this.fail(t('Cabecera óptica no reconocida'));return;}this.expected=HEADER+n+2;}
    if(this.expected&&bytes.length===this.expected){try{const frame=decodeFrame(bytes),period=PERIODS.reduce((b,x)=>Math.abs(x-this.period)<Math.abs(b-this.period)?x:b,200);this.resetPacket();this.onFrame(frame,{period,mode:'mono',protocol:2});}catch(e){this.fail(e.message);}}
   }
  }
 }
 fail(message){this.onError(message);this.resetPacket();}
}

export class OpticalDecoder{
 constructor({onFrame=()=>{},onError=()=>{},onState=()=>{},onBeacon=()=>{},onSignal=()=>{},threshold=8}={}){
  Object.assign(this,{onFrame,onError,onState,onBeacon,onSignal,threshold});this.gate=new LuminanceGate({minContrast:threshold});
  this.mono=new PulseDecoder({onFrame:(f,p)=>this.onFrame(f,p),onError:e=>this.onError(e),onState:s=>this.onState(s),onBeacon:p=>this.onBeacon(p)});
 }
 get phase(){return this.mono.phase;}
 get on(){return this.gate.lit;}
 reset(){this.gate.reset();this.mono.reset();}
 feed(time,rgb){this.gate.minContrast=this.threshold;const s=this.gate.feed(time,rgb);this.onSignal(s);this.mono.feed(time,s.lit,s.edgeTime);}
}
