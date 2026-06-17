package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, KatzencraftMetalsMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(
                        ModBlocks.PLATINUM_BLOCK.get(),
                        ModBlocks.PLATINUM_ORE.get(),
                        ModBlocks.DEEPSLATE_PLATINUM_ORE.get(),
                        ModBlocks.NETHER_PLATINUM_ORE.get(),
                        ModBlocks.END_PLATINUM_ORE.get(),

                        ModBlocks.MYTHRIL_BLOCK.get(),
                        ModBlocks.MYTHRIL_ORE.get(),
                        ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(),
                        ModBlocks.NETHER_MYTHRIL_ORE.get(),
                        ModBlocks.END_MYTHRIL_ORE.get(),

                        ModBlocks.STEEL_BLOCK.get(),
                        ModBlocks.STEEL_BARS.get(),
                        ModBlocks.STEEL_CHAIN.get(),
                        ModBlocks.STEEL_PRESSURE_PLATE.get(),
                        ModBlocks.STEEL_BUTTON.get(),
                        ModBlocks.STEEL_LEVER.get(),
                        ModBlocks.STEEL_DOOR.get(),
                        ModBlocks.STEEL_TRAPDOOR.get()
                );
    }
}