// Recognition results are revisions of indexed hypotheses, not new messages.
const clean=text=>String(text??'').replace(/\s+/g,' ').trim();
const words=text=>clean(text).split(' ').filter(Boolean);
const key=word=>word.toLocaleLowerCase().replace(/[^\p{L}\p{N}]/gu,'');
function combineHypotheses(left,right){
 if(!left)return right;if(!right)return left;
 const a=words(left),b=words(right),ak=a.map(key),bk=b.map(key);
 // Some Android engines return the whole growing utterance at a new index.
 // Replace that cumulative hypothesis; preserve repetitions inside a result.
 if(ak.every((v,i)=>v===bk[i]))return right;
 if(bk.every((v,i)=>v===ak[i]))return left;
 let overlap=0;for(let n=Math.min(a.length,b.length);n>=2;n--)if(ak.slice(-n).every((v,i)=>v===bk[i])){overlap=n;break;}
 return [...a,...b.slice(overlap)].join(' ');
}
export class TranscriptBuffer {
 constructor(){this.committed='';this.slots=[];}
 begin(){this.committed=this.text;this.slots=[];}
 update(event){
  const results=event.results;if(!results)return this.text;
  const from=Math.max(0,Math.min(event.resultIndex??0,results.length));
  this.slots.length=results.length;
  for(let i=from;i<results.length;i++)this.slots[i]={text:clean(results[i]?.[0]?.transcript),final:!!results[i]?.isFinal};
  return this.text;
 }
 get segment(){return this.slots.reduce((text,r)=>combineHypotheses(text,r?.text||''),'');}
 get text(){return clean(`${this.committed} ${this.segment}`);}
}

// One recognition turn per phrase prevents continuous Android engines from
// emitting several copies. A held button may start another independent turn.
export class DictationCapture {
 constructor({Recognition,lang='es-ES',local=false,onText=()=>{},onState=()=>{},onError=()=>{},onComplete=()=>{},schedule=setTimeout,unschedule=clearTimeout}={}){
  Object.assign(this,{Recognition,lang,local,onText,onState,onError,onComplete});this.schedule=(fn,ms)=>schedule(fn,ms);this.unschedule=id=>unschedule(id);this.buffer=new TranscriptBuffer();this.holding=false;this.finished=false;this.recognizer=null;this.restart=null;this.finalTimer=null;
 }
 start(){if(this.finished||this.holding)return;this.holding=true;this.startTurn();}
 startTurn(){
  if(!this.holding||this.finished)return;this.buffer.begin();const r=new this.Recognition();this.recognizer=r;r.lang=this.lang;r.continuous=false;r.interimResults=true;r.maxAlternatives=1;if(this.local)r.processLocally=true;
  const current=()=>!this.finished&&this.recognizer===r;
  r.onstart=()=>{if(!current())return;this.onState(this.holding?'listening':'finishing');if(!this.holding)try{r.stop();}catch{}};
  r.onresult=event=>{if(current())this.onText(this.buffer.update(event));};
  r.onerror=event=>{if(!current())return;if(event.error==='no-speech'||event.error==='aborted'&&!this.holding)return;this.abort();this.onError(event.error);};
  r.onend=()=>{if(!current())return;this.recognizer=null;this.unschedule(this.finalTimer);this.finalTimer=null;if(this.holding){this.onState('restarting');this.restart=this.schedule(()=>{this.restart=null;this.startTurn();},350);}else this.complete();};
  try{this.onState('starting');r.start();}catch(e){this.abort();this.onError(e.message||'audio-capture');}
 }
 release(){
  if(this.finished||!this.holding)return;this.holding=false;this.unschedule(this.restart);this.restart=null;
  if(!this.recognizer){this.complete();return;}this.onState('finishing');
  // Keep receiving the final revision after release, even if onstart is late.
  this.finalTimer=this.schedule(()=>this.complete(),4000);try{this.recognizer.stop();}catch{this.complete();}
 }
 complete(){if(this.finished)return;const text=this.buffer.text;this.finish();this.onComplete(text);}
 finish(){this.finished=true;this.holding=false;this.unschedule(this.restart);this.unschedule(this.finalTimer);const r=this.recognizer;this.recognizer=null;try{r?.abort();}catch{}this.onState('idle');}
 abort(){if(!this.finished)this.finish();}
}
