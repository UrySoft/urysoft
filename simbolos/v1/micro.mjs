import {crc32,rsEncode,rsRepair} from './codec.mjs?v=micro1';
import {toIndexedPNG} from './png.mjs?v=micro1';

export const microLevel=5;
export const microLimit=512*1024;
export const microSide=1536;
const markerRows=8;
const palette=[[17,17,17],[32,91,212],[242,193,36],[199,41,107]];
const marker=[0,2,1,3,1,2,0,3,2,3,0,1,3,1,2,0];
const magic=0x55525935; // URY5
const headerSize=16;
const parity=24;
const blockSize=255;
const dataSize=blockSize-parity;
const blockCount=Math.ceil((microLimit+headerSize)/dataSize);
const start=microSide*markerRows/4;

export function encodeMicro(input,text=false){
 if(!(input instanceof Uint8Array)||input.length>microLimit)throw Error('Este modo admite hasta 524.288 bytes.');
 const frame=new Uint8Array(blockCount*dataSize);
 const view=new DataView(frame.buffer);
 view.setUint32(0,magic);
 view.setUint32(4,input.length);
 view.setUint32(8,crc32(input));
 view.setUint8(12,Number(text));
 frame.set(input,headerSize);

 // Four successive pixels hold one byte, with the most significant bits first.
 const raster=new Uint8Array(microSide*microSide/4);
 for(let y=0;y<markerRows;y++)for(let x=0;x<marker.length;x++){
  raster[y*microSide/4+(x>>2)]|=marker[x]<<(6-(x%4)*2);
 }
 // Distribute adjacent raster bytes across blocks to tolerate local damage.
 for(let block=0;block<blockCount;block++){
  const code=rsEncode(frame.subarray(block*dataSize,(block+1)*dataSize),parity);
  for(let i=0;i<blockSize;i++)raster[start+i*blockCount+block]=code[i];
 }
 return toIndexedPNG(microSide,microSide,raster,palette,76800);
}

const byRed=new Uint8Array(256).fill(255);
palette.forEach(([red],index)=>{byRed[red]=index});
export function readMicro({width,height,pixels}){
 if(width!==microSide||height!==microSide)return null;
 const stride=pixels.length/(width*height);
 if(stride!==3&&stride!==4)return null;

 function colorAt(index){
  const offset=index*stride;
  const red=pixels[offset],green=pixels[offset+1],blue=pixels[offset+2];
  const exact=byRed[red];
  if(exact!==255&&palette[exact][1]===green&&palette[exact][2]===blue)return exact;
  let best=0,minimum=Infinity;
  for(let c=0;c<palette.length;c++){
   const [r,g,b]=palette[c];
   const d=(red-r)**2+(green-g)**2+(blue-b)**2;
   if(d<minimum){best=c;minimum=d}
  }
  return best;
 }
 const marked=[0,3,6].some(y=>marker.every((color,x)=>{
  const k=(y*width+x)*stride;
  return palette[color].every((channel,i)=>channel===pixels[k+i]);
 }));
 if(!marked)return null;

 const frame=new Uint8Array(blockCount*dataSize),code=new Uint8Array(blockSize);
 let corrected=0;
 for(let block=0;block<blockCount;block++){
  for(let i=0;i<blockSize;i++){
   const pixel=4*(start+i*blockCount+block);
   code[i]=(colorAt(pixel)<<6)|(colorAt(pixel+1)<<4)|(colorAt(pixel+2)<<2)|colorAt(pixel+3);
  }
  corrected+=rsRepair(code,parity);
  frame.set(code.subarray(0,dataSize),block*dataSize);
 }
 const view=new DataView(frame.buffer),length=view.getUint32(4),kind=view.getUint8(12);
 if(view.getUint32(0)!==magic||length>microLimit||kind>1)throw Error('Cabecera de alta densidad inválida.');
 const data=frame.slice(headerSize,headerSize+length);
 if(crc32(data)!==view.getUint32(8))throw Error('No se pudo verificar el contenido de alta densidad.');
 return {data,level:microLevel,corrected,kind:kind?'text':'file'};
}
