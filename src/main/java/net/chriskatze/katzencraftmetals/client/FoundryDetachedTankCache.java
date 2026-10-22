package net.chriskatze.katzencraftmetals.client;

import net.chriskatze.katzencraftmetals.block.entity.FoundryVesselSnapshot;
import net.chriskatze.katzencraftmetals.network.FoundryDetachedPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Read-only visual replica. Full vessel layouts are retained even when only one chunk is visible. */
public final class FoundryDetachedTankCache {
    private record Key(ResourceLocation dimension,long chunk) {}
    private record ChunkView(long revision,Map<UUID,FoundryVesselSnapshot> vessels) {}
    private final Map<Key,ChunkView> chunks=new HashMap<>();
    private final Map<ResourceLocation,Map<UUID,FoundryVesselSnapshot>> resolved=new HashMap<>();
    public static final FoundryDetachedTankCache INSTANCE=new FoundryDetachedTankCache();
    public static void accept(FoundryDetachedPayload payload){INSTANCE.apply(payload);}
    public void apply(FoundryDetachedPayload payload) {
        var key=new Key(payload.dimension(),payload.chunk());var old=chunks.get(key);
        if(old!=null&&old.revision>payload.revision())return;
        if(payload.unwatch())chunks.remove(key);else chunks.put(key,new ChunkView(payload.revision(),payload.vessels()));
        resolved.remove(payload.dimension());
    }
    public Map<UUID,FoundryVesselSnapshot> views(ResourceLocation dimension) {
        return resolved.computeIfAbsent(dimension,this::resolve);
    }
    private Map<UUID,FoundryVesselSnapshot> resolve(ResourceLocation dimension) {
        Map<UUID,FoundryVesselSnapshot> result=new HashMap<>();Map<UUID,Long> versions=new HashMap<>();
        chunks.forEach((key,chunk)->{if(key.dimension.equals(dimension))chunk.vessels.forEach((id,s)->{
            if(chunk.revision>=versions.getOrDefault(id,-1L)){versions.put(id,chunk.revision);result.put(id,s);}
        });});return Map.copyOf(result);
    }
    public void clear(){chunks.clear();resolved.clear();}
}
