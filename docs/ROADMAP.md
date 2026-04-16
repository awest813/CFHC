# CFHC Roadmap

> Prioritized action plan derived from the April 2026 engine audit.  
> Items are grouped by urgency; within each group they are roughly ordered by impact.

---

## 🔴 Critical — Fix First

### 1. Update `targetSdk` to match `compileSdk`
`build.gradle` declares `compileSdk 35` but `targetSdk 30`. This mismatch means newer runtime behaviour changes are untested and the app will eventually be blocked from the Play Store.

**Action:** set `targetSdk 35` and verify permissions, storage, and notification APIs still work.

### 2. Break the recruiting ↔ MainActivity circular dependency
`RecruitingActivity` imports `MainActivity` and vice-versa. This is the single biggest blocker for platform expansion.

**Action:**
- Extract pure recruiting logic into a `RecruitingController` (no Android imports).
- Have `RecruitingActivity` delegate to that controller for game-state mutations.
- Communicate back to the main screen through a listener interface or the existing `GameFlowManager`.

### 3. Begin decomposing League and Team God Objects
`League.java` (6 485 LOC, 109 public methods, 46 public mutable fields) and `Team.java` (5 487 LOC, 31 public mutable fields) concentrate too much responsibility.

**Action (incremental):**
- Extract record-keeping into `LeagueRecordKeeper` / `TeamRecords`.
- Extract stat-aggregation into `LeagueStats` / `TeamStats`.
- Make collections private; expose through unmodifiable getters.

### 4. Encapsulate public mutable collections
Dozens of `public ArrayList<…>` fields on League and Team allow callers to bypass all validation.

**Action:** convert to `private` fields with `getXxx()` returning `Collections.unmodifiableList(…)` and mutation methods where needed.

---

## ⚠️ High — Near-Term Improvements

### 5. Replace `System.out.println` with `PlatformLog`
Production simulation code still logs through `System.out`. The `PlatformLog` shim already exists but is not used everywhere.

**Action:** find-and-replace across `simulation/` and `positions/` packages.

### 6. Improve error handling for save/load I/O
`IOException` is caught and silently swallowed in `LeagueSaveStorage` and parts of `League`. Users see blank save slots with no explanation.

**Action:**
- Log full stack traces via `PlatformLog`.
- Surface a user-visible error message through `GameUiBridge`.
- Write saves to a temp file first, then atomically rename.

### 7. Extract save/load logic from `MainActivity`
Import, export, and persistence code lives inside `MainActivity` (3 656 LOC). It needs to move into the simulation layer so non-Android shells can use it.

**Action:** create a `SaveLoadService` in `simulation/` that `MainActivity` (and future shells) call.

### 8. Add null-safety annotations and checks
Critical paths (e.g., `findConference()` return values, roster lookups) lack null checks and crash without clear diagnostics.

**Action:** annotate public APIs with `@Nullable`/`@NonNull`; add `Objects.requireNonNull()` at key boundaries.

### 9. Update AndroidX dependencies
`appcompat 1.4.2`, `material 1.6.1`, and others are ~2 years behind current releases, missing security and bug fixes.

**Action:** bump to latest stable versions and regression-test.

---

## 📋 Medium — Structured Refactoring

### 10. Split `MainActivity` into focused controllers
**Target pieces:** `GameStateManager`, `GameNavigator`, `SaveLoadController`, `ImportExportController`.

### 11. Separate `RecruitingSessionData` concerns
471 LOC mixing simulation state and UI state. Split into `RecruitingSimulation` (portable) and `RecruitingUIState` (Android).

### 12. Add save-file schema versioning
Currently only `League.saveVer = "v1.4e"`. Add a structured version header and forward-migration logic so format changes don't break old saves.

### 13. Organise comparator package
77 single-file comparators in one flat directory. Consider sub-packages (`comparator.coach`, `comparator.team`, `comparator.player`) or consolidation into lambda-based helpers.

### 14. Clean up legacy files
`PlayerProfile.java` exists alongside `PlayerProfileV2.java`. Remove the old version once V2 is confirmed stable.

---

## 🟢 Longer-Term — Platform Expansion & Quality

### 15. Introduce a headless simulation facade
A single entry-point class that exposes: create dynasty → load save → advance week → resolve off-season → prepare recruiting data. This facade is the API surface for future shells.

### 16. Add a unit-test suite
Zero automated tests today. Start with the simulation package (League, Team, Game) since it is already platform-independent.

### 17. Document the threading model
The engine assumes single-threaded access. If desktop or server shells ever introduce concurrency, data corruption is likely. Document the contract and consider `@ThreadSafe` annotations.

### 18. Build an iOS shell
Requires items 2, 7, and 15 first. Target a native SwiftUI app that talks to the shared core through a platform bridge.

### 19. Build a desktop shell
Requires items 2, 7, and 15 first. Target a denser management UI with wider tables and multi-panel views (see [Platform Expansion](platform-expansion.md)).

### 20. Adopt a structured logging framework
Replace the lightweight `PlatformLog` shim with SLF4J (portable) + Timber (Android) for richer log levels, formatting, and sink routing.

---

## Reference

- [Platform Expansion Notes](platform-expansion.md) — design goals for iOS and desktop shells
- [README — Engine Audit Summary](../README.md#engine-audit-summary) — high-level findings
