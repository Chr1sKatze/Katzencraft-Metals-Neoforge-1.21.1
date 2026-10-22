package net.chriskatze.katzencraftmetals.metal;

import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.Form.*;
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.StorageSound.*;
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.metal;
import static net.chriskatze.katzencraftmetals.metal.MetalAlloy.input;
import static net.chriskatze.katzencraftmetals.metal.MetalOreGeneration.Distribution.*;

/**
 * Main place to declare metals. Standard ingots/nuggets/storage cubes and their
 * generated resources are automatic; supply textures separately. Foundry is opt-in.
 * Ore generation is opt-in here; raw/ore block registration, tools and armor remain manual.
 * See docs/ADDING_METALS.md for texture conventions and remaining manual steps.
 *
 * <p><strong>Append new metals at the END; never reorder existing declarations.</strong>
 * Foundry synchronization IDs depend on declaration order. Keep existing IDs stable.
 */
public final class MetalCatalog {

    // 1. ADDING METALS
    // metal(id, displayName) uses the mod namespace and standard ingot/nugget/block IDs.
    // Required: .density(positiveValue), which controls molten layering (heavier below).
    // Defaults: 6 units/ingot; storage hardness 5, resistance 6, METAL sound, storage requirement 4 (Iron).
    // Tool stats/capability come from .equipment(MetalEquipmentProfiles.X).
    // Set .oreMiningLevel(1..8) and .storageMiningLevel(1..8) independently.
    // Override storage values with .storageMiningLevel(), .storageSound(), .storageStrength(),
    // .storageResistance(). Mining level applies only to the storage block, not ores.
    // .meltingTime(100) sets ticks per 6 units; .ingotCoolingTime(100) and
    // .blockCoolingTime(100) independently configure Table/Basin cooling (default 100).
    // .foundry() enables molten registration/melting; molten_<metal>.png and its
    // .png.mcmeta are shared by tank surfaces, sides and faucet streams.
    // .foundry() automatically uses the metal's normal storage-block texture
    // for cooled Basin castings.
    // .flowing(...) optionally overrides the faucet stream texture.
    // The Foundry Table uses separate ingot textures.
    // .form(type, "namespace:id") declares extra forms; specialized content stays manual.
    // .localizedName(locale, name) and .formName(locale, formId, name) override generated names.
    // Finish with .build(); validation also runs across the completed catalog.
    //
    // EXAMPLE ONLY (append inside ORDERED; Electrum is not registered here):
    // EXAMPLE ONLY:
    // metal("electrum", "Electrum")
    // .density(12500)
    //    .meltingTime(120)
    //    .ingotCoolingTime(80)
    //    .blockCoolingTime(180)
    //    .foundry()
    //    .alloy(140, input("iron", 1), input("gold", 1))
    //            .build();
    // Inputs are positive whole ingots: 1 -> 6 units. Output is ALWAYS 6 units of this
    // metal, even with multiple inputs; no refund/byproducts. Up to 3 distinct metals.
    // .alloy(processingTicks, inputs...) sets time. Short iron/copper/gold mean Minecraft;
    // other short IDs mean this mod. Explicit namespace:path works; unknown IDs are rejected.

    // 2. METAL DECLARATIONS — normally edit only this section; append new entries
    private static final List<MetalDefinition> ORDERED = List.of(
            vanilla("iron", "Iron", true)
                    .density(7874)
                    .equipment(MetalEquipmentProfiles.IRON)
                    .oreMiningLevel(3).storageMiningLevel(3)
                    .foundry()
                    .build(),

            vanilla("copper", "Copper", false)
                    .density(8960)
                    .equipment(MetalEquipmentProfiles.COPPER)
                    .oreMiningLevel(3).storageMiningLevel(3)
                    .foundry()
                    .build(),

            metal("steel", "Steel")
                    .density(7850)
                    .equipment(MetalEquipmentProfiles.STEEL)
                    .storageMiningLevel(4)
                    .storageSound(COPPER)
                    .foundry()
                    .alloy(input("iron", 1))
                    .build(),

            oreMetal("mythril", "Mythril")
                    .density(9500)
                    .equipment(MetalEquipmentProfiles.MYTHRIL)
                    .oreMiningLevel(7).storageMiningLevel(7)
                    .meltingTime(160)
                    .ingotCoolingTime(120)
                    .blockCoolingTime(240)
                    // Explicit deposits only; forms alone never enable generation.
                    .oreGeneration(ORE, c -> c.dimension("minecraft:overworld").biomes("#minecraft:is_overworld")
                            .replace("#minecraft:stone_ore_replaceables").veinSize(8).attemptsPerChunk(3)
                            .height(0, 64).distribution(TRIANGLE))
                    .oreGeneration(DEEPSLATE_ORE, c -> c.dimension("minecraft:overworld").biomes("#minecraft:is_overworld")
                            .replace("#minecraft:deepslate_ore_replaceables").veinSize(8).attemptsPerChunk(3)
                            .height(-64, 0).distribution(BOTTOM_HEAVY))
                    .oreGeneration(NETHER_ORE, c -> c.dimension("minecraft:the_nether").biomes("#minecraft:is_nether")
                            .replace("minecraft:netherrack").veinSize(8).attemptsPerChunk(3)
                            .height(8, 112).distribution(UNIFORM))
                    .oreGeneration(END_ORE, c -> c.dimension("minecraft:the_end").biomes("#minecraft:is_end")
                            .replace("minecraft:end_stone").veinSize(8).attemptsPerChunk(3)
                            .height(0, 80).distribution(TRIANGLE))
                    .foundry()
                    .alloy(160, input("steel", 1), input("platinum", 1))
                    .build(),

            vanilla("gold", "Gold", true)
                    .density(19300)
                    .equipment(MetalEquipmentProfiles.GOLD)
                    .oreMiningLevel(4).storageMiningLevel(4)
                    .form(NETHER_ORE, "minecraft:nether_gold_ore")
                    .oreMiningLevel(NETHER_ORE,2)
                    .foundry()
                    .build(),

            oreMetal("platinum", "Platinum")
                    .density(21450)
                    .oreMiningLevel(6).storageMiningLevel(6)
                    // Explicit deposits only; forms alone never enable generation.
                    .oreGeneration(ORE, c -> c.dimension("minecraft:overworld").biomes("#minecraft:is_overworld")
                            .replace("#minecraft:stone_ore_replaceables").veinSize(10).attemptsPerChunk(5)
                            .height(0, 64).distribution(TRIANGLE))
                    .oreGeneration(DEEPSLATE_ORE, c -> c.dimension("minecraft:overworld").biomes("#minecraft:is_overworld")
                            .replace("#minecraft:deepslate_ore_replaceables").veinSize(10).attemptsPerChunk(5)
                            .height(-64, 0).distribution(BOTTOM_HEAVY))
                    .oreGeneration(NETHER_ORE, c -> c.dimension("minecraft:the_nether").biomes("#minecraft:is_nether")
                            .replace("minecraft:netherrack").veinSize(10).attemptsPerChunk(5)
                            .height(8, 112).distribution(UNIFORM))
                    .oreGeneration(END_ORE, c -> c.dimension("minecraft:the_end").biomes("#minecraft:is_end")
                            .replace("minecraft:end_stone").veinSize(10).attemptsPerChunk(5)
                            .height(0, 80).distribution(TRIANGLE))
                    .foundry()
                    .alloy(input("iron", 1), input("gold", 1))
                    .build(),

            // Provisional fictional density (kg/m3); refine visually without changing sync order.
            // Reuse vanilla objects and generation. Ore/storage gate 6 preserves Steel-first access.
            MetalDefinition.builder("minecraft:netherite", "Netherite", 20000, 6)
                    .form(INGOT, "minecraft:netherite_ingot")
                    .form(STORAGE_BLOCK, "minecraft:netherite_block")
                    .form(NUGGET, "katzencraftmetals:netherite_nugget")
                    .form(RAW, "minecraft:netherite_scrap")
                    .form(ORE, "minecraft:ancient_debris")
                    .equipment(MetalEquipmentProfiles.NETHERITE)
                    .oreMiningLevel(6).storageMiningLevel(6)
                    .foundry()
                    .build());

    // 3. INTERNAL LOOKUP AND VALIDATION — normally leave unchanged
    private static final Map<ResourceLocation, MetalDefinition> BY_ID = index(ORDERED);

    private MetalCatalog() {}

    public static List<MetalDefinition> values() { return ORDERED; }

    public static Optional<MetalDefinition> get(ResourceLocation id) { return Optional.ofNullable(BY_ID.get(id)); }

    public static Map<ResourceLocation, MetalDefinition> index(List<MetalDefinition> definitions) {
        Map<ResourceLocation, MetalDefinition> result = new LinkedHashMap<>();
        java.util.Set<String> translationKeys = new java.util.HashSet<>();
        java.util.Set<ResourceLocation> forms = new java.util.HashSet<>();
        for (var definition : definitions) {
            if (result.putIfAbsent(definition.id(), definition) != null || !translationKeys.add(definition.translationKey()))
                throw new IllegalArgumentException("Duplicate metal ID/name key: " + definition.id());
            for (var form : definition.forms().keySet()) if (!forms.add(form))
                throw new IllegalArgumentException("Duplicate metal form: " + form);
        }
        for (var definition : definitions) {
            if (definition.alloy() == null) continue;
            for (var input : definition.alloy().inputs()) {
                var ingredient = result.get(input.metal());
                if (ingredient == null || ingredient.foundry() == null)
                    throw new IllegalArgumentException("Unknown or Foundry-disabled alloy ingredient " + input.metal() + " for " + definition.id());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    // 4. SPECIALIZED DECLARATION HELPERS

    /**
     * Standard mod metal plus declared raw items and four ore variants.
     * Additional objects/assets/loot/mining remain manual; generation is configured above;
     * only standard ingot/nugget/storage forms register automatically.
     * Storage defaults come from metal(); configure density and storage mining level above.
     */
    private static MetalDefinition.Builder oreMetal(String id, String name) {
        return metal(id, name).form(RAW, "katzencraftmetals:raw_" + id)
                .form(ORE, "katzencraftmetals:" + id + "_ore")
                .form(DEEPSLATE_ORE, "katzencraftmetals:deepslate_" + id + "_ore")
                .form(NETHER_ORE, "katzencraftmetals:nether_" + id + "_ore")
                .form(END_ORE, "katzencraftmetals:end_" + id + "_ore");
    }

    /**
     * References existing Minecraft ingot/block/raw/raw-block/normal/deepslate ore forms,
     * optionally a nugget. Does not register duplicates or change vanilla block properties.
     * Only use existing vanilla forms; copper has no nugget. Density and Foundry are explicit.
     */
    private static MetalDefinition.Builder vanilla(String id, String name, boolean nugget) {
        var builder = MetalDefinition.builder("minecraft:" + id, name, 0, 6)
                .form(INGOT, "minecraft:" + id + "_ingot").form(STORAGE_BLOCK, "minecraft:" + id + "_block")
                .form(RAW, "minecraft:raw_" + id).form(RAW_BLOCK, "minecraft:raw_" + id + "_block")
                .form(ORE, "minecraft:" + id + "_ore").form(DEEPSLATE_ORE, "minecraft:deepslate_" + id + "_ore");
        if (nugget) builder.form(NUGGET, "minecraft:" + id + "_nugget");
        return builder;
    }
}
