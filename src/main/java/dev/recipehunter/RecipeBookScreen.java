package dev.recipehunter;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Two-page book over the live world. All coordinates are book-local. */
public final class RecipeBookScreen extends Screen {
 private final RecipeHunter app;private boolean all,treeMode;private String query="",selected="",category="All";
 private int page,materialPage,ticks;private float scale;private int left,top;private long turnedAt;
 private static final int W=720,H=430,L=28,R=382,COL=306;
 private EditBox search;private final List<Entry> entries=new ArrayList<>();private final List<Text> texts=new ArrayList<>();private final List<ItemIcon> icons=new ArrayList<>();
 private record Text(String text,int x,int y,int width,int color){}
 private record Entry(Book.Goal goal,int x,int y,int width){}
 private record ItemIcon(String id,int x,int y){}
 public RecipeBookScreen(RecipeHunter app,boolean all,String query,String selected){super(Component.literal(all?"All Recipes":"Recipe Book"));this.app=app;this.all=all;this.query=query;this.selected=selected;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){app.closeBook();}
 private void button(String label,int x,int y,int w,Runnable action){addRenderableWidget(new GlowButton(label,x,y,w,action));}
 private void text(String value,int x,int y,int width,int color){texts.add(new Text(value,x,y,width,color));}
 private void flip(){turnedAt=System.nanoTime();UiSounds.page(app);rebuildWidgets();}
 @Override protected void init(){
  scale=(float)Math.min(width/(double)W,height/(double)H)*Math.min(.94,.88*app.book.guiScale);left=(int)((width-W*scale)/2);top=(int)((height-H*scale)/2);
  entries.clear();texts.clear();icons.clear();
  text(all?"ALL RECIPES":"RECIPE BOOK",L,36,COL,Theme.INK);
  if(!all)text("Your saved recipes show here",L,51,COL,Theme.MUTED);
  button(all?"Recipe Book":"All Recipes",L,68,108,()->{all=!all;page=0;flip();});
  button("Completed",L+114,68,92,()->app.open(new HunterScreen(app,HunterScreen.Mode.BOOK,"")));
  button("Close",W-78,14,50,this::onClose);
  search=addRenderableWidget(new EditBox(font,L,96,COL,20,Component.literal("Search recipe or item")));search.setMaxLength(100);search.setValue(query);search.setHint(Component.literal("Search recipe or item..."));search.setTextColor(Theme.INK);search.setTextColorUneditable(Theme.MUTED);search.setBordered(false);
  search.setResponder(value->{query=value;page=0;});
  button("Search",L+230,119,76,()->{all=true;flip();});
  if(all){List<String> categories=List.of("All","Crafting","Forge","Pets","Other");int x=L;for(String c:categories){button((category.equals(c)?"*":"")+c,x,140,60,()->{category=c;page=0;flip();});x+=62;}}
  List<Catalog.Item> items=all?catalogItems():app.book.goals.stream().filter(g->query.isBlank()||Catalog.normal(app.catalog.item(g.id).name).contains(Catalog.normal(query))).map(g->app.catalog.item(g.id)).toList();
  int perPage=5,totalPages=Math.max(1,(items.size()+perPage-1)/perPage);page=Math.clamp(page,0,totalPages-1);
  for(int i=page*perPage;i<Math.min(items.size(),(page+1)*perPage);i++){
   var item=items.get(i);int y=170+(i-page*perPage)*43;Book.Goal goal=app.book.find(item.id);
   icons.add(new ItemIcon(item.id,L,y+1));
   button(app.trim(item.name,COL-28),L+23,y,COL-23,()->{selected=item.id;materialPage=0;flip();});
   if(goal!=null){entries.add(new Entry(goal,L+23,y+24,136));
    button(goal.visible?"Hide":"Show",L+164,y+21,42,()->{goal.visible=!goal.visible;app.persist();rebuildWidgets();});
    button("^",L+208,y+21,22,()->{app.book.move(goal,-1);app.persist();rebuildWidgets();});
    button("v",L+232,y+21,22,()->{app.book.move(goal,1);app.persist();rebuildWidgets();});
    button("Remove",L+257,y+21,49,()->{app.removeGoal(goal);rebuildWidgets();});
   }else text(app.book.completed.contains(item.id)?"Completed ✓":"",L+24,y+25,220,0xff426c20);
  }
  if(items.isEmpty())text("No recipes here yet",L,181,COL,Theme.MUTED);
  button("<",L,394,28,()->{page=Math.max(0,page-1);flip();});button(">",L+COL-28,394,28,()->{page=Math.min(totalPages-1,page+1);flip();});text("Page "+(page+1)+" / "+totalPages,L+92,401,180,Theme.MUTED);
  if(all||!selected.isEmpty())detail();else settings();
 }
 private List<Catalog.Item> catalogItems(){return app.catalog.items.values().stream().filter(i->!i.id.endsWith("_NPC")&&!i.id.endsWith("_BOSS"))
  .filter(i->query.isBlank()||Catalog.normal(i.name).contains(Catalog.normal(query)))
  .filter(i->switch(category){case "Crafting"->i.routes.stream().anyMatch(r->"crafting".equals(r.type));case "Forge"->i.routes.stream().anyMatch(r->"forge".equals(r.type));case "Pets"->i.name.endsWith(" Pet");case "Other"->i.routes.isEmpty();default->true;})
  .sorted(Comparator.comparing(i->i.name)).toList();}
 private void settings(){
  text("GUI SETTINGS",R,36,COL,Theme.INK);
  text("GUI Scale",R,72,130,Theme.MUTED);button("-",R+156,66,26,()->{app.book.guiScale=Math.max(.65,app.book.guiScale-.1);app.persist();rebuildWidgets();});button("+",R+188,66,26,()->{app.book.guiScale=Math.min(1.6,app.book.guiScale+.1);app.persist();rebuildWidgets();});
  button("HUD position & size",R,96,COL,()->app.open(new HudEditor(app)));
  button("Sounds: "+(app.book.uiSounds?"On":"Off"),R,124,COL,()->{app.book.uiSounds=!app.book.uiSounds;app.persist();rebuildWidgets();});
  button("Animation: "+(app.book.animations?"On":"Off"),R,152,COL,()->{app.book.animations=!app.book.animations;app.persist();rebuildWidgets();});
  text("FEATURES",R,198,COL,Theme.INK);
  button("Recipe Planner  >",R,221,COL,()->{all=true;page=0;flip();});
  button("Diary  >",R,254,COL,()->app.open(new ToolsScreen(app,"Craft Diary")));
  button("Calculator  >",R,287,COL,()->app.open(new ToolsScreen(app,"Calculator")));
  text("AI / API",R,347,COL,Theme.MUTED);button("Assistant settings & chat",R,365,COL,()->app.open(new ToolsScreen(app,"AI")));
 }
 private void detail(){
  if(selected.isEmpty()){text("Select a recipe",R,45,COL,Theme.MUTED);return;}
  Book.Goal goal=app.goal(selected);Catalog.Item item=app.catalog.item(selected);icons.add(new ItemIcon(selected,R,36));text(item.name,R+24,40,COL-24,Theme.INK);
  button("-",R,63,24,()->quantity(goal,Math.max(1,goal.count-1)));button("+",R+28,63,24,()->quantity(goal,Math.min(1000000,goal.count+1)));
  EditBox count=addRenderableWidget(new EditBox(font,R+58,63,70,20,Component.literal("Quantity")));count.setMaxLength(7);count.setValue(Long.toString(goal.count));count.setTextColor(Theme.INK);count.setBordered(false);
  button("Set",R+132,63,40,()->{try{quantity(goal,Long.parseLong(count.getValue()));}catch(NumberFormatException e){app.say("Enter a whole quantity from 1 to 1000000.");}});
  button("Reset x1",R+180,63,72,()->quantity(goal,1));
  button("Back",R+259,63,47,()->{selected="";all=false;flip();});
  RecipeProgress engine=new RecipeProgress(app.catalog);RecipeProgress.Node tree=app.tree(goal);
  text("Materials (known stock)",R,94,COL,Theme.INK);
  List<RecipeProgress.Node> materials;if(treeMode){materials=new ArrayList<>();for(var child:tree.children)treeRows(child,goal,materials);}else materials=engine.displayed(tree,goal);int pages=Math.max(1,(materials.size()+2)/3);materialPage=Math.clamp(materialPage,0,pages-1);
  for(int i=materialPage*3;i<Math.min(materials.size(),materialPage*3+3);i++){
   var node=materials.get(i);int y=112+(i-materialPage*3)*44;icons.add(new ItemIcon(node.id,R,y));
   button((node.children.isEmpty()?"":goal.expanded.contains(node.id)?"- ":"+ ")+app.trim(app.catalog.item(node.id).name,COL-39),R+24,y,COL-24,()->{if(!goal.expanded.remove(node.id))goal.expanded.add(node.id);treeMode=true;app.persist();flip();});
   text(node.row().percent()+" | "+number(node.equivalent)+" / "+Catalog.qty(node.total),R+24,y+21,COL-24,Theme.MUTED);
   String parent=node.parent.isEmpty()?selected:node.parent;text((node.forge>0?"Forge: "+node.forge+" | ":node.reserved>0?"Reserved: "+number(node.reserved)+" | ":"")+"For: "+app.catalog.item(parent).name,R+24,y+32,COL-24,Theme.MUTED);
  }
  button("<",R,247,24,()->{materialPage=Math.max(0,materialPage-1);flip();});button(">",R+28,247,24,()->{materialPage=Math.min(pages-1,materialPage+1);flip();});
  button(treeMode?"Materials":"Recipe tree",R+60,247,104,()->{treeMode=!treeMode;materialPage=0;flip();});
  button("Reset branches",R+172,247,134,()->{goal.expanded.clear();app.persist();flip();});
  selector("Material Display",new ArrayList<>(engine.materialOptions(tree)),goal.materialDisplay,R,274,value->{goal.materialDisplay=value;app.persist();rebuildWidgets();});
  selector("Gemstone Display",new ArrayList<>(engine.gemOptions(tree)),goal.gemDisplay,R,306,value->{goal.gemDisplay=value;app.persist();rebuildWidgets();});
  if(!item.routes.isEmpty()){
   Catalog.Route route=item.routes.get(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size()));
   if(!route.slots.isEmpty())for(int i=0;i<route.slots.size()&&i<9;i++){var slot=route.slots.get(i);if(slot!=null){icons.add(new ItemIcon(slot.id,R+i%3*19,340+i/3*18));text(Long.toString(slot.count),R+11+i%3*19,350+i/3*18,20,Theme.MUTED);}}
   else {text(route.type.toUpperCase(Locale.ROOT),R,345,100,Theme.MUTED);text("Output: "+route.count,R,360,100,Theme.MUTED);}
   icons.add(new ItemIcon(item.id,R+84,359));
   if(item.routes.size()>1)button("Route "+(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size())+1),R+127,354,90,()->{goal.choices.merge(item.id,1,Integer::sum);app.persist();flip();});
  }else text("Acquired directly",R,351,COL,Theme.MUTED);
  button("Save as Goal",R+60,395,186,()->{app.save(goal);rebuildWidgets();});
 }
 private void treeRows(RecipeProgress.Node node,Book.Goal goal,List<RecipeProgress.Node> rows){rows.add(node);if(goal.expanded.contains(node.id))for(var child:node.children)treeRows(child,goal,rows);}
 private void selector(String title,List<String> options,String current,int x,int y,java.util.function.Consumer<String> action){if(options.isEmpty())return;text(title,x,y,COL,Theme.MUTED);int w=COL/options.size();for(int i=0;i<options.size();i++){String option=options.get(i);button((option.equals(current)?"* ":"")+option,x+i*w,y+10,w-3,()->action.accept(option));}}
 private void quantity(Book.Goal goal,long n){if(n<1||n>1000000){app.say("Enter a whole quantity from 1 to 1000000.");return;}goal.count=n;app.persist();UiSounds.count(app);rebuildWidgets();}
 private static String number(double n){return n==Math.rint(n)?String.format(Locale.US,"%,.0f",n):String.format(Locale.US,"%,.3f",n);}
 @Override public void tick(){super.tick();if(++ticks%10==0&&!(getFocused() instanceof EditBox))rebuildWidgets();}
 private MouseButtonEvent local(MouseButtonEvent e){return new MouseButtonEvent((e.x()-left)/scale,(e.y()-top)/scale,e.buttonInfo());}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){var p=local(e);for(var icon:icons)if(p.x()>=icon.x&&p.x()<icon.x+18&&p.y()>=icon.y&&p.y()<icon.y+18){app.gameRecipe(icon.id);return true;}return super.mouseClicked(p,twice);}
 @Override public boolean mouseReleased(MouseButtonEvent e){return super.mouseReleased(local(e));}
 @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(local(e),dx/scale,dy/scale);}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(x<left+W*scale/2){page=Math.max(0,page+(sy<0?1:-1));flip();return true;}return super.mouseScrolled((x-left)/scale,(y-top)/scale,sx,sy);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  int lx=(int)((mx-left)/scale),ly=(int)((my-top)/scale);g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(scale,scale);
  Theme.background(g,font,W,H,"");for(var t:texts)g.text(font,app.trim(t.text,t.width),t.x,t.y,t.color,false);
  super.extractRenderState(g,lx,ly,dt);
  for(var icon:icons)g.item(Icons.stack(app,icon.id),icon.x,icon.y);
  for(var entry:entries){var root=app.tree(entry.goal);double fraction=root.fraction();g.fill(entry.x,entry.y,entry.x+entry.width,entry.y+7,0xffb9a780);g.fill(entry.x,entry.y,entry.x+(int)(entry.width*fraction),entry.y+7,0xff528b25);g.text(font,root.row().percent(),entry.x,entry.y+9,Theme.INK,false);}
  // A page sweeps across the spine; the real world remains visible outside the book.
  double t=(System.nanoTime()-turnedAt)/1e9/.32;if(app.book.animations&&t>=0&&t<1){int w=(int)(Math.sin(t*Math.PI)*(W/2-15));int x=t<.5?W/2-w:W/2;g.fill(x,29,x+w,H-12,0xe8e9d6a9);g.fill(x,29,x+2,H-12,0xffa88751);}
  g.pose().popMatrix();for(var icon:icons)if(lx>=icon.x&&lx<icon.x+18&&ly>=icon.y&&ly<icon.y+18)RecipePreview.render(app,g,icon.id,selected.isEmpty()?null:app.goal(selected),mx,my);
 }
}
