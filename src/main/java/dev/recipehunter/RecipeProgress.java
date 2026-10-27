package dev.recipehunter;
import java.util.*;

/** Selected recipe tree. Ownership is allocated once; display preferences never enter evaluation. */
public final class RecipeProgress {
 public static final class Node {
  public final String id,parent;public final long total,owned,pending,forge;public final boolean checked;
  public double equivalent,reserved;public final List<Node> children=new ArrayList<>();
  Node(String id,String parent,long total,long owned,long pending,long forge,boolean checked){this.id=id;this.parent=parent;this.total=total;this.owned=owned;this.pending=pending;this.forge=forge;this.checked=checked;equivalent=owned;}
  public double remaining(){return Math.max(0,total-equivalent-reserved);}
  public double fraction(){return total==0?1:Math.clamp(equivalent/total,0,1);}
  public MaterialProgress.Row row(){return new MaterialProgress.Row(id,Math.min(total,owned),total,pending,forge,checked,equivalent,"");}
 }
 private final Catalog catalog;private int visited;
 public RecipeProgress(Catalog catalog){this.catalog=catalog;}
 public Node evaluate(Book.Goal goal,Map<String,Long> stock,Map<String,Long> pending,Map<String,Long> forge){
  Map<String,Long> planned=new HashMap<>(stock);pending.forEach((id,n)->planned.merge(id,n,Math::addExact));
  return allocateReserved(goal,new HashMap<>(stock),planned,pending,forge);
 }
 public Node allocateReserved(Book.Goal goal,Map<String,Long> owned,Map<String,Long> planned,Map<String,Long> pending,Map<String,Long> forge){
  Node actual=allocate(goal,owned,pending,forge),reserved=allocate(goal,planned,Map.of(),Map.of());reservations(actual,reserved);return actual;
 }
 private void reservations(Node actual,Node reserved){actual.reserved=Math.max(0,reserved.equivalent-actual.equivalent);for(int i=0;i<actual.children.size();i++)reservations(actual.children.get(i),reserved.children.get(i));}
 /** Pass one mutable stock pool for ordered goals to avoid promising the same item twice. */
 public Node allocate(Book.Goal goal,Map<String,Long> pool,Map<String,Long> pending,Map<String,Long> forge){
  visited=0;Long existing=(goal.freshCraft||goal.crafted>0)?pool.remove(goal.id):null;
  if(existing!=null&&goal.targetBaseline>=0)pool.put(goal.id,Math.max(0,existing-goal.targetBaseline-goal.crafted));
  try{return build(goal.id,"",Math.max(0,goal.count-goal.crafted),0,goal,pool,pending,forge,new HashSet<>());}
  finally{if(existing!=null)pool.put(goal.id,existing);}
 }
 private Node build(String id,String parent,long total,long covered,Book.Goal goal,Map<String,Long> pool,Map<String,Long> pending,Map<String,Long> forge,Set<String> path){
  if(++visited>20000)throw new IllegalArgumentException("Recipe graph exceeds safe node limit");
  boolean checked=goal.checked.contains(id);creditCompressed(id,total-covered,goal,pool);long available=Math.max(0,pool.getOrDefault(id,0L));
  if(parent.isEmpty()&&(goal.freshCraft||goal.crafted>0)&&goal.targetBaseline<0)available=0;
  long used=Math.min(Math.max(0,total-covered),available);if(used>0)pool.put(id,available-used);
  long have=Math.min(total,covered+used);
  Node node=new Node(id,parent,total,have,Math.max(0,pending.getOrDefault(id,0L)),Math.max(0,forge.getOrDefault(id,0L)),checked);
  Catalog.Item item=catalog.items.get(id);
  if(total==0||item==null||item.raw||item.routes.isEmpty()||path.contains(id)||path.size()>=32)return node;
  Catalog.Route route=item.routes.get(Math.floorMod(goal.choices.getOrDefault(id,0),item.routes.size()));
  if(route.count<=0||route.inputs.isEmpty())return node;
  path.add(id);long fullRuns=(total-1)/route.count+1,missing=total-have,runs=missing==0?0:(missing-1)/route.count+1;
  double fraction=1;
  for(var in:route.inputs){
   if(in.count<=0)continue;
   long childTotal=Math.multiplyExact(fullRuns,in.count),childCovered=Math.multiplyExact(fullRuns-runs,in.count);
   Node child=build(in.id,id,childTotal,childCovered,goal,pool,pending,forge,path);node.children.add(child);
   long outstanding=childTotal-childCovered;
   if(outstanding>0)fraction=Math.min(fraction,Math.clamp((child.equivalent-childCovered)/outstanding,0,1));
  }
  path.remove(id);
  if(!node.children.isEmpty())node.equivalent=Math.min(total,have+fraction*runs*route.count);
  return node;
 }
 /** Allocate surplus higher compression tiers using only the selected, recorded single-input routes. */
 private void creditCompressed(String target,long need,Book.Goal goal,Map<String,Long> pool){
  if(need<=pool.getOrDefault(target,0L))return;
  for(String source:new ArrayList<>(pool.keySet())){
   if(source.equals(target)||pool.getOrDefault(source,0L)<=0)continue;
   String current=source;long ratio=1;Set<String> seen=new HashSet<>();
   while(seen.add(current)&&(current.startsWith("ENCHANTED_")||current.matches("(?:FLAWED|FINE|FLAWLESS)_[A-Z_]+_GEM"))){
    Catalog.Item item=catalog.items.get(current);if(item==null||item.routes.isEmpty())break;
    Catalog.Route route=item.routes.get(Math.floorMod(goal.choices.getOrDefault(current,0),item.routes.size()));
    if(route.inputs.size()!=1||route.count!=1||!"crafting".equals(route.type))break;
    var in=route.inputs.getFirst();if(in.count<=0)break;
    try{ratio=Math.multiplyExact(ratio,in.count);}catch(ArithmeticException ex){break;}
    current=in.id;
    if(current.equals(target)){
     long missing=need-pool.getOrDefault(target,0L);if(missing<=0)return;
     long used=Math.min(pool.get(source),(missing-1)/ratio+1);
     pool.put(source,pool.get(source)-used);pool.merge(target,Math.multiplyExact(used,ratio),Math::addExact);break;
    }
   }
  }
 }
 public List<Node> displayed(Node root,Book.Goal goal){List<Node> result=new ArrayList<>();collect(root,goal,result,true);return result;}
 private void collect(Node node,Book.Goal goal,List<Node> result,boolean root){
  boolean gem=node.id.matches("(?:ROUGH|FLAWED|FINE|FLAWLESS|PERFECT)_[A-Z_]+_GEM");
  boolean stop=node.children.isEmpty();
  if(gem)stop|=node.id.startsWith(goal.gemDisplay.toUpperCase(Locale.ROOT)+"_");
  else if("Enchanted".equals(goal.materialDisplay))stop|=node.id.startsWith("ENCHANTED_")&&!node.id.endsWith("_BLOCK");
  else if("Enchanted Block".equals(goal.materialDisplay))stop|=node.id.startsWith("ENCHANTED_");
  else stop|=!root&&(node.id.startsWith("ENCHANTED_")||node.id.startsWith("REFINED_")||catalog.item(node.id).routes.stream().anyMatch(r->"forge".equals(r.type)));
  if(goal.expanded.contains(node.id))stop=false;
  if(stop||node.children.isEmpty()){
   result.add(node);
   // Perfect gemstones retain their non-gemstone costs in every presentation.
   if(gem)for(Node child:node.children)if(!child.id.endsWith("_GEM"))collect(child,goal,result,false);
  }else for(Node child:node.children)collect(child,goal,result,false);
 }
 public Set<String> materialOptions(Node root){Set<String> out=new LinkedHashSet<>(List.of("Compact"));walk(root,n->{if(n.id.startsWith("ENCHANTED_"))out.add(n.id.endsWith("_BLOCK")?"Enchanted Block":"Enchanted");});return out;}
 public Set<String> gemOptions(Node root){Set<String> out=new LinkedHashSet<>();for(String tier:List.of("Fine","Flawless","Perfect"))walk(root,n->{if(n.id.startsWith(tier.toUpperCase(Locale.ROOT)+"_")&&n.id.endsWith("_GEM"))out.add(tier);});return out;}
 private void walk(Node n,java.util.function.Consumer<Node> action){action.accept(n);for(Node child:n.children)walk(child,action);}
}
