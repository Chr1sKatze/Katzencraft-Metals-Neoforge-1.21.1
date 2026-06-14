package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KatzencraftMetalsMod.MODID);

    // =========================
    // COAL
    // =========================

    public static final DeferredItem<Item> CRUSHED_COAL =
            ITEMS.registerSimpleItem("crushed_coal");

    // =========================
    // IRON
    // =========================

    public static final DeferredItem<Item> IRON_POWDER =
            ITEMS.registerSimpleItem("iron_powder");

    // =========================
    // STEEL
    // =========================

    public static final DeferredItem<Item> STEEL_NUGGET =
            ITEMS.registerSimpleItem("steel_nugget");

    public static final DeferredItem<Item> STEEL_INGOT =
            ITEMS.registerSimpleItem("steel_ingot");

    public static final DeferredItem<Item> STEEL_POWDER =
            ITEMS.registerSimpleItem("steel_powder");

    public static final DeferredItem<BlockItem> STEEL_BARS =
            ITEMS.registerSimpleBlockItem("steel_bars", ModBlocks.STEEL_BARS);

    public static final DeferredItem<BlockItem> STEEL_CHAIN =
            ITEMS.registerSimpleBlockItem("steel_chain", ModBlocks.STEEL_CHAIN);

    public static final DeferredItem<BlockItem> STEEL_PRESSURE_PLATE =
            ITEMS.registerSimpleBlockItem("steel_pressure_plate", ModBlocks.STEEL_PRESSURE_PLATE);

    public static final DeferredItem<BlockItem> STEEL_BUTTON =
            ITEMS.registerSimpleBlockItem("steel_button", ModBlocks.STEEL_BUTTON);

    public static final DeferredItem<BlockItem> STEEL_LEVER =
            ITEMS.registerSimpleBlockItem("steel_lever", ModBlocks.STEEL_LEVER);

    public static final DeferredItem<BlockItem> STEEL_DOOR =
            ITEMS.registerSimpleBlockItem("steel_door", ModBlocks.STEEL_DOOR);

    public static final DeferredItem<BlockItem> STEEL_TRAPDOOR =
            ITEMS.registerSimpleBlockItem("steel_trapdoor", ModBlocks.STEEL_TRAPDOOR);

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
    // STEEL TOOLS
    // =========================

    public static final DeferredItem<PickaxeItem> STEEL_PICKAXE =
            ITEMS.register("steel_pickaxe",
                    () -> new PickaxeItem(
                            ModToolTiers.STEEL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.STEEL,
                                                    1.0F,
                                                    -2.8F
                                            )
                                    )
                    ));

    public static final DeferredItem<AxeItem> STEEL_AXE =
            ITEMS.register("steel_axe",
                    () -> new AxeItem(
                            ModToolTiers.STEEL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.STEEL,
                                                    1.5F,
                                                    -3.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<ShovelItem> STEEL_SHOVEL =
            ITEMS.register("steel_shovel",
                    () -> new ShovelItem(
                            ModToolTiers.STEEL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.STEEL,
                                                    1.5F,
                                                    -3.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<HoeItem> STEEL_HOE =
            ITEMS.register("steel_hoe",
                    () -> new HoeItem(
                            ModToolTiers.STEEL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.STEEL,
                                                    -2.0F,
                                                    -1.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<SwordItem> STEEL_SWORD =
            ITEMS.register("steel_sword",
                    () -> new SwordItem(
                            ModToolTiers.STEEL,
                            new Item.Properties()
                                    .attributes(
                                            SwordItem.createAttributes(
                                                    ModToolTiers.STEEL,
                                                    3,
                                                    -2.4F
                                            )
                                    )
                    ));

    // =========================
    // STEEL ARMOR
    // =========================

    public static final DeferredItem<ArmorItem> STEEL_HELMET =
            ITEMS.register("steel_helmet",
                    () -> new ArmorItem(
                            ModArmorMaterials.STEEL,
                            ArmorItem.Type.HELMET,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(24)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> STEEL_CHESTPLATE =
            ITEMS.register("steel_chestplate",
                    () -> new ArmorItem(
                            ModArmorMaterials.STEEL,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(24)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> STEEL_LEGGINGS =
            ITEMS.register("steel_leggings",
                    () -> new ArmorItem(
                            ModArmorMaterials.STEEL,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(24)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> STEEL_BOOTS =
            ITEMS.register("steel_boots",
                    () -> new ArmorItem(
                            ModArmorMaterials.STEEL,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(24)
                                    )
                    ));

    // =========================
    // MYTHRIL TOOLS
    // =========================

    public static final DeferredItem<PickaxeItem> MYTHRIL_PICKAXE =
            ITEMS.register("mythril_pickaxe",
                    () -> new PickaxeItem(
                            ModToolTiers.MYTHRIL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.MYTHRIL,
                                                    1.0F,
                                                    -2.8F
                                            )
                                    )
                    ));

    public static final DeferredItem<AxeItem> MYTHRIL_AXE =
            ITEMS.register("mythril_axe",
                    () -> new AxeItem(
                            ModToolTiers.MYTHRIL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.MYTHRIL,
                                                    1.5F,
                                                    -3.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<ShovelItem> MYTHRIL_SHOVEL =
            ITEMS.register("mythril_shovel",
                    () -> new ShovelItem(
                            ModToolTiers.MYTHRIL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.MYTHRIL,
                                                    1.5F,
                                                    -3.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<HoeItem> MYTHRIL_HOE =
            ITEMS.register("mythril_hoe",
                    () -> new HoeItem(
                            ModToolTiers.MYTHRIL,
                            new Item.Properties()
                                    .attributes(
                                            DiggerItem.createAttributes(
                                                    ModToolTiers.MYTHRIL,
                                                    -2.0F,
                                                    -1.0F
                                            )
                                    )
                    ));

    public static final DeferredItem<SwordItem> MYTHRIL_SWORD =
            ITEMS.register("mythril_sword",
                    () -> new SwordItem(
                            ModToolTiers.MYTHRIL,
                            new Item.Properties()
                                    .attributes(
                                            SwordItem.createAttributes(
                                                    ModToolTiers.MYTHRIL,
                                                    4,
                                                    -2.4F
                                            )
                                    )
                    ));

    // =========================
    // MYTHRIL ARMOR
    // =========================

    public static final DeferredItem<ArmorItem> MYTHRIL_HELMET =
            ITEMS.register("mythril_helmet",
                    () -> new ArmorItem(
                            ModArmorMaterials.MYTHRIL,
                            ArmorItem.Type.HELMET,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(40)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> MYTHRIL_CHESTPLATE =
            ITEMS.register("mythril_chestplate",
                    () -> new ArmorItem(
                            ModArmorMaterials.MYTHRIL,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(40)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> MYTHRIL_LEGGINGS =
            ITEMS.register("mythril_leggings",
                    () -> new ArmorItem(
                            ModArmorMaterials.MYTHRIL,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(40)
                                    )
                    ));

    public static final DeferredItem<ArmorItem> MYTHRIL_BOOTS =
            ITEMS.register("mythril_boots",
                    () -> new ArmorItem(
                            ModArmorMaterials.MYTHRIL,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties()
                                    .durability(
                                            ArmorItem.Type.HELMET.getDurability(40)
                                    )
                    ));

    // =========================
    // REGISTER
    // =========================

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}