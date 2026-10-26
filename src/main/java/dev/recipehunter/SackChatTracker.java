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
  message.visit((style,text)->{
   if(style.getHoverEvent() instanceof HoverEvent.ShowText show)hovers.add(show.value().getString());
   return Optional.empty();
  },net.minecraft.network.chat.Style.EMPTY);
  Map<String,Long> result=new LinkedHashMap<>();
  for(String tooltip:hovers){
   SackChanges.parse(tooltip).forEach((name,n)->{
    String id=id(name,catalog);
    if(!id.isEmpty())result.merge(id,n,Math::addExact);
   });
  }
  return result;
 }
 public static String id(String label,Catalog catalog){
  return catalog.resolve(label);
 }
}
