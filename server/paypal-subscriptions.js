// Server-only PayPal adapter. Never include clientSecret in a browser bundle or APK.
const ORIGIN='https://urysoft.pages.dev';
export class PaymentError extends Error {
  constructor(code,status=502){super(code);this.code=code;this.status=status;}
}
const id=(value,prefix)=>typeof value==='string'&&new RegExp('^'+prefix+'-[A-Z0-9]{8,40}$').test(value);
const sixEuros=money=>money?.currency_code==='EUR'&&typeof money.value==='string'&&/^6(?:\.0{1,2})?$/.test(money.value);
export function annualFolderyPlan(plan,expectedId){
  const cycles=plan?.billing_cycles;
  const c=Array.isArray(cycles)&&cycles.length===1?cycles[0]:null;
  const prefs=plan?.payment_preferences;
  return plan?.id===expectedId&&plan.status==='ACTIVE'&&c?.tenure_type==='REGULAR'&&c.sequence===1&&c.total_cycles===0&&
    c.frequency?.interval_unit==='YEAR'&&c.frequency.interval_count===1&&
    sixEuros(c.pricing_scheme?.fixed_price)&&
    (!prefs?.setup_fee||Number(prefs.setup_fee.value)===0)&&
    (!plan.taxes||Number(plan.taxes.percentage)===0)&&plan.quantity_supported!==true;
}
export function paypal(env,send=fetch){
  const mode=env.PAYPAL_ENVIRONMENT;
  if(!['sandbox','live'].includes(mode)||!env.PAYPAL_CLIENT_ID||!env.PAYPAL_CLIENT_SECRET)throw new PaymentError('PAYMENT_NOT_CONFIGURED',503);
  if(mode==='live'&&env.FOLDERY_LIVE_PAYMENTS!=='enabled')throw new PaymentError('LIVE_PAYMENTS_DISABLED',503);
  const base=mode==='sandbox'?'https://api-m.sandbox.paypal.com':'https://api-m.paypal.com';
  let access;
  async function json(path,options={}){
    // Workers supports manual/follow, not redirect:error. Reject every non-2xx
    // response before reading it; never forward Authorization to another host.
    const response=await send(base+path,{...options,redirect:'manual',signal:AbortSignal.timeout(12000)});
    if(!response.ok)throw new PaymentError('PAYPAL_REQUEST_FAILED',response.status===429?503:502);
    if(response.status===204)return null;
    const text=await response.text();
    if(text.length>262144)throw new PaymentError('PAYPAL_RESPONSE_TOO_LARGE');
    try{return JSON.parse(text)}catch{throw new PaymentError('PAYPAL_RESPONSE_INVALID')}
  }
  async function request(path,method='GET',body,requestId){
    if(!access){
      const auth=await json('/v1/oauth2/token',{method:'POST',headers:{Authorization:'Basic '+btoa(env.PAYPAL_CLIENT_ID+':'+env.PAYPAL_CLIENT_SECRET),'Content-Type':'application/x-www-form-urlencoded'},body:'grant_type=client_credentials'});
      if(typeof auth.access_token!=='string'||auth.access_token.length>4096)throw new PaymentError('PAYPAL_TOKEN_INVALID');
      access=auth.access_token;
    }
    return json(path,{method,headers:{Authorization:'Bearer '+access,'Content-Type':'application/json',...(requestId?{'PayPal-Request-Id':requestId}:{} )},...(body?{body:JSON.stringify(body)}:{})});
  }
  async function plan(){
    if(!id(env.PAYPAL_PLAN_ID,'P'))throw new PaymentError('PLAN_NOT_CONFIGURED',503);
    const p=await request('/v1/billing/plans/'+env.PAYPAL_PLAN_ID);
    if(!annualFolderyPlan(p,env.PAYPAL_PLAN_ID))throw new PaymentError('PLAN_DOES_NOT_MATCH_OFFER',503);
    return p;
  }
  return {
    request,plan,
    async create(accountId,requestId,lang='es'){
      if(!/^[a-f0-9]{64}$/.test(accountId)||!/^[-a-zA-Z0-9]{16,38}$/.test(requestId))throw new PaymentError('INVALID_CHECKOUT',400);
      await plan();
      const data=await request('/v1/billing/subscriptions','POST',{
        plan_id:env.PAYPAL_PLAN_ID,custom_id:accountId,
        application_context:{brand_name:'UrySoft',locale:lang==='en'?'en-US':'es-ES',shipping_preference:'NO_SHIPPING',user_action:'SUBSCRIBE_NOW',
          return_url:ORIGIN+'/foldery/premium/?result=approved&lang='+(['ca','en'].includes(lang)?lang:'es'),cancel_url:ORIGIN+'/foldery/premium/?result=cancelled&lang='+(['ca','en'].includes(lang)?lang:'es')}
      },requestId);
      const approval=data.links?.find(l=>l.rel==='approve'&&l.method==='GET')?.href;
      let url;try{url=new URL(approval)}catch{throw new PaymentError('INVALID_APPROVAL_URL')}
      const host=mode==='sandbox'?'www.sandbox.paypal.com':'www.paypal.com';
      if(!id(data.id,'I')||url.protocol!=='https:'||url.hostname!==host||url.port||url.username||url.password||url.pathname!=='/webapps/billing/subscriptions')throw new PaymentError('INVALID_APPROVAL_URL');
      return {id:data.id,approvalUrl:url.href};
    },
    async subscription(subscriptionId,accountId){
      if(!id(subscriptionId,'I'))throw new PaymentError('INVALID_SUBSCRIPTION',400);
      const data=await request('/v1/billing/subscriptions/'+subscriptionId);
      if(data.id!==subscriptionId||data.plan_id!==env.PAYPAL_PLAN_ID||data.custom_id!==accountId||data.plan_overridden===true)throw new PaymentError('SUBSCRIPTION_OWNER_MISMATCH',403);
      return data;
    },
    async transactions(subscriptionId,now=Date.now()){
      if(!id(subscriptionId,'I'))throw new PaymentError('INVALID_SUBSCRIPTION',400);
      const query=new URLSearchParams({start_time:new Date(now-370*86400000).toISOString(),end_time:new Date(now).toISOString()});
      const data=await request('/v1/billing/subscriptions/'+subscriptionId+'/transactions?'+query);
      if(!Array.isArray(data.transactions)||data.total_pages>1)throw new PaymentError('PAYMENT_HISTORY_INCOMPLETE');
      return data.transactions;
    },
    async verifyWebhook(headers,event){
      if(!env.PAYPAL_WEBHOOK_ID)throw new PaymentError('WEBHOOK_NOT_CONFIGURED',503);
      const names=['paypal-auth-algo','paypal-cert-url','paypal-transmission-id','paypal-transmission-sig','paypal-transmission-time'];
      const values=names.map(n=>headers.get(n));
      if(values.some(v=>!v||v.length>2048))return false;
      let cert;try{cert=new URL(values[1])}catch{return false}
      if(cert.protocol!=='https:'||cert.username||cert.password||cert.port||!['api.paypal.com','api.sandbox.paypal.com','api-m.paypal.com','api-m.sandbox.paypal.com'].includes(cert.hostname)||!cert.pathname.startsWith('/v1/notifications/certs/'))return false;
      const r=await request('/v1/notifications/verify-webhook-signature','POST',{
        auth_algo:values[0],cert_url:values[1],transmission_id:values[2],transmission_sig:values[3],transmission_time:values[4],webhook_id:env.PAYPAL_WEBHOOK_ID,webhook_event:event});
      return r?.verification_status==='SUCCESS';
    }
  };
}

// Status ACTIVE alone and the browser return URL never constitute proof of payment.
export function paidEntitlement(subscription,transactions,now=Date.now()){
  const free={premium:false,paidThrough:null,renewing:false};
  if(!['ACTIVE','CANCELLED','SUSPENDED','EXPIRED'].includes(subscription?.status)||!Array.isArray(transactions))return free;
  const eligible=transactions.filter(t=>t.status==='COMPLETED'&&typeof t.id==='string'&&sixEuros(t.amount_with_breakdown?.gross_amount)&&Number.isFinite(Date.parse(t.time))&&Date.parse(t.time)<=now+300000);
  const settled=new Map();
  for(const t of transactions)if(t.status!=='COMPLETED'&&typeof t.id==='string')settled.set(t.id,false);
  const payments=eligible.filter(t=>!settled.has(t.id)).sort((a,b)=>Date.parse(b.time)-Date.parse(a.time));
  if(!payments.length)return free;
  const start=new Date(payments[0].time),end=new Date(start),month=start.getUTCMonth();
  end.setUTCFullYear(start.getUTCFullYear()+1);
  if(end.getUTCMonth()!==month)end.setUTCDate(0); // leap-day subscriptions end on Feb 28.
  return {premium:end.getTime()>now,paidThrough:end.toISOString(),renewing:subscription.status==='ACTIVE'};
}
