# Android UI brief — broadcast HUD (v1.5 redesign)

Working rules for anyone restyling an Android screen in this repo. The visual
language is defined in [`style_guide.md`](../style_guide.md) and already shipped on
the desktop shell; the Android app is being brought onto the same tokens.

## Tokens (use these, never raw hex)

Colours (`res/values/colors.xml`):

| Role | Token |
|---|---|
| Window canvas | `@color/cf_canvas` |
| Sidebar / toolbar | `@color/cf_sidebar` |
| Card | `@color/cf_card` · elevated `@color/cf_card_elevated` · pressed `@color/cf_card_hover` |
| Borders | `@color/cf_border` · highlight `@color/cf_border_highlight` · `@color/cf_divider` |
| Text | `@color/cf_text_primary` · `@color/cf_text_secondary` · `@color/cf_text_muted` |
| Rating / active / positive | `@color/cf_emerald` (on it: `@color/cf_on_emerald`) · fills `@color/cf_green` |
| Rank / stars / prestige / primary CTA | `@color/cf_gold` · `@color/cf_amber` (on it: `@color/cf_on_gold`) |
| Opponent / alert / danger | `@color/cf_crimson` · `@color/cf_crimson_bright` (on it: `@color/cf_on_crimson`) |
| Semantic | `@color/cf_win` `@color/cf_loss` `@color/cf_positive` `@color/cf_negative` `@color/cf_warning` `@color/cf_info` |
| Glows / glass | `@color/cf_emerald_dim` `@color/cf_emerald_glow` `@color/cf_gold_glow` `@color/cf_crimson_glow` `@color/cf_glass` `@color/cf_glass_border` |

Legacy names (`surfaceCard`, `textPrimary`, `accentGold`, …) now resolve to the same
palette, so existing layouts are already on-brand; prefer `cf_*` in new/edited XML.

Dimensions (`res/values/dimens.xml`): spacing `cf_space_xs/sm/md/lg/xl/xxl`
(4/8/12/16/20/24dp), radius `cf_radius_sm/md/lg` (4/8/12dp), `cf_stroke`, type ramp
`cf_text_rating/hero/display/title/body/caption/micro/stat`, sprite box
`cf_sprite_avatar_width/height`, `cf_card_padding_h/v`.

## Backgrounds (`res/drawable`)

- Cards: `bg_cf_card`, `bg_cf_card_elevated`, `bg_cf_card_canvas` (inset tile),
  `bg_cf_card_accent` (emerald leading bar = "active"/"your team"), `bg_cf_card_crimson`
  (opponent / alert), `bg_cf_rating_tile` (emerald-bordered rating tile).
- Pills: `bg_cf_pill_neutral/emerald/gold/crimson`, solid `bg_cf_pill_solid_gold/emerald`.
- Controls: `bg_action_primary` (gold CTA), `bg_action_secondary`, `bg_action_danger`,
  `bg_action_win`, `bg_cf_input` (spinner/edit field), `bg_cf_list_selector`.
- Misc: `bg_cf_divider` (1dp), `bg_cf_progress` / `bg_cf_progress_gold` (ProgressBar
  `progressDrawable`), `bg_toolbar`, `bg_bottom_nav`, `bg_dialog_surface`, `bg_dialog_section`.
- Legacy `bg_mobile_panel`, `bg_mobile_panel_strong`, `bg_mobile_chip`, `bg_mobile_row`,
  `bg_home_stat` are re-skinned onto the tokens and remain valid.

## Typography (`res/values/styles.xml`)

`TextAppearance.Cfhc.Cf.Rating` (44sp emerald digits) · `.Cf.Hero` · `.Cf.Display` ·
`.Cf.Title` · `.Cf.Body` · `.Cf.Caption` · `.Cf.Micro` (10sp heavy tracking, muted) ·
`.Cf.Stat` / `.Cf.StatBold` / `.Cf.StatAccent` (JetBrains Mono: **every number, record,
score, rating, dollar figure, clock**) · `.Cf.Script` (Caveat, team nickname accent only).
Widgets: `Widget.Cfhc.Button.Primary/Secondary/Emerald/Danger`, `Widget.Cfhc.Pill(.Emerald/.Gold/.Crimson)`,
`Widget.Cfhc.CardTitle` (upper-case section label), `Widget.Cfhc.ProgressBar(.Gold)`,
`Widget.Cfhc.Spinner`. Dialogs inherit `ThemeOverlay.Cfhc.Dialog` from the app theme.

Sprites (`res/drawable-nodpi`, render with `android:scaleType="fitCenter"` and keep the
pixel look): `sprite_avatar_qb_green`, `sprite_avatar_lb_green`, `sprite_avatar_opponent_crimson`,
`sprite_avatar_showcase_qb/lb`, `sprite_crest_bears/bison/eagles/wildcats`,
`sprite_trophy_crystal/heisman/ring`.

## Composition rules

1. Card = `bg_cf_card`, padding `cf_card_padding_h`/`cf_card_padding_v`, a
   `Widget.Cfhc.CardTitle` label on top, 8dp between stacked cards.
2. Numbers right-aligned in mono; labels left in Inter. Never mix fonts within a column.
3. One gold primary action per screen; everything else secondary/emerald.
4. Touch targets ≥ 44dp; text ≥ 11sp except `Cf.Micro` labels.
5. Accessibility: `contentDescription` on every image/icon-only control; `labelFor` on
   inputs; keep `tools:text` placeholders instead of shipping fake data.
6. Phone-first portrait; no horizontal scroll; keep `fillViewport` scroll containers.

## Hard constraints

- **The Android SDK is not available in the cloud container.** Every change must pass
  `python3 scripts/verify_android_res.py` (XML parse, resource references, `R.id`
  coverage, no raw hex) — treat it as the compile step. Only use attributes you know
  the widget class supports; prefer plain framework views and the Material widgets
  already used in the repo (`MaterialButton`, `NavigationView`, `Toolbar`).
- Never remove or rename an `android:id` that Java references; never change the
  engine string protocol adapters parse; no `simulation/` / `positions/` /
  `recruiting/` (engine) edits beyond presentation text covered by tests.
- Java edits are styling only (`ContextCompat.getColor(ctx, R.color.cf_*)` instead of
  `Color.parseColor`, visibility toggles, text appearance). No behaviour changes.
- New strings go in a per-area file (`res/values/strings_<area>.xml`), new drawables
  are prefixed `bg_<area>_` / `ic_<area>_`, so parallel work never collides.
