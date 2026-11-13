import {t} from './i18n.js?v=4.0.0';
const stopped=()=>new DOMException(t('Detenido'),'AbortError');
export const wait=(ms,signal)=>new Promise((resolve,reject)=>{
 const abort=()=>{clearTimeout(timer);signal?.removeEventListener('abort',abort);reject(stopped());},timer=setTimeout(()=>{signal?.removeEventListener('abort',abort);resolve();},Math.max(0,ms));
 signal?.addEventListener('abort',abort,{once:true});if(signal?.aborted)abort();
});
export async function countdown(seconds,{signal,onTick=()=>{},now=()=>performance.now(),sleep=wait}={}){
 const started=now();for(let left=Math.max(0,Math.round(seconds));left>0;left--){if(signal?.aborted)throw stopped();onTick(left);await sleep(started+(seconds-left+1)*1000-now(),signal);}if(signal?.aborted)throw stopped();
}
export async function transmit(waves,{setLight,period=400,signal,onProgress=()=>{},now=()=>performance.now(),sleep=wait}={}){
 const total=waves.reduce((n,w)=>n+w.duration,0),started=now();let elapsed=0;
 for(const wave of waves){
  if(signal?.aborted)throw stopped();if(now()-(started+elapsed)>Math.max(100,period*.7))throw Error(t('No se mantiene esta velocidad. Redúcela y repite.'));
  const before=now();await setLight(!!wave.rgb[0]);if(signal?.aborted)throw stopped();
  if(now()-before>period*.7)throw Error(t('El flash cambia demasiado despacio. Reduce la velocidad o usa Pantalla.'));
  elapsed+=wave.duration;onProgress(elapsed/total);await sleep(started+elapsed-now(),signal);
 }
 if(signal?.aborted)throw stopped();
}
