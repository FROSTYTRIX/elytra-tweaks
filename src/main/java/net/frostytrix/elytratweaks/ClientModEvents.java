package net.frostytrix.elytratweaks;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ElytraTweaksMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    public static final KeyMapping.Category ELYTRA_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(ElytraTweaksMod.MOD_ID, "controls")
    );

    public static KeyMapping ELYTRA_DEPLOY_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(ELYTRA_CATEGORY);

        ELYTRA_DEPLOY_KEY = new KeyMapping(
                "key.elytra_tweaks.deploy",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                ELYTRA_CATEGORY
        );

        event.register(ELYTRA_DEPLOY_KEY);
    }
}