package dev.recipehunter;
import java.util.Map;
/** Material readiness is deliberately not an input to ownership filtering. */
public final class CompletionFilter {
 public enum Mode { ALL, HIDE_COMPLETED, OWNED_ONLY }
 public static boolean include(Mode mode,String id,Map<String,Long> physical,java.util.Set<String> crafted){
  boolean owned=physical.getOrDefault(id,0L)>0;
  return switch(mode){case ALL->true;case HIDE_COMPLETED->!owned&&!crafted.contains(id);case OWNED_ONLY->owned;};
 }
}
