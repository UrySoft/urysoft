package cat.urysoft.widgets;

import android.content.*;
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
public final class FolderyFeaturesTest {
    private Context c;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();}
    @Test public void newBrandAndOwnerLinkAreConsistentAndOpenTheSavedProfile(){
        assertEquals(AppBrand.NAME,c.getString(R.string.app_name));
        try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){
            Shadows.shadowOf(home.get()).getNextStartedActivity();TextView link=home.get().findViewById(R.id.brand_link);assertNotNull(link);assertTrue(link.getText().toString().contains(AppBrand.OWNER));link.performClick();
            Intent opened=Shadows.shadowOf(home.get()).getNextStartedActivity();assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals(AppBrand.LINKEDIN,opened.getDataString());
            home.get().findViewById(R.id.help_button).performClick();assertNotNull(Shadows.shadowOf(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()));
        }
    }
    @Test public void savedShadowsRemainIndependentAndOldWidgetsKeepTheirAppearance(){
        WidgetConfig first=new WidgetConfig();first.shadowEnabled=true;first.shadowColor=0xff6834d5;first.shadowOpacity=72;first.shadowSharpness=18;first.shadowDistance=13;
        assertTrue(ConfigStore.put(c,601,first));assertTrue(ConfigStore.put(c,602,new WidgetConfig()));WidgetConfig restored=ConfigStore.get(c,601);
        assertTrue(restored.shadowEnabled);assertEquals(first.shadowColor,restored.shadowColor);assertEquals(72,restored.shadowOpacity);assertEquals(18,restored.shadowSharpness);assertEquals(13,restored.shadowDistance);assertFalse(ConfigStore.get(c,602).shadowEnabled);
        WidgetConfig old=WidgetConfig.fromJson("{\"iconDp\":160,\"opacity\":0}");assertFalse(old.shadowEnabled);assertEquals(32,old.iconPadding());assertEquals(160,old.iconDp);assertEquals(0,old.opacity);
        WidgetConfig bad=WidgetConfig.fromJson("{\"shadowOpacity\":130,\"shadowSharpness\":-7,\"shadowDistance\":80}");assertEquals(100,bad.shadowOpacity);assertEquals(0,bad.shadowSharpness);assertEquals(16,bad.shadowDistance);
    }
    @Test public void shadowControlsSaveValuesAndSurviveThemeRecreation(){
        WidgetConfig config=new WidgetConfig();config.apps.add(new ComponentName(c,MainActivity.class).flattenToString());Bundle saved=new Bundle();saved.putString("config",config.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(saved).start().resume().visible()){
            editor.get().findViewById(R.id.tab_icons).performClick();assertEquals(View.VISIBLE,editor.get().findViewById(R.id.panel_icons).getVisibility());
            assertEquals(View.GONE,editor.get().findViewById(R.id.shadow_controls).getVisibility());editor.get().findViewById(R.id.shadow_toggle).performClick();
            assertEquals(View.VISIBLE,editor.get().findViewById(R.id.shadow_controls).getVisibility());
            ((SeekBar)editor.get().findViewById(R.id.shadow_opacity)).setProgress(83);((SeekBar)editor.get().findViewById(R.id.shadow_sharpness)).setProgress(91);((SeekBar)editor.get().findViewById(R.id.shadow_distance)).setProgress(9);
            editor.get().findViewById(R.id.shadow_color).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();EditText hex=findInput(dialog.getWindow().getDecorView());assertNotNull(hex);hex.setText("#15B8A6");dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            editor.get().getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",2).commit();editor.recreate();
            Bundle out=new Bundle();editor.saveInstanceState(out);WidgetConfig restored=WidgetConfig.fromJson(out.getString("config"));assertTrue(restored.shadowEnabled);assertEquals(83,restored.shadowOpacity);assertEquals(91,restored.shadowSharpness);assertEquals(9,restored.shadowDistance);assertEquals(0xff15b8a6,restored.shadowColor);assertEquals(1,out.getInt("panel"));assertTrue(Ui.DARK);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(View.VISIBLE,editor.get().findViewById(R.id.panel_icons).getVisibility());
        }
    }
    @Test public void sharedCellsScaleToTheirColumnWithoutMovingForShadows(){
        WidgetConfig v=new WidgetConfig();v.shadowEnabled=true;v.labels=false;v.columns=2;v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());
        assertEquals(38,WidgetRenderer.resolve(c,v,112,180).iconDp);
        assertEquals(232,WidgetRenderer.resolve(c,v,500,400).iconDp);
        WidgetConfig resolved=WidgetRenderer.resolve(c,v,300,180);assertEquals(2,resolved.columns);assertEquals(132,resolved.iconDp);
        View row=WidgetRenderer.cell(c,resolved,0).apply(c,new FrameLayout(c));ImageView image=row.findViewById(R.id.cell_icon);
        row.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,132),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,1000),View.MeasureSpec.AT_MOST));row.layout(0,0,row.getMeasuredWidth(),row.getMeasuredHeight());
        assertEquals(-Ui.dp(c,32),image.getPaddingLeft());assertEquals(Ui.dp(c,132),image.getWidth());assertFalse(row.hasFocusable());
    }
    @Test public void leavingWithUnsavedChangesLetsTheUserKeepOrDiscardThem(){
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().findViewById(R.id.tab_icons).performClick();editor.get().findViewById(R.id.shadow_toggle).performClick();editor.get().onBackPressed();
            android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertFalse(editor.get().isFinishing());dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(editor.get().isFinishing());
            editor.get().onBackPressed();dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(editor.get().isFinishing());
        }
    }
    private static EditText findInput(View v){if(v instanceof EditText)return(EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText found=findInput(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}return null;}
}
