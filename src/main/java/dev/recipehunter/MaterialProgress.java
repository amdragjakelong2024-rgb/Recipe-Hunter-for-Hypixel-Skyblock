package dev.recipehunter;
import java.util.*;
/** Stable required totals with owned and pending counts kept separate. */
public final class MaterialProgress {
 public record Row(String id,long have,long total,long pending,long forge,boolean checked){
  public boolean done(){return checked||have>=total;}
  public double fraction(){return total<=0?1:Math.min(1,have/(double)total);}
  public String count(){return have+"/"+total;}
 }
 public static List<Row> rows(Book.Goal goal,Planner.Plan plan,Map<String,Long> stock,Map<String,Long> pending,Map<String,Long> forge){
  List<Row> rows=new ArrayList<>();for(var e:plan.demand.entrySet()){
   String id=e.getKey();long total=e.getValue();if(total<=0||plan.routes.containsKey(id))continue;
   rows.add(new Row(id,Math.min(total,Math.max(0,stock.getOrDefault(id,0L))),total,Math.max(0,pending.getOrDefault(id,0L)),Math.max(0,forge.getOrDefault(id,0L)),goal.checked.contains(id)));
  }return rows;
 }
}
