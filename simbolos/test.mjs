import assert from 'node:assert/strict';
import {render,encodeCells,decodeCells,readImage,limits,palette,corners} from './v1/codec.mjs';
import {handle} from './v1/http.mjs';
import {findCorners} from './v1/finders.mjs';
import {fromPNG,toPNG} from './v1/png.mjs';
import {randomBytes} from 'node:crypto';
const url='http://localhost/api/simbolos/v1/';
const req=(action,type,body)=>handle(new Request(url+action,{method:'POST',headers:{'Content-Type':type},body}));
function lum(p){return p.reduce((v,c,i)=>{let x=c/255;return v+[.2126,.7152,.0722][i]*(x<=.04045?x/12.92:((x+.055)/1.055)**2.4)},0)}
for(let i=0;i<16;i++)for(let j=0;j<16;j++)if((i<8)!==(j<8)){let a=lum(palette[i]),b=lum(palette[j]);assert((Math.max(a,b)+.05)/(Math.min(a,b)+.05)>=3)}
for(const l of [1,2,3,4]){
 for(const size of [0,1,limits[l]-1,limits[l]]){
  const data=Uint8Array.from(randomBytes(size));const cells=encodeCells(data,l);
  cells[35]^=15;cells[81]^=21;assert.deepEqual(decodeCells(cells,l).data,data);
  const res=await req('encode?level='+l,'application/octet-stream',data);assert.equal(res.status,200);const png=new Uint8Array(await res.arrayBuffer());
  const decoded=await req('decode','image/png',png);assert.equal(decoded.status,200);assert.equal(decoded.headers.get('X-UrySoft-Level'),String(l));assert.deepEqual(new Uint8Array(await decoded.arrayBuffer()),data);
 }
 assert.equal((await req('encode?level='+l,'application/octet-stream',new Uint8Array(limits[l]+1))).status,413);
}
assert.equal((await req('decode','image/png',new Uint8Array(12))).status,422);
const real=new Uint8Array(randomBytes(1024)),im=render(real,3);
const pts=findCorners(im);assert(pts);assert.deepEqual(readImage(im,pts).data,real);
const tilted={width:1000,height:1400,pixels:new Uint8Array(1000*1400*3).fill(200)};
for(let y=0;y<tilted.height;y++)for(let x=0;x<tilted.width;x++){
 let sy=Math.floor((y-80)/1.02),sx=Math.floor((x-40-.065*sy)/1.04);
 if(sx>=0&&sy>=0&&sx<im.width&&sy<im.height){const k=(sy*im.width+sx)*3,j=(y*tilted.width+x)*3;tilted.pixels.set(im.pixels.subarray(k,k+3),j)}
}
assert.deepEqual(readImage(tilted,findCorners(tilted)).data,real);
console.log('OK: cuatro niveles, extremos de tamaño, contraste, paridad, ida y vuelta HTTP, errores y fotografía sintética con localización automática.');
for(const level of [1,2,3,4]){
 for(const value of ['x','https://urysoft.com','  Bon dia, Oriol!\nCiència 🚀  ','x'.repeat(limits[level])]){
  const encoded=await req('encode?level='+level,'text/plain; charset=utf-8',value);
  assert.equal(encoded.status,200);
  const png=new Uint8Array(await encoded.arrayBuffer());
  const image=await fromPNG(png);
  assert(image.width>0&&image.height>0);
  const decoded=await req('decode','image/png',png);
  assert.equal(decoded.headers.get('X-UrySoft-Kind'),'text');
  assert.equal(await decoded.text(),value);
 }
}
for(const level of [1,2,3,4]){
 for(const type of ['text/plain','application/octet-stream']){
  for(const axis of ['rows','columns']){
   const payload='https://urysoft.com';
   const res=await req('encode?level='+level+'&'+axis+'=10',type,payload);
   assert.equal(res.status,200);
   const png=new Uint8Array(await res.arrayBuffer()),img=await fromPNG(png);
   assert.equal(axis==='rows'?img.height:img.width,416);
   assert.notEqual(img.width,img.height);
   const result=await req('decode','image/png',png);
   assert.equal(result.headers.get('X-UrySoft-Kind'),type==='text/plain'?'text':'file');
   assert.equal(await result.text(),payload);
  }
 }
}
for(const query of ['rows=0','rows=2.5','columns=61','rows=3&columns=4']){
 assert.equal((await req('encode?'+query,'text/plain','hola')).status,400);
}
const {packText,unpackText}=await import('./v1/text.mjs');
const {planText}=await import('./v1/codec.mjs');
for(const value of ['https://urysoft.com','https://','http://www.example.org/à','\uFEFFÀé\n🚀  ','Un mensaje que se repite. '.repeat(300),'x'.repeat(65536)]){
 const original=new TextEncoder().encode(value),packed=packText(original);
 assert.deepEqual(unpackText(packed.mode,packed.bytes),original);
 if(value.includes('urysoft')){
  assert.equal(packed.bytes.length,7);
  const {columns,rows}=planText(original,3);
  assert.equal(columns*rows,12);
 }
 if(value.length<20000){
  const image=render(original,3,true);
  assert.deepEqual(readImage(image).data,original);
 }
}
assert.throws(()=>packText(new Uint8Array(65537)));
for(const level of [1,2,3,4])for(const type of ['text/plain','application/octet-stream']){
 const url='https://urysoft.com';
 for(const shape of ['','&rows=1','&columns=9']){
  const encoded=await req('encode?marks=0&level='+level+shape,type,url);
  assert.equal(encoded.status,200);
  const png=new Uint8Array(await encoded.arrayBuffer());
  const image=await fromPNG(png);
  if(level===3&&type==='text/plain'&&!shape)assert.deepEqual([image.width,image.height],[96,128]);
  if(shape==='&rows=1')assert.equal(image.height,32);
  const decoded=await req('decode','image/png',png);
  assert.equal(decoded.headers.get('X-UrySoft-Kind'),type==='text/plain'?'text':'file');
  assert.equal(await decoded.text(),url);
 }
}

const denseFile=randomBytes(2676);
const denseText=randomBytes(2200).toString('base64').slice(0,2676);
assert(packText(new TextEncoder().encode(denseText)).bytes.length>2047);
assert.deepEqual([render(denseFile,4).width,render(denseFile,4).height],[840,1188]);
for(const [query,type,payload,kind] of [
 ['&marks=0','application/octet-stream',denseFile,'file'],
 ['&columns=23','application/octet-stream',denseFile,'file'],
 ['&rows=30','text/plain',denseText,'text'],
 ['&marks=0','text/plain',denseText,'text']
]){
 const encoded=await req('encode?level=4'+query,type,payload);
 assert.equal(encoded.status,200);
 const png=await encoded.arrayBuffer(),image=await fromPNG(new Uint8Array(png));
 if(query==='&marks=0'&&kind==='file')assert.deepEqual([image.width,image.height],[23*32,30*32]);
 if(query==='&columns=23')assert.equal(image.width,26*32);
 if(query==='&rows=30')assert.equal(image.height,33*32);
 const decoded=await req('decode','image/png',png);
 assert.equal(decoded.status,200);
 assert.equal(decoded.headers.get('X-UrySoft-Level'),'4');
 assert.equal(decoded.headers.get('X-UrySoft-Kind'),kind);
 assert.deepEqual(Buffer.from(await decoded.arrayBuffer()),Buffer.from(payload));
}
const damagedDense=render(denseFile,4,false,'auto',undefined,false);
for(let y=4;y<28;y++)for(let x=4;x<28;x++){
 const pixel=(y*damagedDense.width+x)*3;
 damagedDense.pixels.set([17,17,17],pixel);
}
const repairedDense=readImage(damagedDense);
assert(repairedDense.corrected>0);
assert.deepEqual(Buffer.from(repairedDense.data),denseFile);
console.log('OK: alta capacidad de 2.676 bytes en 690 casillas, formatos con y sin marcas, ejes fijos, texto largo y reparación de una casilla dañada.');

const {microLimit,microSide,readMicro}=await import('./v1/micro.mjs');
const microFile=randomBytes(microLimit);
const microResponse=await req('encode?level=5','application/octet-stream',microFile);
assert.equal(microResponse.status,200);
const microPNG=new Uint8Array(await microResponse.arrayBuffer());
const microView=new DataView(microPNG.buffer);
assert.equal(microView.getUint8(24),2); // Two bits per indexed pixel.
assert.equal(microView.getUint32(41),76800); // Horizontal pixels per metre.
assert.equal(microView.getUint32(45),76800);
assert.equal(microView.getUint8(49),1); // Metre is the physical unit.
const microImage=await fromPNG(microPNG);
assert.deepEqual([microImage.width,microImage.height],[microSide,microSide]);
assert.equal(microSide/76800,.02);
const microDecoded=await req('decode','image/png',microPNG);
assert.equal(microDecoded.status,200);
assert.equal(microDecoded.headers.get('X-UrySoft-Level'),'5');
assert.equal(microDecoded.headers.get('X-UrySoft-Kind'),'file');
assert.deepEqual(Buffer.from(await microDecoded.arrayBuffer()),microFile);

for(let y=8;y<11;y++)for(let x=0;x<microSide;x++){
 microImage.pixels.fill(17,(y*microSide+x)*3,(y*microSide+x)*3+3);
}
const repairedMicro=readMicro(microImage);
assert(repairedMicro.corrected>0);
assert.deepEqual(Buffer.from(repairedMicro.data),microFile);
const microText='Text exacte amb accents i emoji 🚀\n';
const microTextResponse=await req('encode?level=5','text/plain',microText);
assert.equal(microTextResponse.status,200);
const decodedMicroText=await req('decode','image/png',await microTextResponse.arrayBuffer());
assert.equal(decodedMicroText.headers.get('X-UrySoft-Kind'),'text');
assert.equal(await decodedMicroText.text(),microText);
assert.equal((await req('encode?level=5','application/octet-stream',new Uint8Array(microLimit+1))).status,413);
assert.equal((await req('encode?level=5&rows=2','text/plain','x')).status,400);
console.log('OK: 512 KiB en 2 × 2 cm, densidad física, recuperación exacta, texto y corrección de daño localizado.');
