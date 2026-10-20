package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.metal.StandardMetalForms;
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
        for (var form : StandardMetalForms.entries()) if (!form.form().block) basicItem(form.item());




        basicItem(ModItems.RAW_PLATINUM.get());

        basicItem(ModItems.RAW_MYTHRIL.get());

        basicItem(ModItems.SAPPHIRE_GEM.get());
        basicItem(ModItems.AMETHYST_GEM.get());

        // Explicitly approved temporary iron artwork; no authored texture files are changed.
        withExistingParent("copper_sword",mcLoc("item/handheld")).texture("layer0",mcLoc("item/iron_sword"));
        withExistingParent("copper_pickaxe",mcLoc("item/handheld")).texture("layer0",mcLoc("item/iron_pickaxe"));
        withExistingParent("copper_axe",mcLoc("item/handheld")).texture("layer0",mcLoc("item/iron_axe"));
        withExistingParent("copper_shovel",mcLoc("item/handheld")).texture("layer0",mcLoc("item/iron_shovel"));
        withExistingParent("copper_hoe",mcLoc("item/handheld")).texture("layer0",mcLoc("item/iron_hoe"));
        withExistingParent("copper_helmet",mcLoc("item/generated")).texture("layer0",mcLoc("item/iron_helmet"));
        withExistingParent("copper_chestplate",mcLoc("item/generated")).texture("layer0",mcLoc("item/iron_chestplate"));
        withExistingParent("copper_leggings",mcLoc("item/generated")).texture("layer0",mcLoc("item/iron_leggings"));
        withExistingParent("copper_boots",mcLoc("item/generated")).texture("layer0",mcLoc("item/iron_boots"));

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