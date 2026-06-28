package net.chriskatze.katzencraftmetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Converts one input item into an integer amount of a registered molten
 * metal. It deliberately has no ItemStack output.
 */
public record FoundryMeltingRecipe(
        Ingredient ingredient,
        ResourceLocation moltenMetal,
        int moltenAmount,
        int processingTime
) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(
            SingleRecipeInput input,
            Level level
    ) {
        return ingredient.test(
                input.getItem(0)
        );
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
        return ModRecipes.FOUNDRY_MELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FOUNDRY_MELTING_TYPE.get();
    }

    public static class Serializer
            implements RecipeSerializer<FoundryMeltingRecipe> {

        public static final MapCodec<FoundryMeltingRecipe> CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                        Ingredient.CODEC_NONEMPTY
                                                .fieldOf("ingredient")
                                                .forGetter(
                                                        FoundryMeltingRecipe::ingredient
                                                ),

                                        ResourceLocation.CODEC
                                                .fieldOf("molten_metal")
                                                .forGetter(
                                                        FoundryMeltingRecipe::moltenMetal
                                                ),

                                        Codec.intRange(
                                                        1,
                                                        Integer.MAX_VALUE
                                                )
                                                .fieldOf("molten_amount")
                                                .forGetter(
                                                        FoundryMeltingRecipe::moltenAmount
                                                ),

                                        Codec.intRange(
                                                        1,
                                                        Integer.MAX_VALUE
                                                )
                                                .optionalFieldOf(
                                                        "processing_time",
                                                        20
                                                )
                                                .forGetter(
                                                        FoundryMeltingRecipe::processingTime
                                                )
                                )
                                        .apply(
                                                instance,
                                                FoundryMeltingRecipe::new
                                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                FoundryMeltingRecipe
                > STREAM_CODEC =
                StreamCodec.of(
                        (
                                buffer,
                                recipe
                        ) -> {
                            Ingredient.CONTENTS_STREAM_CODEC.encode(
                                    buffer,
                                    recipe.ingredient()
                            );

                            buffer.writeResourceLocation(
                                    recipe.moltenMetal()
                            );

                            buffer.writeVarInt(
                                    recipe.moltenAmount()
                            );

                            buffer.writeVarInt(
                                    recipe.processingTime()
                            );
                        },
                        buffer ->
                                new FoundryMeltingRecipe(
                                        Ingredient.CONTENTS_STREAM_CODEC.decode(
                                                buffer
                                        ),
                                        buffer.readResourceLocation(),
                                        buffer.readVarInt(),
                                        buffer.readVarInt()
                                )
                );

        @Override
        public MapCodec<FoundryMeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
                RegistryFriendlyByteBuf,
                FoundryMeltingRecipe
                > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
