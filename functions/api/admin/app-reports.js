import { reviewReports } from '../../../server/app-moderation.js';

export const onRequest = context => reviewReports(context);
