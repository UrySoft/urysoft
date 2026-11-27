import {packText,unpackText} from './text.mjs';
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
function layout(level,count=690,compact=false){
 if(![1,2,3].includes(level))throw Error('Nivel desconocido');
 const size=Math.floor(count*bits[level]/8);
 const blocks=compact?Math.max(1,Math.ceil(size/255)):6;
 return Array.from({length:blocks},(_,i)=>Math.floor((size+blocks-1-i)/blocks));
}
function readBits(bytes,start,n){let v=0;for(let i=start;i<start+n;i++)v=v*2+((bytes[i>>3]>>(7-i%8))&1);return v}
function writeBits(bytes,start,n,v){for(let i=0;i<n;i++){const k=start+i;bytes[k>>3]|=((v>>(n-1-i))&1)<<(7-k%8)}}
export function encodeCells(input,level=3,side=0,rows=side,fileGrid=false,packed=null){
 const count=side?side*rows:690;
 const lengths=layout(level,count,!!side),p=packed?textParity(lengths,level):parity[level];if(!(input instanceof Uint8Array)||(packed?packed.bytes.length:input.length)>limits[level])throw Error('Fichero demasiado grande o inválido');
 const frame=new Uint8Array(lengths.reduce((a,b)=>a+b-p,0));
 if(packed){
  const view=new DataView(frame.buffer);view.setUint16(0,(packed.mode<<11)|packed.bytes.length);view.setUint32(2,crc32(input));frame.set(packed.bytes,6);
 }else{frame.set([85,83,89,49,level,fileGrid?3:(side?2:1),input.length>>8,input.length&255]);new DataView(frame.buffer).setUint32(8,crc32(input));frame.set(input,12)}
 let off=0;const blocks=lengths.map(n=>{const a=rsEncode(frame.subarray(off,off+n-p),p);off+=n-p;return a});
 const stream=[];for(let j=0;j<lengths[0];j++)for(const b of blocks)if(j<b.length)stream.push(b[j]);
 return Uint16Array.from({length:count},(_,i)=>readBits(stream,i*bits[level],bits[level]));
}
export function decodeCells(cells,level,side=0,rows=side,packed=false){
 const count=side?side*rows:690;
 const lengths=layout(level,count,!!side),p=packed?textParity(lengths,level):parity[level];if(cells.length!==count)throw Error('Cuadrícula incorrecta');
 const stream=new Uint8Array(Math.ceil(count*bits[level]/8));
 cells.forEach((v,i)=>writeBits(stream,i*bits[level],bits[level],v));
 let off=0,corrected=0;const blocks=lengths.map(n=>new Uint8Array(n));
 for(let j=0;j<lengths[0];j++)for(const b of blocks)if(j<b.length)b[j]=stream[off++];
 const bytes=[];for(const b of blocks){corrected+=rsRepair(b,p);bytes.push(...b.subarray(0,b.length-p))}
 const frame=Uint8Array.from(bytes),len=frame[6]*256+frame[7];
 if(packed){
  const view=new DataView(frame.buffer),tag=view.getUint16(0),length=tag&2047;
  if(length>frame.length-6||length>limits[level])throw Error('Longitud inválida');
  const data=unpackText(tag>>>11,frame.subarray(6,6+length));
  if(crc32(data)!==view.getUint32(2))throw Error('Texto dañado');
  return {data,level,corrected,kind:'text'};
 }
 if(frame.slice(0,6).join()!==[85,83,89,49,level,side?(frame[5]===3?3:2):1].join()||len>frame.length-12||len>limits[level])throw Error('Cabecera inválida');
 const data=frame.slice(12,12+len);if(crc32(data)!==new DataView(frame.buffer).getUint32(8))throw Error('Integridad incorrecta');
 return {data,level,corrected,kind:frame[5]===2?"text":"file"};
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
export function textSide(bytes,level){
 if(!Number.isInteger(bytes)||bytes<1||bytes>limits[level])throw Error('Longitud de texto fuera de límite');
 for(let side=4;side<=32;side++){
  const blocks=layout(level,side*side,true);
  if(blocks.reduce((sum,n)=>sum+n-parity[level],0)>=12+bytes)return side;
 }
 throw Error('Texto demasiado grande');
}
export function gridSize(bytes,level,text,axis='auto',value){
 if(!Number.isInteger(bytes)||bytes<0||bytes>limits[level])throw Error('Contenido fuera del límite del nivel.');
 if(axis==='auto'){
  if(!text)return {columns:23,rows:30};
  const side=textSide(bytes,level);return {columns:side,rows:side};
 }
 if(!['columns','rows'].includes(axis)||!Number.isInteger(value)||value<1||value>60)throw Error('Elige un número entero entre 1 y 60.');
 for(let other=1;other<=60;other++){
  const blocks=layout(level,value*other,true);
  if(blocks.reduce((sum,n)=>sum+n-parity[level],0)>=12+bytes)
   return axis==='columns'?{columns:value,rows:other}:{columns:other,rows:value};
 }
 throw Error('El contenido necesita más de 60 casillas en la otra dimensión. Aumenta la dimensión fija o el nivel.');
}
function textParity(lengths,level){
 // Short blocks retain four repair bytes, longer ones use the existing level budget.
 return lengths[0]<=40?4:parity[level];
}
export function planText(input,level,axis='auto',value){
 const packed=packText(input);
 if(packed.bytes.length>limits[level])throw Error('El texto comprimido supera los '+limits[level]+' bytes de este nivel.');
 if(axis!=='auto'&&(!['columns','rows'].includes(axis)||!Number.isInteger(value)||value<1||value>60))throw Error('Elige un número entero entre 1 y 60.');
 let best,area=Infinity;
 for(let columns=1;columns<=60;columns++){
  if(axis==='columns'&&columns!==value)continue;
  for(let rows=axis==='rows'?value:1;rows<=60;rows++){
   const candidate=(columns+3)*(rows+3);
   if(candidate>area)break;
   const count=columns*rows,size=Math.floor(count*bits[level]/8);
   if(size>=packed.bytes.length+10){
    const lengths=layout(level,count,true);
    if(size-lengths.length*textParity(lengths,level)>=packed.bytes.length+6){
     if(candidate<area||Math.abs(rows-columns)<Math.abs(best.rows-best.columns)){
      best={columns,rows,packed};area=candidate;
     }
     break;
    }
   }
   if(axis==='rows')break;
  }
 }
 if(!best)throw Error('Aumenta la dimensión fija o el nivel.');
 return best;
}
// Dimension tags occupy the central half of the distance between corner markers.
function tagPosition(i,span){return 32+span*(.25+i/24)}
export function render(input,level=3,text=false,axis='auto',value,marks=true){
 const fixed=text||axis!=='auto'||!marks,compact=fixed;
 const grid=text?planText(input,level,axis,value):gridSize(input.length,level,false,axis,value);
 const side=compact?grid.columns:0,cols=grid.columns+2,rows=grid.rows+2;
 const width=!marks?grid.columns*32:compact?(cols+1)*32:840;
 const height=!marks?grid.rows*32:compact?(rows+1)*32:1188;
 const left=!marks?-32:compact?16:20,top=!marks?-32:compact?16:32;
 const cells=encodeCells(input,level,side,grid.rows,fixed&&!text,grid.packed),pixels=new Uint8Array(width*height*3).fill(255);
 function rect(x,y,w,h,color){for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++){const k=3*(j*width+i);pixels[k]=color[0];pixels[k+1]=color[1];pixels[k+2]=color[2]}}
 const inset=marks?0:1;
 for(let r=inset;r<rows-inset;r++)for(let c=inset;c<cols-inset;c++){
  const x=left+c*32,y=top+r*32;
  if((r===0||r===rows-1)&&(c===0||c===cols-1)){for(const [s,v] of [[28,black],[20,white],[12,black]])rect(x+(32-s)/2,y+(32-s)/2,s,s,v);continue}
  if(!r||r===rows-1||!c||c===cols-1){if(compact)continue;rect(x+3,y+3,26,26,level===1?((r+c)%2?black:white):palette[(r+c-1+16)%16]);continue}
  const [mask,ink,bg]=unpack(cells[(r-1)*(cols-2)+c-1],level);rect(x+1,y+1,30,30,bg);
  for(let j=0;j<16;j++)if(mask>>j&1)rect(x+4+6*(j%4),y+4+6*(j>>2),6,6,ink);
 }
 if(!marks)return {width,height,pixels};
 if(compact){
  // Fixed strips between the finders carry the dimension and colour references.
  const tag=0xA80|side,span=side*32;
  if(fixed){
   const sx=(grid.columns+1)*32,sy=(grid.rows+1)*32;
   for(let i=0;i<12;i++){
    const mask=1<<(11-i);
    const x=Math.round(tagPosition(i,sx)),y=Math.round(tagPosition(i,sy));
    if(((text?0xE40:0xD40)|grid.columns)&mask)rect(x,26,Math.round(tagPosition(i+1,sx))-x,12,black);
    if((0xB40|grid.rows)&mask)rect(26,y,12,Math.round(tagPosition(i+1,sy))-y,black);
   }
  }else for(let i=0;i<12;i++)if(tag & (1<<(11-i)))rect(48+Math.floor(i*span/12),26,Math.floor((i+1)*span/12)-Math.floor(i*span/12),12,black);
  if(level>1)for(let i=0;i<16;i++)rect(48+i*span/16,top+(rows-1)*32+8,span/16,16,palette[i]);
  return {width,height,pixels};
 }
 // QR: https://urysoft.pages.dev/simbolos/#descodificar
 const footer=["111111101011010011110001001111111","100000101010010010100001101000001","101110100011000001000001101011101","101110101110001010001011101011101","101110100010101000001000101011101","100000100110100001111100001000001","111111101010101010101010101111111","000000001000001001001000100000000","101101110100101011111100001001011","000100010011100010111111001101111","110011101001001011000101101111011","011110000100111010111001001101001","010110111010000000100010110011011","100001010101010011100000100101010","011101110100111110100000101111100","001111010011011010111100111000100","011110100111100100111100111011100","000010010101001101001011101011011","100000110110100111001100000110110","011111011010001100010011010010001","010011100000100000111101000001101","110010001011011100100011001001001","000010101001011011100111011000111","010110011110001010110010100110010","100101100010010111000010111110000","000000001111110111110010100011000","111111101010001000011111101010000","100000101100000010000101100011111","101110100001110101001001111110110","101110101010101000111010100100001","101110101101100001010100111101000","100000100110101111010001110110001","111111101001101111000101001010100"];
 footer.forEach((row,y)=>[...row].forEach((bit,x)=>{if(bit==="1")rect(28+x*3,1080+y*3,3,3,black)}));
 return {width,height,pixels};
}
function projector(quad,left=36,top=48,dxRef=768,dyRef=992){
 const [[x0,y0],[x1,y1],[x2,y2],[x3,y3]]=quad;
 const dx=x0-x1+x2-x3,dy=y0-y1+y2-y3,den=(x1-x2)*(y3-y2)-(x3-x2)*(y1-y2);
 if(!Number.isFinite(den)||Math.abs(den)<1)throw Error('Esquinas inválidas');
 const g=(dx*(y3-y2)-(x3-x2)*dy)/den,h=((x1-x2)*dy-dx*(y1-y2))/den;
 return (x,y)=>{const u=(x-left)/dxRef,v=(y-top)/dyRef,d=1+g*u+h*v;
 return [Math.round((x0+(x1-x0+g*x1)*u+(x3-x0+h*x3)*v)/d),Math.round((y0+(y1-y0+g*y1)*u+(y3-y0+h*y3)*v)/d)]};
}
export function readImage(img,points){
 const {width:w,height:h,pixels}=img,stride=pixels.length/(w*h);if(![3,4].includes(stride))throw Error('Formato de píxel inválido');
 function scan(side,rows=side,rectQuad=null,packed=false,bare=false){
 const compact=side>0,span=(side+1)*32,extent=(side+3)*32;
 const quad=rectQuad||points||(compact?[[32,32],[32+span,32],[32+span,32+span],[32,32+span]].map(([x,y])=>[x*w/extent,y*h/extent]):corners.map(([x,y])=>[x*w/width,y*h/height]));
 if(quad.length!==4||quad.some(p=>!Array.isArray(p)||p.length!==2||p.some(k=>!Number.isFinite(k))))throw Error('Se requieren cuatro centros');
 for(let rotation=0;rotation<(rectQuad||bare?1:4);rotation++){
 const rotated=quad.map((_,i)=>quad[(i+rotation)%4]);
 const project=bare?((x,y)=>[x-48,y-48]):compact?projector(rotated,32,32,span,(rows+1)*32):projector(rotated);
 function sample(x,y){const [px,py]=project(x,y);if(px<0||px>=w||py<0||py>=h)throw Error('Esquina fuera de imagen');const k=(py*w+px)*stride;return [pixels[k],pixels[k+1],pixels[k+2]]}
 if(compact&&!rectQuad&&!bare){
  let tag=0;
  for(let i=0;i<12;i++)tag=tag*2+Number(sample(48+(i+.5)*side*32/12,32).reduce((a,b)=>a+b,0)<384);
  if(tag!==(0xA80|side))continue;
 }
 const ref=bare?palette:compact?Array.from({length:16},(_,i)=>sample(48+(i+.5)*side*2,32+(rows+1)*32)):Array.from({length:16},(_,i)=>sample(20+(i+1)*32+16,48));
 for(const level of [1,2,3]){
  try{
  const cells=[];
  for(let r=1;r<=(rows||30);r++)for(let c=1;c<=(side||23);c++){
   const x=(compact?16:20)+32*c,y=(compact?16:32)+32*r,samples=[];
   const background=[[3,3],[29,3],[3,29],[29,29]].map(([dx,dy])=>sample(x+dx,y+dy));
   const bg=background[0].map((_,i)=>{
    const channel=background.map(rgb=>rgb[i]).sort((a,b)=>a-b);
    return (channel[1]+channel[2])/2;
   });
   for(let j=0;j<16;j++)samples.push(sample(x+7+6*(j%4),y+7+6*(j>>2)));
   let ink=samples[0];for(const s of samples)if(distance(s,bg)>distance(ink,bg))ink=s;
   let mask=0;for(let i=0;i<16;i++)if(distance(samples[i],ink)<distance(samples[i],bg))mask|=1<<i;
   let shape=0,minimum=17;
   for(let i=0;i<64;i++){let z=mask^shapes[i],n=0;while(z){z&=z-1;n++}if(n<minimum){shape=i;minimum=n}}
   function glyphColor(on,fallback){
    const colors=samples.filter((_,index)=>((shapes[shape]>>index)&1)===on);
    if(!colors.length)return fallback;
    return [0,1,2].map(ch=>colors.map(rgb=>rgb[ch]).sort((a,b)=>a-b)[colors.length>>1]);
   }
   const fg=level===1?0:nearest(packed?glyphColor(1,ink):ink,ref),back=level===3?nearest(packed?glyphColor(0,bg):bg,ref):0;
   cells.push(shape+(fg<<6)+(level===3?(back%8)<<10:0));
  }
  return decodeCells(cells,level,side,rows,packed);
  }catch(e){/* The verified frame selects the rotation and profile. */}
 }}
 }
 // A native digital grid has exactly 32 pixels per cell and uses the known palette.
 if(!points&&w%32===0&&h%32===0&&w<=60*32&&h<=60*32){
  for(const packed of [true,false]){
   const result=scan(w/32,h/32,null,packed,true);
   if(result)return result;
  }
 }
 // The original export has a one-cell inset to each marker centre.
 const frame=points||[[32,32],[w-32,32],[w-32,h-32],[32,h-32]];
 if(frame.length===4)for(let turn=0;turn<4;turn++){
  const quad=frame.map((_,i)=>frame[(i+turn)%4]);
  try{
   const project=projector(quad,0,0,1,1);
   function tag(vertical){
    let value=0;
    for(let i=0;i<12;i++){
     const t=.25+(i+.5)/24;
     const [x,y]=project(vertical?0:t,vertical?t:0);
     if(x<0||y<0||x>=w||y>=h)return 0;
     const k=(y*w+x)*stride;
     value=value*2+Number(pixels[k]+pixels[k+1]+pixels[k+2]<384);
    }
    return value;
   }
   const top=tag(false),left=tag(true),cols=top&63,rows=left&63;
   const packed=(top&0xFC0)===0xE40;
   if((packed||(top&0xFC0)===0xD40)&&(left&0xFC0)===0xB40&&cols>=1&&cols<=60&&rows>=1&&rows<=60){
    const result=scan(cols,rows,quad,packed);if(result)return result;
   }
  }catch{/* Try the next orientation or the legacy page geometry. */}
 }
 const order=Array.from({length:29},(_,i)=>i+4).sort((a,b)=>Math.abs((a+3)*32-w)-Math.abs((b+3)*32-w));
 if(!points&&Math.abs(w/h-width/height)<.04)order.unshift(0);else order.push(0);
 for(const side of order){try{const result=scan(side);if(result)return result}catch{/* Try another geometry. */}}
 throw Error('No se pudo verificar el archivo. Prueba con el PNG original o con una captura de pantalla que incluya toda la cuadrícula.');
}
