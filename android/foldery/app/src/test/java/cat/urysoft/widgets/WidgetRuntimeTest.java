package cat.urysoft.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 34})
@LooperMode(LooperMode.Mode.PAUSED)
public final class WidgetRuntimeTest {
    private Context context;
    @Before public void setup() { context = RuntimeEnvironment.getApplication(); }

    @Test public void settingsRoundTripAndWidgetsRemainIndependent() {
        WidgetConfig first = new WidgetConfig(); first.name = "Trabajo"; first.iconDp = 80; first.opacity = 67;
        first.background = Color.BLUE; first.labels = false; first.badges = false;
        first.apps.add(new ComponentName(context, MainActivity.class).flattenToString());
        WidgetConfig second = new WidgetConfig(); second.name = "Personal"; second.iconDp = 28;
        ConfigStore.put(context, 101, first); ConfigStore.put(context, 202, second);
        assertEquals("Trabajo", ConfigStore.get(context, 101).name);
        assertEquals(80, ConfigStore.get(context, 101).iconDp);
        assertEquals(67, ConfigStore.get(context, 101).opacity);
        assertFalse(ConfigStore.get(context, 101).labels);
        assertFalse(ConfigStore.get(context, 101).badges);
        assertEquals("Personal", ConfigStore.get(context, 202).name);
        ConfigStore.remove(context, 101);
        assertTrue(ConfigStore.has(context, 202));
        assertEquals(0, new WidgetConfig().opacity);
    }
    @Test public void allIconSizesInflateAndCanHideLabels() {
        WidgetConfig config = new WidgetConfig(); config.apps.add(new ComponentName(context, MainActivity.class).flattenToString());
        for (int size = 28; size <= 160; size += 2) {
            config.iconDp = size; config.labels = false;
            View cell = WidgetRenderer.cell(context, config, 0).apply(context, new FrameLayout(context));
            assertEquals(0,CellRenderer.measure(context,config).titleHeight);
            assertNotNull(((ImageView) cell.findViewById(R.id.cell_icon)).getDrawable());
            assertNotNull(cell.getContentDescription());
        }
    }
    @Test public void widgetInflatesWithTransparentBackgroundAndEditing() {
        WidgetConfig config = new WidgetConfig();
        View root = WidgetRenderer.build(context, 100, config, 300, 160).apply(context, new AppWidgetHostView(context));
        assertEquals(View.GONE, root.findViewById(R.id.widget_background).getVisibility());
        assertEquals(View.VISIBLE, root.findViewById(R.id.widget_edit).getVisibility());
        assertTrue(root.findViewById(R.id.widget_grid) instanceof GridView);
    }
    @Test public void configuredBackgroundAndOptionalControlsApply() {
        WidgetConfig config = new WidgetConfig(); config.opacity = 80; config.heading = true; config.name = "Diario"; config.editButton = false;
        View root = WidgetRenderer.build(context, 100, config, 300, 160).apply(context, new AppWidgetHostView(context));
        assertEquals(View.VISIBLE, root.findViewById(R.id.widget_background).getVisibility());
        assertEquals(View.GONE, root.findViewById(R.id.widget_edit).getVisibility());
        assertEquals("Diario", ((TextView) root.findViewById(R.id.widget_title)).getText().toString());
    }
    @Test public void screensOpenAndEditorRestoresItsDraft() {
        try (ActivityController<MainActivity> main = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNotNull(findText(main.get().getWindow().getDecorView(), "+  Crear widget"));
        }
        WidgetConfig config = new WidgetConfig(); config.name = "Un nombre bastante largo para conservar"; config.iconDp = 70;
        Bundle state = new Bundle(); state.putString("config", config.toJson());
        try (ActivityController<ConfigureActivity> editor = Robolectric.buildActivity(ConfigureActivity.class).create(state).start().resume().visible()) {
            assertNotNull(findText(editor.get().getWindow().getDecorView(), config.name));
            Bundle saved = new Bundle(); editor.saveInstanceState(saved);
            assertEquals(70, WidgetConfig.fromJson(saved.getString("config")).iconDp);
            assertNotNull(findText(editor.get().getWindow().getDecorView(), "Añadir al escritorio"));
        }
    }
    @Test public void draftsForSeparatePinRequestsDoNotOverwriteEachOther() {
        WidgetConfig a = new WidgetConfig(); a.name = "Uno";
        WidgetConfig b = new WidgetConfig(); b.name = "Dos";
        String idA = ConfigStore.draft(context, a), idB = ConfigStore.draft(context, b);
        assertNotEquals(idA, idB); assertEquals("Uno", ConfigStore.takeDraft(context, idA).name);
        assertEquals("Dos", ConfigStore.takeDraft(context, idB).name); assertNull(ConfigStore.takeDraft(context, idA));
    }
    @Test public void widgetTapLaunchesOnlyAnApplicationInItsOwnConfiguration() {
        String component = new ComponentName(context, MainActivity.class).flattenToString();
        WidgetConfig config = new WidgetConfig(); config.apps.add(component); ConfigStore.put(context, 101, config);
        Intent allowed = new Intent(context, LaunchActivity.class).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 101).putExtra("component", component);
        try (ActivityController<LaunchActivity> controller = Robolectric.buildActivity(LaunchActivity.class, allowed).create()) {
            Intent launched = Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertNotNull(launched); assertEquals(ComponentName.unflattenFromString(component), launched.getComponent());
        }
        Intent blocked = new Intent(context, LaunchActivity.class).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, 101).putExtra("component", "other.app/other.app.Main");
        try (ActivityController<LaunchActivity> controller = Robolectric.buildActivity(LaunchActivity.class, blocked).create()) {
            assertNull(Shadows.shadowOf(controller.get()).getNextStartedActivity());
        }
    }
    @Test public void fallbackTemplateIsUsedOnlyOnceAndNewWidgetsDefaultToTransparent() {
        WidgetConfig config = new WidgetConfig(); config.opacity = 85; config.name = "Preparado";
        ConfigStore.putTemplate(context, config);
        assertEquals(85, ConfigStore.takeTemplate(context).opacity);
        assertEquals(0, ConfigStore.takeTemplate(context).opacity);
        assertEquals(0, new WidgetConfig().opacity);
    }
    private static TextView findText(View view, String text) {
        if (view instanceof TextView && text.equals(((TextView)view).getText().toString())) return (TextView)view;
        if (view instanceof ViewGroup) for(int i=0; i<((ViewGroup)view).getChildCount(); i++) {
            TextView found = findText(((ViewGroup)view).getChildAt(i), text); if(found != null) return found;
        }
        return null;
    }
}
