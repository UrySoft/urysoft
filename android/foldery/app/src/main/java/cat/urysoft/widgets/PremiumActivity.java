package cat.urysoft.widgets;

import android.os.Bundle;
import android.widget.*;

/** Approved translated paywall, candidate for live service; activation is gated. */
public final class PremiumActivity extends LanguageActivity {
    private boolean busy,closed;
    private TextView stateText;
    @Override public void onCreate(Bundle state){Ui.applyTheme(this);super.onCreate(state);}
    @Override public void onResume(){super.onResume();closed=false;if(!languageRestarting){render();refresh();}}
    @Override public void onPause(){closed=true;super.onPause();}
    private void message(int text){new android.app.AlertDialog.Builder(this).setTitle(R.string.premium_title).setMessage(text).setPositiveButton(android.R.string.ok,null).show();}
    private void start(boolean restore){
        if(busy)return;busy=true;stateText.setText(R.string.premium_connecting);
        new Thread(()->{try{String url=PremiumClient.start(getApplicationContext(),restore);runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed())try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception e){message(R.string.premium_connection_error);}});}catch(Exception e){runOnUiThread(()->{busy=false;if(!closed)message(R.string.premium_connection_error);});}},"foldery-premium-start").start();
    }
    private void refresh(){
        if(busy)return;busy=true;
        new Thread(()->{boolean ok=true;try{PremiumClient.refresh(getApplicationContext());}catch(Exception e){ok=false;}final boolean success=ok;runOnUiThread(()->{busy=false;if(!closed){render();if(!success)stateText.setText(R.string.premium_connection_error);}});},"foldery-premium-check").start();
    }
    private void render(){
        Ui.applyTheme(this);LinearLayout root=Ui.setup(this),bar=Ui.row(this);
        bar.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,8));
        bar.addView(Ui.iconButton(this,"back",getString(R.string.ui_back_21),view->finish()),new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        TextView title=Ui.text(this,getString(R.string.premium_title),21,Ui.INK,true);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(bar);
        LinearLayout page=Ui.scroll(root);
        int stateResource=PremiumLicense.active(this)?R.string.premium_active:("pending_payment".equals(PremiumLicense.prefs(this).getString("lastState",""))?R.string.premium_waiting_payment:R.string.premium_intro);
        stateText=Ui.text(this,getString(stateResource),15,Ui.MUTED,false);Ui.add(page,stateText,8);
        PremiumPolicy.Feature feature=PremiumPolicy.Feature.NONE;
        try{feature=PremiumPolicy.Feature.valueOf(getIntent().getStringExtra("reason"));}catch(Exception ignored){}
        Ui.add(page,Ui.text(this,getString(PremiumOffer.reason(feature)),15,Ui.MUTED,false),8);
        Ui.add(page,Ui.text(this,getString(R.string.premium_heading),27,Ui.INK,true),16);
        Ui.add(page,Ui.text(this,getString(R.string.premium_coffee),17,Ui.MUTED,false),10);
        Ui.add(page,Ui.text(this,getString(R.string.premium_monthly),38,Ui.BLUE,true),22);
        Ui.add(page,Ui.text(this,getString(R.string.premium_annual),16,Ui.INK,true),5);
        Ui.add(page,Ui.text(this,getString(R.string.premium_renewal),14,Ui.MUTED,false),4);
        LinearLayout benefits=Ui.card(page);
        for(int resource:new int[]{R.string.premium_benefit_widgets,R.string.premium_benefit_shortcuts,R.string.premium_benefit_designs,R.string.premium_benefit_background})Ui.add(benefits,Ui.text(this,"✓  "+getString(resource),16,Ui.INK,false),12);
        Button subscribe=Ui.button(this,getString(PremiumLicense.active(this)?R.string.premium_manage:R.string.premium_subscribe),true,view->{if(PremiumLicense.active(this))startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://www.paypal.com/myaccount/autopay/")));else start(false);});subscribe.setId(R.id.premium_subscribe);Ui.add(page,subscribe,20);
        Button later=Ui.button(this,getString(R.string.premium_later),false,view->finish());later.setId(R.id.premium_later);Ui.add(page,later,8);
        Button restore=Ui.button(this,getString(R.string.premium_restore),false,view->start(true));restore.setId(R.id.premium_restore);Ui.add(page,restore,8);
        Ui.add(page,Ui.button(this,getString(R.string.premium_refresh),false,view->refresh()),8);
        Ui.add(page,Ui.text(this,getString(R.string.premium_cancel),13,Ui.MUTED,false),16);
        Ui.add(page,Ui.text(this,getString(R.string.premium_existing),13,Ui.MUTED,false),10);
        Ui.add(page,Ui.text(this,getString(R.string.premium_offline),13,Ui.MUTED,false),16);
    }
}
