package dev.recipehunter;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
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
    if(stage==0&&ticks>100&&mc.screen instanceof TitleScreen){
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
    else if(stage==6&&ticks>20&&!pending){Path out=Path.of("ui-smoke");Files.createDirectories(out);Files.writeString(out.resolve("PASS.txt"),"Native Minecraft UI rendered. Search click, text entry and focus retention passed. No Hypixel connection.\n");mc.stop();stage=7;}
    if(ticks>1200&&stage<7)throw new IllegalStateException("UI smoke timed out at stage "+stage);
   }catch(Throwable e){failure=e.toString();try{Files.createDirectories(Path.of("ui-smoke"));Files.writeString(Path.of("ui-smoke/FAIL.txt"),failure);}catch(Exception ignored){}e.printStackTrace();mc.stop();stage=7;}
  });
 }
 private static void capture(RecipeHunter app,String name){pending=true;Screenshot.takeScreenshot(app.mc.getMainRenderTarget(),image->{try(image){Files.createDirectories(Path.of("ui-smoke"));image.writeToFile(Path.of("ui-smoke",name));}catch(Exception ex){failure=ex.toString();}finally{pending=false;}});}
}
