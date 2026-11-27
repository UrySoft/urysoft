# USY1 · Formato UrySoft Símbolos

**Geometría.** Página A4, 840 × 1.188 píxeles a 4 píxeles por milímetro. Cuadrícula de 25 columnas por 32 filas de 32 píxeles con origen superior izquierdo (20,32). El perímetro contiene cuatro marcadores con anillos negros y blancos y la paleta de referencia. El interior tiene 23 × 30 = 690 posiciones. Se recorren por filas. En la primera fila exterior, las columnas 1 a 16 (contando desde cero) identifican los 16 colores del perfil de color. El pie incluye un código QR que abre el descodificador web de UrySoft.

**Forma y giro.** El fichero codec.mjs publica los 16 enteros del alfabeto v2. Una máscara se lee de arriba abajo, en cada fila de izquierda a derecha, con el bit menos significativo en la esquina superior izquierda. El giro positivo es antihorario. Cada glifo se dibuja como 4 × 4 módulos de 6 píxeles con origen (4,4) dentro de su casilla. El fondo coloreado abarca los píxeles 1 a 30 en ambos ejes. El código bajo de 6 bits es 4 × índice de forma + giro (0 a 3).

**Campos de la casilla.** En el nivel 1 se guarda el código de 6 bits. En el nivel 2 se anteponen 4 bits de índice de trazo. En el nivel 3 se anteponen además 3 bits de índice de fondo dentro del grupo opuesto al trazo. Las paletas y la función render del módulo v1 son la referencia exacta de dibujo. Los perfiles de esta versión no comparten compatibilidad binaria con las maquetas gráficas previas.

**Trama.** Bytes 0–3: USY1 en ASCII. Byte 4: nivel. Byte 5: valor 1. Bytes 6–7: longitud del fichero sin signo en big-endian. Bytes 8–11: CRC-32/ISO-HDLC del contenido, big-endian. A partir del byte 12 se guardan los datos completos y se rellena con ceros hasta alcanzar la capacidad de entrada de los seis bloques. Se guarda el archivo sin compresión. No se incorpora el nombre del fichero ni se cifra el contenido.

**Corrección.** Se toman floor(690 × bitsPorCasilla / 8) bytes de canal. Se reparten en seis bloques consecutivos de longitud semejante, adjudicando los bytes sobrantes a los primeros bloques. Cada bloque contiene datos y, al final, 6/10/12 bytes de paridad para niveles 1/2/3, respectivamente. Reed–Solomon sistemático abreviado sobre GF(256), polinomio 0x11D, generador 2, raíces consecutivas desde exponente 0. Cada bloque corrige hasta 3/5/6 bytes erróneos de posición desconocida, respectivamente. CRC-32 se comprueba tras la reparación. En el nivel 3 se limita a 1.024 bytes útiles por contrato.

**Entrelazado y bits.** Se emite el primer byte de cada bloque, luego el segundo y así sucesivamente, saltando bloques agotados. Los bytes se leen desde su bit más significativo. El flujo resultante se divide en palabras de 6, 10 o 13 bits, también más significativo primero, rellenando con cero los bits finales de la última casilla.

**Referencia ejecutable.** codec.mjs contiene encodeCells, decodeCells, render y readImage. El servicio prueba cada nivel y cada giro y acepta únicamente una cabecera concordante y el CRC correcto. finders.mjs propone centros candidatos para fotografías. La comprobación de integridad decide si la interpretación es admisible.

