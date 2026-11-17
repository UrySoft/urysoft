package cat.urysoft.widgets;
public final class UpdaterTests {
    static int checks;
    static final String VALID="{\"schemaVersion\":1,\"app\":\"foldery\",\"status\":\"published\",\"versionCode\":20,\"versionName\":\"1.5.9\",\"downloadUrl\":\"https://github.com/UrySoft/urysoft/releases/download/foldery-1.5.9/Foldery-1.5.9.apk\",\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"changelog\":{\"es\":\"Cambios\",\"ca\":\"Canvis\",\"en\":\"Changes\"}}";
    static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    static void invalid(String json,String name){checks++;try{UpdateManifest.parse(json);throw new AssertionError(name);}catch(AssertionError fail){throw fail;}catch(Exception expected){}}
    public static void main(String[] args)throws Exception {
        UpdateManifest valid=UpdateManifest.parse(VALID);
        check(valid.newerThan(19),"offers newer version");
        check(!valid.newerThan(20),"same version is current");
        check(!valid.newerThan(21),"no downgrade offer");
        check("Cambios".equals(valid.notes("es")),"Spanish notes");
        check("Canvis".equals(valid.notes("ca")),"Catalan notes");
        check("Changes".equals(valid.notes("fr")),"English fallback");
        check(!UpdateManifest.parse("{\"schemaVersion\":1,\"app\":\"foldery\",\"status\":\"unpublished\"}").newerThan(19),"unpublished not offered");
        invalid("not json","broken JSON");
        invalid(VALID.replace("\"schemaVersion\":1","\"schemaVersion\":2"),"unknown schema");
        invalid(VALID.replace("\"foldery\"","\"lumen\""),"wrong app");
        invalid(VALID.replace("\"published\"","\"draft\""),"unknown status");
        invalid(VALID.replace("\"versionCode\":20","\"versionCode\":0"),"invalid code");
        invalid(VALID.replace("\"versionCode\":20","\"versionCode\":20.5"),"fractional code");
        invalid(VALID.replace("https://github.com/","http://github.com/"),"HTTP denied");
        invalid(VALID.replace("github.com/","github.com.evil.test/"),"wrong host");
        invalid(VALID.replace("github.com/","github.com@evil.test/"),"userinfo trick");
        invalid(VALID.replace("github.com/","github.com:444/"),"unexpected port");
        invalid(VALID.replace("/UrySoft/urysoft/","/AnotherOwner/urysoft/"),"wrong repository");
        invalid(VALID.replace("foldery-1.5.9/Foldery","../Foldery"),"path traversal");
        invalid(VALID.replace(".apk\"",".apk?redirect=elsewhere\""),"query denied");
        invalid(VALID.replace(".apk\"",".exe\""),"not APK");
        invalid(VALID.replace("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","bad"),"checksum required");
        System.out.println("PASS: "+checks+" updater manifest and version checks.");
    }
}
