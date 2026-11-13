let keyCache={keys:[],expires:0};
const bytes=value=>Uint8Array.from(atob(value.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));
async function getKeys(force=false){
  if(!force&&keyCache.expires>Date.now())return keyCache.keys;
  const response=await fetch('https://www.googleapis.com/oauth2/v3/certs');
  if(!response.ok)throw Error('Google verification unavailable');
  const data=await response.json();if(!Array.isArray(data.keys))throw Error('Invalid Google keys');
  const seconds=Number(response.headers.get('Cache-Control')?.match(/max-age=(\d+)/)?.[1]||300);
  keyCache={keys:data.keys,expires:Date.now()+Math.min(seconds,86400)*1000};return data.keys;
}
export async function googleAdmin(token,clientId,allowedEmail){
  try{
    if(!clientId||!allowedEmail||typeof token!=='string'||token.length>10000)return false;
    const parts=token.split('.');if(parts.length!==3)return false;
    const decode=value=>JSON.parse(new TextDecoder().decode(bytes(value)));
    const header=decode(parts[0]),payload=decode(parts[1]);
    if(header.alg!=='RS256'||typeof header.kid!=='string')return false;
    const now=Math.floor(Date.now()/1000);
    if(!['accounts.google.com','https://accounts.google.com'].includes(payload.iss)||payload.aud!==clientId||typeof payload.exp!=='number'||payload.exp<=now||typeof payload.iat!=='number'||payload.iat>now+60||!payload.sub||payload.email_verified!==true||typeof payload.email!=='string'||payload.email.toLowerCase()!==allowedEmail.toLowerCase())return false;
    let jwk=(await getKeys()).find(k=>k.kid===header.kid);
    if(!jwk)jwk=(await getKeys(true)).find(k=>k.kid===header.kid);
    if(!jwk||jwk.kty!=='RSA'||jwk.alg!=='RS256')return false;
    const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
    return await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,bytes(parts[2]),new TextEncoder().encode(parts[0]+'.'+parts[1]));
  }catch{return false}
}
