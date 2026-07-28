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

### Known pre-existing issues (NOT environment problems)
- `./gradlew test` / `:app:testDebugUnitTest` **fails to compile** on `master`: desktop-only Swing tests under
  `src/test/java/desktop/**` (e.g. `PlayerSearchPanelTest`) are pulled into the Android `test` source set, which
  excludes desktop classes and has no JDK Swing. This also fails on CI's `android` job. Run engine unit tests via
  the `desktop-standalone` gate instead (that path compiles/runs the shared engine + desktop tests correctly).
- `simulation.GameBoxScoreTest.boxScore_overtimeScoreIsRecorded` is **flaky** (unseeded random game sim; a team can
  legitimately score 0 in OT). Re-run the test class if it fails in isolation.
