'use strict';
async function updateDownload(){
  const r=await fetch('/updates/teclairc.json',{cache:'no-store'});if(!r.ok)return;
  const data=await r.json(),name=data.versionName;
  if(data.app!=='teclairc'||data.status!=='published'||typeof name!=='string'||!/^\d+(?:\.\d+){1,3}$/.test(name))return;
  const url=new URL(data.downloadUrl);
  const own=url.href===location.origin+'/descargas/teclairc/TeclaIRC-'+name+'.apk';
  const release=url.href==='https://github.com/UrySoft/urysoft/releases/download/teclairc-'+name+'/TeclaIRC-'+name+'.apk';
  if(!own&&!release)return;
  document.querySelector('#download').href=url.href;
  document.querySelector('#version').textContent='Versión '+name+' · Android 8 o posterior';
}
async function updateStatus(){
  const r=await fetch('/api/teclairc/live/status',{cache:'no-store'});if(!r.ok)return;
  const status=await r.json();
  document.querySelector('#availability').textContent=status.available===true?'Para suscribirte o restaurar tu compra, abre Ajustes → TeclaIRC Premium en la aplicación.':'El alta de nuevas suscripciones está pendiente de activación.';
}
updateDownload().catch(()=>{});updateStatus().catch(()=>{});
