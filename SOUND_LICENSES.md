# Sound and Desktop Library Licenses

## UI and Game Sound Effects

### Generated WAV SFX (Original Work — No License Required)
`desktop-assets/sfx/*.wav` are the desktop shell's preferred sound effects:
original PCM synthesized for this project (soft plucked tones, sweeps, and a
vibrato whistle — click, advance, confirm, error, play, firstdown, whistle,
win, loss). They replaced the inherited OGG set on desktop because most of
those files decode to no audio (corrupt) or are near-inaudible, and the
retro-buzzer character read as harsh static during rapid UI navigation.
Gradle copies them onto the desktop classpath at `assets/sounds/` and
`DesktopAudioManager` prefers them per event (the OGGs remain the fallback
and the Android `res/raw` source).

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

### March recordings (Public Domain — U.S. Government Works)
`desktop-assets/soundtrack/*.ogg` are real military-band performances of
classic football-marching-band music, sourced from Wikimedia Commons. As
works of the U.S. federal government (performed by U.S. military bands)
they are in the public domain; the underlying compositions (Sousa, Bagley)
are themselves public domain.

| In-game name | Work | Performer | Source (Wikimedia Commons) |
|---|---|---|---|
| `fight_song.ogg` | The Stars and Stripes Forever (J.P. Sousa) | United States Marine Band | `File:USMC stars stripes forever.ogg` |
| `dashboard_organ.ogg` | The Washington Post March (J.P. Sousa) | U.S. Army Band | `File:Washington Post March - U.S. Army Band.ogg` |
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
