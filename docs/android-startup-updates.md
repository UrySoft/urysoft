# Comprobación al abrir las aplicaciones Android UrySoft

Regla solicitada por Oriol: consultar siempre si hay versiones nuevas al iniciar o volver a abrir una aplicación. La consulta debe ser asíncrona, no bloquear el inicio y no depender de un límite de 24 horas ni de una fecha guardada de éxito. Mientras exista una consulta en curso se reutiliza esa comprobación, evitando duplicar peticiones simultáneas.

Si no hay red o la consulta falla, la aplicación sigue funcionando y el fallo automático es silencioso. Al abrir de nuevo se reintenta. Se conserva la búsqueda manual. Una versión nueva se compara por versionCode y se ofrece al usuario para que decida descargar e instalar; no se desinstala ni se instala automáticamente.

Implementado y publicado en NexoMesh 0.11.3 (19), Lumen 4.0.3 (40003), Foldery 1.6.1 (24) y Widgery 3.1.1 (60), con la firma anterior de cada aplicación. Pruebas contra el comprobador real con transporte y ciclo de vida simulados: reapertura inmediata tras éxito, falta de red, reintento inmediato, concurrencia, salida a segundo plano, aviso de versión nueva, búsqueda manual y nueva instancia de actividad. No se han utilizado teléfonos físicos para estas pruebas.

Minairó 6.34.3 (75) incorpora el mismo comprobador al abrir y un botón manual en Ajustes, con entrega privada y firma original. Se mantiene su ficha pública como próximamente: el feed devuelve unpublished hasta que se autorice una distribución pública.

Las fuentes utilizadas son las de las versiones publicadas Foldery 1.6.0 y Widgery 3.1.0, y el proyecto actual Minairó 6.34.2. No se han reconstruido versiones anteriores ni se han simplificado funciones. Las firmas de Foldery y Widgery se han contrastado con los instaladores publicados; la de Minairó, con su almacén original privado.
