package cat.urysoft.widgets;

import android.appwidget.AppWidgetManager;
import android.content.*;
import java.util.*;

public final class ConfigStore {
    private static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("widgets", Context.MODE_PRIVATE); }
    public static WidgetConfig get(Context c, int id) { String raw=prefs(c).getString("widget." + id,null);return raw==null?WidgetConfig.defaults(c):WidgetConfig.fromJson(raw); }
    public static boolean has(Context c, int id) { return !isDeleted(c,id)&&prefs(c).contains("widget." + id); }
    public static synchronized boolean put(Context c, int id, WidgetConfig value) { return !isDeleted(c,id)&&PremiumAccess.canPersist(c,id,value)&&prefs(c).edit().putString("widget." + id, value.toJson()).commit(); }
    public static synchronized void remove(Context c, int id) { prefs(c).edit().remove("widget." + id).remove("deleted."+id).apply(); }
    public static boolean isDeleted(Context c,int id){return prefs(c).getBoolean("deleted."+id,false);}
    public static synchronized boolean markDeleted(Context c,int id){return prefs(c).edit().remove("widget."+id).putBoolean("deleted."+id,true).commit();}
    public static synchronized boolean delete(Context c,int id){
        for(int own:hostIds(c))if(own==id)return markDeleted(c,id);
        return false;
    }
    public static WidgetConfig takeTemplate(Context c) {
        String raw = prefs(c).getString("template", null);
        prefs(c).edit().remove("template").apply();
        return raw==null?WidgetConfig.defaults(c):WidgetConfig.fromJson(raw);
    }
    public static void putTemplate(Context c, WidgetConfig value) { prefs(c).edit().putString("template", value.toJson()).commit(); }
    public static int[] hostIds(Context c) { return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, WidgetProvider.class)); }
    public static int[] ids(Context c){
        int[] hosted=hostIds(c);int[] active=new int[hosted.length];int count=0;
        for(int id:hosted)if(!isDeleted(c,id))active[count++]=id;
        return Arrays.copyOf(active,count);
    }
    public static String draft(Context c, WidgetConfig value) {
        String id = UUID.randomUUID().toString();
        // Pending pins can be confirmed later; retain up to ten independent drafts.
        ArrayList<String> keys = new ArrayList<>();
        for (String k : prefs(c).getAll().keySet()) if (k.startsWith("draft.")) keys.add(k);
        android.content.SharedPreferences.Editor ed = prefs(c).edit();
        if (keys.size() >= 10) for (String k : keys) ed.remove(k);
        ed.putString("draft." + id, value.toJson()).commit();
        return id;
    }
    public static WidgetConfig takeDraft(Context c, String id) {
        if (id == null) return null;
        String raw = prefs(c).getString("draft." + id, null);
        if (raw == null) return null;
        prefs(c).edit().remove("draft." + id).commit();
        return raw==null?WidgetConfig.defaults(c):WidgetConfig.fromJson(raw);
    }
    public static Set<String> watchedPackages(Context c) {
        Set<String> result = new HashSet<>();
        for (int id : ids(c)) {
            WidgetConfig config = get(c, id);
            if (!config.badges) continue;
            for (String flat : config.apps) {
                ComponentName comp = ComponentName.unflattenFromString(flat);
                if (comp != null) result.add(comp.getPackageName());
            }
        }
        return result;
    }
}
