package cat.urysoft.widgets;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;

public final class IconRenderer {
    public static final String[] SHAPES = {"Original","Circle","Rounded square","Square","Hexagon","Diamond"};
    public static String[] shapes(Context c){return new String[]{c.getString(R.string.ui_original_37),c.getString(R.string.ui_circle_38),c.getString(R.string.ui_rounded_square_39),c.getString(R.string.ui_square_40),c.getString(R.string.ui_hexagon_41),c.getString(R.string.ui_diamond_42),c.getString(R.string.shape_triangle),c.getString(R.string.shape_octagon),c.getString(R.string.shape_star),c.getString(R.string.shape_squircle),c.getString(R.string.shape_shield),c.getString(R.string.shape_heart)};}
    public static final String[] STYLES = {"Original colors","Monochrome","Tinted","Framed"};
    public static String[] styles(Context c){return new String[]{c.getString(R.string.ui_original_colors_43),c.getString(R.string.ui_monochrome_44),c.getString(R.string.ui_tinted_45),c.getString(R.string.ui_framed_46),c.getString(R.string.style_sepia),c.getString(R.string.style_vivid),c.getString(R.string.style_soft),c.getString(R.string.style_negative)};}
    public static Bitmap app(Context c, String flat, int size, int count) { return app(c, flat, new WidgetConfig(), size, count); }
    public static Bitmap app(Context c, String flat, WidgetConfig config, int size, int count) {
        android.util.DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        int total = Math.max(1, config.apps.size());
        int memoryCap = Math.max(24, (int)Math.sqrt(metrics.widthPixels * (double)metrics.heightPixels * .85 / total));
        int padding = config.iconPadding();
        int canvasPx = Math.max(1, Math.min(Math.min(512, memoryCap), Math.round((size + padding * 2) * metrics.density)));
        return appPixels(c,flat,config,size,count,canvasPx);
    }
    static Bitmap appAtScale(Context c,String flat,WidgetConfig config,float size,int count,float scale){
        return appPixels(c,flat,config,size,count,Math.max(1,Math.min(1024,Math.round((size+config.iconPadding()*2)*scale))));
    }
    private static Bitmap appPixels(Context c,String flat,WidgetConfig config,float size,int count,int canvasPx){
        int padding=config.iconPadding();
        Bitmap bitmap = Bitmap.createBitmap(canvasPx, canvasPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float unit = canvasPx / (size + padding * 2f), margin = padding * unit, side = size * unit;
        RectF box = new RectF(margin, margin, margin + side, margin + side);
        Drawable icon = AppCatalog.icon(c, flat);
        if (config.iconShape == 0 && config.iconStyle == 0) drawDrawable(canvas, icon, box);
        else {
            int saved = canvas.saveLayer(0, 0, canvasPx, canvasPx, null);
            Path mask = shape(box, config.iconShape == 0 ? 2 : config.iconShape);
            canvas.clipPath(mask);
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG); fill.setColor(config.iconStyle == 2 ? mix(Color.WHITE, config.iconAccent, .12f) : Color.WHITE);
            if(config.iconStyle!=0)canvas.drawRect(box, fill);
            if (config.iconStyle != 3 && icon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable)icon;
                drawDrawable(canvas, adaptive.getBackground(), box);
                RectF foreground = new RectF(box); foreground.inset(-side * .15f, -side * .15f);
                drawDrawable(canvas, adaptive.getForeground(), foreground);
            } else {
                RectF inside = new RectF(box); inside.inset(side * (config.iconStyle == 3 ? .15f : 0f), side * (config.iconStyle == 3 ? .15f : 0f));
                drawCover(canvas, icon, inside);
            }
            canvas.restoreToCount(saved);
            if (config.iconStyle == 1 || config.iconStyle == 2 || config.iconStyle>=4) {
                Paint tone = new Paint(Paint.ANTI_ALIAS_FLAG);
                ColorMatrix matrix = new ColorMatrix(); matrix.setSaturation(0);
                if (config.iconStyle == 2) {
                    float r = Color.red(config.iconAccent)/255f, g = Color.green(config.iconAccent)/255f, b = Color.blue(config.iconAccent)/255f;
                    matrix.set(new float[]{.213f*r,.715f*r,.072f*r,0,0, .213f*g,.715f*g,.072f*g,0,0, .213f*b,.715f*b,.072f*b,0,0, 0,0,0,1,0});
                }
                if(config.iconStyle==4)matrix.set(new float[]{.393f,.769f,.189f,0,0,.349f,.686f,.168f,0,0,.272f,.534f,.131f,0,0,0,0,0,1,0});
                if(config.iconStyle==5)matrix.setSaturation(1.65f);
                if(config.iconStyle==6)matrix.set(new float[]{.8f,0,0,0,38,0,.8f,0,0,38,0,0,.8f,0,38,0,0,0,1,0});
                if(config.iconStyle==7)matrix.set(new float[]{-1,0,0,0,255,0,-1,0,0,255,0,0,-1,0,255,0,0,0,1,0});
                tone.setColorFilter(new ColorMatrixColorFilter(matrix));
                Bitmap toned = Bitmap.createBitmap(canvasPx, canvasPx, Bitmap.Config.ARGB_8888);
                new Canvas(toned).drawBitmap(bitmap, 0, 0, tone); bitmap.recycle();bitmap = toned; canvas = new Canvas(bitmap);
            }
            if (config.iconStyle == 3) {
                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG); border.setColor(config.iconAccent); border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(Math.max(1, side * .035f));
                RectF inset = new RectF(box); inset.inset(border.getStrokeWidth()/2, border.getStrokeWidth()/2);
                canvas.drawPath(shape(inset, config.iconShape == 0 ? 2 : config.iconShape), border);
            }
        }
        bitmap=ShadowRenderer.apply(bitmap,config,unit);canvas=new Canvas(bitmap);
        if (count > 0) drawBadge(canvas, size, padding, unit, count);
        return bitmap;
    }
    // Fill the chosen mask instead of fitting an inset square inside it.
    private static void drawCover(Canvas canvas,Drawable drawable,RectF box){
        if(drawable==null)return;
        int iw=Math.max(1,drawable.getIntrinsicWidth()),ih=Math.max(1,drawable.getIntrinsicHeight());
        float scale=Math.max(box.width()/iw,box.height()/ih);
        int w=Math.round(iw*scale),h=Math.round(ih*scale);
        int x=Math.round(box.centerX()-w/2f),y=Math.round(box.centerY()-h/2f);
        drawable.setBounds(x,y,x+w,y+h);drawable.draw(canvas);
    }
    private static void drawDrawable(Canvas canvas, Drawable drawable, RectF box) {
        if (drawable == null) return;
        int iw = Math.max(1, drawable.getIntrinsicWidth()), ih = Math.max(1, drawable.getIntrinsicHeight());
        float scale = Math.min(box.width()/iw, box.height()/ih);
        int w = Math.round(iw*scale), h = Math.round(ih*scale);
        int x = Math.round(box.centerX()-w/2f), y = Math.round(box.centerY()-h/2f);
        drawable.setBounds(x,y,x+w,y+h); drawable.draw(canvas);
    }
    public static Path shape(RectF box, int kind) {
        Path path = new Path();
        if(kind==6||kind==7||kind==8){int n=kind==6?3:kind==7?8:10;for(int i=0;i<n;i++){double a=-Math.PI/2+i*Math.PI*2/n;float radius=kind==8&&i%2==1?.47f:1f;float x=box.centerX()+(float)Math.cos(a)*box.width()/2*radius,y=box.centerY()+(float)Math.sin(a)*box.height()/2*radius;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();}
        else if(kind==9){for(int i=0;i<=120;i++){double a=i*Math.PI*2/120;float x=box.centerX()+(float)(Math.signum(Math.cos(a))*Math.pow(Math.abs(Math.cos(a)),.5))*box.width()/2,y=box.centerY()+(float)(Math.signum(Math.sin(a))*Math.pow(Math.abs(Math.sin(a)),.5))*box.height()/2;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();}
        else if(kind==10){path.moveTo(box.left,box.top);path.lineTo(box.right,box.top);path.cubicTo(box.right,box.centerY(),box.right,box.top+box.height()*.9f,box.centerX(),box.bottom);path.cubicTo(box.left,box.top+box.height()*.9f,box.left,box.centerY(),box.left,box.top);path.close();}
        else if(kind==11){float x=box.left,y=box.top,w=box.width(),h=box.height();path.moveTo(x+w/2,y+h);path.cubicTo(x-w*.2f,y+h*.45f,x,y-h*.2f,x+w/2,y+h*.22f);path.cubicTo(x+w,y-h*.2f,x+w*1.2f,y+h*.45f,x+w/2,y+h);path.close();}
        else if (kind == 1) path.addOval(box, Path.Direction.CW);
        else if (kind == 4 || kind == 5) {
            int points = kind == 4 ? 6 : 4;
            for (int i=0;i<points;i++) {
                double angle = -Math.PI/2 + i*Math.PI*2/points;
                float x=box.centerX()+(float)Math.cos(angle)*box.width()/2, y=box.centerY()+(float)Math.sin(angle)*box.height()/2;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();
        } else if (kind == 3) path.addRect(box, Path.Direction.CW);
        else path.addRoundRect(box, box.width()*.23f, box.height()*.23f, Path.Direction.CW);
        return path;
    }
    private static int mix(int a, int b, float t) { return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t), Math.round(Color.green(a)*(1-t)+Color.green(b)*t), Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t)); }
    static void drawBadge(Canvas canvas, float size, int padding, float unit, int count) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); String value=count>99?"99+":Integer.toString(count);
        // Paint on top of the icon, inside its bounds: no clipping by the
        // collection viewport or adjacent cells, even with zero spacing.
        float radius=Math.min(18,size*.22f)*unit;
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextSize(radius*1.25f);
        float inset=Math.min(2,size*.04f)*unit,stroke=Math.min(1,size*.025f)*unit;
        float available=Math.max(1,size*unit-inset*2-stroke*2);
        float width=Math.min(available,Math.max(radius*2,p.measureText(value)+8*unit));
        if(p.measureText(value)>width-4*unit)p.setTextSize(p.getTextSize()*Math.max(.1f,(width-4*unit)/p.measureText(value)));
        float right=(padding+size)*unit-inset-stroke,top=padding*unit+inset+stroke;
        RectF pill=new RectF(right-width,top,right,top+Math.min(radius*2,available));
        p.setColor(Color.WHITE);canvas.drawRoundRect(new RectF(pill.left-stroke,pill.top-stroke,pill.right+stroke,pill.bottom+stroke),radius+stroke,radius+stroke,p);
        p.setColor(0xffc52e46);canvas.drawRoundRect(pill,radius,radius,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(value,pill.centerX(),pill.centerY()-(p.ascent()+p.descent())/2,p);
    }
    public static Bitmap background(Context c, WidgetConfig config, int widthDp, int heightDp) {
        android.util.DisplayMetrics metrics=c.getResources().getDisplayMetrics();
        float memoryScale=(float)Math.sqrt(metrics.widthPixels*(double)metrics.heightPixels*.25/Math.max(1,widthDp*(double)heightDp));
        float scale=Math.min(memoryScale,Math.min(metrics.density,512f/Math.max(1,Math.max(widthDp,heightDp))));
        int w=Math.max(1,Math.round(widthDp*scale)),h=Math.max(1,Math.round(heightDp*scale));
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(config.color());
        Canvas canvas=new Canvas(bitmap);Path mask=new Path();mask.addRoundRect(new RectF(0,0,w,h),config.cornerDp*scale,config.cornerDp*scale,Path.Direction.CW);canvas.clipPath(mask);canvas.drawColor(config.color());BackgroundStore.draw(c,canvas,config,new RectF(0,0,w,h));return bitmap;
    }
}
