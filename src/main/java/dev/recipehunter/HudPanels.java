package dev.recipehunter;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudPanels {
    public static HudLayout layout(RecipeHunter app,Book.Goal goal,int index,int w,int h){
        if(goal.hud==null){goal.hud=new HudLayout();goal.hud.scale=app.book.hudScale;goal.hud.move(w-HudLayout.WIDTH*goal.hud.scale-8,12+index*73*goal.hud.scale,w,h);}return goal.hud;
    }
    public static void render(RecipeHunter app,GuiGraphicsExtractor g,int mx,int my,long pageClock){
        String hover=null;Book.Goal hoverGoal=null;int index=0;
        for(Book.Goal goal:app.book.goals){if(!goal.visible)continue;
            HudLayout l=layout(app,goal,index++,g.guiWidth(),g.guiHeight());float s=(float)l.scale(g.guiWidth(),g.guiHeight());
            int x=l.left(g.guiWidth(),g.guiHeight()),y=l.top(g.guiWidth(),g.guiHeight());
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(s,s);
            g.fill(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xdd101921);g.outline(0,0,HudLayout.WIDTH,HudLayout.HEIGHT,0xff526a77);
            g.text(app.mc.font,app.trim(app.catalog.item(goal.id).name,226),4,4,0xff000000|app.catalog.item(goal.id).color,true);
            Planner.Plan p=app.hudPlans.get(goal.id);
            if(p==null)g.text(app.mc.font,"Checking materials...",4,20,0xffacbdca,true);
            else{
                var leaves=new ArrayList<>(p.leaves.entrySet());int pages=Math.max(1,(leaves.size()+2)/3),page=(int)Math.floorMod(pageClock,pages);
                if(leaves.isEmpty())g.text(app.mc.font,p.warnings.isEmpty()?"Checklist ready / goal owned":"Check recipe warnings",4,22,0xff83d9a5,true);
                for(int i=page*3;i<Math.min(page*3+3,leaves.size());i++){
                    var e=leaves.get(i);int row=20+(i-page*3)*12;
                    g.text(app.mc.font,app.trim(Catalog.amount(e.getKey(),e.getValue())+" "+app.catalog.item(e.getKey()).name,226),4,row,0xffd4dbe1,true);
                    double lx=(mx-x)/s,ly=(my-y)/s;
                    if(lx>=0&&lx<HudLayout.WIDTH&&ly>=row&&ly<row+12){hover=e.getKey();hoverGoal=goal;}
                }
                if(pages>1)g.text(app.mc.font,(page+1)+"/"+pages,205,55,0xff849bab,false);
            }
            g.pose().popMatrix();
        }
        if(hover!=null)RecipePreview.render(app,g,hover,hoverGoal,mx,my);
    }
}
