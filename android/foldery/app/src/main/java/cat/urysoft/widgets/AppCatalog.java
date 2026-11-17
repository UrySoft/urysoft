package cat.urysoft.widgets;

import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.*;

public final class AppCatalog {
    public static final class Entry {
        public final String component, label, packageName;
        Entry(String component, String label, String packageName) { this.component = component; this.label = label; this.packageName = packageName; }
    }
    public static List<Entry> load(Context c) {
        c=Language.context(c);
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = c.getPackageManager().queryIntentActivities(i, 0);
        ArrayList<Entry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : resolved) {
            if (!r.activityInfo.enabled || !r.activityInfo.exported) continue;
            String flat = new ComponentName(r.activityInfo.packageName, r.activityInfo.name).flattenToString();
            if (seen.add(flat)) result.add(new Entry(flat, r.loadLabel(c.getPackageManager()).toString(), r.activityInfo.packageName));
        }
        for(ContactStore.Entry contact:ContactStore.all(c))result.add(new Entry(contact.id,contact.name,c.getString(R.string.ui_contact_open_details_91)));
        Collator collator = Collator.getInstance();
        result.sort((a, b) -> collator.compare(a.label, b.label));
        return result;
    }
    public static String label(Context c, String flat) {
        if(ContactStore.isContact(flat)){ContactStore.Entry contact=ContactStore.get(c,flat);return contact==null?c.getString(R.string.ui_contact_unavailable_92):contact.name;}
        ComponentName comp = ComponentName.unflattenFromString(flat);
        if (comp == null) return c.getString(R.string.ui_app_unavailable_93);
        try { return c.getPackageManager().getActivityInfo(comp, 0).loadLabel(c.getPackageManager()).toString(); }
        catch (PackageManager.NameNotFoundException e) { return comp.getPackageName() + c.getString(R.string.ui_unavailable_94); }
    }
    public static Drawable icon(Context c, String flat) {
        if(ContactStore.isContact(flat))return ContactStore.icon(c,flat);
        ComponentName comp = ComponentName.unflattenFromString(flat);
        try { if (comp != null) return c.getPackageManager().getActivityIcon(comp).mutate(); }
        catch (PackageManager.NameNotFoundException ignored) {}
        return c.getPackageManager().getDefaultActivityIcon().mutate();
    }
}
