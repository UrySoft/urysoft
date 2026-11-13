import {t} from './i18n.js?v=4.0.0';
const stopped=()=>new DOMException(t('Detenido'),'AbortError');
export function stopStream(stream){stream?.getTracks().forEach(track=>track.stop());}
export async function listCameras(mediaDevices){
 if(!mediaDevices?.enumerateDevices)return[];
 const seen=new Set();return(await mediaDevices.enumerateDevices()).filter(d=>d.kind==='videoinput'&&d.deviceId&&!seen.has(d.deviceId)&&seen.add(d.deviceId)).map((d,i)=>({id:d.deviceId,label:d.label||t`Cámara ${i+1}`}));
}
export function cameraConstraints(selection='rear',recording=false){
 const source=selection.startsWith('device:')?{deviceId:{exact:selection.slice(7)}}:{facingMode:{ideal:selection==='front'?'user':'environment'}};
 return{video:{...source,width:{ideal:720},height:{ideal:1280},aspectRatio:{ideal:9/16},frameRate:{ideal:recording?60:30,max:60}},audio:false};
}
export async function openCameraStream(mediaDevices,selection,{recording=false,signal}={}){
 if(!mediaDevices?.getUserMedia)throw Error(t('Abre el enlace seguro en Chrome para Android para usar la cámara.'));
 if(signal?.aborted)throw stopped();const stream=await mediaDevices.getUserMedia(cameraConstraints(selection,recording));
 if(signal?.aborted){stopStream(stream);throw stopped();}return stream;
}
export function supportsTorch(track){
 let capability,setting;try{capability=track.getCapabilities?.().torch;}catch{}try{setting=track.getSettings?.().torch;}catch{}
 return capability===true||Array.isArray(capability)&&capability.includes(true)&&capability.includes(false)||capability===undefined&&typeof setting==='boolean';
}
export class TorchLight{
 constructor(stream){this.stream=stream;this.track=stream.getVideoTracks()[0];this.closed=false;this.on=false;}
 async set(on){
  if(this.closed||this.track.readyState==='ended')throw Error(t('El flash se ha desconectado. Vuelve a seleccionarlo.'));
  await this.track.applyConstraints({torch:!!on,advanced:[{torch:!!on}]});
  if(this.closed||this.track.readyState==='ended')throw stopped();
  const actual=this.track.getSettings?.().torch;if(typeof actual==='boolean'&&actual!==!!on)throw Error(t('La cámara no ha aplicado el cambio del flash. Prueba otra cámara o usa Pantalla.'));
  this.on=!!on;
 }
 close(){this.closed=true;this.on=false;stopStream(this.stream);}
}
export async function openTorch(mediaDevices,{selection='auto',signal}={}){
 if(globalThis.LumenNative){
  if(signal?.aborted)throw stopped();
  const error=LumenNative.prepareTorch();if(error)throw Error(error);
  return {stream:null,track:{label:'Android',addEventListener(){}},closed:false,async set(on){if(this.closed||signal?.aborted)throw stopped();const error=LumenNative.setTorch(!!on);if(error)throw Error(error);},close(){this.closed=true;LumenNative.setTorch(false);}};
 }

 if(!mediaDevices?.getUserMedia)throw Error(t('Este navegador no permite controlar el flash. Abre el enlace seguro en Chrome para Android o usa Pantalla.'));
 const tried=new Set();let candidates=selection==='auto'?['rear']:[selection];
 for(let at=0;at<candidates.length;at++){
  if(signal?.aborted)throw stopped();let stream;
  try{
   stream=await openCameraStream(mediaDevices,candidates[at],{signal});const track=stream.getVideoTracks()[0],id=track?.getSettings?.().deviceId;
   if(id)tried.add(id);
   if(track&&supportsTorch(track)){
    const light=new TorchLight(stream);await light.set(false);if(signal?.aborted){light.close();throw stopped();}return light;
   }
  }catch(error){stopStream(stream);if(['NotAllowedError','SecurityError','AbortError'].includes(error.name))throw error;if(selection!=='auto')throw error;}
  stopStream(stream);
  if(selection==='auto'&&at===0){const cameras=await listCameras(mediaDevices);cameras.sort((a,b)=>Number(/back|rear|tras|posterior|environment/i.test(b.label))-Number(/back|rear|tras|posterior|environment/i.test(a.label)));candidates.push(...cameras.filter(d=>!tried.has(d.id)).map(d=>'device:'+d.id));}
 }
 throw Error(selection==='auto'?t('No se ha encontrado un flash controlable. Elige otra cámara o usa Pantalla.'):t('Esta cámara no ofrece un flash controlable. Elige otra o usa Pantalla.'));
}
