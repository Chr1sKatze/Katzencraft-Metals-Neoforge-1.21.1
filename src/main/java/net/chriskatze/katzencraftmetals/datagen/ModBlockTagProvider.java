package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.metal.StandardMetalForms;
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
        java.util.Set<net.minecraft.world.level.block.Block> pickaxeBlocks = new java.util.LinkedHashSet<>();
        for (var form : StandardMetalForms.entries()) if (form.form().block) pickaxeBlocks.add(form.block());

        java.util.Collections.addAll(pickaxeBlocks,
                                ModBlocks.PLATINUM_ORE.get(),
                        ModBlocks.DEEPSLATE_PLATINUM_ORE.get(),
                        ModBlocks.NETHER_PLATINUM_ORE.get(),
                        ModBlocks.END_PLATINUM_ORE.get(),

                                ModBlocks.MYTHRIL_ORE.get(),
                        ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(),
                        ModBlocks.NETHER_MYTHRIL_ORE.get(),
                        ModBlocks.END_MYTHRIL_ORE.get(),

                                ModBlocks.CUT_STEEL_BLOCK.get(),
                        ModBlocks.CUT_STEEL_SLAB.get(),
                        ModBlocks.CUT_STEEL_STAIRS.get(),
                        ModBlocks.STEEL_BARS.get(),
                        ModBlocks.STEEL_CHAIN.get(),
                        ModBlocks.STEEL_PRESSURE_PLATE.get(),
                        ModBlocks.STEEL_BUTTON.get(),
                        ModBlocks.STEEL_LEVER.get(),
                        ModBlocks.STEEL_DOOR.get(),
                        ModBlocks.STEEL_TRAPDOOR.get(),

                        ModBlocks.FUEL_CHAMBER.get(),
                        ModBlocks.FOUNDRY_CONTROLLER.get(),
                        ModBlocks.FOUNDRY_TANK.get(),
                        ModBlocks.FOUNDRY_BASIN.get(),
                        ModBlocks.FOUNDRY_TABLE.get(),
                        ModBlocks.FOUNDRY_FAUCET.get()
                );

        // All eight levels are material-independent requirements, including currently empty levels.
        for(int level=1;level<=8;level++)tag(net.chriskatze.katzencraftmetals.config.BlockRequirements.tag(level));
        var legacyDiamond=net.chriskatze.katzencraftmetals.config.BlockRequirements.legacyDiamondBlocks();
        for(var entry:net.chriskatze.katzencraftmetals.config.BlockRequirements.configured().entrySet()) {
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(entry.getKey()).orElseThrow();
            tag(net.chriskatze.katzencraftmetals.config.BlockRequirements.tag(entry.getValue())).add(block);
            pickaxeBlocks.add(block);
            // Only existing Steel/Platinum/Mythril content retains saved Diamond-pickaxe access.
            if(legacyDiamond.contains(entry.getKey()))
                tag(net.chriskatze.katzencraftmetals.config.BlockRequirements.LEGACY_DIAMOND).add(block);
        }

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(pickaxeBlocks.toArray(net.minecraft.world.level.block.Block[]::new));
        tag(BlockTags.CLIMBABLE)
                .add(ModBlocks.STEEL_LADDER.get());
    }
}