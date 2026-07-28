# Desktop App Fix List

Prioritized audit of the Swing desktop shell (branch work starting from merge-all).  
Status key: 🔲 open · 🔄 in progress · ✅ done

---

## Critical

1. ✅ **User discipline never applied**  
   After `playWeek()`, Android runs `suspendPlayerSetup` when `userTeam.disciplineAction` is set. Desktop only shows an info dialog and never calls `Team.disciplineAction(...)`.  
   Files: `LeagueHomeView.java`, `DesktopUiBridge.java`, `DisciplineDialog.java`

2. ✅ **Bulk advance silently skips career decisions**  
   `setSuppressBlockingUi(true)` makes contracts / jobs / promotions / coordinators / redshirt / transfers / midseason / awards no-ops (transfers may auto-decline).  
   Files: `LeagueHomeView.java`, `DesktopUiBridge.java`  
   **Fix:** informational prompts stay suppressed; career decisions use `invokeAndWait` on the EDT.

---

## High

3. ✅ **Default save directory unused**  
   `DesktopAppPaths` (`~/.cfhc/saves`) exists but save/load still start in arbitrary cwd.  
   Files: `DesktopAppPaths.java`, `LeagueHomeView.java`, `LauncherFrame.java`

4. ✅ **In-season recruiting progress ephemeral**  
   Docked board warns progress won’t survive close; no checkpoint into save.  
   Files: `LeagueHomeView.java`, `RecruitingPanel.java`, `DesktopRecruitingCheckpoint.java`

5. ✅ **Packaging metadata** (unsigned jpackage image / portable zip; signed MSI/DMG blocked)  
   `desktopJpackageImage` carries `--app-version` / `--vendor` / `--description`; jar is `CFHC-desktop-<desktopVersion>.jar`.  
   File: `desktop-standalone/engine/build.gradle`

6. ✅ **Drop prototype/alpha labeling**  
   Main banner + jar naming cleaned up. ROADMAP #19 (graduate desktop) remains open for full release polish.  
   Files: `Main.java`, Gradle jar tasks

---

## Medium

7. ✅ Theme leftovers (file chooser restyle for light+dark; FlatLaf light/dark bundled)
8. ✅ Transfer portal read-only vs Android summary parity (`My Transfers` / `All Transfers` + summary pane)
9. ✅ Redshirt UX copy/expectations vs Android (season list + auto-redshirt note)
10. ✅ Further split `LeagueHomeView` — extracted `DesktopBulkSimulator` for bulk advance
11. ✅ Bulk sim threading vs single-thread engine contract — advances hop to EDT via `invokeAndWait`
12. ✅ Desktop tests for discipline/bulk gates, theme, recruiting checkpoint, transfer summary
13. ✅ Keep shells on `SimulationFacade` for import APIs — coach/roster CSV via facade

---

## Low

14. ✅ Accessibility (high-contrast + table/filter accessible names across main screens)
15. ✅ Auto-update (manual Help → Check for Updates via GitHub Releases) / portable zip+JRE (`desktopPortableZip`)
16. ✅ macOS `.dmg` / Windows `.msi` unsigned jpackage tasks (`desktopDmg` / `desktopMsi`; signed builds remain cert-blocked). Aqua menus / file association still open.
17. ✅ Audio OGG SPI packaging (vorbisspi + jorbis + tritonus-share in desktop jar; fail-soft on decode errors)
18. ✅ FlatLaf light/dark LAF (bundled `libs/flatlaf-*.jar`; system LAF fallback)

---

## Already solid

- SeasonController week flow + docked recruiting NLI gate  
- Career dialogs on Play-Week path (when not suppressed)  
- Dark theme v1, keyboard shortcuts, depth chart, playbook  
- Save/load/export + CSV import; `desktopStandaloneGate` CI  
- `DesktopBulkSimulator` EDT hops for bulk advance  
- `DesktopRecruitingSessionStore` for checkpoint lifecycle  

---

## Start order (this pass)

1. ✅ Discipline parity (Critical #1)
2. ✅ Bulk advance stop-at-dialog policy (Critical #2)
3. ✅ Wire `DesktopAppPaths` for save/load (High #3)
4. ✅ Recruiting checkpoint (#4)
5. ✅ Packaging metadata (#5)
6. ✅ Drop prototype/alpha labeling (#6)

Next: continue ROADMAP #4 (ooc / W-L lists, League pools) and #3 god-object slices;
macOS Aqua menus / file associations; signed MSI/DMG remains cert-blocked; true Linux `.AppImage` still open.
LeagueHomeView screens are extracted via `LeagueScreen` (shell polish remains).
Done optional pass: FlatLaf LAF, Help → Check for Updates, `desktopJpackageImage` + `desktopPortableZip`,
unsigned `desktopDmg`/`desktopMsi` scaffolds.
