package net.chriskatze.katzencraftmetals.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Stable, recipe-id-sorted access to every Foundry alloy recipe. */
public final class FoundryAlloyCatalog {

    private FoundryAlloyCatalog() {
    }

    public static List<RecipeHolder<FoundryAlloyRecipe>> getRecipes(
            Level level
    ) {
        if (level == null) {
            return List.of();
        }

        return level.getRecipeManager()
                .getAllRecipesFor(
                        ModRecipes.FOUNDRY_ALLOY_TYPE.get()
                )
                .stream()
                .sorted(
                        Comparator.comparing(
                                holder ->
                                        holder.id().toString()
                        )
                )
                .toList();
    }

    public static Optional<RecipeHolder<FoundryAlloyRecipe>> byIndex(
            Level level,
            int index
    ) {
        List<RecipeHolder<FoundryAlloyRecipe>> recipes =
                getRecipes(level);

        if (index < 0 || index >= recipes.size()) {
            return Optional.empty();
        }

        return Optional.of(recipes.get(index));
    }

    public static Optional<RecipeHolder<FoundryAlloyRecipe>> byId(
            Level level,
            ResourceLocation id
    ) {
        if (id == null) {
            return Optional.empty();
        }

        return getRecipes(level)
                .stream()
                .filter(holder -> holder.id().equals(id))
                .findFirst();
    }
}
