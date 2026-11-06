# CFHC v1.5.0 — Release Notes

Android `v1.5.0` (versionCode 321) · Desktop `1.5` · Requires Android 7.0+ (API 24) · Desktop: Java 17+

## Play Console "What's new" (under 500 characters)

> Autosave after every week, and the game resumes where you left off. New: Gameday Plan coaching,
> an Offseason Hub, Team Home cards (Roster Spotlight, Locker Room, Program), box scores from Weekly
> Scores, and a Mock Draft all year. Fixed: teams playing twice in a week, saves playing several
> weeks at once after loading, and crashes at the first Play Week and the Season Summary. Refreshed
> look; updated for Android 16.

## Highlights

- **Autosave and resume.** The dynasty is saved after every week and whenever Android puts the app in the
  background. If Android closes the app, it reopens at the same week. Home → Load → **Resume Autosave**.
  Saving to a slot no longer freezes the screen on slow storage.
- **Gameday Plan.** Drawer → Team → Gameday Plan: choose fourth-down aggression and your halftime and
  fourth-quarter calls for when you lead or trail. The plan is applied in every game you play.
- **Offseason Hub.** A ten-step offseason checklist with "Advance One Step" (Drawer → Simulation, in the offseason).
- **Team Home.** New cards: Roster Spotlight (your two best players), Locker Room (chemistry, leadership, buy-in)
  and Program (prestige trend, budget, NIL tier, facilities, discipline, incoming class). The team overall now shows
  a grade and stars.
- **More to explore.** Tap any final in Weekly Scores for its box score; Mock Draft is in the drawer all year;
  the Playbook screen colours how well each scheme fits your staff and quarterback.

## Fixes

- **Schedules.** About a quarter of games used to fall in different weeks for the two teams, so some teams played
  twice in a week and not at all in another. Every game now falls in a single week for both teams.
- **Loading a save** could play several weeks of games on the next Play Week. Fixed.
- **Crashes fixed:** Team Home after the first Play Week of each season, and the Season Summary in a dynasty's first
  season (desktop showed "summary unavailable"; later seasons showed the previous year's prestige change).
- Depth chart and redshirt checkboxes could toggle the wrong player while scrolling.
- Smaller fixes: clipped empty-state messages, overlapping text in the Transfer Portal, blank cards in Settings,
  Weekly Scores and League History.

## Look and feel

Refreshed lists and dialogs, rating badges and drawn star ratings, crisp pixel-art crests and trophies, consistent
spacing. The theme toggle is now labelled **Midnight / Slate** (both are dark themes; "Light" was misleading).

## Platform

- Targets Android 16 (API 36), with layouts that stay clear of notches and the navigation bar.
- Licenses screen (Home → Credits → Licenses). The chart library was replaced, and unused desktop audio libraries
  were removed from the Android download.

## Compatibility

- **Existing saves load as before.** A dynasty saved in the middle of a season by an older version keeps that
  season's old schedule (including any doubled weeks) until the season ends. New seasons use the fixed schedule.
- Saves made by 1.5.0 record schedule positions that **older versions do not understand**. Don't open 1.5.0 saves
  in 1.4.x.
- Gameday Plan choices are stored on the device and apply to every dynasty on it.

## Known limitations

- Weather does not vary by week of the season (a long-standing engine quirk; changing it would alter every
  simulated result, so it is planned as a separate balance change).
- This release was verified with unit, screen (Robolectric) and long-run simulation tests, and a minified release
  build. Before the Play upload, run the device smoke test in [release-runbook.md](release-runbook.md) §3.
