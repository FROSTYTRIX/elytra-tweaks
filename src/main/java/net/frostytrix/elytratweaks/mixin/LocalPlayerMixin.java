package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ClientModEvents;
import net.frostytrix.elytratweaks.ElytraTweaksModClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // Tracks if the flight was started by your 'G' key
    @Unique
    private boolean elytraTweaks$isLegitFlight = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStepHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        // 1. Reset our legit state when you land or enter water
        if (!player.isFallFlying()) {
            this.elytraTweaks$isLegitFlight = false;
        }

        // 2. Handle the Custom 'G' Key
        if (ClientModEvents.ELYTRA_DEPLOY_KEY != null) {
            while (ClientModEvents.ELYTRA_DEPLOY_KEY.consumeClick()) {
                if (player.isFallFlying()) {
                    // Turn OFF: We must manually send the toggle packet to stop
                    player.stopFallFlying();
                    player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    this.elytraTweaks$isLegitFlight = false;
                } else {
                    // Turn ON: tryToStartFallFlying() automatically sends the network packet for us!
                    if (player.tryToStartFallFlying()) {
                        this.elytraTweaks$isLegitFlight = true;
                    }
                }
            }
        }

        // 3. The Enforcer (The Anti-Spacebar Weapon)
        // If the spacebar triggered flight, 'isLegitFlight' will be false.
        if (player.isFallFlying() && !this.elytraTweaks$isLegitFlight) {
            // Fold the wings instantly
            player.stopFallFlying();
            // Send the toggle packet to force the server to stop the spacebar flight too
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }
}