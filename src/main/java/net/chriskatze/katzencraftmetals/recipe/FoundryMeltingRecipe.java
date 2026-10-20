package net.chriskatze.katzencraftmetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.Optional;

/** One input item becomes molten units; optional recycling is resolved against the actual stack. */
public record FoundryMeltingRecipe(Ingredient ingredient, ResourceLocation moltenMetal,
                                  int moltenAmount, int processingTime, Optional<Recovery> recycling)
        implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_PROCESSING_TIME = 100;

    public FoundryMeltingRecipe(Ingredient ingredient, ResourceLocation metal, int amount, int ticks) {
        this(ingredient, metal, amount, ticks, Optional.empty());
    }

    public FoundryMeltingRecipe {
        if (moltenAmount <= 0 || processingTime <= 0) throw new IllegalArgumentException("Melting yield/time must be positive");
        if (recycling.isPresent()) {
            var recovery = recycling.get();
            if (moltenAmount % 6 != 0 || recovery.minimumOre() > moltenAmount / 6)
                throw new IllegalArgumentException("Recycling requires whole ore quantities and minimum <= maximum");
            if (recovery.durabilityScaling()) {
                for (var input : ingredient.getItems()) if (input.getMaxDamage() <= 0)
                    throw new IllegalArgumentException("Durability recycling requires a damageable input: " + input);
            }
        }
    }

    /** Minimum is in whole ore equivalents; positive halves round upward (Math.round). */
    public record Recovery(int minimumOre, boolean durabilityScaling) {
        public static final Codec<Recovery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("minimum_ore").forGetter(Recovery::minimumOre),
                Codec.BOOL.optionalFieldOf("durability_scaling", false).forGetter(Recovery::durabilityScaling)
        ).apply(instance, Recovery::new));
        public Recovery {
            if (minimumOre < 0) throw new IllegalArgumentException("Minimum recovery cannot be negative");
        }
        public int units(ItemStack stack, int maximumUnits) {
            if (!durabilityScaling) return maximumUnits;
            int durability = stack.getMaxDamage();
            if (durability <= 0) return 0;
            long remaining = Math.max(0L, Math.min((long) durability, (long) durability - stack.getDamageValue()));
            int maxOre = maximumUnits / 6;
            long ore = minimumOre + Math.round((double) (maxOre - minimumOre) * remaining / durability);
            return Math.multiplyExact((int) Math.max(minimumOre, Math.min(maxOre, ore)), 6);
        }
    }

    /** Shared by processing and tooltips. A zero-yield damaged item cannot start a melt. */
    public Optional<FoundryMeltingRecipe> resolve(ItemStack stack) {
        if (stack.isEmpty() || !ingredient.test(stack)) return Optional.empty();
        if (recycling.isEmpty()) return Optional.of(this);
        int amount = recycling.get().units(stack, moltenAmount);
        return amount > 0 ? Optional.of(new FoundryMeltingRecipe(ingredient, moltenMetal, amount, processingTime)) : Optional.empty();
    }

    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.getItem(0)); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.FOUNDRY_MELTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.FOUNDRY_MELTING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<FoundryMeltingRecipe> {
        public static final MapCodec<FoundryMeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FoundryMeltingRecipe::ingredient),
                ResourceLocation.CODEC.fieldOf("molten_metal").forGetter(FoundryMeltingRecipe::moltenMetal),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("molten_amount").forGetter(FoundryMeltingRecipe::moltenAmount),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME).forGetter(FoundryMeltingRecipe::processingTime),
                Recovery.CODEC.optionalFieldOf("recycling").forGetter(FoundryMeltingRecipe::recycling)
        ).apply(instance, FoundryMeltingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FoundryMeltingRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    buffer.writeResourceLocation(recipe.moltenMetal());
                    buffer.writeVarInt(recipe.moltenAmount());
                    buffer.writeVarInt(recipe.processingTime());
                    buffer.writeBoolean(recipe.recycling().isPresent());
                    recipe.recycling().ifPresent(recovery -> {
                        buffer.writeVarInt(recovery.minimumOre());
                        buffer.writeBoolean(recovery.durabilityScaling());
                    });
                }, buffer -> {
                    var input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                    var metal = buffer.readResourceLocation();
                    int amount = buffer.readVarInt(), ticks = buffer.readVarInt();
                    var recovery = buffer.readBoolean() ? Optional.of(new Recovery(buffer.readVarInt(), buffer.readBoolean())) : Optional.<Recovery>empty();
                    return new FoundryMeltingRecipe(input, metal, amount, ticks, recovery);
                });
        @Override public MapCodec<FoundryMeltingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FoundryMeltingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
