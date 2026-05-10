package net.chriskatze.katzencraftmetals.worldgen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> PLATINUM_ORE =
            createKey("platinum_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_PLATINUM_ORE =
            createKey("nether_platinum_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> END_PLATINUM_ORE =
            createKey("end_platinum_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> MYTHRIL_ORE =
            createKey("mythril_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_MYTHRIL_ORE =
            createKey("nether_mythril_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> END_MYTHRIL_ORE =
            createKey("end_mythril_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> EXTRA_MONSTER_ROOM =
            createKey("extra_monster_room");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest netherrack = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endStone = new BlockMatchTest(Blocks.END_STONE);

        register(context, PLATINUM_ORE, Feature.ORE, new OreConfiguration(
                List.of(
                        OreConfiguration.target(stoneReplaceables, ModBlocks.PLATINUM_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_PLATINUM_ORE.get().defaultBlockState())
                ),
                6
        ));

        register(context, NETHER_PLATINUM_ORE, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(netherrack, ModBlocks.NETHER_PLATINUM_ORE.get().defaultBlockState())),
                8
        ));

        register(context, END_PLATINUM_ORE, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(endStone, ModBlocks.END_PLATINUM_ORE.get().defaultBlockState())),
                8
        ));

        register(context, MYTHRIL_ORE, Feature.ORE, new OreConfiguration(
                List.of(
                        OreConfiguration.target(stoneReplaceables, ModBlocks.MYTHRIL_ORE.get().defaultBlockState()),
                        OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_MYTHRIL_ORE.get().defaultBlockState())
                ),
                4
        ));

        register(context, NETHER_MYTHRIL_ORE, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(netherrack, ModBlocks.NETHER_MYTHRIL_ORE.get().defaultBlockState())),
                5
        ));

        register(context, END_MYTHRIL_ORE, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(endStone, ModBlocks.END_MYTHRIL_ORE.get().defaultBlockState())),
                5
        ));

        register(context, EXTRA_MONSTER_ROOM, Feature.MONSTER_ROOM, NoneFeatureConfiguration.INSTANCE);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(
                Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, name)
        );
    }

    private static <FC extends net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            F feature,
            FC configuration
    ) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}