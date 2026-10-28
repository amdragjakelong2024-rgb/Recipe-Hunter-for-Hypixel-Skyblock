package dev.recipehunter;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Drives the production chat parser, sack balances and HUD refresh path without a server. */
final class LiveTrackingTest {
 static void run(Catalog c)throws Exception{
  for(String target:List.of("ENCHANTED_MITHRIL","ENCHANTED_COBBLESTONE","ENCHANTED_WHEAT","ENCHANTED_CARROT","ENCHANTED_POTATO","ENCHANTED_SUGAR","ENCHANTED_NETHER_STALK","ENCHANTED_RAW_FISH","ENCHANTED_RAW_SALMON","ENCHANTED_BONE","ENCHANTED_ROTTEN_FLESH","ENCHANTED_ENDER_PEARL","ENCHANTED_OAK_LOG")){
   var route=c.item(target).routes.getFirst();PlannerTest.check(route.inputs.size()==1,"single material live fixture "+target);var in=route.inputs.getFirst();
   Book book=new Book();var goal=new Book.Goal(target);goal.count=100;book.goals.add(goal);MenuTracker menus=new MenuTracker();
   var empty=LiveProgress.refresh(c,book,Map.of(),List.of());ProgressTest.eq(empty.trees().get(target).equivalent,0,"empty fixture "+target);
   var pickup=LiveProgress.refresh(c,book,Map.of(in.id,in.count),List.of());PlannerTest.check(pickup.trees().get(target).equivalent>0,"inventory acquisition updates HUD tree "+target);
   var message=Component.literal("[Sacks] +"+in.count+" items. (Last 10s.)").withStyle(s->s.withHoverEvent(new HoverEvent.ShowText(Component.literal("Added items:\n +"+in.count+" "+c.item(in.id).name+" (Mining Sack)"))));
   menus.sackMessage(message.getString(),SackChatTracker.changes(message,c));
   var deposited=LiveProgress.refresh(c,book,menus.stock(),List.of());ProgressTest.eq(deposited.trees().get(target).equivalent,pickup.trees().get(target).equivalent,"inventory to sack keeps progress "+target);
   long half=in.count/2;
   var removed=Component.literal("[Sacks] -"+half+" items. (Last 10s.)").withStyle(s->s.withHoverEvent(new HoverEvent.ShowText(Component.literal("Removed items:\n "+half+" "+c.item(in.id).name+" (Fishing Sack)"))));
   menus.sackMessage(removed.getString(),SackChatTracker.changes(removed,c));var pool=new HashMap<>(menus.stock());pool.merge(in.id,half,Long::sum);
   ProgressTest.eq(LiveProgress.refresh(c,book,pool,List.of()).trees().get(target).equivalent,pickup.trees().get(target).equivalent,"withdrawal is not a second acquisition "+target);
   var compacted=LiveProgress.refresh(c,book,Map.of(target,route.count),List.of());ProgressTest.eq(compacted.trees().get(target).equivalent,pickup.trees().get(target).equivalent,"auto compactor retains progress "+target);
   PlannerTest.check(!deposited.rows().get(target).isEmpty(),"HUD rows emitted "+target);
   menus.clear();ProgressTest.eq(LiveProgress.refresh(c,book,menus.stock(),List.of()).trees().get(target).equivalent,0,"reconnect clears sack evidence "+target);
  }
  MenuTracker balances=new MenuTracker();balances.observeSack("GLACITE_JEWEL",60,1);
  PlannerTest.check(balances.serverBalance("You have no Glacite Jewel in your Sacks!",c),"server zero message recognized");
  PlannerTest.eq(balances.stock().getOrDefault("GLACITE_JEWEL",-1L),0,"server zero replaces stale balance");
  PlannerTest.eq(MenuTracker.stored("Stored: 1.5k / 20k"),-1,"abbreviated quantities are unknown, never guessed");
  PlannerTest.check(MenuTracker.enderPage("Ender Chest (1/9)").equals(MenuTracker.enderPage("Ender Chest (1/5)")),"page capacity change does not duplicate storage");
  Book book=new Book();var goal=new Book.Goal("ENCHANTED_MITHRIL");goal.count=960;book.goals.add(goal);
  var tiny=LiveProgress.refresh(c,book,Map.of("MITHRIL_ORE",1L),List.of());PlannerTest.check(tiny.rows().get(goal.id).stream().anyMatch(r->r.equivalent()>0&&!r.percent().equals("0%")),"one mined Mithril updates visible sub-percent progress");
  var job=CraftDiary.parse(c,"forge 2 Refined Mithril 4h",1000);book.goals.clear();book.goals.add(new Book.Goal("REFINED_MITHRIL"));
  var pending=LiveProgress.refresh(c,book,Map.of(),List.of(job));ProgressTest.eq(pending.trees().get("REFINED_MITHRIL").equivalent,0,"forge is not physical stock");PlannerTest.check(pending.trees().get("REFINED_MITHRIL").forge>0,"forge visible in live refresh");
  // A Forge job is planned output, never a phantom physical item.
  Book forgeBook=new Book();var forgedGoal=new Book.Goal("REFINED_MITHRIL");
  forgedGoal.materialDisplay="Enchanted";forgeBook.goals.add(forgedGoal);
  var working=LiveProgress.refresh(c,forgeBook,Map.of(),List.of(CraftDiary.parse(c,"forge 1 Refined Mithril 4h",1000)));
  var forging=working.trees().get("REFINED_MITHRIL");
  ProgressTest.eq(forging.equivalent,0,"active Forge job never becomes loose stock");
  ProgressTest.eq(forging.inForge,1,"active Forge output credited only to queued progress");
  ProgressTest.eq(forging.remaining(),0,"queued output removes remaining grind demand");
  var forgingRows=working.rows().get("REFINED_MITHRIL");
  PlannerTest.check(forgingRows.stream().anyMatch(r->r.inForge()>0&&r.have()==0&&r.stillNeeded()==0),"Forge ingredients show as in progress, not 8k uncollected");
  var claimedForge=LiveProgress.refresh(c,forgeBook,Map.of("REFINED_MITHRIL",1L),List.of());
  ProgressTest.eq(claimedForge.trees().get("REFINED_MITHRIL").equivalent,1,"claim becomes owned only when observed in inventory");
  ProgressTest.eq(claimedForge.trees().get("REFINED_MITHRIL").inForge,0,"claim clears in-progress bar");
  // Verify every Forge route, rather than patching one mining recipe.
  int forgeRoutes=0;
  for(var item:c.items.values())for(int index=0;index<item.routes.size();index++){
   var route=item.routes.get(index);if(!"forge".equals(route.type))continue;
   var g=new Book.Goal(item.id);g.count=route.count;g.choices.put(item.id,index);
   var projected=new RecipeProgress(c).evaluate(g,Map.of(),Map.of(item.id,route.count),Map.of(item.id,route.count));
   ProgressTest.eq(projected.equivalent,0,"no phantom Forge output "+item.id);
   ProgressTest.eq(projected.inForge,route.count,"Forge output projected across route "+item.id);
   ProgressTest.eq(projected.remaining(),0,"Forge output not requested again "+item.id);
   forgeRoutes++;
  }
  PlannerTest.check(forgeRoutes>=100,"whole Forge catalogue covered");
  Book repeatBook=new Book();var repeat=new Book.Goal("ENCHANTED_MITHRIL");repeat.freshCraft=true;repeat.count=10;repeat.checked.add("MITHRIL_ORE");repeat.owned.put("MITHRIL_ORE",999999L);repeatBook.goals.add(repeat);
  ProgressTest.eq(LiveProgress.refresh(c,repeatBook,Map.of("ENCHANTED_MITHRIL",10L),List.of()).trees().get(repeat.id).equivalent,0,"old crafted item, checklist and manual override cannot fill a new craft");
  ProgressTest.eq(LiveProgress.refresh(c,repeatBook,Map.of("ENCHANTED_MITHRIL",10L,"MITHRIL_ORE",160L),List.of()).trees().get(repeat.id).equivalent,1,"newly gathered materials count toward repeat craft");
  var hidden=new Book.Goal("ENCHANTED_MITHRIL");hidden.count=960;hidden.visible=false;Book queue=new Book();queue.goals.add(hidden);queue.goals.add(new Book.Goal("REFINED_MITHRIL"));
  PlannerTest.check(LiveProgress.refresh(c,queue,Map.of("MITHRIL_ORE",160L),List.of()).trees().get("REFINED_MITHRIL").equivalent>0,"hidden goal cannot consume visible goal progress");
  hidden.visible=true;PlannerTest.check(LiveProgress.refresh(c,queue,Map.of("MITHRIL_ORE",160L),List.of()).trees().get("REFINED_MITHRIL").equivalent>0,"another visible goal cannot freeze this goal counter");
  var translated=Component.translatable("chat.type.text",Component.literal("Server"),Component.literal("[Sacks] +16 items.").withStyle(s->s.withHoverEvent(new HoverEvent.ShowText(Component.literal("Added items:\n16 Mithril")))));
  PlannerTest.eq(SackChatTracker.changes(translated,c).getOrDefault("MITHRIL_ORE",0L),16,"nested translated Sack hover without category suffix");
  // Snapshot/message overlap must not permanently freeze all later mining gains.
  MenuTracker overlap=new MenuTracker();overlap.sacks.put("MITHRIL_ORE",1000L);
  overlap.sackSnapshots.put("MITHRIL_ORE",System.currentTimeMillis());
  overlap.sackMessage("[Sacks] +160 items. (Last 10s.)",Map.of("MITHRIL_ORE",160L));
  PlannerTest.check(!overlap.stock().containsKey("MITHRIL_ORE"),"overlap does not invent a total");
  overlap.sackSnapshots.put("MITHRIL_ORE",System.currentTimeMillis()-20000);
  overlap.sackMessage("[Sacks] +160 items. (Last 10s.)",Map.of("MITHRIL_ORE",160L));
  PlannerTest.eq(overlap.stock().getOrDefault("MITHRIL_ORE",0L),160,"next unambiguous batch resumes mining progress");
  overlap.sackMessage("[Sacks] +160 items. (Last 10s.)",Map.of("MITHRIL_ORE",160L));
  PlannerTest.eq(overlap.stock().getOrDefault("MITHRIL_ORE",0L),320,"subsequent mining batch increases progress again");
  PlannerTest.check(!overlap.sackSnapshots.containsKey("MITHRIL_ORE"),"recovery does not claim a synced absolute total");
  // The initial output is excluded, but newly compacted output must remain counted.
  Book cycles=new Book();Book.Goal cycle=new Book.Goal("ENCHANTED_MITHRIL");cycle.count=10;
  cycles.restart(cycle,Map.of(cycle.id,10L));
  var raw=LiveProgress.refresh(c,cycles,Map.of(cycle.id,10L,"MITHRIL_ORE",160L),List.of());
  var compacted=LiveProgress.refresh(c,cycles,Map.of(cycle.id,11L),List.of());
  ProgressTest.eq(raw.trees().get(cycle.id).equivalent,1,"repeat mines raw above old finished output");
  ProgressTest.eq(compacted.trees().get(cycle.id).equivalent,1,"repeat preserves new auto-compacted target output");
  ProgressTest.eq(new Planner(c).goal(cycle,Map.of(cycle.id,11L)).missing.get(cycle.id),9,"planner uses same target baseline");
  cycles.finish(cycle);PlannerTest.check(cycles.find(cycle.id)==cycle&&cycle.finished&&!cycle.visible,"finish retains goal for reset");
  cycles.restart(cycle,Map.of(cycle.id,20L));
  PlannerTest.check(!cycle.finished&&cycle.visible&&cycle.crafted==0&&cycle.count==10&&!cycles.completed.contains(cycle.id),"reset keeps quantity and clears completion");
  ProgressTest.eq(LiveProgress.refresh(c,cycles,Map.of(cycle.id,20L),List.of()).trees().get(cycle.id).equivalent,0,"reset does not reuse finished copies");
  ProgressTest.eq(LiveProgress.refresh(c,cycles,Map.of(cycle.id,20L,"MITHRIL_ORE",160L),List.of()).trees().get(cycle.id).equivalent,1,"mining resumes after reset");
  var saved=java.nio.file.Files.createTempDirectory("rh-reset").resolve("book.json");cycles.write(saved);var loaded=Book.read(saved);
  PlannerTest.eq(loaded.find(cycle.id).targetBaseline,20,"restart baseline survives restart");
  System.out.println("PASS: live inventory, itemized Sack chat, transfers, compaction and HUD recomputation fixtures across 13 material families");
 }
}
