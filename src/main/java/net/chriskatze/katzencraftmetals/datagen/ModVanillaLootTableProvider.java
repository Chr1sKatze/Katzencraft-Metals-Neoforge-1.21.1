package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModVanillaLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<net.minecraft.resources.ResourceKey<LootTable>, LootTable.Builder> writer) {

        addSimpleDungeon(writer);
        addAbandonedMineshaft(writer);
        addDesertPyramid(writer);
    }

    private void addSimpleDungeon(BiConsumer<net.minecraft.resources.ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.SIMPLE_DUNGEON,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_INGOT.get())
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                                        .add(EmptyLootItem.emptyItem().setWeight(5))
                        )
        );
    }

    private void addAbandonedMineshaft(BiConsumer<net.minecraft.resources.ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.ABANDONED_MINESHAFT,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))
                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(10)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(8)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                                        .add(EmptyLootItem.emptyItem().setWeight(8))
                        )
        );
    }

    private void addDesertPyramid(BiConsumer<net.minecraft.resources.ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                BuiltInLootTables.DESERT_PYRAMID,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                                        .add(LootItem.lootTableItem(Items.DIAMOND)
                                                .setWeight(3)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(15)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 7.0F))))
                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(5)
                                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(4)
                                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                                        .add(EmptyLootItem.emptyItem().setWeight(5))
                        )
        );
    }
}