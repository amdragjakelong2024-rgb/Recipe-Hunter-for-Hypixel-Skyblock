package dev.recipehunter;
import java.util.*;
import java.nio.file.*;

/** Pure catalogue/ledger fixtures; these do not claim in-game verification. */
public final class ProgressTest {
 static int checks;
 static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static void eq(double a,double b,String label){check(Math.abs(a-b)<1e-6,label+": "+a+" != "+b);}
 public static void main(String[] args)throws Exception{run(new Catalog(Files.newInputStream(Path.of(args[0]))));}
 public static void run(Catalog c)throws Exception{
  RecipeProgress engine=new RecipeProgress(c);
  Book.Goal mithril=new Book.Goal("ENCHANTED_MITHRIL");mithril.count=960;
  var raw=engine.evaluate(mithril,Map.of("MITHRIL_ORE",1600L),Map.of(),Map.of());eq(raw.equivalent,10,"raw Mithril contributes fractional crafted units");
  var compacted=engine.evaluate(mithril,Map.of("ENCHANTED_MITHRIL",10L),Map.of(),Map.of());eq(compacted.equivalent,raw.equivalent,"compaction preserves equivalent progress");
  Book.Goal diamond=new Book.Goal("ENCHANTED_DIAMOND_BLOCK");diamond.count=35;diamond.materialDisplay="Enchanted";
  var d=engine.evaluate(diamond,Map.of("ENCHANTED_DIAMOND",160L),Map.of(),Map.of());
  var ds=engine.displayed(d,diamond);eq(ds.getFirst().total,5600,"35 blocks = 5600 enchanted diamonds");eq(d.equivalent,1,"enchanted diamonds contribute one block");
  diamond.materialDisplay="Enchanted Block";eq(engine.displayed(d,diamond).getFirst().total,35,"block presentation retains original count");eq(d.equivalent,1,"display change does not reset progress");
  Book.Goal ruby=new Book.Goal("FLAWLESS_RUBY_GEM");ruby.gemDisplay="Fine";
  var rt=engine.evaluate(ruby,Map.of(),Map.of(),Map.of());check(engine.displayed(rt,ruby).stream().anyMatch(n->n.id.equals("FINE_RUBY_GEM")),"Flawless Ruby displays Fine");
  Book.Goal jade=new Book.Goal("PERFECT_JADE_GEM");
  for(String tier:List.of("Fine","Flawless","Perfect")){jade.gemDisplay=tier;var tree=engine.evaluate(jade,Map.of(),Map.of(),Map.of());check(engine.displayed(tree,jade).stream().anyMatch(n->n.id.equals("JADE_CRYSTAL")),"crystal retained at "+tier);}
  var pool=new HashMap<>(Map.of("ENCHANTED_DIAMOND_BLOCK",1L));Book.Goal first=new Book.Goal("ENCHANTED_DIAMOND_BLOCK"),second=new Book.Goal("ENCHANTED_DIAMOND_BLOCK");
  eq(engine.allocate(first,pool,Map.of(),Map.of()).equivalent,1,"first queued goal allocated");eq(engine.allocate(second,pool,Map.of(),Map.of()).equivalent,0,"second goal cannot reuse same stock");
  var pending=engine.evaluate(first,Map.of(),Map.of(first.id,1L),Map.of(first.id,1L));eq(pending.equivalent,0,"Forge pending is not owned");eq(pending.forge,1,"Forge reservation displayed separately");
  var tiny=new MaterialProgress.Row("TEST",0,1000000,0,0,false,1,"");check(!tiny.percent().equals("0%"),"tiny nonzero progress visible");
  var nearly=new MaterialProgress.Row("TEST",99999,100000,0,0,false,99999,"");check(!nearly.percent().equals("100%"),"unfinished never rounds to 100%");
  MaterialLedger ledger=new MaterialLedger();check(ledger.get("Sacks","WHEAT").quality()==MaterialLedger.Quality.UNKNOWN,"unscanned stock unknown");
  ledger.snapshot("Inventory",Map.of("WHEAT",64L),1,MaterialLedger.Quality.LIVE);ledger.snapshot("Sacks",Map.of(),1,MaterialLedger.Quality.LAST_SYNCED);
  eq(ledger.known().get("WHEAT"),64,"inventory observed");ledger.snapshot("Inventory",Map.of(),2,MaterialLedger.Quality.LIVE);ledger.snapshot("Sacks",Map.of("WHEAT",64L),2,MaterialLedger.Quality.LAST_SYNCED);eq(ledger.known().get("WHEAT"),64,"transfer replaces sources without duplication");
  ledger.clear();check(ledger.known().isEmpty(),"reconnect/profile resets evidence");
  int routes=0,unknown=0,graphs=0;List<String> issues=new ArrayList<>();
  for(var item:c.items.values()){
   check(!item.id.toLowerCase(Locale.ROOT).contains("netherite"),"SkyBlock only");
   for(var r:item.routes){routes++;for(var in:r.inputs){check(in.count>0,"positive quantity "+item.id);if(!c.items.containsKey(in.id)&&!in.id.equals("SKYBLOCK_COIN")){unknown++;issues.add(item.id+" -> "+in.id);}}}
   try{var node=engine.evaluate(new Book.Goal(item.id),Map.of(),Map.of(),Map.of());check(Double.isFinite(node.equivalent)&&node.equivalent>=0&&node.equivalent<=node.total,"finite bounded progress "+item.id);graphs++;}catch(ArithmeticException|IllegalArgumentException ex){issues.add(item.id+": "+ex.getMessage());}
  }
  Path report=Path.of("build/reports/catalogue-progress.txt");Files.createDirectories(report.getParent());Files.writeString(report,"Items: "+c.items.size()+"\nRoutes: "+routes+"\nEvaluated graphs: "+graphs+"\nUnknown inputs: "+unknown+"\n"+String.join("\n",issues)+"\nSynthetic fixtures only; live Hypixel acceptance remains unverified.\n");
  check(unknown==0,"all ingredient IDs resolve; see catalogue-progress.txt");
  System.out.println("PASS: "+checks+" progress/ledger checks; "+graphs+" catalogue graphs; "+routes+" routes");
 }
}
