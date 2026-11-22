# CFHC — Next Release (draft)

Changes since v1.5.0. Version numbers are not bumped yet.

## Gameplay

- **Shared recruit pool.** Every program — yours and the CPU's — signs from one national prospect pool. Rivals
  commit some prospects in an early signing wave, and contested prospects commit elsewhere if you wait too long
  ("Contested", "Deciding", "Committed: X" on the board). CPU programs fill the rest in a late wave.
- **Smarter CPU coaches.** Realistic fourth-down decisions (~1.6 tries a game), sensible field-goal ranges,
  timeouts in the two-minute drill, victory-formation kneels, a second-half two-point chart, and onside kicks
  when down two scores late. Leaders run the clock instead of running up 100-point scores.
- **Game engine fixes.** No more overtime punts; NCAA two-point overtime rules; injured and suspended players sit;
  bench players recover; home field and coaching edges help the offense (they used to add sacks); playoff games
  are neutral sites; momentum goes to the team that forced the turnover; realistic passing, penalties and scoring.
- **Season and career.** Odd-sized conferences no longer play rematches; same-conference rivals always meet;
  awards are decided every season (not only when the Awards screen is opened) and counted once; rosters stay at
  75; budgets and prestige no longer inflate forever; elite backs, tight ends and linemen can declare early for the
  NFL; CPU staffs choose a practice focus (a trade-off between traits, the same as yours).
- **You can't be fired with career movement turned off.**

## Saves

- A save made at any week — including the playoffs and mid-recruiting — reloads and plays on exactly as if it had
  never been saved (box scores, news and awards included).
- Saves end with an integrity line, so a cut-off file is refused instead of loading as a smaller league; writes
  are crash-safe. Names containing `& | : ;` no longer break saves.
- Older saves still load (schema v1.5e; v1.4e onward supported).

## Audio

- Music no longer takes audio focus while muted or paused, so it won't pause your podcast; it pauses when you
  leave the app and keeps playing in Recruiting. Unplugging headphones pauses it; switching music on resumes.
- One result sound per week that matches your game; louder, levelled stingers; tracks matched in loudness.
- Desktop music streams instead of loading whole tracks into memory.

## Performance

- Seasons simulate about 3x faster; the offseason step that froze for ~4 s is gone; weekly autosaves do most
  of their work off the UI thread.

## Look and feel

- Android: buttons show their real colors again (they all rendered emerald, so losses looked like wins and the
  gold "Play Week" call to action was green); labels line up with values in box scores and scouting reports;
  layouts hold up on small phones and large font sizes; star grades with ★; cleaner recruiting rows.
- Desktop: popups no longer cut off their last lines; tables size their columns so names fit; a contract
  dialog no longer shows "2500%" win rates; Esc closes detail dialogs; consistent headers and buttons.
- Android has a real light theme, "Daylight" (Settings → theme): light surfaces, dark text, contrast checked
  against WCAG AA. Dark stays the default.
- Works correctly on Turkish-language systems (sounds, save folder and commands used to break there).

## Text

- Natural play-by-play ("rushed for no gain", "for a loss of 3 yards", "gain of 1 yard"); fixed plurals,
  articles and typos across news stories.
