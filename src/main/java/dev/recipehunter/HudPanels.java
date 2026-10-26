package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class HudPanels {
 public static final int PER_PAGE=7;
 public static int pages(RecipeHunter app,Book.Goal goal){var rows=app.hudProgress.get(goal.id);return rows==null?1:Math.max(1,(rows.size()+PER_PAGE-1)/PER_PAGE);}
 public static boolean pageClick(RecipeHunter app,Book.Goal goal,double mx,double my,int w,int h){var l=goal.hud;double x=(mx-l.left(w,h))/l.scale(w,h),y=(my-l.top(w,h))/l.scale(w,h);if(y<HudLayout.HEIGHT-21||y>=HudLayout.HEIGHT-2)return false;int delta=x>=6&&x<=31?-1:x>=HudLayout.WIDTH-31&&x<=HudLayout.WIDTH-6?1:0;if(delta==0)return false;int next=BookViewport.page(goal.hudPage,delta,pages(app,goal));if(next!=goal.hudPage){goal.hudPage=next;app.persist();}return true;}
 public static HudLayout layout(RecipeHunter app,Book.Goal goal,int index,int w,int h){if(goal.hud==null){goal.hud=new HudLayout();goal.hud.scale=app.book.hudScale;goal.hud.move(w-HudLayout.WIDTH*goal.hud.scale-8-(index/Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.WIDTH*goal.hud.scale+8),12+(index%Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.HEIGHT*goal.hud.scale+8),w,h);}return goal.hud;}
 public static void render(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,long pageClock){
  int index=0;for(Book.Goal goal:app.book.goals)if(goal.visible)renderGoal(app,g,mx,my,goal,index++);
 }
 private static void renderGoal(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,Book.Goal goal,int index){
  HudLayout l=layout(app,goal,index,g.guiWidth(),g.guiHeight());float scale=(float)l.scale(g.guiWidth(),g.guiHeight());int x=l.left(g.guiWidth(),g.guiHeight()),y=l.top(g.guiWidth(),g.guiHeight());
  g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
  g.fill(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xeeeadbb5);g.outline(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xff6c472a);g.fill(1,1,HudLayout.WIDTH-1,20,0xff4d3924);
  g.item(Icons.stack(app,goal.id),3,2);g.text(app.mc.font,app.trim(app.catalog.item(goal.id).name,HudLayout.WIDTH-25),23,6,0xffd4ed85,true);
  List<MaterialProgress.Row> rows=app.hudProgress.get(goal.id);int pages=rows==null?1:Math.max(1,(rows.size()+PER_PAGE-1)/PER_PAGE),page=Math.clamp(goal.hudPage,0,pages-1);
  if(rows==null)g.text(app.mc.font,"Loading...",7,28,Theme.INK,false);
  else if(rows.isEmpty())g.text(app.mc.font,"Ready",7,28,0xff406820,false);
  else for(int i=page*PER_PAGE;i<Math.min(page*PER_PAGE+PER_PAGE,rows.size());i++){
   var row=rows.get(i);int top=25+(i-page*PER_PAGE)*26;g.item(Icons.stack(app,row.id()),5,top);
   g.text(app.mc.font,app.trim(app.catalog.item(row.id()).name,HudLayout.WIDTH-31),26,top,row.done()?0xff396920:Theme.INK,false);
   String extra=String.format(Locale.US,"%,.2f / %,d",row.equivalent(),row.total());
   if(row.forge()>0)extra+=" | Forge: "+row.forge();
   else if(row.pending()>0)extra+=" | Planned: "+row.pending();
   g.text(app.mc.font,app.trim(extra,HudLayout.WIDTH-93),26,top+11,Theme.MUTED,false);
   g.text(app.mc.font,row.percent(),HudLayout.WIDTH-64,top+11,row.done()?0xff396920:Theme.INK,false);
   g.fill(26,top+22,HudLayout.WIDTH-8,top+24,0xffb5a477);g.fill(26,top+22,26+(int)((HudLayout.WIDTH-34)*row.fraction()),top+24,0xff72a32b);
  }
  int footer=HudLayout.HEIGHT-21;
  g.fill(6,footer,31,footer+18,page>0?0xffc4a477:0xffdac9a5);g.outline(6,footer,25,18,0xff977248);g.text(app.mc.font,"<",15,footer+5,page>0?Theme.INK:Theme.MUTED,false);
  g.fill(HudLayout.WIDTH-31,footer,HudLayout.WIDTH-6,footer+18,page+1<pages?0xffc4a477:0xffdac9a5);g.outline(HudLayout.WIDTH-31,footer,25,18,0xff977248);g.text(app.mc.font,">",HudLayout.WIDTH-22,footer+5,page+1<pages?Theme.INK:Theme.MUTED,false);
  g.text(app.mc.font,(page+1)+" / "+pages,HudLayout.WIDTH/2-14,footer+5,Theme.MUTED,false);g.pose().popMatrix();
  if(mx>=x&&mx<x+HudLayout.WIDTH*scale&&my>=y&&my<y+20*scale)RecipePreview.render(app,g,goal.id,goal,mx,my);
 }
}
