Original prompt: keep working game lements

## 2026-05-07

- Picked up an existing in-progress Java/Android CFHC refactor, focused around game/simulation elements: private `League`/`Team` collections, `TeamStats`/`LeagueStats` extraction, and call-site updates.
- No prior `progress.md` existed.
- Ran `.\gradlew.bat test`; it passed before additional edits.
- Wired `League` average/coach-rating wrapper methods through `LeagueStats` so the extracted stats helper is actually used consistently.
- Reran `.\gradlew.bat test`; it passed after the `LeagueStats` delegation change.
- Found more `(int) Math.random() * n` precedence bugs in player recruiting costs, playbook fallbacks, and realignment prestige loss; fixed them with parenthesized casts.
- Reran the `(int) Math.random() * n` scan; no matches remain.
- Reran `.\gradlew.bat test`; it passed after the random-precedence fixes.
- Suggested next pass: continue decomposing `League`/`Team` by extracting scheduling/realignment logic, or add regression tests around recruiting cost randomness and playbook fallback randomization.

## 2026-05-07 Audit and polish pass

- Audited broad Java/Android bug patterns: random precedence, always-true boolean conditions, suspicious indentation, min-API lint errors, null-returning collection hooks, and layout lint errors.
- Fixed RPI/SOS poll score schedule logic so played Conference/OOC games count again and byes are skipped explicitly.
- Fixed neutral/postseason revenue branching in `Game.playGame()`; Conference/OOC games now use home/away revenue, non-regular games use neutral-event revenue.
- Fixed a fumble tackler weighting where DLs had a zero chance to be selected on WR fumbles.
- Changed base `Player.getDetailAllStatsList()` to return an empty list instead of `null`, and fixed the unreachable "5th Yr Sr" year label.
- Cleared Android lint errors without changing custom back-navigation behavior by adding explicit `MissingSuperCall` suppressions where back handling is intentionally custom.
- Replaced Java 8+ file APIs flagged by Android lint with stream-based IO in desktop/resource and import/save paths.
- Added constraints/alignment fixes for `team_home.xml` and `rankings_main.xml`.
- Added a regression test that verifies RPI/SOS poll scoring counts a played regular-season game.
- Verification: `.\gradlew.bat check` passes, including compile, unit tests, and Android lint. Lint still reports warnings, but no errors.

## 2026-05-07 Continued polish pass

- Fixed a coach-advantage simulation bug in `Game.getCoachAdv()`: when the away team has possession, the home defensive playbook is now compared against defensive coordinator/head coach defensive strategy fields, and the away offensive playbook is compared against offensive strategy fields.
- Cleared additional default-locale lint warnings across focused desktop dialogs/views by using `Locale.ROOT` for internal upper/lower casing and deterministic `String.format` calls.
- Fixed three `ScrollViewSize` layout warnings by making scroll child heights `wrap_content` in `activity_tutorial.xml`, `box_scores.xml`, and `play_window2.xml`.
- Reran `.\gradlew.bat check`; it passes. Remaining lint warnings are down to 579, primarily broader Android inflate-root warnings, adapter view-holder warnings, scoped storage compatibility, and `LeagueHomeView` locale warnings.

## 2026-05-07 Quick polish pass

- Finished the remaining `LeagueHomeView` locale-safe formatting/search updates with `Locale.ROOT`.
- Removed the two obsolete `layout_alignParentStart` attributes from `play_window2.xml`.
- Reran `.\gradlew.bat check`; it still passes cleanly.
