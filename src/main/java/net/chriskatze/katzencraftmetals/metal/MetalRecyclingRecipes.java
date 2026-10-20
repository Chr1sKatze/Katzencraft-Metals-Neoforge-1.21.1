package net.chriskatze.katzencraftmetals.metal;

import net.chriskatze.katzencraftmetals.recipe.FoundryMeltingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Explicit developer-owned recycling declarations; emits the existing Foundry melting recipe type. */
public final class MetalRecyclingRecipes {
    private static final List<Definition> RECIPES = List.of(
            recycle("minecraft:iron_pickaxe").into("iron").ore(3)
                    .durabilityScaling(true).minimumRecovery(1).build(),
            recycle("minecraft:iron_chestplate").into("iron").ore(8)
                    .durabilityScaling(true).minimumRecovery(1).build());

    private MetalRecyclingRecipes() {}
    public static Builder recycle(String item) { return new Builder(ResourceLocation.parse(item)); }
    public static List<Definition> values() { return RECIPES; }

    public static void generate(RecipeOutput output) {
        var inputs = new HashSet<ResourceLocation>();
        for (var definition : RECIPES) {
            if (!inputs.add(definition.input())) throw new IllegalArgumentException("Duplicate recycling input: " + definition.input());
            output.accept(ResourceLocation.fromNamespaceAndPath("katzencraftmetals",
                    "foundry_melting/recycling/" + definition.input().getNamespace() + "/" + definition.input().getPath()),
                    definition.recipe(), null);
        }
    }

    public record Definition(ResourceLocation input, ResourceLocation metal, int ore, boolean durabilityScaling,
                             int minimumRecovery, Integer meltingTime) {
        public Definition {
            if (ore <= 0 || minimumRecovery < 0 || minimumRecovery > ore)
                throw new IllegalArgumentException("Invalid recycling recovery range: " + input);
            try { Math.multiplyExact(ore, 6); }
            catch (ArithmeticException e) { throw new IllegalArgumentException("Recycling yield overflows: " + input, e); }
            if (meltingTime != null && meltingTime <= 0) throw new IllegalArgumentException("Invalid recycling time: " + input);
            var output = MetalCatalog.get(metal).filter(entry -> entry.foundry() != null)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown/unsupported recycling metal: " + metal));
            if (meltingTime == null) output.meltingTimeFor(input, ore * 6); // Validate multiplication/time overflow now.
        }
        /** Resolve item objects only after registry creation (datagen), never during catalog bootstrap. */
        public FoundryMeltingRecipe recipe() {
            var item = BuiltInRegistries.ITEM.getOptional(input).filter(value -> value != Items.AIR)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown recycling input: " + input));
            var output = MetalCatalog.get(metal).orElseThrow();
            return new FoundryMeltingRecipe(Ingredient.of(item), metal, ore * 6,
                    meltingTime != null ? meltingTime : output.meltingTimeFor(input, ore * 6),
                    Optional.of(new FoundryMeltingRecipe.Recovery(minimumRecovery, durabilityScaling)));
        }
    }

    public static final class Builder {
        private final ResourceLocation input;
        private ResourceLocation metal;
        private int ore, minimum = 0;
        private boolean scaling;
        private Integer ticks;
        private Builder(ResourceLocation input) { this.input = input; }
        public Builder into(String metal) { this.metal = MetalAlloy.input(metal, 1).metal(); return this; }
        public Builder ore(int ore) { this.ore = ore; return this; }
        public Builder durabilityScaling(boolean enabled) { scaling = enabled; return this; }
        public Builder minimumRecovery(int ore) { minimum = ore; return this; }
        public Builder meltingTime(int ticks) { this.ticks = ticks; return this; }
        public Definition build() {
            if (metal == null) throw new IllegalArgumentException("Recycling needs .into(...): " + input);
            return new Definition(input, metal, ore, scaling, minimum, ticks);
        }
    }
}
