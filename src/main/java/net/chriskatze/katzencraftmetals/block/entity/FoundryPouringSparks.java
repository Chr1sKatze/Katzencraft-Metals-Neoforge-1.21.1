package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/** Shared vanilla Basin impact spark; a table may force one confirmed-transfer accent. */
public final class FoundryPouringSparks {
    public static void spawnImpact(Level level, double x, double y, double z,
                                   double driftX, double driftZ, boolean guaranteed) {
        if (!level.isClientSide()) return;
        if (guaranteed) level.addAlwaysVisibleParticle(ParticleTypes.LAVA, true, x,y,z,driftX,.010,driftZ);
        else level.addParticle(ParticleTypes.LAVA, x,y,z,driftX,.010,driftZ);
    }
    private FoundryPouringSparks() {}
}
