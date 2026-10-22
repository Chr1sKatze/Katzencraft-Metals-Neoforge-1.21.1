package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid="katzencraftmetals")
public final class FoundryDetachedSync {
    private FoundryDetachedSync() {}
    public static Map<UUID,FoundryVesselSnapshot> inChunk(FoundryDetachedVessels data,long chunk) {
        var result=new LinkedHashMap<UUID,FoundryVesselSnapshot>();
        data.snapshots().forEach((id,s)->{if(s.positions().stream().anyMatch(p->ChunkPos.asLong(p.getX()>>4,p.getZ()>>4)==chunk))result.put(id,s);});return Map.copyOf(result);
    }
    private static FoundryDetachedPayload payload(ServerLevel level,FoundryDetachedVessels data,long chunk) {
        return new FoundryDetachedPayload(level.dimension().location(),chunk,data.visualRevision(),false,inChunk(data,chunk));
    }
    @SubscribeEvent public static void sent(ChunkWatchEvent.Sent event) {
        var level=event.getLevel();var data=FoundryDetachedVessels.get(level);
        PacketDistributor.sendToPlayer(event.getPlayer(),payload(level,data,event.getPos().toLong()));
    }
    @SubscribeEvent public static void unwatch(ChunkWatchEvent.UnWatch event) {
        var level=event.getLevel();var data=FoundryDetachedVessels.get(level);
        PacketDistributor.sendToPlayer(event.getPlayer(),new FoundryDetachedPayload(level.dimension().location(),event.getPos().toLong(),data.visualRevision(),true,Map.of()));
    }
    /** Block state + complete active BE data arrive before the detached visual is retired.
     * sendBlockUpdated alone is batched by chunk ticks and can otherwise leave a visible gap. */
    public static void handoff(ServerLevel level,FoundryControllerBlockEntity controller,Set<BlockPos> positions) {
        if(level.players().isEmpty())return;
        var chunks=new HashSet<Long>();for(var p:positions)chunks.add(ChunkPos.asLong(p.getX()>>4,p.getZ()>>4));
        var players=new HashSet<net.minecraft.server.level.ServerPlayer>();
        var watchers=new HashMap<Long,Set<net.minecraft.server.level.ServerPlayer>>();
        for(long chunk:chunks){var tracking=Set.copyOf(level.getChunkSource().chunkMap.getPlayers(new ChunkPos(chunk),false));watchers.put(chunk,tracking);players.addAll(tracking);}
        var state=new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level,controller.getBlockPos());
        var blockEntity=controller.getUpdatePacket();var data=FoundryDetachedVessels.get(level);
        for(var player:players) {
            player.connection.send(state);if(blockEntity!=null)player.connection.send(blockEntity);
            for(long chunk:chunks)if(watchers.get(chunk).contains(player))PacketDistributor.sendToPlayer(player,payload(level,data,chunk));
        }
    }
    public static void changed(ServerLevel level,FoundryDetachedVessels data,Set<BlockPos> affected) {
        if(level.players().isEmpty())return;
        var chunks=new HashSet<Long>();for(var p:affected)chunks.add(ChunkPos.asLong(p.getX()>>4,p.getZ()>>4));
        for(long chunk:chunks)PacketDistributor.sendToPlayersTrackingChunk(level,new ChunkPos(chunk),payload(level,data,chunk));
    }
}
