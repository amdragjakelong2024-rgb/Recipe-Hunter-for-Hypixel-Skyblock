package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Per-goal controls used only with an unlocked inventory/chat/editor cursor. */
public final class HudControls {
 public static final List<String> MATERIALS=List.of("Compact","Enchanted","Enchanted Block"),GEMS=List.of("Fine","Flawless","Perfect");
 public static void cycle(Book.Goal goal,boolean gems){if(gems)goal.gemDisplay=SettingsScreen.next(goal.gemDisplay,GEMS);else goal.materialDisplay=SettingsScreen.next(goal.materialDisplay,MATERIALS);goal.hudPage=0;}
 public static boolean click(RecipeHunter app,Book.Goal goal,double mx,double my,int w,int h){var l=goal.hud;double x=(mx-l.left(w,h))/l.scale(w,h),y=(my-l.top(w,h))/l.scale(w,h);if(y< HudLayout.HEIGHT-46||y>=HudLayout.HEIGHT-25||x<6||x>=HudLayout.WIDTH-6)return false;cycle(goal,x>=HudLayout.WIDTH/2);app.persist();return true;}
 public static void render(RecipeHunter app,GuiGraphicsExtractor g,Book.Goal goal){int y=HudLayout.HEIGHT-46;g.fill(6,y,HudLayout.WIDTH-6,y+20,Theme.well());g.outline(6,y,HudLayout.WIDTH-12,20,Theme.edge());g.text(app.mc.font,app.trim(goal.materialDisplay,126),10,y+6,Theme.ink(),false);g.text(app.mc.font,app.trim(goal.gemDisplay,126),HudLayout.WIDTH/2+6,y+6,Theme.ink(),false);}
}
