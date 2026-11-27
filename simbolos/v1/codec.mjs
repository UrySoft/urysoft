// UrySoft Símbolos v1. Pure transforms shared by HTTP, browser and Android.
export const glyphs=[25715,4623,13405,14601,21395,23788,49932,54128,24113,11730,62516,40839,64018,57240,7190,30605];
export const palette=['111111','341520','192d13','13253b','35163a','163130','3a260b','2c2245','c35b67','b07613','73822b','238e64','208bb2','7872bd','aa69a0','9e7967'].map(h=>[0,2,4].map(i=>parseInt(h.slice(i,i+2),16)));
const black=[0,0,0], white=[255,255,255],bits=[0,6,10,13], parity=[0,6,10,12];
export const limits=[0,469,790,1024];
const exp=new Uint8Array(510),log=new Uint8Array(256);
for(let i=0,v=1;i<255;i++){exp[i]=exp[i+255]=v;log[v]=i;v<<=1;if(v&256)v^=285;}
const mul=(a,b)=>a&&b?exp[log[a]+log[b]]:0;
const div=(a,b)=>{if(!b)throw Error('Bloque ilegible');return a?exp[log[a]+255-log[b]]:0};
function evaluate(poly,x){let v=0;for(const p of poly)v=mul(v,x)^p;return v;}
function rsEncode(data,n){
 let g=[1];for(let i=0;i<n;i++){const t=new Uint8Array(g.length+1);g.forEach((v,j)=>{t[j]^=v;t[j+1]^=mul(v,exp[i])});g=t}
 const out=new Uint8Array(data.length+n);out.set(data);
 for(let i=0;i<data.length;i++){const f=out[i];for(let j=0;j<g.length;j++)out[i+j]^=mul(f,g[j])}
 out.set(data);return out;
}
function rsRepair(block,n){
 const s=Array.from({length:n},(_,i)=>evaluate(block,exp[i]));if(s.every(v=>!v))return 0;
 let c=[1],b=[1],degree=0,delay=1,last=1;
 for(let k=0;k<n;k++){
  let d=s[k];for(let j=1;j<=degree;j++)d^=mul(c[j]||0,s[k-j]);
  if(!d){delay++;continue}
  const old=c.slice(),scale=div(d,last);
  for(let j=0;j<b.length;j++)c[j+delay]=(c[j+delay]||0)^mul(scale,b[j]);
  if(2*degree<=k){degree=k+1-degree;b=old;last=d;delay=1}else delay++;
 }
 if(degree>n/2)throw Error('Demasiados errores');
 const positions=[],xs=[];
 for(let i=0;i<block.length;i++){const x=exp[block.length-1-i];if(evaluate([...c].reverse(),div(1,x))===0){positions.push(i);xs.push(x)}}
 if(positions.length!==degree)throw Error('Bloque ilegible');
 const m=Array.from({length:degree},(_,r)=>xs.map(x=>exp[(r*log[x])%255]).concat(s[r]));
 for(let i=0;i<degree;i++){
  const pivot=m.findIndex((row,k)=>k>=i&&row[i]);if(pivot<0)throw Error('Bloque ilegible');
  [m[i],m[pivot]]=[m[pivot],m[i]];const factor=m[i][i];
  for(let j=i;j<=degree;j++)m[i][j]=div(m[i][j],factor);
  for(let k=0;k<degree;k++)if(k!==i){const a=m[k][i];for(let j=i;j<=degree;j++)m[k][j]^=mul(a,m[i][j])}
 }
 positions.forEach((p,i)=>block[p]^=m[i][degree]);
 if(s.some((_,i)=>evaluate(block,exp[i])))throw Error('Corrección incompleta');return degree;
}
export function crc32(bytes){let c=-1;for(const v of bytes){c^=v;for(let j=0;j<8;j++)c=(c>>>1)^((c&1)?0xedb88320:0)}return (~c)>>>0}
function layout(level){
 if(![1,2,3].includes(level))throw Error('Nivel desconocido');
 const size=Math.floor(690*bits[level]/8);
 return Array.from({length:6},(_,i)=>Math.floor((size+5-i)/6));
}
function readBits(bytes,start,n){let v=0;for(let i=start;i<start+n;i++)v=v*2+((bytes[i>>3]>>(7-i%8))&1);return v}
function writeBits(bytes,start,n,v){for(let i=0;i<n;i++){const k=start+i;bytes[k>>3]|=((v>>(n-1-i))&1)<<(7-k%8)}}
export function encodeCells(input,level=3){
 const lengths=layout(level),p=parity[level];if(!(input instanceof Uint8Array)||input.length>limits[level])throw Error('Fichero demasiado grande o inválido');
 const frame=new Uint8Array(lengths.reduce((a,b)=>a+b-p,0));
 frame.set([85,83,89,49,level,1,input.length>>8,input.length&255]);new DataView(frame.buffer).setUint32(8,crc32(input));frame.set(input,12);
 let off=0;const blocks=lengths.map(n=>{const a=rsEncode(frame.subarray(off,off+n-p),p);off+=n-p;return a});
 const stream=[];for(let j=0;j<lengths[0];j++)for(const b of blocks)if(j<b.length)stream.push(b[j]);
 return Uint16Array.from({length:690},(_,i)=>readBits(stream,i*bits[level],bits[level]));
}
export function decodeCells(cells,level){
 const lengths=layout(level),p=parity[level];if(cells.length!==690)throw Error('Cuadrícula incorrecta');
 const stream=new Uint8Array(Math.ceil(690*bits[level]/8));
 cells.forEach((v,i)=>writeBits(stream,i*bits[level],bits[level],v));
 let off=0,corrected=0;const blocks=lengths.map(n=>new Uint8Array(n));
 for(let j=0;j<lengths[0];j++)for(const b of blocks)if(j<b.length)b[j]=stream[off++];
 const bytes=[];for(const b of blocks){corrected+=rsRepair(b,p);bytes.push(...b.subarray(0,b.length-p))}
 const frame=Uint8Array.from(bytes),len=frame[6]*256+frame[7];
 if(frame.slice(0,6).join()!==[85,83,89,49,level,1].join()||len>limits[level])throw Error('Cabecera inválida');
 const data=frame.slice(12,12+len);if(crc32(data)!==new DataView(frame.buffer).getUint32(8))throw Error('Integridad incorrecta');
 return {data,level,corrected};
}
function rotate(m){let t=0;for(let y=0;y<4;y++)for(let x=0;x<4;x++)t|=((m>>(4*y+x))&1)<<(4*(3-x)+y);return t}
const shapes=glyphs.flatMap(m=>[m,rotate(m),rotate(rotate(m)),rotate(rotate(rotate(m)))]);
const distance=(a,b)=>(a[0]-b[0])**2+(a[1]-b[1])**2+(a[2]-b[2])**2;
function nearest(color,pal){let best=0;for(let i=1;i<pal.length;i++)if(distance(color,pal[i])<distance(color,pal[best]))best=i;return best}
function unpack(v,l){const ink=l===1?0:(v>>6)&15;
 return [shapes[v&63],l===1?black:palette[ink],l<3?white:palette[(ink<8?8:0)+(v>>10)]];
}
// A4 at 4 pixels/mm. Finder centres are expressed in source pixels.
export const width=840,height=1188;
export const corners=[[36,48],[804,48],[804,1040],[36,1040]];
export function render(input,level=3){
 const cells=encodeCells(input,level),pixels=new Uint8Array(width*height*3).fill(255);
 function rect(x,y,w,h,color){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++){const k=3*(j*width+i);pixels[k]=color[0];pixels[k+1]=color[1];pixels[k+2]=color[2]}}
 for(let r=0;r<32;r++)for(let c=0;c<25;c++){
  const x=20+c*32,y=32+r*32;
  if((r===0||r===31)&&(c===0||c===24)){for(const [s,v] of [[28,black],[20,white],[12,black]])rect(x+(32-s)/2,y+(32-s)/2,s,s,v);continue}
  if(!r||r===31||!c||c===24){rect(x+3,y+3,26,26,level===1?((r+c)%2?black:white):palette[(r+c-1+16)%16]);continue}
  const [mask,ink,bg]=unpack(cells[(r-1)*23+c-1],level);rect(x+1,y+1,30,30,bg);
  for(let j=0;j<16;j++)if(mask>>j&1)rect(x+4+6*(j%4),y+4+6*(j>>2),6,6,ink);
 }
 // QR: https://urysoft.pages.dev/simbolos/#descodificar
 const footer=["111111101011010011110001001111111","100000101010010010100001101000001","101110100011000001000001101011101","101110101110001010001011101011101","101110100010101000001000101011101","100000100110100001111100001000001","111111101010101010101010101111111","000000001000001001001000100000000","101101110100101011111100001001011","000100010011100010111111001101111","110011101001001011000101101111011","011110000100111010111001001101001","010110111010000000100010110011011","100001010101010011100000100101010","011101110100111110100000101111100","001111010011011010111100111000100","011110100111100100111100111011100","000010010101001101001011101011011","100000110110100111001100000110110","011111011010001100010011010010001","010011100000100000111101000001101","110010001011011100100011001001001","000010101001011011100111011000111","010110011110001010110010100110010","100101100010010111000010111110000","000000001111110111110010100011000","111111101010001000011111101010000","100000101100000010000101100011111","101110100001110101001001111110110","101110101010101000111010100100001","101110101101100001010100111101000","100000100110101111010001110110001","111111101001101111000101001010100"];
 footer.forEach((row,y)=>[...row].forEach((bit,x)=>{if(bit==="1")rect(28+x*3,1080+y*3,3,3,black)}));
 return {width,height,pixels};
}
function projector(quad){
 const [[x0,y0],[x1,y1],[x2,y2],[x3,y3]]=quad;
 const dx=x0-x1+x2-x3,dy=y0-y1+y2-y3,den=(x1-x2)*(y3-y2)-(x3-x2)*(y1-y2);
 if(!Number.isFinite(den)||Math.abs(den)<1)throw Error('Esquinas inválidas');
 const g=(dx*(y3-y2)-(x3-x2)*dy)/den,h=((x1-x2)*dy-dx*(y1-y2))/den;
 return (x,y)=>{const u=(x-36)/768,v=(y-48)/992,d=1+g*u+h*v;
 return [Math.round((x0+(x1-x0+g*x1)*u+(x3-x0+h*x3)*v)/d),Math.round((y0+(y1-y0+g*y1)*u+(y3-y0+h*y3)*v)/d)]};
}
export function readImage(img,points){
 const {width:w,height:h,pixels}=img,stride=pixels.length/(w*h);if(![3,4].includes(stride))throw Error('Formato de píxel inválido');
 const quad=points||corners.map(([x,y])=>[x*w/width,y*h/height]);
 if(quad.length!==4||quad.some(p=>!Array.isArray(p)||p.length!==2||p.some(k=>!Number.isFinite(k))))throw Error('Se requieren cuatro centros');
 for(let rotation=0;rotation<4;rotation++){
 const project=projector(quad.map((_,i)=>quad[(i+rotation)%4]));
 function sample(x,y){const [px,py]=project(x,y);if(px<0||px>=w||py<0||py>=h)throw Error('Esquina fuera de imagen');const k=(py*w+px)*stride;return [pixels[k],pixels[k+1],pixels[k+2]]}
 const ref=Array.from({length:16},(_,i)=>sample(20+(i+1)*32+16,48));
 for(const level of [1,2,3]){
  try{
  const cells=[];
  for(let r=1;r<=30;r++)for(let c=1;c<=23;c++){
   const x=20+32*c,y=32+32*r,bg=sample(x+2,y+2),samples=[];
   for(let j=0;j<16;j++)samples.push(sample(x+7+6*(j%4),y+7+6*(j>>2)));
   let ink=samples[0];for(const s of samples)if(distance(s,bg)>distance(ink,bg))ink=s;
   let mask=0;for(let i=0;i<16;i++)if(distance(samples[i],ink)<distance(samples[i],bg))mask|=1<<i;
   let shape=0,minimum=17;
   for(let i=0;i<64;i++){let z=mask^shapes[i],n=0;while(z){z&=z-1;n++}if(n<minimum){shape=i;minimum=n}}
   const fg=level===1?0:nearest(ink,ref),back=level===3?nearest(bg,ref):0;
   cells.push(shape+(fg<<6)+(level===3?(back%8)<<10:0));
  }
  return decodeCells(cells,level);
  }catch(e){/* The verified frame selects the rotation and profile. */}
 }}
 throw Error('Página ilegible. Comprueba los cuatro centros o utiliza la imagen original.');
}

