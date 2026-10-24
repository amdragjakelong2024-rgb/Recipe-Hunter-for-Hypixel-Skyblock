package dev.recipehunter;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public final class Assistant {
 public String status="Choose a material and press Guide.";public List<WalkingPath.Point> path=List.of();private BlockPos target;private int tick;
 public static String gem(String id){for(String g:List.of("JADE","AMBER","AMETHYST","SAPPHIRE","RUBY","TOPAZ","JASPER"))if(id.endsWith("_"+g+"_GEM"))return g;return "";}
 public static List<String> guide(Catalog c,String id){
  String gem=gem(id);List<String> lines=new ArrayList<>();
  if(!gem.isEmpty()){
   String region=switch(gem){case "JADE"->"Mithril Deposits / Mines of Divan";case "AMBER"->"Goblin Holdout";case "AMETHYST"->"Jungle";case "SAPPHIRE"->"Precursor Remnants";case "TOPAZ"->"Magma Fields";case "JASPER"->"Fairy Grotto (random location)";default->"Crystal Hollows";};
   lines.add("Type /warp hollows. If you have the required scroll, /warp nucleus is another starting point.");lines.add("Find "+region+". Mine "+gem.toLowerCase(Locale.ROOT)+" with sufficient Breaking Power; compress gems through the recipe tiers.");
   lines.add("Generated structures do not have fixed coordinates. Time estimates use your measured collection rate.");
   if(id.startsWith("PERFECT_"))lines.add("Perfect gems also require their crystal and the Forge. Check the recipe for amounts and unlock requirements.");
  }else if(id.equals("HYPERION")||id.equals("NECRON_HANDLE")||id.equals("NECRON_BLADE")||id.equals("WITHER_CATALYST")){
   lines.add("Type /warp dungeon_hub. Join a party for Catacombs Floor VII or Master VII when unlocked.");lines.add("Necron's Handle is a Bedrock Chest drop; keep coins for opening the chest. A Hyperion also requires its other recipe materials; Handle alone is not the whole weapon.");
  }else if(id.startsWith("YOUNG_DRAGON")||id.equals("YOUNG_FRAGMENT")||id.equals("SUMMONING_EYE")){
   lines.add("Type /warp end. Reach Dragon's Nest and fight Zealots / Special Zealots for Summoning Eyes.");lines.add("Eight Summoning Eyes total summon a dragon. The type is random: this does NOT guarantee a Young Dragon or leggings.");lines.add("Young Dragon Leggings can be crafted from 70 Young Dragon Fragments. Loot eligibility and damage matter for dragon drops.");
  }else if(id.equals("SLUDGE_JUICE")){
   lines.add("Type /warp hollows and find the Jungle. Mine Hard Stone with a Jungle Pickaxe, or kill Sludges.");
  }else{
   for(String s:c.item(id).sources)if(!s.toLowerCase(Locale.ROOT).contains("bazaar")&&!s.toLowerCase(Locale.ROOT).contains("auction"))lines.add(s);
   if(lines.isEmpty())lines.add("No verified navigation route recorded for this item. Open Sources or inspect its ingredients.");
  }
  return lines;
 }
 public void reset(){path=List.of();status="Choose a goal for grind recommendations.";}
 public void tick(RecipeHunter app,String context){
  if(!app.book.assistant)return;if(++tick%40!=0)return;
  Book.Goal goal=app.book.find(app.book.assistantItem);if(goal==null)goal=app.book.find(app.book.last);
  if(goal==null){status="Save a recipe to compare missing materials.";return;}
  var ranked=app.estimates.rank(app,goal,false);
  status=ranked.isEmpty()?"No missing materials; check pending Forge jobs.":ranked.getFirst().label(app.catalog);
 }
}
