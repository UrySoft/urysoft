import { playRequest } from '../../../server/play-billing.js';

export function onRequest(context) {
  return playRequest({ ...context, params: { app: 'teclairc' } });
}
