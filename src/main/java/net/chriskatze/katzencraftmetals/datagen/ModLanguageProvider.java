package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, KatzencraftMetalsMod.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Items
        add(ModItems.CRUSHED_COAL.get(), "Crushed Coal");

        add(ModItems.CRUSHED_IRON_ORE.get(), "Crushed Iron Ore");

        add(ModItems.STEEL_NUGGET.get(), "Steel Nugget");
        add(ModItems.STEEL_INGOT.get(), "Steel Ingot");
        add(ModItems.STEEL_CHARGE.get(), "Steel Charge");

        add(ModItems.PLATINUM_NUGGET.get(), "Platinum Nugget");
        add(ModItems.RAW_PLATINUM.get(), "Raw Platinum");
        add(ModItems.PLATINUM_INGOT.get(), "Platinum Ingot");
        add(ModItems.CRUSHED_PLATINUM_ORE.get(), "Crushed Platinum Ore");

        add(ModItems.MYTHRIL_NUGGET.get(), "Mythril Nugget");
        add(ModItems.RAW_MYTHRIL.get(), "Raw Mythril");
        add(ModItems.MYTHRIL_INGOT.get(), "Mythril Ingot");
        add(ModItems.CRUSHED_MYTHRIL_ORE.get(), "Crushed mythril Ore");

        add(ModItems.SAPPHIRE_GEM.get(), "Sapphire");
        add(ModItems.AMETHYST_GEM.get(), "Amethyst");

        add(ModItems.STEEL_SWORD.get(), "Steel Sword");
        add(ModItems.STEEL_PICKAXE.get(), "Steel Pickaxe");
        add(ModItems.STEEL_AXE.get(), "Steel Axe");
        add(ModItems.STEEL_SHOVEL.get(), "Steel Shovel");
        add(ModItems.STEEL_HOE.get(), "Steel Sword");
        add(ModItems.STEEL_HELMET.get(), "Steel Helmet");
        add(ModItems.STEEL_CHESTPLATE.get(), "Steel Chestplate");
        add(ModItems.STEEL_LEGGINGS.get(), "Steel Leggings");
        add(ModItems.STEEL_BOOTS.get(), "Steel Boots");
        add(ModItems.STEEL_HORSE_ARMOR.get(), "Steel Horse Armor");

        add(ModItems.MYTHRIL_SWORD.get(), "Mythril Sword");
        add(ModItems.MYTHRIL_PICKAXE.get(), "Mythril Pickaxe");
        add(ModItems.MYTHRIL_AXE.get(), "Mythril Axe");
        add(ModItems.MYTHRIL_SHOVEL.get(), "Mythril Shovel");
        add(ModItems.MYTHRIL_HOE.get(), "Mythril Hoe");
        add(ModItems.MYTHRIL_HELMET.get(), "Mythril Helmet");
        add(ModItems.MYTHRIL_CHESTPLATE.get(), "Mythril Chestplate");
        add(ModItems.MYTHRIL_LEGGINGS.get(), "Mythril Leggings");
        add(ModItems.MYTHRIL_BOOTS.get(), "Mythril Boots");

        // Blocks
        add(ModBlocks.STEEL_BLOCK.get(), "Steel Block");
        add(ModBlocks.CUT_STEEL_BLOCK.get(), "Cut Steel Block");
        add(ModBlocks.STEEL_TRAPDOOR.get(), "Steel Trapdoor");
        add(ModBlocks.STEEL_DOOR.get(), "Steel Door");
        add(ModBlocks.STEEL_CHAIN.get(), "Steel Chain");
        add(ModBlocks.STEEL_BARS.get(), "Steel Bars");
        add(ModBlocks.STEEL_BUTTON.get(), "Steel Button");
        add(ModBlocks.STEEL_LEVER.get(), "Steel Lever");
        add(ModBlocks.STEEL_PRESSURE_PLATE.get(), "Steel Pressure Plate");
        add(ModBlocks.CUT_STEEL_STAIRS.get(), "Cut Steel Stairs");
        add(ModBlocks.CUT_STEEL_SLAB.get(), "Cut Steel Slab");
        add(ModBlocks.STEEL_LADDER.get(), "Steel Ladder");

        add(ModBlocks.PLATINUM_BLOCK.get(), "Platinum Block");
        add(ModBlocks.PLATINUM_ORE.get(), "Platinum Ore");
        add(ModBlocks.DEEPSLATE_PLATINUM_ORE.get(), "Deepslate Platinum Ore");
        add(ModBlocks.NETHER_PLATINUM_ORE.get(), "Nether Platinum Ore");
        add(ModBlocks.END_PLATINUM_ORE.get(), "End Platinum Ore");

        add(ModBlocks.MYTHRIL_BLOCK.get(), "Mythril Block");
        add(ModBlocks.MYTHRIL_ORE.get(), "Mythril Ore");
        add(ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(), "Deepslate Mythril Ore");
        add(ModBlocks.NETHER_MYTHRIL_ORE.get(), "Nether Mythril Ore");
        add(ModBlocks.END_MYTHRIL_ORE.get(), "End Mythril Ore");

        add(ModBlocks.CRUSHER.get(), "Crusher");
        add(ModBlocks.FUEL_CHAMBER.get(), "Fuel Chamber");
        add(ModBlocks.FOUNDRY_CONTROLLER.get(), "Foundry Controller");
        add(ModBlocks.FOUNDRY_TANK.get(), "Foundry Tank");
        add(ModBlocks.CASTING_CAULDRON.get(), "Casting Cauldron");
        add(ModBlocks.FOUNDRY_FAUCET.get(), "Foundry Faucet");

        // Molten metals
        add("metal.katzencraftmetals.iron", "Iron");
        add("metal.katzencraftmetals.copper", "Copper");

        // Foundry Controller GUI
        add("gui.katzencraftmetals.foundry.empty", "Empty");
        add("gui.katzencraftmetals.foundry.ore_amount", "%s ore");
        add("gui.katzencraftmetals.foundry.select_output", "Click to select output");
        add("gui.katzencraftmetals.foundry.selected_output", "Selected output");
    }
}
