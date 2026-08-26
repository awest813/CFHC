package desktop;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Converts compressed audio streams (bundled OGGs via the vorbisspi SPI) to
 * 16-bit PCM before playback.
 *
 * <p>{@code AudioSystem.getAudioInputStream(...)} on an OGG returns a stream
 * still in {@code VORBISENC} format. Handing that stream straight to
 * {@code Clip.open()} plays the raw compressed bytes as if they were PCM —
 * harsh static — or throws and silently falls back. Every bundled audio file
 * must pass through {@link #toPcm} first; WAV files pass through unchanged.
 */
final class AudioDecoding {

    private AudioDecoding() {
    }

    /**
     * Returns a PCM view of {@code in}. Streams that are already PCM are
     * returned as-is; anything else (VORBISENC) is converted to signed
     * 16-bit little-endian PCM at the source sample rate and channel count.
     */
    static AudioInputStream toPcm(AudioInputStream in) {
        AudioFormat fmt = in.getFormat();
        AudioFormat.Encoding enc = fmt.getEncoding();
        if (AudioFormat.Encoding.PCM_SIGNED.equals(enc)
                || AudioFormat.Encoding.PCM_UNSIGNED.equals(enc)
                || AudioFormat.Encoding.PCM_FLOAT.equals(enc)) {
            return in;
        }
        AudioFormat target = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                fmt.getSampleRate(),
                16,
                fmt.getChannels(),
                fmt.getChannels() * 2,
                fmt.getSampleRate(),
                false);
        return AudioSystem.getAudioInputStream(target, in);
    }
}
