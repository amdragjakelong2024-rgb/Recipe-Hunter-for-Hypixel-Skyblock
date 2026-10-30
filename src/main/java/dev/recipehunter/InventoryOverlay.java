package dev.recipehunter;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
public final class InventoryOverlay {
 public static void register(RecipeHunter app){ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{
  if(!(screen instanceof InventoryScreen)&&!(screen instanceof net.minecraft.client.gui.screens.ChatScreen)||!app.onHypixel())return;
  final Book.Goal[] drag={null};final double[] offset=new double[2];final long[] page={0};
  if(screen instanceof InventoryScreen){Screens.getWidgets(screen).add(new GlowButton("Recipe Hunter",4,h-24,100,app::openHome));
  Screens.getWidgets(screen).add(new GlowButton("- HUD",108,h-24,58,()->zoom(app,-.1,w,h)));
  Screens.getWidgets(screen).add(new GlowButton("+ HUD",170,h-24,58,()->zoom(app,.1,w,h)));
  Screens.getWidgets(screen).add(new GlowButton("Reset HUD",232,h-24,82,()->{var goal=app.book.active();if(goal!=null){goal.hud=null;app.persist();}}));
  }
  ScreenEvents.afterExtract(screen).register((s,g,mx,my,dt)->{HudPanels.render(app,g,drag[0]==null?mx:-1,drag[0]==null?my:-1,page[0]);ForgeHud.render(app,g,false);});
  ScreenMouseEvents.allowMouseClick(screen).register((s,e)->{
   if(app.book.forgeVisible&&app.book.forgeHud.contains(e.x(),e.y(),w,h)){if(e.button()==0)app.open(new ForgeScreen(app));return false;}
   var goal=hit(app,e.x(),e.y(),w,h);if(goal==null)return true;var l=goal.hud;
   if(!l.contains(e.x(),e.y(),w,h))return true;
   if(e.button()==0&&HudControls.click(app,goal,e.x(),e.y(),w,h))return false;
   if(e.button()==0&&HudPanels.pageClick(app,goal,e.x(),e.y(),w,h))return false;
   if(e.button()==1){goal.hudPage=BookViewport.page(goal.hudPage,1,HudPanels.pages(app,goal));app.persist();return false;}
   if(e.button()==0){app.select(goal.id,false);return false;}return true;
  });
  ScreenMouseEvents.allowMouseDrag(screen).register((s,e,dx,dy)->{if(drag[0]==null)return true;drag[0].hud.move(e.x()-offset[0],e.y()-offset[1],w,h);return false;});
  ScreenMouseEvents.allowMouseRelease(screen).register((s,e)->{if(drag[0]==null)return true;drag[0]=null;app.persist();return false;});
  ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,sx,sy)->{var goal=hit(app,x,y,w,h);if(goal!=null){goal.hud.zoom(sy*.08,w,h);app.persist();return false;}return true;});
  ScreenEvents.remove(screen).register(s->{if(drag[0]!=null){drag[0]=null;app.persist();}});
 });}
 private static Book.Goal hit(RecipeHunter app,double x,double y,int w,int h){Book.Goal hit=null;int i=0;for(var goal:app.book.goals)if(goal.visible){HudPanels.layout(app,goal,i++,w,h);if(HudPanels.hit(app,goal,x,y,w,h))hit=goal;}return hit;}
 private static void zoom(RecipeHunter app,double amount,int w,int h){var g=app.book.active();if(g!=null){HudPanels.layout(app,g,0,w,h).zoom(amount,w,h);app.persist();}}
}
