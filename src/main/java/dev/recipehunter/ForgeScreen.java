package dev.recipehunter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
/** Preview and editor for the same always-visible HUD. Never sends Forge clicks. */
public final class ForgeScreen extends Screen {
 private final RecipeHunter app;private boolean dragging;private double dx,dy;
 public ForgeScreen(RecipeHunter app){super(Component.literal("Recipe Hunter Forge"));this.app=app;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override protected void init(){
  addRenderableWidget(new GlowButton(app.book.forgeVisible?"HUD: On":"HUD: Off",8,height-26,85,()->{app.book.forgeVisible=!app.book.forgeVisible;app.persist();rebuildWidgets();}));
  addRenderableWidget(new GlowButton(app.book.forgeAlerts()?"Alerts: On":"Alerts: Off",98,height-26,85,()->{app.book.forgeNotifications=!app.book.forgeAlerts();app.persist();rebuildWidgets();}));
  addRenderableWidget(new GlowButton("Recipe book",188,height-26,100,app::openBook));
  addRenderableWidget(new GlowButton("Done",width-65,height-26,60,this::onClose));
 }
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(super.mouseClicked(e,twice))return true;if(e.button()!=0||!app.book.forgeHud.contains(e.x(),e.y(),width,height))return false;dragging=true;dx=e.x()-app.book.forgeHud.left(width,height);dy=e.y()-app.book.forgeHud.top(width,height);return true;}
 @Override public boolean mouseDragged(MouseButtonEvent e,double x,double y){if(!dragging)return super.mouseDragged(e,x,y);app.book.forgeHud.move(e.x()-dx,e.y()-dy,width,height);return true;}
 @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;app.persist();return true;}return super.mouseReleased(e);}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(!app.book.forgeHud.contains(x,y,width,height))return super.mouseScrolled(x,y,sx,sy);app.book.forgeHud.zoom(sy*.05,width,height);app.persist();return true;}
 @Override public void onClose(){app.persist();app.mc.setScreen(null);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){g.text(font,"Forge preview | Drag to move | Wheel to resize | ESC to save",8,5,0xffffffff,true);ForgeHud.render(app,g,true);g.fill(0,height-30,width,height,Theme.paper());super.extractRenderState(g,mx,my,dt);}
}
