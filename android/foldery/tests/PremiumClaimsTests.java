package cat.urysoft.widgets;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class PremiumClaimsTests {
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static String read(Path p)throws Exception{return new String(Files.readAllBytes(p),StandardCharsets.UTF_8).trim();}
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]);String token=read(root.resolve("valid.jwt")),device=read(root.resolve("device.txt")),key=read(root.resolve("public.txt"));long now=System.currentTimeMillis()/1000;
  check(PremiumClaims.valid(token,device,key,now),"valid server signature");
  check(!PremiumClaims.valid(token,"0".repeat(64),key,now),"different install cannot reuse license");
  check(!PremiumClaims.valid(token,device,key,now+31L*86400),"lease expires after offline window");
  check(!PremiumClaims.valid(token,device,key,1),"future-issued license rejected");
  String[] parts=token.split("\\.");String altered=parts[0]+"."+parts[1]+"."+(parts[2].startsWith("A")?"B":"A")+parts[2].substring(1);
  check(!PremiumClaims.valid(altered,device,key,now),"altered signature rejected");
  check(!PremiumClaims.valid(parts[0]+"."+parts[1]+".",device,key,now),"unsigned token rejected");
  check(!PremiumClaims.valid("true",device,key,now),"local paid flag rejected");
  check(!PremiumClaims.valid(token,device,"invalid-public-key",now),"invalid key rejected");
  check(!PremiumClaims.valid(null,device,key,now),"missing token rejected");
  check(!PremiumClaims.valid(token,device,null,now),"missing key rejected");
  System.out.println("PASS: "+checks+" Android license-verifier checks");
 }
}
