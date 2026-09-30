package dev.recipehunter;

/** Resolution-independent position; no player identifiers or network data. */
public final class HudLayout {
    public double x=1,y=0,scale=1;
    public static final int WIDTH=234,HEIGHT=66;
    private static double finite(double n,double fallback){return Double.isFinite(n)?n:fallback;}
    public double scale(int w,int h){return Math.max(0.1,Math.min(Math.clamp(finite(scale,1),0.5,2),Math.min(w/(double)WIDTH,h/(double)HEIGHT)));}
    public int left(int w,int h){return (int)Math.round(Math.clamp(finite(x,1),0,1)*Math.max(0,w-WIDTH*scale(w,h)));}
    public int top(int w,int h){return (int)Math.round(Math.clamp(finite(y,0),0,1)*Math.max(0,h-HEIGHT*scale(w,h)));}
    public void move(double px,double py,int w,int h){double s=scale(w,h);x=Math.clamp(px/Math.max(1,w-WIDTH*s),0,1);y=Math.clamp(py/Math.max(1,h-HEIGHT*s),0,1);}
    public void zoom(double delta,int w,int h){int px=left(w,h),py=top(w,h);scale=Math.clamp(finite(scale,1)+delta,0.5,2);move(px,py,w,h);}
    public boolean contains(double mx,double my,int w,int h){double s=scale(w,h);return mx>=left(w,h)&&my>=top(w,h)&&mx<left(w,h)+WIDTH*s&&my<top(w,h)+HEIGHT*s;}
}
