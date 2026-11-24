import { reviewAccess } from '../../../server/review-access.js';

// This verifier authenticates an invitation and is never used to sign or renew access.
const invitationVerifier = '532b721beb50705cac7248cddf7fc19fee87d6b13485489179cd47848f35aa72';

export function onRequest(context) {
  return reviewAccess({ ...context, params: { app: 'widgery' } }, Date.now(), invitationVerifier);
}
