import assert from 'node:assert/strict';
import {liveProfile} from '../server/storagey-live-profile.js';
import {onRequest} from '../functions/api/storagey/live/[[path]].js';
const sandbox={PREMIUM_DB:{sandbox:true},STORAGEY_LICENSE_PRIVATE_KEY:'test-signing-key',PAYPAL_CLIENT_ID:'test-client',PAYPAL_CLIENT_SECRET:'test-secret',GOOGLE_CLIENT_ID:'owner-admin-client',STORAGEY_GOOGLE_CLIENT_ID:'test-google-client'};
assert.equal(liveProfile(sandbox),null);
const live={...sandbox,STORAGEY_LIVE_PAYMENTS:'enabled',STORAGEY_LIVE_SELLER_READY:'yes',STORAGEY_LIVE_GOOGLE_PUBLIC:'yes',STORAGEY_LIVE_DB:{live:true},STORAGEY_LIVE_PAYPAL_CLIENT_ID:'live-client',STORAGEY_LIVE_PAYPAL_CLIENT_SECRET:'live-secret',STORAGEY_LIVE_PAYPAL_PLAN_ID:'P-LIVE123456789',STORAGEY_LIVE_PAYPAL_WEBHOOK_ID:'WH-LIVE123',STORAGEY_LIVE_GOOGLE_CLIENT_ID:'live-google-client',STORAGEY_LIVE_LICENSE_PRIVATE_KEY:'live-signing-key'};
assert.equal(liveProfile(live).PAYPAL_ENVIRONMENT,'live');
for(const key of Object.keys(live).filter(k=>k.startsWith('STORAGEY_LIVE_'))){const missing={...live};delete missing[key];assert.equal(liveProfile(missing),null,'missing live field must fail closed: '+key);}
for(const [key,value] of [['STORAGEY_LIVE_DB',sandbox.PREMIUM_DB],['STORAGEY_LIVE_LICENSE_PRIVATE_KEY',sandbox.STORAGEY_LICENSE_PRIVATE_KEY],['STORAGEY_LIVE_PAYPAL_CLIENT_ID',sandbox.PAYPAL_CLIENT_ID],['STORAGEY_LIVE_PAYPAL_CLIENT_SECRET',sandbox.PAYPAL_CLIENT_SECRET],['STORAGEY_LIVE_GOOGLE_CLIENT_ID',sandbox.GOOGLE_CLIENT_ID],['STORAGEY_LIVE_GOOGLE_CLIENT_ID',sandbox.STORAGEY_GOOGLE_CLIENT_ID]])assert.equal(liveProfile({...live,[key]:value}),null,'test/admin configuration must not be reused');
let networkCalls=0;const oldFetch=globalThis.fetch;globalThis.fetch=async()=>{networkCalls++;throw Error('Disabled live service must never contact a provider')};
const result=await onRequest({env:sandbox,params:{path:['device','start']},request:new Request('https://urysoft.pages.dev/api/storagey/live/device/start',{method:'POST'})});
assert.equal(result.status,503);assert.equal(networkCalls,0);globalThis.fetch=oldFetch;
const disabledStatus=await onRequest({env:sandbox,request:new Request('https://urysoft.pages.dev/api/storagey/live/status')});
assert.equal(disabledStatus.status,200);assert.deepEqual(await disabledStatus.json(),{available:false});
assert.equal(disabledStatus.headers.get('Cache-Control'),'no-store');
const enabledStatus=await onRequest({env:live,request:new Request('https://urysoft.pages.dev/api/storagey/live/status')});
assert.deepEqual(await enabledStatus.json(),{available:true});
console.log('PASS: explicit activation, seller/public-Google gates, separate database/credentials/signing key, and zero network calls while disabled.');
