## Goal: Add a procedural soundtrack engine to CFHC

Build a background-music (BGM) engine that generates original chiptune-style stadium music procedurally in-code (no external audio files needed — 100% original, no licensing issues), with the infrastructure to drop in OGG tracks later. Wire the `DesktopStatusFooter` to show the real track name, animate the equalizer, and allow play/pause.

## Why procedural generation (not downloaded tracks)

The user asked for "MIT projects and compatible open source." Rather than downloading third-party audio files (which requires finding, verifying licensing, and bundling binary assets), I'll generate original music in-code using `javax.sound.sampled` synthesis. This is:
- **100% original** — no licensing concerns whatsoever
- **Zero binary asset bloat** — no OGG files in the repo
- **Extensible** — when real OGG tracks are added later, the engine detects and plays them instead

## Architecture

### Part 1 — `simulation/SoundtrackEngine.java` (portable interface)

```java
public interface SoundtrackEngine {
    enum Track { DASHBOARD_ORGAN, FIGHT_SONG, OFFSEASON_CALM, RECRUITING groove }
    enum State { PLAYING, PAUSED, STOPPED }

    void play(Track track);
    void pause();
    void resume();
    void stop();
    void setVolume(float volume);  // 0.0–1.0, separate from SFX
    float getVolume();
    void setMuted(boolean muted);
    boolean isMuted();
    Track getCurrentTrack();
    State getState();
    void dispose();

    SoundtrackEngine NO_OP = new SoundtrackEngine() { /* all no-op */ };
}
```

Separate from `AudioManager` (SFX) — BGM has different lifecycle needs (looping, pause/resume, separate volume channel).

### Part 2 — `desktop/DesktopSoundtrackEngine.java` (desktop implementation)

**Procedural audio synthesis** using `javax.sound.sampled.SourceDataLine` on a dedicated daemon thread:
- Each `Track` defines a sequence of notes (frequency + duration) as a melodic loop
- A synthesis thread generates 16-bit PCM samples (sine + harmonic + envelope) and writes to `SourceDataLine` continuously
- `play()` starts the thread; `pause()` stops writing (silence); `stop()` kills the thread
- Volume applied via `FloatControl(MASTER_GAIN)`, same formula as existing `DesktopAudioManager`
- Graceful fallback: if `SourceDataLine` unavailable (headless), silently no-ops

**Track definitions** (procedural melodies — original compositions):
- `DASHBOARD_ORGAN` — slow stadium-organ arpeggio in C major (I–V–vi–IV progression, the classic "arena" feel)
- `FIGHT_SONG` — upbeat brass-like march in Bb (punchy quarter notes, snare-like noise accent)
- `OFFSEASON_CALM` — gentle piano-like pad (slow whole notes, soft sine + decay)
- `RECRUITING_groove` — energetic bass-driven groove in A minor (eighth-note pulse)

**Future OGG support**: `play()` checks if `assets/sounds/soundtrack/<track>.ogg` exists on the classpath first; if so, loads via vorbisspi + `Clip.loop(LOOP_CONTINUOUSLY)` instead of synthesis. This makes the engine forward-compatible with real music assets.

### Part 3 — Wire `DesktopStatusFooter` to the engine

- Replace hardcoded `"Campus Drive — Midnight Rally"` with the real current track name from `SoundtrackEngine.getCurrentTrack()`
- Replace the static equalizer bars with an **animated equalizer** driven by a `javax.swing.Timer` (40ms interval) that reads the current PCM amplitude from the synthesis thread and paints bar heights proportional to it
- Add a **play/pause toggle**: clicking the speaker icon toggles mute on the soundtrack engine
- Add a reference to `SoundtrackEngine` passed via constructor

### Part 4 — Integration in `LeagueHomeView`

- Instantiate `DesktopSoundtrackEngine` alongside `DesktopAudioManager`
- Pass it to `DesktopStatusFooter`
- Start `DASHBOARD_ORGAN` on app launch; switch tracks on screen change (e.g., `FIGHT_SONG` during game week, `OFFSEASON_CALM` during offseason)
- Dispose on window close

### Part 5 — `SOUND_LICENSES.md` update

Document that the procedural soundtrack is original work generated in-code by the CFHC project (no third-party license required). Note the forward-compatible OGG-drop-in design.

## Out of scope
- **Android BGM** — would need `MediaPlayer` integration; separate effort. Engine interface is ready for it.
- **Real OGG music tracks** — the engine supports them but I'm not bundling third-party audio. The user can drop OGG files into `assets/sounds/soundtrack/` later.
- **Crossfade between tracks** — simple hard-cut for now; crossfade is a nice-to-have.

## Verification
- `compileDesktopJava` — compiles
- `desktopStandaloneGate` — no regression
- `runDesktop` — app launches, music plays (or silently no-ops if headless)
- No new binary assets in the repo

## Files touched
- `simulation/SoundtrackEngine.java` — new (interface + NO_OP)
- `desktop/DesktopSoundtrackEngine.java` — new (procedural synthesis engine)
- `desktop/DesktopStatusFooter.java` — wire track name + animated equalizer + mute toggle
- `desktop/LeagueHomeView.java` — instantiate engine, pass to footer, lifecycle
- `SOUND_LICENSES.md` — document original procedural tracks