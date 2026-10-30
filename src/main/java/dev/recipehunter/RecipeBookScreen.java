package dev.recipehunter;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Two-page book over the live world. All coordinates are book-local. */
public final class RecipeBookScreen extends Screen {
 private final RecipeHunter app;private final UpgradeHover upgradeHover=new UpgradeHover();private boolean all,treeMode,emptyDetail;private String query="",selected="",category="All";
 private int page,materialPage,ticks,totalPages=1;private BookViewport viewport;private boolean refreshSearch;private CompletionFilter.Mode filter=CompletionFilter.Mode.ALL;private float scale;private int left,top;private long turnedAt;
 private static final int W=720,H=430,L=28,R=382,COL=306;
 private EditBox search;private final List<Entry> entries=new ArrayList<>();private final List<Text> texts=new ArrayList<>();private final List<ItemIcon> icons=new ArrayList<>();private final List<int[]> gridCells=new ArrayList<>();
 private record Text(String text,int x,int y,int width,int color){}
 private record Entry(Book.Goal goal,int x,int y,int width){}
 private final List<int[]> cards=new ArrayList<>();private final List<MaterialView> materialViews=new ArrayList<>();
 private record MaterialView(int index,int y){}
 private record ItemIcon(String id,int x,int y,long count){ItemIcon(String id,int x,int y){this(id,x,y,0);}}
 public RecipeBookScreen(RecipeHunter app,boolean all,String query,String selected){super(Component.literal(all?"All Recipes":"Recipe Book"));this.app=app;this.all=all;this.query=query;this.selected=selected;}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){app.closeBook();}
 private BookButton button(String label,int x,int y,int w,Runnable action){return control(label,x,y,w,20,false,action);}
 private BookButton control(String label,int x,int y,int w,int h,boolean quiet,Runnable action){return addRenderableWidget(new BookButton(label,viewport.x(x),viewport.y(y),viewport.size(w),viewport.size(h),viewport.scale(),quiet,action));}
 private void listPage(int delta){int next=BookViewport.page(page,delta,totalPages);if(next==page)return;page=next;flip();}
 private void materialPage(int delta,int total){int next=BookViewport.page(materialPage,delta,total);if(next==materialPage)return;materialPage=next;flip();}
 private void text(String value,int x,int y,int width,int color){texts.add(new Text(value,x,y,width,color));}
 private void flip(){turnedAt=System.nanoTime();UiSounds.page(app);rebuildWidgets();}
 @Override protected void init(){
  viewport=BookViewport.fit(width,height,app.book.guiScale);scale=viewport.scale();left=viewport.left();top=viewport.top();
  addRenderableWidget(new GearButton(viewport.x(685),viewport.y(8),viewport.size(22),()->app.open(new SettingsScreen(app,this))));
  entries.clear();texts.clear();icons.clear();gridCells.clear();cards.clear();materialViews.clear();
  if(!all)text("Your saved recipes show here",L+3,53,COL,Theme.muted());
  control(all?"Recipe Book":"Back to all recipes",L,66,all?88:151,18,false,()->{all=!all;selected="";emptyDetail=false;page=0;rebuildWidgets();});
  control("Hide completed",L+(all?92:155),66,all?112:151,18,false,()->{filter=filter==CompletionFilter.Mode.HIDE_COMPLETED?CompletionFilter.Mode.ALL:CompletionFilter.Mode.HIDE_COMPLETED;page=0;rebuildWidgets();}).selected(filter==CompletionFilter.Mode.HIDE_COMPLETED);
  if(all)control("Owned only",L+208,66,98,18,false,()->{filter=filter==CompletionFilter.Mode.OWNED_ONLY?CompletionFilter.Mode.ALL:CompletionFilter.Mode.OWNED_ONLY;page=0;rebuildWidgets();}).selected(filter==CompletionFilter.Mode.OWNED_ONLY);
  search=addRenderableWidget(new SearchBox(font,viewport.x(L+8),viewport.y(93),viewport.size(COL-53),Math.max(14,viewport.size(20)),Component.literal("Search recipe or item"),viewport.scale()*1.2f));
  search.setMaxLength(100);search.setValue(query);search.setTextColor(Theme.ink());search.setBordered(false);search.setHint(Component.literal("Search recipes..."));
  search.setResponder(value->{query=value;page=0;refreshSearch=true;});
  control("Go",L+COL-38,90,38,22,false,()->{refreshSearch=false;rebuildWidgets();setFocused(search);});
  if(all){List<String> categories=List.of("All","Crafting","Forge","Pets","Other");int x=L;for(String c:categories){control(c,x,117,60,18,false,()->{category=c;page=0;rebuildWidgets();}).selected(category.equals(c));x+=62;}}
  var physical=app.physicalStock();
  List<Catalog.Item> items=(all?catalogItems():app.book.goals.stream().filter(g->query.isBlank()||Catalog.normal(app.catalog.item(g.id).name).contains(Catalog.normal(query))).map(g->app.catalog.item(g.id)).toList()).stream()
    .filter(item->CompletionFilter.include(filter,item.id,physical,app.book.completed)).toList();
  int perPage=6;totalPages=Math.max(1,(items.size()+perPage-1)/perPage);page=Math.clamp(page,0,totalPages-1);
  for(int i=page*perPage;i<Math.min(items.size(),(page+1)*perPage);i++){
   var item=items.get(i);int y=145+(i-page*perPage)*39;Book.Goal saved=app.book.find(item.id);Book.Goal display=saved==null?new Book.Goal(item.id):saved;
   cards.add(new int[]{L,y,COL,37,selected.equals(item.id)?1:0});icons.add(new ItemIcon(item.id,L+5,y+5));
   control(item.name,L+29,y+1,COL-54,15,true,()->{selected=item.id;emptyDetail=false;materialPage=0;rebuildWidgets();});
   if(all){control(">",L+COL-22,y+4,20,29,false,()->{selected=item.id;emptyDetail=false;materialPage=0;rebuildWidgets();});entries.add(new Entry(display,L+32,y+20,COL-88));}
   else {entries.add(new Entry(display,L+32,y+20,108));
    control(saved.visible?"Hide":"Show",L+170,y+17,40,17,false,()->{saved.visible=!saved.visible;app.persist();rebuildWidgets();});
    control("^",L+214,y+17,20,17,false,()->{app.book.move(saved,-1);app.persist();rebuildWidgets();});
    control("v",L+237,y+17,20,17,false,()->{app.book.move(saved,1);app.persist();rebuildWidgets();});
    control("Remove",L+263,y+17,43,17,false,()->{app.removeGoal(saved);rebuildWidgets();});
   }
  }
  if(items.isEmpty())text("No matching recipes",L+8,160,COL-16,Theme.muted());
  button("<",L+5,394,28,()->listPage(-1)).active=page>0;button(">",L+COL-33,394,28,()->listPage(1)).active=page+1<totalPages;
  text("Page "+(page+1)+" / "+totalPages,L+107,401,160,Theme.muted());
  if(all&&selected.isEmpty()&&!emptyDetail){
   Book.Goal active=app.book.active();String preferred=active==null?"ENCHANTED_DIAMOND_BLOCK":active.id;
   if(items.stream().anyMatch(i->i.id.equals(preferred)))selected=preferred;
   else if(!items.isEmpty())selected=items.getFirst().id;
  }
  if(all||!selected.isEmpty())detail();else settings();
 }
 private List<Catalog.Item> catalogItems(){return app.catalog.items.values().stream().filter(i->!i.id.endsWith("_NPC")&&!i.id.endsWith("_BOSS"))
  .filter(i->query.isBlank()||Catalog.normal(i.name).contains(Catalog.normal(query)))
  .filter(i->switch(category){case "Crafting"->i.routes.stream().anyMatch(r->"crafting".equals(r.type));case "Forge"->i.routes.stream().anyMatch(r->"forge".equals(r.type));case "Pets"->i.name.endsWith(" Pet");case "Other"->i.routes.isEmpty();default->true;})
  .sorted(Comparator.comparing(i->i.name)).toList();}
 private void settings(){
  button("Settings",R,84,COL,()->app.open(new SettingsScreen(app,this)));
  button("Craft Diary",R,122,COL,()->app.open(new ToolsScreen(app,"Craft Diary")));
  button("Calculator",R,160,COL,()->app.open(new ToolsScreen(app,"Calculator")));
  button("Forge",R,198,COL,()->app.open(new ForgeScreen(app)));
 }
 private void detail(){
  if(selected.isEmpty()){text("Select a recipe",R,45,COL,Theme.muted());return;}
  Book.Goal goal=app.goal(selected);Catalog.Item item=app.catalog.item(selected);icons.add(new ItemIcon(selected,R,36));text(item.name,R+24,35,COL-24,Theme.ink());
  button("-",R,63,24,()->quantity(goal,Math.max(1,goal.count-1)));button("+",R+28,63,24,()->quantity(goal,Math.min(1000000,goal.count+1)));
  EditBox count=addRenderableWidget(new EditBox(font,viewport.x(R+60),viewport.y(65),viewport.size(66),Math.max(14,viewport.size(20)),Component.literal("Quantity")));count.setMaxLength(7);count.setValue(Long.toString(goal.count));count.setTextColor(Theme.ink());count.setBordered(false);
  button("Set",R+132,63,40,()->{try{quantity(goal,Long.parseLong(count.getValue()));}catch(NumberFormatException e){app.say("Enter a whole quantity from 1 to 1000000.");}});
  button("Reset x1",R+180,63,72,()->quantity(goal,1));
  button("Back",R+259,63,47,()->{selected="";emptyDetail=true;materialPage=0;rebuildWidgets();});
  RecipeProgress engine=new RecipeProgress(app.catalog);RecipeProgress.Node tree=app.tree(goal);
  text(ArmorSets.isSet(goal)?"Required Materials | "+ArmorSets.readyPieces(tree)+"/4 pieces ready":"Required Materials",R,94,COL,Theme.ink());
  List<RecipeProgress.Node> materials;if(treeMode){materials=new ArrayList<>();for(var child:tree.children)treeRows(child,goal,materials);}else materials=engine.displayed(tree,goal);int pages=Math.max(1,(materials.size()+2)/3);materialPage=Math.clamp(materialPage,0,pages-1);
  for(int i=materialPage*3;i<Math.min(materials.size(),materialPage*3+3);i++){
   var node=materials.get(i);int y=112+(i-materialPage*3)*44;icons.add(new ItemIcon(node.id,R,y));
   control((node.children.isEmpty()?"":goal.expanded.contains(node.id)?"- ":"+ ")+app.trim(app.catalog.item(node.id).name,COL-39),R+24,y,COL-24,17,true,()->{if(!goal.expanded.remove(node.id))goal.expanded.add(node.id);treeMode=true;app.persist();flip();});
   materialViews.add(new MaterialView(i,y));
  }
  button("<",R,247,24,()->materialPage(-1,pages)).active=materialPage>0;button(">",R+28,247,24,()->materialPage(1,pages)).active=materialPage+1<pages;
  button(treeMode?"Materials":"Full recipe",R+60,247,104,()->{treeMode=!treeMode;materialPage=0;flip();});
  button("Collapse details",R+172,247,134,()->{goal.expanded.clear();app.persist();flip();});
  selector("Material Display",new ArrayList<>(engine.materialOptions(tree)),goal.materialDisplay,R,274,value->{goal.materialDisplay=value;app.persist();rebuildWidgets();});
  selector("Gemstone Display",new ArrayList<>(engine.gemOptions(tree)),goal.gemDisplay,R,306,value->{goal.gemDisplay=value;app.persist();rebuildWidgets();});
  if(ArmorSets.isSet(goal)){
   for(int i=0;i<4;i++){String piece=goal.armorPieces.get(i);button(app.catalog.item(piece).name,R+(i%2)*155,340+(i/2)*24,151,()->{selected=piece;treeMode=true;materialPage=0;rebuildWidgets();});}
   button("Restore individual HUDs",R,375,COL,()->{ArmorSets.split(app.book,goal);app.persist();rebuildWidgets();});
  }else if(!item.routes.isEmpty()){
   Catalog.Route route=item.routes.get(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size()));
   if(!route.slots.isEmpty())for(int i=0;i<route.slots.size()&&i<9;i++){int sx=R+i%3*19,sy=340+i/3*18;gridCells.add(new int[]{sx,sy});var slot=route.slots.get(i);if(slot!=null)icons.add(new ItemIcon(slot.id,sx,sy,slot.count));}
   else {text(route.type.toUpperCase(Locale.ROOT),R,345,100,Theme.muted());text("Output: "+route.count,R,360,100,Theme.muted());}
   text(">",R+66,365,16,Theme.ink());icons.add(new ItemIcon(item.id,R+84,359,route.count));
   if(item.routes.size()>1)button("Recipe "+(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size())+1),R+127,354,90,()->{goal.choices.merge(item.id,1,Integer::sum);app.persist();flip();});
  }else text("Acquired directly",R,351,COL,Theme.muted());
  if(!ArmorSets.isSet(goal)&&ArmorSets.detect(app.catalog,goal.id).size()==4)button("Track full armor set",R+127,375,179,()->{var set=ArmorSets.combine(app.catalog,app.book,goal.id);if(set!=null){selected=set.id;materialPage=0;app.persist();rebuildWidgets();}});
  if(app.book.find(goal.id)==null)button("Save as Goal",R+60,395,186,()->{app.save(goal);rebuildWidgets();});
  else {
   button(goal.finished?"Finished":"Finish",R,395,94,()->{app.finishGoal(goal);rebuildWidgets();}).active=!goal.finished;
   button("Reset recipe",R+102,395,108,()->{app.restartGoal(goal);rebuildWidgets();});
   button("Save",R+218,395,88,()->{app.save(goal);rebuildWidgets();});
  }
 }
 private void treeRows(RecipeProgress.Node node,Book.Goal goal,List<RecipeProgress.Node> rows){var remaining=RecipeProgress.remaining(node);if(remaining.total==0)return;rows.add(remaining);if(goal.expanded.contains(node.id))for(var child:node.children)treeRows(child,goal,rows);}
 private void selector(String title,List<String> options,String current,int x,int y,java.util.function.Consumer<String> action){if(options.isEmpty())return;text(title,x,y,COL,Theme.muted());int w=COL/options.size();for(int i=0;i<options.size();i++){String option=options.get(i);button((option.equals(current)?"* ":"")+option,x+i*w,y+10,w-3,()->action.accept(option));}}
 private void quantity(Book.Goal goal,long n){if(n<1||n>1000000){app.say("Enter a whole quantity from 1 to 1000000.");return;}goal.count=n;app.persist();UiSounds.count(app);rebuildWidgets();}
 private static String number(double n){return n==Math.rint(n)?String.format(Locale.US,"%,.0f",n):String.format(Locale.US,"%,.3f",n);}
 @Override public void tick(){
  super.tick();ticks++;
  if(refreshSearch){int cursor=search.getCursorPosition();boolean focused=search.isFocused();refreshSearch=false;rebuildWidgets();if(focused){setFocused(search);search.setCursorPosition(Math.min(cursor,search.getValue().length()));}}
 }
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
  if(super.mouseClicked(e,twice))return true;
  double x=viewport.localX(e.x()),y=viewport.localY(e.y());
  for(var icon:icons)if(x>=icon.x&&x<icon.x+18&&y>=icon.y&&y<icon.y+18){app.gameRecipe(icon.id);return true;}return false;
 }
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
  if(x>=viewport.x(L)&&x<=viewport.x(L+COL)&&y>=viewport.y(140)&&y<=viewport.y(385)){listPage(sy<0?1:-1);return true;}
  return super.mouseScrolled(x,y,sx,sy);
 }
 private void heading(GuiGraphicsExtractor g,String value,int x,int y,float size){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(size,size);g.text(font,value,0,0,Theme.ink(),false);g.pose().popMatrix();}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  int lx=(int)viewport.localX(mx),ly=(int)viewport.localY(my);
  g.pose().pushMatrix();g.pose().translate(left,top);g.pose().scale(scale,scale);
  Theme.background(g,font,W,H,"");heading(g,all?"ALL RECIPES":"RECIPE BOOK",L+5,29,1.8f);
  if(!all&&selected.isEmpty())heading(g,"YOUR BOOK",R,31,1.5f);
  g.fill(L,89,L+COL-41,112,0x557e6340);g.outline(L,89,COL-41,23,0xff9a7950);

  for(var card:cards){g.fill(card[0],card[1],card[0]+card[2],card[1]+card[3],card[4]==1?0x557c4e20:0x15774c24);g.outline(card[0],card[1],card[2],card[3],card[4]==1?0xfff2dfb2:0x487c5b32);g.fill(card[0]+3,card[1]+3,card[0]+25,card[1]+33,0x33795a30);}
  for(var t:texts)g.text(font,app.trim(t.text,t.width),t.x,t.y,t.color,false);
  if(!selected.isEmpty()){
   var goal=app.goal(selected);var tree=app.tree(goal);g.fill(R+24,50,R+COL,56,Theme.well());g.fill(R+24,50,R+24+(int)((COL-24)*tree.fraction()),56,0xff4b9a24);
   List<RecipeProgress.Node> nodes;if(treeMode){nodes=new ArrayList<>();for(var child:tree.children)treeRows(child,goal,nodes);}else nodes=new RecipeProgress(app.catalog).displayed(tree,goal);
   for(var view:materialViews)if(view.index<nodes.size()){
    var node=nodes.get(view.index);int y=view.y;
    g.fill(R,y,R+20,y+30,0x33795a30);
    g.text(font,app.trim((node.equivalent>node.physical?"Eq. ":"Known ")+number(node.equivalent)+" / "+Catalog.qty(node.total),COL-126),R+24,y+20,Theme.muted(),false);
    g.fill(R+COL-98,y+21,R+COL-31,y+27,Theme.well());if(node.inForge>0&&node.total>0)g.fill(R+COL-98,y+21,R+COL-98+(int)(67*node.row().forgeFraction()),y+27,0xffd69d3a);g.fill(R+COL-98,y+21,R+COL-98+(int)(67*node.fraction()),y+27,0xff4b9a24);g.text(font,node.row().percent(),R+COL-28,y+20,Theme.good(),false);
    String parent=node.parent.isEmpty()?selected:node.parent;
    String detail=node.row().needed()+(node.reserved>0?" | "+(node.forge>0?"Forge: ":"Planned: ")+number(node.reserved):"");
    g.text(font,app.trim(detail,COL-24),R+24,y+32,Theme.muted(),false);
   }
  }
  for(var cell:gridCells)g.outline(cell[0]-1,cell[1]-1,18,18,0xffb59c70);
  for(var icon:icons){g.item(Icons.stack(app,ArmorSets.isSet(app.book.find(icon.id))?ArmorSets.iconId(app.book.find(icon.id)):icon.id),icon.x,icon.y);if(icon.count>1)g.text(font,Long.toString(icon.count),icon.x+8,icon.y+9,0xffffffff,true);}
  for(var entry:entries){var root=app.tree(entry.goal);double fraction=root.fraction();g.fill(entry.x,entry.y,entry.x+entry.width,entry.y+7,Theme.well());g.outline(entry.x,entry.y,entry.width,7,0xff74674f);g.fill(entry.x+1,entry.y+1,entry.x+1+(int)((entry.width-2)*fraction),entry.y+6,0xff4b9a24);g.text(font,root.row().percent(),entry.x+entry.width+4,entry.y,Theme.good(),false);}
  g.pose().popMatrix();
  // Stable position with a travelling gold highlight; no navigation downtime.
  String brand="RECIPE HUNTER";float brandScale=Math.max(.65f,scale*1.4f);
  int brandY=Math.max(2,top-Math.round(11*brandScale));
  g.pose().pushMatrix();g.pose().translate((width-font.width(brand)*brandScale)/2,brandY);g.pose().scale(brandScale,brandScale);
  double phase=(ticks+dt)/12.0;int bx=0;
  for(int i=0;i<brand.length();i++){String letter=brand.substring(i,i+1);double glow=(Math.sin(phase-i*.55)+1)/2;int color=0xff000000|((int)(205+50*glow)<<16)|((int)(146+95*glow)<<8)|(int)(57+125*glow);g.text(font,letter,bx,0,color,true);bx+=font.width(letter);}
  g.pose().popMatrix();
  super.extractRenderState(g,mx,my,dt);
  String hovered="";
  for(var icon:icons)if(lx>=icon.x&&lx<icon.x+18&&ly>=icon.y&&ly<icon.y+18){RecipePreview.render(app,g,icon.id,selected.isEmpty()?null:app.goal(selected),mx,my);hovered=icon.id;}
  if(!selected.isEmpty())for(var icon:icons)if(icon.x==R&&icon.y>=112&&icon.y<=200&&lx>=R&&lx<R+COL&&ly>=icon.y&&ly<icon.y+40)hovered=icon.id;
  upgradeHover.render(app,g,hovered,selected.isEmpty()?null:app.goal(selected),mx,my);
 }
}
