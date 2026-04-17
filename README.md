
<div align="center">

# 🏈 College Football Head Coach (CFHC)

**Build a dynasty. Recruit legends. Win championships.**

*An open-source college football dynasty simulation for Android — recruit, game-plan, and manage your program from the ground up.*

[![Version](https://img.shields.io/badge/version-v1.4.5-blue?style=flat-square)](https://github.com/awest813/CFHC/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20(API%2024%2B)-green?style=flat-square&logo=android)](https://developer.android.com)
[![Language](https://img.shields.io/badge/language-Java%2017-orange?style=flat-square&logo=java)](https://openjdk.org)
[![Build](https://img.shields.io/badge/build-Gradle%208.7-lightgrey?style=flat-square)](https://gradle.org)
[![License](https://img.shields.io/badge/license-see%20LICENSE-informational?style=flat-square)](LICENSE)
[![Community](https://img.shields.io/badge/reddit-%2Fr%2Ffootballcoach-FF4500?style=flat-square&logo=reddit)](https://www.reddit.com/r/footballcoach/)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [FAQ](#faq)
- [Engine Audit Summary](#engine-audit-summary)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

CFHC is an expanded take on the original *College Football Coach* game. It wraps a deep, play-by-play simulation engine in a full career-mode loop — get hired, recruit, call plays, advance your coaching career, or get fired.

Everything runs **100% offline on Android** with no ads, no in-app purchases, and no accounts required. The simulation core is being progressively decoupled from Android so the same engine can eventually power iOS and desktop clients.

| | |
|:---|:---|
| **Version** | v1.4.5 |
| **Platform** | Android — minSdk 24, targetSdk 30 |
| **Language** | Java 17 |
| **Build** | Gradle 8.7 |
| **Codebase** | ~35,000 LOC · 173 files · 7 packages |

---

## Features

### Career Mode
- Start as a new hire and build your reputation season by season
- Get hired, earn promotions, or face the hot seat
- Hire and manage offensive and defensive coordinators
- Team facilities investment and a basic monetary system
- Full coaching-staff influence on player development

### Recruiting
- Full off-season recruiting cycle with prospect scouting
- 7,000+ first/last name database with geographic home regions
- Scholarship management and roster-size limits
- Undergraduate and graduate transfers
- Standard and medical redshirt options

### Game Simulation
- Tick-by-tick play-by-play simulation with realistic outcomes
- Multiple offensive and defensive playbooks
- Full offensive, defensive, and special-teams stat tracking
- Box scores, play-by-play logs, and dynamic news articles
- Mid-season and end-of-season player progression

### League & Structure
- Realistic multi-division conference structures
- Bowl games + 4-team playoff (expanded playoff option available)
- Conference realignment, promotion/relegation, and random team generation
- Computer poll logic and dynamic school prestige
- Off-season coaching changes and program infraction system
- Full league, team, coach, and player stat history
- Player awards and year-end honors

### Customization
- Import custom universes, rosters, and coaches from CSV
- Edit team names, conference names, and coach names in-game
- Television contract deals
- Material Light and Dark themes

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
├── comparator/      Sorting/ranking helpers (77 comparator files)
│                    (~1.3K LOC, platform-independent)
├── recruiting/      Recruiting flow — session data, controllers, UI
│                    (~1.5K LOC, partially Android-coupled)
├── ui/              Android list adapters, profiles, roster views
│                    (~1.8K LOC, Android-only)
└── antdroid/        Android shell — activities, dialogs, navigation
                     (~6.6K LOC, Android-only)
```

### Platform Bridge (already implemented)

The simulation layer communicates with any host platform through four thin interfaces:

| Interface | Purpose |
|:---|:---|
| `GameUiBridge` | UI callbacks — crash handling, recruiting hand-off, discipline prompts; includes a `NO_OP` default |
| `PlatformResourceProvider` | Asset and string loading, decoupled from Android resources |
| `GameFlowManager` | State-transition orchestration (start game, advance season, etc.) |
| `PlatformLog` | Logging shim that replaces `android.util.Log` |

> See [Platform Expansion](docs/platform-expansion.md) for the full multi-platform strategy.

---

## Getting Started

### Prerequisites

| Tool | Version |
|:---|:---|
| Android Studio | Latest stable |
| JDK | 17+ |
| Android SDK | API level 35 |

### Build & Run

```bash
git clone https://github.com/awest813/CFHC.git
cd CFHC
./gradlew assembleDebug
```

Install the generated APK from `build/outputs/apk/debug/` onto a device or emulator, or run directly via **Run ▶** in Android Studio.

---

## FAQ

<details>
<summary><strong>How do I save my game?</strong></summary>

The game saves automatically at the start of each new season — a prompt will appear. You can also manually save just before recruiting begins. Save files always restore to the beginning of the season in which they were created.
</details>

<details>
<summary><strong>How do I view play-by-play?</strong></summary>

Go to **Schedule → tap a score box → tap the blue menu bar** at the top of the pop-up.
</details>

<details>
<summary><strong>How do I import a custom universe or roster?</strong></summary>

Prepare your CSV files following the format documented in the sample universes included with the app, then use **Import** from the main menu. Teams, rosters, and coaches can all be imported independently.
</details>

---

## Engine Audit Summary

A full audit of the simulation engine was completed in April 2026. Key findings:

| Area | Score | Notes |
|:---|:---:|:---|
| Architecture | 6 / 10 | Good platform separation; `League` and `Team` remain God Objects (~6.5K and ~5.5K LOC) |
| Code Quality | 5 / 10 | Public mutable collections, minimal error handling, `System.out.println` in production |
| Testability | 3 / 10 | No unit tests; public mutable state makes isolation difficult |
| Portability | 4 / 10 | Recruiting package has circular Android coupling; save/load tied to `MainActivity` |
| Dependency Health | 5 / 10 | `targetSdk 30` lags `compileSdk 35`; AndroidX libraries ~2 years behind |

**Top issues:**

1. **God Objects** — `League.java` (6,485 LOC, 109 public methods) and `Team.java` (5,487 LOC) need decomposition
2. **Public mutable fields** — 46 in `League`, 31 in `Team`; callers bypass all validation
3. **Recruiting ↔ MainActivity circular dependency** — primary blocker for cross-platform expansion
4. **Silent I/O failures** — `IOException` caught and swallowed; users see blank save slots with no explanation
5. **No test suite** — zero automated tests

See the full [Roadmap](docs/ROADMAP.md) for the prioritized remediation plan.

---

## Contributing

CFHC is free and open-source. All contributions are welcome:

- **Bug reports & feature requests** — open an [issue](https://github.com/awest813/CFHC/issues)
- **Code contributions** — fork the repo, make your changes, and open a pull request
- **Community discussion** — join [/r/footballcoach](https://www.reddit.com/r/footballcoach/) on Reddit

Please keep pull requests focused and describe *what* changed and *why*. There is no formal style guide yet — match the conventions in the file you're editing.

---

## License

See [LICENSE](LICENSE) for details.
