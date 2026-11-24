import assert from 'node:assert/strict';
import test from 'node:test';
import { DatabaseSync } from 'node:sqlite';
import { readFileSync } from 'node:fs';
import { randomUUID, generateKeyPairSync, sign } from 'node:crypto';
import { normalizeReport, submitReport, reviewReports } from '../server/app-moderation.js';

const site = 'https://urysoft.pages.dev';
const now = Date.now();
const day = 86400000;
const base = () => ({ reference: randomUUID(), reason:'abuse', subject:'message',
  network:'irc.chathispano.com:6697', channel:'#madrid', alias:'Example', excerpt:'Texto de prueba' });
function database(migrated = true) {
  const db = new DatabaseSync(':memory:');
  db.exec(readFileSync(new URL('../migrations/0001-comments.sql', import.meta.url), 'utf8'));
  if (migrated) db.exec(readFileSync(new URL('../migrations/0006-app-moderation.sql', import.meta.url), 'utf8'));
  const prepare = sql => ({
    bind: (...args) => ({
      first: async () => db.prepare(sql).get(...args),
      all: async () => ({ results:db.prepare(sql).all(...args) }),
      run: async () => db.prepare(sql).run(...args),
    }),
  });
  const batch = async statements => {
    db.exec('BEGIN');
    try {
      for (const statement of statements) await statement.run();
      db.exec('COMMIT');
    } catch (error) {
      db.exec('ROLLBACK');
      throw error;
    }
  };
  return { raw:db, env:{
    COMMENTS_DB:{ prepare,batch }, COMMENTS_ADMIN_KEY:'fixture',
    COMMENTS_ADMIN_EMAIL:'moderator@example.invalid', GOOGLE_CLIENT_ID:'device-admin-test',
  } };
}

function request(body, app = 'teclairc', extra={}) {
  return new Request(site + '/api/' + app + '/moderation-report', {
    method:'POST', body:JSON.stringify(body), headers:{'Content-Type':'application/json', 'CF-Connecting-IP':'192.0.2.8', ...extra},
  });
}

test('Keeps only the matching app metadata and requires evidence for content reports', () => {
  const report = normalizeReport('teclairc', { ...base(), source:'private source', action:'notification' });
  assert.equal(report.action, '');
  assert.equal(Object.hasOwn(report, 'source'), false);
  assert.equal(normalizeReport('teclairc', { ...base(), subject:'user' }).excerpt, '');
  assert.throws(() => normalizeReport('teclairc', { ...base(), channel:'private conversation' }));
  assert.throws(() => normalizeReport('teclairc', { ...base(), excerpt:'' }));
  assert.throws(() => normalizeReport('minairo', { ...base(), subject:'video', action:'image', details:'', excerpt:'cannot attach as video' }));
  const image = normalizeReport('minairo', { ...base(), subject:'image', action:'image', details:'Describe generated image' });
  assert.equal(image.excerpt, '');
  assert.equal(image.alias, '');
  assert.equal(image.network, '');
  assert.throws(() => normalizeReport('minairo', { ...base(), subject:'text', action:'unexpected' }));
});

test('Rejects cross-site or oversized submissions and makes retries idempotent in SQLite', async () => {
  const { raw, env } = database();
  const report = base();
  assert.equal((await submitReport({ request:request(report, 'teclairc', { Origin:'https://invalid.example' }), env }, 'teclairc', now)).status, 403);
  assert.equal((await submitReport({ request:request({ ...report, details:'a'.repeat(20000) }), env }, 'teclairc', now)).status, 413);
  const initial = await submitReport({ request:request(report), env }, 'teclairc', now);
  assert.equal(initial.status, 201);
  assert.equal((await initial.json()).reference, report.reference);
  const again = await submitReport({ request:request(report), env }, 'teclairc', now + 1);
  assert.equal(again.status, 200);
  assert.equal(raw.prepare('SELECT COUNT(*) AS n FROM app_moderation_reports').get().n, 1);
  for (let i=0; i<7; i++) {
    assert.equal((await submitReport({ request:request(base()), env }, 'teclairc', now)).status, 201);
  }
  assert.equal((await submitReport({ request:request(base()), env }, 'teclairc', now)).status, 429);
  assert.equal(raw.prepare('SELECT COUNT(*) AS n FROM app_moderation_reports').get().n, 8);
  raw.close();
});

test('Moderation requires the administrator, expires old reports and rejects stale decisions', async t => {
  const { raw, env } = database();
  assert.equal((await submitReport({ request:request(base()), env }, 'teclairc', now - 91 * day)).status, 201);
  assert.equal((await submitReport({ request:request(base()), env }, 'teclairc', now)).status, 201);
  const admin = site + '/api/admin/app-reports';
  assert.equal((await reviewReports({ request:new Request(admin), env }, now)).status, 401);
  const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength:2048 });
  const key = { ...publicKey.export({ format:'jwk' }), kid:'qa-key', alg:'RS256' };
  t.mock.method(globalThis,'fetch',async () => Response.json({ keys:[key] }));
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  const token = email => {
    const payload = { iss:'https://accounts.google.com', sub:'moderator', aud:env.GOOGLE_CLIENT_ID,
      iat:Math.floor(now / 1000), exp:Math.floor(now / 1000)+300, email, email_verified:true };
    const signed = encode({ alg:'RS256', kid:key.kid }) + '.' + encode(payload);
    return signed + '.' + sign('sha256', Buffer.from(signed), privateKey).toString('base64url');
  };
  const authorization = 'Bearer ' + token(env.COMMENTS_ADMIN_EMAIL);
  const read = url => new Request(url || admin, { headers:{Authorization:authorization} });
  const write = (body, origin=site) => new Request(admin, { method:'PATCH', body:JSON.stringify(body),
    headers:{Authorization:authorization, 'Content-Type':'application/json', Origin:origin} });
  assert.equal((await reviewReports({ request:new Request(admin, { headers:{Authorization:'Bearer ' + token('other@example.invalid')} }), env }, now)).status, 401);
  const { items } = await (await reviewReports({ request:read(), env }, now)).json();
  assert.equal(items.length, 1);
  assert.equal(raw.prepare('SELECT COUNT(*) AS n FROM app_moderation_reports').get().n, 1);
  const change = { id:items[0].id, version:items[0].updated_at, status:'resolved', resolution:'Contactado el operador en la prueba' };
  assert.equal((await reviewReports({ request:write(change,'https://other.example'), env }, now)).status, 403);
  assert.equal((await reviewReports({ request:write({...change,resolution:''}), env }, now)).status, 400);
  assert.equal((await reviewReports({ request:write(change), env }, now)).status, 200);
  assert.equal((await reviewReports({ request:write(change), env }, now)).status, 409);
  const closed = await (await reviewReports({ request:read(admin+'?status=resolved'), env },now)).json();
  assert.equal(closed.items[0].resolution, change.resolution);
  raw.close();
});

test('the first app report installs the same schema as the migration', async () => {
  const { env, raw } = database(false);
  const expected = database().raw;
  const report = base();
  const response = await submitReport({ request: request(report), env }, 'teclairc', now);
  assert.equal(response.status, 201);
  const schema = db => db.prepare("SELECT name, sql FROM sqlite_master WHERE name LIKE 'app_moderation_%' ORDER BY name").all();
  assert.deepEqual(schema(raw), schema(expected));
  assert.equal(raw.prepare('SELECT reference FROM app_moderation_reports').get().reference, report.reference);
});
