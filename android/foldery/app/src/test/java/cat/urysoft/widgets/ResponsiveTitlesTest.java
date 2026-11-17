package cat.urysoft.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,34})
@LooperMode(LooperMode.Mode.PAUSED)
public final class ResponsiveTitlesTest {
    private Context c;private String app;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();app=new ComponentName(c,MainActivity.class).flattenToString();}
    @Test public void allTitleAndSpacingValuesPersistIndependentlyAndMigrateSafely(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.labelSizeSp=27;v.labelColor=0xff28b8ae;v.labelGapDp=21;v.paddingHorizontalDp=8;v.paddingVerticalDp=17;v.columnGapDp=26;v.rowGapDp=34;v.shadowEnabled=true;v.shadowAngle=315;ConfigStore.put(c,901,v);ConfigStore.put(c,902,new WidgetConfig());
        WidgetConfig r=ConfigStore.get(c,901);assertEquals(27,r.labelSizeSp);assertEquals(v.labelColor,r.labelColor);assertEquals(21,r.labelGapDp);assertEquals(8,r.paddingHorizontalDp);assertEquals(17,r.paddingVerticalDp);assertEquals(26,r.columnGapDp);assertEquals(34,r.rowGapDp);assertEquals(315,r.shadowAngle);assertEquals(12,ConfigStore.get(c,902).labelSizeSp);
        WidgetConfig old=WidgetConfig.fromJson("{\"iconDp\":160,\"expandIcons\":true,\"labelColor\":-1}");assertEquals(12,old.labelSizeSp);assertEquals(6,old.labelGapDp);assertEquals(12,old.paddingHorizontalDp);assertEquals(12,old.columnGapDp);
        WidgetConfig bad=WidgetConfig.fromJson("{\"labelSizeSp\":100,\"labelGapDp\":-2,\"paddingHorizontalDp\":200,\"paddingVerticalDp\":-1,\"columnGapDp\":77,\"rowGapDp\":-3}");assertEquals(32,bad.labelSizeSp);assertEquals(0,bad.labelGapDp);assertEquals(40,bad.paddingHorizontalDp);assertEquals(0,bad.paddingVerticalDp);assertEquals(40,bad.columnGapDp);assertEquals(0,bad.rowGapDp);
    }
    @Test public void iconsFillTheirColumnsAndHeightAndTitleSizeNeverShrinkThem(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.columns=3;WidgetConfig r=WidgetRenderer.resolve(c,v,360,90);assertEquals(104,r.iconDp);assertEquals(104f,r.cellWidthDp,0f);
        v.labelSizeSp=32;v.labelGapDp=32;v.rowGapDp=40;v.paddingVerticalDp=40;v.shadowEnabled=true;
        assertEquals(104,WidgetRenderer.resolve(c,v,360,90).iconDp);assertEquals(104,WidgetRenderer.resolve(c,v,360,600).iconDp);
        v.columns=1;assertEquals(336,WidgetRenderer.resolve(c,v,360,90).iconDp);v.columns=2;assertEquals(162,WidgetRenderer.resolve(c,v,360,90).iconDp);
        v.paddingHorizontalDp=0;v.columnGapDp=0;assertEquals(180,WidgetRenderer.resolve(c,v,360,90).iconDp);
    }
    @Test public void automaticColumnsAdaptToWidthAndNumberOfAccesses(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);assertEquals(1,WidgetRenderer.resolve(c,v,360,200).columns);
        for(int i=0;i<47;i++)v.apps.add("synthetic.app"+i+"/synthetic.Main");assertEquals(4,WidgetRenderer.resolve(c,v,360,200).columns);assertEquals(2,WidgetRenderer.resolve(c,v,200,200).columns);
        v.columns=8;v.columnGapDp=40;v.paddingHorizontalDp=40;WidgetConfig narrow=WidgetRenderer.resolve(c,v,112,200);assertEquals(1,narrow.columns);assertEquals(32,narrow.iconDp);assertEquals(0,narrow.columnGapDp);
    }
    @Test public void realWidgetUsesTheSelectedOuterAndInterItemMargins(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.apps.add(new ComponentName(c,ConfigureActivity.class).flattenToString());v.columns=2;v.paddingHorizontalDp=23;v.paddingVerticalDp=17;v.columnGapDp=20;v.rowGapDp=26;
        View root=WidgetRenderer.build(c,910,v,360,400).apply(c,new AppWidgetHostView(c));View content=root.findViewById(R.id.widget_content);GridView grid=root.findViewById(R.id.widget_grid);
        grid.setVisibility(View.VISIBLE);root.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,360),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,400),View.MeasureSpec.EXACTLY));root.layout(0,0,Ui.dp(c,360),Ui.dp(c,400));
        assertEquals(Ui.dp(c,23),content.getPaddingLeft());assertEquals(Ui.dp(c,23),content.getPaddingRight());assertEquals(Ui.dp(c,17),content.getPaddingTop());assertEquals(Ui.dp(c,17),content.getPaddingBottom());assertEquals(Ui.dp(c,20),grid.getHorizontalSpacing());
        WidgetConfig r=WidgetRenderer.resolve(c,v,360,400);View last=WidgetRenderer.cell(c,r,1).apply(c,new FrameLayout(c));assertEquals(0,last.getPaddingBottom());r.apps.add(app+"suffix");View first=WidgetRenderer.cell(c,r,0).apply(c,new FrameLayout(c));assertEquals(Ui.dp(c,26),first.getPaddingBottom());
    }
    @Test public void editorSavesFontCustomColorAndSpacingThroughThemeRecreation(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);Bundle state=new Bundle();state.putString("config",v.toJson());state.putInt("panel",1);
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            ((SeekBar)editor.get().findViewById(R.id.label_size)).setProgress(16);((SeekBar)editor.get().findViewById(R.id.label_gap)).setProgress(15);((SeekBar)editor.get().findViewById(R.id.column_gap)).setProgress(9);((SeekBar)editor.get().findViewById(R.id.row_gap)).setProgress(13);((SeekBar)editor.get().findViewById(R.id.widget_padding_horizontal)).setProgress(4);((SeekBar)editor.get().findViewById(R.id.widget_padding_vertical)).setProgress(11);
            editor.get().findViewById(R.id.label_color).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();EditText input=findInput(dialog.getWindow().getDecorView());assertNotNull(input);input.setText("#28B8AE");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",2).commit();editor.recreate();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));Bundle out=new Bundle();editor.saveInstanceState(out);WidgetConfig r=WidgetConfig.fromJson(out.getString("config"));
            assertEquals(24,r.labelSizeSp);assertEquals(15,r.labelGapDp);assertEquals(18,r.columnGapDp);assertEquals(26,r.rowGapDp);assertEquals(4,r.paddingHorizontalDp);assertEquals(11,r.paddingVerticalDp);assertEquals(0xff28b8ae,r.labelColor);assertEquals(0,r.opacity);assertTrue(Ui.DARK);
            assertNull(editor.get().findViewById(R.id.expand_control));assertNotNull(editor.get().findViewById(R.id.layout_summary));
        }
    }
    @Test public void disablingTitlesRemovesTheirHeightAndGapWithoutChangingTheIcon(){
        WidgetConfig v=new WidgetConfig();v.apps.add(app);v.columns=3;v.labelGapDp=32;v.labelSizeSp=24;WidgetConfig r=WidgetRenderer.resolve(c,v,360,400);CellRenderer.Metrics visible=CellRenderer.measure(c,r);
        v.labels=false;WidgetConfig hidden=WidgetRenderer.resolve(c,v,360,400);CellRenderer.Metrics invisible=CellRenderer.measure(c,hidden);assertEquals(r.iconDp,hidden.iconDp);assertEquals(hidden.iconDp,invisible.titleTop,0f);assertEquals(0,invisible.titleHeight);assertTrue(visible.height>invisible.height);
    }
    @Test public void zeroSpacingCanProduceCompactRowsSmallerThan48(){
        WidgetConfig v=new WidgetConfig();v.columns=8;v.labels=false;v.paddingHorizontalDp=0;v.paddingVerticalDp=0;v.columnGapDp=0;v.rowGapDp=0;
        for(int i=0;i<16;i++)v.apps.add("compact.app"+i+"/compact.Main");WidgetConfig r=WidgetRenderer.resolve(c,v,240,200);assertEquals(30,r.iconDp);assertEquals(30f,CellRenderer.measure(c,r).height,0f);
        View row=WidgetRenderer.cell(c,r,0).apply(c,new FrameLayout(c));row.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,30),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,1000),View.MeasureSpec.AT_MOST));row.layout(0,0,row.getMeasuredWidth(),row.getMeasuredHeight());assertEquals(Ui.dp(c,30),row.getHeight());
    }
    private static EditText findInput(View view){if(view instanceof EditText)return(EditText)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText found=findInput(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
}
