package cat.urysoft.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TitlePositionTest {
    @Test public void everyPositionSupportsMultipleColumnsAndShadows() {
        Context context=RuntimeEnvironment.getApplication();
        for(int columns : new int[]{1,2,4,8}) {
            for(int position=0;position<4;position++) {
                WidgetConfig v=new WidgetConfig();
                v.columns=columns;
                v.labelPosition=position;
                v.labelColor=Color.GREEN;
                v.labelFont=(columns+position)%5;
                v.shadowEnabled=true;
                v.shadowColor=Color.MAGENTA;
                v.shadowOpacity=100;
                v.shadowDistance=4;
                v.shadowSharpness=100;
                for(int i=0;i<columns;i++) v.apps.add("example.long.title"+i+"/.Main");
                WidgetConfig resolved=WidgetRenderer.resolve(context,v,400,240);
                assertEquals(columns,resolved.columns);
                assertEquals(v.labelFont,resolved.labelFont);
                CellRenderer.Metrics m=CellRenderer.measure(context,resolved);
                assertTrue(m.titleLeft+m.titleWidth<=m.width+.01f);
                if(position==WidgetConfig.LEFT) assertTrue(m.titleLeft+m.titleWidth<=m.iconLeft+.01f);
                if(position==WidgetConfig.RIGHT) assertTrue(m.titleLeft>=m.iconLeft+m.icon-.01f);
                if(position==WidgetConfig.ABOVE) assertTrue(m.titleTop+m.titleHeight<=m.iconTop);
                if(position==WidgetConfig.BELOW) assertTrue(m.titleTop>=m.iconTop+m.icon);
                Bitmap image=CellRenderer.app(context,v.apps.get(0),resolved,0);
                float scale=image.getWidth()/(m.width+2*CellRenderer.HALO);
                int x0=Math.round((CellRenderer.HALO+m.titleLeft)*scale);
                int x1=Math.round((CellRenderer.HALO+m.titleLeft+m.titleWidth)*scale);
                int y0=Math.round((CellRenderer.HALO+m.titleTop)*scale);
                int y1=Math.min(image.getHeight(),Math.round((CellRenderer.HALO+m.height)*scale));
                int text=0,shadow=0;
                for(int y=y0;y<y1;y++) for(int x=x0;x<x1;x++){
                    int pixel=image.getPixel(x,y);
                    if(Color.alpha(pixel)<25) continue;
                    if(Color.green(pixel)>200 && Color.red(pixel)<40) text++;
                    if(Color.red(pixel)>200 && Color.blue(pixel)>200 && Color.green(pixel)<40) shadow++;
                }
                assertTrue("Visible title",text>0);
                assertTrue("Visible shadow",shadow>0);
                image.recycle();
            }
        }
    }
}
