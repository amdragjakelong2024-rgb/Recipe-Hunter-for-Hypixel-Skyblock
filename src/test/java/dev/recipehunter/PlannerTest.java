package dev.recipehunter;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class PlannerTest {
    static int checks=0;
    static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static void eq(long actual,long expected,String label){check(actual==expected,label+": "+actual+" != "+expected);}
    public static void main(String[] args)throws Exception {
        Catalog c=new Catalog(Files.newInputStream(Path.of(args[0])));Planner p=new Planner(c);
        check(Ownership.petJson("{\"type\":\"GRIFFIN\",\"tier\":\"EPIC\"}").equals("GRIFFIN;3"),"Epic pet metadata");
        check(Ownership.petJson("{\"type\":\"GRIFFIN\",\"tier\":\"LEGENDARY\"}").equals("GRIFFIN;4"),"Legendary pet metadata");
        check(Ownership.petJson("broken").isEmpty(),"malformed metadata is not ownership");
        check(Ownership.petJson("{\"type\":\"GRIFFIN\",\"tier\":\"UNKNOWN\"}").isEmpty(),"unknown rarity not invented");
        var stone=p.plan("GRIFFIN_UPGRADE_STONE_LEGENDARY",1,Map.of(),Map.of());
        eq(stone.demand.get("ENCHANTED_GOLD_BLOCK"),96,"legendary gold blocks");
        eq(stone.demand.get("ENCHANTED_ANCIENT_CLAW"),32,"legendary claws");
        eq(stone.leaves.get("MYTHOS_FRAGMENT"),16,"mythos");
        eq(stone.leaves.get("GRIFFIN_FEATHER"),168,"feathers include base stone");
        eq(stone.leaves.get("SOUL_STRING"),256,"soul string");
        eq(stone.leaves.get("ANCIENT_CLAW"),5120,"raw claws");
        eq(stone.leaves.get("GOLD_INGOT"),2460160,"raw gold including base stone");
        eq(stone.leaves.get("COBBLESTONE"),1280,"raw cobble");
        check(stone.warnings.isEmpty(),"legendary cycle-free");
        var epicOwned=p.plan("GRIFFIN;4",1,Map.of("GRIFFIN;3",1L),Map.of());
        eq(epicOwned.leaves.get("SKYBLOCK_COIN"),500000,"only legendary Kat cost");
        check(!epicOwned.demand.containsKey("GRIFFIN_UPGRADE_STONE_EPIC"),"owned Epic removes earlier upgrades");
        eq(epicOwned.leaves.get("GRIFFIN_FEATHER"),168,"Epic owned feathers");
        var full=p.plan("GRIFFIN;4",1,Map.of(),Map.of());
        eq(full.leaves.get("GRIFFIN;0"),1,"common pet acquisition leaf");
        eq(full.leaves.get("SKYBLOCK_COIN"),760100,"full Kat fees, excludes separate common purchase");
        check(full.demand.containsKey("GRIFFIN_UPGRADE_STONE_EPIC"),"full earlier chain");
        check(p.plan("GRIFFIN;4",1,Map.of("GRIFFIN;4",1L),Map.of()).leaves.isEmpty(),"already have target");
        var stock=p.plan("GRIFFIN;4",1,Map.of("GRIFFIN;3",1L,"ENCHANTED_GOLD_BLOCK",96L,"BASE_GRIFFIN_UPGRADE_STONE",1L,"BRAIDED_GRIFFIN_FEATHER",1L),Map.of());
        check(!stock.leaves.containsKey("GOLD_INGOT"),"owned intermediates remove raw needs");
        check(!stock.leaves.containsKey("SOUL_STRING"),"owned braided feathers remove string");
        var term=p.plan("TERMINATOR",1,Map.of(),Map.of());
        eq(term.leaves.get("JUDGEMENT_CORE"),1,"core");
        eq(term.leaves.get("SOUL_STRING"),1024,"Terminator soul string");
        check(term.warnings.isEmpty(),"Terminator cycle-free");
        eq(term.leaves.get("INK_SACK-4"),6553600,"Terminator raw lapis");
        check(c.uses.get("NECRON_HANDLE").contains("NECRON_BLADE"),"handle leads to blade");
        check(c.item("NECRON_HANDLE").routes.isEmpty(),"handle uncraftable");
        check(c.search("legendary griffin").getFirst().id.equals("GRIFFIN;4"),"rarity exact search");
        check(c.search("necrons handle").getFirst().id.equals("NECRON_HANDLE"),"fuzzy apostrophe search");
        check(c.search("terminatr").stream().anyMatch(i->i.id.equals("TERMINATOR")),"typo");
        check(c.search("griffin").stream().filter(i->i.name.endsWith(" Pet")).count()==6,"all six Griffin rarities");
        // Shared output rounding fixture: two branches demand one each, output batch two => ONE craft.
        for(String id:List.of("TEST_ROOT","TEST_A","TEST_B","TEST_SHARED","TEST_RAW")){Catalog.Item item=new Catalog.Item();item.id=id;item.name=id;c.items.put(id,item);}
        route(c,"TEST_ROOT",1,"TEST_A",1,"TEST_B",1);route(c,"TEST_A",1,"TEST_SHARED",1);route(c,"TEST_B",1,"TEST_SHARED",1);route(c,"TEST_SHARED",2,"TEST_RAW",3);
        eq(p.plan("TEST_ROOT",1,Map.of(),Map.of()).leaves.get("TEST_RAW"),3,"shared rounding");
        eq(p.plan("TEST_ROOT",1,Map.of("TEST_SHARED",1L),Map.of()).leaves.get("TEST_RAW"),3,"stock used once");
        check(p.plan("TEST_ROOT",1,Map.of("TEST_SHARED",2L),Map.of()).leaves.isEmpty(),"shared stock fully covers");
        route(c,"TEST_RAW",1,"TEST_ROOT",1);var cyclic=p.plan("TEST_ROOT",1,Map.of(),Map.of());check(!cyclic.warnings.isEmpty(),"cycle reported");check(cyclic.leaves.containsKey("TEST_RAW"),"cycle remains unmet leaf");
        var gems=p.materials("PERFECT_JADE_GEM",3,Map.of(),Map.of());
        eq(gems.leaves.get("FINE_JADE_GEM"),1200,"three Perfect use 1200 Fine");
        eq(gems.leaves.get("JADE_CRYSTAL"),3,"crystals preserved");
        check(!gems.leaves.containsKey("ROUGH_JADE_GEM"),"no Rough below Fine");
        var gemStock=p.materials("PERFECT_JADE_GEM",3,Map.of("PERFECT_JADE_GEM",1L,"FLAWLESS_JADE_GEM",2L,"FINE_JADE_GEM",40L),Map.of());
        eq(gemStock.leaves.get("FINE_JADE_GEM"),600,"subtract owned Perfect Flawless and Fine");
        eq(gemStock.leaves.get("JADE_CRYSTAL"),2,"owned Perfect removes crystal cost");
        eq(p.materials("ENCHANTED_COAL_BLOCK",2,Map.of(),Map.of()).leaves.get("ENCHANTED_COAL"),320,"coal blocks show enchanted stacks");
        check(!p.materials("ENCHANTED_COAL_BLOCK",2,Map.of(),Map.of()).leaves.containsKey("COAL"),"raw coal hidden below enchanted");
        eq(p.materials("COAL",12,Map.of(),Map.of()).leaves.get("COAL"),12,"direct raw remains");
        var drill=p.materials("DIVAN_DRILL",1,Map.of(),Map.of());
        eq(drill.leaves.get("ENCHANTED_COAL"),320,"drill enchanted coal");
        eq(drill.leaves.get("FINE_JADE_GEM"),64,"drill Fine jade");
        check(drill.warnings.isEmpty(),"practical drill no cycles");

        Catalog.Item mixed=new Catalog.Item();mixed.id="TEST_MIXED";mixed.name="Mixed";c.items.put(mixed.id,mixed);
        route(c,"TEST_MIXED",1,"COAL",10,"ENCHANTED_COAL",2);
        var mix=p.materials("TEST_MIXED",1,Map.of(),Map.of());
        eq(mix.leaves.get("COAL"),10,"mixed direct coal retained");
        eq(mix.leaves.get("ENCHANTED_COAL"),2,"mixed enchanted coal separate");
        Book.Goal boots=new Book.Goal("DIVAN_BOOTS");
        var required=p.goal(boots,Map.of());
        eq(required.leaves.get("FLAWLESS_RUBY_GEM"),1,"boots keep required Flawless");
        eq(required.leaves.get("FINE_JADE_GEM"),40,"mixtures retain required Fine jade");
        check(!required.leaves.containsKey("ROUGH_JADE_GEM"),"no unwanted Rough jade");
        boots.fineGems=true;eq(p.goal(boots,Map.of()).leaves.get("FINE_RUBY_GEM"),80,"boots can expand Flawless to Fine");
        boots.checked.add("SLUDGE_JUICE");check(!p.goal(boots,Map.of()).leaves.containsKey("SLUDGE_JUICE"),"check removes sludge");
        boots.checked.remove("SLUDGE_JUICE");eq(p.goal(boots,Map.of()).leaves.get("SLUDGE_JUICE"),3200,"undo restores sludge");
        boots.checked.add("GEMSTONE_MIXTURE");check(!p.goal(boots,Map.of()).leaves.containsKey("SLUDGE_JUICE"),"parent check removes descendants");
        boots.checked.clear();
        StorageLedger ledger=new StorageLedger();ledger.replace("chest A",Map.of("SLUDGE_JUICE",2000L));ledger.replace("chest B",Map.of("SLUDGE_JUICE",1200L));
        eq(ledger.totals().get("SLUDGE_JUICE"),3200,"sum separate chests");
        ledger.replace("chest A",Map.of("SLUDGE_JUICE",2000L));eq(ledger.totals().get("SLUDGE_JUICE"),3200,"reopening does not duplicate");
        check(!p.goal(boots,ledger.totals()).leaves.containsKey("SLUDGE_JUICE"),"scanned chests cover goal");
        ledger.replace("chest A",Map.of());eq(ledger.totals().get("SLUDGE_JUICE"),1200,"withdrawn chest stock replaced");
        ledger.clear();check(ledger.totals().isEmpty(),"clear session chest cache");
        var start=new WalkingPath.Point(0,0,0);var end=new WalkingPath.Point(3,0,0);
        var walk=WalkingPath.find(start,end,v->v.y()==0&&v.z()==0&&v.x()>=0&&v.x()<=3,100);
        eq(walk.size(),4,"shortest simple walk path");
        check(WalkingPath.find(start,end,v->v.y()==0&&v.z()==0&&v.x()!=1,100).isEmpty(),"blocked path not invented");
        check(!Assistant.guide(c,"YOUNG_DRAGON_LEGGINGS").stream().anyMatch(v->v.contains("Auction")||v.contains("Bazaar")),"Ironman guide");
        check(Assistant.guide(c,"YOUNG_DRAGON_LEGGINGS").stream().anyMatch(v->v.contains("random")),"dragon type not guaranteed");
        check(Celebration.craftMenu("Craft Item"),"craft context accepted");
        check(!Celebration.craftMenu("Chest"),"chest is not a craft");
        Path temp=Files.createTempDirectory("recipehunter-test").resolve("book.json");Book b=new Book();var g=new Book.Goal("GRIFFIN;4");g.visible=false;g.checked.add("SLUDGE_JUICE");g.fineGems=true;b.history.add("DIVAN_BOOTS");b.assistant=true;g.rates.put("SOUL_STRING",120.0);b.goals.add(g);b.last=g.id;b.write(temp);Book copy=Book.read(temp);check(!copy.find(g.id).visible,"persist visibility");check(copy.find(g.id).rates.get("SOUL_STRING")==120.0,"persist rate");
        check(copy.find(g.id).checked.contains("SLUDGE_JUICE"),"checkmark persists");check(copy.find(g.id).fineGems,"gem preference persists");check(copy.history.contains("DIVAN_BOOTS"),"past goal persists");check(copy.assistant,"assistant toggle persists");
        HudLayout layout=new HudLayout();layout.move(100,80,800,600);
        eq(layout.left(800,600),100,"HUD drag X");eq(layout.top(800,600),80,"HUD drag Y");
        layout.zoom(.5,800,600);eq(layout.left(800,600),100,"zoom keeps anchor X");eq(layout.top(800,600),80,"zoom keeps anchor Y");
        layout.move(-100,10000,800,600);eq(layout.left(800,600),0,"left clamp");
        check(layout.top(800,600)+HudLayout.HEIGHT*layout.scale(800,600)<=600.5,"bottom clamp");
        layout.zoom(100,800,600);check(layout.scale==2,"scale upper limit");layout.zoom(-100,800,600);check(layout.scale==.5,"scale lower limit");
        layout.move(10000,10000,160,90);check(layout.left(160,90)+HudLayout.WIDTH*layout.scale(160,90)<=160.5,"small screen fits");
        g.hud=layout;b.write(temp);Book positioned=Book.read(temp);check(positioned.find(g.id).hud.x==layout.x&&positioned.find(g.id).hud.scale==layout.scale,"HUD position and scale persist");
        Files.writeString(temp,"{\"goals\":[{\"id\":\"DIVAN_BOOTS\",\"count\":1}],\"last\":\"DIVAN_BOOTS\"}");
        check(Book.read(temp).goals.getFirst().hud==null,"older book migrates without invented origin");
        Catalog.Route coal=c.item("ENCHANTED_COAL").routes.getFirst();check(coal.slots.size()==9&&coal.slots.get(0)==null&&coal.slots.get(1).count==32,"preview preserves empty slots and counts");
        check(c.item("PERFECT_JADE_GEM").routes.getFirst().slots.isEmpty(),"Forge has no invented crafting layout");
        Files.writeString(temp,"invalid");try{Book.read(temp);throw new AssertionError("corrupt book accepted");}catch(IOException expected){check(Files.readString(temp).equals("invalid"),"corrupt original preserved");}
        System.out.println("PASS: "+checks+" checks; "+c.items.size()+" catalogue items.");
        System.out.println("Legendary stone raw totals: "+stone.leaves);
        System.out.println("Epic -> Legendary totals: "+epicOwned.leaves);
        System.out.println("Terminator warnings: "+term.warnings);
    }
    static void route(Catalog c,String id,long output,Object... inputs){Catalog.Route r=new Catalog.Route();r.type="crafting";r.count=output;for(int i=0;i<inputs.length;i+=2)r.inputs.add(new Catalog.Ingredient((String)inputs[i],((Number)inputs[i+1]).longValue()));c.item(id).routes.add(r);}
}
