package dev.recipehunter;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Header-only drag interception keeps normal inventory slots usable. */
public final class InventoryOverlay {
 public static void register(RecipeHunter app){ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{
  if(!(screen instanceof InventoryScreen)||!app.onHypixel())return;
  final Book.Goal[] drag={null};final double[] offset=new double[2];final long[] page={0};
  Screens.getButtons(screen).add(Button.builder(Component.literal("RH Book"),b->app.openBook()).bounds(4,h-24,70,20).build());
  Screens.getButtons(screen).add(Button.builder(Component.literal("Diary"),b->app.open(new ToolsScreen(app,"Craft Diary"))).bounds(78,h-24,55,20).build());
  ScreenEvents.afterExtract(screen).register((s,g,mx,my,dt)->HudPanels.render(app,g,drag[0]==null?mx:-1,drag[0]==null?my:-1,page[0]));
  ScreenMouseEvents.allowMouseClick(screen).register((s,e)->{
   int index=0;for(var goal:app.book.goals)if(goal.visible){var l=HudPanels.layout(app,goal,index++,w,h);double top=l.top(w,h);if(l.contains(e.x(),e.y(),w,h)&&e.y()<top+17*l.scale(w,h)){
    if(e.button()==0){drag[0]=goal;offset[0]=e.x()-l.left(w,h);offset[1]=e.y()-top;return false;}if(e.button()==1){page[0]++;return false;}
   }}return true;
  });
  ScreenMouseEvents.allowMouseDrag(screen).register((s,e,dx,dy)->{if(drag[0]==null)return true;drag[0].hud.move(e.x()-offset[0],e.y()-offset[1],w,h);return false;});
  ScreenMouseEvents.allowMouseRelease(screen).register((s,e)->{if(drag[0]==null)return true;drag[0]=null;app.persist();return false;});
  ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,sx,sy)->{int index=0;for(var goal:app.book.goals)if(goal.visible){var l=HudPanels.layout(app,goal,index++,w,h);if(l.contains(x,y,w,h)&&y<l.top(w,h)+17*l.scale(w,h)){l.zoom(sy*.05,w,h);app.persist();return false;}}return true;});
  ScreenEvents.remove(screen).register(s->{if(drag[0]!=null){drag[0]=null;app.persist();}});
 });}
}
