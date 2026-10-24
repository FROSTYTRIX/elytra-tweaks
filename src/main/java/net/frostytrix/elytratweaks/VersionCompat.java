package net.frostytrix.elytratweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The few game APIs that differ between the Minecraft versions this mod supports.
// Every other class is identical on all branches; only this file and ClientModEvents change.
// Client-side only.
public final class VersionCompat {
    private VersionCompat() {
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.selected;
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        inventory.selected = slot;
    }

    // Whether the stack is something to glide with, even if it is too damaged to fly right now.
    public static boolean isGlider(ItemStack stack, Player player) {
        return stack.is(Items.ELYTRA) || stack.canElytraFly(player);
    }

    // Whether the stack could be worn in the chest slot and glided with right now.
    public static boolean isUsableChestGlider(ItemStack stack, Player player) {
        return stack.canElytraFly(player);
    }

    // The item the player glides with, even when it is too damaged to fly (for the HUD/warning).
    public static ItemStack getGliderStack(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isGlider(chest, player) ? chest : ItemStack.EMPTY;
    }

    // Whether the player wears something they could glide with right now.
    public static boolean hasUsableGlider(Player player) {
        return isUsableChestGlider(player.getItemBySlot(EquipmentSlot.CHEST), player);
    }

    // Vanilla "swap with hotbar slot" click (number key over a slot) in the player's inventory menu.
    public static void swapWithHotbar(LocalPlayer player, int menuSlot, int hotbarSlot) {
        Minecraft.getInstance().gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, menuSlot, hotbarSlot, ClickType.SWAP, player);
    }
}
