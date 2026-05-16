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
        add(ModItems.PLATINUM_NUGGET.get(), "Platinum Nugget");
        add(ModItems.RAW_PLATINUM.get(), "Raw Platinum");
        add(ModItems.PLATINUM_INGOT.get(), "Platinum Ingot");
        add(ModItems.PLATINUM_POWDER.get(), "Platinum Powder");

        add(ModItems.MYTHRIL_NUGGET.get(), "Mythril Nugget");
        add(ModItems.RAW_MYTHRIL.get(), "Raw Mythril");
        add(ModItems.MYTHRIL_INGOT.get(), "Mythril Ingot");
        add(ModItems.MYTHRIL_POWDER.get(), "Mythril Powder");

        add(ModItems.SAPPHIRE_GEM.get(), "Sapphire");
        add(ModItems.AMETHYST_GEM.get(), "Amethyst");

        add(ModItems.COMMON_WEAPON_SCROLL.get(), "Common Weapon Scroll");

        // Blocks
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

        // Enchantments
        add("enchantment.katzencraftmetals.critical_chance", "Critical Chance");
        add("enchantment.katzencraftmetals.critical_damage", "Critical Damage");
        add("enchantment.katzencraftmetals.attack_speed", "Attack Speed");
        add("enchantment.katzencraftmetals.attack_damage", "Attack Damage");
    }
}