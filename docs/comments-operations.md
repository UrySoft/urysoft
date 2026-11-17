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


## Evolución y control operativo (17/11/2026)

Estadísticas: cinco series simultáneas seleccionables (páginas, entradas, clics de descarga, actividad de 30 segundos y otros clics), agrupación por día/semana/mes, tabla accesible y consulta con flechas del teclado. Fechas rápidas de 7/30/90/180/365 días y rango manual de hasta 400 días. Comparación con el periodo inmediatamente anterior de igual duración, conservando los filtros de página y país. Si falta histórico, no se calcula un porcentaje. Los días sin medición son nulos y los días medidos sin eventos son cero. El día actual es parcial y no se extrapola. Los grupos semanales comienzan el lunes, en UTC; primer y último grupo pueden ser parciales. CSV incluye las cinco series diarias, cobertura, filtros aplicados y totales anteriores. Minairó se incorpora a los clics de descarga desde esta publicación, sin rellenar el pasado.

Servicios y versiones: consultas autenticadas y de solo lectura. Se comprueba acceso a los contadores y, por aplicación, se reutiliza el perfil aislado de producción para consultar estados de premium_checkouts y vigencias registradas. Fallos de una base se muestran sin impedir las demás comprobaciones. No se envían secretos ni datos de compradores al navegador. Es una instantánea de datos locales, no conciliación financiera, monitorización continua ni confirmación de ingresos. Los manifiestos públicos de /updates/ proporcionan las versiones y enlaces a instaladores; no se comprueba la firma ni la descarga completa. Accesos a los proveedores para despliegues, recuperación y suscripciones.

Historial global: últimas acciones de moderación y denuncias paginadas de 25 en 25; solo metadatos y acceso al editor existente para ver antes/después. Usa las tablas existentes, sin migraciones ni cambios en pagos.

Validación: node tests/admin.test.mjs; node tests/admin-evolution.test.mjs; node tests/comments.test.mjs; node tests/live-profile.test.mjs. Revisión visual aislada con datos ficticios de 30 días: gráfica, selección de series, agrupación semanal, servicios, versiones e historial. No se inicia sesión en producción durante esta prueba.

Incidencia previa observada: tests/foldery-live-profile.test.mjs espera FOLDERY_LIVE_TAX_REVIEWED en liveProfile, pero el archivo original de producción ya no lo comprueba. No se modifica ese perfil en esta actualización del panel. La prueba específica anterior falla por esa discrepancia; las pruebas de estadísticas y administración pasan.
