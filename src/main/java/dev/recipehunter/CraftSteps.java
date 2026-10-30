package dev.recipehunter;
import java.util.*;
/** Recipe-derived next steps. Mixed recipes require every ingredient; queued output is not spendable. */
public final class CraftSteps {
 public record Step(String id,int route,long output,Map<String,Long> required,Map<String,Long> have,long batches){}
 public static Step step(Catalog c,String id,int index,Map<String,Long> stock){
  var r=c.item(id).routes.get(index);Map<String,Long> need=new LinkedHashMap<>(),have=new LinkedHashMap<>();
  for(var in:r.inputs)need.merge(in.id,in.count,Math::addExact);
  long batches=need.isEmpty()?0:Long.MAX_VALUE;
  for(var e:need.entrySet()){long n=Math.max(0,stock.getOrDefault(e.getKey(),0L));have.put(e.getKey(),n);batches=Math.min(batches,e.getValue()>0?n/e.getValue():0);}
  return new Step(id,index,r.count,need,have,batches);
 }
 public static List<Step> next(Catalog c,String input,Map<String,Long> stock,Set<String> preferred){
  List<Step> out=new ArrayList<>();for(String id:c.uses.getOrDefault(input,List.of()))for(int i=0;i<c.item(id).routes.size();i++){
   var r=c.item(id).routes.get(i);if(!Set.of("crafting","forge").contains(r.type)||r.inputs.stream().noneMatch(in->in.id.equals(input)))continue;
   // Exclude dismantling cycles. Do not assume every item with 'enchanted' in its name is a conversion.
   if(c.item(input).routes.stream().anyMatch(back->back.inputs.stream().anyMatch(in->in.id.equals(id))))continue;
   out.add(step(c,id,i,stock));
  }
  out.sort(Comparator.<Step>comparingInt(v->preferred.contains(v.id)?0:1).thenComparingInt(v->v.required.size()).thenComparing(v->c.item(v.id).name));return out;
 }
 public static Set<String> targets(Collection<RecipeProgress.Node> roots){Set<String> out=new LinkedHashSet<>();for(var n:roots)collect(n,out);return out;}
 private static void collect(RecipeProgress.Node n,Set<String> out){out.add(n.id);for(var child:n.children)collect(child,out);}
}
