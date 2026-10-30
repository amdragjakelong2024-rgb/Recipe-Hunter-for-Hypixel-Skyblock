package dev.recipehunter;

import java.util.*;

/**
 * Shows material progress in the requested unit while crediting ingredients of
 * known one-input compression chains. This is an inventory equivalence estimate,
 * not a claim that a partial enchanted item already exists.
 */
public final class MaterialProgress {
 public record Row(String id,long have,long total,long pending,long forge,boolean checked,double equivalent,String conversion,double reserved,double inForge){
  public Row(String id,long have,long total,long pending,long forge,boolean checked,double equivalent,String conversion){this(id,have,total,pending,forge,checked,equivalent,conversion,0,0);}
  public double forgeFraction(){return total<=0?1:Math.clamp((equivalent+inForge)/total,0,1);}
  public double stillNeeded(){return remaining();}
  public double remaining(){return Math.max(0,total-equivalent-reserved);}
  public String needed(){double n=remaining();return n<64?String.format(Locale.US,"Need %,.0f",Math.ceil(n)):String.format(Locale.US,"Need %,.0f (~%,.1f stacks)",Math.ceil(n),n/64.0);}
  public Row(String id,long have,long total,long pending,long forge,boolean checked){
   this(id,have,total,pending,forge,checked,have,"");
  }
  public boolean done(){return equivalent>=total;}
  public double fraction(){return total<=0?1:Math.clamp(equivalent/total,0.0,1.0);}
  public String count(){return have+"/"+total+(conversion.isEmpty()?"":" (+"+conversion+")");}
  public String percent(){return percentage(equivalent,total);}
  public static String percentage(double have,long total){if(total<=0||have>=total)return "100%";if(!(have>0))return "0%";double n=100*(have/total);return n<1?"<1%":Math.min(99,(int)Math.floor(n))+"%";}

 }

 private record Requirement(String id,long total){}

 public static List<Row> rows(Book.Goal goal,Planner.Plan plan,Map<String,Long> stock,Map<String,Long> pending,Map<String,Long> forge){
  return rows(goal,plan,stock,pending,forge,null);
 }

 public static List<Row> rows(Book.Goal goal,Planner.Plan plan,Map<String,Long> stock,Map<String,Long> pending,Map<String,Long> forge,Catalog catalog){
  List<Requirement> requirements=new ArrayList<>();
  for(var e:plan.demand.entrySet()){
   if(e.getValue()>0&&!plan.routes.containsKey(e.getKey()))requirements.add(new Requirement(e.getKey(),e.getValue()));
  }
  return evaluate(requirements,goal,stock,pending,forge,catalog);
 }

 /** Used for direct ingredient detail rows that are not necessarily plan leaves. */
 public static Row single(String id,long total,Map<String,Long> stock,Catalog catalog,Map<String,Integer> choices){
  Book.Goal temporary=new Book.Goal(id);
  if(choices!=null)temporary.choices.putAll(choices);
  return evaluate(List.of(new Requirement(id,total)),temporary,stock,Map.of(),Map.of(),catalog).getFirst();
 }

 private static List<Row> evaluate(List<Requirement> requirements,Book.Goal goal,Map<String,Long> stock,
                                   Map<String,Long> pending,Map<String,Long> forge,Catalog catalog){
  Map<String,Long> remaining=new HashMap<>();
  stock.forEach((id,n)->{if(n!=null&&n>0)remaining.put(id,n);});

  // First reserve every explicitly requested unit. Only surplus lower tiers can
  // contribute to a higher one, so the same piece is never counted twice.
  Map<String,Long> direct=new HashMap<>();
  for(Requirement r:requirements){
   long available=remaining.getOrDefault(r.id,0L);
   long reserved=Math.min(r.total,available);
   direct.merge(r.id,reserved,Long::sum);
   remaining.put(r.id,available-reserved);
  }

  List<Row> result=new ArrayList<>();
  for(Requirement req:requirements){
   long total=req.total;
   long owned=Math.min(total,direct.getOrDefault(req.id,0L));
   double equivalent=owned;
   StringJoiner extra=new StringJoiner(", ");
   if(catalog!=null&&equivalent<total){
    String current=req.id;
    Set<String> visited=new HashSet<>();visited.add(current);
    double ratio=1.0;
    // Descend only recipes with one input. Multicomponent routes must never
    // magically award credit for just one of their ingredients.
    for(int depth=0;depth<12 && equivalent<total;depth++){
     Catalog.Item item=catalog.items.get(current);
     if(item==null||item.routes==null||item.routes.isEmpty()||item.raw)break;
     int routeIndex=Math.floorMod(goal.choices.getOrDefault(current,0),item.routes.size());
     Catalog.Route route=item.routes.get(routeIndex);
     if(route.count<=0||route.inputs==null||route.inputs.size()!=1)break;
     Catalog.Ingredient ingredient=route.inputs.getFirst();
     if(ingredient.count<=0||ingredient.id==null||!visited.add(ingredient.id))break;
     ratio*=route.count/(double)ingredient.count;
     if(!Double.isFinite(ratio)||ratio<=0)break;
     current=ingredient.id;
     long available=remaining.getOrDefault(current,0L);
     if(available<=0)continue;
     double needed=(total-equivalent)/ratio;
     long maxToUse=needed>=Long.MAX_VALUE?Long.MAX_VALUE:(long)Math.ceil(needed-1e-9);
     long used=Math.min(available,Math.max(0,maxToUse));
     if(used==0)continue;
     equivalent=Math.min(total,equivalent+used*ratio);
     remaining.put(current,available-used);
     String label=catalog.item(current).name;
     extra.add(String.format(Locale.US,"%,d %s",used,label));
    }
   }
   result.add(new Row(req.id,owned,total,Math.max(0,pending.getOrDefault(req.id,0L)),
          Math.max(0,forge.getOrDefault(req.id,0L)),goal.checked.contains(req.id),
          equivalent,extra.toString()));
  }
  return result;
 }
}
