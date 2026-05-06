# Cleanup Audit

## Phase 1: Baseline

- [x] Captured Git status on `master` against `origin/master`.
- [x] Confirmed existing local edits are still present and should be reviewed before any broad refactor commit:
  - `docs/ROADMAP.md`
  - `src/main/java/antdroid/cfbcoach/DepthChartDialogController.java`
  - `src/main/java/antdroid/cfbcoach/MainActivity.java`
  - `src/main/java/positions/Player.java`
  - `src/main/java/simulation/Game.java`
  - `src/main/java/simulation/League.java`
  - `src/main/java/simulation/Team.java`
  - `src/main/java/staff/Staff.java`
  - `src/test/java/simulation/SaveRoundTripTest.java`
  - `src/main/java/simulation/LeagueStats.java`
  - `src/main/java/simulation/TeamStats.java`
- [x] Ran baseline build: `.\gradlew.bat test assembleDebug desktopJar`.
- [x] Built Android debug APK: `build/outputs/apk/debug/CFHC-debug.apk`.
- [x] Built desktop jar: `build/libs/CFHC-desktop-prototype.jar`.
- [x] Launched desktop `new` flow from the jar and confirmed the Java process stayed responsive.
- [ ] Decide ownership for each dirty file: intentional cleanup, stale experiment, or ready-to-commit change.

## Phase 2: Debug And Stabilize

- [x] Re-ran full test/build suite after baseline capture.
- [x] Added focused Android-free regression coverage for schedule/OOC encapsulation and injury/suspension cleanup.
- [x] Ran `clean test assembleDebug desktopJar` before committing the cleanup pass.
- [x] Reviewed compiler warnings from a non-up-to-date clean build and fixed unchecked generic warnings that indicated type-safety risk.
- [ ] Add or expand regression tests for:
  - [x] Save/load round trip
  - [x] Season advance
  - [x] Scheduling/OOC mutators
  - [x] Recruiting session preparation
  - [ ] Transfers
  - [x] Player injury/suspension cleanup
  - [ ] Player progression invariants
  - [ ] Desktop launch/load flows beyond facade-level save import/load
  - [ ] Android-safe core smoke paths beyond build + shared facade tests

## Phase 3: Gameplay Audit

For each gameplay loop, verify expected behavior, capture edge cases, add or fix tests, and polish UI text/flow when the behavior is understood.

### New Game Setup

- [x] Team selection: covered by `SimulationFacadeTest.loadDefaultLeague_setsLeagueAndSeasonController` and `setLeague_assignsFacadeUserTeamToLeague`.
- [ ] Coach creation: verify Android and desktop flows create a usable head coach profile with valid ratings, contract, name, and user-controlled team link.
- [x] Default settings: covered at the shared-engine level by default `League` construction in full-season and facade tests.
- [x] Custom universe import: covered by `LeagueCustomDataImporterTest` and `LeagueImportWorkflowTest`; still needs player-facing Android/desktop UI smoke.
- Edge cases to audit: missing/duplicate team names, invalid custom CSV rows, no selected user team, blank coach name, unsupported prestige mode.
- Polish pass: keep startup errors actionable and use the same naming for "new game", "custom universe", and "coach" across Android and desktop.

### Season Loop

- [x] Schedule generation: covered by `FullSeasonTest` and save/load schedule round-trip tests.
- [x] Rankings: indirectly covered by full-season advance; needs explicit ranking invariant tests.
- [x] Weekly advance: covered by `SeasonControllerResultTest`.
- [x] Game simulation: covered by full-season smoke; needs focused box-score/stat invariants.
- [ ] Box scores: verify final score, quarter score, stat rows, and empty stat groups after sim/load.
- [ ] Standings: verify conference/division records and sort order after a controlled schedule.
- [ ] Awards: verify awards are generated once and persist through save/load.
- [x] Postseason: full-season smoke verifies a champion is crowned.
- Edge cases to audit: BYE weeks, teams with short schedules, overtime, postseason games after save/load, rankings before any games are played.
- Polish pass: make week/status text consistent between Android home, desktop home, and notification dialogs.

### Roster Loop

- [ ] Depth chart: verify starters/subs remain valid after injuries, suspensions, transfers, and redshirts.
- [x] Redshirts: existing full-season flow touches redshirt stage; needs direct active-player count tests.
- [x] Injuries: covered by save round-trip and `TeamStateRegressionTest.curePlayers_clearsInjuryFlagsAndTrackedInjuryList`.
- [x] Suspensions: covered by `TeamStateRegressionTest.healSuspension_expiresOneWeekSuspension`.
- [ ] Transfers: full-season flow reaches transfer stage; needs deterministic transfer pool/roster tests.
- [ ] Progression: needs direct player progression invariant tests for rating bounds, year advancement, and graduation/removal.
- Edge cases to audit: too few active players at a position, all starters injured/suspended, medical redshirt + injury overlap, graduating transfer players.
- Polish pass: roster status labels should use one vocabulary for injured/suspended/redshirt/transfer on both platforms.

### Recruiting Loop

- [x] Prospect generation: covered by `SimulationFacadeTest.prepareRecruitingSession_buildsPortableRecruitingState`.
- [ ] Scouting: verify scout grade/cost updates and display text after interactions.
- [ ] Scholarships: verify budget decrement, cannot overspend, and scholarship count/roster limits.
- [ ] Commitments: verify recruited players enter the right roster group and persist into the next season.
- [x] Roster limits: `RosterRules` is used by the recruiting session; add focused limit tests if not already present.
- Edge cases to audit: no affordable recruits, full roster, duplicate recruit names, auto-filter removing current list, empty position group.
- Polish pass: recruiting board filters and expand/collapse text should be consistent and easy to scan.

### Career Loop

- [ ] Coach ratings: verify annual rating changes and bounds.
- [ ] Staff hiring: verify OC/DC replacement flows on Android and desktop.
- [ ] Jobs: verify job offers, promotions, and user team reassignment.
- [ ] Firing: verify fired user gets a recoverable flow and league remains playable.
- [x] Prestige: full-season flow touches prestige updates; needs explicit program prestige bounds tests.
- [x] History/records: save/load round trip and full-season history checks exist; needs targeted record update tests.
- Edge cases to audit: no available staff, user fired after championship/offseason event, conference realignment plus job change, record ties.
- Polish pass: career messages should be concise, clear, and not platform-specific in shared presentation text.

### Save/Load

- [ ] Old saves: collect known legacy saves and add compatibility fixtures.
- [x] New saves: covered by `SaveRoundTripTest` and `SimulationFacadeTest.saveToSlot_usesSharedSaveLoadService`.
- [x] Desktop portability: covered by `SimulationFacadeTest.importSave_selectsUserTeamForPortableSave`.
- [ ] Android storage behavior: build verified; needs emulator/device save slot smoke.
- Edge cases to audit: names with commas, missing user team, saved postseason game, imported save with custom universe resources.
- Polish pass: save/load errors should explain whether the file is invalid, unsupported, or missing resources.

## Phase 4: Cross-Platform Audit

- [x] Confirmed `simulation`, `positions`, and `staff` have no Android, AndroidX, Swing, AWT, `antdroid`, or `desktop` imports.
- [x] Confirmed desktop package has no Android or `antdroid` imports.
- [x] Confirmed Android packages `antdroid` and `ui` have no Swing/AWT/desktop imports.
- [ ] Split Android-only recruiting UI classes out of the `recruiting` package:
  - `recruiting.RecruitingActivity`
  - `recruiting.RecruitingDialogController`
- [x] Confirmed portable recruiting classes remain shared by desktop/Android-facing tests:
  - `RecruitingController`
  - `RecruitingPresentation`
  - `RecruitingSessionData`
  - `RecruitingPlayerRecord`
- [x] Replaced new Android dependency leakage: none found outside the known recruiting UI classes.
- [x] Verify `assembleDebug`.
- [x] Verify `desktopJar`.
- [x] Verify `runDesktop -PdesktopArgs="new"`.
- [x] Verify a save created through the shared engine can be loaded through the shared engine/facade where intended.

## Phase 6: Gameplay Polish

- [x] Replaced placeholder Android activity titles and tutorial copy with player-facing labels.
- [x] Clarified Android depth chart instructions, save/close actions, and recruiting roster-needs wording.
- [x] Added small-screen clipping guards to Android schedule, recruiting header/chips, recruiting actions, and box-score header rows.
- [x] Tightened Android box-score labels and stat headings for scanning.
- [x] Harmonized desktop postseason/offseason wording and clarified schedule/depth-chart labels.
- [x] Improved desktop recruiting and box-score empty states.
- [ ] Run emulator/device visual smoke for small-screen Android clipping.
- [ ] Do a hands-on desktop pass for keyboard traversal and dialog resizing beyond compile/build verification.

## Findings

- Current baseline is green, but the worktree is intentionally not clean.
- Unchecked compile warnings were reduced to broad deprecation notes after typing saved-game stat lists and the recruiting position adapter.
- Android device/emulator launch was not part of this pass; APK build is verified.
- Desktop launch is verified by process responsiveness, not UI automation.
- Phase 4 boundary scan found Android dependencies only in known Android UI packages plus `recruiting.RecruitingActivity` and `recruiting.RecruitingDialogController`.
- Gradle `runDesktop -PdesktopArgs="new"` launched `desktop.Main new` and stayed responsive; launched CFHC desktop processes were stopped afterward to avoid build file locks.
- Phase 6 static polish reduced obvious placeholder/debug copy and added clipping guards, but visual device/emulator QA is still outstanding.
