package net.chriskatze.katzencraftmetals.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

public class ModVanillaFishingLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        addFishing(writer);
        addFishingFish(writer);
        addFishingJunk(writer);
        addFishingTreasure(writer);
    }

    private static ResourceKey<LootTable> gameplayLoot(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("gameplay/" + name)
        );
    }

    // gameplay/fishing
    private void addFishing(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("fishing"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .add(NestedLootTable.lootTableReference(gameplayLoot("fishing/fish"))
                                                .setWeight(85))
                                        .add(NestedLootTable.lootTableReference(gameplayLoot("fishing/junk"))
                                                .setWeight(10))
                                        .add(NestedLootTable.lootTableReference(gameplayLoot("fishing/treasure"))
                                                .setWeight(5))
                        )
        );
    }

    // gameplay/fishing/fish
    private void addFishingFish(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("fishing/fish"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .add(LootItem.lootTableItem(Items.COD).setWeight(60))
                                        .add(LootItem.lootTableItem(Items.SALMON).setWeight(25))
                                        .add(LootItem.lootTableItem(Items.TROPICAL_FISH).setWeight(2))
                                        .add(LootItem.lootTableItem(Items.PUFFERFISH).setWeight(13))
                        )
        );
    }

    // gameplay/fishing/junk
    private void addFishingJunk(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("fishing/junk"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .add(LootItem.lootTableItem(Items.LILY_PAD).setWeight(17))
                                        .add(LootItem.lootTableItem(Items.BOWL).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.FISHING_ROD).setWeight(2))
                                        .add(LootItem.lootTableItem(Items.LEATHER).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.STICK).setWeight(5))
                                        .add(LootItem.lootTableItem(Items.STRING).setWeight(5))
                                        .add(LootItem.lootTableItem(Items.POTION).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.BONE).setWeight(10))
                                        .add(LootItem.lootTableItem(Items.INK_SAC).setWeight(1))
                                        .add(LootItem.lootTableItem(Items.TRIPWIRE_HOOK).setWeight(10))
                        )
        );
    }

    // gameplay/fishing/treasure
    private void addFishingTreasure(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("fishing/treasure"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .add(LootItem.lootTableItem(Items.NAME_TAG))
                                        .add(LootItem.lootTableItem(Items.SADDLE))
                                        .add(LootItem.lootTableItem(Items.NAUTILUS_SHELL))
                                        .add(LootItem.lootTableItem(Items.BOW))
                                        .add(LootItem.lootTableItem(Items.FISHING_ROD))
                                        .add(LootItem.lootTableItem(Items.BOOK))
                        )
        );
    }
}