package net.chriskatze.katzencraftmetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record CrusherRecipe(
        Ingredient input,
        ItemStack mainOutput,
        ItemStack secondOutput,
        float secondChance,
        ItemStack thirdOutput,
        float thirdChance,
        int processingTime
) implements Recipe<SingleRecipeInput> {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return input.test(recipeInput.getItem(0));
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
        return mainOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return mainOutput.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRUSHER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRUSHER_TYPE.get();
    }

    public ItemStack getMainOutput() {
        return mainOutput.copy();
    }

    public ItemStack getSecondOutput() {
        return secondOutput.copy();
    }

    public ItemStack getThirdOutput() {
        return thirdOutput.copy();
    }

    public static class Serializer implements RecipeSerializer<CrusherRecipe> {

        public static final MapCodec<CrusherRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrusherRecipe::input),
                ItemStack.CODEC.fieldOf("main_output").forGetter(CrusherRecipe::mainOutput),

                ItemStack.OPTIONAL_CODEC.optionalFieldOf("second_output", ItemStack.EMPTY).forGetter(CrusherRecipe::secondOutput),
                Codec.FLOAT.optionalFieldOf("second_chance", 0.0f).forGetter(CrusherRecipe::secondChance),

                ItemStack.OPTIONAL_CODEC.optionalFieldOf("third_output", ItemStack.EMPTY).forGetter(CrusherRecipe::thirdOutput),
                Codec.FLOAT.optionalFieldOf("third_chance", 0.0f).forGetter(CrusherRecipe::thirdChance),

                Codec.INT.optionalFieldOf("processing_time", 100).forGetter(CrusherRecipe::processingTime)
        ).apply(instance, CrusherRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input());
                    ItemStack.STREAM_CODEC.encode(buf, recipe.mainOutput());

                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.secondOutput());
                    buf.writeFloat(recipe.secondChance());

                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.thirdOutput());
                    buf.writeFloat(recipe.thirdChance());

                    buf.writeVarInt(recipe.processingTime());
                },
                buf -> new CrusherRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                        ItemStack.STREAM_CODEC.decode(buf),

                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                        buf.readFloat(),

                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                        buf.readFloat(),

                        buf.readVarInt()
                )
        );

        @Override
        public MapCodec<CrusherRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}