package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.client.FoundryDetachedTankCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.ArrayList;

/** Uses existing Controller updates and detached chunk replicas; no new synchronization. */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
public final class FoundryTankSoundManager {
    private static final FoundryTankSoundTracker TRACKER = new FoundryTankSoundTracker();
    private static ClientLevel trackedLevel;

    private FoundryTankSoundManager() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel || minecraft.player == null) clear();
        trackedLevel = level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) return;
        if (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS) <= 0) { clear(); return; }
        var sources = new ArrayList<FoundryTankSoundTracker.Source>();
        BlockPos player = minecraft.player.blockPosition();
        // Include Controllers just beyond the audible tank's chunk. Read loaded chunks only.
        for (int x = (player.getX() >> 4) - 2; x <= (player.getX() >> 4) + 2; x++) {
            for (int z = (player.getZ() >> 4) - 2; z <= (player.getZ() >> 4) + 2; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof FoundryControllerBlockEntity controller) || controller.isRemoved()) continue;
                    var network = controller.getOwnedTankNetwork();
                    if (network != null) sources.add(new FoundryTankSoundTracker.Source(network.getTankPositions(),
                            !network.getMoltenContents().isEmpty()));
                }
            }
        }
        FoundryDetachedTankCache.INSTANCE.views(level.dimension().location()).values().forEach(snapshot ->
                sources.add(new FoundryTankSoundTracker.Source(snapshot.positions(), !snapshot.contents().isEmpty())));
        // Only create audible sources, while keeping empty entries to suppress stale detached views.
        sources.removeIf(source -> source.cells().stream().noneMatch(p -> p.closerToCenterThan(minecraft.player.position(), 16)));
        var sounds = minecraft.getSoundManager();
        TRACKER.update(sources, p -> level.hasChunkAt(p) && level.getBlockState(p).is(ModBlocks.FOUNDRY_TANK.get()),
                sounds::play, sounds::stop);
    }

    private static void clear() {
        TRACKER.clear(sound -> Minecraft.getInstance().getSoundManager().stop(sound));
        trackedLevel = null;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) clear(); }

    @EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        private Reload() {}
        @SubscribeEvent public static void loaded(SoundEngineLoadEvent event) {
            Minecraft.getInstance().execute(FoundryTankSoundManager::clear);
        }
    }
}
