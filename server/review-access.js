import { catalog } from './play-billing.js';
import { sha256 } from './foldery-live-license.js';

const encoder = new TextEncoder();
const day = 86400000;
const reply = (data, status = 200) => Response.json(data, {
  status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' },
});
const isHexDigest = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const hexBytes = hex => Uint8Array.from(hex.match(/../g), byte => parseInt(byte, 16));
const asHex = data => Array.from(new Uint8Array(data), byte => byte.toString(16).padStart(2, '0')).join('');

// The only credential retained on a device is bound to that install and this app.
// Rotating the configured invitation invalidates its outstanding renewals.
async function renewalKey(digest) {
  return crypto.subtle.importKey('raw', hexBytes(digest), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign', 'verify']);
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

export async function reviewAccess({ request, env, params }, now = Date.now()) {
  const app = Object.hasOwn(catalog, params.app) ? catalog[params.app] : null;
  if (!app) return reply({ error: 'UNKNOWN_APP' }, 404);
  if (request.method !== 'POST') return reply({ error: 'METHOD_NOT_ALLOWED' }, 405);
  if (request.headers.get('Origin')) return reply({ error: 'ORIGIN_NOT_ALLOWED' }, 403);
  if (request.headers.get('Content-Type')?.split(';')[0].trim() !== 'application/json') return reply({ error: 'JSON_REQUIRED' }, 415);
  if (Number(request.headers.get('Content-Length') || 0) > 512) return reply({ error: 'TOO_LARGE' }, 413);
  const bearer = request.headers.get('Authorization')?.match(/^Bearer ([a-z]{2}_[A-Za-z0-9_-]{43})$/)?.[1];
  if (!bearer?.startsWith(app.prefix)) return reply({ error: 'INSTALLATION_REQUIRED' }, 401);

  const digest = env[app.env + '_PLAY_REVIEW_SHA256'];
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
    const key = await renewalKey(digest);
    const binding = encoder.encode('review-install-v1|' + app.app + '|' + device);
    if (Object.hasOwn(fields, 'invitation')) {
      if (typeof fields.invitation !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(fields.invitation)
          || await sha256(fields.invitation) !== digest) return reply({ error: 'INVALID_INVITATION' }, 403);
    } else if (!isHexDigest(fields.renewal) || !await crypto.subtle.verify('HMAC', key, hexBytes(fields.renewal), binding)) {
      return reply({ error: 'INVALID_RENEWAL' }, 403);
    }
    const account = await sha256('complimentary-review|' + app.app + '|' + device);
    const license = await app.issue({ PAYPAL_ENVIRONMENT: 'live', [app.env + '_LICENSE_PRIVATE_KEY']: signer },
      device, account, now + day, now, now);
    if (!license) return reply({ error: 'REVIEW_ACCESS_UNAVAILABLE' }, 503);
    return reply({ license, renewal: asHex(await crypto.subtle.sign('HMAC', key, binding)) });
  } catch {
    return reply({ error: 'REVIEW_ACCESS_UNAVAILABLE' }, 503);
  }
}
