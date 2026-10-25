package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;

/** Discovers audible loaded Faucets, including pours already active when joining.
 * No server sound broadcast, new packets, renderer dependency or forced chunk loads. */
@EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT)
public final class FoundryFaucetSoundManager {
    private static final Map<BlockPos, FoundryFaucetLoopSoundInstance> LOOPS = new HashMap<>();
    private static ClientLevel trackedLevel;

    private FoundryFaucetSoundManager() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != trackedLevel || minecraft.player == null) clear();
        trackedLevel = level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) return;
        // The engine drops muted channels without ticking their instances. Do not
        // retain a dead loop or repeatedly submit sounds while a volume slider is zero.
        if (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS) <= 0) {
            clear();
            return;
        }
        LOOPS.values().removeIf(FoundryFaucetLoopSoundInstance::isStopped);
        BlockPos player = minecraft.player.blockPosition();
        // Normal attenuation is 16 blocks: the player's chunk and its immediate neighbors suffice.
        for (int x = (player.getX() >> 4) - 1; x <= (player.getX() >> 4) + 1; x++) {
            for (int z = (player.getZ() >> 4) - 1; z <= (player.getZ() >> 4) + 1; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof FoundryFaucetBlockEntity faucet) || LOOPS.containsKey(entity.getBlockPos())) continue;
                    if (!flowing(level, faucet) || !entity.getBlockPos().closerToCenterThan(minecraft.player.position(), 16)) continue;
                    var loop = new FoundryFaucetLoopSoundInstance(entity.getBlockPos(),
                            () -> minecraft.level == level && flowing(level, faucet));
                    LOOPS.put(entity.getBlockPos().immutable(), loop);
                    minecraft.getSoundManager().play(loop);
                }
            }
        }
    }

    static boolean flowing(Level level, FoundryFaucetBlockEntity faucet) {
        BlockPos pos = faucet.getBlockPos();
        if (faucet.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != faucet
                || faucet.getLevel() != level) return false;
        var state = faucet.getBlockState();
        if (!state.hasProperty(FoundryFaucetBlock.FACING)) return false;
        var tank = pos.relative(state.getValue(FoundryFaucetBlock.FACING).getOpposite());
        if (!level.hasChunkAt(tank) || !level.getBlockState(tank).is(ModBlocks.FOUNDRY_TANK.get())) return false;
        var target = FoundryFaucetBlockEntity.findBasinTarget(level, pos);
        return target != null && !target.basin().isRemoved()
                && net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetStreamAnimation.hasVisibleStream(faucet, target.basin());
    }

    private static void clear() {
        net.chriskatze.katzencraftmetals.client.renderer.FoundryFaucetStreamAnimation.clearSoundStates();
        if (!LOOPS.isEmpty()) {
            var sounds = Minecraft.getInstance().getSoundManager();
            LOOPS.values().forEach(loop -> { loop.cancel(); sounds.stop(loop); });
        }
        LOOPS.clear();
        trackedLevel = null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) clear(); }

    @EventBusSubscriber(modid = KatzencraftMetalsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        private Reload() {}
        @SubscribeEvent public static void loaded(SoundEngineLoadEvent event) {
            Minecraft.getInstance().execute(FoundryFaucetSoundManager::clear);
        }
    }
}
