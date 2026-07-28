<div align="center">

# College Football Head Coach (CFHC)

**Dynasty college football simulation — Android app plus a JVM desktop shell on one shared engine.**

[![Version](https://img.shields.io/badge/version-v1.4.5-blue?style=flat-square)](https://github.com/awest813/CFHC/releases)
[![Android](https://img.shields.io/badge/Android-minSdk%2024%20%7C%20targetSdk%2035-green?style=flat-square&logo=android)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.9%20%7C%20AGP%208.7-lightgrey?style=flat-square&logo=gradle)](https://gradle.org)
[![License](https://img.shields.io/badge/license-see%20LICENSE-informational?style=flat-square)](LICENSE)
[![Reddit](https://img.shields.io/badge/reddit-r%2Ffootballcoach-FF4500?style=flat-square&logo=reddit)](https://www.reddit.com/r/footballcoach/)

</div>

---

## About

**CFHC** is an open-source college football coaching simulation: hire staff, recruit, run seasons with play-by-play detail, and chase titles. The Android build is **fully offline** (no ads, IAP, or accounts). A **Swing desktop shell** lives in the same repository and compiles against the **same engine packages** as mobile (`simulation`, `positions`, `staff`, `comparator`, portable `recruiting`, and `desktop/**`), while Android-specific UI stays under `antdroid/` and `ui/`.

| | |
|:---|:---|
| **Current release** | v1.4.5 (`versionCode` 320) |
| **Package** | `antdroid.cfbcoach` |
| **Language / toolchain** | Java 17 · Gradle 8.9 · Android Gradle Plugin 8.7 |
| **Automated tests** | JUnit — engine flows, recruiting, comparators, persistence (coverage is growing, not exhaustive) |

### Gradle modules

| Module | Role |
|:---|:---|
| **`:engine`** | `java-library` — portable packages: `simulation`, `positions`, `staff`, `comparator`, `recruiting` (shared with desktop). |
| **`:app`** | `com.android.application` — `antdroid.*`, `ui.*`, resources, manifest; depends on `:engine`. |
| **(root)** | Desktop jar tasks (`desktopJar`, `compileDesktopJava`, `desktopVerify`) and project-wide checks; sources still live under `src/main/java` in the repo root. |

`./gradlew assembleDebug` and `./gradlew test` run the Android app module; the debug APK is under `app/build/outputs/apk/debug/`. Run `./gradlew :app:lintDebug` before large UI changes (CI runs it on the **android** job).

---

## Clone and prerequisites

| Goal | You need |
|:---|:---|
| **Android app** | Android Studio (or JDK 17 + Android SDK with **API 35**), this repo |
| **Desktop only** | **JDK 17** only — no Android SDK required |
| **Optional** | Device or emulator for APK install |

```bash
git clone https://github.com/awest813/CFHC.git
cd CFHC
```

---

## Building and running

### Android

From the repository root (same directory as `gradlew`):

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/`. Other variants (e.g. `beta`, `release`) use the usual Gradle `assemble*` / `bundle*` tasks (e.g. `:app:assembleRelease`). Open the project in Android Studio and use **Run** for iterative development.

### Desktop (no Android SDK)

The standalone JVM project compiles the shared engine, runs unit tests with the **repository root** as the working directory, and builds a runnable jar — **without** the Android SDK.

**Play a release jar** (Java 17+):

```bash
java -jar CFHC-desktop-1.4e.jar          # Career Hub launcher
java -jar CFHC-desktop-1.4e.jar help
java -jar CFHC-desktop-1.4e.jar play ~/.cfhc/saves/my-league.cfb
```

Saves default to `~/.cfhc/saves` on Linux (`~/Library/Application Support/CFHC/saves` on macOS, `%APPDATA%\CFHC\saves` on Windows). Use **File → Save** (Ctrl+S); desktop prompts when a new season starts and on exit if unsaved.

**Build from source:**

```bash
./gradlew -p desktop-standalone :engine:desktopStandaloneGate
```

Jar output: `desktop-standalone/engine/build/libs/CFHC-desktop-1.4e.jar` (also `build/libs/` from the root `desktopJar` task).

If you already use the **root** Gradle build (Android Studio), equivalent desktop tasks live there:

| Task | Purpose |
|:---|:---|
| `desktopVerify` | Import scan for forbidden Android APIs in engine packages, required resource files, desktop Java compile |
| `desktopJar` | Produces `CFHC-desktop-1.4e.jar` (see `build/libs/`) |
| `runDesktop` | Runs `desktop.Main`; optional `-PdesktopArgs="..."` |

```bash
./gradlew desktopVerify
./gradlew desktopJar
./gradlew runDesktop
./gradlew runDesktop -PdesktopArgs="new"
java -jar build/libs/CFHC-desktop-1.4e.jar play path/to/save.cfb
java -jar build/libs/CFHC-desktop-1.4e.jar help
```

| CLI (jar or `runDesktop`) | |
|:---|:---|
| *(no args)* | Opens the Swing launcher |
| `new` | New-game flow, then league UI |
| `play <file.cfb>` / `view <file>` | Load a save (prompts for a user team if needed) |
| `inspect <file>` | Print save metadata |
| `help` | Usage |

Unsigned Linux jpackage app-image scaffold: `./gradlew -p desktop-standalone :engine:desktopAppImage` (requires JDK `jpackage`; produces an app directory, not a `.AppImage` file).

**Resources:** `DesktopResourceContract` lists required `res/values` XML and asset paths. Gradle desktop tasks must run from the **repo root** so resources and tests resolve consistently.

**Engine rule:** `checkEngineImports` / the standalone build fail if engine packages import `android.*`, `androidx.*`, or `antdroid.*`.

**Licenses:** The jar embeds `LICENSE` (CC0) and `SOUND_LICENSES.md` (CC BY sounds + LGPL OGG SPI). In-app: **Help → Licenses & Attribution**.

### Continuous integration

GitHub Actions runs two jobs in parallel:

| Job | What it does |
|:---|:---|
| **desktop-jvm** | `desktop-standalone` gate: tests + jar + verification — **Android SDK env vars cleared** |
| **android** | `./gradlew test` with a provisioned SDK (debug / beta / release unit test variants) |

Pushes and PRs to `main` / `master` trigger CI; **workflow_dispatch** allows manual runs.

---

## Architecture

### Source layout (high level)

```
src/main/java/
├── simulation/   Core sim — leagues, games, seasons, records (platform-neutral)
├── positions/    Player models
├── staff/        Coaching staff models
├── comparator/   Sorting / ranking helpers
├── recruiting/   Portable recruiting logic (extra UI under antdroid/…/recruiting/)
├── ui/           Android UI helpers
├── antdroid/     Android app shell (activities, navigation, dialogs)
└── desktop/      Swing desktop shell (excluded from APK via Gradle source set)
```

### Platform bridge

Host code talks to the engine through small interfaces (Android and desktop both wired):

| Interface | Role |
|:---|:---|
| `GameUiBridge` | UI callbacks (errors, recruiting handoffs, etc.); includes a no-op default |
| `PlatformResourceProvider` | Strings and assets without direct `R` / asset manager coupling |
| `GameFlowManager` | High-level flow (start game, season advances, recruiting transitions) |
| `PlatformLog` | Logging without `android.util.Log` |

More detail: [docs/platform-expansion.md](docs/platform-expansion.md).

---

## Features (summary)

- **Career:** hiring, staff, facilities, contracts, promotions and pressure
- **Recruiting:** full cycle, large name pools, scholarships, transfers, redshirt options
- **Simulation:** play-by-play, multiple schemes, stats, news, awards, progression
- **League:** conferences, bowls, playoff options, polls, prestige, infractions, history
- **Customization:** CSV import for universes/rosters/coaches; in-game renames; light/dark themes
- **Desktop:** Career Hub launcher, league shell, docked recruiting, dark/high-contrast themes, CSV import/export — Java 17 jar (`CFHC-desktop-1.4e.jar`)

---

## Documentation

| Doc | Contents |
|:---|:---|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Priorities, technical debt, planned work |
| [docs/THREADING.md](docs/THREADING.md) | Single-thread engine mutation contract |
| [docs/platform-expansion.md](docs/platform-expansion.md) | Cross-platform bridge and migration notes |
| [docs/desktop-release-notes-1.4e.md](docs/desktop-release-notes-1.4e.md) | Desktop 1.4e run / save / attribution notes |

---

## FAQ

<details>
<summary><strong>How do I save?</strong></summary>

**Android:** The game can auto-save at season boundaries (you’ll see a prompt). Restoring a save returns you to the **start of the season** in which that file was created.

**Desktop:** Use **File → Save** (Ctrl+S). Saves default to your CFHC saves folder (`~/.cfhc/saves` on Linux). Desktop prompts to save when a new season begins and when you exit with unsaved changes. There is no silent auto-save — save before quitting.
</details>

<details>
<summary><strong>Where is play-by-play on Android?</strong></summary>

**Schedule → tap a score → tap the blue bar** at the top of the pop-up.
</details>

<details>
<summary><strong>How do I import custom data?</strong></summary>

Use formats aligned with the bundled sample universes, then **Import** from the main menu. Teams, rosters, and coaches can be imported separately.
</details>

<details>
<summary><strong>What does the desktop build support?</strong></summary>

New career wizard, load/save (`.cfb` / `.sav` / `.txt`), week-by-week and bulk season advance, docked recruiting with disk checkpoints, transfer summaries, redshirts, depth chart/playbook, CSV coach/roster import, and league export. Requires **Java 17+**. Run `java -jar CFHC-desktop-1.4e.jar`. Some Android-only UI flows remain more complete on mobile.
</details>

---

## Contributing

Contributions are welcome: [Issues](https://github.com/awest813/CFHC/issues) for bugs and ideas, pull requests for code. Keep changes scoped and explain what changed and why. Match the style of the files you touch.

Community: [/r/footballcoach](https://www.reddit.com/r/footballcoach/).

---

## License

See [LICENSE](LICENSE).
