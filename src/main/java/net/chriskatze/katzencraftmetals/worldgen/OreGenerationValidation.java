package net.chriskatze.katzencraftmetals.worldgen;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Validate active catalog entries against the actual loaded server/datapacks, including modded dimensions. */
@EventBusSubscriber(modid = "katzencraftmetals")
public final class OreGenerationValidation {
    private OreGenerationValidation() {}
    @SubscribeEvent
    public static void validate(ServerAboutToStartEvent event) {
        var access = event.getServer().registryAccess();
        var dimensions = access.registryOrThrow(Registries.LEVEL_STEM);
        for (var entry : ModOreGeneration.entries()) {
            String label = "Ore generation " + entry.id();
            var dimension = dimensions.getOptional(entry.config().dimension()).orElseThrow(() -> new IllegalArgumentException(label + ": missing dimension " + entry.config().dimension()));
            var type = dimension.type().value();
            if (entry.config().maxY() < type.minY() || entry.config().minY() >= type.minY() + type.height())
                throw new IllegalArgumentException(label + ": heights do not intersect dimension build limits");
            require(access.registryOrThrow(Registries.BLOCK), entry.block().toString(), label);
            entry.config().replacements().forEach(s -> require(access.registryOrThrow(Registries.BLOCK), s, label));
            entry.config().biomes().forEach(s -> require(access.registryOrThrow(Registries.BIOME), s, label));
        }
    }
    private static <T> void require(Registry<T> registry, String selector, String label) {
        if (selector.startsWith("#")) {
            var tag = TagKey.create(registry.key(), ResourceLocation.parse(selector.substring(1)));
            if (registry.getTag(tag).filter(set -> set.size() > 0).isEmpty())
                throw new IllegalArgumentException(label + ": missing/empty tag " + selector);
        } else if (!registry.containsKey(ResourceLocation.parse(selector)))
            throw new IllegalArgumentException(label + ": unknown ID " + selector);
    }
}
