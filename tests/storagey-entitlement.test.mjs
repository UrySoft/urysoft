import assert from 'node:assert/strict';
import { generateKeyPairSync, verify } from 'node:crypto';
import { annualStorageyPlan, paidEntitlement } from '../server/storagey-live-paypal.js';
import { issueStorageyLicense } from '../server/storagey-live-license.js';

const now=Date.parse('2026-11-20T10:00:00Z');
const annual={id:'P-STORAGEY2026',status:'ACTIVE',billing_cycles:[{tenure_type:'REGULAR',sequence:1,total_cycles:0,
  frequency:{interval_unit:'YEAR',interval_count:1},pricing_scheme:{fixed_price:{currency_code:'EUR',value:'6.00'}}}]};
assert.equal(annualStorageyPlan(annual,annual.id),true);
const wrong=structuredClone(annual);
wrong.billing_cycles[0].frequency.interval_unit='MONTH';
assert.equal(annualStorageyPlan(wrong,wrong.id),false);

const subscription={status:'ACTIVE'};
const payment={id:'transaction-test',status:'COMPLETED',time:'2026-11-19T10:00:00Z',
  amount_with_breakdown:{gross_amount:{currency_code:'EUR',value:'6.00'}}};
assert.equal(paidEntitlement(subscription,[],now).premium,false);
assert.equal(paidEntitlement(subscription,[{...payment,status:'PENDING'}],now).premium,false);
assert.equal(paidEntitlement(subscription,[payment,{...payment,status:'REFUNDED'}],now).premium,false);
const cancelled=paidEntitlement({status:'CANCELLED'},[payment],now);
assert.deepEqual(cancelled,{premium:true,paidThrough:'2027-11-19T10:00:00.000Z',renewing:false});

const {privateKey,publicKey}=generateKeyPairSync('rsa',{modulusLength:2048});
const env={PAYPAL_ENVIRONMENT:'live',STORAGEY_LICENSE_PRIVATE_KEY:privateKey.export({type:'pkcs8',format:'der'}).toString('base64')};
const device='a'.repeat(64), account='b'.repeat(64), paidUntil=Date.parse(cancelled.paidThrough);
const license=await issueStorageyLicense(env,device,account,paidUntil,now,now);
const [header,payload,signature]=license.split('.');
assert.equal(verify('sha256',Buffer.from(header+'.'+payload),publicKey,Buffer.from(signature,'base64url')),true);
const claims=JSON.parse(Buffer.from(payload,'base64url'));
assert.equal(claims.aud,'com.urysoft.storagey');
assert.equal(claims.device,device);
assert.equal(claims.exp,now/1000+30*86400);
assert.equal(await issueStorageyLicense(env,device,account,now,now,now),null);
console.log('PASS: annual offer, completed payments, cancellation, refund and signed device-bound expiry.');
