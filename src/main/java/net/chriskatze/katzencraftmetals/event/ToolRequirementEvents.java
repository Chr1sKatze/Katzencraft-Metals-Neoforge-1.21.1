package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.config.BlockRequirements;
import net.chriskatze.katzencraftmetals.config.ToolLevels;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public class ToolRequirementEvents {

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {

        BlockState state = event.getTargetBlock();

        int requiredLevel =
                BlockRequirements.getRequirement(
                        state.getBlock()
                );

        int toolLevel =
                ToolLevels.getLevel(
                        event.getEntity().getMainHandItem()
                );

        event.setCanHarvest(
                toolLevel >= requiredLevel
        );
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {

        int requiredLevel =
                BlockRequirements.getRequirement(
                        event.getState().getBlock()
                );

        int toolLevel =
                ToolLevels.getLevel(
                        event.getEntity().getMainHandItem()
                );

        if (toolLevel < requiredLevel) {
            event.setNewSpeed(
                    event.getNewSpeed() * 0.01F
            );
        }
    }
}