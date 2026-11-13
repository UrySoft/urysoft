CREATE TABLE IF NOT EXISTS comments (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 app TEXT NOT NULL,
 name TEXT NOT NULL,
 kind TEXT NOT NULL CHECK(kind IN ('review','idea','bug')),
 message TEXT NOT NULL,
 rating INTEGER NOT NULL CHECK(rating BETWEEN 0 AND 5),
 status TEXT NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','approved','hidden')),
 reply TEXT NOT NULL DEFAULT '',
 created_at TEXT NOT NULL,
 updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS comments_app_status ON comments(app,status,created_at);
CREATE TABLE IF NOT EXISTS comment_limits (bucket TEXT PRIMARY KEY,count INTEGER NOT NULL,expires INTEGER NOT NULL);
