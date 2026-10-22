package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.BiPredicate;

/** Immutable pre-edit vessel. Integer units are apportioned within each horizontal layer,
 * in density order, with a stable position-order remainder. Nothing is moved out of a removed tank. */
public record FoundryVesselSnapshot(BlockPos anchor, Set<BlockPos> positions, Map<ResourceLocation,Integer> contents, BlockPos renderOrigin) {
    public FoundryVesselSnapshot(BlockPos anchor, Set<BlockPos> positions, Map<ResourceLocation,Integer> contents) {
        this(anchor, positions, contents, anchor);
    }
    public static final Comparator<BlockPos> ORDER = Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
    public FoundryVesselSnapshot {
        positions = Set.copyOf(positions); contents = Map.copyOf(contents); anchor = anchor.immutable(); renderOrigin = renderOrigin.immutable();
        if (!positions.isEmpty() && !positions.contains(anchor)) throw new IllegalArgumentException("Vessel anchor outside layout");
        if (contents.values().stream().anyMatch(v -> v <= 0) || contents.values().stream().mapToLong(Integer::longValue).sum() > (long)positions.size()*FoundryTankNetwork.TANK_CAPACITY)
            throw new IllegalArgumentException("Invalid vessel quantities");
    }
    /** Preserve only the units attributed to surviving cells before the edit, never capacity-trim the pooled top. */
    public FoundryVesselSnapshot retain(Set<BlockPos> surviving) {
        if (!positions.containsAll(surviving)) throw new IllegalArgumentException("Survivors outside original layout");
        Map<ResourceLocation,Integer> remaining = new HashMap<>(contents), kept = new LinkedHashMap<>();
        var layers = new TreeMap<Integer,List<BlockPos>>();
        positions.stream().sorted(ORDER).forEach(p -> layers.computeIfAbsent(p.getY(), k -> new ArrayList<>()).add(p));
        for (var cells : layers.values()) {
            int free = cells.size()*FoundryTankNetwork.TANK_CAPACITY, cursor = 0;
            for (var metal : ModMoltenMetals.heaviestFirst()) {
                int amount = Math.min(free,remaining.getOrDefault(metal.id(),0));
                for (int i=0;i<amount;i++) if (surviving.contains(cells.get((cursor+i)%cells.size()))) kept.merge(metal.id(),1,Integer::sum);
                cursor = (cursor+amount)%cells.size();free-=amount;
                remaining.computeIfPresent(metal.id(),(id,old)->old-amount);
            }
        }
        if (remaining.values().stream().anyMatch(v -> v != 0)) throw new IllegalStateException("Unallocated vessel metal");
        var nextAnchor = surviving.contains(anchor) ? anchor : surviving.stream().min(ORDER).orElse(anchor);
        return new FoundryVesselSnapshot(nextAnchor,surviving,kept,renderOrigin);
    }
    /** One deterministic valid component only. Grow a valid anchored subset if removal broke the simple-fluid rule. */
    public static Set<BlockPos> survivors(Set<BlockPos> available, List<BlockPos> preferred, BlockPos anchor,
                                           BiPredicate<BlockPos,BlockPos> connected) {
        if (available.isEmpty()) return Set.of();
        BlockPos seed = preferred.stream().filter(available::contains).findFirst()
                .orElse(available.contains(anchor) ? anchor : available.stream().min(ORDER).orElseThrow());
        var component = new LinkedHashSet<BlockPos>();var queue = new ArrayDeque<BlockPos>();component.add(seed);queue.add(seed);
        while(!queue.isEmpty()) {var p=queue.remove();for(var d:Direction.values()) {var n=p.relative(d);if(available.contains(n)&&!component.contains(n)&&connected.test(p,n)){component.add(n);queue.add(n);}}}
        if (FoundryTankStructureValidation.validate(component).valid()) return Set.copyOf(component);
        var kept = new LinkedHashSet<BlockPos>();kept.add(seed);boolean changed;
        do {changed=false;for(var p:component.stream().sorted(ORDER).toList()) {
            if(kept.contains(p)||kept.stream().noneMatch(n->n.distManhattan(p)==1&&connected.test(n,p)))continue;
            var proposal=new HashSet<>(kept);proposal.add(p);
            if(FoundryTankStructureValidation.validate(proposal).valid()){kept.add(p);changed=true;}
        }}while(changed);
        return Set.copyOf(kept);
    }
}
