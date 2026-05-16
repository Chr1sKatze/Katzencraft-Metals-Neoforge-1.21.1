package net.chriskatze.katzencraftmetals.datagen;

import net.chriskatze.katzencraftmetals.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModVanillaGameplayLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        addCatMorningGift(writer);
        addPiglinBartering(writer);
    }

    private static ResourceKey<LootTable> gameplayLoot(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("gameplay/" + name)
        );
    }

    private static SetItemCountFunction.Builder count(float min, float max) {
        return SetItemCountFunction.setCount(UniformGenerator.between(min, max));
    }

    private void addCatMorningGift(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("cat_morning_gift"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.RABBIT_HIDE)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.RABBIT_FOOT)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.FEATHER)
                                                .setWeight(10)
                                                .apply(count(1.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.CHICKEN)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.PHANTOM_MEMBRANE)
                                                .setWeight(2)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addPiglinBartering(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                gameplayLoot("piglin_bartering"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))

                                        .add(LootItem.lootTableItem(Items.OBSIDIAN)
                                                .setWeight(40)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.CRYING_OBSIDIAN)
                                                .setWeight(40)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.QUARTZ)
                                                .setWeight(40)
                                                .apply(count(5.0F, 12.0F)))

                                        .add(LootItem.lootTableItem(Items.GRAVEL)
                                                .setWeight(40)
                                                .apply(count(8.0F, 16.0F)))

                                        .add(LootItem.lootTableItem(Items.BLACKSTONE)
                                                .setWeight(40)
                                                .apply(count(8.0F, 16.0F)))

                                        .add(LootItem.lootTableItem(Items.SOUL_SAND)
                                                .setWeight(40)
                                                .apply(count(4.0F, 12.0F)))

                                        .add(LootItem.lootTableItem(Items.ENDER_PEARL)
                                                .setWeight(20)
                                                .apply(count(2.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(20)
                                                .apply(count(3.0F, 9.0F)))

                                        .add(LootItem.lootTableItem(Items.FIRE_CHARGE)
                                                .setWeight(20)
                                                .apply(count(1.0F, 5.0F)))

                                        .add(LootItem.lootTableItem(Items.IRON_NUGGET)
                                                .setWeight(20)
                                                .apply(count(10.0F, 36.0F)))

                                        .add(LootItem.lootTableItem(Items.SPECTRAL_ARROW)
                                                .setWeight(20)
                                                .apply(count(6.0F, 12.0F)))

                                        .add(LootItem.lootTableItem(Items.NETHER_BRICK)
                                                .setWeight(10)
                                                .apply(count(4.0F, 16.0F)))

                                        .add(LootItem.lootTableItem(Items.POTION)
                                                .setWeight(10)
                                                .apply(SetPotionFunction.setPotion(Potions.FIRE_RESISTANCE)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(5)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(2)
                                                .apply(count(1.0F, 2.0F)))
                        )
        );
    }
}