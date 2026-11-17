package cat.urysoft.widgets;

import android.content.Context;

/** One gate for every save/pin path. Checkout never sets a local paid boolean. */
final class PremiumAccess {
    private PremiumAccess(){}
    static boolean active(Context context){return PremiumLicense.active(context);}
    static PremiumPolicy.Design design(WidgetConfig config){
        if(config==null)return null;
        String shadow=config.shadowEnabled?config.shadowColor+":"+config.shadowOpacity+":"+config.shadowSharpness+":"+config.shadowDistance+":"+config.shadowAngle:"";
        return new PremiumPolicy.Design(config.apps.size(),config.iconShape,config.iconStyle,config.backgroundImage,config.backgroundMode,shadow);
    }
    static int configuredOthers(Context context,int except){
        int total=0;for(int id:ConfigStore.ids(context))if(id!=except&&ConfigStore.has(context,id))total++;return total;
    }
    static boolean canCreate(Context context){return PremiumPolicy.canCreate(active(context),configuredOthers(context,-1));}
    static int limit(Context context,WidgetConfig baseline){return PremiumPolicy.shortcutLimit(active(context),design(baseline));}
    static PremiumPolicy.Feature requirement(Context context,int id,WidgetConfig value){
        WidgetConfig before=id!=-1&&ConfigStore.has(context,id)?ConfigStore.get(context,id):null;
        return PremiumPolicy.saving(active(context),design(before),design(value),configuredOthers(context,id));
    }
    static boolean canPersist(Context context,int id,WidgetConfig value){return requirement(context,id,value)==PremiumPolicy.Feature.NONE;}
}
