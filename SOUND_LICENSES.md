# Sound and Desktop Library Licenses

## UI and Game Sound Effects

### Generated WAV SFX (Original Work — No License Required)
`desktop-assets/sfx/*.wav` are the game's sound effects on every platform:
original PCM synthesized for this project (soft plucked tones, sweeps, and a
vibrato whistle — click, advance, confirm, error, play, firstdown, whistle,
win, loss). They replaced the previously inherited OGG set because most of
those files decoded to no audio (corrupt) or were near-inaudible, and the
retro-buzzer character read as harsh static during rapid UI navigation.
Gradle copies them onto the desktop classpath at `assets/sounds/` and into
Android's `src/main/res/raw/` (same resource names, so `R.raw.*` references
are unchanged). No third-party sound effects are distributed anymore.

### MP3SPI (LGPL 2.1+)
- **Artifact:** `com.googlecode.soundlibs:mp3spi:1.9.5.4`
- **Source:** https://www.javazoom.net/mp3spi/mp3spi.html (JavaZoom)
- **License:** GNU Lesser General Public License v2.1 or later
- **Purpose:** MP3 playback support on the desktop platform via Java's
  Service Provider Interface (SPI) for `javax.sound.sampled` (used for the
  bundled MP3 main theme; the 17 toolchain predates JDK-native MP3).
- **Usage:** Bundled in the desktop jar from `libs/mp3spi-*.jar`.

### JLayer (LGPL 2.1+)
- **Artifact:** `com.googlecode.soundlibs:jlayer:1.0.1.4`
- **License:** GNU Lesser General Public License v2.1 or later
- **Purpose:** The MP3 decoder required by MP3SPI at runtime.
- **Usage:** Bundled in the desktop jar from `libs/jlayer-*.jar`.

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

### Main theme: "Marching Band" (Pixabay Content License)
`desktop-assets/soundtrack/marching_band.mp3` is the default dashboard /
main-menu theme.
- **Source:** https://pixabay.com/music/marching-band-marching-band-485958/
- **Licensor:** stereo_color — https://pixabay.com/users/stereo_color-54563004/
- **License:** Pixabay Content License (https://pixabay.com/service/terms/)
  — free for commercial and non-commercial use, no attribution required.
- **Audio File ID:** 485958 (license certificate dated 2026-08-26)
- **Changes:** Renamed to the engine's track name; audio unmodified.

### March recordings (Public Domain — U.S. Government Works)
`desktop-assets/soundtrack/*.ogg` are real military-band performances of
classic football-marching-band music, sourced from Wikimedia Commons. As
works of the U.S. federal government (performed by U.S. military bands)
they are in the public domain; the underlying compositions (Sousa, Bagley)
are themselves public domain.

| In-game name | Work | Performer | Source (Wikimedia Commons) |
|---|---|---|---|
| `fight_song.ogg` | The Stars and Stripes Forever (J.P. Sousa) | United States Marine Band | `File:USMC stars stripes forever.ogg` |
| `offseason_calm.ogg` | National Emblem March (E.E. Bagley) | U.S. Army Band | `File:National Emblem - U.S. Army Band.ogg` |
| `recruiting_groove.ogg` | Semper Fidelis March (J.P. Sousa) | U.S. Navy Band | `File:Semper Fidelis March - U.S. Navy Band.ogg` |

- **License:** Public domain (U.S. government work; compositions also PD)
- **Retrieved from:** commons.wikimedia.org (see per-file source above)
- **Changes:** Renamed to the engine's track names; audio unmodified.
- **Packaging:** Kept in `desktop-assets/` (outside `src/main/assets`) so
  the Android APK is not bloated; Gradle copies them onto the desktop
  classpath and bundles them into the desktop jar.

### Procedural Synthesis Fallback (Original Work — No License Required)
If a march OGG is missing or no audio line can be opened, the
`DesktopSoundtrackEngine` falls back to original in-code PCM synthesis:
band-limited melody, chord pad, bass, and enveloped percussion voices with
original progressions written for this project. That fallback is entirely
original work.
