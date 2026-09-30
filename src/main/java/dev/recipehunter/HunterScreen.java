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
    public enum Mode { SEARCH,BOOK,DETAIL,SETTINGS }
    private final RecipeHunter app;private final Mode mode;private final String query;
    private String focus;private int tab=0,page=0;private Book.Goal goal;private Planner.Plan plan;
    private EditBox search,owned,rate;private final List<Icon> icons=new ArrayList<>();private final List<Label> labels=new ArrayList<>();
    private final List<Hover> hovers=new ArrayList<>();
    private record Hover(int x,int y,int w,String text,String id){}
    private record Icon(String id,int x,int y,long count){}private record Label(String text,int x,int y,int color){}
    private float uiScale=1;private int actualWidth,actualHeight;
    private int bodyY=122,footerY,rowsPerPage;private List<String> notes=List.of();
    public HunterScreen(RecipeHunter app,Mode mode,String q){super(Component.literal("Recipe Hunter"));this.app=app;this.mode=mode;query=q;focus=q;}
    @Override public boolean isPauseScreen(){return false;}
    private Button button(String text,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());}
    private void label(String text,int x,int y,int color){labels.add(new Label(text,x,y,color));}
    @Override protected void init(){
        actualWidth=app.mc.getWindow().getGuiScaledWidth();actualHeight=app.mc.getWindow().getGuiScaledHeight();
        uiScale=(float)Math.min(app.book.guiScale,Math.min(actualWidth/520.0,actualHeight/360.0));
        width=(int)(actualWidth/uiScale);height=(int)(actualHeight/uiScale);
        icons.clear();labels.clear();hovers.clear();footerY=height-58;rowsPerPage=Math.max(1,(footerY-bodyY-18)/23);
        int w=Math.max(140,width-24);search=addRenderableWidget(new EditBox(font,12,28,w-80,20,Component.literal("Search item")));search.setMaxLength(100);search.setValue(mode==Mode.SEARCH?query:"");search.setHint(Component.literal("Search item or pet rarity..."));
        button("Search",width-86,28,74,()->app.command(search.getValue()));
        button("Book",12,53,58,app::openBook);
        button("GUI",width-66,53,54,()->app.open(new HudEditor(app)));
        if(mode!=Mode.DETAIL)button("Assistant: "+(app.book.assistant?"ON":"OFF"),76,53,125,()->{app.book.assistant=!app.book.assistant;app.persist();refresh();});
        if(mode==Mode.SETTINGS)settings();else if(mode==Mode.BOOK)book();else if(mode==Mode.SEARCH)results();else detail();
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
        button("Forget chest scans",12,210,190,()->{app.ownership.storage.clear();refresh();});button("Pet scan instructions",210,210,190,()->app.scanPets(""));
        label("Chest snapshots: "+app.ownership.storage.size()+" | Pets pages: "+app.ownership.petPageCount(),12,244,0xffb9d5c9);
        label("Open each island chest to scan it. Reopen after changes.",12,264,0xffb9d5c9);
        label("Esc closes this screen. Scans reset on reconnect/profile switch.",12,282,0xffb9d5c9);
    }
    private void checkedRow(String id,String suffix,int y,int x,int w){
        boolean checked=goal.checked.contains(id);
        button(checked?"[x]":"[ ]",x,y,28,()->{app.toggleCheck(goal,id);refresh();});
        row(id,suffix,y,x+32,w-32,()->inspect(id));
    }
    private void results(){
        label("Choose a result — pet rarities are separate goals",12,88,0xffa9bac8);
        List<Catalog.Item> items=app.catalog.search(query);int start=page*rowsPerPage;
        for(int i=start;i<Math.min(items.size(),start+rowsPerPage);i++){
            Catalog.Item item=items.get(i);row(item.id,"",bodyY+(i-start)*23,12,width-24,()->app.select(item.id,true));
        }
        pages(items.size());
    }
    private void book(){
        label("Saved goals: "+app.book.goals.size()+"  |  Account: "+app.mc.getUser().getName(),12,88,0xffa9bac8);
        int start=page*rowsPerPage;
        for(int i=start;i<Math.min(app.book.goals.size(),start+rowsPerPage);i++){
            Book.Goal g=app.book.goals.get(i);int y=bodyY+(i-start)*23;
            row(g.id,"",y,12,width-174,()->app.select(g.id,true));
            button(g.visible?"Hide":"Show",width-156,y,66,()->{g.visible=!g.visible;app.persist();refresh();});
            button("Remove",width-86,y,74,()->{app.book.history.add(g.id);app.book.goals.remove(g);if(app.book.last.equals(g.id))app.book.last=app.book.goals.isEmpty()?"":app.book.goals.getLast().id;app.persist();page=0;refresh();});
        }
        if(app.book.goals.isEmpty())label("Search for an item, then click Save or use /rh save.",12,bodyY,0xffd1dce5);
        pages(app.book.goals.size());
    }
    private void detail(){
        goal=app.goal(query);plan=app.plan(goal);Catalog.Item item=app.catalog.item(focus);
        button(app.book.find(goal.id)==null?"Save":"Saved",75,53,62,()->{app.save(goal);refresh();});
        button("Scan Pets",142,53,82,()->app.scanPets(query));
        if(!focus.equals(query))button("Main goal",229,53,83,()->{focus=query;page=0;tab=0;refresh();});
        else button("Refresh",229,53,83,this::refresh);
        button("Assistant: "+(app.book.assistant?"ON":"OFF"),318,53,125,()->{app.book.assistant=!app.book.assistant;app.persist();refresh();});
        String[] tabs={"Recipe","Materials","All steps","Used for","Sources"};
        int tw=Math.min(88,(width-24)/tabs.length);
        for(int n=0;n<tabs.length;n++){final int ix=n;button((tab==n?"> ":"")+tabs[n],12+n*tw,78,tw-2,()->{tab=ix;page=0;refresh();});}
        label(app.trim(item.name,width-160),12,105,0xff000000|item.color);
        if(!focus.equals(query))button("Save as goal",width-132,101,120,()->app.select(focus,false));
        button("Guide",width-202,101,64,()->{app.guide(focus);tab=4;page=0;refresh();});
        if(tab==0)recipe(item);else if(tab==1)materials();else if(tab==2)steps();else if(tab==3)uses();else sources(item);
        if(tab!=4)stockControls(item);
    }
    private Catalog.Route selected(Catalog.Item item){if(item.routes.isEmpty())return null;return item.routes.get(Math.floorMod(goal.choices.getOrDefault(item.id,0),item.routes.size()));}
    private void recipe(Catalog.Item item){
        Catalog.Route r=selected(item);
        if(r==null){
            List<String> lines=new ArrayList<>();lines.add("No crafting / Forge / Kat recipe in this catalogue.");lines.addAll(item.sources);if(item.sources.isEmpty())lines.add("Acquisition not documented here. Check the source wiki; missing data does not prove the item is uncraftable.");
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
            Catalog.Ingredient in=r.inputs.get(n);String suffix=Catalog.amount(in.id,in.count)+(stock.getOrDefault(in.id,0L)>0?"  [have "+stock.get(in.id)+"]":"");
            checkedRow(in.id,suffix,y+(n-start)*23,rx,rw);
        }
        pageButtons(r.inputs.size(),available);
    }
    private void materials(){
        Map<String,Long> shown=new LinkedHashMap<>(plan.leaves);for(String id:goal.checked)shown.putIfAbsent(id,0L);
        List<Map.Entry<String,Long>> entries=new ArrayList<>(shown.entrySet());entries.sort(Comparator.comparing(e->app.catalog.item(e.getKey()).name));
        int start=page*rowsPerPage;
        for(int i=start;i<Math.min(entries.size(),start+rowsPerPage);i++){
            var e=entries.get(i);String rateText="";double rate=goal.rates.getOrDefault(e.getKey(),0.0);if(rate>0)rateText=String.format(Locale.US," ~%.1f h",e.getValue()/rate);
            checkedRow(e.getKey(),goal.checked.contains(e.getKey())?"Done (click [x] to undo)":Catalog.amount(e.getKey(),e.getValue())+rateText,bodyY+(i-start)*23,12,width-24);
        }
        if(entries.isEmpty())label(plan.warnings.isEmpty()?"Materials ready, or this goal is already owned.":"Calculation incomplete — see Sources.",12,bodyY,0xff83d9a5);
        pages(entries.size());
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
        List<String> lines=new ArrayList<>();
        lines.add("Goal: "+app.catalog.item(query).name);lines.add("Ownership: inventory + visited Pets pages + opened island chest snapshots. Unopened storage is UNKNOWN; revisit chests after changes.");
        for(var e:app.stock(goal).entrySet())if(plan.demand.containsKey(e.getKey())&&e.getValue()>0)lines.add("Verified / entered stock: "+e.getValue()+"x "+app.catalog.item(e.getKey()).name);
        if(!item.requirement.isBlank())lines.add(item.requirement);
        lines.addAll(item.sources);if(item.sources.isEmpty())lines.add("No farming location recorded for this item. Check its wiki.");
        lines.addAll(plan.warnings);
        if(!plan.surplus.isEmpty())for(var e:plan.surplus.entrySet())lines.add("Craft output remainder: "+e.getValue()+"x "+app.catalog.item(e.getKey()).name);
        lines.addAll(Assistant.guide(app.catalog,focus));
        lines.add("Gems: Required preserves ingredient quality; Fine expands higher qualities. Change using the Gems button. Checkmarks exclude a whole ingredient branch; uncheck to restore it.");
        lines.add("Time: enter your measured items/hour on the material page. Estimates exclude travel, event downtime and RNG variation. Zero = unknown.");
        lines.add("Kat costs and times in the catalogue are base values; pet level / bonuses may reduce them. Check Kat before upgrading.");
        lines.add("Exact arithmetic uses source recipes; this is not a generative AI chat. Alternative recipes can be chosen with Next route.");
        lines.add("Recipe data: NEU "+app.catalog.date+" / "+app.catalog.commit.substring(0,8)+". Offline snapshot, not a live update.");
        if(!item.wiki.isBlank())lines.add(item.wiki);
        showNotes(lines);
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
        button("Gems: "+(goal.fineGems?"Fine":"Required"),325,y,145,()->{goal.fineGems=!goal.fineGems;app.persist();refresh();});
    }
    private void inspect(String id){focus=id;tab=0;page=0;refresh();}
    private void row(String id,String suffix,int y,int x,int w,Runnable action){
        Catalog.Item item=app.catalog.item(id);String name=(suffix.isEmpty()?"":suffix+"  ")+item.name;
        Button b=button(app.trim(name,Math.max(15,w-28)),x+22,y,Math.max(20,w-22),action);
        b.setMessage(Component.literal(app.trim(name,Math.max(15,w-28))).withStyle(style->style.withColor(item.color)));
        String tip=item.name+(suffix.isEmpty()?"":"\n"+suffix)+"\n"+(item.requirement==null?"":item.requirement);
        if(!item.sources.isEmpty())tip+="\n"+item.sources.getFirst();tip+="\nClick to inspect this ingredient.";
        hovers.add(new Hover(x+22,y,w-22,tip,id));icons.add(new Icon(id,x+2,y+2,0));
    }
    private void pages(int size){pageButtons(size,rowsPerPage);}
    private void pageButtons(int size,int capacity){int total=Math.max(1,(size+capacity-1)/capacity);page=Math.min(page,total-1);int y=footerY-17;
        button("<",width-108,y,24,()->{page=Math.max(0,page-1);refresh();});button(">",width-36,y,24,()->{page=Math.min(total-1,page+1);refresh();});label((page+1)+"/"+total,width-80,y+6,0xffa9bac8);}
    private ItemStack icon(String id){return Icons.stack(app,id);}
    private MouseButtonEvent scaled(MouseButtonEvent e){return new MouseButtonEvent(e.x()/uiScale,e.y()/uiScale,e.buttonInfo());}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){return super.mouseClicked(scaled(e),twice);}
    @Override public boolean mouseReleased(MouseButtonEvent e){return super.mouseReleased(scaled(e));}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return super.mouseDragged(scaled(e),dx/uiScale,dy/uiScale);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){return super.mouseScrolled(x/uiScale,y/uiScale,sx,sy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
        int physicalMx=mx,physicalMy=my;mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        g.pose().pushMatrix();g.pose().scale(uiScale,uiScale);
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath("recipehunter","textures/background.png"),0,0,0f,0f,width,height,2047,1091,2047,1091);
        g.fill(0,0,width,height,0xbc101923);g.fill(0,0,width,23,0xff24384a);
        g.text(font,"RECIPE HUNTER  /  IRONMAN",12,8,0xffefc77d,true);
        for(Label l:labels)g.text(font,l.text,l.x,l.y,l.color,false);
        super.extractRenderState(g,mx,my,dt);
        for(Icon i:icons){g.fill(i.x-1,i.y-1,i.x+19,i.y+19,0xff293846);g.item(icon(i.id),i.x,i.y);if(i.count>0)g.text(font,Long.toString(i.count),i.x+10,i.y+12,0xffffffff,true);
            if(mx>=i.x&&mx<i.x+22&&my>=i.y&&my<i.y+22)g.setTooltipForNextFrame(Component.literal(app.catalog.item(i.id).name+(i.count>0?"\n"+Catalog.amount(i.id,i.count):"")),physicalMx,physicalMy);
        }
        g.pose().popMatrix();
        for(Hover h:hovers)if(mx>=h.x&&mx<h.x+h.w&&my>=h.y&&my<h.y+20)RecipePreview.render(app,g,h.id,goal,physicalMx,physicalMy);
    }
}
