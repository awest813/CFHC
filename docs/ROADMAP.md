# CFHC Roadmap

> Prioritized action plan derived from the April 2026 engine audit.
> Items are grouped by urgency; within each group they are ordered roughly by impact.
>
> **Status key:** 🔲 not started · 🔄 in progress · ✅ done

---

## 🔴 Critical — Fix First

These items block Play Store compliance or cross-platform expansion and should be addressed before anything else.

---

### 1. ✅ Bump `targetSdk` to match `compileSdk`

`build.gradle` declares `compileSdk 35` but `targetSdk 30`. The mismatch means newer Android runtime behaviour is untested, and the Play Store will eventually reject apps targeting API < 33.

**Actions:**
- ~~Set `targetSdk 35`.~~ ✅ Done — `build.gradle` now has `targetSdk 35`.
- ~~Audit `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` usage~~ ✅ Done — permissions capped at `maxSdkVersion 32` in `AndroidManifest.xml`.
- Verify notification, foreground-service, and exact-alarm permission flows.
- Smoke-test on API 35 emulator before release.

---

### 2. ✅ Break the `RecruitingActivity ↔ MainActivity` circular dependency

These two activities import each other. It is the single biggest blocker for running the sim on non-Android hosts.

**Actions:**
- ~~Extract pure recruiting logic into a `RecruitingController` class with no Android imports.~~ ✅ Done — `RecruitingController.java` extracted; `RecruitingActivity` delegates all game-state mutations to it.
- ~~Have `RecruitingActivity` delegate all game-state mutations to that controller.~~ ✅ Done.
- ~~Replace direct `MainActivity` calls with a listener interface or the existing `GameFlowManager`.~~ ✅ Done — `RecruitingActivity` no longer imports `MainActivity`; uses `GameFlowManager` and `GameNavigation` instead.

---

### 3. 🔲 Decompose `League` and `Team` God Objects

`League.java` — 6,485 LOC · 109 public methods · 46 public mutable fields.
`Team.java` — 5,487 LOC · 31 public mutable fields.

**Actions (incremental — do not rewrite in one pass):**
- Extract record-keeping → `LeagueRecordKeeper` / `TeamRecords` *(partially done — `TeamRecords.java` and `LeagueRecords.java` exist)*.
- Extract stat aggregation → `LeagueStats` / `TeamStats`.
- Make internal collections `private`; expose via unmodifiable getters.
- Move scheduling logic → `ScheduleManager`.

---

### 4. 🔲 Encapsulate public mutable collections

Dozens of `public ArrayList<…>` fields on `League` and `Team` let callers bypass all validation.

**Actions:**
- Change fields to `private`.
- Add `getXxx()` returning `Collections.unmodifiableList(…)`.
- Add explicit mutation methods (`addPlayer`, `removePlayer`, etc.) where the list must change.

---

## ⚠️ High — Near-Term Improvements

---

### 5. ✅ Replace `System.out.println` with `PlatformLog`

Production simulation code still logs to stdout. The `PlatformLog` shim exists but is not used everywhere.

**Actions:**
- ~~Search-and-replace all `System.out.println` calls in `simulation/` and `positions/`.~~ ✅ Done — no `System.out.println` calls remain in `simulation/` or `positions/` (other than `PlatformLog`'s own default implementation).
- ~~Delete the direct stdout calls; route through `PlatformLog.d/i/w/e`.~~ ✅ Done.

---

### 6. ✅ Fix silent I/O failures in save/load

`IOException` is caught and swallowed in `LeagueSaveStorage` and parts of `League`. Users see blank save slots with no explanation.

**Actions:**
- ~~Log full stack traces via `PlatformLog`.~~ ✅ Done — `LeagueSaveStorage` and `League.saveLeague()` log via `PlatformLog.e`.
- ~~Surface a user-visible error message through `GameUiBridge`.~~ ✅ Done — `MainActivity.saveLeague()` now checks the return value of `saveToSlot` and shows a long error Toast on failure.
- ~~Write to a `.tmp` file first, then atomically rename to the real save path.~~ ✅ Done — `League.saveLeague()` now writes to `<name>.tmp`, then renames to the final slot file.

---

### 7. ✅ Extract slot save/load orchestration out of `MainActivity`

Slot persistence used to be wired directly through `MainActivity`, which made reuse awkward for other shells.

**Actions:**
- ~~Create a `SaveLoadService` class in `simulation/`.~~ ✅ Done — `SaveLoadService.java` exists and handles slot management.
- ~~Move slot-save path resolution and orchestration behind shared services.~~ ✅ Done — `LeagueSaveStorage` and `SaveLoadService` now own the reusable save-slot flow.
- ~~Have `MainActivity` (and any future shell) delegate to `SaveLoadService`.~~ ✅ Done — `MainActivity` now saves through `saveLoadService.saveToSlot()`.

---

### 8. 🔲 Add null-safety annotations and checks

Critical paths (e.g., `findConference()` return values, roster lookups) lack null checks and crash without meaningful diagnostics.

**Actions:**
- Annotate public API methods with `@Nullable` / `@NonNull`.
- Add `Objects.requireNonNull()` guards at key entry points.
- Fix the highest-risk call sites identified during the audit.

---

### 9. ✅ Update AndroidX and material dependencies

`appcompat:1.4.2` and `material:1.6.1` were ~2 years behind; they were missing security patches and API improvements needed by `targetSdk 35`.

**Actions:**
- ~~Bump to latest stable versions (`appcompat 1.7.x`, `material 1.12.x`, etc.).~~ ✅ Done — `build.gradle` now has `appcompat:1.7.0` and `material:1.13.0`.
- Run full regression smoke-test on device/emulator.

---

## 📋 Medium — Structured Refactoring

These do not block any immediate release but will make the codebase significantly easier to extend and test.

---

### 10. 🔲 Split `MainActivity` into focused controllers

At ~3,656 LOC, `MainActivity` owns too many concerns. Target split:

| New class | Responsibility |
|:---|:---|
| `GameStateManager` | Season state, flags, current-week tracking |
| `GameNavigator` | Screen routing and back-stack management |
| `SaveLoadController` | Delegates to `SaveLoadService` (see #7) |
| `ImportExportController` | CSV import/export orchestration |

---

### 11. 🔲 Separate `RecruitingSessionData` concerns

471 LOC mixes simulation state with UI state.

**Actions:**
- Split into `RecruitingSimulation` (portable — goes in `simulation/` or `recruiting/`).
- Split into `RecruitingUIState` (Android-only — stays in `recruiting/` or `antdroid/`).

---

### 12. 🔲 Add save-file schema versioning

Currently only `League.saveVer = "v1.4e"`. Format changes silently break old saves.

**Actions:**
- Add a structured version header at the top of each save file.
- Write a migration layer that upgrades older formats on load.

---

### 13. 🔲 Organise the comparator package

77 single-file comparators live in one flat directory.

**Options:**
- Group into sub-packages: `comparator.coach`, `comparator.team`, `comparator.player`.
- Or consolidate small comparators into lambda helpers on the classes they sort.

---

### 14. 🔲 Remove legacy `PlayerProfile.java`

`PlayerProfile.java` exists alongside `PlayerProfileV2.java`. Once V2 is confirmed stable, delete the old version and update all references.

---

## 🟢 Longer-Term — Platform Expansion & Quality

These items require the Critical and High items above to be largely complete first.

---

### 15. 🔲 Introduce a headless simulation facade

A single entry-point class (`SimulationFacade` or similar) exposing a clean API:

```
createDynasty(config) → Dynasty
loadSave(path) → Dynasty
advanceWeek(dynasty) → WeekResult
resolveOffseason(dynasty) → OffseasonResult
prepareRecruitingData(dynasty) → RecruitingSnapshot
```

This facade becomes the API surface for iOS and desktop shells.
*Requires items 2 and 7.*

---

### 16. 🔲 Expand the automated test suite

Initial JUnit coverage now exists (`ComparatorTest`, recruiting tests, full-season simulation, and save/load round-trip), but coverage is still too narrow for safe large refactors.

**Suggested starting points:**
- `Game.java` — deterministic outcomes given a fixed seed.
- `LeagueSaveStorage` / `SaveLoadService` — failure cases and corrupted-save handling.
- Recruiting + bridge flows — verify Android-independent controller behavior.

---

### 17. 🔲 Document the threading model

The engine assumes single-threaded access throughout. If a desktop or server shell introduces background threads, data corruption becomes likely.

**Actions:**
- Add a `THREADING.md` doc describing the single-thread contract.
- Annotate key classes with `@NotThreadSafe`.
- Identify the safest future path (e.g., single game-thread with message-passing).

---

### 18. 🔲 Build an iOS shell

Target a native SwiftUI app that talks to the shared core through the platform bridge.
*Requires items 2, 7, and 15.*

---

### 19. 🔲 Graduate the desktop shell from prototype to supported app

The Swing shell already exists. The remaining work is to harden it, close parity gaps, and make packaging/distribution practical.
*Requires items 2, 7, and 15. See [Platform Expansion](platform-expansion.md) and [Desktop improvement roadmap](desktop-improvement-roadmap.md) for design goals.*

---

### 20. 🔲 Adopt a structured logging framework

Replace the lightweight `PlatformLog` shim with SLF4J (portable) + Timber (Android) for richer log levels, tag-based filtering, and pluggable sinks (Logcat, file, remote).

---

## Reference

| Doc | Purpose |
|:---|:---|
| [Platform Expansion](platform-expansion.md) | Design goals for iOS and desktop shells |
| [README — Engine Audit Summary](../README.md#engine-audit-summary) | High-level audit findings |
| [Privacy Policy](../Privacy-Policy.md) | App privacy disclosures |
