package cat.urysoft.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.widget.Toast;

/** An explicit Activity PendingIntent keeps widget launches compatible with modern Android. */
public final class LaunchActivity extends LanguageActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String flat = getIntent().getStringExtra("component");
        if (!ConfigStore.isDeleted(this,id)&&flat != null && ConfigStore.get(this, id).apps.contains(flat)) {
            if(ContactStore.isContact(flat)){
                Intent contact=ContactStore.viewIntent(this,flat);
                if(contact!=null)try{startActivity(contact);}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,R.string.contact_unavailable,Toast.LENGTH_LONG).show();}
                else Toast.makeText(this,R.string.contact_unavailable,Toast.LENGTH_LONG).show();
                finish();return;
            }
            ComponentName comp = ComponentName.unflattenFromString(flat);
            if (comp != null) try {
                Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(comp).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launch);
            } catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(this, R.string.launch_unavailable, Toast.LENGTH_LONG).show(); }
        }
        finish();
    }
}
