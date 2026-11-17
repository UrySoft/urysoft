package cat.urysoft.widgets;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.util.UUID;

/** Private, bounded image imports; no broad storage permission. */
public final class BackgroundStore {
 private static String cached="";private static Bitmap image;
 private static File file(Context c,String id){if(!id.matches("[a-f0-9-]{36}"))return null;return new File(new File(c.getNoBackupFilesDir(),"backgrounds"),id+".png");}
 public static String add(Context c,Uri uri)throws IOException{
  byte[] bytes;try(InputStream in=c.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   if(in==null)throw new IOException();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>20*1024*1024)throw new IOException();out.write(b,0,n);}bytes=out.toByteArray();
  }
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0)throw new IOException();o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1536)o.inSampleSize*=2;o.inJustDecodeBounds=false;
  Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(bitmap==null)throw new IOException();
  try{android.media.ExifInterface exif=new android.media.ExifInterface(new ByteArrayInputStream(bytes));int orientation=exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(270);m.postScale(-1,1);break;case 8:m.setRotate(270);break;}if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}}catch(IOException ignored){}
  String id=UUID.randomUUID().toString();File target=file(c,id);target.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(target)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();out.getFD().sync();}finally{bitmap.recycle();}return id;
 }
 public static synchronized void draw(Context c,Canvas canvas,WidgetConfig v,RectF dest){
  File f=file(c,v.backgroundImage);if(f==null||!f.isFile())return;
  if(!cached.equals(f.getAbsolutePath())){if(image!=null)image.recycle();image=BitmapFactory.decodeFile(f.getAbsolutePath());cached=f.getAbsolutePath();}
  if(image==null)return;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);p.setAlpha(Math.round(v.opacity*255f/100));
  int save=canvas.save();canvas.clipRect(dest);float iw=image.getWidth(),ih=image.getHeight();
  if(v.backgroundMode==3){BitmapShader shader=new BitmapShader(image,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);Matrix matrix=new Matrix();float tile=Math.min(dest.width(),dest.height())/3f;matrix.setScale(tile/iw,tile/iw);matrix.postTranslate(dest.left,dest.top);shader.setLocalMatrix(matrix);p.setShader(shader);canvas.drawRect(dest,p);}
  else{float sx=dest.width()/iw,sy=dest.height()/ih;float scale=v.backgroundMode==1?Math.min(sx,sy):v.backgroundMode==4?Math.min(1,Math.min(sx,sy)):Math.max(sx,sy);RectF target=v.backgroundMode==2?dest:new RectF(dest.centerX()-iw*scale/2,dest.centerY()-ih*scale/2,dest.centerX()+iw*scale/2,dest.centerY()+ih*scale/2);canvas.drawBitmap(image,null,target,p);}
  canvas.restoreToCount(save);
 }
}
