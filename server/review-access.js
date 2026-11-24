import { catalog } from './play-billing.js';
import { sha256 } from './foldery-live-license.js';

const day = 86400000;
const reply = (data, status = 200) => Response.json(data, {
  status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' },
});
const isHexDigest = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const asHex = data => Array.from(new Uint8Array(data), byte => byte.toString(16).padStart(2, '0')).join('');

const maxIdleTime = 90 * day;

async function reviewRenewal(db, app, device, verifier, fields, now) {
  const redeeming = Object.hasOwn(fields, 'invitation');
  if (redeeming) {
    if (typeof fields.invitation !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(fields.invitation)
        || await sha256(fields.invitation) !== verifier) return null;
  } else if (!isHexDigest(fields.renewal)) {
    return null;
  }

  // Only the random renewal is sent to the installation. Its public invitation verifier
  // cannot produce a renewal, and rotating the verifier invalidates all earlier grants.
  await db.prepare(`CREATE TABLE IF NOT EXISTS play_review_grants (
    token_hash TEXT PRIMARY KEY, app TEXT NOT NULL, device TEXT NOT NULL,
    verifier TEXT NOT NULL, expires INTEGER NOT NULL
  )`).run();
  if (!redeeming) {
    const entry = await db.prepare(`UPDATE play_review_grants SET expires=?
      WHERE token_hash=? AND app=? AND device=? AND verifier=? AND expires>? RETURNING expires`)
      .bind(now + maxIdleTime, await sha256(fields.renewal), app, device, verifier, now).first();
    return entry ? fields.renewal : null;
  }
  const renewal = asHex(crypto.getRandomValues(new Uint8Array(32)));
  await db.prepare('DELETE FROM play_review_grants WHERE app=? AND (expires<=? OR verifier<>?)')
    .bind(app, now, verifier).run();
  await db.prepare('INSERT INTO play_review_grants(token_hash,app,device,verifier,expires) VALUES(?,?,?,?,?)')
    .bind(await sha256(renewal), app, device, verifier, now + maxIdleTime).run();
  return renewal;
}

async function readFields(request) {
  if (!request.body) return null;
  let raw = '';
  for await (const chunk of request.body.pipeThrough(new TextDecoderStream())) {
    raw += chunk;
    if (raw.length > 512) return null;
  }
  try { return JSON.parse(raw); } catch { return null; }
}

export async function reviewAccess({ request, env, params }, now = Date.now(), defaultVerifier) {
  const app = Object.hasOwn(catalog, params.app) ? catalog[params.app] : null;
  if (!app) return reply({ error: 'UNKNOWN_APP' }, 404);
  if (request.method !== 'POST') return reply({ error: 'METHOD_NOT_ALLOWED' }, 405);
  if (request.headers.get('Origin')) return reply({ error: 'ORIGIN_NOT_ALLOWED' }, 403);
  if (request.headers.get('Content-Type')?.split(';')[0].trim() !== 'application/json') return reply({ error: 'JSON_REQUIRED' }, 415);
  if (Number(request.headers.get('Content-Length') || 0) > 512) return reply({ error: 'TOO_LARGE' }, 413);
  const bearer = request.headers.get('Authorization')?.match(/^Bearer ([a-z]{2}_[A-Za-z0-9_-]{43})$/)?.[1];
  if (!bearer?.startsWith(app.prefix)) return reply({ error: 'INSTALLATION_REQUIRED' }, 401);

  const digest = env[app.env + '_PLAY_REVIEW_SHA256'] ?? defaultVerifier;
  const signer = env[app.env + '_LIVE_LICENSE_PRIVATE_KEY'];
  const db = env[app.env + '_LIVE_DB'];
  if (!isHexDigest(digest) || !signer || !db) return reply({ error: 'REVIEW_ACCESS_UNAVAILABLE' }, 503);

  try {
    const fields = await readFields(request);
    if (!fields || typeof fields !== 'object' || Array.isArray(fields) || Object.keys(fields).length !== 1) {
      return reply({ error: 'INVALID_REQUEST' }, 400);
    }
    const device = await sha256(bearer);
    const address = request.headers.get('CF-Connecting-IP') || 'unknown';
    const bucket = await sha256([digest, 'review-rate', app.app, address, Math.floor(now / 3600000)].join('|'));
    const usage = await db.prepare('INSERT INTO premium_limits(bucket,count,expires) VALUES(?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<90 RETURNING count')
      .bind(bucket, now + 7200000).first();
    if (!usage) return reply({ error: 'TRY_LATER' }, 429);
    await db.prepare('DELETE FROM premium_limits WHERE expires<?').bind(now).run();
    const renewal = await reviewRenewal(db, app.app, device, digest, fields, now);
    if (!renewal) return reply({ error: Object.hasOwn(fields, 'invitation') ? 'INVALID_INVITATION' : 'INVALID_RENEWAL' }, 403);

    const account = await sha256('complimentary-review|' + app.app + '|' + device);
    const license = await app.issue({ PAYPAL_ENVIRONMENT: 'live', [app.env + '_LICENSE_PRIVATE_KEY']: signer },
      device, account, now + day, now, now);
    if (!license) return reply({ error: 'REVIEW_ACCESS_UNAVAILABLE' }, 503);
    return reply({ license, renewal });
  } catch {
    return reply({ error: 'REVIEW_ACCESS_UNAVAILABLE' }, 503);
  }
}
