package dev.recipehunter;
import java.nio.file.*;
import java.util.*;

/** Inventory -> running Forge -> ready -> claimed, through production parser and HUD models. */
public final class ForgeProgressTest {
 static int checks;
 static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 static void eq(double a,double b,String label){check(Math.abs(a-b)<1e-6,label+": "+a+" != "+b);}
 static MaterialProgress.Row row(Catalog c,Book b,Map<String,Long> stock,List<CraftDiary.Entry> jobs,String id){return LiveProgress.refresh(c,b,stock,jobs).rows().get(b.goals.getFirst().id).stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();}
 public static void main(String[] args)throws Exception{run(new Catalog(Files.newInputStream(Path.of(args[0]))));}
 public static void run(Catalog c){
  Book b=new Book();var g=new Book.Goal("TITANIUM_DRILL_3");g.materialDisplay="Enchanted";b.goals.add(g);
  Map<String,Long> stock=new HashMap<>(Map.of("TITANIUM_DRILL_2",1L,"ENCHANTED_MITHRIL",960L));
  var before=row(c,b,stock,List.of(),"ENCHANTED_MITHRIL");eq(before.total(),8000,"starting demand");eq(before.remaining(),7040,"starting missing");
  List<CraftDiary.Entry> jobs=new ArrayList<>();
  for(int slot=1;slot<=5;slot++){
   var job=ForgeTracking.job(c,slot,"§9Refined Mithril","§7Time Remaining: §a5h 59m 58s",1000);
   check(job!=null,"real menu format recognized");ForgeTracking.replace(jobs,slot,job);
   stock.put("ENCHANTED_MITHRIL",960L-slot*160);
   var after=row(c,b,stock,jobs,"ENCHANTED_MITHRIL");eq(after.total(),8000-slot*160,"Forge subtracts committed ingredients from demand");eq(after.have(),960-slot*160,"never calls committed ingredients physical");eq(after.remaining(),before.remaining(),"start job preserves remaining grind");
  }
  var queued=row(c,b,stock,jobs,"ENCHANTED_MITHRIL");eq(queued.total(),7200,"five jobs leave 7200 total");check(queued.needed().contains("110.0 stacks"),"missing shown in stacks");
  for(int tick=0;tick<20;tick++){ForgeTracking.tab(c,jobs,"Forges:\n1) Refined Mithril: 5h 59m\n2) Refined Mithril: 5h 59m",2000+tick);eq(row(c,b,stock,jobs,"ENCHANTED_MITHRIL").total(),7200,"partial refresh never removes unseen jobs or adds duplicates");}
  ForgeTracking.tab(c,jobs,"Forges:\n1) Refined Mithril: Ready!",3000);eq(CraftDiary.pending(jobs).get("REFINED_MITHRIL"),5,"ready is still pending, not owned");
  ForgeTracking.tab(c,jobs,"Forges:\n1) EMPTY",4000);stock.put("REFINED_MITHRIL",1L);
  eq(row(c,b,stock,jobs,"ENCHANTED_MITHRIL").remaining(),7040,"claim preserves remaining grind");
  eq(row(c,b,stock,jobs,"ENCHANTED_MITHRIL").total(),7200,"claim cannot count output and its old slot twice");
  ForgeTracking.tab(c,jobs,"Player list\n2) EMPTY",5000);eq(jobs.size(),4,"non-Forge lines cannot clear jobs");
  check(ForgeTracking.job(c,1,"Refined Mithril","Duration: 6h\nClick to forge!",0)==null,"recipe preview is not a running job");
  check(ForgeTracking.job(c,1,"Refined Mithril","Time Remaining: nonsense",0)==null,"unknown time is not a job");
  Book compact=new Book();var plateGoal=new Book.Goal("MITHRIL_PLATE");compact.goals.add(plateGoal);
  var plateStock=Map.of("GOLDEN_PLATE",1L,"REFINED_TITANIUM",1L,"ENCHANTED_IRON_BLOCK",1L);
  var refining=new CraftDiary.Entry();refining.id="REFINED_MITHRIL";refining.count=5;
  var engine=new RecipeProgress(c);var compactTree=engine.evaluate(plateGoal,plateStock,CraftDiary.pending(List.of(refining)),CraftDiary.pending(List.of(refining)));
  eq(compactTree.acquisition.remaining(),0,"pending child jobs cover ready-to-forge parent demand");eq(compactTree.acquisition.equivalent,0,"pending children never become actual finished output");
  var observed=new ArrayList<CraftDiary.Entry>();var watcher=new ForgeTracking.Observations(observed);
  var watched=ForgeTracking.job(c,1,"Refined Mithril","Time Remaining: 1h",0);
  watcher.accept(1,watched);eq(observed.size(),0,"one transient frame is not accepted");watcher.accept(1,watched);eq(observed.size(),1,"stable slot accepted");
  watcher.accept(1,null);watcher.accept(1,watched);eq(observed.size(),1,"transient empty slot preserves work");
  observed.clear();watcher.accept(1,watched);watcher.accept(1,watched);eq(observed.size(),0,"unchanged slot cannot resurrect claimed output");
  watcher.accept(1,null);watcher.accept(1,null);watcher.accept(1,watched);watcher.accept(1,watched);eq(observed.size(),1,"new job after empty slot is tracked");
  // Every recorded Forge route, including multi-input and batched-output recipes.
  int routes=0;
  for(var item:c.items.values())for(var route:item.routes)if(route.type.equals("forge")){
   routes++;Book all=new Book();var goal=new Book.Goal(item.id);goal.count=route.count*2;goal.materialDisplay="Enchanted";goal.choices.put(item.id,item.routes.indexOf(route));all.goals.add(goal);
   var e=new CraftDiary.Entry();e.id=item.id;e.count=route.count;e.source="forge";
   var pendingRows=LiveProgress.refresh(c,all,Map.of(),List.of(e)).rows().get(item.id);
   var claimedRows=LiveProgress.refresh(c,all,Map.of(item.id,route.count),List.of()).rows().get(item.id);
   Map<String,Double> pendingNeed=new TreeMap<>(),claimedNeed=new TreeMap<>();
   for(var r:pendingRows){if(r.remaining()>0)pendingNeed.put(r.id(),r.remaining());eq(r.have(),0,"pending never physical "+item.id);}
   for(var r:claimedRows)if(r.remaining()>0)claimedNeed.put(r.id(),r.remaining());
   check(pendingNeed.equals(claimedNeed),"claim preserves all acquisition needs "+item.id+" "+pendingNeed+" != "+claimedNeed);
  }
  System.out.println("PASS: "+checks+" Forge lifecycle checks across "+routes+" Forge routes");
 }
}
