package dev.recipehunter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class ForgeHud {
 public static void render(RecipeHunter app,GuiGraphicsExtractor g,boolean force){
  if(!force&&!app.book.forgeVisible)return;var l=app.book.forgeHud;int w=g.guiWidth(),h=g.guiHeight();
  g.pose().pushMatrix();g.pose().translate(l.left(w,h),l.top(w,h));g.pose().scale((float)l.scale(w,h),(float)l.scale(w,h));panel(app,g);g.pose().popMatrix();
 }
 public static void panel(RecipeHunter app,GuiGraphicsExtractor g){
  long now=System.currentTimeMillis();var jobs=ForgeState.active(app.menus.forge);
  g.fill(0,0,ForgeLayout.W,ForgeLayout.H,Theme.paper());g.outline(0,0,ForgeLayout.W,ForgeLayout.H,Theme.edge());g.fill(1,1,ForgeLayout.W-1,19,0xff4d3924);
  g.text(app.mc.font,"RECIPE HUNTER — FORGE",6,6,0xffffdb87,true);g.text(app.mc.font,RecipeHunter.VERSION,ForgeLayout.W-33,6,0xffc8b99b,false);
  for(int slot=1;slot<=7;slot++){
   int sn=slot,y=24+(slot-1)*20;var e=jobs.stream().filter(v->ForgeState.slot(v)==sn).findFirst().orElse(null);
   g.fill(6,y-1,24,y+17,Theme.well());
   if(e==null){g.text(app.mc.font,""+slot,11,y+3,Theme.ink(),false);g.text(app.mc.font,(app.menus.forgeState.slots.getOrDefault(slot,ForgeState.Slot.UNKNOWN)==ForgeState.Slot.BUSY?"Awaiting update":app.menus.forgeState.slots.getOrDefault(slot,ForgeState.Slot.UNKNOWN).toString()),30,y+3,Theme.muted(),false);}
   else {g.item(Icons.stack(app,e.id),7,y);g.text(app.mc.font,app.trim(app.catalog.item(e.id).name,ForgeLayout.W-35),30,y,Theme.ink(),false);g.text(app.mc.font,e.readyVerified?"Ready to Collect":e.readyAt<=now?"Timer elapsed — unverified":"Busy | "+ForgeState.duration(e.readyAt-now),30,y+10,e.readyVerified?Theme.good():Theme.muted(),false);}
  }
  g.text(app.mc.font,"Finished in: "+ForgeCommands.finished(app.menus,now),6,169,Theme.ink(),false);
  g.text(app.mc.font,app.trim(app.menus.forgeState.observedAt==0?"Open Forge or enable its Tab widget":app.menus.forgeState.source+" | seen "+ForgeState.duration(now-app.menus.forgeState.observedAt)+" ago",ForgeLayout.W-12),6,184,Theme.muted(),false);
 }
}
