package dev.recipehunter;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;

/** Uses recorded slot positions only: forge/upgrades are labelled ingredient lists. */
public final class RecipePreview {
    public static void render(RecipeHunter app,GuiGraphicsExtractor g,String id,Book.Goal goal,int mx,int my){
        Catalog.Item item=app.catalog.item(id);
        int w=Math.min(390,g.guiWidth()),h=Math.min(g.guiHeight(),Math.max(132,42+item.routes.stream().mapToInt(r->r.inputs.size()).max().orElse(0)*12));
        int x=Math.clamp(mx+12,0,Math.max(0,g.guiWidth()-w)),y=Math.clamp(my+12,0,Math.max(0,g.guiHeight()-h));
        g.fill(x,y,x+w,y+h,0xff111c28);g.outline(x,y,w,h,0xffc4a46c);
        g.text(app.mc.font,app.trim(item.name,w-12),x+6,y+6,0xff000000|item.color,true);
        if(item.routes.isEmpty()){
            g.item(Icons.stack(app,id),x+7,y+25);
            g.text(app.mc.font,"No crafting recipe",x+30,y+28,0xffe0d5ba,false);
            String source=item.sources.isEmpty()?"Source not recorded":item.sources.getFirst();
            g.textWithWordWrap(app.mc.font,net.minecraft.network.chat.Component.literal(app.trim(source,3*(w-12))),x+6,y+51,w-12,0xffc4d0dc);return;
        }
        int choice=goal==null?0:goal.choices.getOrDefault(id,0);
        Catalog.Route route=item.routes.get(Math.floorMod(choice,item.routes.size()));
        boolean grid=route.slots!=null&&route.slots.size()==9;
        List<Catalog.Ingredient> slots=grid?route.slots:route.inputs;
        g.text(app.mc.font,app.trim((grid?"Crafting":route.type)+" — full recipe",w-12),x+6,y+20,0xffbac8d4,false);
        for(int i=0;i<Math.min(slots.size(),9);i++){
            int sx=x+7+(i%3)*24,sy=y+34+(i/3)*24;g.fill(sx-1,sy-1,sx+22,sy+22,0xff344352);
            Catalog.Ingredient in=slots.get(i);if(in==null||in.id==null||in.id.isBlank()||in.count<1)continue;
            g.item(Icons.stack(app,in.id),sx+2,sy+1);
            if(in.count>1)g.text(app.mc.font,Long.toString(in.count),sx+2,sy+14,0xffffffff,true);
        }
        g.text(app.mc.font,">",x+87,y+61,0xffe9d39b,true);g.item(Icons.stack(app,id),x+106,y+57);
        g.text(app.mc.font,"x"+route.count,x+129,y+61,0xffffffff,true);
        long target=goal!=null&&id.equals(goal.id)?goal.count:route.count;long runs=(target-1)/route.count+1;
        g.text(app.mc.font,"Target: x"+target,x+6,y+h-14,0xffb9e6dc,false);
        for(int i=0;i<route.inputs.size();i++){var in=route.inputs.get(i);g.text(app.mc.font,app.trim((in.count*runs)+"x "+app.catalog.item(in.id).name,w-160),x+156,y+34+i*12,0xffdae4f1,false);}
    }
}
