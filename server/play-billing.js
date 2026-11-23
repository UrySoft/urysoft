import { issueStorageyLicense } from './storagey-live-license.js';
import { issueTeclaIRCLicense } from './teclairc-live-license.js';
import { issueMinairoLicense } from './minairo-live-license.js';
import { issueFolderyLicense } from './foldery-live-license.js';
import { issueWidgeryLicense } from './widgery-live-license.js';

export const catalog = Object.freeze({
  storagey: { app: 'com.urysoft.storagey', prefix: 'sp_', env: 'STORAGEY', issue: issueStorageyLicense },
  teclairc: { app: 'com.urysoft.uryirc', prefix: 'ti_', env: 'TECLAIRC', issue: issueTeclaIRCLicense },
  minairo: { app: 'com.urysoft.minairo', prefix: 'mp_', env: 'MINAIRO', issue: issueMinairoLicense },
  foldery: { app: 'cat.urysoft.widgets', prefix: 'fp_', env: 'FOLDERY', product: 'foldery_premium', issue: issueFolderyLicense },
  widgery: { app: 'com.urisoft.widgery', prefix: 'wp_', env: 'WIDGERY', product: 'widgery_premium', issue: issueWidgeryLicense },
});
const defaultProduct = 'premium';
const plan = 'annual';
const oauthUrl = 'https://oauth2.googleapis.com/token';
const publisher = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/';
const day = 86400000;
const encoder = new TextEncoder();
const b64 = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
const hash = async input => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', encoder.encode(input))), byte => byte.toString(16).padStart(2, '0')).join('');
const encodeJson = value => b64(encoder.encode(JSON.stringify(value)));
const response = (value, status = 200) => Response.json(value, { status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' } });

export function evaluatePurchase(purchase, boundAccount, now, allowTestPurchases = false, product = defaultProduct) {
  if (purchase?.externalAccountIdentifiers?.obfuscatedExternalAccountId !== boundAccount) return { state: 'owner_mismatch' };
  if (purchase.testPurchase && !allowTestPurchases) return { state: 'test_purchase' };
  if (purchase.subscriptionState === 'SUBSCRIPTION_STATE_PENDING') return { state: 'pending_payment' };
  if (!['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD', 'SUBSCRIPTION_STATE_CANCELED'].includes(purchase.subscriptionState)) return { state: 'free' };
  const eligible = (purchase.lineItems || []).filter(item =>
    item.productId === product && item.offerDetails?.basePlanId === plan && item.autoRenewingPlan
    && Date.parse(item.expiryTime) > now
  );
  const item = eligible.reduce((latest, item) => !latest || Date.parse(item.expiryTime) > Date.parse(latest.expiryTime) ? item : latest, null);
  if (!item) return { state: 'free' };
  if (!['ACKNOWLEDGEMENT_STATE_PENDING', 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED'].includes(purchase.acknowledgementState)) return { state: 'free' };
  return {
    state: 'premium',
    expires: Date.parse(item.expiryTime),
    renewing: item.autoRenewingPlan.autoRenewEnabled === true,
    needsAcknowledgement: purchase.acknowledgementState === 'ACKNOWLEDGEMENT_STATE_PENDING',
  };
}

let oauthCache;
async function accessToken(serviceAccount, now, fetcher) {
  if (oauthCache?.keyId === serviceAccount.private_key_id && oauthCache.expires > now + 60000) return oauthCache.accessToken;
  const pem = serviceAccount.private_key.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, '');
  const key = await crypto.subtle.importKey('pkcs8', Uint8Array.from(atob(pem), char => char.charCodeAt(0)), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign']);
  const iat = Math.floor(now / 1000);
  const claims = {
    iss: serviceAccount.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: oauthUrl, iat, exp: iat + 300,
  };
  const packageToSign = encodeJson({ alg: 'RS256', typ: 'JWT', kid: serviceAccount.private_key_id }) + '.' + encodeJson(claims);
  const signature = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, encoder.encode(packageToSign));
  const result = await fetcher(oauthUrl, {
    method: 'POST', redirect: 'error', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: packageToSign + '.' + b64(signature) }),
  });
  if (!result.ok) throw new Error('Google token unavailable');
  const grant = await result.json();
  if (!grant.access_token || !Number.isFinite(grant.expires_in)) throw new Error('Google token invalid');
  oauthCache = { keyId: serviceAccount.private_key_id, expires: now + 1000 * grant.expires_in, accessToken: grant.access_token };
  return grant.access_token;
}

function serviceAccount(env) {
  try {
    const value = JSON.parse(env.GOOGLE_PLAY_SERVICE_ACCOUNT);
    if (value.type === 'service_account' && value.client_email && value.private_key_id && value.private_key) return value;
  } catch {}
  return null;
}

export async function playRequest({ request, env, params }, fetcher = fetch, now = Date.now()) {
  const entry = Object.hasOwn(catalog, params.app) ? catalog[params.app] : null;
  if (!entry) return response({ error: 'UNKNOWN_APP' }, 404);
  const product = entry.product ?? defaultProduct;
  const signer = env[entry.env + '_LIVE_LICENSE_PRIVATE_KEY'];
  const google = serviceAccount(env);
  const db = env[entry.env + '_LIVE_DB'];
  const ready = env.GOOGLE_PLAY_BILLING_ENABLED === 'yes' && !!google && !!signer && !!db;
  if (request.method === 'GET') return response({ available: ready, product, plan });
  if (!ready) return response({ error: 'BILLING_UNAVAILABLE' }, 503);
  if (request.method !== 'POST') return response({ error: 'METHOD_NOT_ALLOWED' }, 405);
  const deviceToken = request.headers.get('Authorization')?.match(/^Bearer ([a-z]{2}_[A-Za-z0-9_-]{43})$/)?.[1];
  if (!deviceToken?.startsWith(entry.prefix)) return response({ error: 'UNAUTHENTICATED_DEVICE' }, 401);
  if (request.headers.get('Origin')) return response({ error: 'ORIGIN_NOT_ALLOWED' }, 403);
  if (Number(request.headers.get('Content-Length') || 0) > 8192) return response({ error: 'BODY_TOO_LARGE' }, 413);
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) return response({ error: 'JSON_REQUIRED' }, 415);
  try {
    const address = request.headers.get('CF-Connecting-IP') || 'no-ip';
    const billingBucket = await hash(google.private_key_id + ':google-play:' + entry.app + ':' + address + ':' + Math.floor(now / 3600000));
    const rate = await db.prepare('INSERT INTO premium_limits(bucket,count,expires) VALUES(?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<90 RETURNING count')
      .bind(billingBucket, now + 7200000).first();
    if (!rate) return response({ error: 'TRY_LATER' }, 429);
    await db.prepare('DELETE FROM premium_limits WHERE expires<?').bind(now).run();
    if (!request.body) return response({ error: 'TOKEN_REQUIRED' }, 400);
    let raw = '';
    for await (const chunk of request.body.pipeThrough(new TextDecoderStream())) {
      raw += chunk;
      if (raw.length > 8192) return response({ error: 'BODY_TOO_LARGE' }, 413);
    }
    let purchaseToken;
    try { purchaseToken = JSON.parse(raw)?.purchaseToken; } catch {}
    if (typeof purchaseToken !== 'string' || !/^[!-~]{12,4096}$/.test(purchaseToken)) return response({ error: 'TOKEN_REQUIRED' }, 400);
    const device = await hash(deviceToken);
    const bearer = await accessToken(google, now, fetcher);
    const headers = { Authorization: 'Bearer ' + bearer };
    const base = publisher + entry.app + '/purchases/';
    const token = encodeURIComponent(purchaseToken);
    const lookup = () => fetcher(base + 'subscriptionsv2/tokens/' + token, { headers, redirect: 'error' });
    const verified = await lookup();
    if ([404, 410].includes(verified.status)) return response({ state: 'free' });
    if (!verified.ok) throw new Error('Verification unavailable');
    const allowTestPurchases = env.GOOGLE_PLAY_ALLOW_TEST_PURCHASES === 'yes';
    const entitlement = evaluatePurchase(await verified.json(), device, now, allowTestPurchases, product);
    if (entitlement.state !== 'premium') return response({ state: entitlement.state });
    const account = await hash('google-play:' + entry.app + ':' + device);
    const signing = { PAYPAL_ENVIRONMENT: 'live', [entry.env + '_LICENSE_PRIVATE_KEY']: signer };
    const license = await entry.issue(signing, device, account, entitlement.expires, now - 29 * day, now);
    if (!license) throw new Error('License unavailable');
    if (entitlement.needsAcknowledgement) {
      const acknowledgement = await fetcher(base + 'subscriptions/' + product + '/tokens/' + token + ':acknowledge', {
        headers: { ...headers, 'Content-Type': 'application/json' }, method: 'POST', body: '{}', redirect: 'error',
      });
      if (!acknowledgement.ok) {
        const retried = await lookup();
        if (!retried.ok) throw new Error('Acknowledgement unavailable');
        const verifiedAgain = evaluatePurchase(await retried.json(), device, now, allowTestPurchases, product);
        if (verifiedAgain.state !== 'premium' || verifiedAgain.needsAcknowledgement
          || verifiedAgain.expires !== entitlement.expires || verifiedAgain.renewing !== entitlement.renewing) throw new Error('Acknowledgement unavailable');
      }
    }
    return response({ state: 'premium', license, renewing: entitlement.renewing });
  } catch {
    return response({ error: 'PLAY_VERIFICATION_UNAVAILABLE' }, 503);
  }
}
