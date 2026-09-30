package dev.recipehunter;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Cursor is unlocked only while this editor is open. ESC saves and returns to play. */
public final class HudEditor extends Screen {
    private final RecipeHunter app;
    private Book.Goal dragging;
    private double offsetX,offsetY;
    private long page;
    public HudEditor(RecipeHunter app){super(Component.literal("Recipe Hunter HUD editor"));this.app=app;}
    @Override protected void init(){
        addRenderableWidget(Button.builder(Component.literal("Settings"),b->app.open(new HunterScreen(app,HunterScreen.Mode.SETTINGS,""))).bounds(8,height-25,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width-88,height-25,80,20).build());
        for(Book.Goal goal:app.book.goals)if(goal.visible)app.hudPlans.put(goal.id,app.plan(goal));
    }
    private Book.Goal hit(double mx,double my){
        Book.Goal result=null;int i=0;
        for(Book.Goal goal:app.book.goals)if(goal.visible&&HudPanels.layout(app,goal,i++,width,height).contains(mx,my,width,height))result=goal;
        return result;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice))return true;
        Book.Goal goal=hit(e.x(),e.y());if(goal==null)return false;
        if(e.button()==1){page++;return true;}
        if(e.button()!=0)return false;
        dragging=goal;offsetX=e.x()-goal.hud.left(width,height);offsetY=e.y()-goal.hud.top(width,height);return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging==null)return super.mouseDragged(e,dx,dy);
        dragging.hud.move(e.x()-offsetX,e.y()-offsetY,width,height);return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging!=null){dragging=null;app.persist();return true;}return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){Book.Goal goal=hit(x,y);if(goal==null)return super.mouseScrolled(x,y,sx,sy);goal.hud.zoom(sy*0.05,width,height);app.persist();return true;}
    @Override public void onClose(){app.persist();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        g.fill(0,0,width,height,0x66101820);
        g.text(font,"Drag panel | Wheel: size | Right-click: next materials | ESC: save",8,4,0xffffffff,true);
        if(app.book.goals.stream().noneMatch(v->v.visible))g.text(font,"Save and show a recipe in /rh first.",12,35,0xffe1c991,true);
        super.extractRenderState(g,mx,my,dt);
        HudPanels.render(app,g,dragging==null?mx:-1,dragging==null?my:-1,page);
    }
}
