package cat.urysoft.widgets;
public final class AlignmentPolicyTests {
 public static void main(String[] args){int checks=0;for(int spare=-100;spare<1000;spare++)for(int position=0;position<9;position++){int s=Math.max(0,spare);int x=AlignmentPolicy.horizontal(spare,position),y=AlignmentPolicy.vertical(spare,position);if(x<0||x>s||y<0||y>s)throw new AssertionError();if(position==4&&(x!=s/2||y!=s/2))throw new AssertionError();if(spare<0&&(x!=0||y!=0))throw new AssertionError();checks++;}System.out.println("PASS: "+checks+" alignment and overflow checks");}
}