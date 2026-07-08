package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.worldgen.ModBiomeModifiers;
import net.chriskatze.katzencraftmetals.worldgen.ModConfiguredFeatures;
import net.chriskatze.katzencraftmetals.worldgen.ModPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID
)
public class DataGenerators {

    private static final RegistrySetBuilder BUILDER =
            new RegistrySetBuilder()
                    .add(
                            Registries.CONFIGURED_FEATURE,
                            ModConfiguredFeatures::bootstrap
                    )
                    .add(
                            Registries.PLACED_FEATURE,
                            ModPlacedFeatures::bootstrap
                    )
                    .add(
                            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                            ModBiomeModifiers::bootstrap
                    );

    @SubscribeEvent
    public static void gatherData(
            GatherDataEvent event
    ) {
        DataGenerator generator =
                event.getGenerator();

        PackOutput output =
                generator.getPackOutput();

        ExistingFileHelper existingFileHelper =
                event.getExistingFileHelper();

        CompletableFuture<HolderLookup.Provider> lookupProvider =
                event.getLookupProvider();

        generator.addProvider(
                event.includeClient(),
                new ModItemModelProvider(
                        output,
                        existingFileHelper
                )
        );

        generator.addProvider(
                event.includeClient(),
                new ModBlockStateProvider(
                        output,
                        existingFileHelper
                )
        );

        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(output)
        );

        generator.addProvider(
                event.includeServer(),
                new ModRecipeProvider(
                        output,
                        lookupProvider
                )
        );

        BlockTagsProvider blockTagsProvider =
                new ModBlockTagProvider(
                        output,
                        lookupProvider,
                        existingFileHelper
                );

        generator.addProvider(
                event.includeServer(),
                blockTagsProvider
        );

        generator.addProvider(
                event.includeServer(),
                new LootTableProvider(
                        output,
                        Collections.emptySet(),
                        List.of(
                                new LootTableProvider.SubProviderEntry(
                                        ModBlockLootTableProvider::new,
                                        LootContextParamSets.BLOCK
                                ),
                                new LootTableProvider.SubProviderEntry(
                                        registries ->
                                                new ModVanillaLootTableProvider(),
                                        LootContextParamSets.CHEST
                                ),
                                new LootTableProvider.SubProviderEntry(
                                        registries ->
                                                new ModVanillaEntityLootTableProvider(),
                                        LootContextParamSets.ENTITY
                                ),
                                new LootTableProvider.SubProviderEntry(
                                        registries ->
                                                new ModVanillaFishingLootTableProvider(),
                                        LootContextParamSets.FISHING
                                ),
                                new LootTableProvider.SubProviderEntry(
                                        registries ->
                                                new ModVanillaGameplayLootTableProvider(),
                                        LootContextParamSets.GIFT
                                ),
                                new LootTableProvider.SubProviderEntry(
                                        registries ->
                                                new ModVanillaArchaeologyLootTableProvider(),
                                        LootContextParamSets.ARCHAEOLOGY
                                )
                        ),
                        lookupProvider
                )
        );

        generator.addProvider(
                event.includeServer(),
                new DatapackBuiltinEntriesProvider(
                        output,
                        lookupProvider,
                        BUILDER,
                        Set.of(
                                KatzencraftMetalsMod.MODID
                        )
                )
        );
    }
}
