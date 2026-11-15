# Administración de UrySoft

Acceso: /admin/ (también compatible /admin/comments/). Google Identity Services y verificación RS256 en servidor, correo administrador autorizado y caducidad; token únicamente en memoria. No se amplían los permisos de Google. No se exponen bases de datos ni secretos al navegador.

## Moderación
API autenticada /api/admin/*. Búsqueda por app, estado, tipo, texto y denuncias; paginación por ID de 25 en 25. Edición de alias, mensaje, tipo, valoración y respuesta; nota privada. Ediciones de nombre/mensaje público señaladas como editadas por UrySoft. Aprobación, ocultación, rechazo, papelera y restauración a pendientes. Guardado con versión optimista para evitar sobrescribir cambios de otra pestaña.

Auditoría atómica antes/después. El original se consulta independientemente de las últimas 100 entradas de historial. La eliminación permanente solo está disponible desde la papelera, exige escribir ELIMINAR y elimina comentario, denuncias e historial; no se ha ejecutado sobre datos reales durante el desarrollo. La anterior API PATCH /api/comments/admin responde 410 para impedir cambios sin historial.

Denuncias en /api/comments/report: solo mensajes publicados, motivos tipados, detalles hasta 1000 caracteres, origen propio, límite temporal de cinco por hora. El motivo y los detalles nunca se publican. Resolver/descartar/reabrir no cambia la visibilidad del comentario. El panel abre la ficha original y permite moderar desde el propio editor de la denuncia.

## Estadísticas
Contadores propios agregados en COMMENTS_DB, sin cookies de seguimiento ni identificadores persistentes de visitantes. Se excluyen admin, compra, API, robots reconocibles y señales DNT/GPC. Página, entradas estimadas, clics, clics de descarga y páginas visibles 30 segundos. País de request.cf.country, dominio de referencia, dispositivo/navegador categorizados, idioma seleccionado y fuente de campaña permitida. No IP en estadísticas, consultas privadas, texto de formularios, huellas digitales ni usuarios únicos. Seudónimos temporales con clave para límites antiabuso, aislados de estadísticas; deduplicación por evento aleatorio 24 horas. Limpieza al recibir nuevos eventos; agregados hasta 400 días. Fechas UTC. Los bloqueadores, referencias ocultas y robots no reconocidos limitan la exactitud. Clic de APK no prueba descarga completada ni instalación.

Filtro por fechas (hasta 400 días), página y país. CSV seguro frente a fórmulas. Clasificaciones de hasta 50 filas por dimensión, serie diaria, resumen y recomendaciones basadas únicamente en datos recogidos. La medición empieza con la activación, sin estadísticas inventadas ni importación de solicitudes técnicas de Cloudflare como visitas humanas.

## SEO
robots.txt y sitemap.xml con portada, cinco apps, ayuda y Calaix. Canonical y vistas previas Open Graph. Informe técnico generado a partir de HTML publicado en server/seo-report.js. Las consultas de búsqueda, impresiones y posiciones requieren Search Console: se enlaza su panel oficial, no se simula integración ni datos.

## Publicación y pruebas
Aplicar migrations/0005-administration.sql solo a COMMENTS_DB, después de 0001-comments.sql. Migración aditiva (tres columnas y cinco tablas); no aplicar a bases Premium. En producción se verificó 1 comentario conservado, 3 columnas y 5 tablas nuevas. No se modificaron suscripciones ni pagos.

node tests/admin.test.mjs y node tests/comments.test.mjs. Pruebas con SQLite real y claves Google ficticias: autorización, origen, validación, historial, versiones, privacidad, denuncias, estados, paginación, agregación, deduplicación, exclusiones y SEO. Vista local aislada con datos ficticios en .work/admin-preview.mjs; nunca se despliega ni se copia esa base a producción.
