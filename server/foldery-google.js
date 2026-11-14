// Customer identity is deliberately separate from googleAdmin and its owner allowlist.
const unb64=s=>Uint8Array.from(atob(s.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));
export async function folderyGoogle(token,clientId,nonce,send=fetch,now=Date.now()){
  try{
    if(!clientId||!nonce||typeof token!=='string'||token.length>12000)return null;
    const parts=token.split('.');if(parts.length!==3||parts.some(p=>!/^[A-Za-z0-9_-]+$/.test(p)))return null;
    const decode=s=>JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(unb64(s)));
    const h=decode(parts[0]),p=decode(parts[1]),seconds=Math.floor(now/1000);
    if(h.alg!=='RS256'||typeof h.kid!=='string'||!['https://accounts.google.com','accounts.google.com'].includes(p.iss)||p.aud!==clientId||(p.azp&&p.azp!==clientId)||p.nonce!==nonce||!Number.isInteger(p.exp)||p.exp<=seconds||!Number.isInteger(p.iat)||p.iat>seconds+60||p.iat<seconds-7200||typeof p.sub!=='string'||!p.sub||p.sub.length>255||p.email_verified!==true)return null;
    const r=await send('https://www.googleapis.com/oauth2/v3/certs',{redirect:'error',signal:AbortSignal.timeout(8000)});if(!r.ok)return null;
    const keys=await r.json(),jwk=keys.keys?.find(k=>k.kid===h.kid&&k.kty==='RSA'&&k.alg==='RS256');if(!jwk)return null;
    const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
    if(!await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,unb64(parts[2]),new TextEncoder().encode(parts[0]+'.'+parts[1])))return null;
    return {sub:p.sub}; // Do not store Google profile, email or ID tokens in the payment database.
  }catch{return null}
}
