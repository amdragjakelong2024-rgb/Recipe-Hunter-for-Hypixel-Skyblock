package dev.recipehunter;
import java.util.*;

/** Expands a selected acyclic recipe graph in parent-before-child order.
 * Shared demands are summed BEFORE output rounding and stock subtraction. */
public final class Planner {
    public static final class Plan {
        public Map<String,Long> demand=new LinkedHashMap<>(),missing=new LinkedHashMap<>(),leaves=new LinkedHashMap<>(),crafts=new LinkedHashMap<>(),surplus=new LinkedHashMap<>();
        public Map<String,Catalog.Route> routes=new HashMap<>(); public List<String> warnings=new ArrayList<>();
    }
    final Catalog c;
    public Planner(Catalog catalog){c=catalog;}
    public Plan plan(String root,long quantity,Map<String,Long> stock,Map<String,Integer> choices){
        return calculate(root,quantity,stock,choices,false,true,Set.of());
    }
    public Plan materials(String root,long quantity,Map<String,Long> stock,Map<String,Integer> choices){
        return calculate(root,quantity,stock,choices,true,true,Set.of());
    }
    public Plan goal(Book.Goal g,Map<String,Long> stock){return calculate(g.id,g.count,stock,g.choices,true,g.fineGems,g.checked);}
    private Plan calculate(String root,long quantity,Map<String,Long> stock,Map<String,Integer> choices,boolean practical,boolean fineGems,Set<String> checked){
        if(quantity<1||quantity>1_000_000)throw new IllegalArgumentException("Quantity must be 1..1,000,000");
        Plan p=new Plan();List<String> order=new ArrayList<>();Map<String,Integer> state=new HashMap<>();
        visit(root,choices,state,order,p,new ArrayDeque<>(),practical,fineGems,root);Collections.reverse(order);p.demand.put(root,quantity);
        for(String id:order){
            long demand=p.demand.getOrDefault(id,0L),need=Math.max(0,demand-Math.max(0,stock.getOrDefault(id,0L)));if(checked.contains(id))need=0;p.missing.put(id,need);
            if(need==0)continue;Catalog.Route r=p.routes.get(id);
            if(r==null){p.leaves.merge(id,need,Math::addExact);continue;}
            long runs=(need-1)/r.count+1;p.crafts.put(id,runs);long extra=Math.subtractExact(Math.multiplyExact(runs,r.count),need);if(extra>0)p.surplus.put(id,extra);
            for(Catalog.Ingredient in:r.inputs)p.demand.merge(in.id,Math.multiplyExact(in.count,runs),Math::addExact);
        }
        return p;
    }
    private boolean enchanted(String id){return id.startsWith("ENCHANTED_") || c.item(id).name.startsWith("Enchanted ");}
    private void visit(String id,Map<String,Integer> choices,Map<String,Integer> state,List<String> order,Plan p,Deque<String> path,boolean practical,boolean fineGems,String root){
        if(state.containsKey(id))return;state.put(id,1);path.addLast(id);Catalog.Item item=c.item(id);
        if(!item.raw&&!item.routes.isEmpty()){
            int ix=Math.floorMod(choices.getOrDefault(id,0),item.routes.size());Catalog.Route chosen=item.routes.get(ix);
            // Do not silently invent an alternative if the selected conversion leads to a cycle.
            boolean stop=practical && ((fineGems?id.matches("FINE_[A-Z_]+_GEM"):(!id.equals(root)&&id.matches("(?:ROUGH|FLAWED|FINE|FLAWLESS|PERFECT)_[A-Z_]+_GEM"))) || (enchanted(id) && chosen.inputs.stream().noneMatch(in->enchanted(in.id))));
            boolean cycle=chosen.inputs.stream().anyMatch(in->state.getOrDefault(in.id,0)==1);
            if(stop) { /* This is the requested collection unit; keep its actual recipe inspectable. */ }
            else if(cycle)p.warnings.add("Conversion cycle stopped at "+item.name+". Acquire this item directly or choose another route.");
            else {p.routes.put(id,chosen);for(Catalog.Ingredient in:chosen.inputs)visit(in.id,choices,state,order,p,path,practical,fineGems,root);}
        }
        path.removeLast();state.put(id,2);order.add(id);
    }
}
