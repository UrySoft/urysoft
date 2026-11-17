package cat.urysoft.widgets;

/** Pure entitlement rules; existing designs are never deleted or silently downgraded. */
final class PremiumPolicy {
    static final int FREE_SHORTCUTS=8,MAX_SHORTCUTS=48;
    enum Feature { NONE, WIDGETS, SHORTCUTS, SHAPES, STYLES, BACKGROUND, SHADOWS }
    static final class Design {
        final int count,shape,style,backgroundMode;
        final String background,shadow;
        Design(int count,int shape,int style,String background,int backgroundMode,String shadow){
            this.count=count;this.shape=shape;this.style=style;this.background=background;
            this.backgroundMode=backgroundMode;this.shadow=shadow;
        }
    }
    static boolean canCreate(boolean premium,int configuredWidgets){return premium||configuredWidgets==0;}
    static int shortcutLimit(boolean premium,Design existing){
        return premium?MAX_SHORTCUTS:Math.max(FREE_SHORTCUTS,existing==null?0:existing.count);
    }
    static Feature saving(boolean premium,Design before,Design after,int otherWidgets){
        if(after.count>MAX_SHORTCUTS || after.count<0)return Feature.SHORTCUTS;
        if(premium)return Feature.NONE;
        if(before==null && otherWidgets>0)return Feature.WIDGETS;
        if(after.count>shortcutLimit(false,before))return Feature.SHORTCUTS;
        if(after.shape>2 && (before==null||after.shape!=before.shape))return Feature.SHAPES;
        if(after.style>1 && (before==null||after.style!=before.style))return Feature.STYLES;
        if(!after.background.isEmpty() && (before==null||!after.background.equals(before.background)||after.backgroundMode!=before.backgroundMode))return Feature.BACKGROUND;
        if(!after.shadow.isEmpty() && (before==null||!after.shadow.equals(before.shadow)))return Feature.SHADOWS;
        return Feature.NONE;
    }
}
