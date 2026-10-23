package net.frostytrix.elytratweaks;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// The few game APIs that differ between the Minecraft versions this mod supports.
// Every other class is identical on all branches; only this file and ClientModEvents change.
public final class VersionCompat {
    private VersionCompat() {
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.selected;
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        inventory.selected = slot;
    }

    // The item the player glides with, even when it is too damaged to fly (for the HUD/warning).
    public static ItemStack getGliderStack(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA) || chest.canElytraFly(player) ? chest : ItemStack.EMPTY;
    }

    // Whether the player wears something they could glide with right now.
    public static boolean hasUsableGlider(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player);
    }
}
