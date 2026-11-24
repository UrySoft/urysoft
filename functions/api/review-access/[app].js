import { reviewAccess } from '../../../server/review-access.js';

export const onRequest = context => reviewAccess(context);
