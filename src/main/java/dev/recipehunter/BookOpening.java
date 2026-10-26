package dev.recipehunter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;

/** Opening shares the final book's exact viewport; no enchantment-table camera offset. */
public final class BookOpening extends Screen {
 private final RecipeHunter app;private long started;private boolean finished;private final boolean closing;
 public BookOpening(RecipeHunter app){this(app,false);}
 public BookOpening(RecipeHunter app,boolean closing){super(Component.literal("Recipe Hunter"));this.app=app;this.closing=closing;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override protected void init(){if(started==0){started=System.nanoTime();if(closing)UiSounds.close(app);else UiSounds.open(app);}}
 static float opening(double elapsed){float t=(float)Math.clamp(elapsed/.38,0,1);return t*t*t*(t*(6*t-15)+10);}
 private void finish(){if(finished)return;finished=true;if(closing)app.mc.setScreen(null);else app.openHomeContent();}
 @Override public void tick(){if((System.nanoTime()-started)/1e9>=.38)finish();}
 @Override public void onClose(){app.mc.setScreen(null);}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){finish();return true;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  double age=(System.nanoTime()-started)/1e9;float open=closing?1-opening(age):opening(age);BookViewport v=BookViewport.fit(width,height,app.book.guiScale);
  g.pose().pushMatrix();g.pose().translate(v.x(BookViewport.WIDTH/2.0),v.y(BookViewport.HEIGHT/2.0));
  g.pose().scale(v.scale()*(.045f+.955f*open),v.scale()*(.9f+.1f*open));g.pose().translate(-BookViewport.WIDTH/2f,-BookViewport.HEIGHT/2f);
  Theme.background(g,font,BookViewport.WIDTH,BookViewport.HEIGHT,"");g.pose().popMatrix();
 }
}
