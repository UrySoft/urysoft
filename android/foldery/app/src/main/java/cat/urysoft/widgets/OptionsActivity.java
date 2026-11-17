package cat.urysoft.widgets;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

/** Application-wide choices live outside the user's widget collection. */
public final class OptionsActivity extends LanguageActivity {
    private UrySoftUpdater updater;
    @Override public void onCreate(Bundle state){Ui.applyTheme(this);super.onCreate(state);updater=new UrySoftUpdater(this);}
    @Override public void onResume(){super.onResume();if(languageRestarting)return;updater.resume();render();}
    @Override public void onPause(){if(updater!=null)updater.pause();super.onPause();}
    private void render(){
        Ui.applyTheme(this);LinearLayout root=Ui.setup(this);
        LinearLayout bar=Ui.row(this);bar.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,8));
        bar.addView(Ui.iconButton(this,"back",getString(R.string.ui_back_21),v->finish()),new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        TextView title=Ui.text(this,getString(R.string.ui_options_22),22,Ui.INK,true);title.setPadding(Ui.dp(this,12),0,Ui.dp(this,8),0);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton help=Ui.iconButton(this,"help",getString(R.string.ui_help_24),v->Ui.help(this));help.setId(R.id.help_button);bar.addView(help,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));root.addView(bar);
        LinearLayout page=Ui.scroll(root);
        LinearLayout language=Ui.card(page);Ui.heading(language,"settings",getString(R.string.language_title),getString(R.string.language_detail));Ui.add(language,Language.selector(this),12);
        LinearLayout appearance=Ui.card(page);Ui.heading(appearance,"theme",getString(R.string.ui_app_appearance_26),getString(R.string.ui_choose_a_theme_or_follow_your_device_setti_27));Ui.add(appearance,Ui.themeSelector(this),12);
        LinearLayout notifications=Ui.card(page);notifications.setId(R.id.notification_options);boolean granted=BadgeState.permitted(this);
        Ui.heading(notifications,"bell",getString(R.string.ui_notification_badges_29),granted?getString(R.string.ui_access_enabled_active_notifications_are_co_30):getString(R.string.ui_optional_access_to_show_notification_count_31));
        Ui.add(notifications,Ui.button(this,granted?getString(R.string.ui_manage_access_32):getString(R.string.ui_enable_notification_badges_33),false,v->Ui.notificationAccess(this)),14);
        Ui.add(notifications,Ui.text(this,getString(R.string.ui_you_can_enable_or_disable_badges_in_each_w_34),13,Ui.MUTED,false),10);
        LinearLayout premium=Ui.card(page);Ui.heading(premium,"icons",getString(R.string.premium_title),getString(R.string.premium_monthly)+" · "+getString(R.string.premium_annual));
        Ui.add(premium,Ui.button(this,getString(R.string.premium_view),false,v->PremiumOffer.show(this,PremiumPolicy.Feature.NONE)),12);
        LinearLayout updates=Ui.card(page);Ui.heading(updates,"settings",getString(R.string.update_title),getString(R.string.update_detail));
        Button check=Ui.button(this,getString(R.string.update_check),false,v->updater.check(true));check.setId(R.id.check_updates);Ui.add(updates,check,12);
        Button about=Ui.button(this,getString(R.string.ui_about_foldery_2_ai_and_urysoft_35),false,v->AppBrand.about(this));about.setId(R.id.about_button);Ui.add(page,about,20);
    }
}
