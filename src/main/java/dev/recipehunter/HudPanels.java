package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class HudPanels {
 public static HudLayout layout(RecipeHunter app,Book.Goal goal,int index,int w,int h){if(goal.hud==null){goal.hud=new HudLayout();goal.hud.scale=app.book.hudScale;goal.hud.move(w-HudLayout.WIDTH*goal.hud.scale-8-(index/Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.WIDTH*goal.hud.scale+8),12+(index%Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.HEIGHT*goal.hud.scale+8),w,h);}return goal.hud;}
 public static void render(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,long pageClock){
  int index=0;for(Book.Goal goal:app.book.goals)if(goal.visible)renderGoal(app,g,mx,my,goal,index++);
 }
 private static void renderGoal(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,Book.Goal goal,int index){
  HudLayout l=layout(app,goal,index,g.guiWidth(),g.guiHeight());float scale=(float)l.scale(g.guiWidth(),g.guiHeight());int x=l.left(g.guiWidth(),g.guiHeight()),y=l.top(g.guiWidth(),g.guiHeight());
  g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
  g.fill(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xeeeadbb5);g.outline(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xff6c472a);g.fill(1,1,HudLayout.WIDTH-1,20,0xff4d3924);
  g.item(Icons.stack(app,goal.id),3,2);g.text(app.mc.font,app.trim(app.catalog.item(goal.id).name,HudLayout.WIDTH-25),23,6,0xffd4ed85,true);
  List<MaterialProgress.Row> rows=app.hudProgress.get(goal.id);int pages=rows==null?1:Math.max(1,(rows.size()+2)/3),page=(int)Math.floorMod(goal.hudPage,pages);
  if(rows==null)g.text(app.mc.font,"Loading...",7,28,Theme.INK,false);
  else if(rows.isEmpty())g.text(app.mc.font,"Ready",7,28,0xff406820,false);
  else for(int i=page*3;i<Math.min(page*3+3,rows.size());i++){
   var row=rows.get(i);int top=25+(i-page*3)*31;g.item(Icons.stack(app,row.id()),5,top);
   g.text(app.mc.font,app.trim(app.catalog.item(row.id()).name,HudLayout.WIDTH-31),26,top,row.done()?0xff396920:Theme.INK,false);
   String extra=row.count();
   if(row.forge()>0)extra+=" | Forge: "+row.forge();
   else if(row.pending()>0)extra+=" | Planned: "+row.pending();
   g.text(app.mc.font,app.trim(extra,HudLayout.WIDTH-93),26,top+11,Theme.MUTED,false);
   g.text(app.mc.font,row.percent(),HudLayout.WIDTH-64,top+11,row.done()?0xff396920:Theme.INK,false);
   g.fill(26,top+23,HudLayout.WIDTH-8,top+25,0xffb5a477);g.fill(26,top+23,26+(int)((HudLayout.WIDTH-34)*row.fraction()),top+25,0xff72a32b);
  }
  String sync=app.menus.sackSynced()?"Sacks: last synced":app.menus.sackTracked()?"Sacks: observed changes":"Sacks: Unknown";
  g.text(app.mc.font,app.trim(sync,HudLayout.WIDTH-52),7,HudLayout.HEIGHT-11,Theme.MUTED,false);
  g.text(app.mc.font,(page+1)+"/"+pages,HudLayout.WIDTH-30,HudLayout.HEIGHT-11,Theme.MUTED,false);g.pose().popMatrix();
  if(mx>=x&&mx<x+HudLayout.WIDTH*scale&&my>=y&&my<y+HudLayout.HEIGHT*scale)RecipePreview.render(app,g,goal.id,goal,mx,my);
 }
}
