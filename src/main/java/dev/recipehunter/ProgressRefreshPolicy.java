package dev.recipehunter;
import java.util.*;

/**
 * Live ownership is sampled every five ticks, but recipe graph expansion is only
 * needed when observed stock, Forge reservations or goal settings change.
 * Forge countdowns/notifications continue independently while the tree is cached.
 */
public final class ProgressRefreshPolicy {
 private Map<String,Long> stock,planned,forge;
 private List<String> goals;
 public boolean changed(Map<String,Long> current,Book book,List<CraftDiary.Entry> jobs){
  Map<String,Long> nextPlanned=CraftDiary.mergeReservations(book.diary,jobs);
  Map<String,Long> nextForge=CraftDiary.pending(jobs);
  List<String> nextGoals=new ArrayList<>(book.goals.size());
  for(Book.Goal g:book.goals)
   nextGoals.add(g.id+"|"+g.count+"|"+g.crafted+"|"+g.targetBaseline+"|"+g.finished+"|"+
      g.freshCraft+"|"+g.materialDisplay+"|"+g.gemDisplay+"|"+g.fineGems+"|"+
      g.compact+"|"+g.armorPieces+"|"+g.visible+"|"+g.expanded+"|"+g.choices+"|"+g.checked);
  if(stock!=null&&stock.equals(current)&&planned.equals(nextPlanned)&&forge.equals(nextForge)&&goals.equals(nextGoals))return false;
  stock=new HashMap<>(current);planned=nextPlanned;forge=nextForge;goals=nextGoals;
  return true;
 }
 public void clear(){stock=null;planned=null;forge=null;goals=null;}
}
