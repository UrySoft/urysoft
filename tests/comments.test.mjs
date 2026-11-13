import {readFileSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import assert from 'node:assert/strict';
const {onRequest}=await import('data:text/javascript;base64,'+Buffer.from(readFileSync('functions/api/comments/[[path]].js','utf8')).toString('base64'));
const db=new DatabaseSync(':memory:');db.exec(readFileSync('migrations/0001-comments.sql','utf8'));
function prepared(sql,values=[]){return {
 bind(...args){return prepared(sql,args)},
 first:async()=>db.prepare(sql).get(...values),
 all:async()=>({results:db.prepare(sql).all(...values)}),
 run:async()=>{const r=db.prepare(sql).run(...values);return {meta:{last_row_id:Number(r.lastInsertRowid)}}}
}}
const env={COMMENTS_ADMIN_KEY:'test-only-key',COMMENTS_DB:{prepare:prepared}};
async function call(method,body,admin=false,key='test-only-key'){return onRequest({env,params:{path:admin?['admin']:[]},request:new Request('https://example.com/api/comments'+(admin?'/admin':'?app=lumen'),{method,headers:{Origin:'https://example.com','Content-Type':'application/json',Authorization:'Bearer '+key},...(body?{body:JSON.stringify(body)}:{})})})}
const message={name:'Tester',kind:'idea',rating:4,message:'Una propuesta de prueba <script>alert(1)</script>',consent:true,website:''};
assert.equal((await onRequest({env:{},params:{path:[]},request:new Request('https://example.com/api/comments?app=lumen')})).status,503);
assert.equal((await call('POST',{...message,consent:false})).status,400);
assert.equal((await call('POST',message)).status,201);
assert.deepEqual(await (await call('GET')).json(),[]);
assert.equal((await call('GET',null,true,'wrong')).status,401);
const pending=await (await call('GET',null,true)).json();assert.equal(pending[0].status,'pending');
assert.equal((await call('PATCH',{id:pending[0].id,status:'approved',reply:'Gracias por tu propuesta.'},true)).status,200);
const published=await (await call('GET')).json();assert.equal(published.length,1);assert.equal(published[0].reply,'Gracias por tu propuesta.');assert.equal(published[0].message,message.message);
await call('PATCH',{id:pending[0].id,status:'hidden',reply:''},true);assert.deepEqual(await (await call('GET')).json(),[]);
for(let i=0;i<4;i++)assert.equal((await call('POST',message)).status,201);
assert.equal((await call('POST',message)).status,429);
console.log('Passed: unavailable configuration, validation, private pending messages, admin authentication, approval, reply, hiding and submission limit.');
