package games.finn.dualstarters.mixin;

import com.cobblemon.mod.common.client.net.starter.StarterUIPacketHandler;
import com.cobblemon.mod.common.net.messages.client.starter.OpenStarterUIPacket;

import games.finn.dualstarters.client.StarterPickerScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces Cobblemon's single-starter selection screen with the Dual Starters picker.
 *
 * <p>This is the single interception point: Cobblemon only sends
 * {@link OpenStarterUIPacket} to a client when the server-side flow decided the player
 * should choose a starter (i.e. {@code !starterSelected && !starterLocked}). So by taking
 * over the open here, we inherit Cobblemon's "new player" gate and its trigger (the same
 * keybind/flow that opens the normal starter menu), and reopening works for free.</p>
 *
 * <p>{@code remap = false} because the target is a Cobblemon class, not a Minecraft class;
 * its names are literal and must not be run through Minecraft's mapping.</p>
 */
@Mixin(value = StarterUIPacketHandler.class, remap = false)
public class StarterUIPacketHandlerMixin {

    @Inject(
            method = "handle(Lcom/cobblemon/mod/common/net/messages/client/starter/OpenStarterUIPacket;Lnet/minecraft/client/Minecraft;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dualstarters$openDualPicker(OpenStarterUIPacket packet, Minecraft minecraft, CallbackInfo ci) {
        // Ensure the screen is opened on the client thread, then suppress Cobblemon's own screen.
        minecraft.execute(() -> minecraft.setScreen(new StarterPickerScreen(packet.getCategories())));
        ci.cancel();
    }
}
