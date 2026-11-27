import {crc32} from './codec.mjs';
const signature=Uint8Array.of(137,80,78,71,13,10,26,10);
const join=parts=>{const out=new Uint8Array(parts.reduce((n,p)=>n+p.length,0));let i=0;for(const p of parts){out.set(p,i);i+=p.length}return out};
const word=n=>{const a=new Uint8Array(4);new DataView(a.buffer).setUint32(0,n);return a};
function chunk(name,p){const t=new TextEncoder().encode(name),body=join([t,p]);return join([word(p.length),body,word(crc32(body))])}
async function writePNG(width,height,rows,depth,type,density,palette){
 const compressed=await new Response(new Blob([rows]).stream().pipeThrough(new CompressionStream('deflate'))).arrayBuffer();
 const header=chunk('IHDR',join([word(width),word(height),Uint8Array.of(depth,type,0,0,0)]));
 const dimensions=chunk('pHYs',join([word(density),word(density),Uint8Array.of(1)]));
 const coloring=palette?[chunk('PLTE',Uint8Array.from(palette.flat()))]:[];
 return join([signature,header,dimensions,...coloring,chunk('IDAT',new Uint8Array(compressed)),chunk('IEND',[])]);
}
function filterlessRows(raster,line,height){
 const rows=new Uint8Array((line+1)*height);
 for(let y=0;y<height;y++)rows.set(raster.subarray(y*line,(y+1)*line),y*(line+1)+1);
 return rows;
}
export function toPNG({width,height,pixels}){
 return writePNG(width,height,filterlessRows(pixels,3*width,height),8,2,4000);
}
export function toIndexedPNG(width,height,raster,palette,density){
 return writePNG(width,height,filterlessRows(raster,Math.ceil(width/4),height),2,3,density,palette);
}
export async function readBounded(stream,max){
 if(!stream)throw Error('Cuerpo vacío');
 const reader=stream.getReader(),parts=[];let n=0;
 try{for(;;){const {value,done}=await reader.read();if(done)break;n+=value.length;if(n>max){await reader.cancel();throw Error('Tamaño excesivo')}parts.push(value)}}finally{reader.releaseLock()}
 return join(parts);
}
export async function fromPNG(a){
 if(!signature.every((v,i)=>a[i]===v))throw Error('Se requiere PNG');
 const view=new DataView(a.buffer,a.byteOffset,a.byteLength),parts=[];let width=0,height=0,stride=0,ended=false,indexed=false,palette;
 for(let k=8;k+12<=a.length;){const n=view.getUint32(k);if(k+12+n>a.length)throw Error('PNG truncado');
  const name=String.fromCharCode(...a.slice(k+4,k+8)),p=a.subarray(k+8,k+8+n);
  if(view.getUint32(k+8+n)!==crc32(a.subarray(k+4,k+8+n)))throw Error('PNG corrupto');
  if(name==='IHDR'){if(k!==8||n!==13)throw Error('Cabecera PNG inválida');width=view.getUint32(k+8);height=view.getUint32(k+12);stride=({2:3,6:4})[p[9]];
   indexed=p[9]===3&&p[8]===2;
   if(indexed)stride=1;
   if((!indexed&&(!stride||p[8]!==8))||p[10]||p[11]||p[12]||!width||!height||width*height>4000000)throw Error('Formato PNG no compatible o superior a 4 megapíxeles');
  }
  if(name==='PLTE')palette=p;
  if(name==='tRNS'&&indexed)throw Error('La imagen de color indexado debe ser opaca.');
  if(name==='IDAT')parts.push(p);if(name==='IEND'){ended=true;break}k+=12+n;
 }
 if(!width||!ended||!parts.length||(indexed&&(!palette?.length||palette.length%3||palette.length>12)))throw Error('PNG incompleto');
 const line=indexed?Math.ceil(width/4):width*stride,expected=(line+1)*height;
 const raw=await readBounded(new Blob(parts).stream().pipeThrough(new DecompressionStream('deflate')),expected);
 if(raw.length!==expected)throw Error('PNG incompleto');
 const pixels=new Uint8Array(line*height);let k=0;
 for(let y=0;y<height;y++){const filter=raw[k++];if(filter>4)throw Error('Filtro PNG inválido');
  for(let j=0;j<line;j++){const i=y*line+j,a=j>=stride?pixels[i-stride]:0,b=y?pixels[i-line]:0,c=y&&j>=stride?pixels[i-line-stride]:0;
   let f=0;if(filter===1)f=a;if(filter===2)f=b;if(filter===3)f=(a+b)>>1;if(filter===4){const p=a+b-c,da=Math.abs(p-a),db=Math.abs(p-b),dc=Math.abs(p-c);f=da<=db&&da<=dc?a:db<=dc?b:c}
   pixels[i]=raw[k++]+f;
  }
 }
 if(!indexed)return {pixels,width,height};
 const colors=new Uint8Array(width*height*3);
 for(let y=0;y<height;y++)for(let x=0;x<width;x++){
  const color=((pixels[y*line+(x>>2)]>>(6-(x%4)*2))&3)*3;
  if(color>=palette.length)throw Error('Índice de color inválido.');
  const out=(y*width+x)*3;
  colors[out]=palette[color];colors[out+1]=palette[color+1];colors[out+2]=palette[color+2];
 }
 return {pixels:colors,width,height};
}

