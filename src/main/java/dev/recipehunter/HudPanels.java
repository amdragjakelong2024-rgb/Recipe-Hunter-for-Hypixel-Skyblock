package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class HudPanels {
 public static final int PER_PAGE=7;private static final UpgradeHover hover=new UpgradeHover();
 public static int contentHeight(RecipeHunter app,Book.Goal goal){var rows=app.hudProgress.get(goal.id);int n=rows==null?1:Math.min(PER_PAGE,Math.max(1,rows.size()-Math.clamp(goal.hudPage,0,pages(app,goal)-1)*PER_PAGE));return 25+n*36;}
 public static boolean hit(RecipeHunter app,Book.Goal goal,double mx,double my,int w,int h){var l=goal.hud;if(l==null)return false;double x=(mx-l.left(w,h))/l.scale(w,h),y=(my-l.top(w,h))/l.scale(w,h);return x>=0&&x<HudLayout.WIDTH&&y>=0&&(y<contentHeight(app,goal)||(y>=HudLayout.HEIGHT-46&&y<HudLayout.HEIGHT));}
 public static int pages(RecipeHunter app,Book.Goal goal){var rows=app.hudProgress.get(goal.id);return rows==null?1:Math.max(1,(rows.size()+PER_PAGE-1)/PER_PAGE);}
 public static boolean pageClick(RecipeHunter app,Book.Goal goal,double mx,double my,int w,int h){var l=goal.hud;double x=(mx-l.left(w,h))/l.scale(w,h),y=(my-l.top(w,h))/l.scale(w,h);if(y<HudLayout.HEIGHT-21||y>=HudLayout.HEIGHT-2)return false;int delta=x>=6&&x<=31?-1:x>=HudLayout.WIDTH-31&&x<=HudLayout.WIDTH-6?1:0;if(delta==0)return false;int next=BookViewport.page(goal.hudPage,delta,pages(app,goal));if(next!=goal.hudPage){goal.hudPage=next;app.persist();}return true;}
 public static HudLayout layout(RecipeHunter app,Book.Goal goal,int index,int w,int h){if(goal.hud==null){goal.hud=new HudLayout();goal.hud.scale=app.book.hudScale;goal.hud.move(w-HudLayout.WIDTH*goal.hud.scale-8-(index/Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.WIDTH*goal.hud.scale+8),12+(index%Math.max(1,(int)(h/(HudLayout.HEIGHT*goal.hud.scale+8))))*(HudLayout.HEIGHT*goal.hud.scale+8),w,h);}return goal.hud;}
 private static long forging(RecipeProgress.Node node){if(node==null)return 0;if(node.acquisition!=null)node=node.acquisition;long count=node.queuedForge;for(var child:node.children)count+=forging(child);return count;}
 public static void render(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,long pageClock){
  int index=0;String hovered="";Book.Goal hoverGoal=null;for(Book.Goal goal:app.book.goals)if(goal.visible){renderGoal(app,g,mx,my,goal,index++);var l=goal.hud;double x=(mx-l.left(g.guiWidth(),g.guiHeight()))/l.scale(g.guiWidth(),g.guiHeight()),y=(my-l.top(g.guiWidth(),g.guiHeight()))/l.scale(g.guiWidth(),g.guiHeight());var rows=app.hudProgress.get(goal.id);if(rows!=null&&x>=0&&x<HudLayout.WIDTH&&y>=25&&y<contentHeight(app,goal)){int i=Math.clamp(goal.hudPage,0,pages(app,goal)-1)*PER_PAGE+(int)((y-25)/36);if(i<rows.size()){hovered=rows.get(i).id();hoverGoal=goal;}}}hover.render(app,g,hovered,hoverGoal,mx,my);
 }
 private static void renderGoal(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,Book.Goal goal,int index){
  HudLayout l=layout(app,goal,index,g.guiWidth(),g.guiHeight());float scale=(float)l.scale(g.guiWidth(),g.guiHeight());int x=l.left(g.guiWidth(),g.guiHeight()),y=l.top(g.guiWidth(),g.guiHeight());
  g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
  g.fill(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,Theme.paper());g.outline(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,Theme.edge());g.fill(1,1,HudLayout.WIDTH-1,20,0xff4d3924);
  g.item(Icons.stack(app,ArmorSets.iconId(goal)),3,2);g.text(app.mc.font,app.trim(app.catalog.item(goal.id).name,HudLayout.WIDTH-25),23,6,0xffd4ed85,true);
  List<MaterialProgress.Row> rows=app.hudProgress.get(goal.id);int pages=rows==null?1:Math.max(1,(rows.size()+PER_PAGE-1)/PER_PAGE),page=Math.clamp(goal.hudPage,0,pages-1);
  if(rows==null)g.text(app.mc.font,"Loading...",7,28,Theme.ink(),false);
  else if(rows.isEmpty())g.text(app.mc.font,"Materials covered; check crafting/Forge",7,28,Theme.good(),false);
  else for(int i=page*PER_PAGE;i<Math.min(page*PER_PAGE+PER_PAGE,rows.size());i++){
   var row=rows.get(i);int top=25+(i-page*PER_PAGE)*36;g.item(Icons.stack(app,row.id()),5,top);
   g.text(app.mc.font,app.trim(app.catalog.item(row.id()).name,HudLayout.WIDTH-31),26,top,row.done()?Theme.good():Theme.ink(),false);
   String extra=(row.equivalent()>row.have()?"Eq. ":"Known ")+String.format(Locale.US,"%,.2f / %,d",row.equivalent(),row.total());
   if(row.inForge()>0)extra=String.format(Locale.US,"%,.0f + %,.0f in Forge / %,d",row.equivalent(),row.inForge(),row.total());
   else if(row.reserved()>0)extra+=" | "+(row.forge()>0?"Forge: ":"Planned: ")+String.format(Locale.US,"%.0f",row.reserved());

   g.text(app.mc.font,app.trim(extra,HudLayout.WIDTH-93),26,top+11,Theme.muted(),false);
   g.text(app.mc.font,row.percent(),HudLayout.WIDTH-64,top+11,row.done()?Theme.good():Theme.ink(),false);
   g.text(app.mc.font,app.trim(row.needed(),HudLayout.WIDTH-34),26,top+22,Theme.muted(),false);
   g.fill(26,top+32,HudLayout.WIDTH-8,top+34,Theme.well());if(row.inForge()>0)g.fill(26,top+32,26+(int)((HudLayout.WIDTH-34)*row.forgeFraction()),top+34,0xffd69d3a);g.fill(26,top+32,26+(int)((HudLayout.WIDTH-34)*row.fraction()),top+34,0xff72a32b);
  }
  HudControls.render(app,g,goal);
  int footer=HudLayout.HEIGHT-21;long forging=forging(app.progressTrees.get(goal.id));if(ArmorSets.isSet(goal))g.text(app.mc.font,ArmorSets.readyPieces(app.progressTrees.get(goal.id))+"/4 pieces",38,footer+5,Theme.ink(),false);else if(forging>0)g.text(app.mc.font,"Forge: "+forging,38,footer+5,Theme.ink(),false);
  g.fill(6,footer,31,footer+18,page>0?Theme.button():Theme.well());g.outline(6,footer,25,18,0xff977248);g.text(app.mc.font,"<",15,footer+5,page>0?Theme.ink():Theme.muted(),false);
  g.fill(HudLayout.WIDTH-31,footer,HudLayout.WIDTH-6,footer+18,page+1<pages?Theme.button():Theme.well());g.outline(HudLayout.WIDTH-31,footer,25,18,0xff977248);g.text(app.mc.font,">",HudLayout.WIDTH-22,footer+5,page+1<pages?Theme.ink():Theme.muted(),false);
  g.text(app.mc.font,(page+1)+" / "+pages,HudLayout.WIDTH/2-14,footer+5,Theme.muted(),false);g.pose().popMatrix();
  if(mx>=x&&mx<x+HudLayout.WIDTH*scale&&my>=y&&my<y+20*scale)RecipePreview.render(app,g,goal.id,goal,mx,my);
 }
}
