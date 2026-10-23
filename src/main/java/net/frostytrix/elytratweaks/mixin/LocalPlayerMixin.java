package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // All flight logic lives in ElytraController; run it at the start of every client tick,
    // before vanilla reads the jump key and moves the player.
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void elytraTweaks$onAiStep(CallbackInfo ci) {
        ElytraController.tick((LocalPlayer) (Object) this);
    }
}
