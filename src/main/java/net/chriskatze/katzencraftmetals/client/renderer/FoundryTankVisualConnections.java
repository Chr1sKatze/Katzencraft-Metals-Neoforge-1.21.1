package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Client-side visual connection rules for Foundry Tanks.
 *
 * Two directly adjacent Tanks connect visually when they belong to the same
 * logical Tank layout:
 *
 * - active Tanks compare their persistent Controller owner UUID
 * - unassigned Tanks compare their persistent orphan-layout UUID
 *
 * This allows two physically touching orphan layouts to retain separate
 * exterior frames.
 *
 * Older saved orphan Tanks that do not yet have an orphan-layout UUID keep
 * the previous null-to-null connection behavior until a newly placed Tank
 * causes that legacy section to be migrated.
 */
public final class FoundryTankVisualConnections {

    private FoundryTankVisualConnections() {
    }

    public static boolean isSameComponent(
            FoundryTankBlockEntity tank,
            Direction direction
    ) {
        return getSameComponentNeighbor(
                tank,
                direction
        ) != null;
    }

    @Nullable
    public static FoundryTankBlockEntity getSameComponentNeighbor(
            FoundryTankBlockEntity tank,
            Direction direction
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return null;
        }

        BlockPos neighborPos =
                tank.getBlockPos()
                        .relative(direction);

        return getMatchingTank(
                level,
                neighborPos,
                tank
        );
    }

    /**
     * Returns true when the adjacent Tank in edgeDirection contributes the
     * same exposed face tile.
     *
     * Merely having an adjacent Tank is not enough. Its face in faceDirection
     * must also be exposed, otherwise the current tile sits at the edge of a
     * stepped or L-shaped facade and must draw a frame there.
     */
    public static boolean hasAdjacentExposedFace(
            FoundryTankBlockEntity tank,
            Direction faceDirection,
            Direction edgeDirection
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return false;
        }

        BlockPos adjacentPos =
                tank.getBlockPos()
                        .relative(edgeDirection);

        FoundryTankBlockEntity adjacentTank =
                getMatchingTank(
                        level,
                        adjacentPos,
                        tank
                );

        if (adjacentTank == null) {
            return false;
        }

        BlockPos inFrontOfAdjacent =
                adjacentPos.relative(
                        faceDirection
                );

        return getMatchingTank(
                level,
                inFrontOfAdjacent,
                tank
        ) == null;
    }

    public static boolean hasAnyHorizontalNeighbor(
            FoundryTankBlockEntity tank
    ) {
        return isSameComponent(
                tank,
                Direction.NORTH
        )
                || isSameComponent(
                tank,
                Direction.SOUTH
        )
                || isSameComponent(
                tank,
                Direction.WEST
        )
                || isSameComponent(
                tank,
                Direction.EAST
        );
    }

    /**
     * A Faucet points away from its Tank. Therefore a Faucet occupying the
     * block in faceDirection belongs to this Tank face only when its FACING
     * property equals faceDirection.
     */
    public static boolean hasAttachedFaucet(
            FoundryTankBlockEntity tank,
            Direction faceDirection
    ) {
        if (!faceDirection.getAxis().isHorizontal()) {
            return false;
        }

        Level level =
                tank.getLevel();

        if (level == null) {
            return false;
        }

        BlockState faucetState =
                level.getBlockState(
                        tank.getBlockPos()
                                .relative(faceDirection)
                );

        return faucetState.getBlock()
                instanceof FoundryFaucetBlock
                && faucetState.hasProperty(
                FoundryFaucetBlock.FACING
        )
                && faucetState.getValue(
                FoundryFaucetBlock.FACING
        ) == faceDirection;
    }

    @Nullable
    private static FoundryTankBlockEntity getMatchingTank(
            Level level,
            BlockPos position,
            FoundryTankBlockEntity originTank
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(position);

        if (!(blockEntity instanceof FoundryTankBlockEntity otherTank)) {
            return null;
        }

        UUID originNetworkId =
                originTank.getNetworkId();

        UUID otherNetworkId =
                otherTank.getNetworkId();

        /*
         * If either Tank belongs to an active/owned network, only the
         * Controller UUID determines the connection.
         */
        if (
                originNetworkId != null
                        || otherNetworkId != null
        ) {
            return originNetworkId != null
                    && originNetworkId.equals(
                    otherNetworkId
            )
                    ? otherTank
                    : null;
        }

        UUID originOrphanLayoutId =
                originTank.getOrphanLayoutId();

        UUID otherOrphanLayoutId =
                otherTank.getOrphanLayoutId();

        /*
         * Backward compatibility for old worlds: two legacy orphan Tanks with
         * no layout UUID still connect exactly as before.
         */
        if (
                originOrphanLayoutId == null
                        && otherOrphanLayoutId == null
        ) {
            return otherTank;
        }

        return originOrphanLayoutId != null
                && originOrphanLayoutId.equals(
                otherOrphanLayoutId
        )
                ? otherTank
                : null;
    }
}
