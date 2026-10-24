package dev.recipehunter;
import java.util.*;
import java.util.regex.*;

/** Journal text is never treated as inventory. Reservations are explicit and editable. */
public final class CraftDiary {
 public static final class Entry {
  public String key=UUID.randomUUID().toString(),text="",id="",source="diary";
  public long count,readyAt,createdAt=System.currentTimeMillis(); public boolean active=true;
 }
 public static Entry parse(Catalog catalog,String text,long now){
  Entry e=new Entry();e.text=text;e.createdAt=now;
  String normalized=text.replaceAll("(?iuU)\\b(dvě|dva|two)\\b","2").replaceAll("(?iuU)\\b(jeden|jednu|one)\\b","1").replaceAll("(?iuU)\\b(pět|five)\\b","5").replaceAll("(?iuU)mithril(?:ů|u|y|i)\\b","Mithril");
  String n=Catalog.normal(normalized);
  if(!n.contains("forge")||n.matches(".*\\b(not|never|cancel|cancelled|nechci|nemam|nem m|zruseno)\\b.*"))return e;
  Catalog.Item match=catalog.items.values().stream().filter(i->!i.id.endsWith("_NPC")&&!i.id.endsWith("_BOSS"))
   .filter(i->n.contains(Catalog.normal(i.name))||n.contains(Catalog.normal(i.id)))
   .max(Comparator.comparingInt(i->i.name.length())).orElse(null);
  if(match==null)return e;
  Matcher quantity=Pattern.compile("(?i)(?:^|\\s)([0-9]+)\\s*x?\\s*"+Pattern.quote(match.name)).matcher(normalized);
  if(!quantity.find())return e;
  long count;try{count=Long.parseLong(quantity.group(1));}catch(NumberFormatException ex){return e;}
  if(count<1||count>1000000)return e;
  Matcher time=Pattern.compile("(?i)([0-9]+(?:[.,][0-9]+)?)\\s*(h(?:ours?)?|hod(?:iny|in)?|m(?:in(?:utes?)?)?|s(?:ec(?:onds?)?)?)\\b").matcher(normalized);
  double seconds=0;while(time.find()){double value=Double.parseDouble(time.group(1).replace(',','.'));String unit=time.group(2).toLowerCase(Locale.ROOT);seconds+=value*(unit.startsWith("h")?3600:unit.startsWith("m")?60:1);}
  // Ambiguous ranges stay notes. No silent guessed deduction.
  if(seconds<=0||seconds>31536000||text.matches(".*[0-9]\\s*[-–]\\s*[0-9].*"))return e;
  e.id=match.id;e.count=count;e.readyAt=now+(long)(seconds*1000);return e;
 }
 public static Map<String,Long> pending(List<Entry> entries){Map<String,Long> out=new HashMap<>();for(Entry e:entries)if(e.active&&!e.id.isEmpty()&&e.count>0)out.merge(e.id,e.count,Math::addExact);return out;}
 public static Map<String,Long> mergeReservations(List<Entry> diary,List<Entry> forge){
  Map<String,Long> out=pending(diary);pending(forge).forEach((id,n)->out.merge(id,n,Math::max));return out;
 }
 public static String remaining(Entry e,long now){if(e.readyAt==0)return "note only";return e.readyAt<=now?"ready; claim not verified":String.format(Locale.US,"%.2f h left",(e.readyAt-now)/3600000.0);}
}
