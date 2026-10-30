package dev.recipehunter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Dedicated settings book. All controls persist; Back restores the invoking screen. */
public final class SettingsScreen extends Screen {
 private final RecipeHunter app;private final Screen parent;private BookViewport view;
 public SettingsScreen(RecipeHunter app,Screen parent){super(Component.literal("Settings"));this.app=app;this.parent=parent;}
 private String on(boolean b){return b?"On":"Off";}
 private void change(Runnable action){action.run();app.persist();rebuildWidgets();}
 private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(new BookButton(label,view.x(x),view.y(y),view.size(w),view.size(23),view.scale(),false,()->change(action)));}
 public static String next(String current,List<String> values){return values.get((values.indexOf(current)+1)%values.size());}
 @Override protected void init(){view=BookViewport.fit(width,height,app.book.guiScale);
  button("Sound Effects: "+on(app.book.uiSounds),32,90,300,()->app.book.uiSounds=!app.book.uiSounds);
  button("UI Animations: "+on(app.book.animations),32,120,300,()->app.book.animations=!app.book.animations);
  button("Celebrations / Music: "+on(app.book.celebrations&&app.book.celebrationSound),32,150,300,()->{boolean value=!(app.book.celebrations&&app.book.celebrationSound);app.book.celebrations=value;app.book.celebrationSound=value;});
  button("Forge HUD: "+(app.book.forgeVisible?"Show":"Hide"),32,230,300,()->app.book.forgeVisible=!app.book.forgeVisible);
  button("Forge Notifications: "+on(app.book.forgeAlerts()),32,260,300,()->app.book.forgeNotifications=!app.book.forgeAlerts());
  button("Move Forge HUD",32,300,145,()->app.open(new ForgeScreen(app)));
  button("Move Recipe HUDs",187,300,145,()->app.open(new HudEditor(app)));
  button("Crafting Notifications: "+on(app.book.craftAlerts()),384,90,300,()->app.book.craftingNotifications=!app.book.craftAlerts());
  button("Trigger: "+app.book.notificationTrigger,384,120,300,()->app.book.notificationTrigger=next(app.book.notificationTrigger,List.of("Craft Ready","Goal Reached","Both")));
  button("Notification Sound: "+on(app.book.notificationSound),384,150,300,()->app.book.notificationSound=!app.book.notificationSound);
  button("Material Display: "+app.book.defaultMaterialDisplay,384,230,300,()->{app.book.defaultMaterialDisplay=next(app.book.defaultMaterialDisplay,HudControls.MATERIALS);for(var g:app.book.goals)g.materialDisplay=app.book.defaultMaterialDisplay;});
  button("Gemstone Display: "+app.book.defaultGemDisplay,384,260,300,()->{app.book.defaultGemDisplay=next(app.book.defaultGemDisplay,HudControls.GEMS);for(var g:app.book.goals)g.gemDisplay=app.book.defaultGemDisplay;});
  button("-",384,290,32,()->scale(-.1));button("+",652,290,32,()->scale(.1));
  button("Theme: "+(app.book.darkMode?"Dark":"Light"),384,320,300,()->app.book.darkMode=!app.book.darkMode);
  button("Book Size -",32,350,145,()->app.book.guiScale=Math.max(.65,app.book.guiScale-.1));button("Book Size +",187,350,145,()->app.book.guiScale=Math.min(1.6,app.book.guiScale+.1));
  button("Back",32,393,90,this::onClose);
 }
 private void scale(double d){app.book.hudScale=Math.clamp(app.book.hudScale+d,.65,1.6);for(var goal:app.book.goals)if(goal.hud!=null)goal.hud.scale=app.book.hudScale;}
 @Override public void onClose(){app.persist();if(parent==null)app.openHomeContent();else app.open(parent);}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  g.pose().pushMatrix();g.pose().translate(view.left(),view.top());g.pose().scale(view.scale(),view.scale());Theme.background(g,font,720,430,"");
  g.pose().pushMatrix();g.pose().translate(32,28);g.pose().scale(2,2);g.text(font,"SETTINGS",0,0,Theme.ink(),false);g.pose().popMatrix();
  for(Object[] h:new Object[][]{{"GENERAL",32,68},{"FORGE",32,208},{"NOTIFICATIONS",384,68},{"DISPLAY",384,208}})g.text(font,(String)h[0],(int)h[1],(int)h[2],Theme.ink(),false);
  g.text(font,String.format(Locale.US,"HUD Scale: %.0f%%",app.book.hudScale*100),438,298,Theme.ink(),false);
  g.pose().popMatrix();super.extractRenderState(g,mx,my,dt);
 }
}
