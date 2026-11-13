# Recursos visuales de Lumen

## Icono

`assets/apps/lumen/icon-android-official.svg` es una transcripción determinista del drawable Android `res/drawable/ic_lumen.xml`, comprobado dentro del APK oficial Lumen 4.0.2. Conserva viewport, trazados y colores #101b24 / #91e5cf. No es un rediseño generado. Sustituye en la ficha y en el catálogo a la variante web blanca.

## Capturas actuales

Se han capturado cuatro pantallas en cada idioma (ES/CA/EN), a 412 × 915 píxeles CSS: Hablar, Escuchar, Frases rápidas y Opciones. Los nombres siguen screen-{pantalla}-{idioma}-4-0-2.png. La web y las miniaturas seleccionan el archivo según el idioma activo.

Los controles se ejecutan con los archivos assets/www del APK oficial 4.0.2. Son capturas de navegador de la interfaz incluida en el instalador, no capturas de Android emulado. Los textos de demostración se escribieron sin emitirlos.

La vista Escuchar usa un proveedor de cámara de demostración únicamente en la rama de captura, activado por cameraDemo=1. Una imagen generada de un móvil con la linterna encendida se presenta como MediaStream de un lienzo. Se conservan los controles y el muestreo de Lumen, pero no se simulan paquetes recibidos ni se muestra una recepción conseguida. Tanto la imagen como su leyenda identifican la cámara simulada en cada idioma. La descarga de la aplicación no contiene este proveedor.

La rama lumen-capture-4-0-2 conserva la sesión reproducible. No contiene firmas ni claves privadas.

## Cómic

Ilustración generada con la herramienta integrada ImageGen. El original queda guardado localmente en `assets/apps/lumen/lumen-zombie-comic-original.png`; la web usa `lumen-zombie-comic.jpg`, con el mismo contenido y codificación más ligera. Los contenedores SVG de la ficha muestran cada viñeta por separado, sin alterar el dibujo. Las leyendas son texto HTML traducido a ES/CA/EN. La ilustración no sustituye a capturas reales.

Prompt utilizado:

Create one polished, stylish, humorous six-panel wordless comic page (3 columns by 2 rows), landscape 1536x1024, for Lumen, a real text-over-light communication app. Franco-Belgian graphic novel meets modern editorial illustration, confident ink lines, cinematic navy/teal night palette, warm coral details, mint-white light accents, readable compositions, consistent adult characters: a curly-haired woman in orange jacket and a bearded man in blue jacket. Friendly goofy zombies, no gore, no injuries, no weapons. Story: panel1 city communications blackout, the two heroes in neighboring nearby buildings, silly zombies milling in street; panel2 woman sees phone's no-network symbol, exasperated comic face; panel3 she types a short message on a phone with simple abstract screen, no readable UI or text; panel4 she aims phone flashlight from one window toward the man's smartphone CAMERA at the neighboring window, clear line of sight, illustrate a few discrete mint light pulses in the air as a visual metaphor, not a laser beam; panel5 man sees a clear check mark on receiving phone, delighted, while a zombie below confusedly taps a dead landline handset; panel6 the two exchange relieved thumbs up from their nearby windows, with a goofy zombie holding up a cardboard no-signal pictogram and looking puzzled. Each panel must be a separate coherent scene, crisp gutter borders, high-quality artwork appropriate for a professional app website, charming expressive acting. Absolutely no words, no letters, no caption text, no speech balloons, no brand logo, no watermark. Do not imply satellite, wireless mesh, long distance or guaranteed rescue; only close-range visible light and receiving camera. This is fictional promotional illustration, not screenshots. Leave no blank caption boxes.

## Imagen para la cámara de demostración

Creada con ImageGen integrado. La escena se conserva en la rama de captura como camera-demo.jpg. Prompt:

Create a realistic but clearly staged demonstration photograph to serve as a simulated receiving-camera view inside a light communication app. Portrait 1024x1536. A hand holds a plain unbranded dark smartphone BACK toward the viewer, at a short distance in a softly lit neutral room. The rear LED flashlight is on: a small intense white light spot positioned at the exact center of the entire image, with a gentle realistic glow, not a laser beam. The phone is vertical; place its top camera/flash module at the image center by framing the body of the phone below center. Another phone receiving this light is outside the frame, represented by this camera viewpoint. Soft navy/gray background, subdued lighting, natural lens blur, clear visible phone silhouette, high quality mobile camera aesthetic. No words, no letters, no logos, no screen interface, no arrows, no UI overlay, no people’s faces, no readable personal data. This is a fictional camera demonstration scene, not evidence of a tested transmission. Compose for a tall narrow camera preview with the bright point and the handset remaining visible if cropped slightly on the sides.
