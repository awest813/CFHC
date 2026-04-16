
# College Football Head Coach (CFHC)

> A college football dynasty simulation — recruit, game-plan, and manage your roster to build a championship program. Open-source, offline, and fully customizable.

---

## Project Overview

CFHC is an expansion of the original _College Football Coach_ game. The simulation engine covers recruiting, game-day strategy, player progression, coaching staff, conference structures, and more — all wrapped in a career-mode loop where you can get hired, win titles, or get fired.

The game is **Android-first** today, but the core simulation is being extracted into a platform-independent layer so the same engine can power future iOS and desktop clients (see [Roadmap](docs/ROADMAP.md) and [Platform Expansion](docs/platform-expansion.md)).

| | |
|---|---|
| **Current version** | v1.4.5 |
| **Platform** | Android (minSdk 24 / targetSdk 30) |
| **Language** | Java 17 |
| **Build tool** | Gradle 8.7 |
| **Codebase** | ~35 000 LOC across 173 files in 7 packages |

---

## Features

### General
- Career mode — start as a new hire and work your way up
- Hire offensive and defensive coordinators
- Import custom universes, rosters, and coaches from CSV
- Edit team names, conference names, and coach names in-game
- Realistic conference and team structures
- Bowl games + 4-team playoff (expanded playoff option available)
- 7 000+ first/last name database with geographic home regions
- Television contract deals
- Conference realignment, promotion/relegation, and random team generation options
- Full league, team, coach, and player stat history
- Player awards and year-end honors
- Box scores, play-by-play logs, and dynamic news articles
- Material Light and Dark themes

### Simulation Engine
- Play-by-play game simulation with realistic outcomes
- Mid-season and end-of-season player progression
- Head coach and coordinator influence on progression
- Multiple offensive and defensive playbooks
- Full offensive, defensive, and special-teams stat tracking
- Off-season coaching changes and infraction system
- Computer poll logic, school prestige dynamics
- Standard and medical redshirting
- Undergraduate and graduate transfers
- Player suspensions and dismissals
- Team facilities and basic monetary system

---

## Architecture

```
src/main/java/
├── simulation/      Core engine — League, Team, Game, conferences, records
│                    (~18K LOC, platform-independent)
├── positions/       Player models — 11 position classes + base Player
│                    (~4.6K LOC, platform-independent)
├── staff/           Coaching staff — HeadCoach, OC, DC
│                    (~0.9K LOC, platform-independent)
├── comparator/      Sorting/ranking comparators (77 files)
│                    (~1.3K LOC, platform-independent)
├── recruiting/      Recruiting flow — session data, controllers, UI
│                    (~1.5K LOC, partially Android-coupled)
├── ui/              Android list adapters, profiles, roster views
│                    (~1.8K LOC, Android-only)
└── antdroid/        Android shell — activities, dialogs, navigation
                     (~6.6K LOC, Android-only)
```

**Platform bridge interfaces** (already implemented):
- `GameUiBridge` — UI callback abstraction with a `NO_OP` default
- `PlatformResourceProvider` — asset/string loading
- `GameFlowManager` — state-transition orchestration
- `PlatformLog` — logging shim (replaces `android.util.Log`)

---

## Engine Audit Summary

A full audit of the simulation engine was completed in April 2026. Key findings:

| Area | Rating | Notes |
|------|--------|-------|
| Architecture | 6/10 | Good platform separation; League and Team are still "God Objects" (~6.5K and ~5.5K LOC) |
| Code Quality | 5/10 | Public mutable collections, minimal error handling, `System.out.println` in production |
| Testability | 3/10 | No unit tests; public mutable state makes isolated testing difficult |
| Portability | 4/10 | Recruiting package has circular Android coupling; save/load tied to `MainActivity` |
| Dependency Health | 5/10 | `targetSdk 30` lags behind `compileSdk 35`; AndroidX libraries ~2 years outdated |

Top issues identified:
1. **God Objects** — `League.java` (6 485 LOC, 109 public methods) and `Team.java` (5 487 LOC) need decomposition
2. **Public mutable fields** — 46 in League, 31 in Team; callers can bypass all validation
3. **Recruiting ↔ MainActivity circular dependency** — blocks platform expansion
4. **Silent I/O failures** — `IOException` caught and swallowed without user feedback
5. **No test suite** — zero automated tests

See the full [Roadmap](docs/ROADMAP.md) for the prioritized remediation plan.

---

## Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 17+
- Android SDK with API level 35

### Build & Run
```bash
git clone https://github.com/awest813/CFHC.git
cd CFHC
./gradlew assembleDebug
```
Install the APK on a device or emulator, or run directly from Android Studio.

---

## FAQ

**Saving games**
The game saves at the start of each season (a pop-up will prompt you). You can also save just before recruiting begins. Save files always restore to the beginning of the season in which they were created.

**Viewing play-by-play**
Open **Schedule → tap a score box → tap the blue menu bar** at the top of the pop-up.

---

## Contributing

This project is free and open-source. Contributions, bug reports, and feature suggestions are welcome:
- Open an issue or pull request on GitHub
- Join the community at [/r/footballcoach](https://www.reddit.com/r/footballcoach/)

---

## License

See [LICENSE](LICENSE) for details.
