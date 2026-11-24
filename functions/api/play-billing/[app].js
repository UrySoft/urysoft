import { playRequest } from '../../../server/play-billing.js';

export const onRequest = context => playRequest(context);
