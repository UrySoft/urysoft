package cat.urysoft.widgets;

import java.util.*;

/** Counts visible active alerts, never notification text or unread-message estimates. */
public final class BadgeMath {
    public static final class Event {
        public final String packageName, group;
        public final boolean summary, ongoing, allowed;
        public Event(String pkg, String group, boolean summary, boolean ongoing, boolean allowed) {
            this.packageName = pkg; this.group = group; this.summary = summary; this.ongoing = ongoing; this.allowed = allowed;
        }
    }
    public static Map<String, Integer> count(List<Event> events) {
        Set<String> childGroups = new HashSet<>();
        for (Event e : events) if (e.allowed && !e.ongoing && !e.summary && e.group != null) childGroups.add(e.packageName + "\n" + e.group);
        Map<String, Integer> result = new HashMap<>();
        for (Event e : events) {
            if (!e.allowed || e.ongoing || (e.summary && childGroups.contains(e.packageName + "\n" + e.group))) continue;
            result.put(e.packageName, result.getOrDefault(e.packageName, 0) + 1);
        }
        return result;
    }
}
