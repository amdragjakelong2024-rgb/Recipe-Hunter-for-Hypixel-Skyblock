package dev.recipehunter;
import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class Book {
    public static final class Goal {
        public String id; public boolean visible=true; public long count=1;
        public Set<String> checked=new HashSet<>();
        public boolean fineGems=false;
        public HudLayout hud;
        public Map<String,Long> owned=new HashMap<>();
        public Map<String,Integer> choices=new HashMap<>();
        public Map<String,Double> rates=new HashMap<>();
        public Goal(String id){this.id=id;}
    }
    public Set<String> history=new HashSet<>();
    public boolean assistant=false,celebrations=true,celebrationSound=true;
    public double guiScale=1.0,hudScale=1.0;
    public String assistantItem="";
    public List<Goal> goals=new ArrayList<>(); public String last="";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static Book read(Path path)throws IOException {
        if(!Files.exists(path))return new Book();
        try(var r=Files.newBufferedReader(path)){
            Book b=GSON.fromJson(r,Book.class);if(b==null||b.goals==null)throw new IOException("Invalid book");
            if(b.history==null)b.history=new HashSet<>();
            b.guiScale=Math.clamp(b.guiScale,0.65,1.6);b.hudScale=Math.clamp(b.hudScale,0.65,1.6);
            for(Goal g:b.goals){if(g.checked==null)g.checked=new HashSet<>();b.history.add(g.id);if(g.id==null||g.count<1)throw new IOException("Invalid goal");if(g.owned==null)g.owned=new HashMap<>();if(g.choices==null)g.choices=new HashMap<>();if(g.rates==null)g.rates=new HashMap<>();}
            return b;
        }catch(JsonParseException e){throw new IOException("Invalid book; original file preserved",e);}
    }
    public void write(Path path)throws IOException {
        Files.createDirectories(path.getParent());Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(this));
        try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    public Goal find(String id){return goals.stream().filter(g->g.id.equals(id)).findFirst().orElse(null);}
}
