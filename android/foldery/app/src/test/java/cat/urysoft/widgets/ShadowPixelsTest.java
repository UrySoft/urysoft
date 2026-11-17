package cat.urysoft.widgets;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public final class ShadowPixelsTest {
    private Context c;private String flat;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();flat=new ComponentName(c,MainActivity.class).flattenToString();}
    private WidgetConfig config(){WidgetConfig v=new WidgetConfig();v.apps.add(flat);v.iconDp=160;v.iconShape=1;v.shadowEnabled=true;v.shadowOpacity=100;v.shadowColor=0xffbc18b9;v.shadowSharpness=100;v.shadowDistance=16;return v;}
    @Test public void coloredShadowFollowsTheSilhouetteWhileIconAndBackgroundStayUnchanged(){
        WidgetConfig v=config();Bitmap shaded=IconRenderer.app(c,flat,v,160,0);v.shadowOpacity=0;Bitmap plain=IconRenderer.app(c,flat,v,160,0);
        int x=112,y=200;assertEquals(0,Color.alpha(plain.getPixel(x,y)));assertEquals(0xffbc18b9,shaded.getPixel(x,y));assertEquals(plain.getPixel(112,112),shaded.getPixel(112,112));assertEquals(0,v.opacity);
        v.shadowOpacity=100;v.shadowDistance=0;Bitmap near=IconRenderer.app(c,flat,v,160,0);assertEquals(0,Color.alpha(near.getPixel(x,y)));
        v.shadowDistance=16;v.shadowOpacity=40;Bitmap faint=IconRenderer.app(c,flat,v,160,0);assertTrue(Color.alpha(faint.getPixel(x,y))<Color.alpha(shaded.getPixel(x,y)));
    }
    @Test public void softnessChangesRealPixelsAndExtremeShadowsRemainInsideTheirCanvas()throws Exception{
        WidgetConfig v=config();Bitmap sharp=IconRenderer.app(c,flat,v,160,0);v.shadowSharpness=0;Bitmap soft=IconRenderer.app(c,flat,v,160,0);int sharpPartial=0,softPartial=0;
        for(int y=0;y<soft.getHeight();y++)for(int x=0;x<soft.getWidth();x++){int a=Color.alpha(sharp.getPixel(x,y)),b=Color.alpha(soft.getPixel(x,y));if(a>0&&a<250)sharpPartial++;if(b>0&&b<250)softPartial++;}
        assertTrue("Soft shadow should have a wider alpha transition",softPartial>sharpPartial*2);
        for(int i=0;i<soft.getWidth();i++){assertEquals(0,Color.alpha(soft.getPixel(i,0)));assertEquals(0,Color.alpha(soft.getPixel(i,soft.getHeight()-1)));assertEquals(0,Color.alpha(soft.getPixel(0,i)));assertEquals(0,Color.alpha(soft.getPixel(soft.getWidth()-1,i)));}
        Bitmap strip=Bitmap.createBitmap(448,224,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(strip);canvas.drawColor(0xffcdd5e5);canvas.drawBitmap(sharp,0,0,null);canvas.drawBitmap(soft,224,0,null);save(strip,"icon-shadow-comparison.png");
    }
    @Test public void renderHomeEditorAndShadowControlsInBothThemes()throws Exception{
        for(int mode:new int[]{1,2}){
            c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",mode).commit();String theme=mode==2?"dark":"light";
            try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){save(render(home.get().getWindow().getDecorView(),360,800),"foldery-home-"+theme+".png");}
            WidgetConfig v=config();v.iconDp=64;v.shadowColor=Color.BLACK;v.shadowSharpness=35;v.shadowOpacity=55;v.shadowDistance=6;v.columns=2;v.apps.add(new ComponentName(c,ConfigureActivity.class).flattenToString());v.apps.add(new ComponentName(c,LaunchActivity.class).flattenToString());Bundle state=new Bundle();state.putString("config",v.toJson());state.putInt("panel",1);
            try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));View decor=editor.get().getWindow().getDecorView();save(render(decor,360,800),"foldery-editor-"+theme+".png");
                ScrollView scroll=editor.get().findViewById(R.id.settings_scroll);View card=editor.get().findViewById(R.id.shadow_controls);scroll.scrollTo(0,((View)card.getParent()).getTop()+((View)((View)card.getParent()).getParent()).getTop()+((View)((View)((View)card.getParent()).getParent()).getParent()).getTop());save(render(decor,360,800),"foldery-shadow-"+theme+".png");
            }
        }
    }
    @Test public void controlsStayInsideACompactScreenWithLargeText()throws Exception{
        android.content.res.Configuration original=new android.content.res.Configuration(c.getResources().getConfiguration());android.content.res.Configuration large=new android.content.res.Configuration(original);large.fontScale=1.5f;c.getResources().updateConfiguration(large,c.getResources().getDisplayMetrics());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            View decor=editor.get().getWindow().getDecorView();save(render(decor,320,800),"foldery-large-text.png");
            View root=editor.get().findViewById(R.id.editor_root);assertTrue(root.getWidth()<=320);assertTrue(editor.get().findViewById(R.id.tab_apps).getHeight()>=48);
        }finally{c.getResources().updateConfiguration(original,c.getResources().getDisplayMetrics());}
    }
    private static Bitmap render(View view,int width,int height){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));return b;}
    private static void save(Bitmap image,String name)throws Exception{String directory=System.getProperty("ury.qa.dir");if(directory==null)return;File folder=new File(directory);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
