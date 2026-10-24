package net.frostytrix.elytratweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

// The few game APIs that differ between the Minecraft versions this mod supports.
// Every other class is identical on all branches; only this file and ClientModEvents change.
// Client-side only.
public final class VersionCompat {
    private VersionCompat() {
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.getSelectedSlot();
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        inventory.setSelectedSlot(slot);
    }

    // Whether the stack is something to glide with, even if it is too damaged to fly right now.
    public static boolean isGlider(ItemStack stack, Player player) {
        return stack.has(DataComponents.GLIDER);
    }

    // Whether the stack could be worn in the chest slot and glided with right now.
    public static boolean isUsableChestGlider(ItemStack stack, Player player) {
        return LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST);
    }

    // The item the player glides with, even when it is too damaged to fly (for the HUD/warning).
    public static ItemStack getGliderStack(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isGlider(stack, player)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // Whether the player wears something they could glide with right now.
    public static boolean hasUsableGlider(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
                return true;
            }
        }
        return false;
    }

    // Vanilla "swap with hotbar slot" click (number key over a slot) in the player's inventory menu.
    public static void swapWithHotbar(LocalPlayer player, int menuSlot, int hotbarSlot) {
        Minecraft.getInstance().gameMode.handleContainerInput(
                player.inventoryMenu.containerId, menuSlot, hotbarSlot, ContainerInput.SWAP, player);
    }
}
