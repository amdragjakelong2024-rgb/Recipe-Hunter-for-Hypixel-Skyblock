package dev.recipehunter;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.client.input.*;

/** Explicit CI-only opt-in. No server connection and no writes to a user's saved book. */
public final class UiSmoke {
 private static int ticks,stage;private static boolean pending;private static String failure="";
 public static void register(RecipeHunter app){
  if(!Boolean.getBoolean("recipehunter.uiSmoke"))return;
  ClientTickEvents.END_CLIENT_TICK.register(mc->{
   if(mc.player!=null)return;
   ticks++;
   try{
    if(stage==0&&ticks>100&&mc.screen!=null&&mc.getOverlay()==null){
     BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach(p->p.apply());
     app.ensureAccount();
     verifyStacks(app);
     app.book=new Book();app.book.animations=false;
     for(String id:new String[]{"MITHRIL_PLATE","ENCHANTED_DIAMOND_BLOCK","PERFECT_JADE_GEM","ENCHANTED_MITHRIL"})if(app.catalog.items.containsKey(id))app.book.goals.add(new Book.Goal(id));
     mc.setScreen(new RecipeBookScreen(app,true,"","MITHRIL_PLATE"));stage=1;ticks=0;
    }else if(stage==1&&ticks>40&&!pending){capture(app,"all-recipes.png");stage=2;ticks=0;}
    else if(stage==2&&ticks>20&&!pending){
     RecipeBookScreen screen=(RecipeBookScreen)mc.screen;
     EditBox field=(EditBox)screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();
     screen.mouseClicked(new MouseButtonEvent(field.getX()+3,field.getY()+3,new MouseButtonInfo(0,0)),false);
     for(char c:"mithril".toCharArray())screen.charTyped(new CharacterEvent(c));
     if(!field.getValue().equals("mithril"))throw new IllegalStateException("Search input did not receive text: "+field.getValue());
     stage=3;ticks=0;
    }else if(stage==3&&ticks>20&&!pending){
     EditBox field=(EditBox)mc.screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();
     if(!field.getValue().equals("mithril")||!field.isFocused())throw new IllegalStateException("Search focus/text lost on refresh");
     capture(app,"search-focused.png");stage=4;ticks=0;
    }else if(stage==4&&ticks>20&&!pending){mc.setScreen(new RecipeBookScreen(app,false,"",""));stage=5;ticks=0;}
    else if(stage==5&&ticks>30&&!pending){capture(app,"recipe-book.png");stage=6;ticks=0;}
    else if(stage==6&&ticks>20&&!pending){mc.setScreen(new ToolsScreen(app,"Craft Diary"));stage=7;ticks=0;}
    else if(stage==7&&ticks>20&&!pending){
     var field=journal(app);mc.screen.mouseClicked(new MouseButtonEvent(field.getX()+4,field.getY()+4,new MouseButtonInfo(0,0)),false);
     for(char c:"forge 2 Refined Mithril 4h".toCharArray())mc.screen.charTyped(new CharacterEvent(c));
     if(!app.book.diaryDraft.equals("forge 2 Refined Mithril 4h"))throw new IllegalStateException("Diary click/typing failed");
     click(app,"Save entry");if(app.book.diary.size()!=1||!app.book.diaryDraft.isEmpty())throw new IllegalStateException("Diary save failed");stage=8;ticks=0;
    }else if(stage==8&&ticks>20&&!pending){
     capture(app,"diary.png");click(app,"Edit");journal(app).setValue("forge 2 Refined Mithril 4h - checked the job");click(app,"Save changes");
     if(app.book.diary.size()!=1||!app.book.diary.getFirst().text.endsWith("checked the job"))throw new IllegalStateException("Diary edit duplicated or lost entry");
     click(app,"[ ]");if(!app.book.diary.getFirst().done)throw new IllegalStateException("Diary checkbox failed");
     click(app,"New entry");journal(app).setValue("Unfinished diary draft");mc.setScreen(new RecipeBookScreen(app,false,"",""));mc.setScreen(new ToolsScreen(app,"Craft Diary"));
     if(!journal(app).getValue().equals("Unfinished diary draft"))throw new IllegalStateException("Diary draft lost on navigation");stage=9;ticks=0;
    }else if(stage==9&&ticks>20&&!pending){
     click(app,"Delete");if(!app.book.diary.isEmpty())throw new IllegalStateException("Diary delete failed");
     if(!failure.isEmpty())throw new IllegalStateException(failure);Path out=Path.of("ui-smoke");Files.createDirectories(out);Files.writeString(out.resolve("PASS.txt"),"Native Minecraft UI rendered. Search click/type/focus; diary click/type/save/edit/check/delete/draft retention; raw/modern/legacy item identity and live counter checks passed. No Hypixel connection.\n");mc.stop();stage=10;
    }
    if(ticks>1200&&stage<10)throw new IllegalStateException("UI smoke timed out at stage "+stage);
   }catch(Throwable e){failure=e.toString();try{Files.createDirectories(Path.of("ui-smoke"));Files.writeString(Path.of("ui-smoke/FAIL.txt"),failure);}catch(Exception ignored){}e.printStackTrace();mc.stop();stage=10;}
  });
 }
 private static MultiLineEditBox journal(RecipeHunter app){return (MultiLineEditBox)app.mc.screen.children().stream().filter(c->c instanceof MultiLineEditBox).findFirst().orElseThrow();}
 private static void click(RecipeHunter app,String label){var b=(Button)app.mc.screen.children().stream().filter(c->c instanceof Button candidate&&candidate.getMessage().getString().equals(label)).findFirst().orElseThrow();app.mc.screen.mouseClicked(new MouseButtonEvent(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0,new MouseButtonInfo(0,0)),false);}
 private static void verifyStacks(RecipeHunter app){
  var iron=new ItemStack(Items.IRON_INGOT,16);var mithril=new ItemStack(Items.PRISMARINE_CRYSTALS,160);mithril.set(DataComponents.CUSTOM_NAME,Component.literal("Mithril"));
  var modern=new ItemStack(Items.IRON_INGOT,8);var tag=new CompoundTag();tag.putString("id","IRON_INGOT");modern.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  var legacy=new ItemStack(Items.IRON_INGOT,4);var outer=new CompoundTag();outer.putString("id","minecraft:iron_ingot");outer.put("ExtraAttributes",tag);legacy.set(DataComponents.CUSTOM_DATA,CustomData.of(outer));
  var counted=Ownership.countStacks(List.of(iron,mithril,modern,legacy),app.catalog);
  if(counted.getOrDefault("IRON_INGOT",0L)!=28||counted.getOrDefault("MITHRIL_ORE",0L)!=160)throw new IllegalStateException("Live raw/modern/legacy identity failed: "+counted);
  Book fixture=new Book();var goal=new Book.Goal("ENCHANTED_MITHRIL");goal.count=960;goal.freshCraft=true;fixture.goals.add(goal);
  double before=LiveProgress.refresh(app.catalog,fixture,Map.of(),List.of()).trees().get(goal.id).equivalent;
  double after=LiveProgress.refresh(app.catalog,fixture,counted,List.of()).trees().get(goal.id).equivalent;
  if(before!=0||after!=1)throw new IllegalStateException("Real ItemStack pickup did not update live progress");
 }
 private static void capture(RecipeHunter app,String name){pending=true;Screenshot.takeScreenshot(app.mc.getMainRenderTarget(),image->{try(image){Files.createDirectories(Path.of("ui-smoke"));image.writeToFile(Path.of("ui-smoke",name));}catch(Exception ex){failure=ex.toString();}finally{pending=false;}});}
}
