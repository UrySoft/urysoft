package cat.urysoft.widgets;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class Ui {
    public static int BG = 0xfff4f6fc, INK = 0xff15203b, MUTED = 0xff53617b, BLUE = 0xff3159de, LINE = 0xffdce2ef, SURFACE = Color.WHITE, SOFT = 0xffeaf0ff;
    public static boolean DARK;
    public static int themeMode(Context c) { return c.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("mode", 0); }
    public static void setThemeMode(Activity a, int mode) { a.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("mode", mode).apply(); a.recreate(); }
    public static boolean isDark(Context c) {
        int mode = themeMode(c);
        return mode == 2 || (mode == 0 && (c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    }
    public static void applyTheme(Activity a) {
        DARK = isDark(a); a.setTheme(DARK ? R.style.AppThemeDark : R.style.AppTheme);
        BG = DARK ? 0xff0d1422 : 0xfff4f6fc; INK = DARK ? 0xffeef3ff : 0xff15203b;
        MUTED = DARK ? 0xffadbbd2 : 0xff53617b; BLUE = DARK ? 0xffa4bdff : 0xff3159de;
        LINE = DARK ? 0xff31415d : 0xffdce2ef; SURFACE = DARK ? 0xff182235 : Color.WHITE; SOFT = DARK ? 0xff23334f : 0xffeaf0ff;
    }
    public static Spinner themeSelector(Activity a) {
        Spinner spinner = new Spinner(a); spinner.setId(R.id.app_theme); spinner.setContentDescription(a.getString(R.string.ui_app_theme_303)); spinner.setMinimumHeight(dp(a, 48));
        spinner.setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, new String[]{a.getString(R.string.ui_follow_device_settings_304), a.getString(R.string.ui_light_mode_305), a.getString(R.string.ui_dark_mode_306)}));
        spinner.setSelection(themeMode(a));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { if (position != themeMode(a)) setThemeMode(a, position); }
            public void onNothingSelected(AdapterView<?> parent) {}
        }); return spinner;
    }
    public static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    public static LinearLayout column(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v; }
    public static LinearLayout row(Context c) { LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    public static GradientDrawable rounded(int color, int radius, Context c) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d; }
    public static RippleDrawable ripple(Context c, int color, int radius) { return new RippleDrawable(ColorStateList.valueOf(DARK ? 0x26ffffff : 0x183159de), rounded(color,radius,c), rounded(Color.WHITE,radius,c)); }
    public static ImageButton iconButton(Context c, String kind, String description, View.OnClickListener action) {
        ImageButton b=new ImageButton(c);b.setImageDrawable(new Glyph(kind,INK));b.setContentDescription(description);b.setPadding(dp(c,13),dp(c,13),dp(c,13),dp(c,13));
        b.setBackground(ripple(c,SOFT,14));b.setMinimumHeight(dp(c,48));b.setMinimumWidth(dp(c,48));b.setOnClickListener(action);return b;
    }
    public static void toolbarActions(Activity a,LinearLayout bar){
        ImageButton help=iconButton(a,"help",a.getString(R.string.ui_help_24),v->help(a));help.setId(R.id.help_button);bar.addView(help,new LinearLayout.LayoutParams(dp(a,48),dp(a,48)));
        ImageButton options=iconButton(a,"settings",a.getString(R.string.ui_app_options_307),v->a.startActivity(new Intent(a,OptionsActivity.class)));options.setId(R.id.options_button);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(a,48),dp(a,48));p.setMarginStart(dp(a,4));bar.addView(options,p);
    }
    public static void heading(LinearLayout parent, String icon, String title, String detail) {
        Context c=parent.getContext();LinearLayout row=row(c);ImageView glyph=new ImageView(c);glyph.setImageDrawable(new Glyph(icon,BLUE));glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(glyph,new LinearLayout.LayoutParams(dp(c,22),dp(c,22)));
        TextView text=text(c,title,18,INK,true);text.setPadding(dp(c,10),0,0,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));parent.addView(row);
        if(detail!=null)add(parent,text(c,detail,14,MUTED,false),8);
    }
    /** Turns an existing configuration card into a keyboard-accessible disclosure. */
    public static void collapsible(LinearLayout card,String key) {
        if(card.getChildCount()==0||!(card.getChildAt(0) instanceof LinearLayout))return;
        Context c=card.getContext();LinearLayout header=(LinearLayout)card.getChildAt(0);
        String title=((TextView)header.getChildAt(1)).getText().toString();
        LinearLayout body=column(c);
        while(card.getChildCount()>1){View child=card.getChildAt(1);card.removeViewAt(1);body.addView(child);}
        card.addView(body,new LinearLayout.LayoutParams(-1,-2));
        TextView arrow=text(c,"",22,BLUE,true);arrow.setGravity(Gravity.CENTER);header.addView(arrow,new LinearLayout.LayoutParams(dp(c,32),dp(c,48)));
        header.setMinimumHeight(dp(c,48));header.setBackground(ripple(c,SURFACE,12));header.setFocusable(true);header.setClickable(true);
        for(int i=0;i<header.getChildCount();i++)header.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        android.content.SharedPreferences prefs=c.getSharedPreferences("editor_sections",Context.MODE_PRIVATE);
        final boolean[] expanded={false};
        Runnable apply=()->{
            body.setVisibility(expanded[0]?View.VISIBLE:View.GONE);arrow.setText(expanded[0]?"⌃":"⌄");
            header.setContentDescription(title+", "+(expanded[0]?c.getString(R.string.ui_expanded_collapse_309):c.getString(R.string.ui_collapsed_expand_310)));
            if(Build.VERSION.SDK_INT>=30)header.setStateDescription(expanded[0]?c.getString(R.string.ui_expanded_311):c.getString(R.string.ui_collapsed_312));
        };
        header.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());
                info.addAction(expanded[0]?android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE:android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            }
            @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args){
                if((action==android.view.accessibility.AccessibilityNodeInfo.ACTION_EXPAND&&!expanded[0])||(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_COLLAPSE&&expanded[0]))return host.performClick();
                return super.performAccessibilityAction(host,action,args);
            }
        });
        header.setOnClickListener(v->{
            if(expanded[0]&&body.hasFocus())header.requestFocus();
            expanded[0]=!expanded[0];prefs.edit().putBoolean(key,expanded[0]).apply();apply.run();
            header.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        });apply.run();
    }
    public static void divider(LinearLayout parent) { View line=new View(parent.getContext());line.setBackgroundColor(LINE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(parent.getContext(),1));p.topMargin=dp(parent.getContext(),12);p.bottomMargin=dp(parent.getContext(),12);parent.addView(line,p); }
    public static TextView text(Context c, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setLineSpacing(dp(c, 2), 1); return t;
    }
    public static Button button(Context c, String value, boolean primary, View.OnClickListener action) {
        Button b = new Button(c); b.setText(value); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary ? (DARK ? 0xff10203d : Color.WHITE) : BLUE);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setMinHeight(dp(c, 48)); b.setMinimumHeight(dp(c,48));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6)); b.setBackground(rounded(primary ? BLUE : SOFT, 16, c));
        String kind="check";
        if(value.equals(c.getString(R.string.ui_add_to_home_screen_237)))kind="home";
        else if(value.equals(c.getString(R.string.delete_widget)))kind="delete";
        else if(value.equals(c.getString(R.string.ui_customize_widget_16)))kind="settings";
        else if(value.equals(c.getString(R.string.ui_refresh_photos_164)))kind="refresh";
        else if(value.equals(c.getString(R.string.ui_add_contact_163)))kind="contact";
        else if(value.equals(c.getString(R.string.ui_create_widget_19)))kind="add";
        else if(value.toLowerCase(java.util.Locale.ROOT).contains("color"))kind="palette";
        Glyph symbol=new Glyph(kind,primary?(DARK?0xff10203d:Color.WHITE):BLUE);symbol.setBounds(0,0,dp(c,18),dp(c,18));b.setCompoundDrawables(symbol,null,null,null);b.setCompoundDrawablePadding(dp(c,6));b.setContentDescription(value);b.setTooltipText(value);
        b.setBackground(ripple(c,primary?BLUE:SOFT,14));b.setStateListAnimator(null);b.setOnClickListener(action);return b;
    }
    public static void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(parent.getContext(), top); parent.addView(child, p);
    }
    public static void spacer(LinearLayout p, int height) { View v = new View(p.getContext()); p.addView(v, new LinearLayout.LayoutParams(1, dp(p.getContext(), height))); }
    public static LinearLayout card(LinearLayout parent) {
        Context c = parent.getContext(); LinearLayout card = column(c); card.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        GradientDrawable bg=rounded(SURFACE,20,c);bg.setStroke(dp(c,1),LINE);card.setBackground(bg);add(parent,card,14);return card;
    }
    public static LinearLayout setup(Activity a) {
        LinearLayout root = column(a); root.setBackgroundColor(BG); a.setContentView(root);
        int lightBars = DARK ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= 27 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | lightBars);
        if (Build.VERSION.SDK_INT >= 30 && a.getWindow().getInsetsController()!=null) a.getWindow().getInsetsController().setSystemBarsAppearance(DARK ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        if (Build.VERSION.SDK_INT >= 30) a.getWindow().setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets(); return root;
    }
    public static LinearLayout scroll(LinearLayout root) {
        Context c = root.getContext(); ScrollView scroll = new ScrollView(c); scroll.setFillViewport(true);
        LinearLayout page = column(c); page.setPadding(dp(c, 20), dp(c, 16), dp(c, 20), dp(c, 28)); scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return page;
    }
    public static void section(LinearLayout page, String label) { TextView t = text(page.getContext(), label, 12, MUTED, true); t.setLetterSpacing(.09f); add(page, t, 26); }
    public interface Toggle { void change(boolean checked); }
    public static LinearLayout toggle(LinearLayout parent, String title, String detail, boolean value, Toggle change) {
        Context c = parent.getContext(); LinearLayout r = row(c); r.setPadding(0, dp(c, 10), 0, dp(c, 10));
        LinearLayout copy = column(c); copy.addView(text(c, title, 16, INK, true)); if (detail != null) add(copy, text(c, detail, 13, MUTED, false), 3);
        r.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); Switch s = new Switch(c); s.setContentDescription(title); s.setChecked(value);
        s.setButtonTintList(ColorStateList.valueOf(BLUE)); s.setThumbTintList(new ColorStateList(new int[][] {new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{BLUE, 0xff8a96ae}));
        s.setMinHeight(dp(c, 48)); s.setPadding(dp(c, 12), 0, 0, 0); r.addView(s);
        s.setOnCheckedChangeListener((b, v) -> change.change(v)); r.setOnClickListener(v -> s.setChecked(!s.isChecked())); add(parent, r, 0); return r;
    }
    public static void notificationAccess(Activity a) {
        new AlertDialog.Builder(a).setTitle(a.getString(R.string.ui_enable_notification_badges_33))
            .setMessage(a.getString(R.string.ui_android_will_ask_for_notification_access_f_313))
            .setNegativeButton(a.getString(R.string.ui_not_now_314), null).setPositiveButton(a.getString(R.string.ui_continue_315), (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                if (Build.VERSION.SDK_INT >= 30) intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, new ComponentName(a, NotificationCountService.class).flattenToString());
                try { a.startActivity(intent); } catch (ActivityNotFoundException e) { a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            }).show();
    }
    public static void help(Activity a) {
        new AlertDialog.Builder(a).setTitle(a.getString(R.string.ui_your_widget_in_three_steps_316))
            .setMessage(a.getString(R.string.ui_1_select_your_apps_or_add_contacts_if_you__317))
            .setPositiveButton(a.getString(R.string.ui_got_it_300), null).setNeutralButton(a.getString(R.string.ui_app_information_318), (d, w) -> a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + a.getPackageName())))).show();
    }
}
