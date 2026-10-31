package dev.recipehunter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
/** Vanilla parchment, leather binding and experience-orb green/gold accents. */
public final class Theme {
 public static boolean dark(){return RecipeHunter.INSTANCE!=null&&RecipeHunter.INSTANCE.book.darkMode;}
 public static int ink(){return dark()?0xfff0e1c5:0xff3b2a1c;}
 public static int muted(){return dark()?0xffc7b99f:0xff66513a;}
 public static int paper(){return dark()?0xff3d352c:0xffeadbb5;}
 public static int edge(){return dark()?0xffaa8c60:0xff6c472a;}
 public static int well(){return dark()?0xff514537:0xffc5b58e;}
 public static int good(){return dark()?0xffa9d677:0xff396920;}
 public static int button(){return dark()?0xff594a38:0xffd6bd91;}
 public static int hover(){return dark()?0xff736044:0xffecd5a7;}
 public static int rainbow(double offset){double t=.5+.5*Math.sin(System.nanoTime()/1e9*1.5+offset*6.28);int r=(int)(100+125*t),green=(int)(210+40*t);return 0xff000000|(r<<16)|(green<<8)|48;}
 public static int ink(int color){if(dark())return 0xff000000|Math.max(175,(color>>16)&255)<<16|Math.max(165,(color>>8)&255)<<8|Math.max(145,color&255);int r=(color>>16)&255,g=(color>>8)&255,b=color&255;return 0xff000000|((r*2/5)<<16)|((g*2/5)<<8)|(b*2/5);}
 public static void background(GuiGraphicsExtractor g,Font font,int w,int h,String title){
  g.blit(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath("recipehunter","textures/gui/recipe_book.png"),0,0,0f,0f,w,h,1656,950,1656,950,dark()?0xff54493e:0xffffffff);
  g.text(font,"v"+RecipeHunter.VERSION,w-52,h-12,0xffffe0ab,true);
  int x=12;for(int i=0;i<title.length();i++){String c=title.substring(i,i+1);g.text(font,c,x,8,rainbow(i*.035),true);x+=font.width(c);}
 }
}
