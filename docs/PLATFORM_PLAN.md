# Platform Plan: macOS/iOS + Desktop & Android Polish

## Current Architecture

```
┌─────────────────────────────────────────────────┐
│  UI Layer (platform-specific)                   │
│  desktop/ (27 files, 440KB) — Swing             │
│  antdroid/ (30 files, 331KB) — Android Views    │
│  ui/ (21 files, 71KB) — Android ArrayAdapters   │
├─────────────────────────────────────────────────┤
│  Platform Bridges (4 interfaces)                │
│  GameUiBridge, GameFlowManager,                 │
│  PlatformResourceProvider, AudioManager         │
├─────────────────────────────────────────────────┤
│  Engine (pure Java, zero platform imports)      │
│  simulation/ (41 files), positions/ (15 files), │
│  staff/ (4 files), comparator/ (77 files),      │
│  recruiting/ (7 files)                          │
└─────────────────────────────────────────────────┘
```

The engine is **already portable** — no Android, Swing, or platform imports.
Platform integration uses only 4 small interfaces. The desktop already proves
non-Android implementations of all 4 work.

---

## Phase 1: Desktop Polish (2-3 weeks)

Goal: Production-quality desktop app ready for macOS distribution.

### 1.1 Split God Object — LeagueHomeView (155KB)
**Priority: Critical | Effort: 5-7 days**

Currently LeagueHomeView is a monolithic JFrame that builds all 15+ screens.
Decompose into:

| New Class | Screens | ~Size |
|-----------|---------|-------|
| `DashboardPanel` | Home dashboard | 8KB |
| `StandingsPanel` | Conference standings | 6KB |
| `ScoreboardPanel` | Weekly scores + box scores | 12KB |
| `CoachProfilePanel` | My Coach view | 10KB |
| `PollRankingsPanel` | Top 25 polls | 6KB |
| `TeamRankingsPanel` | Team stat rankings | 8KB |
| `PlayerStatsPanel` | Player stat rankings | 8KB |
| `PlayerSearchPanel` | Player search + detail | 10KB |
| `NewsPanel` | News stories | 6KB |
| `CoachesPanel` | Coach database | 8KB |
| `HallOfFamePanel` | Hall of Fame | 6KB |
| `LeagueRecordsPanel` | League history/records | 8KB |
| `SettingsPanel` | Game settings | 10KB |
| `RecruitingPanel` | Recruiting board (already separate) | 18KB |
| `LeagueHomeView` | Shell: nav, menu, frame, wiring | ~30KB |

Each panel implements `LeagueScreen` interface:

```java
interface LeagueScreen {
    String title();
    JComponent build(League league, LeagueRecord record);
    void refresh(LeagueRecord record);
    JComponent searchTarget();
}
```

### 1.2 macOS App Bundle
**Priority: High | Effort: 1-2 days**

- Create `.app` bundle with `Info.plist` (CFBundleIdentifier, LSUIElement, etc.)
- Set macOS `UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())` (Aqua)
- Add macOS native menu bar integration (`Desktop.getDesktop().setAboutHandler()`, `setPreferencesHandler()`, `setQuitHandler()`)
- Add file association for `.cfb` files
- Create `.dmg` packaging via Gradle task
- Sign and notarize (needs Apple Developer account — optional for distribution)

### 1.3 Desktop Audio Fix
**Priority: Medium | Effort: 1 day**

- Current: `DesktopAudioManager` uses `javax.sound.sampled` + `vorbisspi` for OGG
- Fix: Ensure audio works on macOS (Clip doesn't always work well on macOS)
- Alternative: Bundle a small native OGG decoder or pre-convert to WAV

### 1.4 Platform Persistence
**Priority: Medium | Effort: 1 day**

- Save files go to user home (`~/.cfhc/saves/`) on all platforms
- Settings stored in `~/.cfhc/prefs.properties`
- Auto-detect OS and use appropriate paths
- Handle case-sensitive filesystems (macOS/Linux)

### 1.5 Desktop Testing Gap
**Priority: Medium | Effort: 2-3 days**

Currently only 2 desktop tests. Add:
- `DesktopThemeTest` — verify all color constants are non-null, contrast ratios
- `DesktopResourceProviderTest` — verify all required resources load
- `DesktopUiBridgeTest` — verify all bridge methods work (can use headless mode `-Djava.awt.headless=true`)

---

## Phase 2: Android Polish (2-3 weeks)

Goal: Production-quality Android app.

### 2.1 Split God Object — MainActivity (159KB)
**Priority: Critical | Effort: 5-7 days**  
*(ROADMAP.md item #10 — already marked in-progress)*

MainActivity implements every screen inline. It's 3,600+ lines. Decompose:

| New Fragment/Controller | Screens | ~Size |
|-------------------------|---------|-------|
| `TeamHomeFragment` | Team home dashboard | 12KB |
| `TeamRosterFragment` | Roster view with depth chart | 10KB |
| `TeamScheduleFragment` | Schedule + results | 8KB |
| `StandingsFragment` | Conference standings | 6KB |
| `ScoreboardFragment` | Weekly scores | 8KB |
| `PlayerStatsFragment` | Player stat rankings | 8KB |
| `TeamStatsFragment` | Team stat rankings | 8KB |
| `PollRankingsFragment` | Poll rankings | 6KB |
| `NewsFragment` | News feed | 6KB |
| `AwardsFragment` | Awards/Bowls/CCG | 8KB |
| `HallOfFameFragment` | Hall of Fame | 6KB |
| `LeagueHistoryFragment` | League records | 8KB |
| `CoachDatabaseFragment` | Coach database | 8KB |
| `SettingsFragment` | Settings | 10KB |
| `MainActivity` | Navigation drawer shell, wiring | ~20KB |

Use Android Architecture Components: `ViewModel` + `LiveData` for each fragment.
Each fragment takes `League` reference and a view model.

### 2.2 Split God Object — Home (15KB → fragments)
**Priority: Medium | Effort: 2 days**

`Home.java` handles launcher, save manager, import, settings, tutorial, about.
Split into fragments or separate activities.

### 2.3 Remove Deprecated APIs
**Priority: Low | Effort: 1 day**

Build output shows deprecation warnings. Audit and replace:
- `ListView` → `RecyclerView` (has better performance, already in most screens)
- Any raw `AsyncTask` → `ExecutorService` or coroutines
- Deprecated `ActivityResultContracts` → current versions

### 2.4 Android CI Setup
**Priority: High | Effort: 1 day**

- GitHub Actions workflow for Android builds
- Run `./gradlew :app:assembleDebug :app:lintDebug :app:test`
- Upload APK artifact
- Run on PR and push to main

### 2.5 Android Versioning Cleanup
**Priority: Low | Effort: 0.5 days**

- `versionName 'v1.4.5'`, `versionCode 320` — bump on each release
- Add automated version bump script or comment with versioning policy

---

## Phase 3: macOS Native App (2-4 weeks)

Goal: Native-feeling macOS app using the existing Java engine + Swift UI.

### 3.1 Engine Packaging
**Effort: 2 days**

Package engine as a standalone JAR with a thin native API:

```
engine-macos.jar
├── com/cfhc/engine/
│   ├── CfhcEngine.java          // Public API
│   ├── EngineBridge.java         // JNI or process bridge
│   └── ... (existing packages)
```

`CfhcEngine` public API:

```java
public class CfhcEngine {
    public static CfhcEngine create(Path dataDir);
    public LeagueRecord newGame(PrestigeMode mode, String teamName, CoachConfig config);
    public LeagueRecord loadGame(Path saveFile);
    public void saveGame(Path saveFile);
    public GameResult advanceWeek();
    public RecruitingSession getRecruiting();
    public LeagueRecord getCurrentState();
    public List<PlayerRecord> searchPlayers(String query);
    // ... all operations the UI needs
}
```

### 3.2 Option A: Java Swing + macOS Polish (Recommended First Step)
**Effort: 3-5 days** — build on existing desktop code

- Apply Phase 1 desktop polish (already covering macOS)
- Set Aqua Look & Feel
- Create `.app` bundle and `.dmg`
- Add macOS native menus (About, Preferences, Quit)
- Enable full-screen mode
- Add Touch Bar support (optional)
- **Result**: Working macOS app sharing 100% code with desktop

### 3.3 Option B: SwiftUI Frontend (Better UX, More Work)
**Effort: 3-4 weeks** — for a truly native macOS and iOS app

SwiftUI app structure:

```
CFHC/
├── Shared/               # Swift packages
│   ├── Engine/            # JNI bridge to Java engine
│   ├── Models/            # Swift model structs
│   └── Resources/         # Strings, names, teams XML
├── macOS/
│   ├── CFHCApp.swift
│   ├── Views/
│   │   ├── LauncherView.swift
│   │   ├── HomeView.swift        // Dashboard
│   │   ├── StandingsView.swift
│   │   ├── ScoreboardView.swift
│   │   ├── RosterView.swift
│   │   ├── RecruitingView.swift
│   │   ├── SettingsView.swift
│   │   └── ... (12-15 views)
│   └── CFHC.entitlements
├── iOS/
│   ├── CFHCApp.swift
│   ├── Views/
│   │   └── ... (same views, iOS-adapted)
│   └── Info.plist
└── Package.swift
```

Bridge approach: Use Java Native Interface (JNI) on macOS.
On iOS: J2ObjC to translate engine Java → Objective-C → Swift.

Engine adapter layer (in Java):

```java
// Runs as an embedded library loaded via JNI
public class CfhcEngineNative {
    static { System.loadLibrary("cfhc_engine"); }
    
    // Returns JSON strings to the Swift layer
    public static native String createGame(String configJson);
    public static native String loadGame(String path);
    public static native String advanceWeek();
    public static native String getState();
    // ... 
}
```

### 3.4 Comparison: Option A vs B

| Factor | Option A (Swing) | Option B (SwiftUI) |
|--------|-----------------|-------------------|
| Time to macOS app | 3-5 days | 3-4 weeks |
| Native look & feel | Good (Aqua L&F) | Excellent |
| Code sharing with iOS | None | UI reuse via SwiftUI |
| Maintenance | 1 codebase (Java) | 2 codebases (Java engine + Swift UI) |
| Risk | Very low | Medium (JNI complexity) |
| Distributable via App Store | No (Java) | Yes (native) |
| Needed for iOS path | No | Yes |

**Recommendation**: Do Option A first (quick win), then Option B later.

---

## Phase 4: iOS App (6-10 weeks)

Goal: Native iOS app.

### 4.1 Engine Translation
**Effort: 2-3 weeks**

The engine is ~150 Java files. Options for running on iOS:

**Option 1: J2ObjC** (Google's Java → Objective-C translator)
- Translates Java source to Objective-C (not Swift, but ObjC is callable from Swift)
- Supported by Google (used in Google apps for iOS)
- Limitations: No reflection, no dynamic class loading, limited Java 8+ features
- Our engine uses: File I/O, ArrayList, HashMap, String, Math — all supported
- Need to adapt: Random (can use platform), InputStream (platform-specific)
- Result: ~150 .m/.h files, compiled as a static library

**Option 2: GraalVM Native Image**
- Compiles Java to native ARM64 binary
- More limited on iOS (experimental support as of 2024-2025)
- Result: One .a static library with exported C functions

**Recommendation**: J2ObjC is the most proven path for Java-on-iOS.

### 4.2 SwiftUI UI Layer
**Effort: 4-6 weeks**

Same SwiftUI views as macOS Option B (see section 3.3), adapted for iOS:
- Navigation uses `TabView` or `NavigationStack` instead of sidebar
- Touch-optimized layouts (larger tap targets, swipe gestures)
- iOS-specific: Haptic feedback, Dynamic Type, Dark Mode
- Share ~80% of SwiftUI code between macOS and iOS via a shared Swift package

### 4.3 Platform-Specific Implementations

| Interface | macOS | iOS |
|-----------|-------|-----|
| PlatformResourceProvider | Bundle resources (.strings, .plist) | Same, in asset catalog |
| AudioManager | AVAudioPlayer | AVAudioPlayer |
| GameUiBridge | Callback to SwiftUI | Callback to SwiftUI |
| GameFlowManager | SwiftUI navigation | SwiftUI navigation |
| File I/O | FileManager.default | FileManager.default |
| Settings | UserDefaults / NSUbiquitousKeyValueStore | Same |

### 4.4 App Store Readiness
**Effort: 1 week**

- App Store privacy labels
- Screenshots at required sizes
- App description, keywords
- Age rating questionnaire
- TestFlight setup
- In-app purchase (free app, no IAP needed)
- No network = no privacy concerns

---

## Phase 5: Cross-Cutting Improvements (Ongoing)

### 5.1 Engine Decomposition (ROADMAP #3)
**Effort: Ongoing** — `League.java` (120KB) and `Team.java` (80KB) are god objects.

- Split `League` into: `LeagueCore`, `SeasonManager`, `BowlManager`, `RankingEngine`, `NewsGenerator`
- Split `Team` into: `RosterManager`, `DepthChartManager`, `StatsTracker`, `TeamFinance`

### 5.2 Collection Encapsulation (ROADMAP #4)
**Effort: 2-3 days** — Currently many public `ArrayList` fields are directly mutated.
- Make collections private, expose via unmodifiable views or accessor methods
- Prevents accidental mutation from UI code

### 5.3 Engine Test Coverage
**Effort: Ongoing** — Current: 47 test files, ~272+ test cases.

Target coverage:
- `simulation/`: 85% line coverage (prioritize `League`, `Game`, `SeasonController`)
- `positions/`: 90% (most dense logic)
- `recruiting/`: 80%
- `comparator/`: 95% (simple classes)

### 5.4 CI Pipeline
**Effort: 2-3 days**

```
GitHub Actions workflows:
├── ci.yml
│   ├── Desktop: compileDesktopJava + tests (runs on ubuntu/macos)
│   ├── Android: assembleDebug + lintDebug + test (runs on ubuntu)
│   └── Check: engine imports check
├── release.yml
│   ├── Build desktop .jar + .dmg
│   ├── Build Android .apk + .aab
│   └── Tag and create GitHub Release
└── ios.yml (future)
    └── Build iOS via J2ObjC + Xcode
```

---

## Summary: Effort & Timeline

| Phase | What | Effort | Prerequisite |
|-------|------|--------|-------------|
| **1** | Desktop polish | 2-3 weeks | None |
| **2** | Android polish | 2-3 weeks | None |
| **3** | macOS (Option A, Swing) | 3-5 days | Phase 1 |
| **3** | macOS (Option B, SwiftUI) | 3-4 weeks | Phase 1 |
| **4** | iOS | 6-10 weeks | Phase 3 Option B (shared SwiftUI) |
| **5** | Cross-cutting | Ongoing | None |

### Fastest path to all platforms:

```
Phase 1 (Desktop) ──► Phase 3A (macOS Swing) ──► macOS app ready
    │
Phase 2 (Android)  ──► Android app polished
    │
Phase 5 (Engine)   ──► Engine clean + tested
    │
Phase 3B/4 (SwiftUI + J2ObjC) ──► iOS app + native macOS app
```

**Total for all platforms**: ~12-16 weeks of work (excluding ongoing engine improvements).

### Critical dependencies:

1. **J2ObjC evaluation** — Before starting iOS, validate J2ObjC can translate the engine.
   Test with: `recruiting/`, `staff/`, `comparator/` (simplest packages first).
   Key risk areas: `java.util.Random` (need seedable replacement), `java.io.File` (need NSFileManager adapter).

2. **Audio format** — OGG Vorbis files in `assets/sounds/` and `res/raw/`.
   macOS: AVAudioPlayer supports OGG via Core Audio.
   iOS: AVAudioPlayer supports OGG.
   Desktop: Currently needs vorbisspi JAR — works on all OS.
   Alternatively: convert to `.wav` or `.mp3` for wider compatibility.

3. **Resource format** — Engine reads XML strings/names/teams via `PlatformResourceProvider`.
   Desktop parses XML with regex (fragile).
   Better: Convert to JSON or use a proper XML parser (not Android-dependent).
   Mac/iOS: Can use `XMLParser` / `PropertyListDecoder`.
