package cat.urysoft.widgets;

import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import java.time.Duration;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,34})
@LooperMode(LooperMode.Mode.PAUSED)
public final class EditorFeaturesTest {
    private Context c;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();}
    private WidgetConfig selection(){WidgetConfig v=new WidgetConfig();v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());v.apps.add(new ComponentName(c,ConfigureActivity.class).flattenToString());v.columns=2;v.labels=false;v.editButton=false;return v;}
    @Test public void maximumIconSizeAndNewAppearanceSettingsSurviveSaving(){
        WidgetConfig v=selection();v.iconDp=160;v.iconShape=5;v.iconStyle=3;v.iconAccent=0xff3d9d78;v.expandIcons=true;
        ConfigStore.put(c,501,v);WidgetConfig restored=ConfigStore.get(c,501);
        assertEquals(160,restored.iconDp);assertEquals(5,restored.iconShape);assertEquals(3,restored.iconStyle);assertEquals(v.iconAccent,restored.iconAccent);assertTrue(restored.expandIcons);
        WidgetConfig old=WidgetConfig.fromJson("{\"iconDp\":80,\"opacity\":0}");assertEquals(80,old.iconDp);assertEquals(0,old.iconShape);assertEquals(0,old.iconStyle);assertFalse(old.expandIcons);
    }
    @Test public void fixedColumnsUseTheFullWidthAndAreIndependentOfHeight(){
        WidgetConfig v=selection();v.expandIcons=true;
        assertEquals(232,WidgetRenderer.resolve(c,v,500,500).iconDp);
        assertEquals(232,WidgetRenderer.resolve(c,v,500,90).iconDp);
        v.apps.add("example.one/example.one.Main");v.apps.add("example.two/example.two.Main");
        WidgetConfig compact=WidgetRenderer.resolve(c,v,300,180);assertEquals(2,compact.columns);assertEquals(132,compact.iconDp);
        for(int i=0;i<20;i++)v.apps.add("many.apps"+i+"/sample.Main");assertEquals(132,WidgetRenderer.resolve(c,v,300,80).iconDp);
        v.iconDp=28;v.expandIcons=false;assertEquals(132,WidgetRenderer.resolve(c,v,300,180).iconDp);
    }
    @Test public void manualExpansionKeepsThreeColumnsWithoutCompressingImages(){
        WidgetConfig v=selection();v.columns=3;v.expandIcons=true;for(int i=0;i<4;i++)v.apps.add("sample.app"+i+"/sample.Main");
        WidgetConfig value=WidgetRenderer.resolve(c,v,300,360);assertEquals(3,value.columns);assertEquals(84,value.iconDp);
        View root=WidgetRenderer.build(c,101,v,300,360).apply(c,new AppWidgetHostView(c));
        // The Android 8 service is not bound in this component test. Make the empty
        // grid visible so it is measured, as it is after its real service is bound.
        root.findViewById(R.id.widget_grid).setVisibility(View.VISIBLE);
        root.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,300),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY));root.layout(0,0,Ui.dp(c,300),Ui.dp(c,360));
        assertEquals(3,((GridView)root.findViewById(R.id.widget_grid)).getNumColumns());
    }
    @Test public void draggingInsideThePreviewChangesApplicationOrder(){
        WidgetConfig v=selection();PreviewView preview=new PreviewView(c);List<String> result=new ArrayList<>();preview.setListener((apps,removed,index)->{result.clear();result.addAll(apps);});preview.configure(v);preview.layout(0,0,Ui.dp(c,320),Ui.dp(c,220));
        Rect first=new Rect(),second=new Rect();preview.getAccessibilityNodeProvider().createAccessibilityNodeInfo(1).getBoundsInParent(first);preview.getAccessibilityNodeProvider().createAccessibilityNodeInfo(2).getBoundsInParent(second);
        touch(preview,MotionEvent.ACTION_DOWN,first.centerX(),first.centerY());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout()+50));
        touch(preview,MotionEvent.ACTION_MOVE,second.centerX(),second.centerY());touch(preview,MotionEvent.ACTION_UP,second.centerX(),second.centerY());
        assertEquals(Arrays.asList(v.apps.get(1),v.apps.get(0)),result);
    }
    @Test public void removeButtonAndAccessibleActionsEditTheSamePreview(){
        WidgetConfig v=selection();PreviewView preview=new PreviewView(c);List<String> result=new ArrayList<>();preview.setListener((apps,removed,index)->{result.clear();result.addAll(apps);});preview.configure(v);preview.layout(0,0,Ui.dp(c,320),Ui.dp(c,220));
        AccessibilityNodeProvider provider=preview.getAccessibilityNodeProvider();assertTrue(provider.performAction(1,R.id.preview_move_after,null));assertEquals(v.apps.get(1),result.get(0));
        Rect first=new Rect();provider.createAccessibilityNodeInfo(1).getBoundsInParent(first);float removeX=first.left+Ui.dp(c,2),removeY=first.top+Ui.dp(c,2);touch(preview,MotionEvent.ACTION_DOWN,removeX,removeY);touch(preview,MotionEvent.ACTION_UP,removeX,removeY);
        assertEquals(1,result.size());assertEquals(v.apps.get(0),result.get(0));
    }
    @Test public void all48AppsRemainReachableThroughPreviewScrolling(){
        WidgetConfig v=new WidgetConfig();v.columns=2;v.labels=false;v.editButton=false;for(int i=0;i<48;i++)v.apps.add("sample.app"+i+"/sample.Main");
        PreviewView preview=new PreviewView(c);preview.setListener((apps,removed,index)->{});preview.configure(v);preview.layout(0,0,Ui.dp(c,320),Ui.dp(c,220));AccessibilityNodeProvider provider=preview.getAccessibilityNodeProvider();
        assertEquals(48,provider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID).getChildCount());assertFalse(provider.createAccessibilityNodeInfo(48).isVisibleToUser());
        for(int i=0;i<30;i++)provider.performAction(AccessibilityNodeProvider.HOST_VIEW_ID,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null);
        assertTrue(provider.createAccessibilityNodeInfo(48).isVisibleToUser());
    }
    @Test public void darkModeAppliesToTheHomeScreenAndEditor(){
        c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",2).commit();
        try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){
            assertTrue(Ui.DARK);assertEquals(0xff0d1422,Ui.BG);assertNull(home.get().findViewById(R.id.app_theme));
        }
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            assertEquals(0xff0d1422,((ColorDrawable)editor.get().findViewById(R.id.editor_root).getBackground()).getColor());assertTrue(Ui.DARK);
        }
    }
    @Test public void smallPreviewStaysOutsideTheSettingsScrollAndLargePreviewReturnsInline(){
        WidgetConfig v=selection();Bundle state=new Bundle();state.putString("config",v.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(120));View root=editor.get().findViewById(R.id.editor_root);PreviewView preview=editor.get().findViewById(R.id.editor_preview);
            root.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,1000),View.MeasureSpec.EXACTLY));root.layout(0,0,Ui.dp(c,360),Ui.dp(c,1000));
            assertSame(editor.get().findViewById(R.id.preview_dock),((View)preview.getParent()).getParent());
            root.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,260),View.MeasureSpec.EXACTLY));root.layout(0,0,Ui.dp(c,360),Ui.dp(c,260));
            assertSame(editor.get().findViewById(R.id.inline_preview),((View)preview.getParent()).getParent());
            assertFalse(LayoutPolicy.shouldPinPreview(200,400));assertTrue(LayoutPolicy.shouldPinPreview(199,400));
        }
    }
    @Test public void previewRemovalCanBeUndoneWithoutASeparateReorderList(){
        WidgetConfig v=selection();Bundle state=new Bundle();state.putString("config",v.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(120));PreviewView preview=editor.get().findViewById(R.id.editor_preview);preview.getAccessibilityNodeProvider().performAction(1,R.id.preview_remove,null);
            TextView undo=findPrefix(editor.get().getWindow().getDecorView(),"Deshacer:");assertNotNull(undo);undo.performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(120));
            Bundle saved=new Bundle();editor.saveInstanceState(saved);assertEquals(v.apps,WidgetConfig.fromJson(saved.getString("config")).apps);
        }
    }
    private static void touch(View view,int action,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,action,x,y,0);view.onTouchEvent(e);e.recycle();}
    private static TextView findPrefix(View view,String prefix){if(view instanceof TextView&&((TextView)view).getText().toString().startsWith(prefix))return(TextView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView v=findPrefix(((ViewGroup)view).getChildAt(i),prefix);if(v!=null)return v;}return null;}
}
