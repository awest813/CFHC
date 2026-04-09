# Platform Expansion Foundation

This project is still an Android-first app, but the simulation stack is close to being reusable across platforms. The first portability goal is to treat Android as one shell around a shared game core rather than the place where the sim itself lives.

## Recommended target shape

- Shared game core: `simulation`, `positions`, `staff`, `comparator`, and the non-Android parts of recruiting/team logic.
- Android shell: activities, adapters, XML layouts, Android storage, dialogs, and navigation.
- iPhone shell: SwiftUI/UIKit front end that talks to the shared core through a platform bridge or a serialized state/service layer.
- Desktop shell: either a Java desktop client, a web/electron client backed by the shared core, or a future server-driven shell.

## First extraction completed

- `League` and `Team` no longer depend directly on `MainActivity`.
- Simulation-side logging no longer requires `android.util.Log`.
- A small `GameUiBridge` interface now defines the UI callbacks the core needs:
  - crash handling
  - recruiting handoff
  - transfer decisions
  - spinner/team refresh after realignment
  - discipline decision prompts
- The bridge now has a `NO_OP` implementation plus headless-safe constructor overloads, so non-Android shells can boot the core before their UI workflow is finished.

## Next steps

1. Move save/import/export parsing out of `MainActivity` into shared services so Android is not the only place that understands league bootstrapping.
2. Replace direct file/UI coupling in recruiting with serializable recruiting state objects and action methods.
3. Introduce a headless simulation facade for common flows such as:
   - create new dynasty
   - load save
   - advance week
   - resolve offseason
   - prepare recruiting data
4. Once that facade exists, build:
   - an iPhone shell around the facade/state objects
   - a desktop shell around the same facade/state objects

## Practical product interpretation

- iPhone version: aim for a native-feeling SwiftUI app with the same career-mode loop and a touch-first dashboard.
- Desktop version: aim for a denser management view with wider comparison tables, persistent side navigation, and multi-panel recruiting/team screens.
