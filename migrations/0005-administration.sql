-- Apply only to COMMENTS_DB. Existing comments are preserved.
ALTER TABLE comments ADD COLUMN moderation_state TEXT NOT NULL DEFAULT 'normal';
ALTER TABLE comments ADD COLUMN moderation_note TEXT NOT NULL DEFAULT '';
ALTER TABLE comments ADD COLUMN edited_at TEXT;
CREATE TABLE IF NOT EXISTS comment_reports (
 id INTEGER PRIMARY KEY AUTOINCREMENT, comment_id INTEGER NOT NULL,
 reason TEXT NOT NULL, details TEXT NOT NULL DEFAULT '',
 status TEXT NOT NULL DEFAULT 'open', created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS reports_comment ON comment_reports(comment_id,status);
CREATE TABLE IF NOT EXISTS moderation_audit (
 id INTEGER PRIMARY KEY AUTOINCREMENT, comment_id INTEGER NOT NULL, action TEXT NOT NULL,
 before_json TEXT NOT NULL, after_json TEXT NOT NULL, created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS audit_comment ON moderation_audit(comment_id,id);
CREATE TABLE IF NOT EXISTS analytics_counts (
 day TEXT NOT NULL, path TEXT NOT NULL, event TEXT NOT NULL, target TEXT NOT NULL,
 country TEXT NOT NULL, referrer TEXT NOT NULL, device TEXT NOT NULL, browser TEXT NOT NULL,
 language TEXT NOT NULL, source TEXT NOT NULL, hits INTEGER NOT NULL DEFAULT 1,
 PRIMARY KEY(day,path,event,target,country,referrer,device,browser,language,source)
);
CREATE INDEX IF NOT EXISTS analytics_day_event ON analytics_counts(day,event);
CREATE TABLE IF NOT EXISTS analytics_dedupe (id TEXT PRIMARY KEY, expires INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS admin_meta (key TEXT PRIMARY KEY,value TEXT NOT NULL);
INSERT OR IGNORE INTO admin_meta(key,value) VALUES('analytics_started',strftime('%Y-%m-%dT%H:%M:%fZ','now'));
