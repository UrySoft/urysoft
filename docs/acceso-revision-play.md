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
