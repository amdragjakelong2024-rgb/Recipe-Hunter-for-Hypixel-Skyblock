package dev.recipehunter;
import java.util.*;

/** Each HUD compares its goal with current observed stock; a saved goal is not a stock reservation. */
public final class LiveProgress {
 public record Snapshot(Map<String,RecipeProgress.Node> trees,Map<String,List<MaterialProgress.Row>> rows){}
 public static Snapshot refresh(Catalog catalog,Book book,Map<String,Long> observed,List<CraftDiary.Entry> forgeJobs){
  var pending=CraftDiary.mergeReservations(book.diary,forgeJobs);var forge=CraftDiary.pending(forgeJobs);
  var engine=new RecipeProgress(catalog);Map<String,RecipeProgress.Node> trees=new LinkedHashMap<>();Map<String,List<MaterialProgress.Row>> rows=new LinkedHashMap<>();
  for(var goal:book.goals){
   var tree=engine.evaluate(goal,observed,pending,forge);
   trees.put(goal.id,tree);rows.put(goal.id,engine.displayed(tree,goal).stream().map(RecipeProgress.Node::row).toList());
  }
  return new Snapshot(trees,rows);
 }
}
