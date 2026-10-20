package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Datagen-only recipes for craftable equipment.
 *
 * Keeps tool and armor recipe boilerplate out of ModRecipeProvider.
 */
public final class ModEquipmentRecipeProvider {

    private ModEquipmentRecipeProvider() {
    }

    public static void build(
            RecipeOutput recipeOutput
    ) {
        equipmentSet(recipeOutput,"copper",Items.COPPER_INGOT,
                ModItems.COPPER_SWORD.get(),ModItems.COPPER_PICKAXE.get(),ModItems.COPPER_AXE.get(),
                ModItems.COPPER_SHOVEL.get(),ModItems.COPPER_HOE.get(),ModItems.COPPER_HELMET.get(),
                ModItems.COPPER_CHESTPLATE.get(),ModItems.COPPER_LEGGINGS.get(),ModItems.COPPER_BOOTS.get());
        equipmentSet(recipeOutput,"netherite",Items.NETHERITE_INGOT,
                Items.NETHERITE_SWORD,Items.NETHERITE_PICKAXE,Items.NETHERITE_AXE,Items.NETHERITE_SHOVEL,
                Items.NETHERITE_HOE,Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS);
        buildSteelEquipmentRecipes(
                recipeOutput
        );

        buildMythrilEquipmentRecipes(
                recipeOutput
        );
    }

    private static void buildSteelEquipmentRecipes(
            RecipeOutput recipeOutput
    ) {
        equipmentSet(
                recipeOutput,
                "steel",
                ModItems.STEEL_INGOT.get(),
                ModItems.STEEL_SWORD.get(),
                ModItems.STEEL_PICKAXE.get(),
                ModItems.STEEL_AXE.get(),
                ModItems.STEEL_SHOVEL.get(),
                ModItems.STEEL_HOE.get(),
                ModItems.STEEL_HELMET.get(),
                ModItems.STEEL_CHESTPLATE.get(),
                ModItems.STEEL_LEGGINGS.get(),
                ModItems.STEEL_BOOTS.get()
        );
    }

    private static void buildMythrilEquipmentRecipes(
            RecipeOutput recipeOutput
    ) {
        equipmentSet(
                recipeOutput,
                "mythril",
                ModItems.MYTHRIL_INGOT.get(),
                ModItems.MYTHRIL_SWORD.get(),
                ModItems.MYTHRIL_PICKAXE.get(),
                ModItems.MYTHRIL_AXE.get(),
                ModItems.MYTHRIL_SHOVEL.get(),
                ModItems.MYTHRIL_HOE.get(),
                ModItems.MYTHRIL_HELMET.get(),
                ModItems.MYTHRIL_CHESTPLATE.get(),
                ModItems.MYTHRIL_LEGGINGS.get(),
                ModItems.MYTHRIL_BOOTS.get()
        );
    }

    private static void equipmentSet(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike sword,
            ItemLike pickaxe,
            ItemLike axe,
            ItemLike shovel,
            ItemLike hoe,
            ItemLike helmet,
            ItemLike chestplate,
            ItemLike leggings,
            ItemLike boots
    ) {
        sword(
                recipeOutput,
                materialName,
                ingot,
                sword
        );

        pickaxe(
                recipeOutput,
                materialName,
                ingot,
                pickaxe
        );

        axe(
                recipeOutput,
                materialName,
                ingot,
                axe
        );

        shovel(
                recipeOutput,
                materialName,
                ingot,
                shovel
        );

        hoe(
                recipeOutput,
                materialName,
                ingot,
                hoe
        );

        helmet(
                recipeOutput,
                materialName,
                ingot,
                helmet
        );

        chestplate(
                recipeOutput,
                materialName,
                ingot,
                chestplate
        );

        leggings(
                recipeOutput,
                materialName,
                ingot,
                leggings
        );

        boots(
                recipeOutput,
                materialName,
                ingot,
                boots
        );
    }

    private static void sword(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT,
                        result
                )
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void pickaxe(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.TOOLS,
                        result
                )
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void axe(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.TOOLS,
                        result
                )
                .pattern("II")
                .pattern("IS")
                .pattern(" S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void shovel(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.TOOLS,
                        result
                )
                .pattern("I")
                .pattern("S")
                .pattern("S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void hoe(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.TOOLS,
                        result
                )
                .pattern("II")
                .pattern(" S")
                .pattern(" S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void helmet(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT,
                        result
                )
                .pattern("III")
                .pattern("I I")
                .define('I', ingot)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void chestplate(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT,
                        result
                )
                .pattern("I I")
                .pattern("III")
                .pattern("III")
                .define('I', ingot)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void leggings(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT,
                        result
                )
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ingot)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static void boots(
            RecipeOutput recipeOutput,
            String materialName,
            ItemLike ingot,
            ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT,
                        result
                )
                .pattern("I I")
                .pattern("I I")
                .define('I', ingot)
                .unlockedBy(
                        hasIngotName(
                                materialName
                        ),
                        RecipeProviderAccessor.hasItem(
                                ingot
                        )
                )
                .save(
                        recipeOutput
                );
    }

    private static String hasIngotName(
            String materialName
    ) {
        return "has_"
                + materialName
                + "_ingot";
    }
}