package dev.recipehunter;
/** Compact Forge overlay with its own independent drag/scale bounds. */
public final class ForgeLayout {
 public double x=0,y=.3,zoom=1;public static final int W=254,H=197;
 private static double finite(double n,double fallback){return Double.isFinite(n)?n:fallback;}
 public double scale(int w,int h){return Math.max(.1,Math.min(Math.clamp(finite(zoom,1),.5,2),Math.min(w/(double)W,h/(double)H)));}
 public int left(int w,int h){return (int)(Math.clamp(finite(x,0),0,1)*Math.max(0,w-W*scale(w,h)));}
 public int top(int w,int h){return (int)(Math.clamp(finite(y,.3),0,1)*Math.max(0,h-H*scale(w,h)));}
 public void move(double px,double py,int w,int h){x=Math.clamp(px/Math.max(1,w-W*scale(w,h)),0,1);y=Math.clamp(py/Math.max(1,h-H*scale(w,h)),0,1);}
 public void zoom(double d,int w,int h){int px=left(w,h),py=top(w,h);zoom=Math.clamp(finite(zoom,1)+d,.5,2);move(px,py,w,h);}
 public boolean contains(double mx,double my,int w,int h){return mx>=left(w,h)&&my>=top(w,h)&&mx<left(w,h)+W*scale(w,h)&&my<top(w,h)+H*scale(w,h);}
}
