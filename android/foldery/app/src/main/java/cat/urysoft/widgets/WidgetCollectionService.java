package cat.urysoft.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.*;

/** Android 8–11 fallback. Android 12+ receives the collection directly. */
public final class WidgetCollectionService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        final int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        return new RemoteViewsFactory() {
            private WidgetConfig config = new WidgetConfig();
            public void onCreate() { config = WidgetRenderer.resolveStored(WidgetCollectionService.this, id); }
            public void onDataSetChanged() { config = WidgetRenderer.resolveStored(WidgetCollectionService.this, id); }
            public void onDestroy() {}
            public int getCount() { return ConfigStore.isDeleted(WidgetCollectionService.this,id)?0:RowRenderer.count(config); }
            public RemoteViews getViewAt(int p) { return !ConfigStore.isDeleted(WidgetCollectionService.this,id)&&p >= 0 && p < RowRenderer.count(config) ? WidgetRenderer.row(WidgetCollectionService.this, config, p) : null; }
            public RemoteViews getLoadingView() { return null; }
            public int getViewTypeCount() { return LayoutPolicy.CELL_TYPES; }
            public long getItemId(int p) { return p; }
            public boolean hasStableIds() { return false; }
        };
    }
}
