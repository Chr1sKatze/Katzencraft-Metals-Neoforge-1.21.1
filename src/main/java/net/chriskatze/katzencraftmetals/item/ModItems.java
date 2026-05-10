package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KatzencraftMetalsMod.MODID);

    // =========================
    // PLATINUM
    // =========================

    public static final DeferredItem<Item> PLATINUM_NUGGET =
            ITEMS.registerSimpleItem("platinum_nugget");

    public static final DeferredItem<Item> RAW_PLATINUM =
            ITEMS.registerSimpleItem("raw_platinum");

    public static final DeferredItem<Item> PLATINUM_INGOT =
            ITEMS.registerSimpleItem("platinum_ingot");

    public static final DeferredItem<Item> PLATINUM_POWDER =
            ITEMS.registerSimpleItem("platinum_powder");

    // =========================
    // MYTHRIL
    // =========================

    public static final DeferredItem<Item> MYTHRIL_NUGGET =
            ITEMS.registerSimpleItem("mythril_nugget");

    public static final DeferredItem<Item> RAW_MYTHRIL =
            ITEMS.registerSimpleItem("raw_mythril");

    public static final DeferredItem<Item> MYTHRIL_INGOT =
            ITEMS.registerSimpleItem("mythril_ingot");

    public static final DeferredItem<Item> MYTHRIL_POWDER =
            ITEMS.registerSimpleItem("mythril_powder");

    // =========================
    // GEMS
    // =========================

    public static final DeferredItem<Item> SAPPHIRE_GEM =
            ITEMS.registerSimpleItem("sapphire_gem");

    public static final DeferredItem<Item> AMETHYST_GEM =
            ITEMS.registerSimpleItem("amethyst_gem");

    // =========================
    // REGISTER
    // =========================

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}