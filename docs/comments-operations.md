# Comentarios y fichas de aplicaciones

La primera ficha completa es `/apps/lumen/`. Conserva ES/CA/EN, el tema elegido y la descarga oficial 4.0.2. Los iconos y capturas proceden de la copia local de recuperación de Lumen 4.0.1. Las capturas se identifican como 3.7–3.9 para no presentarlas como imágenes actuales de 4.0.2.

## Administración

Acceso: `/admin/comments/`. Inicio de sesión con Google mediante Google Identity Services. El servidor verifica la firma RS256 con las claves públicas de Google, el emisor, el destinatario, la caducidad y el correo verificado autorizado. El token se conserva solo en memoria y deja de aceptarse al caducar. Salir elimina la sesión local. La clave manual anterior ya no se acepta.

Los estados son pendiente, publicado y oculto. Todos los envíos entran pendientes. Guardar una respuesta con el estado pendiente u oculto no la publica; el comentario y la respuesta se muestran únicamente en estado publicado. No se requieren cuentas de GitHub. No se recogen correos ni se envían avisos: el visitante debe volver a la ficha para ver la respuesta publicada.

La lista de administración muestra los 200 mensajes más recientes y la pública los 100 publicados más recientes. Esta primera versión no incluye paginación ni borrado desde el panel. Los mensajes se conservan en D1 hasta que el administrador los elimine desde Cloudflare. La aprobación modera la visibilidad, no verifica la identidad del alias ni que el autor haya instalado la app.

## Infraestructura

- D1 `urysoft-comments`, jurisdicción UE; binding de producción `COMMENTS_DB`.
- Inicializar con `migrations/0001-comments.sql`.
- Variables de producción `GOOGLE_CLIENT_ID` y `COMMENTS_ADMIN_EMAIL`. El correo autorizado es oriolba@gmail.com.
- `COMMENTS_ADMIN_KEY` se conserva exclusivamente como secreto interno para los hashes de límites de envío; ya no permite entrar al panel.
- Pages Functions atiende únicamente `/api/comments` y subrutas.
- Límite de cinco envíos por hora y dirección de origen, mediante identificador hash con secreto y ventana horaria. No se almacena la dirección IP en las tablas. Los identificadores de límite caducan tras dos horas y se limpian en el siguiente envío aceptado.
- Las operaciones de escritura requieren mismo origen; no se habilita CORS. Todo el contenido público y administrativo se presenta como texto, sin interpretar HTML de los usuarios.
- Honeypot y límite básico de frecuencia; no se ha integrado CAPTCHA. Se recomienda revisar el volumen de abuso antes de ampliar el formulario.

## Verificación

Ejecutar `node tests/comments.test.mjs` para verificar esquema, validación, autenticación, mensajes pendientes privados, aprobación, respuesta, ocultación y límite de frecuencia. Las comprobaciones locales usan SQLite real y un adaptador de las operaciones D1.

Las otras aplicaciones conservan sus fichas existentes hasta preparar sus contenidos y recursos reales. El backend ya admite identificadores lumen, nexomesh, foldery, widgery y minairo.
