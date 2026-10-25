package dev.recipehunter;
import java.util.*;
import java.util.regex.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;

/** Observed collection rates are primary evidence. Stats alone cannot determine travel/RNG. */
public final class GrindEstimates {
 public final Map<String,Double> stats=new TreeMap<>(),liveRates=new HashMap<>(),modelRates=new HashMap<>();
 private Map<String,Long> previous=Map.of();private final Map<String,Long> gained=new HashMap<>();
 private long since,last;private String context="";public boolean recording=false;
 public record Estimate(String id,long missing,double hours,double waitHours,String evidence,int difficulty) {
  public String label(Catalog c){return c.item(id).name+": "+Catalog.qty(missing)+" | "+(Double.isFinite(hours)?String.format(Locale.US,"~%.2f h active",hours):"time unknown")+(waitHours>0?String.format(Locale.US," + %.2f h base Forge wait",waitHours):"");}
 }
 public void reset(){previous=Map.of();gained.clear();liveRates.clear();modelRates.clear();stats.clear();since=last=0;recording=false;}
 public void start(){previous=Map.of();gained.clear();liveRates.clear();since=System.currentTimeMillis();last=0;recording=true;}
 public void tick(RecipeHunter app){
  long now=System.currentTimeMillis();if(now-last<1000)return;last=now;
  if(app.mc.screen instanceof AbstractContainerScreen<?> screen){
   String title=Catalog.normal(screen.getTitle().getString());
   if(title.equals("your skyblock profile")||title.equals("stats")||title.equals("your stats")||title.equals("mining stats"))
    for(var slot:screen.getMenu().slots){if(slot.container==app.mc.player.getInventory())continue;var lore=slot.getItem().get(DataComponents.LORE);if(lore==null)continue;
     for(var line:lore.lines()){Matcher m=Pattern.compile("(?i)(Mining Speed|Mining Fortune|Farming Fortune|Foraging Fortune|Pristine|Breaking Power)\\s*:?\\s*([0-9,]+(?:\\.[0-9]+)?)").matcher(line.getString().replaceAll("§.",""));if(m.find())stats.put(m.group(1).toLowerCase(Locale.ROOT),Double.parseDouble(m.group(2).replace(",","")));}
    }
  }
  if(!recording)return;
  // Any menu or location transition invalidates the sample: chest transfers/crafts are not grinding.
  if(app.mc.screen!=null||!context.equals(app.context)){previous=Map.of();context=app.context;since=now;gained.clear();return;}
  Map<String,Long> current=app.ownership.inventory(app.mc);
  if(!previous.isEmpty())for(var e:current.entrySet()){long delta=e.getValue()-previous.getOrDefault(e.getKey(),0L);if(delta>0)gained.merge(e.getKey(),delta,Long::sum);}
  previous=current;if(now-since<30000)return;
  gained.forEach((id,n)->liveRates.put(id,n*3600000.0/(now-since)));
 }
 public List<Estimate> rank(RecipeHunter app,Book.Goal goal,boolean easiest){
  List<Estimate> out=new ArrayList<>();for(var e:app.plan(goal).leaves.entrySet()){
   var full=app.planner.plan(e.getKey(),e.getValue(),Map.of(),goal.choices);double hours=0,wait=0;boolean known=true;String evidence="measured / entered rate";
   double direct=goal.rates.getOrDefault(e.getKey(),liveRates.getOrDefault(e.getKey(),modelRates.getOrDefault(e.getKey(),0.0)));
   if(direct>0){hours=e.getValue()/direct;if(modelRates.containsKey(e.getKey())&&!goal.rates.containsKey(e.getKey())&&!liveRates.containsKey(e.getKey()))evidence="approximate mining model; supplied assumptions";}else{
    for(var leaf:full.leaves.entrySet()){double rate=goal.rates.getOrDefault(leaf.getKey(),liveRates.getOrDefault(leaf.getKey(),modelRates.getOrDefault(leaf.getKey(),0.0)));if(rate<=0){known=false;break;}hours+=leaf.getValue()/rate;if(modelRates.containsKey(leaf.getKey())&&!goal.rates.containsKey(leaf.getKey())&&!liveRates.containsKey(leaf.getKey()))evidence="includes approximate mining model";}
    if(!known){hours=Double.POSITIVE_INFINITY;evidence="record a sample or enter rate";}
   }
   // Conservative serial base wait, explicitly separate from active collection.
   for(var craft:full.crafts.entrySet()){var r=full.routes.get(craft.getKey());wait+=r.seconds*craft.getValue()/3600.0;}
   String requirement=app.catalog.item(e.getKey()).requirement;
   int difficulty=(!Double.isFinite(hours)?2:0)+(requirement!=null&&!requirement.isBlank()?1:0);
   out.add(new Estimate(e.getKey(),e.getValue(),hours,wait,evidence,difficulty));
  }
  Comparator<Estimate> time=Comparator.comparingDouble(Estimate::hours).thenComparingDouble(Estimate::waitHours).thenComparing(Estimate::id);
  out.sort(easiest?Comparator.comparingInt(Estimate::difficulty).thenComparing(time):time);return out;
 }
}
