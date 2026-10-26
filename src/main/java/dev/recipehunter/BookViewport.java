package dev.recipehunter;
/** One transform shared by native widget bounds and the painted book. */
public record BookViewport(float scale,int left,int top) {
 public static final int WIDTH=720,HEIGHT=430;
 public static BookViewport fit(int width,int height,double preference){
  float s=(float)(Math.min(width/(double)WIDTH,height/(double)HEIGHT)*Math.min(.94,.88*preference));
  return new BookViewport(s,Math.round((width-WIDTH*s)/2),Math.round((height-HEIGHT*s)/2));
 }
 public int x(double x){return left+(int)Math.round(x*scale);}
 public int y(double y){return top+(int)Math.round(y*scale);}
 public int size(double n){return Math.max(1,(int)Math.round(n*scale));}
 public double localX(double x){return (x-left)/scale;}
 public double localY(double y){return (y-top)/scale;}
 public static int page(int current,int delta,int pages){return Math.clamp(current+delta,0,Math.max(0,pages-1));}
}
