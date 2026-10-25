package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import java.util.*;

/** Native state-based light: no render-side inventories, ticking lights, or client authority. */
@EventBusSubscriber(modid = "katzencraftmetals")
public final class FoundryMoltenLighting {
    private FoundryMoltenLighting() {}

    /** Occupancy matches pooled rendering: spread each horizontal level evenly, bottom first. */
    static Set<BlockPos> occupied(FoundryVesselSnapshot snapshot) {
        long remaining = snapshot.contents().values().stream().mapToLong(Integer::longValue).sum();
        var levels = new TreeMap<Integer, Set<BlockPos>>();
        for (var pos : snapshot.positions()) levels.computeIfAbsent(pos.getY(), y -> new HashSet<>()).add(pos);
        var result = new HashSet<BlockPos>();
        for (var row : levels.values()) {
            if (remaining <= 0) break;
            result.addAll(row);
            remaining -= (long) row.size() * FoundryTankNetwork.TANK_CAPACITY;
        }
        return result;
    }

    /** Reconcile only loaded positions. Level.setBlock queues native lighting and ordinary state sync. */
    static void update(Level level, Set<BlockPos> affected, FoundryVesselSnapshot active) {
        if (!(level instanceof ServerLevel server) || affected.isEmpty()) return;
        var lit = new HashSet<BlockPos>();
        if (active != null) lit.addAll(occupied(active));
        for (var snapshot : FoundryDetachedVessels.get(server).snapshots().values()) {
            if (!Collections.disjoint(affected, snapshot.positions())) lit.addAll(occupied(snapshot));
        }
        apply(level, affected, lit);
    }

    static void apply(Level level, Set<BlockPos> affected, Set<BlockPos> lit) {
        if (level.isClientSide()) return;
        for (var pos : affected) {
            if (!level.hasChunkAt(pos)) continue; // Never load chunks to update a light.
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof FoundryTankBlock && state.getValue(FoundryTankBlock.MOLTEN_LIT) != lit.contains(pos)) {
                // No neighbor notifications: lighting is not a connectivity or inventory change.
                level.setBlock(pos, state.setValue(FoundryTankBlock.MOLTEN_LIT, lit.contains(pos)), Block.UPDATE_CLIENTS);
            }
        }
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // Run after chunk promotion; persistent flags already cover the initial chunk/light packet.
        var server = level.getServer();
        // execute() can run inline on the server thread while chunk loading is unfinished.
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> reconcileChunk(level, chunk)));
    }

    static void reconcileChunk(ServerLevel level, LevelChunk chunk) {
        if (level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z) != chunk) return;
        var positions = new HashSet<BlockPos>();
        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (!section.maybeHas(state -> state.getBlock() instanceof FoundryTankBlock)) continue;
            for (int y=0;y<16;y++) for (int z=0;z<16;z++) for (int x=0;x<16;x++) {
                if (section.getBlockState(x,y,z).getBlock() instanceof FoundryTankBlock)
                    positions.add(new BlockPos(chunk.getPos().getMinBlockX()+x,chunk.getMinBuildHeight()+i*16+y,chunk.getPos().getMinBlockZ()+z));
            }
        }
        if (positions.isEmpty()) return;
        // An active owner may itself be unloaded. Retain persisted flags where no loaded
        // authoritative snapshot is available, rather than darkening such tanks speculatively.
        var lit = new HashSet<BlockPos>();
        for (var pos : positions) if (level.getBlockState(pos).getValue(FoundryTankBlock.MOLTEN_LIT)) lit.add(pos);
        for (var snapshot : FoundryDetachedVessels.get(level).snapshots().values()) {
            lit.removeAll(snapshot.positions()); lit.addAll(occupied(snapshot));
        }
        // A valid vessel fits within four blocks: owners can only be in this or an adjacent chunk.
        // Read already loaded chunks only, and use persisted snapshots without structure discovery.
        for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++) {
            var neighbor=level.getChunkSource().getChunkNow(chunk.getPos().x+dx,chunk.getPos().z+dz);
            if (neighbor == null) continue;
            for (var entity : neighbor.getBlockEntities().values()) {
                if (entity instanceof FoundryControllerBlockEntity controller) {
                    var snapshot=controller.getTankStorage().snapshot();
                    lit.removeAll(snapshot.positions()); lit.addAll(occupied(snapshot));
                }
            }
        }
        apply(level, positions, lit);
    }
}
