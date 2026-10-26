package dev.recipehunter;
import net.minecraft.world.item.*;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.*;
import com.google.common.collect.ImmutableMultimap;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class Icons {
 private static final Map<String,ItemStack> CACHE=new HashMap<>();private static JsonObject definitions;private static Object resources;private static long refreshed;
 private static JsonObject definitions(){if(definitions==null){try(var in=Icons.class.getResourceAsStream("/assets/recipehunter/icons.json.gz")){definitions=JsonParser.parseReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){definitions=new JsonObject();}}return definitions;}
 public static ItemStack stack(RecipeHunter app,String id){
  if(resources!=app.mc.getResourceManager()||System.nanoTime()-refreshed>15_000_000_000L){resources=app.mc.getResourceManager();refreshed=System.nanoTime();CACHE.clear();}
  if(app.mc.player!=null)for(ItemStack s:app.mc.player.getInventory().getNonEquipmentItems())if(Ownership.resolvedId(s,app.catalog).equals(id))return s.copy();
  return CACHE.computeIfAbsent(id,k->build(app,k)).copy();
 }
 private static ItemStack build(RecipeHunter app,String id){
  try{JsonObject d=definitions().has(id)?definitions().getAsJsonObject(id):new JsonObject();String icon=d.has("base")?d.get("base").getAsString():app.catalog.item(id).icon;
   Item base=BuiltInRegistries.ITEM.getValue(Identifier.parse(icon));ItemStack s=new ItemStack(base==null||base==Items.AIR?Items.PAPER:base);
   if(d.has("texture")){String texture=d.get("texture").getAsString();Property property=d.has("signature")?new Property("textures",texture,d.get("signature").getAsString()):new Property("textures",texture);
    GameProfile profile=new GameProfile(UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)),"RecipeHunter",new PropertyMap(ImmutableMultimap.of("textures",property)));s.set(DataComponents.PROFILE,ResolvableProfile.createResolved(profile));}
   if(d.has("dye"))s.set(DataComponents.DYED_COLOR,new DyedItemColor(d.get("dye").getAsInt()));
   if(d.has("glint"))s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
   if(d.has("model")){Identifier model=Identifier.parse(d.get("model").getAsString());Identifier resource=Identifier.fromNamespaceAndPath(model.getNamespace(),"items/"+model.getPath()+".json");if(app.mc.getResourceManager().getResource(resource).isPresent())s.set(DataComponents.ITEM_MODEL,model);}
   s.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(app.catalog.item(id).name));
   var tag=new net.minecraft.nbt.CompoundTag();var attrs=new net.minecraft.nbt.CompoundTag();attrs.putString("id",id);tag.put("ExtraAttributes",attrs);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return s;
  }catch(Exception e){return new ItemStack(Items.PAPER);}
 }
 public static void clear(){CACHE.clear();}
}
