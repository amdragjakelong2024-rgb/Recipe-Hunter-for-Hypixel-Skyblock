package dev.recipehunter;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;

/** The native animated Minecraft book, rendered in a full-screen viewport. */
public final class BookOpening extends Screen {
 private final RecipeHunter app;private BookModel model;private long started;private boolean finished;private final boolean closing;
 public BookOpening(RecipeHunter app){this(app,false);}
 public BookOpening(RecipeHunter app,boolean closing){super(Component.literal("Recipe Hunter"));this.app=app;this.closing=closing;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override protected void init(){model=new BookModel(app.mc.getEntityModels().bakeLayer(ModelLayers.BOOK));if(started==0){started=System.nanoTime();if(closing)UiSounds.close(app);else UiSounds.open(app);}}
 static float opening(double elapsed){float t=(float)Math.clamp(elapsed/.8,0,1);return t*t*t*(t*(6*t-15)+10);}
 private void finish(){if(finished)return;finished=true;if(closing)app.mc.setScreen(null);else app.openHomeContent();}
 @Override public void tick(){if((System.nanoTime()-started)/1e9>=(closing?.8:1.05))finish();}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){finish();return true;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  double age=(System.nanoTime()-started)/1e9;float open=closing?1-opening(age):opening(age);

  g.book(model,Identifier.fromNamespaceAndPath("minecraft","textures/entity/enchantment/enchanting_table_book.png"),Math.min(width,height)*.72f,open,(float)(closing?2.4-age*1.5:.2+age*2.1),0,0,width,height);
  String title="RECIPE HUNTER";int x=(width-font.width(title))/2;
  for(int i=0;i<title.length();i++){String c=title.substring(i,i+1);g.text(font,c,x,16,Theme.rainbow(i*.035),true);x+=font.width(c);}
  g.centeredText(font,"Click to skip",width/2,height-20,0xffc4b99d);
 }
}
