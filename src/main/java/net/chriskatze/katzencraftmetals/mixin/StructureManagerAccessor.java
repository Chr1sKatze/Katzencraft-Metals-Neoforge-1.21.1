package net.chriskatze.katzencraftmetals.mixin;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureManager.class)
public interface StructureManagerAccessor {
    @Accessor("level") LevelAccessor katzencraftmetals$level();
}
