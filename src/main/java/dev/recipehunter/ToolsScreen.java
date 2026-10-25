package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

public final class ToolsScreen extends Screen {
 private final RecipeHunter app;private final String mode;private EditBox input,model,key,miningId,strength,drops,uptime,speed,fortune;private String result="";private int page;private float scale=1;
 public ToolsScreen(RecipeHunter a,String m){super(Component.literal(m));app=a;mode=m;}
 private void button(String s,int x,int y,int w,Runnable r){addRenderableWidget(new GlowButton(s,x,y,w,r));}
 private EditBox field(int x,int y,int w,String hint){EditBox e=addRenderableWidget(new EditBox(font,x,y,w,20,Component.literal(hint)));e.setMaxLength(1000);e.setHint(Component.literal(hint));return e;}
 @Override protected void init(){
  int aw=app.mc.getWindow().getGuiScaledWidth(),ah=app.mc.getWindow().getGuiScaledHeight();scale=(float)Math.min(1,Math.min(aw/520.0,ah/360.0));width=(int)(aw/scale);height=(int)(ah/scale);
  button("Home",8,27,60,app::openHome);button("Diary",72,27,60,()->app.open(new ToolsScreen(app,"Craft Diary")));button("Calc",136,27,60,()->app.open(new ToolsScreen(app,"Calculator")));button("AI",200,27,45,()->app.open(new ToolsScreen(app,"AI")));
  if(mode.equals("Calculator")){input=field(8,60,width-16,"(320 * 160) / 64");button("Calculate",8,85,100,()->{try{result=Double.toString(Calculator.evaluate(input.getValue()));}catch(RuntimeException e){result="Invalid arithmetic expression.";}});
   miningId=field(8,151,width-16,"Material ID, e.g. MITHRIL (model excludes Pristine/RNG)");
   speed=field(8,177,100,"Mining speed");speed.setValue(Double.toString(app.estimates.stats.getOrDefault("mining speed",0.0)));
   fortune=field(114,177,100,"Mining fortune");fortune.setValue(Double.toString(app.estimates.stats.getOrDefault("mining fortune",0.0)));
   strength=field(8,203,100,"Block strength");drops=field(114,203,100,"Base drops");uptime=field(220,203,100,"Uptime 0..1");
   button("Use mining estimate",8,229,160,()->{try{String id=miningId.getValue().trim().toUpperCase(Locale.ROOT);if(!app.catalog.items.containsKey(id))throw new IllegalArgumentException();double r=MiningModel.rate(Double.parseDouble(speed.getValue()),Double.parseDouble(fortune.getValue()),Double.parseDouble(strength.getValue()),Double.parseDouble(drops.getValue()),Double.parseDouble(uptime.getValue()));app.estimates.modelRates.put(id,r);result=String.format(Locale.US,"Approximate model: %,.0f %s/hour",r,id);}catch(RuntimeException ex){result="Enter valid item ID, stats, block strength, base drops and uptime (0..1).";}});return;}
  if(mode.equals("AI")){
   button(app.ai.provider.name(),8,58,110,()->{app.ai.switchProvider();rebuildWidgets();});
   model=field(124,58,Math.max(80,width-132),"Provider model ID");model.setValue(app.ai.model);
   key=field(8,84,width-16,app.ai.configured()?"Saved API key (leave blank to keep)":"API key");key.addFormatter((text,i)->Component.literal("*".repeat(text.length())).getVisualOrderText());
   button("Save settings",8,110,100,()->applyAi());
   button("Test connection",114,110,120,()->{applyAi();app.ai.testConnection();});
   button("Clear key",240,110,85,()->{app.ai.clearKey();key.setValue("");});
   button("Clear chat",331,110,85,()->app.ai.clearChat());
   input=field(8,height-56,width-16,"Ask about your goals, materials or grind priorities...");input.setMaxLength(2000);
   button("Send (API billing)",8,height-30,140,()->{if(app.ai.busy||input.getValue().isBlank())return;applyAi();app.ai.request(summary(),input.getValue());input.setValue("");page=0;});
   button("Analyze goals",154,height-30,110,()->{if(app.ai.busy)return;applyAi();app.ai.request(summary());page=0;});return;
  }

  input=field(8,58,width-16,"2 Refined Mithril forge 4h (or any note)");
  button("Save note",8,84,92,()->{String text=input.getValue().trim();if(text.isEmpty())return;var e=CraftDiary.parse(app.catalog,text,System.currentTimeMillis());app.book.diary.add(e);app.persist();result=e.id.isEmpty()?"Saved as a note; no automatic deduction.":"Reserved "+e.count+"x "+app.catalog.item(e.id).name+". Verify the parsed job below.";input.setValue("");rebuildWidgets();});
  button("<",106,84,25,()->{page=Math.max(0,page-1);rebuildWidgets();});button(">",136,84,25,()->{page++;rebuildWidgets();});
  int per=Math.max(1,(height-165)/34);page=Math.min(page,Math.max(0,(app.book.diary.size()-1)/per));
  for(int i=page*per;i<Math.min(app.book.diary.size(),(page+1)*per);i++){var e=app.book.diary.get(i);int y=139+(i-page*per)*34;
   button(e.active?"Exclude":"Include",8,y,67,()->{e.active=!e.active;app.persist();rebuildWidgets();});button("Delete",79,y,55,()->{app.book.diary.remove(e);app.persist();rebuildWidgets();});
  }
 }
 private void applyAi(){if(app.ai.busy)return;app.ai.model=model.getValue().trim();if(!key.getValue().isBlank())app.ai.key(key.getValue());app.ai.save();key.setValue("");}
 public String summary(){
  StringBuilder s=new StringBuilder("Observed stats: "+app.estimates.stats+"\n");
  for(var goal:app.book.goals){s.append("Goal ").append(goal.count).append("x ").append(app.catalog.item(goal.id).name).append('\n');for(var e:app.estimates.rank(app,goal,false))s.append(e.label(app.catalog)).append('\n');}
  for(var e:app.book.diary)s.append("User note (active=").append(e.active).append("): ").append(e.text).append('\n');
  for(var e:app.menus.forge)s.append("Forge: ").append(e.count).append("x ").append(e.id).append(" ").append(CraftDiary.remaining(e,System.currentTimeMillis())).append('\n');return s.toString();
 }
 private net.minecraft.client.input.MouseButtonEvent scaled(net.minecraft.client.input.MouseButtonEvent e){return new net.minecraft.client.input.MouseButtonEvent(e.x()/scale,e.y()/scale,e.buttonInfo());}
 @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){return super.mouseClicked(scaled(e),twice);}
 @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent e){return super.mouseReleased(scaled(e));}
 @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent e,double dx,double dy){return super.mouseDragged(scaled(e),dx/scale,dy/scale);}
 @Override public boolean isPauseScreen(){return false;}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){page=Math.max(0,page+(sy<0?1:-1));if(!mode.equals("AI"))rebuildWidgets();return true;}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  g.pose().pushMatrix();g.pose().scale(scale,scale);try{
  mx=(int)(mx/scale);my=(int)(my/scale);Theme.background(g,font,width,height,"RECIPE HUNTER / "+mode);super.extractRenderState(g,mx,my,dt);
  if(mode.equals("Calculator")){g.textWithWordWrap(font,Component.literal(result),8,112,width-16,0xffdceee4);g.text(font,"Optional mining assumptions (not a measured rate):",8,137,0xffefc77d,false);return;}
  if(mode.equals("AI")){
   g.textWithWordWrap(font,Component.literal("Optional paid request. Plus does not include API credit. Sends goals, observed stats and diary notes to the selected provider. Credentials are saved privately on this device. Do not share the private settings folder."),8,138,width-16,0xffefc77d);
   g.text(font,app.trim(app.ai.busy?"Waiting for provider...":app.ai.status,width-16),8,173,0xffaacbbb,false);
   List<net.minecraft.util.FormattedCharSequence> lines=font.split(Component.literal(app.ai.answer),width-16);int per=Math.max(1,(height-250)/11);int start=Math.min(page*per,Math.max(0,lines.size()-1));for(int i=start;i<Math.min(lines.size(),start+per);i++)g.text(font,lines.get(i),8,185+(i-start)*11,0xffdceee4);return;
  }
  g.text(font,app.trim(result,width-16),8,113,0xffefc77d,false);int per=Math.max(1,(height-165)/34);
  for(int i=page*per;i<Math.min(app.book.diary.size(),(page+1)*per);i++){var e=app.book.diary.get(i);int y=139+(i-page*per)*34;g.text(font,app.trim(e.text,width-146),140,y,0xffdceee4,false);g.text(font,app.trim((e.id.isEmpty()?"Note":e.count+"x "+e.id)+" | "+CraftDiary.remaining(e,System.currentTimeMillis()),width-146),140,y+12,0xffaacbbb,false);}
  }finally{g.pose().popMatrix();}
 }
}
