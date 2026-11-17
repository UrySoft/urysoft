package cat.urysoft.widgets;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class CrossEnvironmentTests {
 static String read(Path dir,String name)throws Exception{return new String(Files.readAllBytes(dir.resolve(name)),StandardCharsets.UTF_8).trim();}
 public static void main(String[] args)throws Exception{
  Path dir=Paths.get(args[0]);String device=read(dir,"device.txt"),key=read(dir,"public.txt");long now=System.currentTimeMillis()/1000;
  if(!PremiumClaims.valid(read(dir,"valid.jwt"),device,key,now))throw new AssertionError("live license rejected");
  if(PremiumClaims.valid(read(dir,"sandbox-environment.jwt"),device,key,now))throw new AssertionError("correctly signed test environment accepted by live verifier");
  if(PremiumClaims.valid(read(dir,"sandbox-key-id.jwt"),device,key,now))throw new AssertionError("test signing-key identity accepted by live verifier");
  System.out.println("PASS: live license accepted; correctly signed Sandbox environment and key ID rejected.");
 }
}
