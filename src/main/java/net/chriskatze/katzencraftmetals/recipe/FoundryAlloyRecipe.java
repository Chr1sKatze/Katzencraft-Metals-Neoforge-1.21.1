package net.chriskatze.katzencraftmetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Converts one to three stored molten metals into another molten metal.
 *
 * Recipes are selected explicitly in the Controller GUI, so matches() is not
 * used for automatic lookup.
 */
public record FoundryAlloyRecipe(
        List<FoundryAlloyIngredient> ingredients,
        ResourceLocation outputMetal,
        int outputAmount,
        int requiredTemperature,
        int processingTime,
        int experience,
        int requiredTier
) implements Recipe<SingleRecipeInput> {

    public FoundryAlloyRecipe {
        ingredients = List.copyOf(ingredients);

        if (ingredients.isEmpty() || ingredients.size() > 3) {
            throw new IllegalArgumentException(
                    "Foundry alloy recipes require between one and three ingredients."
            );
        }

        if (outputMetal == null) {
            throw new IllegalArgumentException(
                    "Foundry alloy output metal cannot be null."
            );
        }

        if (
                outputAmount <= 0
                        || requiredTemperature < 0
                        || processingTime <= 0
                        || experience < 0
                        || requiredTier < 1
                        || requiredTier > 4
        ) {
            throw new IllegalArgumentException(
                    "Invalid numeric value in Foundry alloy recipe."
            );
        }
    }

    @Override
    public boolean matches(
            SingleRecipeInput input,
            Level level
    ) {
        return false;
    }

    @Override
    public ItemStack assemble(
            SingleRecipeInput input,
            HolderLookup.Provider registries
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return true;
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FOUNDRY_ALLOY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FOUNDRY_ALLOY_TYPE.get();
    }

    public static final class Serializer
            implements RecipeSerializer<FoundryAlloyRecipe> {

        public static final MapCodec<FoundryAlloyRecipe> CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                        FoundryAlloyIngredient.CODEC
                                                .listOf()
                                                .fieldOf("ingredients")
                                                .forGetter(FoundryAlloyRecipe::ingredients),
                                        ResourceLocation.CODEC
                                                .fieldOf("output_metal")
                                                .forGetter(FoundryAlloyRecipe::outputMetal),
                                        Codec.intRange(1, Integer.MAX_VALUE)
                                                .fieldOf("output_amount")
                                                .forGetter(FoundryAlloyRecipe::outputAmount),
                                        Codec.intRange(0, Integer.MAX_VALUE)
                                                .fieldOf("required_temperature")
                                                .forGetter(FoundryAlloyRecipe::requiredTemperature),
                                        Codec.intRange(1, Integer.MAX_VALUE)
                                                .optionalFieldOf("processing_time", 100)
                                                .forGetter(FoundryAlloyRecipe::processingTime),
                                        Codec.intRange(0, Integer.MAX_VALUE)
                                                .optionalFieldOf("experience", 1)
                                                .forGetter(FoundryAlloyRecipe::experience),
                                        Codec.intRange(1, 4)
                                                .optionalFieldOf("required_tier", 1)
                                                .forGetter(FoundryAlloyRecipe::requiredTier)
                                ).apply(instance, FoundryAlloyRecipe::new)
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                FoundryAlloyRecipe
                > STREAM_CODEC =
                StreamCodec.of(
                        (buffer, recipe) -> {
                            buffer.writeVarInt(recipe.ingredients().size());

                            for (
                                    FoundryAlloyIngredient ingredient :
                                    recipe.ingredients()
                            ) {
                                FoundryAlloyIngredient.STREAM_CODEC.encode(
                                        buffer,
                                        ingredient
                                );
                            }

                            buffer.writeResourceLocation(recipe.outputMetal());
                            buffer.writeVarInt(recipe.outputAmount());
                            buffer.writeVarInt(recipe.requiredTemperature());
                            buffer.writeVarInt(recipe.processingTime());
                            buffer.writeVarInt(recipe.experience());
                            buffer.writeVarInt(recipe.requiredTier());
                        },
                        buffer -> {
                            int ingredientCount = buffer.readVarInt();

                            if (
                                    ingredientCount < 1
                                            || ingredientCount > 3
                            ) {
                                throw new IllegalArgumentException(
                                        "Invalid Foundry alloy ingredient count: "
                                                + ingredientCount
                                );
                            }

                            java.util.ArrayList<FoundryAlloyIngredient> ingredients =
                                    new java.util.ArrayList<>(ingredientCount);

                            for (int index = 0; index < ingredientCount; index++) {
                                ingredients.add(
                                        FoundryAlloyIngredient.STREAM_CODEC.decode(
                                                buffer
                                        )
                                );
                            }

                            return new FoundryAlloyRecipe(
                                    ingredients,
                                    buffer.readResourceLocation(),
                                    buffer.readVarInt(),
                                    buffer.readVarInt(),
                                    buffer.readVarInt(),
                                    buffer.readVarInt(),
                                    buffer.readVarInt()
                            );
                        }
                );

        @Override
        public MapCodec<FoundryAlloyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
                RegistryFriendlyByteBuf,
                FoundryAlloyRecipe
                > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
