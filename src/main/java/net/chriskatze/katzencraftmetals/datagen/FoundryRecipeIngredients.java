package net.chriskatze.katzencraftmetals.datagen;

import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Full Deepslate building cubes; ores, infested/reinforced blocks and partial blocks are excluded. */
public final class FoundryRecipeIngredients {
    private FoundryRecipeIngredients() {}
    public static final TagKey<Item> DEEPSLATE = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("katzencraftmetals:foundry_deepslate"));
}
