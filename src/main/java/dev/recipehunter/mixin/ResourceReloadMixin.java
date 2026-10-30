package dev.recipehunter.mixin;
import dev.recipehunter.Icons;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;
@Mixin(Minecraft.class)
public abstract class ResourceReloadMixin {
 @Inject(method="reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"))
 private void recipehunter$reload(CallbackInfoReturnable<CompletableFuture<Void>> ci){Icons.clear();}
}
