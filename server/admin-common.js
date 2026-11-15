import {googleAdmin} from './google-auth.js';
export const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
export async function authorize(request,env){const a=request.headers.get('Authorization')||'';return a.startsWith('Bearer ')&&await googleAdmin(a.slice(7),env.GOOGLE_CLIENT_ID,env.COMMENTS_ADMIN_EMAIL)}
export const sameOrigin=request=>request.headers.get('Origin')===new URL(request.url).origin;
export async function readBody(request,max=16000){if(!request.headers.get('Content-Type')?.startsWith('application/json'))throw {status:415};const raw=await request.text();if(raw.length>max)throw {status:413};try{return JSON.parse(raw)}catch{throw {status:400}}}
export async function rateLimit(request,env,scope,limit,window=3600000){
 const input=env.COMMENTS_ADMIN_KEY+':'+scope+':'+(request.headers.get('CF-Connecting-IP')||'unknown')+':'+Math.floor(Date.now()/window);
 const bucket=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(input)))).map(x=>x.toString(16).padStart(2,'0')).join('');
 const row=await env.COMMENTS_DB.prepare('INSERT INTO comment_limits(bucket,count,expires) VALUES(?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<? RETURNING count').bind(bucket,Date.now()+window*2,limit).first();
 return !!row;
}
export async function batch(db,statements){return db.batch(statements)}
