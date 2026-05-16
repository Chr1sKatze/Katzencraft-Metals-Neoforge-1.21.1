package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModVanillaArchaeologyLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        addDesertPyramid(writer);
        addDesertWell(writer);
        addOceanRuinCold(writer);
        addOceanRuinWarm(writer);
        addTrailRuinsCommon(writer);
        addTrailRuinsRare(writer);
    }

    private static ResourceKey<LootTable> archaeologyLoot(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("archaeology/" + name)
        );
    }

    private static SetItemCountFunction.Builder count(float min, float max) {
        return SetItemCountFunction.setCount(UniformGenerator.between(min, max));
    }

    // archaeology/desert_pyramid
    private void addDesertPyramid(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("desert_pyramid"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.TNT).setWeight(4)
                                                .apply(count(1.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    // archaeology/desert_well
    private void addDesertWell(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("desert_well"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.BRICK).setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.STICK).setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(3)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(2)
                                                .apply(count(1.0F, 3.0F)))
                        )
        );
    }

    // archaeology/ocean_ruin_cold
    private void addOceanRuinCold(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("ocean_ruin_cold"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.COAL).setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.WHEAT).setWeight(8)
                                                .apply(count(1.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(2)
                                                .apply(count(1.0F, 3.0F)))
                        )
        );
    }

    // archaeology/ocean_ruin_warm
    private void addOceanRuinWarm(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("ocean_ruin_warm"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10)
                                                .apply(count(1.0F, 8.0F)))

                                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(5)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.TROPICAL_FISH).setWeight(8)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    // archaeology/trail_ruins_common
    private void addTrailRuinsCommon(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("trail_ruins_common"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.BRICK).setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.CLAY_BALL).setWeight(10)
                                                .apply(count(1.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(Items.WHEAT).setWeight(8)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get()).setWeight(2)
                                                .apply(count(1.0F, 2.0F)))
                        )
        );
    }

    // archaeology/trail_ruins_rare
    private void addTrailRuinsRare(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                archaeologyLoot("trail_ruins_rare"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(6)
                                                .apply(count(1.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(8)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get()).setWeight(3)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get()).setWeight(2)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }
}