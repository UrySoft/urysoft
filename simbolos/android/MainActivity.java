package com.urysoft.simbolos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;
import java.util.Collections;

public class MainActivity extends Activity {
 private static final String HOST="appassets.androidplatform.net";
 private WebView web;
 private ValueCallback<Uri[]> selection;
 private byte[] pending;
 private String mime;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  web=new WebView(this);setContentView(web);
  web.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setAllowFileAccess(false);
  settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.addJavascriptInterface(new Saver(),"UrySoftAndroid");
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req){
    Uri u=req.getUrl();if(!HOST.equals(u.getHost()))return null;
    String path=u.getPath();if(path==null||!path.matches("/(?:index[.]html|ui[.]mjs|v1/(?:codec|finders|png)[.]mjs)"))return missing();
    try{return new WebResourceResponse(path.endsWith("html")?"text/html":"text/javascript","utf-8",getAssets().open(path.substring(1)));}
    catch(IOException e){return missing();}
   }
   private WebResourceResponse missing(){return new WebResourceResponse("text/plain","utf-8",404,"Not found",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
    Uri u=r.getUrl();if(r.isForMainFrame() && ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) && u.getHost()!=null){
     try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(android.content.ActivityNotFoundException e){Toast.makeText(MainActivity.this,"No hay navegador disponible",Toast.LENGTH_SHORT).show();}
    }
    return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
    if(selection!=null)selection.onReceiveValue(null);selection=cb;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
    String[] types=p.getAcceptTypes();i.setType(types.length>0&&types[0].startsWith("image/")?"image/*":"*/*");
    try{startActivityForResult(i,10);}catch(Exception e){selection.onReceiveValue(null);selection=null;}return true;
   }
  });web.loadUrl("https://"+HOST+"/index.html");
 }
 public class Saver {
  @JavascriptInterface public void save(String filename,String type,String data){
   if(data.length()>4000000)return;
   runOnUiThread(()->{if(pending!=null)return;
    try{pending=Base64.decode(data,Base64.DEFAULT);mime="image/png".equals(type)?type:"application/octet-stream";
     Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);
     i.putExtra(Intent.EXTRA_TITLE,"image/png".equals(mime)?"urysoft-simbolos.png":"recuperado.bin");startActivityForResult(i,11);
    }catch(Exception e){pending=null;Toast.makeText(MainActivity.this,"No se pudo abrir el destino",Toast.LENGTH_SHORT).show();}
   });
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);Uri uri=result==RESULT_OK&&data!=null?data.getData():null;
  if(request==10&&selection!=null){selection.onReceiveValue(uri==null?null:new Uri[]{uri});selection=null;}
  if(request==11){byte[] saved=pending;pending=null;if(uri!=null&&saved!=null){
   try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")){if(stream==null)throw new IOException();stream.write(saved);Toast.makeText(this,"Archivo guardado",Toast.LENGTH_SHORT).show();}
   catch(IOException e){new AlertDialog.Builder(this).setMessage("No se pudo guardar el archivo").setPositiveButton("Aceptar",null).show();}
  }}
 }
 @Override protected void onDestroy(){if(selection!=null){selection.onReceiveValue(null);selection=null;}web.removeJavascriptInterface("UrySoftAndroid");web.destroy();super.onDestroy();}
}
