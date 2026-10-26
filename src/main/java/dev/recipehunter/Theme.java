package dev.recipehunter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
/** Vanilla parchment, leather binding and experience-orb green/gold accents. */
public final class Theme {
 public static final int INK=0xff3b2a1c,MUTED=0xff66513a;
 public static int rainbow(double offset){double t=.5+.5*Math.sin(System.nanoTime()/1e9*1.5+offset*6.28);int r=(int)(100+125*t),green=(int)(210+40*t);return 0xff000000|(r<<16)|(green<<8)|48;}
 public static int ink(int color){int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return 0xff000000|((r*2/5)<<16)|((g*2/5)<<8)|(b*2/5);}
 public static void background(GuiGraphicsExtractor g,Font font,int w,int h,String title){
  g.blit(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath("recipehunter","textures/gui/recipe_book.png"),0,0,0f,0f,w,h,1656,950,1656,950);
  int x=12;for(int i=0;i<title.length();i++){String c=title.substring(i,i+1);g.text(font,c,x,8,rainbow(i*.035),true);x+=font.width(c);}
 }
}
