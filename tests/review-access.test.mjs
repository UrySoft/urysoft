import assert from 'node:assert/strict';
import { createHash, generateKeyPairSync, randomBytes, verify } from 'node:crypto';
import test from 'node:test';
import { DatabaseSync } from 'node:sqlite';
import { catalog, evaluatePurchase } from '../server/play-billing.js';
import { reviewAccess } from '../server/review-access.js';

const hash = value => createHash('sha256').update(value).digest('hex');
const invitation = randomBytes(32).toString('base64url');
const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const key = privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64');
const sqlite = new DatabaseSync(':memory:');
sqlite.exec('CREATE TABLE premium_limits(bucket TEXT PRIMARY KEY, count INTEGER NOT NULL, expires INTEGER NOT NULL)');
const db = {
  prepare(sql) {
    const statement = sqlite.prepare(sql);
    return {
      run: () => statement.run(),
      bind: (...args) => ({ first: () => statement.get(...args), run: () => statement.run(...args) }),
    };
  },
};
const now = Date.UTC(2026, 10, 24);
function context(slug, device, fields, credential = invitation) {
  const { env: name } = catalog[slug];
  return {
    params: { app: slug },
    env: { [name + '_PLAY_REVIEW_SHA256']: hash(credential), [name + '_LIVE_LICENSE_PRIVATE_KEY']: key, [name + '_LIVE_DB']: db },
    request: new Request('https://example.invalid/api/review-access/' + slug, {
      method: 'POST', headers: { Authorization: 'Bearer ' + device, 'Content-Type': 'application/json' },
      body: JSON.stringify(fields),
    }),
  };
}

for (const [slug, app] of Object.entries(catalog)) {
  test(slug + ' allows two fresh installations and renews only the original installation licence', async () => {
    const device = app.prefix + randomBytes(32).toString('base64url');
    const other = app.prefix + randomBytes(32).toString('base64url');
    const enter = await reviewAccess(context(slug, device, { invitation }), now);
    assert.equal(enter.status, 200);
    const grant = await enter.json(), parts = grant.license.split('.');
    assert.ok(verify('sha256', Buffer.from(parts.slice(0, 2).join('.')), publicKey, Buffer.from(parts[2], 'base64url')));
    const claims = JSON.parse(Buffer.from(parts[1], 'base64url'));
    assert.equal(claims.aud, app.app);
    assert.equal(claims.device, hash(device));
    assert.equal(claims.tier, 'premium');
    assert.equal(claims.exp, (now + 86400000) / 1000);
    assert.equal((await reviewAccess(context(slug, device, { renewal: grant.renewal }), now + 3600000)).status, 200);
    assert.equal((await reviewAccess(context(slug, other, { renewal: grant.renewal }), now)).status, 403);
    const next = await reviewAccess(context(slug, other, { invitation }), now);
    assert.equal(next.status, 200);
    assert.notEqual((await next.json()).renewal, grant.renewal);
    const rotated = randomBytes(32).toString('base64url');
    assert.equal((await reviewAccess(context(slug, device, { invitation }, rotated), now)).status, 403);
    assert.equal((await reviewAccess(context(slug, device, { renewal: grant.renewal }, rotated), now)).status, 403);
  });
}

test('missing config, malformed credentials and cross-app requests never grant access', async () => {
  const device = 'sp_' + randomBytes(32).toString('base64url');
  const make = fields => context('storagey', device, fields);
  const off = make({ invitation });
  delete off.env.STORAGEY_PLAY_REVIEW_SHA256;
  assert.equal((await reviewAccess(off, now)).status, 503);
  assert.equal((await reviewAccess(make({ invitation: 'guess' }), now)).status, 403);
  assert.equal((await reviewAccess(make({ invitation: invitation.repeat(20) }), now)).status, 400);
  assert.equal((await reviewAccess(make({ invitation, renewal: 'a'.repeat(64) }), now)).status, 400);
  assert.equal((await reviewAccess(context('minairo', device, { invitation }), now)).status, 401);
});

test('widget purchases only validate for their own product in Google Play', () => {
  const item = { productId: 'foldery_premium', offerDetails: { basePlanId: 'annual' }, autoRenewingPlan: { autoRenewEnabled: true }, expiryTime: new Date(now + 86400000).toISOString() };
  const response = { externalAccountIdentifiers: { obfuscatedExternalAccountId: 'device' }, subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE', acknowledgementState: 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED', lineItems: [item] };
  assert.equal(evaluatePurchase(response, 'device', now, false, catalog.foldery.product).state, 'premium');
  assert.equal(evaluatePurchase(response, 'device', now, false, catalog.widgery.product).state, 'free');
  assert.equal(evaluatePurchase(response, 'device', now).state, 'free');
});

test('idle grants expire, invitations remain reusable and no reusable secret is stored in the database', async () => {
  const device = 'fp_' + randomBytes(32).toString('base64url');
  const make = fields => context('foldery', device, fields);
  const { renewal } = await (await reviewAccess(make({ invitation }), now)).json();
  const saved = sqlite.prepare('SELECT token_hash, device FROM play_review_grants WHERE device=?').get(hash(device));
  assert.equal(saved.token_hash, hash(renewal));
  const later = now + 91 * 86400000;
  assert.equal((await reviewAccess(make({ renewal }), later)).status, 403);
  assert.equal((await reviewAccess(make({ invitation }), later)).status, 200);
  const disabled = make({ invitation });
  disabled.env.FOLDERY_PLAY_REVIEW_SHA256 = 'disabled';
  assert.equal((await reviewAccess(disabled, now, hash(invitation))).status, 503);
});
