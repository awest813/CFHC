# CFHC Ship-Readiness Audit & Plan

> Full audit of engine mechanics, Android app, desktop shell, and build/CI/release
> infrastructure, performed September 2026 against the tree at `6a7db91` (+ dirty working tree).
>
> **Verdict:** the game is feature-complete and mechanically sound — no stub features exist on
> any platform. What stands between this tree and a shippable release is **release engineering**
> (signing key, clean commit, tagged release process) plus a short list of **player-noticeable
> polish bugs**. Nothing requires design work or new features.

## Progress

| Phase | Status |
|-------|--------|
| 0 — Freeze the tree | ✅ Done — feature batch committed (113 files: weather/momentum/interactive gameday coaching + tests + art); `quickVerify` gate run on the committed content. **Release identity confirmed as v1.4.5 / versionCode 320 / desktop 1.4e** — GitHub shows no releases and no tags, so v1.4.5 was never published; everything (README, release notes, jar name, `DesktopVersion`) already agrees on that identity. Contingency: if 320 was already uploaded to the Play Console, bump to 321 at release time. |
| 1 — Release engineering | ✅ Done — release keystore generated (`C:\Users\allen\cfhc-signing\cfhc-release.jks`, 4096-bit RSA, alias `cfhc`; credentials in gitignored `signing.properties`, template tracked). Debug-signed release packaging now **fails fast** unless `-PallowDebugSignedRelease` (CI-only escape hatch). Signed APK/AAB built and verified: real-keystore signature (`CN=CFHC Release`), `mapping.txt` produced, all 8 `getIdentifier()`-resolved engine strings present **with correct values** in the shrunk APK (`res/raw/keep.xml` belt-and-braces added). **Emulator smoke test passed**: release APK on API 35 AVD — launch → new-league wizard → full 130-team league generation → dashboard, all with real names (player name pools confirmed via generated coach name). Stress suites ran green via the new `-PrunStress` gate: `Stress100SeasonsTest` + `Debug20SeasonsFlowTest`, 0 failures (~20 min). `release.yml` workflow added (v* tag → signed AAB/APK + desktop jar → GitHub Release) with `docs/release-runbook.md`. Pushed to `origin/master`; **both CI jobs green** (also fixed a CI breakage en route: Google removed the obsolete `tools` SDK package, so `setup-android` failed on every commit until it was dropped from the packages list). **Remaining (user-gated):** add the 4 CI secrets (`CFHC_KEYSTORE_BASE64`, `CFHC_STORE_PASSWORD`, `CFHC_KEY_ALIAS`, `CFHC_KEY_PASSWORD`), then tag `v1.4.5` → release workflow publishes → verify in-app update check (steps in `docs/release-runbook.md`). |
| 2 — Sim fixes (S1–S2) | ✅ Done — marquee flags (`seniorDay`/`homecomingGame`/`rivalryGame`) now persist in `GameRecord` GM lines (backwards-compatible parse, old 8-field lines load with flags false) and are re-applied by `restoreScheduledGames`; `Weather.fumbleMultiplier()` is wired into both fumble rolls (pass + run). New `MarqueeFlagPersistenceTest` (4 tests incl. legacy-line compat) and a multiplier test in `GameEnvironmentTest`. `BowlTieInTest` made structurally robust: it pins the legacy 4-team playoff, because the expanded format legitimately auto-bids up to five champions into the CFP, which made its assertion season-luck (surfaced as a beta-variant failure after the fumble change shifted that seed's outcomes). |
| 3 — Android fixes | ✅ Done — removed unused legacy storage permissions (A6); BGM/SFX mutes persist via `cfhc_home` prefs and re-apply on launch (A4); Export menu item hidden outside preseason instead of refusing after tap (A5); explicit `backup_rules.xml` + `data_extraction_rules.xml` (saves/exports participate in cloud backup and device transfer, A7); adaptive launcher icon (`mipmap-anydpi-v26` + round + inset foreground PNGs generated from the existing crest, A9); default uncaught-exception handler logs via `PlatformLog` then delegates to the system flow (A8); bulk-sim toast now reports the 60-week cap instead of stopping silently (A3). A1/A2 (threading + ANR surgery) intentionally deferred as higher-risk refactors. `lintDebug` clean. |
| 4 — Desktop fixes | ✅ Done — `rebuildStatusBar()` disposes the old `DesktopStatusFooter` (stops the leaked 50 ms equalizer timer, D1); launch failures (`fail()`, new-league, play-mode catches) now surface a Swing error dialog, headless-safe (D3); new-game league creation and open-save parsing run on a worker thread behind an application-modal indeterminate wait dialog instead of freezing the EDT (D2). D4/D5 (modal stacking, soundtrack decode) remain deferred as tracked. |
| 5 — Test hardening | 🔲 |
| 6 — Release checklist | 🔲 |

---

## 1. Audit findings

### 1.1 SHIP BLOCKERS — must fix before any release build

| # | Finding | Evidence |
|---|---------|----------|
| B1 | **Dirty tree: 124 uncommitted paths.** 8 new production source files are untracked (`Momentum.java`, `Weather.java`, `SimRandom.java`, `GameCoachPlan.java`, `GameCoachListener.java`, `TeamMoraleSnapshot.java`, `desktop/CoachDecisionDialog.java`, `desktop/OffseasonHubDialog.java`) and modified files depend on them — HEAD itself no longer compiles. 8 new test files and `scripts/verify_assets.py` are also untracked. | `git status` |
| B2 | **No release keystore exists.** `app/build.gradle:18-31` reads `CFHC_KEYSTORE*` env vars and **silently falls back to the debug keystore**, so `assembleRelease` produces a Play-ready-looking APK signed with the debug key. Uploading that would permanently bind the Play app identity to a debug keystore. | `app/build.gradle:18-31`; no `*.jks`/`*.keystore` on disk |
| B3 | **Release build never exercised.** `minifyEnabled` + `shrinkResources` are on; `AndroidResourceProvider` resolves engine resources by name via `getIdentifier()` at runtime. Nothing automated tests a minified build — if the resource shrinker strips name-matched XMLs, league generation renders `[teams]` placeholders and the whole game breaks. | `app/build.gradle:74-77`, `AndroidResourceProvider.java:21,30` |
| B4 | **No git tags, no release workflow.** `git tag` is empty; there is no `release*.yml`. `DesktopUpdateChecker` (Help → Check for Updates) polls GitHub Releases that have never existed, so the updater's tag/asset conventions are unverified against a real release. | `DesktopUpdateChecker.java:20-32`, `.github/workflows/` |

### 1.2 ENGINE MECHANICS — verified solid

Every README feature claim traced to a real, wired implementation: play-by-play, weather/
momentum/penalties/icing/halftime adjustments, interactive coaching (pregame/halftime/crunch
checkpoints all consumed in play logic), 6 off + 5 def playbooks, awards (Heisman → All-American),
progression (season + mid-season + practice focus), bowls with prestige-ordered tie-ins, expanded
12-team playoff, AP/Coaches polls, prestige, infractions with postseason bans, rivalries +
trophies, senior day/homecoming, realignment V2 + history, coaching carousel/hot seat, transfer
portal (in/out/unmatched), draft night, signing day, redshirts incl. medical, full recruiting
cycle with needs-based CPU recruiting and roster limits, CoachSkills (all 5 branches consumed),
contracts/facilities/NIL, CSV import, atomic versioned saves with loud schema failure.
Zero TODO/FIXME/"not implemented" strings in the engine; the only `XXX` hits are record-holder
sentinels replaced after the first season.

### 1.3 PLAYER-NOTICEABLE SIM BUGS — fix before release

| # | Finding | Evidence | Fix |
|---|---------|----------|-----|
| S1 | **Marquee-game flags lost on save/load.** `seniorDay`/`homecomingGame`/`rivalryGame` are not serialized by `GameSerializer`, and re-tagging only happens on a new schedule build — not on load. After a mid-season save/load, remaining rivalry/senior-day/homecoming games lose the crowd modifier, banners, attendance bonus, marquee news, and (for rivalries) trophy bookkeeping. | `GameSerializer.java:19-82`, `Game.java:97-99`, `ScheduleManager.java:241-281` | Persist the 3 booleans per scheduled game, or re-run `tagMarqueeGames` on load |
| S2 | **`Weather.fumbleMultiplier()` is dead code** — javadoc claims weather feeds fumble rates; nothing calls it. Either wire it into the fumble roll or correct the javadoc. | `Weather.java:83` | Wire it (small, adds fidelity) or fix docs |
| S3 | **CPU never calls defensive timeouts** — timeouts only used for kick-icing, the trailing-Q4 bank, and user crunch burns. A player watching a CPU comeback notices the missing behavior. Design gap, not a bug. | `Game.java:465-467, 2860-2872` | Optional; small addition to `quarterCheck` |

### 1.4 ANDROID — should-fix before release

| # | Finding | Evidence |
|---|---------|----------|
| A1 | `prepareSelectedTeam` mutates `League` on a raw background thread — violates the single-thread contract (mitigated only by the modal prep dialog; the bulk simulator next to it shows the correct pattern). | `MainActivity.java:576-622`, `docs/THREADING.md` |
| A2 | Save + full league load run on the UI thread — classic ANR on slow storage/deep dynasties. | `MainActivity.java:479-519, 1629-1642, 1034-1036` |
| A3 | Bulk-sim: progress dialog leaked on rotation; `played < 60` cap silently truncates long "Sim to Next Decision" runs. | `MainActivity.java:1356-1435` |
| A4 | Sound (BGM/SFX) mute settings not persisted — reset every launch. Theme is persisted, so users will expect parity. | `SettingsDialogController.java:224-246` |
| A5 | Export menu item visible year-round but toasts "disabled, preseason only" — hide/disable outside week 0. | `MainActivity.java:1683-1690`, `menu_main.xml` |
| A6 | Legacy `WRITE_/READ_EXTERNAL_STORAGE` declared but no code requests them (all I/O uses app dirs) — remove for Play permissions review. | `AndroidManifest.xml:8-15` |
| A7 | `allowBackup="true"` with no backup rules — decide dynasty-save device-transfer behavior explicitly. | `AndroidManifest.xml:19` |
| A8 | No uncaught-exception handler; a mid-season crash loses unsaved progress with no diagnostics. | `CfhcApplication.java` (empty) |
| A9 | No adaptive icon — legacy PNGs white-plaque on modern launchers. | `res/mipmap-*` |
| A10 | Deprecated `setSystemUiVisibility` under targetSdk 35 edge-to-edge — verify no content under system bars on an API 35 device. | `PlatformUiHelper.java:100-109`, `ImmersiveDialogHelper.java:26-31` |
| A11 | Recruiting payload passed via Intent String extra — grows with roster/board size toward the ~1MB Binder ceiling deep in a dynasty. Pass the saved checkpoint key instead. | `MainActivity.java:1032-1036`, `GameNavigation.java:45` |

### 1.5 DESKTOP — should-fix before release

| # | Finding | Evidence |
|---|---------|----------|
| D1 | **Soundtrack footer timer leak** — `rebuildStatusBar()` replaces `DesktopStatusFooter` without `dispose()`; its 50 ms Swing timer only stops on quit. `refresh()` runs after every week, so a season leaks dozens of repainting timers. | `LeagueHomeView.java:1888-1893`, `DesktopStatusFooter.java:111-112` |
| D2 | League construction/load runs on the EDT with no busy indicator (New Game Wizard + open-save), while `Main.launchPlayMode` correctly uses a worker — inconsistent multi-second freezes. | `NewGameWizard.java:226-249`, `LeagueHomeView.java:1376-1391` |
| D3 | GUI launch failures are silent (`System.err.println` only) — double-clicked jar or macOS file association with a bad save shows nothing. | `Main.java:141-144, 175-178` |
| D4 | Modal dialog parade at week/bulk boundaries (documented, deferred — the worst desktop UX). | `DesktopUiBridge.java:66-75`, `docs/desktop-gameplay-polish.md:15` |
| D5 | Soundtrack decode on the EDT — visible hitch on slower machines. | `DesktopSoundtrackEngine.java:136-164` |

### 1.6 BUILD / CI / TESTS — should-fix

| # | Finding | Evidence |
|---|---------|----------|
| T1 | No Android instrumentation/UI tests at all; all 126 test files are JVM JUnit. | no `androidTest/`, no espresso deps |
| T2 | `Stress100SeasonsTest` / `Debug20SeasonsFlowTest` excluded from every CI gate — run on demand only. | `app/build.gradle:136-142`, `desktop-standalone/engine/build.gradle:342-345` |
| T3 | SDK-free desktop gate skips `positions/` and `staff/` tests (19 files) — they only run in the Android CI job. | `desktop-standalone/engine/build.gradle:40-48` |
| T4 | Version strings live in 3 hand-synced places (Android `v1.4.5`/320, `desktopVersion=1.4e`, `DesktopVersion` fallback); `bumpVersion` only bumps Android. | `app/build.gradle:64-65`, `gradle.properties:9`, `DesktopVersion.java:25` |
| T5 | No `org.gradle.jvmargs` — R8 release builds on default heap. | `gradle.properties` |
| T6 | `app/lint.xml` exists but no `lint {}` block references it; verify its baselines are current. | `app/build.gradle` |

Deferrable: GraphView 4.2.2 (only runtime dep, no CVEs), AGP/Gradle one minor behind, bundled
audio-SPI jars old-but-standard, root clutter (`threads_report.txt`, `implementation_plan.md`,
`ui_redesign_plan.md`), tracked 2018 `captures/` file, no Dependabot/verification metadata.

### 1.7 Verified working (no action)

- **Android:** all 22 drawer destinations + menu items wired to real handlers; save/load 10-slot
  flow, bulk sim runner (THREADING-compliant), BGM + 11 SFX wired, recruiting screen fully
  functional, settings all applied with mid-season lock rules, in-app tutorial, no memory leaks,
  no placeholder strings, manifest clean (only launcher exported, no ads/GMS).
- **Desktop:** all 14 LeagueScreen panels data-driven (no stubs); save/dirty-tracking/exit
  prompts, recruiting checkpoint sidecar, update checker, jpackage/zip/dmg/msi tasks with jar
  gate; EDT contract honored in bulk sim; all required resource-contract files present.
- **Tests:** ~530 real assertions-based tests; critical paths covered (full season, bowl tie-ins,
  playoff, save round-trip + corrupt saves + schema failure, recruiting cycle, CSV import,
  golden save fixture).
- **Docs:** `desktop-fix-list.md` all 18 items done; parity is far better than the README
  disclaimer suggests.

---

## 2. The plan

### Phase 0 — Freeze the tree (blocker B1) · ~half a day
1. Review + commit all 124 dirty paths (they are one coherent feature batch: weather/momentum/
   interactive-coaching + parity work). Confirm `git status --short` is empty and HEAD compiles.
2. Decide the release version now (bump Android to next patch and sync `desktopVersion`) so all
   later builds use the final identity.

### Phase 1 — Release engineering (blockers B2–B4) · 1–2 days
1. Generate + secure the release keystore (`keytool -genkeypair`, keep `.jks` untracked —
   `.gitignore` already covers it); export `CFHC_KEYSTORE*` env vars; **make the debug fallback
   fail-fast for `assembleRelease`** so a wrong-signed APK can never be produced silently.
2. Build `:app:assembleRelease`/`bundleRelease` and **smoke-test the minified APK on a device**:
   new-league generation must show real team/conference/player names (R8/resource-shrink check).
   Add `tools:keep` for the engine resource XMLs as belt-and-braces.
3. Run the stress suites once (`Stress100SeasonsTest`, `Debug20SeasonsFlowTest`) and the full
   `quickVerify` gate; push and get both CI jobs green.
4. Add a `release.yml` workflow (or documented manual runbook): tag → build signed AAB/APK +
   desktop jar + portable zip → attach to GitHub Release. Publish one real release and verify
   **Help → Check for Updates** finds it — this is the updater's first-ever live exercise.

### Phase 2 — Sim fixes (S1–S2) · 1 day
1. **S1 marquee flags:** persist `seniorDay`/`homecomingGame`/`rivalryGame` in `GameSerializer`
   (backwards-compatible: default false on old loads), or re-tag on load if the schedule is
   intact. Add a save/load round-trip test asserting a mid-season save preserves an unplayed
   rivalry game's flag.
2. **S2 weather fumbles:** wire `fumbleMultiplier()` into the Game fumble roll + test, or fix the
   javadoc. Wiring is preferred — it's a two-line change with existing weather test patterns.
3. Optional (S3): CPU defensive timeouts in `quarterCheck` — small, safe, high realism payoff.

### Phase 3 — Android pre-release fixes (A1–A9) · 2–3 days
Priority order: A6 (remove unused permissions — Play review), A4 (persist sound settings),
A5 (hide export outside preseason), A7 (backup rules: exclude nothing sensitive or set
`dataExtractionRules`), A9 (adaptive icon), A8 (default crash handler), A3 (bulk-sim cap message
+ dialog leak), A1 (move `prepareSelectedTeam` onto the bulk-sim threading pattern), A2 (progress
dialog around save/load). A10/A11: verify on API 35 device; fix A11 only if payload sizes approach
the limit. Then run `:app:lintDebug` and a manual pass through every drawer destination on a device.

### Phase 4 — Desktop fixes (D1–D3) · 1 day
D1 footer timer dispose (small, real leak), D3 error dialogs on launch failure (small), D2 worker +
progress for league build/load (moderate). D4/D5 (modal stacking, EDT decode) can trail into a
post-release patch if time-boxed.

### Phase 5 — Test hardening (T1–T6) · 1–2 days
1. Add `positions/` + `staff/` to the standalone desktop gate (T3) — removes a silent blind spot
   from the SDK-free path.
2. Wire the two stress suites into a scheduled/nightly CI job instead of full exclusion (T2).
3. Add `org.gradle.jvmargs=-Xmx2g` (T5); reference or delete `app/lint.xml` (T6).
4. Centralize the version: make `bumpVersion` also update `desktopVersion` + `DesktopVersion`
   fallback, or read all three from one property (T4).
5. Optional: one instrumented smoke test (launch → new league → advance a week) on the emulator
   CI job (T1).

### Phase 6 — Release checklist (executes Phases 0–5, then)
1. `./gradlew bumpVersion -Ppatch` + sync desktop version + README jar-name references.
2. `./gradlew quickVerify` green; both CI jobs green on the release commit.
3. Signed `bundleRelease` (AAB) + `assembleRelease` APK (verify signature, not debug-signed) +
   `desktopStandaloneGate` jar + `desktopPortableZip`.
4. Manual device pass: new league → season week with interactive decisions → recruiting →
   save/load mid-season → playoff → offseason → Check for Updates. Same flow once on desktop.
5. Tag (`v1.4.6` style or the updater's preferred `desktop-` convention — pick one and document
   it), publish the GitHub Release with notes (extend `docs/desktop-release-notes-1.4e.md`),
   then confirm the in-app updater detects it.

**Total estimate: ~6–9 focused days to a fully shippable release.** Phases 0–2 are the
non-negotiable core (clean tree, real signing, verified release build, marquee-flag fix);
Phases 3–5 can be trimmed to their priority items if the release date is fixed.
