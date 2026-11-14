-- Apply only to FOLDERY_LIVE_DB before enabling the reviewed live purchase flow.
CREATE TABLE IF NOT EXISTS premium_agreements (
 request_id TEXT PRIMARY KEY,
 account_id TEXT NOT NULL,
 terms_version TEXT NOT NULL,
 privacy_version TEXT NOT NULL,
 accepted_at INTEGER NOT NULL,
 lang TEXT NOT NULL,
 annual_amount_cents INTEGER NOT NULL,
 currency TEXT NOT NULL,
 automatic_renewal INTEGER NOT NULL
);
