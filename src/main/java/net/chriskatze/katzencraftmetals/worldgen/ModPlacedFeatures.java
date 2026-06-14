package net.chriskatze.katzencraftmetals.worldgen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> PLATINUM_ORE_PLACED =
            createKey("platinum_ore_placed");

    public static final ResourceKey<PlacedFeature> NETHER_PLATINUM_ORE_PLACED =
            createKey("nether_platinum_ore_placed");

    public static final ResourceKey<PlacedFeature> END_PLATINUM_ORE_PLACED =
            createKey("end_platinum_ore_placed");

    public static final ResourceKey<PlacedFeature> MYTHRIL_ORE_PLACED =
            createKey("mythril_ore_placed");

    public static final ResourceKey<PlacedFeature> NETHER_MYTHRIL_ORE_PLACED =
            createKey("nether_mythril_ore_placed");

    public static final ResourceKey<PlacedFeature> END_MYTHRIL_ORE_PLACED =
            createKey("end_mythril_ore_placed");

    public static final ResourceKey<PlacedFeature> EXTRA_MONSTER_ROOM_PLACED =
            createKey("extra_monster_room_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures =
                context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, PLATINUM_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.PLATINUM_ORE),
                orePlacement(20, -64, 64));

        register(context, NETHER_PLATINUM_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_PLATINUM_ORE),
                orePlacement(20, 0, 120));

        register(context, END_PLATINUM_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.END_PLATINUM_ORE),
                orePlacement(20, 0, 100));

        register(context, MYTHRIL_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.MYTHRIL_ORE),
                orePlacement(15, -64, 64));

        register(context, NETHER_MYTHRIL_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_MYTHRIL_ORE),
                orePlacement(15, 0, 120));

        register(context, END_MYTHRIL_ORE_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.END_MYTHRIL_ORE),
                orePlacement(15, 0, 100));

        register(context, EXTRA_MONSTER_ROOM_PLACED,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.EXTRA_MONSTER_ROOM),
                List.of(
                        CountPlacement.of(200),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.absolute(-64),
                                VerticalAnchor.absolute(64)
                        ),
                        BiomeFilter.biome()
                ));
    }

    private static List<PlacementModifier> orePlacement(int veinsPerChunk, int minY, int maxY) {
        return List.of(
                CountPlacement.of(veinsPerChunk),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(
                        VerticalAnchor.absolute(minY),
                        VerticalAnchor.absolute(maxY)
                ),
                BiomeFilter.biome()
        );
    }

    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(
                Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, name)
        );
    }

    private static void register(
            BootstrapContext<PlacedFeature> context,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> configuredFeature,
            List<PlacementModifier> placementModifiers
    ) {
        context.register(key, new PlacedFeature(configuredFeature, placementModifiers));
    }
}