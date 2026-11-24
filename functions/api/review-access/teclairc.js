import { reviewAccess } from '../../../server/review-access.js';

// This verifier authenticates an invitation and is never used to sign or renew access.
const invitationVerifier = 'e9ee0c8db2b8363a98d92078d6f7220d37aec04e3f2f6077581fc2e1809799a8';

export function onRequest(context) {
  return reviewAccess({ ...context, params: { app: 'teclairc' } }, Date.now(), invitationVerifier);
}
