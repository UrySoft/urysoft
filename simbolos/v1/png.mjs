import {crc32} from './codec.mjs';
const signature=Uint8Array.of(137,80,78,71,13,10,26,10);
const join=parts=>{const out=new Uint8Array(parts.reduce((n,p)=>n+p.length,0));let i=0;for(const p of parts){out.set(p,i);i+=p.length}return out};
const word=n=>{const a=new Uint8Array(4);new DataView(a.buffer).setUint32(0,n);return a};
function chunk(name,p){const t=new TextEncoder().encode(name),body=join([t,p]);return join([word(p.length),body,word(crc32(body))])}
export async function toPNG({width,height,pixels}){
 const rows=new Uint8Array((1+3*width)*height);
 for(let y=0;y<height;y++)rows.set(pixels.subarray(y*3*width,(y+1)*3*width),y*(1+3*width)+1);
 const compressed=await new Response(new Blob([rows]).stream().pipeThrough(new CompressionStream('deflate'))).arrayBuffer();
 return join([signature,chunk('IHDR',join([word(width),word(height),Uint8Array.of(8,2,0,0,0)])),chunk('pHYs',join([word(4000),word(4000),Uint8Array.of(1)])),chunk('IDAT',new Uint8Array(compressed)),chunk('IEND',[])]);
}
export async function readBounded(stream,max){
 if(!stream)throw Error('Cuerpo vacío');
 const reader=stream.getReader(),parts=[];let n=0;
 try{for(;;){const {value,done}=await reader.read();if(done)break;n+=value.length;if(n>max){await reader.cancel();throw Error('Tamaño excesivo')}parts.push(value)}}finally{reader.releaseLock()}
 return join(parts);
}
export async function fromPNG(a){
 if(!signature.every((v,i)=>a[i]===v))throw Error('Se requiere PNG');
 const view=new DataView(a.buffer,a.byteOffset,a.byteLength),parts=[];let width=0,height=0,stride=0,ended=false;
 for(let k=8;k+12<=a.length;){const n=view.getUint32(k);if(k+12+n>a.length)throw Error('PNG truncado');
  const name=String.fromCharCode(...a.slice(k+4,k+8)),p=a.subarray(k+8,k+8+n);
  if(view.getUint32(k+8+n)!==crc32(a.subarray(k+4,k+8+n)))throw Error('PNG corrupto');
  if(name==='IHDR'){if(k!==8||n!==13)throw Error('Cabecera PNG inválida');width=view.getUint32(k+8);height=view.getUint32(k+12);stride=({2:3,6:4})[p[9]];
   if(!stride||p[8]!==8||p[10]||p[11]||p[12]||!width||!height||width*height>4000000)throw Error('PNG RGB/RGBA de 8 bits sin entrelazar; máximo 4 megapíxeles')}
  if(name==='IDAT')parts.push(p);if(name==='IEND'){ended=true;break}k+=12+n;
 }
 if(!width||!ended||!parts.length)throw Error('PNG incompleto');
 const line=width*stride,expected=(line+1)*height;
 const raw=await readBounded(new Blob(parts).stream().pipeThrough(new DecompressionStream('deflate')),expected);
 if(raw.length!==expected)throw Error('PNG incompleto');
 const pixels=new Uint8Array(line*height);let k=0;
 for(let y=0;y<height;y++){const filter=raw[k++];if(filter>4)throw Error('Filtro PNG inválido');
  for(let j=0;j<line;j++){const i=y*line+j,a=j>=stride?pixels[i-stride]:0,b=y?pixels[i-line]:0,c=y&&j>=stride?pixels[i-line-stride]:0;
   let f=0;if(filter===1)f=a;if(filter===2)f=b;if(filter===3)f=(a+b)>>1;if(filter===4){const p=a+b-c,da=Math.abs(p-a),db=Math.abs(p-b),dc=Math.abs(p-c);f=da<=db&&da<=dc?a:db<=dc?b:c}
   pixels[i]=raw[k++]+f;
  }
 }
 return {pixels,width,height};
}

