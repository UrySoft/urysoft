package cat.urysoft.widgets;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.util.TypedValue;

/** Shared layout and rasterizer for icons, titles and their shadows. */
public final class CellRenderer {
    public static final int HALO=32;
    private CellRenderer(){}
    public static final class Metrics {
        public final float width,icon,titleTop,height;
        public final int titleHeight;
        Metrics(float width,float icon,float titleTop,int titleHeight,float height){this.width=width;this.icon=icon;this.titleTop=titleTop;this.titleHeight=titleHeight;this.height=height;}
    }
    public static float textSizeDp(Context c,WidgetConfig v){return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,v.labelSizeSp,c.getResources().getDisplayMetrics())/c.getResources().getDisplayMetrics().density;}
    static StaticLayout headingLayout(Context c,WidgetConfig v,float innerWidthDp){
        android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);paint.setColor(v.labelColor);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14,dm));
        return StaticLayout.Builder.obtain(v.name,0,v.name.length(),paint,Math.max(1,Math.round(innerWidthDp*dm.density)-Ui.dp(c,6)*2)).setIncludePad(true).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).build();
    }
    public static Metrics measure(Context c,WidgetConfig v){
        float width=Math.max(1,v.cellWidthDp>0?v.cellWidthDp:v.iconDp),icon=width;
        TextPaint paint=textPaint(c,v,1);Paint.FontMetrics fm=paint.getFontMetrics();int labelHeight=v.labels?(int)Math.ceil((fm.descent-fm.ascent)*2.1f)+2:0;
        float titleTop=icon+(v.labels?v.labelGapDp:0),height=Math.max(1,titleTop+labelHeight);
        return new Metrics(width,icon,titleTop,labelHeight,height);
    }
    private static TextPaint textPaint(Context c,WidgetConfig v,float unit){
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);paint.setColor(v.labelColor);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(textSizeDp(c,v)*unit);return paint;
    }
    public static Bitmap app(Context c,String component,WidgetConfig v,int count){
        Metrics m=measure(c,v);float width=m.width+HALO*2,height=m.height+HALO*2;android.util.DisplayMetrics display=c.getResources().getDisplayMetrics();
        double budget=display.widthPixels*(double)display.heightPixels*.80/Math.max(1,v.apps.size());
        float unit=Math.min(display.density,Math.min(1024f/Math.max(width,height),(float)Math.sqrt(budget/(width*height))));unit=Math.max(1f/Math.max(width,height),unit);
        int w=Math.max(1,Math.round(width*unit)),h=Math.max(1,Math.round(height*unit));Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
        WidgetConfig plain=WidgetConfig.fromJson(v.toJson());plain.shadowEnabled=false;
        Bitmap icon=IconRenderer.appAtScale(c,component,plain,m.icon,0,unit);float left=(m.width-m.icon)/2*unit;
        canvas.drawBitmap(icon,null,new RectF(left,0,left+(m.icon+HALO*2)*unit,(m.icon+HALO*2)*unit),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));icon.recycle();
        if(v.labels){
            TextPaint paint=textPaint(c,v,unit);String label=AppCatalog.label(c,component);int textWidth=Math.max(1,Math.round(m.width*unit));
            StaticLayout layout=StaticLayout.Builder.obtain(label,0,label.length(),paint,textWidth).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(0,1.1f).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).build();
            canvas.save();canvas.translate(HALO*unit,(HALO+m.titleTop)*unit);layout.draw(canvas);canvas.restore();
        }
        Bitmap result=ShadowRenderer.apply(bitmap,v,unit);
        if(count>0){Canvas front=new Canvas(result);front.translate(left,0);IconRenderer.drawBadge(front,m.icon,HALO,unit,count);}
        result.setDensity(display.densityDpi);
        return result;
    }
}
