import {apps} from './catalog-admin.js';

(()=>{
 if(/^\/(admin|api|foldery\/premium|widgery\/premium)/.test(location.pathname)||navigator.doNotTrack==='1'||navigator.globalPrivacyControl)return;
 const path=location.pathname.replace(/index\.html$/,''),source=new URLSearchParams(location.search).get('utm_source')||'';
 const emit=(event,target='')=>{const payload={id:crypto.randomUUID(),event,path,target,referrer:document.referrer,language:(document.getElementById('lang')?.value||document.documentElement.lang).slice(0,2),source};fetch('/api/analytics',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload),keepalive:true,credentials:'omit'}).catch(()=>{})};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>emit('page'),{once:true});else emit('page');let visible=0,last=Date.now(),engaged=false;
 const timer=setInterval(()=>{const now=Date.now();if(!document.hidden)visible+=Math.min(now-last,1000);last=now;if(visible>=30000&&!engaged){engaged=true;emit('engaged');clearInterval(timer)}},1000);
 document.addEventListener('click',e=>{const a=e.target.closest?.('a[href]');if(!a)return;let u;try{u=new URL(a.href,location.href)}catch{return}if(!['http:','https:'].includes(u.protocol))return;
  if(/\.apk$/i.test(u.pathname)){
   const name=u.pathname.toLowerCase(),app=apps.find(({id})=>name.includes(id))?.id;if(app)emit('download',app);
  }else emit('click',u.origin+u.pathname);
 },{capture:true});
})();
