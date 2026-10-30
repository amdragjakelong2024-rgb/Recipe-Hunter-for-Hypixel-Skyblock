package dev.recipehunter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
/** Pixel gear with a native, keyboard-accessible Settings button. */
public final class GearButton extends Button {
 public GearButton(int x,int y,int size,Runnable action){super(x,y,size,size,Component.literal("Settings"),b->action.run(),DEFAULT_NARRATION);}
 @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float dt){int x=getX(),y=getY(),w=getWidth(),h=getHeight();g.fill(x,y,x+w,y+h,isHoveredOrFocused()?Theme.hover():Theme.well());g.outline(x,y,w,h,Theme.edge());int cx=x+w/2,cy=y+h/2,r=Math.max(3,w/4),c=Theme.ink();g.outline(cx-r,cy-r,r*2,r*2,c);g.fill(cx-1,cy-r-2,cx+2,cy-r+1,c);g.fill(cx-1,cy+r-1,cx+2,cy+r+2,c);g.fill(cx-r-2,cy-1,cx-r+1,cy+2,c);g.fill(cx+r-1,cy-1,cx+r+2,cy+2,c);}
}
