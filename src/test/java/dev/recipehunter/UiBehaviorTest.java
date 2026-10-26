package dev.recipehunter;
import java.util.*;
public final class UiBehaviorTest {
 public static void run(){
  for(int[] size:new int[][]{{320,240},{640,360},{854,480},{1920,1080},{3440,1440}})for(double scale:new double[]{.65,1,1.6}){
   var v=BookViewport.fit(size[0],size[1],scale);
   PlannerTest.check(v.left()>=0&&v.top()>=0&&v.x(720)<=size[0]&&v.y(430)<=size[1],"book remains inside viewport");
   PlannerTest.check(Math.abs(v.x(360)-size[0]/2.0)<=1&&Math.abs(v.y(215)-size[1]/2.0)<=1,"opening and content share centered viewport");
   PlannerTest.check(Math.abs(v.localX(v.x(40))-40)<=1.6&&Math.abs(v.localY(v.y(96))-96)<=1.6,"search hitbox matches painted bounds");
  }
  PlannerTest.eq(BookViewport.page(0,1,1),0,"one-page next does not animate or advance");PlannerTest.eq(BookViewport.page(0,-1,1),0,"one-page previous remains still");PlannerTest.eq(BookViewport.page(1,-1,3),0,"manual previous page");PlannerTest.eq(BookViewport.page(1,1,3),2,"manual next page");PlannerTest.eq(BookViewport.page(2,1,3),2,"no wrap past last page");
  var mats=Map.of("ENCHANTED_DIAMOND",160L);
  PlannerTest.check(CompletionFilter.include(CompletionFilter.Mode.HIDE_COMPLETED,"ENCHANTED_DIAMOND_BLOCK",mats,Set.of()),"craftable is not completed");
  PlannerTest.check(!CompletionFilter.include(CompletionFilter.Mode.OWNED_ONLY,"ENCHANTED_DIAMOND_BLOCK",mats,Set.of()),"owned filter excludes merely available ingredients");
  PlannerTest.check(CompletionFilter.include(CompletionFilter.Mode.OWNED_ONLY,"ENCHANTED_DIAMOND_BLOCK",Map.of("ENCHANTED_DIAMOND_BLOCK",1L),Set.of()),"owned filter accepts physical finished item");
  PlannerTest.check(!CompletionFilter.include(CompletionFilter.Mode.HIDE_COMPLETED,"ENCHANTED_DIAMOND_BLOCK",Map.of(),Set.of("ENCHANTED_DIAMOND_BLOCK")),"hide completed respects recorded craft history");
  PlannerTest.eq(HudPanels.PER_PAGE,7,"HUD displays seven materials per page");
 }
}
