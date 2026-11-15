import {liveProfile} from '../../../../server/widgery-live-profile.js';
import {liveRequest} from '../../../../server/widgery-live-core.js';
export async function onRequest(context){
  const env=liveProfile(context.env);
  const url=new URL(context.request.url);
  if(context.request.method==='GET'&&url.pathname==='/api/widgery/live/status')return Response.json({available:!!env},{headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  if(!env)return Response.json({error:'LIVE_PAYMENTS_NOT_AVAILABLE'},{status:503,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  return liveRequest({...context,env});
}
