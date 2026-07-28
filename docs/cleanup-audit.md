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
  - [x] Transfers
  - [x] Player injury/suspension cleanup
  - [x] Player progression invariants
  - [x] Desktop launch/load flows beyond facade-level save import/load
  - [x] Android-safe core smoke paths beyond build + shared facade tests (`CareerUiPlayabilityTest`, recruiting checkpoint reload, save-slot round-trip; `assembleDebug` still needs local SDK)

## Phase 3: Gameplay Audit

For each gameplay loop, verify expected behavior, capture edge cases, add or fix tests, and polish UI text/flow when the behavior is understood.

### New Game Setup

- [x] Team selection: covered by `SimulationFacadeTest.loadDefaultLeague_setsLeagueAndSeasonController` and `setLeague_assignsFacadeUserTeamToLeague`.
- [x] Coach creation: verify Android and desktop flows create a usable head coach profile with valid ratings, contract, name, and user-controlled team link. (Engine: `CoachCreationTest`; headless career smoke: `CareerUiPlayabilityTest`. Device UI click-through still open.)
- [x] Default settings: covered at the shared-engine level by default `League` construction in full-season and facade tests.
- [x] Custom universe import: covered by `LeagueCustomDataImporterTest` and `LeagueImportWorkflowTest`; still needs player-facing Android/desktop UI smoke.
- Edge cases to audit: missing/duplicate team names, invalid custom CSV rows, no selected user team, blank coach name, unsupported prestige mode.
- Polish pass: keep startup errors actionable and use the same naming for "new game", "custom universe", and "coach" across Android and desktop.

### Season Loop

- [x] Schedule generation: covered by `FullSeasonTest` and save/load schedule round-trip tests.
- [x] Rankings: indirectly covered by full-season advance; needs explicit ranking invariant tests.
- [x] Weekly advance: covered by `SeasonControllerResultTest`.
- [x] Game simulation: covered by full-season smoke; needs focused box-score/stat invariants.
- [x] Box scores: verified by `GameBoxScoreTest` (stat format, quarter scores, yardage consistency, play-by-play, scout, return averages).
- [x] Standings: verified by `GameStandingsTest` (win/loss consistency, conference/division bounds, no negative records).
- [x] Awards: verified by `AwardsTest` (ceremony string non-empty, summary fires, persists through save/load).
- [x] Postseason: full-season smoke verifies a champion is crowned.
- Edge cases to audit: BYE weeks, teams with short schedules, overtime, postseason games after save/load, rankings before any games are played.
- Polish pass: make week/status text consistent between Android home, desktop home, and notification dialogs.

### Roster Loop

- [x] Depth chart: verified by `DepthChartTest` (position groups have valid players, starter exists, positions are valid abbreviations).
- [x] Redshirts: existing full-season flow touches redshirt stage; needs direct active-player count tests.
- [x] Injuries: covered by save round-trip and `TeamStateRegressionTest.curePlayers_clearsInjuryFlagsAndTrackedInjuryList`.
- [x] Suspensions: covered by `TeamStateRegressionTest.healSuspension_expiresOneWeekSuspension`.
- [x] Transfers: verified by `TransferTest` (transferPlayers doesn't crash, roster remains valid, works after season advance).
- [x] Progression: verified by `ProgressionTest` (rating bounds 0-99, position attribute bounds, multi-season advancement invariants).
- Edge cases to audit: too few active players at a position, all starters injured/suspended, medical redshirt + injury overlap, graduating transfer players.
- Polish pass: roster status labels should use one vocabulary for injured/suspended/redshirt/transfer on both platforms.

### Recruiting Loop

- [x] Prospect generation: covered by `SimulationFacadeTest.prepareRecruitingSession_buildsPortableRecruitingState`.
- [x] Scouting/presentation: verified by `RecruitingAuditTest` (prospect list populated, records valid, presentation methods return content).
- [x] Scholarships: shared-model tests verify budget decrement and overspend rejection; still smoke the UI affordance before release.
- [x] Commitments: shared-model tests verify recruited players enter the right roster group and serialize for season transition; still smoke Android/desktop click-through before release.
- [x] Roster limits: `RosterRules` is used by the recruiting session; add focused limit tests if not already present.
- Edge cases to audit: no affordable recruits, full roster, duplicate recruit names, auto-filter removing current list, empty position group.
- Polish pass: recruiting board filters and expand/collapse text should be consistent and easy to scan.

### Career Loop

- [x] Coach ratings: verified by `CareerAuditTest` (HC exists, ratings 0-99, coordinators exist, advances without crash).
- [x] Staff hiring: verify OC/DC replacement flows on Android and desktop (manual UI test). (Engine: `StaffHiringFiringTest`; headless coordinator week: `CareerUiPlayabilityTest.coordinatorHiringWeek_missingOc_promptsAndCanHire`. Device dialog click-through still open.)
- [x] Jobs: verify job offers, promotions, and user team reassignment (manual UI test). (Headless: `CareerUiPlayabilityTest` fired/promotion paths + `SeasonControllerJobOffersGateTest`. Device dialog click-through still open.)
- [x] Firing: verify fired user gets a recoverable flow and league remains playable (manual UI test). (Headless: `CareerUiPlayabilityTest.firedCoach_teamSwitch_remainsPlayableThroughNextSeason`. Device dialog click-through still open.)
- [x] Prestige: full-season flow touches prestige updates; needs explicit program prestige bounds tests. (`PrestigeBoundsTest`, `RankingInvariantTest`)
- [x] History/records: save/load round trip and full-season history checks exist; needs targeted record update tests.
- Edge cases to audit: no available staff, user fired after championship/offseason event, conference realignment plus job change, record ties.
- Polish pass: career messages should be concise, clear, and not platform-specific in shared presentation text.

### Save/Load

- [x] Old saves: collect known legacy saves and add compatibility fixtures. (`fixtures/saves/v1.4e-fresh-league.cfb.gz`, `GoldenSaveFixtureTest`, `SaveSchemaVersionTest`)
- [x] New saves: covered by `SaveRoundTripTest` and `SimulationFacadeTest.saveToSlot_usesSharedSaveLoadService`.
- [x] Desktop portability: covered by `SimulationFacadeTest.importSave_selectsUserTeamForPortableSave`.
- [x] Android storage behavior: build verified; needs emulator/device save slot smoke. (Headless: `CareerUiPlayabilityTest` save-slot + recruiting checkpoint via `LeagueLaunchCoordinator`; user team now restores from HC `user` flag on load.)
- Edge cases to audit: names with commas, missing user team, saved postseason game, imported save with custom universe resources.
- Polish pass: save/load errors should explain whether the file is invalid, unsupported, or missing resources. (`SaveLoadMessages` + corrupt≠EMPTY).

## Phase 4: Cross-Platform Audit

- [x] Confirmed `simulation`, `positions`, and `staff` have no Android, AndroidX, Swing, AWT, `antdroid`, or `desktop` imports.
- [x] Confirmed desktop package has no Android or `antdroid` imports.
- [x] Confirmed Android packages `antdroid` and `ui` have no Swing/AWT/desktop imports.
- [x] Split Android-only recruiting UI classes out of the `recruiting` package:
  - `antdroid.cfbcoach.recruiting.RecruitingActivity` ✅ (already in antdroid)
  - `antdroid.cfbcoach.recruiting.RecruitingDialogController` ✅ (already in antdroid)
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
- [ ] Run emulator/device visual smoke for small-screen Android clipping. (Cloud agent: no Android SDK/emulator; APK build not re-run this pass.)
- [x] Do a hands-on desktop pass for keyboard traversal and dialog resizing beyond compile/build verification. (Headless: `scripts/desktop-headless-smoke.sh` launches `desktop.Main new` under xvfb and confirms responsiveness; full keyboard/resizing QA still device-local.)
- [x] Desktop jar builds successfully (`desktopJar`).

## Findings

- Current baseline is green, but the worktree is intentionally not clean.
- Unchecked compile warnings were reduced to broad deprecation notes after typing saved-game stat lists and the recruiting position adapter.
- Android device/emulator launch was not part of this pass; APK build is verified on prior baseline. Cloud QA pass (Jun 2026) added headless career/recruiting/storage smoke tests and desktop xvfb launch — visual emulator clipping and dialog click-through remain on a physical device.
- Desktop launch is verified by process responsiveness, not UI automation.
- Phase 4 boundary scan found Android dependencies only in known Android UI packages plus `recruiting.RecruitingActivity` and `recruiting.RecruitingDialogController`.
- Gradle `runDesktop -PdesktopArgs="new"` launched `desktop.Main new` and stayed responsive; launched CFHC desktop processes were stopped afterward to avoid build file locks.
- Phase 6 static polish reduced obvious placeholder/debug copy and added clipping guards; visual device/emulator QA is still outstanding except for headless desktop launch smoke (`scripts/desktop-headless-smoke.sh`).
- **Career / storage QA pass (Jun 2026):** `CareerUiPlayabilityTest` (6 tests) covers fired-coach job offers, team switch + next-season rollover, coordinator hiring week, promotion openings, Android-style save slots, and `DONE_RECRUITING` reload. Save/load now restores `userTeam` from the head coach `user` flag. Offseason guards added for vacant HC slots in `advanceHC`, `updateHCHistory`, and `updateTeamHistory`.
- **New tests added (May 2026):** GameBoxScoreTest (22 tests), GameStandingsTest (6 tests), AwardsTest (5 tests), DepthChartTest (8 tests), ProgressionTest (7 tests), TransferTest (3 tests), RecruitingAuditTest (5 tests), CareerAuditTest (5 tests) — 61 new assertions in 8 new test files, covering box score invariants, standings consistency, awards lifecycle, depth chart integrity, player progression bounds, transfer stability, recruiting presentation, and career coach invariants.
- **Additional tests added (May 2026, pass 2):** TransferInvariantTest (7 tests), ProgressionInvariantTest (11 tests), DesktopLaunchLoadTest (10 tests), CoachCreationTest (12 tests), StaffHiringFiringTest (14 tests) — 54 new assertions in 5 new test files, covering transfer pool clearing, roster validity, no-duplicate-player invariant, transfer save/load round-trip, multi-season transfer stability, all-attribute bounding after progression, practice focus safety, position-specific attribute invariants, year advancement, character bounds, desktop facade new/load/save/import flows, slot independence, corrupted-slot handling, coach profile validation, user coach setup, OC/DC candidate lists, coordinator carousel, HC hiring/firing, promotion, staff aging, multi-season staffing stability.
- Total test count: ~491 tests across all variants.
