# Sound and Desktop Library Licenses

## UI and Game Sound Effects

Sound files in `src/main/res/raw/` and `src/main/assets/sounds/` are sourced from:

### blips by NotExplosive (CC BY 4.0)
- **Source:** https://github.com/notexplosive/blips
- **License:** Creative Commons Attribution 4.0 International (CC BY 4.0)
- **Attribution:** Sound effects by NotExplosive, used under CC BY 4.0
- **Files used (original → in-game name):**
  click.ogg → click.ogg, bong.ogg → confirm.ogg + win.ogg, buzz.ogg → error.ogg,
  whistle1.ogg → whistle.ogg, metal-tap.ogg → play.ogg, snap.ogg → firstdown.ogg,
  metal.ogg → advance.ogg, tag2.ogg → loss.ogg
- **Changes:** Files renamed to match game event names; no audio content modified.

### VorbisSPI (LGPL 2.1+)
- **Source:** https://github.com/trilarion/vorbisspi
- **License:** GNU Lesser General Public License v2.1 or later
- **Purpose:** Provides OGG Vorbis playback support on the desktop platform via
  Java's Service Provider Interface (SPI) for `javax.sound.sampled`.
- **Usage:** Bundled in the desktop jar from `libs/vorbisspi-*.jar`.

### JOrbis (LGPL 2.1+)
- **Artifact:** `com.googlecode.soundlibs:jorbis:0.0.17.4`
- **License:** GNU Lesser General Public License v2.1 or later
- **Purpose:** Native Java OGG Vorbis decoder required by VorbisSPI at runtime.
- **Usage:** Bundled in the desktop jar from `libs/jorbis-*.jar`.

### Tritonus Share (LGPL 2.1+)
- **Artifact:** `com.googlecode.soundlibs:tritonus-share:0.3.7.4`
- **License:** GNU Lesser General Public License v2.1 or later
- **Purpose:** Shared sampled-audio helpers (`TAudioFileReader`) required by VorbisSPI.
- **Usage:** Bundled in the desktop jar from `libs/tritonus-share-*.jar`.

## Desktop UI libraries

### FlatLaf (Apache 2.0)
- **Artifact:** `com.formdev:flatlaf:3.5.4`
- **Source:** https://www.formdev.com/flatlaf/ / https://github.com/JFormDesigner/FlatLaf
- **License:** Apache License 2.0
- **Purpose:** Cross-platform light/dark Swing look-and-feel used by the desktop shell.
- **Usage:** Bundled in the desktop jar from `libs/flatlaf-*.jar`.

## Soundtrack / Background Music

### Procedural Synthesis Engine (Original Work — No License Required)
The background music is generated entirely in-code by the CFHC project's
`DesktopSoundtrackEngine`. No third-party audio files are used. The music
is synthesized in real-time from mathematical waveforms (sine, sawtooth,
square) and original melodic compositions written specifically for this
project. All four soundtrack tracks are original works:

- **Stadium Organ — Dashboard:** C major I–V–vi–IV arpeggio progression
- **Fight Song — Game Day:** Brassy Bb march with snare accents
- **Offseason Reflection:** Slow piano-like pad in whole notes
- **Recruiting Groove:** Driving eighth-note bass line in A minor

The engine is forward-compatible with recorded OGG tracks: if files are
placed at `src/main/assets/sounds/soundtrack/<track>.ogg`, the engine will
load and loop them via VorbisSPI instead of synthesizing. Any such files
must be documented here with their source and license.
