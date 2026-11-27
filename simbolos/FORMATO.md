# USY1 · Formato UrySoft Símbolos

**Geometría.** Página A4, 840 × 1.188 píxeles a 4 píxeles por milímetro. Cuadrícula de 25 columnas por 32 filas de 32 píxeles con origen superior izquierdo (20,32). El perímetro contiene cuatro marcadores con anillos negros y blancos y la paleta de referencia. El interior tiene 23 × 30 = 690 posiciones. Se recorren por filas. En la primera fila exterior, las columnas 1 a 16 (contando desde cero) identifican los 16 colores del perfil de color. El pie incluye un código QR que abre el descodificador web de UrySoft.

**Forma y giro.** El fichero codec.mjs publica los 16 enteros del alfabeto v2. Una máscara se lee de arriba abajo, en cada fila de izquierda a derecha, con el bit menos significativo en la esquina superior izquierda. El giro positivo es antihorario. Cada glifo se dibuja como 4 × 4 módulos de 6 píxeles con origen (4,4) dentro de su casilla. El fondo coloreado abarca los píxeles 1 a 30 en ambos ejes. El código bajo de 6 bits es 4 × índice de forma + giro (0 a 3).

**Campos de la casilla.** En el nivel 1 se guarda el código de 6 bits. En el nivel 2 se anteponen 4 bits de índice de trazo. En el nivel 3 se anteponen además 3 bits de índice de fondo dentro del grupo opuesto al trazo. Las paletas y la función render del módulo v1 son la referencia exacta de dibujo. Los perfiles de esta versión no comparten compatibilidad binaria con las maquetas gráficas previas.

**Trama.** Bytes 0–3: USY1 en ASCII. Byte 4: nivel. Byte 5: valor 1. Bytes 6–7: longitud del fichero sin signo en big-endian. Bytes 8–11: CRC-32/ISO-HDLC del contenido, big-endian. A partir del byte 12 se guardan los datos completos y se rellena con ceros hasta alcanzar la capacidad de entrada de los seis bloques. Se guarda el archivo sin compresión. No se incorpora el nombre del fichero ni se cifra el contenido.

**Corrección.** Se toman floor(690 × bitsPorCasilla / 8) bytes de canal. Se reparten en seis bloques consecutivos de longitud semejante, adjudicando los bytes sobrantes a los primeros bloques. Cada bloque contiene datos y, al final, 6/10/12 bytes de paridad para niveles 1/2/3, respectivamente. Reed–Solomon sistemático abreviado sobre GF(256), polinomio 0x11D, generador 2, raíces consecutivas desde exponente 0. Cada bloque corrige hasta 3/5/6 bytes erróneos de posición desconocida, respectivamente. CRC-32 se comprueba tras la reparación. En el nivel 3 se limita a 1.024 bytes útiles por contrato.

**Entrelazado y bits.** Se emite el primer byte de cada bloque, luego el segundo y así sucesivamente, saltando bloques agotados. Los bytes se leen desde su bit más significativo. El flujo resultante se divide en palabras de 6, 10 o 13 bits, también más significativo primero, rellenando con cero los bits finales de la última casilla.

**Referencia ejecutable.** codec.mjs contiene encodeCells, decodeCells, render y readImage. El servicio prueba cada nivel y cada giro y acepta únicamente una cabecera concordante y el CRC correcto. finders.mjs propone centros candidatos para fotografías. La comprobación de integridad decide si la interpretación es admisible.


## Nivel 4 · Alta capacidad

Para imágenes digitales. La página automática conserva las 690 posiciones útiles y su geometría. Cada posición tiene un marco de color `#DEEBF6` y 16 módulos de 6 × 6 píxeles en el mismo orden por filas. Cada módulo contiene dos bits. Los valores 0, 1, 2 y 3 se dibujan respectivamente como `#111111`, `#205BD4`, `#F2C124` y `#C7296B`. Se consumen primero los bits más significativos de cada palabra de 32 bits.

Se mantiene la cabecera de archivo de 12 bytes con nivel 4. Se distribuyen los bytes del canal entre tantos bloques como sean necesarios para no superar 230 bytes por bloque. Se conservan el entrelazado y la comprobación de integridad. La página automática tiene 12 bloques de 230 bytes y 6 bytes de paridad en cada uno, de modo que caben 2.676 bytes de archivo. Cada bloque completo repara hasta tres errores de byte de posición desconocida.

En cuadrículas compactas de texto de nivel 4, la cabecera contiene un byte de modo, dos bytes de longitud comprimida y cuatro bytes de comprobación de integridad del texto original. Los enteros multibyte se escriben en orden de byte más significativo primero. Los bloques cortos conservan los cuatro bytes de paridad del modo de texto. Los niveles anteriores mantienen su representación.

## Nivel 5 · 512 KiB en 2 × 2 cm

Formato para el archivo digital original. El PNG usa una paleta opaca de cuatro colores con los mismos índices del nivel 4. El raster mide 1.536 × 1.536 píxeles y declara 76.800 píxeles por metro en cada eje. Cada grupo de cuatro píxeles consecutivos contiene un byte, del par más significativo al menos significativo. Se recorren las filas de arriba abajo y de izquierda a derecha.

Las ocho primeras filas se reservan para identificar el formato. Sus primeros 16 píxeles repiten los índices `0, 2, 1, 3, 1, 2, 0, 3, 2, 3, 0, 1, 3, 1, 2, 0`. El resto de cada fila se rellena con el índice cero. A partir de la novena fila se escriben los datos.

La trama tiene una cabecera de 16 bytes. Los bytes 0–3 contienen `URY5`, los bytes 4–7 contienen la longitud original y los bytes 8–11 contienen la suma de integridad CRC-32. El byte 12 vale 0 para archivo o 1 para texto. Los tres siguientes están reservados y a cero. Los enteros multibyte usan primero el byte más significativo. El contenido de hasta 524.288 bytes se almacena directamente y se rellena con ceros.

La trama se distribuye en 2.270 bloques de 231 bytes útiles y 24 bytes de paridad Reed–Solomon por bloque. Se escribe el primer byte de todos los bloques, luego el segundo y así sucesivamente. El espacio restante se rellena con el índice cero. Cada bloque puede corregir hasta 12 bytes erróneos de posición desconocida. La lectura requiere verificar la cabecera y la suma de integridad antes de devolver el contenido.
