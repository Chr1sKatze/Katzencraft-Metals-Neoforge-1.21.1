package net.chriskatze.katzencraftmetals.datagen;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * Tiny datagen utility for recipe unlock criteria.
 *
 * RecipeProvider#has(...) is protected, so small helper providers outside
 * ModRecipeProvider need their own public equivalent.
 */
public final class RecipeProviderAccessor {

    private RecipeProviderAccessor() {
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItem(
            ItemLike item
    ) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(
                item
        );
    }

    public static String itemName(
            ItemLike item
    ) {
        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(
                        item.asItem()
                );

        return id.getPath();
    }
}
