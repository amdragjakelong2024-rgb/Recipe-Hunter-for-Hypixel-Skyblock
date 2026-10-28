package dev.recipehunter;
import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class Book {
    public static final class Goal {
        public String id; public boolean visible=true; public long count=1; public List<String> armorPieces=new ArrayList<>();
        public int hudPage=0; public String materialDisplay="Compact",gemDisplay="Fine";
        public Set<String> checked=new HashSet<>();
        public boolean fineGems=false;
        public boolean compact=true,freshCraft=false,finished=false; public long crafted=0,targetBaseline=-1; public Set<String> expanded=new HashSet<>();
        public HudLayout hud;
        public Map<String,Long> owned=new HashMap<>();
        public Map<String,Integer> choices=new HashMap<>();
        public Map<String,Double> rates=new HashMap<>();
        public Goal(String id){this.id=id;}
    }
    public Set<String> history=new HashSet<>(),completed=new HashSet<>(),celebrated=new HashSet<>();
    public List<CraftDiary.Entry> diary=new ArrayList<>();
    public boolean uiSounds=true,animations=true;
    public boolean celebrations=true,celebrationSound=true;
    public double guiScale=1.0,hudScale=1.0;
    public String diaryDraft="",diaryEditingKey="";
    public List<Goal> goals=new ArrayList<>(); public String last="";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static Book read(Path path)throws IOException {
        if(!Files.exists(path))return new Book();
        try(var r=Files.newBufferedReader(path)){
            Book b=GSON.fromJson(r,Book.class);if(b==null||b.goals==null)throw new IOException("Invalid book");
            if(b.history==null)b.history=new HashSet<>();
            if(b.celebrated==null)b.celebrated=new HashSet<>();if(b.completed==null)b.completed=new HashSet<>();if(b.diary==null)b.diary=new ArrayList<>();
            if(b.diaryDraft==null)b.diaryDraft="";if(b.diaryEditingKey==null)b.diaryEditingKey="";
            b.guiScale=Math.clamp(b.guiScale,0.65,1.6);b.hudScale=Math.clamp(b.hudScale,0.65,1.6);
            for(Goal g:b.goals){if(g.armorPieces==null)g.armorPieces=new ArrayList<>();g.freshCraft=true;if(g.materialDisplay==null)g.materialDisplay="Compact";if(g.gemDisplay==null)g.gemDisplay="Fine";if(g.expanded==null)g.expanded=new HashSet<>();if(g.checked==null)g.checked=new HashSet<>();b.history.add(g.id);if(g.id==null||g.count<1)throw new IOException("Invalid goal");if(g.owned==null)g.owned=new HashMap<>();if(g.choices==null)g.choices=new HashMap<>();if(g.rates==null)g.rates=new HashMap<>();}
            return b;
        }catch(JsonParseException e){throw new IOException("Invalid book; original file preserved",e);}
    }
    public void write(Path path)throws IOException {
        Files.createDirectories(path.getParent());Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(this));
        try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    /** Start another craft without changing display, quantity or actual material stock. */
    public void restart(Goal g,Map<String,Long> observed){
        g.crafted=0;g.finished=false;g.freshCraft=true;g.targetBaseline=observed.getOrDefault(g.id,0L);
        g.checked.clear();g.owned.clear();g.hudPage=0;g.visible=true;
        completed.remove(g.id);celebrated.remove(g.id);last=g.id;
        if(find(g.id)==null)goals.add(g);
    }
    public void finish(Goal g){g.crafted=g.count;g.finished=true;g.visible=false;completed.add(g.id);}
    public Goal active(){return goals.stream().filter(g->g.visible).findFirst().orElse(null);}
    public void move(Goal goal,int delta){int i=goals.indexOf(goal);if(i<0)return;int to=Math.clamp(i+delta,0,goals.size()-1);if(i!=to){goals.remove(i);goals.add(to,goal);}}
    public Goal find(String id){return goals.stream().filter(g->g.id.equals(id)).findFirst().orElse(null);}
}
