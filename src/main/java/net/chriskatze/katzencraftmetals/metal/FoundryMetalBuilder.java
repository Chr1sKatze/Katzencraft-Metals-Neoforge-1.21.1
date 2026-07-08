package net.chriskatze.katzencraftmetals.metal;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * Small helper for declaring molten metals in one compact block.
 *
 * This is intentionally runtime-side, not datagen-side:
 * ModMoltenMetals is used by Controller sync ids, GUI data counts, storage,
 * rendering, and recipes while the game is running.
 */
public final class FoundryMetalBuilder {

    private final ResourceLocation id;
    private final String translationKey;

    private ResourceLocation animatedTexture =
            modBlockTexture(
                    "molten_copper"
            );

    private ResourceLocation cooledTexture =
            modBlockTexture(
                    "steel_block"
            );

    private int density =
            1;

    private int unitsPerOre =
            6;

    private Supplier<ItemStack> castResultFactory =
            () -> ItemStack.EMPTY;

    private FoundryMetalBuilder(
            String namespace,
            String name
    ) {
        id =
                ResourceLocation.fromNamespaceAndPath(
                        namespace,
                        name
                );

        translationKey =
                "metal."
                        + KatzencraftMetalsMod.MODID
                        + "."
                        + name;
    }

    public static FoundryMetalBuilder mod(
            String name
    ) {
        return new FoundryMetalBuilder(
                KatzencraftMetalsMod.MODID,
                name
        );
    }

    public static FoundryMetalBuilder vanilla(
            String name
    ) {
        return new FoundryMetalBuilder(
                "minecraft",
                name
        );
    }

    public FoundryMetalBuilder animatedTexture(
            String textureName
    ) {
        animatedTexture =
                modBlockTexture(
                        textureName
                );

        return this;
    }

    public FoundryMetalBuilder animatedTexture(
            String namespace,
            String textureName
    ) {
        animatedTexture =
                blockTexture(
                        namespace,
                        textureName
                );

        return this;
    }

    public FoundryMetalBuilder animatedTexture(
            ResourceLocation texture
    ) {
        animatedTexture =
                texture;

        return this;
    }

    public FoundryMetalBuilder cooledTexture(
            String textureName
    ) {
        cooledTexture =
                modBlockTexture(
                        textureName
                );

        return this;
    }

    public FoundryMetalBuilder vanillaCooledTexture(
            String textureName
    ) {
        cooledTexture =
                blockTexture(
                        "minecraft",
                        textureName
                );

        return this;
    }

    public FoundryMetalBuilder cooledTexture(
            String namespace,
            String textureName
    ) {
        cooledTexture =
                blockTexture(
                        namespace,
                        textureName
                );

        return this;
    }

    public FoundryMetalBuilder cooledTexture(
            ResourceLocation texture
    ) {
        cooledTexture =
                texture;

        return this;
    }

    public FoundryMetalBuilder density(
            int density
    ) {
        this.density =
                density;

        return this;
    }

    public FoundryMetalBuilder unitsPerOre(
            int unitsPerOre
    ) {
        this.unitsPerOre =
                unitsPerOre;

        return this;
    }

    public FoundryMetalBuilder castResult(
            ItemLike itemLike
    ) {
        castResultFactory =
                () -> new ItemStack(
                        itemLike
                );

        return this;
    }

    public FoundryMetalBuilder castResult(
            Supplier<ItemStack> castResultFactory
    ) {
        this.castResultFactory =
                castResultFactory;

        return this;
    }

    public MoltenMetalDefinition build() {
        return new MoltenMetalDefinition(
                id,
                translationKey,
                animatedTexture,
                cooledTexture,
                density,
                unitsPerOre,
                castResultFactory
        );
    }

    private static ResourceLocation modBlockTexture(
            String textureName
    ) {
        return blockTexture(
                KatzencraftMetalsMod.MODID,
                textureName
        );
    }

    private static ResourceLocation blockTexture(
            String namespace,
            String textureName
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                namespace,
                "textures/block/"
                        + textureName
                        + ".png"
        );
    }
}
