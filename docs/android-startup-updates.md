# Comprobación al abrir las aplicaciones Android UrySoft

Regla solicitada por Oriol: consultar siempre si hay versiones nuevas al iniciar o volver a abrir una aplicación. La consulta debe ser asíncrona, no bloquear el inicio y no depender de un límite de 24 horas ni de una fecha guardada de éxito. Mientras exista una consulta en curso se reutiliza esa comprobación, evitando duplicar peticiones simultáneas.

Si no hay red o la consulta falla, la aplicación sigue funcionando y el fallo automático es silencioso. Al abrir de nuevo se reintenta. Se conserva la búsqueda manual. Una versión nueva se compara por versionCode y se ofrece al usuario para que decida descargar e instalar; no se desinstala ni se instala automáticamente.

Implementado y publicado en NexoMesh 0.11.3 (19) y Lumen 4.0.3 (40003), con la firma anterior de cada aplicación. Pruebas contra el comprobador real con transporte y ciclo de vida simulados: reapertura inmediata tras éxito, falta de red, reintento inmediato, concurrencia, salida a segundo plano, aviso de versión nueva, búsqueda manual y nueva instancia de actividad. No se han utilizado teléfonos físicos para estas pruebas.

El alcance restante requiere aplicar y verificar el mismo comportamiento sobre las fuentes actuales de Foldery, Widgery y Minairó. No se han reconstruido ni reemplazado sus versiones con proyectos antiguos.
