CREATE TABLE IF NOT EXISTS app_moderation_reports (
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
);
CREATE INDEX IF NOT EXISTS app_moderation_queue ON app_moderation_reports(status,id);
CREATE INDEX IF NOT EXISTS app_moderation_retention ON app_moderation_reports(created_at);
