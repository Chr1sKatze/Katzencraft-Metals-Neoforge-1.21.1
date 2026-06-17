package net.chriskatze.katzencraftmetals.config;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ToolLevels {

    public static int getLevel(ItemStack stack) {

        Item item = stack.getItem();

        if (item == Items.WOODEN_PICKAXE) {
            return MiningLevels.WOOD;
        }

        if (item == Items.STONE_PICKAXE) {
            return MiningLevels.STONE;
        }

        if (item == Items.IRON_PICKAXE) {
            return MiningLevels.IRON;
        }

        if (item == ModItems.STEEL_PICKAXE.get()) {
            return MiningLevels.STEEL;
        }

        if (item == Items.DIAMOND_PICKAXE) {
            return MiningLevels.DIAMOND;
        }

        if (item == ModItems.MYTHRIL_PICKAXE.get()) {
            return MiningLevels.MYTHRIL;
        }

        return 0;
    }
}