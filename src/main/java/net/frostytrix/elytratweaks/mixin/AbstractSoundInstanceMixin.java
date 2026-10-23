package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.Config;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ElytraOnPlayerSoundInstance doesn't override getVolume(), so the gliding wind
// volume is read through AbstractSoundInstance. Scale it by the "windVolume" setting.
@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin {

    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void elytraTweaks$scaleElytraWind(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ElytraOnPlayerSoundInstance) {
            cir.setReturnValue(cir.getReturnValue() * (Config.WIND_VOLUME.get() / 100.0F));
        }
    }
}
