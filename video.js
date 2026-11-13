import {t} from './i18n.js?v=4.0.0';
import {demuxWebM} from './webm.js?v=4.0.0';
const aborted=()=>new DOMException(t('Análisis cancelado'),'AbortError');
function check(signal){if(signal?.aborted)throw aborted();}
function waitEvent(target,name,{signal,timeout=12000}={}){return new Promise((resolve,reject)=>{const finish=(fn,v)=>{clearTimeout(timer);target.removeEventListener(name,done);target.removeEventListener('error',fail);signal?.removeEventListener('abort',cancel);fn(v);},done=e=>finish(resolve,e),fail=()=>finish(reject,Error(t('No se pudo leer el vídeo.'))),cancel=()=>finish(reject,aborted()),timer=setTimeout(()=>finish(reject,Error(t('El vídeo tarda demasiado en responder.'))),timeout);target.addEventListener(name,done,{once:true});target.addEventListener('error',fail,{once:true});signal?.addEventListener('abort',cancel,{once:true});if(signal?.aborted)cancel();});}
export async function prepareVideo(video,blob,{signal}={}){check(signal);video.pause();video.srcObject=null;const url=URL.createObjectURL(blob);const pending=waitEvent(video,'loadeddata',{signal});video.src=url;video.load();try{await pending;return url;}catch(e){URL.revokeObjectURL(url);throw e;}}
async function analyzeCodecs(blob,receiver,point,{signal,onProgress}){
 if(typeof VideoDecoder==='undefined'||typeof EncodedVideoChunk==='undefined')throw Error(t('Usar analizador compatible.'));
 const data=demuxWebM(await blob.arrayBuffer());check(signal);const supported=await VideoDecoder.isConfigSupported(data.config);if(!supported.supported)throw Error(t('Usar analizador compatible.'));
 let frames=0,first=Infinity,last=0,failure=null;const decoder=new VideoDecoder({output:frame=>{try{if(!signal?.aborted){receiver.feed(frame,frame.timestamp/1000,frame.displayWidth,frame.displayHeight,point);frames++;first=Math.min(first,frame.timestamp);last=Math.max(last,frame.timestamp);if(frames%24===0)onProgress({fraction:Math.min(.99,(last-data.chunks[0].timestamp)/Math.max(1,data.chunks.at(-1).timestamp-data.chunks[0].timestamp)),frames,method:'frames'});}}catch(e){failure=e;}finally{frame.close();}},error:e=>{failure=e;}});
 const stop=()=>{try{decoder.close();}catch{}};signal?.addEventListener('abort',stop,{once:true});
 try{decoder.configure(data.config);for(const chunk of data.chunks){check(signal);if(failure)throw failure;while(decoder.decodeQueueSize>8){await new Promise(r=>setTimeout(r,0));check(signal);if(failure)throw failure;}decoder.decode(new EncodedVideoChunk({type:chunk.key?'key':'delta',timestamp:chunk.timestamp,data:chunk.data}));}await decoder.flush();check(signal);if(failure)throw failure;onProgress({fraction:1,frames,method:'frames'});return{frames,duration:(last-first)/1e6,method:'frames'};}finally{signal?.removeEventListener('abort',stop);stop();}
}
async function seek(video,time,signal){check(signal);if(Math.abs(video.currentTime-time)<.0001&&video.readyState>=2)return;const pending=waitEvent(video,'seeked',{signal,timeout:15000});video.currentTime=time;await pending;check(signal);}
async function analyzeCompatible(video,receiver,point,{signal,onProgress,durationHint}){
 video.pause();let duration=video.duration;
 if(!Number.isFinite(duration)){if(durationHint>0)duration=durationHint;else{await seek(video,1e10,signal);duration=video.duration;if(!Number.isFinite(duration))duration=video.currentTime;}}
 if(!Number.isFinite(duration)||duration<=0||duration>3600)throw Error(t('No se puede determinar la duración del vídeo.'));
 let frames=0;const step=1/60,count=Math.ceil(duration/step);for(let i=0;i<count;i++){check(signal);const time=Math.min(i*step,duration-.001);await seek(video,time,signal);receiver.feed(video,time*1000,video.videoWidth,video.videoHeight,point);frames++;if(i%12===0){onProgress({fraction:i/count,frames,method:'compatible'});await new Promise(r=>setTimeout(r,0));}}
 onProgress({fraction:1,frames,method:'compatible'});return{frames,duration,method:'compatible'};
}
export async function analyzeVideo(blob,video,receiver,point,{signal,onProgress=()=>{},durationHint=0}={}){
 check(signal);receiver.reset();let result;
 try{result=await analyzeCodecs(blob,receiver,point,{signal,onProgress});}catch(e){if(signal?.aborted||e.name==='AbortError')throw aborted();receiver.reset();onProgress({fraction:0,frames:0,method:'compatible'});result=await analyzeCompatible(video,receiver,point,{signal,onProgress,durationHint});}
 return result;
}
export function recordingOptions(){const types=['video/webm;codecs=vp8','video/webm;codecs=vp9','video/webm','video/mp4'];const mimeType=types.find(t=>MediaRecorder.isTypeSupported?.(t));return{...(mimeType?{mimeType}:{}),videoBitsPerSecond:2_500_000};}
