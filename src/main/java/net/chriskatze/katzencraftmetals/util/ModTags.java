package net.chriskatze.katzencraftmetals.util;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {

        public static final TagKey<Block> NEEDS_STEEL_TOOL =
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "needs_steel_tool"
                        )
                );

        public static final TagKey<Block> NEEDS_DIAMOND_TOOL =
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "needs_diamond_tool"
                        )
                );

        public static final TagKey<Block> NEEDS_MYTHRIL_TOOL =
                TagKey.create(
                        Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(
                                KatzencraftMetalsMod.MODID,
                                "needs_mythril_tool"
                        )
                );
    }
}