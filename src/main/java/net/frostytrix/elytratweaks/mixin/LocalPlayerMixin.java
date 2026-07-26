package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ClientModEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // Handle the mod's dedicated deploy key (default: Left Alt)
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void elytraTweaks$onAiStep(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (ClientModEvents.ELYTRA_DEPLOY_KEY == null) return;

        while (ClientModEvents.ELYTRA_DEPLOY_KEY.consumeClick()) {
            // The server treats START_FALL_FLYING as "start if possible, else stop",
            // deciding from its own flag. We must send it on BOTH toggle directions so
            // the client and server flags never drift out of phase — otherwise the first
            // stop press only re-syncs the server and you'd have to press again.
            if (player.isFallFlying()) {
                // Toggle OFF mid-air: fold locally, then tell the server to stop.
                player.stopFallFlying();
                player.connection.send(new ServerboundPlayerCommandPacket(
                        player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            } else {
                // Toggle ON: set the client flag, then tell the server to start.
                if (player.tryToStartFallFlying()) {
                    player.connection.send(new ServerboundPlayerCommandPacket(
                            player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            }
        }
    }

    // Block the vanilla jump/spacebar from ever deploying the elytra.
    // Only affects the call inside aiStep — the mod's own keybind call is untouched.
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z")
    )
    private boolean elytraTweaks$blockVanillaSpacebarDeploy(LocalPlayer instance) {
        return false;
    }
}
