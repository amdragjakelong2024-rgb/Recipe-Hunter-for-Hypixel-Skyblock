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
   lines.add("The green trail only uses nearby visible gemstone blocks and loaded walkable ground. Explore until a reachable vein is visible; generated structures do not have fixed coordinates.");
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
 public void reset(){target=null;path=List.of();status="Choose a material and press Guide.";}
 public void tick(RecipeHunter app,String context){
  Minecraft mc=app.mc;if(!app.book.assistant||mc.player==null||mc.level==null){reset();return;}
  String id=app.book.assistantItem;if(id.isEmpty())return;tick++;
  if(tick%40==0){
   path=List.of();target=null;String gem=gem(id);
   if(!gem.isEmpty()&&WorldContext.hollows(context))findGem(mc,gem);
   if((id.startsWith("YOUNG_DRAGON")||id.equals("YOUNG_FRAGMENT")||id.equals("SUMMONING_EYE"))&&(context.contains("the end")||context.contains("dragon")||context.contains("zealot"))){
    double nearest=24*24;for(var e:mc.level.entitiesForRendering())if(e.getName().getString().toLowerCase(Locale.ROOT).contains("zealot")&&mc.player.hasLineOfSight(e)){
     double d=mc.player.distanceToSqr(e);if(d<nearest){nearest=d;target=e.blockPosition();}
    }
   }
   if(target==null){
    if(!gem.isEmpty()&&WorldContext.hollows(context))status="No visible "+gem.toLowerCase(Locale.ROOT)+" vein nearby. "+guide(app.catalog,id).get(1);
    else status=guide(app.catalog,id).getFirst();
   }
   else {
    WalkingPath.Point start=new WalkingPath.Point(mc.player.blockPosition().getX(),mc.player.blockPosition().getY(),mc.player.blockPosition().getZ());
    List<WalkingPath.Point> best=List.of();
    for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){var goal=new WalkingPath.Point(target.getX()+d[0],target.getY(),target.getZ()+d[1]);if(!walkable(mc,goal))continue;var candidate=WalkingPath.find(start,goal,p->walkable(mc,p),1800);if(!candidate.isEmpty()&&(best.isEmpty()||candidate.size()<best.size()))best=candidate;}
    path=best;status=path.isEmpty()?"Visible vein found; no local walking path. Explore a passage.":"Follow the green trail to "+(gem.isEmpty()?"visible Zealot":gem.toLowerCase(Locale.ROOT))+". "+(path.size()-1)+" steps.";
   }
  }
  if(tick%6==0&&mc.screen==null)for(int i=0;i<Math.min(path.size(),70);i++){var p=path.get(i);mc.level.addParticle(new DustParticleOptions(0x42ed83,0.7f),p.x()+0.5,p.y()+0.15,p.z()+0.5,0,0,0);if(i>0){var prev=path.get(i-1);mc.level.addParticle(new DustParticleOptions(0x42ed83,0.7f),(p.x()+prev.x())/2.0+0.5,(p.y()+prev.y())/2.0+0.15,(p.z()+prev.z())/2.0+0.5,0,0,0);}}
 }
 private void findGem(Minecraft mc,String gem){
  String color=switch(gem){case "JADE"->"lime";case "AMBER"->"orange";case "AMETHYST"->"purple";case "SAPPHIRE"->"light_blue";case "RUBY"->"red";case "TOPAZ"->"yellow";default->"magenta";};
  BlockPos base=mc.player.blockPosition();double nearest=Double.MAX_VALUE;
  for(int x=-12;x<=12;x++)for(int y=-5;y<=5;y++)for(int z=-12;z<=12;z++){
   BlockPos p=base.offset(x,y,z);if(!mc.level.hasChunkAt(p))continue;
   String block=BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(p).getBlock()).getPath();if(!block.equals(color+"_stained_glass")&&!block.equals(color+"_stained_glass_pane"))continue;
   double dist=mc.player.position().distanceToSqr(Vec3.atCenterOf(p));if(dist>=nearest)continue;
   var hit=mc.level.clip(new ClipContext(mc.player.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
   if(hit.getBlockPos().equals(p)){target=p;nearest=dist;}
  }
 }
 private boolean walkable(Minecraft mc,WalkingPath.Point p){
  BlockPos pos=new BlockPos(p.x(),p.y(),p.z());if(!mc.level.hasChunkAt(pos))return false;
  var feet=mc.level.getBlockState(pos);var head=mc.level.getBlockState(pos.above());var ceiling=mc.level.getBlockState(pos.above(2));var floor=mc.level.getBlockState(pos.below());
  String floorId=BuiltInRegistries.BLOCK.getKey(floor.getBlock()).getPath();
  return feet.getCollisionShape(mc.level,pos).isEmpty()&&head.getCollisionShape(mc.level,pos.above()).isEmpty()&&ceiling.getCollisionShape(mc.level,pos.above(2)).isEmpty()&&feet.getFluidState().isEmpty()&&head.getFluidState().isEmpty()&&!floor.getCollisionShape(mc.level,pos.below()).isEmpty()&&!Set.of("magma_block","campfire","soul_campfire","cactus","fire").contains(floorId);
 }
}
