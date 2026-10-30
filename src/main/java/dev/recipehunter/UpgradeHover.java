package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Two-second dwell, derived from actual recipe inputs and the current physical pool. */
public final class UpgradeHover {
 private String key="";private long since;
 public void render(RecipeHunter app,GuiGraphicsExtractor g,String id,int mx,int my){
  long now=System.currentTimeMillis();if(!id.equals(key)){key=id;since=now;}if(id.isEmpty()||now-since<2000)return;
  var steps=CraftSteps.next(app.catalog,id,app.physicalStock(),CraftSteps.targets(app.progressTrees.values()));
  List<String> lines=new ArrayList<>();lines.add("Next recipe from "+app.catalog.item(id).name);
  if(steps.isEmpty())lines.add("No further crafting / Forge recipe.");
  for(var step:steps.stream().limit(3).toList()){
   lines.add(step.output()+"x "+app.catalog.item(step.id()).name+" | "+step.batches()+" batch(es) ready");
   for(var e:step.required().entrySet()){
    long have=step.have().getOrDefault(e.getKey(),0L);lines.add("  "+app.catalog.item(e.getKey()).name+": "+Catalog.qty(have)+" / "+e.getValue()+" (need "+Math.max(0,e.getValue()-have)+")");
   }
  }
  if(steps.size()>3)lines.add("+"+(steps.size()-3)+" other uses; open recipe for all inputs.");
  lines.add("Owned materials only; Forge output counts after collection.");
  int w=Math.min(420,g.guiWidth()-4),max=Math.max(2,(g.guiHeight()-16)/12);if(lines.size()>max){lines=new ArrayList<>(lines.subList(0,max-1));lines.add("More ingredients in the full recipe.");}
  int h=lines.size()*12+12,x=Math.clamp(mx+10,2,Math.max(2,g.guiWidth()-w-2)),y=Math.clamp(my+12,2,Math.max(2,g.guiHeight()-h-2));
  g.fill(x,y,x+w,y+h,0xffeadbb5);g.outline(x,y,w,h,0xff6c472a);int yy=y+6;for(String line:lines){g.text(app.mc.font,app.trim(line,w-12),x+6,yy,Theme.INK,false);yy+=12;}
 }
}
