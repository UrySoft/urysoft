package cat.urysoft.widgets;

import android.app.Activity;
import android.content.Context;

/** Recreate a returning editor with its saved draft when another screen changes language. */
public class LanguageActivity extends Activity {
    private String attachedLanguage;
    protected boolean languageRestarting;
    @Override protected void attachBaseContext(Context base){
        attachedLanguage=Language.signature(base);super.attachBaseContext(Language.context(base));
    }
    @Override protected void onResume(){
        super.onResume();languageRestarting=!Language.signature(this).equals(attachedLanguage);
        if(languageRestarting)recreate();
    }
}
