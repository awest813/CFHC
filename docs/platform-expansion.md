# Platform Expansion

CFHC is Android-first today, but the simulation stack is close to being reusable across platforms. The goal is to treat Android as *one shell* around a shared game core rather than the place where the sim itself lives.

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
- `GameUiBridge` interface defines all UI callbacks the core needs:
  - Crash / fatal-error handling
  - Recruiting hand-off
  - Transfer decision prompts
  - Spinner / team-refresh after realignment
  - Discipline decision prompts
- `GameUiBridge` ships with a `NO_OP` implementation and headless-safe constructor overloads, so non-Android shells can boot the core before their UI workflow is ready.
- `PlatformResourceProvider` abstracts asset and string loading away from Android resources.
- `GameFlowManager` centralises season state-transitions.

**Still blocking expansion:**
- Save/load and import/export logic lives in `MainActivity` (see [Roadmap item 7](ROADMAP.md#7--extract-saveload-logic-out-of-mainactivity)).
- `RecruitingActivity ↔ MainActivity` circular dependency (see [Roadmap item 2](ROADMAP.md#2--break-the-recruitingactivity--mainactivity-circular-dependency)).
- No headless simulation facade yet (see [Roadmap item 15](ROADMAP.md#15--introduce-a-headless-simulation-facade)).

---

## Remaining Steps to Enable Other Shells

1. **Move save / import / export parsing** out of `MainActivity` into shared services in `simulation/` so Android is not the only host that understands league bootstrapping.
2. **Decouple recruiting** — replace direct file/UI coupling with serializable recruiting-state objects and action methods.
3. **Introduce a headless simulation facade** with a clean public API for common flows:
   - `createDynasty(config)`
   - `loadSave(path)`
   - `advanceWeek(dynasty)`
   - `resolveOffseason(dynasty)`
   - `prepareRecruitingData(dynasty)`
4. Once the facade exists, build:
   - An **iPhone shell** around the facade/state objects.
   - A **desktop shell** around the same facade/state objects.

---

## Shell Design Goals

### iPhone
- Native SwiftUI app with a touch-first dashboard.
- Same full career-mode loop as Android.
- Clean tab-bar navigation (Team · Recruiting · Schedule · History).
- Communicate with the Java core through a serialized state/service layer or a C-interop bridge (e.g., J2ObjC or a REST microservice for prototyping).

### Desktop
- Denser management view aimed at mouse/keyboard users.
- Wider stat comparison tables, persistent side navigation, multi-panel recruiting screens.
- Possible targets: Java Swing/JavaFX (reuses existing JVM), Electron backed by the shared core, or a web front-end talking to a local simulation server.

---

## Reference

- [Roadmap](ROADMAP.md) — full prioritized action plan
- [README](../README.md) — project overview and engine audit summary
