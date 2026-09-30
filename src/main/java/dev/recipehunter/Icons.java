package dev.recipehunter;
import net.minecraft.world.item.*;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
public final class Icons {
 public static ItemStack stack(RecipeHunter app,String id){
  if(app.mc.player!=null)for(ItemStack s:app.mc.player.getInventory().getNonEquipmentItems())if(Ownership.id(s).equals(id))return s.copy();
  try{String icon=app.catalog.item(id).icon;if(icon.equals("minecraft:skull"))icon="minecraft:player_head";
   String gem=Assistant.gem(id);if(!gem.isEmpty())icon="minecraft:"+switch(gem){case "JADE"->"lime";case "RUBY"->"red";case "AMBER"->"orange";case "AMETHYST"->"purple";case "SAPPHIRE"->"light_blue";case "TOPAZ"->"yellow";default->"magenta";}+"_stained_glass";
   Item base=BuiltInRegistries.ITEM.getValue(Identifier.parse(icon));return new ItemStack(base==null||base==Items.AIR?Items.PAPER:base);
  }catch(Exception e){return new ItemStack(Items.PAPER);}
 }
}
