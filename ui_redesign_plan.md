# Franchise Dashboard & UI Redesign Plan

## Objective
Create a "crisp," high-fidelity sports broadcast console HUD interface for **College Football Head Coach (CFHC)** that adopts the layout, visual identity, and information density of the reference design, featuring dark obsidian backdrops, slate card containers, neon emerald highlights, trophy gold accents, and custom pixel-art player spotlight components.

---

## Visual Identity & Design System
- **Color Tokens**:
  - `Midnight Obsidian`: `#060c14` (Window & Viewport Background)
  - `Deep Navy Slate`: `#0d1726` / `#111c2e` (Card Containers & Surface Panels)
  - `Neon Emerald Green`: `#00e676` / `#10b981` (Overall Ratings, Active Selection, Positive Morale)
  - `Trophy Gold`: `#f59e0b` / `#fbbf24` (Star Ratings, Rankings, Prestige Badges)
  - `Crimson Maroon`: `#881337` / `#9f1239` (Matchup Opponent Highlights & Alerts)
  - `Dark Slate Border`: `#1e293b` (1px Panel Dividers & Outer Borders)
- **Typography**:
  - **Headlines & School Titles**: `Outfit` / `Inter` Bold Display.
  - **Script Mascot Accents**: `Caveat` Cursive ("*Owls*").
  - **Numeric Data & Metrics**: `JetBrains Mono` Monospaced font for clean vertical alignment.

---

## Interface Layout (11-Card Dashboard Grid)

### 1. Top Broadcast Header
- Team Primary Shield & Crest Logo
- School Name (**PINE VALLEY STATE**) + Script Nickname (*Owls*)
- Season / Week Indicator (`2026 SEASON • WEEK 8`)
- Head Coach Info (`HC ELIJAH CARTER`, Career Record: `28-17`)
- Notification badge envelope (`[3]`)

### 2. Left Vertical Navigation Sidebar
- 15 navigation options (`DASHBOARD`, `TEAM MANAGEMENT`, `ROSTER`, `DEPTH CHART`, `GAME PLAN`, `RECRUITING`, `SCOUTING`, `TRAINING`, `SCHEDULE`, `STATS & HISTORY`, `CONFERENCE`, `FACILITIES`, `FINANCES`, `PROGRAM PRESTIGE`, `SETTINGS`)
- Active neon green indicator bar
- Yellow pill badge for recruiting alerts (`14`)
- School Seal Footer Emblem ("EST. 1898")

### 3. Main Dashboard Grid (11 Cards)
- **Team Overall**: Giant `82` green rating, `B+` grade, 4-star display, Offense/Defense/Special Teams sub-cards, National #24 & Conf #3 ranks.
- **Next Game Matchup**: Split team cards (Pine Valley State Owls 5-2 vs Redwood University Maroons 4-3), kickoff time, stadium location.
- **Top News Carousel**: Hero stadium image, headline, dot pagination.
- **Conference Standings**: Great North 8-team table with rank highlights.
- **Weekly Schedule**: Mon-Sun timeline with activity icon badges.
- **Recruiting Pipeline**: US Map visual with regional target pins and "VIEW RECRUITING BOARD" action button.
- **Program Finances**: Budget ($34.2M), Current Balance ($5.8M), Weekly Spend ($642K).
- **Program Prestige**: Metallic 3D Shield (#78 RISING) with progress bar fill.
- **Team Morale**: Smiley gauge, win streak checklist, Chemistry (82), Leadership (78), Buy-In (85) sliders.
- **Roster Spotlight**: Dual pixel-art player cards (QB Mason Harrison 88 OVR & LB Jalen Bryant 84 OVR) with stats & last game line.
- **Upcoming Games**: Schedule preview (Weeks 9-13) with difficulty stars.

### 4. Bottom Status & Audio Bar
- Gamepad Controller Legend (`(A) SELECT`, `(B) BACK`, `(Y) HELP`)
- Audio Soundtrack Ticker ("Campus Drive — Midnight Rally") + Equalizer Visualizer

---

## Execution Status

- [x] **Web Live Sandbox Overhaul (`preview/`)**: Rebuilt `index.html`, `styles.css`, and `app.js` into an interactive replication of the screenshot dashboard.
- [x] **Desktop Theme Modernization (`src/main/java/desktop/DesktopTheme.java`)**: Updated default dark theme colors to obsidian, navy slate, neon emerald, and gold tokens.
- [x] **Verification**: Verified via test suite and preview setup.
