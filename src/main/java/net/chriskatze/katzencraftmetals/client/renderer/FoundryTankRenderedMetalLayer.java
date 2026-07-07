package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.resources.ResourceLocation;

record FoundryTankRenderedMetalLayer(
        ResourceLocation metal,
        float minY,
        float maxY,
        boolean renderTop,
        boolean renderBottom
) {
}
