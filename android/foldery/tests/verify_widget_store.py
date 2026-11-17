#!/usr/bin/env python3
"""Exercise the production ConfigStore against small in-memory Android test doubles.
This verifies storage isolation, tombstones and stale writes, not Android UI or launcher behavior.
"""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
sources={
'android/content/SharedPreferences.java':'''package android.content; import java.util.*; public interface SharedPreferences {String getString(String k,String d);boolean getBoolean(String k,boolean d);boolean contains(String k);Map<String,?> getAll();Editor edit();interface Editor {Editor putString(String k,String v);Editor putBoolean(String k,boolean v);Editor remove(String k);boolean commit();void apply();}}''',
'android/content/Context.java':'''package android.content;public abstract class Context {public static final int MODE_PRIVATE=0;public abstract SharedPreferences getSharedPreferences(String name,int mode);}''',
'android/content/ComponentName.java':'''package android.content;public class ComponentName {private String pkg;public ComponentName(Context c,Class<?> t){}private ComponentName(String p){pkg=p;}public static ComponentName unflattenFromString(String s){return s.contains("/")?new ComponentName(s.split("/")[0]):null;}public String getPackageName(){return pkg;}}''',
'android/appwidget/AppWidgetManager.java':'''package android.appwidget;import android.content.*;public class AppWidgetManager {public static int[] hosted={11,12,13};public static AppWidgetManager getInstance(Context c){return new AppWidgetManager();}public int[] getAppWidgetIds(ComponentName n){return hosted.clone();}}''',
'cat/urysoft/widgets/WidgetProvider.java':'''package cat.urysoft.widgets;public class WidgetProvider {}''',
'cat/urysoft/widgets/WidgetConfig.java':'''package cat.urysoft.widgets;import java.util.*;import android.content.*;public class WidgetConfig {public boolean badges=true;public ArrayList<String> apps=new ArrayList<>();public String toJson(){return String.join(",",apps);}public static WidgetConfig fromJson(String s){WidgetConfig c=new WidgetConfig();if(s!=null&&!s.isEmpty())c.apps.addAll(Arrays.asList(s.split(",")));return c;}public static WidgetConfig defaults(Context c){return new WidgetConfig();}}''',
'cat/urysoft/widgets/WidgetStoreChecks.java':'''package cat.urysoft.widgets;
import android.content.*;import java.util.*;
public class WidgetStoreChecks {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static class Memory implements SharedPreferences {
  Map<String,Object> data=new HashMap<>();
  public String getString(String k,String d){return (String)data.getOrDefault(k,d);}
  public boolean getBoolean(String k,boolean d){return (Boolean)data.getOrDefault(k,d);}
  public boolean contains(String k){return data.containsKey(k);}
  public Map<String,?> getAll(){return new HashMap<>(data);}
  public Editor edit(){return new Editor(){Map<String,Object> pending=new HashMap<>();Set<String> removed=new HashSet<>();
   public Editor putString(String k,String v){pending.put(k,v);return this;}
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor remove(String k){removed.add(k);return this;}
   public boolean commit(){for(String k:removed)data.remove(k);data.putAll(pending);return true;}
   public void apply(){commit();}
  };}
 }
 static class TestContext extends Context {Memory memory;TestContext(Memory m){memory=m;}public SharedPreferences getSharedPreferences(String n,int m){return memory;}}
 public static void main(String[] args){
  Memory disk=new Memory();Context c=new TestContext(disk);WidgetConfig a=new WidgetConfig(),b=new WidgetConfig();a.apps.add("one/.Main");b.apps.add("two/.Main");
  check(ConfigStore.put(c,11,a),"save first");check(ConfigStore.put(c,12,b),"save second");
  String draft=ConfigStore.draft(c,a);ConfigStore.putTemplate(c,b);
  check(ConfigStore.watchedPackages(c).size()==2,"both widgets counted before deletion");
  check(ConfigStore.delete(c,11),"delete owned widget");
  check(!ConfigStore.has(c,11)&&ConfigStore.isDeleted(c,11),"configuration removed and tombstone set");
  check(!disk.contains("widget.11"),"serialized shortcuts erased");
  check(ConfigStore.get(c,11).apps.isEmpty(),"removed widget has no launchable shortcuts");
  check(Arrays.equals(ConfigStore.ids(c),new int[]{12,13}),"only selected widget hidden");
  check(ConfigStore.hostIds(c).length==3,"launcher IDs are not owned by provider");
  check(ConfigStore.get(c,12).apps.equals(b.apps),"other widget unchanged");
  check(ConfigStore.watchedPackages(c).equals(Collections.singleton("two")),"removed widget excluded from notification tracking");
  check(!ConfigStore.put(c,11,a),"stale editor cannot revive removed widget");
  check(!ConfigStore.delete(c,99),"unowned ID cannot be deleted");
  check(ConfigStore.get(c,12).apps.equals(b.apps),"unowned deletion has no side effects");
  Context reopened=new TestContext(disk);check(ConfigStore.isDeleted(reopened,11)&&!ConfigStore.has(reopened,11),"new context retains deleted state");
  check(ConfigStore.takeDraft(c,draft).apps.equals(a.apps),"unrelated pending pin untouched");
  check(ConfigStore.takeTemplate(c).apps.equals(b.apps),"unrelated template untouched");
  check(ConfigStore.delete(c,11),"repeat deletion is idempotent");
  ConfigStore.remove(c,11);check(!ConfigStore.isDeleted(c,11),"launcher deletion clears tombstone");
  check(ConfigStore.put(c,11,a),"genuinely reused launcher ID can be configured");
  check(ConfigStore.markDeleted(c,13),"restored deleted marker");check(!ConfigStore.has(c,13)&&ConfigStore.isDeleted(c,13),"restored deleted state remains inactive");
  System.out.println("PASS: "+checks+" production widget-store checks with in-memory Android doubles.");
 }
}'''}
# Storage isolation only; entitlement policy is covered by PremiumTests.java.
sources['cat/urysoft/widgets/PremiumAccess.java']='package cat.urysoft.widgets; import android.content.Context; final class PremiumAccess {static boolean canPersist(Context c,int id,WidgetConfig value){return true;}}'
with tempfile.TemporaryDirectory(prefix='foldery-store-') as d:
 work=Path(d);paths=[]
 for name,content in sources.items():
  p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8');paths.append(str(p))
 paths.append(str(root/'app/src/main/java/cat/urysoft/widgets/ConfigStore.java'))
 subprocess.run(['java','com.sun.tools.javac.Main','--release','8','-d',d,*paths],check=True)
 subprocess.run(['java','-cp',d,'cat.urysoft.widgets.WidgetStoreChecks'],check=True)
