package dev.recipehunter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
public final class Theme {
 public static int rainbow(double offset){return 0xff000000|java.awt.Color.HSBtoRGB((float)((System.nanoTime()/1e9/18+offset)%1),.62f,1f);}
 public static void background(GuiGraphicsExtractor g,Font font,int w,int h,String title){
  g.fill(0,0,w,h,0xff0b1020);
  double t=System.nanoTime()/1e9;
  for(int i=0;i<12;i++){int x=(int)((i*137+t*(i%2==0?7:-5))%Math.max(1,w));if(x<0)x+=w;int y=(int)(h*.5+Math.sin(t*.3+i)*h*.4);g.fill(x,y,Math.min(w,x+90),Math.min(h,y+2),(rainbow(i*.08)&0xffffff)|0x18000000);}
  g.fill(0,0,w,24,0xe5141c30);
  int x=12;for(int i=0;i<title.length();i++){String c=title.substring(i,i+1);g.text(font,c,x,8,rainbow(i*.035),true);x+=font.width(c);}
  for(int i=0;i<w;i+=8)g.fill(i,23,Math.min(w,i+8),24,rainbow(i/(double)Math.max(w,1)));
 }
}
