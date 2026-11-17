package cat.urysoft.widgets;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Real mask previews, independent of the installed applications. */
public final class ShapeChoice extends View {
 private final int kind;private final Paint p=new Paint(3);
 public ShapeChoice(Context c,int kind,String label){super(c);this.kind=kind;setContentDescription(label);setFocusable(true);setMinimumHeight(Ui.dp(c,76));}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;RectF b=new RectF(getWidth()/2f-23*d,8*d,getWidth()/2f+23*d,54*d);p.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,Ui.BLUE,0xff15b8a6,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);canvas.drawPath(IconRenderer.shape(b,kind==0?2:kind),p);p.setShader(null);p.setColor(Ui.INK);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);canvas.drawText(getContentDescription().toString(),getWidth()/2f,70*d,p);if(isSelected()){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(Ui.BLUE);canvas.drawRoundRect(new RectF(d,d,getWidth()-d,getHeight()-d),10*d,10*d,p);p.setStyle(Paint.Style.FILL);}}
}
