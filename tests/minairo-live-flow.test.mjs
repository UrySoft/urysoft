import assert from 'node:assert/strict';
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import {liveRequest as onRequest} from '../server/minairo-live-core.js';
import {minairoGoogle} from '../server/minairo-google.js';
import {sha256} from '../server/minairo-live-license.js';
const db=new DatabaseSync(':memory:');db.exec(readFileSync('migrations/0002-minairo-premium.sql','utf8'));
db.exec(readFileSync(new URL('../migrations/0003-minairo-live-agreements.sql',import.meta.url),'utf8'));
db.exec(readFileSync(new URL('../migrations/0004-minairo-live-confirmations.sql',import.meta.url),'utf8'));
function prepared(sql,values=[]){return {bind(...v){return prepared(sql,v)},first:async()=>db.prepare(sql).get(...values),all:async()=>({results:db.prepare(sql).all(...values)}),run:async()=>{const r=db.prepare(sql).run(...values);return {meta:{changes:r.changes}}}}}
const pair=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
const jwk={...await crypto.subtle.exportKey('jwk',pair.publicKey),kid:'google-test',alg:'RS256'};
const privateKey=Buffer.from(await crypto.subtle.exportKey('pkcs8',pair.privateKey)).toString('base64');
const planId='P-4G2879280H781535RNL4JYZQ';
const env={PREMIUM_DB:{prepare:prepared},MINAIRO_GOOGLE_CLIENT_ID:'premium-only-client',MINAIRO_LICENSE_PRIVATE_KEY:privateKey,MINAIRO_PREMIUM_ENABLED:'live',PAYPAL_ENVIRONMENT:'live',MINAIRO_LIVE_PAYMENTS:'enabled',PAYPAL_CLIENT_ID:'test-only',PAYPAL_CLIENT_SECRET:'test-only',PAYPAL_PLAN_ID:planId,PAYPAL_WEBHOOK_ID:'WH-TEST'};
const device='mp_'+'A'.repeat(43),secondDevice='mp_'+'B'.repeat(43);
let owner,subStatus='APPROVAL_PENDING',transactionStatus='COMPLETED',paymentTime=new Date().toISOString(),createCalls=0,verifyWebhook='FAILURE';
const oldFetch=globalThis.fetch;
globalThis.fetch=async(url,opts)=>{
 if(url==='https://www.googleapis.com/oauth2/v3/certs')return Response.json({keys:[jwk]});
 assert(url.startsWith('https://api-m.paypal.com/'));
 if(url.endsWith('/v1/oauth2/token'))return Response.json({access_token:'test'});
 if(url.endsWith('/v1/billing/plans/'+planId))return Response.json({id:planId,status:'ACTIVE',billing_cycles:[{frequency:{interval_unit:'YEAR',interval_count:1},tenure_type:'REGULAR',sequence:1,total_cycles:0,pricing_scheme:{fixed_price:{value:'12.0',currency_code:'EUR'}}}],payment_preferences:{setup_fee:{value:'0.0',currency_code:'EUR'}}});
 if(url.endsWith('/v1/billing/subscriptions')){createCalls++;const b=JSON.parse(opts.body);owner=b.custom_id;return Response.json({id:'I-TESTSUB123456',links:[{rel:'approve',method:'GET',href:'https://www.paypal.com/webapps/billing/subscriptions?ba_token=TEST'}]});}
 if(url.endsWith('/v1/billing/subscriptions/I-TESTSUB123456'))return Response.json({id:'I-TESTSUB123456',plan_id:planId,custom_id:owner,status:subStatus});
 if(url.includes('/transactions?'))return Response.json({transactions:[{id:'SALE-TEST',status:transactionStatus,time:paymentTime,amount_with_breakdown:{gross_amount:{value:'12.00',currency_code:'EUR'}}}],total_pages:1});
 if(url.endsWith('/v1/notifications/verify-webhook-signature'))return Response.json({verification_status:verifyWebhook});
 throw Error('Unexpected mocked request');
};
async function googleToken(nonce,extra={}){const h=Buffer.from(JSON.stringify({alg:'RS256',kid:'google-test'})).toString('base64url');const p=Buffer.from(JSON.stringify({iss:'https://accounts.google.com',aud:'premium-only-client',sub:'test-customer-google-sub',nonce,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,email_verified:true,...extra})).toString('base64url');return h+'.'+p+'.'+Buffer.from(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',pair.privateKey,new TextEncoder().encode(h+'.'+p))).toString('base64url')}
async function call(path,body={},token=device,extraHeaders={},overrides=env){return onRequest({env:overrides,params:{path:path.split('/')},request:new Request('https://urysoft.pages.dev/api/minairo/live/'+path,{method:'POST',headers:{'Content-Type':'application/json',Origin:'https://urysoft.pages.dev',Authorization:'Bearer '+token,'CF-Connecting-IP':'192.0.2.1',...extraHeaders},body:JSON.stringify(body)})})}
async function start(token=device,mode='subscribe'){const r=await call('device/start',{mode},token);assert.equal(r.status,200);const code=new URL((await r.json()).url).hash.slice(1);const session=await (await call('session',{code})).json();return {code,session}}
assert.equal((await call('device/start',{},device,{},{})).status,503);
assert.equal((await call('device/start',{},'bad-token')).status,401);
assert.equal((await call('device/start',{},device,{Origin:'https://evil.example'})).status,403);
assert.equal((await (await call('device/status')).json()).state,'awaiting_google');
const {code,session}=await start();assert.equal(session.googleClientId,'premium-only-client');assert.equal(session.environment,'live');
assert.equal((await call('checkout',{code})).status,401);
assert.equal((await call('receipt',{code})).status,401);
for(const extra of [{aud:'admin-client'},{iss:'https://evil.example'},{exp:1},{email_verified:false}])assert.equal(await minairoGoogle(await googleToken(session.nonce,extra),'premium-only-client',session.nonce),null);
assert.equal(await minairoGoogle(await googleToken(session.nonce),'premium-only-client',session.nonce,async(url,opts)=>{assert.equal(opts.redirect,'manual');return new Response(null,{status:302,headers:{Location:'https://evil.example'}})}),null);
assert.equal((await call('google',{code,credential:await googleToken('wrong-nonce')})).status,401);
assert.equal((await call('google',{code,credential:await googleToken(session.nonce)})).status,200);
assert.equal((await call('google',{code,credential:await googleToken(session.nonce)})).status,409);
assert.equal((await (await call('device/status')).json()).state,'free');
for(const invalid of [{},{termsAccepted:false,termsVersion:'2026-11-16'},{termsAccepted:'true',termsVersion:'2026-11-16'},{termsAccepted:true,termsVersion:'old'}])assert.equal((await call('checkout',{code,...invalid})).status,400);
assert.equal(createCalls,0,'no PayPal subscription without explicit current terms acceptance');
const accepted={code,termsAccepted:true,termsVersion:'2026-11-16',lang:'ca'};
const checkoutWithReceipt=await call('checkout',accepted);assert.equal(checkoutWithReceipt.status,200);assert.equal(createCalls,1);
const receiptCookie=checkoutWithReceipt.headers.get('Set-Cookie');assert(receiptCookie.includes('HttpOnly'));assert(receiptCookie.includes('Secure'));assert(receiptCookie.includes('SameSite=Lax'));assert(receiptCookie.includes('Max-Age=3600'));
const storedReceiptSession=db.prepare('SELECT * FROM premium_receipt_sessions').get();assert.notEqual(storedReceiptSession.token_hash,receiptCookie.split(';')[0].split('=')[1]);assert(storedReceiptSession.expires_at>Date.now()+3500000);
async function receiptGet(cookie,headers={}){return onRequest({env,params:{path:['receipt']},request:new Request('https://urysoft.pages.dev/api/minairo/live/receipt',{headers:{...(cookie?{Cookie:cookie.split(';')[0]}:{}),...headers}})});}
assert.equal((await receiptGet()).status,401);assert.equal((await receiptGet('__Host-minairo_receipt='+'X'.repeat(43))).status,401);
assert.equal((await receiptGet(receiptCookie,{'Sec-Fetch-Site':'cross-site'})).status,403);
assert.equal((await receiptGet(receiptCookie)).status,409,'browser return cannot manufacture a paid confirmation');
assert.equal(db.prepare('SELECT count(*) n FROM premium_confirmations').get().n,0);
assert.equal((await call('checkout',accepted)).status,200);assert.equal(createCalls,1,'pending checkout reused instead of duplicate subscription');
const acceptance=db.prepare('SELECT * FROM premium_agreements').get();
assert.equal(acceptance.account_id,owner);assert.equal(acceptance.terms_version,'2026-11-16');assert.equal(acceptance.lang,'ca');assert.equal(acceptance.annual_amount_cents,1200);assert.equal(acceptance.currency,'EUR');assert.equal(acceptance.automatic_renewal,1);assert(acceptance.accepted_at>0);
assert.equal(db.prepare('SELECT count(*) n FROM premium_agreements').get().n,1,'retry does not overwrite the original acceptance proof');
assert.equal((await (await call('device/status',{approved:true,license:'forged'})).json()).state,'pending_payment');
subStatus='ACTIVE';db.exec('UPDATE premium_checkouts SET verified_at=0');
let granted=await (await call('device/status')).json();assert.equal(granted.state,'premium');assert(granted.license);
const documentResponse=await receiptGet(receiptCookie);assert.equal(documentResponse.status,200);assert.equal(documentResponse.headers.get('Cache-Control'),'no-store');assert(documentResponse.headers.get('Content-Disposition').includes('attachment'));
const confirmation=await documentResponse.text();assert(confirmation.includes('lang="ca"'));assert(confirmation.includes('I-TESTSUB123456'));assert(confirmation.includes('12 €'));assert(confirmation.includes('14 dies'));assert(confirmation.includes('Privacitat de Minairó'));assert(!confirmation.includes('<script'));assert(!confirmation.includes(device));
assert.equal(await (await call('receipt',{code})).text(),confirmation,'Google-authenticated account can recover exact saved document');
const [h,p,s]=granted.license.split('.');assert(await crypto.subtle.verify('RSASSA-PKCS1-v1_5',pair.publicKey,Buffer.from(s,'base64url'),new TextEncoder().encode(h+'.'+p)));
const claims=JSON.parse(Buffer.from(p,'base64url'));assert.equal(claims.device,await sha256(device));assert.equal(claims.aud,'com.urysoft.minairo');assert.equal(claims.environment,'live');assert(claims.exp<=claims.iat+30*86400);assert(claims.exp<=claims.paidThrough);
assert.equal((await (await call('checkout',{code})).json()).alreadyPremium,true);
const restored=await start(secondDevice,'restore');assert.equal((await call('google',{code:restored.code,credential:await googleToken(restored.session.nonce)})).status,200);
const restoredLicense=await (await call('device/status',{},secondDevice)).json();assert.equal(restoredLicense.state,'premium');assert.equal(JSON.parse(Buffer.from(restoredLicense.license.split('.')[1],'base64url')).device,await sha256(secondDevice));
assert.equal((await call('checkout',{code:restored.code})).status,401);
assert.equal(await (await call('receipt',{code:restored.code})).text(),confirmation,'second installation of same Google account restores immutable confirmation');
subStatus='CANCELLED';db.exec('UPDATE premium_checkouts SET verified_at=0');granted=await (await call('device/status')).json();assert.equal(granted.state,'premium');assert.equal(granted.renewing,false);
transactionStatus='REFUNDED';db.exec('UPDATE premium_checkouts SET verified_at=0');assert.equal((await (await call('device/status')).json()).state,'free');
assert.equal(await (await receiptGet(receiptCookie)).text(),confirmation,'original contract confirmation remains available after refund without granting Premium');
transactionStatus='COMPLETED';paymentTime=new Date(Date.now()-367*86400000).toISOString();db.exec('UPDATE premium_checkouts SET verified_at=0');assert.equal((await (await call('device/status')).json()).state,'free');
db.exec('UPDATE premium_challenges SET expires_at=0');assert.equal((await call('session',{code})).status,410);
const webhookHeaders={'paypal-auth-algo':'SHA256withRSA','paypal-cert-url':'https://api.sandbox.paypal.com/v1/notifications/certs/CERT-TEST','paypal-transmission-id':'test','paypal-transmission-sig':'test','paypal-transmission-time':new Date().toISOString()};
const event={id:'WH-EVENT-TEST',event_type:'BILLING.SUBSCRIPTION.CANCELLED',resource:{id:'I-TESTSUB123456'}};
assert.equal((await call('webhook',event,device,webhookHeaders)).status,401);assert.equal(db.prepare('SELECT count(*) n FROM premium_webhooks').get().n,0);
verifyWebhook='SUCCESS';assert.equal((await call('webhook',event,device,webhookHeaders)).status,200);assert.equal((await call('webhook',event,device,webhookHeaders)).status,200);assert.equal(db.prepare('SELECT count(*) n FROM premium_webhooks').get().n,1);
// Fixtures contain test-only keys, for the Android verifier's host-side tests.
mkdirSync('.work/minairo/license-fixtures',{recursive:true});
writeFileSync('.work/minairo/license-fixtures/valid.jwt',restoredLicense.license);
writeFileSync('.work/minairo/license-fixtures/device.txt',await sha256(secondDevice));
writeFileSync('.work/minairo/license-fixtures/public.txt',Buffer.from(await crypto.subtle.exportKey('spki',pair.publicKey)).toString('base64'));
const redirectStart=await (await call('device/start',{mode:'restore',lang:'ca'},secondDevice)).json();
const redirectCode=new URL(redirectStart.url).hash.slice(1),redirectSession=await (await call('session',{code:redirectCode})).json();
const redirectToken=await googleToken(redirectSession.nonce);
async function redirect(credential,cookie='csrf-test',csrf='csrf-test',requestOrigin='null'){
 return onRequest({env,params:{path:['google','redirect']},request:new Request('https://urysoft.pages.dev/api/minairo/live/google/redirect',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',Origin:requestOrigin,Cookie:cookie?'g_csrf_token='+cookie:''},body:new URLSearchParams({credential,g_csrf_token:csrf})})});
}
assert.equal((await redirect(redirectToken,'csrf-test','csrf-test','https://evil.example')).status,403);
assert.equal((await redirect(redirectToken,'')).status,403);
const browserFailure=await onRequest({env,params:{path:['google','redirect']},request:new Request('https://urysoft.pages.dev/api/minairo/live/google/redirect',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',Accept:'text/html'},body:'credential=invalid'})});assert.equal(browserFailure.status,303);assert(new URL(browserFailure.headers.get('Location')).searchParams.get('result')==='signin_error');
assert.equal((await redirect(redirectToken,'different-cookie')).status,403);
const rejectedAudience=await redirect(await googleToken(redirectSession.nonce,{aud:'admin-client'}));assert.equal(rejectedAudience.status,401);assert.equal((await rejectedAudience.json()).error,'GOOGLE_SIGN_IN_FAILED_AUDIENCE');
const direct=await redirect(redirectToken);assert.equal(direct.status,303);
const returned=new URL(direct.headers.get('Location'));assert.equal(returned.origin,'https://urysoft.pages.dev');assert.equal(returned.searchParams.get('lang'),'ca');assert.equal(returned.pathname,'/minairo/premium-real/');
assert.notEqual(returned.hash.slice(1),redirectCode,'browser credential rotated after Google return');
assert.equal((await (await call('session',{code:returned.hash.slice(1)})).json()).signedIn,true);
assert.equal((await redirect(redirectToken)).status,410,'signed return cannot be replayed');
const thirdDevice='mp_'+'C'.repeat(43),third=await start(thirdDevice);
assert.equal((await call('google',{code:third.code,credential:await googleToken(third.session.nonce,{sub:'different-customer'})})).status,200);
assert.equal((await call('receipt',{code:third.code,requestId:acceptance.request_id,accountId:owner})).status,404,'another Google account cannot choose a receipt owner');
db.exec("UPDATE premium_limits SET count=30 WHERE bucket LIKE 'receipt:%'");assert.equal((await receiptGet(receiptCookie)).status,429,'receipt download requests are limited');
db.exec('UPDATE premium_receipt_sessions SET expires_at=0');assert.equal((await receiptGet(receiptCookie)).status,401,'receipt cookie expires');
db.exec('DROP TABLE premium_agreements');const beforeMissingProof=createCalls;
assert.equal((await call('checkout',{code:third.code,termsAccepted:true,termsVersion:'2026-11-16'},thirdDevice)).status,500);
assert.equal(createCalls,beforeMissingProof,'failed acceptance persistence must prevent PayPal creation');
globalThis.fetch=oldFetch;
const recoveryClaims=JSON.parse(Buffer.from(restoredLicense.license.split('.')[1],'base64url'));
async function signedFixture(environment,kid){const h=Buffer.from(JSON.stringify({alg:'RS256',typ:'JWT',kid})).toString('base64url'),p=Buffer.from(JSON.stringify({...recoveryClaims,environment})).toString('base64url');return h+'.'+p+'.'+Buffer.from(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',pair.privateKey,new TextEncoder().encode(h+'.'+p))).toString('base64url')}
writeFileSync('.work/minairo/license-fixtures/sandbox-environment.jwt',await signedFixture('sandbox','minairo-live-2026-01'));
writeFileSync('.work/minairo/license-fixtures/sandbox-key-id.jwt',await signedFixture('live','minairo-2026-01'));
console.log('PASS: terms acceptance, payment-verified immutable offline confirmations, same-account restoration, cookie hashing/expiry/security, cross-site and other-account rejection, receipt rate limits, and existing payment/licensing regressions.');
