package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** One selected next craft, after a continuous three-second dwell. Computation is revision-cached. */
public final class UpgradeHover {
 private final HoverDwell dwell=new HoverDwell();private String cachedKey="";private long revision=-1;private List<String> lines=List.of();
 public void render(RecipeHunter app,GuiGraphicsExtractor g,String id,int mx,int my){render(app,g,id,null,mx,my);}
 public void render(RecipeHunter app,GuiGraphicsExtractor g,String id,Book.Goal goal,int mx,int my){
  String key=id.isEmpty()?"":id+":"+(goal==null?"":goal.id);if(!dwell.ready(key,System.currentTimeMillis()))return;
  if(!key.equals(cachedKey)||revision!=app.materialRevision){cachedKey=key;revision=app.materialRevision;
   String next=CraftReadiness.next(app.catalog,id,goal,goal==null?null:app.tree(goal));
   lines=next.isEmpty()?List.of("Next Craft: None"):describe(app.catalog,CraftReadiness.assess(app.catalog,next,goal==null?Map.of():goal.choices,app.observedStock));
  }
  int localWidth=390;List<String> wrapped=new ArrayList<>();for(String line:lines){String rest=line;while(app.mc.font.width(rest)>localWidth-12){String cut=app.mc.font.plainSubstrByWidth(rest,localWidth-12);wrapped.add(cut);rest=rest.substring(cut.length());}wrapped.add(rest);}
  int height=wrapped.size()*11+12;float scale=(float)Math.min(1,Math.min((g.guiWidth()-4)/(double)localWidth,(g.guiHeight()-4)/(double)height));
  int w=(int)(localWidth*scale),h=(int)(height*scale),x=Math.clamp(mx+10,2,Math.max(2,g.guiWidth()-w-2)),y=Math.clamp(my+12,2,Math.max(2,g.guiHeight()-h-2));
  g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.fill(0,0,localWidth,height,Theme.paper());g.outline(0,0,localWidth,height,Theme.edge());int yy=6;
  for(String line:wrapped){g.text(app.mc.font,line,6,yy,line.equals("Ready to Craft")?Theme.good():Theme.ink(),false);yy+=11;}g.pose().popMatrix();
 }
 public static List<String> describe(Catalog c,CraftReadiness.Result r){List<String> out=new ArrayList<>();out.add("Next Craft: "+c.item(r.id()).name);out.add("Requires:");r.required().forEach((id,n)->out.add("  "+c.item(id).name+" x"+Catalog.qty(n)));
  out.add("You Have:");r.required().forEach((id,n)->{long owned=r.owned().get(id),available=r.available().get(id);out.add("  "+c.item(id).name+" x"+Catalog.qty(owned)+(available>owned?" (+"+(available-owned)+" through crafting)":""));});
  out.add("Missing:");boolean none=true;for(var e:r.required().entrySet()){long n=e.getValue()-r.available().get(e.getKey());if(n>0){none=false;out.add("  "+c.item(e.getKey()).name+" x"+Catalog.qty(n));}}if(none)out.add("  None");out.add(r.ready()?"Ready to Craft":"Not Ready");out.add("Unobserved storage: Unknown. Unlocks still apply.");return List.copyOf(out);}
}
