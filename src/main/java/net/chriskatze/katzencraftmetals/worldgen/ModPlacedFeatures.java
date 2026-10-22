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

    public static final ResourceKey<PlacedFeature> EXTRA_MONSTER_ROOM_PLACED =
            createKey("extra_monster_room_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures =
                context.lookup(Registries.CONFIGURED_FEATURE);

        ModOreGeneration.placed(context);

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