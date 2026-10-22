package net.chriskatze.katzencraftmetals.network;

import net.chriskatze.katzencraftmetals.block.entity.FoundryVesselSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Server-to-client full replacement of the detached visual views intersecting one watched chunk. */
public record FoundryDetachedPayload(ResourceLocation dimension, long chunk, long revision, boolean unwatch,
                                     Map<UUID,FoundryVesselSnapshot> vessels) implements CustomPacketPayload {
    public FoundryDetachedPayload { vessels=Map.copyOf(vessels); }
    public static final Type<FoundryDetachedPayload> TYPE=new Type<>(ResourceLocation.parse("katzencraftmetals:detached_foundry"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FoundryDetachedPayload> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeResourceLocation(p.dimension);b.writeLong(p.chunk);b.writeVarLong(p.revision);b.writeBoolean(p.unwatch);b.writeVarInt(p.vessels.size());
        p.vessels.forEach((id,s)->{
            b.writeUUID(id);b.writeBlockPos(s.anchor());b.writeBlockPos(s.renderOrigin());b.writeVarInt(s.positions().size());
            s.positions().stream().sorted(FoundryVesselSnapshot.ORDER).forEach(b::writeBlockPos);
            b.writeVarInt(s.contents().size());s.contents().forEach((metal,amount)->{b.writeResourceLocation(metal);b.writeVarInt(amount);});
        });
    },b->{
        var dim=b.readResourceLocation();long chunk=b.readLong(),revision=b.readVarLong();boolean unwatch=b.readBoolean();
        int count=size(b,65536);var views=new LinkedHashMap<UUID,FoundryVesselSnapshot>();
        for(int i=0;i<count;i++) {
            var id=b.readUUID();var anchor=b.readBlockPos();var origin=b.readBlockPos();int cells=size(b,64);var positions=new HashSet<BlockPos>();
            for(int n=0;n<cells;n++)if(!positions.add(b.readBlockPos()))throw new IllegalArgumentException("Duplicate visual tank position");
            int metals=size(b,4096);var contents=new LinkedHashMap<ResourceLocation,Integer>();
            for(int n=0;n<metals;n++)if(contents.putIfAbsent(b.readResourceLocation(),b.readVarInt())!=null)throw new IllegalArgumentException("Duplicate visual metal");
            if(views.putIfAbsent(id,new FoundryVesselSnapshot(anchor,positions,contents,origin))!=null)throw new IllegalArgumentException("Duplicate visual vessel");
        }return new FoundryDetachedPayload(dim,chunk,revision,unwatch,views);
    });
    private static int size(RegistryFriendlyByteBuf b,int max){int count=b.readVarInt();if(count<0||count>max)throw new IllegalArgumentException("Invalid detached visual payload size");return count;}
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
