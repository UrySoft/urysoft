/* Native adapters are active only in the bundled Android application. */
(()=>{
 if(!window.LumenNative)return;
 let recognizer=null,utterance=null,sequence=0;
 const voiceEvents=new EventTarget();
 class NativeRecognition{
  constructor(){this.lang=navigator.language;this.interimResults=true;this.active=false;}
  start(){if(this.active)throw Error('Recognition already started');this.active=true;this.token=String(++sequence);recognizer=this;LumenNative.startRecognition(this.lang,this.token);}
  stop(){if(this.active)LumenNative.stopRecognition(this.token);}
  abort(){if(!this.active)return;this.active=false;LumenNative.cancelRecognition(this.token);if(recognizer===this)recognizer=null;}
 }
 window.SpeechRecognition=NativeRecognition;
 window.__lumenRecognition=(token,type,text)=>{
  const r=recognizer;if(!r||!r.active||r.token!==token)return;
  if(type==='start')r.onstart?.();
  if(type==='result'||type==='partial'){
   const item=[{transcript:text,confidence:1}];item.isFinal=type==='result';r.onresult?.({resultIndex:0,results:[item]});
  }
  if(type==='error')r.onerror?.({error:text});
  if(type==='end'){r.active=false;recognizer=null;r.onend?.();}
 };
 window.SpeechSynthesisUtterance=class{constructor(text){this.text=text;this.lang=navigator.language;this.pitch=1;this.rate=1;this.volume=1;}};
 const synthesis={
  getVoices(){try{return JSON.parse(LumenNative.voices());}catch{return[];}},
  speak(u){if(!u.text)return;utterance=u;this.speaking=true;const id=String(++sequence);u._nativeId=id;LumenNative.speak(JSON.stringify({text:u.text,language:u.lang,pitch:u.pitch,rate:u.rate,voice:u.voice?.voiceURI||'',id}));},
  cancel(){LumenNative.stopSpeaking();const u=utterance;utterance=null;this.speaking=false;u?.onend?.();},
  resume(){},pause(){this.cancel();},speaking:false,
  addEventListener:(...args)=>voiceEvents.addEventListener(...args),removeEventListener:(...args)=>voiceEvents.removeEventListener(...args)
 };
 Object.defineProperty(window,'speechSynthesis',{value:synthesis,configurable:true});
 window.__lumenSpeech=(id,type)=>{const u=utterance;if(!u||u._nativeId!==id)return;if(type==='start')u.onstart?.();else{utterance=null;synthesis.speaking=false;type==='error'?u.onerror?.({error:'synthesis-failed'}):u.onend?.();}};
 window.__lumenVoices=()=>voiceEvents.dispatchEvent(new Event('voiceschanged'));
 const ready=()=>{const select=document.getElementById('flashCamera');if(select)select.hidden=true;const light=document.getElementById('lightScreen');if(light)new MutationObserver(()=>LumenNative.brightness(!light.hidden)).observe(light,{attributes:true,attributeFilter:['hidden']});};
 document.readyState==='loading'?document.addEventListener('DOMContentLoaded',ready):ready();
})();
