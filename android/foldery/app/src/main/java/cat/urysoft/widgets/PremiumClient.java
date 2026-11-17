package cat.urysoft.widgets;
import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/** All network work runs off the UI thread; credentials go only to the official HTTPS API. */
final class PremiumClient {
 static synchronized void refreshIfDue(Context context){
  Context app=context.getApplicationContext();long now=System.currentTimeMillis();
  if(!PremiumLicense.prefs(app).contains("license")||now-PremiumLicense.prefs(app).getLong("lastAttempt",0)<3600000L)return;
  PremiumLicense.prefs(app).edit().putLong("lastAttempt",now).apply();
  new Thread(()->{try{refresh(app);}catch(Exception ignored){}},"foldery-premium-refresh").start();
 }
 static JSONObject request(Context c,String path,JSONObject body)throws Exception{
  HttpsURLConnection connection=(HttpsURLConnection)new URL("https://urysoft.pages.dev/api/foldery/live/"+path).openConnection();
  connection.setRequestMethod("POST");connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);connection.setDoOutput(true);
  connection.setRequestProperty("Authorization","Bearer "+PremiumLicense.token(c));connection.setRequestProperty("Content-Type","application/json");
  try{
   byte[] data=body.toString().getBytes("UTF-8");connection.setFixedLengthStreamingMode(data.length);
   try(OutputStream output=connection.getOutputStream()){output.write(data);}
   if(connection.getResponseCode()!=200)throw new IOException("Premium service unavailable");
   try(InputStream input=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1){if(out.size()+n>32768)throw new IOException("Response too large");out.write(buffer,0,n);}return new JSONObject(out.toString("UTF-8"));
   }
  }finally{connection.disconnect();}
 }
 static String start(Context c,boolean restore)throws Exception{
  String lang=c.getResources().getConfiguration().getLocales().get(0).getLanguage();
  String url=request(c,"device/start",new JSONObject().put("mode",restore?"restore":"subscribe").put("lang",lang)).getString("url");
  java.net.URI uri=new java.net.URI(url);
  if(!"https".equals(uri.getScheme())||!"urysoft.pages.dev".equals(uri.getHost())||!"/foldery/premium-real/".equals(uri.getPath())||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getFragment()==null||!uri.getFragment().matches("[A-Za-z0-9_-]{43}"))throw new IOException("Invalid subscription destination");
  return url;
 }
 static void refresh(Context c)throws Exception{
  JSONObject data=request(c,"device/status",new JSONObject());String state=data.optString("state");
  if(!"live".equals(data.optString("environment"))&&!"awaiting_google".equals(state))throw new IOException("Unexpected payment environment");
  if("premium".equals(state))PremiumLicense.save(c,data.getString("license"));
  else if("free".equals(state)||"pending_payment".equals(state))PremiumLicense.save(c,null);
  else if(!"awaiting_google".equals(state))throw new IOException("Unexpected subscription response");
  PremiumLicense.prefs(c).edit().putString("lastState",state).apply();
 }
}
