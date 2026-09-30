package dev.recipehunter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.*;
import net.minecraft.network.chat.Component;
public final class WorldContext {
 public static String text(Minecraft mc){
  if(mc.level==null)return "";StringBuilder out=new StringBuilder();var board=mc.level.getScoreboard();var objective=board.getDisplayObjective(DisplaySlot.SIDEBAR);
  if(objective!=null){out.append(objective.getDisplayName().getString()).append('\n');for(var score:board.listPlayerScores(objective)){out.append(PlayerTeam.formatNameForTeam(board.getPlayersTeam(score.owner()),score.ownerName()).getString()).append('\n');}}
  if(mc.getConnection()!=null)for(var p:mc.getConnection().getListedOnlinePlayers())if(p.getTabListDisplayName()!=null)out.append(p.getTabListDisplayName().getString()).append('\n');
  return out.toString().replaceAll("§.","").toLowerCase(java.util.Locale.ROOT);
 }
 public static boolean island(String text){return text.contains("your island")||text.contains("private island");}
 public static boolean hollows(String t){return t.contains("crystal hollows")||t.contains("mithril deposits")||t.contains("precursor remnants")||t.contains("goblin holdout")||t.contains("crystal nucleus")||t.contains("mines of divan");}
}
