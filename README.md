
<div align="center">

# 🏈 College Football Head Coach (CFHC)

**Build a dynasty. Recruit legends. Win championships.**

*An open-source college football dynasty simulation for Android, with a Swing desktop prototype built on the same core engine.*

[![Version](https://img.shields.io/badge/version-v1.4.5-blue?style=flat-square)](https://github.com/awest813/CFHC/releases)
[![Platform](https://img.shields.io/badge/platform-Android%20%2B%20Desktop%20Prototype-green?style=flat-square&logo=android)](https://developer.android.com)
[![Language](https://img.shields.io/badge/language-Java%2017-orange?style=flat-square&logo=java)](https://openjdk.org)
[![Build](https://img.shields.io/badge/build-Gradle%208.9%20%7C%20AGP%208.7-lightgrey?style=flat-square)](https://gradle.org)
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
- [Project Status Snapshot](#project-status-snapshot)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

CFHC is an expanded take on the original *College Football Coach* game. It wraps a deep, play-by-play simulation engine in a full career-mode loop — get hired, recruit, call plays, advance your coaching career, or get fired.

The Android app runs **100% offline** with no ads, no in-app purchases, and no accounts required. The repository also includes a **Swing desktop prototype** that exercises the same shared simulation code while the project continues its broader platform-decoupling work.

| | |
|:---|:---|
| **Version** | v1.4.5 |
| **Platform** | Android — minSdk 24, targetSdk 35; Swing desktop prototype |
| **Language** | Java 17 |
| **Build** | Gradle 8.9 wrapper · Android Gradle Plugin 8.7 |
| **Codebase** | ~46,000 Java LOC · 207 production files · 8 packages |
| **Tests** | 6 JUnit tests covering simulation, recruiting, comparators, and save/load |

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

### Desktop Prototype
- Swing launcher and in-game shell built from the shared Java simulation core
- Desktop dark mode, keyboard/mouse workflows, and multi-tab league views
- Shared export support for league files and portable resource loading

---

## Architecture

```
src/main/java/
├── simulation/      Core engine — League, Team, Game, conferences, records
│                    (~21K LOC, platform-independent)
├── positions/       Player models — 11 position classes + base Player
│                    (~4.7K LOC, platform-independent)
├── staff/           Coaching staff — HeadCoach, OC, DC
│                    (~0.9K LOC, platform-independent)
├── comparator/      Sorting/ranking helpers (77 comparator files)
│                    (~1.3K LOC, platform-independent)
├── recruiting/      Recruiting flow — controller, activity, session/presentation state
│                    (~1.7K LOC, partially Android-coupled)
├── ui/              Android list adapters, profiles, roster views
│                    (~1.8K LOC, Android-only)
├── antdroid/        Android shell — activities, dialogs, navigation
│                    (~6.5K LOC, Android-only)
└── desktop/         Swing desktop shell — launcher, league home, dialogs, theming
                     (~8.1K LOC, desktop-only)
```

### Platform Bridge (already implemented)

The simulation layer communicates with host shells through four thin interfaces:

| Interface | Purpose |
|:---|:---|
| `GameUiBridge` | UI callbacks — crash handling, recruiting hand-off, discipline prompts; includes a `NO_OP` default |
| `PlatformResourceProvider` | Asset and string loading, decoupled from Android resources |
| `GameFlowManager` | State-transition orchestration (start game, advance season, recruiting hand-off, etc.) |
| `PlatformLog` | Logging shim that replaces `android.util.Log` |

Android and the desktop prototype already use this bridge. See [Platform Expansion](docs/platform-expansion.md) for the current cross-platform status.

---

## Getting Started

### Prerequisites

| Tool | Version |
|:---|:---|
| Android Studio | Latest stable |
| JDK | 17+ |
| Android SDK | API level 35 |

### Android Build & Test

```bash
git clone https://github.com/awest813/CFHC.git
cd CFHC
./gradlew test
./gradlew assembleDebug
```

Install the generated APK from `build/outputs/apk/debug/` onto a device or emulator, or run directly via **Run ▶** in Android Studio.

### Desktop Prototype

```bash
./gradlew desktopJar
./gradlew runDesktop -PdesktopArgs="new"
```

You can also load an exported save with `./gradlew runDesktop -PdesktopArgs="play path/to/save.cfb"`.

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

<details>
<summary><strong>Does the repository include automated tests?</strong></summary>

Yes. The current JUnit suite covers comparator ordering, recruiting session behavior, a full-season simulation flow, and save/load round trips. Coverage is still limited, but the project no longer starts from zero.
</details>

<details>
<summary><strong>What does the desktop prototype support today?</strong></summary>

The Swing shell can launch a new dynasty, load exported saves, play through seasons, handle recruiting in a docked tab, and export leagues. It is still a prototype, so some Android flows remain more complete.
</details>

---

## Project Status Snapshot

An April 2026 audit identified the project's biggest technical risks. Several high-priority items from that audit have already been addressed, so the current snapshot is:

| Area | Score | Notes |
|:---|:---:|:---|
| Architecture | 6 / 10 | Good platform separation; `League` and `Team` remain God Objects (~6.5K and ~5.5K LOC) |
| Code Quality | 6 / 10 | Logging and save-failure handling improved; public mutable collections still need cleanup |
| Testability | 5 / 10 | Initial JUnit coverage exists, but core engine isolation is still difficult |
| Portability | 6 / 10 | Platform bridge, shared save/export services, and desktop shell are in place; no headless facade yet |
| Dependency Health | 7 / 10 | `targetSdk` now matches `compileSdk`; AndroidX stack is current enough for API 35 work |

**Top remaining issues:**

1. **God Objects** — `League.java` (6,485 LOC, 109 public methods) and `Team.java` (5,487 LOC) need decomposition
2. **Public mutable fields** — 46 in `League`, 31 in `Team`; callers bypass all validation
3. **No headless simulation facade** — alternate shells still depend on UI-oriented orchestration
4. **Import flow is still Android-heavy** — custom-universe parsing is shared, but roster/coach import orchestration still lives in Android classes
5. **Test coverage is still thin** — the existing suite needs broader scenario and regression coverage

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
