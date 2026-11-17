# Foldery 1.6.3 (26)

Fixes the remaining launcher clipping reported with the 1.6.2 screenshot. Per-icon drawing outside a collection child was not portable, despite passing an isolated RemoteViews test.

The collection now contains full-width composited row strips. Shadows are drawn from the combined silhouettes before foreground content, inside the row image, including adjacent-row shadow contributions. The first and last rows reserve the shadow outset. Each app or contact retains its own precisely sized, accessible touch target connected to the existing launch template. The preview uses the same row rasterizer. Android 8–11 uses the same rows through the RemoteViewsService fallback; Android 12+ uses RemoteCollectionItems. Nine-position alignment remains supported.

Native tests mount the complete AppWidgetHostView and GridView in an active window and compare shadow pixels with the uncut row raster in eight directions. Tests also exercise touch targets, an actual contact tap and contact details launch, multiple rows and scrolling to the final row. Physical launcher validation remains pending.

Build: android/foldery/build_local.py with Java 17+, Python 3 and Android SDK 36; original private signing directory required. Package cat.urysoft.widgets, original certificate retained. Release and public feed/home version updates are gated by the full widget tests in publish-foldery-1.6.3.yml. The 1.6.2 workflow no longer responds to later Android source changes, preventing old-version republishing.
