package dev.recipehunter;
import java.util.*;
/** Reopening replaces a chest snapshot; it never adds another copy. Session scoped. */
public final class StorageLedger {
    private final Map<String,Map<String,Long>> chests=new HashMap<>();
    public void replace(String key,Map<String,Long> items){chests.put(key,new HashMap<>(items));}
    public void clear(){chests.clear();}
    public int size(){return chests.size();}
    public Map<String,Long> totals(){Map<String,Long> total=new HashMap<>();for(var c:chests.values())c.forEach((id,n)->total.merge(id,n,Math::addExact));return total;}
}
