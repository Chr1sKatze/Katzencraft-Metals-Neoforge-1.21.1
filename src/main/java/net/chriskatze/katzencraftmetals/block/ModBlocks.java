package net.chriskatze.katzencraftmetals.block;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.custom.CrusherBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KatzencraftMetalsMod.MODID);

    public static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(KatzencraftMetalsMod.MODID);

    // =========================
    // STEEL
    // =========================

    public static final DeferredBlock<Block> STEEL_BLOCK = registerBlock("steel_block",
            () -> new Block(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()));

    // =========================
    // STEEL BUILDING BLOCKS
    // =========================

    public static final DeferredBlock<Block> STEEL_BARS =
            BLOCKS.register("steel_bars",
                    () -> new IronBarsBlock(
                            Block.Properties.ofFullCopy(Blocks.IRON_BARS)
                    ));

    public static final DeferredBlock<Block> STEEL_CHAIN =
            BLOCKS.register("steel_chain",
                    () -> new ChainBlock(
                            Block.Properties.ofFullCopy(Blocks.CHAIN)
                    ));

    public static final DeferredBlock<Block> STEEL_PRESSURE_PLATE =
            BLOCKS.register("steel_pressure_plate",
                    () -> new PressurePlateBlock(
                            BlockSetType.IRON,
                            Block.Properties.ofFullCopy(Blocks.STONE_PRESSURE_PLATE)
                    ));

    public static final DeferredBlock<Block> STEEL_BUTTON =
            BLOCKS.register("steel_button",
                    () -> new ButtonBlock(
                            BlockSetType.STONE,
                            20,
                            Block.Properties.ofFullCopy(Blocks.STONE_BUTTON)
                    ));

    public static final DeferredBlock<Block> STEEL_LEVER =
            BLOCKS.register("steel_lever",
                    () -> new LeverBlock(
                            Block.Properties.ofFullCopy(Blocks.LEVER)
                    ));

    public static final DeferredBlock<Block> STEEL_DOOR =
            BLOCKS.register("steel_door",
                    () -> new DoorBlock(
                            BlockSetType.IRON,
                            Block.Properties.ofFullCopy(Blocks.IRON_DOOR)
                    ));

    public static final DeferredBlock<Block> STEEL_TRAPDOOR =
            BLOCKS.register("steel_trapdoor",
                    () -> new TrapDoorBlock(
                            BlockSetType.IRON,
                            Block.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR)
                    ));

    // =========================
    // PLATINUM
    // =========================

    public static final DeferredBlock<Block> PLATINUM_BLOCK = registerBlock("platinum_block",
            () -> new Block(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> PLATINUM_ORE = registerBlock("platinum_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_PLATINUM_ORE = registerBlock("deepslate_platinum_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6),
                    Block.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> NETHER_PLATINUM_ORE = registerBlock("nether_platinum_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.NETHERRACK)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> END_PLATINUM_ORE = registerBlock("end_platinum_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.END_STONE)
                            .requiresCorrectToolForDrops()));

    // =========================
    // MYTHRIL
    // =========================

    public static final DeferredBlock<Block> MYTHRIL_BLOCK = registerBlock("mythril_block",
            () -> new Block(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MYTHRIL_ORE = registerBlock("mythril_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.STONE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_MYTHRIL_ORE = registerBlock("deepslate_mythril_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6),
                    Block.Properties.ofFullCopy(Blocks.DEEPSLATE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> NETHER_MYTHRIL_ORE = registerBlock("nether_mythril_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.NETHERRACK)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> END_MYTHRIL_ORE = registerBlock("end_mythril_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    Block.Properties.ofFullCopy(Blocks.END_STONE)
                            .requiresCorrectToolForDrops()));

    // =========================
    // MACHINES
    // =========================

    public static final DeferredBlock<Block> CRUSHER = registerBlock("crusher",
            () -> new CrusherBlock(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    // =========================
    // HELPERS
    // =========================

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> registeredBlock = BLOCKS.register(name, blockSupplier);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }

    private static <T extends Block> DeferredHolder<Item, BlockItem> registerBlockItem(String name, DeferredBlock<T> block) {
        return BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // =========================
    // REGISTER
    // =========================

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ITEMS.register(modEventBus);
    }
}