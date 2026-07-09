package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.datagen.recipe.CrusherRecipeBuilder;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ModFoundryRecipeProvider.build(recipeOutput);
        ModMetalRecipeProvider.build(recipeOutput);
        ModEquipmentRecipeProvider.build(recipeOutput);

        // =========================
        // NORMAL SHAPELESS
        // =========================

        // Coal -> Crushed Coal
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.CRUSHED_COAL.get(),
                        3
                )
                .requires(Items.COAL)
                .unlockedBy(
                        "has_coal",
                        has(Items.COAL)
                )
                .save(recipeOutput);

        // Crushed Coal -> Coal
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        Items.COAL
                )
                .requires(ModItems.CRUSHED_COAL.get(), 3)
                .unlockedBy(
                        "has_crushed_coal",
                        has(ModItems.CRUSHED_COAL.get())
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID + ":coal_from_crushed_coal"
                );

        // Crushed Iron Ore + Crushed Coal -> Steel Charge
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.STEEL_CHARGE.get()
                )
                .requires(ModItems.CRUSHED_IRON_ORE.get())
                .requires(ModItems.CRUSHED_COAL.get())
                .unlockedBy(
                        "has_crushed_iron_ore",
                        has(ModItems.CRUSHED_IRON_ORE.get())
                )
                .unlockedBy(
                        "has_crushed_coal",
                        has(ModItems.CRUSHED_COAL.get())
                )
                .save(recipeOutput);

        // =========================
        // BLAST FURNACE - BUILDING MATERIALS
        // =========================

        // Sand -> Glass
        oreBlasting(recipeOutput,
                Items.SAND,
                RecipeCategory.BUILDING_BLOCKS,
                Items.GLASS,
                0.1f,
                100,
                "glass");

        oreBlasting(recipeOutput,
                Items.RED_SAND,
                RecipeCategory.BUILDING_BLOCKS,
                Items.GLASS,
                0.1f,
                100,
                "glass");

        // Cobblestone -> Stone
        oreBlasting(recipeOutput,
                Items.COBBLESTONE,
                RecipeCategory.BUILDING_BLOCKS,
                Items.STONE,
                0.1f,
                100,
                "stone");

        // Stone -> Smooth Stone
        oreBlasting(recipeOutput,
                Items.STONE,
                RecipeCategory.BUILDING_BLOCKS,
                Items.SMOOTH_STONE,
                0.1f,
                100,
                "smooth_stone");

        // Sandstone -> Smooth Sandstone
        oreBlasting(recipeOutput,
                Items.SANDSTONE,
                RecipeCategory.BUILDING_BLOCKS,
                Items.SMOOTH_SANDSTONE,
                0.1f,
                100,
                "smooth_sandstone");

        // Red Sandstone -> Smooth Red Sandstone
        oreBlasting(recipeOutput,
                Items.RED_SANDSTONE,
                RecipeCategory.BUILDING_BLOCKS,
                Items.SMOOTH_RED_SANDSTONE,
                0.1f,
                100,
                "smooth_red_sandstone");

        // Quartz Block -> Smooth Quartz
        oreBlasting(recipeOutput,
                Items.QUARTZ_BLOCK,
                RecipeCategory.BUILDING_BLOCKS,
                Items.SMOOTH_QUARTZ,
                0.1f,
                100,
                "smooth_quartz");

        // Netherrack -> Nether Brick
        oreBlasting(recipeOutput,
                Items.NETHERRACK,
                RecipeCategory.BUILDING_BLOCKS,
                Items.NETHER_BRICK,
                0.1f,
                100,
                "nether_brick");

        // Stone Bricks -> Cracked Stone Bricks
        oreBlasting(recipeOutput,
                Items.STONE_BRICKS,
                RecipeCategory.BUILDING_BLOCKS,
                Items.CRACKED_STONE_BRICKS,
                0.1f,
                100,
                "cracked_stone_bricks");

        // Nether Bricks -> Cracked Nether Bricks
        oreBlasting(recipeOutput,
                Items.NETHER_BRICKS,
                RecipeCategory.BUILDING_BLOCKS,
                Items.CRACKED_NETHER_BRICKS,
                0.1f,
                100,
                "cracked_nether_bricks");

        // Deepslate Bricks -> Cracked Deepslate Bricks
        oreBlasting(recipeOutput,
                Items.DEEPSLATE_BRICKS,
                RecipeCategory.BUILDING_BLOCKS,
                Items.CRACKED_DEEPSLATE_BRICKS,
                0.1f,
                100,
                "cracked_deepslate_bricks");

        // Deepslate Tiles -> Cracked Deepslate Tiles
        oreBlasting(recipeOutput,
                Items.DEEPSLATE_TILES,
                RecipeCategory.BUILDING_BLOCKS,
                Items.CRACKED_DEEPSLATE_TILES,
                0.1f,
                100,
                "cracked_deepslate_tiles");

        // Polished Blackstone Bricks -> Cracked Polished Blackstone Bricks
        oreBlasting(recipeOutput,
                Items.POLISHED_BLACKSTONE_BRICKS,
                RecipeCategory.BUILDING_BLOCKS,
                Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                0.1f,
                100,
                "cracked_polished_blackstone_bricks");

        // =========================
        // CUT BUILDING BLOCK FAMILIES
        // =========================

        cutBlockFamily(
                recipeOutput,
                ModBlocks.STEEL_BLOCK.get(),
                ModBlocks.CUT_STEEL_BLOCK.get(),
                ModBlocks.CUT_STEEL_STAIRS.get(),
                ModBlocks.CUT_STEEL_SLAB.get(),
                "steel"
        );
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, net.minecraft.world.item.Item input, RecipeCategory category,
                                      net.minecraft.world.item.Item result, float experience, int cookingTime, String group) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(input), category, result, experience, cookingTime, net.minecraft.world.item.crafting.RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new)
                .group(group)
                .unlockedBy("has_" + input.builtInRegistryHolder().key().location().getPath(), has(input))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":" + result.builtInRegistryHolder().key().location().getPath() + "_from_blasting_" + input.builtInRegistryHolder().key().location().getPath());
    }

    // =========================
    // CUT BLOCK FAMILY
    // =========================
    private void cutBlockFamily(
            RecipeOutput recipeOutput,
            Block baseBlock,
            Block cutBlock,
            Block stairs,
            Block slab,
            String materialName
    ) {
        String baseName = materialName + "_block";
        String cutName = "cut_" + materialName + "_block";
        String stairsName = "cut_" + materialName + "_stairs";
        String slabName = "cut_" + materialName + "_slab";

        // 4 base blocks -> 4 cut blocks
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.BUILDING_BLOCKS,
                        cutBlock,
                        4
                )
                .pattern("BB")
                .pattern("BB")
                .define('B', baseBlock)
                .unlockedBy(
                        "has_" + baseName,
                        has(baseBlock)
                )
                .save(recipeOutput);

        // 6 cut blocks -> 4 stairs
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.BUILDING_BLOCKS,
                        stairs,
                        4
                )
                .pattern("C  ")
                .pattern("CC ")
                .pattern("CCC")
                .define('C', cutBlock)
                .unlockedBy(
                        "has_" + cutName,
                        has(cutBlock)
                )
                .save(recipeOutput);

        // 3 cut blocks -> 6 slabs
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.BUILDING_BLOCKS,
                        slab,
                        6
                )
                .pattern("CCC")
                .define('C', cutBlock)
                .unlockedBy(
                        "has_" + cutName,
                        has(cutBlock)
                )
                .save(recipeOutput);

        // =========================
        // STONECUTTER: BASE BLOCK
        // =========================

        // 1 base block -> 4 cut blocks
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(baseBlock),
                        RecipeCategory.BUILDING_BLOCKS,
                        cutBlock,
                        4
                )
                .unlockedBy(
                        "has_" + baseName,
                        has(baseBlock)
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + cutName
                                + "_from_"
                                + baseName
                                + "_stonecutting"
                );

        // 1 base block -> 4 cut stairs
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(baseBlock),
                        RecipeCategory.BUILDING_BLOCKS,
                        stairs,
                        4
                )
                .unlockedBy(
                        "has_" + baseName,
                        has(baseBlock)
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + stairsName
                                + "_from_"
                                + baseName
                                + "_stonecutting"
                );

        // 1 base block -> 8 cut slabs
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(baseBlock),
                        RecipeCategory.BUILDING_BLOCKS,
                        slab,
                        8
                )
                .unlockedBy(
                        "has_" + baseName,
                        has(baseBlock)
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + slabName
                                + "_from_"
                                + baseName
                                + "_stonecutting"
                );

        // =========================
        // STONECUTTER: CUT BLOCK
        // =========================

        // 1 cut block -> 1 stair
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(cutBlock),
                        RecipeCategory.BUILDING_BLOCKS,
                        stairs
                )
                .unlockedBy(
                        "has_" + cutName,
                        has(cutBlock)
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + stairsName
                                + "_from_"
                                + cutName
                                + "_stonecutting"
                );

        // 1 cut block -> 2 slabs
        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(cutBlock),
                        RecipeCategory.BUILDING_BLOCKS,
                        slab,
                        2
                )
                .unlockedBy(
                        "has_" + cutName,
                        has(cutBlock)
                )
                .save(
                        recipeOutput,
                        KatzencraftMetalsMod.MODID
                                + ":"
                                + slabName
                                + "_from_"
                                + cutName
                                + "_stonecutting"
                );
    }
}