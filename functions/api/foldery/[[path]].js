import {paypal,paidEntitlement,PaymentError} from '../../../server/paypal-subscriptions.js';
import {folderyGoogle} from '../../../server/foldery-google.js';
import {sha256,randomToken,issueFolderyLicense} from '../../../server/foldery-license.js';
const origin='https://urysoft.pages.dev';
const json=(body,status=200)=>Response.json(body,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}});
const bad=(code,status=400)=>{throw new PaymentError(code,status)};
async function body(request){
  if(Number(request.headers.get('Content-Length')||0)>20000)bad('BODY_TOO_LARGE',413);
  if(!request.headers.get('Content-Type')?.startsWith('application/json'))bad('JSON_REQUIRED',415);
  const raw=await request.text();if(raw.length>20000)bad('BODY_TOO_LARGE',413);
  try{const value=JSON.parse(raw);if(!value||typeof value!=='object'||Array.isArray(value))bad('INVALID_JSON');return value}catch{bad('INVALID_JSON')}
}
async function reconcile(env,row,force=false,now=Date.now()){
  if(!row?.subscription_id)return row;
  if(!force&&now-row.verified_at<60000)return row;
  const api=paypal(env),subscription=await api.subscription(row.subscription_id,row.account_id);
  const transactions=['ACTIVE','CANCELLED','SUSPENDED','EXPIRED'].includes(subscription.status)?await api.transactions(row.subscription_id,now):[];
  const entitlement=paidEntitlement(subscription,transactions,now);
  const until=entitlement.paidThrough?Date.parse(entitlement.paidThrough):0;
  await env.PREMIUM_DB.prepare('UPDATE premium_checkouts SET status=?,paid_until=?,verified_at=? WHERE account_id=? AND subscription_id=?').bind(subscription.status,until,now,row.account_id,row.subscription_id).run();
  return {...row,status:subscription.status,paid_until:until,verified_at:now};
}
export async function onRequest({request,env,params}){
  const path=(params.path||[]).join('/'),now=Date.now();
  if(!env.PREMIUM_DB||!env.FOLDERY_GOOGLE_CLIENT_ID||!env.FOLDERY_LICENSE_PRIVATE_KEY||env.FOLDERY_PREMIUM_ENABLED!=='sandbox'||env.PAYPAL_ENVIRONMENT!=='sandbox')return json({error:'PREMIUM_NOT_AVAILABLE'},503);
  if(request.method!=='POST')return json({error:'METHOD_NOT_ALLOWED'},405);
  const suppliedOrigin=request.headers.get('Origin');
  // Google's cross-origin form-post can carry an opaque Origin. The signed,
  // one-use nonce and Google's double-submit CSRF cookie are required below.
  if(path==='google/redirect'&&suppliedOrigin&&![origin,'https://accounts.google.com','null'].includes(suppliedOrigin))return json({error:'ORIGIN_NOT_ALLOWED'},403);
  if(!['webhook','google/redirect'].includes(path)&&suppliedOrigin&&suppliedOrigin!==origin)return json({error:'ORIGIN_NOT_ALLOWED'},403);
  const db=env.PREMIUM_DB;
  try{
    if(path==='google/redirect'){
      if(!request.headers.get('Content-Type')?.startsWith('application/x-www-form-urlencoded')||Number(request.headers.get('Content-Length')||0)>20000)bad('INVALID_GOOGLE_RESPONSE');
      const raw=await request.text();if(raw.length>20000)bad('INVALID_GOOGLE_RESPONSE');
      const form=new URLSearchParams(raw),csrf=form.get('g_csrf_token'),cookie=request.headers.get('Cookie')?.split(';').map(s=>s.trim()).find(s=>s.startsWith('g_csrf_token='))?.slice(13);
      if(!csrf||csrf.length>512||csrf!==cookie)bad('GOOGLE_CSRF_FAILED',403);
      const credential=form.get('credential');let nonce;
      try{const p=credential.split('.')[1];nonce=JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(p.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0)))).nonce;}catch{bad('GOOGLE_SIGN_IN_FAILED',401)}
      if(typeof nonce!=='string'||!/^[a-f0-9]{64}$/.test(nonce))bad('GOOGLE_SIGN_IN_FAILED',401);
      const challenge=await db.prepare('SELECT * FROM premium_challenges WHERE code_hash=? AND expires_at>? AND account_id IS NULL').bind(nonce,now).first();
      if(!challenge)bad('LINK_EXPIRED',410);
      let identityFailure='UNKNOWN';
      const identity=await folderyGoogle(credential,env.FOLDERY_GOOGLE_CLIENT_ID,nonce,fetch,now,reason=>identityFailure=reason);if(!identity)bad('GOOGLE_SIGN_IN_FAILED_'+identityFailure,401);
      const account=await sha256('google:'+identity.sub),code=randomToken(),codeHash=await sha256(code);
      const consumed=await db.prepare('UPDATE premium_challenges SET code_hash=?,account_id=? WHERE code_hash=? AND account_id IS NULL AND expires_at>? RETURNING device_id').bind(codeHash,account,nonce,now).first();
      if(!consumed)bad('ALREADY_USED',409);
      await db.prepare('UPDATE premium_devices SET account_id=? WHERE device_id=?').bind(account,consumed.device_id).run();
      const lang=['ca','en'].includes(challenge.lang)?challenge.lang:'es';
      return new Response(null,{status:303,headers:{Location:origin+'/foldery/premium/?lang='+lang+'#'+code,'Cache-Control':'no-store','Referrer-Policy':'no-referrer'}});
    }
    const data=await body(request);
    if(path==='webhook'){
      const api=paypal(env);if(!await api.verifyWebhook(request.headers,data))bad('INVALID_WEBHOOK',401);
      if(typeof data.id!=='string'||data.id.length>100)bad('INVALID_EVENT');
      if(await db.prepare('SELECT event_id FROM premium_webhooks WHERE event_id=?').bind(data.id).first())return json({ok:true});
      const sub=data.event_type?.startsWith('BILLING.SUBSCRIPTION.')?data.resource?.id:data.resource?.billing_agreement_id;
      const row=typeof sub==='string'?await db.prepare('SELECT * FROM premium_checkouts WHERE subscription_id=?').bind(sub).first():null;
      if(row)await reconcile(env,row,true,now);
      await db.prepare('INSERT OR IGNORE INTO premium_webhooks(event_id,processed_at) VALUES(?,?)').bind(data.id,now).run();
      return json({ok:true});
    }
    const ip=await sha256(env.PAYPAL_CLIENT_SECRET+':foldery:'+request.headers.get('CF-Connecting-IP')+':'+Math.floor(now/3600000));
    const rate=await db.prepare('INSERT INTO premium_limits(bucket,count,expires) VALUES(?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<120 RETURNING count').bind(ip,now+7200000).first();
    if(!rate)bad('TRY_LATER',429);
    await db.prepare('DELETE FROM premium_limits WHERE expires<?').bind(now).run();
    if(['device/start','device/status'].includes(path)){
      const token=request.headers.get('Authorization')?.match(/^Bearer (fp_[A-Za-z0-9_-]{43})$/)?.[1];
      if(!token)bad('DEVICE_TOKEN_REQUIRED',401);
      const device=await sha256(token);
      if(path==='device/start'){
        const code=randomToken(),hash=await sha256(code),mode=data.mode==='restore'?'restore':'subscribe',lang=['ca','en'].includes(data.lang)?data.lang:'es';
        await db.prepare('DELETE FROM premium_challenges WHERE expires_at<?').bind(now).run();
        await db.prepare('INSERT OR IGNORE INTO premium_devices(device_id,created_at) VALUES(?,?)').bind(device,now).run();
        await db.prepare('INSERT INTO premium_challenges(code_hash,device_id,mode,lang,expires_at) VALUES(?,?,?,?,?)').bind(hash,device,mode,lang,now+900000).run();
        return json({url:origin+'/foldery/premium/?lang='+lang+'#'+code,expiresAt:now+900000});
      }
      const registered=await db.prepare('SELECT account_id FROM premium_devices WHERE device_id=?').bind(device).first();
      if(!registered?.account_id)return json({state:'awaiting_google',license:null});
      let row=await db.prepare('SELECT * FROM premium_checkouts WHERE account_id=?').bind(registered.account_id).first();
      try{row=await reconcile(env,row,false,now)}catch{if(!row?.verified_at)throw new PaymentError('PAYMENT_VERIFICATION_UNAVAILABLE',503)}
      const license=row?await issueFolderyLicense(env,device,registered.account_id,row.paid_until,row.verified_at,now):null;
      return json({state:license?'premium':'free',license,paidThrough:row?.paid_until||null,renewing:row?.status==='ACTIVE',environment:'sandbox'});
    }
    if(!/^[A-Za-z0-9_-]{43}$/.test(data.code||''))bad('INVALID_LINK');
    const codeHash=await sha256(data.code);
    let challenge=await db.prepare('SELECT * FROM premium_challenges WHERE code_hash=? AND expires_at>?').bind(codeHash,now).first();
    if(!challenge)bad('LINK_EXPIRED',410);
    if(path==='session')return json({googleClientId:env.FOLDERY_GOOGLE_CLIENT_ID,nonce:codeHash,mode:challenge.mode,environment:'sandbox',signedIn:!!challenge.account_id});
    if(path==='google'){
      if(challenge.account_id)bad('ALREADY_USED',409);
      const identity=await folderyGoogle(data.credential,env.FOLDERY_GOOGLE_CLIENT_ID,codeHash);if(!identity)bad('GOOGLE_SIGN_IN_FAILED',401);
      const account=await sha256('google:'+identity.sub);
      const consumed=await db.prepare('UPDATE premium_challenges SET account_id=? WHERE code_hash=? AND account_id IS NULL AND expires_at>? RETURNING device_id').bind(account,codeHash,now).first();
      if(!consumed)bad('ALREADY_USED',409);
      await db.prepare('UPDATE premium_devices SET account_id=? WHERE device_id=?').bind(account,consumed.device_id).run();
      const row=await db.prepare('SELECT * FROM premium_checkouts WHERE account_id=?').bind(account).first();
      return json({ok:true,mode:challenge.mode,hasSubscription:!!row?.subscription_id});
    }
    if(path==='checkout'){
      if(!challenge.account_id||challenge.mode!=='subscribe')bad('GOOGLE_SIGN_IN_REQUIRED',401);
      let row=await db.prepare('SELECT * FROM premium_checkouts WHERE account_id=?').bind(challenge.account_id).first();
      row=await reconcile(env,row,true,now);
      if(row?.paid_until>now)return json({alreadyPremium:true});
      if(row?.subscription_id){
        if(row.status==='APPROVAL_PENDING'&&row.approval_url&&now-row.created_at<86400000)return json({url:row.approval_url});
        // Never start another automatic subscription while one may still renew.
        if(!['CANCELLED','EXPIRED'].includes(row.status))bad('EXISTING_SUBSCRIPTION_REQUIRES_REVIEW',409);
        await db.prepare('DELETE FROM premium_checkouts WHERE account_id=? AND subscription_id=?').bind(row.account_id,row.subscription_id).run();row=null;
      }
      if(!row){
        const reservation=await db.prepare('INSERT OR IGNORE INTO premium_checkouts(account_id,request_id,created_at) VALUES(?,?,?) RETURNING *').bind(challenge.account_id,crypto.randomUUID(),now).first();
        row=reservation||await db.prepare('SELECT * FROM premium_checkouts WHERE account_id=?').bind(challenge.account_id).first();
      }
      if(now-row.created_at>48*3600000)bad('CHECKOUT_REQUIRES_REVIEW',409);
      const result=await paypal(env).create(challenge.account_id,row.request_id,data.lang);
      await db.prepare('UPDATE premium_checkouts SET subscription_id=?,approval_url=?,status=? WHERE account_id=? AND request_id=?').bind(result.id,result.approvalUrl,'APPROVAL_PENDING',challenge.account_id,row.request_id).run();
      return json({url:result.approvalUrl});
    }
    return json({error:'NOT_FOUND'},404);
  }catch(e){return json({error:e instanceof PaymentError?e.code:'REQUEST_FAILED'},e instanceof PaymentError?e.status:500)}
}
