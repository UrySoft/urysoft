package cat.urysoft.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public final class MainActivity extends LanguageActivity {
    private boolean autoCreateStarted;
    private UrySoftUpdater updater;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changes = (p, key) -> { if (key != null && (key.startsWith("widget.")||key.startsWith("deleted."))) runOnUiThread(this::render); };
    @Override public void onCreate(Bundle state) { Ui.applyTheme(this); super.onCreate(state); updater=new UrySoftUpdater(this); autoCreateStarted=state!=null&&state.getBoolean("autoCreateStarted"); }
    @Override public void onSaveInstanceState(Bundle out) { out.putBoolean("autoCreateStarted",autoCreateStarted); super.onSaveInstanceState(out); }
    @Override public void onResume() {
        super.onResume();if(languageRestarting)return; updater.resume(); PremiumClient.refreshIfDue(this); getSharedPreferences("widgets", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(changes); render(); NotificationCountService.rescan(); WidgetRenderer.refreshAsync(this);
        if(ConfigStore.ids(this).length==0&&!autoCreateStarted){autoCreateStarted=true;createWidget(null);} updater.check(false);
    }
    @Override public void onPause() { if(updater!=null)updater.pause(); getSharedPreferences("widgets", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(changes); super.onPause(); }
    private void createWidget(View view) { if(!PremiumAccess.canCreate(this)){PremiumOffer.show(this,PremiumPolicy.Feature.WIDGETS);return;} startActivity(new Intent(this,ConfigureActivity.class)); }
    private void confirmDelete(int id,String name){
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_widget_named,name))
            .setMessage(getString(R.string.delete_widget_confirmation))
            .setNegativeButton(getString(R.string.ui_cancel_110),null)
            .setPositiveButton(getString(R.string.delete_widget_confirm),(d,which)->{
                autoCreateStarted=true;
                if(!ConfigStore.delete(this,id)){Toast.makeText(this,R.string.delete_widget_failed,Toast.LENGTH_LONG).show();return;}
                WidgetRenderer.update(this,id);NotificationCountService.rescan();render();
                Toast.makeText(this,R.string.widget_deleted,Toast.LENGTH_SHORT).show();
            }).create();
        dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.DARK?0xffffb4ab:0xffb3261e));dialog.show();
    }
    private void compactAction(LinearLayout row,Button button){
        button.setTextSize(12);button.setPadding(Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6),Ui.dp(this,6));button.setCompoundDrawablePadding(Ui.dp(this,4));
        button.setMaxLines(2);button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(row.getChildCount()>0)params.setMarginStart(Ui.dp(this,6));row.addView(button,params);
    }
    private void render() {
        Ui.applyTheme(this);
        LinearLayout root=Ui.setup(this);
        LinearLayout bar=Ui.row(this);bar.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,8));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_logo);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);bar.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        LinearLayout brand=Ui.column(this);brand.setPadding(Ui.dp(this,12),0,Ui.dp(this,8),0);TextView title=Ui.text(this,AppBrand.NAME,22,Ui.INK,true);title.setMaxLines(2);brand.addView(title);brand.addView(Ui.text(this,getString(R.string.ui_by_4)+AppBrand.COMPANY,12,Ui.MUTED,true));bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Ui.toolbarActions(this,bar);root.addView(bar);
        LinearLayout page=Ui.scroll(root);
        Ui.add(page,Ui.text(this,getString(R.string.ui_your_spaces_5),28,Ui.INK,true),4);
        Ui.add(page,Ui.text(this,getString(R.string.ui_apps_and_people_one_tap_away_on_your_home__6),15,Ui.MUTED,false),8);
        int[] ids=ConfigStore.ids(this);
        LinearLayout section=Ui.row(this);section.addView(Ui.text(this,getString(R.string.ui_my_widgets_7),18,Ui.INK,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView count=Ui.text(this,Integer.toString(ids.length),13,Ui.BLUE,true);count.setPadding(Ui.dp(this,12),Ui.dp(this,5),Ui.dp(this,12),Ui.dp(this,5));count.setBackground(Ui.rounded(Ui.SOFT,12,this));section.addView(count);Ui.add(page,section,28);
        if(ids.length==0) {
            LinearLayout empty=Ui.card(page);ImageView mark=new ImageView(this);mark.setImageDrawable(new Glyph("folder",Ui.BLUE));mark.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18));mark.setBackground(Ui.rounded(Ui.SOFT,22,this));mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(Ui.dp(this,80),Ui.dp(this,80));mp.gravity=Gravity.CENTER_HORIZONTAL;mp.topMargin=Ui.dp(this,10);empty.addView(mark,mp);
            TextView heading=Ui.text(this,getString(R.string.ui_your_space_starts_here_9),21,Ui.INK,true);heading.setGravity(Gravity.CENTER);Ui.add(empty,heading,20);
            TextView detail=Ui.text(this,getString(R.string.ui_create_your_first_widget_with_your_everyda_10),15,Ui.MUTED,false);detail.setGravity(Gravity.CENTER);Ui.add(empty,detail,10);Ui.spacer(empty,10);
        }
        for(int id:ids) {
            WidgetConfig config=ConfigStore.get(this,id);LinearLayout card=Ui.card(page);
            Ui.add(card,Ui.text(this,config.name,20,Ui.INK,true),0);
            Ui.add(card,Ui.text(this,config.apps.size()+(config.apps.size()==1?getString(R.string.ui_shortcut_11):getString(R.string.ui_shortcuts_12))+(config.opacity==0?getString(R.string.ui_transparent_background_13):getString(R.string.ui_custom_background_14))+(config.shadowEnabled?getString(R.string.ui_with_shadow_15):""),13,Ui.MUTED,false),5);
            PreviewView preview=new PreviewView(this);int[] size=WidgetRenderer.bounds(this,id);preview.setWidgetBounds(size[0],size[1]);preview.configure(config);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,preview.preferredHeight(getResources().getDisplayMetrics().widthPixels-Ui.dp(this,76)));pp.topMargin=Ui.dp(this,14);card.addView(preview,pp);
            LinearLayout actions=Ui.row(this);Ui.add(card,actions,12);
            Button add=Ui.button(this,getString(R.string.ui_add_to_home_screen_237),true,v->{
                if(ConfigStore.isDeleted(this,id)){Toast.makeText(this,R.string.widget_deleted,Toast.LENGTH_LONG).show();return;}
                WidgetPin.request(this,ConfigStore.get(this,id),false);
            });
            add.setId(R.id.add_to_desktop);add.setTag(id);add.setContentDescription(getString(R.string.add_named_to_desktop,config.name));compactAction(actions,add);
            compactAction(actions,Ui.button(this,getString(R.string.ui_customize_widget_16),false,v->startActivity(new Intent(this,ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id))));
            Button delete=Ui.button(this,getString(R.string.delete_widget),false,v->confirmDelete(id,config.name));
            delete.setId(R.id.delete_widget);delete.setTag(id);delete.setTextColor(Ui.DARK?0xffffb4ab:0xffb3261e);delete.setContentDescription(getString(R.string.delete_widget_named,config.name));compactAction(actions,delete);
        }
        Ui.divider(page);Ui.add(page,Ui.text(this,AppBrand.COMPANY+" · "+AppBrand.NAME+" "+AppBrand.VERSION,13,Ui.INK,true),0);
        Ui.add(page,Ui.text(this,getString(R.string.ui_chief_executive_officer_17),12,Ui.MUTED,false),8);page.addView(AppBrand.ownerLink(this));
        Ui.add(page,Ui.text(this,getString(R.string.ui_no_account_offline_no_ads_18),12,Ui.MUTED,false),0);
        LinearLayout footer=Ui.column(this);footer.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,12));footer.setBackgroundColor(Ui.SURFACE);
        footer.addView(Ui.button(this,getString(R.string.ui_create_widget_19),true,this::createWidget));root.addView(footer);
    }
}
