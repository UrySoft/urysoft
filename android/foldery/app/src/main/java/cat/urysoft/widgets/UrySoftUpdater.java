package cat.urysoft.widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

/** Foreground-only, nonblocking checks. Downloads and installation require the user's action. */
final class UrySoftUpdater {
    static final String FEED="https://urysoft.pages.dev/updates/foldery.json";
    private final Activity activity;
    private boolean active,busy;
    UrySoftUpdater(Activity activity){this.activity=activity;}
    void resume(){active=true;}
    void pause(){active=false;}
    void check(boolean manual){
        if(busy || !active)return;
        busy=true;
        new Thread(()->{
            UpdateManifest result=null;Exception failure=null;long installed=0;
            try {
                android.content.pm.PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
                installed=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
                HttpsURLConnection connection=(HttpsURLConnection)new URL(FEED).openConnection();
                connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
                connection.setInstanceFollowRedirects(false);connection.setUseCaches(false);
                try {
                    if(connection.getResponseCode()!=200)throw new java.io.IOException("HTTP");
                    try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                        byte[] buffer=new byte[4096];int n;
                        while((n=input.read(buffer))!=-1){
                            if(bytes.size()+n>65536)throw new java.io.IOException("Feed too large");
                            bytes.write(buffer,0,n);
                        }
                        result=UpdateManifest.parse(bytes.toString("UTF-8"));
                    }
                } finally {connection.disconnect();}
            } catch(Exception error){failure=error;}
            final UpdateManifest update=result;final Exception error=failure;final long current=installed;
            activity.runOnUiThread(()->{
                busy=false;
                if(!active || activity.isFinishing() || activity.isDestroyed())return;
                if(error!=null){if(manual)message(R.string.update_error);return;}
                if(!update.newerThan(current)){if(manual)message(R.string.update_none);return;}
                String language=activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
                new AlertDialog.Builder(activity).setTitle(R.string.update_available)
                    .setMessage(update.versionName+"\n\n"+update.notes(language)+"\n\n"+activity.getString(R.string.update_browser))
                    .setNegativeButton(R.string.update_later,null)
                    .setPositiveButton(R.string.update_download,(dialog,which)->{
                        try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(update.downloadUrl)));}
                        catch(Exception unavailable){message(R.string.update_open_error);}
                    }).show();
            });
        },"urysoft-update").start();
    }
    private void message(int resource){new AlertDialog.Builder(activity).setTitle("UrySoft").setMessage(resource).setPositiveButton(android.R.string.ok,null).show();}
}
