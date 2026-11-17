package cat.urysoft.widgets;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;

/** Public identity; the installed package remains stable across updates. */
public final class AppBrand {
    public static final String NAME = "Foldery.2.AI", COMPANY = "UrySoft", VERSION = "1.6.1";
    public static final String OWNER = "Oriol Badia Campanera";
    public static final String LINKEDIN = "https://www.linkedin.com/in/oriol-badia";
    private AppBrand() {}
    public static TextView ownerLink(Activity a) {
        TextView link = Ui.text(a, OWNER + "  ↗", 14, Ui.BLUE, true);
        link.setId(R.id.brand_link); link.setMinimumHeight(Ui.dp(a,48)); link.setGravity(Gravity.CENTER_VERTICAL);
        link.setContentDescription(OWNER + a.getString(R.string.ui_chief_executive_officer_of_urysoft_open_li_52));
        link.setBackground(Ui.ripple(a, android.graphics.Color.TRANSPARENT, 8));
        link.setOnClickListener(v -> {
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LINKEDIN))); }
            catch (ActivityNotFoundException e) { Toast.makeText(a,a.getString(R.string.ui_no_app_is_available_to_open_this_link_53),Toast.LENGTH_LONG).show(); }
        }); return link;
    }
    public static void about(Activity a) {
        LinearLayout content = Ui.column(a); content.setPadding(Ui.dp(a,24), Ui.dp(a,8), Ui.dp(a,24), Ui.dp(a,12));
        content.addView(Ui.text(a, NAME, 26, Ui.INK, true));
        Ui.add(content, Ui.text(a,a.getString(R.string.ui_an_app_by_54) + COMPANY + a.getString(R.string.ui_version_55) + VERSION,14,Ui.MUTED,false),4);
        Ui.add(content,Ui.text(a,a.getString(R.string.ui_your_home_screen_your_way_independent_widg_56),15,Ui.INK,false),18);
        Ui.add(content,Ui.text(a,a.getString(R.string.ui_chief_executive_officer_17),12,Ui.MUTED,true),20);
        content.addView(ownerLink(a));
        Ui.add(content,Ui.text(a,a.getString(R.string.ui_works_offline_without_an_account_the_linke_57),13,Ui.MUTED,false),12);
        ScrollView scroll=new ScrollView(a);scroll.addView(content);
        new AlertDialog.Builder(a).setView(scroll).setPositiveButton(a.getString(R.string.ui_close_58),null).show();
    }
}
