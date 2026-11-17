package cat.urysoft.widgets;

import android.app.NotificationManager;
import android.content.*;
import java.util.*;

public final class BadgeState {
    private static volatile Map<String, Integer> counts = Collections.emptyMap();
    public static volatile boolean connected = false;
    public static boolean permitted(Context c) {
        ComponentName listener = new ComponentName(c, NotificationCountService.class);
        if (android.os.Build.VERSION.SDK_INT >= 27) return c.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(listener);
        String enabled = android.provider.Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        if (enabled != null) for (String value : enabled.split(":")) if (listener.equals(ComponentName.unflattenFromString(value))) return true;
        return false;
    }
    public static int get(Context c, String flat) {
        if (!connected || !permitted(c)) return 0;
        ComponentName comp = ComponentName.unflattenFromString(flat);
        return comp == null ? 0 : counts.getOrDefault(comp.getPackageName(), 0);
    }
    public static boolean replace(Map<String, Integer> next) {
        if (counts.equals(next)) return false;
        counts = Collections.unmodifiableMap(new HashMap<>(next)); return true;
    }
}
