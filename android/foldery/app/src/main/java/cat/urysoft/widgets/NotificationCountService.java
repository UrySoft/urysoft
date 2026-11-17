package cat.urysoft.widgets;

import android.app.Notification;
import android.content.ComponentName;
import android.os.*;
import android.service.notification.*;
import java.util.*;

public final class NotificationCountService extends NotificationListenerService {
    private static volatile NotificationCountService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable recalculate = this::publish;

    @Override public void onListenerConnected() { instance = this; BadgeState.connected = true; schedule(); }
    @Override public void onListenerDisconnected() {
        handler.removeCallbacks(recalculate); instance = null; BadgeState.connected = false;
        BadgeState.replace(Collections.emptyMap()); WidgetRenderer.refreshAsync(this);
        if (BadgeState.permitted(this)) requestRebind(new ComponentName(this, NotificationCountService.class));
    }
    @Override public void onDestroy() { handler.removeCallbacks(recalculate); if (instance == this) instance = null; BadgeState.connected = false; BadgeState.replace(Collections.emptyMap()); WidgetRenderer.refreshAsync(this); super.onDestroy(); }
    @Override public void onNotificationPosted(StatusBarNotification sbn) { schedule(); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { schedule(); }
    @Override public void onNotificationRankingUpdate(RankingMap rankingMap) { schedule(); }
    private void schedule() { handler.removeCallbacks(recalculate); handler.postDelayed(recalculate, 250); }
    public static void rescan() { NotificationCountService active = instance; if (active != null) active.schedule(); }
    private void publish() {
        if (!BadgeState.connected) return;
        ArrayList<BadgeMath.Event> events = new ArrayList<>();
        try {
            Set<String> watched = ConfigStore.watchedPackages(this);
            StatusBarNotification[] active = getActiveNotifications();
            RankingMap rankingMap = getCurrentRanking();
            if (active != null) for (StatusBarNotification sbn : active) {
                if (!watched.contains(sbn.getPackageName()) || !sbn.getUser().equals(android.os.Process.myUserHandle())) continue;
                Notification n = sbn.getNotification();
                Ranking ranking = new Ranking();
                boolean allowed = rankingMap == null || !rankingMap.getRanking(sbn.getKey(), ranking) || ranking.canShowBadge();
                events.add(new BadgeMath.Event(sbn.getPackageName(), sbn.isGroup() ? sbn.getGroupKey() : null,
                    (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0, sbn.isOngoing(), allowed));
            }
        } catch (SecurityException | IllegalStateException ignored) {
            BadgeState.connected = false;
        }
        if (BadgeState.replace(BadgeMath.count(events))) WidgetRenderer.refreshAsync(this);
    }
}
