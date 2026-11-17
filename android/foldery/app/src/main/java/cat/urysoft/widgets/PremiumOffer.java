package cat.urysoft.widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.Gravity;
import android.widget.*;

/** Contextual offer; browsing and existing widgets do not trigger it. */
final class PremiumOffer {
    private PremiumOffer(){}
    static void show(Activity activity,PremiumPolicy.Feature reason){
        Intent intent=new Intent(activity,PremiumActivity.class).putExtra("reason",reason.name());activity.startActivity(intent);
    }
    static String label(Activity activity,String name){return name+" · "+activity.getString(R.string.premium_badge);}
    static int reason(PremiumPolicy.Feature feature){
        switch(feature){
            case WIDGETS:return R.string.premium_reason_widgets;
            case SHORTCUTS:return R.string.premium_reason_shortcuts;
            case SHAPES:case STYLES:return R.string.premium_reason_styles;
            case BACKGROUND:return R.string.premium_reason_background;
            case SHADOWS:return R.string.premium_reason_shadows;
            default:return R.string.premium_intro;
        }
    }
    static void pending(Activity activity){new AlertDialog.Builder(activity).setTitle(R.string.premium_title).setMessage(R.string.premium_checkout_pending).setPositiveButton(android.R.string.ok,null).show();}
}
