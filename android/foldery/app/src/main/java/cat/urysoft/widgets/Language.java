package cat.urysoft.widgets;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** Framework language selection on Android 13+, private preference on Android 8–12. */
public final class Language {
    private Language(){}
    public static String selected(Context c){
        if(Build.VERSION.SDK_INT>=33){
            LocaleManager manager=c.getSystemService(LocaleManager.class);
            LocaleList locales=manager.getApplicationLocales();
            return locales.isEmpty()?"":LanguagePolicy.normalize(locales.get(0).toLanguageTag());
        }
        return LanguagePolicy.normalize(c.getSharedPreferences("language",Context.MODE_PRIVATE).getString("selected",""));
    }
    private static LocaleList locales(Context c){
        String tag=selected(c);
        if(!tag.isEmpty())return new LocaleList(Locale.forLanguageTag(tag));
        return Build.VERSION.SDK_INT>=33?c.getSystemService(LocaleManager.class).getSystemLocales():Resources.getSystem().getConfiguration().getLocales();
    }
    public static String signature(Context c){return selected(c)+"|"+locales(c).toLanguageTags();}
    public static Context context(Context c){
        LocaleList wanted=locales(c);
        if(wanted.equals(c.getResources().getConfiguration().getLocales()))return c;
        Configuration config=new Configuration(c.getResources().getConfiguration());config.setLocales(wanted);
        return c.createConfigurationContext(config);
    }
    public static void choose(Activity a,String value){
        String tag=LanguagePolicy.normalize(value);
        if(tag.equals(selected(a)))return;
        if(Build.VERSION.SDK_INT>=33){
            a.getSystemService(LocaleManager.class).setApplicationLocales(tag.isEmpty()?LocaleList.getEmptyLocaleList():new LocaleList(Locale.forLanguageTag(tag)));
        }else{
            a.getSharedPreferences("language",Context.MODE_PRIVATE).edit().putString("selected",tag).apply();
            a.recreate();
        }
        WidgetRenderer.refreshAsync(a);
    }
    public static Spinner selector(Activity a){
        Spinner spinner=new Spinner(a);spinner.setId(R.id.app_language);spinner.setMinimumHeight(Ui.dp(a,48));spinner.setContentDescription(a.getString(R.string.language_title));
        String[] labels={a.getString(R.string.language_device),"Català","Español","English"};
        spinner.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels));
        spinner.setSelection(LanguagePolicy.index(selected(a)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){String tag=LanguagePolicy.tag(position);if(!tag.equals(selected(a)))choose(a,tag);}
            public void onNothingSelected(AdapterView<?> parent){}
        });return spinner;
    }
}
