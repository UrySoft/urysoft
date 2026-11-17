package cat.urysoft.widgets;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
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
public final class PreviewPixelsTest {
    private Context c;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();}
    @Test public void transparentBackgroundKeepsIconPixelsOpaqueAcrossRepeatedChanges()throws Exception{
        WidgetConfig v=new WidgetConfig();v.labels=false;v.editButton=false;v.iconDp=80;v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());
        PreviewView preview=new PreviewView(c);preview.configure(v);preview.layout(0,0,320,220);Rect cell=new Rect();preview.getAccessibilityNodeProvider().createAccessibilityNodeInfo(1).getBoundsInParent(cell);
        Bitmap transparent=draw(preview);v.opacity=100;preview.configure(v);Bitmap opaque=draw(preview);v.opacity=0;preview.configure(v);Bitmap transparentAgain=draw(preview);
        int x=cell.centerX(),y=cell.top+40;assertEquals(opaque.getPixel(x,y),transparent.getPixel(x,y));assertEquals(opaque.getPixel(x,y),transparentAgain.getPixel(x,y));
        WidgetConfig empty=WidgetConfig.fromJson(v.toJson());empty.apps.clear();PreviewView blank=new PreviewView(c);blank.configure(empty);blank.layout(0,0,320,220);assertNotEquals(draw(blank).getPixel(x,y),transparent.getPixel(x,y));
        String directory=System.getProperty("ury.qa.dir");if(directory!=null){File folder=new File(directory);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"transparent-preview.png"))){transparent.compress(Bitmap.CompressFormat.PNG,100,out);}}
    }
    @Test public void shapeMasksStylesAndBadgesRenderWithoutChangingTheWidgetBackground(){
        WidgetConfig v=new WidgetConfig();String flat=new ComponentName(c,MainActivity.class).flattenToString();v.apps.add(flat);v.iconDp=160;
        for(int shape=0;shape<6;shape++)for(int style=0;style<4;style++){
            v.iconShape=shape;v.iconStyle=style;Bitmap image=IconRenderer.app(c,flat,v,160,0);assertTrue(image.getWidth()>128);assertEquals(0,Color.alpha(image.getPixel(0,0)));assertTrue(Color.alpha(image.getPixel(image.getWidth()/2,image.getHeight()/2))>0);
            if(shape==1||shape==4||shape==5){int p=Math.round(image.getWidth()*8f/172);assertEquals(0,Color.alpha(image.getPixel(p,p)));}
        }
        v.iconShape=1;Bitmap plain=IconRenderer.app(c,flat,v,160,0),badge=IconRenderer.app(c,flat,v,160,8);boolean changed=false;
        for(int y=1;y<badge.getHeight()/4;y++)for(int x=badge.getWidth()/2;x<badge.getWidth()-1;x++)if(plain.getPixel(x,y)!=badge.getPixel(x,y))changed=true;
        assertTrue(changed);assertEquals(0,v.opacity);
    }
    @Test public void darkEditorRendersTheInteractivePreviewAndSettings()throws Exception{
        c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",2).commit();
        WidgetConfig v=new WidgetConfig();v.iconDp=64;v.iconShape=1;v.iconStyle=3;v.iconAccent=0xff48c4b1;v.columns=2;
        v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());v.apps.add(new ComponentName(c,ConfigureActivity.class).flattenToString());v.apps.add(new ComponentName(c,LaunchActivity.class).flattenToString());
        Bundle state=new Bundle();state.putString("config",v.toJson());
        try(ActivityController<ConfigureActivity> controller=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150));
            View decor=controller.get().getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800);
            Bitmap image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(image));assertTrue(Ui.DARK);
            String directory=System.getProperty("ury.qa.dir");if(directory!=null){File folder=new File(directory);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,"dark-editor.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}
        }
    }
    private Bitmap draw(PreviewView preview){Bitmap b=Bitmap.createBitmap(320,220,Bitmap.Config.ARGB_8888);preview.draw(new Canvas(b));return b;}
}
