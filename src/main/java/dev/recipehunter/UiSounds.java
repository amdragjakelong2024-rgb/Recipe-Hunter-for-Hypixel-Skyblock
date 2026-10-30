package dev.recipehunter;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
public final class UiSounds {
 private static void play(RecipeHunter app,SoundEvent sound,float pitch,float volume){if(app.book.uiSounds)app.mc.getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,volume));}
 public static void notification(RecipeHunter app){app.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP,1.35f,.25f));}
 public static void open(RecipeHunter app){play(app,SoundEvents.BOOK_PAGE_TURN,.8f,.65f);}
 public static void close(RecipeHunter app){play(app,SoundEvents.BOOK_PUT,.9f,.6f);}
 public static void page(RecipeHunter app){play(app,SoundEvents.BOOK_PAGE_TURN,1f,.55f);}
 public static void count(RecipeHunter app){play(app,SoundEvents.EXPERIENCE_ORB_PICKUP,1.35f,.25f);}
 public static void craft(RecipeHunter app){play(app,SoundEvents.SMITHING_TABLE_USE,1f,.35f);}
}
