# Foldery.2.AI 1.5.8 — UrySoft Updater
Package cat.urysoft.widgets retained. Version code 19, following 1.5.7 code 18. Original signing certificate must match before any output APK is delivered.

The home screen checks https://urysoft.pages.dev/updates/foldery.json at most once per day after a successful response. Checks run in a worker thread, only show dialogs while the activity is active, and do not interrupt the first widget setup. Options offers an explicit check. Internet failures are silent for automatic checks and explained for manual checks. Widgets remain usable offline.

Contract: schemaVersion=1, app=foldery, status=unpublished or published. Published entries require a positive integral versionCode, a versionName, a 64-digit SHA-256 and an HTTPS APK URL under the official UrySoft/urysoft release path. No redirects are followed when retrieving the manifest. 8-second connection/read timeouts and a 64-KiB manifest limit.

A newer version is offered; the owner chooses Download or Later. The download opens in the browser and installation remains Android-controlled. This build does not download APKs silently, verify downloaded file bytes itself or force-install a mandatory update.

No subscription logic or PayPal configuration changed. The default unpaid/premium behavior of the original source is retained. The original signing files are deliberately excluded from the distributable source archive. Restore your existing signing folder locally to compile a compatible signed release; never put it on GitHub.

Build: Java 17+, Android platform 36 and build-tools 36.0.0, then python build_local.py --sdk PATH --output PATH. No third-party runtime library was added. tests/UpdaterTests.java uses org.json only in the desktop test harness.
