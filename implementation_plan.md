# Multi-Stage Implementation Plan — Console Broadcast Dynasty Dashboard

This implementation plan outlines the full technical audit, design system integration, component architecture, data bindings, and step-by-step roadmap to make **College Football Head Coach (CFHC)** look like and operate like the reference sports broadcast dynasty dashboard screenshot.

---

## 1. Codebase Audit Findings & Architecture Mapping

### A. Java Swing Desktop App (`src/main/java/desktop/`)
1. **Top Broadcast Header Bar**:
   - `DesktopHeaderBar.java`: Renders school crest emblem logo, school title (**PINE VALLEY STATE**), script mascot accent (*Owls*), season/week tracker (`2026 SEASON • WEEK 8`), head coach profile badge (`HC ELIJAH CARTER`), and notification envelope indicator (`[3]`).
2. **Left Navigation Sidebar**:
   - `DesktopNavSidebar.java`: 15-item vertical menu (`DASHBOARD`, `TEAM MANAGEMENT`, `ROSTER`, `DEPTH CHART`, `GAME PLAN`, `RECRUITING`, `SCOUTING`, `TRAINING`, `SCHEDULE`, `STATS & HISTORY`, `CONFERENCE`, `FACILITIES`, `FINANCES`, `PROGRAM PRESTIGE`, `SETTINGS`) with active neon green selection fill (`#00E676`), yellow badge counter (`14`), and school seal emblem footer.
3. **11 Modular Cards Grid Suite**:
   - `TeamOverallCard`: Big 64px green rating digit (`82`), `B+` grade pill, 4-star gauge, Offense (84), Defense (81), Special Teams (76), National Rank #24 & Conf Rank #3.
   - `NextGameMatchupCard`: Home vs Away team matchup banners (Pine Valley State Owls 5-2 vs Redwood University Maroons 4-3), AT badge, kickoff date/time, and stadium details.
   - `TopNewsCarouselCard`: Hero banner background, headline `OWLS CLIMB TO #24 IN LATEST POLL`, snippet, dot pagination (`● ○ ○ ○ ○ ○`).
   - `ConferenceStandingsCard`: 8-team compact table highlighting user team row in emerald green.
   - `WeeklyScheduleCard`: Timeline list (Mon-Sun) with activity icon badges.
   - `RecruitingPipelineCard`: USA map vector graphic, regional pin badges, 14 commits count, and action button.
   - `ProgramFinancesCard`: Budget ($34.2M), Balance ($5.8M), Weekly Spend ($642K).
   - `ProgramPrestigeCard`: 3D metallic shield badge (#78 RISING) and green progress fill bar.
   - `TeamMoraleCard`: Smiley morale gauge, win streak checklist, Chemistry (82), Leadership (78), Buy-In (85) sliders.
   - `RosterSpotlightCard`: Dual player cards featuring 16-bit pixel-art portraits, OVR badges, stats grid, and last game line.
   - `UpcomingGamesCard`: Schedule preview list (Weeks 9-13) with difficulty stars.
4. **Bottom Controller Status & Audio Footer Bar**:
   - `DesktopStatusFooter.java`: Controller input legend chips `(A) SELECT`, `(B) BACK`, `(Y) HELP` and soundtrack ticker with animated equalizer spectrum bars.

---

## 2. User Review Required

> [!IMPORTANT]
> **Key Architecture & Design Decisions:**
> 1. **Visual System Verification**: All UI components consume tokens from [style_guide.md](style_guide.md) (Midnight Obsidian `#060C14`, Slate Cards `#0D1726`, Neon Emerald `#00E676`, Trophy Gold `#F59E0B`).
> 2. **Console Controller & Power Keyboard Navigation**:
>    - `[Space]` / `[Enter]` -> Trigger `playWeek()` / Advance (Console Action `(A) SELECT`).
>    - `[Escape]` -> Return to Dashboard / Close Dialogs (Console Action `(B) BACK`).
>    - `[F1]` / `[Ctrl+/]` / `[?]` -> Display Keyboard Shortcuts Help (Console Action `(Y) HELP`).
>    - `[1 - 9]` -> Direct jump to sidebar screens 1 to 9.
>    - `[ArrowUp]` / `[ArrowDown]` -> Cycle sidebar menu options.

---

## 3. Open Questions

> [!NOTE]
> 1. **Pixel Art Roster Portraits**: We have generated sprite assets for QB, RB, WR, DL, and Trophy Icons in `preview/sprites/`. Would you like custom procedural pixel art canvas generation for generated recruits in future seasons?

---

## 4. Proposed Multi-Stage Execution Plan

---

### Stage 1: Design Tokens & Base Card Infrastructure
- **[DesktopTheme.java](src/main/java/desktop/DesktopTheme.java)**: Implement color system tokens (`#060C14`, `#0D1726`, `#00E676`, `#F59E0B`, `#881337`, `#1E293B`).
- **[CustomCardPanel.java](src/main/java/desktop/CustomCardPanel.java)**: Rounded dark slate card container base (`#0D1726`, rounded 8px, 1px border `#1E293B`).

---

### Stage 2: Top Broadcast Header & Sidebar Architecture
- **[DesktopHeaderBar.java](src/main/java/desktop/DesktopHeaderBar.java)**: Team logo shield, school title (**PINE VALLEY STATE**), script mascot accent (*Owls*), season/week tracker, and coach badge.
- **[DesktopNavSidebar.java](src/main/java/desktop/DesktopNavSidebar.java)**: 15-item sidebar menu, active green selection indicator (`#00E676`), yellow badge counter (`14`), and school seal logo footer.
- **[LeagueHomeView.java](src/main/java/desktop/LeagueHomeView.java)**: Wire header and sidebar panels.

---

### Stage 3: Modular Java Swing Dashboard Card Suite (11 Cards)
- **[TeamOverallCard.java](src/main/java/desktop/TeamOverallCard.java)**: 64px green rating digit (`82`), B+ grade pill, 4-star gauge, Off/Def/ST ratings, National Rank #24 & Conf Rank #3.
- **[NextGameMatchupCard.java](src/main/java/desktop/NextGameMatchupCard.java)**: Home vs away team banners (Pine Valley State Owls 5-2 vs Redwood University Maroons 4-3), AT badge, kickoff date/time, and stadium details.
- **[RosterSpotlightCard.java](src/main/java/desktop/RosterSpotlightCard.java)**: Dual player cards with pixel art portraits, OVR badges, stats grid, and last game line.
- **[RecruitingPipelineCard.java](src/main/java/desktop/RecruitingPipelineCard.java)**: USA map vector diagram, pin markers, 14 commits count, and action button.
- **[TeamMoraleCard.java](src/main/java/desktop/TeamMoraleCard.java)**: Morale smiley gauge, win streak checklist, and Chemistry/Leadership/Buy-In sliders.
- **[ProgramPrestigeCard.java](src/main/java/desktop/ProgramPrestigeCard.java)**: 3D metallic shield badge (#78 RISING) and green progress fill bar.
- **[ProgramFinancesCard.java](src/main/java/desktop/ProgramFinancesCard.java)**: Budget ($34.2M), Balance ($5.8M), and Weekly Spend ($642K) metrics.
- **[TopNewsCarouselCard.java](src/main/java/desktop/TopNewsCarouselCard.java)**: Hero banner, headline, snippet, and dot pagination.
- **[WeeklyScheduleCard.java](src/main/java/desktop/WeeklyScheduleCard.java)**: Timeline list for Week 8 activities.
- **[UpcomingGamesCard.java](src/main/java/desktop/UpcomingGamesCard.java)**: Schedule list preview for Weeks 9 to 13 with difficulty stars.
- **[DashboardPanel.java](src/main/java/desktop/DashboardPanel.java)**: Modular 4-column card grid layout.

---

### Stage 4: Controller Legend & Status Footer Bar
- **[DesktopStatusFooter.java](src/main/java/desktop/DesktopStatusFooter.java)**: Controller input chips (`(A) SELECT`, `(B) BACK`, `(Y) HELP`) and soundtrack ticker with animated equalizer visualizer.

---

### Stage 5: Web Live Sandbox Sync & Verification
- **[index.html](preview/index.html)**, **[styles.css](preview/styles.css)**, **[app.js](preview/app.js)**: Synchronized layout and interactive controls.
- **Build Checks**: Verified with `./gradlew desktopVerify` (**BUILD SUCCESSFUL**).

---

## 5. Verification Plan

### Automated Build Checks
```powershell
./gradlew desktopVerify
```

### Manual Verification
- **Web Live Sandbox**: Run `python preview/server.py` or `start_preview.bat` and test interactive team switching, ratings sliders, and keyboard shortcuts.
- **Desktop Application**: Run `./gradlew runDesktop` and verify visual design fidelity against the reference image.
