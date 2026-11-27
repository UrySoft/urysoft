# UrySoft Símbolos

Convierte un fichero en una página A4 de símbolos de 8 mm y recupera sus bytes desde una imagen. La versión de formato es **USY1**. El lector comprueba la cabecera, repara errores dentro del presupuesto de cada bloque y verifica el contenido antes de entregarlo.

[Aplicación web](https://urysoft.pages.dev/simbolos/) · [Contrato OpenAPI](./openapi.json) · [Especificación binaria](./FORMATO.md)

## Texto o enlace
En **Escribir texto o pegar un enlace**, introduce el contenido, elige el nivel y pulsa **Crear icono de texto**. Para codificar un fichero usa **Subir archivo**. Cambiar de nivel conserva el texto. El lector recupera el texto, permite copiarlo y ofrece abrir direcciones http o https. El texto se codifica de forma reversible. Se comparan los bytes literales, referencias a fragmentos repetidos y abreviaciones exactas de prefijos y terminaciones de direcciones web. El modo automático escoge la menor superficie total entre rectángulos de hasta 60 filas y columnas, contando marcadores y margen. En caso de empate elige la forma más próxima al cuadrado. Se mantienen las casillas de 32 × 32 píxeles. La vista previa indica bytes originales, bytes de contenido codificado y dimensiones.

El nuevo formato identifica la cuadrícula mediante la etiqueta superior 0xE40 y la etiqueta lateral 0xB40, más sus respectivas dimensiones. La cabecera de seis bytes contiene modo y longitud (5 y 11 bits) y la suma CRC32 del texto original. Los bloques de hasta 40 bytes incluyen cuatro bytes de paridad; los demás usan la paridad del nivel. El lector sigue reconociendo los formatos previos. Se admiten hasta 65.536 bytes originales de texto si la representación cabe en el nivel seleccionado. El fichero de imagen incluye además marcadores y colores de referencia.

El cliente puede enviar el texto directamente a `POST /encode?level=1` con `Content-Type: text/plain; charset=utf-8`. Los niveles 2 y 3 funcionan del mismo modo. Al descodificar, la cabecera `X-UrySoft-Kind` distingue `text` de `file`. Los lectores anteriores requieren actualización para interpretar las cuadrículas compactas.

## Nivel 5 · 512 KiB en 2 × 2 cm

Selecciona el **Nivel 5**, sube un archivo o escribe el texto y guarda la imagen. Cada píxel guarda dos bits en su color. El archivo PNG mide 1.536 × 1.536 píxeles y declara un tamaño físico de 2 × 2 cm. Admite hasta 524.288 bytes de contenido original y añade corrección de errores y control de integridad. Para recuperarlo, vuelve a cargar el PNG original. La geometría y la marca identificadora son fijas. La lectura desde fotografías o impresiones queda pendiente de validación.

## Imagen sin marcas
Desactiva **Incluir márgenes y marcas de lectura** para exportar exclusivamente las casillas. El lector web recupera los datos desde el fichero digital original: mide las filas y columnas con las dimensiones en píxeles y usa la paleta conocida. Para fotos, impresiones y capturas redimensionadas se recomienda mantener las marcas de posición y color. El cuadrado del pie de la página A4 abre el lector web y también desaparece en el modo sin marcas. El servicio usa `marks=0` para omitirlas y `marks=1` por defecto.

## Filas y columnas
En **Forma de la imagen** elige **Fijar columnas** o **Fijar filas**. Introduce de 1 a 60 casillas de datos. La web calcula la menor dimensión complementaria que admite el contenido, la cabecera y la corrección de errores. La vista previa se actualiza al cambiar el contenido, el nivel o el número. Si rebasa 60 en el otro eje, la web pide aumentar el valor fijo. El borde y los márgenes se añaden al exportar. El lector web identifica ambas dimensiones a partir de las marcas. En modo automático se conserva la página A4 para archivos y el rectángulo compacto de menor superficie para texto.

El servicio admite un único parámetro adicional en la codificación, por ejemplo `/encode?level=3&columns=10` o `/encode?level=3&rows=5`. Se aplica tanto a texto como a archivos.

## Primer uso

1. Abre la página web, selecciona un nivel y un archivo dentro del límite de la tabla.
2. Pulsa **Crear página de archivo** y **Guardar**. Imprime en papel A4, a tamaño real. El documento ocupa 210 × 297 mm. La imagen generada incluye la resolución física y un código en el pie que enlaza a esta documentación.
3. Para leerla, selecciona la imagen o una foto de la hoja completa y pulsa **Leer imagen**. Si falla la localización automática, pulsa **Marcar los cuatro centros** y toca los marcadores de las esquinas en el orden indicado.
4. Guarda los bytes verificados como `recuperado.bin`. Puedes devolverles la extensión del archivo original. El formato conserva el contenido y su longitud. El nombre original no se incluye.

La opción inicial procesa el archivo en el dispositivo. **Enviar al servicio web de UrySoft** utiliza las mismas operaciones mediante HTTP y transmite el archivo o la imagen al servidor. El código del servicio procesa la solicitud en memoria.

| Nivel | Apariencia | Bits por casilla | Máximo de bytes del archivo |
|---|---|---:|---:|
| 1 | Símbolo negro sobre blanco | 6 | 469 |
| 2 | Símbolo de uno de 16 colores sobre blanco | 10 | 790 |
| 3 | Símbolo y fondo de color con paleta restringida | 13 | 1.024 |
| 4 | 16 módulos independientes de cuatro colores | 32 | 2.676 |

El nivel 4 guarda hasta 2.676 bytes en las mismas 690 casillas útiles de una página automática. Guarda y usa la imagen digital original para recuperar los datos.

El nivel 3 admite 128 parejas ordenadas de colores. La paleta se divide en dos grupos de ocho. Fondo y trazo pertenecen siempre a grupos opuestos y las 128 parejas superan o igualan la relación de luminancia 3 a 1 en la imagen digital. Los niveles 1 a 3 usan las 16 formas del alfabeto corregido con cuatro orientaciones cada una. Los límites son de **datos útiles**, después del control de integridad y la redundancia. Las láminas de diseño anteriores al formato USY1 sirven como muestras visuales.

## API HTTP v1

Dirección del servicio UrySoft: `https://urysoft.pages.dev/api/simbolos/v1`. Para ejecutar el mismo servicio en tu equipo, instala Node.js 22 o superior y ejecuta desde la raíz del repositorio:

```sh
node simbolos/server.mjs
```

Dirección local: `http://127.0.0.1:8787/api/simbolos/v1`. Las variables opcionales `HOST` y `PORT` eligen la interfaz y el puerto.

| Operación | Cuerpo de entrada | Respuesta correcta |
|---|---|---|
| GET /api/simbolos/v1 | Vacío | Descripción JSON, versión y límites |
| POST /api/simbolos/v1/encode?level=3 | Bytes originales, application/octet-stream | Página image/png |
| POST /api/simbolos/v1/decode | Imagen, image/png | Bytes originales, application/octet-stream |

Ejemplo con curl, desde un directorio con el fichero pequeño `mensaje.txt`:

```sh
curl --fail-with-body -H "Content-Type: application/octet-stream" --data-binary @mensaje.txt "http://127.0.0.1:8787/api/simbolos/v1/encode?level=3" -o pagina.png
curl --fail-with-body -D cabeceras.txt -H "Content-Type: image/png" --data-binary @pagina.png "http://127.0.0.1:8787/api/simbolos/v1/decode" -o recuperado.txt
```

Para usar el alojamiento de UrySoft, cambia el origen local por el del servicio. Se admiten llamadas desde otros orígenes sin credenciales. En descodificación, las cabeceras `X-UrySoft-Level` y `X-UrySoft-Corrected` indican nivel detectado y número de bytes reparados. La ruta de decodificación admite PNG de 8 bits RGB/RGBA o de color indexado de 2 bits, sin entrelazar, hasta 4 millones de píxeles y 12.000.000 bytes de entrada. Usa una imagen opaca. La aplicación web convierte las fotografías que el navegador puede abrir y limita su lado mayor a 1.800 píxeles.

**Cuatro puntos opcionales.** Añade el parámetro `corners` con una matriz JSON codificada para URL. Sus cuatro pares son coordenadas de los **centros de los marcadores**, en píxeles de la imagen enviada y en orden alrededor del perímetro. Para una página original vertical de 840 × 1.188 píxeles: `[[36,48],[804,48],[804,1040],[36,1040]]`. El lector prueba los cuatro giros. Una foto necesita sus propias coordenadas.

**Errores.** La API envía un objeto JSON con la propiedad `error`. 400 indica un parámetro inválido, 404 una ruta desconocida, 405 un método incorrecto, 413 exceso de tamaño, 415 tipo de contenido incompatible y 422 imagen ilegible, dañada o sin una trama íntegra. Ante 422 en una fotografía, incluye los centros o toma una imagen mejor enfocada.

## Integración sin servidor

Los módulos abiertos en `v1/` funcionan sin dependencias de npm. Utiliza un navegador moderno o Node.js 22 o superior. Ejemplo desde el directorio `simbolos`:

```js
import {render, readImage} from './v1/codec.mjs';
import {toPNG, fromPNG} from './v1/png.mjs';
const raw = new TextEncoder().encode('Hola, UrySoft');
const png = await toPNG(render(raw, 1));
const result = readImage(await fromPNG(png));
console.log(new TextDecoder().decode(result.data), result.level, result.corrected);
```

## Comprobaciones

Prueba de los formatos y de los contratos de petición y respuesta:

```sh
node simbolos/test.mjs
```

Se han verificado los tres niveles sobre imágenes digitales y una transformación geométrica sintética con localización de marcadores. La distancia de lectura, las condiciones de luz, la cámara y la impresión requieren ensayos físicos. La comprobación CRC detecta corrupción accidental. La identidad del emisor no forma parte del formato.

## Acrónimos

| Acrónimo | Descripción | Contexto |
|---|---|---|
| API | Application Programming Interface | Interfaz de integración |
| HTTP | Hypertext Transfer Protocol | Transporte del servicio |
| PNG | Portable Network Graphics | Formato de imagen |
| CRC | Cyclic Redundancy Check | Comprobación de integridad |
| QR | Quick Response | Acceso a la documentación desde el pie |
| RGB / RGBA | Red, Green, Blue y canal Alpha | Canales admitidos en el servicio |
| JSON | JavaScript Object Notation | Respuesta de estado y errores |
