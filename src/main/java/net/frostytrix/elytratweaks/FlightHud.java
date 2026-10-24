package net.frostytrix.elytratweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Small overlay just below-right of the crosshair while gliding: speed, altitude,
// pitch, firework count and the glider's durability.
public final class FlightHud {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int WARNING_COLOR = 0xFFFFFF55;
    private static final int DANGER_COLOR = 0xFFFF5555;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 3;

    private FlightHud() {
    }

    public static void render(HudCanvas canvas) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !player.isFallFlying() || !Config.FLIGHT_HUD.get()) {
            return;
        }

        List<Line> lines = new ArrayList<>();
        // Delta movement is in blocks per tick; there are 20 ticks per second.
        double speed = player.getDeltaMovement().length() * 20.0;
        lines.add(new Line(tr("speed", String.format(Locale.ROOT, "%.1f", speed)), TEXT_COLOR));
        lines.add(new Line(tr("altitude", (int) Math.floor(player.getY())), TEXT_COLOR));
        // Minecraft's pitch is negative when looking up; show nose-up as positive.
        lines.add(new Line(tr("pitch", String.format(Locale.ROOT, "%+.0f", -player.getXRot())), TEXT_COLOR));
        // Hands, hotbar and main inventory.
        int rockets = player.getInventory().countItem(Items.FIREWORK_ROCKET);
        lines.add(new Line(tr("rockets", rockets), rockets == 0 ? WARNING_COLOR : TEXT_COLOR));

        ItemStack glider = VersionCompat.getGliderStack(player);
        if (!glider.isEmpty() && glider.isDamageableItem()) {
            int remaining = ElytraController.remainingDurability(glider);
            int color = ElytraController.isLowDurability(glider) ? DANGER_COLOR
                    : remaining * 4 <= glider.getMaxDamage() ? WARNING_COLOR
                    : TEXT_COLOR;
            lines.add(new Line(tr("durability", remaining, glider.getMaxDamage()), color));
        }

        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, minecraft.font.width(line.text()));
        }
        int x = canvas.width() / 2 + 12;
        int y = canvas.height() / 2 + 8;
        canvas.fill(x - PADDING, y - PADDING, x + width + PADDING, y + lines.size() * LINE_HEIGHT + PADDING - 2, BACKGROUND_COLOR);
        for (int i = 0; i < lines.size(); i++) {
            canvas.text(lines.get(i).text(), x, y + i * LINE_HEIGHT, lines.get(i).color());
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(ElytraTweaksMod.MOD_ID + ".hud." + key, args).getString();
    }

    private record Line(String text, int color) {
    }
}
