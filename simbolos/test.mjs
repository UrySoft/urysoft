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
for(const l of [1,2,3]){
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
console.log('OK: tres niveles, extremos de tamaño, contraste, paridad, ida y vuelta HTTP, errores y fotografía sintética con localización automática.');
for(const level of [1,2,3]){
 for(const value of ['x','https://urysoft.com','  Bon dia, Oriol!\nCiència 🚀  ','x'.repeat(limits[level])]){
  const encoded=await req('encode?level='+level,'text/plain; charset=utf-8',value);
  assert.equal(encoded.status,200);
  const png=new Uint8Array(await encoded.arrayBuffer());
  const image=await fromPNG(png);
  assert.equal(image.width,image.height);
  const decoded=await req('decode','image/png',png);
  assert.equal(decoded.headers.get('X-UrySoft-Kind'),'text');
  assert.equal(await decoded.text(),value);
 }
}
