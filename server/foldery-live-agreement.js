// Provisional until the owner approves the matching documents for publication.
export const TERMS_VERSION='2026-11-14';
export const PRIVACY_VERSION='2026-11-14';
export function validAgreement(data){return data.termsAccepted===true&&data.termsVersion===TERMS_VERSION;}
export async function recordAgreement(db,accountId,requestId,lang,now){
  await db.prepare('INSERT OR IGNORE INTO premium_agreements(request_id,account_id,terms_version,privacy_version,accepted_at,lang,annual_amount_cents,currency,automatic_renewal) VALUES(?,?,?,?,?,?,?,?,?)')
    .bind(requestId,accountId,TERMS_VERSION,PRIVACY_VERSION,now,['ca','en'].includes(lang)?lang:'es',600,'EUR',1).run();
}
