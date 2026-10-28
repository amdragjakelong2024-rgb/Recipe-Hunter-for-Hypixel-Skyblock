package dev.recipehunter;
import java.util.*;

/** One virtual goal for four real SkyBlock armor recipes. */
public final class ArmorSets {
 public static final String PREFIX="ARMOR_SET_";
 private static final List<String> SLOTS=List.of("Helmet","Chestplate","Leggings","Boots");
 private ArmorSets(){}
 public static boolean isSet(Book.Goal goal){return goal!=null&&goal.id!=null&&goal.id.startsWith(PREFIX)&&goal.armorPieces!=null&&goal.armorPieces.size()==4;}
 public static String iconId(Book.Goal goal){return isSet(goal)?goal.armorPieces.getFirst():goal.id;}
 private static String stem(String name){
  for(String slot:SLOTS)if(name.endsWith(" "+slot))return name.substring(0,name.length()-slot.length()-1).trim();
  return "";
 }
 public static String id(String name){String base=stem(name);return base.isEmpty()?"":PREFIX+Catalog.normal(base).toUpperCase(Locale.ROOT).replace(' ','_');}
 /** Requires all four pieces with unique, unambiguous catalogue names. */
 public static List<String> detect(Catalog catalog,String selected){
  if(catalog==null||selected==null||!catalog.items.containsKey(selected)||selected.startsWith(PREFIX))return List.of();
  String base=stem(catalog.item(selected).name);if(base.isEmpty())return List.of();
  List<String> result=new ArrayList<>();
  for(String slot:SLOTS){String item=catalog.resolve(base+" "+slot);if(item.isEmpty())return List.of();result.add(item);}
  return result.contains(selected)&&new HashSet<>(result).size()==4?List.copyOf(result):List.of();
 }
 /** Register virtual crafting recipe only for a verified saved set. */
 public static boolean register(Catalog catalog,Book.Goal goal){
  if(catalog==null||!isSet(goal)||new HashSet<>(goal.armorPieces).size()!=4)return false;
  for(String member:goal.armorPieces)if(!catalog.items.containsKey(member))return false;
  if(!goal.id.equals(id(catalog.item(goal.armorPieces.getFirst()).name)))return false;
  if(!detect(catalog,goal.armorPieces.getFirst()).equals(goal.armorPieces))return false;
  if(catalog.items.containsKey(goal.id))return catalog.item(goal.id).routes.stream().anyMatch(r->"armor_set".equals(r.type));
  Catalog.Item item=new Catalog.Item();item.id=goal.id;
  item.name=stem(catalog.item(goal.armorPieces.getFirst()).name)+" Set";
  item.icon=catalog.item(goal.armorPieces.getFirst()).icon;
  Catalog.Route recipe=new Catalog.Route();recipe.type="armor_set";recipe.count=1;
  for(String member:goal.armorPieces)recipe.inputs.add(new Catalog.Ingredient(member,1));
  item.routes.add(recipe);catalog.items.put(item.id,item);return true;
 }
 /** Counts material-ready pieces, not equipped or completed crafting. */
 public static int readyPieces(RecipeProgress.Node root){
  if(root==null)return 0;int ready=0;for(var node:root.children)if(node.total>0&&node.fraction()>=1-1e-9)ready++;return ready;
 }
 public static void averageReadiness(Book.Goal goal,RecipeProgress.Node root){
  if(isSet(goal)&&root.children.size()==4){
   double average=root.children.stream().mapToDouble(RecipeProgress.Node::fraction).average().orElse(0);
   root.equivalent=Math.clamp(root.total*average,0,root.total);
  }
 }
}
