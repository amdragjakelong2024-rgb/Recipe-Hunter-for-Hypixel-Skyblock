package dev.recipehunter;

import java.util.*;
import java.util.regex.*;

/** Hypixel [Sacks] hover details; the headline alone never identifies materials. */
public final class SackChanges {
 private SackChanges(){}
 private static final Pattern ITEM=Pattern.compile(
  "^\\s*([+-]?)\\s*([0-9][0-9,]*)(?:\\s*[x×])?\\s+(.+?)\\s*\\(([^)]*\\bSacks?\\b[^)]*)\\)\\s*$",
  Pattern.CASE_INSENSITIVE);
 private static final Pattern WINDOW=Pattern.compile("(?i)\\(Last\\s+([0-9]+)s\\.\\)");
 public static Map<String,Long> parse(String tooltip){
  Map<String,Long> changes=new LinkedHashMap<>();
  if(tooltip==null)return changes;
  int section=0;
  for(String raw:tooltip.replaceAll("§.","").split("\\R")){
   String line=raw.strip();
   if(line.equalsIgnoreCase("Added items:")){section=1;continue;}
   if(line.equalsIgnoreCase("Removed items:")){section=-1;continue;}
   if(section==0)continue;
   Matcher m=ITEM.matcher(line);
   if(!m.matches())continue;
   try{
    long n=Long.parseLong(m.group(2).replace(",",""));
    if(n<=0||n>100_000_000)continue;
    String name=m.group(3).strip();
    if(name.isBlank())continue;
    int direction=m.group(1).equals("-")?-1:m.group(1).equals("+")?1:section;
    changes.merge(name,Math.multiplyExact(direction,n),Math::addExact);
   }catch(ArithmeticException|NumberFormatException ignored){}
  }
  changes.values().removeIf(n->n==0);
  return changes;
 }
 public static int secondsBack(String header){
  Matcher m=WINDOW.matcher(header==null?"":header);
  if(m.find())try{return Math.min(120,Math.max(1,Integer.parseInt(m.group(1))));}catch(NumberFormatException ignored){}
  return 15;
 }
}
