# Foldery.2.AI 1.5.8

Actualización: consulta automática diaria y botón Buscar actualizaciones en Opciones. Ver ACTUALIZACIONES-1.5.8.md. El resto de este archivo documenta la base 1.5.7.

## Actualización 1.5.3 — Añadir al escritorio

Cada tarjeta de Mis widgets incluye ahora el botón Añadir al escritorio. También aparece en el editor de widgets existentes. Solicita a Android añadir otra instancia independiente con los ajustes actuales; confirma la colocación en el diálogo del escritorio. El widget de origen se conserva.

Si tu escritorio no admite el botón, se muestran instrucciones para añadir desde su selector de widgets, conservando los ajustes.

## Actualización 1.5.2 — eliminar widgets

En Mis widgets pulsa Eliminar widget y confirma el nombre en el diálogo. Cancelar conserva la configuración. Al eliminar, se borra su configuración y desaparece de la lista; los demás widgets se conservan.

Si estaba colocado en el escritorio, se muestra un aviso sin accesos. Mantén pulsado el recuadro desde el escritorio y elige quitarlo para retirar también el espacio ocupado, cuya gestión corresponde al escritorio de Android.

Pruebas de almacenamiento: `python3 tests/verify_widget_store.py` (dobles en memoria, sin interfaz de Android).

## Actualización 1.5.1 — selector de idioma

Abre Opciones → Idioma de la app. Puedes elegir Idioma del dispositivo (predeterminado), Català, Español o English. La elección también se aplica a los widgets; el modo dispositivo usa inglés como respaldo cuando no hay un idioma compatible.

## Actualización 1.5.0 — català · español · English

La interfaz, las ayudas, los mensajes, la accesibilidad y los widgets están disponibles en los tres idiomas. Se utilizan las preferencias de idioma del dispositivo; inglés es el idioma de respaldo. Los nombres que hayas guardado para tus widgets se conservan.

Verificación de recursos: `python3 tests/verify_locales.py`; opcionalmente añade `--aapt2 /ruta/aapt2 --apk /ruta/instalador.apk` para comprobar las traducciones compiladas.

## Actualización 1.4.4

Barra de desplazamiento oculta en el widget y en su vista previa. Los iconos se siguen desplazando mediante el gesto táctil.

## Actualización 1.4.3

Contadores superpuestos dentro de la esquina superior derecha del icono para evitar recortes. Separación horizontal sin espacio oculto: cero deja contiguos los límites de los iconos, también en modo automático. El ancho sobrante se centra en los extremos del grupo.

## Actualización 1.4.2

Toca la cabecera de cualquier apartado de configuración para plegarlo o desplegarlo. Cada apartado recuerda su estado. La flecha indica si está abierto o cerrado.

## Actualización 1.4.1

Sin margen interior artificial al aplicar formas a iconos normales: relleno centrado, conservando proporciones y recortando el excedente. El estilo «Con marco» conserva su separación deliberada.

Columnas automáticas: tamaño seleccionable entre 28 y 300 unidades independientes de densidad; se reduce solo si no cabe. Las columnas manuales siguen aprovechando todo el ancho.

# Foldery.2.AI · UrySoft · 1.4.0

Aplicación Android de widgets de aplicaciones y contactos. Esta actualización es para Oriol, para instalación y evaluación personal. Mantiene el identificador instalado y la clave de firma de las versiones anteriores.

## Instalar o actualizar

1. Abre `UrySoft-Foldery.2.AI-1.4.0.apk` en Android e instala sobre la versión anterior.
2. Si no hay widgets colocados, se abre el creador automáticamente. Si ya hay espacios, puedes personalizarlos desde el inicio.
3. En Apps, elige aplicaciones o añade contactos. Configura el aspecto y pulsa Añadir al escritorio. Confirma la colocación en Android.
4. Para redimensionar, mantén pulsado el widget en el escritorio. Para editarlo, usa Editar o la aplicación.

Si el escritorio no permite colocarlo desde la app, utiliza su selector de widgets y busca Foldery.2.AI. El editor recupera la configuración preparada.

## Títulos

En Iconos → Títulos puedes mostrar u ocultar los nombres, elegir un tamaño de 8 a 32 y seleccionar cualquier color de seis cifras, por ejemplo `#28B8AE`. También hay accesos para blanco y oscuro.

El tamaño de texto respeta el escalado de fuentes del dispositivo. Los nombres se centran y pueden ocupar hasta dos líneas, con puntos suspensivos si hace falta. Al ocultarlos se elimina su altura y su separación respecto al icono.

Los títulos reciben automáticamente la misma sombra que el icono: color, intensidad, nitidez, distancia y dirección. No necesitan un segundo ajuste de sombra. La sombra se pinta detrás del conjunto y no cambia su geometría.

## Márgenes y separación

En Iconos → Márgenes y separación:

| Ajuste | Intervalo | Efecto |
| --- | --- | --- |
| Icono y título | 0–32 | Distancia vertical entre la imagen y el área de texto. |
| Separación horizontal | 0–40, pasos de 2 | Espacio entre columnas. |
| Separación entre filas | 0–40, pasos de 2 | Espacio entre filas; no se añade después de la última. |
| Margen lateral del widget | 0–40 | Espacio exterior a izquierda y derecha. |
| Margen superior e inferior | 0–40 | Espacio exterior arriba y abajo. |

Los valores de espacio son unidades independientes de la densidad. Los ajustes se guardan por widget y se muestran en la vista previa. Restablecer espaciado recupera 6 entre icono y título y 12 para los demás valores.

Una sombra amplia puede llegar al borde del widget si eliges márgenes muy pequeños. Ampliar ese margen deja más sitio para dibujarla.

## Tamaño máximo por columna

El tamaño del icono se calcula a partir del ancho disponible, los márgenes y las columnas. El antiguo tamaño fijo y el interruptor de expansión dejan de controlar el resultado.

Con columnas manuales, se conserva la cantidad elegida mientras quepan iconos de al menos 28. Si no caben, se reduce el número de columnas. En modo automático se busca un tamaño de referencia de 64, con un máximo de ocho columnas y sin crear más columnas que accesos.

Para `n` columnas:

`ancho del icono = (ancho del widget − 2 × margen lateral − (n − 1) × separación horizontal) / n`

El cálculo se ajusta al número entero de píxeles que cabe en la columna real, sin redondear hacia arriba. No se limita a 160: puede crecer por encima de ese tamaño si el ancho lo permite. El tamaño de las imágenes generadas se limita según la memoria disponible, conservando su área de presentación.

La altura del widget y el tamaño del título no reducen el icono. Cuando las filas no caben, se desplazan. El margen vertical y los títulos afectan a la altura del contenido, no al ancho del icono. Al redimensionar el widget se recalculan las columnas y sus imágenes.

## Editor y navegación

| Lugar | Contenido |
| --- | --- |
| Apps | Selección con búsqueda, añadir contactos, actualizar fotos y nombre del widget. |
| Iconos | Columnas, tamaño automático, títulos, márgenes, formas, acabados y sombra. |
| Fondo | Color, transparencia, opacidad y redondeado. |
| Opciones del widget | Nombre visible, acceso de edición y contadores para este widget. |
| Barra superior | Ayuda y Opciones de la app. |
| Opciones de la app | Tema claro, oscuro o del dispositivo; acceso a notificaciones e información de UrySoft. |

La vista previa utiliza el mismo renderizador de imágenes y títulos que el widget. Puedes arrastrar para ordenar, tocar la cruz para quitar y usar Deshacer. Hay acciones equivalentes para lector de pantalla. Las filas adicionales se desplazan. Si el panel de vista previa ocupa menos de la mitad del área disponible, se mantiene fuera del desplazamiento de los ajustes.

## Contactos y apertura

Los contactos usan su foto como icono, o iniciales si no hay una imagen accesible. Apps → Actualizar fotos vuelve a consultar los contactos que has añadido. Android puede pedir opcionalmente permiso de lectura para acceder a las fotos. Este permiso permite técnicamente leer la agenda, aunque el código solo consulta las fichas seleccionadas.

Una app se abre al tocar su icono en el widget. Un contacto abre su ficha en la agenda para que puedas llamarle; no se llama automáticamente. La apertura comprueba que el acceso pertenece al widget pulsado. Quitar un icono no elimina la app ni el contacto del teléfono.

Se conserva una instantánea privada de los contactos elegidos. Para actualizar nombre o foto, vuelve a seleccionarlos o usa Actualizar fotos. La app no enumera ni sincroniza continuamente toda la agenda.

## Identidad y privacidad

| Elemento | Valor |
| --- | --- |
| App | Foldery.2.AI |
| Marca | UrySoft |
| Director ejecutivo | Oriol Badia Campanera |
| Perfil vinculado al nombre | https://www.linkedin.com/in/oriol-badia |
| Versión y código | 1.4.0; 6 |
| Identificador instalado | cat.urysoft.widgets |
| Compatibilidad declarada | Android 8 o posterior; objetivo 36 |

La marca y el cargo proceden de las instrucciones de su propietario. El nombre enlazado aparece en el inicio y en Acerca de. LinkedIn se abre en una aplicación externa.

No hay cuenta, anuncios ni permiso de Internet. Tampoco se solicita permiso para escribir contactos o realizar llamadas. Los contadores son opcionales y requieren acceso a notificaciones. Se cuentan avisos activos, sin guardar el texto y sin duplicar resúmenes de grupo; no equivalen necesariamente a mensajes sin leer.

## Compilar y verificar

Abre esta carpeta en Android Studio o utiliza el contenedor de Gradle incluido, con Java 17, plataforma Android 36 y herramientas 36.0.0.

```bash
./gradlew testDebugUnitTest lintDebug assembleRelease
```

Alternativa sin dependencias adicionales de interfaz:

```bash
python3 build_local.py --sdk /ruta/android-sdk
```

La carpeta signing contiene la identidad de firma original y sus propiedades. Debe conservarse privada para poder actualizar las instalaciones anteriores con el mismo certificado.

VERIFICACION.md documenta resultados, límites y comprobaciones del archivo entregado. Las imágenes de verification/previews son renders de componentes con datos sintéticos; no son capturas del teléfono de Oriol.

## Acrónimos

| Acrónimo | Descripción | Contexto |
| --- | --- | --- |
| APK | Paquete de aplicación Android | Archivo instalable. |
| AI | Artificial Intelligence, inteligencia artificial | Nombre comercial. |

## Referencias

- Requisitos de identidad y comportamiento: instrucciones de Oriol en esta conversación.
- app/src/main: implementación entregada, especialmente WidgetConfig, LayoutPolicy, CellRenderer, ShadowRenderer, WidgetRenderer y PreviewView.
- [Composición de widgets Android](https://developer.android.com/develop/ui/views/appwidgets/overview).
- [Vistas remotas y ajuste de márgenes](https://developer.android.com/reference/android/widget/RemoteViews#setViewPadding(int,%20int,%20int,%20int,%20int)).
- [Texto con varias líneas](https://developer.android.com/reference/android/text/StaticLayout.Builder).
- [Medición de imágenes de Android](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/ImageView.java).
- [Fotos de contactos](https://developer.android.com/reference/android/provider/ContactsContract.ContactsColumns#PHOTO_URI).
- [Selección y apertura de contactos](https://developer.android.com/guide/components/intents-common#Contacts).
- [Firma de aplicaciones Android](https://developer.android.com/studio/publish/app-signing).
