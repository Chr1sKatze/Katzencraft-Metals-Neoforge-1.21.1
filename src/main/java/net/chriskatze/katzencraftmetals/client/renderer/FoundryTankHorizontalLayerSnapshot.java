package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

record FoundryTankHorizontalLayerSnapshot(
        FoundryTankHorizontalLayerKey key,
        Map<ResourceLocation, Float> targetAmounts
) {
}
