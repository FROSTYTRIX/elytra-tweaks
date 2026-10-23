package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Layer 1 (proactive): block every client-side attempt to start fall flying that
// wasn't initiated by the mod's deploy key, unless the "jumpDeploy" setting allows it.
// Because this gates the method itself (not a single call site), it catches the
// vanilla jump key deploy AND any other mod that starts gliding through
// tryToStartFallFlying.
@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void elytraTweaks$blockUnintendedDeploy(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        // Only interfere on the client. The server must still process the deploy
        // packet the mod sends, otherwise legitimate flight would never start.
        if (self.level().isClientSide()
                && !ElytraTweaksState.isCustomPress
                && !ElytraTweaksState.isNonKeyDeployAllowed(self)) {
            cir.setReturnValue(false);
        }
    }
}
