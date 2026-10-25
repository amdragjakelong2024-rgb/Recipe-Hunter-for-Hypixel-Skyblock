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
  String name=Catalog.normal(label);
  if(name.equals("mithril")&&catalog.items.containsKey("MITHRIL_ORE"))return "MITHRIL_ORE";
  if(name.equals("enchanted mithril")&&catalog.items.containsKey("ENCHANTED_MITHRIL"))return "ENCHANTED_MITHRIL";
  // Exact names only: fuzzy matches would silently count unrelated materials.
  for(Catalog.Item item:catalog.items.values())if(Catalog.normal(item.name).equals(name))return item.id;
  return "";
 }
}
