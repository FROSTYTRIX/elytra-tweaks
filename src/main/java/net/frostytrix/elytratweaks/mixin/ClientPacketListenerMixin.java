package net.frostytrix.elytratweaks.mixin;

import net.frostytrix.elytratweaks.ElytraTweaksState;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl; // <-- New Import
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Target the parent class where send() actually lives
@Mixin(value = ClientCommonPacketListenerImpl.class, priority = 0)
public abstract class ClientPacketListenerMixin {

    // Using the explicit signature prevents ambiguity if there are overloaded send methods
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {

            // Intercepting the explicit command to start fall flying
            if (commandPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING) {
                if (!ElytraTweaksState.isCustomPress) {

                    // Stop the packet from ever reaching the server if it wasn't triggered by 'G'
                    ci.cancel();
                }
            }
        }
    }
}