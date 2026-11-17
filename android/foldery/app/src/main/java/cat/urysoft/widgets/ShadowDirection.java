package cat.urysoft.widgets;

/** Screen coordinates: right is 0 degrees, down is 90. */
public final class ShadowDirection {
    private ShadowDirection() {}
    public static int normalize(int angle) { return ((angle % 360) + 360) % 360; }
    public static float x(float distance, int angle) { return (float)(distance * Math.cos(Math.toRadians(normalize(angle)))); }
    public static float y(float distance, int angle) { return (float)(distance * Math.sin(Math.toRadians(normalize(angle)))); }
    public static String label(android.content.Context c,int angle){
        String[] labels={c.getString(R.string.ui_right_95),c.getString(R.string.ui_bottom_right_96),c.getString(R.string.ui_down_97),c.getString(R.string.ui_bottom_left_98),c.getString(R.string.ui_left_99),c.getString(R.string.ui_top_left_100),c.getString(R.string.ui_up_101),c.getString(R.string.ui_top_right_102)};
        return labels[((normalize(angle)+22)/45)%8];
    }
    public static String label(int angle) {
        String[] labels={"Right","Bottom right","Down","Bottom left","Left","Top left","Up","Top right"};
        return labels[((normalize(angle)+22)/45)%8];
    }
}
