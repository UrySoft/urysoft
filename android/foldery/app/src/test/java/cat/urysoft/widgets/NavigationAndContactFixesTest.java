package cat.urysoft.widgets;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
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
@Config(sdk={26,34})
@LooperMode(LooperMode.Mode.PAUSED)
public final class NavigationAndContactFixesTest {
    private Context c;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();}
    @Test public void noWidgetsStartsTheCreatorOnceAndBackDoesNotTrapTheUser(){
        try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){
            Intent first=Shadows.shadowOf(home.get()).getNextStartedActivity();assertNotNull(first);assertEquals(ConfigureActivity.class.getName(),first.getComponent().getClassName());
            home.pause().resume();assertNull(Shadows.shadowOf(home.get()).getNextStartedActivity());home.recreate();assertNull(Shadows.shadowOf(home.get()).getNextStartedActivity());
        }
    }
    @Test public void anExistingWidgetStaysOnTheHomeScreenAndOptionsHaveTheirOwnScreen(){
        int id=Shadows.shadowOf(AppWidgetManager.getInstance(c)).createWidget(WidgetProvider.class,R.layout.widget);ConfigStore.put(c,id,new WidgetConfig());
        try(ActivityController<MainActivity> home=Robolectric.buildActivity(MainActivity.class).setup()){
            assertNull(Shadows.shadowOf(home.get()).getNextStartedActivity());assertNull(home.get().findViewById(R.id.app_theme));assertNull(home.get().findViewById(R.id.notification_options));
            assertNotNull(home.get().findViewById(R.id.help_button));home.get().findViewById(R.id.options_button).performClick();assertEquals(OptionsActivity.class.getName(),Shadows.shadowOf(home.get()).getNextStartedActivity().getComponent().getClassName());
            home.get().findViewById(R.id.help_button).performClick();assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
        }
        try(ActivityController<OptionsActivity> options=Robolectric.buildActivity(OptionsActivity.class).setup()){
            assertNotNull(options.get().findViewById(R.id.app_theme));assertNotNull(options.get().findViewById(R.id.notification_options));
            options.get().findViewById(R.id.about_button).performClick();assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
        }
    }
    @Test public void editorToolbarLinksToHelpAndOptionsAndRetainsTheUnsavedWidget(){
        WidgetConfig v=new WidgetConfig();v.name="Mi espacio sin guardar";v.shadowAngle=225;Bundle saved=new Bundle();saved.putString("config",v.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(saved).start().resume().visible()){
            editor.get().findViewById(R.id.options_button).performClick();assertEquals(OptionsActivity.class.getName(),Shadows.shadowOf(editor.get()).getNextStartedActivity().getComponent().getClassName());
            editor.get().findViewById(R.id.help_button).performClick();assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
            editor.pause();c.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putInt("mode",2).commit();editor.resume();editor.recreate();
            Bundle out=new Bundle();editor.saveInstanceState(out);WidgetConfig restored=WidgetConfig.fromJson(out.getString("config"));assertEquals(v.name,restored.name);assertEquals(225,restored.shadowAngle);assertTrue(Ui.DARK);
        }
    }
    @Test public void shadowChangesNeverChangeResolvedSizeColumnsOrMeasuredLabels(){
        WidgetConfig v=new WidgetConfig();v.apps.add(new ComponentName(c,MainActivity.class).flattenToString());v.columns=3;
        for(boolean expanded:new boolean[]{false,true})for(boolean labels:new boolean[]{false,true})for(int width:new int[]{112,280,360,500}){
            v.expandIcons=expanded;v.labels=labels;v.shadowEnabled=false;WidgetConfig plain=WidgetRenderer.resolve(c,v,width,300);
            View first=WidgetRenderer.cell(c,plain,0).apply(c,new FrameLayout(c));measure(first,Math.max(48,(width-48)/plain.columns),250);float labelTop=CellRenderer.measure(c,plain).titleTop;int rowHeight=first.getMeasuredHeight();
            v.shadowEnabled=true;v.shadowSharpness=0;v.shadowDistance=16;v.shadowAngle=315;WidgetConfig shaded=WidgetRenderer.resolve(c,v,width,300);
            assertEquals(plain.iconDp,shaded.iconDp);assertEquals(plain.columns,shaded.columns);
            View second=WidgetRenderer.cell(c,shaded,0).apply(c,new FrameLayout(c));measure(second,Math.max(48,(width-48)/plain.columns),250);
            assertEquals(rowHeight,second.getMeasuredHeight());assertEquals(labelTop,CellRenderer.measure(c,shaded).titleTop,0f);assertEquals(first.findViewById(R.id.cell_icon).getHeight(),second.findViewById(R.id.cell_icon).getHeight());
            assertFalse("Collection rows must allow GridView to receive taps",second.hasFocusable());
        }
    }
    @Test public void aProviderWithOnlyADirectPhotoUriStillImportsThePhoto()throws Exception{
        ContactFeaturesTest.FakeContacts provider=provider();provider.advertisePhoto=true;provider.directOnly=true;
        ContactStore.Entry entry=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);assertTrue(entry.hasPhoto);assertFalse(entry.photoAccessDenied);assertEquals(0,provider.wholeBookQueries);
        provider.denied=true;assertNotNull(ContactStore.icon(c,entry.id));
    }
    @Test public void optionalPermissionRetriesTheSelectedPhotoAndDoesNotAddADuplicate()throws Exception{
        ContactFeaturesTest.FakeContacts provider=provider();provider.advertisePhoto=true;provider.requirePhotoPermission=true;
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().onActivityResult(ConfigureActivity.PICK_CONTACT,Activity.RESULT_OK,new Intent().setData(ContactFeaturesTest.SELECTED));
            awaitPermission(editor);assertEquals(1,ContactStore.all(c).size());assertFalse(ContactStore.all(c).get(0).hasPhoto);
            editor.recreate();Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CONTACTS);
            editor.get().onRequestPermissionsResult(ConfigureActivity.PHOTO_PERMISSION,new String[]{Manifest.permission.READ_CONTACTS},new int[]{PackageManager.PERMISSION_GRANTED});awaitReady(editor);
            assertEquals(1,ContactStore.all(c).size());assertTrue(ContactStore.all(c).get(0).hasPhoto);assertEquals(1,savedConfig(editor).apps.size());assertEquals(0,provider.wholeBookQueries);
        }
    }
    @Test public void denyingThePhotoPermissionKeepsAWorkingContactShortcut()throws Exception{
        ContactFeaturesTest.FakeContacts provider=provider();provider.advertisePhoto=true;provider.requirePhotoPermission=true;
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().onActivityResult(ConfigureActivity.PICK_CONTACT,Activity.RESULT_OK,new Intent().setData(ContactFeaturesTest.SELECTED));awaitPermission(editor);
            editor.get().onRequestPermissionsResult(ConfigureActivity.PHOTO_PERMISSION,new String[]{Manifest.permission.READ_CONTACTS},new int[]{PackageManager.PERMISSION_DENIED});awaitReady(editor);
            WidgetConfig saved=savedConfig(editor);assertEquals(1,saved.apps.size());assertNotNull(ContactStore.viewIntent(c,saved.apps.get(0)));assertFalse(ContactStore.get(c,saved.apps.get(0)).hasPhoto);
        }
    }
    @Test public void updatingOldContactShortcutsLoadsMissingPhotosAndPreservesCachedOnes()throws Exception{
        ContactFeaturesTest.FakeContacts provider=provider();provider.hasPhoto=false;ContactStore.Entry old=ContactStore.importSelected(c,ContactFeaturesTest.SELECTED);assertFalse(old.hasPhoto);
        provider.hasPhoto=true;provider.advertisePhoto=true;provider.directOnly=true;provider.requirePhotoPermission=true;
        WidgetConfig v=new WidgetConfig();v.apps.add(old.id);Bundle state=new Bundle();state.putString("config",v.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            editor.get().findViewById(R.id.refresh_contact_photos).performClick();Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CONTACTS);
            editor.get().onRequestPermissionsResult(ConfigureActivity.PHOTO_PERMISSION,new String[]{Manifest.permission.READ_CONTACTS},new int[]{PackageManager.PERMISSION_GRANTED});awaitReady(editor);assertTrue(ContactStore.get(c,old.id).hasPhoto);assertEquals(v.apps,savedConfig(editor).apps);
        }
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_CONTACTS);assertTrue(ContactStore.importSelected(c,ContactFeaturesTest.SELECTED).hasPhoto);assertEquals(0,provider.wholeBookQueries);
    }
    private ContactFeaturesTest.FakeContacts provider()throws Exception{
        ContactFeaturesTest.FakeContacts p=new ContactFeaturesTest.FakeContacts();ProviderInfo info=new ProviderInfo();info.authority=android.provider.ContactsContract.AUTHORITY;p.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(info.authority,p);
        p.photo=new File(c.getCacheDir(),"synthetic-regression-contact.png");Bitmap bitmap=Bitmap.createBitmap(90,90,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffe43d58);try(FileOutputStream out=new FileOutputStream(p.photo)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}return p;
    }
    private static void awaitPermission(ActivityController<ConfigureActivity> editor)throws Exception{for(int i=0;i<500;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));Bundle b=new Bundle();editor.saveInstanceState(b);if(b.getString("pendingPhotoAction")!=null)return;}throw new AssertionError("Photo permission was not requested");}
    private static void awaitReady(ActivityController<ConfigureActivity> editor)throws Exception{for(int i=0;i<500;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));if(editor.get().findViewById(R.id.add_contact).isEnabled())return;}throw new AssertionError("Contact action did not finish");}
    private static WidgetConfig savedConfig(ActivityController<ConfigureActivity> editor){Bundle b=new Bundle();editor.saveInstanceState(b);return WidgetConfig.fromJson(b.getString("config"));}
    private static void measure(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.AT_MOST));view.layout(0,0,width,view.getMeasuredHeight());}
}
