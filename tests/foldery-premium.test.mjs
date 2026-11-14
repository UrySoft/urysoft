import assert from 'node:assert/strict';
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import {onRequest} from '../functions/api/foldery/[[path]].js';
import {folderyGoogle} from '../server/foldery-google.js';
import {sha256} from '../server/foldery-license.js';
const db=new DatabaseSync(':memory:');db.exec(readFileSync('migrations/0002-foldery-premium.sql','utf8'));
function prepared(sql,values=[]){return {bind(...v){return prepared(sql,v)},first:async()=>db.prepare(sql).get(...values),all:async()=>({results:db.prepare(sql).all(...values)}),run:async()=>{const r=db.prepare(sql).run(...values);return {meta:{changes:r.changes}}}}}
const pair=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
const jwk={...await crypto.subtle.exportKey('jwk',pair.publicKey),kid:'google-test',alg:'RS256'};
const privateKey=Buffer.from(await crypto.subtle.exportKey('pkcs8',pair.privateKey)).toString('base64');
const planId='P-4G2879280H781535RNL4JYZQ';
const env={PREMIUM_DB:{prepare:prepared},FOLDERY_GOOGLE_CLIENT_ID:'premium-only-client',FOLDERY_LICENSE_PRIVATE_KEY:privateKey,FOLDERY_PREMIUM_ENABLED:'sandbox',PAYPAL_ENVIRONMENT:'sandbox',PAYPAL_CLIENT_ID:'test-only',PAYPAL_CLIENT_SECRET:'test-only',PAYPAL_PLAN_ID:planId,PAYPAL_WEBHOOK_ID:'WH-TEST'};
const device='fp_'+'A'.repeat(43),secondDevice='fp_'+'B'.repeat(43);
let owner,subStatus='APPROVAL_PENDING',transactionStatus='COMPLETED',paymentTime=new Date().toISOString(),createCalls=0,verifyWebhook='FAILURE';
const oldFetch=globalThis.fetch;
globalThis.fetch=async(url,opts)=>{
 if(url==='https://www.googleapis.com/oauth2/v3/certs')return Response.json({keys:[jwk]});
 assert(url.startsWith('https://api-m.sandbox.paypal.com/'));
 if(url.endsWith('/v1/oauth2/token'))return Response.json({access_token:'test'});
 if(url.endsWith('/v1/billing/plans/'+planId))return Response.json({id:planId,status:'ACTIVE',billing_cycles:[{frequency:{interval_unit:'YEAR',interval_count:1},tenure_type:'REGULAR',sequence:1,total_cycles:0,pricing_scheme:{fixed_price:{value:'6.0',currency_code:'EUR'}}}],payment_preferences:{setup_fee:{value:'0.0',currency_code:'EUR'}}});
 if(url.endsWith('/v1/billing/subscriptions')){createCalls++;const b=JSON.parse(opts.body);owner=b.custom_id;return Response.json({id:'I-TESTSUB123456',links:[{rel:'approve',method:'GET',href:'https://www.sandbox.paypal.com/webapps/billing/subscriptions?ba_token=TEST'}]});}
 if(url.endsWith('/v1/billing/subscriptions/I-TESTSUB123456'))return Response.json({id:'I-TESTSUB123456',plan_id:planId,custom_id:owner,status:subStatus});
 if(url.includes('/transactions?'))return Response.json({transactions:[{id:'SALE-TEST',status:transactionStatus,time:paymentTime,amount_with_breakdown:{gross_amount:{value:'6.00',currency_code:'EUR'}}}],total_pages:1});
 if(url.endsWith('/v1/notifications/verify-webhook-signature'))return Response.json({verification_status:verifyWebhook});
 throw Error('Unexpected mocked request');
};
async function googleToken(nonce,extra={}){const h=Buffer.from(JSON.stringify({alg:'RS256',kid:'google-test'})).toString('base64url');const p=Buffer.from(JSON.stringify({iss:'https://accounts.google.com',aud:'premium-only-client',sub:'test-customer-google-sub',nonce,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,email_verified:true,...extra})).toString('base64url');return h+'.'+p+'.'+Buffer.from(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',pair.privateKey,new TextEncoder().encode(h+'.'+p))).toString('base64url')}
async function call(path,body={},token=device,extraHeaders={},overrides=env){return onRequest({env:overrides,params:{path:path.split('/')},request:new Request('https://urysoft.pages.dev/api/foldery/'+path,{method:'POST',headers:{'Content-Type':'application/json',Origin:'https://urysoft.pages.dev',Authorization:'Bearer '+token,'CF-Connecting-IP':'192.0.2.1',...extraHeaders},body:JSON.stringify(body)})})}
async function start(token=device,mode='subscribe'){const r=await call('device/start',{mode},token);assert.equal(r.status,200);const code=new URL((await r.json()).url).hash.slice(1);const session=await (await call('session',{code})).json();return {code,session}}
assert.equal((await call('device/start',{},device,{},{})).status,503);
assert.equal((await call('device/start',{},'bad-token')).status,401);
assert.equal((await call('device/start',{},device,{Origin:'https://evil.example'})).status,403);
assert.equal((await (await call('device/status')).json()).state,'awaiting_google');
const {code,session}=await start();assert.equal(session.googleClientId,'premium-only-client');assert.equal(session.environment,'sandbox');
assert.equal((await call('checkout',{code})).status,401);
for(const extra of [{aud:'admin-client'},{iss:'https://evil.example'},{exp:1},{email_verified:false}])assert.equal(await folderyGoogle(await googleToken(session.nonce,extra),'premium-only-client',session.nonce),null);
assert.equal(await folderyGoogle(await googleToken(session.nonce),'premium-only-client',session.nonce,async(url,opts)=>{assert.equal(opts.redirect,'manual');return new Response(null,{status:302,headers:{Location:'https://evil.example'}})}),null);
assert.equal((await call('google',{code,credential:await googleToken('wrong-nonce')})).status,401);
assert.equal((await call('google',{code,credential:await googleToken(session.nonce)})).status,200);
assert.equal((await call('google',{code,credential:await googleToken(session.nonce)})).status,409);
assert.equal((await (await call('device/status')).json()).state,'free');
assert.equal((await call('checkout',{code})).status,200);assert.equal(createCalls,1);
assert.equal((await call('checkout',{code})).status,200);assert.equal(createCalls,1,'pending checkout reused instead of duplicate subscription');
assert.equal((await (await call('device/status',{approved:true,license:'forged'})).json()).state,'pending_payment');
subStatus='ACTIVE';db.exec('UPDATE premium_checkouts SET verified_at=0');
let granted=await (await call('device/status')).json();assert.equal(granted.state,'premium');assert(granted.license);
const [h,p,s]=granted.license.split('.');assert(await crypto.subtle.verify('RSASSA-PKCS1-v1_5',pair.publicKey,Buffer.from(s,'base64url'),new TextEncoder().encode(h+'.'+p)));
const claims=JSON.parse(Buffer.from(p,'base64url'));assert.equal(claims.device,await sha256(device));assert.equal(claims.aud,'cat.urysoft.widgets');assert.equal(claims.environment,'sandbox');assert(claims.exp<=claims.iat+30*86400);assert(claims.exp<=claims.paidThrough);
assert.equal((await (await call('checkout',{code})).json()).alreadyPremium,true);
const restored=await start(secondDevice,'restore');assert.equal((await call('google',{code:restored.code,credential:await googleToken(restored.session.nonce)})).status,200);
const restoredLicense=await (await call('device/status',{},secondDevice)).json();assert.equal(restoredLicense.state,'premium');assert.equal(JSON.parse(Buffer.from(restoredLicense.license.split('.')[1],'base64url')).device,await sha256(secondDevice));
assert.equal((await call('checkout',{code:restored.code})).status,401);
subStatus='CANCELLED';db.exec('UPDATE premium_checkouts SET verified_at=0');granted=await (await call('device/status')).json();assert.equal(granted.state,'premium');assert.equal(granted.renewing,false);
transactionStatus='REFUNDED';db.exec('UPDATE premium_checkouts SET verified_at=0');assert.equal((await (await call('device/status')).json()).state,'free');
transactionStatus='COMPLETED';paymentTime=new Date(Date.now()-367*86400000).toISOString();db.exec('UPDATE premium_checkouts SET verified_at=0');assert.equal((await (await call('device/status')).json()).state,'free');
db.exec('UPDATE premium_challenges SET expires_at=0');assert.equal((await call('session',{code})).status,410);
const webhookHeaders={'paypal-auth-algo':'SHA256withRSA','paypal-cert-url':'https://api.sandbox.paypal.com/v1/notifications/certs/CERT-TEST','paypal-transmission-id':'test','paypal-transmission-sig':'test','paypal-transmission-time':new Date().toISOString()};
const event={id:'WH-EVENT-TEST',event_type:'BILLING.SUBSCRIPTION.CANCELLED',resource:{id:'I-TESTSUB123456'}};
assert.equal((await call('webhook',event,device,webhookHeaders)).status,401);assert.equal(db.prepare('SELECT count(*) n FROM premium_webhooks').get().n,0);
verifyWebhook='SUCCESS';assert.equal((await call('webhook',event,device,webhookHeaders)).status,200);assert.equal((await call('webhook',event,device,webhookHeaders)).status,200);assert.equal(db.prepare('SELECT count(*) n FROM premium_webhooks').get().n,1);
// Fixtures contain test-only keys, for the Android verifier's host-side tests.
mkdirSync('.work/foldery/license-fixtures',{recursive:true});
writeFileSync('.work/foldery/license-fixtures/valid.jwt',restoredLicense.license);
writeFileSync('.work/foldery/license-fixtures/device.txt',await sha256(secondDevice));
writeFileSync('.work/foldery/license-fixtures/public.txt',Buffer.from(await crypto.subtle.exportKey('spki',pair.publicKey)).toString('base64'));
const redirectStart=await (await call('device/start',{mode:'restore',lang:'ca'},secondDevice)).json();
const redirectCode=new URL(redirectStart.url).hash.slice(1),redirectSession=await (await call('session',{code:redirectCode})).json();
const redirectToken=await googleToken(redirectSession.nonce);
async function redirect(credential,cookie='csrf-test',csrf='csrf-test',requestOrigin='null'){
 return onRequest({env,params:{path:['google','redirect']},request:new Request('https://urysoft.pages.dev/api/foldery/google/redirect',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',Origin:requestOrigin,Cookie:cookie?'g_csrf_token='+cookie:''},body:new URLSearchParams({credential,g_csrf_token:csrf})})});
}
assert.equal((await redirect(redirectToken,'csrf-test','csrf-test','https://evil.example')).status,403);
assert.equal((await redirect(redirectToken,'')).status,403);
assert.equal((await redirect(redirectToken,'different-cookie')).status,403);
const rejectedAudience=await redirect(await googleToken(redirectSession.nonce,{aud:'admin-client'}));assert.equal(rejectedAudience.status,401);assert.equal((await rejectedAudience.json()).error,'GOOGLE_SIGN_IN_FAILED_AUDIENCE');
const direct=await redirect(redirectToken);assert.equal(direct.status,303);
const returned=new URL(direct.headers.get('Location'));assert.equal(returned.origin,'https://urysoft.pages.dev');assert.equal(returned.searchParams.get('lang'),'ca');assert.equal(returned.pathname,'/foldery/premium/');
assert.notEqual(returned.hash.slice(1),redirectCode,'browser credential rotated after Google return');
assert.equal((await (await call('session',{code:returned.hash.slice(1)})).json()).signedIn,true);
assert.equal((await redirect(redirectToken)).status,410,'signed return cannot be replayed');
globalThis.fetch=oldFetch;
console.log('PASS: Google nonce/audience/replay protection, isolated customer identity, checkout reuse, payment proof, device binding, signed leases, restore, cancellation, refunds, expiry and verified webhook replay handling.');
