package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModVanillaLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        // ============================================================
        // OVERWORLD STRUCTURES
        // ============================================================
        addSimpleDungeon(writer);
        addAbandonedMineshaft(writer);
        addDesertPyramid(writer);
        addJungleTemple(writer);
        addPillagerOutpost(writer);
        addWoodlandMansion(writer);
        addBuriedTreasure(writer);
        addRuinedPortal(writer);
        addIglooChest(writer);

        // ============================================================
        // OCEAN STRUCTURES
        // ============================================================
        addShipwreckMap(writer);
        addShipwreckSupply(writer);
        addShipwreckTreasure(writer);
        addUnderwaterRuinBig(writer);
        addUnderwaterRuinSmall(writer);

        // ============================================================
        // STRONGHOLDS
        // ============================================================
        addStrongholdCorridor(writer);
        addStrongholdCrossing(writer);
        addStrongholdLibrary(writer);

        // ============================================================
        // NETHER
        // ============================================================
        addNetherBridge(writer);
        addBastionBridge(writer);
        addBastionHoglinStable(writer);
        addBastionOther(writer);
        addBastionTreasure(writer);

        // ============================================================
        // END / DEEP DARK
        // ============================================================
        addEndCityTreasure(writer);
        addAncientCity(writer);
        addAncientCityIceBox(writer);

        // ============================================================
        // VILLAGES
        // ============================================================
        addVillageArmorer(writer);
        addVillageButcher(writer);
        addVillageCartographer(writer);
        addVillageDesertHouse(writer);
        addVillageFisher(writer);
        addVillageFletcher(writer);
        addVillageMason(writer);
        addVillagePlainsHouse(writer);
        addVillageSavannaHouse(writer);
        addVillageShepherd(writer);
        addVillageSnowyHouse(writer);
        addVillageTaigaHouse(writer);
        addVillageTannery(writer);
        addVillageTemple(writer);
        addVillageToolsmith(writer);
        addVillageWeaponsmith(writer);

        // ============================================================
        // MISC
        // ============================================================
        addSpawnBonusChest(writer);
    }

        // ============================================================
        // OVERWORLD STRUCTURES
        // ============================================================

    private void addSimpleDungeon(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.SIMPLE_DUNGEON,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        )
        );
    }

    private void addAbandonedMineshaft(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.ABANDONED_MINESHAFT,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                        )
        );
    }

    private void addDesertPyramid(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.DESERT_PYRAMID,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(15)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        )
        );
    }

    private void addJungleTemple(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.JUNGLE_TEMPLE,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))

                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(4)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        )
        );
    }

    private void addPillagerOutpost(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.PILLAGER_OUTPOST,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.WHEAT)
                                                .setWeight(15)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))

                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                        )
        );
    }

    private void addWoodlandMansion(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.WOODLAND_MANSION,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))

                                        .add(LootItem.lootTableItem(Items.DIAMOND)
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        )
        );
    }

    private void addBuriedTreasure(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.BURIED_TREASURE,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))

                                        .add(LootItem.lootTableItem(Items.DIAMOND)
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        )
        );
    }

    private void addRuinedPortal(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.RUINED_PORTAL,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                        .add(LootItem.lootTableItem(Items.OBSIDIAN)
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        )
        );
    }

    private void addIglooChest(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.IGLOO_CHEST,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.APPLE).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.COAL).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 8.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                ));
    }

        // ============================================================
        // OCEAN STRUCTURES
        // ============================================================

    private void addShipwreckMap(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.SHIPWRECK_MAP,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                                        .add(LootItem.lootTableItem(Items.PAPER)
                                                .setWeight(15)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 10.0F))))
                                        .add(LootItem.lootTableItem(Items.COMPASS)
                                                .setWeight(5))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(4)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                        )
        );
    }

    private void addShipwreckSupply(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.SHIPWRECK_SUPPLY,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                        .add(LootItem.lootTableItem(Items.BREAD)
                                                .setWeight(15)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                        .add(LootItem.lootTableItem(Items.WHEAT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                        )
        );
    }

    private void addShipwreckTreasure(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.SHIPWRECK_TREASURE,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(4)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                        )
        );
    }

    private void addUnderwaterRuinBig(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.UNDERWATER_RUIN_BIG,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                                        .add(LootItem.lootTableItem(Items.COAL)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET)
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(4)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                        )
        );
    }

    private void addUnderwaterRuinSmall(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.UNDERWATER_RUIN_SMALL,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                        .add(LootItem.lootTableItem(Items.COAL)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET)
                                                .setWeight(6)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(2)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                        )
        );
    }

    // ============================================================
    // STRONGHOLDS
    // ============================================================

    private void addStrongholdCorridor(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.STRONGHOLD_CORRIDOR,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                ));
    }

    private void addStrongholdCrossing(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.STRONGHOLD_CROSSING,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.BREAD).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                ));
    }

    private void addStrongholdLibrary(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.STRONGHOLD_LIBRARY,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                .add(LootItem.lootTableItem(Items.BOOK).setWeight(15)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.PAPER).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                .add(LootItem.lootTableItem(Items.COMPASS).setWeight(2))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                ));
    }

    // ============================================================
    // NETHER
    // ============================================================

    private void addNetherBridge(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.NETHER_BRIDGE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(Items.NETHER_WART).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 7.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                ));
    }

    private void addBastionBridge(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.BASTION_BRIDGE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(3))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(4)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                ));
    }

    private void addBastionHoglinStable(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.BASTION_HOGLIN_STABLE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.CRYING_OBSIDIAN).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                ));
    }

    private void addBastionOther(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.BASTION_OTHER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                ));
    }

    private void addBastionTreasure(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.BASTION_TREASURE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(4.0F, 8.0F))
                                .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));
    }

    // ============================================================
    // END / DEEP DARK
    // ============================================================

    private void addEndCityTreasure(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.END_CITY_TREASURE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 7.0F))
                                .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(7)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));
    }

    private void addAncientCity(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.ANCIENT_CITY,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(4.0F, 8.0F))
                                .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(6)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.ECHO_SHARD).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(ModItems.MYTHRIL_INGOT.get()).setWeight(5)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                ));
    }

    private void addAncientCityIceBox(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.ANCIENT_CITY_ICE_BOX,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.PACKED_ICE).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.SNOWBALL).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                                .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get()).setWeight(3)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                ));
    }

    // ============================================================
    // VILLAGES
    // ============================================================

    private void addVillageButcher(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_BUTCHER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.BEEF).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageDesertHouse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_DESERT_HOUSE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.DEAD_BUSH).setWeight(10))
                                .add(LootItem.lootTableItem(Items.BREAD).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(1))
                ));
    }

    private void addVillageFisher(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_FISHER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.COD).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.SALMON).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageFletcher(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_FLETCHER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.ARROW).setWeight(15)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 16.0F))))
                                .add(LootItem.lootTableItem(Items.FEATHER).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillagePlainsHouse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.BREAD).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.APPLE).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageSavannaHouse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_SAVANNA_HOUSE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.WHEAT).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                .add(LootItem.lootTableItem(Items.SHORT_GRASS).setWeight(5))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(1))
                ));
    }

    private void addVillageShepherd(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_SHEPHERD,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.WHITE_WOOL).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.SHEARS).setWeight(2))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageSnowyHouse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.SNOWBALL).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                                .add(LootItem.lootTableItem(Items.POTATO).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageTaigaHouse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                .add(LootItem.lootTableItem(Items.SPRUCE_SAPLING).setWeight(6))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(1))
                ));
    }

    private void addVillageTannery(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_TANNERY,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.LEATHER).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(2))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1))
                ));
    }

    private void addVillageTemple(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_TEMPLE,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2))
                ));
    }

    private void addVillageArmorer(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_ARMORER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(14)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                .add(LootItem.lootTableItem(Items.LAVA_BUCKET).setWeight(2))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                ));
    }

    private void addVillageCartographer(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_CARTOGRAPHER,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.PAPER).setWeight(15)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 12.0F))))
                                .add(LootItem.lootTableItem(Items.COMPASS).setWeight(3))
                                .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                ));
    }

    private void addVillageMason(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.VILLAGE_MASON,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(2.0F, 5.0F))
                                .add(LootItem.lootTableItem(Items.CLAY_BALL).setWeight(12)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                                .add(LootItem.lootTableItem(Items.BRICK).setWeight(8)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(2)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                ));
    }

    private void addVillageWeaponsmith(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.VILLAGE_WEAPONSMITH,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                        )
        );
    }

    private void addVillageToolsmith(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.VILLAGE_TOOLSMITH,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                        )
        );
    }

    // ============================================================
    // MISC
    // ============================================================

    private void addSpawnBonusChest(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(BuiltInLootTables.SPAWN_BONUS_CHEST,
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(4.0F, 8.0F))
                                .add(LootItem.lootTableItem(Items.OAK_LOG).setWeight(15)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.BREAD).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                .add(LootItem.lootTableItem(Items.APPLE).setWeight(10)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                .add(LootItem.lootTableItem(Items.STONE_AXE).setWeight(3))
                                .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(1)
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 1.0F))))
                ));
    }
}