import { reviewAccess } from '../../../server/review-access.js';

// This verifier authenticates an invitation and is never used to sign or renew access.
const invitationVerifier = '67cba93d97565603b39e4254f32d89691eeccbbfa29141a1daf23bf492bab5ad';

export function onRequest(context) {
  return reviewAccess({ ...context, params: { app: 'foldery' } }, Date.now(), invitationVerifier);
}
