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

        basicItem(ModItems.CRUSHED_COAL.get());

        basicItem(ModItems.CRUSHED_IRON_ORE.get());

        basicItem(ModItems.STEEL_NUGGET.get());
        basicItem(ModItems.STEEL_INGOT.get());
        basicItem(ModItems.STEEL_CHARGE.get());

        basicItem(ModItems.PLATINUM_NUGGET.get());
        basicItem(ModItems.RAW_PLATINUM.get());
        basicItem(ModItems.PLATINUM_INGOT.get());
        basicItem(ModItems.CRUSHED_PLATINUM_ORE.get());

        basicItem(ModItems.MYTHRIL_NUGGET.get());
        basicItem(ModItems.RAW_MYTHRIL.get());
        basicItem(ModItems.MYTHRIL_INGOT.get());
        basicItem(ModItems.CRUSHED_MYTHRIL_ORE.get());

        basicItem(ModItems.SAPPHIRE_GEM.get());
        basicItem(ModItems.AMETHYST_GEM.get());

        handheldItem(ModItems.STEEL_PICKAXE);
        handheldItem(ModItems.STEEL_AXE);
        handheldItem(ModItems.STEEL_SHOVEL);
        handheldItem(ModItems.STEEL_HOE);
        handheldItem(ModItems.STEEL_SWORD);
        basicItem(ModItems.STEEL_HELMET.get());
        basicItem(ModItems.STEEL_CHESTPLATE.get());
        basicItem(ModItems.STEEL_LEGGINGS.get());
        basicItem(ModItems.STEEL_BOOTS.get());
        basicItem(ModItems.STEEL_HORSE_ARMOR.get());

        handheldItem(ModItems.MYTHRIL_PICKAXE);
        handheldItem(ModItems.MYTHRIL_AXE);
        handheldItem(ModItems.MYTHRIL_SHOVEL);
        handheldItem(ModItems.MYTHRIL_HOE);
        handheldItem(ModItems.MYTHRIL_SWORD);
        basicItem(ModItems.MYTHRIL_HELMET.get());
        basicItem(ModItems.MYTHRIL_CHESTPLATE.get());
        basicItem(ModItems.MYTHRIL_LEGGINGS.get());
        basicItem(ModItems.MYTHRIL_BOOTS.get());
    }

    private void handheldItem(net.neoforged.neoforge.registries.DeferredItem<?> item) {
        withExistingParent(
                item.getId().getPath(),
                mcLoc("item/handheld")
        ).texture(
                "layer0",
                modLoc("item/" + item.getId().getPath())
        );
    }

    private void blockItem(net.neoforged.neoforge.registries.DeferredItem<?> item) {
        withExistingParent(
                item.getId().getPath(),
                modLoc("block/" + item.getId().getPath())
        );
    }
}