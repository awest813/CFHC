# Desktop release notes — 1.4e

## Requirements
- **Java 17+** runtime (`java -version`)
- Optional audio output (OGG UI sounds; fails soft if the mixer is unavailable)

## Run
```bash
java -jar CFHC-desktop-1.4e.jar
java -jar CFHC-desktop-1.4e.jar play /path/to/save.cfb
java -jar CFHC-desktop-1.4e.jar help
```

## Saves
- Default folder: `~/.cfhc/saves` (Linux), `~/Library/Application Support/CFHC/saves` (macOS), `%APPDATA%\CFHC\saves` (Windows)
- **File → Save** (Ctrl+S). Prompted at new-season start and on exit if dirty.
- Recruiting progress is checkpointed beside the save (`.cfb.recruiting`) when you save.

## What’s included
- Career Hub launcher, new-career wizard, load/save
- Week play + bulk sim (career decisions still prompt; progress dialog Interrupt/X cancels)
- Docked recruiting, transfer summaries, redshirts, depth chart / playbook
- FlatLaf light/dark + high contrast, CSV import/export
- Help → Check for Updates (GitHub Releases; manual download)
- macOS: Aqua screen menu bar + About / Preferences / Quit handlers; `.cfb` open via file association (jpackage)

## Packaging (optional)
```bash
./gradlew -p desktop-standalone :engine:desktopJpackageImage
./gradlew -p desktop-standalone :engine:desktopPortableZip
```
Produces an unsigned jpackage app directory (and a zip of it). Not a Linux `.AppImage` file. Unsigned `.dmg`/`.msi` tasks exist for macOS/Windows hosts (`desktopDmg` / `desktopMsi`).

## Attribution
See **Help → Licenses & Attribution**, and `LICENSE` / `SOUND_LICENSES.md` inside the jar
(CC0 game code, CC BY sounds, LGPL OGG SPI, Apache FlatLaf).
