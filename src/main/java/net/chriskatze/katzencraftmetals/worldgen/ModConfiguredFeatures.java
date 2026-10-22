package net.chriskatze.katzencraftmetals.worldgen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> EXTRA_MONSTER_ROOM =
            createKey("extra_monster_room");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        ModOreGeneration.configured(context);

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