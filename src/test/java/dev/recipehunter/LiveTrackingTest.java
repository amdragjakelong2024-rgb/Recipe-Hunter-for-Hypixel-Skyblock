package dev.recipehunter;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Drives the production chat parser, sack balances and HUD refresh path without a server. */
final class LiveTrackingTest {
 static void run(Catalog c){
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
  Book book=new Book();var goal=new Book.Goal("ENCHANTED_MITHRIL");goal.count=960;book.goals.add(goal);
  var tiny=LiveProgress.refresh(c,book,Map.of("MITHRIL_ORE",1L),List.of());PlannerTest.check(tiny.rows().get(goal.id).stream().anyMatch(r->r.equivalent()>0&&!r.percent().equals("0%")),"one mined Mithril updates visible sub-percent progress");
  var job=CraftDiary.parse(c,"forge 2 Refined Mithril 4h",1000);book.goals.clear();book.goals.add(new Book.Goal("REFINED_MITHRIL"));
  var pending=LiveProgress.refresh(c,book,Map.of(),List.of(job));ProgressTest.eq(pending.trees().get("REFINED_MITHRIL").equivalent,0,"forge is not physical stock");PlannerTest.check(pending.trees().get("REFINED_MITHRIL").forge>0,"forge visible in live refresh");
  Book repeatBook=new Book();var repeat=new Book.Goal("ENCHANTED_MITHRIL");repeat.freshCraft=true;repeat.count=10;repeat.checked.add("MITHRIL_ORE");repeat.owned.put("MITHRIL_ORE",999999L);repeatBook.goals.add(repeat);
  ProgressTest.eq(LiveProgress.refresh(c,repeatBook,Map.of("ENCHANTED_MITHRIL",10L),List.of()).trees().get(repeat.id).equivalent,0,"old crafted item, checklist and manual override cannot fill a new craft");
  ProgressTest.eq(LiveProgress.refresh(c,repeatBook,Map.of("ENCHANTED_MITHRIL",10L,"MITHRIL_ORE",160L),List.of()).trees().get(repeat.id).equivalent,1,"newly gathered materials count toward repeat craft");
  var hidden=new Book.Goal("ENCHANTED_MITHRIL");hidden.count=960;hidden.visible=false;Book queue=new Book();queue.goals.add(hidden);queue.goals.add(new Book.Goal("REFINED_MITHRIL"));
  PlannerTest.check(LiveProgress.refresh(c,queue,Map.of("MITHRIL_ORE",160L),List.of()).trees().get("REFINED_MITHRIL").equivalent>0,"hidden goal cannot consume visible goal progress");
  var translated=Component.translatable("chat.type.text",Component.literal("Server"),Component.literal("[Sacks] +16 items.").withStyle(s->s.withHoverEvent(new HoverEvent.ShowText(Component.literal("Added items:\n16 Mithril")))));
  PlannerTest.eq(SackChatTracker.changes(translated,c).getOrDefault("MITHRIL_ORE",0L),16,"nested translated Sack hover without category suffix");
  System.out.println("PASS: live inventory, itemized Sack chat, transfers, compaction and HUD recomputation fixtures across 13 material families");
 }
}
