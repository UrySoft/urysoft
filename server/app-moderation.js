import { authorize, json, rateLimit, readBody, sameOrigin } from './admin-common.js';

const subjects = {
  teclairc: new Set(['message', 'user']),
  minairo: new Set(['text', 'image', 'video', 'post', 'reply']),
};
const reasons = new Set(['abuse', 'sexual', 'violence', 'spam', 'privacy', 'other']);
const statuses = new Set(['open', 'resolved', 'dismissed']);
const actions = new Set(['reply', 'solve', 'translate', 'explain', 'summarize', 'rewrite', 'image', 'social', 'custom', 'notification']);
const referencePattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const retention = 90 * 86400000;

const reportSchema = [
  `CREATE TABLE IF NOT EXISTS app_moderation_reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  reference TEXT NOT NULL UNIQUE,
  app TEXT NOT NULL CHECK (app IN ('teclairc','minairo')),
  subject TEXT NOT NULL CHECK (subject IN ('message','user','text','image','video','post','reply')),
  reason TEXT NOT NULL CHECK (reason IN ('abuse','sexual','violence','spam','privacy','other')),
  details TEXT NOT NULL DEFAULT '',
  excerpt TEXT NOT NULL DEFAULT '',
  network TEXT NOT NULL DEFAULT '',
  channel TEXT NOT NULL DEFAULT '',
  alias TEXT NOT NULL DEFAULT '',
  action TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','resolved','dismissed')),
  resolution TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)`,
  `CREATE INDEX IF NOT EXISTS app_moderation_queue ON app_moderation_reports(status,id)`,
  `CREATE INDEX IF NOT EXISTS app_moderation_retention ON app_moderation_reports(created_at)`,
];

const readyDatabases = new WeakMap();
async function initializeReports(db) {
  let ready = readyDatabases.get(db);
  if (!ready) {
    ready = db.batch(reportSchema.map(sql => db.prepare(sql).bind())).catch(error => {
      readyDatabases.delete(db);
      throw error;
    });
    readyDatabases.set(db, ready);
  }
  await ready;
}


function bounded(value, limit) {
  if (value === undefined) return '';
  if (typeof value !== 'string' || value.length > limit || /[\u0000-\u0008\u000b-\u001f\u007f]/.test(value)) {
    throw { status: 400 };
  }
  return value.trim();
}

export function normalizeReport(app, value) {
  if (!Object.hasOwn(subjects, app) || !value || !subjects[app].has(value.subject)
      || !reasons.has(value.reason) || typeof value.reference !== 'string' || !referencePattern.test(value.reference)) {
    throw { status: 400 };
  }
  const details = bounded(value.details, 1000);
  const excerpt = ['message', 'text', 'post', 'reply'].includes(value.subject) ? bounded(value.excerpt, 1600) : '';
  if ((app === 'minairo' || value.subject === 'message') && !details && !excerpt) throw { status: 400 };
  const report = { reference: value.reference, subject: value.subject, reason: value.reason,
    details, excerpt, network: '', channel: '', alias: '', action: '' };
  if (app === 'teclairc') {
    report.network = bounded(value.network, 259);
    report.channel = bounded(value.channel, 100);
    report.alias = bounded(value.alias, 64);
    if (!/^[a-z0-9.-]+:[0-9]{1,5}$/i.test(report.network) || !report.alias || /\s/.test(report.alias)) throw { status: 400 };
    if (report.channel && !/^[#&+!][^\s,]+$/.test(report.channel)) throw { status: 400 };
  } else {
    report.action = bounded(value.action, 24);
    if (!actions.has(report.action)) throw { status: 400 };
  }
  return report;
}

async function readReport(request) {
  if (!request.headers.get('Content-Type')?.startsWith('application/json')) throw { status: 415 };
  if (!request.body) throw { status: 400 };
  const reader = request.body.getReader();
  const decoder = new TextDecoder();
  let length = 0;
  let content = '';
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      length += value.byteLength;
      if (length > 16000) {
        await reader.cancel();
        throw { status: 413 };
      }
      content += decoder.decode(value, { stream: true });
    }
    return JSON.parse(content + decoder.decode());
  } catch (error) {
    throw { status: error.status || 400 };
  } finally {
    reader.releaseLock();
  }
}

export async function submitReport({ request, env }, app, now = Date.now()) {
  if (request.method !== 'POST') return json({ error: 'METHOD' }, 405);
  if (!env.COMMENTS_DB || !env.COMMENTS_ADMIN_KEY) return json({ error: 'UNAVAILABLE' }, 503);
  if (request.headers.has('Origin')) return json({ error: 'ORIGIN' }, 403);
  if (Number(request.headers.get('Content-Length') || 0) > 18000) return json({ error: 'TOO_LARGE' }, 413);
  try {
    const report = normalizeReport(app, await readReport(request));
    const db = env.COMMENTS_DB;
    await initializeReports(db);
    const existing = await db.prepare('SELECT id FROM app_moderation_reports WHERE reference=? AND app=?')
      .bind(report.reference, app).first();
    if (existing) return json({ ok: true, reference: report.reference }, 200);
    if (!await rateLimit(request, env, 'app-reports:' + app, 8)) return json({ error: 'RATE_LIMIT' }, 429);
    await db.batch([
      db.prepare('INSERT INTO app_moderation_reports(reference,app,subject,reason,details,excerpt,network,channel,alias,action,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(reference) DO NOTHING')
        .bind(report.reference, app, report.subject, report.reason, report.details, report.excerpt, report.network, report.channel, report.alias, report.action, now, now),
      db.prepare('DELETE FROM app_moderation_reports WHERE created_at<?').bind(now - retention),
      db.prepare('DELETE FROM comment_limits WHERE expires<?').bind(now),
    ]);
    return json({ ok: true, reference: report.reference }, 201);
  } catch (error) {
    return json({ error: error.status ? 'BAD_REPORT' : 'UNAVAILABLE' }, error.status || 503);
  }
}

export async function reviewReports({ request, env }, now = Date.now()) {
  if (!env.COMMENTS_DB) return json({ error: 'Denuncias de aplicaciones no disponibles.' }, 503);
  if (!await authorize(request, env)) return json({ error: 'Acceso no autorizado o sesión caducada.' }, 401);
  const db = env.COMMENTS_DB;
  try {
    await initializeReports(db);
    if (request.method === 'GET') {
      const query = new URL(request.url).searchParams;
      const app = query.get('app') || '';
      const status = query.get('status') || 'open';
      const before = query.has('before') ? Number(query.get('before')) : Number.MAX_SAFE_INTEGER;
      if ((app && !Object.hasOwn(subjects, app)) || !statuses.has(status) || !Number.isSafeInteger(before) || before < 1) {
        return json({ error: 'Filtro no válido.' }, 400);
      }
      await db.prepare('DELETE FROM app_moderation_reports WHERE created_at<?').bind(now - retention).run();
      const rows = (await db.prepare('SELECT * FROM app_moderation_reports WHERE status=? AND id<? AND (?=\'\' OR app=?) ORDER BY id DESC LIMIT 26')
        .bind(status, before, app, app).all()).results;
      return json({ items: rows.slice(0, 25), next: rows.length > 25 ? rows[24].id : null });
    }
    if (request.method !== 'PATCH') return json({ error: 'Método no admitido.' }, 405);
    if (!sameOrigin(request)) return json({ error: 'Origen no autorizado.' }, 403);
    const body = await readBody(request, 4000);
    if (!body || !Number.isSafeInteger(body.id) || body.id < 1 || !Number.isSafeInteger(body.version) || !statuses.has(body.status)) {
      return json({ error: 'Datos de revisión no válidos.' }, 400);
    }
    const resolution = bounded(body.resolution, 1000);
    if (body.status !== 'open' && !resolution) return json({ error: 'Indica la decisión de moderación.' }, 400);
    const row = await db.prepare('UPDATE app_moderation_reports SET status=?,resolution=?,updated_at=? WHERE id=? AND updated_at=? AND created_at>=? RETURNING id')
      .bind(body.status, resolution, Math.max(now, body.version + 1), body.id, body.version, now - retention).first();
    return row ? json({ ok: true }) : json({ error: 'La denuncia cambió. Actualiza la lista.' }, 409);
  } catch (error) {
    return json({ error: error.status ? 'Datos no válidos.' : 'Denuncias de aplicaciones no disponibles.' }, error.status || 503);
  }
}
