# Sound Effect Licenses

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
- **Usage:** The vorbisspi JAR is bundled in the desktop distribution (`libs/` directory).
