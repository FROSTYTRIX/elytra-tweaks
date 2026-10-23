package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ClientModEvents;
import net.frostytrix.elytratweaks.Config;
import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // Whether the current flight is one we want: started by the deploy key, or
    // accepted by the "jumpDeploy" setting.
    @Unique
    private boolean elytraTweaks$wantGliding = false;

    // Deploy key state on the previous tick, used by HOLD mode to detect a release.
    @Unique
    private boolean elytraTweaks$keyWasDown = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void elytraTweaks$onAiStep(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        // Clear intent once the flight is over (landed, water, server stopped it...).
        if (!player.isFallFlying()) {
            this.elytraTweaks$wantGliding = false;
        }

        KeyMapping key = ClientModEvents.ELYTRA_DEPLOY_KEY;
        if (key != null) {
            if (Config.DEPLOY_KEY_MODE.get() == Config.DeployKeyMode.HOLD) {
                // HOLD only looks at whether the key is down, so drop queued presses.
                while (key.consumeClick()) {
                }
                boolean down = key.isDown();
                if (down && !player.isFallFlying()) {
                    // tryToStartFallFlying refuses on the ground, so holding the key
                    // while falling deploys as soon as gliding is possible.
                    this.elytraTweaks$deploy(player);
                } else if (!down && this.elytraTweaks$keyWasDown && player.isFallFlying()) {
                    this.elytraTweaks$stop(player);
                }
                this.elytraTweaks$keyWasDown = down;
            } else {
                while (key.consumeClick()) {
                    if (player.isFallFlying()) {
                        this.elytraTweaks$stop(player);
                    } else {
                        this.elytraTweaks$deploy(player);
                    }
                }
                this.elytraTweaks$keyWasDown = false;
            }
        }

        // Layer 2 (reactive safety net): we're gliding but never asked to be. If the
        // jump setting allows this kind of deploy, accept it once; otherwise fold the
        // wings. Client-only, no stop packet, so it never traps the player in the
        // prone/crawling hitbox on the server.
        if (Config.SAFETY_NET.get() && player.isFallFlying() && !this.elytraTweaks$wantGliding) {
            if (ElytraTweaksState.isNonKeyDeployAllowed(player)) {
                this.elytraTweaks$wantGliding = true;
            } else {
                player.stopFallFlying();
            }
        }
    }

    // The server treats START_FALL_FLYING as "start if possible, else stop", deciding
    // from its own flag. Sending it on BOTH directions keeps the client and server in
    // phase, so stopping mid-air is a single press.
    @Unique
    private void elytraTweaks$deploy(LocalPlayer player) {
        // isCustomPress lets this one call through PlayerEntityMixin's client-side block.
        ElytraTweaksState.isCustomPress = true;
        try {
            if (player.tryToStartFallFlying()) {
                this.elytraTweaks$wantGliding = true;
                player.connection.send(new ServerboundPlayerCommandPacket(
                        player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } finally {
            ElytraTweaksState.isCustomPress = false;
        }
    }

    @Unique
    private void elytraTweaks$stop(LocalPlayer player) {
        this.elytraTweaks$wantGliding = false;
        player.stopFallFlying();
        player.connection.send(new ServerboundPlayerCommandPacket(
                player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }
}
