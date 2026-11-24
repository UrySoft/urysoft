import { reviewAccess } from '../../../server/review-access.js';

// This verifier authenticates an invitation and is never used to sign or renew access.
const invitationVerifier = 'c53df80d63de2b7c2678e379796b20e59d28d3ab49c8320571d0e740434d5129';

export function onRequest(context) {
  return reviewAccess({ ...context, params: { app: 'storagey' } }, Date.now(), invitationVerifier);
}
