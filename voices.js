const normalize=value=>String(value||'').normalize('NFD').replace(/\p{Diacritic}/gu,'').toLowerCase();
const names={male:['alvaro','arnau','dario','elias','nil','saul','teo','tristan','marcelo','lorenzo','gonzalo','juan','manuel','emilio','luis','javier','andres','carlos','jorge','cecilio','gerardo'],female:['elvira','abril','estrella','irene','laia','lia','triana','vera','ximena','isidora','arabella','sofia','catalina','salome','maria','belkys','ramona','andrea','marta','teresa','karla','dalia','beatriz','candela','carlota']};
export const voiceId=v=>JSON.stringify([v.voiceURI||'',v.lang||'',v.name||'']);
export function voiceGender(voice){const text=normalize(`${voice.name||''} ${voice.voiceURI||''}`).replace(/multilingualneural|neural|dragonhdlatest/g,' ');if(/\b(female|femenina|femenino|mujer)\b/.test(text))return'female';if(/\b(male|masculina|masculino|hombre)\b/.test(text))return'male';const words=new Set(text.split(/[^a-z]+/));for(const gender of['female','male'])if(names[gender].some(n=>words.has(n)))return gender;return'unknown';}
export function languageVoices(voices,language){const base=language.toLowerCase().split('-')[0];return voices.filter(v=>String(v.lang).toLowerCase().split(/[-_]/)[0]===base);}
export function chooseVoice(voices,{language='es-ES',gender='male',selected=''}={}){
 gender=gender==='female'?'female':'male';const available=languageVoices(voices,language),manual=available.find(v=>voiceId(v)===selected);
 const score=v=>(voiceGender(v)===gender?100:voiceGender(v)==='unknown'?40:0)+(v.lang.toLowerCase()===language.toLowerCase()?20:0)+(v.localService?5:0)+(v.default?1:0);
 const voice=manual||[...available].sort((a,b)=>score(b)-score(a))[0]||null,matched=!!voice&&voiceGender(voice)===gender;
 return{voice,matched,manual:!!manual,pitch:matched||manual&&voiceGender(manual)!=='unknown'?1:gender==='male'?.8:1.18,gender};
}
