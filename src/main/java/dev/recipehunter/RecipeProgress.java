package dev.recipehunter;
import java.util.*;

/** Selected recipe tree. Ownership is allocated once; display preferences never enter evaluation. */
public final class RecipeProgress {
 public static final class Node {
  public final String id,parent;public final long total,owned,pending,forge;public final boolean checked;
  public Node acquisition;public long covered,physical,queued,queuedForge;public double equivalent,reserved,inForge;public boolean combined;public final List<Node> children=new ArrayList<>();
  Node(String id,String parent,long total,long owned,long pending,long forge,boolean checked){this.id=id;this.parent=parent;this.total=total;this.owned=owned;this.pending=pending;this.forge=forge;this.checked=checked;physical=owned;equivalent=owned;}
  public double remaining(){return Math.max(0,total-equivalent-reserved);}
  public double fraction(){return total==0?1:Math.clamp(equivalent/total,0,1);}
  public MaterialProgress.Row row(){return new MaterialProgress.Row(id,Math.min(total,physical),total,pending,forge,checked,equivalent,"",reserved,inForge);}
 }
 private final Catalog catalog;private int visited;private Map<String,Long> physicalPool;private Map<String,Long> queuedPool=Map.of(),forgePool=Map.of();
 public RecipeProgress(Catalog catalog){this.catalog=catalog;}
 public Node evaluate(Book.Goal goal,Map<String,Long> stock,Map<String,Long> pending,Map<String,Long> forge){
  Map<String,Long> planned=new HashMap<>(stock);pending.forEach((id,n)->planned.merge(id,n,Math::addExact));
  ArmorSets.register(catalog,goal);
  Node actual=allocateReserved(goal,new HashMap<>(stock),planned,pending,forge);
  if(!forge.isEmpty()){
   Map<String,Long> projected=new HashMap<>(stock);forge.forEach((id,n)->{if(n>0)projected.merge(id,n,Math::addExact);});
   creditForge(actual,allocate(goal,projected,Map.of(),Map.of()));
  }
  ArmorSets.averageReadiness(goal,actual);if(actual.acquisition!=null)ArmorSets.averageReadiness(goal,actual.acquisition);
  return actual;
 }
 public Node allocateReserved(Book.Goal goal,Map<String,Long> owned,Map<String,Long> planned,Map<String,Long> pending,Map<String,Long> forge){
  Map<String,Long> acquisitionStock=new HashMap<>(owned);
  Node actual=allocate(goal,owned,pending,forge),reserved=allocate(goal,planned,Map.of(),Map.of());reservations(actual,reserved);
  if(!pending.isEmpty()){
   queuedPool=new HashMap<>(pending);forgePool=new HashMap<>(forge);
   try{linkAcquisition(actual,allocate(goal,acquisitionStock,pending,forge));}finally{queuedPool=Map.of();forgePool=Map.of();}
  }
  return actual;
 }
 private void creditForge(Node actual,Node projected){actual.inForge=Math.min(Math.max(0,actual.total-actual.equivalent),Math.max(0,projected.equivalent-actual.equivalent));for(int i=0;i<actual.children.size();i++)creditForge(actual.children.get(i),projected.children.get(i));}
 private void linkAcquisition(Node actual,Node acquisition){actual.acquisition=acquisition;actual.reserved=Math.max(actual.reserved,acquisition.reserved);for(int i=0;i<actual.children.size();i++)linkAcquisition(actual.children.get(i),acquisition.children.get(i));}
 private void reservations(Node actual,Node reserved){actual.reserved=Math.max(0,reserved.equivalent-actual.equivalent);for(int i=0;i<actual.children.size();i++)reservations(actual.children.get(i),reserved.children.get(i));}
 /** Pass one mutable stock pool for ordered goals to avoid promising the same item twice. */
 public Node allocate(Book.Goal goal,Map<String,Long> pool,Map<String,Long> pending,Map<String,Long> forge){
  visited=0;Long existing=(goal.freshCraft||goal.crafted>0)?pool.remove(goal.id):null;
  if(existing!=null&&goal.targetBaseline>=0)pool.put(goal.id,Math.max(0,existing-goal.targetBaseline-goal.crafted));
  physicalPool=new HashMap<>(pool);
  try{return build(goal.id,"",Math.max(0,goal.count-goal.crafted),0,goal,pool,pending,forge,new HashSet<>());}
  finally{if(existing!=null)pool.put(goal.id,existing);}
 }
 private Node build(String id,String parent,long total,long covered,Book.Goal goal,Map<String,Long> pool,Map<String,Long> pending,Map<String,Long> forge,Set<String> path){
  if(++visited>20000)throw new IllegalArgumentException("Recipe graph exceeds safe node limit");
  boolean checked=goal.checked.contains(id);creditCompressed(id,total-covered,goal,pool);long available=Math.max(0,pool.getOrDefault(id,0L));
  if(parent.isEmpty()&&(goal.freshCraft||goal.crafted>0)&&goal.targetBaseline<0)available=0;
  long used=Math.min(Math.max(0,total-covered),available);if(used>0)pool.put(id,available-used);
  long queued=Math.min(Math.max(0,total-covered-used),Math.max(0,queuedPool.getOrDefault(id,0L)));
  if(queued>0)queuedPool.put(id,queuedPool.get(id)-queued);
  long forging=Math.min(queued,Math.max(0,forgePool.getOrDefault(id,0L)));if(forging>0)forgePool.put(id,forgePool.get(id)-forging);
  long have=Math.min(total,covered+used+queued);
  long actual=Math.min(used,Math.max(0,physicalPool.getOrDefault(id,0L)));physicalPool.put(id,physicalPool.getOrDefault(id,0L)-actual);
  Node node=new Node(id,parent,total,have,Math.max(0,pending.getOrDefault(id,0L)),Math.max(0,forge.getOrDefault(id,0L)),checked);
  node.covered=covered;node.physical=actual;node.equivalent=have-queued;node.reserved=queued;node.queued=queued;node.inForge=forging;node.queuedForge=forging;
  Catalog.Item item=catalog.items.get(id);
  if(total==0||item==null||item.raw||item.routes.isEmpty()||path.contains(id)||path.size()>=32)return node;
  Catalog.Route route=item.routes.get(Math.floorMod(goal.choices.getOrDefault(id,0),item.routes.size()));
  if(route.count<=0||route.inputs.isEmpty())return node;
  path.add(id);long fullRuns=(total-1)/route.count+1,missing=total-have,runs=missing==0?0:(missing-1)/route.count+1;
  double fraction=1,withPending=1,withForge=1;
  for(var in:route.inputs){
   if(in.count<=0)continue;
   long childTotal=Math.multiplyExact(fullRuns,in.count),childCovered=Math.multiplyExact(fullRuns-runs,in.count);
   Node child=build(in.id,id,childTotal,childCovered,goal,pool,pending,forge,path);node.children.add(child);
   long outstanding=childTotal-childCovered;
   if(outstanding>0){withForge=Math.min(withForge,Math.clamp((child.equivalent+child.inForge-childCovered)/outstanding,0,1));fraction=Math.min(fraction,Math.clamp((child.equivalent-childCovered)/outstanding,0,1));withPending=Math.min(withPending,Math.clamp((child.equivalent+child.reserved-childCovered)/outstanding,0,1));}
  }
  path.remove(id);
  if(!node.children.isEmpty()){
   node.equivalent=Math.min(total,have-queued+fraction*runs*route.count);
   node.reserved=Math.min(total-node.equivalent,queued+Math.max(0,withPending-fraction)*runs*route.count);
   node.inForge=Math.min(total-node.equivalent,forging+Math.max(0,withForge-fraction)*runs*route.count);
  }
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
     pool.put(source,pool.get(source)-used);physicalPool.put(source,Math.max(0,physicalPool.getOrDefault(source,0L)-used));pool.merge(target,Math.multiplyExact(used,ratio),Math::addExact);break;
    }
   }
  }
 }
 /** Merge identical item IDs in the summary only. The full recipe tree stays intact
  * to show which intermediate needs each input. One inventory allocation is shared
  * across the original nodes, so merge their credited amounts, never re-read stock.
  */
 public List<Node> displayed(Node root,Book.Goal goal){
  List<Node> expanded=new ArrayList<>();collect(root.acquisition==null?root:root.acquisition,goal,expanded,true);
  return consolidate(expanded);
 }
 static List<Node> consolidate(List<Node> expanded){
  Map<String,Node> totals=new LinkedHashMap<>();
  for(Node part:expanded){
   Node first=totals.get(part.id);
   if(first==null){totals.put(part.id,part);continue;}
   long required=Math.addExact(first.total,part.total);
   long actual=Math.addExact(first.owned,part.owned);
   // Pending/forge reflect the same global server quantity in each tree node,
   // not separate reservations. Avoid reporting the same job twice.
   Node merged=new Node(part.id,"",required,Math.min(required,actual),
      Math.max(first.pending,part.pending),Math.max(first.forge,part.forge),
      first.checked&&part.checked);
   merged.equivalent=Math.min(required,Math.max(0,first.equivalent)+Math.max(0,part.equivalent));
   merged.reserved=Math.min(required,Math.max(0,first.reserved)+Math.max(0,part.reserved));
   merged.inForge=Math.min(required-merged.equivalent,first.inForge+part.inForge);
   merged.combined=true;
   totals.put(part.id,merged);
  }
  return new ArrayList<>(totals.values());
 }
 private void collect(Node node,Book.Goal goal,List<Node> result,boolean root){
  boolean gem=node.id.matches("(?:ROUGH|FLAWED|FINE|FLAWLESS|PERFECT)_[A-Z_]+_GEM");
  boolean stop=node.children.isEmpty();
  if(gem)stop|=node.id.startsWith(goal.gemDisplay.toUpperCase(Locale.ROOT)+"_");
  else if("Enchanted".equals(goal.materialDisplay))stop|=node.id.startsWith("ENCHANTED_")&&!node.id.endsWith("_BLOCK");
  else if("Enchanted Block".equals(goal.materialDisplay))stop|=node.id.startsWith("ENCHANTED_");
  else stop|=!root&&(node.id.startsWith("ENCHANTED_")||node.id.startsWith("REFINED_")||catalog.item(node.id).routes.stream().anyMatch(r->"forge".equals(r.type)));
  if(goal.expanded.contains(node.id))stop=false;
  if(stop||node.children.isEmpty()){
   Node remaining=remaining(node);if(remaining.total>0)result.add(remaining);
   // Perfect gemstones retain their non-gemstone costs in every presentation.
   if(gem)for(Node child:node.children)if(!child.id.endsWith("_GEM"))collect(child,goal,result,false);
  }else {
   if(node.queued>0){Node job=new Node(node.id,node.parent,node.queued,0,node.pending,node.forge,false);job.reserved=node.queued;job.inForge=node.queuedForge;result.add(job);}
   for(Node child:node.children)collect(child,goal,result,false);
  }
 }
 /** Costs already embedded in an owned ancestor are NOT loose items in storage.
  * Keep full graph coverage for overall progress, strip it from acquisition rows.
  */
 public static Node remaining(Node node){
  if(node.acquisition!=null)node=node.acquisition;
  long total=Math.max(0,node.total-node.covered);
  Node row=new Node(node.id,node.parent,total,Math.min(total,node.physical),node.pending,node.forge,node.checked);
  row.equivalent=Math.clamp(node.equivalent-node.covered,0,total);
  row.reserved=Math.min(Math.max(0,total-row.equivalent),node.reserved);row.inForge=Math.min(Math.max(0,total-row.equivalent),node.inForge);row.children.addAll(node.children);return row;
 }
 public Set<String> materialOptions(Node root){Set<String> out=new LinkedHashSet<>(List.of("Compact"));walk(root,n->{if(n.id.startsWith("ENCHANTED_"))out.add(n.id.endsWith("_BLOCK")?"Enchanted Block":"Enchanted");});return out;}
 public Set<String> gemOptions(Node root){Set<String> out=new LinkedHashSet<>();for(String tier:List.of("Fine","Flawless","Perfect"))walk(root,n->{if(n.id.startsWith(tier.toUpperCase(Locale.ROOT)+"_")&&n.id.endsWith("_GEM"))out.add(tier);});return out;}
 private void walk(Node n,java.util.function.Consumer<Node> action){action.accept(n);for(Node child:n.children)walk(child,action);}
}
