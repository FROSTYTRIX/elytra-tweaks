package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Layer 1 (proactive): block every client-side attempt to start fall flying that
// wasn't initiated by the mod's deploy key. Because this gates the method itself
// (not a single call site), it catches the vanilla spacebar deploy AND any other
// mod that tries to start gliding through tryToStartFallFlying — which the old
// call-site @Redirect could not do in a heavy modpack.
@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void elytraTweaks$blockUnintendedDeploy(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        // Only interfere on the client. The server must still process the deploy
        // packet the mod sends, otherwise legitimate flight would never start.
        if (self.level().isClientSide() && !ElytraTweaksState.isCustomPress) {
            cir.setReturnValue(false);
        }
    }
}
