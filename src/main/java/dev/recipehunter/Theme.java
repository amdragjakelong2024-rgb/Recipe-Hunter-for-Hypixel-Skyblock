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
  g.fill(0,0,w,h,0xff51251c);
  g.fill(2,2,w-2,h-2,0xff78382a);
  g.outline(0,0,w,h,0xff2d1b14);
  Identifier paper=Identifier.fromNamespaceAndPath("minecraft","textures/gui/book.png");int mid=w/2;
  g.blit(RenderPipelines.GUI_TEXTURED,paper,5,25,32f,12f,mid-5,h-30,116,150,256,256);
  g.blit(RenderPipelines.GUI_TEXTURED,paper,mid,25,32f,12f,w-mid-5,h-30,116,150,256,256);
  for(int i=0;i<5;i++){g.fill(mid-5+i,25,mid-4+i,h-5,((9+i*3)<<24)|0x50351c);g.fill(mid+i,25,mid+1+i,h-5,((21-i*3)<<24)|0x50351c);}
  // Layered page edges and leather corners give the book a visible thickness.
  for(int i=0;i<3;i++){g.fill(6+i*2,h-7+i*2,w-6-i*2,h-6+i*2,i%2==0?0xffb4a27c:0xffe6d9b7);}
  g.fill(0,24,5,h,0xff3d2417);g.fill(w-5,24,w,h,0xff3d2417);
  g.fill(5,24,w-5,26,0xffb3a578);g.fill(5,h-6,w-5,h-5,0xffb3a578);
  // Pixel brass corner caps; keep decoration inside the physical book.
  for(int cx:new int[]{2,w-10})for(int cy:new int[]{2,h-10}){
   g.fill(cx,cy,cx+8,cy+8,0xff9b6b27);g.fill(cx+1,cy+1,cx+7,cy+3,0xffe1b75e);g.fill(cx+1,cy+1,cx+3,cy+7,0xffc59b49);
  }
  int x=12;for(int i=0;i<title.length();i++){String c=title.substring(i,i+1);g.text(font,c,x,8,rainbow(i*.035),true);x+=font.width(c);}
 }
}
