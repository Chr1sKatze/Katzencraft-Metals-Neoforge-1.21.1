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
        // BLAST FURNACE
        // =========================

        // Crushed Platinum Ore -> Platinum Nugget
        oreBlasting(
                recipeOutput,
                ModItems.CRUSHED_PLATINUM_ORE.get(),
                RecipeCategory.MISC,
                ModItems.PLATINUM_NUGGET.get(),
                0.0f,
                100,
                "platinum"
        );

        // Crushed Mythril Ore -> Mythril Nugget
        oreBlasting(
                recipeOutput,
                ModItems.CRUSHED_MYTHRIL_ORE.get(),
                RecipeCategory.MISC,
                ModItems.MYTHRIL_NUGGET.get(),
                0.0f,
                100,
                "mythril"
        );

        // Steel Charge -> Steel Nugget
        oreBlasting(
                recipeOutput,
                ModItems.STEEL_CHARGE.get(),
                RecipeCategory.MISC,
                ModItems.STEEL_NUGGET.get(),
                0.0f,
                100,
                "steel"
        );

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
        // BLOCK <-> INGOT
        // =========================

        // Steel
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STEEL_BLOCK.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.STEEL_INGOT.get(), 9)
                .requires(ModBlocks.STEEL_BLOCK.get())
                .unlockedBy("has_steel_block", has(ModBlocks.STEEL_BLOCK.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":steel_ingot_from_block");

        // Platinum
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLATINUM_BLOCK.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.PLATINUM_INGOT.get())
                .unlockedBy("has_platinum_ingot", has(ModItems.PLATINUM_INGOT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get(), 9)
                .requires(ModBlocks.PLATINUM_BLOCK.get())
                .unlockedBy("has_platinum_block", has(ModBlocks.PLATINUM_BLOCK.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_ingot_from_block");

        // Mythril
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MYTHRIL_BLOCK.get())
                .pattern("III")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get(), 9)
                .requires(ModBlocks.MYTHRIL_BLOCK.get())
                .unlockedBy("has_mythril_block", has(ModBlocks.MYTHRIL_BLOCK.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_ingot_from_block");

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

        // =========================
        // STEEL TOOLS
        // =========================

        // Sword
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_SWORD.get())
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ModItems.STEEL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Pickaxe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STEEL_PICKAXE.get())
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', ModItems.STEEL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Axe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STEEL_AXE.get())
                .pattern("II")
                .pattern("IS")
                .pattern(" S")
                .define('I', ModItems.STEEL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Shovel
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STEEL_SHOVEL.get())
                .pattern("I")
                .pattern("S")
                .pattern("S")
                .define('I', ModItems.STEEL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Hoe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STEEL_HOE.get())
                .pattern("II")
                .pattern(" S")
                .pattern(" S")
                .define('I', ModItems.STEEL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // =========================
        // STEEL ARMOR
        // =========================

        // Helmet
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_HELMET.get())
                .pattern("III")
                .pattern("I I")
                .define('I', ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Chestplate
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_CHESTPLATE.get())
                .pattern("I I")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Leggings
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_LEGGINGS.get())
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // Boots
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.STEEL_BOOTS.get())
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput);

        // =========================
        // MYTHRIL TOOLS
        // =========================

        // Sword
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_SWORD.get())
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Pickaxe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_PICKAXE.get())
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Axe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_AXE.get())
                .pattern("II")
                .pattern("IS")
                .pattern(" S")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Shovel
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_SHOVEL.get())
                .pattern("I")
                .pattern("S")
                .pattern("S")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Hoe
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_HOE.get())
                .pattern("II")
                .pattern(" S")
                .pattern(" S")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // =========================
        // MYTHRIL ARMOR
        // =========================

        // Helmet
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_HELMET.get())
                .pattern("III")
                .pattern("I I")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Chestplate
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_CHESTPLATE.get())
                .pattern("I I")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Leggings
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_LEGGINGS.get())
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // Boots
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_BOOTS.get())
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput);

        // =========================
        // CRUSHER
        // =========================

        // Coal
        CrusherRecipeBuilder.crushing(
                        Items.COAL,
                        ModItems.CRUSHED_COAL.get(), 3)
                .processingTime((300))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/coal")
                );

        // Iron
        CrusherRecipeBuilder.crushing(
                        Items.RAW_IRON,
                        ModItems.CRUSHED_IRON_ORE.get(), 1)
                .processingTime((300))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/raw_iron")
                );

// Platinum
        CrusherRecipeBuilder.crushing(
                        ModItems.RAW_PLATINUM.get(),
                        ModItems.CRUSHED_PLATINUM_ORE.get(),
                        1)
                .processingTime(300)
                .secondOutput(Items.GOLD_NUGGET, 1, 0.25f)
                .thirdOutput(Items.DIAMOND, 1, 0.10f)
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/raw_platinum")
                );

        // Mythril
        CrusherRecipeBuilder.crushing(
                ModItems.RAW_MYTHRIL.get(),
                ModItems.CRUSHED_MYTHRIL_ORE.get(), 1)
                    .processingTime(300)
                    .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(KatzencraftMetalsMod.MODID, "crusher/raw_mythril")
        );

        // =========================
        // INGOT <-> NUGGET
        // =========================

        // Steel
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STEEL_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModItems.STEEL_NUGGET.get())
                .unlockedBy("has_steel_nugget", has(ModItems.STEEL_NUGGET.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":steel_ingot_from_nuggets");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.STEEL_NUGGET.get(), 6)
                .requires(ModItems.STEEL_INGOT.get())
                .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":steel_nugget_from_ingot");

        // Platinum
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PLATINUM_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModItems.PLATINUM_NUGGET.get())
                .unlockedBy("has_platinum_nugget", has(ModItems.PLATINUM_NUGGET.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_ingot_from_nuggets");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLATINUM_NUGGET.get(), 6)
                .requires(ModItems.PLATINUM_INGOT.get())
                .unlockedBy("has_platinum_ingot", has(ModItems.PLATINUM_INGOT.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":platinum_nugget_from_ingot");

        // Mythril
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MYTHRIL_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModItems.MYTHRIL_NUGGET.get())
                .unlockedBy("has_mythril_nugget", has(ModItems.MYTHRIL_NUGGET.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_ingot_from_nuggets");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MYTHRIL_NUGGET.get(), 6)
                .requires(ModItems.MYTHRIL_INGOT.get())
                .unlockedBy("has_mythril_ingot", has(ModItems.MYTHRIL_INGOT.get()))
                .save(recipeOutput, KatzencraftMetalsMod.MODID + ":mythril_nugget_from_ingot");
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