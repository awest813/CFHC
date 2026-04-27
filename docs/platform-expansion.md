# Platform Expansion

CFHC is still Android-first, but the simulation stack is already shared by the Android app and a Swing desktop prototype. The long-term goal is to treat Android as *one shell* around a reusable game core rather than the place where the sim itself lives.

---

## Target Architecture

```
┌─────────────────────────────────────────────┐
│                Shared Game Core              │
│  simulation · positions · staff · comparator │
│  + portable parts of recruiting / team logic │
└──────────┬──────────────┬────────────────────┘
           │              │              │
    ┌──────▼──────┐ ┌─────▼──────┐ ┌────▼──────────┐
    │  Android    │ │   iPhone   │ │    Desktop    │
    │    Shell    │ │   Shell    │ │    Shell      │
    │ (Activities │ │  (SwiftUI) │ │ (Java/Web/…)  │
    │  Adapters   │ │            │ │               │
    │   Dialogs)  │ │            │ │               │
    └─────────────┘ └────────────┘ └───────────────┘
```

Each shell communicates with the core through the platform bridge interfaces (`GameUiBridge`, `PlatformResourceProvider`, `GameFlowManager`, `PlatformLog`). No shell should need to reach into simulation internals directly.

---

## Current Status

**Completed:**
- `League` and `Team` no longer depend directly on `MainActivity`.
- Simulation-side logging replaced with `PlatformLog` (no `android.util.Log` dependency).
- Save-slot orchestration now lives in shared persistence helpers (`LeagueSaveStorage` / `SaveLoadService`) instead of `MainActivity`.
- Shared export helpers (`LeagueExportController`) are used by both Android and desktop flows.
- `GameUiBridge` interface defines all UI callbacks the core needs:
  - Crash / fatal-error handling
  - Recruiting hand-off
  - Transfer decision prompts
  - Spinner / team-refresh after realignment
  - Discipline decision prompts
- `GameUiBridge` ships with a `NO_OP` implementation and headless-safe constructor overloads, so non-Android shells can boot the core before their UI workflow is ready.
- `PlatformResourceProvider` abstracts asset and string loading away from Android resources.
- `GameFlowManager` centralises season state-transitions.
- The `RecruitingActivity ↔ MainActivity` circular dependency has been removed; recruiting now goes through shared controller/flow abstractions.
- A Swing desktop shell exists today (`desktop.*`, `runDesktop`, `desktopJar`) and can run the shared core.

**Still blocking expansion:**
- No headless simulation facade yet (see [Roadmap item 15](ROADMAP.md#15--introduce-a-headless-simulation-facade)).
- Import flow is still partly Android-owned (`LeagueImportFlowController`, Android file picking, roster/coach import orchestration).
- The engine still assumes single-threaded, UI-driven orchestration; alternate shells need a clearer threading + lifecycle contract.

---

## Remaining Steps to Enable Other Shells

1. **Finish sharing import flows** — custom-universe parsing already lives in `simulation/`, but Android still owns file selection plus roster/coach import orchestration.
2. **Introduce a headless simulation facade** with a clean public API for common flows:
   - `createDynasty(config)`
   - `loadSave(path)`
   - `advanceWeek(dynasty)`
   - `resolveOffseason(dynasty)`
   - `prepareRecruitingData(dynasty)`
3. **Document save/schema/threading contracts** so non-Android shells can safely drive the engine without depending on Activity timing.
4. **Keep hardening the desktop shell** around the facade/state objects as the proving ground for non-Android support.
5. Once the facade exists, build:
   - An **iPhone shell** around the facade/state objects.
   - Future desktop alternatives (JavaFX/web/native) around the same facade/state objects if Swing stops being the preferred shell.

---

## Shell Design Goals

### iPhone
- Native SwiftUI app with a touch-first dashboard.
- Same full career-mode loop as Android.
- Clean tab-bar navigation (Team · Recruiting · Schedule · History).
- Communicate with the Java core through a serialized state/service layer or a C-interop bridge (e.g., J2ObjC or a REST microservice for prototyping).

### Desktop
- Current state: Swing prototype with launcher, league-home workspace, recruiting tab, dark mode, and shared export support.
- Near-term goal: denser management view aimed at mouse/keyboard users.
- Longer-term options: continue with Swing, move to JavaFX, or expose the shared core to a web/native shell once the facade is stable.

---

## Reference

- [Roadmap](ROADMAP.md) — full prioritized action plan
- [README](../README.md) — project overview and current status snapshot
