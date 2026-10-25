package dev.recipehunter;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Reads itemized server chat hover tooltips, not the untyped sack headline. */
public final class SackChatTracker {
 private SackChatTracker(){}
 public static boolean isNotice(Component message){
  return message!=null&&message.getString().replaceAll("§.","").contains("[Sacks]");
 }
 public static Map<String,Long> changes(Component message,Catalog catalog){
  if(catalog==null||!isNotice(message))return Map.of();
  Set<String> hovers=new LinkedHashSet<>();
  collect(message,hovers,0);
  Map<String,Long> result=new LinkedHashMap<>();
  for(String tooltip:hovers){
   SackChanges.parse(tooltip).forEach((name,n)->{
    String id=id(name,catalog);
    if(!id.isEmpty())result.merge(id,n,Math::addExact);
   });
  }
  return result;
 }
 private static void collect(Component component,Set<String> tooltips,int depth){
  if(depth>24)return;
  HoverEvent hover=component.getStyle().getHoverEvent();
  if(hover instanceof HoverEvent.ShowText show)tooltips.add(show.value().getString());
  for(Component sibling:component.getSiblings())collect(sibling,tooltips,depth+1);
 }
 public static String id(String label,Catalog catalog){
  return catalog.resolve(label);
 }
}
