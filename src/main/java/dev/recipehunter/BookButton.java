package dev.recipehunter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Native screen-space hitbox; independent painted text size. */
public final class BookButton extends Button {
 private final float textScale;private final boolean quiet;private boolean selected;
 public BookButton(String label,int x,int y,int w,int h,float scale,boolean quiet,Runnable action){super(x,y,w,h,Component.literal(label),b->action.run(),DEFAULT_NARRATION);textScale=scale;this.quiet=quiet;}
 public BookButton selected(boolean value){selected=value;return this;}
 @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){
  int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean hover=isHoveredOrFocused()&&active;
  if(!quiet){g.fill(x,y,x+w,y+h,selected?0xff704526:0xffbfa178);g.fill(x+1,y+1,x+w-1,y+h-2,selected?0xff85532e:hover?0xffecd5a7:0xffd6bd91);g.outline(x,y,w,h,0xff785330);g.fill(x+2,y+1,x+w-2,y+2,0xffedd7af);}
  else if(hover)g.fill(x,y,x+w,y+h,0x33764c26);
  var font=Minecraft.getInstance().font;float s=Math.min(textScale,(w-6f)/Math.max(1,font.width(getMessage())));s=Math.max(.1f,s);
  g.pose().pushMatrix();g.pose().translate(x+(quiet?4:(w-font.width(getMessage())*s)/2),y+(h-8*s)/2);g.pose().scale(s,s);g.text(font,getMessage(),0,0,!active?0xff947f60:selected?0xffffe5b6:Theme.INK,false);g.pose().popMatrix();
 }
}
