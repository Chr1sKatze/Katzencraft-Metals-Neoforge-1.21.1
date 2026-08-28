package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
public final class FoundryFaucetInteractionEvents {

    private FoundryFaucetInteractionEvents() {
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

        if (!(
                level.getBlockState(pos)
                        .getBlock()
                        instanceof FoundryFaucetBlock
        )) {
            return;
        }

        /*
         * This catches shift-right-click before the held item or held block gets
         * to handle the click. Without this event hook, block items can steal the
         * interaction before FoundryFaucetBlock#useWithoutItem is reached.
         */
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide()) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (
                blockEntity instanceof FoundryFaucetBlockEntity faucet
                        && player instanceof ServerPlayer serverPlayer
        ) {
            serverPlayer.openMenu(
                    faucet,
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
    }
}
