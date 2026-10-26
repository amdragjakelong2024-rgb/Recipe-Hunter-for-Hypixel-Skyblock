package dev.recipehunter;
import java.util.*;

/** Same recomputation path for client ticks and acquisition/transfer regression tests. */
public final class LiveProgress {
 public record Snapshot(Map<String,RecipeProgress.Node> trees,Map<String,List<MaterialProgress.Row>> rows){}
 public static Snapshot refresh(Catalog catalog,Book book,Map<String,Long> observed,List<CraftDiary.Entry> forgeJobs){
  Map<String,Long> pool=new HashMap<>(observed);
  var pending=CraftDiary.mergeReservations(book.diary,forgeJobs);var forge=CraftDiary.pending(forgeJobs);
  Map<String,Long> planned=new HashMap<>(pool);pending.forEach((id,n)->planned.merge(id,n,Math::addExact));
  var engine=new RecipeProgress(catalog);Map<String,RecipeProgress.Node> trees=new LinkedHashMap<>();Map<String,List<MaterialProgress.Row>> rows=new LinkedHashMap<>();
  for(var goal:book.goals){if(!goal.visible)continue;var tree=engine.allocateReserved(goal,pool,planned,pending,forge);trees.put(goal.id,tree);rows.put(goal.id,engine.displayed(tree,goal).stream().map(RecipeProgress.Node::row).toList());}
  for(var goal:book.goals)if(!goal.visible){var tree=engine.evaluate(goal,observed,pending,forge);trees.put(goal.id,tree);rows.put(goal.id,engine.displayed(tree,goal).stream().map(RecipeProgress.Node::row).toList());
  return new Snapshot(trees,rows);
 }
}
