package cat.urysoft.widgets;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.util.*;

/** Snapshots only contacts explicitly chosen in Android's contact picker. */
public final class ContactStore {
    private static final String PREFIX="contact:";
    private ContactStore() {}
    public static final class Entry {
        public final String id, name; public final Uri uri; public final boolean hasPhoto, photoAccessDenied;
        Entry(String id,String name,Uri uri,boolean hasPhoto){this(id,name,uri,hasPhoto,false);}
        Entry(String id,String name,Uri uri,boolean hasPhoto,boolean denied){this.id=id;this.name=name;this.uri=uri;this.hasPhoto=hasPhoto;this.photoAccessDenied=denied;}
    }
    public static boolean isContact(String id){return id!=null&&id.startsWith(PREFIX);}
    private static String key(String id){
        if(!isContact(id))return null;
        try{String value=id.substring(PREFIX.length());return UUID.fromString(value).toString().equals(value)?value:null;}catch(IllegalArgumentException e){return null;}
    }
    static boolean validUri(Uri uri){return uri!=null&&"content".equals(uri.getScheme())&&ContactsContract.AUTHORITY.equals(uri.getAuthority())&&!uri.getPathSegments().isEmpty()&&"contacts".equals(uri.getPathSegments().get(0));}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("selected_contacts",Context.MODE_PRIVATE);}
    private static File photoFile(Context c,String id){String key=key(id);return key==null?null:new File(new File(c.getNoBackupFilesDir(),"contact_photos"),key+".png");}
    public static Entry get(Context c,String id){
        if(key(id)==null)return null;
        try{String raw=prefs(c).getString(id,null);if(raw==null)return null;JSONObject j=new JSONObject(raw);Uri uri=Uri.parse(j.getString("uri"));if(!validUri(uri))return null;return new Entry(id,j.optString("name",c.getString(R.string.ui_contact_121)),uri,j.optBoolean("photo",false)&&photoFile(c,id).isFile());}catch(JSONException|ClassCastException e){return null;}
    }
    public static List<Entry> all(Context c){ArrayList<Entry> result=new ArrayList<>();for(String id:prefs(c).getAll().keySet()){Entry entry=get(c,id);if(entry!=null)result.add(entry);}return result;}
    public static int count(List<String> items){int n=0;for(String id:items)if(isContact(id))n++;return n;}
    public static synchronized Entry importSelected(Context c,Uri selected)throws IOException {
        if(!validUri(selected))throw new IOException("Invalid contact URI");
        String name=null,lookup=null,photoUri=null,thumbnailUri=null;long contactId=-1;boolean advertised=false;
        String[] columns={ContactsContract.Contacts._ID,ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,ContactsContract.Contacts.LOOKUP_KEY,ContactsContract.Contacts.PHOTO_URI,ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,ContactsContract.Contacts.PHOTO_ID};
        Cursor result;
        try{result=c.getContentResolver().query(selected,columns,null,null,null);}
        catch(IllegalArgumentException e){result=c.getContentResolver().query(selected,Arrays.copyOf(columns,3),null,null,null);}
        try(Cursor cursor=result){
            if(cursor==null||!cursor.moveToFirst())throw new IOException("Contact unavailable");
            int n=cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),k=cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY),i=cursor.getColumnIndex(ContactsContract.Contacts._ID);
            if(n>=0)name=cursor.getString(n);if(k>=0)lookup=cursor.getString(k);if(i>=0)contactId=cursor.getLong(i);
            photoUri=value(cursor,ContactsContract.Contacts.PHOTO_URI);thumbnailUri=value(cursor,ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);advertised=photoUri!=null||thumbnailUri!=null||value(cursor,ContactsContract.Contacts.PHOTO_ID)!=null;
        }catch(SecurityException|IllegalArgumentException e){throw new IOException("Contact permission no longer available",e);}
        if(name==null||name.trim().isEmpty())name=c.getString(R.string.ui_contact_121);else name=name.trim();
        Uri stable=lookup!=null&&contactId>=0?ContactsContract.Contacts.getLookupUri(contactId,lookup):selected;
        if(stable==null||!validUri(stable))stable=selected;
        String id=null;Entry previous=null;
        for(Entry entry:all(c))if(sameContact(entry.uri,stable)){id=entry.id;previous=entry;break;}
        if(id==null)id=PREFIX+UUID.randomUUID();
        PhotoResult loaded=new PhotoResult();
        // PHOTO_URI also covers providers whose photos are not exposed below /contacts/id.
        for(String raw:new String[]{photoUri,thumbnailUri})if(loaded.bitmap==null&&raw!=null){
            Uri uri=Uri.parse(raw);if(!"content".equals(uri.getScheme()))continue;
            try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in!=null)loaded.bitmap=decodePhoto(in);}
            catch(SecurityException e){loaded.denied=true;}catch(IOException|IllegalArgumentException ignored){}
        }
        for(Uri uri:new LinkedHashSet<>(Arrays.asList(selected,stable)))for(boolean high:new boolean[]{true,false})if(loaded.bitmap==null){
            try(InputStream in=ContactsContract.Contacts.openContactPhotoInputStream(c.getContentResolver(),uri,high)){if(in!=null)loaded.bitmap=decodePhoto(in);}
            catch(SecurityException e){loaded.denied=true;}catch(IOException|IllegalArgumentException ignored){}
        }
        Bitmap photo=loaded.bitmap;
        File file=photoFile(c,id);boolean hasPhoto=photo!=null||(previous!=null&&previous.hasPhoto&&(loaded.denied||advertised));
        if(photo!=null){
            if(!file.getParentFile().isDirectory()&&!file.getParentFile().mkdirs())throw new IOException("Unable to store contact photo");
            AtomicFile atomic=new AtomicFile(file);FileOutputStream out=null;
            try{out=atomic.startWrite();if(!photo.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Unable to encode contact photo");atomic.finishWrite(out);}catch(IOException e){atomic.failWrite(out);throw e;}finally{photo.recycle();}
        }
        try{JSONObject json=new JSONObject();json.put("name",name);json.put("uri",stable.toString());json.put("photo",hasPhoto);if(!prefs(c).edit().putString(id,json.toString()).commit())throw new IOException("Unable to save contact");}catch(JSONException e){throw new IOException(e);}
        if(!hasPhoto&&file.exists())file.delete();
        return new Entry(id,name,stable,hasPhoto,photo==null&&(loaded.denied||advertised));
    }
    private static String value(Cursor cursor,String column){int i=cursor.getColumnIndex(column);return i<0||cursor.isNull(i)?null:cursor.getString(i);}
    private static final class PhotoResult{Bitmap bitmap;boolean denied;}
    public static int refreshSelected(Context c){
        int photos=0;
        for(Entry entry:all(c))try{if(importSelected(c,entry.uri).hasPhoto)photos++;}catch(IOException|SecurityException|IllegalArgumentException ignored){if(entry.hasPhoto)photos++;}
        return photos;
    }
    private static Bitmap decodePhoto(InputStream input)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int read,total=0;
        while((read=input.read(buffer))!=-1){total+=read;if(total>12*1024*1024)throw new IOException("Photo exceeds size limit");bytes.write(buffer,0,read);}
        byte[] raw=bytes.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0)return null;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
        Bitmap decoded=BitmapFactory.decodeByteArray(raw,0,raw.length,options);if(decoded==null)return null;
        int side=Math.min(decoded.getWidth(),decoded.getHeight()),target=Math.min(512,side);Bitmap square=Bitmap.createBitmap(target,target,Bitmap.Config.ARGB_8888);
        int x=(decoded.getWidth()-side)/2,y=(decoded.getHeight()-side)/2;new Canvas(square).drawBitmap(decoded,new Rect(x,y,x+side,y+side),new Rect(0,0,target,target),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));decoded.recycle();return square;
    }
    public static Drawable icon(Context c,String id){
        Entry entry=get(c,id);Bitmap bitmap=null;if(entry!=null&&entry.hasPhoto)bitmap=BitmapFactory.decodeFile(photoFile(c,id).getAbsolutePath());
        if(bitmap==null){bitmap=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xff3268a8);canvas.drawRoundRect(0,0,128,128,28,28,p);p.setColor(Color.WHITE);p.setTextSize(48);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));String name=entry==null?"?":entry.name;String[] words=name.trim().split("\\s+");String initials=letter(words[0]);if(words.length>1)initials+=letter(words[words.length-1]);canvas.drawText(initials.toUpperCase(Locale.getDefault()),64,64-(p.ascent()+p.descent())/2,p);}
        return new BitmapDrawable(c.getResources(),bitmap);
    }
    private static String letter(String word){return word.isEmpty()?"?":new String(Character.toChars(word.codePointAt(0)));}
    private static boolean sameContact(Uri a,Uri b){
        if(a.equals(b))return true;List<String> first=a.getPathSegments(),second=b.getPathSegments();
        return first.size()>=3&&second.size()>=3&&"lookup".equals(first.get(1))&&"lookup".equals(second.get(1))&&first.get(2).equals(second.get(2));
    }
    public static Intent viewIntent(Context c,String id){Entry entry=get(c,id);return entry==null?null:new Intent(Intent.ACTION_VIEW).setDataAndType(entry.uri,ContactsContract.Contacts.CONTENT_ITEM_TYPE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
}
