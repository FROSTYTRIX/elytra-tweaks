package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setSharedFlag", at = @At("HEAD"), cancellable = true)
    private void onSetSharedFlag(int flag, boolean value, CallbackInfo ci) {
        // Flag 7 is the internal entity flag for Fall Flying (Gliding)
        if (flag == 7 && value) {
            Entity entity = (Entity) (Object) this;

            // Only enforce this restriction on the client-side player
            if (entity.level().isClientSide() && entity instanceof Player) {
                if (!ElytraTweaksState.isCustomPress) {
                    // Slam the door shut on any unauthorized attempt to activate flight state
                    ci.cancel();
                }
            }
        }
    }
}