package dev.recipehunter;
import java.nio.file.*;
import java.io.IOException;

/** Removes only the obsolete provider files owned by this mod, without reading secrets. */
final class RetiredSettings {
 static void remove(Path dir)throws IOException{
  if(Files.isSymbolicLink(dir))return;
  for(String name:new String[]{"credentials.bin","credentials.tmp","device.key"})Files.deleteIfExists(dir.resolve(name));
 }
}
