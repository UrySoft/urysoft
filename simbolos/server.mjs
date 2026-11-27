import http from 'node:http';
import {Readable} from 'node:stream';
import {handle} from './v1/http.mjs';
const host=process.env.HOST||'127.0.0.1',port=Number(process.env.PORT||8787);
http.createServer(async(req,res)=>{
 try{
 const request=new Request('http://localhost'+req.url,{method:req.method,headers:req.headers,...(['GET','HEAD'].includes(req.method)?{}:{body:Readable.toWeb(req),duplex:'half'})});
 const result=await handle(request);res.writeHead(result.status,Object.fromEntries(result.headers));res.end(Buffer.from(await result.arrayBuffer()));
 }catch{res.writeHead(400);res.end()}
}).listen(port,host,()=>console.log('UrySoft Símbolos: http://'+host+':'+port+'/api/simbolos/v1'));

