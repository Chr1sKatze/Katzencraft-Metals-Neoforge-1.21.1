package net.chriskatze.katzencraftmetals.client.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Client-only reconciliation: vessel footprints survive attached/detached ownership changes. */
final class FoundryTankSoundTracker {
    record Source(Set<BlockPos> cells, boolean molten) {
        Source { cells = Set.copyOf(cells); }
    }
    private static final class Binding {
        Set<BlockPos> cells;
        final FoundryTankLoopSoundInstance sound;
        Binding(Source source) {
            cells = source.cells();
            sound = new FoundryTankLoopSoundInstance(center(cells));
        }
        void updateLayout(Source source) {
            if (cells.equals(source.cells())) return;
            cells = source.cells();
            sound.moveTo(center(cells));
        }
    }

    /** Equal-sized Tank volumes: centroid of their centers, not their bounding box or molten distribution. */
    private static Vec3 center(Set<BlockPos> cells) {
        long x=0, y=0, z=0;
        for (BlockPos pos : cells) { x+=pos.getX(); y+=pos.getY(); z+=pos.getZ(); }
        return new Vec3((double)x/cells.size()+0.5D, (double)y/cells.size()+0.5D, (double)z/cells.size()+0.5D);
    }

    private final List<Binding> bindings = new ArrayList<>();

    void update(List<Source> sources, Predicate<BlockPos> loadedTank,
                Consumer<FoundryTankLoopSoundInstance> play, Consumer<FoundryTankLoopSoundInstance> stop) {
        bindings.removeIf(binding -> binding.sound.isStopped());
        var used = new HashSet<Binding>();
        var occupied = new HashSet<BlockPos>();
        for (Source source : sources) {
            // Attached sources are supplied first. A stale detached view cannot double-play
            // or override an authoritative empty attached vessel during ordered handoff.
            if (!Collections.disjoint(occupied, source.cells())) continue;
            occupied.addAll(source.cells());
            if (!source.molten()) continue;
            if (source.cells().stream().noneMatch(loadedTank)) continue;
            Binding binding = bindings.stream().filter(b -> !used.contains(b)
                    && !Collections.disjoint(b.cells, source.cells())).findFirst().orElse(null);
            if (binding == null) {
                binding = new Binding(source);
                bindings.add(binding);
                play.accept(binding.sound);
            }
            binding.updateLayout(source);
            binding.sound.setMolten(true);
            used.add(binding);
        }
        for (var iterator = bindings.iterator(); iterator.hasNext();) {
            Binding binding = iterator.next();
            if (used.contains(binding)) continue;
            // Merges must retire redundant loops immediately, rather than overlap fades.
            boolean merged = used.stream().anyMatch(b -> !Collections.disjoint(b.cells, binding.cells));
            if (merged || binding.cells.stream().noneMatch(loadedTank)) {
                binding.sound.cancel(); stop.accept(binding.sound); iterator.remove();
            } else binding.sound.setMolten(false);
        }
    }

    void clear(Consumer<FoundryTankLoopSoundInstance> stop) {
        bindings.forEach(binding -> { binding.sound.cancel(); stop.accept(binding.sound); });
        bindings.clear();
    }
}
