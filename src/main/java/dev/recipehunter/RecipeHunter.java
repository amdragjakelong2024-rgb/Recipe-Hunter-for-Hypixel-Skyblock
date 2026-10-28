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
    public final MenuTracker menus=new MenuTracker(); public final GrindEstimates estimates=new GrindEstimates();
    public final MaterialLedger ledger=new MaterialLedger();
    public final Map<String,RecipeProgress.Node> progressTrees=new HashMap<>();
    public Catalog catalog; public Planner planner;public Book book=new Book();public final Ownership ownership=new Ownership();
    public String error="",lastViewed="",account="";public Book.Goal draft;
    private Path bookPath;private boolean writable=true;private Screen nextScreen;
    private int ticks;public String context="";public final Celebration celebration=new Celebration();
    public final Map<String,Planner.Plan> hudPlans=new HashMap<>();
    public final Map<String,List<MaterialProgress.Row>> hudProgress=new HashMap<>();
    public RecipeProgress.Node tree(Book.Goal goal){
        RecipeProgress.Node cached=progressTrees.get(goal.id);if(cached!=null&&book.find(goal.id)==goal)return cached;
        return new RecipeProgress(catalog).evaluate(goal,stock(goal),CraftDiary.mergeReservations(book.diary,menus.forge),CraftDiary.pending(menus.forge));
    }
    public List<MaterialProgress.Row> progress(Book.Goal goal){return new RecipeProgress(catalog).displayed(tree(goal),goal).stream().map(RecipeProgress.Node::row).toList();}
    @Override public void onInitializeClient(){
        INSTANCE=this; InventoryOverlay.register(this);
        try{RetiredSettings.remove(FabricLoader.getInstance().getConfigDir().resolve("recipehunter-private"));}catch(IOException e){System.err.println("[Recipe Hunter] Could not remove retired private settings.");}
        try{catalog=new Catalog(getClass().getResourceAsStream("/assets/recipehunter/catalog.json"));planner=new Planner(catalog);}catch(Exception e){error=e.getMessage();}
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{dispatcher.register(literal("rh")
            .executes(ctx->{openHome();return 1;})
            .then(argument("query",StringArgumentType.greedyString()).executes(ctx->{command(StringArgumentType.getString(ctx,"query"));return 1;})));
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{if(world.isClientSide()&&onHypixel())ownership.clicked(mc,hit.getBlockPos(),context);return InteractionResult.PASS;});
        ClientTickEvents.END_CLIENT_TICK.register(client->tick());
        UiSmoke.register(this);
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{ownership.clear();menus.clear();ledger.clear();progressTrees.clear();hudProgress.clear();estimates.reset();celebration.reset();draft=null;account="";});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{ownership.clear();menus.clear();ledger.clear();progressTrees.clear();hudProgress.clear();estimates.reset();celebration.reset();hudPlans.clear();});
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            if(catalog!=null&&onHypixel()){
                celebration.message(this,message.getString());
                menus.serverBalance(message.getString(),catalog);
                menus.sackMessage(message.getString(),SackChatTracker.changes(message,catalog));
            }
            String text=message.getString().toLowerCase(Locale.ROOT);
            if(text.contains("profile")&&(text.contains("switched")||text.contains("switching")||text.contains("joined"))){ownership.clear();menus.clear();ledger.clear();progressTrees.clear();hudProgress.clear();estimates.reset();for(Book.Goal g:book.goals)g.owned.clear();for(var e:book.diary)e.active=false;persist();celebration.reset();hudPlans.clear();}
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("recipehunter","goals"),(g,dt)->hud(g));
    }
    void ensureAccount(){
        String id=mc.getUser().getProfileId().toString();if(id.equals(account))return;account=id;
        bookPath=FabricLoader.getInstance().getConfigDir().resolve("recipehunter").resolve(id+".json");
        try{book=Book.read(bookPath);writable=true;for(Book.Goal g:book.goals){g.owned.clear();if(ArmorSets.isSet(g)&&!ArmorSets.register(catalog,g))throw new IOException("Invalid saved armor set: "+g.id);}}catch(IOException e){book=new Book();writable=false;say("Book could not be loaded; original file preserved. "+e.getMessage());}
    }
    public boolean persist(){progressTrees.clear();hudProgress.clear();ensureAccount();if(!writable){say("Cannot save: fix or back up the unreadable book file first.");return false;}try{book.write(bookPath);return true;}catch(IOException e){say("Save failed: "+e.getMessage());return false;}}
    public void say(String s){if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("[Recipe Hunter] "+s));}
    public void open(Screen s){nextScreen=s;}
    public void openHome(){ensureAccount();if(catalog==null){say(error);return;}if(mc.screen instanceof RecipeBookScreen||mc.screen instanceof ToolsScreen||mc.screen instanceof HunterScreen)openHomeContent();else if(book.animations)open(new BookOpening(this));else openHomeContent();}
    public void closeBook(){if(book.animations)open(new BookOpening(this,true));else mc.setScreen(null);}
    public void openHomeContent(){open(new RecipeBookScreen(this,true,"",""));}
    public void removeGoal(Book.Goal g){book.goals.remove(g);if(draft==g)draft=null;hudPlans.remove(g.id);book.last=book.goals.isEmpty()?"":book.goals.getFirst().id;persist();}
    public void gameRecipe(String id){if(id.startsWith(ArmorSets.PREFIX))return;if(!onHypixel()||mc.getConnection()==null){say("Join Hypixel SkyBlock to open server recipes.");return;}if(!id.matches("[A-Z0-9_;:-]+"))return;mc.setScreen(null);mc.getConnection().sendCommand("viewrecipe "+id);}
    public void openBook(){ensureAccount();if(catalog==null){say(error);return;}open(new RecipeBookScreen(this,false,"",""));}
    public void command(String query){
        ensureAccount();if(catalog==null){say(error);return;}String q=query.trim();
        if(q.toLowerCase(Locale.ROOT).startsWith("set ")){String id=catalog.resolve(q.substring(4).trim());if(id.isEmpty())say("Use a full armor piece name, e.g. Final Destination Helmet.");else saveArmorSet(id);return;}
        if(q.startsWith("open ")){String id=q.substring(5).trim();if(catalog.items.containsKey(id))select(id,false);else say("Unknown SkyBlock recipe.");return;}
        if(q.toLowerCase(Locale.ROOT).startsWith("status ")){String id=catalog.resolve(q.substring(7));if(id.isEmpty()){say("Unknown or ambiguous SkyBlock item. Use its exact ID.");return;}say(id);for(String line:ledger.diagnostics(id))say(line);say("Observed physical total: "+physicalStock().getOrDefault(id,0L)+". Unseen storage is Unknown. Recipe coverage is not ownership.");say("Forge reserved: "+CraftDiary.pending(menus.forge).getOrDefault(id,0L)+" | session-acquired total: Unknown (transfers are not acquisitions)");return;}
        switch(q.toLowerCase(Locale.ROOT)){
            case ""->openHome();
            case "book"->openBook();
            case "set"->{if(lastViewed.isEmpty())say("Select an armor piece first, then use /rh set.");else saveArmorSet(lastViewed);}
            case "help"->{for(String line:List.of("/rh - all recipes","/rh book - saved recipes","/rh <name> - search recipes","/rh current - active recipe (click to open)","/rh diary - Craft Diary","/rh calc - calculator","/rh gui - move/resize HUD and change pages","/rh settings - appearance settings","/rh save - save last selected recipe","/rh set - combine four matching armor pieces into one HUD","/rh hide | /rh show - toggle last goal","/rh bigger | /rh smaller - book size","/rh sackstatus - Sack diagnostics","/rh resync - discard stale storage observations","/rh status <ITEM_ID> - observed item sources"))say(line);}
            case "current"->{Book.Goal active=book.active();if(active==null){say("No active recipe. Save and show a goal first.");return;}mc.player.sendSystemMessage(Component.literal("[Recipe Hunter] "+catalog.item(active.id).name+" x"+active.count+"  ").append(Component.literal("[Open recipe]").withStyle(style->style.withColor(0x88cc55).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/rh open "+active.id)))));}

            case "diary"->open(new ToolsScreen(this,"Craft Diary"));
            case "calc"->open(new ToolsScreen(this,"Calculator"));
            case "gui"->open(new HudEditor(this));
            case "settings"->open(new HunterScreen(this,HunterScreen.Mode.SETTINGS,""));
            case "resync"->{menus.clear();ownership.clear();ledger.clear();progressTrees.clear();hudProgress.clear();say("Storage observations cleared. Inventory updates automatically. Open each Sack and Ender Chest page once to synchronise their totals; enable Sack Notifications for later live updates.");}
            case "status"->{say("Use /rh status <SkyBlock item ID> for source counts and update times. Inventory is live; unseen storage is Unknown.");}
            case "sackstatus"->{say(menus.sackStatus()+" | notifications: "+menus.sackNoticeCount+" | parsed: "+menus.sackParsedCount+" | no details: "+menus.sackUnparsedCount+". Enable Hypixel SkyBlock Settings > Personal > Chat Feedback > Sack Notifications.");}
            case "bigger"->resizeGui(0.1);
            case "smaller"->resizeGui(-0.1);
            case "save"->{if(lastViewed.isEmpty())say("First search for a recipe using /rh <name>.");else save(goal(lastViewed));}
            case "hide","show"->{Book.Goal g=book.find(book.last);if(g==null)say("No saved goal yet.");else{g.visible=q.equalsIgnoreCase("show");persist();say((g.visible?"Shown: ":"Hidden: ")+catalog.item(g.id).name);}}
            default->{List<Catalog.Item> hits=catalog.search(q);if(hits.isEmpty()){say("No matching item: "+q);return;}
                if(Catalog.score(Catalog.normal(q),Catalog.normal(hits.getFirst().name))==0)select(hits.getFirst().id,true);
                else open(new RecipeBookScreen(this,true,q,""));}
        }
    }
    public Book.Goal goal(String id){Book.Goal g=book.find(id);if(g!=null)return g;if(draft==null||!draft.id.equals(id)){draft=new Book.Goal(id);draft.freshCraft=true;draft.targetBaseline=physicalStock().getOrDefault(id,0L);}return draft;}
    public void select(String id,boolean verifyPet){
        lastViewed=id;goal(id);
        if(verifyPet&&catalog.item(id).name.endsWith(" Pet")&&!ownership.petsVisited)say("Open /pet yourself and visit every page to verify your pets. Then reopen /rh book or this recipe.");
        open(new RecipeBookScreen(this,true,"",id));
    }
    public void resizeGui(double amount){ensureAccount();book.guiScale=Math.clamp(book.guiScale+amount,0.65,1.6);persist();open(new HunterScreen(this,HunterScreen.Mode.SETTINGS,""));}
    public void toggleCheck(Book.Goal goal,String id){if(!goal.checked.remove(id))goal.checked.add(id);persist();hudPlans.clear();}
    public boolean onHypixel(){var server=mc.getCurrentServer();if(server==null)return false;String host=server.ip.toLowerCase(Locale.ROOT).split(":")[0];return host.equals("hypixel.net")||host.endsWith(".hypixel.net");}
    public void scanPets(String returnId){ownership.clearPets();say("Type /pet, visit ALL pages, then return to /rh. Loaded pages will be counted automatically.");mc.setScreen(null);}
    public Map<String,Long> physicalStock(){Map<String,Long> out=onHypixel()?ownership.stock(mc,catalog):new HashMap<>();if(onHypixel())menus.stock().forEach((id,n)->out.merge(id,n,Long::sum));return out;}
    public Map<String,Long> stock(Book.Goal g){Map<String,Long> stock=onHypixel()?ownership.stock(mc,catalog):new HashMap<>();if(onHypixel())menus.stock().forEach((id,n)->stock.merge(id,n,Math::addExact));return stock;}
    public Planner.Plan plan(Book.Goal g){try{return planner.goal(g,stock(g),CraftDiary.mergeReservations(book.diary,menus.forge));}catch(RuntimeException e){Planner.Plan p=new Planner.Plan();p.warnings.add("Cannot calculate this route: "+e.getMessage());return p;}}
    public void save(Book.Goal g){ensureAccount();if(!writable){say("Book file is unreadable; saving is disabled to protect it.");return;}if(g.finished){book.restart(g,physicalStock());}else if(book.find(g.id)==null){g.targetBaseline=physicalStock().getOrDefault(g.id,0L);book.goals.add(g);}book.last=g.id;book.history.add(g.id);g.visible=true;if(persist())say("Saved: "+catalog.item(g.id).name);}
    /** Group the four saved pieces into one HUD and a single stock allocation. */
    public String saveArmorSet(String pieceId){
        ensureAccount();if(!writable){say("Book is not writable.");return "";}
        var parts=ArmorSets.detect(catalog,pieceId);
        if(parts.size()!=4){say("No complete four-piece armor set for this item.");return "";}
        String id=ArmorSets.id(catalog.item(pieceId).name);
        Book.Goal grouped=book.find(id);
        if(grouped==null){
            grouped=new Book.Goal(id);grouped.armorPieces.addAll(parts);grouped.freshCraft=true;grouped.targetBaseline=0;
            for(Book.Goal old:book.goals)if(parts.contains(old.id)){
                grouped.choices.putAll(old.choices);
                grouped.checked.addAll(old.checked);
                if(old.hud!=null&&grouped.hud==null)grouped.hud=old.hud;
                grouped.materialDisplay=old.materialDisplay;grouped.gemDisplay=old.gemDisplay;
            }
            if(!ArmorSets.register(catalog,grouped)){say("Unable to register armor set.");return "";}
        }
        book.goals.removeIf(g->parts.contains(g.id));
        if(book.find(id)==null)book.goals.add(grouped);
        grouped.visible=true;book.last=id;lastViewed=id;book.history.add(id);
        if(persist())say("Armor set saved: "+catalog.item(id).name+" (4 pieces, one shared tracker).");
        return id;
    }
    public void restartGoal(Book.Goal g){book.restart(g,physicalStock());persist();say("Restarted: "+catalog.item(g.id).name);}
    public void finishGoal(Book.Goal g){book.finish(g);persist();}
    public void completed(String id,long count){
        Book.Goal g=book.find(id);if(g==null||g.finished)return;UiSounds.craft(this);book.completed.add(id);g.crafted=Math.min(g.count,g.crafted+count);
        if(g.crafted>=g.count){book.completed.add(id);book.finish(g);hudPlans.remove(id);say("Completed: "+catalog.item(id).name+". You can start this recipe again.");}persist();
    }
    void tick(){
        if(catalog==null||mc.player==null)return;ensureAccount();ticks++;
        if(onHypixel()){
            if(ticks%10==0)context=WorldContext.text(mc);
            ownership.scanPets(mc);ownership.scanChest(mc,context,catalog);menus.tick(this);estimates.tick(this);celebration.tick(this);
        }
        if(nextScreen!=null){Screen s=nextScreen;nextScreen=null;mc.setScreen(s);}
        if(ticks%5==0){
            hudPlans.clear();hudProgress.clear();progressTrees.clear();
            long now=System.currentTimeMillis();
            ledger.snapshot("Inventory",ownership.inventory(mc,catalog),now,MaterialLedger.Quality.LIVE);
            ledger.snapshot("Ender Chest",menus.ender.totals(),menus.observedAt,MaterialLedger.Quality.LAST_SYNCED);
            ledger.snapshot("Island chests",ownership.storage.totals(),now,MaterialLedger.Quality.LAST_SYNCED);
            ledger.snapshot("Sacks",Map.of(),now,MaterialLedger.Quality.UNKNOWN);
            for(var e:menus.sacks.entrySet())ledger.put("Sacks",e.getKey(),new MaterialLedger.Balance(e.getValue(),Math.max(menus.lastSackMessageAt,menus.sackSnapshots.getOrDefault(e.getKey(),0L)),menus.uncertainSacks.contains(e.getKey())?MaterialLedger.Quality.UNKNOWN:menus.sackSnapshots.containsKey(e.getKey())?MaterialLedger.Quality.LAST_SYNCED:MaterialLedger.Quality.SESSION_ONLY));
            Map<String,Long> pool=new HashMap<>(onHypixel()?ownership.stock(mc,catalog):Map.of());if(onHypixel())menus.stock().forEach((id,n)->pool.merge(id,n,Long::sum));
            for(var goal:book.goals)if(goal.freshCraft&&goal.targetBaseline<0)goal.targetBaseline=pool.getOrDefault(goal.id,0L);
            try{
                var fresh=LiveProgress.refresh(catalog,book,pool,menus.forge);
                progressTrees.putAll(fresh.trees());hudProgress.putAll(fresh.rows());
                for(Book.Goal goal:book.goals)hudPlans.put(goal.id,plan(goal));
            }catch(RuntimeException e){error="Progress unavailable: "+e.getMessage();}

        }
    }
    void hud(GuiGraphicsExtractor g){
        if(catalog==null||mc.player==null||mc.options.hideGui||!onHypixel())return;
        if(mc.screen==null){
            HudPanels.render(this,g,-1,-1,0);

        }
        celebration.render(this,g);
    }
    String trim(String s,int width){return mc.font.plainSubstrByWidth(s,Math.max(8,width));}
}
