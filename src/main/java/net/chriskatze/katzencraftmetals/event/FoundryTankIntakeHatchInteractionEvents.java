package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID)
public final class FoundryTankIntakeHatchInteractionEvents {

    private FoundryTankIntakeHatchInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        Player player =
                event.getEntity();

        if (
                event.getHand() != InteractionHand.MAIN_HAND
                        || !player.isShiftKeyDown()
        ) {
            return;
        }

        Level level =
                event.getLevel();

        BlockPos pos =
                event.getPos();

        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return;
        }

        /*
         * Catch shift-right-click before held blocks/items can steal the click.
         */
        event.setCanceled(true);
        event.setCancellationResult(
                InteractionResult.SUCCESS
        );

        if (level.isClientSide()) {
            return;
        }

        if (!tank.isTopTank()) {
            player.displayClientMessage(
                    Component.literal(
                            "Only a top Tank can be opened as an intake hatch."
                    ),
                    true
            );

            return;
        }

        if (!tank.hasActiveController()) {
            player.displayClientMessage(
                    Component.literal(
                            "The intake hatch needs an active Foundry Controller."
                    ),
                    true
            );

            return;
        }

        tank.setIntakeHatchOpen(
                !tank.isIntakeHatchOpen()
        );

        player.displayClientMessage(
                Component.literal(
                        tank.isIntakeHatchOpen()
                                ? "Foundry intake hatch opened."
                                : "Foundry intake hatch closed."
                ),
                true
        );
    }
}
