package net.frostytrix.elytratweaks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// The few game APIs that differ between the Minecraft versions this mod supports.
// Every other class is identical on all branches; only this file and ClientModEvents change.
public final class VersionCompat {
    private VersionCompat() {
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.getSelectedSlot();
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        inventory.setSelectedSlot(slot);
    }

    // The item the player glides with, even when it is too damaged to fly (for the HUD/warning).
    public static ItemStack getGliderStack(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.has(DataComponents.GLIDER)) {
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
}
