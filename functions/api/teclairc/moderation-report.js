import { submitReport } from '../../../server/app-moderation.js';

export const onRequest = context => submitReport(context, 'teclairc');
