package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KatzencraftMetalsMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PLATINUM_NUGGET.get());
        basicItem(ModItems.RAW_PLATINUM.get());
        basicItem(ModItems.PLATINUM_INGOT.get());
        basicItem(ModItems.PLATINUM_POWDER.get());

        basicItem(ModItems.MYTHRIL_NUGGET.get());
        basicItem(ModItems.RAW_MYTHRIL.get());
        basicItem(ModItems.MYTHRIL_INGOT.get());
        basicItem(ModItems.MYTHRIL_POWDER.get());

        basicItem(ModItems.SAPPHIRE_GEM.get());
        basicItem(ModItems.AMETHYST_GEM.get());
    }
}