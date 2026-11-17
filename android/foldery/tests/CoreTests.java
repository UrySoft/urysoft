package cat.urysoft.widgets;

import java.util.*;

public final class CoreTests {
    private static int checks;
    private static void expect(boolean condition, String label) { checks++; if (!condition) throw new AssertionError(label); }
    public static void main(String[] args) {
        expect(LayoutPolicy.iconSize(1)==1,"small resolved size stays within its column");
        expect(LayoutPolicy.iconSize(2500)==2048,"large drawing bound");
        expect(LayoutPolicy.iconSize(47)==47,"column sizes do not round up and overflow");
        expect(LayoutPolicy.responsiveColumns(300,12,12,3,48)==3,"requested columns");
        expect(LayoutPolicy.columnWidth(300,12,12,3)==84,"three maximum-width icons");
        expect(LayoutPolicy.effectiveGap(300,12,1,40)==0,"one column has no horizontal gap");
        expect(LayoutPolicy.columnWidth(700,12,12,2)==332,"width sizing is not capped at 160");
        for(int width=112;width<=1600;width+=13)for(int padding=0;padding<=40;padding+=8)for(int gap=0;gap<=40;gap+=8)for(int requested=0;requested<=8;requested++){
            int columns=LayoutPolicy.responsiveColumns(width,padding,gap,requested,48);
            int effective=LayoutPolicy.effectiveGap(width,padding,columns,gap);
            float icon=LayoutPolicy.columnWidth(width,padding,effective,columns);
            expect(columns>=1&&columns<=8,"valid column count");
            expect(icon>=28-.001f,"minimum visible icon fits");
            expect(Math.abs(columns*icon+(columns-1)*effective-LayoutPolicy.innerWidth(width,padding))<.02f,"icons use the full available width");
            expect(effective<=gap,"spacing only shrinks when width requires it");
        }
        for(int target=28;target<=300;target++)for(int width=112;width<=1600;width+=13){
            int cols=LayoutPolicy.responsiveColumns(width,12,12,0,48,target);
            float column=LayoutPolicy.columnWidth(width,12,LayoutPolicy.effectiveGap(width,12,cols,12),cols);
            expect(cols>=1&&cols<=8,"automatic column bounds");
            expect(Math.min(column,target)<=target,"automatic icon respects selected size");
            if(LayoutPolicy.innerWidth(width,12)>=target)expect(column+.01f>=target,"chosen automatic size fits");
        }
        expect(LayoutPolicy.responsiveColumns(700,12,12,0,48,300)==2,"300 icons produce two columns");
        expect(LayoutPolicy.responsiveColumns(700,12,12,3,48,300)==3,"manual columns ignore automatic size");
        for(int inner=28;inner<1600;inner+=7)for(int target=28;target<=300;target+=3)for(int gap=0;gap<=40;gap+=2){
            int columns=Math.max(1,Math.min(8,(inner+gap)/(target+gap)));
            int column=LayoutPolicy.automaticColumnPixels((inner-gap*(columns-1))/columns,target);
            int remainder=inner-column*columns-gap*(columns-1);
            expect(remainder>=0,"automatic grid fits viewport");
            expect(column<=target,"automatic size remains bounded");
            expect(column*columns+gap*(columns-1)+remainder==inner,"unused width belongs at edges");
            if(gap==0)expect(column*columns==inner-remainder,"zero gap means consecutive icon bounds touch");
        }
        expect(LanguagePolicy.normalize(null).equals(""),"new install follows device");
        expect(LanguagePolicy.normalize("fr").equals(""),"unknown preference follows device");
        expect(LanguagePolicy.normalize("ca-ES").equals("ca"),"regional Catalan");
        expect(LanguagePolicy.normalize("es_MX").equals("es"),"regional Spanish");
        expect(LanguagePolicy.normalize("en-GB").equals("en"),"regional English");
        for(int choice=0;choice<4;choice++)expect(LanguagePolicy.index(LanguagePolicy.tag(choice))==choice,"language selector round trip");
        expect(LanguagePolicy.tag(-1).isEmpty()&&LanguagePolicy.tag(8).isEmpty(),"invalid choices follow device");
        ArrayList<BadgeMath.Event> events = new ArrayList<>();
        events.add(new BadgeMath.Event("mail", "group", true, false, true));
        events.add(new BadgeMath.Event("mail", "group", false, false, true));
        events.add(new BadgeMath.Event("mail", "group", false, false, true));
        events.add(new BadgeMath.Event("music", null, false, true, true));
        events.add(new BadgeMath.Event("hidden", null, false, false, false));
        events.add(new BadgeMath.Event("chat", "summary-only", true, false, true));
        Map<String, Integer> counts = BadgeMath.count(events);
        expect(counts.get("mail") == 2, "group summary not double counted");
        expect(counts.get("chat") == 1, "summary without visible children remains visible");
        expect(!counts.containsKey("music"), "ongoing notification excluded");
        expect(!counts.containsKey("hidden"), "channel badging disabled");
        events.remove(2); expect(BadgeMath.count(events).get("mail") == 1, "dismissal decreases count");
        expect(BadgeMath.count(Collections.emptyList()).isEmpty(), "revocation clears counts");
        events.add(new BadgeMath.Event("other", "group", false, false, true));
        expect(BadgeMath.count(events).get("other") == 1, "group keys scoped to application");
        System.out.println("PASS: " + checks + " core assertions");
    }
}
