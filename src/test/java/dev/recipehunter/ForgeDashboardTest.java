package dev.recipehunter;
import java.util.*;
import java.nio.file.*;
final class ForgeDashboardTest {
 static void run(Catalog c)throws Exception{
  long now=System.currentTimeMillis();var menus=new MenuTracker();
  var a=ForgeTracking.job(c,1,"Refined Mithril","Time Remaining: 6h",now);
  var b=ForgeTracking.job(c,2,"Mithril Plate","Time Remaining: 18h",now);
  menus.observeForge(1,a,ForgeState.Slot.BUSY,"test");menus.observeForge(1,a,ForgeState.Slot.BUSY,"test");
  menus.observeForge(2,b,ForgeState.Slot.BUSY,"test");menus.observeForge(2,b,ForgeState.Slot.BUSY,"test");
  PlannerTest.eq(ForgeState.waitMillis(menus.forge,now),18*3600000L,"concurrent wait is max, not sum");
  PlannerTest.check(!menus.forgeState.full(menus.forge),"unseen slots cannot imply full Forge");
  for(int i=3;i<=7;i++){menus.observeForge(i,null,ForgeState.Slot.LOCKED,"test");menus.observeForge(i,null,ForgeState.Slot.LOCKED,"test");}
  PlannerTest.check(menus.forgeState.full(menus.forge),"all known unlocked slots occupied");
  PlannerTest.eq(ForgeState.waitMillis(menus.forge,now+19*3600000L),0,"clock finishes without reopening menu");
  Book book=new Book();var alerts=new ProgressAlerts();
  PlannerTest.check(alerts.update(c,book,Map.of(),List.of(),menus,now).isEmpty(),"full warning waits for stable state");
  alerts.update(c,book,Map.of(),List.of(),menus,now);
  PlannerTest.check(alerts.update(c,book,Map.of(),List.of(),menus,now).stream().anyMatch(t->t.contains("full")),"full warning edge");
  PlannerTest.check(alerts.update(c,book,Map.of(),List.of(),menus,now).isEmpty(),"full warning not repeated");
  PlannerTest.eq(alerts.update(c,book,Map.of(),List.of(),menus,now+19*3600000L).size(),2,"both ready jobs notified");
  PlannerTest.check(alerts.update(c,book,Map.of(),List.of(),menus,now+19*3600000L).isEmpty(),"ready notifications once per job");
  menus.claimed(book,a.id,1);PlannerTest.check(!menus.forgeState.full(menus.forge),"claimed inactive slot not full");
  var root=new RecipeProgress(c).evaluate(new Book.Goal("REFINED_MITHRIL"),Map.of(),Map.of(),Map.of());
  alerts.reset();menus.clear();alerts.update(c,book,Map.of("ENCHANTED_MITHRIL",159L),List.of(root),menus,now);
  for(int i=0;i<2;i++)PlannerTest.check(alerts.update(c,book,Map.of("ENCHANTED_MITHRIL",160L),List.of(root),menus,now).isEmpty(),"threshold debounced");
  PlannerTest.check(alerts.update(c,book,Map.of("ENCHANTED_MITHRIL",160L),List.of(root),menus,now).stream().anyMatch(t->t.contains("Refined Mithril")),"first Refined Mithril notification at 160");
  PlannerTest.check(alerts.update(c,book,Map.of("ENCHANTED_MITHRIL",160L),List.of(root),menus,now).isEmpty(),"craft notification not repeated");
  var next=CraftSteps.next(c,"ENCHANTED_MITHRIL",Map.of("ENCHANTED_MITHRIL",159L),Set.of("REFINED_MITHRIL"));
  PlannerTest.check(next.getFirst().id().equals("REFINED_MITHRIL"),"goal-related next craft prioritized");PlannerTest.eq(next.getFirst().batches(),0,"159 is not 160");
  PlannerTest.eq(CraftSteps.step(c,"MITHRIL_PLATE",0,Map.of("REFINED_MITHRIL",5L)).batches(),0,"five Refined Mithril alone cannot make a mixed plate");
  for(String id:List.of("ENCHANTED_MELON","ENCHANTED_MELON_BLOCK","ENCHANTED_CARROT","ENCHANTED_GOLDEN_CARROT","REFINED_MITHRIL")){
   var route=c.item(id).routes.getFirst();var input=route.inputs.getFirst();
   PlannerTest.check(CraftSteps.next(c,input.id,Map.of(input.id,input.count),Set.of(id)).stream().anyMatch(x->x.id().equals(id)&&x.batches()==1),"actual conversion chain "+id);
  }
  int routes=0;for(var item:c.items.values())for(int i=0;i<item.routes.size();i++){
   var r=item.routes.get(i);if(!Set.of("forge","crafting").contains(r.type)||r.inputs.isEmpty())continue;
   Map<String,Long> stock=new HashMap<>();for(var in:r.inputs)stock.merge(in.id,in.count,Long::sum);
   PlannerTest.eq(CraftSteps.step(c,item.id,i,stock).batches(),1,"every recipe's full ingredient batch "+item.id);
   String key=stock.keySet().iterator().next();stock.put(key,stock.get(key)-1);
   PlannerTest.eq(CraftSteps.step(c,item.id,i,stock).batches(),0,"one missing ingredient prevents readiness "+item.id);routes++;
  }
  menus.sacks.put("MITHRIL_ORE",123L);SackBridge.apply(menus,Map.of("MITHRIL_ORE",1000L),now);
  menus.sackMessage("[Sacks] +160 items.",Map.of("MITHRIL_ORE",160L));
  PlannerTest.eq(menus.stock().get("MITHRIL_ORE"),1000,"bridge replaces rather than adds or doubles server delta");
  SackBridge.apply(menus,Map.of("MITHRIL_ORE",1160L),now+1000);PlannerTest.eq(menus.stock().get("MITHRIL_ORE"),1160,"bridge live update");
  SackBridge.apply(menus,Map.of(),now+2000);PlannerTest.check(!menus.stock().containsKey("MITHRIL_ORE"),"bridge loses certainty without inventing totals");
  PlannerTest.check(SackBridge.itemId("internalName:ENCHANTED_MITHRIL").equals("ENCHANTED_MITHRIL"),"SkyHanni value class identity");
  PlannerTest.check(!SackBridge.exact("OUTDATED")&&!SackBridge.exact("ALRIGHT"),"approximate cached counts not imported");
  PlannerTest.eq(SackChanges.parse("Added items\n+160 Mithril (Mining Sack)").get("Mithril"),160,"signed hover lines tolerate header variants");
  var l=new ForgeLayout();l.move(-10,99999,320,240);PlannerTest.check(l.contains(l.left(320,240),l.top(320,240),320,240),"Forge hitbox includes painted boundary");
  PlannerTest.check(!l.contains(l.left(320,240)-1,l.top(320,240),320,240),"outside Forge does not intercept click");
  book.forgeVisible=false;book.forgeHud.x=.25;book.forgeHud.zoom=1.4;var path=Files.createTempDirectory("rh043").resolve("book.json");book.write(path);var read=Book.read(path);
  PlannerTest.check(!read.forgeVisible&&read.forgeHud.x==.25&&read.forgeHud.zoom==1.4,"Forge layout and visibility persist");Files.delete(path);Files.delete(path.getParent());
  System.out.println("PASS: Forge dashboard, readiness edges, Sack bridge and "+routes+" next-step routes");
 }
}
