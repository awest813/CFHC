package desktop;

import simulation.SoundtrackEngine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
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
            state = State.PLAYING;
            return;
        }
        state = State.PLAYING;
        startSynth();
    }

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
            // IoStreams helper (not InputStream.readAllBytes) — Android lint
            // scans the shared tree and readAllBytes needs API 33 > minSdk 24.
            bytes = simulation.IoStreams.readAllBytes(in);
        } catch (Exception e) {
            return false;
        }
        try (javax.sound.sampled.AudioInputStream raw = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(bytes));
             javax.sound.sampled.AudioInputStream ais = AudioDecoding.toPcm(raw)) {
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
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

        SynthVoice voice = new SynthVoice(currentTrack);
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

            double maxAmp = 0;
            for (int i = 0; i < BUFFER_FRAMES; i++) {
                double sample = voice.nextSample(rng);
                // Soft limiting to prevent clipping.
                sample = Math.tanh(sample);
                short s = (short) (sample * Short.MAX_VALUE);
                buf[i * 2] = (byte) (s & 0xFF);
                buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                maxAmp = Math.max(maxAmp, Math.abs(sample));
            }
            amplitude = (float) maxAmp;
            line.write(buf, 0, buf.length);
        }
    }

    // ── Synthesis helpers ─────────────────────────────────────────────────

    /**
     * One arranged voice of the fallback soundtrack: melody + chord pad +
     * bass + (for march/groove tracks) enveloped percussion, all
     * phase-continuous and passed through a gentle one-pole lowpass.
     */
    private static final class SynthVoice {
        private final Track track;
        private final double beatDur;    // seconds per beat
        private final Note[] melody;
        private final Chord[] chords;
        private final boolean percussive;

        private int melodyIdx;
        private double melodyPos;        // seconds into current melody note
        private double melodyLen;        // duration of current melody note (s)
        private int chordIdx;
        private double chordPos;
        private double chordLen;
        private double beatPos;
        private int beatCount;

        private double melodyPhase;
        private final double[] padPhase = new double[3];
        private double bassPhase;
        private double lowpassY;

        private static final double LOWPASS_ALPHA =
                1.0 - Math.exp(-2.0 * Math.PI * 3200.0 / SAMPLE_RATE);

        SynthVoice(Track track) {
            this.track = track;
            double bpm = track == Track.FIGHT_SONG ? 132
                    : track == Track.RECRUITING_GROOVE ? 112
                    : track == Track.OFFSEASON_CALM ? 66 : 84;
            this.beatDur = 60.0 / bpm;
            this.melody = getLoop(track);
            this.chords = getChords(track);
            this.percussive = track == Track.FIGHT_SONG || track == Track.RECRUITING_GROOVE;
            this.melodyLen = melody.length > 0 ? melody[0].beats * beatDur : beatDur;
            this.chordLen = chords.length > 0 ? chords[0].beats * beatDur : 4 * beatDur;
        }

        /** Produces the next output sample. */
        double nextSample(Random rng) {
            double dt = 1.0 / SAMPLE_RATE;

            Note note = melody.length > 0 ? melody[melodyIdx] : null;
            Chord chord = chords.length > 0 ? chords[chordIdx] : null;

            // ── Melody voice (REST notes are true silence). ──
            double melodyOut = 0;
            double freq = note != null ? note.freq : 0;
            if (freq > 0) {
                double env = noteEnv(melodyPos, melodyLen);
                melodyPhase += 2 * Math.PI * freq * dt;
                if (melodyPhase > 2 * Math.PI) melodyPhase -= 2 * Math.PI;
                melodyOut = melodyWave(melodyPhase) * env * melodyGain();
            } else {
                melodyPhase = 0;
            }

            // ── Chord pad (root + third + fifth). ──
            double padOut = 0;
            if (chord != null) {
                double env = padEnv(chordPos, chordLen);
                int[] intervals = chord.minor
                        ? new int[] {0, 3, 7} : new int[] {0, 4, 7};
                for (int v = 0; v < 3; v++) {
                    double f = semitones(chord.rootFreq, intervals[v]);
                    padPhase[v] += 2 * Math.PI * f * dt;
                    if (padPhase[v] > 2 * Math.PI) padPhase[v] -= 2 * Math.PI;
                    padOut += Math.sin(padPhase[v]) / 3.0;
                }
                padOut *= env * padGain();

                // ── Bass follows the chord root one octave down. ──
                bassPhase += 2 * Math.PI * (chord.rootFreq / 2.0) * dt;
                if (bassPhase > 2 * Math.PI) bassPhase -= 2 * Math.PI;
                double pulse = percussive
                        ? 0.6 + 0.4 * Math.exp(-beatPos / (beatDur * 0.3)) : 1.0;
                padOut += Math.sin(bassPhase) * env * bassGain() * pulse;
            }

            // ── Percussion: enveloped noise bursts, never step functions. ──
            double percOut = 0;
            if (percussive) {
                double accent = (beatCount % 4 == 0) ? 1.0 : 0.55;
                percOut = (rng.nextDouble() * 2.0 - 1.0)
                        * 0.05 * accent * Math.exp(-beatPos / 0.028);
            }

            // ── Advance timers. ──
            melodyPos += dt;
            if (melodyPos >= melodyLen && melody.length > 0) {
                melodyPos -= melodyLen;
                melodyIdx = (melodyIdx + 1) % melody.length;
                melodyLen = Math.max(0.02, melody[melodyIdx].beats * beatDur);
                melodyPhase = 0;
            }
            chordPos += dt;
            if (chordPos >= chordLen && chords.length > 0) {
                chordPos -= chordLen;
                chordIdx = (chordIdx + 1) % chords.length;
                chordLen = Math.max(0.05, chords[chordIdx].beats * beatDur);
            }
            beatPos += dt;
            if (beatPos >= beatDur) {
                beatPos -= beatDur;
                beatCount++;
            }

            // ── Mix through a gentle lowpass to remove residual harshness. ──
            double mixed = melodyOut + padOut + percOut;
            lowpassY += LOWPASS_ALPHA * (mixed - lowpassY);
            return lowpassY;
        }

        /** Band-limited melody timbre per track (no raw saw/square steps). */
        private double melodyWave(double phase) {
            switch (track) {
                case DASHBOARD_ORGAN:
                    return Math.sin(phase) + 0.35 * Math.sin(2 * phase)
                            + 0.12 * Math.sin(3 * phase);
                case FIGHT_SONG: // brass-ish: stacked harmonics
                    return Math.sin(phase) + 0.40 * Math.sin(2 * phase)
                            + 0.20 * Math.sin(3 * phase) + 0.10 * Math.sin(4 * phase);
                case OFFSEASON_CALM:
                    return Math.sin(phase) + 0.15 * Math.sin(2 * phase);
                case RECRUITING_GROOVE: // soft square via saturated sine
                    return Math.tanh(2.2 * Math.sin(phase));
                default:
                    return Math.sin(phase);
            }
        }

        private double melodyGain() {
            switch (track) {
                case OFFSEASON_CALM: return 0.34;
                case RECRUITING_GROOVE: return 0.20;
                default: return 0.26;
            }
        }

        private double padGain() {
            switch (track) {
                case OFFSEASON_CALM: return 0.13;
                case FIGHT_SONG: return 0.08;
                default: return 0.10;
            }
        }

        private double bassGain() {
            return track == Track.RECRUITING_GROOVE ? 0.20 : 0.14;
        }

        /** Pad envelopes are slower than melody envelopes for legato feel. */
        private double padEnv(double t, double dur) {
            double attack = track == Track.FIGHT_SONG || track == Track.RECRUITING_GROOVE
                    ? 0.05 : 0.25;
            double release = Math.min(0.30, dur * 0.3);
            if (t < attack) return t / attack;
            if (t > dur - release) return Math.max(0, (dur - t) / release);
            return 1.0;
        }
    }

    /** ADSR-like envelope: fast attack, sustain, proportional release. */
    private static double noteEnv(double t, double dur) {
        double attack = 0.008;
        double release = Math.min(0.12, dur * 0.4);
        if (t < attack) return t / attack;
        if (t > dur - release) return Math.max(0, (dur - t) / release);
        return 0.78;
    }

    private static double semitones(double baseFreq, double semis) {
        return baseFreq * Math.pow(2.0, semis / 12.0);
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

    /** A pad/bass chord in the procedural loop: root + quality + beats. */
    private static final class Chord {
        final double rootFreq;
        final boolean minor;
        final double beats;
        Chord(double rootFreq, boolean minor, double beats) {
            this.rootFreq = rootFreq;
            this.minor = minor;
            this.beats = beats;
        }
    }

    // Note frequencies (Hz)
    private static final double REST = 0;
    private static final double C4 = 261.63, D4 = 293.66, E4 = 329.63, F4 = 349.23,
            G4 = 392.00, A4 = 440.00, B4 = 493.88;
    private static final double C5 = 523.25, D5 = 587.33, E5 = 659.25, F5 = 698.46,
            G5 = 783.99, A5 = 880.00;
    private static final double Bb4 = 466.16, E5b = 622.25;
    private static final double A2 = 110.00, A3 = 220.00, E3 = 164.81, G3 = 196.00, C3 = 130.81;
    private static final double G2 = 98.00, F2 = 87.31, Bb2 = 116.54, Eb2 = 77.78;

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

    /**
     * Returns the pad/bass chord loop for a track. Each progression is an
     * original harmony written for CFHC and totals the same length as the
     * track's melody loop so the two re-align every pass.
     */
    private static Chord[] getChords(Track track) {
        switch (track) {
            case DASHBOARD_ORGAN: // C–G–Am–F under the 10-beat melody loop
                return new Chord[] {
                    new Chord(C3, false, 2.5), new Chord(G2, false, 2.5),
                    new Chord(A2, true, 2.5), new Chord(F2, false, 2.5),
                };
            case FIGHT_SONG: // Bb–F–Eb under the 6.5-beat melody loop
                return new Chord[] {
                    new Chord(Bb2, false, 2.5), new Chord(F2, false, 2.0),
                    new Chord(Eb2, false, 2.0),
                };
            case OFFSEASON_CALM: // C–Am–F–G under the 16-beat melody loop
                return new Chord[] {
                    new Chord(C3, false, 4), new Chord(A2, true, 4),
                    new Chord(F2, false, 4), new Chord(G2, false, 4),
                };
            case RECRUITING_GROOVE: // Am–C under the 4-beat melody loop
                return new Chord[] {
                    new Chord(A2, true, 2), new Chord(C3, false, 2),
                };
            default:
                return new Chord[] { new Chord(C3, false, 2), new Chord(A2, true, 2) };
        }
    }
}
