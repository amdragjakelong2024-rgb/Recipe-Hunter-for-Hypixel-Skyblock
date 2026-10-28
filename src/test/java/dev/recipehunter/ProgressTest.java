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
  // Repeated input paths must become ONE summary/HUD line without creating stock.
  RecipeProgress.Node root=new RecipeProgress.Node("TEST_DRILL","",1,0,0,0,false);
  RecipeProgress.Node ironA=new RecipeProgress.Node("ENCHANTED_IRON","TEST_PLATE",800,160,5,2,false);
  RecipeProgress.Node ironB=new RecipeProgress.Node("ENCHANTED_IRON","TEST_DRILL_PART",160,0,5,2,false);
  root.children.add(ironA);root.children.add(ironB);
  var unique=engine.displayed(root,new Book.Goal("TEST_DRILL"));
  check(unique.size()==1,"identical material merges into one summary row");
  var consolidated=unique.getFirst();
  eq(consolidated.total,960,"800 + 160 iron requirements shown as 960 total");
  eq(consolidated.equivalent,160,"160 physical iron is counted only once across branches");
  eq(consolidated.forge,2,"repeated Forge snapshot is not doubled");
  eq(consolidated.pending,5,"repeated pending snapshot is not doubled");
  check(consolidated.combined,"merged row flags multiple recipe steps");
  check(root.children.size()==2,"real dependency tree retains both paths");
  Book.Goal selectedDrill=new Book.Goal("TITANIUM_DRILL_2");
  selectedDrill.materialDisplay="Enchanted";
  Catalog.Item x555=c.items.values().stream().filter(item->item.name.equals("Titanium Drill DR-X555")).findFirst().orElse(null);
  if(x555!=null){
   selectedDrill.id=x555.id;
   var drillTree=engine.evaluate(selectedDrill,Map.of("ENCHANTED_IRON",160L),Map.of(),Map.of());
   var drillSummary=engine.displayed(drillTree,selectedDrill);
   check(drillSummary.stream().map(n->n.id).distinct().count()==drillSummary.size(),"actual DR-X555 has no repeated material IDs on HUD");
   var ironRows=drillSummary.stream().filter(n->n.id.equals("ENCHANTED_IRON")).toList();
   check(ironRows.size()==1,"DR-X555 iron appears once");
   check(ironRows.getFirst().total>=160,"DR-X555 iron demand is nonzero");
   check(ironRows.getFirst().equivalent<=160,"DR-X555 raw inventory cannot be counted twice");
  }

  var upgrade=new Book.Goal("TITANIUM_DRILL_3");upgrade.materialDisplay="Enchanted";
  var upgradeRows=engine.displayed(engine.evaluate(upgrade,Map.of("TITANIUM_DRILL_2",1L),Map.of(),Map.of()),upgrade);
  var jewels=upgradeRows.stream().filter(n->n.id.equals("GLACITE_JEWEL")).findFirst().orElseThrow();
  eq(jewels.total,50,"X455 already covers 60 historical Jewels; upgrade needs only 50");
  eq(jewels.physical,0,"owned drill is not loose Jewels");eq(jewels.equivalent,0,"historical Jewels do not fill acquisition row");
  check(upgradeRows.stream().noneMatch(n->Set.of("ENCHANTED_COAL","ENCHANTED_REDSTONE","TREASURITE").contains(n.id)),"owned X455 removes historical-only ingredients");
  for(var item:c.items.values())for(var route:item.routes){
   var g=new Book.Goal(item.id);g.choices.put(item.id,item.routes.indexOf(route));g.materialDisplay="Enchanted";
   Map<String,Long> observed=new HashMap<>();for(var in:route.inputs)observed.merge(in.id,in.count,Long::sum);
   var rows=engine.displayed(engine.evaluate(g,observed,Map.of(),Map.of()),g);
   Map<String,Long> shown=new HashMap<>();for(var row:rows)shown.merge(row.id,row.row().have(),Long::sum);
   for(var e:shown.entrySet())check(e.getValue()<=observed.getOrDefault(e.getKey(),0L),"no invented physical stock for "+item.id+" -> "+e.getKey());
  }

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
  eq(pending.remaining(),0,"Forge reservation removes grind demand without awarding ownership");
  Book.Goal enchantedTarget=new Book.Goal("ENCHANTED_DIAMOND");enchantedTarget.count=160;
  eq(engine.evaluate(enchantedTarget,Map.of("ENCHANTED_DIAMOND_BLOCK",1L),Map.of(),Map.of()).equivalent,160,"Auto-Compactor block preserves enchanted target progress");
  for(String id:List.of("ENCHANTED_COBBLESTONE","ENCHANTED_WHEAT","ENCHANTED_CARROT","ENCHANTED_POTATO","ENCHANTED_SUGAR","ENCHANTED_NETHER_STALK","ENCHANTED_RAW_FISH","ENCHANTED_RAW_SALMON","ENCHANTED_BONE","ENCHANTED_ROTTEN_FLESH","ENCHANTED_ENDER_PEARL","ENCHANTED_OAK_LOG")){
   var item=c.items.get(id);check(item!=null,"category fixture exists: "+id);var route=item.routes.getFirst();Map<String,Long> ingredients=new HashMap<>();for(var in:route.inputs)ingredients.merge(in.id,in.count,Long::sum);
   var goal=new Book.Goal(id);eq(engine.evaluate(goal,ingredients,Map.of(),Map.of()).fraction(),1,"category recipe inputs fill "+id);
  }
  var repeat=new Book.Goal("ENCHANTED_DIAMOND");repeat.freshCraft=true;eq(engine.evaluate(repeat,Map.of("ENCHANTED_DIAMOND",1L),Map.of(),Map.of()).equivalent,0,"repeat craft cannot disassemble existing target to credit itself");
  var sheet=new Book();var saved=new Book.Goal("ENCHANTED_DIAMOND_BLOCK");saved.visible=false;saved.hudPage=3;saved.materialDisplay="Enchanted";saved.gemDisplay="Perfect";saved.hud=new HudLayout();saved.hud.x=.25;sheet.goals.add(saved);
  Path file=Files.createTempDirectory("rh-book-test").resolve("book.json");sheet.write(file);var loaded=Book.read(file).goals.getFirst();check(!loaded.visible&&loaded.hudPage==3&&loaded.materialDisplay.equals("Enchanted")&&loaded.gemDisplay.equals("Perfect"),"goal visibility/page/units survive restart");eq(loaded.hud.x,.25,"HUD placement persisted");Files.delete(file);Files.delete(file.getParent());
  int routes=0,unknown=0,graphs=0;List<String> issues=new ArrayList<>();
  for(var item:c.items.values()){
   check(!item.id.toLowerCase(Locale.ROOT).contains("netherite"),"SkyBlock only");
   for(var r:item.routes){routes++;Book.Goal alternate=new Book.Goal(item.id);alternate.choices.put(item.id,item.routes.indexOf(r));var alternative=engine.evaluate(alternate,Map.of(),Map.of(),Map.of());check(Double.isFinite(alternative.equivalent),"selected alternative route evaluated "+item.id);for(var in:r.inputs){check(in.count>0,"positive quantity "+item.id);if(!c.items.containsKey(in.id)&&!in.id.equals("SKYBLOCK_COIN")){unknown++;issues.add(item.id+" -> "+in.id);}}}
   try{var node=engine.evaluate(new Book.Goal(item.id),Map.of(),Map.of(),Map.of());check(Double.isFinite(node.equivalent)&&node.equivalent>=0&&node.equivalent<=node.total,"finite bounded progress "+item.id);graphs++;}catch(ArithmeticException|IllegalArgumentException ex){issues.add(item.id+": "+ex.getMessage());}
  }
  Path report=Path.of("build/reports/catalogue-progress.txt");Files.createDirectories(report.getParent());Files.writeString(report,"Items: "+c.items.size()+"\nRoutes: "+routes+"\nEvaluated graphs: "+graphs+"\nUnknown inputs: "+unknown+"\n"+String.join("\n",issues)+"\nSynthetic fixtures only; live Hypixel acceptance remains unverified.\n");
  check(unknown==0,"all ingredient IDs resolve; see catalogue-progress.txt");
  // Armor set combines real piece recipes into one shared pool and one HUD row per material.
  var fd=ArmorSets.detect(c,"FINAL_DESTINATION_HELMET");
  check(fd.size()==4,"Final Destination has four real armor pieces");
  var fdSet=new Book.Goal(ArmorSets.id(c.item(fd.getFirst()).name));
  fdSet.armorPieces.addAll(fd);
  check(ArmorSets.register(c,fdSet),"register four-piece Final Destination set");
  var fdTree=engine.evaluate(fdSet,Map.of(),Map.of(),Map.of());
  check(fdTree.children.size()==4,"all four recipes remain in dependency tree");
  var fdSummary=engine.displayed(fdTree,fdSet);
  check(fdSummary.stream().map(row->row.id).distinct().count()==fdSummary.size(),"armor materials appear once in shared HUD");
  for(var expected:List.of("NULL_SPHERE","ENCHANTED_OBSIDIAN","ENCHANTED_REDSTONE"))
   check(fdSummary.stream().anyMatch(row->row.id.contains(expected)),"armor summary contains "+expected);
  var fdOwned=engine.evaluate(fdSet,Map.of("ENCHANTED_OBSIDIAN",96L),Map.of(),Map.of());
  var obs=engine.displayed(fdOwned,fdSet).stream().filter(n->n.id.equals("ENCHANTED_OBSIDIAN")).findFirst().orElseThrow();
  eq(obs.physical,96,"96 owned obsidian is credited only once across entire armor set");
  check(obs.total>96,"armor obsidian total combines multiple pieces");
  check(ArmorSets.readyPieces(fdOwned)==0,"partial materials are not marked four pieces ready");
  fdSet.count=2;
  var twice=engine.evaluate(fdSet,Map.of(),Map.of(),Map.of());
  var original=fdSummary.stream().filter(n->n.id.equals("ENCHANTED_OBSIDIAN")).findFirst().orElseThrow();
  var doubled=engine.displayed(twice,fdSet).stream().filter(n->n.id.equals("ENCHANTED_OBSIDIAN")).findFirst().orElseThrow();
  eq(doubled.total,original.total*2,"two armor sets double ingredient requirements");
  var bookWithSet=new Book();bookWithSet.goals.add(fdSet);
  var tempSet=Files.createTempDirectory("rh-armor-goal").resolve("book.json");
  bookWithSet.write(tempSet);var loadedSet=Book.read(tempSet).goals.getFirst();
  check(loadedSet.armorPieces.equals(fd)&&ArmorSets.isSet(loadedSet),"armor set persists and restores all four IDs");
  Files.delete(tempSet);Files.delete(tempSet.getParent());
  System.out.println("PASS: "+checks+" progress/ledger checks; "+graphs+" catalogue graphs; "+routes+" routes");
 }
}
