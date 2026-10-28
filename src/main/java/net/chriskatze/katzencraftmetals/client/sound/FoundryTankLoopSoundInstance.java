package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;

/** One native streamed loop per molten vessel, independent of its Controller identity. */
final class FoundryTankLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME = 0.50F;
    private boolean molten = true;
    private Vec3 position;

    FoundryTankLoopSoundInstance(Vec3 position) {
        super(ModSounds.FOUNDRY_TANK_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        moveTo(position);
        volume = VOLUME;
        pitch = 1.0F;
        looping = true;
        delay = 0;
        relative = false;
        attenuation = SoundInstance.Attenuation.LINEAR;
    }

    Vec3 position() { return position; }
    void setMolten(boolean molten) { this.molten = molten; }
    void moveTo(Vec3 position) {
        this.position = position;
        x = position.x;
        y = position.y;
        z = position.z;
    }
    void cancel() { stop(); }
    @Override public boolean canStartSilent() { return true; }
    @Override public void tick() {
        volume = molten ? VOLUME : Math.max(0, volume - VOLUME / 4);
        if (volume <= 0.001F) stop();
    }
}
