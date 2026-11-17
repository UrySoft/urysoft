package cat.urysoft.widgets;

import java.util.Locale;

/** Stable picker values, independent from translated labels. */
public final class LanguagePolicy {
    private LanguagePolicy(){}
    public static String normalize(String value){
        if(value==null)return "";
        String language=Locale.forLanguageTag(value.replace('_','-')).getLanguage();
        return "ca".equals(language)||"es".equals(language)||"en".equals(language)?language:"";
    }
    public static int index(String value){String tag=normalize(value);return "ca".equals(tag)?1:"es".equals(tag)?2:"en".equals(tag)?3:0;}
    public static String tag(int index){return index==1?"ca":index==2?"es":index==3?"en":"";}
}
