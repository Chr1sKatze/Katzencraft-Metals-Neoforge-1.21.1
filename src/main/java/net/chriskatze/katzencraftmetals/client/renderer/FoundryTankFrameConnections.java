package net.chriskatze.katzencraftmetals.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.function.BiPredicate;

/** Reciprocal saved connection flags, sampled only during chunk model-data rebuilds. */
final class FoundryTankFrameConnections {
    private FoundryTankFrameConnections() {}

    static Set<BlockPos> collect(BlockPos origin, BiPredicate<BlockPos, Direction> connected) {
        Set<BlockPos> result = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        result.add(origin); pending.add(origin);
        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                // All corner-mask probes lie in this 3x3x3 neighborhood.
                if (Math.abs(next.getX() - origin.getX()) > 1
                        || Math.abs(next.getY() - origin.getY()) > 1
                        || Math.abs(next.getZ() - origin.getZ()) > 1
                        || result.contains(next)) continue;
                if (connected.test(current, direction) && result.add(next)) pending.addLast(next);
            }
        }
        return Set.copyOf(result);
    }
}
