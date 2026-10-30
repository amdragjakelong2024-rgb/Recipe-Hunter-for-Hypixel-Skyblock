package dev.recipehunter;
import java.util.*;
/** Allocate each observed item once. Only ordinary crafting can prepare missing inputs; Forge output must exist. */
public final class CraftReadiness {
 public record Result(String id,long output,Map<String,Long> required,Map<String,Long> owned,Map<String,Long> available,boolean ready){}
 private final Catalog c;private final Map<String,Integer> choices;private int calls;
 private CraftReadiness(Catalog c,Map<String,Integer> choices){this.c=c;this.choices=choices;}
 private boolean consume(String id,long n,Map<String,Long> pool,Set<String> path){
  if(++calls>4000||path.size()>32)return false;
  long owned=Math.max(0,pool.getOrDefault(id,0L)),take=Math.min(n,owned);pool.put(id,owned-take);n-=take;if(n==0)return true;
  var item=c.item(id);if(item.routes.isEmpty()||!path.add(id))return false;
  var r=item.routes.get(Math.floorMod(choices.getOrDefault(id,0),item.routes.size()));
  if(!"crafting".equals(r.type)||r.count<1||r.inputs.isEmpty())return false;
  long batches=(n-1)/r.count+1;for(var in:r.inputs)if(!consume(in.id,Math.multiplyExact(in.count,batches),pool,path))return false;
  pool.merge(id,Math.multiplyExact(r.count,batches)-n,Long::sum);path.remove(id);return true;
 }
 public static Result assess(Catalog c,String id,Map<String,Integer> choices,Map<String,Long> stock){
  var item=c.item(id);if(item.routes.isEmpty())return new Result(id,1,Map.of(),Map.of(),Map.of(),false);
  var r=item.routes.get(Math.floorMod(choices.getOrDefault(id,0),item.routes.size()));Map<String,Long> required=new LinkedHashMap<>(),owned=new LinkedHashMap<>(),available=new LinkedHashMap<>();
  for(var in:r.inputs)required.merge(in.id,in.count,Math::addExact);
  Map<String,Long> pool=new HashMap<>(stock);boolean ready=!required.isEmpty()&&Set.of("crafting","forge","armor_set").contains(r.type);
  for(var e:required.entrySet()){
   long n=e.getValue();owned.put(e.getKey(),Math.max(0,stock.getOrDefault(e.getKey(),0L)));Map<String,Long> trial=new HashMap<>(pool);
   boolean enough=new CraftReadiness(c,choices).consume(e.getKey(),n,trial,new HashSet<>());
   if(enough){available.put(e.getKey(),n);pool=trial;continue;}
   ready=false;long low=0,high=n;
   while(low<high){long mid=low+(high-low+1)/2;trial=new HashMap<>(pool);if(new CraftReadiness(c,choices).consume(e.getKey(),mid,trial,new HashSet<>()))low=mid;else high=mid-1;}
   available.put(e.getKey(),low);if(low>0)new CraftReadiness(c,choices).consume(e.getKey(),low,pool,new HashSet<>());
  }
  return new Result(id,r.count,required,owned,available,ready);
 }
 public static String next(Catalog c,String ingredient,Book.Goal goal,RecipeProgress.Node tree){
  if(tree!=null){String parent=parent(tree,ingredient);if(!parent.isEmpty())return parent;}
  var candidates=CraftSteps.next(c,ingredient,Map.of(),Set.of());return candidates.isEmpty()?"":candidates.getFirst().id();
 }
 private static String parent(RecipeProgress.Node node,String ingredient){
  if(!node.id.startsWith(ArmorSets.PREFIX)&&node.children.stream().anyMatch(n->n.id.equals(ingredient)))return node.id;
  for(var child:node.children){String found=parent(child,ingredient);if(!found.isEmpty())return found;}return "";
 }
}
