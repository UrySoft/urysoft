import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import {analyticsSummary,normalizeEvent} from '../server/site-analytics.js';
import {operationsSummary} from '../server/admin-operations.js';
import {aggregate} from '../assets/admin/charts.js';
const sql=new DatabaseSync(':memory:');for(const file of ['0001-comments.sql','0005-administration.sql'])sql.exec(readFileSync('migrations/'+file,'utf8'));
function prepare(query,args=[]){return {bind(...args){return prepare(query,args)},first:async()=>sql.prepare(query).get(...args),all:async()=>({results:sql.prepare(query).all(...args)})}}
const db={prepare},today=new Date().toISOString().slice(0,10),day=n=>new Date(Date.parse(today)+n*86400000).toISOString().slice(0,10);
sql.prepare("UPDATE admin_meta SET value=? WHERE key='analytics_started'").run(day(-100)+'T12:00:00Z');
const insert=sql.prepare("INSERT INTO analytics_counts(day,path,event,target,country,referrer,device,browser,language,source,hits) VALUES(? ,? ,? ,'','ES','Directo','Móvil','Chrome','es','',?)");
for(const [offset,event,hits,path] of [[-5,'page',7,'/'],[-4,'download',2,'/'],[-2,'page',10,'/'],[0,'page',4,'/'],[0,'engaged',3,'/'],[0,'click',6,'/'],[0,'page',8,'/apps/minairo/']])insert.run(day(offset),path,event,hits);
const summary=(from,to,extra='')=>analyticsSummary(db,new URL('https://example.com/?from='+from+'&to='+to+extra));
let a=await summary(day(-2),today);assert.equal(a.daily.length,3);assert.equal(a.daily[1].pages,0);assert.equal(a.daily[2].pages,12);assert.equal(a.daily[2].engaged,3);assert.equal(a.daily[2].clicks,6);assert.equal(a.previous.from,day(-5));assert.equal(a.previous.to,day(-3));assert.equal(a.previous.totals.page,7);assert.equal(a.previous.totals.download,2);assert.equal(a.previous.complete,true);
a=await summary(day(-2),today,'&path=/');assert.equal(a.totals.page,14);assert.equal(a.previous.totals.page,7);
sql.prepare("UPDATE admin_meta SET value=? WHERE key='analytics_started'").run(day(-1)+'T12:00:00Z');
a=await summary(day(-2),today);assert.equal(a.daily[0].available,false);assert.equal(a.daily[0].pages,null);assert.equal(a.daily[1].pages,0);assert.equal(a.previous.complete,false);assert.equal(a.partial,true);
for(const [from,to,extra] of [['2026-02-30',today,''],[day(1),day(1),''],[day(-400),today,''],[today,today,'&path=/admin/'],[today,today,'&country=bad']])await assert.rejects(()=>summary(from,to,extra),e=>e.status===400);
assert.equal((await summary(today,today)).daily.length,1);
const rows=[{day:'2026-11-01',available:false,pages:null},{day:'2026-11-02',available:true,pages:5},{day:'2026-11-03',available:true,pages:3}];
const weekly=aggregate(rows,'week');assert.equal(weekly[0].day,'2026-10-26');assert.equal(weekly[0].pages,null);assert.equal(weekly[1].day,'2026-11-02');assert.equal(weekly[1].pages,8);
const monthly=aggregate(rows,'month');assert.equal(monthly[0].pages,8);assert.equal(monthly[0].partial,true);
assert.equal(normalizeEvent({id:crypto.randomUUID(),event:'download',path:'/apps/minairo/',target:'minairo'},new Request('https://example.com/',{headers:{'User-Agent':'Mozilla/5.0'}})).target,'minairo');
let ops=await operationsSummary({COMMENTS_DB:db});assert.equal(ops.services[0].ok,true);assert.equal(ops.services[1].ok,false);
const env={COMMENTS_DB:db,FOLDERY_LIVE_PAYMENTS:'enabled',FOLDERY_LIVE_SELLER_READY:'yes',FOLDERY_LIVE_GOOGLE_PUBLIC:'yes',FOLDERY_LIVE_DB:db};
for(const key of ['PAYPAL_CLIENT_ID','PAYPAL_CLIENT_SECRET','PAYPAL_PLAN_ID','PAYPAL_WEBHOOK_ID','GOOGLE_CLIENT_ID','LICENSE_PRIVATE_KEY'])env['FOLDERY_LIVE_'+key]='private-'+key;
ops=await operationsSummary(env);assert.equal(ops.services[1].ok,false);assert.ok(!JSON.stringify(ops).includes('private-'));
sql.exec(readFileSync('migrations/0002-foldery-premium.sql','utf8'));
ops=await operationsSummary(env);assert.equal(ops.services[1].ok,true);assert.equal(ops.services[1].entitlements,0);assert.deepEqual(ops.services[1].counts,[]);
console.log('Passed: date bounds, missing history vs zero, previous period, filters, all daily indicators, week/month boundaries, Minairó downloads, service isolation, missing tables and secret redaction.');

const browserRequest=new Request('https://www.urysoft.com/api/analytics',{headers:{'User-Agent':'Mozilla/5.0'}});
for(const [path,target] of [['/apps/storagey/','storagey'],['/teclairc/','teclairc']]){
 const event=normalizeEvent({id:crypto.randomUUID(),event:'download',path,target},browserRequest);
 assert.equal(event.target,target);
}
assert.ok(normalizeEvent({id:crypto.randomUUID(),event:'page',path:'/tienda/'},browserRequest));
const catalog=JSON.parse(readFileSync('assets/playground/catalog.json','utf8'));
const destination=new URL(catalog.items[0].url);
const target=normalizeEvent({id:crypto.randomUUID(),event:'click',path:'/calaix/',target:destination.href+'?private=ignored#fragment'},browserRequest).target;
assert.equal(target,destination.origin+destination.pathname);
sql.prepare("INSERT INTO analytics_counts(day,path,event,target,country,referrer,device,browser,language,source,hits) VALUES(?,'/calaix/','click',?,'ES','Interno','Móvil','Chrome','es','',3)").run(today,target);
assert.deepEqual((await summary(today,today,'&path=/calaix/')).destinations.map(row=>({...row})),[{label:target,value:3}]);
assert.ok(ops.services.some(service=>service.name==='Storagey Premium'));
assert.ok(ops.services.some(service=>service.name==='TeclaIRC Premium'));
