package cat.urysoft.widgets;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;

/** One placement flow for the creator, editor and configured-widget cards. */
public final class WidgetPin {
    private WidgetPin(){}
    public static void request(Activity a,WidgetConfig source,boolean finishOnRequest){
        WidgetConfig config=WidgetConfig.fromJson(source.toJson());
        PremiumPolicy.Feature needed=PremiumAccess.requirement(a,-1,config);if(needed!=PremiumPolicy.Feature.NONE){PremiumOffer.show(a,needed);return;}
        if(config.apps.isEmpty()){Toast.makeText(a,R.string.ui_choose_at_least_one_app_or_contact_294,Toast.LENGTH_LONG).show();return;}
        config.name=config.name.trim();if(config.name.isEmpty())config.name=a.getString(R.string.default_widget_name);
        AppWidgetManager manager=AppWidgetManager.getInstance(a);
        if(!manager.isRequestPinAppWidgetSupported()){help(a,config);return;}
        String draft=ConfigStore.draft(a,config);
        Intent callback=new Intent(a,PinResultReceiver.class).putExtra("draft",draft).setData(Uri.parse("urywidgets://pin/"+draft));
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0);
        PendingIntent pending=PendingIntent.getBroadcast(a,0,callback,flags);
        Bundle extras=new Bundle();
        if(Build.VERSION.SDK_INT>=31)extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,WidgetRenderer.build(a,-1,config,280,160));
        boolean requested;
        try{requested=manager.requestPinAppWidget(new ComponentName(a,WidgetProvider.class),extras,pending);}
        catch(SecurityException|IllegalStateException e){help(a,config);return;}
        if(requested){Toast.makeText(a,R.string.ui_confirm_the_widget_placement_on_your_home__297,Toast.LENGTH_LONG).show();if(finishOnRequest)a.finish();}
        else help(a,config);
    }
    private static void help(Activity a,WidgetConfig config){
        ConfigStore.putTemplate(a,config);
        new AlertDialog.Builder(a).setTitle(R.string.ui_add_it_from_the_home_screen_298)
            .setMessage(R.string.ui_your_home_screen_does_not_support_adding_w_299)
            .setPositiveButton(R.string.ui_got_it_300,null).show();
    }
}
