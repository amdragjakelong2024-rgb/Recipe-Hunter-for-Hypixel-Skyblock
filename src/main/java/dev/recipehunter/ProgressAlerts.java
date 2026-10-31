package dev.recipehunter;
import java.util.*;
/** Debounced rising edges with independent crafting, goal and Forge preferences. */
public final class ProgressAlerts {
 private final Map<String,Boolean> announced=new HashMap<>();private final Map<String,Integer> stable=new HashMap<>();private final Map<String,Long> lastAlert=new HashMap<>();
 private final Map<String,Boolean> readiness=new HashMap<>();private Map<String,Long> previousStock=Map.of();private String settings="";
 public void reset(){announced.clear();stable.clear();lastAlert.clear();readiness.clear();previousStock=Map.of();settings="";}
 private boolean edge(String key,boolean state,long now){if(!state){stable.remove(key);announced.put(key,false);return false;}int count=stable.merge(key,1,Integer::sum);if(count<3||announced.getOrDefault(key,false))return false;announced.put(key,true);if(now-lastAlert.getOrDefault(key,Long.MIN_VALUE/2)<30000)return false;lastAlert.put(key,now);return true;}
 public List<String> update(Catalog c,Book book,Map<String,Long> stock,Collection<RecipeProgress.Node> trees,MenuTracker menus,long now){
  String signature=book.goals.stream().map(g->g.id+g.choices+g.count+g.crafted+g.finished+g.visible).toList().toString();if(!stock.equals(previousStock)||!signature.equals(settings)){readiness.clear();previousStock=new HashMap<>(stock);settings=signature;}
  List<String> out=new ArrayList<>();Set<String> live=new HashSet<>();
  for(var goal:book.goals){
   String key="craft:"+goal.id;live.add(key);boolean ready=false;
   if(goal.visible&&!goal.finished){ready=readiness.computeIfAbsent(goal.id,id->CraftReadiness.canCraft(c,id,goal.choices,stock));}
   if(edge(key,ready,now)&&book.craftAlerts()&&book.triggerCraft())out.add("You can craft now "+c.item(goal.id).name);
   key="goal:"+goal.id;live.add(key);long owned=Math.max(0,stock.getOrDefault(goal.id,0L)-(goal.freshCraft?Math.max(0,goal.targetBaseline):0));
   boolean reached=goal.finished||goal.crafted>=goal.count||owned>=goal.count;
   if(ArmorSets.isSet(goal))reached=goal.armorPieces.stream().allMatch(id->stock.getOrDefault(id,0L)>=goal.count)||goal.finished;
   if(goal.finished&&!announced.containsKey(key))announced.put(key,true);
   if(edge(key,reached,now)&&book.craftAlerts()&&book.triggerGoal())out.add("Goal reached: "+c.item(goal.id).name);
  }
  for(var e:ForgeState.active(menus.forge)){
   String key="forge:"+e.key+":"+e.id+":"+e.createdAt;live.add(key);
   if(edge(key,e.readyVerified||e.readyAt>0&&e.readyAt<=now,now)&&book.forgeAlerts())out.add((e.readyVerified?"Ready to Collect: ":"Forge timer finished; verify in game: ")+c.item(e.id).name);
  }
  live.add("full");if(edge("full",menus.forgeState.full(menus.forge),now)&&book.forgeAlerts())out.add("Forge is full.");
  announced.keySet().retainAll(live);stable.keySet().retainAll(live);lastAlert.keySet().retainAll(live);return out;
 }
}
