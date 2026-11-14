# Foldery Premium: integration in Sandbox only

This integration cannot accept live payments. Both the API entry point and the web checkout require the Sandbox environment. The original live PayPal plan remains a draft, and Foldery's public update manifest remains unpublished.

The approved offer is EUR 6 annually (EUR 0.50 per month equivalent), automatically renewing. Free: one widget, eight app/contact shortcuts, three shapes and two finishes. Premium: multiple widgets, up to 48 shortcuts per widget, all twelve shapes and eight finishes, photo backgrounds and advanced shadows. Existing saved widgets and designs remain intact.

## Identity and payment proof

Google customer sign-in has its own client ID, distinct from the comments administration client. It does not grant access to moderation. The server checks the Google signature, issuer, audience, expiry and a one-use, expiring nonce before binding an installation to a customer. The database stores a hash of the Google subject, not email, profile or Google tokens.

The Android app creates a random installation secret, stores it in app-private data with backups disabled, and sends it only in an HTTPS Authorization header to the official API. Browser handoff links contain a short-lived random code in a URL fragment. The browser removes the fragment before loading Google's library. Browser return parameters and local paid flags never unlock Premium.

The server checks the configured PayPal plan against the annual EUR 6 offer, assigns the verified account ID as custom_id, and verifies ownership again when reconciling the subscription. Completed EUR 6 payments establish a paid period. ACTIVE without payment, pending payments, expired payments and refunded transactions do not grant access. Pending checkouts reuse their original subscription instead of creating duplicate renewals. Incomplete creation reservations older than 48 hours require review rather than blindly retrying after PayPal's idempotency retention.

Cancellation stops renewal; an already paid period remains valid. A signed, device-bound RS256 license expires after at most 30 days offline, and never later than the paid period. Online refresh can remove a refunded or expired entitlement sooner. This offline window is disclosed in the app and checkout. A rooted device or modified APK is outside this protection model.

## Server configuration

Use a separate D1 binding named PREMIUM_DB, initialized with migrations/0002-foldery-premium.sql. Do not apply this migration to COMMENTS_DB. Configure:

- FOLDERY_PREMIUM_ENABLED=sandbox
- PAYPAL_ENVIRONMENT=sandbox
- PAYPAL_CLIENT_ID, PAYPAL_PLAN_ID, PAYPAL_WEBHOOK_ID
- FOLDERY_GOOGLE_CLIENT_ID (customer client, not GOOGLE_CLIENT_ID)
- PAYPAL_CLIENT_SECRET and FOLDERY_LICENSE_PRIVATE_KEY as encrypted secrets.

The signing value is base64 PKCS8 RSA private key. Only its public SPKI verification key is compiled into Android. Neither private key, client secret nor original APK signing files belong in GitHub, the static website or source/APK archives.

PayPal webhook target: https://urysoft.pages.dev/api/foldery/webhook. Verify notifications through PayPal's signature-verification API before processing. Duplicate event IDs are recorded, and reconciliation fetches the current subscription and transaction state instead of trusting a notification's claimed status.

## Validation and remaining release requirements

Run node tests/paypal-subscriptions.test.mjs, node tests/foldery-premium.test.mjs and the existing comments tests. Native PremiumPolicy and PremiumClaims host tests cover limits, legacy configurations, signed licenses, device binding, tampering and expiry. The Android build verifies the original signing certificate and the existing core tests.

Before live release: complete a real browser Sandbox purchase and restoration test; test the APK on a phone; confirm the seller account and final tax/invoicing treatment; complete Google's public branding/consent configuration and publish its audience; prepare a separate live PayPal connection and reconcile its plan; add the live webhook; explicitly enable and test the live path in a subsequent reviewed change. None of these is implied by passing mocked integration tests.

Official references: https://developer.paypal.com/api/subscriptions/v1/ ; https://developer.paypal.com/subscriptions/webhooks/ ; https://developers.google.com/identity/gsi/web/reference/js-reference .
