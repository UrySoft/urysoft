package cat.urysoft.widgets;

import android.content.*;
import android.content.pm.ProviderInfo;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public final class ContactAndDirectionPixelsTest {
    private Context c;private String app;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();app=new ComponentName(c,MainActivity.class).flattenToString();}
    @Test public void allEightDirectionsMoveTheActualShadowWithoutChangingTheIcon()throws Exception{
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.iconDp=160;v.iconShape=1;v.shadowEnabled=true;v.shadowOpacity=100;v.shadowColor=0xffd32fa0;v.shadowSharpness=100;v.shadowDistance=16;
        Bitmap sheet=Bitmap.createBitmap(896,448,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(sheet);canvas.drawColor(0xffd2d9e6);
        for(int angle=0;angle<360;angle+=45){v.shadowAngle=angle;Bitmap image=IconRenderer.app(c,app,v,160,0);int x=Math.round(112+ShadowDirection.x(88,angle)),y=Math.round(112+ShadowDirection.y(88,angle));assertEquals("Shadow direction "+angle,v.shadowColor,image.getPixel(x,y));
            v.shadowSharpness=0;Bitmap diffuse=IconRenderer.app(c,app,v,160,0);v.shadowSharpness=100;
            for(int edge=0;edge<224;edge++){assertEquals(0,Color.alpha(diffuse.getPixel(edge,0)));assertEquals(0,Color.alpha(diffuse.getPixel(edge,223)));assertEquals(0,Color.alpha(diffuse.getPixel(0,edge)));assertEquals(0,Color.alpha(diffuse.getPixel(223,edge)));}
            int i=angle/45;canvas.drawBitmap(image,(i%4)*224,(i/4)*224,null);
        }save(sheet,"shadow-eight-directions.png");
    }
    @Test public void contactPhotoSurvivesLossOfProviderAccessAndUsesTheSameWidgetRenderer()throws Exception{
        ContactFeaturesTest.FakeContacts provider=installProvider(true);ContactStore.Entry entry=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);assertTrue(entry.hasPhoto);provider.denied=true;
        WidgetConfig v=new WidgetConfig();v.apps.add(entry.id);v.iconDp=96;v.iconShape=1;Bitmap photo=IconRenderer.app(c,entry.id,v,96,0);assertEquals(0xffe43d58,photo.getPixel(photo.getWidth()/2,photo.getHeight()/2));
        View cell=WidgetRenderer.cell(c,v,0).apply(c,new FrameLayout(c));assertEquals("Laia Riera",cell.getContentDescription().toString());assertNotNull(((ImageView)cell.findViewById(R.id.cell_icon)).getDrawable());assertEquals(0,provider.wholeBookQueries);
    }
    @Test public void mixedPreviewAndDirectionControlsRenderInBothThemes()throws Exception{
        ContactFeaturesTest.FakeContacts provider=installProvider(true);ContactStore.Entry contact=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);
        for(int mode:new int[]{1,2}){
            c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",mode).commit();
            WidgetConfig v=new WidgetConfig();v.apps.add(app);v.apps.add(contact.id);v.iconDp=64;v.columns=2;v.shadowEnabled=true;v.shadowSharpness=45;v.shadowOpacity=65;v.shadowAngle=315;v.shadowDistance=9;
            Bundle state=new Bundle();state.putString("config",v.toJson());state.putInt("panel",1);
            try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));View decor=editor.get().getWindow().getDecorView();render(decor);
                ScrollView scroll=editor.get().findViewById(R.id.settings_scroll);View angle=editor.get().findViewById(R.id.shadow_angle);android.graphics.Rect rect=new android.graphics.Rect(0,0,angle.getWidth(),angle.getHeight());scroll.offsetDescendantRectToMyCoords(angle,rect);scroll.scrollTo(0,Math.max(0,rect.top-Ui.dp(c,210)));save(render(decor),mode==2?"contacts-directions-dark.png":"contacts-directions-light.png");
                editor.get().findViewById(R.id.tab_apps).performClick();save(render(decor),mode==2?"contacts-selection-dark.png":"contacts-selection-light.png");
            }
        }
    }
    private ContactFeaturesTest.FakeContacts installProvider(boolean withPhoto)throws Exception{
        ContactFeaturesTest.FakeContacts provider=new ContactFeaturesTest.FakeContacts();ProviderInfo info=new ProviderInfo();info.authority=ContactsContract.AUTHORITY;provider.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,provider);provider.hasPhoto=withPhoto;
        provider.photo=new File(c.getCacheDir(),"synthetic-contact-photo.png");Bitmap bitmap=Bitmap.createBitmap(96,64,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffe43d58);try(FileOutputStream out=new FileOutputStream(provider.photo)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}return provider;
    }
    private static Bitmap render(View view){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));view.layout(0,0,360,800);}Bitmap b=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));return b;}
    private static void save(Bitmap image,String name)throws Exception{String directory=System.getProperty("ury.qa.dir");if(directory==null)return;File folder=new File(directory);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
