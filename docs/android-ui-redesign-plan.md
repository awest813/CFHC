# Android UI Redesign Plan

> Goal: bring the Android app to the "dark sports broadcast console HUD" design language
> already defined in [`style_guide.md`](../style_guide.md), implemented in the web
> [`preview/`](../preview/) sandbox, and adopted by the desktop shell
> (`DesktopTheme`, `TeamMoraleCard`, sprite cards). Android is the uncovered surface:
> it currently uses **zero** sprites, has no custom views, and styles everything with
> scattered hardcoded colors.
>
> Status key: 🔲 not started · 🔄 in progress · ✅ done. Phases 0–6 of the
> [ship-readiness plan](ship-readiness-audit.md) are ✅ — v1.4.5 is published; this
> redesign targets the v1.5 cycle.

---

## 1. Where Android stands today (audited Sept 2026)

| Surface | Count | Notes |
|---|---|---|
| Activities | 4 | Home, MainActivity (game shell), RecruitingActivity, TutorialActivity |
| User-visible destinations | ~35 | 26 drawer items = 7 in-`mainList` list pages + ~16 dialogs + actions; plus first-run modal chain, Quick Browse, recruiting board, tutorial |
| Layout files | 48 (6,060 lines) | **5 dead layouts (~1,400 lines)**: `box_scores`, `play_window2`, `game_editor`, `playwindow`, `injury_report` |
| Dialog construction | 86 `AlertDialog.Builder` across 30 files | only Home uses `MaterialAlertDialogBuilder`; 25 dialog controllers with 66 `show*` entry points |
| List adapters | 24 classes (~1,953 lines) | all `ArrayAdapter`/`BaseExpandableListAdapter`; no RecyclerView, no ViewHolder in most |
| `findViewById` | ~443 | worst: `GameDialogController` 50, `PlayerProfileDialogController` 43 |
| Hardcoded colors in Java | 67 `Color.parseColor`/`0xFF` | concentrated in adapters → light theme is broken there today |
| Hardcoded dp/sp in layouts | 1,155 dp + 256 sp | vs **7** dimens tokens |
| Custom views | 0 | nothing `extends View` anywhere |
| Sprite/imagery use | 0 | the only bitmaps shipped are the 3 menu logos |

Structural facts that shape the plan: pages are rendered by swapping the adapter on a
single `mainList` ListView driven by two "examine" Spinners (`content_main.xml`);
current page is an int 0–12 in `GameStateManager` with an if/else re-render chain
(`MainActivity.resetUI`); adapters consume pre-formatted engine strings and split them
by `&`/`!!` with positional indexes; there is no Android screenshot tooling (the
desktop `UiSnapshotTool` is the pattern to port).

---

## 2. Design direction (already decided — do not re-litigate)

From `style_guide.md` / `preview/`:

- **Palette**: Midnight Obsidian `#060C14` canvas → Card Slate `#0D1726` / Elevated
  `#111C2E` panels with `#1E293B` borders; Neon Emerald `#00E676` for ratings/active
  states, Athletic Green `#10B981` fills; Trophy Gold `#F59E0B` / Amber `#FBBF24` for
  stars, ranks, prestige; Crimson `#881337`/`#9F1239` for opponent/alert highlights.
- **Type**: Outfit (display) + Inter (body) + JetBrains Mono (all numerics) + Caveat
  (mascot script accent). All four are OFL — bundle as TTFs in `res/font` (no
  downloadable fonts: the app is offline-first and GMS-free).
- **Components**: bordered slate cards with 8dp radius, grade pills, star gauges,
  giant emerald rating digits, sidebar nav with active green bar + gold badge pills,
  16-bit pixel-art player/crest/trophy sprites rendered pixelated.
- **Assets**: `preview/sprites/ui/` (5 player avatars, 4 crests, 3 trophies) plus the
  position sprite sheets (`pixel_qb_sprite.jpg` etc.) — all currently referenced only
  by the web preview. They need slicing/downscaling before Android can use them.

---

## 3. Key architectural decisions

| Decision | Choice | Rationale |
|---|---|---|
| UI framework | **Stay on the View/XML system in Java** — design tokens + custom views + ViewBinding + RecyclerView. **Not Compose.** | Solo Java codebase, ship-ready app, shared engine untouched; Compose would force a paradigm rewrite of 25 controllers, not a reskin. Revisit only after this redesign proves the component library. |
| ViewBinding | Enable `viewBinding true` in `app/build.gradle`; migrate per-screen as it's touched | Kills the 443 `findViewById` incrementally with zero runtime cost; works with Java. |
| Lists | RecyclerView + `ListAdapter` (DiffUtil) for all list pages and long dialogs | ViewHolder recycling + animations; current ListView rebinds everything each render. |
| Navigation | Keep DrawerLayout + int page state; **do not** move to fragments/Navigation component this cycle | The controller/page-swap architecture works and is testable; restyling the drawer delivers 90% of the visual win at 10% of the risk. |
| Dialogs | One styled shell component (the `team_rankings_dialog` pattern, already reused 14×) promoted to the standard; all dialogs adopt it | Collapses 86 builder call sites into consistent chrome; keeps the 25 controllers as logic-only classes. |
| Theming | Token-first: every color/dimension/typography style resolves via theme attributes; **no raw hex in Java or layouts** | Fixes the light theme (currently broken in 67 places) and makes the obsidian HUD a theme, not a sprinkle. |
| Sprites | `res/drawable-nodpi/` PNGs, rendered with paint filter OFF (pixelated) in a shared helper | nodpi avoids density scaling that blurs pixel art. |
| Fonts | Bundle Inter, JetBrains Mono, Caveat (OFL; add `SOUND_LICENSES.md`-style attribution) | Offline-first; matches preview exactly. Outfit optional if Inter covers display weights. |

---

## 4. Target component library (`ui/components/`)

New Java view classes (the heart of the redo — everything else composes these):

1. `CfhcCardView` — slate panel, 1dp border, 8dp radius, optional title + section label.
2. `StatGrid` / `StatCell` — monospace numeric cells (roster lines, finances, records).
3. `RatingBadge` — giant emerald digit + grade pill + star gauge (drawn, not text stars).
4. `PlayerSpriteCard` — pixel avatar + name + OVR + last-game line (the preview's
   Roster Spotlight card; desktop `TeamMoraleCard` is the styling reference).
5. `CrestView` — team/conference crest with tinting from team colors.
6. `TrophyIconView` — trophy sprite badges (awards, history, records screens).
7. `DialogShell` — standard dialog chrome: title, subtitle chip row, content slot,
   footer button row. Plus `Dialogs` facade replacing raw `AlertDialog.Builder` usage.
8. `RecyclerView` row set: `ListRowBinding` helpers so every list page shares one
   row visual language (sprite slot, primary/secondary text, right-aligned mono stat).

---

## 5. Execution waves

Each wave ends green: `quickVerify` + `lintDebug` + emulator smoke of the touched
screens + commit. No wave mixes re-skinning with engine changes.

### Wave 0 — Foundations & demolition (~1 day)
- Delete the 5 dead layouts (~1,400 lines) and the hidden legacy spinner in
  `content_main.xml`; rename the `antdroid.cfbcoach.RecruitingDialogController` →
  `RecruitingOptionsDialogController` (two same-named classes exist today).
- Add tokens: extend `colors.xml` to the full style-guide palette, `dimens.xml`
  (spacing/radius/type sizes), `res/font/` (Inter, JetBrains Mono, Caveat),
  `TextAppearance.Cfhc.*` completed for all 4 families.
- Enable ViewBinding. Copy sprites → `res/drawable-nodpi/` (downscaled: crests are
  ~360KB today → target <40KB each; slice position sheets into per-position avatar
  PNGs). Extend `scripts/verify_assets.py` to check them.
- **Screenshot harness**: `scripts/android_snapshots.sh` — boots the emulator AVD,
  installs the debug APK, drives each screen via `adb shell input`, captures
  `adb exec-out screencap` PNGs into `preview/android-snapshots/`. This is the
  before/after evidence for every later wave.

### Wave 1 — Component library (~2–3 days)
- Build the 8 components in §4 with unit-tested binding logic where practical and a
  `component_gallery.xml` debug screen reachable via the Home "About" tap (a living
  showcase, like `preview/index.html`).
- Convert `PlatformUiHelper`'s 4 parallel dialog-shell binders onto `DialogShell`.
- Acceptance: gallery screen shows every token/component; no existing screen changed
  except the 4 shell-bound dialog families.

### Wave 2 — Home + MainActivity shell (~2–3 days)
- `content_home.xml`: obsidian hero, gold primary button, crest + version chip;
  Manage Dynasties gets save-slot cards.
- `content_main.xml`: broadcast header (crest, school + script nickname, season/week
  chip row — extends the existing `mainSeason*` chips), restyled drawer
  (`activity_main_drawer.xml` + `nav_header_main.xml` with active green bar and badge
  pills), Quick Browse grid → bottom action bar, `mainList` → RecyclerView.
- First-run chain (`selectTeam` → coach name → style → playbooks → goals): restyle
  into a consistent wizard using `DialogShell` + crest/sprite imagery.
- Acceptance: drawer + all 7 list pages still render every destination; screenshots
  captured before/after.

### Wave 3 — The 7 list pages + adapters (~3–4 days)
- Migrate `ui/` adapters (`TeamHome`, `TeamRoster`, `IndividualStats`, `TeamStatsList`,
  `GameScheduleList`, `MainRankings`, `CoachDatabase`, `PlayerProfileV2`,
  `StatsRowAdapter`, …) to RecyclerView rows built on §4 components.
- **Keep the engine-string protocol intact this wave** (a `RowModel` parses the
  positional strings in ONE place per adapter) — de-coupling the engine string
  protocol into typed row models is a separate later refactor, not this one.
- Replace all 67 hardcoded `Color.parseColor` sites with theme attrs; win/loss/
  positive/negative semantic colors.
- Home page ("Team Home" = `team_home.xml`, 261-line row) becomes the preview's
  11-card dashboard stack: Team Overall (RatingBadge), Next Game Matchup (split
  cards), News carousel card, standings mini-table, morale card (sprite-backed, port
  of desktop `TeamMoraleCard`), Roster Spotlight (2 × PlayerSpriteCard), finances,
  prestige shield.
- Acceptance: every drawer list page + Team Home renders from new components;
  light theme manually checked on each.

### Wave 4 — Dialogs (~3–4 days)
- Sweep the 25 dialog controllers / 86 builders onto `DialogShell` + components.
  Biggest first: `GameDialogController` (50 ids), `PlayerProfileDialogController`
  (43, + sprite header), `SettingsDialogController` (508-line layout → sectioned
  card list), `TransferDialogController`, `CoachProfileDialogController`,
  `PostseasonDialogController` (bowl bracket gets trophy sprites).
- Delete the `setDialogMessageTextSize` hack and the 4 legacy shell binders.
- Acceptance: zero raw `AlertDialog.Builder` outside the `Dialogs` facade;
  grep-enforced.

### Wave 5 — Recruiting board + Tutorial (~2–3 days)
- `content_recruiting.xml` + `ExpandableListAdapterRecruiting`: recruit rows become
  PlayerSpriteCards with star gauges, mono ratings, budget header card; crest for
  each school; the recruiting "map" concept from the preview rendered as a regional
  header card using the recruiting-map sprite.
- TutorialActivity: chapter list with icons, styled ScrollView, sprite chapter headers.
- Acceptance: full recruiting cycle played end-to-end on the emulator (scout →
  recruit → finish → handoff back to MainActivity).

### Wave 6 — Polish, theming, regression (~2 days)
- Light theme audit: every screen in both themes via the snapshot script.
- Animations: row press ripples, card state transitions, screen-change fade
  (respect `Settings.Global.ANIMATOR_DURATION_SCALE`).
- Final icon pass (vector menu icons in the drawer), typography sweep
  (no sp literals outside tokens), lint zero-warning check.
- Snapshot suite re-run = the visual regression baseline checked into
  `preview/android-snapshots/`.

**Total: ~15–19 focused days.** Waves 0–2 deliver the visible identity change; 3–5
are the bulk; 6 is the quality gate. Each wave is independently shippable.

---

## 6. Verification strategy

1. **Per-wave gates**: `quickVerify`, `:app:lintDebug`, emulator smoke of touched
   screens (the android-emulator tooling used throughout the ship-readiness work).
2. **Screenshot evidence**: `scripts/android_snapshots.sh` (Wave 0) — before/after
   PNGs for every screen; stored under `preview/android-snapshots/`.
3. **Flow checklist** (run on emulator each wave that touches it): new career →
   team select → week sim with interactive decisions → all 26 drawer destinations →
   recruiting cycle → save/load mid-season → bulk sim → postseason → offseason steps.
4. **Engine isolation**: no `simulation/`/`positions/`/`recruiting/` changes in any
   wave; `checkEngineImports` keeps enforcing it. Desktop stays untouched.

## 7. Risks

| Risk | Mitigation |
|---|---|
| Positional engine-string parsing breaks a row silently | Keep the protocol; centralize parsing per adapter in `RowModel` with fail-loud index guards |
| Sprite licensing/attribution | All assets are self-generated pixel art (verify with `scripts/verify_assets.py`); fonts OFL — add attribution file |
| APK size growth (sprites) | nodpi + downscale (<40KB each), WebP where it doesn't blur pixels; monitor with APK analyzer each wave |
| Regression in the 86 dialogs | Wave 4 is purely mechanical re-chroming; controllers' logic untouched; flow checklist after |
| Light theme regressions | Token rule is grep-enforced (`Color.parseColor` count → 0) + both-theme snapshots |
| Scope creep into navigation/fragment rewrite | Explicit non-goal; the int-page + adapter-swap architecture survives this redesign |

## 8. Non-goals (this cycle)

- Jetpack Compose, fragments, single-activity migration, Navigation component.
- Changing the engine↔UI string protocol (tracked separately).
- Localization/tablet layouts (the app ships English-only, portrait-friendly today).
- Desktop UI changes (already redesigned).
