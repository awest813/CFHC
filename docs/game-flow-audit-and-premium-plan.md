# Game Flow Audit & Premium Elements Plan

> Audit of the season/game-day loop as actually implemented (September 2026), plus a phased plan
> to close the gap between CFHC's current feature set and what a "premium" coaching sim delivers.
>
> **Status key:** 🔲 not started · 🔄 in progress · ✅ done
>
> Companion docs: [ROADMAP.md](ROADMAP.md) (engine/architecture debt) · [desktop-gameplay-polish.md](desktop-gameplay-polish.md) · [THREADING.md](THREADING.md)

---

## Part 1 — Game Flow Audit

### 1.1 How the loop actually works

`SeasonController.advanceWeek()` (`simulation/SeasonController.java:34`) is the single entry point.
It dispatches on `League.currentWeek` against `regSeasonWeeks` (R, default 13, documented as
dynamic), pushes UI prompts through `GameUiBridge`, and mirrors every event into a structured
`SeasonAdvanceResult`. `SeasonFlowOrder` (`simulation/SeasonFlowOrder.java`) is the canonical week
map. Callers: Android `MainActivity.simulateWeek()`
(`antdroid/cfbcoach/MainActivity.java:1254`), desktop `LeagueHomeView.playWeek()`
(`desktop/LeagueHomeView.java:1094`), headless `SimulationFacade.advanceWeek()`
(`simulation/SimulationFacade.java:289`).

Canonical cycle as implemented:

| Week | Phase | Engine work | Dialogs / prompts |
|:--|:--|:--|:--|
| 0 | Preseason | `recruitWalkOns()`, `preseasonNews()` (hot-seat + top-freshman stories) | none |
| 1…R−2 | Regular season | `playWeek()`: games, BYEs, practice outcomes, suspension ticks, injuries, discipline check, committee news (wk 8+), streaks, NIL stipend + coach XP | midseason summary after wk R/2 (+ progression); discipline dialog if flagged |
| R−1 | Conf championship | `playConfChamp()`, final poll, bowl/CFP selection (`BowlManager`) | — |
| R | Bowls / playoff rd 1 | bowl week 1 or expanded first round; Heisman finalists locked | **Awards ceremony** |
| R+1, R+2 | Bowls / QF / SF | bowl weeks 2–3 or QF/SF; NCG scheduled | — |
| R+3 | National championship | NCG, champion news | — |
| R+4 | Season summary | `enterOffseason()`, records/history updates | **Season summary** |
| R+5 | Contracts | `advanceStaff()` (extensions, firings, retirements) | **Contract** |
| R+6 | Job offers | *no-op* (dialog only if user was fired) | **Job offers** (conditional) |
| R+7 | Coach carousel | `coachCarousel()` fills all HC vacancies | **Promotions** |
| R+8 | Coordinator hiring | *no-op engine side* — pure UI slot | **Coordinator hiring** (desktop gates on OC/DC null) |
| R+9 | Graduation | `advanceSeason()` — seniors leave, early NFL entries, development, training camp | **Redshirt list** |
| R+10 | Transfers | `transferPlayers()` — CPU portal placement, user Accept/Decline prompts | per-player portal prompts |
| R+11 | Transfer list | *no-op* (dialog only) | **Transfer list** |
| R+12 | Realignment | `runOffseasonRealignment()` + `hireMissingCoaches()` | **Realignment summary** |
| ≥ R+13 | Recruiting (hard gate) | none — week never advances | NLI notification → **recruiting UI**; CPU recruiting + rollover are driven by the shell (`prepareCpuRecruiting` → `finishRecruitingSeason` → `startNextSeason`) |

**Assessment:** the macro loop is complete and well-factored (one controller, one bridge, one
canonical week map). Its weaknesses are (a) three dead/no-op offseason weeks, (b) UI-driven
recruiting completion that can hang headless automation, (c) magic numbers that drift from
`SeasonFlowOrder`, and (d) a presentation layer that gives the player almost no *texture* for what
just happened — the loop advances correctly but rarely feels like an event.

### 1.2 Shell UX: the same loop, very different feel

| Capability | Android (`antdroid/cfbcoach/`) | Desktop (`desktop/`) |
|:--|:--|:--|
| Advance week | single bottom-bar button | menu + dashboard button + Space/Enter hotkeys |
| Bulk sim | **none** — every postseason/offseason step is its own modal | "Sim Through Postseason" / "Advance Through Offseason" with progress + interrupt |
| Game result feedback | pull-only (tap a score row) | automatic result dialog + win/loss sound |
| Save | **disabled midseason** (Toast) | save anytime, dirty tracking, exit prompt |
| Recruiting | separate activity with full teardown/recreate | docked tab + disk checkpoints |
| Week display | phase chips (`SeasonPresentation`) | raw week integers incl. "WEEK 17" offseason offsets (`DesktopHeaderBar.java:89-98`) |
| Chain of modals per offseason | ~14 non-cancelable dialogs in sequence | same events, suppressed-informational bulk mode |

Android players experience the loop as a long modal gauntlet with no bulk sim and no midseason
saves; desktop players have the tools but the header/feedback layer lags. Both shells share the
same engine deficiency: advancing a week produces a result, not a *story*.

### 1.3 Verified defects in the flow (evidence-backed)

Severity: 🔴 wrong behavior visible to players · 🟠 sim-quality / fragility · 🟡 hygiene.

| # | Sev | Defect | Evidence |
|:--|:--|:--|:--|
| D1 | 🟠 | Conference-championship news hard-codes week 13; mis-files news for any R ≠ 13 (R is documented dynamic) | `simulation/Conference.java:687,699` |
| D2 | 🔴 | User QBs lost to the portal vanish from the "Transfers Out" summary — the `(FCS)` append line is missing, leaving an **empty if-body** | `simulation/League.java:3660-3662` |
| D3 | 🔴 | Unmatched portal players that lose the 50/50 stay in the pool and are silently wiped by `startNextSeason` (players deleted from the game world) | `League.java:3383-3390`, pools cleared at `:5986-5996` |
| D4 | 🟠 | Portal placement scan `for (t = rand; t < size - rand)` with fresh random per player — asymmetric, poll-order-biased | `League.java:3383-3384` |
| D5 | 🟠 | Medical-redshirt rule assumes R=13 (`duration > 12 - week && week < 6`); wrong for any other season length | `simulation/Injury.java:41-52` |
| D6 | 🟠 | `playerSpotlight` uses fixed weeks 5–11 and `.get(0)` on ranked lists — crash-prone if lists empty or R < 12 | `League.java:2108-2160` |
| D7 | 🟠 | Bowl slicing hard-codes index ranges 0-6/6-16/16-end; `getExpPlayoffTeams` can double-schedule bowls if called when `hasScheduledBowls` is set | `simulation/BowlManager.java:117,394-459` |
| D8 | 🔴 | Pre-game counter-strategy **mutates teams' real playbook numbers** and the reset block re-copies the mutated values — AI playbook changes persist into following games and saves | `simulation/Game.java:304-411,665-670` |
| D9 | 🔴 | `GameStatRecorder` mutates game state during stat bookkeeping (INT flips possession/clock; safety triggers a free kick) — stat layer can change outcomes | `simulation/GameStatRecorder.java:364-371,500-512` |
| D10 | 🟠 | No seeded RNG anywhere (`Math.random()` ×189 in `Game.java`; `new Random()` in Injury/Staff/Team/TeamFinance/League/recruiting) — results unreproducible, sim untestable | codebase-wide |
| D11 | 🟡 | Magic numbers re-derive `regSeasonWeeks+N` inline in `SeasonController`, `League.applyLeagueRecord` (`+13`), and the week-99 recruiting sentinel, duplicating `SeasonFlowOrder` | `SeasonController.java:44,84-131`, `League.java:1313,847` |
| D12 | 🟡 | `homeTOs/awayTOs` are actually turnover counters, rendered as "TOs" in the box score | `Game.java:84-85`, `GameBoxScore.java:126-130` |
| D13 | 🟡 | Desktop dashboard "Team Morale" card renders hard-coded 82/78/85 — fake data | `desktop/TeamMoraleCard.java:83-85` |
| D14 | 🟡 | Universe import names column 4 `tmRival` but feeds it to the **division** field — a naming trap that has twice misled readers into thinking rivalry data exists | `League.java:493-495`, `Team.java:311` |
| D15 | 🟡 | Engine-side no-op offseason weeks R+6 and R+11 (dialog-only) | `SeasonController.java:168-176,215-221` |
| D16 | 🟠 | Recruiting gate never advances the week; headless full-year sim needs a 200-step escape cap to avoid hanging | `DesktopBulkSimulator.java:160-176` |

Quick wins (D1, D2, D4, D5, D12) are each ≤ a few lines; D8/D9/D10 are the substantive engine
correctness items and gate the premium game-day work in Part 2.

---

### 1.4 Premium gap analysis — present / shallow / missing

A coaching sim reads as "premium" when three things hold: **consequences** (every decision moves
visible state), **ceremony** (calendar moments feel like events), and **texture** (the world makes
news, noise, and drama on its own). CFHC's engine has real depth in career and player lifecycle
but is thin on ceremony and texture, and its game-day layer lacks the environmental systems
players associate with premium titles.

**Present and deep** (protect, don't rebuild): coaching career (contracts, firing, carousel,
hot seat, coordinator pipelines), player lifecycle (progression, redshirts, portal entry logic,
injuries, character/discipline), postseason structure (4- and 12-team playoffs, 52-bowl pool,
realignment modes), records/history books, and a genuinely complete stat pipeline
(quarter-by-quarter scoring, box scores, PBP persistence).

**Present but shallow** (scalar where a sim would have systems):

| System | Today | Why it undercuts the premium feel |
|:--|:--|:--|
| Poll | one composite score (`StatsTracker.updatePollScore`), updated at checkpoints, not weekly | no Sunday-release ritual, no voter noise, no AP/Coaches disagreement drama |
| Bowl selection | pure poll-order snake | no conference tie-ins, no "bid day" storylines |
| Finances | one integer `teamBudget`; revenue = 3 constants × prestige | wins don't visibly pay; no expenses; no AD pressure |
| Stadium | `teamStadium` saved, never read (dead field) | no capacity, attendance limits, or expansion arc |
| TV | `enableTV` setting stored, zero sim effect (real TV logic sits unused at Conference level) | a toggle that does nothing |
| Coach contracts | year counts only | no salaries, buyouts, or staff-pool tension |
| Hot seat | static threshold string | no AD/booster approval meter to manage |
| NFL departure | early-entry thresholds + mock draft list | no draft event, combine, or destination narrative |
| Awards | single stat composite | no watch lists, voting, or position trophies |
| Team morale | real chemistry feeds a small sim bonus, but the dashboard card shows fake numbers | the loop hides its own depth |

**Missing entirely** (the actual "premium element" gaps):

- **Game-day environment:** weather, crowd noise, in-game penalties, timeouts/icing, momentum, halftime adjustments, officiating variance, special-teams blocks/fakes, targeting/ejections.
- **Interactive gameday:** user games are simmed identically to CPU games — no play-calling, no coach decisions, no halftime strategy choice.
- **Pageantry & identity:** rivalry games/trophies, senior day, homecoming, alternate uniforms, traditions, bowl-week flavor. (No rivalry data exists anywhere — see D14.)
- **Ceremonies:** signing-day event, spring game, draft night, bowl-selection show, award watch lists that evolve weekly.
- **Presentation hooks:** 10 of 16 `AudioEvent`s (whistle, TD cheer, crowd roar, organ) are mapped to files and loaded on both platforms but **never triggered** — `Game.java` has no audio hooks.
- **Reproducibility:** no seeded RNG, so "the same season" can never be replayed, shared, or regression-tested (D10).

---

## Part 2 — Plan: fill in the premium elements

Sequencing logic: correctness first (D8–D10 gate every game-day feature), then texture that
reuses existing data (news/audio/morale), then game-day environment, then identity/ceremony, then
meta depth, with shell parity running alongside. Every phase is independently shippable and
save-compatible unless noted.

### Phase 0 — Flow integrity & testability (foundation) ✅

*Goal: make the loop trustworthy and reproducible. No new features — pure correctness.*

1. ✅ **Seeded RNG service** (D10). `simulation.SimRandom` — one league-scoped stream; the seed is chosen at league creation, persisted as an optional 6th field of the save's `L:` header, and re-bound on load. Every engine `Math.random()` / `new Random()` (Game, Player, all position classes, Injury, Staff, OC/DC, Team, TeamFinance, League portal, recruiting) now draws from it. Legacy saves without a seed get a fresh one on load (additive format change — no version bump, old builds read new saves fine). `SimRandom.pinNextSeed` lets tests construct fully deterministic leagues. Regression coverage: `SeededReplayTest` (same seed → identical full-year digest; different seeds diverge; seed survives save/load).
2. ✅ **Fix the player-visible bugs**: D2 (QB portal FCS summary line restored), D3 (every unmatched portal player is now explicitly dispositioned — rejoins old team or departs for FCS with a news story and summary entry; nothing left for `startNextSeason` to silently delete), D8 (counter-strategy stashes the season-long playbooks once and restores them at game end — no more leakage into later games or saves), D9 (INT/sack clock-downs-possession mutations and the safety award moved out of `GameStatRecorder` into `Game`; the recorder only records).
3. ✅ **Flow-order hygiene**: D1 (CCG news uses `currentWeek + 1`), D4 (portal scan wraps the full team list from a random start instead of the shrinking poll-ordered window), D5 (medical-redshirt window derived from `regSeasonWeeks`), D6 (`playerSpotlight` guards empty boards and clamps its window to the season length), D7 (`bowlScheduleLogic` is idempotent — scheduling happens exactly once; tier cutoffs named), D11 (all `regSeasonWeeks + N` arithmetic routed through `SeasonFlowOrder` helpers; week-99 sentinel named `RECRUITING_SENTINEL_WEEK`), D12 (`homeTOs/awayTOs` → `homeTurnovers/awayTurnovers`; save format positional, unaffected), D14 (misleading `tmRival` import variable renamed `tmDivision`). Also fixed: placement loops no longer skip one portal player per placement (`remove(i)` + `++i`), and the K/DL/LB/CB/S FCS checks no longer NPE on a null `userTeam`.
4. ✅ **Headless-safe recruiting** (D16). `SeasonController.autoCompleteRecruiting()` runs the CPU pass and rolls the year with no UI; the headless 3-year exit path no longer needs the bulk-sim step cap (the desktop cap remains as a bug guard only).

*Exit criteria check: same-seed full-year replay test green; full `gradlew test` suite green; `desktopVerify` green.*

### Phase 1 — Make advancing a week feel like an event (texture from existing data) ✅

*Goal: consequences and ceremony using data the engine already produces. Big perceived-value-per-LOC.*

1. ✅ **Weekly poll release**: `League.snapshotPollRanks()` copies current ranks to a persisted per-team `prevRankTeamPollScore` (new optional 12th field on the save's `T:` lines) before `setTeamRanks()`, then `releaseWeeklyPoll()` publishes a "New Top 25 Released>" news story each regular week with movement markers (▲n / ▼n / NEW) and headlines for the No. 1 team and the user's ranking.
2. ✅ **Week digest**: `League.buildWeekDigest(weekPlayed)` assembles the week-in-review — user result (or BYE), top-10 plus the user's team with poll movement, this week's headlines, the user injury report, and the next matchup. It travels as data on `SeasonAdvanceResult.getWeekDigest()`. Desktop appends it to the existing game-result dialog (and shows a digest-only dialog on BYE weeks); Android auto-popups a "Week In Review" dialog after every in-season advance (result-feedback parity). User-game lookup uses schedule-slot logic mirroring `DesktopWeekResult` (freshly scheduled games have no `week` set — only saves do).
3. ✅ **Real morale backend**: `TeamMoraleSnapshot` (chemistry / leadership / buy-in, 0-100) computed in `Team.getTeamMoraleSnapshot()` from season chemistry, top-11 roster character, staff discipline, the active win streak, and the HC's discipline-culture skill rank. Chemistry remains the exact value `Game.getTeamChemistryAdv()` already consumes (no balance change). The desktop Team Morale card renders the live snapshot — fake 82/78/85 constants removed (D13 closed) — with a morale-reactive smiley and checklist.
4. ✅ **Wire dead audio events**: `SeasonAdvanceResult.getAudioEvent()` carries a result-atmosphere cue the shells play: WHISTLE on the preseason kickoff, CROWD_ROAR for conference-championship/bowl/playoff weeks, STADIUM_ORGAN for the national championship, a CROWD_ROAR upset cue for beating a better-ranked opponent, TOUCHDOWN_CHEER for a routine win. Headless hosts read the same event; no audio code entered the engine (D13's audio sibling from the audit's dead-SFX list).
5. ✅ **Fill the no-op weeks**: R+6 now generates coaching-carousel interest news (user HC courted after a 65%+ win season or top-20 ranking, plus two league-wide carousel buzz headlines); R+11 generates per-team portal-need headlines (positions below roster minimums, seeded shuffle, capped at six) (D15 closed).

*Exit criteria: a regular-week advance visibly changes polls/news/morale/audio on both shells; Android parity for result feedback. Verified by `WeekTextureTest` (5 tests: weekly release + movement, digest content + audio mapping, morale bounds + character response, both formerly-dead weeks, prevRank save round-trip).*

### Phase 2 — Game-day environment (the missing sim layer) ✅

*Goal: model the things every premium football title models. All engine-side, all behind the Phase 0 RNG service, all persisted in the existing box score.*

1. ✅ **Weather** (`simulation/Weather`): generated once per game from home region + calendar week (rain year-round; snow only in cold-region codes and late season, per `COLD_REGION_CUTOFF`). Effects: pass completion −3 (rain) / −5 (snow), rushing −2 in snow, FG accuracy −2..−4, wet-field fumbles ×1.25, run-lean tendency in wet weather. Printed in the PBP header (which persists with the save) and flows through the box-score pipeline.
2. ✅ **Penalties**: pre-snap flag check each play, rate scaled by `(100 − staffDiscipline)` — undisciplined staffs draw 2-3× the flags. Offensive flags (false start/holding) back the offense up and replay the down; defensive flags give 5 yards with a 50% automatic first down. Counted per team and rendered as a "N for Y yds" row in both box-score summaries — closing the loop with the existing discipline system.
3. ✅ **Momentum** (`simulation/Momentum`): home-perspective value in [−1, 1]; scores push it toward the scorer (+0.05..0.22 by points), turnovers +0.18, every snap decays ×0.995. The per-play effect is a bounded ±2 variance modifier inside the same `environmentAdj` term as weather; momentum also swings on turnovers (INTs, downs, blocked/fake punts).
4. ✅ **Timeouts**: real `homeTimeouts`/`awayTimeouts` (3 per half, reset at halftime; distinct from the turnovers renamed in Phase 0). The trailing team banks one at the fourth-quarter turn, and on short-margin late kicks the defense can ice the kicker — consuming a timeout and raising the kicker's pressure bar (95 → 120).
5. ✅ **Halftime adjustments**: at the half, a trailing staff rolls for a second-half edge — chance scales with the Game-Prep skill rank and a >80 tactical rating; success posts "X makes adjustments at the half" in the PBP and a +1 second-half play modifier.
6. ✅ **Special-teams texture**: blocked field goals (1.2% — defense takes over), blocked punts (1% — return team gets great position), desperate fake punts (trailing by 9+ in the fourth, own territory, 8% — first down or stuffed on downs), and punt fair catches deep in own territory.
7. ✅ **Crowd**: home-crowd lift (0..2) from 80+ prestige and a 3+ win streak, folded into `environmentAdj` with the visiting side taking half the effect as hostile-crowd jitter; championship/bowl/semifinal sites are neutral (mirrors the HFA rule).

*Exit criteria: `GameEnvironmentTest` (weather determinism + variety, weather/clamp effects via `environmentAdj`, penalty rates + box-score rows, momentum clamps + real-game bounds, weather in persisted PBP) and `GameBalanceTest` (two seeded full seasons: combined scoring in a 30–85 band, penalties 1–25/game, ≤25% drift across seeds) all green; `SeededReplayTest` proves the whole environment replays deterministically.*

*Bonus determinism catch: the standalone gate exposed two residual unseeded `Collections.shuffle` calls (CPU roster cuts at `Team.cpuCutPlayers` and the mock-draft pool at `League.getMockDraftPlayersList`) that the Phase 0 sweep missed — time-seeded shuffles desynced the league stream between "identical" replays depending on data. Both now route through `SimRandom.shuffle`, and the full-year replay is byte-identical in both the app and standalone test environments.*

### Phase 3 — Pageantry & identity (calendar ceremony) ✅

*Goal: make the schedule feel like a real college football calendar.*

1. ✅ **Rivalry games**: `League.generateRivalries()` runs at every schedule build — teams without a declared rival are paired with their nearest geographic neighbor (cross-conference preferred, so those rivalries live on the OOC slate), with a generated trophy name ("The Bucket (CLE-PIT)" style). Universes can declare rivals via an optional 7th column on team CSV lines (both import paths read it). `ScheduleManager` biases the OOC pairing toward rivals sharing an OOC window, and a guarantee pass swaps two FCS filler games for a head-to-head when an unmet cross-conference pair exists. Every rivalry game is tagged (`Game.rivalryGame`), gets a PBP header line and a +1 marquee-crowd nudge, and post-game the winner claims/retains the trophy with league news and all-time `rivalryWins` tracking. Rivalry bookkeeping persists as four optional fields on the save's `T:` lines (rival name, trophy name, wins, trophy holder).
2. ✅ **Senior day**: each team's final home game is tagged `Game.seniorDay` — PBP header line, +1 home crowd nudge, and a user-team "honors its senior class" headline that week.
3. ✅ **Homecoming**: each team's home game nearest mid-season is tagged `Game.homecomingGame` — same presentation treatment plus a 15% attendance revenue bump in the game-economy block.
4. ✅ **Bowl tie-ins**: `scheduleConferenceTieInBowls` runs before the at-large snake — conferences are ranked by average prestige and paired off, each sending its conference champion (or, when the champ isn't bowl-eligible, its best 6-win team) into the six marquee bowl slots; paired teams leave the at-large pool and publish "Bowl Bids" acceptance news. (Also surfaced and fixed a latent Phase 0 regression the new test caught: expanded-playoff mode had lost its bowl-scheduling trigger when the idempotence guard landed — bowls were never scheduled alongside the first round.)
5. ✅ **Uniforms (cosmetic, scoped down)**: rivalry games carry an explicit "RIVALRY GAME: {trophy}" line in the persisted play-by-play plus trophy/bid news — the presentation layer ships through PBP/news rather than UI color swaps; a `TeamColors`-based banner treatment remains a small follow-up for the shells.

*Exit criteria: `PageantryTest` (mutual rivalry generation with ≤1 odd-team exception, exactly-one senior day/homecoming per team with senior day = final home game, ≥20 scheduled rivalry matchups, trophy processing + news after a played rivalry game, full field round-trip) and `BowlTieInTest` (marquee slots filled, ≥1 champion-vs-champion pairing, bid news) green alongside the Phase 0–2 regression suites.*

### Phase 4 — Career & meta depth (systems with teeth) ✅

*Goal: convert scalar systems into decisions.*

1. ✅ **Coach salaries & buyouts**: salaries are **derived, never persisted** (saves stay compatible) — `headCoachSalary()` from program prestige, coach pedigree and ratings ($0.5M–$12M band); coordinators cheaper. Every head-coach firing now charges a buyout (half the salary × years remaining) to the athletic budget in career mode, and the firing news names the figure. The user contract copy appends the salary and the AD-approval line. An AD-approval < 25 also withdraws the "prove-it" mercy deal for a struggling coach.
2. ✅ **Stadium & attendance arc**: the dead `teamStadium` field is alive — capacity tiers (35k base, +12k per expansion, max tier 5), persisted as a new optional `T:` field; regular-game revenue now scales with butts-in-seats (demand from prestige, record, homecoming/rivalry, capped by capacity, bounded ±15% vs the old path); offseason `upgradeStadiums()` lets rich, prestigious programs expand. `enableTV` turned out to already gate the Conference TV negotiation/profit-sharing engine (the audit was stale there) — profit sharing pays member budgets, now covered by a focused test.
3. ✅ **AD/booster approval meter**: `TeamFinance.getAdApproval()` — a 0–100 derived meter from season prestige change, win percentage, and program discipline; shows in the contract copy, gates prove-it deals, and the hot-seat string stays for the roster UI.
4. ✅ **Polls with voices**: weekly AP (voter noise 0.25) and Coaches (0.10) ballots re-ranked from the composite poll score with seeded per-team noise; published as a "Media Polls>" news story with movement arrows, plus a "Poll Chaos: AP and Coaches split on No. 1" headline when they disagree. The CFP committee keeps using the existing composite ranking. Session-only (no persistence); composite prev-rank stays persisted for the committee poll.
5. ✅ **Awards ceremony 2.0 (first increment)**: new **Lineman of the Year** award following the Defensive POTY pattern exactly (candidate builder in `LeagueAwards` style, ceremony string, news, `wonHeisman`/coach tallies, season reset) and surfaced in the awards-week summary alongside the offensive POTY.
6. ✅ **Draft night**: at offseason graduation, every departing player (seniors + early NFL entries) enters a seeded draft pool — a "Draft Night>" news story publishes the first round with rounds/picks and per-team credit headlines ("X produced N first-round draft picks").
7. ✅ **Signing day**: `finishRecruitingSeason` publishes a "Signing Day>" ceremony as the new season begins — top-5 class rankings from the recomputed class ratings plus user-class headlines. Fully headless via the existing `autoCompleteRecruiting()` path.

*Exit criteria: `CareerDepthTest` (salary band, firing-buyout charge + news, approval bounds/direction, capacity + expansion + round-trip, weekly media-poll story, lineman award latch, draft-night and signing-day news, TV profit-sharing payout) green alongside all prior-phase suites.*

*Data audit (post-Phase 5): `TeamColors` was written for the old fictionalized universe — 57 of 135 real teams rendered with gray fallbacks. All 57 received real brand pairs, Kennesaw State's duplicate "KSU" abbreviation became "KENN", and the FCS filler pool lost its stale entries (Delaware, James Madison, North Dakota State — now FBS members) and "St"-style spellings. Guarded by `TeamNamingAndColorsTest`.*

*Bug caught by the gates: the first media-poll implementation rolled voter noise **inside** the sort comparator, violating the TimSort contract ("Comparison method violates its general contract") whenever the inconsistent comparisons crossed a merge boundary — data-dependent, so it passed some runs and failed others. Voter-noise scores are now drawn once per team per poll up front and the comparator is pure. Rule of thumb going forward: never draw RNG inside a `Comparator`.*

### Phase 5 — Interactive gameday & shell parity (the frontier) ✅

*Being executed in three parts. Part 1 (engine core) is done; parts 2–3 (Android parity, shell UI/offseason hub) follow.*

**Part 1 — Interactive gameday engine core ✅**

Opt-in coaching checkpoints for the user team's games, wired into the existing sim without restructuring it:

- **`GameCoachPlan`**: one decision value — offensive/defensive scheme switch (-1 = unchanged), fourth-down aggression bias (−2..+2), halftime focus (aggressive/balanced/conservative), timeouts to burn (0–3), and a crunch fourth-down intent (auto/punt/field goal/go-for-it). Everything optional; an empty plan defers fully.
- **`GameCoachListener`**: a single synchronous `decide(game, checkpoint)` hook fired at three checkpoints — PREGAME (before the first snap), HALFTIME (locker room, at the Q2→Q3 turn), CRUNCH_TIME (start of the fourth quarter). Installed per league via `League.setGameCoachListener` (session-only setting, default off); shells show their modal decision UI from inside the callback, exactly like the existing career dialogs, and headless tests answer with scripted plans.
- **Effects, all bounded and restore-safe**: pre-game/halftime scheme changes mutate the playbooks for the coached game only — the coach plan is applied *after* the Phase 0 playbook stash, so the D8 end-of-game restore puts the true season books back (verified by test). Halftime focus feeds a ±1 second-half term in `environmentAdj`. Crunch timeouts convert to saved clock (+35s each, capped by timeouts actually left) and the fourth-down intent overrides the crunch-time dispatch (with sane fallbacks: no punting deep in opponent territory, no kicking from midfield). The aggression bias shifts the AUTO fourth-down thresholds only for the coached team.
- **Safety properties, all test-pinned**: no listener → byte-identical simulation (the Phase 0 replay suite is unchanged); CPU-vs-CPU games never invoke the hook even when a listener is installed; checkpoints fire exactly once each per game; identical seeds with identical scripted coaching replay identical seasons.

*`InteractiveGamedayTest` (5 tests) covers the un-coached path, scheme apply + season-book restore, checkpoint cadence + narration, CPU-game exclusion, and coached-season determinism.*

*Parity follow-up (post-part 3): both shells silently DROPPED the midseason report, awards ceremony, season summary, and realignment recap when a bulk run passed their checkpoint — content loss on "Sim Through Postseason" / "Advance Through Offseason". Both now defer those four informational dialogs during bulk and replay them as stacked modals when the run finishes (Android queues into `pendingBulkDialogs`; desktop via `DesktopUiBridge.drainDeferredDialogs()` on all three bulk finish paths). Android also gained a morale display (the engine `buildProgramSummary` now includes the Phase 1 morale readout, shown by both shells' Coach Program dialogs), and bulk runs no longer trigger a full `resetUI()` per advanced week.*

**Part 2 — Android parity ✅**

- **Bulk simulation**: two new drawer items — "Sim to Next Decision" (runs until a decision dialog is needed, the recruiting gate, or a season rollover) and "Sim Through Postseason" (stops at the season-summary step). A background worker advances the sim while each engine step hops onto the UI thread via a latch (per `docs/THREADING.md`, the engine stays single-threaded on the main thread), with a cancellable progress dialog. During the run, decision dialogs (contract, job offers, promotions, coordinator hiring, redshirt list, transfer list, discipline, portal accept/decline) are **queued** as re-invokable actions and the first queued dialog shows when the run ends; informational dialogs (notifications, midseason/season summaries, awards, realignment, week digests) are suppressed, with the final week digest shown at the finish. A 60-step hard cap guards against runaway loops.
- **Midseason saves lifted**: the drawer and options-menu save entries no longer block during a season — the structured save format round-trips any point in a season (the old ban dated from the legacy format that only restored season boundaries). Recruiting checkpoints keep their dedicated slot path, and saving is refused only while a bulk run is active.

*Android-side threading/UI is exercised by `:app:lintDebug` and manual smoke; the engine path it drives (`SeasonController.advanceWeek` + `SeasonAdvanceResult` stop events) is the same headless-tested surface.*

**Part 3 — Desktop interactive UI + offseason hub ✅**

- **Interactive coaching UI**: a `JCheckBoxMenuItem` ("Interactive Coaching") in the desktop Season menu installs/removes the Part 1 listener. While enabled, the user team's games pause at each checkpoint for `CoachDecisionDialog` — Gameplan (offensive/defensive scheme pickers with live strategy names, fourth-down aggression spinner), Halftime (locker-room focus + optional scheme switches), Crunch Time (timeouts to burn, fourth-down intent). "Let the CPU decide" (or Escape) defers with a null plan, making the dialog itself the per-game opt-in. Keyboard-driven: Enter applies, mnemonics on choices, Escape backs out.
- **Header phase chips**: the desktop header replaces the raw "WEEK 17" counter with `SeasonPresentation`'s phase and week chips ("PHASE OFFSEASON • WEEK 19"), reusing the Android presentation layer directly.
- **Dashboard panels wired**: `ProgramHealth`, `PollLeaders`, `Awards`, and `LatestHeadlines` — built during the dashboard redesign but never mounted — are now in the dashboard grid, fed with real data by Phases 1–4.
- **Offseason hub (desktop)**: a Season-menu "Offseason Hub" screen (enabled during the offseason) lists the ten steps from the new `SeasonFlowOrder.offseasonSteps()` with the current step highlighted; "Advance One Step" (Enter) drives the shell's normal week advance, so decision steps still pop their own dialogs on top. On Android, part 2's "Sim to Next Decision" bulk item already delivers the same stop-at-each-decision behavior; a dedicated hub screen there remains optional future polish. (Interaction note preserved from part 2: the coaching listener must defer while a bulk run is active.)

*Tests: `OffseasonStepsTest` (step list shape + week→step mapping across season lengths) alongside the full prior-phase regression set.*

---

### Suggested order & rough sizing

| Phase | Theme | Size | Depends on |
|:--|:--|:--|:--|
| 0 | Integrity + seeded RNG | M (D2/D8/D9 are the real work) | — |
| 1 | Week texture (polls, digest, morale, audio) | M | Phase 0 (seed for poll noise) |
| 2 | Game-day environment | L | Phase 0 |
| 3 | Pageantry (rivalries, bowls, ceremonies) | M | Phase 0 (save migrator), Phase 1 (news/morale) |
| 4 | Career depth (money, polls, draft) | L | Phase 0; morale (1) and environment (2) recommended |
| 5 | Interactive gameday + parity | XL | Phases 0–2 |

### Verification strategy (applies to every phase)

- Engine tests: `./gradlew test` — add a seeded full-year replay fixture per phase; regression tests for every D-item fixed in Phase 0.
- Cross-platform gate: `./gradlew desktopVerify` (engine import scan + resource contract) and `:app:lintDebug` before UI phases.
- Balance check: 10-season seeded headless sims, comparing scoring/injury/portal rates pre/post each Phase 2 change (guard against silent difficulty drift).
- Manual: the existing desktop snapshot tooling (`UiSnapshotTool`) for shell regression.

### Explicit non-goals

- No online/cloud features, accounts, or IAP (the app is fully offline by policy).
- No multiplayer.
- No rewrite of `League`/`Team` god objects here — that is [ROADMAP.md](ROADMAP.md) #3 and proceeds separately; new premium systems must *add focused classes* (e.g. `WeatherService`, `RivalryManager`, `MoraleEngine`) rather than grow the god objects.
