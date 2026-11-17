package cat.urysoft.widgets;

import android.appwidget.AppWidgetManager;
import android.content.*;

public final class PinResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        android.appwidget.AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
        if (info == null || !info.provider.equals(new ComponentName(c, WidgetProvider.class))) return;
        WidgetConfig config = ConfigStore.takeDraft(c, intent.getStringExtra("draft"));
        if (config != null && ConfigStore.put(c, id, config)) { WidgetRenderer.update(c, id); NotificationCountService.rescan(); }
    }
}
