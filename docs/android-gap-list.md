# Android Gap List

> Audit of the Android app (`:app`, package `antdroid.cfbcoach`) against the shared engine, the
> desktop shell, Play Store requirements and the open items in earlier plans. Static audit of
> `claude/tender-clarke-7yh5ya` @ `35ede6a` (Nov 2026). The audit container had no Android SDK,
> so nothing was rebuilt or run on a device; every item below comes from reading the source.
>
> Earlier documents this builds on: [`ship-readiness-audit.md`](ship-readiness-audit.md) (A1–A11),
> [`android-ui-redesign-plan.md`](android-ui-redesign-plan.md) (Waves 0–6), [`ROADMAP.md`](ROADMAP.md).
>
> **Status (follow-up commit 841ed8e):** G1, G2, G3, G5, G6 and G7 are fixed in code, and G4 is
> fixed for saves. The Android module was **not compiled** in the audit container (no SDK). The
> engine half is covered by `simulation.AutosaveTest`. Run CI (`:app:assembleDebug` / `lintDebug`)
> and the device checklist at the end of this file before release.
>
> Priority: **P0** blocks the next Play release or loses user data · **P1** players will notice
> it · **P2** quality or maintainability debt.

---

## Summary

| # | Gap | Area | Pri |
|---|-----|------|-----|
| G1 | ✅ `targetSdk 35` is below Play's 2026 target-API requirement | Platform | P0 |
| G2 | ✅ Recreating the activity (process death, dark-mode toggle, resize, fold) reloads the league from the launch intent, so unsaved progress is lost and a "new league" launch regenerates the whole league | Data safety | P0 |
| G3 | ✅ No autosave. Progress is kept only by a manual save to a slot | Data safety | P0 |
| G4 | 🟡 Save and league load/generation run on the UI thread (ANR risk), carried over from A2 | Threading | P1 |
| G5 | ✅ Team selection mutates `League` on a raw background thread, breaking the threading contract (A1) | Threading | P1 |
| G6 | ✅ Recruiting payload is passed as an Intent String extra and gets close to the ~1 MB Binder limit late in a dynasty (A11) | Stability | P1 |
| G7 | ✅ Deprecated `setSystemUiVisibility` immersive/edge-to-edge code. API 36 removes the edge-to-edge opt-out (A10) | Platform | P1 |
| G8 | Desktop parity: no interactive gameday coaching, no Offseason Hub, no league-scoreboard box scores, plus 9 smaller gaps (§3) | Features | P1/P2 |
| G9 | No Android-side tests: no `androidTest`, no unit tests for `antdroid/` or `ui/`, no UI snapshot baseline | Quality | P1 |
| G10 | Unverified known issue: `./gradlew test` (CI `android` job) reportedly fails to compile | CI | P1 |
| G11 | Accessibility: 26 `contentDescription`s across all layouts, 82 hard-coded UI strings in Java | A11y / i18n | P2 |
| G12 | Redesign leftovers: 0 RecyclerViews, 0 custom views, 81 raw `AlertDialog.Builder`s, 514 literal dp/sp | UI debt | P2 |
| G13 | Large screens and tablets: phone-only layouts, `largeHeap`, `configChanges` doesn't cover multi-window or folding | Platform | P2 |
| G14 | `MainActivity` is 2,529 lines and mixes flow, dialogs, save/load and audio | Maintainability | P2 |

---

## 1. Platform & Play Store

### G1 — Target API below Play requirement (P0)
- `app/build.gradle`: `compileSdk 35`, `targetSdk 35`.
- Play's yearly rule requires app updates to target the previous year's major release. As of
  Aug 31 2026 that is **Android 16 / API 36**. A v1.4.6+ upload at `targetSdk 35` should be
  rejected. **Confirm the exact cut-off date in the Play Console before planning around it.**
- Moving to 36 brings in G7 (edge-to-edge can't be opted out of), predictive back on by default
  (back already goes through `OnBackPressedDispatcher` in all 3 activities, which is good), and
  large-screen orientation/resizability overrides (G13).
- Work: bump `compileSdk`/`targetSdk` to 36, update AGP if needed, re-run `lintDebug`, and smoke-test on an
  API 36 emulator. The CI `setup-android` package list also needs `platforms;android-36`.

### G7 — Legacy system-UI flags (P1)
- `MainActivity.java:2477` (`showSystemUI`), `PlatformUiHelper.java:135`,
  `ImmersiveDialogHelper.java:26` still use `View.setSystemUiVisibility` / `SYSTEM_UI_FLAG_*`,
  which have been deprecated since API 30.
- No code uses `WindowCompat` / `WindowInsetsCompat` / `ViewCompat.setOnApplyWindowInsetsListener`, so content
  has no inset handling. Under enforced edge-to-edge (API 35+, and with no opt-out on 36),
  the toolbar, drawer header and bottom controls can draw under the status and navigation bars.
- Work: switch to `WindowCompat.setDecorFitsSystemWindows(false)` +
  `WindowInsetsControllerCompat`, and pad the root views from insets.

### G13 — Large screens, multi-window, foldables (P2)
- One layout set: no `layout-sw600dp`/`layout-land` variants. Every screen is a phone layout
  stretched across a tablet.
- `MainActivity`/`RecruitingActivity` declare `configChanges="keyboardHidden|orientation|screenSize"`
  only. `uiMode`, `screenLayout`, `smallestScreenSize` and `density` changes still recreate the
  activity, which triggers G2.
- `android:largeHeap="true"` hides memory growth and isn't honoured on every device. Profile a
  20-season dynasty instead of relying on it.

---

## 2. Data safety, threading, stability

### G2 — Activity recreation reloads from the launch intent (P0)
- `MainActivity.onCreate` → `loadGame()` (`MainActivity.java:649`) always rebuilds `simLeague`
  from `GameNavigation.getLaunchRequest(getIntent())`. Nothing is written in
  `onSaveInstanceState`, and `onPause` only pauses audio (`MainActivity.java:2499`).
- Any recreation re-runs the original launch:
  - Launched with **NEW_LEAGUE**: a brand-new league is generated and the user is put back into the
    team picker. The whole session is gone.
  - Launched from a **save slot**: the slot is reloaded, and everything played since the last
    manual save is lost silently.
  - Launched with **DONE_RECRUITING**: the recruit handoff is replayed.
- Triggers: the system killing the process in the background (common on low-RAM devices
  with a `largeHeap` app), toggling system dark mode, changing locale or font scale, entering split-screen, or
  folding/unfolding.
- Work: (a) add `uiMode|screenLayout|smallestScreenSize|density` to `configChanges` as a
  quick mitigation; (b) the real fix is G3 plus resuming from the autosave when
  `savedInstanceState != null`.

### G3 — No autosave (P0)
- Saves happen only through the slot picker (`saveLeague`, `MainActivity.java:1769`) or "Save & Exit".
  There's no autosave after each week, before recruiting or at season rollover, and none in
  `onStop`.
- Work: write a dedicated autosave slot after each `advanceWeek()` and in `onStop`, off the UI
  thread from a snapshot (see G4), then offer "Resume" on Home.

### G4 — UI-thread save / load (P1, A2 still open)
- `saveLoadService.saveToSlot(simLeague, …)` runs inside the dialog click handler on the main
  thread (`MainActivity.java:1781,1790`). `loadGame()` (league generation or a full save parse)
  runs synchronously in `onCreate`.
- Deep dynasties on slow storage risk an ANR on Save, and the new-league path shows a frozen screen on
  launch.
- Work: serialize to a `String` on the UI thread (cheap), then write the file on an executor. Move
  load/generation behind a progress screen, keeping league mutation on one thread, as
  `docs/THREADING.md` requires.

### G5 — `prepareSelectedTeam` mutates `League` off-thread (P1, A1 still open)
- `MainActivity.java:744`: a raw `new Thread` calls `addCoachFreeAgent`, `setTeamRanks`,
  `setTeamBenchMarks` and `updateTeamTalentRatings` on the live league. Only the modal dialog
  prevents a race today.
- Work: do the mutations on the UI thread (they're fast), or move all league mutation onto one
  game-thread executor shared with the bulk simulator (`MainActivity.java:1484`).

### G6 — Recruiting Intent payload (P1, A11 still open)
- `MainActivity.startRecruiting` → `SimulationFacade.buildRecruitingPayload(userTeam)` →
  `GameNavigation.java:45` `putExtra(EXTRA_USER_TEAM_INFO, …)`. The string grows with roster and board size,
  and passing ~1 MB throws `TransactionTooLargeException`.
- Work: write the payload to a file in `getFilesDir()` and pass its key. The desktop
  `DesktopRecruitingCheckpoint` / `SessionStore` already follow this pattern.

---

## 3. Feature parity with desktop (G8)

For each desktop feature: find the engine API it calls, then grep `antdroid/` and `ui/` for that API.

### Missing on Android
| # | Feature | Desktop entry | Android evidence | Pri |
|---|---|---|---|---|
| P1 | **Interactive gameday coaching** (pregame / halftime / crunch checkpoints → `GameCoachPlan`) | `desktop/LeagueHomeView.java:377` → `CoachDecisionDialog.java:32` via `setGameCoachListener` | No `GameCoachListener`/`GameCoachPlan` in Android; only the pre-kickoff scheme change in `GameDialogController.java:174` | P1 |
| P2 | **Offseason Hub** (10-step checklist + "Advance One Step") | `LeagueHomeView.java:388` → `OffseasonHubDialog.java:29` (`SeasonFlowOrder.offseasonSteps()`) | Android uses only `firstOffseasonWeek` (`MainActivity.java:1469`) | P1 |
| P3 | **Team morale card** (morale snapshot, win streak) | `TeamMoraleCard.java:93` (`getTeamMoraleSnapshot`) | Not referenced; only "Team Chemistry" as a rankings category | P2 |
| P4 | About / open-source licenses screen | `LeagueHomeView.java:646-662` | No licenses screen. Needed for the bundled OFL fonts and Apache-licensed AndroidX attribution | P2 |
| — | Update checker | `DesktopUpdateChecker` | Not needed: Play handles updates. Optional in-app update prompt via Play Core | — |

### Partial on Android
| # | Feature | Gap | Evidence | Pri |
|---|---|---|---|---|
| P5 | Redshirts incl. medical | Can only grant in preseason (`MainActivity.java:432`). Can't remove one, and there's no league pool. Medical RS shows only as a status letter (`StatsRowAdapter.java:109`) | desktop `RedshirtDialog.java:152,202,235,272` | P2 |
| P6 | Coordinator hiring | Only offered when the engine reaches that offseason step (`CareerDialogController.java:301`). No menu entry to fire or hire coordinators any time | desktop `CoordinatorHiringDialog.java:113-380` | P2 |
| P7 | Box scores from the league scoreboard | Weekly Scores (`WeeklyScoresDialogController`) has no tap-to-open. Box scores open only from your own schedule (`TeamHome.java:119`, `GameScheduleList.java:70`). There's no view for the V2 detailed stats (`getGameSummaryStrV2`) | desktop `ScoreboardPanel.java:115,264` | P1 |
| P8 | Mock draft | Only inside the begin-recruiting dialog (`RecruitingOptionsDialogController.java:54`). Not in the drawer | desktop View menu `LeagueHomeView.java:491` | P2 |
| P9 | Program dashboard cards (finances/NIL, prestige delta, recruiting pipeline) | Data is scattered across rankings, the home text and the career summary. No dashboard cards (Wave 3's planned 11-card stack) | desktop `DashboardPanel.java:106-118` | P2 |
| P10 | Playbook scheme fit | Shows the `schemeFitNote` text (`TeamStrategyDialogController.java:60`). No fit colouring and no slow-QB read-option warning (`starterQbSpeed`) | desktop `PlaybookDialog.java:221` | P2 |
| P11 | FCS promotions | Available in Settings (`SettingsDialogController.java:92`), not in new-dynasty setup. No "N used" count | desktop `NewGameWizard.java:172` | P2 |
| P12 | Custom-universe / CSV import mid-career | Import runs only when a custom league is launched (`MainActivity.java:273`) | desktop File menu `LeagueHomeView.java:315-326` | P2 |

### At parity (no action)
These are present on Android and use the same engine APIs:
- Job offers / coach market / carousel
- Contracts and retirement
- Coach program and NIL
- Discipline
- Transfer portal
- Season awards
- Hall of Fame and records
- Polls
- Bowl Watch / 12-team bracket (shared `getBowlGameWatchStr`, so recent engine fixes show up automatically)
- Player search
- Coach database
- Depth chart
- Save slots and export
- Practice focus
- Signing day and recruiting
- Bulk sim over the 12-game slate

Weather/momentum widgets and a realignment history screen are missing on **both** shells; weather appears only in the game log.

Android also has two features desktop doesn't: the per-player development plan (`PlayerDevelopmentDialogController`) and the Top 25 history archive (`HistoryDialogController.java:26`).

---

## 4. Quality, tests, CI

### G9 — No Android-side tests (P1)
- No `src/androidTest`, no instrumentation tests, and no unit tests under `src/test/java` for any
  `antdroid.*` or `ui.*` class. Parsers that were worth testing live in `simulation`
  (`WeekDigestViewTest`). Adapters still split engine strings by `&`/`!!` positionally with
  no test coverage.
- `preview/android-snapshots/` has only a `before/` set. The Wave 6 "after" baseline was never
  captured because it needs an emulator.
- Work: Robolectric (or plain JUnit on extracted `RowModel` parsers) for each adapter's string
  protocol. One Espresso smoke test covering new league → pick team → play week → save → reload.
  Capture the snapshot baseline with `scripts/android_snapshots.py`.

### G10 — `./gradlew test` compile failure: verify (P1)
- `AGENTS.md` says `:app:testDebugUnitTest` fails to compile because desktop Swing tests leak
  into the Android test source set. `app/build.gradle` now has `exclude 'desktop/**'` on
  `test`, and no test outside `src/test/java/desktop/` imports `desktop.*`, Swing or AWT, so
  this may already be fixed. Re-run on an SDK machine, then update `AGENTS.md` or fix the
  remaining leak. Until then the CI `android` job (`./gradlew test`) can't be trusted as a gate.
- `simulation.GameBoxScoreTest.boxScore_overtimeScoreIsRecorded` is flaky (unseeded). Seed it.

---

## 5. Accessibility & localization (G11, P2)
- 26 `contentDescription` attributes across all layouts. Crest/sprite `ImageView`s and
  icon-only buttons are mostly unlabeled for TalkBack. Rating tiles and grade pills convey
  meaning through colour alone.
- 82 `setText("…")` / `setTitle("…")` / `makeText(…, "…")` literals in `antdroid/` and `ui/`, and
  most user-visible copy comes from engine strings. There are no `values-*` locale folders. Localization
  is an explicit non-goal, but literals should still move to `strings.xml` for consistency and
  lint.
- Font scaling: large-font-safe buttons were done in Wave 6. List rows with fixed-width mono columns
  haven't been checked at 200% font scale.

---

## 6. UI redesign leftovers (G12, P2)
Items the redesign plan still lists as open, recounted on this tree:

| Item | Plan target | Today |
|---|---|---|
| RecyclerView migration of list pages | all list pages | **0** RecyclerView uses (`ListView` + adapter swap) |
| Custom `View` components (§4: RatingBadge, StarGauge, PlayerSpriteCard, …) | 8 | **0** |
| `Dialogs` facade / no raw builders (grep-enforced) | 0 raw | **81** `AlertDialog.Builder` sites |
| dp/sp literals → tokens | none outside tokens | **514** literal dp/sp in layouts |
| Light-theme audit per screen | every screen, both themes | not done (needs emulator) |
| Snapshot re-baseline | `preview/android-snapshots/after` | missing |
| Component gallery debug screen | reachable from Home | missing |

## 7. Maintainability (G14, P2)
- `MainActivity.java` is 2,529 lines. It owns league lifecycle, save/load, bulk sim, first-run
  wizard, menus, drawer routing, audio and recruiting handoff. G2–G5 all live here.
  Splitting out a `LeagueSession` (owns `League` and the game-thread executor, plus save/autosave)
  would make the P0/P1 fixes above much smaller and testable on the JVM.

---

## Suggested order
1. **G1 + G7** together: the API 36 bump forces the inset work anyway.
2. **G2 + G3**: autosave slot plus restore-on-recreate. Add the `configChanges` stopgap first,
   since it's a one-line change.
3. **G4 / G5 / G6**: one game-thread executor, file-based recruiting handoff.
4. **G10 → G9**: get a trusted Android test gate, then add the smoke and parser tests.
5. **G8** parity: P1 (gameday coaching) and P2 (Offseason Hub) first, then P7 (box scores); then G11–G14.

---

## Follow-up fixes (841ed8e) and how to verify them

| Gap | What changed |
|---|---|
| G1 | `compileSdk`/`targetSdk` 36. `android.suppressUnsupportedCompileSdk=36` because AGP 8.7 was tested only up to 35; bump AGP when convenient. CI installs `platforms;android-36` |
| G2 | `MainActivity.onSaveInstanceState` queues an autosave and records its timestamp. On recreation, `checkRestoreFromAutosave` waits for queued writes and resumes via `LaunchRequest.resumeAutosave()` if the file is at least that new. Otherwise, or if the file is unreadable, it falls back to the launch intent. `configChanges` adds `screenLayout|smallestScreenSize|uiMode` |
| G3 | Autosave after every played week and at the end of bulk sims (`autosave.cfb`, not a numbered slot). Home → Load shows **Resume Autosave** |
| G4 | Slot saves: `League.serializeSave()` on the UI thread, then `SaveWriter` (single save thread) runs `LeagueSaveStorage.writeAtomically`. **Still open:** league load/generation in `onCreate` stays on the UI thread |
| G5 | `prepareSelectedTeam` posts to the UI thread instead of a raw `Thread` |
| G6 | `GameNavigation.createRecruitingIntent` writes `recruitingPayload.txt` and passes its name. Falls back to the extra if the write fails |
| G7 | `PlatformUiHelper.hideSystemUI` and dialogs use `WindowInsetsControllerCompat`. `applySafeInsets` pads every activity for system bars and display cutouts |

Device checklist (API 36 emulator plus one API 24–28 device):
1. New league → pick team → name coach → play 2 weeks → toggle system dark mode → still the same week. Then Developer options → "Don't keep activities" → background and return → still the same week.
2. Same with `adb shell am kill antdroid.cfbcoach` while backgrounded.
3. Save to a slot, and Save & Exit: snackbar appears and the slot loads back.
4. Bulk sim → background mid-run → return: no crash, and the league resumes.
5. Late-dynasty recruiting (10+ seasons): recruiting opens, and Done returns to MainActivity.
6. Notched device in landscape plus 3-button navigation: toolbar, drawer and bottom controls are not covered. Dialogs stay immersive.
7. Home → Load → Resume Autosave.

