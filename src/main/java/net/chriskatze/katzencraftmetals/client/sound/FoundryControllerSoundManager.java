package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects actual Foundry melting transitions from the synced Controller LIT
 * blockstate and owns the client-side positional sound sequence.
 *
 * No extra network packet is needed: LIT already means "actually melting".
 */
@EventBusSubscriber(
        modid = KatzencraftMetalsMod.MODID,
        value = Dist.CLIENT
)
public final class FoundryControllerSoundManager {

    /*
     * Two chunks in every direction is comfortably beyond the normal audible
     * distance of these mono block sounds while avoiding a full-world scan.
     */
    private static final int CHUNK_SCAN_RADIUS =
            2;

    /*
     * Startup is ~3.2 s (64 ticks). Start the loop a little early and let its
     * own fade-in overlap the end of startup.
     */
    private static final int LOOP_DELAY_TICKS =
            52;

    private static final Map<BlockPos, Boolean>
            LAST_MELTING_STATE =
            new HashMap<>();

    private static final Map<
            BlockPos,
            FoundryControllerLoopSoundInstance
            >
            ACTIVE_LOOPS =
            new HashMap<>();

    private static ClientLevel trackedLevel;

    private FoundryControllerSoundManager() {
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        ClientLevel level =
                minecraft.level;

        if (
                level == null
                        || minecraft.player == null
        ) {
            clearForNoLevel();
            return;
        }

        if (trackedLevel != level) {
            LAST_MELTING_STATE.clear();
            ACTIVE_LOOPS.clear();
            trackedLevel = level;
        }

        ACTIVE_LOOPS.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        .isStopped()
                );

        BlockPos playerPos =
                minecraft.player.blockPosition();

        int playerChunkX =
                playerPos.getX() >> 4;

        int playerChunkZ =
                playerPos.getZ() >> 4;

        for (
                int chunkX =
                        playerChunkX - CHUNK_SCAN_RADIUS;
                chunkX <=
                        playerChunkX + CHUNK_SCAN_RADIUS;
                chunkX++
        ) {
            for (
                    int chunkZ =
                            playerChunkZ - CHUNK_SCAN_RADIUS;
                    chunkZ <=
                            playerChunkZ + CHUNK_SCAN_RADIUS;
                    chunkZ++
            ) {
                LevelChunk chunk =
                        level.getChunkSource()
                                .getChunk(
                                        chunkX,
                                        chunkZ,
                                        ChunkStatus.FULL,
                                        false
                                );

                if (chunk == null) {
                    continue;
                }

                for (
                        BlockEntity blockEntity
                        : chunk.getBlockEntities()
                                .values()
                ) {
                    if (
                            blockEntity
                                    instanceof FoundryControllerBlockEntity
                    ) {
                        inspectController(
                                minecraft,
                                level,
                                blockEntity.getBlockPos()
                        );
                    }
                }
            }
        }
    }

    private static void inspectController(
            Minecraft minecraft,
            ClientLevel level,
            BlockPos pos
    ) {
        BlockPos key =
                pos.immutable();

        BlockState state =
                level.getBlockState(
                        key
                );

        boolean melting =
                state.hasProperty(
                        FoundryControllerBlock.LIT
                )
                        && state.getValue(
                        FoundryControllerBlock.LIT
                );

        Boolean previous =
                LAST_MELTING_STATE.put(
                        key,
                        melting
                );

        if (!melting) {
            return;
        }

        FoundryControllerLoopSoundInstance
                existingLoop =
                ACTIVE_LOOPS.get(
                        key
                );

        /*
         * The Controller was already melting when it first entered our scan
         * range (for example when the player walks toward it or joins the
         * world). In that case we should hear the established furnace loop,
         * not pretend a fresh ignition just happened.
         */
        if (previous == null) {
            if (
                    existingLoop == null
                            || existingLoop.isStopped()
            ) {
                startLoopNow(
                        minecraft,
                        level,
                        key
                );
            }

            return;
        }

        /*
         * Real false -> true transition: actual melting has just started.
         */
        if (!previous) {
            startMeltingSequence(
                    minecraft,
                    level,
                    key
            );
            return;
        }

        /*
         * Stable active furnace. This also recovers the loop after a chunk was
         * unloaded/reloaded while the same Controller remained LIT.
         */
        if (
                existingLoop == null
                        || existingLoop.isStopped()
        ) {
            startLoopNow(
                    minecraft,
                    level,
                    key
            );
        }
    }

    private static void startMeltingSequence(
            Minecraft minecraft,
            ClientLevel level,
            BlockPos pos
    ) {
        SoundManager soundManager =
                minecraft.getSoundManager();

        soundManager.play(
                new FoundryControllerStartupSoundInstance(
                        level,
                        pos
                )
        );

        FoundryControllerLoopSoundInstance
                existingLoop =
                ACTIVE_LOOPS.get(
                        pos
                );

        /*
         * If the furnace only paused for less than its fade-out time, keep the
         * existing loop alive. Its own tick() will smoothly rise again while
         * the startup sound plays.
         */
        if (
                existingLoop != null
                        && !existingLoop.isStopped()
        ) {
            return;
        }

        FoundryControllerLoopSoundInstance loop =
                new FoundryControllerLoopSoundInstance(
                        level,
                        pos
                );

        ACTIVE_LOOPS.put(
                pos,
                loop
        );

        soundManager.playDelayed(
                loop,
                LOOP_DELAY_TICKS
        );
    }

    private static void startLoopNow(
            Minecraft minecraft,
            ClientLevel level,
            BlockPos pos
    ) {
        FoundryControllerLoopSoundInstance loop =
                new FoundryControllerLoopSoundInstance(
                        level,
                        pos
                );

        ACTIVE_LOOPS.put(
                pos,
                loop
        );

        minecraft.getSoundManager()
                .play(
                        loop
                );
    }

    private static void clearForNoLevel() {
        if (trackedLevel == null) {
            return;
        }

        LAST_MELTING_STATE.clear();
        ACTIVE_LOOPS.clear();
        trackedLevel = null;
    }
}
