package net.frostytrix.elytratweaks;

import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Client-side flight logic, run once per tick by LocalPlayerMixin at the start of aiStep,
// before vanilla reads the jump key and moves the player.
public final class ElytraController {
    // How long a firework takeoff may wait to leave the ground before giving up.
    private static final int TAKEOFF_TIMEOUT_TICKS = 20;
    // Chest armor slot in the player's inventory menu (armor slots are HEAD, CHEST, LEGS, FEET).
    private static final int CHEST_MENU_SLOT = InventoryMenu.ARMOR_SLOT_START + 1;
    // If no flight follows an auto-swap (e.g. a failed takeoff), put the chestplate back after this.
    private static final int SWAP_BACK_GRACE_TICKS = 20;

    private static LocalPlayer lastPlayer;
    // Whether the current flight is one we want: started by the deploy key or a takeoff,
    // or accepted by the "jumpDeploy" setting.
    private static boolean wantGliding;
    // Deploy key state on the previous tick, used by HOLD mode to detect a release.
    private static boolean deployKeyWasDown;
    // Ticks since a firework takeoff started, or -1 when none is in progress.
    private static int takeoffTicks = -1;
    private static boolean durabilityWarned;
    // Whether the server last told us our player is gliding (from its entity data, see
    // EntityMixin). Folding the wings on the client doesn't change it.
    private static boolean serverGliding;
    // Auto-swap: whether an elytra was put on for the current flight, and what it replaced
    // (EMPTY when the chest slot was empty, so there is nothing to put back).
    private static boolean swapActive;
    private static ItemStack swappedOut = ItemStack.EMPTY;
    private static boolean flownSinceSwap;
    private static int ticksSinceSwap;

    private ElytraController() {
    }

    public static void tick(LocalPlayer player) {
        if (player != lastPlayer) {
            reset(player);
        }

        // Clear per-flight state once the flight is over (landed, water, server stopped it...).
        if (!player.isFallFlying()) {
            wantGliding = false;
            durabilityWarned = false;
        }

        handleDeployKey(player);
        handleTakeoff(player);
        swapBackAfterLanding(player);
        applySafetyNet(player);
        warnLowDurability(player);
    }

    public static void onServerGlidingState(LocalPlayer player, boolean gliding) {
        if (player != lastPlayer) {
            reset(player);
        }
        serverGliding = gliding;
    }

    // New world or respawn: forget the previous player's state.
    private static void reset(LocalPlayer player) {
        lastPlayer = player;
        wantGliding = false;
        deployKeyWasDown = false;
        takeoffTicks = -1;
        durabilityWarned = false;
        serverGliding = false;
        swapActive = false;
        swappedOut = ItemStack.EMPTY;
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
                if (takeoffTicks < 0 && !player.isFallFlying() && !player.isInWater() && hasRocket(player)) {
                    equipGliderFromInventory(player);
                    // No "is an elytra worn?" check: accessory-slot mods (e.g. Elytra Slot) let players
                    // glide without one in the chest slot, so just try and let the game decide. The
                    // rocket is only fired once gliding actually starts.
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
    private static void applySafetyNet(LocalPlayer player) {
        if (Config.SAFETY_NET.get() && player.isFallFlying() && !wantGliding) {
            if (ElytraTweaksState.isNonKeyDeployAllowed(player)) {
                wantGliding = true;
            } else {
                player.stopFallFlying();
                if (serverGliding) {
                    // Something started this flight on the server, which would keep us gliding
                    // (and wear the elytra down) until we land. The server is gliding, so
                    // START_FALL_FLYING makes it stop. Never sent when the server isn't gliding:
                    // it would start gliding instead, trapping the player in the gliding hitbox.
                    serverGliding = false;
                    player.connection.send(new ServerboundPlayerCommandPacket(
                            player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
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
        // Only swap in the air: HOLD mode calls this every tick while the key is held on the ground.
        if (!player.onGround()) {
            equipGliderFromInventory(player);
        }
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
        serverGliding = false;
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

    // Auto-swap (opt-in): wear an elytra from the inventory in place of whatever is in the
    // chest slot, remembering what it replaced so it can go back after landing.
    private static void equipGliderFromInventory(LocalPlayer player) {
        if (!Config.AUTO_SWAP.get() || swapActive || VersionCompat.hasUsableGlider(player) || !canUseInventory(player)) {
            return;
        }
        int index = findInventorySlot(player, stack -> VersionCompat.isUsableChestGlider(stack, player));
        if (index < 0) {
            return;
        }
        Slot chest = player.inventoryMenu.getSlot(CHEST_MENU_SLOT);
        ItemStack worn = chest.getItem();
        // Curse of Binding (or a slot that won't take the glider): leave everything as it is.
        if ((!worn.isEmpty() && !chest.mayPickup(player)) || !chest.mayPlace(player.getInventory().getItem(index))) {
            return;
        }
        swappedOut = worn.copy();
        swapActive = true;
        flownSinceSwap = false;
        ticksSinceSwap = 0;
        swapChestWith(player, index);
    }

    private static void swapBackAfterLanding(LocalPlayer player) {
        if (!swapActive) {
            return;
        }
        ticksSinceSwap++;
        if (player.isFallFlying()) {
            flownSinceSwap = true;
            return;
        }
        boolean landed = player.onGround() || player.isInWater();
        boolean flightOver = flownSinceSwap || ticksSinceSwap >= SWAP_BACK_GRACE_TICKS;
        if (!landed || !flightOver || takeoffTicks >= 0 || !canUseInventory(player)) {
            return;
        }
        swapActive = false;
        ItemStack original = swappedOut;
        swappedOut = ItemStack.EMPTY;
        // Nothing was worn before, or the player changed their chest armor in the meantime.
        if (original.isEmpty() || !VersionCompat.isGlider(player.getItemBySlot(EquipmentSlot.CHEST), player)) {
            return;
        }
        int index = findInventorySlot(player, stack -> ItemStack.isSameItemSameComponents(stack, original));
        Slot chest = player.inventoryMenu.getSlot(CHEST_MENU_SLOT);
        if (index >= 0 && chest.mayPickup(player) && chest.mayPlace(player.getInventory().getItem(index))) {
            swapChestWith(player, index);
        }
    }

    // Swap the chest slot with inventory slot `index` (0-35) using vanilla "swap with hotbar"
    // clicks, which either swap both items or do nothing, so nothing is ever left on the cursor.
    // A main-inventory slot goes through the selected hotbar slot, which ends up as it was.
    private static void swapChestWith(LocalPlayer player, int index) {
        if (index < Inventory.getSelectionSize()) {
            VersionCompat.swapWithHotbar(player, CHEST_MENU_SLOT, index);
        } else {
            int hotbar = VersionCompat.getSelectedSlot(player.getInventory());
            // In the inventory menu, main inventory slots 9-35 keep their inventory index.
            VersionCompat.swapWithHotbar(player, index, hotbar);
            VersionCompat.swapWithHotbar(player, CHEST_MENU_SLOT, hotbar);
            VersionCompat.swapWithHotbar(player, index, hotbar);
        }
    }

    // Hotbar first, then the main inventory; -1 if nothing matches.
    private static int findInventorySlot(LocalPlayer player, Predicate<ItemStack> matches) {
        for (int slot = 0; slot < InventoryMenu.INV_SLOT_END; slot++) {
            if (matches.test(player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    // Inventory clicks go to the player's own inventory menu, so only when no other menu is open.
    private static boolean canUseInventory(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null && minecraft.gameMode != null
                && player.containerMenu == player.inventoryMenu && !player.isSpectator();
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
