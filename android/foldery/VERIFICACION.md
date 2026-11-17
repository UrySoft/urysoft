# Versión 1.5.7

Doce formas con selector visual: originales y triángulo, octógono, estrella, cuadrado suavizado, escudo y corazón. Ocho acabados: originales, monocromo, tintado, marco, sepia, vivo, suave y negativo.

Fondos del dispositivo mediante selector de documentos, copia privada con lectura limitada a 20 MB y resolución reducida, orientación normalizada. Cinco ajustes: recortar, encajar, estirar, mosaico proporcional y centrar. Imagen y modo guardados por widget, mismos dibujos en vista previa y escritorio. Las imágenes privadas se conservan para no romper referencias de otros widgets.

Selector de apps y contactos con tarjetas y columnas automáticas según anchura y escala del texto. Se conserva tamaño mínimo 1x1.

Compilación completa y firma verificadas; 1.551.815 comprobaciones de lógica, 22 de almacenamiento con dobles en memoria y 3.717 de traducciones superadas. Los ensayos de lógica no verifican los efectos visuales nuevos. Sin pruebas visuales de imágenes, formas o rejilla en teléfono/emulador. Versión 1.5.7, código 18.

---

# Versión 1.5.5

Añadir, Editar y Eliminar en una sola fila de tres columnas iguales, separadas por seis unidades. Eliminado el texto explicativo bajo Añadir. Iconos, descripciones accesibles y confirmación de borrado conservados. Compilación y comprobaciones de traducciones superadas; sin validación visual en teléfono.

# Versión 1.5.4

Paneles cerrados inicialmente en cada apertura. Botones globales de 48 unidades de altura mínima, texto de 14, iconos vectoriales y menor relleno. Guardar y Añadir en una sola fila del editor. Etiquetas abreviadas en los tres idiomas. Compilación y 3.452 comprobaciones de recursos superadas. Sin validación visual en dispositivo.

# Actualización 1.5.3: botón Añadir al escritorio

- Botón principal Añadir al escritorio en cada tarjeta de Mis widgets, antes de Personalizar y Eliminar.
- Descripción accesible con el nombre y texto visible que explica que se crea otra instancia independiente.
- Botón adicional en el pie del editor de widgets existentes. Utiliza los ajustes actuales como copia sin sobrescribir el widget de origen. La creación inicial conserva su botón principal.
- Flujo de colocación compartido en WidgetPin: copia de la configuración, validación de accesos, borrador independiente por solicitud, vista previa y confirmación mediante el escritorio de Android.
- Solo el resultado confirmado por Android guarda la configuración bajo el nuevo identificador. Cancelar o rechazar la solicitud no cambia el widget original.
- Si el escritorio no admite la solicitud directa, se conservan los ajustes y se muestran las instrucciones ya existentes para añadir desde el selector del escritorio.
- Mensajes y descripción nuevos en català, español e inglés.
- Compilación completa, 1.551.815 comprobaciones de lógica y 22 pruebas del almacenamiento de producción con dobles en memoria superadas.
- 3.446 comprobaciones de recursos; 242 textos y plurales completos en los tres idiomas y presentes en el instalador.
- Firma y metadatos verificados: 1.5.3, código 14; mismo certificado que 1.5.2.
- No se ha probado esta actualización con un escritorio Android real ni se ha ejecutado la batería histórica de interfaz. La aparición de la confirmación de colocación queda pendiente de validación en teléfono.

---

# Actualización 1.5.2: borrar un widget previa confirmación

- Botón Eliminar widget en cada tarjeta de Mis widgets, con texto de acción diferenciado.
- Diálogo de confirmación con el nombre elegido, Cancelar y Eliminar; explica la eliminación definitiva de la configuración y la retirada manual del recuadro del escritorio.
- Solo la acción positiva borra la configuración seleccionada. El resto de widgets, los borradores de colocación y las plantillas se conservan.
- Configuración borrada y marca de eliminación guardadas en la misma edición de preferencias. La tarjeta desaparece y deja de participar en el seguimiento de notificaciones.
- El widget colocado se sustituye por un aviso sin accesos ni botones de lanzamiento. La aplicación proveedora no posee el escritorio: la retirada física del recuadro se realiza manteniéndolo pulsado desde el escritorio.
- Las actualizaciones de idioma, reinicio o notificaciones mantienen el estado eliminado. Los accesos antiguos y los editores abiertos no pueden volver a activar el widget borrado; el servicio de colecciones antiguo devuelve cero accesos.
- Al retirar realmente el widget del escritorio se limpia también su marca, permitiendo reutilizar correctamente el identificador si Android lo asigna de nuevo. La restauración migra los estados eliminados.
- 22 pruebas ejecutadas sobre ConfigStore de producción con dobles de Context, SharedPreferences y AppWidgetManager en memoria: aislamiento, persistencia entre contextos, marcas, rechazo de identificadores ajenos, escrituras antiguas y seguimiento de avisos. No son pruebas de la interfaz Android.
- 3.413 comprobaciones de traducciones superadas; 240 textos y plurales en los tres idiomas.
- Compilación completa, 1.551.815 comprobaciones de lógica, firma y metadatos verificados: 1.5.2, código 13, mismo certificado que 1.5.1.
- Sin prueba del diálogo ni del escritorio real en dispositivo/emulador; queda pendiente validación visual en teléfono.

Referencia oficial de la gestión de identificadores por el host del widget: https://developer.android.com/reference/android/appwidget/AppWidgetHost

---

# Actualización 1.5.1: selector de idioma

- Nuevo apartado Idioma de la app en Opciones: Idioma del dispositivo (predeterminado), Català, Español, English.
- La opción predeterminada no establece ninguna preferencia propia de idioma; sigue el dispositivo con los recursos ingleses de respaldo.
- Android 13 y posteriores: preferencia nativa mediante LocaleManager, coherente con el selector de idioma de la app en ajustes de Android. Restablecer al dispositivo elimina los idiomas específicos.
- Android 8–12: elección persistida localmente y contextos localizados, sin cambiar el idioma del teléfono.
- Pantallas con contexto localizado y detección de cambios al volver a una pantalla abierta. El editor utiliza su mecanismo existente de guardado del borrador al recrearse.
- Renderizado del widget y sus celdas con el idioma elegido, tanto para colecciones modernas como para el servicio de Android 8–11. Actualización solicitada al elegir otro idioma.
- 1.551.815 comprobaciones de lógica superadas, incluidas normalización de idiomas regionales, selección predeterminada y correspondencia de las cuatro opciones.
- 3.310 comprobaciones de recursos superadas; 233 textos y plurales completos en los tres idiomas y presentes en el instalador.
- Compilación completa, firma y metadatos verificados: 1.5.1, código 12, mismo certificado que 1.5.0.
- No se han ejecutado pruebas de ciclo de vida/interfaz de esta actualización en emulador ni en dispositivo físico. La persistencia visual y la conservación del borrador requieren validación en dispositivo.

Documentación oficial: https://developer.android.com/reference/android/app/LocaleManager

---

# Actualización 1.5.0: català, español, English

- 230 recursos de texto y plurales de notificaciones completos en los tres idiomas. Incluye inicio, opciones, editor, ayudas, diálogos, mensajes, selectores, accesibilidad y widget.
- Inglés en los recursos predeterminados, català y español en sus variantes; Android resuelve el idioma según las preferencias del dispositivo y utiliza inglés cuando no hay coincidencia disponible.
- Idiomas admitidos declarados en la configuración de idiomas de Android. Sin selección propia que altere el idioma del dispositivo por defecto.
- Recepción del cambio de idioma del sistema para actualizar los widgets colocados.
- Nombres nuevos por defecto localizados; nombres guardados por el usuario, contactos y nombres de otras aplicaciones conservados como datos.
- Eliminados los textos españoles incrustados en la interfaz Java. Identificadores internos, claves de persistencia, enlaces y nombres de marca conservados.
- 3.267 comprobaciones de localización superadas: paridad de claves, plurales, argumentos de formato, referencias, textos XML y presencia efectiva de las tres variantes en el instalador compilado.
- Compilación completa, 1.551.805 comprobaciones de lógica, firma y metadatos verificados. Versión 1.5.0, código 11; mismo certificado que 1.4.4. Actualizada también la versión visible en Información.
- Sin pruebas en teléfono ni repetición de la batería histórica de interfaz. Las comprobaciones de recursos no equivalen a una validación visual de todas las pantallas.

Documentación oficial de referencia: https://developer.android.com/guide/topics/resources/localization

---

# Actualización 1.4.4: desplazamiento sin barra visible

- Barra de desplazamiento oculta en las 168 variantes de rejilla del widget; brillo de sobre desplazamiento desactivado.
- Eliminada la barra dibujada en la vista previa. Se conserva la lógica de desplazamiento táctil y de arrastre.
- Compilación completa, firma y metadatos verificados: versión 1.4.4, código 10; mismo certificado que 1.4.3.
- Atributos comprobados en las 168 variantes. Las 1.551.805 comprobaciones de lógica siguen pasando; no verifican la interacción visual.
- Sin validación en dispositivo físico ni repetición de la batería histórica de interfaz.

---

# Actualización 1.4.3: contadores y separación horizontal

- Contador pintado sobre el icono después de su sombra, enteramente dentro de sus límites: esquina superior derecha con borde blanco y tamaño acotado incluso en iconos pequeños. No sobresale hacia celdas contiguas ni hacia fuera del área visible.
- En columnas automáticas, el ancho sobrante se reparte entre los dos extremos del conjunto, no entre los iconos. El ancho de celda coincide con el icono. Separación horizontal cero produce límites contiguos; las formas redondeadas conservan naturalmente sus esquinas transparentes y las imágenes originales pueden contener sus propios márgenes.
- Vista previa y widget usan los mismos anchos e inserciones laterales. El tamaño elegido hasta 300 se conserva cuando cabe.
- Compilación completa, firma y metadatos verificados: versión 1.4.3, código 9; mismo certificado que 1.4.2.
- 1.551.805 comprobaciones de lógica superadas, incluidas las relaciones entre ancho, separación, tamaño automático y espacio sobrante.
- No se ha probado esta actualización en un teléfono ni se ha vuelto a ejecutar la batería histórica de pruebas de interfaz y píxeles. La superposición y el recorte visual requieren confirmación en dispositivo.

---

# Actualización 1.4.2: paneles plegables

- Todos los apartados del editor admiten colapsar y expandir tocando la cabecera.
- Flecha de estado, cabecera enfocable, interacción de teclado y acciones de accesibilidad Expandir/Colapsar.
- Preferencia de apertura guardada para cada apartado, conservada al cambiar de pestaña, recrear la pantalla o abrir de nuevo el editor.
- Los valores configurados permanecen en sus controles al plegar el apartado; la vista previa conserva su posición independiente.
- Compilación completa y firma verificadas, mismo certificado que 1.4.1. Versión 1.4.2, código 8.
- Las 241.405 comprobaciones de lógica siguen pasando; no comprueban la interacción visual de los paneles. Esta actualización no se ha probado en un teléfono ni se ha ejecutado de nuevo la batería histórica de interfaz.

---

# Verificación de la actualización 1.4.1

- Compilación completa de recursos y Java, generación del ejecutable y firma: correcta.
- 241.405 comprobaciones de lógica superadas, incluyendo tamaños automáticos 28–300, columnas manuales y restricciones de ancho.
- Firma verificada y mismo certificado que la versión 1.4.0; nombre de paquete conservado.
- Metadatos comprobados: versión 1.4.1, código 7, Android mínimo 26 y objetivo 36.
- Revisión del encaje: eliminado el margen artificial del 8 % en iconos normales con forma; escalado proporcional para cubrir la máscara sin añadir fondo blanco. El estilo con marco conserva su margen.
- Esta actualización no se ha probado en un teléfono ni se ha vuelto a ejecutar la batería anterior de pruebas de interfaz. Los resultados que siguen corresponden a la versión 1.4.0.

---

# Verificación de Foldery.2.AI · UrySoft · 1.4.0

Fecha: 30/10/2026. Actualización para Oriol, para instalación y evaluación personal de su aplicación.

## Resultado

- `UrySoft-Foldery.2.AI-1.4.0.apk` compilado, alineado y firmado; el archivo final se comprueba después de escribirlo.
- Identificador instalado `cat.urysoft.widgets`, versión 1.4.0 y código 6. Nombre visible Foldery.2.AI. Mismo certificado que la versión 1.3.1, para permitir instalar la actualización.
- **111 pruebas automatizadas pasadas**, sin fallos, errores ni omisiones.
- **149.054 aserciones numéricas** de tamaños, columnas, márgenes y contadores pasadas. Son recorridos de combinaciones; no equivalen a ese número de situaciones de uso independientes.
- Análisis estático: **0 errores y 24 advertencias**, sin desactivar el análisis global. Ejecución final `BUILD SUCCESSFUL`.

| Grupo | Casos pasados | Fallos |
| --- | --- | --- |
| ContactAndDirectionPixelsTest | 3 | 0 |
| ContactFeaturesTest | 16 | 0 |
| EditorFeaturesTest | 18 | 0 |
| FolderyFeaturesTest | 10 | 0 |
| NavigationAndContactFixesTest | 16 | 0 |
| PreviewPixelsTest | 3 | 0 |
| ResponsiveTitlesTest | 14 | 0 |
| ShadowPixelsTest | 4 | 0 |
| TitleAndSpacingPixelsTest | 8 | 0 |
| WidgetRuntimeTest | 16 | 0 |
| WidgetTapFixTest | 3 | 0 |

Los componentes se comprueban en las plataformas 26 y 34. Las comparaciones de píxeles y los toques sobre el adaptador real se ejecutan con motor gráfico nativo en la plataforma 34. Se incluye una configuración de densidad 420, además de la configuración de referencia de densidad 160.

## Títulos configurables y sombra compartida

En Iconos → Títulos se puede mostrar u ocultar el nombre, ajustar el tamaño entre 8 y 32 y elegir un color arbitrario de seis cifras. El tamaño respeta el escalado de fuentes del dispositivo. Se admiten hasta dos líneas con puntos suspensivos para nombres largos. Se reserva el espacio de dos líneas para alinear los accesos de cada fila.

El icono y el texto se componen antes de aplicar una única sombra a su silueta. Por tanto, el título comparte color, intensidad, nitidez, distancia y ángulo de sombra con el icono. Los contadores se pintan delante después, para conservar su legibilidad.

Se comprueban los píxeles del color elegido, el aumento de la altura de las letras al cambiar el tamaño, el desplazamiento del título al ajustar su separación y la invariabilidad de las coordenadas de la foto. La prueba de ocho direcciones verifica la sombra de las letras sin mover el texto original. También se comprueba que las sombras difusas extremas permanezcan dentro de su imagen.

El selector de color y los valores de texto y espacios se guardan y sobreviven a la recreación del editor por cambio de tema. Cambiar el color del título no vuelve opaco el fondo transparente.

## Márgenes y compacidad

Se configuran independientemente: separación icono–título, separación entre columnas, separación entre filas, margen lateral y margen superior e inferior del widget. Los espacios se guardan por widget.

La separación de filas no se añade después de la última. Con títulos desactivados y separación cero se pueden obtener filas de 30 unidades de altura en la configuración probada, sin imponer el mínimo antiguo de 48. La foto y el área de texto usan el mismo renderizador en vista previa y widget.

Las pruebas miden los márgenes aplicados a las vistas reales, su separación horizontal y el relleno entre filas. Las comparaciones gráficas verifican las coordenadas de foto y título entre el widget y su vista previa, incluidos nombres del widget de una o dos líneas. Se revisan los controles en temas claro y oscuro.

## Iconos al ancho máximo de sus columnas

El cálculo toma el ancho útil del widget, descuenta márgenes y separaciones, y utiliza el ancho real de la columna. La cantidad manual se mantiene mientras quepan las columnas; si no caben, se reduce para conservar un ancho mínimo práctico. En modo automático se tiene en cuenta el ancho y el número de accesos, con un máximo de ocho columnas.

El tamaño ya no depende del antiguo selector fijo ni del interruptor de expansión. Puede superar 160. La altura del widget, el tamaño del texto, la separación vertical y la sombra no reducen el ancho del icono; las filas adicionales se desplazan.

Se comprueban ejemplos de 104, 162, 180, 232 y 336 de ancho y la independencia respecto a la altura. El recorrido numérico verifica que las columnas ocupen el ancho útil descontando exactamente su separación, a través de distintos anchos y márgenes.

Los diseños compilados cubren ocho columnas y 21 separaciones horizontales para mantener la compatibilidad con Android 8. Una única celda adaptable sustituye los diseños de tamaño fijo. El ajuste se hace a los píxeles físicos de la cuadrícula; la prueba con densidad 420 verifica dos, tres y ocho columnas, incluida la foto en la última columna.

## Regresiones y apertura

Siguen pasando las pruebas de fondo transparente, formas y acabados, sombras, temas, borradores, vista previa fija, reordenación, eliminación y deshacer.

También pasan las pruebas de importación de contactos y fotos directas, permisos de fotos concedidos o denegados, recuperación de imágenes, persistencia, selección cancelada y referencias de contacto actualizadas. Se utiliza un proveedor sintético que rechaza consultas al conjunto de la agenda y escrituras.

El widget se aloja en una ventana activa y se envían eventos táctiles a su adaptador real. La app seleccionada se abre mediante su componente y el contacto abre su ficha. Se conserva la comprobación de que el acceso pertenezca al widget pulsado. No se realiza una llamada automáticamente.

La memoria total de las imágenes de 48 accesos con texto grande se comprueba frente al presupuesto del widget. Las imágenes pueden reducir su resolución interna para respetarlo, manteniendo su área de presentación.

## Advertencias del análisis estático

| Identificador | Cantidad |
| --- | --- |
| ApplySharedPref | 3 |
| CustomSplashScreen | 1 |
| SetTextI18n | 15 |
| UnusedAttribute | 5 |

Las advertencias corresponden a preferencias compartidas, atributos utilizados según versión, la actividad de apertura y textos compuestos en castellano. No se han suprimido globalmente. La exclusión local del método antiguo de navegación atrás conserva su justificación para las versiones anteriores a la plataforma 33.

## Alcance y límites

No se ha conectado un teléfono físico ni se ha ejecutado un emulador de sistema completo. Queda verificar en el teléfono de Oriol la actualización de su instalación y la integración con su escritorio concreto, sus contactos y sus ajustes de fuentes.

Las imágenes de `verification/previews/` son renders de componentes con datos sintéticos. No son capturas de un teléfono de Oriol. Se han inspeccionado el editor, títulos, márgenes y la comparación del widget con la vista previa. Las pruebas no constituyen una certificación de accesibilidad.

Los márgenes exteriores muy pequeños pueden recortar sombras amplias en el borde físico del widget. Una altura reducida muestra una porción del contenido y permite desplazarlo; conviene redimensionar el widget si se desea ver filas completas.

## Acrónimos

| Acrónimo | Descripción | Contexto |
| --- | --- | --- |
| APK | Paquete de aplicación Android | Archivo de instalación verificado. |
| AI | Artificial Intelligence, inteligencia artificial | Nombre comercial indicado por el propietario. |

## Referencias

- Requisitos e identidad: instrucciones de Oriol en esta conversación.
- `app/src/main`: código y recursos entregados. El modelo está en WidgetConfig; el cálculo de columnas en LayoutPolicy y WidgetRenderer; texto y sombra en CellRenderer y ShadowRenderer; vista previa en PreviewView.
- `app/src/test` y `tests/CoreTests.java`: pruebas ejecutadas.
- `verification/TEST-*.xml`: resultados de las 111 pruebas.
- `verification/lint-results-debug.xml` y `verification/gradle-checks.log`: análisis estático y ejecución final.
- `verification/native-build.log`, `package-metadata.txt`, `update-signature.txt` y `SHA256.txt`: compilación, nombre, versión e integridad del archivo entregado.
- [Vistas remotas de Android](https://developer.android.com/reference/android/widget/RemoteViews).
- [Texto con varias líneas](https://developer.android.com/reference/android/text/StaticLayout.Builder).
- [Medición de imágenes](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/ImageView.java).
- `README.md`: instalación, uso, compilación y demás referencias.
