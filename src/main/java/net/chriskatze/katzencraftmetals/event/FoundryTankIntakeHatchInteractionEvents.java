package net.chriskatze.katzencraftmetals.event;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

        Direction clickedFace =
                event.getFace();

        if (clickedFace == null) {
            return;
        }

        /*
         * Only tank top and tank side shift-clicks are special foundry
         * attachment interactions. Bottom clicks can pass through normally.
         */
        if (
                clickedFace != Direction.UP
                        && !clickedFace.getAxis().isHorizontal()
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

        if (clickedFace == Direction.UP) {
            toggleIntakeHatch(
                    level,
                    pos,
                    tank,
                    player
            );

            return;
        }

        toggleSideFaucet(
                level,
                pos,
                clickedFace,
                player
        );
    }

    private static void toggleIntakeHatch(
            Level level,
            BlockPos pos,
            FoundryTankBlockEntity tank,
            Player player
    ) {
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

        playAttachmentClick(
                level,
                pos,
                tank.isIntakeHatchOpen()
                        ? 1.2f
                        : 0.8f
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

    private static void toggleSideFaucet(
            Level level,
            BlockPos tankPos,
            Direction side,
            Player player
    ) {
        BlockPos faucetPos =
                tankPos.relative(
                        side
                );

        BlockState currentState =
                level.getBlockState(
                        faucetPos
                );

        if (isMatchingFaucet(
                currentState,
                side
        )) {
            level.destroyBlock(
                    faucetPos,
                    false
            );

            playAttachmentClick(
                    level,
                    faucetPos,
                    0.8f
            );

            player.displayClientMessage(
                    Component.literal(
                            "Foundry faucet removed."
                    ),
                    true
            );

            return;
        }

        if (!currentState.isAir()) {
            player.displayClientMessage(
                    Component.literal(
                            "That Tank side is blocked."
                    ),
                    true
            );

            return;
        }

        BlockState faucetState =
                ModBlocks.FOUNDRY_FAUCET
                        .get()
                        .defaultBlockState()
                        .setValue(
                                FoundryFaucetBlock.FACING,
                                side
                        );

        if (!faucetState.canSurvive(
                level,
                faucetPos
        )) {
            player.displayClientMessage(
                    Component.literal(
                            "A Foundry faucet cannot be attached there."
                    ),
                    true
            );

            return;
        }

        boolean placed =
                level.setBlockAndUpdate(
                        faucetPos,
                        faucetState
                );

        if (!placed) {
            player.displayClientMessage(
                    Component.literal(
                            "The Foundry faucet could not be attached."
                    ),
                    true
            );

            return;
        }

        playAttachmentClick(
                level,
                faucetPos,
                1.2f
        );

        player.displayClientMessage(
                Component.literal(
                        "Foundry faucet attached."
                ),
                true
        );
    }

    private static boolean isMatchingFaucet(
            BlockState state,
            Direction side
    ) {
        return state.is(
                ModBlocks.FOUNDRY_FAUCET.get()
        )
                && state.hasProperty(
                FoundryFaucetBlock.FACING
        )
                && state.getValue(
                FoundryFaucetBlock.FACING
        ) == side;
    }

    private static void playAttachmentClick(
            Level level,
            BlockPos pos,
            float pitch
    ) {
        level.playSound(
                null,
                pos,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.5f,
                pitch
        );
    }
}
