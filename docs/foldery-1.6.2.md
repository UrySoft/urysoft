# Foldery 1.6.2 (25)

Based on the complete published Foldery 1.6.1 project. Package cat.urysoft.widgets; original signing certificate retained.

The icon ImageView now includes the full transparent 32 dp shadow halo. Negative layout margins preserve the existing cell dimensions and icon position. Negative ImageView padding previously placed the shadow outside the view drawing bounds, clipping it even though its parents did not clip children.

Nine physical alignment positions are stored per widget, defaulting to center. Alignment uses the spare width and height after margins, heading and edit button. Overflow starts at the top, preserving access to all rows. Preview and desktop use the same resolved offsets. English, Catalan and Spanish resources included.

Sources: android/foldery/. Build with Python 3, Java 17+ and Android platform/build tools 36 using python3 build_local.py --sdk /path/to/sdk --output Foldery-1.6.2.apk. The original private signing directory is required and deliberately excluded from this repository. No replacement key is generated.

Publication is gated on native pixel and RemoteViews measurement tests in .github/workflows/publish-foldery-1.6.2.yml. It verifies the signed installer digest and original certificate before publishing and updating the public feed and website version. Physical phone validation remains pending.
