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
 private static final Map<String,ItemStack> CACHE=new HashMap<>();
 // Rendering must not re-scan and copy the entire player inventory for each icon,
 // especially with several visible goals. Build one read-only icon index per client tick.
 private static final Map<String,ItemStack> INVENTORY_ICONS=new HashMap<>();
 private static JsonObject definitions;private static Object resources,inventoryPlayer;
 private static int inventoryTick=Integer.MIN_VALUE;
 private static void inventoryIcons(RecipeHunter app){
  var player=app.mc.player;
  if(player==inventoryPlayer&&(player==null||player.tickCount==inventoryTick))return;
  inventoryPlayer=player;inventoryTick=player==null?Integer.MIN_VALUE:player.tickCount;
  INVENTORY_ICONS.clear();
  if(player==null||app.catalog==null)return;
  for(ItemStack s:player.getInventory().getNonEquipmentItems()){
   if(s.isEmpty())continue;
   String id=Ownership.resolvedId(s,app.catalog);
   if(!id.isEmpty()&&!INVENTORY_ICONS.containsKey(id))INVENTORY_ICONS.put(id,s.copy());
  }
 }
 private static JsonObject definitions(){if(definitions==null){try(var in=Icons.class.getResourceAsStream("/assets/recipehunter/icons.json.gz")){definitions=JsonParser.parseReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){definitions=new JsonObject();}}return definitions;}
 public static ItemStack stack(RecipeHunter app,String id){
  if(resources!=app.mc.getResourceManager()){resources=app.mc.getResourceManager();CACHE.clear();}
  inventoryIcons(app);
  // These cached copies are used only as read-only render inputs (g.item).
  ItemStack current=INVENTORY_ICONS.get(id);
  return current!=null?current:CACHE.computeIfAbsent(id,k->build(app,k));
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
 public static void invalidateInventory(){inventoryTick=Integer.MIN_VALUE;}
 public static void clear(){CACHE.clear();INVENTORY_ICONS.clear();inventoryPlayer=null;inventoryTick=Integer.MIN_VALUE;}
}
