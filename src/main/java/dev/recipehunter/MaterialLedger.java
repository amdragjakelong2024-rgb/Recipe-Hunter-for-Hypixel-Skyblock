package dev.recipehunter;
import java.util.*;

/** Per-source evidence. A missing baseline is unknown, never an absolute zero. */
public final class MaterialLedger {
 public enum Quality { LIVE, LAST_SYNCED, SESSION_ONLY, UNKNOWN }
 public record Balance(long amount,long updatedAt,Quality quality){}
 private final Map<String,Map<String,Balance>> sources=new LinkedHashMap<>();
 public void clear(){sources.clear();}
 public void snapshot(String source,Map<String,Long> stock,long at,Quality quality){
  Map<String,Balance> values=new LinkedHashMap<>();stock.forEach((id,n)->{if(n>=0)values.put(id,new Balance(n,at,quality));});sources.put(source,values);
 }
 public Balance get(String source,String id){return sources.getOrDefault(source,Map.of()).getOrDefault(id,new Balance(0,0,Quality.UNKNOWN));}
 public Map<String,Long> known(){Map<String,Long> result=new LinkedHashMap<>();for(var values:sources.values())for(var e:values.entrySet())if(e.getValue().quality!=Quality.UNKNOWN)result.merge(e.getKey(),e.getValue().amount,Math::addExact);return result;}
 public List<String> diagnostics(String id){List<String> lines=new ArrayList<>();for(var source:sources.keySet()){Balance b=get(source,id);lines.add(source+": "+(b.quality==Quality.UNKNOWN?"Unknown":Catalog.qty(b.amount))+" | "+b.quality+" | updated "+(b.updatedAt==0?"never":(System.currentTimeMillis()-b.updatedAt)/1000+"s ago"));}return lines;}
}
