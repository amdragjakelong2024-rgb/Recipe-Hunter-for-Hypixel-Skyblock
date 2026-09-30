package dev.recipehunter;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Catalog {
    public static final class Ingredient { public String id; public long count; public Ingredient(String i,long n){id=i;count=n;} }
    public static final class Route { public String type; public long count=1,seconds; public List<Ingredient> inputs=new ArrayList<>(),slots=new ArrayList<>(); }
    public static final class Item { public String id,name,icon,requirement,wiki; public int color=0xffffff; public boolean raw; public List<Route> routes=new ArrayList<>(); public List<String> sources=new ArrayList<>(); }
    private static final class FileData { String date,commit; List<Item> items; }
    public final Map<String,Item> items=new LinkedHashMap<>();
    public final Map<String,List<String>> uses=new HashMap<>();
    public String date,commit;
    public Catalog(InputStream in)throws IOException {
        if(in==null)throw new IOException("Recipe catalogue missing");
        try(var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            FileData data=new Gson().fromJson(reader,FileData.class);date=data.date;commit=data.commit;
            for(Item i:data.items){i.name=i.name.replaceAll("[\\uE000-\\uF8FF]", "").trim();items.put(i.id,i);}
        }
        for(Item item:items.values())for(Route r:item.routes)for(Ingredient in2:r.inputs) {
            List<String> list=uses.computeIfAbsent(in2.id,k->new ArrayList<>());if(!list.contains(item.id))list.add(item.id);
        }
    }
    public Item item(String id){ Item i=items.get(id);if(i!=null)return i; i=new Item();i.id=id;i.name=id;i.icon="minecraft:paper";i.wiki="";i.requirement="";return i; }
    public static String normal(String s){return s.toLowerCase(Locale.ROOT).replaceAll("§.","").replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
    public List<Item> search(String q){
        String n=normal(q);if(n.isBlank())return List.of();
        return items.values().stream().filter(i->!i.id.endsWith("_NPC")&&!i.id.endsWith("_BOSS"))
            .map(i->Map.entry(i,score(n,normal(i.name)))).filter(e->e.getValue()<500)
            .sorted(Comparator.<Map.Entry<Item,Integer>>comparingInt(Map.Entry::getValue).thenComparing(e->e.getKey().name))
            .limit(80).map(Map.Entry::getKey).toList();
    }
    static int score(String q,String s){
        if(q.equals(s)||s.equals(q+" pet"))return 0;
        if(s.startsWith(q+" "))return 10+s.length()-q.length();
        if(s.contains(q))return 40+s.length()-q.length();
        boolean all=Arrays.stream(q.split(" ")).allMatch(s::contains);if(all)return 70+s.length()-q.length();
        int distance=distance(q,s);return distance<=Math.max(2,q.length()/4)?200+distance:999;
    }
    static int distance(String a,String b){int[] prev=new int[b.length()+1];for(int j=0;j<prev.length;j++)prev[j]=j;for(int i=1;i<=a.length();i++){int[] next=new int[b.length()+1];next[0]=i;for(int j=1;j<next.length;j++)next[j]=Math.min(Math.min(next[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=next;}return prev[b.length()];}
    public static String amount(String id,long n){return id.equals("SKYBLOCK_COIN")?String.format(Locale.US,"%,d coins",n):qty(n);}
    public static String qty(long n){return String.format(Locale.US,"%,d",n)+(n>=64?" ("+n/64+" stack"+(n/64==1?"":"s")+(n%64==0?"":" + "+n%64)+")":"");}
}
