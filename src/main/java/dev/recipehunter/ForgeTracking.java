package dev.recipehunter;
import java.util.*;
import java.util.regex.*;

/** Exact server menu/tab observations. Missing or loading slots are not empty slots. */
public final class ForgeTracking {
 private ForgeTracking(){}
 public static String clean(String s){return s.replaceAll("§.","").strip();}
 public static boolean empty(String name){return clean(name).matches("(?i)(?:Slot #[1-7]|EMPTY|LOCKED)");}
 public static CraftDiary.Entry job(Catalog c,int slot,String label,String lore,long now){
  String text=clean(lore),name=clean(label);
  Matcher timer=Pattern.compile("(?im)^\\s*Time Remaining:\\s*(.+)$").matcher(text);
  String time=timer.find()?timer.group(1):text.matches("(?is).*click to (?:claim|collect).*" )?"Ready!":"";
  return timed(c,slot,name,time,now);
 }
 private static CraftDiary.Entry timed(Catalog c,int slot,String label,String time,long now){
  if(slot<1||slot>7)return null;
  String id=c.resolve(label);var item=c.items.get(id);
  if(item==null||item.routes.stream().noneMatch(r->r.type.equals("forge")))return null;
  long seconds=0;String t=clean(time);
  if(!t.matches("(?i)(?:Ready!|Completed!)")){
   if(!t.matches("(?i)(?:[0-9]+[dhms]\\s*)+"))return null;
   Matcher m=Pattern.compile("(?i)([0-9]+)([dhms])").matcher(t);
   try{while(m.find())seconds=Math.addExact(seconds,Math.multiplyExact(Long.parseLong(m.group(1)),switch(m.group(2).toLowerCase(Locale.ROOT)){case "d"->86400L;case "h"->3600L;case "m"->60L;default->1L;}));}catch(ArithmeticException|NumberFormatException e){return null;}
  }
  if(seconds>31536000)return null;
  var e=new CraftDiary.Entry();e.key="forge:"+slot;e.source="forge";e.id=id;e.text=item.name;
  // A Forge slot is one job. Its icon stack size is not an output quantity.
  e.count=item.routes.stream().filter(r->r.type.equals("forge")).findFirst().orElseThrow().count;
  e.readyAt=now+seconds*1000;return e;
 }
 public static void replace(List<CraftDiary.Entry> jobs,int slot,CraftDiary.Entry job){jobs.removeIf(e->e.key.equals("forge:"+slot));if(job!=null)jobs.add(job);}
 public static final class Observations {
  private final Map<Integer,String> candidate=new HashMap<>(),accepted=new HashMap<>();
  private final Map<Integer,Integer> scans=new HashMap<>();private final List<CraftDiary.Entry> jobs;
  public Observations(List<CraftDiary.Entry> jobs){this.jobs=jobs;}
  public void clear(){candidate.clear();accepted.clear();scans.clear();}
  public void accept(int slot,CraftDiary.Entry entry){
   String signature=entry==null?"empty":entry.id+":"+entry.count;
   int count=signature.equals(candidate.put(slot,signature))?scans.getOrDefault(slot,0)+1:1;scans.put(slot,count);
   if(count>=2&&!signature.equals(accepted.get(slot))){replace(jobs,slot,entry);accepted.put(slot,signature);}
   else if(count>=2&&entry!=null){
    var current=jobs.stream().filter(e->e.key.equals("forge:"+slot)).findFirst().orElse(null);
    if(current!=null&&current.active&&current.count>0){if(entry.readyAt<current.readyAt||Math.abs(entry.readyAt-current.readyAt)>3000)current.readyAt=entry.readyAt;}
    else if(entry.readyAt>System.currentTimeMillis()+60000){replace(jobs,slot,entry);}
   }
  }
 }
 public static void tab(Catalog c,List<CraftDiary.Entry> jobs,String text,long now){tab(c,jobs,text,now,(slot,e)->replace(jobs,slot,e));}
 public interface SlotObserver {void accept(int slot,CraftDiary.Entry e,ForgeState.Slot state);}
 public static void tab(Catalog c,List<CraftDiary.Entry> jobs,String text,long now,java.util.function.BiConsumer<Integer,CraftDiary.Entry> observer){tabSlots(c,text,now,(slot,e,state)->observer.accept(slot,e));}
 public static void tabSlots(Catalog c,String text,long now,SlotObserver observer){
  String cleaned=clean(text);
  if(!Pattern.compile("(?im)^\\s*Forges?\\s*(?::|\\([0-9]+\\))?\\s*$").matcher(cleaned).find())return;
  for(String line:cleaned.split("\\R")){
   Matcher m=Pattern.compile("^\\s*([1-7])\\)\\s*(.+?)\\s*$").matcher(line);if(!m.matches())continue;
   int slot=Integer.parseInt(m.group(1));String body=m.group(2);
   if(empty(body)){observer.accept(slot,null,body.equalsIgnoreCase("LOCKED")?ForgeState.Slot.LOCKED:ForgeState.Slot.EMPTY);continue;}
   int sep=body.lastIndexOf(":");if(sep<0)continue;
   var entry=timed(c,slot,body.substring(0,sep).strip(),body.substring(sep+1),now);
   if(entry!=null)observer.accept(slot,entry,ForgeState.Slot.BUSY);
  }
 }
}
