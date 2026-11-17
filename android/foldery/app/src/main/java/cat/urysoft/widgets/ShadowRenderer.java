package cat.urysoft.widgets;

import android.graphics.*;

/** One silhouette shadow for the icon and its title, in screen coordinates. */
public final class ShadowRenderer {
    private ShadowRenderer(){}
    public static Bitmap apply(Bitmap source,WidgetConfig config,float unit){
        if(!config.shadowEnabled||config.shadowOpacity<=0)return source;
        float radius=(100-config.shadowSharpness)*.08f*unit;Paint maskPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        if(radius>.01f)maskPaint.setMaskFilter(new BlurMaskFilter(radius,BlurMaskFilter.Blur.NORMAL));
        int[] offset=new int[2];Bitmap mask=source.extractAlpha(maskPaint,offset);
        Bitmap result=Bitmap.createBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);
        Paint shadow=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);shadow.setColor(config.shadowColor);shadow.setAlpha(Math.round(config.shadowOpacity*255f/100f));
        canvas.drawBitmap(mask,offset[0]+ShadowDirection.x(config.shadowDistance*unit,config.shadowAngle),offset[1]+ShadowDirection.y(config.shadowDistance*unit,config.shadowAngle),shadow);
        canvas.drawBitmap(source,0,0,null);mask.recycle();source.recycle();return result;
    }
}
