package net.frostytrix.elytratweaks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class ElytraTweaksState {
    // Set to true only while the mod's own deploy key is starting fall flight,
    // so PlayerEntityMixin can tell an intentional deploy from every other
    // (vanilla jump key / other mod) attempt and block the latter on the client.
    public static boolean isCustomPress = false;

    // Whether a deploy that did NOT come from the deploy key is allowed by the
    // "jumpDeploy" setting. Client-side only: reads the client config.
    public static boolean isNonKeyDeployAllowed(Player player) {
        return switch (Config.JUMP_DEPLOY.get()) {
            case BLOCKED -> false;
            case WITH_FIREWORK -> player.getMainHandItem().is(Items.FIREWORK_ROCKET)
                    || player.getOffhandItem().is(Items.FIREWORK_ROCKET);
            case VANILLA -> true;
        };
    }
}
