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
 private final ForgeTracking.Observations forgeObservations=new ForgeTracking.Observations(forge);
 private Object menu;private int ticks;private long lastForgeMenuAt;
 public void clear(){forgeObservations.clear();lastForgeMenuAt=0;ender.clear();sacks.clear();sackSnapshots.clear();uncertainSacks.clear();forge.clear();menu=null;ticks=0;lastSackSnapshotAt=0;lastSackMessageAt=0;lastSackNoticeAt=0;sackNoticeCount=0;sackParsedCount=0;sackUnparsedCount=0;}
 public static long stored(String lore){
  Matcher m=Pattern.compile("(?im)^(?:Stored|Amount)\\s*:\\s*([0-9][0-9,]*)(?=\\s*(?:/|$))").matcher(lore.strip());
  if(!m.find())return -1;try{return Long.parseLong(m.group(1).replace(",",""));}catch(NumberFormatException e){return -1;}
 }
 public static String enderPage(String title){
  Matcher m=Pattern.compile("(?i)^Ender Chest(?: \\(([0-9]+)/[0-9]+\\)| Page ([0-9]+))?$").matcher(title);
  if(!m.matches())return "";return "page:"+(m.group(1)!=null?m.group(1):m.group(2)!=null?m.group(2):"1");
 }
 public void observeSack(String id,long amount,long at){if(amount<0)return;sacks.put(id,amount);sackSnapshots.put(id,at);uncertainSacks.remove(id);lastSackSnapshotAt=at;observedAt=at;}
 public boolean serverBalance(String message,Catalog catalog){
  Matcher m=Pattern.compile("^You have no (.+) in your Sacks!$").matcher(message.replaceAll("§.","").strip());
  if(!m.matches())return false;String id=catalog.resolve(m.group(1));if(id.isEmpty())return false;
  observeSack(id,0,System.currentTimeMillis());return true;
 }
 public void tick(RecipeHunter app){
  if(!(app.mc.screen instanceof AbstractContainerScreen<?> screen)){menu=null;ticks=0;return;}
  if(menu!=screen.getMenu()){menu=screen.getMenu();ticks=0;}if(++ticks<5||ticks%5!=0)return;
  String title=screen.getTitle().getString().replaceAll("§.","").trim();
  boolean ec=!enderPage(title).isEmpty(),sack=title.matches("(?i)(?:Small |Medium |Large )?(?:Enchanted )?[A-Za-z ]+ Sack"),fg=title.equalsIgnoreCase("The Forge");
  if(!ec&&!sack&&!fg)return;Map<String,Long> contents=new HashMap<>();
  for(var slot:screen.getMenu().slots){if(slot.container==app.mc.player.getInventory())continue;var stack=slot.getItem();String id=Ownership.resolvedId(stack,app.catalog);var lore=stack.get(DataComponents.LORE);String text=lore==null?"":String.join("\n",lore.lines().stream().map(l->l.getString().replaceAll("§.","")).toList());
   if(ec&&slot.getContainerSlot()<slot.container.getContainerSize()-9&&!id.isEmpty()&&!text.toLowerCase(Locale.ROOT).contains("click to view"))contents.merge(id,(long)stack.getCount(),Long::sum);
   if(sack){long n=stored(text);if(n>=0){
    if(id.isEmpty()){String name=Catalog.normal(stack.getHoverName().getString());id=app.catalog.resolve(name);}
    if(!id.isEmpty()){observeSack(id,n,System.currentTimeMillis());}
   }}
   if(fg&&slot.getContainerSlot()>=10&&slot.getContainerSlot()<=16){
    int forgeSlot=slot.getContainerSlot()-9;String name=stack.getHoverName().getString();
    if(ForgeTracking.empty(name))forgeObservations.accept(forgeSlot,null);
    else {var job=ForgeTracking.job(app.catalog,forgeSlot,name,text,System.currentTimeMillis());if(job!=null)forgeObservations.accept(forgeSlot,job);}
   }
  }
  if(ec)ender.replace(enderPage(title),contents);if(fg)lastForgeMenuAt=System.currentTimeMillis();
  if(sack)lastSackSnapshotAt=System.currentTimeMillis();
  observedAt=System.currentTimeMillis();
 }
 public void tabForge(RecipeHunter app){
  if(System.currentTimeMillis()-lastForgeMenuAt<2000||app.mc.getConnection()==null)return;
  StringBuilder lines=new StringBuilder();for(var p:app.mc.getConnection().getListedOnlinePlayers())if(p.getTabListDisplayName()!=null)lines.append(p.getTabListDisplayName().getString()).append('\n');
  ForgeTracking.tab(app.catalog,forge,lines.toString(),System.currentTimeMillis(),forgeObservations::accept);
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
   // Once the overlap window ends, resume from a conservative observed lower bound.
   // The old snapshot is ambiguous; keeping its uncertainty forever froze mining HUDs.
   if(uncertainSacks.remove(e.getKey())){sacks.remove(e.getKey());sackSnapshots.remove(e.getKey());}
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
