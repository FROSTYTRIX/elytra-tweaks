package net.frostytrix.elytratweaks.mixin;

import java.util.List;

import net.frostytrix.elytratweaks.ElytraController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    @Final
    protected static EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID;

    // This overload only runs for data received from the server (SynchedEntityData.assignValues),
    // after it has been applied. When it carries our own player's flags, the gliding flag is
    // exactly what the server has, so pass it on to ElytraController.
    @Inject(method = "onSyncedDataUpdated(Ljava/util/List;)V", at = @At("TAIL"))
    private void elytraTweaks$onServerData(List<SynchedEntityData.DataValue<?>> values, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player) {
            for (SynchedEntityData.DataValue<?> value : values) {
                if (value.id() == DATA_SHARED_FLAGS_ID.id()) {
                    ElytraController.onServerGlidingState(player, player.isFallFlying());
                    return;
                }
            }
        }
    }
}
