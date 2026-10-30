package dev.recipehunter;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.loader.api.FabricLoader;
/** Optional read-only bridge to SkyHanni's active profile, never a disk scrape or a second additive source. */
public final class SackBridge {
 private Object profile;private boolean unavailable;public String status="Server Sack messages";
 public void reset(){profile=null;unavailable=false;status="Server Sack messages";}
 public static String itemId(Object key){String s=String.valueOf(key);return s.startsWith("internalName:")?s.substring(13):s;}
 public static boolean exact(Object status){return status!=null&&status.toString().equals("CORRECT");}
 public void sync(RecipeHunter app){
  if(unavailable||!FabricLoader.getInstance().isModLoaded("skyhanni"))return;
  try{
   Class<?> cls=Class.forName("at.hannibal2.skyhanni.data.ProfileStorageData");Object instance=cls.getField("INSTANCE").get(null);
   Object current=cls.getMethod("getSackProfiles").invoke(instance);
   if(current==null){status="Waiting for SkyHanni profile";return;}
   if(profile!=null&&profile!=current){app.menus.clear();app.ownership.clear();app.alerts.reset();app.progressTrees.clear();app.hudProgress.clear();}
   profile=current;
   Map<?,?> data=(Map<?,?>)current.getClass().getMethod("getSackContents").invoke(current);
   Map<String,Long> exact=new HashMap<>();
   for(var entry:data.entrySet()){
    String id=itemId(entry.getKey());if(!app.catalog.items.containsKey(id)||entry.getValue()==null)continue;
    Object value=entry.getValue();if(!exact(value.getClass().getMethod("getStatus").invoke(value)))continue;
    long amount=((Number)value.getClass().getMethod("getAmount").invoke(value)).longValue();if(amount>=0)exact.put(id,amount);
   }
   apply(app.menus,exact,System.currentTimeMillis());status="SkyHanni: "+exact.size()+" exact Sack balances";
  }catch(ReflectiveOperationException|LinkageError|ClassCastException e){
   // Optional mod API changed: retain the independent packet tracker; do not crash or assume totals.
   unavailable=true;status="SkyHanni bridge unavailable; server Sack messages";app.menus.bridgedSacks.clear();
  }
 }
 public static void apply(MenuTracker menus,Map<String,Long> exact,long now){
  for(String id:menus.bridgedSacks)if(!exact.containsKey(id))menus.uncertainSacks.add(id);
  menus.bridgedSacks.clear();
  exact.forEach((id,n)->{menus.observeSack(id,n,now);menus.bridgedSacks.add(id);});
 }
}
