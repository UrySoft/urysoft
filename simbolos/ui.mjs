import {render,readImage,limits} from './v1/codec.mjs';
import {findCorners} from './v1/finders.mjs';
const el=id=>document.getElementById(id),canvas=el('view'),ctx=canvas.getContext('2d',{willReadFrequently:true}),out=el('status');
const endpoint=location.protocol==='https:'&&!location.host.includes('appassets')?location.origin+'/api/simbolos/v1':'https://urysoft.pages.dev/api/simbolos/v1';
let sourcePNG=null;
const compactMode=()=>el('kind').value==='text';
function syncKind(){
 el('textBox').hidden=!compactMode();el('fileBox').hidden=compactMode();
 el('encode').textContent=compactMode()?'Crear imagen compacta':'Crear página';
 reset();
}
function showText(data){
 const value=new TextDecoder('utf-8',{fatal:true,ignoreBOM:true}).decode(data);
 el('restored').value=value;el('resultText').hidden=false;
 const link=el('open');link.hidden=true;link.removeAttribute('href');
 try{const url=new URL(value);if(['https:','http:'].includes(url.protocol)){link.href=url.href;link.hidden=false}}catch{/* Plain text. */}
}
el('copy').onclick=()=>run(async()=>{
 try{await navigator.clipboard.writeText(el('restored').value)}
 catch{el('restored').select();if(!document.execCommand('copy'))throw Error('Selecciona y copia el texto recuperado.')}
 out.textContent='Texto copiado.';
});
el('kind').onchange=syncKind;el('message').oninput=reset;
let output=null,filename='',image=null,points=null,busy=false;
function reset(){el('resultText').hidden=true;canvas.classList.remove('compact');sourcePNG=null;output=null;image=null;points=null;el('save').hidden=true;el('manual').hidden=true;canvas.hidden=true;out.textContent=''}
function setResult(blob,name){output=blob;filename=name;el('save').hidden=false;el('save').textContent='Guardar '+name}
async function run(fn){if(busy)return;busy=true;for(const id of ['encode','decode','manual','source','photo','level','mode','kind','message'])el(id).disabled=true;
 try{await fn()}catch(e){out.textContent=e.message}finally{busy=false;for(const id of ['encode','decode','manual','source','photo','level','mode','kind','message'])el(id).disabled=false}}
async function call(action,body,type){
 const res=await fetch(endpoint+'/'+action,{method:'POST',headers:{'Content-Type':type},body,signal:AbortSignal.timeout(30000)});
 if(!res.ok){const why=await res.json().catch(()=>({}));throw Error(why.error||'Servicio no disponible ('+res.status+')')}return res;
}
async function draw(blob){
 const bmp=await createImageBitmap(blob);try{const scale=Math.min(1,2048/Math.max(bmp.width,bmp.height),Math.sqrt(4000000/(bmp.width*bmp.height)));
 canvas.width=Math.max(1,Math.floor(bmp.width*scale));canvas.height=Math.max(1,Math.floor(bmp.height*scale));ctx.drawImage(bmp,0,0,canvas.width,canvas.height);canvas.hidden=false;
 }finally{bmp.close()}
}
async function uploadPNG(){
 if(sourcePNG)return sourcePNG;
 const rgb=new Uint8Array(image.width*image.height*3);
 for(let i=0,j=0;i<image.pixels.length;i+=4,j+=3)rgb.set(image.pixels.subarray(i,i+3),j);
 const {toPNG}=await import('./v1/png.mjs');
 return new Blob([await toPNG({...image,pixels:rgb})],{type:'image/png'});
}
async function create(){
 reset();const text=compactMode();const file=text?new Blob([el('message').value],{type:'text/plain'}):el('source').files[0],l=Number(el('level').value);if(!file)throw Error('Selecciona un archivo.');
 if(text&&!file.size)throw Error('Escribe un texto o un enlace.');
 if(file.size>limits[l])throw Error('Este nivel admite '+limits[l]+' bytes. El fichero tiene '+file.size+'.');
 let blob;if(el('mode').value==='web'){blob=await(await call('encode?level='+l,file,text?'text/plain;charset=utf-8':'application/octet-stream')).blob();await draw(blob)}
 else{const img=render(new Uint8Array(await file.arrayBuffer()),l,text);canvas.width=img.width;canvas.height=img.height;const d=ctx.createImageData(img.width,img.height);
 for(let i=0,j=0;i<img.pixels.length;i+=3,j+=4){d.data[j]=img.pixels[i];d.data[j+1]=img.pixels[i+1];d.data[j+2]=img.pixels[i+2];d.data[j+3]=255}ctx.putImageData(d,0,0);canvas.hidden=false;
 const {toPNG}=await import('./v1/png.mjs');blob=new Blob([await toPNG(img)],{type:'image/png'});}
 canvas.classList.toggle('compact',text);canvas.style.setProperty('--print-side',canvas.width/4+'mm');
 setResult(blob,'urysoft-'+(text?'texto-':'')+'nivel-'+l+'.png');out.textContent=file.size+' bytes · '+canvas.width+' × '+canvas.height+' píxeles.';
}
async function recover(markers){
 let data,level,corrected,kind;
 if(el('mode').value==='web'){
 const suffix=markers?'?corners='+encodeURIComponent(JSON.stringify(markers)):'';
 const res=await call('decode'+suffix,await uploadPNG(),'image/png');data=await res.arrayBuffer();kind=res.headers.get('X-UrySoft-Kind');level=res.headers.get('X-UrySoft-Level');corrected=res.headers.get('X-UrySoft-Corrected');
 }else{let result;try{result=readImage(image,markers)}catch(e){if(markers)throw e;for(const compact of [false,true]){const auto=findCorners(image,compact);if(auto){try{result=readImage(image,auto);break}catch{/* Try the other finder geometry. */}}}if(!result)throw e}
 ({data,level,corrected,kind}=result)}
 if(kind==='text')showText(data);
 else setResult(new Blob([data],{type:'application/octet-stream'}),'recuperado.bin');
 el('manual').hidden=true;out.textContent='Nivel '+level+' · '+data.byteLength+' bytes verificados · '+corrected+' bytes corregidos.';
}
async function loadPhoto(){
 reset();const file=el('photo').files[0];if(!file)throw Error('Selecciona una imagen.');await draw(file);
 image={width:canvas.width,height:canvas.height,pixels:ctx.getImageData(0,0,canvas.width,canvas.height).data};
 if(file.size<=12000000){
  const header=new Uint8Array(await file.slice(0,8).arrayBuffer());
  if(header.join(',')==='137,80,78,71,13,10,26,10'){
   const {fromPNG}=await import('./v1/png.mjs');
   try{
    const original=await fromPNG(new Uint8Array(await file.arrayBuffer()));
    if(original.width===canvas.width && original.height===canvas.height){image=original;sourcePNG=file}
   }catch{/* Other PNG formats use the browser-decoded pixels. */}
  }
 }
 el('manual').hidden=false;await recover(null);
}
const names=['superior izquierdo','superior derecho','inferior derecho','inferior izquierdo'];
el('encode').onclick=()=>run(create);el('decode').onclick=()=>run(loadPhoto);
el('manual').onclick=()=>{points=[];out.textContent='Toca el centro del marcador '+names[0]+'.'};
canvas.onclick=e=>{if(!points||busy)return;const rect=canvas.getBoundingClientRect();points.push([(e.clientX-rect.left)*canvas.width/rect.width,(e.clientY-rect.top)*canvas.height/rect.height]);
 if(points.length===4){const p=points;points=null;run(()=>recover(p))}else out.textContent='Toca el centro del marcador '+names[points.length]+'.'};
el('save').onclick=()=>run(async()=>{if(!output)return;
 if(window.UrySoftAndroid){const bytes=new Uint8Array(await output.arrayBuffer());let s='';for(let i=0;i<bytes.length;i+=8192)s+=String.fromCharCode(...bytes.subarray(i,i+8192));UrySoftAndroid.save(filename,output.type,btoa(s))}
 else{const a=document.createElement('a');a.href=URL.createObjectURL(output);a.download=filename;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),5000)}});
for(const id of ['source','photo','level','mode'])el(id).onchange=reset;
