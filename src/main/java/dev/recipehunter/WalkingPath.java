package dev.recipehunter;
import java.util.*;
import java.util.function.Predicate;
/** Bounded shortest walk path on a local, loaded, collision-checked grid. No mining or movement. */
public final class WalkingPath {
 public record Point(int x,int y,int z){public Point add(int dx,int dy,int dz){return new Point(x+dx,y+dy,z+dz);}public int distance(Point p){return Math.abs(x-p.x)+Math.abs(y-p.y)+Math.abs(z-p.z);}}
 public static List<Point> find(Point start,Point goal,Predicate<Point> walkable,int limit){
  record Node(Point p,int score){}PriorityQueue<Node> open=new PriorityQueue<>(Comparator.comparingInt(Node::score));Map<Point,Integer> costs=new HashMap<>();Map<Point,Point> parent=new HashMap<>();
  open.add(new Node(start,start.distance(goal)));costs.put(start,0);int visits=0;
  while(!open.isEmpty()&&visits++<limit){Point p=open.remove().p();if(p.equals(goal)){List<Point> path=new ArrayList<>();for(Point n=p;n!=null;n=parent.get(n))path.add(n);Collections.reverse(path);return path;}
   for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1}){
    Point n=p.add(d[0],dy,d[1]);if(Math.abs(n.x-start.x)>32||Math.abs(n.z-start.z)>32||Math.abs(n.y-start.y)>12||!walkable.test(n))continue;
    // Stepping up needs extra headroom over the starting cell, checked by caller through standability.
    int cost=costs.get(p)+1+Math.abs(dy);if(cost>=costs.getOrDefault(n,Integer.MAX_VALUE))continue;costs.put(n,cost);parent.put(n,p);open.add(new Node(n,cost+n.distance(goal)));
   }
  }return List.of();
 }
}
