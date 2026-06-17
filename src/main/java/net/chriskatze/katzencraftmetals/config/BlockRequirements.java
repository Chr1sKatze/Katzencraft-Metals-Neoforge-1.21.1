package net.chriskatze.katzencraftmetals.config;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class BlockRequirements {

    private static final Map<Block, Integer> REQUIREMENTS = new HashMap<>();

    static {

        REQUIREMENTS.put(ModBlocks.STEEL_BLOCK.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_BARS.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_CHAIN.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_PRESSURE_PLATE.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_BUTTON.get(), MiningLevels.STONE);
        REQUIREMENTS.put(ModBlocks.STEEL_LEVER.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_DOOR.get(), MiningLevels.IRON);
        REQUIREMENTS.put(ModBlocks.STEEL_TRAPDOOR.get(), MiningLevels.WOOD);

        // Platinum

        REQUIREMENTS.put(ModBlocks.PLATINUM_ORE.get(), MiningLevels.STEEL);

        REQUIREMENTS.put(
                ModBlocks.DEEPSLATE_PLATINUM_ORE.get(),
                MiningLevels.STEEL
        );

        REQUIREMENTS.put(
                ModBlocks.NETHER_PLATINUM_ORE.get(),
                MiningLevels.STEEL
        );

        REQUIREMENTS.put(
                ModBlocks.END_PLATINUM_ORE.get(),
                MiningLevels.STEEL
        );

        REQUIREMENTS.put(
                ModBlocks.PLATINUM_BLOCK.get(),
                MiningLevels.STEEL
        );

        // Mythril

        REQUIREMENTS.put(
                ModBlocks.MYTHRIL_ORE.get(),
                MiningLevels.DIAMOND
        );

        REQUIREMENTS.put(
                ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(),
                MiningLevels.DIAMOND
        );

        REQUIREMENTS.put(
                ModBlocks.NETHER_MYTHRIL_ORE.get(),
                MiningLevels.DIAMOND
        );

        REQUIREMENTS.put(
                ModBlocks.END_MYTHRIL_ORE.get(),
                MiningLevels.DIAMOND
        );

        REQUIREMENTS.put(
                ModBlocks.MYTHRIL_BLOCK.get(),
                MiningLevels.DIAMOND
        );
    }

    public static int getRequirement(Block block) {
        return REQUIREMENTS.getOrDefault(
                block,
                MiningLevels.WOOD
        );
    }
}