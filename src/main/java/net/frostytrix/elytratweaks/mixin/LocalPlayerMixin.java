package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ClientModEvents;
import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // Tracks whether the player asked to be gliding via the mod's deploy key.
    @Unique
    private boolean elytraTweaks$wantGliding = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void elytraTweaks$onAiStep(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        // Clear intent once we're back on the ground.
        if (player.onGround()) {
            this.elytraTweaks$wantGliding = false;
        }

        // Handle the mod's dedicated deploy key (default: Left Alt).
        if (ClientModEvents.ELYTRA_DEPLOY_KEY != null) {
            while (ClientModEvents.ELYTRA_DEPLOY_KEY.consumeClick()) {
                // The server treats START_FALL_FLYING as "start if possible, else stop",
                // deciding from its own flag. Sending it on BOTH toggle directions keeps
                // the client and server in phase, so stopping mid-air is a single press.
                if (player.isFallFlying()) {
                    // Toggle OFF mid-air.
                    this.elytraTweaks$wantGliding = false;
                    player.stopFallFlying();
                    player.connection.send(new ServerboundPlayerCommandPacket(
                            player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                } else {
                    // Toggle ON. isCustomPress lets our deploy through PlayerEntityMixin's
                    // client-side block for exactly this one call.
                    ElytraTweaksState.isCustomPress = true;
                    if (player.tryToStartFallFlying()) {
                        this.elytraTweaks$wantGliding = true;
                        player.connection.send(new ServerboundPlayerCommandPacket(
                                player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    }
                    ElytraTweaksState.isCustomPress = false;
                }
            }
        }

        // Layer 2 (reactive safety net): if we're gliding but never asked to be, fold
        // the wings. This catches any deploy path that slipped past the proactive block
        // (odd modpack interactions). Client-only, no stop packet — so it never traps the
        // player in the prone/crawling hitbox on the server.
        if (!this.elytraTweaks$wantGliding && player.isFallFlying()) {
            player.stopFallFlying();
        }
    }
}
