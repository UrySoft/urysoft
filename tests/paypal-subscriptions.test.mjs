import assert from 'node:assert/strict';
import {annualFolderyPlan,paidEntitlement,paypal} from '../server/paypal-subscriptions.js';
const planId='P-4G2879280H781535RNL4JYZQ';
const plan={id:planId,status:'ACTIVE',quantity_supported:false,billing_cycles:[{tenure_type:'REGULAR',sequence:1,total_cycles:0,frequency:{interval_unit:'YEAR',interval_count:1},pricing_scheme:{fixed_price:{value:'6.0',currency_code:'EUR'}}}],payment_preferences:{setup_fee:{value:'0.0',currency_code:'EUR'}}};
assert(annualFolderyPlan(plan,planId));
for(const change of [p=>p.status='CREATED',p=>p.quantity_supported=true,p=>p.billing_cycles[0].frequency.interval_unit='MONTH',p=>p.billing_cycles[0].frequency.interval_count=2,p=>p.billing_cycles[0].pricing_scheme.fixed_price.value='6.01',p=>p.billing_cycles[0].pricing_scheme.fixed_price.currency_code='USD',p=>p.billing_cycles.push(structuredClone(p.billing_cycles[0])),p=>p.payment_preferences.setup_fee.value='1',p=>p.taxes={percentage:'21'},p=>p.billing_cycles[0].total_cycles=1]){
 const copy=structuredClone(plan);change(copy);assert.equal(annualFolderyPlan(copy,planId),false);
}
const now=Date.parse('2026-11-14T12:00:00Z');
const payment={id:'SALE-TEST',status:'COMPLETED',time:'2026-11-14T11:00:00Z',amount_with_breakdown:{gross_amount:{value:'6.00',currency_code:'EUR'}}};
assert.equal(paidEntitlement({status:'ACTIVE'},[],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[payment],now).premium,true);
assert.equal(paidEntitlement({status:'CANCELLED'},[payment],now).premium,true);
assert.equal(paidEntitlement({status:'CANCELLED'},[payment],now).renewing,false);
assert.equal(paidEntitlement({status:'APPROVAL_PENDING'},[payment],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[payment],now+367*86400000).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[{...payment,status:'PENDING'}],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[{...payment,time:'2027-01-01T00:00:00Z'}],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[payment,{...payment,status:'REFUNDED'}],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[{...payment,amount_with_breakdown:{gross_amount:{value:'0.50',currency_code:'EUR'}}}],now).premium,false);
assert.equal(paidEntitlement({status:'ACTIVE'},[{...payment,time:'2024-02-29T12:00:00Z'}],Date.parse('2024-03-01')).paidThrough,'2025-02-28T12:00:00.000Z');
assert.throws(()=>paypal({PAYPAL_ENVIRONMENT:'live',PAYPAL_CLIENT_ID:'test',PAYPAL_CLIENT_SECRET:'test'}),/LIVE_PAYMENTS_DISABLED/);
assert.throws(()=>paypal({PAYPAL_ENVIRONMENT:'other',PAYPAL_CLIENT_ID:'test',PAYPAL_CLIENT_SECRET:'test'}),/PAYMENT_NOT_CONFIGURED/);
const env={PAYPAL_ENVIRONMENT:'sandbox',PAYPAL_CLIENT_ID:'test-only',PAYPAL_CLIENT_SECRET:'test-only',PAYPAL_PLAN_ID:planId,PAYPAL_WEBHOOK_ID:'TEST-WH'};
let calls=[];let approval='https://www.sandbox.paypal.com/webapps/billing/subscriptions?ba_token=TEST';let owner='a'.repeat(64);
const send=async(url,opts)=>{
 calls.push({url,opts});assert(url.startsWith('https://api-m.sandbox.paypal.com/'));assert.equal(opts.redirect,'error');
 if(url.endsWith('/v1/oauth2/token'))return Response.json({access_token:'test-token'});
 if(url.endsWith('/v1/billing/plans/'+planId))return Response.json(plan);
 if(url.endsWith('/v1/billing/subscriptions')&&opts.method==='POST'){const b=JSON.parse(opts.body);assert.equal(b.plan_id,planId);assert.equal(b.custom_id,owner);assert.equal(b.application_context.shipping_preference,'NO_SHIPPING');return Response.json({id:'I-SUBTEST12345',links:[{rel:'approve',method:'GET',href:approval}]});}
 if(url.endsWith('/v1/billing/subscriptions/I-SUBTEST12345'))return Response.json({id:'I-SUBTEST12345',plan_id:planId,custom_id:owner,status:'ACTIVE'});
 if(url.endsWith('/v1/notifications/verify-webhook-signature'))return Response.json({verification_status:'SUCCESS'});
 throw Error('Unexpected mock path');
};
const api=paypal(env,send);assert.equal((await api.create(owner,'foldery-test-request-123')).id,'I-SUBTEST12345');
assert.equal(calls.filter(c=>c.url.endsWith('/v1/oauth2/token')).length,1);
assert.equal((await api.subscription('I-SUBTEST12345',owner)).status,'ACTIVE');
await assert.rejects(()=>api.subscription('I-SUBTEST12345','b'.repeat(64)),/SUBSCRIPTION_OWNER_MISMATCH/);
for(const bad of ['https://evil.example/','http://www.sandbox.paypal.com/webapps/billing/subscriptions','https://www.paypal.com/webapps/billing/subscriptions','https://www.sandbox.paypal.com@evil.example/webapps/billing/subscriptions']){approval=bad;await assert.rejects(()=>api.create(owner,'foldery-test-request-123'),/INVALID_APPROVAL_URL/);}
await assert.rejects(()=>api.create('user-supplied-email','request'),/INVALID_CHECKOUT/);
assert.equal(await api.verifyWebhook(new Headers(),{}),false);
const headers=new Headers({'paypal-auth-algo':'SHA256withRSA','paypal-cert-url':'https://api.sandbox.paypal.com/v1/notifications/certs/CERT-TEST','paypal-transmission-id':'test','paypal-transmission-sig':'test','paypal-transmission-time':new Date(now).toISOString()});
assert.equal(await api.verifyWebhook(headers,{id:'EVENT'}),true);
headers.set('paypal-cert-url','https://evil.example/cert');assert.equal(await api.verifyWebhook(headers,{}),false);
console.log('PASS: PayPal pricing, ownership, cancellation, payment confirmation, webhook verification and sandbox isolation.');
