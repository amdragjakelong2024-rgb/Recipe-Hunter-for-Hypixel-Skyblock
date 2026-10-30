package dev.recipehunter.mixin;
import dev.recipehunter.RecipeHunter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class ServerMessageMixin {
 @Inject(method="handleSystemChat",at=@At("TAIL"))
 private void recipehunter$observe(ClientboundSystemChatPacket packet,CallbackInfo ci){var app=RecipeHunter.INSTANCE;if(app!=null)app.serverMessage(packet.content(),packet.overlay());}
 @Inject(method={"handleContainerSetSlot","handleContainerContent"},at=@At("TAIL"))
 private void recipehunter$inventory(CallbackInfo ci){dev.recipehunter.Icons.invalidateInventory();}
}
