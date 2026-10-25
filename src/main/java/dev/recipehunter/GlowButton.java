package dev.recipehunter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
public final class GlowButton extends Button {
 private double glow;private long last=System.nanoTime();
 public GlowButton(String label,int x,int y,int w,Runnable action){super(x,y,w,20,Component.literal(label),b->action.run(),DEFAULT_NARRATION);}
 @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
  long now=System.nanoTime();double elapsed=Math.min(.1,(now-last)/1e9);last=now;glow+=( (isHoveredOrFocused()?1:0)-glow)*Math.min(1,elapsed*14);
  int x=getX(),y=getY(),w=getWidth();
  if(glow>.01)g.fill(x,y,x+w,y+20,((int)(glow*40)<<24)|0x936527);
  g.fill(x,y+19,x+w,y+20,0x448a693f);
  if(glow>.1)g.outline(x,y,w,20,Theme.rainbow(x*.001));
  if(glow>.01)g.fill(x+1,y+18,x+1+(int)((w-2)*glow),y+19,Theme.rainbow(x*.001));
  var font=Minecraft.getInstance().font;g.text(font,getMessage(),x+(w-font.width(getMessage()))/2,y+6,active?Theme.INK:Theme.MUTED,false);
 }
}
