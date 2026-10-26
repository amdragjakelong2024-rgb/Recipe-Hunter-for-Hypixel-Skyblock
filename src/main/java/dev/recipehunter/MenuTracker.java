package dev.recipehunter;
import java.util.*;
import java.util.regex.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;

/** Strict own-storage menu allowlist. Never count displayed recipe/auction icons as stock. */
public final class MenuTracker {
 public final StorageLedger ender=new StorageLedger();public final Map<String,Long> sacks=new HashMap<>();
 public final List<CraftDiary.Entry> forge=new ArrayList<>();public long observedAt,lastSackSnapshotAt,lastSackMessageAt,lastSackNoticeAt;public int sackNoticeCount,sackParsedCount,sackUnparsedCount;
 public final Map<String,Long> sackSnapshots=new HashMap<>();public final Set<String> uncertainSacks=new HashSet<>();
 private Object menu;private int ticks;
 public void clear(){ender.clear();sacks.clear();sackSnapshots.clear();uncertainSacks.clear();forge.clear();menu=null;ticks=0;lastSackSnapshotAt=0;lastSackMessageAt=0;lastSackNoticeAt=0;sackNoticeCount=0;sackParsedCount=0;sackUnparsedCount=0;}
 public static long stored(String lore){Matcher m=Pattern.compile("(?i)(?:Stored|Amount)\\s*:\\s*([0-9][0-9,]*)").matcher(lore);if(!m.find())return -1;try{return Long.parseLong(m.group(1).replace(",",""));}catch(NumberFormatException e){return -1;}}
 public void tick(RecipeHunter app){
  if(!(app.mc.screen instanceof AbstractContainerScreen<?> screen)){menu=null;ticks=0;return;}
  if(menu!=screen.getMenu()){menu=screen.getMenu();ticks=0;}if(++ticks<10||ticks%10!=0)return;
  String title=screen.getTitle().getString().replaceAll("§.","").trim();
  boolean ec=title.matches("(?i)Ender Chest(?: \\([0-9]+/[0-9]+\\)| Page [0-9]+)?"),sack=title.matches("(?i)(?:Small |Medium |Large )?(?:Enchanted )?[A-Za-z ]+ Sack"),fg=title.equalsIgnoreCase("The Forge");
  if(!ec&&!sack&&!fg)return;Map<String,Long> contents=new HashMap<>();List<CraftDiary.Entry> jobs=new ArrayList<>();
  for(var slot:screen.getMenu().slots){if(slot.container==app.mc.player.getInventory())continue;var stack=slot.getItem();String id=Ownership.resolvedId(stack,app.catalog);var lore=stack.get(DataComponents.LORE);String text=lore==null?"":String.join("\n",lore.lines().stream().map(l->l.getString().replaceAll("§.","")).toList());
   if(ec&&!id.isEmpty()&&!text.toLowerCase(Locale.ROOT).contains("click to view"))contents.merge(id,(long)stack.getCount(),Long::sum);
   if(sack){long n=stored(text);if(n>=0){
    if(id.isEmpty()){String name=Catalog.normal(stack.getHoverName().getString());id=app.catalog.resolve(name);}
    if(!id.isEmpty()){sacks.put(id,n);sackSnapshots.put(id,System.currentTimeMillis());uncertainSacks.remove(id);}
   }}
   if(fg&&(text.toLowerCase(Locale.ROOT).contains("time remaining")||text.toLowerCase(Locale.ROOT).contains("click to collect")||text.toLowerCase(Locale.ROOT).contains("click to claim"))){
    if(id.isEmpty()){String name=Catalog.normal(stack.getHoverName().getString());id=app.catalog.resolve(name);}
    if(id.isEmpty()||!app.catalog.items.containsKey(id))continue;
    CraftDiary.Entry e=new CraftDiary.Entry();e.key="forge:"+slot.index;e.source="forge";e.id=id;e.count=Math.max(1,stack.getCount());e.text=app.catalog.item(id).name;
    long sec=0;Matcher tm=Pattern.compile("([0-9]+)\\s*([dhms])\\b",Pattern.CASE_INSENSITIVE).matcher(text);while(tm.find())sec+=Long.parseLong(tm.group(1))*switch(tm.group(2).toLowerCase(Locale.ROOT)){case "d"->86400;case "h"->3600;case "m"->60;default->1;};
    e.readyAt=System.currentTimeMillis()+sec*1000;jobs.add(e);
   }
  }
  if(ec)ender.replace(title,contents);if(fg){forge.clear();forge.addAll(jobs);}
  if(sack)lastSackSnapshotAt=System.currentTimeMillis();
  observedAt=System.currentTimeMillis();
 }
 /** Apply only exact per-item chat deltas, never ambiguous aggregate notifications. */
 public void sackMessage(String header,Map<String,Long> delta){
  if(header==null||!header.contains("[Sacks]"))return;
  sackNoticeCount++;lastSackNoticeAt=System.currentTimeMillis();
  if(delta.isEmpty()){sackUnparsedCount++;return;}
  sackParsedCount++;
  long now=System.currentTimeMillis();
  for(var e:delta.entrySet()){
   long snapshot=sackSnapshots.getOrDefault(e.getKey(),0L);
   if(snapshot>0&&now-snapshot<=SackChanges.secondsBack(header)*1000L){
    // A batched message overlapping a snapshot cannot be split reliably.
    uncertainSacks.add(e.getKey());continue;
   }
   long before=sacks.getOrDefault(e.getKey(),0L);
   long next=e.getValue()>0?(before>Long.MAX_VALUE-e.getValue()?Long.MAX_VALUE:before+e.getValue()):Math.max(0,before+e.getValue());
   sacks.put(e.getKey(),next);
  }
  lastSackMessageAt=now;
 }
 public String sackStatus(){
  if(sackParsedCount>0)return "Sack chat live (updates batched)";
  if(sackNoticeCount>0)return "Sack messages seen, item details unreadable";
  if(lastSackSnapshotAt>0)return "Sacks: snapshot only (enable notifications)";
  return "Sacks: enable notifications";
 }
 public boolean sackSynced(){return lastSackSnapshotAt>0;}
 public boolean sackTracked(){return lastSackMessageAt>0;}
 public Map<String,Long> stock(){Map<String,Long> out=new HashMap<>(ender.totals());sacks.forEach((id,n)->{if(!uncertainSacks.contains(id))out.merge(id,n,Long::sum);});return out;}
 public void claimed(Book book,String id,long n){
  long remaining=n;for(var e:forge)if(e.active&&e.id.equals(id)){long used=Math.min(remaining,e.count);e.count-=used;remaining-=used;if(e.count==0)e.active=false;}
  remaining=n;for(var e:book.diary)if(e.active&&e.id.equals(id)){long used=Math.min(remaining,e.count);e.count-=used;remaining-=used;if(e.count==0)e.active=false;}
 }
}
