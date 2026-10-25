package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Filters block damage only; Creeper entity damage, knockback and other blocks are untouched. */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public final class FoundryExplosionProtection {
    private FoundryExplosionProtection() {}

    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate event) {
        if (!(event.getExplosion().getDirectSourceEntity() instanceof Creeper)) return;
        event.getAffectedBlocks().removeIf(pos -> {
            var state = event.getLevel().getBlockState(pos);
            return state.is(ModBlocks.FOUNDRY_CONTROLLER.get()) || state.is(ModBlocks.FOUNDRY_TANK.get());
        });
    }
}
