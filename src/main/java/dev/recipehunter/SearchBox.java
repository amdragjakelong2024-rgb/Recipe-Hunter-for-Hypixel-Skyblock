package dev.recipehunter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Larger native input text, with matching cursor placement and selection coordinates. */
public final class SearchBox extends EditBox {
 private final float textScale;
 public SearchBox(Font font,int x,int y,int width,int height,Component label,float scale){super(font,x,y,width,height,label);textScale=Math.max(.5f,scale);}
 @Override public int getInnerWidth(){return Math.max(1,(int)(super.getInnerWidth()/textScale));}
 private MouseButtonEvent local(MouseButtonEvent e){return new MouseButtonEvent(getX()+(e.x()-getX())/textScale,e.y(),e.buttonInfo());}
 @Override public void onClick(MouseButtonEvent e,boolean twice){super.onClick(local(e),twice);}
 @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){super.onDrag(local(e),dx/textScale,dy);}
 @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  g.pose().pushMatrix();g.pose().translate(getX(),getY());g.pose().scale(textScale,textScale);g.pose().translate(-getX(),-getY());
  super.extractWidgetRenderState(g,mx,my,dt);g.pose().popMatrix();
 }
}
