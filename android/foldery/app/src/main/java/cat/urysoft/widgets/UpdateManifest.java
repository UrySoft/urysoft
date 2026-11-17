package cat.urysoft.widgets;

import java.net.URI;
import org.json.JSONObject;

/** The same version-one feed contract used by UrySoft's other Android apps. */
final class UpdateManifest {
    final boolean published;
    final long versionCode;
    final String versionName, downloadUrl;
    final JSONObject changelog;

    private UpdateManifest(boolean published, long code, String name, String url, JSONObject notes) {
        this.published=published;versionCode=code;versionName=name;downloadUrl=url;changelog=notes;
    }
    static UpdateManifest parse(String json) throws Exception {
        JSONObject data=new JSONObject(json);
        if(data.optInt("schemaVersion")!=1 || !"foldery".equals(data.optString("app")))
            throw new IllegalArgumentException("Wrong update feed");
        String status=data.optString("status");
        if("unpublished".equals(status))return new UpdateManifest(false,0,"","",null);
        Object rawCode=data.opt("versionCode");
        if(!"published".equals(status) || !(rawCode instanceof Number))
            throw new IllegalArgumentException("Invalid update status or version");
        Number code=(Number)rawCode;
        long version=code.longValue();
        String name=data.optString("versionName").trim(),url=data.optString("downloadUrl");
        URI uri=new URI(url);
        String path=uri.getPath();
        if(version<=0 || code.doubleValue()!=version || name.isEmpty() || name.length()>80
            || !"https".equals(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())
            || (uri.getPort()!=-1 && uri.getPort()!=443) || uri.getUserInfo()!=null
            || uri.getQuery()!=null || uri.getFragment()!=null || path==null
            || !path.startsWith("/UrySoft/urysoft/releases/download/") || !path.endsWith(".apk")
            || !uri.normalize().equals(uri) || path.contains("..") || path.contains("\\"))
            throw new IllegalArgumentException("Invalid update download");
        if(!data.optString("sha256").matches("[a-fA-F0-9]{64}"))
            throw new IllegalArgumentException("Missing APK checksum");
        return new UpdateManifest(true,version,name,url,data.optJSONObject("changelog"));
    }
    boolean newerThan(long installed){return published && versionCode>installed;}
    String notes(String language){return changelog==null?"":changelog.optString(language,changelog.optString("en",""));}
}
