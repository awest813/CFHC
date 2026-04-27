# Desktop shell improvement roadmap

This document tracks planned work for the **Swing desktop prototype** (`desktop.*`, `runDesktop` / `desktopJar` in Gradle). It complements the main [ROADMAP.md](ROADMAP.md) (engine and Android) and the shared-shell notes in [platform-expansion.md](platform-expansion.md).

---

## Done recently

- **Docked recruiting** — NLI signing runs in a **Recruiting** tab instead of a blocking modal; bulk sim stops at recruiting until finished.
- **Dark mode (v1)** — **View → Dark mode**, launcher checkbox, **Settings → Dark mode (desktop shell)**; preference stored under `Preferences` node `cfhc/desktop`. League home tables, status bar, dashboard, recruiting panel, and common dialogs pick up themed colors; `UIManager` hints improve JOptionPane / lists in dark mode.
- **NewGameWizard + JOptionPane dark surfaces** — Wizard pages use `DesktopTheme` colors; string confirms/errors use `messageForDialog`; scroll-heavy help/summary dialogs (`How to Play`, keyboard shortcuts, bridge scroll text) style `JTextArea` + viewport so message panes do not flash white in dark mode.
- **File chooser + dialog shells (dark)** — `DesktopTheme.styleFileChooser` recursively themes typical Swing subcomponents inside `JFileChooser` in dark mode, plus extra `FileChooser.*` `UIManager` keys. **Native shell–hosted views** (e.g. some Windows folder panes) are still drawn by the OS and **may differ by platform**; Swing-embedded lists/details should match the shell theme.
- **League home tab surfaces** — `DesktopTheme` helpers (`styleTabRoot`, `styleToolbar`, `styleDataTableInScroll`, `styleListShell`, `titledBorder`, `styleLeagueSettingsPanel`, `styleLabelsDeep`) applied across `LeagueHomeView` tabs (standings split, polls, rankings, player search, league history, scoreboard, news, coaches DB, HoF, records, My Coach, settings, dashboard).

---

## Near term (UX & parity)

1. **Theme polish (remaining)** — Chase any remaining default-white panels. Choosers already use `DesktopTheme.styleFileChooser` and extra `UIManager` keys (see **File chooser + dialog shells (dark)** above); **native shell views can still differ by OS**. If that is unacceptable, escalate to **FlatLaf** or a custom chooser / accessory panel.
2. **Tab surfaces** — `LeagueHomeView` analytics tabs now use `DesktopTheme.styleTabRoot`, `styleToolbar` / `styleFormControl`, `styleDataTableInScroll` (fixes viewport styling after tables mount), `styleListShell`, and `titledBorder` on dashboard + history + coach profile. Sweep any remaining secondary views (e.g. small dialogs) the same way.
3. **Android parity gaps** — Track features still missing or simplified on desktop (full recruiting edge cases, save slot UX, Android import parity, and broader shared-service reuse).
4. **Keyboard** — Document and implement more accelerators (e.g. jump to tab by number, focus table filter).

---

## Medium term (architecture)

1. **Simulation facade** — Align with [platform-expansion.md](platform-expansion.md): thin headless API over `League` / `SeasonController` so desktop (and future hosts) do not depend on UI timing inside the engine.
2. **Save/import layer** — Keep pushing desktop persistence and import/export flows toward shared `simulation` services so paths, errors, and formats match Android where intended.
3. **Threading contract** — Document single-thread rules for `League` mutation; keep UI workers from advancing the sim concurrently without a game queue.

---

## Longer term

1. **Packaging** — Signed installers (e.g. jpackage), auto-update channel, optional portable zip + JRE.
2. **Accessibility** — High-contrast theme variant, focus traversal audit, screen-reader labels on tables.
3. **Optional UI stack** — Evaluate JavaFX or embedded WebView only if the team commits to a second UI layer; default remains lightweight Swing + shared core.

---

## How to contribute

Pick an unchecked item, open a focused PR, and reference this file. For engine-wide items, use [ROADMAP.md](ROADMAP.md) and link back here when the change is desktop-specific.
