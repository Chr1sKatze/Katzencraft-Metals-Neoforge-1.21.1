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
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModVanillaEntityLootTableProvider implements net.minecraft.data.loot.LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {

        // ============================================================
        // OVERWORLD HOSTILE MOBS
        // ============================================================
        addZombie(writer);
        addSkeleton(writer);
        addCreeper(writer);
        addSpider(writer);
        addCaveSpider(writer);
        addEnderman(writer);
        addWitch(writer);
        addSlime(writer);
        addDrowned(writer);
        addHusk(writer);
        addStray(writer);
        addPhantom(writer);
        addSilverfish(writer);
        addEndermite(writer);
        addBogged(writer);

        // ============================================================
        // OVERWORLD STRUCTURE / SPECIAL HOSTILES
        // ============================================================
        addGuardian(writer);
        addElderGuardian(writer);
        addWarden(writer);

        // ============================================================
        // ILLAGERS
        // ============================================================
        addPillager(writer);
        addVindicator(writer);
        addEvoker(writer);
        addRavager(writer);
        addVex(writer);

        // ============================================================
        // NETHER MOBS
        // ============================================================
        addBlaze(writer);
        addGhast(writer);
        addMagmaCube(writer);
        addPiglin(writer);
        addPiglinBrute(writer);
        addZombifiedPiglin(writer);
        addWitherSkeleton(writer);
        addHoglin(writer);
        addZoglin(writer);

        // ============================================================
        // END MOBS
        // ============================================================
        addShulker(writer);

        // ============================================================
        // TRIAL CHAMBER MOBS
        // ============================================================
        addBreeze(writer);

        // ============================================================
        // BOSSES
        // ============================================================
        addWither(writer);
        addEnderDragon(writer);

        // ============================================================
        // PASSIVE ANIMALS
        // ============================================================

        addCow(writer);
        addPig(writer);
        addChicken(writer);
        addRabbit(writer);

        addSheepWhite(writer);
        addSheepOrange(writer);
        addSheepMagenta(writer);
        addSheepLightBlue(writer);
        addSheepYellow(writer);
        addSheepLime(writer);
        addSheepPink(writer);
        addSheepGray(writer);
        addSheepLightGray(writer);
        addSheepCyan(writer);
        addSheepPurple(writer);
        addSheepBlue(writer);
        addSheepBrown(writer);
        addSheepGreen(writer);
        addSheepRed(writer);
        addSheepBlack(writer);

        addMooshroom(writer);
        addGoat(writer);
        addHorse(writer);
        addDonkey(writer);
        addMule(writer);
        addLlama(writer);
        addTraderLlama(writer);
        addCamel(writer);
        addSniffer(writer);
        addArmadillo(writer);

        // ============================================================
        // AQUATIC ANIMALS
        // ============================================================

        addCod(writer);
        addSalmon(writer);
        addTropicalFish(writer);
        addPufferfish(writer);
        addSquid(writer);
        addGlowSquid(writer);
        addDolphin(writer);
        addTurtle(writer);
        addAxolotl(writer);
        addFrog(writer);
        addTadpole(writer);

        // ============================================================
        // AMBIENT ANIMALS
        // ============================================================

        addWolf(writer);
        addCat(writer);
        addOcelot(writer);
        addParrot(writer);
        addFox(writer);
        addPanda(writer);
        addPolarBear(writer);
        addBee(writer);
        addBat(writer);

        // ============================================================
        // VILLAGERS
        // ============================================================

        addVillager(writer);
        addWanderingTrader(writer);
        addIronGolem(writer);
        addSnowGolem(writer);
    }

    // ============================================================
    // HELPER
    // ============================================================

    private static ResourceKey<LootTable> entityLoot(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("entities/" + name)
        );
    }

    private static SetItemCountFunction.Builder count(float min, float max) {
        return SetItemCountFunction.setCount(
                UniformGenerator.between(min, max)
        );
    }

    // ============================================================
    // OVERWORLD HOSTILE MOBS
    // ============================================================

    private void addZombie(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("zombie"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(5)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addSkeleton(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("skeleton"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.BONE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.ARROW)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addCreeper(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("creeper"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.GUNPOWDER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addSpider(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("spider"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.SPIDER_EYE)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addCaveSpider(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("cave_spider"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.SPIDER_EYE)
                                                .setWeight(4)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addEnderman(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("enderman"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ENDER_PEARL)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addWitch(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("witch"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.REDSTONE)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST)
                                                .setWeight(6)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.GLASS_BOTTLE)
                                                .setWeight(6)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addSlime(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("slime"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SLIME_BALL)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addDrowned(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("drowned"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.COPPER_INGOT)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addHusk(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("husk"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.SAND)
                                                .setWeight(5)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addStray(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("stray"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.BONE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.ARROW)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.TIPPED_ARROW)
                                                .setWeight(2)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addPhantom(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("phantom"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.PHANTOM_MEMBRANE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addSilverfish(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("silverfish"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.STONE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addEndermite(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("endermite"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ENDER_PEARL)
                                                .setWeight(2)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addBogged(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("bogged"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.BONE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.ARROW)
                                                .setWeight(8)
                                                .apply(count(0.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.BROWN_MUSHROOM)
                                                .setWeight(5)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    // ============================================================
    // OVERWORLD STRUCTURE / SPECIAL HOSTILES
    // ============================================================

    private void addGuardian(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("guardian"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.PRISMARINE_SHARD)
                                                .setWeight(12)
                                                .apply(count(0.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.COD)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addElderGuardian(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("elder_guardian"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS)
                                                .setWeight(10)
                                                .apply(count(2.0F, 6.0F)))

                                        .add(LootItem.lootTableItem(Items.WET_SPONGE)
                                                .setWeight(3)
                                                .apply(count(1.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(count(1.0F, 2.0F)))
                        )
        );
    }

    private void addWarden(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("warden"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 6.0F))

                                        .add(LootItem.lootTableItem(Items.ECHO_SHARD)
                                                .setWeight(12)
                                                .apply(count(2.0F, 8.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_INGOT.get())
                                                .setWeight(5)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(3)
                                                .apply(count(1.0F, 2.0F)))
                        )
        );
    }

    // ============================================================
    // ILLAGERS
    // ============================================================

    private void addPillager(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("pillager"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.ARROW)
                                                .setWeight(12)
                                                .apply(count(1.0F, 6.0F)))

                                        .add(LootItem.lootTableItem(Items.CROSSBOW)
                                                .setWeight(1))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addVindicator(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("vindicator"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.EMERALD)
                                                .setWeight(8)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.IRON_AXE)
                                                .setWeight(1))
                        )
        );
    }

    private void addEvoker(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("evoker"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.TOTEM_OF_UNDYING)
                                                .setWeight(1))

                                        .add(LootItem.lootTableItem(Items.EMERALD)
                                                .setWeight(8)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addRavager(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("ravager"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))

                                        .add(LootItem.lootTableItem(Items.SADDLE)
                                                .setWeight(6))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(2.0F, 6.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addVex(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("vex"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(0.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
                                                .setWeight(5)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    // ============================================================
    // NETHER MOBS
    // ============================================================

    private void addBlaze(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("blaze"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.BLAZE_ROD)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addGhast(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("ghast"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.GHAST_TEAR)
                                                .setWeight(8)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.GUNPOWDER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addMagmaCube(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("magma_cube"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.MAGMA_CREAM)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addPiglin(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("piglin"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET)
                                                .setWeight(12)
                                                .apply(count(1.0F, 6.0F)))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addPiglinBrute(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("piglin_brute"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 4.0F))

                                        .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                                                .setWeight(10)
                                                .apply(count(1.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(2)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addZombifiedPiglin(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("zombified_piglin"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET)
                                                .setWeight(8)
                                                .apply(count(0.0F, 3.0F)))
                        )
        );
    }

    private void addWitherSkeleton(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("wither_skeleton"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.COAL)
                                                .setWeight(12)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.BONE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addHoglin(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("hoglin"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.PORKCHOP)
                                                .setWeight(12)
                                                .apply(count(2.0F, 5.0F)))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addZoglin(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("zoglin"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
                                                .setWeight(12)
                                                .apply(count(1.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    // ============================================================
    // END MOBS
    // ============================================================

    private void addShulker(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("shulker"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SHULKER_SHELL)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_NUGGET.get())
                                                .setWeight(2)
                                                .apply(count(1.0F, 2.0F)))
                        )
        );
    }

    // ============================================================
    // TRIAL CHAMBER MOBS
    // ============================================================

    private void addBreeze(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("breeze"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.WIND_CHARGE)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    // ============================================================
    // BOSSES
    // ============================================================

    private void addWither(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("wither"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.NETHER_STAR)
                                                .setWeight(10))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_INGOT.get())
                                                .setWeight(5)
                                                .apply(count(2.0F, 5.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(3)
                                                .apply(count(1.0F, 3.0F)))
                        )
        );
    }

    private void addEnderDragon(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("ender_dragon"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(ModItems.MYTHRIL_INGOT.get())
                                                .setWeight(10)
                                                .apply(count(4.0F, 8.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(8)
                                                .apply(count(3.0F, 6.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_INGOT.get())
                                                .setWeight(6)
                                                .apply(count(4.0F, 10.0F)))
                        )
        );
    }

    // ============================================================
    // PASSIVE ANIMALS
    // ============================================================

    private void addCow(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("cow"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.BEEF)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(6)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addPig(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("pig"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.PORKCHOP)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))
                        )
        );
    }

    private void addChicken(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("chicken"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.CHICKEN)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.FEATHER)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addRabbit(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("rabbit"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.RABBIT)
                                                .setWeight(10)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.RABBIT_HIDE)
                                                .setWeight(6)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.RABBIT_FOOT)
                                                .setWeight(1)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addSheepWhite(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/white"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.WHITE_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepOrange(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/orange"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.ORANGE_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepMagenta(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/magenta"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.MAGENTA_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepLightBlue(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/light_blue"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.LIGHT_BLUE_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepYellow(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/yellow"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.YELLOW_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepLime(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/lime"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.LIME_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepPink(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/pink"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.PINK_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepGray(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/gray"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.GRAY_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepLightGray(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/light_gray"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.LIGHT_GRAY_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepCyan(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/cyan"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.CYAN_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepPurple(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/purple"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.PURPLE_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepBlue(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/blue"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.BLUE_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepBrown(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/brown"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.BROWN_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepGreen(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/green"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.GREEN_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepRed(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/red"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.RED_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addSheepBlack(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(entityLoot("sheep/black"),
                LootTable.lootTable().withPool(
                        LootPool.lootPool()
                                .setRolls(UniformGenerator.between(1.0F, 3.0F))
                                .add(LootItem.lootTableItem(Items.MUTTON).setWeight(10)
                                        .apply(count(1.0F, 2.0F)))
                                .add(LootItem.lootTableItem(Items.BLACK_WOOL).setWeight(5)
                                        .apply(count(0.0F, 1.0F)))
                ));
    }

    private void addMooshroom(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("mooshroom"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.BEEF)
                                                .setWeight(10)
                                                .apply(count(1.0F, 4.0F)))

                                        .add(LootItem.lootTableItem(Items.RED_MUSHROOM)
                                                .setWeight(8)
                                                .apply(count(1.0F, 5.0F)))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(6)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addGoat(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("goat"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.MUTTON)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addHorse(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("horse"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 3.0F)))
                        )
        );
    }

    private void addDonkey(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("donkey"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addMule(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("mule"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addLlama(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("llama"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.WHITE_WOOL)
                                                .setWeight(5)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    // entities/trader_llama
    private void addTraderLlama(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("trader_llama"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.WHITE_WOOL)
                                                .setWeight(5)
                                                .apply(count(0.0F, 1.0F)))

                                        .add(LootItem.lootTableItem(Items.LEAD)
                                                .setWeight(2)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addCamel(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("camel"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addSniffer(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("sniffer"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.MOSS_BLOCK)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.TORCHFLOWER_SEEDS)
                                                .setWeight(4)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addArmadillo(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("armadillo"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.ARMADILLO_SCUTE)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    // ============================================================
    // AQUATIC ANIMALS
    // ============================================================

    private void addCod(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("cod"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.COD)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addSalmon(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("salmon"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SALMON)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addTropicalFish(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("tropical_fish"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.TROPICAL_FISH)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addPufferfish(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("pufferfish"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.PUFFERFISH)
                                                .setWeight(10)
                                                .apply(count(1.0F, 1.0F)))
                        )
        );
    }

    private void addSquid(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("squid"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.INK_SAC)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))
                        )
        );
    }

    private void addGlowSquid(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("glow_squid"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 3.0F))

                                        .add(LootItem.lootTableItem(Items.GLOW_INK_SAC)
                                                .setWeight(10)
                                                .apply(count(1.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addDolphin(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("dolphin"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.COD)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addTurtle(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("turtle"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SEAGRASS)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.TURTLE_SCUTE)
                                                .setWeight(2)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addAxolotl(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("axolotl"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.TROPICAL_FISH_BUCKET)
                                                .setWeight(2))

                                        .add(LootItem.lootTableItem(ModItems.SAPPHIRE_GEM.get())
                                                .setWeight(1))
                        )
        );
    }

    private void addFrog(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("frog"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SLIME_BALL)
                                                .setWeight(5)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addTadpole(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("tadpole"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.SLIME_BALL)
                                                .setWeight(1)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    // ============================================================
    // AMBIENT MOBS
    // ============================================================

    private void addWolf(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("wolf"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.BONE)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.LEATHER)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addCat(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("cat"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(6)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addOcelot(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("ocelot"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.STRING)
                                                .setWeight(5)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.COD)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addParrot(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("parrot"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.FEATHER)
                                                .setWeight(10)
                                                .apply(count(0.0F, 3.0F)))
                        )
        );
    }

    private void addFox(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("fox"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SWEET_BERRIES)
                                                .setWeight(8)
                                                .apply(count(0.0F, 3.0F)))

                                        .add(LootItem.lootTableItem(Items.RABBIT_HIDE)
                                                .setWeight(3)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addPanda(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("panda"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.BAMBOO)
                                                .setWeight(12)
                                                .apply(count(1.0F, 4.0F)))
                        )
        );
    }

    private void addPolarBear(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("polar_bear"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.SALMON)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.COD)
                                                .setWeight(8)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addBee(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("bee"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.HONEYCOMB)
                                                .setWeight(5)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    private void addBat(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("bat"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 1.0F))

                                        .add(LootItem.lootTableItem(Items.PHANTOM_MEMBRANE)
                                                .setWeight(1)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }

    // ============================================================
    // VILLAGERS
    // ============================================================

    private void addVillager(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("villager"),
                LootTable.lootTable()
        );
    }

    private void addWanderingTrader(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("wandering_trader"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(1.0F, 2.0F))

                                        .add(LootItem.lootTableItem(Items.EMERALD)
                                                .setWeight(5)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(Items.LEAD)
                                                .setWeight(3)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addIronGolem(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("iron_golem"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(3.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                                .setWeight(15)
                                                .apply(count(3.0F, 5.0F)))

                                        .add(LootItem.lootTableItem(Items.POPPY)
                                                .setWeight(10)
                                                .apply(count(0.0F, 2.0F)))

                                        .add(LootItem.lootTableItem(ModItems.PLATINUM_NUGGET.get())
                                                .setWeight(2)
                                                .apply(count(0.0F, 2.0F)))
                        )
        );
    }

    private void addSnowGolem(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        writer.accept(
                entityLoot("snow_golem"),
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(UniformGenerator.between(2.0F, 5.0F))

                                        .add(LootItem.lootTableItem(Items.SNOWBALL)
                                                .setWeight(15)
                                                .apply(count(2.0F, 8.0F)))

                                        .add(LootItem.lootTableItem(Items.CARVED_PUMPKIN)
                                                .setWeight(1)
                                                .apply(count(0.0F, 1.0F)))
                        )
        );
    }
}