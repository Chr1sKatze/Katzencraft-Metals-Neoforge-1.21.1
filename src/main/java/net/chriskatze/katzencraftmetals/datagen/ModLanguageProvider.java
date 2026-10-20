package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {

    private final String locale;
    public ModLanguageProvider(PackOutput output) { this(output, "en_us"); }
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, KatzencraftMetalsMod.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        MetalLanguageEntries.entries(net.chriskatze.katzencraftmetals.metal.MetalCatalog.values(), locale).forEach(this::add);
        add("tooltip.katzencraftmetals.foundry_recovery", locale.equals("de_de")
                ? "Gießerei-Rückgewinnung: %s %s-Erz" : "Foundry Recovery: %s %s Ore");
        add(ModItems.COPPER_SWORD.get(), locale.equals("de_de") ? "Kupferschwert" : "Copper Sword");
        add(ModItems.COPPER_PICKAXE.get(), locale.equals("de_de") ? "Kupferspitzhacke" : "Copper Pickaxe");
        add(ModItems.COPPER_AXE.get(), locale.equals("de_de") ? "Kupferaxt" : "Copper Axe");
        add(ModItems.COPPER_SHOVEL.get(), locale.equals("de_de") ? "Kupferschaufel" : "Copper Shovel");
        add(ModItems.COPPER_HOE.get(), locale.equals("de_de") ? "Kupferhacke" : "Copper Hoe");
        add(ModItems.COPPER_HELMET.get(), locale.equals("de_de") ? "Kupferhelm" : "Copper Helmet");
        add(ModItems.COPPER_CHESTPLATE.get(), locale.equals("de_de") ? "Kupferbrustpanzer" : "Copper Chestplate");
        add(ModItems.COPPER_LEGGINGS.get(), locale.equals("de_de") ? "Kupferbeinschutz" : "Copper Leggings");
        add(ModItems.COPPER_BOOTS.get(), locale.equals("de_de") ? "Kupferstiefel" : "Copper Boots");
        if (!locale.equals("en_us")) return;
        // Items

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

        add(ModBlocks.FUEL_CHAMBER.get(), "Fuel Chamber");
        add(ModBlocks.FOUNDRY_CONTROLLER.get(), "Foundry Controller");
        add(ModBlocks.FOUNDRY_TANK.get(), "Foundry Tank");
        add(ModBlocks.FOUNDRY_BASIN.get(), "Foundry Basin");
        add(ModBlocks.FOUNDRY_TABLE.get(), "Foundry Table");
        add(ModBlocks.FOUNDRY_FAUCET.get(), "Foundry Faucet");

        // Molten metals

        // Foundry Controller GUI
        add("gui.katzencraftmetals.foundry.empty", "Empty");
        add("gui.katzencraftmetals.foundry.ore_amount", "%s ore");
        add("gui.katzencraftmetals.foundry.select_output", "Click to select output");
        add("gui.katzencraftmetals.foundry.selected_output", "Selected output");
    }
}
