# Acceso de cortesía en Google Play

El equipo de revisión puede abrir las funciones Premium con un código de su aplicación. La instalación recibe una licencia firmada válida durante 24 horas. La app la renueva mientras el código siga activo. La invitación original sirve también para una segunda instalación.

| Aplicación | Secreto de configuración en producción | Identificador del producto de Google Play |
| --- | --- | --- |
| Foldery | `FOLDERY_PLAY_REVIEW_SHA256` | `foldery_premium` |
| Widgery | `WIDGERY_PLAY_REVIEW_SHA256` | `widgery_premium` |
| TeclaIRC | `TECLAIRC_PLAY_REVIEW_SHA256` | `premium` |
| Storagey | `STORAGEY_PLAY_REVIEW_SHA256` | `premium` |
| Minairó | `MINAIRO_PLAY_REVIEW_SHA256` | `premium` |

Genera para cada aplicación 32 bytes aleatorios, codificados en base64url sin relleno. Conserva el código en el gestor de credenciales de UrySoft. Configura como secreto en Cloudflare el resumen SHA-256 hexadecimal de los 43 caracteres del código, sin salto de línea. Cada aplicación usa sus enlaces existentes a su base de datos y a su clave de firma de licencias en producción. El servidor limita los intentos por dirección de red y nunca necesita guardar la invitación de texto en el dispositivo.

Comprueba la activación desde dos instalaciones limpias de la versión que se enviará. Abre Premium, entra en las opciones y selecciona **Acceso de cortesía**. Pega el código y comprueba una función que normalmente requiera la suscripción. Entrega las instrucciones de cada aplicación y su código únicamente en la declaración privada de acceso de Play Console. Los servicios externos que requieran una cuenta de proveedor necesitan sus propias instrucciones y una prueba independiente.

Para revocar el acceso, cambia el resumen por el de una invitación nueva y actualiza los datos privados de revisión en Google Play. La siguiente renovación de la instalación comprobará el cambio. Una licencia en un dispositivo sin conexión caduca en un máximo de 24 horas desde su emisión.

## Instrucciones en inglés para el formulario privado de Google Play

Utiliza una entrada por aplicación y añade el código operativo únicamente al formulario privado una vez validado en producción. El texto siguiente sirve de base para las cinco entradas. Completa la comprobación de una función de cada aplicación con la compilación que vaya a revisión.

> Open the app's Premium screen, open its options and choose the option to enter a courtesy access code. Paste the code supplied in this private Play Console entry and confirm. Return to the app to use Premium features. You can use the same code on another clean installation of this app. An internet connection is required to activate and renew access. You can test the subscription features without starting a purchase or entering payment details.

| Aplicación | Comprobación para añadir a sus instrucciones tras probarla en Android |
| --- | --- |
| Foldery | Create a second folder widget and add it to the home screen. |
| Widgery | Open the Premium screen again, confirm that the subscription features are unlocked and configure a web widget. |
| TeclaIRC | Enable chat thumbnails, open a channel with a shared image link, then open its gallery and tap the thumbnail. |
| Storagey | Reopen the Premium screen and confirm the active entitlement before testing the file manager with sample files. |
| Minairó | Open an action that generates an image and verify that its watermark option can be turned off. |

Si una función requiere iniciar sesión en un proveedor externo, añade sus instrucciones y un acceso de prueba autorizado para ese proveedor en la entrada privada. El código de UrySoft activa la licencia de la aplicación. Usa datos de muestra para la revisión. [Requisitos de acceso de Google Play](https://support.google.com/googleplay/android-developer/answer/15748846)
