// Text representation codes are permanent: 0 = literals, 1 = LZ tokens,
// 2 onwards restore an exact ASCII prefix and optional suffix.
export const maxTextBytes=65536;
const prefixes=['https://','http://','https://www.','http://www.'];
const suffixes=['','.com','.org','.es'];
const encoder=new TextEncoder();
const decoder=new TextDecoder('utf-8',{fatal:true,ignoreBOM:true});

function compress(input){
 const out=[],positions=new Map();
 const key=i=>(input[i]<<16)|(input[i+1]<<8)|input[i+2];
 let i=0;
 while(i<input.length){
  const flags=out.length;out.push(0);
  for(let bit=0;bit<8&&i<input.length;bit++){
   const start=i,prior=positions.get(key(i));
   let length=0;
   if(prior!==undefined&&i-prior<=65535){
    while(length<258&&i+length<input.length&&input[prior+length]===input[i+length])length++;
   }
   if(length>=4){
    const distance=i-prior;
    out[flags]|=1<<bit;out.push(distance>>8,distance&255,length-3);i+=length;
   }else out.push(input[i++]);
   for(let j=start;j<i&&j+2<input.length;j++)positions.set(key(j),j);
  }
 }
 return Uint8Array.from(out);
}

export function packText(input){
 if(!input.length||input.length>maxTextBytes)throw Error('El texto debe ocupar entre 1 y 65.536 bytes.');
 const value=decoder.decode(input);
 let best={mode:0,bytes:input};
 function consider(mode,bytes){if(bytes.length<best.bytes.length)best={mode,bytes}}
 prefixes.forEach((prefix,p)=>suffixes.forEach((suffix,s)=>{
  if(value.startsWith(prefix)&&value.endsWith(suffix)&&value.length>=prefix.length+suffix.length)
   consider(2+p*4+s,encoder.encode(value.slice(prefix.length,value.length-suffix.length)));
 }));
 consider(1,compress(input));
 return best;
}

export function unpackText(mode,bytes){
 if(mode===0)return bytes;
 if(mode>=2&&mode<18){
  const prefix=prefixes[(mode-2)>>2],suffix=suffixes[(mode-2)%4];
  return encoder.encode(prefix+decoder.decode(bytes)+suffix);
 }
 if(mode!==1)throw Error('Modo de texto desconocido');
 const out=[];
 for(let i=0;i<bytes.length;){
  const flags=bytes[i++];
  for(let bit=0;bit<8&&i<bytes.length;bit++){
   if(flags&(1<<bit)){
    if(i+3>bytes.length)throw Error('Texto truncado');
    const distance=bytes[i++]*256+bytes[i++],length=bytes[i++]+3;
    if(!distance||distance>out.length||out.length+length>maxTextBytes)throw Error('Referencia de texto inválida');
    for(let j=0;j<length;j++)out.push(out[out.length-distance]);
   }else{if(out.length===maxTextBytes)throw Error('Tamaño excesivo');out.push(bytes[i++])}
  }
 }
 return Uint8Array.from(out);
}
