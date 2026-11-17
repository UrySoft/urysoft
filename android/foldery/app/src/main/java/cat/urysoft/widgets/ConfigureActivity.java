package cat.urysoft.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.InputFilter;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.Normalizer;
import java.util.*;

public final class ConfigureActivity extends LanguageActivity {
    static final int PICK_CONTACT = 6104;
    static final int PHOTO_PERMISSION = 6105;
    private Button contactButton, refreshPhotosButton;
    private String pendingContactUri, pendingPhotoAction;
    private boolean allowPhotoPermission=true;
    private int themeMode;
    private boolean darkTheme;
    private int catalogRequest;
    private final Button[] directionButtons = new Button[8];
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetConfig config;
    private PreviewView preview;
    private LinearLayout root, previewPanel, previewDock, inlinePreviewSlot;
    private LinearLayout shadowControls;
    private Button shadowColorButton,titleColorButton;
    private TextView layoutSummary;
    private LinearLayout automaticSizeControls;
    private ScrollView settingsScroll;
    private final LinearLayout[] panels = new LinearLayout[4];
    private final Button[] tabButtons = new Button[4];
    private int selectedPanel;
    private String initialConfig;
    private Button undoButton;
    private String removedApp;
    private int removedPosition;
    private boolean adjustingPreview;
    private TextView selectedCount, opacityValue;
    private EditText nameInput;
    private SeekBar opacitySlider;
    private Button chooseButton, saveButton;
    private List<AppCatalog.Entry> entries = Collections.emptyList();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean closed;
    private final Runnable previewUpdate = () -> {
        if (preview == null || closed) return;
        preview.configure(config);renderSelectionSummary();updateLayoutSummary();updateStickyPreview();
    };

    @Override public void onCreate(Bundle state) {
        Ui.applyTheme(this); super.onCreate(state); setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if(ConfigStore.isDeleted(this,widgetId)){Toast.makeText(this,R.string.widget_deleted,Toast.LENGTH_LONG).show();finish();return;}
            AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            if (info == null || !new ComponentName(this, WidgetProvider.class).equals(info.provider)) { finish(); return; }
        }
        if (state != null) config = WidgetConfig.fromJson(state.getString("config"));
        else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) config = ConfigStore.has(this, widgetId) ? ConfigStore.get(this, widgetId) : ConfigStore.takeTemplate(this);
        else config = WidgetConfig.defaults(this);
        selectedPanel=state==null?0:state.getInt("panel",0);
        initialConfig=state==null?config.toJson():state.getString("initialConfig",config.toJson());
        pendingContactUri=state==null?null:state.getString("pendingContactUri");
        pendingPhotoAction=state==null?null:state.getString("pendingPhotoAction");
        allowPhotoPermission=state==null||state.getBoolean("allowPhotoPermission",true);
        themeMode=Ui.themeMode(this);darkTheme=Ui.isDark(this);
        render();
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::leaveEditor);
        reloadCatalog();
        if(pendingContactUri!=null)importContact(Uri.parse(pendingContactUri),allowPhotoPermission);
        if("refreshing".equals(pendingPhotoAction))refreshContactPhotos();
    }
    @Override public void onResume() { super.onResume();if(languageRestarting)return;if(themeMode!=Ui.themeMode(this)||darkTheme!=Ui.isDark(this)){recreate();return;}if(preview!=null)updatePreview();NotificationCountService.rescan(); }
    // Android 13+ uses the OnBackInvokedDispatcher registered in onCreate.
    // This override handles the Back button only on earlier supported versions.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { leaveEditor(); }
    @Override public void onDestroy() { closed = true; main.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle out) { if (config != null) { out.putString("config", config.toJson()); out.putInt("panel", selectedPanel); out.putString("initialConfig", initialConfig); out.putString("pendingContactUri",pendingContactUri);out.putString("pendingPhotoAction",pendingPhotoAction);out.putBoolean("allowPhotoPermission",allowPhotoPermission); } super.onSaveInstanceState(out); }
    private void updatePreview() { main.removeCallbacks(previewUpdate); main.postDelayed(previewUpdate, 60); }
    private void render() {
        root=Ui.setup(this);root.setId(R.id.editor_root);
        LinearLayout toolbar=Ui.row(this);toolbar.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,8));
        toolbar.addView(Ui.iconButton(this,"back",getString(R.string.ui_back_21),v->leaveEditor()),new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        LinearLayout title=Ui.column(this);title.setPadding(Ui.dp(this,12),0,Ui.dp(this,8),0);title.addView(Ui.text(this,widgetId==AppWidgetManager.INVALID_APPWIDGET_ID?getString(R.string.ui_new_widget_151):getString(R.string.ui_customize_widget_16),18,Ui.INK,true));title.addView(Ui.text(this,AppBrand.NAME+" · "+AppBrand.COMPANY,12,Ui.MUTED,false));toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Ui.toolbarActions(this,toolbar);root.addView(toolbar);
        previewDock=Ui.column(this);previewDock.setId(R.id.preview_dock);root.addView(previewDock);
        HorizontalScrollView navigation=new HorizontalScrollView(this);navigation.setHorizontalScrollBarEnabled(false);navigation.setFillViewport(true);navigation.setPadding(Ui.dp(this,16),Ui.dp(this,4),Ui.dp(this,16),Ui.dp(this,4));
        LinearLayout tabs=Ui.row(this);navigation.addView(tabs);String[] names={getString(R.string.ui_apps_152),getString(R.string.ui_icons_153),getString(R.string.ui_background_154),getString(R.string.ui_options_22)},glyphs={"apps","icons","layers","settings"};int[] tabIds={R.id.tab_apps,R.id.tab_icons,R.id.tab_background,R.id.tab_settings};
        for(int i=0;i<4;i++) { final int index=i;Button tab=Ui.button(this,names[i],false,v->selectPanel(index));tab.setId(tabIds[i]);tab.setTextSize(14);tab.setMinWidth(0);tab.setMinimumWidth(0);tab.setMinHeight(Ui.dp(this,48));tab.setPadding(Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,10));Glyph glyph=new Glyph(glyphs[i],Ui.BLUE);glyph.setBounds(0,0,Ui.dp(this,16),Ui.dp(this,16));tab.setCompoundDrawables(glyph,null,null,null);tab.setCompoundDrawablePadding(Ui.dp(this,6));tabButtons[i]=tab;tabs.addView(tab,new LinearLayout.LayoutParams(-2,-2)); }
        root.addView(navigation);
        settingsScroll=new ScrollView(this);settingsScroll.setId(R.id.settings_scroll);settingsScroll.setFillViewport(true);settingsScroll.setClipToPadding(false);
        LinearLayout scrollContent=Ui.column(this);settingsScroll.addView(scrollContent);root.addView(settingsScroll,new LinearLayout.LayoutParams(-1,0,1));
        inlinePreviewSlot=Ui.column(this);inlinePreviewSlot.setId(R.id.inline_preview);scrollContent.addView(inlinePreviewSlot);
        LinearLayout page=Ui.column(this);page.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),Ui.dp(this,24));scrollContent.addView(page);
        previewPanel=Ui.column(this);previewPanel.setPadding(Ui.dp(this,20),Ui.dp(this,4),Ui.dp(this,20),Ui.dp(this,10));
        LinearLayout previewTitle=Ui.row(this);TextView label=Ui.text(this,getString(R.string.ui_preview_155),10,Ui.MUTED,true);label.setLetterSpacing(.10f);previewTitle.addView(label,new LinearLayout.LayoutParams(0,-2,1));previewTitle.addView(Ui.text(this,getString(R.string.ui_live_156),11,Ui.BLUE,true));previewPanel.addView(previewTitle);
        preview=new PreviewView(this);preview.setId(R.id.editor_preview);
        if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID) { int[] bounds=WidgetRenderer.bounds(this,widgetId);preview.setWidgetBounds(bounds[0],bounds[1]); }
        preview.setListener((apps,removed,position)->{
            config.apps.clear();config.apps.addAll(apps);
            if(removed!=null) { removedApp=removed;removedPosition=position;undoButton.setText(getString(R.string.ui_undo_157)+AppCatalog.label(this,removed));undoButton.setVisibility(View.VISIBLE); }
            renderSelectionSummary();updatePreview();
        });
        LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(-1,Ui.dp(this,210));previewParams.topMargin=Ui.dp(this,8);previewPanel.addView(preview,previewParams);
        Ui.add(previewPanel,Ui.text(this,getString(R.string.ui_hold_and_drag_to_reorder_to_remove_158),12,Ui.MUTED,false),6);
        undoButton=Ui.button(this,getString(R.string.ui_undo_159),false,v->{if(removedApp!=null&&!config.apps.contains(removedApp)){config.apps.add(Math.min(removedPosition,config.apps.size()),removedApp);removedApp=null;undoButton.setVisibility(View.GONE);updatePreview();}});undoButton.setVisibility(View.GONE);Ui.add(previewPanel,undoButton,5);
        inlinePreviewSlot.addView(previewPanel);root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{updateStickyPreview();updateLayoutSummary();});
        int[] panelIds={R.id.panel_apps,R.id.panel_icons,R.id.panel_background,R.id.panel_settings};
        for(int i=0;i<4;i++){panels[i]=Ui.column(this);panels[i].setId(panelIds[i]);page.addView(panels[i]);}

        LinearLayout appCard=Ui.card(panels[0]);Ui.heading(appCard,"apps",getString(R.string.ui_apps_and_contacts_160),getString(R.string.ui_combine_your_apps_with_shortcuts_to_the_pe_161));
        selectedCount=Ui.text(this,"",14,Ui.BLUE,true);Ui.add(appCard,selectedCount,16);
        chooseButton=Ui.button(this,getString(R.string.ui_loading_apps_162),true,v->openPicker());chooseButton.setEnabled(false);Ui.add(appCard,chooseButton,12);
        contactButton=Ui.button(this,getString(R.string.ui_add_contact_163),false,v->pickContact());contactButton.setId(R.id.add_contact);Ui.add(appCard,contactButton,10);
        refreshPhotosButton=Ui.button(this,getString(R.string.ui_refresh_photos_164),false,v->requestPhotoRefresh());refreshPhotosButton.setId(R.id.refresh_contact_photos);Ui.add(appCard,refreshPhotosButton,8);
        Ui.add(appCard,Ui.text(this,getString(R.string.ui_uses_the_contact_s_photo_as_its_icon_tap_t_165),12,Ui.MUTED,false),8);
        Ui.add(appCard,Ui.text(this,getString(R.string.ui_android_may_ask_for_permission_to_read_pho_166),12,Ui.MUTED,false),6);
        LinearLayout name=Ui.card(panels[0]);Ui.heading(name,"folder",getString(R.string.ui_widget_name_167),null);
        nameInput=input(getString(R.string.ui_for_example_every_day_168),config.name);nameInput.setContentDescription(getString(R.string.ui_widget_name_167));nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64)});Ui.add(name,nameInput,12);watch(nameInput,text->{config.name=text;updatePreview();});

        LinearLayout appearance=Ui.card(panels[1]);Ui.heading(appearance,"apps",getString(R.string.ui_columns_and_size_169),getString(R.string.ui_icons_fill_the_usable_width_of_each_column_170));
        addChoice(appearance,getString(R.string.ui_columns_171),new String[]{getString(R.string.ui_automatic_172),getString(R.string.ui_1_column_173),getString(R.string.ui_2_columns_174),getString(R.string.ui_3_columns_175),getString(R.string.ui_4_columns_176),getString(R.string.ui_5_columns_177),getString(R.string.ui_6_columns_178),getString(R.string.ui_7_columns_179),getString(R.string.ui_8_columns_180)},config.columns,value->{config.columns=value;if(automaticSizeControls!=null)automaticSizeControls.setVisibility(value==0?View.VISIBLE:View.GONE);updatePreview();});
        automaticSizeControls=Ui.column(this);appearance.addView(automaticSizeControls);automaticSizeControls.setVisibility(config.columns==0?View.VISIBLE:View.GONE);
        slider(automaticSizeControls,getString(R.string.ui_icon_size_181),28,300,config.autoIconDp,value->{config.autoIconDp=value;updatePreview();return Integer.toString(value);},R.id.auto_icon_size);
        Ui.add(automaticSizeControls,Ui.text(this,getString(R.string.ui_the_column_count_adapts_to_your_chosen_siz_182),12,Ui.MUTED,false),4);
        layoutSummary=Ui.text(this,"",14,Ui.BLUE,true);layoutSummary.setId(R.id.layout_summary);Ui.add(appearance,layoutSummary,8);
        Ui.add(appearance,Ui.text(this,getString(R.string.ui_size_follows_the_widget_width_and_your_mar_183),13,Ui.MUTED,false),8);
        LinearLayout titles=Ui.card(panels[1]);titles.setId(R.id.title_controls);Ui.heading(titles,"icons",getString(R.string.ui_titles_184),null);
        Ui.toggle(titles,getString(R.string.ui_show_titles_185),getString(R.string.ui_the_name_of_each_app_or_contact_186),config.labels,value->{config.labels=value;updatePreview();});
        slider(titles,getString(R.string.ui_text_size_187),8,32,config.labelSizeSp,value->{config.labelSizeSp=value;updatePreview();return Integer.toString(value);},R.id.label_size);
        titleColorButton=Ui.button(this,"",false,v->customColor(3));titleColorButton.setId(R.id.label_color);Ui.add(titles,titleColorButton,10);updateTitleColorLabel();
        LinearLayout labelColors=Ui.row(this);LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMarginEnd(Ui.dp(this,8));
        labelColors.addView(Ui.button(this,getString(R.string.ui_white_188),false,v->{config.labelColor=Color.WHITE;updateTitleColorLabel();updatePreview();}),half);
        labelColors.addView(Ui.button(this,getString(R.string.ui_dark_189),false,v->{config.labelColor=0xff15203b;updateTitleColorLabel();updatePreview();}),new LinearLayout.LayoutParams(0,-2,1));Ui.add(titles,labelColors,8);
        Ui.add(titles,Ui.text(this,getString(R.string.ui_text_shadows_share_the_icon_s_color_sharpn_190),12,Ui.MUTED,false),10);
        LinearLayout spacing=Ui.card(panels[1]);spacing.setId(R.id.spacing_controls);Ui.heading(spacing,"settings",getString(R.string.ui_margins_and_spacing_191),getString(R.string.ui_adjust_the_space_used_by_each_shortcut_192));
        slider(spacing,getString(R.string.ui_icon_to_title_193),0,32,config.labelGapDp,value->{config.labelGapDp=value;updatePreview();return Integer.toString(value);},R.id.label_gap);
        addAlignment(spacing);
        slider(spacing,getString(R.string.ui_horizontal_spacing_194),0,20,config.columnGapDp/2,value->{config.columnGapDp=value*2;updatePreview();return Integer.toString(value*2);},R.id.column_gap);
        slider(spacing,getString(R.string.ui_row_spacing_195),0,20,config.rowGapDp/2,value->{config.rowGapDp=value*2;updatePreview();return Integer.toString(value*2);},R.id.row_gap);
        slider(spacing,getString(R.string.ui_widget_side_margins_196),0,40,config.paddingHorizontalDp,value->{config.paddingHorizontalDp=value;updatePreview();return Integer.toString(value);},R.id.widget_padding_horizontal);
        slider(spacing,getString(R.string.ui_top_and_bottom_margins_197),0,40,config.paddingVerticalDp,value->{config.paddingVerticalDp=value;updatePreview();return Integer.toString(value);},R.id.widget_padding_vertical);
        Ui.add(spacing,Ui.button(this,getString(R.string.ui_reset_spacing_198),false,v->{((SeekBar)findViewById(R.id.label_gap)).setProgress(6);((SeekBar)findViewById(R.id.column_gap)).setProgress(6);((SeekBar)findViewById(R.id.row_gap)).setProgress(6);((SeekBar)findViewById(R.id.widget_padding_horizontal)).setProgress(12);((SeekBar)findViewById(R.id.widget_padding_vertical)).setProgress(12);}),10);
        LinearLayout icons=Ui.card(panels[1]);Ui.heading(icons,"icons",getString(R.string.ui_shape_and_style_199),null);
        GridLayout shapes=new GridLayout(this);shapes.setColumnCount(3);String[] shapeNames=IconRenderer.shapes(this);
        for(int i=0;i<shapeNames.length;i++){final int kind=i;ShapeChoice choice=new ShapeChoice(this,i,i>2?PremiumOffer.label(this,shapeNames[i]):shapeNames[i]);choice.setSelected(config.iconShape==i);choice.setOnClickListener(v->{config.iconShape=kind;for(int n=0;n<shapes.getChildCount();n++){shapes.getChildAt(n).setSelected(n==kind);shapes.getChildAt(n).invalidate();}updatePreview();if(kind>2 && (premiumBaseline()==null || premiumBaseline().iconShape!=kind))previewOffer(PremiumPolicy.Feature.SHAPES);});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=Ui.dp(this,80);lp.columnSpec=GridLayout.spec(i%3,1f);lp.rowSpec=GridLayout.spec(i/3);shapes.addView(choice,lp);}Ui.add(icons,shapes,10);
        String[] finishes=IconRenderer.styles(this);for(int i=2;i<finishes.length;i++)finishes[i]=PremiumOffer.label(this,finishes[i]);
        addChoice(icons,getString(R.string.ui_style_201),finishes,config.iconStyle,value->{boolean changed=config.iconStyle!=value;config.iconStyle=value;updatePreview();if(changed&&value>1)previewOffer(PremiumPolicy.Feature.STYLES);});
        Ui.add(icons,Ui.button(this,getString(R.string.ui_tint_and_frame_color_202),false,v->customColor(1)),10);
        LinearLayout shadow=Ui.card(panels[1]);Ui.heading(shadow,"layers",getString(R.string.ui_icon_and_title_shadow_203),getString(R.string.ui_both_elements_share_the_same_effect_204));
        LinearLayout shadowToggle=Ui.toggle(shadow,PremiumOffer.label(this,getString(R.string.ui_enable_shadow_205)),getString(R.string.ui_independent_settings_for_this_widget_206),config.shadowEnabled,value->{config.shadowEnabled=value;shadowControls.setVisibility(value?View.VISIBLE:View.GONE);updatePreview();if(value)previewOffer(PremiumPolicy.Feature.SHADOWS);});shadowToggle.setId(R.id.shadow_toggle);
        shadowControls=Ui.column(this);shadowControls.setId(R.id.shadow_controls);shadow.addView(shadowControls);shadowControls.setVisibility(config.shadowEnabled?View.VISIBLE:View.GONE);
        shadowColorButton=Ui.button(this,"",false,v->customColor(2));shadowColorButton.setId(R.id.shadow_color);Ui.add(shadowControls,shadowColorButton,12);updateShadowColorLabel();
        slider(shadowControls,getString(R.string.ui_opacity_207),0,100,config.shadowOpacity,value->{config.shadowOpacity=value;updatePreview();return value+" %";},R.id.shadow_opacity);
        slider(shadowControls,getString(R.string.ui_sharpness_208),0,100,config.shadowSharpness,value->{config.shadowSharpness=value;updatePreview();return value+" %";},R.id.shadow_sharpness);
        Ui.add(shadowControls,Ui.text(this,getString(R.string.ui_0_soft_100_sharp_edge_209),12,Ui.MUTED,false),0);
        slider(shadowControls,getString(R.string.ui_distance_from_icon_210),0,16,config.shadowDistance,value->{config.shadowDistance=value;updatePreview();return value==0?getString(R.string.ui_attached_211):Integer.toString(value);},R.id.shadow_distance);
        addShadowDirection(shadowControls);
        Ui.add(shadowControls,Ui.text(this,getString(R.string.ui_shadows_do_not_change_the_layout_very_smal_212),12,Ui.MUTED,false),0);
        Ui.add(shadowControls,Ui.button(this,getString(R.string.ui_reset_shadow_213),false,v->{config.shadowColor=Color.BLACK;((SeekBar)findViewById(R.id.shadow_opacity)).setProgress(45);((SeekBar)findViewById(R.id.shadow_sharpness)).setProgress(60);((SeekBar)findViewById(R.id.shadow_distance)).setProgress(4);((SeekBar)findViewById(R.id.shadow_angle)).setProgress(90);updateShadowColorLabel();updatePreview();}),12);

        LinearLayout background=Ui.card(panels[2]);Ui.heading(background,"layers",getString(R.string.ui_widget_background_214),getString(R.string.ui_transparent_by_default_icons_always_remain_215));
        GridLayout swatches=new GridLayout(this);swatches.setColumnCount(3);int[] colors={0xff18233e,0xffffffff,0xff000000,0xff3159de,0xff8162c9,0xff167b71};String[] colorNames={getString(R.string.ui_navy_216),getString(R.string.ui_white_188),getString(R.string.ui_black_217),getString(R.string.ui_blue_218),getString(R.string.ui_lavender_219),getString(R.string.ui_green_220)};
        for(int i=0;i<colors.length;i++){final int color=colors[i];Button b=Ui.button(this,colorNames[i],false,v->setColor(color));b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(Ui.dp(this,5),Ui.dp(this,10),Ui.dp(this,5),Ui.dp(this,10));b.setTextColor(color==Color.WHITE?0xff15203b:Color.WHITE);b.setBackground(Ui.ripple(this,color,12));GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.columnSpec=GridLayout.spec(i%3,1f);gp.rowSpec=GridLayout.spec(i/3);gp.setMargins(Ui.dp(this,3),Ui.dp(this,5),Ui.dp(this,3),Ui.dp(this,5));swatches.addView(b,gp);}
        Ui.add(background,swatches,12);Ui.add(background,Ui.button(this,getString(R.string.ui_choose_another_color_221),false,v->customColor(0)),8);Ui.add(background,Ui.button(this,getString(R.string.ui_transparent_background_13),false,v->{config.opacity=0;opacitySlider.setProgress(0);updatePreview();}),8);
        Ui.divider(background);LinearLayout opacityRow=Ui.row(this);opacityRow.addView(Ui.text(this,getString(R.string.ui_opacity_222),15,Ui.INK,true),new LinearLayout.LayoutParams(0,-2,1));opacityValue=Ui.text(this,config.opacity+" %",15,Ui.BLUE,true);opacityRow.addView(opacityValue);background.addView(opacityRow);
        opacitySlider=new SeekBar(this);opacitySlider.setMax(100);opacitySlider.setProgress(config.opacity);opacitySlider.setMinimumHeight(Ui.dp(this,48));opacitySlider.setContentDescription(getString(R.string.ui_background_opacity_percentage_223));opacitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean user){config.opacity=value;opacityValue.setText(value+" %");updatePreview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});Ui.add(background,opacitySlider,4);
        slider(background,getString(R.string.ui_corner_rounding_224),0,32,config.cornerDp,value->{config.cornerDp=value;updatePreview();return Integer.toString(value);});

        Ui.add(background,Ui.button(this,PremiumOffer.label(this,getString(R.string.choose_background_image)),false,v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,704);}catch(ActivityNotFoundException e){Toast.makeText(this,R.string.background_image_error,Toast.LENGTH_LONG).show();}}),8);
        Ui.add(background,Ui.button(this,getString(R.string.remove_background_image),false,v->{config.backgroundImage="";updatePreview();}),6);
        addChoice(background,getString(R.string.background_image_mode),new String[]{getString(R.string.bg_crop),getString(R.string.bg_fit),getString(R.string.bg_stretch),getString(R.string.bg_tile),getString(R.string.bg_center)},config.backgroundMode,value->{config.backgroundMode=value;updatePreview();});

        LinearLayout details=Ui.card(panels[3]);Ui.heading(details,"settings",getString(R.string.ui_widget_shortcuts_225),null);
        Ui.toggle(details,getString(R.string.ui_show_widget_name_226),null,config.heading,value->{config.heading=value;updatePreview();});
        Ui.toggle(details,getString(R.string.ui_edit_button_227),getString(R.string.ui_you_can_also_edit_from_228)+AppBrand.NAME+".",config.editButton,value->{config.editButton=value;updatePreview();});
        LinearLayout notifications=Ui.card(panels[3]);Ui.heading(notifications,"bell",getString(R.string.ui_notifications_229),null);Ui.toggle(notifications,getString(R.string.ui_show_badges_230),getString(R.string.ui_only_on_apps_in_this_widget_231),config.badges,value->{config.badges=value;updatePreview();});
        Ui.add(notifications,Ui.text(this,getString(R.string.ui_manage_notification_access_in_the_app_s_op_232),13,Ui.MUTED,false),6);
        Ui.add(notifications,Ui.button(this,getString(R.string.ui_notification_badge_options_233),false,v->startActivity(new Intent(this,OptionsActivity.class))),12);
        Ui.add(notifications,Ui.text(this,getString(R.string.ui_active_notifications_are_counted_counts_ma_234),12,Ui.MUTED,false),10);
        for(int panel=0;panel<panels.length;panel++)for(int section=0;section<panels[panel].getChildCount();section++){
            View card=panels[panel].getChildAt(section);
            if(card instanceof LinearLayout)Ui.collapsible((LinearLayout)card,"panel_"+panel+"_section_"+section);
        }
        LinearLayout footer=Ui.column(this);footer.setPadding(Ui.dp(this,20),Ui.dp(this,10),Ui.dp(this,20),Ui.dp(this,12));footer.setBackgroundColor(Ui.SURFACE);
        saveButton=Ui.button(this,widgetId==AppWidgetManager.INVALID_APPWIDGET_ID?getString(R.string.ui_add_to_home_screen_237):getString(R.string.ui_save_changes_238),true,v->save());LinearLayout actions=Ui.row(this);footer.addView(actions);actions.addView(saveButton,new LinearLayout.LayoutParams(0,-2,1));
        if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID){
            Button add=Ui.button(this,getString(R.string.ui_add_to_home_screen_237),false,v->WidgetPin.request(this,config,false));
            add.setId(R.id.add_to_desktop);LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(0,-2,1);actionParams.setMarginStart(Ui.dp(this,8));actions.addView(add,actionParams);
        }
        root.addView(footer);
        root.setFocusableInTouchMode(true);root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);root.requestFocus();selectPanel(selectedPanel);renderSelectionSummary();updateLayoutSummary();updatePreview();
    }
    private void addAlignment(LinearLayout parent) {
        Ui.add(parent,Ui.text(this,getString(R.string.widget_alignment),15,Ui.INK,true),12);
        GridLayout compass=new GridLayout(this);compass.setColumnCount(3);
        String[] arrows={"↖","↑","↗","←","●","→","↙","↓","↘"};
        int[] names={R.string.align_top_left,R.string.align_top,R.string.align_top_right,R.string.align_left,R.string.align_center,R.string.align_right,R.string.align_bottom_left,R.string.align_bottom,R.string.align_bottom_right};
        Button[] buttons=new Button[9];
        Runnable refresh=()->{for(int k=0;k<9;k++){boolean selected=config.alignment==k;buttons[k].setSelected(selected);buttons[k].setTextColor(selected?(Ui.DARK?0xff10203d:Color.WHITE):Ui.BLUE);buttons[k].setBackground(Ui.ripple(this,selected?Ui.BLUE:Ui.SOFT,12));}};
        for(int i=0;i<9;i++){
            final int position=i;Button button=Ui.button(this,arrows[i],false,v->{config.alignment=position;refresh.run();updatePreview();});
            buttons[i]=button;button.setContentDescription(getString(names[i]));button.setTextSize(23);button.setPadding(0,0,0,0);button.setMinWidth(0);button.setMinimumWidth(0);
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=Ui.dp(this,52);gp.columnSpec=GridLayout.spec(i%3,1f);gp.rowSpec=GridLayout.spec(i/3);gp.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));compass.addView(button,gp);
        }
        refresh.run();Ui.add(parent,compass,8);
    }
    private void addShadowDirection(LinearLayout parent) {
        Ui.add(parent,Ui.text(this,getString(R.string.ui_shadow_direction_239),15,Ui.INK,true),14);
        GridLayout compass=new GridLayout(this);compass.setColumnCount(3);
        int[] angles={225,270,315,180,-1,0,135,90,45};String[] arrows={"↖","↑","↗","←","●","→","↙","↓","↘"};
        for(int i=0;i<9;i++){
            final int angle=angles[i];View cell;
            if(angle<0){TextView center=Ui.text(this,getString(R.string.ui_icon_240),12,Ui.MUTED,true);center.setGravity(Gravity.CENTER);cell=center;}
            else{Button button=Ui.button(this,arrows[i],false,v->((SeekBar)findViewById(R.id.shadow_angle)).setProgress(angle));button.setContentDescription(getString(R.string.ui_shadow_241)+ShadowDirection.label(this,angle));button.setTextSize(23);button.setPadding(0,0,0,0);button.setMinHeight(Ui.dp(this,48));button.setMinWidth(0);button.setMinimumWidth(0);directionButtons[angle/45]=button;cell=button;}
            GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=Ui.dp(this,52);gp.columnSpec=GridLayout.spec(i%3,1f);gp.rowSpec=GridLayout.spec(i/3);gp.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));compass.addView(cell,gp);
        }
        Ui.add(parent,compass,8);
        slider(parent,getString(R.string.ui_angle_242),0,359,config.shadowAngle,value->{config.shadowAngle=value;updateDirectionButtons();updatePreview();return value+"°";},R.id.shadow_angle);
        Ui.add(parent,Ui.text(this,getString(R.string.ui_0_right_90_down_180_left_270_up_243),12,Ui.MUTED,false),0);
    }
    private void updateDirectionButtons(){for(int i=0;i<directionButtons.length;i++){Button button=directionButtons[i];if(button==null)continue;boolean selected=config.shadowAngle==i*45;button.setSelected(selected);button.setTextColor(selected?(Ui.DARK?0xff10203d:Color.WHITE):Ui.BLUE);button.setBackground(Ui.ripple(this,selected?Ui.BLUE:Ui.SOFT,12));}}
    private void reloadCatalog(){
        final int request=++catalogRequest;
        new Thread(()->{List<AppCatalog.Entry> loaded=AppCatalog.load(getApplicationContext());main.post(()->{if(!closed&&request==catalogRequest){entries=loaded;chooseButton.setText(getString(R.string.ui_choose_apps_and_contacts_244));renderSelectionSummary();}});},"app-catalog").start();
    }
    private WidgetConfig premiumBaseline(){return widgetId!=-1&&ConfigStore.has(this,widgetId)?ConfigStore.get(this,widgetId):null;}
    private int shortcutLimit(){return PremiumAccess.limit(this,premiumBaseline());}
    private void shortcutOffer(){if(shortcutLimit()>=LayoutPolicy.MAX_APPS)Toast.makeText(this,R.string.ui_this_widget_is_full_remove_a_shortcut_and__250,Toast.LENGTH_LONG).show();else PremiumOffer.show(this,PremiumPolicy.Feature.SHORTCUTS);}
    private void previewOffer(PremiumPolicy.Feature feature){if(!PremiumAccess.active(this))PremiumOffer.show(this,feature);}
    private void pickContact(){
        if(config.apps.size()>=shortcutLimit()){shortcutOffer();return;}
        if(config.apps.size()>=LayoutPolicy.MAX_APPS){Toast.makeText(this,getString(R.string.ui_this_widget_supports_up_to_48_shortcuts_re_246),Toast.LENGTH_LONG).show();return;}
        try{startActivityForResult(new Intent(Intent.ACTION_PICK).setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE),PICK_CONTACT);}
        catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,getString(R.string.ui_no_contact_picker_is_available_on_this_dev_247),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==704&&result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();new Thread(()->{try{String id=BackgroundStore.add(getApplicationContext(),uri);main.post(()->{if(!closed){config.backgroundImage=id;config.opacity=100;opacitySlider.setProgress(100);updatePreview();previewOffer(PremiumPolicy.Feature.BACKGROUND);}});}catch(Exception e){main.post(()->{if(!closed)Toast.makeText(this,R.string.background_image_error,Toast.LENGTH_LONG).show();});}},"background-import").start();}
        if(request==PICK_CONTACT&&result==RESULT_OK&&data!=null&&data.getData()!=null)importContact(data.getData());
    }
    private void importContact(Uri uri){importContact(uri,true);}
    private void importContact(Uri uri,boolean mayRequestPermission){
        allowPhotoPermission=mayRequestPermission;
        pendingContactUri=uri.toString();contactButton.setEnabled(false);contactButton.setText(getString(R.string.ui_adding_contact_248));chooseButton.setEnabled(false);renderSelectionSummary();
        new Thread(()->{
            ContactStore.Entry entry=null;
            try{entry=ContactStore.importSelected(getApplicationContext(),uri);}catch(java.io.IOException|RuntimeException ignored){/* Show a recoverable message without exposing contact data. */}
            final ContactStore.Entry imported=entry;
            if(imported!=null)WidgetRenderer.refreshAsync(getApplicationContext());
            main.post(()->{
                if(closed)return;pendingContactUri=null;contactButton.setEnabled(true);contactButton.setText(getString(R.string.ui_add_contact_163));chooseButton.setEnabled(true);renderSelectionSummary();
                if(imported==null){Toast.makeText(this,getString(R.string.ui_could_not_read_the_contact_select_it_again_249),Toast.LENGTH_LONG).show();return;}
                if(!config.apps.contains(imported.id)){
                    if(config.apps.size()>=shortcutLimit()){shortcutOffer();return;}
                    if(config.apps.size()>=LayoutPolicy.MAX_APPS){Toast.makeText(this,getString(R.string.ui_this_widget_is_full_remove_a_shortcut_and__250),Toast.LENGTH_LONG).show();return;}
                    config.apps.add(imported.id);
                }
                preview.refreshImages();renderSelectionSummary();updatePreview();reloadCatalog();
                if(imported.photoAccessDenied&&mayRequestPermission&&checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                    pendingPhotoAction=uri.toString();renderSelectionSummary();requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},PHOTO_PERMISSION);return;
                }
                Toast.makeText(this,imported.hasPhoto?getString(R.string.ui_contact_added_with_photo_251):getString(R.string.ui_contact_added_photo_unavailable_showing_in_252),Toast.LENGTH_SHORT).show();
            });
        },"selected-contact").start();
    }
    private void requestPhotoRefresh(){
        if(ContactStore.all(this).isEmpty())return;
        if(checkSelfPermission(android.Manifest.permission.READ_CONTACTS)==android.content.pm.PackageManager.PERMISSION_GRANTED){refreshContactPhotos();return;}
        pendingPhotoAction="refresh";renderSelectionSummary();requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS},PHOTO_PERMISSION);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);if(request!=PHOTO_PERMISSION)return;
        String action=pendingPhotoAction;pendingPhotoAction=null;renderSelectionSummary();
        if(action==null)return;
        if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED){
            // Refresh existing shortcuts too, including contacts imported before this update.
            if(!"refresh".equals(action))importContact(Uri.parse(action),false);else refreshContactPhotos();
        }else Toast.makeText(this,getString(R.string.ui_you_can_keep_using_contacts_with_initials__255),Toast.LENGTH_LONG).show();
    }
    private void refreshContactPhotos(){
        pendingPhotoAction="refreshing";renderSelectionSummary();
        new Thread(()->{int photos=ContactStore.refreshSelected(getApplicationContext());WidgetRenderer.refreshAsync(getApplicationContext());main.post(()->{
            if(closed)return;pendingPhotoAction=null;preview.refreshImages();updatePreview();reloadCatalog();renderSelectionSummary();
            Toast.makeText(this,photos>0?getString(R.string.ui_contact_photos_refreshed_256):getString(R.string.ui_no_available_photos_found_your_shortcuts_a_257),Toast.LENGTH_LONG).show();
        });},"contact-photos").start();
    }
    private void selectPanel(int index) {
        selectedPanel=Math.max(0,Math.min(3,index));
        for(int i=0;i<4;i++){if(panels[i]!=null)panels[i].setVisibility(i==selectedPanel?View.VISIBLE:View.GONE);Button tab=tabButtons[i];if(tab!=null){boolean active=i==selectedPanel;tab.setSelected(active);tab.setTextColor(active?(Ui.DARK?0xff10203d:Color.WHITE):Ui.MUTED);tab.setBackground(Ui.ripple(this,active?Ui.BLUE:Color.TRANSPARENT,12));for(android.graphics.drawable.Drawable d:tab.getCompoundDrawables())if(d!=null)d.setColorFilter(active?(Ui.DARK?0xff10203d:Color.WHITE):Ui.MUTED,android.graphics.PorterDuff.Mode.SRC_IN);}}
        if(settingsScroll!=null)settingsScroll.scrollTo(0,0);
    }
    private void updateShadowColorLabel() { if(shadowColorButton!=null){shadowColorButton.setText(getString(R.string.ui_shadow_color_259)+String.format(Locale.ROOT,"#%06X",config.shadowColor&0xffffff));android.graphics.drawable.GradientDrawable swatch=Ui.rounded(config.shadowColor,6,this);swatch.setStroke(Ui.dp(this,1),Ui.LINE);swatch.setBounds(0,0,Ui.dp(this,24),Ui.dp(this,24));shadowColorButton.setCompoundDrawables(swatch,null,null,null);shadowColorButton.setCompoundDrawablePadding(Ui.dp(this,10));} }
    private void leaveEditor() {
        if(config.toJson().equals(initialConfig)){finish();return;}
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_leave_without_saving_261)).setMessage(getString(R.string.ui_changes_to_this_widget_have_not_been_appli_262)).setNegativeButton(getString(R.string.ui_keep_editing_263),null).setPositiveButton(getString(R.string.ui_discard_changes_264),(d,w)->finish()).show();
    }
    private void updateStickyPreview() {
        if(adjustingPreview||root==null||preview==null||root.getWidth()==0||root.getHeight()==0)return;
        adjustingPreview=true;
        try {
            int desired=preview.preferredHeight(root.getWidth()-Ui.dp(this,40));
            if(preview.getLayoutParams().height!=desired) { preview.getLayoutParams().height=desired;preview.requestLayout(); }
            int usable=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();
            int measuredChrome=previewPanel.getMeasuredHeight()-preview.getMeasuredHeight();
            int panelHeight=desired+Math.max(measuredChrome,Ui.dp(this,62)+(undoButton!=null&&undoButton.getVisibility()==View.VISIBLE?Ui.dp(this,60):0));
            boolean pinned=LayoutPolicy.shouldPinPreview(panelHeight,usable);
            LinearLayout target=pinned?previewDock:inlinePreviewSlot;
            if(previewPanel.getParent()!=target) { ((ViewGroup)previewPanel.getParent()).removeView(previewPanel);target.addView(previewPanel); }
            previewDock.setVisibility(pinned?View.VISIBLE:View.GONE);inlinePreviewSlot.setVisibility(pinned?View.GONE:View.VISIBLE);
        } finally { adjustingPreview=false; }
    }
    private void updateLayoutSummary(){if(layoutSummary!=null&&preview!=null)layoutSummary.setText(preview.resolvedColumns()+(preview.resolvedColumns()==1?getString(R.string.ui_column_265):getString(R.string.ui_columns_266))+getString(R.string.ui_icon_width_267)+preview.resolvedIconSize());}
    private void updateTitleColorLabel(){if(titleColorButton!=null){titleColorButton.setText(getString(R.string.ui_text_color_268)+String.format(Locale.ROOT,"#%06X",config.labelColor&0xffffff));android.graphics.drawable.GradientDrawable swatch=Ui.rounded(config.labelColor,6,this);swatch.setStroke(Ui.dp(this,1),Ui.LINE);swatch.setBounds(0,0,Ui.dp(this,24),Ui.dp(this,24));titleColorButton.setCompoundDrawables(swatch,null,null,null);titleColorButton.setCompoundDrawablePadding(Ui.dp(this,10));}}
    private interface Choice { void select(int value); }
    private void addChoice(LinearLayout card,String label,String[] values,int selected,Choice choice) {
        Ui.add(card,Ui.text(this,label,15,Ui.INK,true),14);Spinner spinner=new Spinner(this);spinner.setMinimumHeight(Ui.dp(this,50));spinner.setContentDescription(label);
        spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values));spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){choice.select(i);}public void onNothingSelected(AdapterView<?> p){}});Ui.add(card,spinner,4);
    }
    private EditText input(String hint, String value) {
        EditText input = new EditText(this); input.setTextSize(16); input.setTextColor(Ui.INK); input.setHintTextColor(Ui.MUTED); input.setHint(hint); input.setText(value);
        input.setSingleLine(true); input.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12)); input.setMinHeight(Ui.dp(this, 54)); input.setBackground(Ui.rounded(Ui.BG, 12, this)); return input;
    }
    private interface TextChange { void change(String text); }
    private void watch(EditText e, TextChange callback) { e.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) { callback.change(s.toString()); }
        public void afterTextChanged(Editable e) {}
    }); }
    private interface SliderChange { String change(int value); }
    private void slider(LinearLayout parent, String label, int min, int max, int initial, SliderChange change) { slider(parent,label,min,max,initial,change,View.NO_ID); }
    private void slider(LinearLayout parent, String label, int min, int max, int initial, SliderChange change, int id) {
        LinearLayout row = Ui.row(this); row.addView(Ui.text(this, label, 15, Ui.INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = Ui.text(this, change.change(initial), 16, Ui.BLUE, true); row.addView(value); Ui.add(parent, row, 10);
        SeekBar bar = new SeekBar(this); bar.setId(id); bar.setMinimumHeight(Ui.dp(this,48)); bar.setMax(max - min); bar.setProgress(initial - min); bar.setContentDescription(label);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean user) { value.setText(change.change(v + min)); }
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        }); Ui.add(parent, bar, 0);
    }
    private void setColor(int color) { config.background = color; if (config.opacity == 0) { config.opacity = 85; opacitySlider.setProgress(85); } updatePreview(); }
    private void customColor(int target) {
        int current=target==3?config.labelColor:target==2?config.shadowColor:target==1?config.iconAccent:config.background;
        EditText hex=input(getString(R.string.ui_rrggbb_269),String.format(Locale.ROOT,"#%06X",current&0xffffff));hex.setSelectAllOnFocus(true);hex.setContentDescription(getString(R.string.ui_hexadecimal_color_270));
        LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),0);
        GridLayout palette=new GridLayout(this);palette.setColumnCount(3);int[] colors={Color.BLACK,Color.WHITE,0xff3159de,0xff9b6dff,0xff15b8a6,0xffe64980};
        for(int i=0;i<colors.length;i++){final int color=colors[i];Button swatch=Ui.button(this,"",false,v->hex.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff)));swatch.setContentDescription(String.format(Locale.ROOT,getString(R.string.ui_choose_color_06x_271),color&0xffffff));swatch.setMinWidth(0);swatch.setMinimumWidth(0);swatch.setPadding(0,0,0,0);android.graphics.drawable.GradientDrawable bg=Ui.rounded(color,12,this);bg.setStroke(Ui.dp(this,1),Ui.LINE);swatch.setBackground(bg);GridLayout.LayoutParams sp=new GridLayout.LayoutParams();sp.width=0;sp.height=Ui.dp(this,48);sp.columnSpec=GridLayout.spec(i%3,1f);sp.rowSpec=GridLayout.spec(i/3);sp.setMargins(Ui.dp(this,3),0,Ui.dp(this,3),Ui.dp(this,10));palette.addView(swatch,sp);}content.addView(palette);content.addView(hex);
        Ui.add(content,Ui.text(this,getString(R.string.ui_enter_a_color_or_select_a_sample_272),13,Ui.MUTED,false),8);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(target==3?getString(R.string.ui_title_color_273):target==2?getString(R.string.ui_shadow_color_274):target==1?getString(R.string.ui_style_color_275):getString(R.string.ui_background_color_276)).setView(content).setNegativeButton(getString(R.string.ui_cancel_110),null).setPositiveButton(getString(R.string.ui_apply_277),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String raw=hex.getText().toString().trim();if(!raw.startsWith("#"))raw="#"+raw;if(!raw.matches("#[a-fA-F0-9]{6}")){hex.setError(getString(R.string.ui_enter_six_characters_for_example_3159de_279));return;}int color=Color.parseColor(raw);if(target==3){config.labelColor=color;updateTitleColorLabel();updatePreview();}else if(target==2){config.shadowColor=color;updateShadowColorLabel();updatePreview();}else if(target==1){config.iconAccent=color;updatePreview();}else setColor(color);dialog.dismiss();}));dialog.show();
    }
    private void renderSelectionSummary() {
        boolean busy=pendingContactUri!=null||pendingPhotoAction!=null;
        if(selectedCount!=null){int contacts=ContactStore.count(config.apps);int apps=config.apps.size()-contacts;selectedCount.setText(apps+(apps==1?getString(R.string.ui_app_280):getString(R.string.ui_apps_281))+contacts+(contacts==1?getString(R.string.ui_contact_maximum_282):getString(R.string.ui_contacts_maximum_283))+shortcutLimit());}
        if(saveButton!=null){boolean ready=!config.apps.isEmpty()&&!busy;saveButton.setEnabled(ready);saveButton.setAlpha(ready?1f:.5f);}
        if(contactButton!=null){contactButton.setEnabled(!busy);contactButton.setText(pendingContactUri!=null?getString(R.string.ui_adding_contact_248):getString(R.string.ui_add_contact_163));}
        if(chooseButton!=null)chooseButton.setEnabled(!busy&&!entries.isEmpty());
        if(refreshPhotosButton!=null){refreshPhotosButton.setVisibility(ContactStore.all(this).isEmpty()?View.GONE:View.VISIBLE);refreshPhotosButton.setEnabled(!busy);refreshPhotosButton.setText("refreshing".equals(pendingPhotoAction)?getString(R.string.ui_refreshing_photos_284):getString(R.string.ui_refresh_photos_164));}
    }
    private static String normalized(String s) { return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT); }
    private void openPicker() {
        Dialog dialog = new Dialog(this); LinearLayout content = Ui.column(this); content.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16)); content.setBackgroundColor(Ui.BG);
        content.addView(Ui.text(this, getString(R.string.ui_apps_and_contacts_160), 23, Ui.INK, true));
        Ui.add(content, Ui.text(this, getString(R.string.ui_personal_profile_apps_and_added_contacts_m_286), 13, Ui.MUTED, false), 4);
        EditText search = input(getString(R.string.ui_search_by_name_287), ""); search.setContentDescription(getString(R.string.ui_search_apps_288)); Ui.add(content, search, 12);
        final ArrayList<String> selection = new ArrayList<>(config.apps);
        final ArrayList<AppCatalog.Entry> filtered = new ArrayList<>(entries);
        TextView count = Ui.text(this, selection.size() + getString(R.string.ui_selected_289), 14, Ui.MUTED, true); Ui.add(content, count, 12);
        GridView list = new GridView(this); list.setNumColumns(GridView.AUTO_FIT);list.setColumnWidth(Ui.dp(this,Math.round(150*Math.max(1f,getResources().getConfiguration().fontScale))));list.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);list.setHorizontalSpacing(Ui.dp(this,8));list.setVerticalSpacing(Ui.dp(this,4));list.setVerticalScrollBarEnabled(false); content.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() { return filtered.size(); }
            public Object getItem(int p) { return filtered.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int position, View recycled, android.view.ViewGroup parent) {
                LinearLayout row; PickHolder holder;
                if (recycled == null) {
                    row = Ui.column(ConfigureActivity.this); LinearLayout iconRow=Ui.row(ConfigureActivity.this);row.addView(iconRow);row.setPadding(Ui.dp(ConfigureActivity.this, 4), Ui.dp(ConfigureActivity.this, 10), Ui.dp(ConfigureActivity.this, 4), Ui.dp(ConfigureActivity.this, 10)); row.setMinimumHeight(Ui.dp(ConfigureActivity.this, 72));
                    holder = new PickHolder(); holder.icon = new ImageView(ConfigureActivity.this); holder.icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); iconRow.addView(holder.icon, new LinearLayout.LayoutParams(Ui.dp(ConfigureActivity.this, 42), Ui.dp(ConfigureActivity.this, 42)));
                    LinearLayout copy = Ui.column(ConfigureActivity.this); copy.setPadding(Ui.dp(ConfigureActivity.this, 12), 0, Ui.dp(ConfigureActivity.this, 4), 0);
                    holder.label = Ui.text(ConfigureActivity.this, "", 14, Ui.INK, true); holder.label.setMaxLines(2); holder.label.setEllipsize(TextUtils.TruncateAt.END); copy.addView(holder.label);
                    holder.pkg = Ui.text(ConfigureActivity.this, "", 11, Ui.MUTED, false); holder.pkg.setSingleLine(true); holder.pkg.setEllipsize(TextUtils.TruncateAt.MIDDLE); copy.addView(holder.pkg); row.addView(copy, new LinearLayout.LayoutParams(-1, -2));
                    holder.check = new CheckBox(ConfigureActivity.this); holder.check.setClickable(false); holder.check.setFocusable(false); holder.check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); iconRow.addView(holder.check); row.setTag(holder);
                } else { row = (LinearLayout) recycled; holder = (PickHolder) row.getTag(); }
                AppCatalog.Entry e = filtered.get(position); holder.icon.setImageDrawable(AppCatalog.icon(ConfigureActivity.this, e.component)); holder.label.setText(e.label); holder.pkg.setText(e.packageName); holder.check.setChecked(selection.contains(e.component)); row.setContentDescription(e.label + (selection.contains(e.component) ? getString(R.string.ui_selected_290) : getString(R.string.ui_not_selected_291))); return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, position, id) -> {
            String flat = filtered.get(position).component;
            if (selection.contains(flat)) selection.remove(flat); else if (selection.size() < shortcutLimit()) selection.add(flat); else shortcutOffer();
            count.setText(selection.size() + getString(R.string.ui_selected_289)); adapter.notifyDataSetChanged();
        });
        watch(search, text -> { String q = normalized(text.trim()); filtered.clear(); for (AppCatalog.Entry e : entries) if (normalized(e.label).contains(q) || normalized(e.packageName).contains(q)) filtered.add(e); adapter.notifyDataSetChanged(); });
        LinearLayout actions = Ui.row(this); Button cancel = Ui.button(this, getString(R.string.ui_cancel_110), false, v -> dialog.dismiss()); Button done = Ui.button(this, getString(R.string.ui_apply_selection_293), true, v -> { config.apps.clear(); config.apps.addAll(selection); renderSelectionSummary(); updatePreview(); dialog.dismiss(); });
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1); half.setMarginEnd(Ui.dp(this, 8)); actions.addView(cancel, half); actions.addView(done, new LinearLayout.LayoutParams(0, -2, 1)); Ui.add(content, actions, 8);
        dialog.setContentView(content); dialog.setOnShowListener(d -> dialog.getWindow().setLayout(-1, Math.round(getResources().getDisplayMetrics().heightPixels * .88f))); dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); dialog.show();
    }
    private static final class PickHolder { ImageView icon; TextView label, pkg; CheckBox check; }
    private void save() {
        PremiumPolicy.Feature needed=PremiumAccess.requirement(this,widgetId,config);if(needed!=PremiumPolicy.Feature.NONE){PremiumOffer.show(this,needed);return;}
        if(widgetId!=AppWidgetManager.INVALID_APPWIDGET_ID&&ConfigStore.isDeleted(this,widgetId)){Toast.makeText(this,R.string.widget_deleted,Toast.LENGTH_LONG).show();finish();return;}
        if (config.apps.isEmpty()) { Toast.makeText(this, getString(R.string.ui_choose_at_least_one_app_or_contact_294), Toast.LENGTH_SHORT).show(); return; }
        config.name = config.name.trim(); if (config.name.isEmpty()) config.name = getString(R.string.default_widget_name);
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(nameInput.getWindowToken(), 0);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (!ConfigStore.put(this, widgetId, config)) { Toast.makeText(this, getString(R.string.ui_could_not_save_changes_please_try_again_295), Toast.LENGTH_LONG).show(); return; }
            WidgetRenderer.update(this, widgetId); NotificationCountService.rescan();
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)); finish(); return;
        }
        WidgetPin.request(this,config,true);
    }
}
