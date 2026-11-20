# Storagey Premium

Esta integración reutiliza la suscripción, recuperación con Google, verificación del pago y documentación de Foldery. Storagey tiene su propio plan anual de 6 EUR y licencia. El equivalente mensual mostrado es de 0,50 EUR. El cobro se renueva automáticamente y se puede cancelar en PayPal.

Para integrar el checkout en Cloudflare Pages, incorporar las carpetas `functions`, `server`, `storagey` y `legal` en las ubicaciones equivalentes del repositorio UrySoft. No se incluye la futura ficha pública de la aplicación.

Aplicar, en orden, las tres migraciones de `migrations` a una base D1 exclusiva de Storagey. Configurar:

- `STORAGEY_LIVE_DB`
- `STORAGEY_LIVE_PAYPAL_CLIENT_ID`, `STORAGEY_LIVE_PAYPAL_CLIENT_SECRET`
- `STORAGEY_LIVE_PAYPAL_PLAN_ID`, `STORAGEY_LIVE_PAYPAL_WEBHOOK_ID`
- `STORAGEY_LIVE_GOOGLE_CLIENT_ID`, con origen `https://urysoft.pages.dev` y retorno `https://urysoft.pages.dev/api/storagey/live/google/redirect`
- `STORAGEY_LIVE_LICENSE_PRIVATE_KEY`, clave RSA PKCS#8 en base64 correspondiente a la clave pública del instalador
- `STORAGEY_LIVE_SELLER_READY=yes`, `STORAGEY_LIVE_GOOGLE_PUBLIC=yes`, `STORAGEY_LIVE_PAYMENTS=enabled` cuando estén configuradas y verificadas las integraciones

Guardar los secretos en la configuración del servidor. Si se cambia la clave privada de licencias, actualizar `PremiumPublicKey.java` y volver a firmar el instalador con su certificado de actualización habitual.

El webhook es `https://urysoft.pages.dev/api/storagey/live/webhook`. El estado público de disponibilidad es `https://urysoft.pages.dev/api/storagey/live/status`.

Ejecutar las pruebas locales desde esta carpeta:

```sh
node tests/storagey-live-profile.test.mjs
node tests/storagey-entitlement.test.mjs
```

## Canal de actualización

Publicar `updates/storagey.json` en la raíz del sitio. El manifiesto incluido indica que aún no hay una descarga pública. Para cada release, subir primero el instalador firmado al repositorio UrySoft/urysoft con el tag `storagey-X.Y.Z` y el nombre `Storagey-X.Y.Z.apk`. Una vez comprobada su descarga, actualizar el manifiesto a `published` con `versionCode`, `versionName`, `downloadUrl`, `sha256` y `changelog` en es, ca y en, con el mismo formato que Foldery.
