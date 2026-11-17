package cat.urysoft.widgets;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.json.JSONObject;

/** Verifies the server signature before interpreting any entitlement. */
final class PremiumClaims {
 static boolean valid(String token,String device,String publicKey,long nowSeconds){
  try{
   if(token==null||token.length()>16000||device==null||!device.matches("[a-f0-9]{64}"))return false;
   String[] parts=token.split("\\.",-1);if(parts.length!=3)return false;
   for(String part:parts)if(!part.matches("[A-Za-z0-9_-]+"))return false;
   JSONObject head=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8));
   if(!"RS256".equals(head.optString("alg"))||!"JWT".equals(head.optString("typ"))||!"foldery-live-2026-01".equals(head.optString("kid")))return false;
   PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
   Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(key);verifier.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
   if(!verifier.verify(Base64.getUrlDecoder().decode(parts[2])))return false;
   JSONObject p=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8));
   for(String name:new String[]{"iat","exp","paidThrough"}){Object v=p.get(name);if(!(v instanceof Integer)&&!(v instanceof Long))return false;}
   long iat=p.getLong("iat"),exp=p.getLong("exp"),paid=p.getLong("paidThrough");
   return "https://urysoft.pages.dev".equals(p.optString("iss"))&&"cat.urysoft.widgets".equals(p.optString("aud"))&&"live".equals(p.optString("environment"))&&device.equals(p.optString("device"))&&p.optString("account").matches("[a-f0-9]{64}")&&"premium".equals(p.optString("tier"))&&iat>0&&iat<=nowSeconds+60&&exp>nowSeconds&&exp>iat&&exp<=iat+30L*86400+60&&paid>=exp;
  }catch(Exception ignored){return false;}
 }
}
