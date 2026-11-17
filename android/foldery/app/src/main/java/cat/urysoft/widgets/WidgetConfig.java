package cat.urysoft.widgets;

import android.graphics.Color;
import org.json.*;
import java.util.*;

public final class WidgetConfig {
    public String backgroundImage="";
    public int backgroundMode=0;
    public String name = "My shortcuts";
    public final ArrayList<String> apps = new ArrayList<>();
    public int iconDp = 48, background = 0xff18233e, opacity = 0, labelColor = Color.WHITE, columns = 0, cornerDp = 24;
    public int iconShape = 0, iconStyle = 0, iconAccent = 0xff3159de;
    public int labelSizeSp = 12, labelGapDp = 6, paddingHorizontalDp = 12, paddingVerticalDp = 12;
    public int columnGapDp = 12, rowGapDp = 12;
    public int autoIconDp = 64;
    public int alignment = 4; // Nine positions, row-major; center by default.
    public int gridInsetTopPx;
    public boolean autoSized; // Resolved layout only; not persisted.
    // Recalculated for each widget size; this value is never persisted.
    public float cellWidthDp;
    public int gridInsetLeftPx, gridInsetRightPx; // Resolved, never persisted.
    public boolean shadowEnabled = false;
    public int shadowColor = Color.BLACK, shadowOpacity = 45, shadowSharpness = 60, shadowDistance = 4;
    public int shadowAngle = 90;
    public boolean labels = true, badges = true, heading = false, editButton = true, expandIcons = false;

    // Shadows paint behind the icon without changing the cell or label position.
    public int iconPadding() { return 32; }
    public int extraPadding() { return 0; }

    public int color() { return (background & 0x00ffffff) | (Math.round(opacity * 255f / 100f) << 24); }
    public String toJson() {
        try {
            JSONObject j = new JSONObject();
            j.put("alignment",alignment);j.put("autoIconDp",autoIconDp);j.put("backgroundImage",backgroundImage);j.put("backgroundMode",backgroundMode);j.put("name", name); j.put("apps", new JSONArray(apps)); j.put("iconDp", iconDp);
            j.put("background", background); j.put("opacity", opacity); j.put("labelColor", labelColor);
            j.put("columns", columns); j.put("cornerDp", cornerDp); j.put("labels", labels);
            j.put("labelSizeSp",labelSizeSp);j.put("labelGapDp",labelGapDp);j.put("paddingHorizontalDp",paddingHorizontalDp);j.put("paddingVerticalDp",paddingVerticalDp);j.put("columnGapDp",columnGapDp);j.put("rowGapDp",rowGapDp);
            j.put("badges", badges); j.put("heading", heading); j.put("editButton", editButton);
            j.put("expandIcons", expandIcons); j.put("iconShape", iconShape); j.put("iconStyle", iconStyle); j.put("iconAccent", iconAccent);
            j.put("shadowEnabled", shadowEnabled); j.put("shadowColor", shadowColor); j.put("shadowOpacity", shadowOpacity);
            j.put("shadowSharpness", shadowSharpness); j.put("shadowDistance", shadowDistance); j.put("shadowAngle", shadowAngle);
            return j.toString();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }
    public static WidgetConfig defaults(android.content.Context c){WidgetConfig v=new WidgetConfig();v.name=Language.context(c).getString(R.string.default_widget_name);return v;}
    public static WidgetConfig fromJson(String raw) {
        WidgetConfig c = new WidgetConfig();
        if (raw == null) return c;
        try {
            JSONObject j = new JSONObject(raw);
            c.alignment=bound(j.optInt("alignment",4),0,8);
            c.autoIconDp=bound(j.optInt("autoIconDp",64),28,300);
            c.backgroundImage=j.optString("backgroundImage","");if(!c.backgroundImage.matches("[a-f0-9-]{36}"))c.backgroundImage="";c.backgroundMode=bound(j.optInt("backgroundMode",0),0,4);
            c.name = j.optString("name", c.name); c.iconDp = LayoutPolicy.iconSize(j.optInt("iconDp", 48));
            c.background = j.optInt("background", c.background); c.opacity = Math.max(0, Math.min(100, j.optInt("opacity", 0)));
            c.labelColor = j.optInt("labelColor", c.labelColor); c.columns = Math.max(0, Math.min(8, j.optInt("columns", 0)));
            c.labelSizeSp=bound(j.optInt("labelSizeSp",12),8,32);c.labelGapDp=bound(j.optInt("labelGapDp",6),0,32);
            c.paddingHorizontalDp=bound(j.optInt("paddingHorizontalDp",12),0,40);c.paddingVerticalDp=bound(j.optInt("paddingVerticalDp",12),0,40);
            c.columnGapDp=evenGap(j.optInt("columnGapDp",12));c.rowGapDp=evenGap(j.optInt("rowGapDp",12));
            c.cornerDp = Math.max(0, Math.min(32, j.optInt("cornerDp", 24)));
            c.labels = j.optBoolean("labels", true); c.badges = j.optBoolean("badges", true);
            c.heading = j.optBoolean("heading", false); c.editButton = j.optBoolean("editButton", true);
            c.expandIcons = j.optBoolean("expandIcons", false);
            c.iconShape = Math.max(0, Math.min(11, j.optInt("iconShape", 0)));
            c.iconStyle = Math.max(0, Math.min(7, j.optInt("iconStyle", 0)));
            c.iconAccent = j.optInt("iconAccent", c.iconAccent) | 0xff000000;
            c.shadowEnabled = j.optBoolean("shadowEnabled", false);
            c.shadowColor = j.optInt("shadowColor", Color.BLACK) | 0xff000000;
            c.shadowOpacity = Math.max(0, Math.min(100, j.optInt("shadowOpacity", 45)));
            c.shadowSharpness = Math.max(0, Math.min(100, j.optInt("shadowSharpness", 60)));
            c.shadowDistance = Math.max(0, Math.min(16, j.optInt("shadowDistance", 4)));
            c.shadowAngle = ShadowDirection.normalize(j.optInt("shadowAngle", 90));
            JSONArray a = j.optJSONArray("apps");
            if (a != null) for (int i = 0; i < a.length() && c.apps.size() < LayoutPolicy.MAX_APPS; i++) {
                String v = a.optString(i); if (!v.isEmpty() && !c.apps.contains(v)) c.apps.add(v);
            }
        } catch (JSONException ignored) { /* Keep safe defaults if old data is malformed. */ }
        return c;
    }
    private static int bound(int value,int low,int high){return Math.max(low,Math.min(high,value));}
    private static int evenGap(int value){return bound(Math.round(value/2f)*2,0,40);}
}
