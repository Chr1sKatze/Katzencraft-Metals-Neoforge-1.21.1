package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {

    public ModBlockLootTableProvider(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
    }

    @Override
    protected void generate() {
        // machines
        dropSelf(ModBlocks.FUEL_CHAMBER.get());
        dropSelf(ModBlocks.FOUNDRY_CONTROLLER.get());
        dropSelf(ModBlocks.CRUSHER.get());
        dropSelf(ModBlocks.FOUNDRY_TANK.get());
        dropSelf(ModBlocks.CASTING_CAULDRON.get());
        dropSelf(ModBlocks.FOUNDRY_FAUCET.get());

        // blocks
        dropSelf(ModBlocks.STEEL_BLOCK.get());
        dropSelf(ModBlocks.STEEL_BARS.get());
        dropSelf(ModBlocks.STEEL_CHAIN.get());
        dropSelf(ModBlocks.STEEL_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.STEEL_BUTTON.get());
        dropSelf(ModBlocks.STEEL_LEVER.get());
        dropSelf(ModBlocks.STEEL_DOOR.get());
        dropSelf(ModBlocks.STEEL_TRAPDOOR.get());
        dropSelf(ModBlocks.STEEL_LADDER.get());

        dropSelf(ModBlocks.PLATINUM_BLOCK.get());

        dropSelf(ModBlocks.MYTHRIL_BLOCK.get());

        // Cut steel building blocks
        dropSelf(ModBlocks.CUT_STEEL_BLOCK.get());
        dropSelf(ModBlocks.CUT_STEEL_STAIRS.get());
        add(ModBlocks.CUT_STEEL_SLAB.get(), block -> createSlabItemTable(block));

        // simple raw ore drop + gemstone bonus chance
        add(ModBlocks.PLATINUM_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_PLATINUM.get(), 0, 0f, ModItems.AMETHYST_GEM.get(), 0.05f));
        add(ModBlocks.DEEPSLATE_PLATINUM_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_PLATINUM.get(), 0, 0f, ModItems.AMETHYST_GEM.get(), 0.05f));
        add(ModBlocks.NETHER_PLATINUM_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_PLATINUM.get(), 0, 0f, ModItems.AMETHYST_GEM.get(), 0.05f));
        add(ModBlocks.END_PLATINUM_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_PLATINUM.get(), 0, 0f, ModItems.AMETHYST_GEM.get(), 0.05f));

        // weighted ore drops + gemstone bonus chance
        add(ModBlocks.MYTHRIL_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_MYTHRIL.get(), 4, 0.18f, ModItems.SAPPHIRE_GEM.get(), 0.05f));
        add(ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_MYTHRIL.get(), 4, 0.18f, ModItems.SAPPHIRE_GEM.get(), 0.05f));
        add(ModBlocks.NETHER_MYTHRIL_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_MYTHRIL.get(), 4, 0.18f, ModItems.SAPPHIRE_GEM.get(), 0.05f));
        add(ModBlocks.END_MYTHRIL_ORE.get(),
                block -> createWeightedOreDropsWithGemChance(block, ModItems.RAW_MYTHRIL.get(), 4, 0.18f, ModItems.SAPPHIRE_GEM.get(), 0.05f));
    }

    /**
     * Creates an ore drop table with:
     * - Silk Touch support (drops the ore block itself)
     * - 1 guaranteed base raw drop
     * - additional bonus raw drops using a binomial distribution
     * - optional flat gem bonus chance on normal mining only
     *
     * HOW IT WORKS
     *
     * Raw ore drops:
     * Total raw drops = base drop + Binomial(extraRolls, extraChance)
     *
     * Example with:
     *   extraRolls = 4
     *   extraChance = 0.18
     *
     * The game performs 4 independent "extra raw drop attempts":
     *   - each attempt has an 18% chance to succeed
     *   - each success adds +1 raw item
     *
     * So outcomes look like:
     *   0 successes -> 1 total raw drop
     *   1 success   -> 2 total raw drops
     *   2 successes -> 3 total raw drops
     *   3 successes -> 4 total raw drops
     *   4 successes -> 5 total raw drops
     *
     * Because the chance is low (0.18), fewer successes are more likely:
     *   - 1 raw drop is most common
     *   - 2 raw drops are fairly common
     *   - 3+ raw drops become increasingly rare
     *
     * FORTUNE
     *
     * Fortune affects ONLY the initial base raw drop.
     * The extra binomial raw drops are NOT increased by Fortune.
     *
     * GEM BONUS
     *
     * The gem bonus is a separate independent loot pool:
     *   - it is checked only once
     *   - it is NOT affected by Silk Touch
     *   - it is NOT affected by Fortune
     *   - it is NOT multiplied by extra raw-drop rolls
     *
     * Example:
     *   gemDropChance = 0.05f
     * means a flat 5% chance to also receive 1 gemstone on normal mining.
     */
    protected LootTable.Builder createWeightedOreDropsWithGemChance(
            Block block,
            Item rawDrop,
            int extraRolls,
            float extraChance,
            Item bonusGem,
            float gemDropChance
    ) {
        HolderLookup.RegistryLookup<Enchantment> registryLookup =
                this.registries.lookupOrThrow(Registries.ENCHANTMENT);

        LootTable.Builder table = this.createSilkTouchDispatchTable(
                block,
                this.applyExplosionDecay(
                        block,
                        LootItem.lootTableItem(rawDrop)
                                // Base drop starts at 1 by default.
                                // Fortune affects only this base portion.
                                .apply(ApplyBonusCount.addOreBonusCount(
                                        registryLookup.getOrThrow(Enchantments.FORTUNE)
                                ))
                                // Add 0..extraRolls additional drops with diminishing probability.
                                // These extra drops are NOT affected by Fortune.
                                .apply(SetItemCountFunction.setCount(
                                        BinomialDistributionGenerator.binomial(extraRolls, extraChance),
                                        true
                                ))
                )
        );

        // Independent bonus gem roll:
        // - only on normal mining
        // - not affected by Fortune
        // - not affected by extra raw-drop rolls
        // - exactly one check, exactly one gem if successful
        table.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(doesNotHaveSilkTouch())
                .add(LootItem.lootTableItem(bonusGem)
                        .when(LootItemRandomChanceCondition.randomChance(gemDropChance)))
        );

        return table;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                ModBlocks.PLATINUM_BLOCK.get(),
                ModBlocks.PLATINUM_ORE.get(),
                ModBlocks.DEEPSLATE_PLATINUM_ORE.get(),
                ModBlocks.NETHER_PLATINUM_ORE.get(),
                ModBlocks.END_PLATINUM_ORE.get(),

                ModBlocks.MYTHRIL_BLOCK.get(),
                ModBlocks.MYTHRIL_ORE.get(),
                ModBlocks.DEEPSLATE_MYTHRIL_ORE.get(),
                ModBlocks.NETHER_MYTHRIL_ORE.get(),
                ModBlocks.END_MYTHRIL_ORE.get(),

                ModBlocks.STEEL_BLOCK.get(),
                ModBlocks.CUT_STEEL_BLOCK.get(),
                ModBlocks.STEEL_BARS.get(),
                ModBlocks.STEEL_CHAIN.get(),
                ModBlocks.STEEL_PRESSURE_PLATE.get(),
                ModBlocks.STEEL_BUTTON.get(),
                ModBlocks.STEEL_LEVER.get(),
                ModBlocks.STEEL_DOOR.get(),
                ModBlocks.STEEL_TRAPDOOR.get(),
                ModBlocks.CUT_STEEL_STAIRS.get(),
                ModBlocks.CUT_STEEL_SLAB.get(),
                ModBlocks.STEEL_LADDER.get(),

                ModBlocks.CRUSHER.get(),
                ModBlocks.FUEL_CHAMBER.get(),
                ModBlocks.FOUNDRY_CONTROLLER.get(),
                ModBlocks.FOUNDRY_TANK.get(),
                ModBlocks.CASTING_CAULDRON.get(),
                ModBlocks.FOUNDRY_FAUCET.get()
        );
    }
}