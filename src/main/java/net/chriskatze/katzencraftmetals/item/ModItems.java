package net.chriskatze.katzencraftmetals.item;

import net.chriskatze.katzencraftmetals.metal.StandardMetalForms;
import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles.ToolKind;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(KatzencraftMetalsMod.MODID);

    private static final java.util.Map<String, DeferredItem<Item>> STANDARD_ITEMS = new java.util.LinkedHashMap<>();
    private static DeferredItem<Item> standardItem(String path) {
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, path);
        var form = StandardMetalForms.find(id).orElseThrow();
        if (form.form().block) throw new IllegalArgumentException("Expected ingot/nugget: " + path);
        return STANDARD_ITEMS.computeIfAbsent(path, ITEMS::registerSimpleItem);
    }

    // =========================
    // STEEL
    // =========================

    public static final DeferredItem<Item> STEEL_NUGGET =
            standardItem("steel_nugget");

    public static final DeferredItem<Item> STEEL_INGOT =
            standardItem("steel_ingot");

    public static final DeferredItem<BlockItem> STEEL_DOOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_DOOR);

    public static final DeferredItem<BlockItem> STEEL_TRAPDOOR_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_TRAPDOOR);

    public static final DeferredItem<BlockItem> STEEL_LEVER_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_LEVER);

    public static final DeferredItem<BlockItem> STEEL_BARS_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_BARS);

    public static final DeferredItem<BlockItem> STEEL_CHAIN_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_CHAIN);

    public static final DeferredItem<BlockItem> STEEL_PRESSURE_PLATE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_PRESSURE_PLATE);

    public static final DeferredItem<BlockItem> STEEL_BUTTON_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_BUTTON);

    public static final DeferredItem<BlockItem> STEEL_LADDER_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_LADDER);

    // =========================
    // PLATINUM
    // =========================

    public static final DeferredItem<Item> PLATINUM_NUGGET =
            standardItem("platinum_nugget");

    public static final DeferredItem<Item> RAW_PLATINUM =
            ITEMS.registerSimpleItem("raw_platinum");

    public static final DeferredItem<Item> PLATINUM_INGOT =
            standardItem("platinum_ingot");

    // =========================
    // MYTHRIL
    // =========================

    public static final DeferredItem<Item> MYTHRIL_NUGGET =
            standardItem("mythril_nugget");

    public static final DeferredItem<Item> RAW_MYTHRIL =
            ITEMS.registerSimpleItem("raw_mythril");

    public static final DeferredItem<Item> MYTHRIL_INGOT =
            standardItem("mythril_ingot");

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
            ITEMS.register("steel_pickaxe", () -> new PickaxeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(ToolKind.PICKAXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"), ToolKind.PICKAXE)));

    public static final DeferredItem<AxeItem> STEEL_AXE =
            ITEMS.register("steel_axe", () -> new AxeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(ToolKind.AXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"), ToolKind.AXE)));

    public static final DeferredItem<ShovelItem> STEEL_SHOVEL =
            ITEMS.register("steel_shovel", () -> new ShovelItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(ToolKind.SHOVEL),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"), ToolKind.SHOVEL)));

    public static final DeferredItem<HoeItem> STEEL_HOE =
            ITEMS.register("steel_hoe", () -> new HoeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(ToolKind.HOE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"), ToolKind.HOE)));

    public static final DeferredItem<SwordItem> STEEL_SWORD =
            ITEMS.register("steel_sword", () -> new SwordItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").tier(ToolKind.SWORD),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel"), ToolKind.SWORD)));

    // =========================
    // STEEL ARMOR
    // =========================

    public static final DeferredItem<ArmorItem> STEEL_HELMET =
            ITEMS.register("steel_helmet", () -> new ArmorItem(
                    ModArmorMaterials.STEEL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").armorDurability(ArmorItem.Type.HELMET))));

    public static final DeferredItem<ArmorItem> STEEL_CHESTPLATE =
            ITEMS.register("steel_chestplate", () -> new ArmorItem(
                    ModArmorMaterials.STEEL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").armorDurability(ArmorItem.Type.CHESTPLATE))));

    public static final DeferredItem<ArmorItem> STEEL_LEGGINGS =
            ITEMS.register("steel_leggings", () -> new ArmorItem(
                    ModArmorMaterials.STEEL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").armorDurability(ArmorItem.Type.LEGGINGS))));

    public static final DeferredItem<ArmorItem> STEEL_BOOTS =
            ITEMS.register("steel_boots", () -> new ArmorItem(
                    ModArmorMaterials.STEEL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:steel").armorDurability(ArmorItem.Type.BOOTS))));

    public static final DeferredItem<AnimalArmorItem> STEEL_HORSE_ARMOR =
            ITEMS.register(
                    "steel_horse_armor",
                    () -> new AnimalArmorItem(
                            ModArmorMaterials.STEEL,
                            AnimalArmorItem.BodyType.EQUESTRIAN,
                            false,
                            new Item.Properties().stacksTo(1)
                    )
            );

    // =========================
    // MYTHRIL TOOLS
    // =========================

    public static final DeferredItem<PickaxeItem> MYTHRIL_PICKAXE =
            ITEMS.register("mythril_pickaxe", () -> new PickaxeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(ToolKind.PICKAXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"), ToolKind.PICKAXE)));

    public static final DeferredItem<AxeItem> MYTHRIL_AXE =
            ITEMS.register("mythril_axe", () -> new AxeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(ToolKind.AXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"), ToolKind.AXE)));

    public static final DeferredItem<ShovelItem> MYTHRIL_SHOVEL =
            ITEMS.register("mythril_shovel", () -> new ShovelItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(ToolKind.SHOVEL),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"), ToolKind.SHOVEL)));

    public static final DeferredItem<HoeItem> MYTHRIL_HOE =
            ITEMS.register("mythril_hoe", () -> new HoeItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(ToolKind.HOE),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"), ToolKind.HOE)));

    public static final DeferredItem<SwordItem> MYTHRIL_SWORD =
            ITEMS.register("mythril_sword", () -> new SwordItem(
                    MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").tier(ToolKind.SWORD),
                    toolProperties(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril"), ToolKind.SWORD)));

    // =========================
    // MYTHRIL ARMOR
    // =========================

    public static final DeferredItem<ArmorItem> MYTHRIL_HELMET =
            ITEMS.register("mythril_helmet", () -> new ArmorItem(
                    ModArmorMaterials.MYTHRIL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").armorDurability(ArmorItem.Type.HELMET))));

    public static final DeferredItem<ArmorItem> MYTHRIL_CHESTPLATE =
            ITEMS.register("mythril_chestplate", () -> new ArmorItem(
                    ModArmorMaterials.MYTHRIL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").armorDurability(ArmorItem.Type.CHESTPLATE))));

    public static final DeferredItem<ArmorItem> MYTHRIL_LEGGINGS =
            ITEMS.register("mythril_leggings", () -> new ArmorItem(
                    ModArmorMaterials.MYTHRIL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").armorDurability(ArmorItem.Type.LEGGINGS))));

    public static final DeferredItem<ArmorItem> MYTHRIL_BOOTS =
            ITEMS.register("mythril_boots", () -> new ArmorItem(
                    ModArmorMaterials.MYTHRIL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("katzencraftmetals:mythril").armorDurability(ArmorItem.Type.BOOTS))));

    public static final DeferredItem<SwordItem> COPPER_SWORD =
            ITEMS.register("copper_sword", () -> new SwordItem(MetalEquipmentProfiles.forMetal("minecraft:copper").tier(ToolKind.SWORD),
                    toolProperties(MetalEquipmentProfiles.forMetal("minecraft:copper"),ToolKind.SWORD)));
    public static final DeferredItem<PickaxeItem> COPPER_PICKAXE =
            ITEMS.register("copper_pickaxe", () -> new PickaxeItem(MetalEquipmentProfiles.forMetal("minecraft:copper").tier(ToolKind.PICKAXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("minecraft:copper"),ToolKind.PICKAXE)));
    public static final DeferredItem<AxeItem> COPPER_AXE =
            ITEMS.register("copper_axe", () -> new AxeItem(MetalEquipmentProfiles.forMetal("minecraft:copper").tier(ToolKind.AXE),
                    toolProperties(MetalEquipmentProfiles.forMetal("minecraft:copper"),ToolKind.AXE)));
    public static final DeferredItem<ShovelItem> COPPER_SHOVEL =
            ITEMS.register("copper_shovel", () -> new ShovelItem(MetalEquipmentProfiles.forMetal("minecraft:copper").tier(ToolKind.SHOVEL),
                    toolProperties(MetalEquipmentProfiles.forMetal("minecraft:copper"),ToolKind.SHOVEL)));
    public static final DeferredItem<HoeItem> COPPER_HOE =
            ITEMS.register("copper_hoe", () -> new HoeItem(MetalEquipmentProfiles.forMetal("minecraft:copper").tier(ToolKind.HOE),
                    toolProperties(MetalEquipmentProfiles.forMetal("minecraft:copper"),ToolKind.HOE)));
    public static final DeferredItem<ArmorItem> COPPER_HELMET =
            ITEMS.register("copper_helmet", () -> new ArmorItem(ModArmorMaterials.COPPER,ArmorItem.Type.HELMET,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("minecraft:copper").armorDurability(ArmorItem.Type.HELMET))));
    public static final DeferredItem<ArmorItem> COPPER_CHESTPLATE =
            ITEMS.register("copper_chestplate", () -> new ArmorItem(ModArmorMaterials.COPPER,ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("minecraft:copper").armorDurability(ArmorItem.Type.CHESTPLATE))));
    public static final DeferredItem<ArmorItem> COPPER_LEGGINGS =
            ITEMS.register("copper_leggings", () -> new ArmorItem(ModArmorMaterials.COPPER,ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("minecraft:copper").armorDurability(ArmorItem.Type.LEGGINGS))));
    public static final DeferredItem<ArmorItem> COPPER_BOOTS =
            ITEMS.register("copper_boots", () -> new ArmorItem(ModArmorMaterials.COPPER,ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(MetalEquipmentProfiles.forMetal("minecraft:copper").armorDurability(ArmorItem.Type.BOOTS))));

    /** Convert total player values to additive modifiers, preserving native tier damage. */
    private static Item.Properties toolProperties(MetalEquipmentProfiles.Profile profile, ToolKind kind) {
        var stats=profile.tool(kind);
        return new Item.Properties().attributes(DiggerItem.createAttributes(profile.tier(kind),
                stats.damage()-1-profile.tierAttackBonus(), stats.attacksPerSecond()-4));
    }

    // =========================
    // REGISTER
    // =========================

    public static void register(IEventBus modEventBus) {
        for (var form : StandardMetalForms.entries()) {
            if (!form.form().block) standardItem(form.id().getPath());
        }
        ITEMS.register(modEventBus);
    }
}