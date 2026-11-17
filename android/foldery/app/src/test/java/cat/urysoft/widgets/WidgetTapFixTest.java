package cat.urysoft.widgets;

import android.appwidget.*;
import android.content.*;
import android.content.pm.ProviderInfo;
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
import org.robolectric.shadows.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public final class WidgetTapFixTest {
    private Context c;private String app;private ContactStore.Entry contact;
    @Before public void setup()throws Exception{
        c=RuntimeEnvironment.getApplication();app=new ComponentName(c,MainActivity.class).flattenToString();
        ContactFeaturesTest.FakeContacts p=new ContactFeaturesTest.FakeContacts();p.advertisePhoto=true;p.directOnly=true;ProviderInfo info=new ProviderInfo();info.authority=android.provider.ContactsContract.AUTHORITY;p.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(info.authority,p);
        p.photo=new File(c.getCacheDir(),"synthetic-widget-photo.png");Bitmap b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);b.eraseColor(0xffe43d58);try(FileOutputStream out=new FileOutputStream(p.photo)){b.compress(Bitmap.CompressFormat.PNG,100,out);}contact=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);
    }
    @Test public void realCollectionItemsSendAppAndContactReferencesThroughTheWidgetTemplate()throws Exception{
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.apps.add(contact.id);v.columns=2;ConfigStore.put(c,730,v);
        try(ActivityController<android.app.Activity> window=Robolectric.buildActivity(android.app.Activity.class).setup().visible()){
        AppWidgetHostView host=new AppWidgetHostView(window.get());View root=WidgetRenderer.build(c,730,v,360,240).apply(c,host);host.addView(root);window.get().setContentView(host);GridView grid=root.findViewById(R.id.widget_grid);
        measure(root,360,240);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));measure(root,360,240);
        assertNotNull(grid.getAdapter());assertEquals(2,grid.getAdapter().getCount());
        for(int index=0;index<2;index++){
            View cell=grid.getChildAt(index);assertNotNull(cell);assertFalse(cell.hasFocusable());
            long now=SystemClock.uptimeMillis();float x=cell.getLeft()+cell.getWidth()/2f,y=cell.getTop()+20;
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_UP,x,y,0);
            grid.dispatchTouchEvent(down);grid.dispatchTouchEvent(up);down.recycle();up.recycle();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
            Intent intent=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();assertNotNull("A desktop tap must launch an activity",intent);assertEquals(730,intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1));assertEquals(v.apps.get(index),intent.getStringExtra("component"));
            try(ActivityController<LaunchActivity> launch=Robolectric.buildActivity(LaunchActivity.class,intent).create()){
                Intent target=Shadows.shadowOf(launch.get()).getNextStartedActivity();assertNotNull(target);
                if(index==0)assertEquals(ComponentName.unflattenFromString(app),target.getComponent());
                else {assertEquals(Intent.ACTION_VIEW,target.getAction());assertEquals(contact.uri,target.getData());}
            }
        }
        }
    }
    @Test public void previewPhotoAndLabelsStayAtTheSameCoordinatesWhenAShadowIsEnabled()throws Exception{
        WidgetConfig v=new WidgetConfig();v.apps.add(contact.id);v.columns=2;v.iconDp=64;v.labels=true;v.editButton=false;v.shadowSharpness=0;v.shadowDistance=16;v.shadowAngle=225;
        PreviewView preview=new PreviewView(c);preview.setWidgetBounds(320,220);preview.configure(v);Bitmap before=render(preview,344,244);Rect photoBefore=redBounds(before);
        v.shadowEnabled=true;preview.configure(v);Bitmap after=render(preview,344,244);assertEquals(photoBefore,redBounds(after));assertFalse(photoBefore.isEmpty());
        Rect first=new Rect();preview.getAccessibilityNodeProvider().createAccessibilityNodeInfo(1).getBoundsInParent(first);int from=Math.max(0,photoBefore.bottom+6),to=Math.min(after.getHeight(),first.bottom);
        for(int y=from;y<to;y++)for(int x=first.left;x<first.right;x++)if(before.getPixel(x,y)==Color.WHITE)assertEquals("Title pixels must not move",Color.WHITE,after.getPixel(x,y));
        save(before,"preview-before-shadow.png");save(after,"preview-after-shadow.png");
    }
    @Test public void optionsAndHomeRenderAtCompactAndLargeTextSizes()throws Exception{
        int id=Shadows.shadowOf(AppWidgetManager.getInstance(c)).createWidget(WidgetProvider.class,R.layout.widget);WidgetConfig v=new WidgetConfig();v.apps.add(app);v.apps.add(contact.id);v.columns=2;ConfigStore.put(c,id,v);
        for(int mode:new int[]{1,2}){
            c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",mode).commit();String theme=mode==2?"dark":"light";
            try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){save(render(home.get().getWindow().getDecorView(),360,800),"home-1.3.1-"+theme+".png");}
            try(ActivityController<OptionsActivity> options=Robolectric.buildActivity(OptionsActivity.class).setup()){save(render(options.get().getWindow().getDecorView(),360,800),"options-1.3.1-"+theme+".png");}
        }
    }
    private static Rect redBounds(Bitmap b){Rect r=new Rect();boolean found=false;for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(b.getPixel(x,y)==0xffe43d58){if(!found){r.set(x,y,x+1,y+1);found=true;}else r.union(x,y,x+1,y+1);}return r;}
    private static void measure(View v,int w,int h){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);}}
    private static Bitmap render(View v,int w,int h){measure(v,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));return b;}
    private static void save(Bitmap b,String name)throws Exception{String dir=System.getProperty("ury.qa.dir");if(dir==null)return;File folder=new File(dir);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
