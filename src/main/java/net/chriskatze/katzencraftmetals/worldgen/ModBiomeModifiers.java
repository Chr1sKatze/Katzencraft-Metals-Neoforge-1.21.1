package net.chriskatze.katzencraftmetals.worldgen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;


public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> REMOVE_VANILLA_DIAMOND_ORE =
            createKey("remove_vanilla_diamond_ore");

    public static final ResourceKey<BiomeModifier> REMOVE_VANILLA_EMERALD_ORE =
            createKey("remove_vanilla_emerald_ore");

    public static final ResourceKey<BiomeModifier> ADD_EXTRA_MONSTER_ROOM =
            createKey("add_extra_monster_room");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);

        ModOreGeneration.biome(context);

        context.register(REMOVE_VANILLA_DIAMOND_ORE, BiomeModifiers.RemoveFeaturesBiomeModifier.allSteps(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(OrePlacements.ORE_DIAMOND),
                        placedFeatures.getOrThrow(OrePlacements.ORE_DIAMOND_BURIED),
                        placedFeatures.getOrThrow(OrePlacements.ORE_DIAMOND_LARGE),
                        placedFeatures.getOrThrow(OrePlacements.ORE_DIAMOND_MEDIUM)
                )
        ));

        context.register(REMOVE_VANILLA_EMERALD_ORE, BiomeModifiers.RemoveFeaturesBiomeModifier.allSteps(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(OrePlacements.ORE_EMERALD)
                )
        ));

        context.register(ADD_EXTRA_MONSTER_ROOM, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(Tags.Biomes.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.EXTRA_MONSTER_ROOM_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_STRUCTURES
        ));
    }

    private static ResourceKey<BiomeModifier> createKey(String name) {
        return ResourceKey.create(
                NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, name)
        );
    }
}