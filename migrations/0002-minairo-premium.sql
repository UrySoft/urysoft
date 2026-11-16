-- Apply to a separate FOLDERY_PREMIUM database; no change to the comments database.
CREATE TABLE IF NOT EXISTS premium_devices (
 device_id TEXT PRIMARY KEY, account_id TEXT, created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS premium_challenges (
 code_hash TEXT PRIMARY KEY, device_id TEXT NOT NULL, mode TEXT NOT NULL, lang TEXT NOT NULL DEFAULT 'es',
 account_id TEXT, expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS premium_challenge_expiry ON premium_challenges(expires_at);
CREATE TABLE IF NOT EXISTS premium_checkouts (
 account_id TEXT PRIMARY KEY, request_id TEXT NOT NULL UNIQUE,
 subscription_id TEXT UNIQUE, approval_url TEXT, created_at INTEGER NOT NULL,
 status TEXT NOT NULL DEFAULT 'CREATING', paid_until INTEGER NOT NULL DEFAULT 0,
 verified_at INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS premium_webhooks (
 event_id TEXT PRIMARY KEY, processed_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS premium_limits (
 bucket TEXT PRIMARY KEY, count INTEGER NOT NULL, expires INTEGER NOT NULL
);
