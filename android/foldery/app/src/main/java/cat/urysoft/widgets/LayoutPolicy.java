package cat.urysoft.widgets;

/** Width-only sizing, shared by the real widget and its preview. */
public final class LayoutPolicy {
    public static final int MAX_APPS=48,MIN_ICON=28,MAX_ICON=2048,CELL_TYPES=1,AUTO_ICON=64;
    private LayoutPolicy(){}
    public static int automaticColumnPixels(int availableColumnPixels,int targetPixels){return Math.max(1,Math.min(availableColumnPixels,targetPixels));}
    public static int iconSize(int size){return Math.max(1,Math.min(MAX_ICON,size));}
    public static int innerWidth(int width,int padding){return Math.max(MIN_ICON,width-Math.max(0,padding)*2);}
    public static int responsiveColumns(int width,int padding,int gap,int requested,int items){
        return responsiveColumns(width,padding,gap,requested,items,AUTO_ICON);
    }
    public static int responsiveColumns(int width,int padding,int gap,int requested,int items,int target){
        int inner=innerWidth(width,padding),minimum=MIN_ICON+Math.max(0,gap);int fit=Math.max(1,Math.min(8,(inner+gap)/minimum));
        if(requested>0)return Math.max(1,Math.min(requested,fit));
        int automatic=Math.max(1,(inner+gap)/(Math.max(28,Math.min(300,target))+Math.max(0,gap)));
        return Math.max(1,Math.min(Math.min(fit,automatic),Math.max(1,items)));
    }
    public static int effectiveGap(int width,int padding,int columns,int desired){
        if(columns<=1)return 0;
        int maximum=Math.max(0,(innerWidth(width,padding)-columns*MIN_ICON)/(columns-1));
        return Math.min(Math.max(0,desired),maximum-(maximum&1));
    }
    public static float columnWidth(int width,int padding,int gap,int columns){return Math.max(1,(innerWidth(width,padding)-(columns-1)*gap)/(float)Math.max(1,columns));}
    public static int headingHeight(boolean heading,float fontScale){return heading?12+(int)Math.ceil(34*Math.max(1f,fontScale)):0;}
    public static boolean shouldPinPreview(int previewHeight,int availableHeight){return previewHeight>0&&availableHeight>0&&previewHeight*2<availableHeight;}
}
