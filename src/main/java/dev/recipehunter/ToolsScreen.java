package dev.recipehunter;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

/** Diary and calculator share the book viewport and native screen-space input bounds. */
public final class ToolsScreen extends Screen {
 private final RecipeHunter app;private final String mode;private BookViewport view;
 private EditBox input,miningId,strength,drops,uptime,speed,fortune;private MultiLineEditBox journal;
 private String result="";private int page,ticks;private boolean draftDirty;
 private final Map<String,String> fields=new HashMap<>();
 private static final int L=32,R=384,COL=302,PER_PAGE=3;
 public ToolsScreen(RecipeHunter app,String mode){super(Component.literal(mode));this.app=app;this.mode=mode;}
 private boolean diary(){return !mode.equals("Calculator");}
 private BookButton button(String s,int x,int y,int w,Runnable action){return addRenderableWidget(new BookButton(s,view.x(x),view.y(y),view.size(w),view.size(21),view.scale(),false,action));}
 private EditBox field(String key,int x,int y,int w,String hint,String fallback){
  EditBox e=addRenderableWidget(new EditBox(font,view.x(x),view.y(y),view.size(w),Math.max(14,view.size(22)),Component.literal(hint)));
  e.setMaxLength(1000);e.setTextColor(Theme.INK);e.setBordered(false);e.setHint(Component.literal(hint));e.setValue(fields.getOrDefault(key,fallback));e.setResponder(value->fields.put(key,value));return e;
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){
  view=BookViewport.fit(width,height,app.book.guiScale);
  button("Back",L,63,76,()->{saveDraft();app.openHomeContent();});
  button(diary()?"Calculator":"Diary",L+84,63,110,()->{saveDraft();app.open(new ToolsScreen(app,diary()?"Calculator":"Craft Diary"));});
  if(!diary()){calculator();return;}
  journal=addRenderableWidget(MultiLineEditBox.builder().setX(view.x(L+7)).setY(view.y(111))
   .setTextColor(Theme.INK).setTextShadow(false).setCursorColor(Theme.INK).setShowBackground(false)
   .setPlaceholder(Component.literal("Write a note...")).build(font,view.size(COL-14),view.size(238),Component.literal("Diary entry")));
  journal.setCharacterLimit(4000);journal.setValue(app.book.diaryDraft);
  journal.setValueListener(value->{app.book.diaryDraft=value;draftDirty=true;});
  button(app.book.diaryEditingKey.isEmpty()?"Save entry":"Save changes",L,361,132,this::saveEntry);
  button("New entry",L+142,361,110,()->{app.book.diaryDraft="";app.book.diaryEditingKey="";draftDirty=true;saveDraft();rebuildWidgets();});
  page=Math.clamp(page,0,pages()-1);
  for(int i=page*PER_PAGE;i<Math.min(app.book.diary.size(),(page+1)*PER_PAGE);i++){
   var e=app.book.diary.get(i);int y=91+(i-page*PER_PAGE)*94;
   button(e.done?"[x]":"[ ]",R,y,28,()->{e.done=!e.done;app.persist();UiSounds.count(app);rebuildWidgets();});
   button("Edit",R+34,y,48,()->{app.book.diaryEditingKey=e.key;app.book.diaryDraft=e.text;draftDirty=true;saveDraft();rebuildWidgets();setFocused(journal);});
   button(e.active?"Exclude":"Include",R+88,y,84,()->{e.active=!e.active;app.persist();rebuildWidgets();});
   button("Delete",R+178,y,65,()->{app.book.diary.remove(e);if(app.book.diaryEditingKey.equals(e.key)){app.book.diaryEditingKey="";app.book.diaryDraft="";}app.persist();rebuildWidgets();});
  }
  button("<",R,391,30,()->turn(-1)).active=page>0;
  button(">",R+COL-30,391,30,()->turn(1)).active=page+1<pages();
 }
 private int pages(){return Math.max(1,(app.book.diary.size()+PER_PAGE-1)/PER_PAGE);}
 private void turn(int delta){int next=BookViewport.page(page,delta,pages());if(next==page)return;page=next;UiSounds.page(app);rebuildWidgets();}
 private void saveEntry(){
  String text=app.book.diaryDraft.trim();if(text.isEmpty()){result="Write something first.";return;}
  CraftDiary.Entry old=app.book.diary.stream().filter(e->e.key.equals(app.book.diaryEditingKey)).findFirst().orElse(null);
  CraftDiary.Entry entry=old==null?CraftDiary.parse(app.catalog,text,System.currentTimeMillis()):CraftDiary.edit(app.catalog,old,text,System.currentTimeMillis());
  int index=old==null?app.book.diary.size():app.book.diary.indexOf(old);
  if(old==null)app.book.diary.add(entry);else app.book.diary.set(index,entry);
  String draft=app.book.diaryDraft,editing=app.book.diaryEditingKey;app.book.diaryDraft="";app.book.diaryEditingKey="";
  if(!app.persist()){if(old==null)app.book.diary.remove(entry);else app.book.diary.set(index,old);app.book.diaryDraft=draft;app.book.diaryEditingKey=editing;result="Could not save. Your text is still here.";return;}
  draftDirty=false;page=index/PER_PAGE;result="Entry saved";UiSounds.page(app);rebuildWidgets();
 }
 private void saveDraft(){if(diary()&&draftDirty){if(app.persist())draftDirty=false;}}
 @Override public void tick(){super.tick();if(++ticks%20==0)saveDraft();}
 @Override public void removed(){saveDraft();super.removed();}
 @Override public void onClose(){saveDraft();app.closeBook();}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){
  if(journal!=null&&journal.isMouseOver(x,y))return journal.mouseScrolled(x,y,sx,sy);
  if(diary()&&x>=view.x(R)&&x<=view.x(R+COL)&&y>=view.y(87)&&y<=view.y(388)){turn(sy<0?1:-1);return true;}
  return super.mouseScrolled(x,y,sx,sy);
 }
 private void calculator(){
  input=field("expression",L,110,COL,"(320 * 160) / 64","");
  button("Calculate",L,146,132,()->{try{result=Double.toString(Calculator.evaluate(input.getValue()));UiSounds.count(app);}catch(RuntimeException e){result="Invalid arithmetic expression.";}});
  miningId=field("material",R,111,COL,"Material ID","");
  speed=field("speed",R,162,140,"Speed",Double.toString(app.estimates.stats.getOrDefault("mining speed",0.0)));
  fortune=field("fortune",R+152,162,150,"Fortune",Double.toString(app.estimates.stats.getOrDefault("mining fortune",0.0)));
  strength=field("strength",R,214,140,"Strength","");drops=field("drops",R+152,214,150,"Base drops","");
  uptime=field("uptime",R,266,140,"Uptime 0..1","");
  button("Use mining estimate",R,309,COL,()->{try{String id=miningId.getValue().trim().toUpperCase(Locale.ROOT);if(!app.catalog.items.containsKey(id))throw new IllegalArgumentException();double r=MiningModel.rate(Double.parseDouble(speed.getValue()),Double.parseDouble(fortune.getValue()),Double.parseDouble(strength.getValue()),Double.parseDouble(drops.getValue()),Double.parseDouble(uptime.getValue()));app.estimates.modelRates.put(id,r);result=String.format(Locale.US,"Approximate: %,.0f %s/hour",r,id);}catch(RuntimeException ex){result="Enter a valid item ID, stats, strength, drops and uptime (0..1).";}});
 }
 private void text(GuiGraphicsExtractor g,String text,int x,int y,int color){g.text(font,text,x,y,color,false);}
 private void box(GuiGraphicsExtractor g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0x187d613d);g.outline(x,y,w,h,0xffa68b62);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
  g.pose().pushMatrix();g.pose().translate(view.left(),view.top());g.pose().scale(view.scale(),view.scale());
  Theme.background(g,font,720,430,"");
  g.pose().pushMatrix();g.pose().translate(L,31);g.pose().scale(1.8f,1.8f);text(g,diary()?"CRAFT DIARY":"CALCULATOR",0,0,Theme.INK);g.pose().popMatrix();
  if(diary()){
   text(g,app.book.diaryEditingKey.isEmpty()?"NEW ENTRY":"EDIT ENTRY",L,95,Theme.INK);text(g,"MY NOTES",R,62,Theme.INK);box(g,L,107,COL,246);
   text(g,app.trim(result,COL),L,396,Theme.MUTED);
   if(app.book.diary.isEmpty())text(g,"Your saved notes show here",R,98,Theme.MUTED);
   text(g,(page+1)+" / "+pages(),R+COL/2-18,398,Theme.MUTED);
   for(int i=page*PER_PAGE;i<Math.min(app.book.diary.size(),(page+1)*PER_PAGE);i++){
    var e=app.book.diary.get(i);int y=91+(i-page*PER_PAGE)*94;
    var lines=font.split(Component.literal(e.text),COL-8);
    for(int n=0;n<Math.min(4,lines.size());n++)g.text(font,lines.get(n),R+3,y+26+n*10,e.done?0xff5f713d:Theme.INK);
    if(lines.size()>4)text(g,"...",R+COL-22,y+56,Theme.MUTED);
    String stamp=java.time.Instant.ofEpochMilli(e.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    String info=e.done?"Done":!e.active?"Excluded":e.id.isEmpty()?stamp:e.count+"x "+app.catalog.item(e.id).name+" | "+CraftDiary.remaining(e,System.currentTimeMillis());
    text(g,app.trim(info,COL-6),R+3,y+70,Theme.MUTED);g.fill(R,y+86,R+COL,y+87,0x337e6747);
   }
  }else{
   text(g,"EXPRESSION",L,96,Theme.MUTED);box(g,L,107,COL,29);g.textWithWordWrap(font,Component.literal(result),L,184,COL,Theme.INK);
   text(g,"MINING ESTIMATE",R,62,Theme.INK);text(g,"Material",R,96,Theme.MUTED);text(g,"Mining speed",R,146,Theme.MUTED);text(g,"Mining fortune",R+152,146,Theme.MUTED);text(g,"Block strength",R,198,Theme.MUTED);text(g,"Base drops",R+152,198,Theme.MUTED);text(g,"Uptime (0..1)",R,250,Theme.MUTED);
   box(g,R,107,COL,29);for(int y:new int[]{158,210}){box(g,R,y,140,29);box(g,R+152,y,150,29);}box(g,R,262,140,29);
   g.textWithWordWrap(font,Component.literal("Estimate from entered assumptions. Excludes Pristine, travel and random drops."),R,348,COL,Theme.MUTED);
  }
  g.pose().popMatrix();super.extractRenderState(g,mx,my,dt);
 }
}
