package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ClientModEvents;
import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStepHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        while (ClientModEvents.ELYTRA_DEPLOY_KEY.consumeClick()) {
            if (player.isFallFlying()) {
                player.stopFallFlying();

                player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            } else {
                ElytraTweaksState.isCustomPress = true;

                if (player.tryToStartFallFlying()) {
                    player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
                ElytraTweaksState.isCustomPress = false;
            }
        }
    }
}