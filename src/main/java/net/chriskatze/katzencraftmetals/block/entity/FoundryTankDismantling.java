package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Server-only edit transaction. Read old storage before any Controller cache is resolved. */
public final class FoundryTankDismantling {
    private FoundryTankDismantling() {}
    private static final ThreadLocal<Set<Level>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    public static void removed(Level level,BlockPos removed,BlockState oldState) {
        if(!(level instanceof ServerLevel server)||!ACTIVE.get().add(level))return;
        try {
            FoundryControllerBlockEntity owner=null;
            for(var c:nearby(server,removed))if(c.getTankStorage().getBoundTankPositions().contains(removed)){owner=c;break;}
            var data=FoundryDetachedVessels.get(server);
            UUID orphanId=null;FoundryVesselSnapshot snapshot=null;
            if(owner!=null)snapshot=owner.getTankStorage().snapshot();
            else for(var entry:data.snapshots().entrySet())if(entry.getValue().positions().contains(removed)){orphanId=entry.getKey();snapshot=entry.getValue();break;}
            if(snapshot==null) {
                var positions=logicalComponent(server,removed,oldState);
                snapshot=new FoundryVesselSnapshot(positions.stream().min(FoundryVesselSnapshot.ORDER).orElse(removed),positions,Map.of());
            }
            var available=new HashSet<>(snapshot.positions());available.remove(removed);
            available.removeIf(p->!(server.getBlockState(p).getBlock() instanceof FoundryTankBlock));
            List<BlockPos> preferred=owner==null?List.of():owner.getValidTankAttachmentPositions();
            var survivors=FoundryVesselSnapshot.survivors(available,preferred,snapshot.anchor(),(a,b)->connected(server,a,b));
            var retained=snapshot.retain(survivors);
            // Commit contents first. Recursive callbacks see the guard and cannot charge/drop twice.
            if(owner!=null)owner.getTankStorage().replace(retained);
            else data.put(orphanId==null?UUID.randomUUID():orphanId,retained);
            for(var p:snapshot.positions().stream().sorted(FoundryVesselSnapshot.ORDER).toList()) {
                if(!p.equals(removed)&&!survivors.contains(p)&&server.getBlockState(p).getBlock() instanceof FoundryTankBlock)
                    server.destroyBlock(p,true); // Vanilla dropSelf + block removal; no manual duplicate item spawn.
            }
            var affected=new HashSet<FoundryControllerBlockEntity>();
            for(var p:snapshot.positions())affected.addAll(nearby(server,p));
            for(var c:affected)c.markTankStructureDirty();
            for(var c:affected){c.ensureTankNetwork();c.syncToClient();}
        }finally{ACTIVE.get().remove(level);if(ACTIVE.get().isEmpty())ACTIVE.remove();}
    }
    private static List<FoundryControllerBlockEntity> nearby(ServerLevel level,BlockPos at) {
        var result=new ArrayList<FoundryControllerBlockEntity>();
        for(var p:BlockPos.betweenClosed(at.offset(-5,-4,-5),at.offset(5,4,5)))
            if(level.getBlockEntity(p) instanceof FoundryControllerBlockEntity c)result.add(c);
        result.sort(Comparator.comparing(FoundryControllerBlockEntity::getBlockPos,FoundryVesselSnapshot.ORDER));return result;
    }
    static boolean connected(Level level,BlockPos a,BlockPos b) {
        Direction direction=Direction.fromDelta(b.getX()-a.getX(),b.getY()-a.getY(),b.getZ()-a.getZ());
        if(direction==null)return false;
        var first=level.getBlockState(a);var second=level.getBlockState(b);
        return first.getBlock() instanceof FoundryTankBlock && second.getBlock() instanceof FoundryTankBlock
                && first.getValue(FoundryTankBlock.connectionProperty(direction))
                && second.getValue(FoundryTankBlock.connectionProperty(direction.getOpposite()));
    }
    private static Set<BlockPos> logicalComponent(ServerLevel level,BlockPos removed,BlockState old) {
        var result=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();result.add(removed.immutable());queue.add(removed.immutable());
        while(!queue.isEmpty()&&result.size()<=FoundryTankNetwork.MAX_TANK_COUNT) {
            var p=queue.remove();var state=p.equals(removed)?old:level.getBlockState(p);
            for(var d:Direction.values()) {
                var n=p.relative(d);var next=n.equals(removed)?old:level.getBlockState(n);
                if(result.contains(n)||!(next.getBlock() instanceof FoundryTankBlock))continue;
                if(state.getValue(FoundryTankBlock.connectionProperty(d))&&next.getValue(FoundryTankBlock.connectionProperty(d.getOpposite()))){result.add(n);queue.add(n);}
            }
        }return Set.copyOf(result);
    }
}
