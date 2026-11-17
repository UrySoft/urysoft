package cat.urysoft.widgets;

import android.content.Context;

/** Private install identity and a server-signed, device-bound offline entitlement. */
final class PremiumLicense {
    private PremiumLicense(){}
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("foldery_premium_live",0);}
    static synchronized String token(Context c){
        String value=prefs(c).getString("deviceToken",null);
        if(value==null){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);value="fp_"+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);if(!prefs(c).edit().putString("deviceToken",value).commit())throw new IllegalStateException("Private storage unavailable");}
        return value;
    }
    static String device(Context c){try{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(token(c).getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:bytes)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean active(Context context){
        long now=System.currentTimeMillis()/1000,seen=prefs(context).getLong("seen",0);
        if(now+300<seen)return false;
        if(now>seen)prefs(context).edit().putLong("seen",now).apply();
        return PremiumClaims.valid(prefs(context).getString("license",null),device(context),PremiumPublicKey.SPKI,Math.max(now,seen));
    }
    static void save(Context c,String license){
        if(license==null){prefs(c).edit().remove("license").apply();return;}
        if(!PremiumClaims.valid(license,device(c),PremiumPublicKey.SPKI,System.currentTimeMillis()/1000))throw new IllegalArgumentException("Invalid license");
        prefs(c).edit().putString("license",license).putLong("seen",System.currentTimeMillis()/1000).apply();
    }
}
