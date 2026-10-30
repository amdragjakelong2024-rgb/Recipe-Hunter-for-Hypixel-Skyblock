package dev.recipehunter;
import java.util.*;
/** Server-observed slots, never inferred unlocks. Jobs run concurrently. */
public final class ForgeState {
 public enum Slot { UNKNOWN, EMPTY, LOCKED, BUSY }
 public final Map<Integer,Slot> slots=new TreeMap<>();
 public long observedAt;public String source="Not observed";
 public void clear(){slots.clear();observedAt=0;source="Not observed";}
 public void observe(int slot,Slot state,String source,long now){if(slot<1||slot>7)return;slots.put(slot,state);this.source=source;observedAt=now;}
 public static List<CraftDiary.Entry> active(List<CraftDiary.Entry> jobs){return jobs.stream().filter(e->e.active&&!e.done&&e.count>0).sorted(Comparator.comparing(e->e.key)).toList();}
 public boolean full(List<CraftDiary.Entry> jobs){
  if(slots.size()!=7||slots.values().stream().anyMatch(s->s==Slot.UNKNOWN||s==Slot.EMPTY))return false;
  long unlocked=slots.values().stream().filter(s->s==Slot.BUSY).count();return unlocked>0&&active(jobs).size()==unlocked;
 }
 public static long waitMillis(List<CraftDiary.Entry> jobs,long now){return active(jobs).stream().mapToLong(e->Math.max(0,e.readyAt-now)).max().orElse(0);}
 public static String duration(long ms){long sec=Math.max(0,(ms+999)/1000);return String.format(Locale.US,"%dh %02dm %02ds",sec/3600,sec/60%60,sec%60);}
 public static int slot(CraftDiary.Entry e){try{return Integer.parseInt(e.key.substring("forge:".length()));}catch(Exception ignored){return 0;}}
}
