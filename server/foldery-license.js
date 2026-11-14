const encode=bytes=>btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
export const sha256=async text=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(text)))).map(v=>v.toString(16).padStart(2,'0')).join('');
export const randomToken=()=>encode(crypto.getRandomValues(new Uint8Array(32)));
export async function issueFolderyLicense(env,deviceId,accountId,paidUntil,verifiedAt,now=Date.now()){
  const exp=Math.floor(Math.min(paidUntil,verifiedAt+30*86400000)/1000);
  if(!/^[a-f0-9]{64}$/.test(deviceId)||!/^[a-f0-9]{64}$/.test(accountId)||!Number.isFinite(exp)||exp<=now/1000)return null;
  const header={alg:'RS256',typ:'JWT',kid:'foldery-2026-01'};
  const payload={iss:'https://urysoft.pages.dev',aud:'cat.urysoft.widgets',device:deviceId,account:accountId,tier:'premium',iat:Math.floor(now/1000),exp,paidThrough:Math.floor(paidUntil/1000)};
  const enc=o=>encode(new TextEncoder().encode(JSON.stringify(o)));
  const signed=enc(header)+'.'+enc(payload);
  const bytes=Uint8Array.from(atob(env.FOLDERY_LICENSE_PRIVATE_KEY),c=>c.charCodeAt(0));
  const key=await crypto.subtle.importKey('pkcs8',bytes,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
  return signed+'.'+encode(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,new TextEncoder().encode(signed)));
}
