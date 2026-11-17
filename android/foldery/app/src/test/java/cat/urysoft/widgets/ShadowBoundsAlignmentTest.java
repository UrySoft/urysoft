package cat.urysoft.widgets;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w1080dp-h1920dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ShadowBoundsAlignmentTest {
 @Test public void allPositionsAndOverflow(){
  for(int position=0;position<9;position++){
   assertEquals((position%3)*60,AlignmentPolicy.horizontal(120,position));
   assertEquals((position/3)*60,AlignmentPolicy.vertical(120,position));
   assertEquals(0,AlignmentPolicy.vertical(-20,position));
  }
  WidgetConfig v=WidgetConfig.fromJson("{}");assertEquals(4,v.alignment);
  v.alignment=8;assertEquals(8,WidgetConfig.fromJson(v.toJson()).alignment);
 }
 @Test public void actualRemoteViewContainsTheHaloAndKeepsCellDimensions()throws Exception{
  Context c=RuntimeEnvironment.getApplication();String app=new ComponentName(c,MainActivity.class).flattenToString();
  for(int angle=0;angle<360;angle+=45){
   WidgetConfig v=new WidgetConfig();v.apps.add(app);v.columns=1;v.iconDp=160;v.cellWidthDp=160;v.labels=false;v.shadowEnabled=true;v.shadowOpacity=100;v.shadowColor=0xffff00ff;v.shadowSharpness=100;v.shadowDistance=16;v.shadowAngle=angle;
   View view=WidgetRenderer.cell(c,v,0).apply(c,new FrameLayout(c));
   view.measure(View.MeasureSpec.makeMeasureSpec(160,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.AT_MOST));view.layout(0,0,160,view.getMeasuredHeight());
   ImageView image=view.findViewById(R.id.cell_icon);
   assertEquals(224,image.getWidth());assertEquals(224,image.getHeight());assertEquals(-32,image.getLeft());assertEquals(-32,image.getTop());assertEquals(160,view.getHeight());assertEquals(0,image.getPaddingTop());
   Bitmap b=Bitmap.createBitmap(224,224,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.translate(32,32);view.draw(canvas);
   int pixels=0;for(int y=0;y<224;y++)for(int x=0;x<224;x++){int color=b.getPixel(x,y);if((x<32||x>=192||y<32||y>=192)&&Color.red(color)>200&&Color.blue(color)>200&&Color.green(color)<40&&Color.alpha(color)>100)pixels++;}
   assertTrue("Shadow survives outside icon at "+angle,pixels>20);
   String directory=System.getProperty("ury.qa.dir");if(directory!=null){File folder=new File(directory);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"shadow-fixed-"+angle+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
  }
 }
 @Test public void groupUsesAvailableSpaceWithoutChangingIconSize(){
  Context c=RuntimeEnvironment.getApplication();WidgetConfig v=new WidgetConfig();v.apps.add("test/App");v.labels=false;v.editButton=false;v.heading=false;v.columns=0;v.autoIconDp=64;
  int[] offsets=new int[9];
  for(int i=0;i<9;i++){v.alignment=i;WidgetConfig r=WidgetRenderer.resolve(c,v,360,400);assertEquals(64f,r.cellWidthDp,.01f);offsets[i]=r.gridInsetTopPx;if(i>=3)assertTrue(r.gridInsetTopPx>0);}
  assertEquals(offsets[6],offsets[3]*2);v.apps.clear();for(int i=0;i<80;i++)v.apps.add("test/"+i);v.alignment=8;assertEquals(0,WidgetRenderer.resolve(c,v,120,120).gridInsetTopPx);
 }
}
