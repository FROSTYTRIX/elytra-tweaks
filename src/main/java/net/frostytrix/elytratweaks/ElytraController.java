package net.frostytrix.elytratweaks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Client-side flight logic, run once per tick by LocalPlayerMixin at the start of aiStep,
// before vanilla reads the jump key and moves the player.
public final class ElytraController {
    // How long a firework takeoff may wait to leave the ground before giving up.
    private static final int TAKEOFF_TIMEOUT_TICKS = 20;

    private static LocalPlayer lastPlayer;
    // Whether the current flight is one we want: started by the deploy key or a takeoff,
    // or accepted by the "jumpDeploy" setting.
    private static boolean wantGliding;
    // Deploy key state on the previous tick, used by HOLD mode to detect a release.
    private static boolean deployKeyWasDown;
    // Ticks since a firework takeoff started, or -1 when none is in progress.
    private static int takeoffTicks = -1;
    private static boolean durabilityWarned;

    private ElytraController() {
    }

    public static void tick(LocalPlayer player) {
        if (player != lastPlayer) {
            // New world or respawn: forget the previous player's state.
            lastPlayer = player;
            wantGliding = false;
            deployKeyWasDown = false;
            takeoffTicks = -1;
            durabilityWarned = false;
        }

        // Clear per-flight state once the flight is over (landed, water, server stopped it...).
        if (!player.isFallFlying()) {
            wantGliding = false;
            durabilityWarned = false;
        }

        handleDeployKey(player);
        handleTakeoff(player);
        applySafetyNet(player);
        warnLowDurability(player);
    }

    private static void handleDeployKey(LocalPlayer player) {
        KeyMapping key = ClientModEvents.ELYTRA_DEPLOY_KEY;
        if (key == null) {
            return;
        }
        if (Config.DEPLOY_KEY_MODE.get() == Config.DeployKeyMode.HOLD) {
            // HOLD only looks at whether the key is down, so drop queued presses.
            while (key.consumeClick()) {
            }
            boolean down = key.isDown();
            if (down && !player.isFallFlying()) {
                // tryToStartFallFlying refuses on the ground, so holding the key
                // while falling deploys as soon as gliding is possible.
                deploy(player);
            } else if (!down && deployKeyWasDown && player.isFallFlying()) {
                stop(player);
            }
            deployKeyWasDown = down;
        } else {
            while (key.consumeClick()) {
                if (player.isFallFlying()) {
                    stop(player);
                } else {
                    deploy(player);
                }
            }
            deployKeyWasDown = false;
        }
    }

    // Firework takeoff: jump (if on the ground), open the wings, fire a rocket.
    private static void handleTakeoff(LocalPlayer player) {
        KeyMapping key = ClientModEvents.TAKEOFF_KEY;
        if (key != null) {
            while (key.consumeClick()) {
                if (takeoffTicks < 0 && !player.isFallFlying() && !player.isInWater()
                        && VersionCompat.hasUsableGlider(player) && hasRocket(player)) {
                    if (player.onGround()) {
                        player.jumpFromGround();
                    }
                    takeoffTicks = 0;
                }
            }
        }
        if (takeoffTicks < 0) {
            return;
        }

        if (!player.isFallFlying() && !player.onGround()) {
            deploy(player);
        }
        if (player.isFallFlying()) {
            fireRocket(player);
            takeoffTicks = -1;
        } else if (++takeoffTicks > TAKEOFF_TIMEOUT_TICKS) {
            takeoffTicks = -1;
        }
    }

    // Layer 2 (reactive safety net): we're gliding but never asked to be. If the jump
    // setting allows this kind of deploy, accept it once; otherwise fold the wings.
    // Client-only, no stop packet, so it never traps the player in the prone/crawling
    // hitbox on the server.
    private static void applySafetyNet(LocalPlayer player) {
        if (Config.SAFETY_NET.get() && player.isFallFlying() && !wantGliding) {
            if (ElytraTweaksState.isNonKeyDeployAllowed(player)) {
                wantGliding = true;
            } else {
                player.stopFallFlying();
            }
        }
    }

    private static void warnLowDurability(LocalPlayer player) {
        if (durabilityWarned || !player.isFallFlying() || !Config.DURABILITY_WARNING.get()) {
            return;
        }
        ItemStack glider = VersionCompat.getGliderStack(player);
        if (isLowDurability(glider)) {
            durabilityWarned = true;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.5F));
            minecraft.gui.setOverlayMessage(Component.translatable("elytra_tweaks.message.durability_low",
                    remainingDurability(glider)).withStyle(ChatFormatting.RED), false);
        }
    }

    // The server treats START_FALL_FLYING as "start if possible, else stop", deciding
    // from its own flag. Sending it on BOTH directions keeps the client and server in
    // phase, so stopping mid-air is a single press.
    private static void deploy(LocalPlayer player) {
        // isCustomPress lets this one call through PlayerEntityMixin's client-side block.
        ElytraTweaksState.isCustomPress = true;
        try {
            if (player.tryToStartFallFlying()) {
                wantGliding = true;
                player.connection.send(new ServerboundPlayerCommandPacket(
                        player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } finally {
            ElytraTweaksState.isCustomPress = false;
        }
    }

    private static void stop(LocalPlayer player) {
        wantGliding = false;
        player.stopFallFlying();
        player.connection.send(new ServerboundPlayerCommandPacket(
                player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }

    private static boolean hasRocket(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.FIREWORK_ROCKET)) {
                return true;
            }
        }
        return findHotbarRocket(player.getInventory()) >= 0;
    }

    private static int findHotbarRocket(Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).is(Items.FIREWORK_ROCKET)) {
                return slot;
            }
        }
        return -1;
    }

    private static void fireRocket(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.FIREWORK_ROCKET)) {
                minecraft.gameMode.useItem(player, hand);
                return;
            }
        }
        // Not in a hand: briefly select the hotbar slot holding one. useItem tells the
        // server about the slot change before using the item, then we switch back.
        Inventory inventory = player.getInventory();
        int slot = findHotbarRocket(inventory);
        if (slot >= 0) {
            int previous = VersionCompat.getSelectedSlot(inventory);
            VersionCompat.setSelectedSlot(inventory, slot);
            minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            VersionCompat.setSelectedSlot(inventory, previous);
        }
    }

    static int remainingDurability(ItemStack glider) {
        return glider.getMaxDamage() - glider.getDamageValue();
    }

    static boolean isLowDurability(ItemStack glider) {
        if (glider.isEmpty() || !glider.isDamageableItem()) {
            return false;
        }
        int threshold = (int) Math.ceil(glider.getMaxDamage() * Config.DURABILITY_WARNING_PERCENT.get() / 100.0);
        return remainingDurability(glider) <= threshold;
    }
}
