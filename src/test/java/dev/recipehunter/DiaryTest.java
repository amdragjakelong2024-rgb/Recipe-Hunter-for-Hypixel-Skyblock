package dev.recipehunter;
import java.nio.file.*;
import java.util.*;
final class DiaryTest {
 static void run(Catalog c)throws Exception{
  var original=CraftDiary.parse(c,"forge 2 Refined Mithril 4h",1000);
  var changed=CraftDiary.edit(c,original,"forge 2 Refined Mithril 2h",3601000);
  var same=CraftDiary.edit(c,changed,"forge 2 Refined Mithril 2h",5401000);
  PlannerTest.eq(same.readyAt,changed.readyAt,"editing an unchanged duration never restarts timer");
  PlannerTest.check(same.key.equals(original.key)&&same.createdAt==original.createdAt,"edits preserve identity and date");
  same.done=true;PlannerTest.check(CraftDiary.pending(List.of(same)).isEmpty(),"done note no longer reserves materials");same.done=false;same.active=false;PlannerTest.check(CraftDiary.pending(List.of(same)).isEmpty(),"excluded note does not reserve materials");
  Book book=new Book();book.diary.add(same);book.diaryDraft="Unfinished note\nsecond line";book.diaryEditingKey=same.key;
  Path dir=Files.createTempDirectory("rh-diary-test"),file=dir.resolve("book.json");book.write(file);var copy=Book.read(file);
  PlannerTest.check(copy.diaryDraft.equals(book.diaryDraft)&&copy.diaryEditingKey.equals(same.key),"draft and edit target survive restart");PlannerTest.check(copy.diary.getFirst().text.equals(same.text),"full entry persisted");
  Files.writeString(dir.resolve("credentials.bin"),"dummy");Files.writeString(dir.resolve("device.key"),"dummy");Files.writeString(dir.resolve("unrelated.txt"),"keep");RetiredSettings.remove(dir);
  PlannerTest.check(!Files.exists(dir.resolve("credentials.bin"))&&!Files.exists(dir.resolve("device.key"))&&Files.exists(dir.resolve("unrelated.txt")),"retired settings cleanup preserves unrelated files");
  try(var paths=Files.list(dir)){for(var p:paths.toList())Files.delete(p);}Files.delete(dir);
 }
}
