package cat.urysoft.widgets;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Consistent interface icons that scale without raster assets. */
public final class Glyph extends Drawable {
    private final String kind; private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public Glyph(String kind, int color) { this.kind=kind; p.setColor(color); p.setStrokeWidth(1.7f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
    private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
    private void box(Canvas c,float x,float y,float w,float h,float r){c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
    @Override public void draw(Canvas c) {
        int saved=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setStyle(Paint.Style.STROKE);
        switch(kind) {
            case "home": line(c,3,11,12,3);line(c,12,3,21,11);line(c,5,10,5,21);line(c,5,21,19,21);line(c,19,21,19,10);line(c,12,12,12,18);line(c,9,15,15,15);break;
            case "delete": line(c,4,6,20,6);box(c,7,6,10,15,1);line(c,9,3,15,3);line(c,10,10,10,17);line(c,14,10,14,17);break;
            case "refresh": c.drawArc(4,4,20,20,40,290,false,p);line(c,19,3,20,9);line(c,14,8,20,9);break;
            case "contact": c.drawCircle(12,7,4,p);c.drawArc(4,12,20,25,180,180,false,p);break;
            case "palette": c.drawCircle(12,12,9,p);c.drawCircle(8,8,1,p);c.drawCircle(15,7,1,p);c.drawCircle(7,14,1,p);break;
            case "back": line(c,14,5,7,12);line(c,7,12,14,19);line(c,7,12,21,12);break;
            case "add": line(c,12,5,12,19);line(c,5,12,19,12);break;
            case "info": c.drawCircle(12,12,9,p);line(c,12,11,12,17);c.drawPoint(12,7,p);break;
            case "help": c.drawCircle(12,12,9,p);c.drawArc(8,6,16,13,180,220,false,p);line(c,12,12,12,14);c.drawPoint(12,18,p);break;
            case "theme": c.drawCircle(12,12,8,p);p.setStyle(Paint.Style.FILL);c.drawArc(4,4,20,20,90,180,true,p);break;
            case "apps": for(int x=4;x<=14;x+=10)for(int y=4;y<=14;y+=10)box(c,x,y,6,6,1.5f);break;
            case "icons": box(c,4,4,16,16,5);c.drawCircle(12,12,4,p);break;
            case "layers": { Path path=new Path();path.moveTo(12,3);path.lineTo(22,8);path.lineTo(12,13);path.lineTo(2,8);path.close();c.drawPath(path,p);line(c,3,13,12,18);line(c,12,18,21,13);line(c,3,18,12,23);line(c,12,23,21,18);break; }
            case "settings": line(c,4,7,20,7);line(c,4,17,20,17);c.drawCircle(9,7,3,p);c.drawCircle(16,17,3,p);break;
            case "bell": {Path path=new Path();path.moveTo(5,17);path.lineTo(7,14);path.lineTo(7,9);path.cubicTo(7,2,17,2,17,9);path.lineTo(17,14);path.lineTo(19,17);path.close();c.drawPath(path,p);c.drawArc(9,17,15,22,0,180,false,p);break;}
            case "check": line(c,5,12,10,17);line(c,10,17,20,6);break;
            default: {Path path=new Path();path.moveTo(3,8);path.lineTo(3,5);path.quadTo(3,3,5,3);path.lineTo(10,3);path.lineTo(13,6);path.lineTo(19,6);path.quadTo(21,6,21,8);path.lineTo(21,19);path.quadTo(21,21,19,21);path.lineTo(5,21);path.quadTo(3,21,3,19);path.close();c.drawPath(path,p);line(c,3,10,21,10);}
        } c.restoreToCount(saved);
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return 24;}
    @Override public int getIntrinsicHeight(){return 24;}
}
