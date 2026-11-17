package cat.urysoft.widgets;
public final class PremiumTests {
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static PremiumPolicy.Design d(int n,int shape,int style,String bg,String shadow){return new PremiumPolicy.Design(n,shape,style,bg,0,shadow);}
 static void expect(boolean paid,PremiumPolicy.Design before,PremiumPolicy.Design after,int others,PremiumPolicy.Feature f){check(PremiumPolicy.saving(paid,before,after,others)==f,"expected "+f);}
 public static void main(String[] args){
  check(PremiumPolicy.canCreate(false,0),"first widget free");check(!PremiumPolicy.canCreate(false,1),"second widget gated");check(PremiumPolicy.canCreate(true,10),"paid widgets");
  expect(false,null,d(8,2,1,"",""),0,PremiumPolicy.Feature.NONE);
  expect(false,null,d(9,2,1,"",""),0,PremiumPolicy.Feature.SHORTCUTS);
  expect(false,null,d(1,0,0,"",""),1,PremiumPolicy.Feature.WIDGETS);
  expect(true,null,d(48,11,7,"photo","shadow"),10,PremiumPolicy.Feature.NONE);
  expect(true,null,d(49,0,0,"",""),0,PremiumPolicy.Feature.SHORTCUTS);
  expect(false,null,d(1,3,0,"",""),0,PremiumPolicy.Feature.SHAPES);
  expect(false,null,d(1,0,2,"",""),0,PremiumPolicy.Feature.STYLES);
  expect(false,null,d(1,0,0,"photo",""),0,PremiumPolicy.Feature.BACKGROUND);
  expect(false,null,d(1,0,0,"","shadow"),0,PremiumPolicy.Feature.SHADOWS);
  PremiumPolicy.Design legacy=d(48,11,7,"photo","shadow");
  expect(false,legacy,legacy,4,PremiumPolicy.Feature.NONE);
  expect(false,legacy,d(20,11,7,"photo","shadow"),4,PremiumPolicy.Feature.NONE);
  expect(false,legacy,d(8,0,0,"",""),4,PremiumPolicy.Feature.NONE);
  expect(false,legacy,d(48,10,7,"photo","shadow"),4,PremiumPolicy.Feature.SHAPES);
  expect(false,legacy,d(48,11,7,"another","shadow"),4,PremiumPolicy.Feature.BACKGROUND);
  expect(false,legacy,d(48,11,7,"photo","changed"),4,PremiumPolicy.Feature.SHADOWS);
  expect(false,null,d(-1,0,0,"",""),0,PremiumPolicy.Feature.SHORTCUTS);
  System.out.println("PASS: "+checks+" Premium policy checks");
 }
}
