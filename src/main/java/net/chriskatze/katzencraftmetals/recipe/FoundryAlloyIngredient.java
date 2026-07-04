package net.chriskatze.katzencraftmetals.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** One molten-metal requirement inside a Foundry alloy recipe. */
public record FoundryAlloyIngredient(
        ResourceLocation metal,
        int amount
) {

    public static final Codec<FoundryAlloyIngredient> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                    ResourceLocation.CODEC
                                            .fieldOf("metal")
                                            .forGetter(FoundryAlloyIngredient::metal),
                                    Codec.intRange(1, Integer.MAX_VALUE)
                                            .fieldOf("amount")
                                            .forGetter(FoundryAlloyIngredient::amount)
                            ).apply(instance, FoundryAlloyIngredient::new)
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            FoundryAlloyIngredient
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, ingredient) -> {
                        buffer.writeResourceLocation(ingredient.metal());
                        buffer.writeVarInt(ingredient.amount());
                    },
                    buffer ->
                            new FoundryAlloyIngredient(
                                    buffer.readResourceLocation(),
                                    buffer.readVarInt()
                            )
            );

    public FoundryAlloyIngredient {
        if (metal == null) {
            throw new IllegalArgumentException(
                    "Foundry alloy ingredient metal cannot be null."
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Foundry alloy ingredient amount must be positive."
            );
        }
    }
}
