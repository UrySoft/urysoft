CREATE TABLE IF NOT EXISTS premium_receipt_sessions (
 token_hash TEXT PRIMARY KEY, account_id TEXT NOT NULL, request_id TEXT NOT NULL,
 expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS premium_receipt_session_expiry ON premium_receipt_sessions(expires_at);
CREATE TABLE IF NOT EXISTS premium_confirmations (
 request_id TEXT PRIMARY KEY, account_id TEXT NOT NULL, subscription_id TEXT NOT NULL,
 created_at INTEGER NOT NULL, document_html TEXT NOT NULL
);
