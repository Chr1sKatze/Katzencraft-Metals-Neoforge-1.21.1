package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KatzencraftMetalsMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.PLATINUM_BLOCK.get());
        blockWithItem(ModBlocks.PLATINUM_ORE.get());
        blockWithItem(ModBlocks.DEEPSLATE_PLATINUM_ORE.get());
        blockWithItem(ModBlocks.NETHER_PLATINUM_ORE.get());
        blockWithItem(ModBlocks.END_PLATINUM_ORE.get());

        blockWithItem(ModBlocks.MYTHRIL_BLOCK.get());
        blockWithItem(ModBlocks.MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.DEEPSLATE_MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.NETHER_MYTHRIL_ORE.get());
        blockWithItem(ModBlocks.END_MYTHRIL_ORE.get());

        blockWithItem(ModBlocks.CRUSHER.get());
    }

    private void blockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}