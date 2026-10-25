package dev.recipehunter;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;


public final class HunterScreen extends Screen {
    public enum Mode { HOME,SEARCH,BOOK,DETAIL,SETTINGS }
    private final RecipeHunter app;private final Mode mode;private final String query;
    private String focus;private int tab=0,page=0,refreshTicks=0;private Book.Goal goal;private Planner.Plan plan;
    private EditBox search,owned,rate;private final List<Icon> icons=new ArrayList<>();private final List<Label> labels=new ArrayList<>();
    private final List<Hover> hovers=new ArrayList<>();
    private record CountRow(String id,long total,Button button,int width){}
    private final List<CountRow> counts=new ArrayList<>();
    private record Hover(int x,int y,int w,String text,String id){}
    private record Icon(String id,int x,int y,long count){}private record Label(String text,int x,int y,int color){}
    private float uiScale=1;private int actualWidth,actualHeight;
    private int bodyY=122,footerY,rowsPerPage;private List<String> notes=List.of();
    public HunterScreen(RecipeHunter app,Mode mode,String q){super(Component.literal("Recipe Hunter"));this.app=app;this.mode=mode;query=q;focus=q;}
    @Override public void onClose(){app.closeBook();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){super.tick();if(mode!=Mode.DETAIL)return;
        if(++refreshTicks%5==0&&!counts.isEmpty()){
            var stock=app.stock(goal);var forge=CraftDiary.pending(app.menus.forge);
            Map<String,MaterialProgress.Row> materialRows=new HashMap<>();
            for(var material:app.progress(goal))materialRows.put(material.id(),material);
            for(var row:counts){
                var progress=materialRows.getOrDefault(row.id,MaterialProgress.single(row.id,row.total,stock,app.catalog,goal.choices));
                String text=app.catalog.item(row.id).name+" "+progress.have()+"/"+row.total+" ("+progress.percent()+")";
                long n=forge.getOrDefault(row.id,0L);if(n>0)text+=" | Forge: "+n;
                row.button.setMessage(Component.literal(app.trim(text,row.width-40)).withStyle(style->style.withColor(progress.done()?0x85d65c:app.catalog.item(row.id).color)));
                row.button.setTooltip(Tooltip.create(Component.literal(progress.count())));
            }
        }
    }
    private Button button(String text,int x,int y,int w,Runnable action){return addRenderableWidget(new GlowButton(text,x,y,w,action));}
    private void label(String text,int x,int y,int color){labels.add(new Label(text,x,y,color));}
    @Override protected void init(){
        actualWidth=app.mc.getWindow().getGuiScaledWidth();actualHeight=app.mc.getWindow().getGuiScaledHeight();
        uiScale=(float)Math.min(app.book.guiScale,Math.min(actualWidth/520.0,actualHeight/360.0));
        width=(int)(actualWidth/uiScale);height=(int)(actualHeight/uiScale);
        icons.clear();labels.clear();hovers.clear();counts.clear();footerY=height-58;rowsPerPage=Math.max(1,(footerY-bodyY-18)/23);
        int w=Math.max(140,width-24);search=addRenderableWidget(new EditBox(font,12,28,w-80,20,Component.literal("Search item")));search.setMaxLength(100);search.setValue(mode==Mode.SEARCH?query:"");search.setHint(Component.literal("Search item or pet rarity..."));
        button("Search",width-86,28,74,()->app.command(search.getValue()));
        button(mode==Mode.BOOK?"Home":"Book",12,53,58,mode==Mode.BOOK?app::openHome:app::openBook);
        button("GUI",width-66,53,54,()->app.open(new HudEditor(app)));
        if(mode!=Mode.DETAIL){button("Diary",207,53,60,()->app.open(new ToolsScreen(app,"Craft Diary")));button("Calc",272,53,55,()->app.open(new ToolsScreen(app,"Calculator")));button("AI",332,53,45,()->app.open(new ToolsScreen(app,"AI")));}
        if(mode!=Mode.DETAIL)button("Assistant: "+(app.book.assistant?"ON":"OFF"),76,53,125,()->{app.book.assistant=!app.book.assistant;app.persist();refresh();});
        if(mode==Mode.HOME)home();else if(mode==Mode.SETTINGS)settings();else if(mode==Mode.BOOK)book();else if(mode==Mode.SEARCH)results();else detail();
    }
    private void refresh(){rebuildWidgets();}
    private void settings(){
        label("Appearance & assistance",12,90,0xffefc77d);
        button("GUI smaller",12,116,125,()->app.resizeGui(-0.1));button("GUI larger",144,116,125,()->app.resizeGui(0.1));
        label(String.format(java.util.Locale.US,"GUI %.0f%% (fits screen)",app.book.guiScale*100),284,122,0xffd1dce5);
        button("HUD smaller",12,144,125,()->{app.book.hudScale=Math.max(.65,app.book.hudScale-.1);app.persist();refresh();});
        button("HUD larger",144,144,125,()->{app.book.hudScale=Math.min(1.6,app.book.hudScale+.1);app.persist();refresh();});
        button("Celebration: "+(app.book.celebrations?"ON":"OFF"),12,180,190,()->{app.book.celebrations=!app.book.celebrations;app.persist();refresh();});
        button("Music: "+(app.book.celebrationSound?"ON":"OFF"),210,180,150,()->{app.book.celebrationSound=!app.book.celebrationSound;app.persist();refresh();});
        button("Forget storage scans",12,210,190,()->{app.ownership.storage.clear();app.menus.clear();refresh();});button("Pet scan instructions",210,210,190,()->app.scanPets(""));
        label("Chest snapshots: "+app.ownership.storage.size()+" | Pets pages: "+app.ownership.petPageCount(),12,244,0xffb9d5c9);
        label("Open each island chest to scan it. Reopen after changes.",12,264,0xffb9d5c9);
        button("UI sounds: "+(app.book.uiSounds?"ON":"OFF"),12,307,190,()->{app.book.uiSounds=!app.book.uiSounds;app.persist();refresh();});
        label("Esc closes this screen. Scans reset on reconnect/profile switch.",12,282,0xffb9d5c9);
    }
    private Button checkedRow(String id,String suffix,int y,int x,int w){
        boolean checked=goal.checked.contains(id);
        button(checked?"[x]":"[ ]",x,y,28,()->{app.toggleCheck(goal,id);refresh();});
        return row(id,suffix,y,x+32,w-32,()->inspect(id));
    }
    private void results(){
        label("Choose a result — pet rarities are separate goals",12,88,0xffa9bac8);
        List<Catalog.Item> items=app.catalog.search(query);int start=page*rowsPerPage;
        for(int i=start;i<Math.min(items.size(),start+rowsPerPage);i++){
            Catalog.Item item=items.get(i);row(item.id,"",bodyY+(i-start)*23,12,width-24,()->app.select(item.id,true));
        }
        pages(items.size());
    }
    private void home(){
        label("Locate any recipe",12,88,0xffe9e4ff);
        label("Active queue — first visible recipe is shown on your HUD",12,106,0xffa9bac8);
        queue();
    }
    private void book(){
        label("Recipe book | Completed: "+app.book.completed.size()+" | Queue: "+app.book.goals.size(),12,88,0xffa9bac8);
        button("Queue",12,99,80,()->{tab=0;page=0;refresh();});
        button("Completed",98,99,95,()->{tab=1;page=0;refresh();});
        if(tab==0){queue();return;}
        List<String> done=new ArrayList<>(app.book.completed);Collections.sort(done);int start=page*rowsPerPage;
        for(int i=start;i<Math.min(done.size(),start+rowsPerPage);i++){String id=done.get(i);row(id,"",bodyY+(i-start)*23,12,width-24,()->app.select(id,true));}
        pages(done.size());
    }
    private void queue(){
        int start=page*rowsPerPage;
        for(int i=start;i<Math.min(app.book.goals.size(),start+rowsPerPage);i++){
            Book.Goal g=app.book.goals.get(i);int y=bodyY+(i-start)*23;
            row(g.id,(g==app.book.active()?"> ":"")+"x"+g.count,y,12,width-248,()->app.select(g.id,true));
            button("^",width-226,y,24,()->{app.book.move(g,-1);app.persist();refresh();});
            button("v",width-198,y,24,()->{app.book.move(g,1);app.persist();refresh();});
            button(g.visible?"Hide":"Show",width-168,y,64,()->{g.visible=!g.visible;app.persist();refresh();});
            button("Remove",width-98,y,86,()->{app.removeGoal(g);page=0;refresh();});
        }
        if(app.book.goals.isEmpty())label("Search for a recipe above, then save it to your queue.",12,bodyY+5,0xffd1dce5);
        pages(app.book.goals.size());
    }
    private void detail(){
        goal=app.goal(query);plan=app.plan(goal);Catalog.Item item=app.catalog.item(focus);
        button(app.book.find(goal.id)==null?"Save":"Saved",75,53,62,()->{app.save(goal);refresh();});
        button("Finish",142,53,82,()->{app.completed(goal.id,goal.count);app.openBook();});
        if(!focus.equals(query))button("Main goal",229,53,83,()->{focus=query;page=0;tab=0;refresh();});
        else button("Open in recipe",229,53,108,()->app.gameRecipe(focus));
        button("Assistant: "+(app.book.assistant?"ON":"OFF"),343,53,125,()->{app.book.assistant=!app.book.assistant;app.persist();refresh();});
        String[] tabs={"Recipe","Materials","All steps","Used for","Storage","Grind"};
        int tw=Math.min(88,(width-24)/tabs.length);
        for(int n=0;n<tabs.length;n++){final int ix=n;button((tab==n?"> ":"")+tabs[n],12+n*tw,78,tw-2,()->{tab=ix;page=0;UiSounds.page(app);refresh();});}
        label(app.trim(item.name,width-300),12,105,0xff000000|item.color);

        button("-",width-202,101,24,()->{goal.count=Math.max(1,goal.count-1);UiSounds.count(app);app.persist();refresh();});
        button("+",width-174,101,24,()->{goal.count=Math.min(1000000,goal.count+1);UiSounds.count(app);app.persist();refresh();});
        EditBox quantity=addRenderableWidget(new EditBox(font,width-146,101,64,20,Component.literal("Target quantity")));
        quantity.setMaxLength(7);quantity.setValue(Long.toString(goal.count));
        button("Reset x1",width-276,101,70,()->{goal.count=1;UiSounds.count(app);app.persist();refresh();});
        button("Set",width-72,101,60,()->{try{long n=Long.parseLong(quantity.getValue());if(n<1||n>1000000)throw new NumberFormatException();goal.count=n;UiSounds.count(app);app.hudPlans.clear();app.persist();refresh();}catch(NumberFormatException ex){app.say("Enter a whole quantity from 1 to 1000000.");}});
        if(tab==0)recipe(item);else if(tab==1)materials();else if(tab==2)steps();else if(tab==3)uses();else if(tab==5)grind();else sources(item);
        if(tab!=4&&tab!=5)stockControls(item);
    }
    private Catalog.Route selected(Catalog.Item item){if(item.routes.isEmpty())return null;return item.routes.get(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size()));}
    private void recipe(Catalog.Item item){
        Catalog.Route r=selected(item);
        if(r==null){
            List<String> lines=new ArrayList<>();lines.add("No crafting recipe");
            showNotes(lines);return;
        }
        int y=bodyY;
        label(r.type+"  | output: "+r.count+"  | route "+(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size())+1)+"/"+item.routes.size(),12,y,0xffa9bac8);y+=14;
        if(item.routes.size()>1)button("Next route",12,y,88,()->{goal.choices.merge(item.id,1,Integer::sum);app.persist();refresh();});
        if(!r.slots.isEmpty()){
            int gridY=y+(item.routes.size()>1?24:0);
            if(gridY+81<height-45) for(int n=0;n<r.slots.size();n++){Catalog.Ingredient in=r.slots.get(n);if(in!=null)icons.add(new Icon(in.id,16+n%3*27,gridY+n/3*27,in.count));}
            else label("All inputs ->",12,gridY,0xffa9bac8);
        }else {
            label(r.type.equals("katgrade")?"Upgrade at Kat":"Forge",12,y+(item.routes.size()>1?24:0),0xffd1dce5);
            if(r.seconds>0)label(String.format(Locale.US,"%.1f h base wait",r.seconds/3600.0),12,y+38,0xffa9bac8);
        }
        int rx=112,rw=width-124;int available=Math.max(1,(footerY-y-19)/23);int start=page*available;
        Map<String,Long> stock=app.stock(goal);
        for(int n=start;n<Math.min(r.inputs.size(),start+available);n++){
            Catalog.Ingredient in=r.inputs.get(n);long runs=(focus.equals(goal.id)?(goal.count-1)/r.count+1:1);long total=Math.multiplyExact(in.count,runs);String suffix=Math.min(total,stock.getOrDefault(in.id,0L))+"/"+total;
            Button b=checkedRow(in.id,suffix,y+(n-start)*23,rx,rw);counts.add(new CountRow(in.id,total,b,rw-32));
        }
        pageButtons(r.inputs.size(),available);
    }
    private void grind(){
        List<String> lines=new ArrayList<>();lines.add("EASIEST -> HARDEST");
        for(var e:app.estimates.rank(app,goal,true))lines.add(e.label(app.catalog));
        lines.add("LOWEST TIME -> HIGHEST TIME");for(var e:app.estimates.rank(app,goal,false))lines.add(e.label(app.catalog));
        lines.add("Observed stats: "+app.estimates.stats);
        for(var e:app.menus.forge)lines.add("Forge: "+e.count+"x "+app.catalog.item(e.id).name+" | "+CraftDiary.remaining(e,System.currentTimeMillis()));
        showNotes(lines);button(app.estimates.recording?"Stop sample":"Record sample",12,height-30,120,()->{if(app.estimates.recording)app.estimates.recording=false;else app.estimates.start();refresh();});button("Refresh",138,height-30,70,this::refresh);button("AI chat",214,height-30,100,()->app.open(new ToolsScreen(app,"AI")));
    }
    private void materials(){
        List<MaterialProgress.Row> rows=app.progress(goal);int start=page*rowsPerPage;
        for(int i=start;i<Math.min(rows.size(),start+rowsPerPage);i++){var r=rows.get(i);Button b=checkedRow(r.id(),r.count()+(r.forge()>0?" | Forge: "+r.forge():r.pending()>0?" | Planned: "+r.pending():""),bodyY+(i-start)*23,12,width-24);counts.add(new CountRow(r.id(),r.total(),b,width-56));}
        if(rows.isEmpty())label("Ready",12,bodyY,0xff51842b);pages(rows.size());
    }
    private void steps(){
        List<Map.Entry<String,Long>> entries=new ArrayList<>(plan.demand.entrySet());int start=page*rowsPerPage;
        for(int i=start;i<Math.min(entries.size(),start+rowsPerPage);i++){
            var e=entries.get(i);long needed=plan.missing.getOrDefault(e.getKey(),0L);if(e.getValue()==0)continue;
            checkedRow(e.getKey(),Catalog.amount(e.getKey(),needed)+" left / "+e.getValue()+" total",bodyY+(i-start)*23,12,width-24);
        }pages(entries.size());
    }
    private void uses(){List<String> uses=app.catalog.uses.getOrDefault(focus,List.of());int start=page*rowsPerPage;
        for(int i=start;i<Math.min(uses.size(),start+rowsPerPage);i++){String id=uses.get(i);row(id,"",bodyY+(i-start)*23,12,width-24,()->app.select(id,true));}
        if(uses.isEmpty())label("No uses indexed in this snapshot.",12,bodyY,0xffa9bac8);pages(uses.size());}
    private void sources(Catalog.Item item){
        List<String> lines=new ArrayList<>();lines.add(item.name);
        lines.add("Inventory: "+app.ownership.inventory(app.mc).getOrDefault(item.id,0L));
        lines.add("Sacks: "+app.menus.sacks.getOrDefault(item.id,0L));
        lines.add("Ender Chest: "+app.menus.ender.totals().getOrDefault(item.id,0L));
        lines.add("Forge: "+CraftDiary.pending(app.menus.forge).getOrDefault(item.id,0L));
        lines.add("Open storage to refresh");showNotes(lines);
    }
    private void showNotes(List<String> paragraphs){
        List<String> lines=new ArrayList<>();for(String s:paragraphs){String remain=s;while(!remain.isEmpty()){String part=font.plainSubstrByWidth(remain,width-32);if(part.isEmpty())break;lines.add(part);remain=remain.substring(part.length());}lines.add("");}
        int capacity=Math.max(1,(footerY-bodyY-20)/11),start=page*capacity;
        for(int i=start;i<Math.min(lines.size(),start+capacity);i++)label(lines.get(i),12,bodyY+(i-start)*11,0xffd1dce5);
        pageButtons(lines.size(),capacity);
    }
    private void stockControls(Catalog.Item item){
        int y=height-35;label("Have:",12,y-10,0xffa9bac8);label("Items/hour:",104,y-10,0xffa9bac8);
        owned=addRenderableWidget(new EditBox(font,12,y,85,20,Component.literal("Known total owned for "+item.name)));owned.setMaxLength(14);owned.setValue(Long.toString(app.stock(goal).getOrDefault(focus,0L)));
        rate=addRenderableWidget(new EditBox(font,104,y,85,20,Component.literal("Measured items per hour")));rate.setMaxLength(14);rate.setValue(Double.toString(goal.rates.getOrDefault(focus,0.0)));
        button("Apply",196,y,62,()->{try{long n=Long.parseLong(owned.getValue().replace(",",""));double r=Double.parseDouble(rate.getValue().replace(",","."));if(n<0||n>1_000_000_000_000L||!Double.isFinite(r)||r<0)throw new NumberFormatException();goal.owned.put(focus,n);goal.rates.put(focus,r);app.persist();refresh();}catch(NumberFormatException e){app.say("Enter non-negative numbers. Have is a total, not an extra amount.");}});
        button("Clear",264,y,55,()->{goal.owned.remove(focus);goal.rates.remove(focus);app.persist();refresh();});
        button(goal.expanded.contains(focus)?"Collapse":"Expand inputs",325,y,105,()->{if(!goal.expanded.remove(focus))goal.expanded.add(focus);app.persist();tab=1;refresh();});
        button(goal.compact?"Compact":"Full",435,y,70,()->{goal.compact=!goal.compact;app.persist();refresh();});
    }
    private void inspect(String id){focus=id;tab=0;page=0;refresh();}
    private Button row(String id,String suffix,int y,int x,int w,Runnable action){
        Catalog.Item item=app.catalog.item(id);String name=(suffix.isEmpty()?"":suffix+"  ")+item.name;
        Button b=button(app.trim(name,Math.max(15,w-28)),x+22,y,Math.max(20,w-22),action);
        b.setMessage(Component.literal(app.book.completed.contains(id)?"✓ ":"").withStyle(style->style.withColor(0x55ff55)).append(Component.literal(app.trim(name,Math.max(15,w-40))).withStyle(style->style.withColor(item.color))));
        String tip=item.name+(suffix.isEmpty()?"":"\n"+suffix)+"\n"+(item.requirement==null?"":item.requirement);
        if(!item.sources.isEmpty())tip+="\n"+item.sources.getFirst();tip+="\nClick to inspect this ingredient.";
        hovers.add(new Hover(x+22,y,w-22,tip,id));icons.add(new Icon(id,x+2,y+2,0));return b;
    }
    private void pages(int size){pageButtons(size,rowsPerPage);}
    private void pageButtons(int size,int capacity){int total=Math.max(1,(size+capacity-1)/capacity);page=Math.min(page,total-1);int y=footerY-17;
        button("<",width-108,y,24,()->{page=Math.max(0,page-1);UiSounds.page(app);refresh();});button(">",width-36,y,24,()->{page=Math.min(total-1,page+1);UiSounds.page(app);refresh();});label((page+1)+"/"+total,width-80,y+6,0xffa9bac8);}
    private ItemStack icon(String id){return Icons.stack(app,id);}
    private MouseButtonEvent scaled(MouseButtonEvent e){return new MouseButtonEvent(e.x()/uiScale,e.y()/uiScale,e.buttonInfo());}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        MouseButtonEvent p=scaled(e);
        for(Icon i:icons)if(p.x()>=i.x&&p.x()<i.x+20&&p.y()>=i.y&&p.y()<i.y+20){app.gameRecipe(i.id);return true;}
        return super.mouseClicked(p,twice);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){return super.mouseReleased(scaled(e));}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(scaled(e),dx/uiScale,dy/uiScale);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){return super.mouseScrolled(x/uiScale,y/uiScale,sx,sy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        int physicalMx=mx,physicalMy=my;mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        g.pose().pushMatrix();g.pose().scale(uiScale,uiScale);
        Theme.background(g,font,width,height,"RECIPE HUNTER");
        for(Label l:labels)g.text(font,l.text,l.x,l.y,Theme.ink(l.color),false);
        super.extractRenderState(g,mx,my,dt);
        for(Icon i:icons){g.fill(i.x-1,i.y-1,i.x+19,i.y+19,0xffc3b38c);g.item(icon(i.id),i.x,i.y);if(i.count>0)g.text(font,Long.toString(i.count),i.x+10,i.y+12,0xffffffff,true);
            if(mx>=i.x&&mx<i.x+22&&my>=i.y&&my<i.y+22){ /* Icon preview is rendered after the scaled UI below. */ }
        }
        g.pose().popMatrix();
        for(Icon i:icons)if(mx>=i.x&&mx<i.x+20&&my>=i.y&&my<i.y+20)RecipePreview.render(app,g,i.id,goal,physicalMx,physicalMy);
        for(Hover h:hovers)if(mx>=h.x&&mx<h.x+h.w&&my>=h.y&&my<h.y+20)RecipePreview.render(app,g,h.id,goal,physicalMx,physicalMy);
    }
}
