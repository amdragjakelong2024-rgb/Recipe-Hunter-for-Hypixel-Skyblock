package dev.recipehunter;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.inventory.ChestMenu;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;

/** Only live inventory and the player's own Pets menu are ownership evidence.
 * Recipe, auction and other players' profile screens must never count. */
public final class Ownership {
    private final Map<String,Map<String,String>> petPages=new HashMap<>();
    public final StorageLedger storage=new StorageLedger();
    private String pendingChest="",activeChest="";private long clickAt;private Object activeMenu;private int stableTicks;
    public void clicked(Minecraft mc,BlockPos pos,String context){
        pendingChest="";if(mc.level==null||!WorldContext.island(context))return;
        var state=mc.level.getBlockState(pos);
        if(!(state.getBlock() instanceof ChestBlock)&&!state.is(Blocks.BARREL))return;
        BlockPos canonical=pos;
        if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){var other=pos.relative(ChestBlock.getConnectedDirection(state));if(other.asLong()<pos.asLong())canonical=other;}
        pendingChest=canonical.toShortString();clickAt=System.currentTimeMillis();
    }
    public void scanChest(Minecraft mc,String context){
        if(!(mc.screen instanceof AbstractContainerScreen<?> screen)||!(screen.getMenu() instanceof ChestMenu)||!WorldContext.island(context)) {activeMenu=null;activeChest="";stableTicks=0;return;}
        if(activeMenu!=screen.getMenu()){
            activeMenu=screen.getMenu();activeChest=System.currentTimeMillis()-clickAt<4000?pendingChest:"";pendingChest="";stableTicks=0;
        }
        if(activeChest.isEmpty()||++stableTicks<5)return;
        Map<String,Long> content=new HashMap<>();
        for(var slot:screen.getMenu().slots)if(slot.container!=mc.player.getInventory()){String id=id(slot.getItem());if(!id.isEmpty())content.merge(id,(long)slot.getItem().getCount(),Long::sum);}
        storage.replace(activeChest,content);
    }

    public boolean petsVisited=false;
    public void clear(){petPages.clear();storage.clear();pendingChest="";activeChest="";activeMenu=null;petsVisited=false;}
    public static CompoundTag tag(ItemStack s){
        CompoundTag t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return t.contains("id")||t.contains("petInfo")?t:t.getCompoundOrEmpty("ExtraAttributes");
    }
    public static String petId(ItemStack s){
        return petJson(tag(s).getStringOr("petInfo",""));
    }
    public static String petJson(String raw){
        if(raw.isEmpty())return "";
        try{JsonObject p=JsonParser.parseString(raw).getAsJsonObject();String type=p.get("type").getAsString(),tier=p.get("tier").getAsString();int r=List.of("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC").indexOf(tier);return r<0?"":type+";"+r;}catch(RuntimeException e){return "";}
    }
    public static String id(ItemStack s){
        if(s.isEmpty())return "";String pet=petId(s);if(!pet.isEmpty())return pet;
        String id=tag(s).getStringOr("id","");return id;
    }
    public void scanPets(Minecraft mc){
        if(!(mc.screen instanceof AbstractContainerScreen<?> screen)||mc.player==null)return;
        String title=screen.getTitle().getString().replaceAll("§.","");
        if(!title.matches("Pets(?: \\(\\d+/\\d+\\))?"))return;
        petsVisited=true;Map<String,String> pets=new HashMap<>();
        for(var slot:screen.getMenu().slots){
            if(slot.container==mc.player.getInventory())continue;
            ItemStack s=slot.getItem();String id=petId(s);if(id.isEmpty())continue;
            try{JsonObject p=JsonParser.parseString(tag(s).getStringOr("petInfo","")).getAsJsonObject();
                String uuid=p.has("uuid")?p.get("uuid").getAsString():title+":"+slot.index;
                pets.put(uuid,id);
            }catch(RuntimeException ignored){}
        }
        if(!pets.isEmpty())petPages.put(title,pets);
    }
    public void clearPets(){petPages.clear();petsVisited=false;}
    public int petPageCount(){return petPages.size();}
    public Map<String,Long> stock(Minecraft mc){
        Map<String,Long> result=inventory(mc);storage.totals().forEach((id,n)->result.merge(id,n,Long::sum));
        Map<String,String> pets=new HashMap<>();for(var page:petPages.values())pets.putAll(page);for(String id:pets.values())result.merge(id,1L,Math::max);return result;
    }
    public Map<String,Long> inventory(Minecraft mc){
        Map<String,Long> result=new HashMap<>();if(mc.player==null)return result;
        for(ItemStack s:mc.player.getInventory().getNonEquipmentItems()) {
            String id=id(s);if(!id.isEmpty())result.merge(id,(long)s.getCount(),Long::sum);
        }
        for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND)){ItemStack s=mc.player.getItemBySlot(slot);String id=id(s);if(!id.isEmpty())result.merge(id,(long)s.getCount(),Long::sum);}

        return result;
    }
}
