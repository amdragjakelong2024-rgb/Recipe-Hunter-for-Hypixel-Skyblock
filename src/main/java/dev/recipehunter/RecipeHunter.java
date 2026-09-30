package dev.recipehunter;

import java.nio.file.*;
import java.util.*;
import java.io.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;


public final class RecipeHunter implements ClientModInitializer {
    public static RecipeHunter INSTANCE;
    public final Minecraft mc=Minecraft.getInstance();
    public Catalog catalog; public Planner planner;public Book book=new Book();public final Ownership ownership=new Ownership();
    public String error="",lastViewed="",account="";public Book.Goal draft;
    private Path bookPath;private boolean writable=true;private Screen nextScreen;
    private int ticks;public String context="";public final Assistant assistant=new Assistant();public final Celebration celebration=new Celebration();
    public final Map<String,Planner.Plan> hudPlans=new HashMap<>();
    @Override public void onInitializeClient(){
        INSTANCE=this;
        try{catalog=new Catalog(getClass().getResourceAsStream("/assets/recipehunter/catalog.json"));planner=new Planner(catalog);}catch(Exception e){error=e.getMessage();}
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{dispatcher.register(literal("rh")
            .executes(ctx->{openBook();return 1;})
            .then(argument("query",StringArgumentType.greedyString()).executes(ctx->{command(StringArgumentType.getString(ctx,"query"));return 1;})));
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{if(world.isClientSide()&&onHypixel())ownership.clicked(mc,hit.getBlockPos(),context);return InteractionResult.PASS;});
        ClientTickEvents.END_CLIENT_TICK.register(client->tick());
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{ownership.clear();celebration.reset();assistant.reset();draft=null;account="";});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{ownership.clear();celebration.reset();assistant.reset();hudPlans.clear();});
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            if(catalog!=null&&onHypixel())celebration.message(this,message.getString());
            String text=message.getString().toLowerCase(Locale.ROOT);
            if(text.contains("profile")&&(text.contains("switched")||text.contains("switching")||text.contains("joined"))){ownership.clear();for(Book.Goal g:book.goals)g.owned.clear();celebration.reset();assistant.reset();hudPlans.clear();}
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("recipehunter","goals"),(g,dt)->hud(g));
    }
    void ensureAccount(){
        String id=mc.getUser().getProfileId().toString();if(id.equals(account))return;account=id;
        bookPath=FabricLoader.getInstance().getConfigDir().resolve("recipehunter").resolve(id+".json");
        try{book=Book.read(bookPath);writable=true;for(Book.Goal g:book.goals)g.owned.clear();}catch(IOException e){book=new Book();writable=false;say("Book could not be loaded; original file preserved. "+e.getMessage());}
    }
    public boolean persist(){ensureAccount();if(!writable){say("Cannot save: fix or back up the unreadable book file first.");return false;}try{book.write(bookPath);return true;}catch(IOException e){say("Save failed: "+e.getMessage());return false;}}
    public void say(String s){if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("[Recipe Hunter] "+s));}
    public void open(Screen s){nextScreen=s;}
    public void openBook(){ensureAccount();if(catalog==null){say(error);return;}open(new HunterScreen(this,HunterScreen.Mode.BOOK,""));}
    public void command(String query){
        ensureAccount();if(catalog==null){say(error);return;}String q=query.trim();
        switch(q.toLowerCase(Locale.ROOT)){
            case "book"->openBook();
            case "gui"->open(new HudEditor(this));
            case "settings"->open(new HunterScreen(this,HunterScreen.Mode.SETTINGS,""));
            case "assistant"->{book.assistant=!book.assistant;persist();openBook();}
            case "bigger"->resizeGui(0.1);
            case "smaller"->resizeGui(-0.1);
            case "save"->{if(lastViewed.isEmpty())say("First search for a recipe using /rh <name>.");else save(goal(lastViewed));}
            case "hide","show"->{Book.Goal g=book.find(book.last);if(g==null)say("No saved goal yet.");else{g.visible=q.equalsIgnoreCase("show");persist();say((g.visible?"Shown: ":"Hidden: ")+catalog.item(g.id).name);}}
            default->{List<Catalog.Item> hits=catalog.search(q);if(hits.isEmpty()){say("No matching item: "+q);return;}
                if(Catalog.score(Catalog.normal(q),Catalog.normal(hits.getFirst().name))==0)select(hits.getFirst().id,true);
                else open(new HunterScreen(this,HunterScreen.Mode.SEARCH,q));}
        }
    }
    public Book.Goal goal(String id){Book.Goal g=book.find(id);if(g!=null)return g;if(draft==null||!draft.id.equals(id))draft=new Book.Goal(id);return draft;}
    public void select(String id,boolean verifyPet){
        lastViewed=id;goal(id);
        if(verifyPet&&catalog.item(id).name.endsWith(" Pet")&&!ownership.petsVisited)say("Open /pet yourself and visit every page to verify your pets. Then reopen /rh book or this recipe.");
        open(new HunterScreen(this,HunterScreen.Mode.DETAIL,id));
    }
    public void resizeGui(double amount){ensureAccount();book.guiScale=Math.clamp(book.guiScale+amount,0.65,1.6);persist();open(new HunterScreen(this,HunterScreen.Mode.SETTINGS,""));}
    public void toggleCheck(Book.Goal goal,String id){if(!goal.checked.remove(id))goal.checked.add(id);persist();hudPlans.clear();}
    public void guide(String id){book.assistantItem=id;book.assistant=true;assistant.reset();persist();say(Assistant.guide(catalog,id).getFirst());}
    public boolean onHypixel(){var server=mc.getCurrentServer();if(server==null)return false;String host=server.ip.toLowerCase(Locale.ROOT).split(":")[0];return host.equals("hypixel.net")||host.endsWith(".hypixel.net");}
    public void scanPets(String returnId){ownership.clearPets();say("Type /pet, visit ALL pages, then return to /rh. Loaded pages will be counted automatically.");mc.setScreen(null);}
    public Map<String,Long> stock(Book.Goal g){Map<String,Long> stock=onHypixel()?ownership.stock(mc):new HashMap<>();g.owned.forEach((id,n)->stock.merge(id,n,Math::max));return stock;}
    public Planner.Plan plan(Book.Goal g){try{return planner.goal(g,stock(g));}catch(RuntimeException e){Planner.Plan p=new Planner.Plan();p.warnings.add("Cannot calculate this route: "+e.getMessage());return p;}}
    public void save(Book.Goal g){ensureAccount();if(!writable){say("Book file is unreadable; saving is disabled to protect it.");return;}if(book.find(g.id)==null)book.goals.add(g);book.last=g.id;book.history.add(g.id);g.visible=true;if(persist())say("Saved: "+catalog.item(g.id).name);}
    void tick(){
        if(catalog==null||mc.player==null)return;ensureAccount();ticks++;
        if(onHypixel()){
            if(ticks%10==0)context=WorldContext.text(mc);
            ownership.scanPets(mc);ownership.scanChest(mc,context);celebration.tick(this);assistant.tick(this,context);
        }
        if(nextScreen!=null){Screen s=nextScreen;nextScreen=null;mc.setScreen(s);}
        if(ticks%20==0){hudPlans.clear();for(Book.Goal goal:book.goals)if(goal.visible)hudPlans.put(goal.id,plan(goal));}
    }
    void hud(GuiGraphicsExtractor g){
        if(catalog==null||mc.player==null||mc.options.hideGui||!onHypixel())return;
        if(mc.screen==null){
            HudPanels.render(this,g,-1,-1,System.currentTimeMillis()/6000);
            if(book.assistant&&!book.assistantItem.isEmpty()){
                int y2=g.guiHeight()-48;g.fill(4,y2-4,g.guiWidth()-4,g.guiHeight()-3,0xc510221a);g.text(mc.font,"ASSISTANT: "+trim(catalog.item(book.assistantItem).name,g.guiWidth()-95),10,y2,0xff78efad,true);
                g.textWithWordWrap(mc.font,Component.literal(assistant.status),10,y2+13,g.guiWidth()-20,0xffe2f3e8);
            }
        }
        celebration.render(this,g);
    }
    String trim(String s,int width){return mc.font.plainSubstrByWidth(s,Math.max(8,width));}
}
