package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Priority 1 ensures we are the absolute first to intercept this method call
@Mixin(value = Player.class, priority = 1)
public abstract class PlayerMixin {

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void onTryToStartFallFlying(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;

        // Ensure we only block it for the client-side player
        if (player.level().isClientSide()) {

            // If the deployment did not originate from our 'G' key
            if (!ElytraTweaksState.isCustomPress) {
                // Cancel the method instantly. The elytra will not deploy.
                cir.setReturnValue(false);
            }
        }
    }
}