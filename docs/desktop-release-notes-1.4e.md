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
- Dark mode + high contrast, CSV import/export

## Attribution
See **Help → Licenses & Attribution**, and `LICENSE` / `SOUND_LICENSES.md` inside the jar.
