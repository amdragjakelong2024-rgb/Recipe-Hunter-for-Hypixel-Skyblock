package dev.recipehunter;

import java.util.*;
import java.util.regex.*;

/** Parses Hypixel's itemized [Sacks] chat hover tooltip, not its aggregate total. */
public final class SackChanges {
 private SackChanges(){}
 private static final Pattern ITEM=Pattern.compile("^\\s*([0-9][0-9,]*)\\s+(.+?)\\s*\\(([^)]*\\bSacks?\\b[^)]*)\\)\\s*$",Pattern.CASE_INSENSITIVE);
 public static Map<String,Long> parse(String tooltip){
  Map<String,Long> changes=new LinkedHashMap<>();int sign=0;
  if(tooltip==null)return changes;
  for(String raw:tooltip.replaceAll("§.","").split("\\R")){
   String line=raw.trim();
   if(line.equalsIgnoreCase("Added items:")){sign=1;continue;}
   if(line.equalsIgnoreCase("Removed items:")){sign=-1;continue;}
   Matcher matcher=ITEM.matcher(line);
   if(sign==0||!matcher.matches())continue;
   try{
    long n=Long.parseLong(matcher.group(1).replace(",",""));
    if(n<1 || n>100_000_000)continue;
    String name=matcher.group(2).trim();
    if(name.isEmpty())continue;
    changes.merge(name,sign*n,Math::addExact);
   }catch(ArithmeticException ignored){}
  }
  changes.values().removeIf(n->n==0);
  return changes;
 }
 public static int secondsBack(String header){
  Matcher m=Pattern.compile("(?i)\\(Last\\s+([0-9]+)s\\.\\)").matcher(header==null?"":header);
  if(m.find())try{return Math.min(120,Math.max(1,Integer.parseInt(m.group(1))));}catch(NumberFormatException ignored){}
  return 15;
 }
}
