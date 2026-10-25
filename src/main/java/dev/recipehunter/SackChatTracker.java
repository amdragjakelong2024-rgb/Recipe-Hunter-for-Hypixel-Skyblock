package dev.recipehunter;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * Hypixel supplies itemized sack changes as hover data on [Sacks] messages.
 * Never allocate an aggregate "+N items" to a particular ore.
 */
public final class SackChatTracker {
 private SackChatTracker(){}
 public static Map<String,Long> changes(Component message,Catalog catalog){
  String main=message.getString().replaceAll("§.","").trim();
  if(!main.matches("(?is)^\\[Sacks\\].*"))return Map.of();
  Set<String> tooltips=new LinkedHashSet<>();
  collect(message,tooltips,0);
  Map<String,Long> result=new HashMap<>();
  for(String tooltip:tooltips){
   SackChanges.parse(tooltip).forEach((name,n)->{
    String id=id(name,catalog);
    if(!id.isEmpty())result.merge(id,n,Math::addExact);
   });
  }
  return result;
 }
 private static void collect(Component component,Set<String> tooltips,int depth){
  if(depth>16)return;
  var hover=component.getStyle().getHoverEvent();
  if(hover instanceof HoverEvent.ShowText text)tooltips.add(text.value().getString());
  for(Component child:component.getSiblings())collect(child,tooltips,depth+1);
 }
 private static String id(String name,Catalog catalog){
  String needle=Catalog.normal(name);
  // The raw Mithril's Hypixel item ID is MITHRIL_ORE, not MITHRIL.
  if(needle.equals("mithril")&&catalog.items.containsKey("MITHRIL_ORE"))return "MITHRIL_ORE";
  if(needle.equals("enchanted mithril")&&catalog.items.containsKey("ENCHANTED_MITHRIL"))return "ENCHANTED_MITHRIL";
  for(var item:catalog.items.values())if(Catalog.normal(item.name).equals(needle))return item.id;
  return "";
 }
}
