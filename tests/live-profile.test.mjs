import assert from 'node:assert/strict';
import {liveProfile} from '../server/foldery-live-profile.js';
import {onRequest} from '../functions/api/foldery/live/[[path]].js';
const sandbox={PREMIUM_DB:{sandbox:true},FOLDERY_LICENSE_PRIVATE_KEY:'test-signing-key',PAYPAL_CLIENT_ID:'test-client',PAYPAL_CLIENT_SECRET:'test-secret',GOOGLE_CLIENT_ID:'owner-admin-client',FOLDERY_GOOGLE_CLIENT_ID:'test-google-client'};
assert.equal(liveProfile(sandbox),null);
const live={...sandbox,FOLDERY_LIVE_PAYMENTS:'enabled',FOLDERY_LIVE_SELLER_READY:'yes',FOLDERY_LIVE_GOOGLE_PUBLIC:'yes',FOLDERY_LIVE_DB:{live:true},FOLDERY_LIVE_PAYPAL_CLIENT_ID:'live-client',FOLDERY_LIVE_PAYPAL_CLIENT_SECRET:'live-secret',FOLDERY_LIVE_PAYPAL_PLAN_ID:'P-LIVE123456789',FOLDERY_LIVE_PAYPAL_WEBHOOK_ID:'WH-LIVE123',FOLDERY_LIVE_GOOGLE_CLIENT_ID:'live-google-client',FOLDERY_LIVE_LICENSE_PRIVATE_KEY:'live-signing-key'};
assert.equal(liveProfile(live).PAYPAL_ENVIRONMENT,'live');
assert.equal(liveProfile({...live,FOLDERY_LIVE_TAX_REVIEWED:'no'}).PAYPAL_ENVIRONMENT,'live','Tax bookkeeping is not a payment or entitlement dependency');
for(const key of Object.keys(live).filter(k=>k.startsWith('FOLDERY_LIVE_'))){const missing={...live};delete missing[key];assert.equal(liveProfile(missing),null,'missing live field must fail closed: '+key);}
for(const [key,value] of [['FOLDERY_LIVE_DB',sandbox.PREMIUM_DB],['FOLDERY_LIVE_LICENSE_PRIVATE_KEY',sandbox.FOLDERY_LICENSE_PRIVATE_KEY],['FOLDERY_LIVE_PAYPAL_CLIENT_ID',sandbox.PAYPAL_CLIENT_ID],['FOLDERY_LIVE_PAYPAL_CLIENT_SECRET',sandbox.PAYPAL_CLIENT_SECRET],['FOLDERY_LIVE_GOOGLE_CLIENT_ID',sandbox.GOOGLE_CLIENT_ID],['FOLDERY_LIVE_GOOGLE_CLIENT_ID',sandbox.FOLDERY_GOOGLE_CLIENT_ID]])assert.equal(liveProfile({...live,[key]:value}),null,'test/admin configuration must not be reused');
let networkCalls=0;const oldFetch=globalThis.fetch;globalThis.fetch=async()=>{networkCalls++;throw Error('Disabled live service must never contact a provider')};
const result=await onRequest({env:sandbox,params:{path:['device','start']},request:new Request('https://urysoft.pages.dev/api/foldery/live/device/start',{method:'POST'})});
assert.equal(result.status,503);assert.equal(networkCalls,0);globalThis.fetch=oldFetch;
const disabledStatus=await onRequest({env:sandbox,request:new Request('https://urysoft.pages.dev/api/foldery/live/status')});
assert.equal(disabledStatus.status,200);assert.deepEqual(await disabledStatus.json(),{available:false});
assert.equal(disabledStatus.headers.get('Cache-Control'),'no-store');
const enabledStatus=await onRequest({env:live,request:new Request('https://urysoft.pages.dev/api/foldery/live/status')});
assert.deepEqual(await enabledStatus.json(),{available:true});
console.log('PASS: explicit activation, seller/public-Google gates, separate database/credentials/signing key, and zero network calls while disabled.');
