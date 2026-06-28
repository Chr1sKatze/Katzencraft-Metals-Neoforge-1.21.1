package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryMultiMetalStorage;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Set;

/**
 * Moves real multi-metal contents into the surviving Tanks before the existing
 * upward-column dismantling code splits and reassigns the structural network.
 */
@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID
)
public final class FoundryMultiMetalEvents {

    private FoundryMultiMetalEvents() {
    }

    @SubscribeEvent(
            priority = EventPriority.LOWEST
    )
    public static void onTankBreak(
            BlockEvent.BreakEvent event
    ) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        if (level.isClientSide()) {
            return;
        }

        BlockPos pos =
                event.getPos();

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return;
        }

        Set<BlockPos> removedPositions =
                FoundryTankNetwork.findUpwardColumn(
                        level,
                        pos
                );

        if (removedPositions.isEmpty()) {
            return;
        }

        FoundryMultiMetalStorage.prepareRemoval(
                level,
                network,
                removedPositions
        );
    }
}
