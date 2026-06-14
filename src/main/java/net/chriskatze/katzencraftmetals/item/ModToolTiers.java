package net.chriskatze.katzencraftmetals.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class ModToolTiers {

    public static final Tier STEEL = new FixedToolTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            512,
            7.0F,
            3.0F,
            18,
            () -> Ingredient.of(ModItems.STEEL_INGOT.get())
    );

    public static final Tier MYTHRIL = new FixedToolTier(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            1536,
            10.0F,
            5.0F,
            25,
            () -> Ingredient.of(ModItems.MYTHRIL_INGOT.get())
    );

    private ModToolTiers() {
    }
}