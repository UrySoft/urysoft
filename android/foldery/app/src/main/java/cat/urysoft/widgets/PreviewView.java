package cat.urysoft.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.StaticLayout;
import android.view.*;
import android.view.accessibility.*;
import java.util.*;

/** Live widget preview with independent opaque icon paint, scrolling and in-place editing. */
public final class PreviewView extends View {
    public interface Listener { void onAppsChanged(List<String> apps, String removed, int previousPosition); }
    private WidgetConfig config = new WidgetConfig(), resolved = new WidgetConfig(), beforeDrag;
    private final Map<String, Bitmap> icons = new HashMap<>();
    private final Map<Integer,Bitmap> rowImages=new HashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private final ArrayList<Cell> cells = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF wallpaperBounds = new RectF(), widget = new RectF(), body = new RectF(), viewport = new RectF(), trash = new RectF(), floating = new RectF();
    private Shader wallpaper;
    private Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int logicalWidth, logicalHeight = 220, columns = 1, downIndex = -1, dragIndex = -1, focused = -1, hovered = -1;
    private float scale = 1, cellWidth, cellHeight, scrollOffset, contentHeight, downX, downY, pointerX, pointerY;
    private CellRenderer.Metrics cellMetrics;
    private StaticLayout headingLayout;
    private boolean moving, pressedRemove;
    private final Runnable longPress = this::beginDrag;
    private final Runnable autoScroll = new Runnable() { public void run() {
        if (dragIndex < 0) return;
        float edge = Ui.dp(getContext(), 28), delta = pointerY < body.top+edge ? -Ui.dp(getContext(),6) : pointerY > body.bottom-edge ? Ui.dp(getContext(),6) : 0;
        if (delta != 0 && scrollByAmount(delta)) moveDragTarget(pointerX,pointerY);
        handler.postDelayed(this,24);
    }};
    private static final class Cell { final RectF bounds=new RectF(), image=new RectF(), remove=new RectF(); }

    public PreviewView(Context c) { super(c); setMinimumHeight(Ui.dp(c,160)); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); setFocusable(true); setContentDescription(getContext().getString(R.string.ui_widget_preview_with_apps_and_contacts_103)); iconPaint.setAlpha(255); }
    public int resolvedIconSize(){return Math.round(CellRenderer.measure(getContext(),resolved).icon);}
    public int resolvedColumns(){return columns;}
    public void setListener(Listener value) { listener=value; calculate(); invalidate(); }
    public void refreshImages() { icons.clear();rowImages.clear();names.clear();loadIcons();invalidate(); }
    public void setWidgetBounds(int widthDp,int heightDp) { logicalWidth=Math.max(0,widthDp);logicalHeight=Math.max(40,heightDp);calculate();loadIcons();invalidate(); }
    public void configure(WidgetConfig value) {
        if (dragIndex>=0) return;
        String previous = config.toJson(); config=WidgetConfig.fromJson(value.toJson());
        boolean changed=!previous.equals(config.toJson());
        if(changed) { icons.clear();rowImages.clear(); names.clear(); }
        calculate(); loadIcons(); invalidate();
    }
    private void loadIcons() {
        for(int row=0;row<RowRenderer.count(resolved);row++)if(!rowImages.containsKey(row))rowImages.put(row,RowRenderer.render(getContext(),resolved,row));
        for(String flat:config.apps) {
            if(!icons.containsKey(flat))icons.put(flat,CellRenderer.app(getContext(),flat,resolved,config.badges?BadgeState.get(getContext(),flat):0));
            if(!names.containsKey(flat))names.put(flat,AppCatalog.label(getContext(),flat));
        }
    }
    public int preferredHeight(int widthPixels) {
        float d=getResources().getDisplayMetrics().density;
        int width=Math.max(40,logicalWidth>0?logicalWidth:Math.round(widthPixels/d)-24);
        float s=Math.min(1f,Math.max(1,widthPixels-Ui.dp(getContext(),24))/(width*d));
        // Keep the actual widget proportions. Extra rows scroll in the preview,
        // just as they do on the desktop, instead of enlarging the whole editor.
        return Ui.dp(getContext(),Math.max(170,Math.min(420,Math.round(logicalHeight*s)+24)));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        wallpaper=new LinearGradient(0,0,w,h,new int[]{0xff111e3b,0xff324878,0xff627a9d},null,Shader.TileMode.CLAMP);
        int oldSize=resolved.iconDp;calculate();if(oldSize!=resolved.iconDp)icons.clear();rowImages.clear();loadIcons();
    }
    private void calculate() {
        float d=getResources().getDisplayMetrics().density;
        int width=Math.max(40,logicalWidth>0?logicalWidth:Math.round(Math.max(getWidth(),Ui.dp(getContext(),280))/d)-24);
        int previousSize=resolved.iconDp;float previousWidth=resolved.cellWidthDp;resolved=WidgetRenderer.resolve(getContext(),config,width,logicalHeight);columns=resolved.columns;
        if(previousSize!=resolved.iconDp||previousWidth!=resolved.cellWidthDp)icons.clear();rowImages.clear();
        float nativeWidth=Ui.dp(getContext(),width);
        scale=Math.min(1f,Math.max(1,getWidth()-Ui.dp(getContext(),24))/nativeWidth);
        float gridWidth=nativeWidth*scale,left=(getWidth()-gridWidth)/2,outer=Ui.dp(getContext(),12);
        widget.set(left,outer,left+gridWidth,Math.max(outer,getHeight()-outer));
        float px=Ui.dp(getContext(),resolved.paddingHorizontalDp),py=Ui.dp(getContext(),resolved.paddingVerticalDp);
        headingLayout=resolved.heading?CellRenderer.headingLayout(getContext(),config,(nativeWidth-px*2)/d):null;
        float headingHeight=headingLayout==null?0:headingLayout.getHeight()+Ui.dp(getContext(),6)*2;
        float top=widget.top+(py+headingHeight)*scale;
        viewport.set(widget);body.set(widget.left+(px+resolved.gridInsetLeftPx)*scale,top,widget.right-(px+resolved.gridInsetRightPx)*scale,Math.max(top+1,widget.bottom-(py+(resolved.editButton?Ui.dp(getContext(),40):0))*scale));
        cellMetrics=CellRenderer.measure(getContext(),resolved);
        cellWidth=Math.max(1,(cellMetrics.width*d+(int)(resolved.columnGapDp*d))*scale);cellHeight=(cellMetrics.height*d+Ui.dp(getContext(),resolved.rowGapDp))*scale;
        int rows=(config.apps.size()+columns-1)/columns;contentHeight=rows*cellHeight-(rows>0?Ui.dp(getContext(),resolved.rowGapDp)*scale:0)+Ui.dp(getContext(),resolved.shadowOutsetDp)*2*scale;
        scrollOffset=Math.max(0,Math.min(scrollOffset,Math.max(0,contentHeight-body.height())));
        while(cells.size()<config.apps.size())cells.add(new Cell());while(cells.size()>config.apps.size())cells.remove(cells.size()-1);
        updateCells();trash.set(widget.left+6*d,Math.max(widget.top,widget.bottom-44*d),widget.right-6*d,widget.bottom-3*d);
    }
    private void updateCells() {
        float d=getResources().getDisplayMetrics().density,unit=d*scale;
        for(int i=0;i<cells.size();i++) {
            Cell c=cells.get(i);float x=body.left+cellWidth*(i%columns),y=body.top+(resolved.gridInsetTopPx+Ui.dp(getContext(),resolved.shadowOutsetDp))*scale+(i/columns)*cellHeight-scrollOffset;
            float contentLeft=x,contentTop=y;
            boolean last=i/columns==(config.apps.size()-1)/columns;
            c.bounds.set(x,y,x+cellMetrics.width*unit,y+cellMetrics.height*unit+(last?0:Ui.dp(getContext(),resolved.rowGapDp)*scale));
            float halo=Ui.dp(getContext(),CellRenderer.HALO)*scale;
            c.image.set(contentLeft-halo,contentTop-halo,contentLeft+cellMetrics.width*unit+halo,contentTop+cellMetrics.height*unit+halo);
            float diameter=Math.min(cellWidth*.55f,Math.max(14*d,20*d*scale));
            float iconLeft=contentLeft+cellMetrics.iconLeft*unit,iconTop=contentTop+cellMetrics.iconTop*unit;
            float removeLeft=iconLeft-diameter*.35f,removeTop=iconTop-diameter*.35f;
            c.remove.set(removeLeft,removeTop,removeLeft+diameter,removeTop+diameter);
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);float d=getResources().getDisplayMetrics().density;
        paint.setAlpha(255);paint.setShader(wallpaper);wallpaperBounds.set(0,0,getWidth(),getHeight());canvas.drawRoundRect(wallpaperBounds,20*d,20*d,paint);paint.setShader(null);
        paint.setColor(0x187db4ef);canvas.drawCircle(getWidth()*.85f,getHeight()*.2f,90*d,paint);
        paint.setColor(config.color());canvas.drawRoundRect(widget,config.cornerDp*d*scale,config.cornerDp*d*scale,paint);
        canvas.save();Path bgMask=new Path();bgMask.addRoundRect(widget,config.cornerDp*d*scale,config.cornerDp*d*scale,Path.Direction.CW);canvas.clipPath(bgMask);BackgroundStore.draw(getContext(),canvas,config,widget);canvas.restore();
        text.setColor(config.labelColor);text.setTextSize(12*d*getResources().getConfiguration().fontScale*scale);text.setTypeface(Typeface.DEFAULT);text.setTextAlign(Paint.Align.CENTER);
        if(resolved.heading) {
            int saved=canvas.save();canvas.translate(widget.left+(Ui.dp(getContext(),resolved.paddingHorizontalDp)+Ui.dp(getContext(),6))*scale,widget.top+(Ui.dp(getContext(),resolved.paddingVerticalDp)+Ui.dp(getContext(),6))*scale);canvas.scale(scale,scale);if(headingLayout!=null)headingLayout.draw(canvas);canvas.restoreToCount(saved);
        }
        int saved=canvas.save();canvas.clipRect(viewport);iconPaint.setAlpha(255);
        if(dragIndex<0)for(int row=0;row<RowRenderer.count(resolved);row++){
            Bitmap image=rowImages.get(row);if(image==null)continue;
            float y=body.top+(resolved.gridInsetTopPx+Ui.dp(getContext(),resolved.shadowOutsetDp))*scale+RowRenderer.top(getContext(),resolved,row)*d*scale-scrollOffset;
            canvas.drawBitmap(image,null,new RectF(widget.left,y,widget.right,y+RowRenderer.height(getContext(),resolved,row)*d*scale),iconPaint);
        }
        for(int i=0;i<cells.size();i++) {
            Cell c=cells.get(i);if(!RectF.intersects(c.bounds,body))continue;
            String flat=config.apps.get(i);Bitmap bitmap=icons.get(flat);
            if(bitmap!=null&&dragIndex>=0) { iconPaint.setAlpha(i==dragIndex?65:255);canvas.drawBitmap(bitmap,null,c.image,iconPaint);iconPaint.setAlpha(255); }
            if(listener!=null&&i!=dragIndex)drawRemove(canvas,c.remove);
            if(i==focused) { paint.setColor(0xffa4bdff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*d);canvas.drawRoundRect(c.bounds,8*d,8*d,paint);paint.setStyle(Paint.Style.FILL); }
        }
        canvas.restoreToCount(saved);
        if(config.apps.isEmpty()) { text.setTextSize(14*d);text.setTextAlign(Paint.Align.CENTER);canvas.drawText(getContext().getString(R.string.ui_choose_apps_or_contacts_104),getWidth()/2f,getHeight()/2f,text); }
        if(resolved.editButton&&dragIndex<0) { text.setTextAlign(Paint.Align.RIGHT);canvas.drawText(getContext().getString(R.string.ui_edit_105),widget.right-12*d*scale,widget.bottom-12*d*scale,text); }
        if(dragIndex>=0) {
            paint.setColor(trash.contains(pointerX,pointerY)?0xffa51f39:0xe0a82f45);canvas.drawRoundRect(trash,12*d,12*d,paint);
            text.setColor(Color.WHITE);text.setTextSize(14*d);text.setTextAlign(Paint.Align.CENTER);canvas.drawText(getContext().getString(R.string.ui_drop_here_to_remove_106),trash.centerX(),trash.centerY()-(text.ascent()+text.descent())/2,text);
            Bitmap bitmap=icons.get(config.apps.get(dragIndex));float width=(cellMetrics.width+CellRenderer.HALO*2)*d*scale*1.07f,height=(cellMetrics.height+CellRenderer.HALO*2)*d*scale*1.07f;
            floating.set(pointerX-width/2,pointerY-height/2,pointerX+width/2,pointerY+height/2);iconPaint.setAlpha(245);if(bitmap!=null)canvas.drawBitmap(bitmap,null,floating,iconPaint);iconPaint.setAlpha(255);
        }
    }
    private void drawRemove(Canvas canvas,RectF circle) {
        paint.setColor(Color.WHITE);canvas.drawOval(circle,paint);paint.setColor(0xffa3253c);RectF small=new RectF(circle);small.inset(1,1);canvas.drawOval(small,paint);
        paint.setColor(Color.WHITE);paint.setStrokeWidth(Math.max(1,circle.width()*.08f));float m=circle.width()*.32f;
        canvas.drawLine(circle.left+m,circle.top+m,circle.right-m,circle.bottom-m,paint);canvas.drawLine(circle.left+m,circle.bottom-m,circle.right-m,circle.top+m,paint);
    }
    private int hit(float x,float y) { if(!body.contains(x,y))return -1;int col=(int)((x-body.left)/cellWidth),row=(int)((y-body.top+scrollOffset)/cellHeight),index=row*columns+col;return col<columns&&index>=0&&index<cells.size()&&cells.get(index).bounds.contains(x,y)?index:-1; }
    private void beginDrag() {
        if(listener==null||downIndex<0||moving||pressedRemove)return;
        dragIndex=downIndex;beforeDrag=WidgetConfig.fromJson(config.toJson());performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);handler.post(autoScroll);invalidate();
    }
    private boolean scrollByAmount(float delta) { float next=Math.max(0,Math.min(scrollOffset+delta,Math.max(0,contentHeight-body.height())));if(next==scrollOffset)return false;scrollOffset=next;updateCells();invalidate();return true; }
    private void moveDragTarget(float x,float y) {
        if(trash.contains(x,y))return;int target=hit(x,y);
        if(target>=0&&target!=dragIndex) { String flat=config.apps.remove(dragIndex);config.apps.add(target,flat);dragIndex=target;invalidate(); }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        pointerX=event.getX();pointerY=event.getY();
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX=pointerX;downY=pointerY;downIndex=hit(downX,downY);moving=false;pressedRemove=listener!=null&&downIndex>=0&&cells.get(downIndex).remove.contains(downX,downY);
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                if(downIndex>=0&&!pressedRemove&&listener!=null)handler.postDelayed(longPress,ViewConfiguration.getLongPressTimeout());return true;
            case MotionEvent.ACTION_MOVE:
                if(dragIndex>=0) { moveDragTarget(pointerX,pointerY);invalidate();return true; }
                float delta=pointerY-downY;
                if(Math.abs(delta)>ViewConfiguration.get(getContext()).getScaledTouchSlop()||Math.abs(pointerX-downX)>ViewConfiguration.get(getContext()).getScaledTouchSlop()) { moving=true;handler.removeCallbacks(longPress); }
                if(moving) { boolean scrolled=scrollByAmount(-delta);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(scrolled);downY=pointerY; }return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPress);handler.removeCallbacks(autoScroll);
                if(dragIndex>=0) { int index=dragIndex;dragIndex=-1;if(trash.contains(pointerX,pointerY))removeAt(index);else notifyChange(null,-1);beforeDrag=null; }
                else if(!moving&&downIndex>=0) { if(pressedRemove&&cells.get(downIndex).remove.contains(pointerX,pointerY))removeAt(downIndex);else if(listener!=null)showActions(downIndex);performClick(); }
                downIndex=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPress);handler.removeCallbacks(autoScroll);dragIndex=-1;
                if(beforeDrag!=null) { config=beforeDrag;beforeDrag=null;calculate(); }downIndex=-1;invalidate();return true;
            default:return super.onTouchEvent(event);
        }
    }
    @Override public boolean performClick() { super.performClick();return true; }
    private void removeAt(int index) {
        if(listener==null||index<0||index>=config.apps.size())return;String removed=config.apps.remove(index);calculate();loadIcons();notifyChange(removed,index);invalidate();
    }
    private void notifyChange(String removed,int position) { if(listener!=null)listener.onAppsChanged(new ArrayList<>(config.apps),removed,position);sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED); }
    private void moveBy(int index,int delta) { int target=index+delta;if(target<0||target>=config.apps.size())return;Collections.swap(config.apps,index,target);focused=target;notifyChange(null,-1);invalidate(); }
    private void showActions(int index) {
        if(index<0||index>=config.apps.size())return;
        new AlertDialog.Builder(getContext()).setTitle(names.get(config.apps.get(index))).setItems(new String[]{getContext().getString(R.string.ui_move_earlier_107),getContext().getString(R.string.ui_move_later_108),getContext().getString(R.string.ui_remove_from_widget_109)},(dialog,which)->{if(which==2)removeAt(index);else moveBy(index,which==0?-1:1);}).setNegativeButton(getContext().getString(R.string.ui_cancel_110),null).show();
    }
    @Override protected void onDetachedFromWindow() { handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow(); }
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() { return provider; }
    private final AccessibilityNodeProvider provider=new AccessibilityNodeProvider() {
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualId) {
            AccessibilityNodeInfo node=AccessibilityNodeInfo.obtain();
            if(virtualId==HOST_VIEW_ID) {
                PreviewView.super.onInitializeAccessibilityNodeInfo(node);node.setSource(PreviewView.this);node.setClassName("android.widget.GridView");
                node.setCollectionInfo(AccessibilityNodeInfo.CollectionInfo.obtain((config.apps.size()+columns-1)/columns,columns,false));
                for(int i=0;i<config.apps.size();i++)node.addChild(PreviewView.this,i+1);
                if(contentHeight>body.height()) { node.setScrollable(true);node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);node.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); }return node;
            }
            int index=virtualId-1;if(index<0||index>=cells.size())return null;
            node.setPackageName(getContext().getPackageName());node.setSource(PreviewView.this,virtualId);node.setParent(PreviewView.this);node.setClassName("android.widget.Button");node.setEnabled(true);node.setFocusable(true);
            String flat=config.apps.get(index);node.setContentDescription(names.get(flat)+". "+(index+1)+getContext().getString(R.string.ui_of_113)+config.apps.size());
            Rect rect=new Rect();cells.get(index).bounds.roundOut(rect);node.setBoundsInParent(rect);int[] location=new int[2];getLocationOnScreen(location);rect.offset(location[0],location[1]);node.setBoundsInScreen(rect);
            node.setVisibleToUser(RectF.intersects(cells.get(index).bounds,body));node.setAccessibilityFocused(focused==index);node.addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);node.addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
            if(listener!=null) {
                node.setClickable(true);node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                node.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.preview_remove,getContext().getString(R.string.ui_remove_from_widget_109)));
                if(index>0)node.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.preview_move_before,getContext().getString(R.string.ui_move_earlier_107)));
                if(index+1<config.apps.size())node.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.preview_move_after,getContext().getString(R.string.ui_move_later_108)));
            }return node;
        }
        @Override public boolean performAction(int virtualId,int action,Bundle args) {
            if(virtualId==HOST_VIEW_ID&&(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))return scrollByAmount(body.height()*.8f*(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1));
            int index=virtualId-1;if(index<0||index>=config.apps.size())return false;
            if(action==AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) { focused=index;emit(virtualId,AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);invalidate();return true; }
            if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) { focused=-1;invalidate();return true; }
            if(listener==null)return false;
            if(action==AccessibilityNodeInfo.ACTION_CLICK) { showActions(index);return true; }
            if(action==R.id.preview_remove) { removeAt(index);return true; }
            if(action==R.id.preview_move_before||action==R.id.preview_move_after) { moveBy(index,action==R.id.preview_move_before?-1:1);return true; }return false;
        }
    };
    private void emit(int id,int type) { AccessibilityEvent event=AccessibilityEvent.obtain(type);event.setPackageName(getContext().getPackageName());event.setSource(this,id);if(getParent()!=null)getParent().requestSendAccessibilityEvent(this,event); }
    @Override public boolean dispatchHoverEvent(MotionEvent event) {
        AccessibilityManager manager=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager!=null&&manager.isTouchExplorationEnabled()) {
            int target=event.getAction()==MotionEvent.ACTION_HOVER_EXIT?-1:hit(event.getX(),event.getY());
            if(target!=hovered) { if(hovered>=0)emit(hovered+1,AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);if(target>=0)emit(target+1,AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);hovered=target; }return true;
        }return super.dispatchHoverEvent(event);
    }
}
