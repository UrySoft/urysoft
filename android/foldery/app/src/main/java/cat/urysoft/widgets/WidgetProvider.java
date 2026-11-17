package cat.urysoft.widgets;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;

public final class WidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager manager, int[] ids) { for (int id : ids) WidgetRenderer.update(c, id); NotificationCountService.rescan(); }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager manager, int id, Bundle options) { WidgetRenderer.update(c, id); }
    @Override public void onDeleted(Context c, int[] ids) { for (int id : ids) ConfigStore.remove(c, id); NotificationCountService.rescan(); }
    @Override public void onRestored(Context c, int[] oldIds, int[] newIds) {
        for (int i = 0; i < oldIds.length; i++) { if(ConfigStore.isDeleted(c,oldIds[i]))ConfigStore.markDeleted(c,newIds[i]);else ConfigStore.put(c, newIds[i], ConfigStore.get(c, oldIds[i])); ConfigStore.remove(c, oldIds[i]); }
        onUpdate(c, AppWidgetManager.getInstance(c), newIds);
    }
    @Override public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction()))WidgetRenderer.refreshAsync(c);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WidgetRenderer.refreshAsync(c);
            if (BadgeState.permitted(c)) android.service.notification.NotificationListenerService.requestRebind(new ComponentName(c, NotificationCountService.class));
        }
    }
}
