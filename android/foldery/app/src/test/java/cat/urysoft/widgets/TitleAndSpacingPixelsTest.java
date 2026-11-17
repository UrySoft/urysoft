package cat.urysoft.widgets;

import android.app.*;
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
public final class TitleAndSpacingPixelsTest {
    private static final int PHOTO=0xffe43d58,TEXT=0xff27e7e0,SHADOW=0xff15b125;
    private Context c;private ContactStore.Entry contact;
    @Before public void setup()throws Exception{
        c=RuntimeEnvironment.getApplication();ContactFeaturesTest.FakeContacts p=new ContactFeaturesTest.FakeContacts();p.advertisePhoto=true;p.directOnly=true;ProviderInfo info=new ProviderInfo();info.authority=android.provider.ContactsContract.AUTHORITY;p.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(info.authority,p);
        p.photo=new File(c.getCacheDir(),"synthetic-title-photo.png");Bitmap b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);b.eraseColor(PHOTO);try(FileOutputStream out=new FileOutputStream(p.photo)){b.compress(Bitmap.CompressFormat.PNG,100,out);}contact=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);
    }
    private WidgetConfig style(){WidgetConfig v=new WidgetConfig();v.apps.add(contact.id);v.iconDp=96;v.cellWidthDp=128;v.labelSizeSp=24;v.labelGapDp=12;v.labelColor=TEXT;v.shadowColor=SHADOW;v.shadowOpacity=100;v.shadowSharpness=100;v.shadowDistance=12;return v;}
    @Test public void titlesUseTheConfiguredColorSizeAndGapWhileThePhotoStaysFixed()throws Exception{
        WidgetConfig v=style();v.labelSizeSp=12;Bitmap small=CellRenderer.app(c,contact.id,v,0);Rect smallPhoto=bounds(small,PHOTO),smallText=bounds(small,TEXT);assertFalse(smallText.isEmpty());
        v.labelSizeSp=24;Bitmap large=CellRenderer.app(c,contact.id,v,0);Rect largeText=bounds(large,TEXT);assertEquals(smallPhoto,bounds(large,PHOTO));assertTrue(largeText.height()>smallText.height());
        v.labelGapDp+=16;Bitmap spaced=CellRenderer.app(c,contact.id,v,0);assertEquals(largeText.top+16,bounds(spaced,TEXT).top);assertEquals(smallPhoto,bounds(spaced,PHOTO));
        v.labelColor=Color.WHITE;Bitmap white=CellRenderer.app(c,contact.id,v,0);assertTrue(bounds(white,TEXT).isEmpty());assertFalse(bounds(white,Color.WHITE).isEmpty());
        v.labels=false;Bitmap hidden=CellRenderer.app(c,contact.id,v,0);assertTrue(bounds(hidden,Color.WHITE).isEmpty());assertTrue(hidden.getHeight()<white.getHeight());
        save(large,"title-size-and-color.png");save(spaced,"title-gap.png");
    }
    @Test public void theTitleReceivesTheSameDirectionalShadowAsTheIconWithoutMoving()throws Exception{
        WidgetConfig v=style();v.labelColor=Color.WHITE;Bitmap plain=CellRenderer.app(c,contact.id,v,0);Rect photo=bounds(plain,PHOTO),title=bounds(plain,Color.WHITE);assertFalse(title.isEmpty());
        Bitmap sheet=Bitmap.createBitmap(plain.getWidth()*4,plain.getHeight()*2,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(sheet);canvas.drawColor(0xff445779);
        for(int angle=0;angle<360;angle+=45){v.shadowEnabled=true;v.shadowAngle=angle;Bitmap shaded=CellRenderer.app(c,contact.id,v,0);assertEquals(plain.getWidth(),shaded.getWidth());assertEquals(plain.getHeight(),shaded.getHeight());assertEquals(photo,bounds(shaded,PHOTO));assertEquals(title,bounds(shaded,Color.WHITE));
            int dx=Math.round(ShadowDirection.x(v.shadowDistance,angle)),dy=Math.round(ShadowDirection.y(v.shadowDistance,angle)),shadowSamples=0;
            for(int y=title.top;y<title.bottom;y++)for(int x=title.left;x<title.right;x++)if(plain.getPixel(x,y)==Color.WHITE){int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=plain.getWidth()||ny>=plain.getHeight()||Color.alpha(plain.getPixel(nx,ny))!=0)continue;int pixel=shaded.getPixel(nx,ny);if(Color.alpha(pixel)>80&&Color.green(pixel)>Color.red(pixel)*2&&Color.green(pixel)>Color.blue(pixel)*2)shadowSamples++;}
            assertTrue("Missing title shadow in direction "+angle,shadowSamples>5);int i=angle/45;canvas.drawBitmap(shaded,(i%4)*plain.getWidth(),(i/4)*plain.getHeight(),null);
        }save(sheet,"title-shadow-eight-directions.png");
    }
    @Test public void actualWidgetAndPreviewPlacePhotosAndTitlesAtTheSameCoordinates()throws Exception{
        WidgetConfig v=style();v.columns=3;v.paddingHorizontalDp=20;v.paddingVerticalDp=18;v.columnGapDp=18;v.labelSizeSp=18;v.editButton=false;v.shadowEnabled=true;v.shadowSharpness=45;v.shadowOpacity=70;v.shadowAngle=45;v.shadowDistance=6;
        Bitmap actual;
        try(ActivityController<Activity> activity=Robolectric.buildActivity(Activity.class).setup().visible()){
            AppWidgetHostView host=new AppWidgetHostView(activity.get());View root=WidgetRenderer.build(c,933,v,360,400).apply(c,host);host.addView(root);activity.get().setContentView(host);render(root,360,400);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));actual=render(root,360,400);
            GridView grid=root.findViewById(R.id.widget_grid);assertEquals(3,grid.getNumColumns());assertEquals(18,grid.getHorizontalSpacing());assertFalse(grid.getChildAt(0).hasFocusable());
        }
        PreviewView preview=new PreviewView(c);preview.setWidgetBounds(360,400);preview.configure(v);Bitmap simulated=render(preview,384,424);
        save(actual,"widget-responsive-titles.png");save(simulated,"preview-responsive-titles.png");
        Rect actualPhoto=bounds(actual,PHOTO),previewPhoto=bounds(simulated,PHOTO),actualTitle=bounds(actual,TEXT),previewTitle=bounds(simulated,TEXT);assertFalse("Actual photo is missing",actualPhoto.isEmpty());assertFalse("Actual title is missing",actualTitle.isEmpty());previewPhoto.offset(-12,-12);previewTitle.offset(-12,-12);
        near(actualPhoto,previewPhoto,2);near(actualTitle,previewTitle,2);assertEquals(20,actualPhoto.left,2);assertEquals(18,actualPhoto.top,2);assertEquals(94,actualPhoto.width(),2);
        save(actual,"widget-responsive-titles.png");save(simulated,"preview-responsive-titles.png");
    }
    @Test public void widgetHeadingAndMarginsMatchThePreviewForOneAndTwoLineNames()throws Exception{
        for(String name:new String[]{"Día a día","Un espacio con un nombre bastante largo para ocupar dos líneas"}){
            WidgetConfig v=style();v.columns=3;v.paddingHorizontalDp=20;v.paddingVerticalDp=18;v.name=name;v.heading=true;v.editButton=false;
            Bitmap actual;
            try(ActivityController<Activity> activity=Robolectric.buildActivity(Activity.class).setup().visible()){
                AppWidgetHostView host=new AppWidgetHostView(activity.get());View root=WidgetRenderer.build(c,934,v,360,400).apply(c,host);host.addView(root);activity.get().setContentView(host);render(root,360,400);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));actual=render(root,360,400);
            }
            PreviewView preview=new PreviewView(c);preview.setWidgetBounds(360,400);preview.configure(v);Bitmap simulated=render(preview,384,424);Rect photo=bounds(simulated,PHOTO);photo.offset(-12,-12);near(bounds(actual,PHOTO),photo,2);
        }
    }
    @Test public void largeTextAndExtremeSoftShadowsStayInsideTheirRasterCanvas(){
        WidgetConfig v=style();v.iconDp=48;v.cellWidthDp=48;v.labelSizeSp=32;v.labelGapDp=0;v.shadowEnabled=true;v.shadowSharpness=0;v.shadowDistance=16;
        for(int angle=0;angle<360;angle+=45){v.shadowAngle=angle;Bitmap b=CellRenderer.app(c,contact.id,v,0);assertFalse(bounds(b,TEXT).isEmpty());
            for(int x=0;x<b.getWidth();x++){assertEquals(0,Color.alpha(b.getPixel(x,0)));assertEquals(0,Color.alpha(b.getPixel(x,b.getHeight()-1)));}
            for(int y=0;y<b.getHeight();y++){assertEquals(0,Color.alpha(b.getPixel(0,y)));assertEquals(0,Color.alpha(b.getPixel(b.getWidth()-1,y)));}
        }
    }
    @Test @Config(qualifiers="w360dp-h800dp-420dpi") public void fractionalDensityAndEightColumnsUseTheSamePhysicalPixels()throws Exception{
        for(int columns:new int[]{2,3,8}){
            WidgetConfig v=style();v.apps.clear();for(int i=0;i<columns-1;i++)v.apps.add("synthetic.slot"+i+"/synthetic.Main");v.apps.add(contact.id);v.columns=columns;v.paddingHorizontalDp=10;v.paddingVerticalDp=18;v.columnGapDp=6;v.editButton=false;v.labelSizeSp=18;
            int width=Ui.dp(c,360),height=Ui.dp(c,400);Bitmap actual;
            try(ActivityController<Activity> activity=Robolectric.buildActivity(Activity.class).setup().visible()){
                AppWidgetHostView host=new AppWidgetHostView(activity.get());View root=WidgetRenderer.build(c,935,v,360,400).apply(c,host);host.addView(root);activity.get().setContentView(host);render(root,width,height);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));actual=render(root,width,height);
                GridView grid=root.findViewById(R.id.widget_grid);WidgetConfig r=WidgetRenderer.resolve(c,v,360,400);assertEquals(Math.round(r.cellWidthDp*c.getResources().getDisplayMetrics().density),grid.getColumnWidth());
            }
            PreviewView preview=new PreviewView(c);preview.setWidgetBounds(360,400);preview.configure(v);int pw=Ui.dp(c,384),ph=Ui.dp(c,424);Bitmap simulated=render(preview,pw,ph);Rect photo=bounds(simulated,PHOTO);photo.offset(-Math.round((pw-width)/2f),-Ui.dp(c,12));near(bounds(actual,PHOTO),photo,3);
        }
    }
    @Test public void all48CellsStayInsideTheWidgetBitmapBudget(){
        WidgetConfig v=style();v.apps.clear();for(int i=0;i<48;i++)v.apps.add("synthetic.app"+i+"/synthetic.Main");v.columns=4;v.labelSizeSp=32;v.shadowEnabled=true;WidgetConfig r=WidgetRenderer.resolve(c,v,360,240);long total=0;
        for(String app:v.apps){Bitmap b=CellRenderer.app(c,app,r,0);total+=b.getAllocationByteCount();b.recycle();}
        android.util.DisplayMetrics m=c.getResources().getDisplayMetrics();assertTrue("Bitmap payload exceeds display budget",total<=m.widthPixels*(long)m.heightPixels*4*.9);
    }
    @Test public void editorRendersTitlesAndSpacingInBothThemes()throws Exception{
        for(int mode:new int[]{1,2}){
            c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",mode).commit();WidgetConfig v=style();v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());v.apps.add(new ComponentName(c,ConfigureActivity.class).flattenToString());v.columns=3;v.labelSizeSp=18;v.shadowEnabled=true;v.shadowOpacity=55;v.shadowColor=Color.BLACK;v.shadowSharpness=45;v.shadowDistance=5;Bundle state=new Bundle();state.putString("config",v.toJson());state.putInt("panel",1);
            try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));View decor=editor.get().getWindow().getDecorView();render(decor,360,800);ScrollView scroll=editor.get().findViewById(R.id.settings_scroll);String theme=mode==2?"dark":"light";
                scrollTo(scroll,editor.get().findViewById(R.id.title_controls));save(render(decor,360,800),"titles-editor-"+theme+".png");scrollTo(scroll,editor.get().findViewById(R.id.spacing_controls));save(render(decor,360,800),"spacing-editor-"+theme+".png");
            }
        }
    }
    private static void near(Rect a,Rect b,int tolerance){assertEquals(a.left,b.left,tolerance);assertEquals(a.top,b.top,tolerance);assertEquals(a.right,b.right,tolerance);assertEquals(a.bottom,b.bottom,tolerance);}
    private static Rect bounds(Bitmap b,int color){Rect r=new Rect();boolean found=false;for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(b.getPixel(x,y)==color){if(!found){r.set(x,y,x+1,y+1);found=true;}else r.union(x,y,x+1,y+1);}return r;}
    private static Bitmap render(View view,int width,int height){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));return b;}
    private static void scrollTo(ScrollView scroll,View view){Rect r=new Rect(0,0,view.getWidth(),view.getHeight());scroll.offsetDescendantRectToMyCoords(view,r);scroll.scrollTo(0,Math.max(0,r.top));}
    private static void save(Bitmap b,String name)throws Exception{String dir=System.getProperty("ury.qa.dir");if(dir==null)return;File folder=new File(dir);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
