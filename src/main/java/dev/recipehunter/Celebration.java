package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class Celebration {
 private String item="",lastItem="";private long started,lastAt;private Map<String,Long> previous=new HashMap<>();private int craftingGrace=0;private final Set<String> forgeOutputs=new HashSet<>();private int forgeGrace=0;
 public static boolean craftMenu(String title){String t=title.toLowerCase(Locale.ROOT);return t.equals("crafting")||t.equals("craft item")||t.equals("crafting table")||t.equals("quick craft");}
 public void reset(){previous.clear();craftingGrace=0;forgeGrace=0;forgeOutputs.clear();item="";}
 public void message(RecipeHunter app,String text){
  String s=Catalog.normal(text);if(!s.startsWith("you crafted ")&&!s.startsWith("you have crafted "))return;
  for(String id:app.book.history){String name=Catalog.normal(app.catalog.item(id).name);if(s.equals("you crafted "+name)||s.startsWith("you crafted "+name+" ")||s.equals("you have crafted "+name)){start(app,id);break;}}
 }
 public void tick(RecipeHunter app){
  Map<String,Long> now=app.ownership.inventory(app.mc);
  if(app.mc.screen instanceof AbstractContainerScreen<?> screen&&craftMenu(screen.getTitle().getString()))craftingGrace=8;else craftingGrace=Math.max(0,craftingGrace-1);
  if(app.mc.screen instanceof AbstractContainerScreen<?> screen&&screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("forge")){
   forgeGrace=8;for(var slot:screen.getMenu().slots)if(slot.container!=app.mc.player.getInventory()){var stack=slot.getItem();var lore=stack.get(net.minecraft.core.component.DataComponents.LORE);if(lore!=null&&lore.lines().stream().anyMatch(l->l.getString().toLowerCase(Locale.ROOT).contains("claim")))forgeOutputs.add(Ownership.id(stack));}
  }else {forgeGrace=Math.max(0,forgeGrace-1);if(forgeGrace==0)forgeOutputs.clear();}
  if(forgeGrace>0&&!previous.isEmpty())for(String id:app.book.history)if(forgeOutputs.contains(id)&&now.getOrDefault(id,0L)>previous.getOrDefault(id,0L))start(app,id);
  if(craftingGrace>0&&!previous.isEmpty())for(String id:app.book.history){if(now.getOrDefault(id,0L)<=previous.getOrDefault(id,0L))continue;
    boolean consumed=app.catalog.item(id).routes.stream().filter(r->r.type.equals("crafting")).anyMatch(r->r.inputs.stream().allMatch(in->previous.getOrDefault(in.id,0L)-now.getOrDefault(in.id,0L)>=in.count));
    if(consumed)start(app,id);
  }
  previous=now;
 }
 public void start(RecipeHunter app,String id){if(!app.book.celebrations)return;long now=System.currentTimeMillis();if(id.equals(lastItem)&&now-lastAt<4000)return;lastItem=id;lastAt=now;item=id;started=now;
  if(app.book.celebrationSound)app.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,1.0f,0.5f));
 }
 public void render(RecipeHunter app,GuiGraphicsExtractor g){
  if(item.isEmpty()||!app.book.celebrations)return;long age=System.currentTimeMillis()-started;if(age>5500){item="";return;}
  int w=g.guiWidth(),h=g.guiHeight(),cx=w/2,cy=h/2;float fade=Math.min(1f,Math.min(age/350f,(5500-age)/700f));int alpha=(int)(fade*190);
  g.fill(0,0,w,h,alpha<<24|0x071310);g.fill(cx-145,cy-74,cx+145,cy+77,((int)(fade*230)<<24)|0x102b25);
  g.pose().pushMatrix();g.pose().translate(cx-32,cy-52);g.pose().scale(4,4);g.item(Icons.stack(app,item),0,0);g.pose().popMatrix();
  g.centeredText(app.mc.font,"Congratulations!",cx,cy+22,0xffa1f3bd);g.centeredText(app.mc.font,"You crafted",cx,cy+38,0xfff2f4ee);g.centeredText(app.mc.font,app.trim(app.catalog.item(item).name,280),cx,cy+54,0xff000000|app.catalog.item(item).color);
 }
}
