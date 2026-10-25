package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Dimension-local durable escrow only while a vessel has no Controller. A claim removes the escrow atomically. */
public final class FoundryDetachedVessels extends SavedData {
    private ServerLevel observingLevel;
    private long visualRevision;
    public long visualRevision() { return visualRevision; }
    private void changed(Set<BlockPos> affected) { changed(affected,true); }
    private void changed(Set<BlockPos> affected, boolean notifyClients) {
        setDirty(); visualRevision++;
        // Silent claims hand over directly to active storage; do not briefly turn its lights off.
        if (notifyClients && observingLevel != null) FoundryMoltenLighting.update(observingLevel, affected, null);
        if(notifyClients && observingLevel != null)net.chriskatze.katzencraftmetals.network.FoundryDetachedSync.changed(observingLevel,this,affected);
    }
    private final Map<UUID,FoundryVesselSnapshot> vessels = new LinkedHashMap<>();
    public static FoundryDetachedVessels get(ServerLevel level) {
        var data=level.getDataStorage().computeIfAbsent(new Factory<>(FoundryDetachedVessels::new,FoundryDetachedVessels::load),"katzencraftmetals_foundry_vessels");
        data.observingLevel=level;return data;
    }
    public Map<UUID,FoundryVesselSnapshot> snapshots() {return Map.copyOf(vessels);}
    public void put(UUID id,FoundryVesselSnapshot snapshot) {
        if (!snapshot.positions().isEmpty() && vessels.entrySet().stream().anyMatch(e -> !e.getKey().equals(id)
                && !Collections.disjoint(e.getValue().positions(), snapshot.positions())))
            throw new IllegalStateException("Overlapping Foundry escrows");
        var affected=new HashSet<>(snapshot.positions());
        var previous=vessels.get(id);if(previous!=null)affected.addAll(previous.positions());
        if(snapshot.positions().isEmpty())vessels.remove(id);else vessels.put(id,snapshot);changed(affected);
    }
    /** Expand complete escrows atomically. No capacity trimming or new inventory is created. */
    boolean expand(Set<BlockPos> positions) {
        if (!FoundryTankStructureValidation.validate(positions).valid()) return false;
        var matches=vessels.entrySet().stream()
                .filter(e->!Collections.disjoint(e.getValue().positions(),positions)).toList();
        if(matches.isEmpty() || matches.stream().anyMatch(e->!positions.containsAll(e.getValue().positions()))) return false;
        if(matches.size()==1 && matches.getFirst().getValue().positions().equals(positions)) return false;
        var amounts=new LinkedHashMap<net.minecraft.resources.ResourceLocation,Integer>();
        for(var entry:matches)entry.getValue().contents().forEach((id,n)->amounts.merge(id,n,Math::addExact));
        var first=matches.getFirst();var previous=first.getValue();
        var expanded=new FoundryVesselSnapshot(previous.anchor(),positions,amounts,previous.renderOrigin());
        matches.forEach(e->vessels.remove(e.getKey()));
        vessels.put(first.getKey(),expanded);
        // positions includes all old cells: notify watchers of old AND newly occupied chunks once.
        changed(positions);
        return true;
    }
    public Optional<FoundryVesselSnapshot> claim(Set<BlockPos> positions) { return claim(positions,true); }
    Optional<FoundryVesselSnapshot> claim(Set<BlockPos> positions, boolean destinationEmpty) { return claim(positions,destinationEmpty,true); }
    Optional<FoundryVesselSnapshot> claim(Set<BlockPos> positions, boolean destinationEmpty, boolean notifyClients) {
        var matches=vessels.entrySet().stream().filter(e->!Collections.disjoint(e.getValue().positions(),positions)).toList();
        if(matches.isEmpty())return Optional.empty();
        if(!destinationEmpty)throw new IllegalStateException("Cannot overwrite active Foundry contents with escrow");
        // A layout may only claim complete escrows, never copy a partial saved vessel.
        if(matches.stream().anyMatch(e->!positions.containsAll(e.getValue().positions())))throw new IllegalStateException("Partial Foundry escrow claim");
        var amounts=new LinkedHashMap<net.minecraft.resources.ResourceLocation,Integer>();
        var seen=new HashSet<BlockPos>();
        for(var entry:matches) {
            if(!Collections.disjoint(seen,entry.getValue().positions()))throw new IllegalStateException("Overlapping Foundry escrows");
            seen.addAll(entry.getValue().positions());entry.getValue().contents().forEach((id,n)->amounts.merge(id,n,Integer::sum));
        }
        var anchor=matches.getFirst().getValue().anchor();
        var result=new FoundryVesselSnapshot(anchor,positions,amounts);
        var affected=new HashSet<BlockPos>();matches.forEach(e->affected.addAll(e.getValue().positions()));
        matches.forEach(e->vessels.remove(e.getKey()));changed(affected,notifyClients);return Optional.of(result);
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        var list=new ListTag();vessels.forEach((id,snapshot)->{
            var entry=new CompoundTag();entry.putUUID("Id",id);entry.putLong("Anchor",snapshot.anchor().asLong());entry.putLong("RenderOrigin",snapshot.renderOrigin().asLong());
            entry.putLongArray("Positions",snapshot.positions().stream().sorted(FoundryVesselSnapshot.ORDER).mapToLong(BlockPos::asLong).toArray());
            entry.put("Contents",FoundryAlloyAmounts.writeAmounts(snapshot.contents()));list.add(entry);
        });tag.put("Vessels",list);return tag;
    }
    public static FoundryDetachedVessels load(CompoundTag tag,HolderLookup.Provider lookup) {
        var result=new FoundryDetachedVessels();
        for(var value:tag.getList("Vessels",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)value;var positions=new HashSet<BlockPos>();
            for(long p:entry.getLongArray("Positions"))positions.add(BlockPos.of(p));
            var contents=new LinkedHashMap<net.minecraft.resources.ResourceLocation,Integer>();FoundryAlloyAmounts.readAmounts(entry.getCompound("Contents"),contents);
            var snapshot=new FoundryVesselSnapshot(BlockPos.of(entry.getLong("Anchor")),positions,contents,BlockPos.of(entry.getLong(entry.contains("RenderOrigin")?"RenderOrigin":"Anchor")));
            if(result.vessels.putIfAbsent(entry.getUUID("Id"),snapshot)!=null)throw new IllegalStateException("Duplicate Foundry vessel ID");
        }return result;
    }
}
