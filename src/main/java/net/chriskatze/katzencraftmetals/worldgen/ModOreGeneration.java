package net.chriskatze.katzencraftmetals.worldgen;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.metal.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.*;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.*;
import java.util.*;
import java.util.function.Supplier;

/** Registry/datagen adapter. Configuration belongs exclusively to MetalCatalog. */
public final class ModOreGeneration {
    private ModOreGeneration() {}
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, "katzencraftmetals");
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOMES = DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, "katzencraftmetals");
    public static final Supplier<PlacementModifierType<OreHeightPlacement>> HEIGHT = PLACEMENTS.register("ore_height", () -> () -> OreHeightPlacement.CODEC);
    public static final Supplier<PlacementModifierType<OreReplacementFilter>> REPLACEMENT = PLACEMENTS.register("ore_replacement", () -> () -> OreReplacementFilter.CODEC);
    public static final Supplier<MapCodec<CatalogOreBiomeModifier>> BIOME = BIOMES.register("catalog_ores", () -> CatalogOreBiomeModifier.CODEC);
    public static void register(IEventBus bus) { PLACEMENTS.register(bus); BIOMES.register(bus); }

    public record Entry(MetalDefinition metal, MetalOreGeneration config, ResourceLocation id) {
        public OreGenerationScope scope() { return new OreGenerationScope(config.dimension(), config.biomes()); }
        public ResourceLocation block() { return metal.forms().entrySet().stream().filter(e -> e.getValue() == config.form()).findFirst().orElseThrow().getKey(); }
    }
    public static List<Entry> entries() { return entries(MetalCatalog.values()); }
    public static List<Entry> entries(List<MetalDefinition> metals) {
        var result = new ArrayList<Entry>();
        for (var metal : metals) for (int index = 0; index < metal.oreGeneration().size(); index++) {
            var config = metal.oreGeneration().get(index);
            if (config.enabled()) result.add(new Entry(metal, config, id("catalog/" + metal.id().getNamespace() + "/" + metal.id().getPath() + "/" + index)));
        }
        return List.copyOf(result);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("katzencraftmetals", path); }
    public static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, ResourceLocation id) { return ResourceKey.create(registry, id); }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) { configured(context, entries()); }
    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context, List<Entry> entries) {
        var blocks = context.lookup(Registries.BLOCK);
        var biomes = context.lookup(Registries.BIOME);
        for (var entry : entries) {
            // Resolve both IDs and tags during datagen; misspellings cannot silently produce no ore.
            for (String biome : entry.config.biomes()) {
                if (biome.startsWith("#")) biomes.getOrThrow(TagKey.create(Registries.BIOME, ResourceLocation.parse(biome.substring(1))));
                else biomes.getOrThrow(key(Registries.BIOME, ResourceLocation.parse(biome)));
            }
            var state = blocks.getOrThrow(key(Registries.BLOCK, entry.block())).value().defaultBlockState();
            var targets = new ArrayList<OreConfiguration.TargetBlockState>();
            for (String selector : entry.config.replacements()) {
                RuleTest rule;
                if (selector.startsWith("#")) {
                    var tag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(selector.substring(1)));
                    blocks.getOrThrow(tag); rule = new TagMatchTest(tag);
                } else rule = new BlockMatchTest(blocks.getOrThrow(key(Registries.BLOCK, ResourceLocation.parse(selector))).value());
                targets.add(OreConfiguration.target(rule, state));
            }
            context.register(key(Registries.CONFIGURED_FEATURE, entry.id), new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(targets, entry.config.veinSize())));
        }
    }
    public static void placed(BootstrapContext<PlacedFeature> context) { placed(context, entries()); }
    public static void placed(BootstrapContext<PlacedFeature> context, List<Entry> entries) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE);
        for (var entry : entries) context.register(key(Registries.PLACED_FEATURE, entry.id), new PlacedFeature(
                configured.getOrThrow(key(Registries.CONFIGURED_FEATURE, entry.id)), List.of(
                        CountPlacement.of(entry.config.attemptsPerChunk()), InSquarePlacement.spread(),
                        new OreHeightPlacement(entry.scope(), entry.config.minY(), entry.config.maxY(), entry.config.distribution()), BiomeFilter.biome())));
    }
    public static void biome(BootstrapContext<BiomeModifier> context) { biome(context, entries()); }
    public static void biome(BootstrapContext<BiomeModifier> context, List<Entry> entries) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var additions = entries.stream().map(e -> new CatalogOreBiomeModifier.Addition(e.scope(), placed.getOrThrow(key(Registries.PLACED_FEATURE, e.id)))).toList();
        var replacements = replacementScopes(entries).entrySet().stream().map(entry -> new CatalogOreBiomeModifier.Replacement(
                placed.getOrThrow(key(Registries.PLACED_FEATURE, entry.getKey())), entry.getValue())).toList();
        context.register(key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, id("catalog_ores")), new CatalogOreBiomeModifier(additions, replacements));
    }
    public static Map<ResourceLocation, List<OreGenerationScope>> replacementScopes() { return replacementScopes(entries()); }
    public static Map<ResourceLocation, List<OreGenerationScope>> replacementScopes(List<Entry> entries) {
        var result = new LinkedHashMap<ResourceLocation, List<OreGenerationScope>>();
        for (var entry : entries) if (entry.config.mode() == MetalOreGeneration.Mode.REPLACE)
            for (var original : vanillaPlacements(entry.metal.id(), entry.config.form())) {
                var scopes = result.computeIfAbsent(original, ignored -> new ArrayList<>());
                if (!scopes.contains(entry.scope())) scopes.add(entry.scope());
            }
        result.replaceAll((id, scopes) -> List.copyOf(scopes));
        return Collections.unmodifiableMap(result);
    }
    /** Stone/deepslate share native configured features and are replaced as one placement family. */
    public static List<ResourceLocation> vanillaPlacements(ResourceLocation metal, MetalDefinition.Form form) {
        if (!metal.getNamespace().equals("minecraft")) return List.of();
        List<String> paths = switch (metal.getPath()) {
            case "iron" -> form == MetalDefinition.Form.ORE || form == MetalDefinition.Form.DEEPSLATE_ORE
                    ? List.of("ore_iron_upper", "ore_iron_middle", "ore_iron_small") : List.of();
            case "copper" -> form == MetalDefinition.Form.ORE || form == MetalDefinition.Form.DEEPSLATE_ORE
                    ? List.of("ore_copper", "ore_copper_large") : List.of();
            case "gold" -> switch (form) {
                case ORE, DEEPSLATE_ORE -> List.of("ore_gold", "ore_gold_lower", "ore_gold_extra");
                case NETHER_ORE -> List.of("ore_gold_nether", "ore_gold_deltas");
                default -> List.of();
            };
            case "netherite" -> form == MetalDefinition.Form.ORE ? List.of("ore_ancient_debris_large", "ore_debris_small") : List.of();
            default -> List.of();
        };
        return paths.stream().map(ResourceLocation::withDefaultNamespace).toList();
    }
}
