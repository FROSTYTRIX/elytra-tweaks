package net.frostytrix.elytratweaks;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ElytraTweaksMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    public static final KeyMapping.Category ELYTRA_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(ElytraTweaksMod.MOD_ID, "controls")
    );

    public static KeyMapping ELYTRA_DEPLOY_KEY;
    public static KeyMapping TAKEOFF_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(ELYTRA_CATEGORY);

        ELYTRA_DEPLOY_KEY = new KeyMapping(
                "key.elytra_tweaks.deploy",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                ELYTRA_CATEGORY
        );
        // Unbound by default: the takeoff uses a firework automatically, which some
        // servers' anti-cheat may not like, so players opt in by binding it.
        TAKEOFF_KEY = new KeyMapping(
                "key.elytra_tweaks.takeoff",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                ELYTRA_CATEGORY
        );

        event.register(ELYTRA_DEPLOY_KEY);
        event.register(TAKEOFF_KEY);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(ElytraTweaksMod.MOD_ID, "flight_hud"),
                (graphics, deltaTracker) -> FlightHud.render(canvas(graphics)));
    }

    private static HudCanvas canvas(GuiGraphicsExtractor graphics) {
        return new HudCanvas() {
            @Override
            public int width() {
                return graphics.guiWidth();
            }

            @Override
            public int height() {
                return graphics.guiHeight();
            }

            @Override
            public void fill(int x0, int y0, int x1, int y1, int argb) {
                graphics.fill(x0, y0, x1, y1, argb);
            }

            @Override
            public void text(String text, int x, int y, int argb) {
                graphics.text(Minecraft.getInstance().font, text, x, y, argb, true);
            }
        };
    }
}
