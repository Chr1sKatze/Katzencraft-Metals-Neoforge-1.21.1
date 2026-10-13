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
 * One-shot Foundry ignition sound.
 *
 * If melting stops while the startup sound is still playing, it fades away
 * rather than finishing at full volume.
 */
final class FoundryControllerStartupSoundInstance
        extends AbstractTickableSoundInstance {

    private static final float START_VOLUME =
            0.65F;

    private static final float FADE_OUT_PER_TICK =
            0.055F;

    /*
     * The current startup asset is about 3.2 seconds long.
     * Give it a few extra ticks before explicitly retiring the instance.
     */
    private static final int MAX_AGE_TICKS =
            70;

    private final ClientLevel level;
    private final BlockPos pos;

    private int ageTicks;

    FoundryControllerStartupSoundInstance(
            ClientLevel level,
            BlockPos pos
    ) {
        super(
                ModSounds.FOUNDRY_SMELTING_START.get(),
                SoundSource.BLOCKS,
                SoundInstance.createUnseededRandom()
        );

        this.level = level;
        this.pos = pos.immutable();

        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;

        this.volume = START_VOLUME;
        this.pitch = 1.0F;
        this.looping = false;
        this.delay = 0;
        this.attenuation =
                SoundInstance.Attenuation.LINEAR;
        this.relative = false;
    }

    @Override
    public void tick() {
        ageTicks++;

        if (!isStillMelting()) {
            volume =
                    Math.max(
                            0.0F,
                            volume - FADE_OUT_PER_TICK
                    );

            if (volume <= 0.001F) {
                stop();
                return;
            }
        }

        if (ageTicks >= MAX_AGE_TICKS) {
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
