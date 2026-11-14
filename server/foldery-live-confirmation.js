import {sha256,randomToken} from './foldery-live-license.js';
import {PaymentError} from './foldery-live-paypal.js';
import {SELLER,DOCUMENTS} from './foldery-live-documents.js';
import {TERMS_VERSION,PRIVACY_VERSION} from './foldery-live-agreement.js';
const escape=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;');
export const RECEIPT_COOKIE='__Host-foldery_receipt';
export async function receiptCookie(db,row,now){
 const token=randomToken();
 await db.prepare('DELETE FROM premium_receipt_sessions WHERE expires_at<?').bind(now).run();
 await db.prepare('INSERT INTO premium_receipt_sessions(token_hash,account_id,request_id,expires_at) VALUES(?,?,?,?)').bind(await sha256(token),row.account_id,row.request_id,now+3600000).run();
 return RECEIPT_COOKIE+'='+token+'; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600';
}
export async function receiptSession(db,request,now){
 if(request.headers.get('Sec-Fetch-Site')==='cross-site')throw new PaymentError('ORIGIN_NOT_ALLOWED',403);
 const token=request.headers.get('Cookie')?.split(';').map(x=>x.trim()).find(x=>x.startsWith(RECEIPT_COOKIE+'='))?.slice(RECEIPT_COOKIE.length+1);
 if(!/^[A-Za-z0-9_-]{43}$/.test(token||''))throw new PaymentError('RECEIPT_SIGN_IN_REQUIRED',401);
 const session=await db.prepare('SELECT account_id,request_id FROM premium_receipt_sessions WHERE token_hash=? AND expires_at>?').bind(await sha256(token),now).first();
 if(!session)throw new PaymentError('RECEIPT_SIGN_IN_REQUIRED',401);
 const bucket='receipt:'+await sha256(token);
 const rate=await db.prepare('INSERT INTO premium_limits(bucket,count,expires) VALUES(?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 WHERE count<30 RETURNING count').bind(bucket,now+3600000).first();
 if(!rate)throw new PaymentError('TRY_LATER',429);
 return session;
}
const words={es:{title:'Confirmación de contratación · Foldery Premium',issued:'Confirmación emitida',accepted:'Condiciones aceptadas',reference:'Referencia de suscripción PayPal',period:'Periodo pagado verificado hasta',seller:'Vendedor',price:'6 € al año, impuestos incluidos. Equivale a 0,50 €/mes; el cobro es anual y la renovación es automática.',note:'Guarda este archivo. Incluye una copia de las condiciones aceptadas y la información de privacidad facilitada en la compra. Es una confirmación de contratación; no sustituye a la factura fiscal ni acredita el estado actual de futuras renovaciones.',rights:'Puedes cancelar la renovación en PayPal. Desinstalar la app no cancela la suscripción. Se mantienen los 14 días de desistimiento desde la contratación inicial, sin renuncia automática. Para ejercerlo o solicitar factura, contacta con el vendedor.'},ca:{title:'Confirmació de contractació · Foldery Premium',issued:'Confirmació emesa',accepted:'Condicions acceptades',reference:'Referència de subscripció PayPal',period:'Període pagat verificat fins a',seller:'Venedor',price:'6 € l’any, impostos inclosos. Equival a 0,50 €/mes; el cobrament és anual i la renovació és automàtica.',note:'Desa aquest fitxer. Inclou una còpia de les condicions acceptades i la informació de privacitat facilitada en la compra. És una confirmació de contractació; no substitueix la factura fiscal ni acredita l’estat actual de renovacions futures.',rights:'Pots cancel·lar la renovació a PayPal. Desinstal·lar l’app no cancel·la la subscripció. Es mantenen els 14 dies de desistiment des de la contractació inicial, sense renúncia automàtica. Per exercir-lo o demanar factura, contacta amb el venedor.'},en:{title:'Contract confirmation · Foldery Premium',issued:'Confirmation issued',accepted:'Terms accepted',reference:'PayPal subscription reference',period:'Verified paid period until',seller:'Seller',price:'€6 per year, taxes included. Equivalent to €0.50/month; billing is annual with automatic renewal.',note:'Keep this file. It includes a copy of the terms and privacy information acknowledged at purchase. It is a contract confirmation; it does not replace a tax invoice or confirm the current status of future renewals.',rights:'Cancel renewal in PayPal. Uninstalling the app does not cancel the subscription. The 14-day withdrawal right from the initial contract is preserved without an automatic waiver. Contact the seller to exercise it or request an invoice.'}};
export function confirmationHtml(row,agreement,now){
 if(agreement.terms_version!==TERMS_VERSION||agreement.privacy_version!==PRIVACY_VERSION||agreement.annual_amount_cents!==600||agreement.currency!=='EUR'||agreement.automatic_renewal!==1)throw new PaymentError('CONFIRMATION_VERSION_UNAVAILABLE',503);
 const lang=['ca','en'].includes(agreement.lang)?agreement.lang:'es',t=words[lang];
 const date=n=>escape(new Date(n).toISOString());
 return '<!doctype html><html lang="'+lang+'"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>'+t.title+'</title><style>:root{color-scheme:light dark}body{font:16px/1.65 system-ui;max-width:800px;margin:auto;padding:24px 18px;overflow-wrap:anywhere}h1{font-size:28px}h2{font-size:21px}dt{font-weight:700;margin-top:12px}dd{margin-left:0}.summary{border:1px solid #888;border-radius:14px;padding:18px}article{border-top:1px solid #888;margin-top:28px;padding-top:12px}a{color:light-dark(#164caa,#a0c7ff)}@media print{body{color:#111;background:white}}</style></head><body><h1>'+t.title+'</h1><p>'+t.note+'</p><div class="summary"><p><strong>'+t.price+'</strong></p><dl><dt>'+t.reference+'</dt><dd>'+escape(row.subscription_id)+'</dd><dt>'+t.issued+'</dt><dd>'+date(now)+'</dd><dt>'+t.accepted+'</dt><dd>'+date(agreement.accepted_at)+' · '+escape(agreement.terms_version)+'</dd><dt>'+t.period+'</dt><dd>'+date(row.paid_until)+'</dd><dt>'+t.seller+'</dt><dd>'+escape(SELLER.name)+' · UrySoft<br>'+escape(SELLER.taxId)+'<br>'+escape(SELLER.address)+'<br>'+escape(SELLER.postalCode+' '+SELLER.city+' · '+SELLER.country)+'<br><a href="mailto:'+escape(SELLER.email)+'">'+escape(SELLER.email)+'</a></dd></dl><p>'+t.rights+'</p></div><article>'+DOCUMENTS.condiciones[lang]+'</article><article>'+DOCUMENTS.privacidad[lang]+'</article></body></html>';
}
export async function storedConfirmation(db,accountId,requestId){return db.prepare('SELECT document_html FROM premium_confirmations WHERE account_id=? AND request_id=?').bind(accountId,requestId).first();}
export async function createConfirmation(db,row,now){
 const old=await storedConfirmation(db,row.account_id,row.request_id);if(old)return old.document_html;
 if(!row.subscription_id||row.paid_until<=now||!row.verified_at)throw new PaymentError('PAYMENT_CONFIRMATION_PENDING',409);
 const agreement=await db.prepare('SELECT * FROM premium_agreements WHERE account_id=? AND request_id=?').bind(row.account_id,row.request_id).first();
 if(!agreement)throw new PaymentError('CONFIRMATION_ACCEPTANCE_MISSING',409);
 const html=confirmationHtml(row,agreement,now);
 await db.prepare('INSERT OR IGNORE INTO premium_confirmations(request_id,account_id,subscription_id,created_at,document_html) VALUES(?,?,?,?,?)').bind(row.request_id,row.account_id,row.subscription_id,now,html).run();
 return (await storedConfirmation(db,row.account_id,row.request_id)).document_html;
}
export function confirmationResponse(html){return new Response(html,{headers:{'Content-Type':'text/html; charset=utf-8','Content-Disposition':'attachment; filename="Foldery-confirmacion.html"','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}});}

