package dev.recipehunter;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
public final class InventoryOverlay {
 public static void register(RecipeHunter app){ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{
  if(!(screen instanceof InventoryScreen)||!app.onHypixel())return;
  final Book.Goal[] drag={null};final double[] offset=new double[2];final long[] page={0};
  Screens.getWidgets(screen).add(new GlowButton("Recipe Hunter",4,h-24,100,app::openHome));
  Screens.getWidgets(screen).add(new GlowButton("- HUD",108,h-24,58,()->zoom(app,-.1,w,h)));
  Screens.getWidgets(screen).add(new GlowButton("+ HUD",170,h-24,58,()->zoom(app,.1,w,h)));
  Screens.getWidgets(screen).add(new GlowButton("Reset HUD",232,h-24,82,()->{var goal=app.book.active();if(goal!=null){goal.hud=null;app.persist();}}));
  ScreenEvents.afterExtract(screen).register((s,g,mx,my,dt)->HudPanels.render(app,g,drag[0]==null?mx:-1,drag[0]==null?my:-1,page[0]));
  ScreenMouseEvents.allowMouseClick(screen).register((s,e)->{
   var goal=app.book.active();if(goal==null)return true;var l=HudPanels.layout(app,goal,0,w,h);
   if(!l.contains(e.x(),e.y(),w,h))return true;
   if(e.button()==1){page[0]++;return false;}
   if(e.button()==0){if(e.y()<l.top(w,h)+17*l.scale(w,h)){drag[0]=goal;offset[0]=e.x()-l.left(w,h);offset[1]=e.y()-l.top(w,h);}else app.gameRecipe(goal.id);return false;}return true;
  });
  ScreenMouseEvents.allowMouseDrag(screen).register((s,e,dx,dy)->{if(drag[0]==null)return true;drag[0].hud.move(e.x()-offset[0],e.y()-offset[1],w,h);return false;});
  ScreenMouseEvents.allowMouseRelease(screen).register((s,e)->{if(drag[0]==null)return true;drag[0]=null;app.persist();return false;});
  ScreenMouseEvents.allowMouseScroll(screen).register((s,x,y,sx,sy)->{var goal=app.book.active();if(goal!=null&&HudPanels.layout(app,goal,0,w,h).contains(x,y,w,h)){zoom(app,sy*.08,w,h);return false;}return true;});
  ScreenEvents.remove(screen).register(s->{if(drag[0]!=null){drag[0]=null;app.persist();}});
 });}
 private static void zoom(RecipeHunter app,double amount,int w,int h){var g=app.book.active();if(g!=null){HudPanels.layout(app,g,0,w,h).zoom(amount,w,h);app.persist();}}
}
