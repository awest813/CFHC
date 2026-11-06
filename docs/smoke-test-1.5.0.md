# v1.5.0 Device Smoke Test

About 20 minutes on one phone. The goal is to test the signed, minified release build as an
**upgrade over 1.4.5**, with real saves, which no automated test here can cover.

## Build and install (on the machine with the release keystore)

```bash
git checkout master && git pull           # 6cb13f1 or later, CI green
./gradlew :app:assembleRelease            # uses signing.properties; fails fast without it
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk   # CN=CFHC Release
adb install -r app/build/outputs/apk/release/app-release.apk                  # -r keeps app data
```

`-r` upgrades in place and keeps your 1.4.5 saves. If the install is refused for a signature
mismatch, stop: the phone has a build signed with a different key, and uninstalling would delete
its saves. Export them first (in-game Export), then uninstall.

## Checklist

**Upgrade path**
- [ ] Home → Updates shows the v1.5.0 entry. The theme chip reads Midnight/Slate.
- [ ] Load a 1.4.5 save from a slot: Team Home opens, and names are real (no `[teams]` placeholders).

**New league (release build, minified)**
- [ ] New Career → pick a team → coach setup: names, conferences and bowls look normal.
- [ ] Team Home: rating badge (number, grade, stars), Roster Spotlight (tap opens a profile), Locker Room, Program.
- [ ] Play Week from the preseason: no crash on the week-1 Team Home (was a crash).
- [ ] Play 3 weeks: the record goes 1, 2, 3 games played. One game per week, never two.

**Save, reload, autosave**
- [ ] Save to a slot mid-season, Exit to Main Menu, Load it: same week. The next Play Week plays exactly one game.
- [ ] Play a week, then press Home and swipe the app away from Recents. Reopen → Load → **Resume Autosave**: same week.
- [ ] Toggle system dark mode while in a league: the screen stays, nothing reloads.

**Screens**
- [ ] Weekly Scores: tap a final → box score. Mock Draft from the drawer.
- [ ] Gameday Plan: walk the five cards. Play a game, then check the box score log for "COACHING:" lines.
- [ ] Depth chart and redshirts (preseason): scroll, toggle boxes, save. The right players change.
- [ ] Recruiting board: expand and collapse recruits, recruit one, finish recruiting.

**Season end**
- [ ] Sim Through Postseason, then step into the offseason: the Season Summary appears (was a crash in season 1).
- [ ] Offseason Hub (drawer) shows the checklist and Advance One Step.

**Layout**
- [ ] Notched phone in landscape plus 3-button navigation: nothing is hidden behind the notch or the bars.
- [ ] Home → Credits → Licenses opens.

If all boxes pass, reply "tag it". If something fails, describe it or send a screenshot.
