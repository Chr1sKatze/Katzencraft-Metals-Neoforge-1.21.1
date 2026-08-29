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
 * The scheduler draws the selected metal from the complete connected network,
 * but the physical liquid height must still be able to sustain a full scheduled
 * pour at the faucet's tank level.
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

        Map<Integer, Integer> availableVolumeAtOrAboveY =
                createHeightAvailabilityMap(
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
                        availableVolumeAtOrAboveY
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
                            availableVolumeAtOrAboveY
                    )
            ) {
                reserve(
                        candidate,
                        availableByMetal,
                        availableVolumeAtOrAboveY
                );

                allowedStarts.add(
                        faucetPos.immutable()
                );
            }
        }

        return allowedStarts;
    }

    /**
     * For each tank Y-level, count how much total molten volume exists at or
     * above that level.
     *
     * Example:
     * - bottom faucet: capacity below = 0, so it can use the whole network
     *   volume as its height source.
     * - upper faucet: capacity below = every tank block in lower layers, so it
     *   waits until enough molten volume exists above those lower layers.
     *
     * This avoids the old mistake of requiring one single local tank column to
     * contain the whole cauldron amount.
     */
    private static Map<Integer, Integer> createHeightAvailabilityMap(
            FoundryTankNetwork network
    ) {
        Map<Integer, Integer> tanksPerY =
                new HashMap<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            tanksPerY.merge(
                    tankPos.getY(),
                    1,
                    Integer::sum
            );
        }

        Map<Integer, Integer> result =
                new HashMap<>();

        int totalMoltenAmount =
                network.getTotalMoltenAmount();

        for (Integer y : tanksPerY.keySet()) {
            int capacityBelowY =
                    0;

            for (Map.Entry<Integer, Integer> entry : tanksPerY.entrySet()) {
                if (entry.getKey() >= y) {
                    continue;
                }

                capacityBelowY +=
                        entry.getValue()
                                * FoundryTankBlockEntity.CAPACITY;
            }

            result.put(
                    y,
                    Math.max(
                            0,
                            totalMoltenAmount - capacityBelowY
                    )
            );
        }

        return result;
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
         * planned job. The selected metal may be drawn from the complete
         * connected network, but the total liquid height at this faucet's
         * physical Y-level must also be able to sustain the full pour.
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
                tankPos.getY(),
                metal,
                requiredAmount
        );
    }

    private static boolean canReserve(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<Integer, Integer> availableVolumeAtOrAboveY
    ) {
        return availableByMetal.getOrDefault(
                candidate.metal(),
                0
        ) >= candidate.requiredAmount()
                && availableVolumeAtOrAboveY.getOrDefault(
                candidate.tankY(),
                0
        ) >= candidate.requiredAmount();
    }

    private static void reserveActivePour(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<Integer, Integer> availableVolumeAtOrAboveY
    ) {
        if (canReserve(
                candidate,
                availableByMetal,
                availableVolumeAtOrAboveY
        )) {
            reserve(
                    candidate,
                    availableByMetal,
                    availableVolumeAtOrAboveY
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

        zeroHeightAvailabilityAtOrAbove(
                candidate.tankY(),
                availableVolumeAtOrAboveY
        );
    }

    private static void reserve(
            FaucetPlanCandidate candidate,
            Map<ResourceLocation, Integer> availableByMetal,
            Map<Integer, Integer> availableVolumeAtOrAboveY
    ) {
        availableByMetal.put(
                candidate.metal(),
                availableByMetal.getOrDefault(
                        candidate.metal(),
                        0
                ) - candidate.requiredAmount()
        );

        /*
         * A successful pour reservation removes that much total liquid volume
         * from the network, so every faucet height budget must be reduced.
         */
        for (Map.Entry<Integer, Integer> entry : availableVolumeAtOrAboveY.entrySet()) {
            entry.setValue(
                    Math.max(
                            0,
                            entry.getValue()
                                    - candidate.requiredAmount()
                    )
            );
        }
    }

    private static void zeroHeightAvailabilityAtOrAbove(
            int tankY,
            Map<Integer, Integer> availableVolumeAtOrAboveY
    ) {
        for (Map.Entry<Integer, Integer> entry : availableVolumeAtOrAboveY.entrySet()) {
            if (entry.getKey() >= tankY) {
                entry.setValue(
                        0
                );
            }
        }
    }

    private record FaucetPlanCandidate(
            BlockPos faucetPos,
            BlockPos tankPos,
            int tankY,
            ResourceLocation metal,
            int requiredAmount
    ) {
    }
}
