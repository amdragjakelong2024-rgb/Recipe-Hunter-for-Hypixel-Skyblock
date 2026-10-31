package dev.recipehunter;
import java.util.*;
import java.nio.file.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Screenshot;
/** CI-only continuation: actual native widgets, both themes, persistence, and diagnostics with 1/4/8 HUDs. */
final class Test2UiSmoke {
 private static int stage,ticks;private static boolean pending;private static String error="";private static Book.Goal set;private static List<String> perf=new ArrayList<>();
 static boolean tick(RecipeHunter app)throws Exception{
  if(!error.isEmpty())throw new IllegalStateException(error);if(pending||++ticks<22)return false;ticks=0;
  var mc=app.mc;
  switch(stage++){
   case 0->{app.book=new Book();app.book.animations=false;app.book.notificationSound=false;set=ArmorSets.combine(app.catalog,app.book,"FINAL_DESTINATION_HELMET");if(set==null)throw new IllegalStateException("Set creation");refresh(app,Map.of("ENCHANTED_OBSIDIAN",96L));mc.setScreen(new RecipeBookScreen(app,false,"",set.id));}
   case 1->{capture(app,"test2-armor-light.png");click(app,"Settings");app.applyPendingScreen();if(!(mc.screen instanceof SettingsScreen)||mc.screen.children().stream().anyMatch(c->c instanceof EditBox))throw new IllegalStateException("Dedicated settings navigation or search clutter");}
   case 2->{capture(app,"test2-settings-light.png");click(app,"Theme: Light");click(app,"Sound Effects: On");click(app,"Forge Notifications: On");click(app,"Crafting Notifications: On");click(app,"Trigger: Both");var p=Path.of("ui-smoke/settings-roundtrip.json");app.book.write(p);var read=Book.read(p);if(!read.darkMode||read.uiSounds||read.forgeAlerts()||read.craftAlerts()||!read.notificationTrigger.equals("Craft Ready"))throw new IllegalStateException("Settings persistence failed");}
   case 3->{capture(app,"test2-settings-dark.png");click(app,"Back");app.applyPendingScreen();if(!(mc.screen instanceof RecipeBookScreen))throw new IllegalStateException("Settings Back lost previous screen");refresh(app,Map.of("ENCHANTED_OBSIDIAN",96L));}
   case 4->{capture(app,"test2-armor-dark.png");click(app,"Final Destination Helmet");mc.setScreen(new ToolsScreen(app,"Craft Diary"));}
   case 5->{capture(app,"test2-diary-dark.png");click(app,"Calculator");app.applyPendingScreen();var field=(EditBox)mc.screen.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();field.setValue("320 / 64");click(app,"Calculate");}
   case 6->{capture(app,"test2-calculator-dark.png");app.command("forge hide");if(app.book.forgeVisible)throw new IllegalStateException("forge hide");int jobs=app.menus.forge.size();app.command("forge show");if(!app.book.forgeVisible||jobs!=app.menus.forge.size())throw new IllegalStateException("Forge visibility stopped tracking");app.command("forge slot1 info");mc.setScreen(new ForgeScreen(app));}
   case 7->{capture(app,"test2-forge-dark.png");Book.Goal g=new Book.Goal("REFINED_MITHRIL");app.observedStock=Map.of("ENCHANTED_MITHRIL",160L);app.materialRevision++;mc.setScreen(new Screen(Component.literal("Three-second tooltip")){final UpgradeHover hover=new UpgradeHover();@Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float dt){hover.render(app,graphics,"ENCHANTED_MITHRIL",g,30,40);}});}
   case 8,9,10->{} // Dwell remains on the same real rendered material for >3 seconds.
   case 11->{capture(app,"test2-hover-dark.png");setupHudCount(app,1);}
   case 12->{capture(app,"test2-hud-1-dark.png");setupHudCount(app,4);}
   case 13->{capture(app,"test2-hud-4-dark.png");setupHudCount(app,8);}
   case 14->{capture(app,"test2-hud-8-dark.png");app.book.darkMode=false;}
   case 15->{capture(app,"test2-hud-8-light.png");Files.writeString(Path.of("ui-smoke/perf-scenarios.txt"),"Offline native UI diagnostic formatting and cached model scenarios. No player, tracker timings are not gameplay CPU/FPS benchmarks.\n"+String.join("\n",perf));mc.setScreen(new RecipeBookScreen(app,true,"mithril","REFINED_MITHRIL"));}
   case 16->{var v=BookViewport.fit(mc.screen.width,mc.screen.height,app.book.guiScale);var e=new MouseButtonEvent(v.x(690),v.y(15),new MouseButtonInfo(0,0));if(!mc.screen.mouseClicked(e,false))throw new IllegalStateException("Gear hitbox");app.applyPendingScreen();if(!(mc.screen instanceof SettingsScreen))throw new IllegalStateException("Top-right gear navigation");}
   default->{Files.writeString(Path.of("ui-smoke/TEST2-PASS.txt"),"Settings gear/Back, no search clutter, Light/Dark book/settings/tools/Forge/HUD/tooltip renders, persisted toggles, armor piece navigation, forge hide/show keeps jobs, 1/4/8 HUD diagnostics and models passed. No Hypixel server or FPS benchmark.\n");return true;}
  }return false;
 }
 private static void refresh(RecipeHunter app,Map<String,Long> stock){var result=LiveProgress.refresh(app.catalog,app.book,stock,List.of());app.observedStock=stock;app.materialRevision++;app.progressTrees.clear();app.progressTrees.putAll(result.trees());app.hudProgress.clear();app.hudProgress.putAll(result.rows());}
 private static void setupHudCount(RecipeHunter app,int count){app.book.goals.clear();for(String id:List.of("REFINED_MITHRIL","MITHRIL_PLATE","ENCHANTED_DIAMOND_BLOCK","ENCHANTED_CARROT","ENCHANTED_MELON_BLOCK","REFINED_TITANIUM","ENCHANTED_IRON_BLOCK","GOLDEN_PLATE")){app.book.goals.add(new Book.Goal(id));if(app.book.goals.size()==count)break;}
  refresh(app,Map.of("ENCHANTED_MITHRIL",160L));var lines=app.performanceLines();if(!lines.get(1).endsWith("visible goal HUDs: "+count))throw new IllegalStateException("perf HUD count");perf.addAll(lines);app.command("perf");app.mc.setScreen(new Screen(Component.literal("HUD diagnostics")){@Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){HudPanels.render(app,g,-1,-1,0);}});
 }
 private static void click(RecipeHunter app,String label){var b=(Button)app.mc.screen.children().stream().filter(c->c instanceof Button button&&button.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new IllegalStateException("Button missing: "+label));app.mc.screen.mouseClicked(new MouseButtonEvent(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0,new MouseButtonInfo(0,0)),false);}
 private static void capture(RecipeHunter app,String name){pending=true;Screenshot.takeScreenshot(app.mc.getMainRenderTarget(),image->{try(image){image.writeToFile(Path.of("ui-smoke",name));}catch(Exception e){error=e.toString();}finally{pending=false;}});}
}
