package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Positional looping Foundry furnace sound.
 *
 * The loop fades in after the startup sound and fades out naturally whenever
 * the Controller stops actively melting. There is deliberately no stop sound.
 */
final class FoundryControllerLoopSoundInstance
        extends AbstractTickableSoundInstance {

    private static final float MAX_VOLUME =
            0.55F;

    private static final float FADE_IN_PER_TICK =
            0.050F;

    /*
     * 0.55 / 0.018 ~= 31 ticks, or roughly 1.55 seconds.
     */
    private static final float FADE_OUT_PER_TICK =
            0.018F;

    private final ClientLevel level;
    private final BlockPos pos;

    FoundryControllerLoopSoundInstance(
            ClientLevel level,
            BlockPos pos
    ) {
        super(
                ModSounds.FOUNDRY_SMELTING_LOOP.get(),
                SoundSource.BLOCKS,
                SoundInstance.createUnseededRandom()
        );

        this.level = level;
        this.pos = pos.immutable();

        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;

        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.looping = true;
        this.delay = 0;
        this.attenuation =
                SoundInstance.Attenuation.LINEAR;
        this.relative = false;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return isStillMelting();
    }

    @Override
    public void tick() {
        if (isStillMelting()) {
            volume =
                    Math.min(
                            MAX_VOLUME,
                            volume + FADE_IN_PER_TICK
                    );
            return;
        }

        volume =
                Math.max(
                        0.0F,
                        volume - FADE_OUT_PER_TICK
                );

        if (volume <= 0.001F) {
            stop();
        }
    }

    private boolean isStillMelting() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (
                minecraft.level != level
                        || !level.hasChunkAt(pos)
        ) {
            return false;
        }

        BlockState state =
                level.getBlockState(
                        pos
                );

        return state.is(
                ModBlocks.FOUNDRY_CONTROLLER.get()
        )
                && state.hasProperty(
                FoundryControllerBlock.LIT
        )
                && state.getValue(
                FoundryControllerBlock.LIT
        );
    }
}
