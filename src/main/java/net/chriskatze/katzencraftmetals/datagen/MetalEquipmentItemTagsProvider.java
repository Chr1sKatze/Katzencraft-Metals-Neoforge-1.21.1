package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.config.ToolLevels;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.chriskatze.katzencraftmetals.metal.MetalEquipmentProfiles;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.data.tags.ItemTagsProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.util.concurrent.CompletableFuture;

public final class MetalEquipmentItemTagsProvider extends ItemTagsProvider {
    public MetalEquipmentItemTagsProvider(PackOutput out,CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<net.minecraft.data.tags.TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> blocks,ExistingFileHelper files){
        super(out,lookup,blocks,"katzencraftmetals",files);
    }
    @Override protected void addTags(HolderLookup.Provider lookup){
        tag(FoundryRecipeIngredients.DEEPSLATE).add(
                net.minecraft.world.item.Items.DEEPSLATE, net.minecraft.world.item.Items.COBBLED_DEEPSLATE,
                net.minecraft.world.item.Items.POLISHED_DEEPSLATE, net.minecraft.world.item.Items.DEEPSLATE_BRICKS,
                net.minecraft.world.item.Items.CRACKED_DEEPSLATE_BRICKS, net.minecraft.world.item.Items.DEEPSLATE_TILES,
                net.minecraft.world.item.Items.CRACKED_DEEPSLATE_TILES, net.minecraft.world.item.Items.CHISELED_DEEPSLATE);
        for(var form:net.chriskatze.katzencraftmetals.metal.StandardMetalForms.entries()) {
            if(form.form()!=net.chriskatze.katzencraftmetals.metal.MetalDefinition.Form.NUGGET)continue;
            var metalTag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c","nuggets/"+form.metal().id().getPath()));
            tag(metalTag).add(form.item());
            tag(net.neoforged.neoforge.common.Tags.Items.NUGGETS).addTag(metalTag);
        }
        for(int level=1;level<=8;level++)tag(ToolLevels.tag(level));
        for(var profile:MetalEquipmentProfiles.activeProfiles()){
            var id=ResourceLocation.fromNamespaceAndPath(profile.vanilla()?"minecraft":"katzencraftmetals",profile.id()+"_pickaxe");
            tag(ToolLevels.tag(profile.miningLevel())).add(BuiltInRegistries.ITEM.getOptional(id).orElseThrow());
        }
        tag(ItemTags.SWORDS).add(ModItems.COPPER_SWORD.get());
        tag(ItemTags.PICKAXES).add(ModItems.COPPER_PICKAXE.get());
        tag(ItemTags.AXES).add(ModItems.COPPER_AXE.get());
        tag(ItemTags.SHOVELS).add(ModItems.COPPER_SHOVEL.get());
        tag(ItemTags.HOES).add(ModItems.COPPER_HOE.get());
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COPPER_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COPPER_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COPPER_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COPPER_BOOTS.get());
    }
}

