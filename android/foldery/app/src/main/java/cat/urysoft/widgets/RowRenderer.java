package cat.urysoft.widgets;
import android.content.Context;
import android.graphics.*;
/** Composite complete row strips before handing them to the launcher. No shadow crosses a child view boundary. */
public final class RowRenderer {
    private RowRenderer(){}
    public static int count(WidgetConfig v){return (v.apps.size()+Math.max(1,v.columns)-1)/Math.max(1,v.columns);}
    public static float top(Context c,WidgetConfig v,int row){CellRenderer.Metrics m=CellRenderer.measure(c,v);return row*(m.height+v.rowGapDp)-(row==0?v.shadowOutsetDp:0);}
    public static float height(Context c,WidgetConfig v,int row){return CellRenderer.measure(c,v).height+(row==count(v)-1?v.shadowOutsetDp:v.rowGapDp)+(row==0?v.shadowOutsetDp:0);}
    public static Bitmap render(Context c,WidgetConfig v,int row){
        CellRenderer.Metrics m=CellRenderer.measure(c,v);float width=Math.max(1,v.surfaceWidthDp),height=height(c,v,row),top=top(c,v,row),halo=CellRenderer.HALO;
        android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();
        float totalHeight=count(v)*(m.height+v.rowGapDp)+v.shadowOutsetDp*2;
        float unit=Math.min(dm.density,(float)Math.sqrt(dm.widthPixels*(double)dm.heightPixels*.7/Math.max(1,(width+halo*2)*(totalHeight+halo*2*count(v)))));unit=Math.max(.05f,unit);
        int fullW=Math.max(1,Math.round((width+halo*2)*unit)),fullH=Math.max(1,Math.round((height+halo*2)*unit));
        Bitmap plane=Bitmap.createBitmap(fullW,fullH,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(plane);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        WidgetConfig plain=WidgetConfig.fromJson(v.toJson());plain.cellWidthDp=v.cellWidthDp;plain.shadowEnabled=false;
        float left=v.paddingHorizontalDp+v.gridInsetLeftPx/dm.density;
        for(int i=0;i<v.apps.size();i++){
            float x=left+(i%Math.max(1,v.columns))*(m.width+v.columnGapDp),y=(i/Math.max(1,v.columns))*(m.height+v.rowGapDp);
            if(y+m.height+halo<top-halo||y-halo>top+height+halo)continue;
            Bitmap cell=CellRenderer.app(c,v.apps.get(i),plain,0);
            canvas.drawBitmap(cell,null,new RectF(x*unit,(y-top)*unit,(x+m.width+halo*2)*unit,(y-top+m.height+halo*2)*unit),paint);cell.recycle();
        }
        plane=ShadowRenderer.apply(plane,v,unit);canvas=new Canvas(plane);
        if(v.badges)for(int i=0;i<v.apps.size();i++){
            int count=BadgeState.get(c,v.apps.get(i));if(count<=0)continue;
            float x=left+(i%Math.max(1,v.columns))*(m.width+v.columnGapDp),y=(i/Math.max(1,v.columns))*(m.height+v.rowGapDp);
            canvas.save();canvas.translate((x+halo+m.iconLeft)*unit,(y-top+halo+m.iconTop)*unit);IconRenderer.drawBadge(canvas,m.icon,0,unit,count);canvas.restore();
        }
        int x=Math.round(halo*unit),y=Math.round(halo*unit),w=Math.min(fullW-x,Math.max(1,Math.round(width*unit))),h=Math.min(fullH-y,Math.max(1,Math.round(height*unit)));
        Bitmap result=Bitmap.createBitmap(plane,x,y,w,h);if(result!=plane)plane.recycle();result.setDensity(dm.densityDpi);return result;
    }
}
