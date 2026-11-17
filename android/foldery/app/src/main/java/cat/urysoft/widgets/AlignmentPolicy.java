package cat.urysoft.widgets;
/** Physical positions in a 3x3 compass. Overflow always starts at the top. */
public final class AlignmentPolicy {
    private AlignmentPolicy() {}
    public static int offset(int spare,int axis) {
        spare=Math.max(0,spare);
        return axis==0?0:axis==2?spare:spare/2;
    }
    public static int horizontal(int spare,int alignment){return offset(spare,Math.max(0,Math.min(8,alignment))%3);}
    public static int vertical(int spare,int alignment){return offset(spare,Math.max(0,Math.min(8,alignment))/3);}
}
