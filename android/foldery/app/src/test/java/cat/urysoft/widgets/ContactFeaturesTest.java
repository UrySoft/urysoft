package cat.urysoft.widgets;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
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
public final class ContactFeaturesTest {
    private Context c;private FakeContacts provider;
    static final Uri SELECTED=Uri.parse("content://com.android.contacts/contacts/7");
    @Before public void setup()throws Exception{
        c=RuntimeEnvironment.getApplication();provider=new FakeContacts();ProviderInfo info=new ProviderInfo();info.authority=ContactsContract.AUTHORITY;provider.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,provider);
        File photo=new File(c.getCacheDir(),"synthetic-contact.png");Bitmap bitmap=Bitmap.createBitmap(96,64,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffe43d58);try(FileOutputStream stream=new FileOutputStream(photo)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}provider.photo=photo;
    }
    @Test public void selectedContactAndPhotoPersistWithoutReadingTheAddressBook()throws Exception{
        ContactStore.Entry saved=ContactStore.importSelected(c,SELECTED);assertEquals("Laia Riera",saved.name);assertTrue(saved.hasPhoto);assertTrue(saved.uri.toString().contains("/lookup/fixture-key/7"));
        ContactStore.Entry restored=ContactStore.get(c,saved.id);assertNotNull(restored);assertTrue(restored.hasPhoto);assertEquals(saved.name,AppCatalog.label(c,saved.id));assertEquals(1,ContactStore.all(c).size());assertEquals(0,provider.wholeBookQueries);
        assertEquals(saved.id,ContactStore.importSelected(c,SELECTED).id);assertEquals(1,ContactStore.all(c).size());
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_PERMISSIONS);List<String> permissions=info.requestedPermissions==null?Collections.emptyList():Arrays.asList(info.requestedPermissions);assertTrue(permissions.contains("android.permission.READ_CONTACTS"));assertFalse(permissions.contains("android.permission.CALL_PHONE"));
        assertNotNull(AppCatalog.icon(c,saved.id));assertTrue(AppCatalog.load(c).stream().anyMatch(e->e.component.equals(saved.id)));
    }
    @Test public void contactsWithoutPhotosUseInitialsAndCanStillBeOpened()throws Exception{
        provider.hasPhoto=false;ContactStore.Entry saved=ContactStore.importSelected(c,SELECTED);assertFalse(saved.hasPhoto);assertNotNull(AppCatalog.icon(c,saved.id));Intent intent=ContactStore.viewIntent(c,saved.id);assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals(saved.uri,intent.getData());assertEquals(0,BadgeState.get(c,saved.id));
    }
    @Test public void selectingTheSameContactAfterItsRowChangesRefreshesTheExistingShortcut()throws Exception{
        ContactStore.Entry first=ContactStore.importSelected(c,SELECTED);provider.contactId=22;provider.name="Laia Riera Actualizada";provider.hasPhoto=false;
        ContactStore.Entry refreshed=ContactStore.importSelected(c,Uri.parse("content://com.android.contacts/contacts/22"));assertEquals(first.id,refreshed.id);assertEquals(1,ContactStore.all(c).size());assertEquals(provider.name,AppCatalog.label(c,first.id));assertFalse(refreshed.hasPhoto);assertTrue(refreshed.uri.toString().endsWith("/22"));
    }
    @Test public void widgetLaunchesOnlyItsOwnContactAndPreservesAppsAlongsideIt()throws Exception{
        ContactStore.Entry contact=ContactStore.importSelected(c,SELECTED);WidgetConfig config=new WidgetConfig();config.apps.add(new ComponentName(c,MainActivity.class).flattenToString());config.apps.add(contact.id);assertTrue(ConfigStore.put(c,410,config));assertEquals(config.apps,ConfigStore.get(c,410).apps);
        Intent launch=new Intent(c,LaunchActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,410).putExtra("component",contact.id);
        try(ActivityController<LaunchActivity> activity=Robolectric.buildActivity(LaunchActivity.class,launch).create()){Intent opened=Shadows.shadowOf(activity.get()).getNextStartedActivity();assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals(contact.uri,opened.getData());}
        launch.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,411);
        try(ActivityController<LaunchActivity> activity=Robolectric.buildActivity(LaunchActivity.class,launch).create()){assertNull(Shadows.shadowOf(activity.get()).getNextStartedActivity());}
        assertNull(ContactStore.get(c,"contact:../../foreign"));assertNull(ContactStore.viewIntent(c,"contact:unknown"));
    }
    @Test public void contactButtonUsesAndroidPickerAndAddsTheResultToTheMixedPreview()throws Exception{
        WidgetConfig config=new WidgetConfig();config.apps.add(new ComponentName(c,MainActivity.class).flattenToString());Bundle state=new Bundle();state.putString("config",config.toJson());
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()){
            editor.get().findViewById(R.id.add_contact).performClick();ShadowActivity.IntentForResult request=Shadows.shadowOf(editor.get()).getNextStartedActivityForResult();assertEquals(Intent.ACTION_PICK,request.intent.getAction());assertEquals(ContactsContract.Contacts.CONTENT_TYPE,request.intent.getType());assertEquals(ConfigureActivity.PICK_CONTACT,request.requestCode);
            editor.get().onActivityResult(request.requestCode,Activity.RESULT_OK,new Intent().setData(SELECTED));WidgetConfig result=awaitItems(editor,2);assertEquals(config.apps.get(0),result.apps.get(0));assertTrue(ContactStore.isContact(result.apps.get(1)));
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));
            PreviewView preview=editor.get().findViewById(R.id.editor_preview);assertTrue(preview.getAccessibilityNodeProvider().performAction(2,R.id.preview_remove,null));Bundle removed=new Bundle();editor.saveInstanceState(removed);assertEquals(1,WidgetConfig.fromJson(removed.getString("config")).apps.size());assertNotNull(ContactStore.get(c,result.apps.get(1)));
        }
    }
    @Test public void canceledOrDeniedSelectionsLeaveTheWidgetUnchanged()throws Exception{
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().onActivityResult(ConfigureActivity.PICK_CONTACT,Activity.RESULT_CANCELED,null);Bundle state=new Bundle();editor.saveInstanceState(state);assertTrue(WidgetConfig.fromJson(state.getString("config")).apps.isEmpty());
            provider.denied=true;editor.get().onActivityResult(ConfigureActivity.PICK_CONTACT,Activity.RESULT_OK,new Intent().setData(SELECTED));
            for(int i=0;i<300&&!editor.get().findViewById(R.id.add_contact).isEnabled();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));}
            editor.saveInstanceState(state);assertTrue(WidgetConfig.fromJson(state.getString("config")).apps.isEmpty());assertTrue(ContactStore.all(c).isEmpty());assertTrue(editor.get().findViewById(R.id.add_contact).isEnabled());
        }
        try{ContactStore.importSelected(c,Uri.parse("file:///private/photo"));fail("Unexpected URI accepted");}catch(IOException expected){}
    }
    @Test public void importingAContactSurvivesRecreationWithoutAddingItTwice()throws Exception{
        provider.gate=new java.util.concurrent.CountDownLatch(1);
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().onActivityResult(ConfigureActivity.PICK_CONTACT,Activity.RESULT_OK,new Intent().setData(SELECTED));assertFalse(editor.get().findViewById(R.id.add_contact).isEnabled());
            editor.recreate();provider.gate.countDown();WidgetConfig result=awaitItems(editor,1);assertTrue(ContactStore.isContact(result.apps.get(0)));assertEquals(1,ContactStore.all(c).size());
        }finally{provider.gate.countDown();}
    }
    @Test public void anglesPreserveTheOldDownwardShadowAndPersistIndependently(){
        assertEquals(90,WidgetConfig.fromJson("{\"shadowEnabled\":true}").shadowAngle);assertEquals(315,WidgetConfig.fromJson("{\"shadowAngle\":-45}").shadowAngle);
        WidgetConfig a=new WidgetConfig();a.shadowAngle=270;WidgetConfig b=new WidgetConfig();b.shadowAngle=35;ConfigStore.put(c,620,a);ConfigStore.put(c,621,b);assertEquals(270,ConfigStore.get(c,620).shadowAngle);assertEquals(35,ConfigStore.get(c,621).shadowAngle);
        try(ActivityController<ConfigureActivity> editor=Robolectric.buildActivity(ConfigureActivity.class).setup()){
            editor.get().findViewById(R.id.tab_icons).performClick();editor.get().findViewById(R.id.shadow_toggle).performClick();((SeekBar)editor.get().findViewById(R.id.shadow_angle)).setProgress(315);Bundle state=new Bundle();editor.saveInstanceState(state);assertEquals(315,WidgetConfig.fromJson(state.getString("config")).shadowAngle);
            Button left=findDescription(editor.get().getWindow().getDecorView(),"Sombra: Izquierda");assertNotNull(left);left.performClick();editor.recreate();editor.saveInstanceState(state);assertEquals(180,WidgetConfig.fromJson(state.getString("config")).shadowAngle);assertTrue(findDescription(editor.get().getWindow().getDecorView(),"Sombra: Izquierda").isSelected());
        }
    }
    private static WidgetConfig awaitItems(ActivityController<ConfigureActivity> activity,int count)throws Exception{for(int i=0;i<500;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(80));Bundle state=new Bundle();activity.saveInstanceState(state);WidgetConfig v=WidgetConfig.fromJson(state.getString("config"));if(v.apps.size()==count)return v;}throw new AssertionError("Contact import did not complete");}
    private static Button findDescription(View view,String text){if(view instanceof Button&&view.getContentDescription()!=null&&text.contentEquals(view.getContentDescription()))return(Button)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button result=findDescription(((ViewGroup)view).getChildAt(i),text);if(result!=null)return result;}return null;}
    public static final class FakeContacts extends ContentProvider {
        File photo;boolean hasPhoto=true,denied,advertisePhoto,directOnly,requirePhotoPermission;int wholeBookQueries;long contactId=7;String name="Laia Riera";java.util.concurrent.CountDownLatch gate;
        @Override public boolean onCreate(){return true;}
        @Override public Cursor query(Uri uri,String[] columns,String selection,String[] args,String sort){
            if(gate!=null)try{gate.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            if(denied)throw new SecurityException("Synthetic denied contact");if("/contacts".equals(uri.getPath())){wholeBookQueries++;throw new SecurityException("Whole address book must not be queried");}
            MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];
            for(int i=0;i<columns.length;i++){switch(columns[i]){case "_id":values[i]=contactId;break;case "display_name":values[i]=name;break;case "lookup":values[i]="fixture-key";break;case "photo_uri":case "photo_thumb_uri":values[i]=hasPhoto&&advertisePhoto?"content://com.android.contacts/display_photo/99":null;break;case "photo_id":values[i]=hasPhoto&&advertisePhoto?99:null;break;case "data15":if(requirePhotoPermission&&getContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Synthetic photo denied");try{values[i]=hasPhoto&&!directOnly?java.nio.file.Files.readAllBytes(photo.toPath()):null;}catch(IOException e){throw new IllegalStateException(e);}break;default:values[i]=null;}}
            cursor.addRow(values);return cursor;
        }
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(requirePhotoPermission&&getContext().checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Synthetic photo denied");if(!hasPhoto||(directOnly&&!uri.getPath().equals("/display_photo/99")))throw new FileNotFoundException();return ParcelFileDescriptor.open(photo,ParcelFileDescriptor.MODE_READ_ONLY);}
        @Override public String getType(Uri uri){return ContactsContract.Contacts.CONTENT_ITEM_TYPE;}
        @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
        @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    }
}
