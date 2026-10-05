# AGENTS.md

## Cursor Cloud specific instructions

CFHC is an offline single-product game (no backend/DB/network). There are two build
front-ends over one shared Java engine:

- **Desktop / engine (JDK 17 only, no Android SDK)** — portable engine + Swing prototype.
- **Android app `:app` (needs Android SDK, platform `android-35`, build-tools `35.0.0`)** — the shipping product.

Standard commands live in `README.md` and `build.gradle`; this section only covers non-obvious cloud caveats.

### Environment (already provisioned in the VM snapshot)
- JDK 17 is installed at `/usr/lib/jvm/java-17-openjdk-amd64` (the VM default `java` is 21). Gradle uses a
  Java 17 **toolchain** for compilation, so Gradle itself runs fine, but run Android tasks with
  `JAVA_HOME` pointing at JDK 17 (interactive shells get this from `~/.bashrc`).
- The Android SDK lives at `~/android-sdk`; `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`PATH` are exported in `~/.bashrc`.
- The committed `local.properties` has a Windows `sdk.dir` and is gitignored-but-tracked. The update script
  rewrites it to `sdk.dir=$HOME/android-sdk` on startup; do **not** commit that local edit.

### Building / running
- **SDK-free gate (engine unit tests + desktop jar):** `./gradlew -p desktop-standalone :engine:desktopStandaloneGate`
- **Android:** `./gradlew :app:assembleDebug` and `./gradlew :app:lintDebug` both work (APK at `app/build/outputs/apk/debug/`).
- **Desktop GUI** (`./gradlew runDesktop`, or `java -jar .../CFHC-desktop-prototype.jar new`) needs a display.
  For headless work use the jar CLI: `help`, `inspect <save>`, and especially `stability`
  (runs a new game + 3 full seasons headlessly — good end-to-end sim smoke test).

### Notes
- The Android `test` source set excludes `src/test/java/desktop/**` (Swing tests run only through the
  `desktop-standalone` gate), so `./gradlew test` / `:app:testDebugUnitTest` compile engine tests only.
- The regular season is 12 games over `League.STANDARD_REG_SEASON_WEEKS` (14) weeks: week 0 preseason, weeks 1-12
  games, week 13 conference championships. Week math goes through `SeasonFlowOrder`; play-button text comes from
  `SeasonPresentation.getPlayWeekLabel`. Saves store the season length in `L:` header field 8; saves without it
  load as the legacy 13-week calendar and switch to 14 weeks at the next season rollover.
- Without the Android SDK, `python3 scripts/verify_android_res.py` checks resources and every `R.*` reference.
