package desktop;

import simulation.SoundtrackEngine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * Desktop soundtrack engine. Plays the bundled public-domain march
 * recordings (Sousa/Bagley performed by U.S. military bands) from
 * {@code assets/sounds/soundtrack/<name>.ogg} via vorbisspi, looping on a
 * long-lived {@link Clip}. If a track's OGG is missing or audio lines are
 * unavailable, it falls back to the original procedural PCM synthesis —
 * and if even that can't open a line (headless CI), it silently no-ops.
 */
public class DesktopSoundtrackEngine implements SoundtrackEngine {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 1;
    private static final int BITS = 16;
    private static final int FRAME_BYTES = CHANNELS * (BITS / 8);
    private static final int BUFFER_FRAMES = 2048;
    /** Nominal amplitude in OGG mode (Clip exposes no meter) for visualizers. */
    private static final float OGG_NOMINAL_AMPLITUDE = 0.55f;

    private volatile float volume = 0.4f;
    private volatile boolean muted = false;
    private volatile Track currentTrack;
    private volatile State state = State.STOPPED;
    private volatile float amplitude = 0f;
    private volatile boolean oggMode = false;

    private Thread synthThread;
    private SourceDataLine line;
    private Clip musicClip;
    private volatile boolean running = false;
    private volatile boolean paused = false;

    // ── Public API ────────────────────────────────────────────────────────

    @Override
    public void play(Track track) {
        if (track == currentTrack && state == State.PLAYING) return;
        stopInternal();
        currentTrack = track;
        paused = false;
        if (startOggLoop(track)) {
            oggMode = true;
            state = State.PLAYING;
            return;
        }
        oggMode = false;
        state = State.PLAYING;
        startSynth();
    }

    /** True when the current track is a bundled OGG recording. */
    boolean isOggMode() { return oggMode; }

    @Override
    public void pause() {
        if (state == State.PLAYING) {
            paused = true;
            state = State.PAUSED;
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop(); // keeps position; resume() loops again
            }
        }
    }

    @Override
    public void resume() {
        if (state == State.PAUSED) {
            paused = false;
            state = State.PLAYING;
            if (musicClip != null && musicClip.isOpen()) {
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
    }

    @Override
    public void stop() {
        stopInternal();
    }

    @Override
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        applyVolumeToLine();
        applyVolumeToClip();
    }

    @Override public float getVolume() { return volume; }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        applyVolumeToLine();
        applyVolumeToClip();
    }

    @Override public boolean isMuted() { return muted; }
    @Override public Track getCurrentTrack() { return currentTrack; }
    @Override public State getState() { return state; }
    @Override public float getAmplitude() { return amplitude; }

    @Override
    public void dispose() {
        stopInternal();
    }

    // ── OGG playback (bundled public-domain marches) ──────────────────────

    private static String oggResourceName(Track track) {
        switch (track) {
            case DASHBOARD_ORGAN: return "assets/sounds/soundtrack/dashboard_organ.ogg";
            case FIGHT_SONG: return "assets/sounds/soundtrack/fight_song.ogg";
            case OFFSEASON_CALM: return "assets/sounds/soundtrack/offseason_calm.ogg";
            case RECRUITING_GROOVE: return "assets/sounds/soundtrack/recruiting_groove.ogg";
            default: return null;
        }
    }

    /**
     * Attempts to load and loop the track's bundled OGG via vorbisspi.
     * Returns false (no exception) when the resource is missing or the
     * audio system can't open a Clip — callers fall back to synthesis.
     */
    private boolean startOggLoop(Track track) {
        String res = oggResourceName(track);
        if (res == null) return false;
        byte[] bytes;
        try (InputStream in = DesktopSoundtrackEngine.class.getClassLoader()
                .getResourceAsStream(res)) {
            if (in == null) return false;
            bytes = in.readAllBytes();
        } catch (Exception e) {
            return false;
        }
        try (javax.sound.sampled.AudioInputStream ais = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(bytes))) {
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP && !paused && running) {
                    // Loop drains only on explicit stop; nothing to do.
                }
            });
            musicClip = clip;
            amplitude = OGG_NOMINAL_AMPLITUDE;
            applyVolumeToClip();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            return true;
        } catch (Exception e) {
            musicClip = null;
            amplitude = 0f;
            return false;
        }
    }

    private void applyVolumeToClip() {
        Clip c = musicClip;
        if (c == null || !c.isOpen()) return;
        try {
            FloatControl ctrl = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float effectiveVol = muted ? 0f : volume;
            float dB = effectiveVol > 0 ? (float) (20 * Math.log10(effectiveVol)) : -80f;
            dB = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB));
            ctrl.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // No gain control on this line.
        }
    }

    // ── Synthesis thread ──────────────────────────────────────────────────

    private void startSynth() {
        running = true;
        synthThread = new Thread(this::synthLoop, "cfhc-soundtrack");
        synthThread.setDaemon(true);
        synthThread.start();
    }

    private void stopInternal() {
        running = false;
        state = State.STOPPED;
        if (musicClip != null) {
            try {
                musicClip.stop();
                musicClip.close();
            } catch (Exception ignored) {
            }
            musicClip = null;
        }
        if (synthThread != null) {
            synthThread.interrupt();
            try { synthThread.join(300); } catch (InterruptedException ignored) {}
            synthThread = null;
        }
        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
        amplitude = 0f;
    }

    private void synthLoop() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, BUFFER_FRAMES * FRAME_BYTES);
            applyVolumeToLine();
            line.start();
        } catch (LineUnavailableException e) {
            // Headless or no audio device — silently degrade.
            running = false;
            state = State.STOPPED;
            return;
        }

        // Each track defines a melodic loop: frequency + duration in beats.
        double bpm = currentTrack == Track.FIGHT_SONG ? 140
                : currentTrack == Track.RECRUITING_GROOVE ? 120
                : currentTrack == Track.OFFSEASON_CALM ? 70 : 90;
        double beatDur = 60.0 / bpm; // seconds per beat
        Note[] loop = getLoop(currentTrack);
        int loopIdx = 0;
        long noteFrame = 0;
        long noteLenFrames = (long) (loop.length > 0 ? loop[0].beats * beatDur * SAMPLE_RATE : SAMPLE_RATE);
        Random rng = new Random(42);

        byte[] buf = new byte[BUFFER_FRAMES * FRAME_BYTES];

        while (running) {
            if (paused) {
                // Write silence while paused.
                Arrays.fill(buf, (byte) 0);
                line.write(buf, 0, buf.length);
                amplitude = 0f;
                continue;
            }

            // Advance to next note if current note finished.
            if (noteFrame >= noteLenFrames && loop.length > 0) {
                loopIdx = (loopIdx + 1) % loop.length;
                noteFrame = 0;
                noteLenFrames = (long) (loop[loopIdx].beats * beatDur * SAMPLE_RATE);
            }

            // Fill buffer with synthesized samples for the current note.
            double maxAmp = 0;
            for (int i = 0; i < BUFFER_FRAMES; i++) {
                double t = (double) (noteFrame + i) / SAMPLE_RATE;
                double sample;
                if (loop.length > 0) {
                    Note n = loop[loopIdx];
                    double envelope = envelope(t, n.beats * beatDur);
                    sample = synth(n.freq, t, currentTrack) * envelope;
                } else {
                    sample = 0;
                }
                // Soft limiting to prevent clipping.
                sample = Math.tanh(sample);

                short s = (short) (sample * Short.MAX_VALUE);
                buf[i * 2] = (byte) (s & 0xFF);
                buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                maxAmp = Math.max(maxAmp, Math.abs(sample));
            }
            noteFrame += BUFFER_FRAMES;
            amplitude = (float) maxAmp;
            line.write(buf, 0, buf.length);
        }
    }

    // ── Synthesis helpers ─────────────────────────────────────────────────

    /** ADSR-like envelope: quick attack, decay, sustain, release. */
    private static double envelope(double t, double duration) {
        double attack = 0.02;
        double release = 0.15;
        if (t < attack) return t / attack;
        if (t > duration - release) return Math.max(0, (duration - t) / release);
        return 0.7; // sustain level
    }

    /**
     * Synthesize one sample for a given frequency and time. The waveform
     * depends on the track character:
     * <ul>
     *   <li>DASHBOARD_ORGAN — sine + 3rd harmonic (organ-like)</li>
     *   <li>FIGHT_SONG — sawtooth-ish (brassy) + noise accent on beat</li>
     *   <li>OFFSEASON_CALM — pure sine with soft decay (piano-ish)</li>
     *   <li>RECRUITING_GROOVE — square wave + sub-bass (chiptune)</li>
     * </ul>
     */
    private static double synth(double freq, double t, Track track) {
        switch (track) {
            case DASHBOARD_ORGAN: {
                double fundamental = Math.sin(2 * Math.PI * freq * t);
                double harm3 = 0.3 * Math.sin(2 * Math.PI * freq * 3 * t);
                double harm5 = 0.15 * Math.sin(2 * Math.PI * freq * 5 * t);
                return (fundamental + harm3 + harm5) * 0.33;
            }
            case FIGHT_SONG: {
                double fundamental = Math.sin(2 * Math.PI * freq * t);
                double saw = 0.4 * sawtooth(freq, t);
                double beatAccent = (t % 0.5 < 0.03) ? 0.15 : 0; // snare-like tick
                return (fundamental + saw) * 0.28 + beatAccent;
            }
            case OFFSEASON_CALM: {
                return 0.4 * Math.sin(2 * Math.PI * freq * t);
            }
            case RECRUITING_GROOVE: {
                double square = 0.35 * squareWave(freq, t);
                double subBass = 0.25 * Math.sin(2 * Math.PI * (freq / 2.0) * t);
                return square + subBass;
            }
            default:
                return 0.3 * Math.sin(2 * Math.PI * freq * t);
        }
    }

    private static double sawtooth(double freq, double t) {
        double phase = (freq * t) % 1.0;
        return 2.0 * phase - 1.0;
    }

    private static double squareWave(double freq, double t) {
        double phase = (freq * t) % 1.0;
        return phase < 0.5 ? 1.0 : -1.0;
    }

    private void applyVolumeToLine() {
        if (line == null) return;
        try {
            FloatControl ctrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float effectiveVol = muted ? 0f : volume;
            float dB = effectiveVol > 0
                    ? (float) (20 * Math.log10(effectiveVol))
                    : -80f;
            dB = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB));
            ctrl.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // MASTER_GAIN not available — volume handled at sample level instead.
        }
    }

    // ── Track note data (original compositions) ───────────────────────────

    /** A note in the procedural loop: frequency in Hz + duration in beats. */
    private static final class Note {
        final double freq;
        final double beats;
        Note(double freq, double beats) { this.freq = freq; this.beats = beats; }
    }

    // Note frequencies (Hz)
    private static final double REST = 0;
    private static final double C4 = 261.63, D4 = 293.66, E4 = 329.63, F4 = 349.23,
            G4 = 392.00, A4 = 440.00, B4 = 493.88;
    private static final double C5 = 523.25, D5 = 587.33, E5 = 659.25, F5 = 698.46,
            G5 = 783.99, A5 = 880.00;
    private static final double Bb4 = 466.16, E5b = 622.25;
    private static final double A2 = 110.00, A3 = 220.00, E3 = 164.81, G3 = 196.00, C3 = 130.81;

    /**
     * Returns the melodic loop for a track. Each track is an original
     * composition written specifically for CFHC.
     */
    private static Note[] getLoop(Track track) {
        switch (track) {
            case DASHBOARD_ORGAN:
                // I–V–vi–IV in C major (C–G–Am–F) — the classic "arena" progression.
                return new Note[] {
                    new Note(C4, 0.5), new Note(E4, 0.5), new Note(G4, 0.5), new Note(C5, 1),
                    new Note(G4, 0.5), new Note(B4, 0.5), new Note(D5, 0.5), new Note(G5, 1),
                    new Note(A4, 0.5), new Note(C5, 0.5), new Note(E5, 0.5), new Note(A5, 1),
                    new Note(F4, 0.5), new Note(A4, 0.5), new Note(C5, 0.5), new Note(F5, 1),
                };
            case FIGHT_SONG:
                // Brassy march in Bb — punchy quarter notes.
                return new Note[] {
                    new Note(Bb4, 0.5), new Note(Bb4, 0.25), new Note(D5, 0.25), new Note(F5, 0.5),
                    new Note(E5b, 0.5), new Note(D5, 0.5), new Note(Bb4, 0.5), new Note(REST, 0.5),
                    new Note(F5, 0.25), new Note(F5, 0.25), new Note(E5b, 0.25), new Note(D5, 0.25),
                    new Note(C5, 0.5), new Note(Bb4, 0.5), new Note(REST, 0.5), new Note(D5, 0.5),
                };
            case OFFSEASON_CALM:
                // Slow, reflective whole notes — piano-like.
                return new Note[] {
                    new Note(C4, 2), new Note(E4, 2), new Note(G4, 2), new Note(B4, 2),
                    new Note(C5, 2), new Note(B4, 2), new Note(G4, 2), new Note(E4, 2),
                };
            case RECRUITING_GROOVE:
                // Driving eighth-note bass groove in A minor.
                return new Note[] {
                    new Note(A2, 0.25), new Note(A3, 0.25), new Note(E3, 0.25), new Note(A3, 0.25),
                    new Note(G3, 0.25), new Note(A3, 0.25), new Note(E3, 0.25), new Note(A3, 0.25),
                    new Note(C3, 0.25), new Note(A3, 0.25), new Note(G3, 0.25), new Note(E3, 0.25),
                    new Note(A2, 0.25), new Note(A3, 0.25), new Note(E3, 0.25), new Note(G3, 0.25),
                };
            default:
                return new Note[] { new Note(C4, 1), new Note(G4, 1), new Note(E4, 1), new Note(C4, 1) };
        }
    }
}
