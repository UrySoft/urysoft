package cat.urysoft.widgets;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.*;
import java.util.concurrent.*;

public final class WidgetRenderer {
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    public static void refreshAsync(Context context) {
        Context c = context.getApplicationContext();
        worker.execute(() -> { for (int id : ConfigStore.hostIds(c)) update(c, id); });
    }
    public static void update(Context c, int id) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        WidgetConfig config = ConfigStore.get(c, id);
        int[] size = bounds(c, id);
        manager.updateAppWidget(id, build(c, id, config, size[0], size[1]));
        if (Build.VERSION.SDK_INT < 31) manager.notifyAppWidgetViewDataChanged(id, R.id.widget_grid);
    }
    public static WidgetConfig resolve(Context c, WidgetConfig stored, int width, int height) {
        WidgetConfig value = WidgetConfig.fromJson(stored.toJson());
        if(width<112||height<100){stored=WidgetConfig.fromJson(stored.toJson());stored.paddingHorizontalDp=Math.min(stored.paddingHorizontalDp,Math.max(0,(width-28)/2));stored.paddingVerticalDp=Math.min(stored.paddingVerticalDp,4);value.paddingHorizontalDp=stored.paddingHorizontalDp;value.paddingVerticalDp=stored.paddingVerticalDp;value.editButton=false;value.heading=false;}

        value.columns=LayoutPolicy.responsiveColumns(width,stored.paddingHorizontalDp,stored.columnGapDp,stored.columns,stored.apps.size(),stored.labels && stored.labelPosition>=WidgetConfig.LEFT?stored.autoIconDp*2:stored.autoIconDp);
        value.columnGapDp=LayoutPolicy.effectiveGap(width,stored.paddingHorizontalDp,value.columns,stored.columnGapDp);
        int innerPx=Ui.dp(c,width)-Ui.dp(c,stored.paddingHorizontalDp)*2;
        int gapPx=(int)(value.columnGapDp*c.getResources().getDisplayMetrics().density);
        int columnPx=Math.max(1,(innerPx-gapPx*(value.columns-1))/value.columns);
        if(stored.columns==0 && !(stored.labels && stored.labelPosition>=WidgetConfig.LEFT))columnPx=LayoutPolicy.automaticColumnPixels(columnPx,Ui.dp(c,stored.autoIconDp));
        value.columns=Math.min(value.columns,Math.max(1,stored.apps.size()));
        int surplus=Math.max(0,innerPx-columnPx*value.columns-gapPx*(value.columns-1));
        // Put unused automatic width at the outer edges, never between icons.
        value.gridInsetLeftPx=AlignmentPolicy.horizontal(surplus,stored.alignment);
        value.gridInsetRightPx=surplus-value.gridInsetLeftPx;
        value.cellWidthDp=columnPx/c.getResources().getDisplayMetrics().density;
        value.autoSized=stored.columns==0;
        value.iconDp=LayoutPolicy.iconSize((int)Math.floor(value.autoSized?Math.min(value.cellWidthDp,stored.autoIconDp):value.cellWidthDp));
        value.surfaceWidthDp=width;
        value.shadowOutsetDp=stored.shadowEnabled&&stored.shadowOpacity>0?(int)Math.ceil(stored.shadowDistance+(100-stored.shadowSharpness)*.16f):0;
        int rows=(stored.apps.size()+value.columns-1)/value.columns;
        CellRenderer.Metrics metrics=CellRenderer.measure(c,value);
        int content=Math.round(rows*metrics.height*c.getResources().getDisplayMetrics().density)+Math.max(0,rows-1)*Ui.dp(c,value.rowGapDp);
        int heading=value.heading?CellRenderer.headingLayout(c,value,Math.max(1,width-value.paddingHorizontalDp*2)).getHeight()+Ui.dp(c,12):0;
        content+=Ui.dp(c,value.shadowOutsetDp)*2;
        int available=Ui.dp(c,Math.max(0,height-value.paddingVerticalDp*2-(value.editButton?40:0)))-heading;
        value.gridInsetTopPx=AlignmentPolicy.vertical(available-content,stored.alignment);
        return value;
    }
    public static int[] bounds(Context c, int id) {
        Bundle options = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
        boolean landscape = c.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        return new int[]{Math.max(40, options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)),
            Math.max(40, options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220))};
    }
    public static WidgetConfig resolveStored(Context c, int id) {
        int[] bounds = bounds(c, id);
        return resolve(c, ConfigStore.get(c, id), bounds[0], bounds[1]);
    }
    public static RemoteViews build(Context c, int id, WidgetConfig stored, int width, int height) {
        c=Language.context(c);
        if(ConfigStore.isDeleted(c,id)){
            RemoteViews deleted=new RemoteViews(c.getPackageName(),R.layout.widget_deleted);
            deleted.setTextViewText(R.id.deleted_widget_message,c.getString(R.string.widget_deleted_desktop));
            return deleted;
        }
        WidgetConfig config = resolve(c, stored, width, height);
        RemoteViews root = new RemoteViews(c.getPackageName(), R.layout.widget);
        root.setViewPadding(R.id.widget_content,0,Ui.dp(c,config.paddingVerticalDp),0,Ui.dp(c,config.paddingVerticalDp));
        root.setViewPadding(R.id.widget_title,Ui.dp(c,config.paddingHorizontalDp+6),Ui.dp(c,6),Ui.dp(c,config.paddingHorizontalDp+6),Ui.dp(c,6));
        root.setViewPadding(R.id.widget_edit,Ui.dp(c,config.paddingHorizontalDp+8),0,Ui.dp(c,config.paddingHorizontalDp+10),0);
        if (config.opacity > 0) {
            root.setImageViewBitmap(R.id.widget_background, IconRenderer.background(c, config, width, height));
            root.setViewVisibility(R.id.widget_background, View.VISIBLE);
        } else root.setViewVisibility(R.id.widget_background, View.GONE);
        root.setViewVisibility(R.id.widget_title, config.heading ? View.VISIBLE : View.GONE);
        root.setTextViewText(R.id.widget_title, config.name); root.setTextColor(R.id.widget_title, config.labelColor);
        root.setTextColor(R.id.widget_edit, config.labelColor); root.setTextColor(R.id.widget_empty, config.labelColor);
        root.setViewVisibility(R.id.widget_edit, config.editButton ? View.VISIBLE : View.GONE);
        // GridView sizing setters were not remotely callable on Android 8.
        // Static collection layouts support the same settings on every supported release.
        root.removeAllViews(R.id.widget_collection_container);
        root.addView(R.id.widget_collection_container, new RemoteViews(c.getPackageName(), R.layout.grid_columns_1_gap_0));
        root.setViewPadding(R.id.widget_grid,0,config.gridInsetTopPx,0,0);
        root.setEmptyView(R.id.widget_grid, R.id.widget_empty);
        Intent edit = new Intent(c, ConfigureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("urywidgets://edit/" + id));
        PendingIntent editPending = WidgetPendingIntents.activity(c, id, edit, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        root.setOnClickPendingIntent(R.id.widget_edit, editPending); root.setOnClickPendingIntent(R.id.widget_empty, editPending);
        Intent launch = new Intent(c, LaunchActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("urywidgets://launch/" + id));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        root.setPendingIntentTemplate(R.id.widget_grid, WidgetPendingIntents.activity(c, id, launch, flags));
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews.RemoteCollectionItems.Builder items = new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(false).setViewTypeCount(LayoutPolicy.CELL_TYPES);
            for (int i = 0; i < RowRenderer.count(config); i++) items.addItem(i, row(c, config, i));
            root.setRemoteAdapter(R.id.widget_grid, items.build());
        } else {
            Intent adapter = new Intent(c, WidgetCollectionService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            adapter.setData(Uri.parse("urywidgets://collection/" + id)); root.setRemoteAdapter(R.id.widget_grid, adapter);
        }
        return root;
    }
    public static RemoteViews row(Context c,WidgetConfig config,int index) {
        c=Language.context(c);RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.widget_surface_row);
        row.setImageViewBitmap(R.id.row_surface,RowRenderer.render(c,config,index));
        row.setViewPadding(R.id.row_actions,Ui.dp(c,config.paddingHorizontalDp)+config.gridInsetLeftPx,index==0?Ui.dp(c,config.shadowOutsetDp):0,Ui.dp(c,config.paddingHorizontalDp)+config.gridInsetRightPx,index==RowRenderer.count(config)-1?Ui.dp(c,config.shadowOutsetDp):Ui.dp(c,config.rowGapDp));

        int columns=Math.max(1,config.columns),gap=Ui.dp(c,config.columnGapDp),width=Math.round(config.cellWidthDp*c.getResources().getDisplayMetrics().density);
        int[] ids={R.id.row_hit_0,R.id.row_hit_1,R.id.row_hit_2,R.id.row_hit_3,R.id.row_hit_4,R.id.row_hit_5,R.id.row_hit_6,R.id.row_hit_7};
        for(int col=0;col<8;col++){
            int id=ids[col];if(col>=columns){row.setViewVisibility(id,View.GONE);continue;}
            int position=index*columns+col;
            row.setInt(id,"setWidth",width+(col==columns-1?0:gap));
            if(position<config.apps.size()){
                String component=config.apps.get(position),label=AppCatalog.label(c,component);int count=config.badges?BadgeState.get(c,component):0;
                row.setViewVisibility(id,View.VISIBLE);
                row.setContentDescription(id,count>0?c.getResources().getQuantityString(R.plurals.notification_count,count,label,count):label);
                row.setOnClickFillInIntent(id,new Intent().putExtra("component",component));
            }else row.setViewVisibility(id,View.INVISIBLE);
        }
        return row;
    }
    public static RemoteViews cell(Context c, WidgetConfig config, int position) {
        c=Language.context(c);
        RemoteViews row = new RemoteViews(c.getPackageName(),R.layout.widget_cell);
        String component = config.apps.get(position), label = AppCatalog.label(c, component);
        int count = config.badges ? BadgeState.get(c, component) : 0;
        row.setImageViewBitmap(R.id.cell_icon,CellRenderer.app(c,component,config,count));
        int halo=Ui.dp(c,CellRenderer.HALO);
        // The ImageView itself includes the transparent halo. Negative margins
        // keep icon positions fixed; negative padding clipped the shadow at its bounds.
        row.setViewPadding(R.id.cell_icon,0,0,0,0);
        boolean lastRow=position/Math.max(1,config.columns)==(config.apps.size()-1)/Math.max(1,config.columns);
        row.setViewPadding(R.id.cell_root,0,0,0,lastRow?0:Ui.dp(c,config.rowGapDp));
        row.setContentDescription(R.id.cell_root, count > 0 ? c.getResources().getQuantityString(R.plurals.notification_count, count, label, count) : label);
        row.setOnClickFillInIntent(R.id.cell_root, new Intent().putExtra("component", component));
        return row;
    }
}
