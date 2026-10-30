package dev.recipehunter;
import java.util.*;
/** Stable rising edges only; no repeated messages every tick. */
public final class ProgressAlerts {
 private final Map<String,Boolean> craftReady=new HashMap<>();private final Map<String,Integer> stable=new HashMap<>();
 private final Map<String,Long> lastAlert=new HashMap<>();
 private final Set<String> finished=new HashSet<>();private boolean wasFull;private int fullScans;
 public void reset(){craftReady.clear();stable.clear();finished.clear();lastAlert.clear();wasFull=false;fullScans=0;}
 public List<String> update(Catalog c,Book book,Map<String,Long> stock,Collection<RecipeProgress.Node> trees,MenuTracker menus,long now){
  List<String> out=new ArrayList<>();Set<String> keys=new HashSet<>();
  for(String id:CraftSteps.targets(trees))for(int i=0;i<c.item(id).routes.size();i++){
   var r=c.item(id).routes.get(i);if(!Set.of("crafting","forge").contains(r.type)||r.inputs.isEmpty())continue;
   var step=CraftSteps.step(c,id,i,stock);String key=id+":"+i;keys.add(key);boolean ready=step.batches()>0;
   if(!ready){stable.remove(key);craftReady.put(key,false);continue;}
   int samples=stable.merge(key,1,Integer::sum);
   if(samples>=3&&!craftReady.getOrDefault(key,false)){craftReady.put(key,true);if(now-lastAlert.getOrDefault(key,Long.MIN_VALUE/2)<30000)continue;lastAlert.put(key,now);out.add("Materials ready: "+r.count+"x "+c.item(id).name+(r.type.equals("forge")?" — start in Forge":" — craft available")+" (check unlocks)");}
  }
  stable.keySet().retainAll(keys);craftReady.keySet().retainAll(keys);
  Set<String> live=new HashSet<>();for(var e:ForgeState.active(menus.forge)){
   String key=e.key+":"+e.id+":"+e.createdAt;live.add(key);
   if(e.readyAt>0&&e.readyAt<=now&&finished.add(key))out.add("Forge ready to collect: "+e.count+"x "+c.item(e.id).name);
  }finished.retainAll(live);
  boolean full=menus.forgeState.full(menus.forge);fullScans=full?fullScans+1:0;
  if(!full)wasFull=false;if(fullScans>=3&&!wasFull){wasFull=true;out.add("Forge is full — all observed unlocked slots are occupied.");}
  return out;
 }
}
