package dev.recipehunter;
import java.util.*;
public final class ForgeCommands {
 public static boolean execute(RecipeHunter app,String query){
  String q=query.toLowerCase(Locale.ROOT).trim();if(!q.startsWith("forge "))return false;
  if(q.equals("forge hide")||q.equals("forge show")){app.book.forgeVisible=q.endsWith("show");app.persist();app.say("Forge HUD "+(app.book.forgeVisible?"shown":"hidden")+". Tracking remains active.");return true;}
  var m=java.util.regex.Pattern.compile("forge slot([1-7]) info").matcher(q);
  if(m.matches()){for(String line:info(app.catalog,app.menus,Integer.parseInt(m.group(1)),System.currentTimeMillis()))app.say(line);return true;}
  app.say("Use /rh forge, /rh forge hide|show, or /rh forge slot1 info (slots 1-7).");return true;
 }
 public static List<String> info(Catalog c,MenuTracker menus,int slot,long now){
  var e=ForgeState.active(menus.forge).stream().filter(j->ForgeState.slot(j)==slot).findFirst().orElse(null);
  if(e==null){String status=menus.forgeState.slots.getOrDefault(slot,ForgeState.Slot.UNKNOWN).toString();if(status.equals("BUSY"))status="UNKNOWN";return List.of("Forge slot "+slot+": "+status,"Item: Unknown | Completion time: Unknown");}
  return List.of("Forge slot "+slot+": "+(e.readyVerified?"Ready":"Busy"),"Item: "+c.item(e.id).name+" x"+e.count,
   e.readyVerified?"Ready to Collect":"Observed remaining: "+ForgeState.duration(e.observedRemainingMs)+" | seen "+ForgeState.duration(now-e.observedAt)+" ago",
   "Finished in: "+(e.readyVerified?"Ready to Collect":e.readyAt<=now?"Timer elapsed; awaiting verification":ForgeState.duration(e.readyAt-now)));
 }
 public static String finished(MenuTracker menus,long now){var jobs=ForgeState.active(menus.forge);if(jobs.isEmpty())return menus.forgeState.slots.size()<7?"Unknown":"No active jobs";
  long remaining=ForgeState.waitMillis(jobs,now);return remaining>0?ForgeState.duration(remaining):jobs.stream().allMatch(e->e.readyVerified)?"Ready to Collect":"Awaiting verification";}
}
