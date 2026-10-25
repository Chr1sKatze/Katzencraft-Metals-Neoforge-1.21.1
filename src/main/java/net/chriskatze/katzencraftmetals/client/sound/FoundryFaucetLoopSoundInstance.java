package net.chriskatze.katzencraftmetals.client.sound;

import net.chriskatze.katzencraftmetals.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/** One native positional loop, driven only by the synchronized Faucet state. */
final class FoundryFaucetLoopSoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME = 0.85F;
    private static final int FADE_OUT_TICKS = 2;
    private final BooleanSupplier flowing;

    FoundryFaucetLoopSoundInstance(BlockPos pos, BooleanSupplier flowing) {
        super(ModSounds.FOUNDRY_POURING_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.flowing = flowing;
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        volume = VOLUME;
        pitch = 1.0F; // The supplied recording is already accelerated.
        looping = true;
        delay = 0;
        relative = false;
        attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override public boolean canStartSilent() { return true; }

    @Override public void tick() {
        // Drain timing comes from the stream animation; fade only after it disappears.
        volume = flowing.getAsBoolean() ? VOLUME : Math.max(0, volume - VOLUME / FADE_OUT_TICKS);
        if (volume <= 0.001F) stop();
    }

    void cancel() { stop(); }

    @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return buffers.getStream(sound.getPath(), looping).thenApply(stream ->
                stream.getFormat().getChannels() == 2 ? new MonoStream(stream) : stream);
    }

    /** OpenAL does not spatialize stereo sources. Preserve the authored OGG and
     * downmix its decoded PCM once per streamed buffer, without changing timing. */
    static final class MonoStream implements AudioStream {
        private final AudioStream source;
        private final AudioFormat format;
        private final ByteOrder order;

        MonoStream(AudioStream source) {
            this.source = source;
            AudioFormat original = source.getFormat();
            if (original.getChannels() != 2 || original.getSampleSizeInBits() != 16
                    || !original.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED))
                throw new IllegalArgumentException("Expected stereo 16-bit Vorbis PCM");
            order = original.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            format = new AudioFormat(original.getSampleRate(), 16, 1, true, original.isBigEndian());
        }

        @Override public AudioFormat getFormat() { return format; }

        @Override public ByteBuffer read(int size) throws IOException {
            ByteBuffer stereo = source.read(Math.max(2, size & ~1) * 2).order(order);
            ByteBuffer mono = ByteBuffer.allocateDirect(stereo.remaining() / 4 * 2).order(order);
            while (stereo.remaining() >= 4) mono.putShort((short) ((stereo.getShort() + stereo.getShort()) / 2));
            return mono.flip();
        }

        @Override public void close() throws IOException { source.close(); }
    }
}
