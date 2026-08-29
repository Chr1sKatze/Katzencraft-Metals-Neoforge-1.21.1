package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Network-level scheduler for lever-controlled automatic pouring.
 *
 * The scheduler checks the total molten column height at the faucet tank, not
 * the physical height of the selected metal layer. This matches the Faucet's
 * existing gameplay behavior: the selected output metal may be drawn from the
 * connected network as long as the combined molten level reaches the Faucet.
 */
final class FoundryTankAutoPourScheduler {

    private static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(pos -> pos.getX())
                    .thenComparingInt(pos -> pos.getZ());

    private FoundryTankAutoPourScheduler() {
    }

    static boolean mayAutoStartFaucet(
            Level level,
            BlockPos faucetPos,
            BlockState faucetState,
            FoundryFaucetBlockEntity faucet
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || faucet == null
        ) {
            return false;
        }

        FoundryTankNetwork network =
                findNetworkForFaucet(
                        level,
                        faucetPos,
                        faucetState
                );

        if (
                network == null
                        || !FoundryTankLeverAutoPourControl
                        .isAutoPourEnabled(
                                level,
                                network
                        )
        ) {
            return false;
        }

        Set<BlockPos> allowedStarts =
                planAllowedStarts(
                        level,
                        network
                );

        return allowedStarts.contains(
                faucetPos.immutable()
        );
    }

    @Nullable
    private static FoundryTankNetwork findNetworkForFaucet(
            Level level,
            BlockPos faucetPos,
            BlockState faucetState
    ) {
        if (
                faucetState == null
                        || !faucetState.hasProperty(
                        FoundryFaucetBlock.FACING
                )
        ) {
            return null;
        }

        Direction facing =
                faucetState.getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos tankPos =
                faucetPos.relative(
                        facing.getOpposite()
                );

        BlockEntity blockEntity =
                level.getBlockEntity(
                        tankPos
                );

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return null;
        }

        return tank.getNetwork();
    }

    private static Set<BlockPos> planAllowedStarts(
            Level level,
            FoundryTankNetwork network
    ) {
        Set<BlockPos> allowedStarts =
                new HashSet<>();

        network.ensureMoltenContentsMigrated();

        Map<ResourceLocation, Integer> availableByMetal =
                new HashMap<>(
                        network.getMoltenContents()
                );

        Map<BlockPos, Integer> localAvailableByTank =
                createLocalAvailabilityMap(
                        network
                );

        List<BlockPos> faucetPositions =
                FoundryTankNetwork.findAttachedFaucets(
                        level,
                        network.getTankPositions()
                );

        faucetPositions.sort(
                POSITION_ORDER
        );

        /*
         * First reserve for pours that are already running, manual or automatic.
         * This prevents a newly scheduled auto-pour from stealing metal that an
         * active stream is already visually committed to using.
         */
        for (BlockPos faucetPos : faucetPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            faucetPos
                    );

            if (!(blockEntity instanceof FoundryFaucetBlockEntity faucet)) {
                continue;
            }

            if (!faucet.isPouring()) {
                continue;
            }

            FaucetPlanCandidate candidate =
                    createCandidate(
                            level,
                            network,
                            faucetPos,
                            faucet,
                            true
                    );

            if (candidate != null) {
                reserveActivePour(
                        candidate,
                        availableByMetal,
                        localAvailableByTank
                );
            }
        }

        /*
         * Then schedule new automatic starts. Every accepted Faucet reserves
         * enough molten metal to completely fill its current target basin.
         */
        for (BlockPos faucetPos : faucetPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            faucetPos
                    );

            if (!(blockEntity instanceof FoundryFaucetBlockEntity faucet)) {
                continue;
            }

            if (
                    faucet.isPouring()
                            || faucet.isDraining()
            ) {
                continue;
            }

            FaucetPlanCandidate candidate =
                    createCandidate(
                            level,
                            network,
                            faucetPos,
                            faucet,
                            false
                    );

            if (
                    candidate != null
                            && canReserve(
                            candidate,
                            availableByMetal,
                            localAvailableByTank
                    )
            ) {
                reserve(
                        candidate,
                        availableByMetal,
                        localAvailableByTank
                );

                allowedStarts.add(
                        faucetPos.immutable()
                );
            }
        }

        return allowedStarts;
    }

    private static Map<BlockPos, Integer> createLocalAvailabilityMap(
            FoundryTankNetwork network
    ) {
        Map<BlockPos, Integer> localAvailableByTank =
                new HashMap<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            localAvailableByTank.put(
                    tankPos.immutable(),
                    Math.max(
                            0,
                            (int) Math.floor(
                                    network.getLocalVisualMoltenAmount(
                                            tankPos
                                    )
                            )
                    )
            );
        }

        return localAvailableByTank;
    }

    @Nullable
    private static FaucetPlanCandidate createCandidate(
            Level level,
            FoundryTankNetwork network,
            BlockPos faucetPos,
            FoundryFaucetBlockEntity faucet,
            boolean activePour
    ) {
        BlockState faucetState =
                level.getBlockState(
                        faucetPos
                );

        if (!faucetState.hasProperty(FoundryFaucetBlock.FACING)) {
            return null;
        }

        Direction facing =
                faucetState.getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos tankPos =
                faucetPos.relative(
                        facing.getOpposite()
                );

        if (!network.getTankPositions().contains(tankPos)) {
            return null;
        }

        BlockEntity tankBlockEntity =
                level.getBlockEntity(
                        tankPos
                );

        if (!(tankBlockEntity instanceof FoundryTankBlockEntity tank)) {
            return null;
        }

        FoundryFaucetBlockEntity.CauldronTarget target =
                FoundryFaucetBlockEntity.findCauldronTarget(
                        level,
                        faucetPos
                );

        if (target == null) {
            return null;
        }

        CastingCauldronBlockEntity cauldron =
                target.cauldron();

        int requiredAmount =
                cauldron.getRemainingCapacity();

        if (
                requiredAmount <= 0
                        || requiredAmount
                        % FoundryFaucetBlockEntity.TRANSFER_AMOUNT
                        != 0
        ) {
            return null;
        }

        ResourceLocation metal =
                activePour && faucet.getPouringMetal() != null
                        ? faucet.getPouringMetal()
                        : faucet.resolveOutputMetal(
                        tank
                ).orElse(null);

        if (metal == null) {
            return null;
        }

        /*
         * For automatic scheduling, the basin must be able to accept the whole
         * planned job. Height is checked by total molten amount at this physical
         * faucet tank, not by the selected metal's layer height.
         */
        if (!cauldron.canAccept(
                metal,
                requiredAmount
        )) {
            return null;
        }

        return new FaucetPlanCandidate(
                faucetPos.immutable(),
                tankPos.immutable(),
                metal,
                requiredAmount
        );
    }

    private static boolean canReserve(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<BlockPos, Integer> localAvailableByTank
    ) {
        return availableByMetal.getOrDefault(
                candidate.metal(),
                0
        ) >= candidate.requiredAmount()
                && localAvailableByTank.getOrDefault(
                candidate.tankPos(),
                0
        ) >= candidate.requiredAmount();
    }

    private static void reserveActivePour(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<BlockPos, Integer> localAvailableByTank
    ) {
        if (canReserve(
                candidate,
                availableByMetal,
                localAvailableByTank
        )) {
            reserve(
                    candidate,
                    availableByMetal,
                    localAvailableByTank
            );

            return;
        }

        /*
         * If an already-running pour can no longer be fully reserved, do not
         * let new automatic starts compete with it this tick. The active pour's
         * normal transfer validation will stop it cleanly if it cannot continue.
         */
        availableByMetal.put(
                candidate.metal(),
                0
        );

        localAvailableByTank.put(
                candidate.tankPos(),
                0
        );
    }

    private static void reserve(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<BlockPos, Integer> localAvailableByTank
    ) {
        availableByMetal.put(
                candidate.metal(),
                availableByMetal.getOrDefault(
                        candidate.metal(),
                        0
                ) - candidate.requiredAmount()
        );

        localAvailableByTank.put(
                candidate.tankPos(),
                localAvailableByTank.getOrDefault(
                        candidate.tankPos(),
                        0
                ) - candidate.requiredAmount()
        );
    }

    private record FaucetPlanCandidate(
            BlockPos faucetPos,
            BlockPos tankPos,
            ResourceLocation metal,
            int requiredAmount
    ) {
    }
}
