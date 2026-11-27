# UrySoft Símbolos

Convierte un fichero en una página A4 de símbolos de 8 mm y recupera sus bytes desde una imagen. La versión de formato es **USY1** y la aplicación Android es la **0.1.1**. El lector comprueba la cabecera, repara errores dentro del presupuesto de cada bloque y verifica el contenido antes de entregarlo.

[Aplicación web](https://urysoft.pages.dev/simbolos/) · [Contrato OpenAPI](./openapi.json) · [Aplicación Android firmada](https://urysoft.pages.dev/simbolos/android/UrySoft-Simbolos-0.1.1.apk) · [Especificación binaria](./FORMATO.md)

## Primer uso

1. Abre la aplicación web o Android, selecciona un nivel y un archivo dentro del límite de la tabla.
2. Pulsa **Crear página** y **Guardar**. Imprime en papel A4, a tamaño real. El documento ocupa 210 × 297 mm. La imagen generada incluye la resolución física y un código en el pie que enlaza a esta documentación.
3. Para leerla, selecciona la imagen o una foto de la hoja completa y pulsa **Leer imagen**. Si falla la localización automática, pulsa **Marcar los cuatro centros** y toca los marcadores de las esquinas en el orden indicado.
4. Guarda los bytes verificados como `recuperado.bin`. Puedes devolverles la extensión del archivo original. El formato conserva el contenido y su longitud. El nombre original no se incluye.

La opción inicial procesa el archivo en el dispositivo. **Enviar al servicio web de UrySoft** utiliza las mismas operaciones mediante HTTP y transmite el archivo o la imagen al servidor. El código del servicio procesa la solicitud en memoria.

| Nivel | Apariencia | Bits por casilla | Máximo de bytes del archivo |
|---|---|---:|---:|
| 1 | Símbolo negro sobre blanco | 6 | 469 |
| 2 | Símbolo de uno de 16 colores sobre blanco | 10 | 790 |
| 3 | Símbolo y fondo de color con paleta restringida | 13 | 1.024 |

El nivel 3 admite 128 parejas ordenadas de colores. La paleta se divide en dos grupos de ocho. Fondo y trazo pertenecen siempre a grupos opuestos y las 128 parejas superan o igualan la relación de luminancia 3 a 1 en la imagen digital. Las tres opciones usan las 16 formas del alfabeto corregido con cuatro orientaciones cada una. Los límites son de **datos útiles**, después del control de integridad y la redundancia. Las láminas de diseño anteriores al formato USY1 sirven como muestras visuales.

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

Para usar el alojamiento de UrySoft, cambia el origen local por el del servicio. Se admiten llamadas desde otros orígenes sin credenciales. En descodificación, las cabeceras `X-UrySoft-Level` y `X-UrySoft-Corrected` indican nivel detectado y número de bytes reparados. La ruta de decodificación admite PNG de 8 bits RGB/RGBA sin entrelazar, hasta 4 millones de píxeles y 12.000.000 bytes de entrada. Usa una imagen opaca. La aplicación web convierte las fotografías que el navegador puede abrir y limita su lado mayor a 1.800 píxeles.

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

## Android y comprobaciones

La aplicación incluye el mismo núcleo y la interfaz web como recursos locales. Requiere Android 8 o posterior y un Android System WebView actualizado con módulos JavaScript, createImageBitmap y CompressionStream. Utiliza el selector del sistema para abrir y guardar archivos. La modalidad local no necesita conexión una vez instalada.

Para compilar en Windows instala JDK 17, la plataforma Android 36 y Build Tools 36.0.0. Define `URY_SIGN_PASS` en el entorno y pasa un almacén PKCS12 propio y su alias. Para actualizar la aplicación oficial se necesita su misma clave de firma.

```powershell
./simbolos/android/build.ps1 -Sdk C:/Android/sdk -Jdk C:/Java/jdk17 -Keystore C:/firma/mi-clave.p12 -Alias mi-alias
```

Prueba de los formatos y de los contratos de petición y respuesta:

```sh
node simbolos/test.mjs
```

Se han verificado los tres niveles sobre imágenes digitales y una transformación geométrica sintética con localización de marcadores. La distancia de lectura, las condiciones de luz, la cámara y la impresión requieren ensayos físicos. La comprobación CRC detecta corrupción accidental. La identidad del emisor no forma parte del formato.

## Acrónimos

| Acrónimo | Descripción | Contexto |
|---|---|---|
| API | Application Programming Interface | Interfaz de integración |
| APK | Android Package | Instalador de Android |
| HTTP | Hypertext Transfer Protocol | Transporte del servicio |
| PNG | Portable Network Graphics | Formato de imagen |
| CRC | Cyclic Redundancy Check | Comprobación de integridad |
| QR | Quick Response | Acceso a la documentación desde el pie |
| RGB / RGBA | Red, Green, Blue y canal Alpha | Canales admitidos en el servicio |
| JSON | JavaScript Object Notation | Respuesta de estado y errores |
| JDK | Java Development Kit | Compilación de la aplicación |

