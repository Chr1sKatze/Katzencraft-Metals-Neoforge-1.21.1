package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

record FoundryTankHorizontalLayerKey(
        Level level,
        BlockPos anchor,
        int y
) {
}
