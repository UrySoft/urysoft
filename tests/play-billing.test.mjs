import assert from 'node:assert/strict';
import { createHash, createPublicKey, generateKeyPairSync, verify } from 'node:crypto';
import test from 'node:test';
import { catalog, evaluatePurchase, playRequest } from '../server/play-billing.js';

const now = Date.parse('2026-11-23T14:00:00Z');
const day = 86400000;
const deviceToken = 'sp_' + 'x'.repeat(43);
const device = createHash('sha256').update(deviceToken).digest('hex');
const token = 'play-fake-purchase-token';
const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const pk = privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64');
const serviceAccount = JSON.stringify({
  type: 'service_account', private_key_id: 'test-key', client_email: 'test@example.invalid',
  private_key: privateKey.export({ type: 'pkcs8', format: 'pem' }),
});
const db = { prepare: () => ({ bind: () => ({ first: async () => ({ count: 1 }), run: async () => {} }) }) };
const env = { GOOGLE_PLAY_BILLING_ENABLED: 'yes', GOOGLE_PLAY_SERVICE_ACCOUNT: serviceAccount,
  STORAGEY_LIVE_DB: db, STORAGEY_LIVE_LICENSE_PRIVATE_KEY: pk };

function purchase(changes = {}) {
  return {
    externalAccountIdentifiers: { obfuscatedExternalAccountId: device },
    subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
    acknowledgementState: 'ACKNOWLEDGEMENT_STATE_PENDING',
    lineItems: [{
      productId: 'premium', expiryTime: new Date(now + 365 * day).toISOString(),
      offerDetails: { basePlanId: 'annual' }, autoRenewingPlan: { autoRenewEnabled: true },
    }],
    ...changes,
  };
}

function request() {
  return new Request('https://urysoft.pages.dev/api/play-billing/storagey', {
    method: 'POST', headers: { Authorization: 'Bearer ' + deviceToken, 'Content-Type': 'application/json' },
    body: JSON.stringify({ purchaseToken: token }),
  });
}

test('Enforces installation binding and rejects test purchases outside the test environment', () => {
  assert.equal(evaluatePurchase(purchase(), 'b'.repeat(64), now).state, 'owner_mismatch');
  assert.equal(evaluatePurchase(purchase({ testPurchase: {} }), device, now).state, 'test_purchase');
  assert.equal(evaluatePurchase(purchase({ testPurchase: {} }), device, now, true).state, 'premium');
});

test('Checks subscription status, expiry, base plan and product', () => {
  for (const status of ['ACTIVE', 'IN_GRACE_PERIOD', 'CANCELED']) {
    assert.equal(evaluatePurchase(purchase({ subscriptionState: 'SUBSCRIPTION_STATE_' + status }), device, now).state, 'premium');
  }
  for (const status of ['ON_HOLD', 'PAUSED', 'EXPIRED']) {
    assert.equal(evaluatePurchase(purchase({ subscriptionState: 'SUBSCRIPTION_STATE_' + status }), device, now).state, 'free');
  }
  assert.equal(evaluatePurchase(purchase({ subscriptionState: 'SUBSCRIPTION_STATE_PENDING' }), device, now).state, 'pending_payment');
  for (const updates of [{ expiryTime: new Date(now).toISOString() }, { expiryTime: 'invalid' }, { productId: 'other' },
      { offerDetails: { basePlanId: 'monthly' } }, { autoRenewingPlan: null }]) {
    assert.equal(evaluatePurchase(purchase({ lineItems: [{ ...purchase().lineItems[0], ...updates }] }), device, now).state, 'free');
  }
});

test('Requires server verification and acknowledgement before issuing a short signed licence', async () => {
  const calls = [];
  const fetcher = async (uri, options) => {
    calls.push(uri);
    if (uri === 'https://oauth2.googleapis.com/token') {
      const assertion = options.body.get('assertion').split('.');
      assert.ok(verify('sha256', Buffer.from(assertion.slice(0, 2).join('.')), publicKey, Buffer.from(assertion[2], 'base64url')));
      return Response.json({ access_token: 'fake-access-token', expires_in: 3600 });
    }
    assert.equal(options.headers.Authorization, 'Bearer fake-access-token');
    if (uri.endsWith(':acknowledge')) {
      assert.equal(options.method, 'POST');
      return new Response(null, { status: 204 });
    }
    return Response.json(purchase());
  };
  const res = await playRequest({ request: request(), env, params: { app: 'storagey' } }, fetcher, now);
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.state, 'premium');
  const parts = data.license.split('.');
  assert.ok(verify('sha256', Buffer.from(parts.slice(0, 2).join('.')), createPublicKey(privateKey), Buffer.from(parts[2], 'base64url')));
  const claims = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
  assert.equal(claims.aud, catalog.storagey.app);
  assert.equal(claims.device, device);
  assert.equal(claims.exp, (now + day) / 1000);
  assert.equal(claims.paidThrough, (now + 365 * day) / 1000);
  assert.match(calls.at(-2), /com\.urysoft\.storagey\/purchases\/subscriptionsv2\/tokens\//);
  assert.match(calls.at(-1), /\/purchases\/subscriptions\/premium\/tokens\/.+:acknowledge$/);
});

test('Fails closed when disabled or acknowledgement fails and recognises an idempotent replay', async () => {
  const context = () => ({ request: request(), env, params: { app: 'storagey' } });
  const off = await playRequest({ ...context(), env: {} }, async () => assert.fail('Network call'), now);
  assert.equal(off.status, 503);
  const fetcher = async uri => uri.endsWith(':acknowledge')
    ? new Response(null, { status: 409 }) : Response.json(purchase());
  const failure = await playRequest(context(), fetcher, now);
  assert.equal(failure.status, 503);
  let acknowledged = false;
  const concurrent = await playRequest(context(), async uri => {
    if (uri.endsWith(':acknowledge')) {
      acknowledged = true;
      return new Response(null, { status: 409 });
    }
    return Response.json(purchase({ acknowledgementState: 'ACKNOWLEDGEMENT_STATE_' + (acknowledged ? 'ACKNOWLEDGED' : 'PENDING') }));
  }, now);
  assert.equal((await concurrent.json()).state, 'premium');
});
