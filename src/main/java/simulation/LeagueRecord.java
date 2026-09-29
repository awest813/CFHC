package simulation;

import java.util.List;

/**
 * A complete, portable snapshot of a League's state.
 * This can be serialized to any format (JSON, XML, Custom)
 * and is entirely platform-agnostic.
 */
public record LeagueRecord(
    String leagueName,
    int year,
    int currentWeek,
    List<ConferenceRecord> conferences,
    List<PlayerRecord> leagueHoF,
    List<DataRecord> leagueRecords,
    String heismanWinnerName,
    String nationalChampName,
    List<GameRecord> scheduledGames,
    long rngSeed,
    Settings settings
) {
    public LeagueRecord {
        if (rngSeed < 0) {
            rngSeed = 0;
        }
    }

    /** Pre-settings call shape: saves and callers without {@link Settings}. */
    public LeagueRecord(String leagueName, int year, int currentWeek, List<ConferenceRecord> conferences,
                        List<PlayerRecord> leagueHoF, List<DataRecord> leagueRecords,
                        String heismanWinnerName, String nationalChampName,
                        List<GameRecord> scheduledGames, long rngSeed) {
        this(leagueName, year, currentWeek, conferences, leagueHoF, leagueRecords, heismanWinnerName,
                nationalChampName, scheduledGames, rngSeed, null);
    }

    /**
     * The league options chosen in New Game / Settings. {@code null} on a
     * {@link LeagueRecord} means the save predates them; {@code League} then falls
     * back to new-league defaults.
     */
    public record Settings(
            boolean careerMode,
            boolean showPotential,
            boolean fullGameLog,
            boolean neverRetire,
            boolean enableTv,
            boolean expandedPlayoffs,
            boolean confRealignment,
            boolean advancedRealignment,
            boolean universalProRel,
            FcsPromotionMode fcsPromotionMode,
            int fcsPromotionCap,
            int fcsPromotionsUsed
    ) {
        public Settings {
            if (fcsPromotionMode == null) {
                fcsPromotionMode = FcsPromotionMode.UNLIMITED;
            }
            if (fcsPromotionCap < 0) {
                fcsPromotionCap = 0;
            }
            if (fcsPromotionsUsed < 0) {
                fcsPromotionsUsed = 0;
            }
        }

        /** What a freshly generated league starts with. */
        public static Settings defaults() {
            return new Settings(true, false, false, false, true, true, true, false, false,
                    FcsPromotionMode.UNLIMITED, League.DEFAULT_FCS_PROMOTION_CAP, 0);
        }

        /** Tab-separated {@code key=value} pairs; readers ignore keys they don't know. */
        public String toSaveLine() {
            return "careerMode=" + flag(careerMode)
                    + "\tshowPotential=" + flag(showPotential)
                    + "\tfullGameLog=" + flag(fullGameLog)
                    + "\tneverRetire=" + flag(neverRetire)
                    + "\tenableTv=" + flag(enableTv)
                    + "\texpandedPlayoffs=" + flag(expandedPlayoffs)
                    + "\tconfRealignment=" + flag(confRealignment)
                    + "\tadvancedRealignment=" + flag(advancedRealignment)
                    + "\tuniversalProRel=" + flag(universalProRel)
                    + "\tfcsPromotion=" + fcsPromotionMode.name()
                    + "\tfcsPromotionCap=" + fcsPromotionCap
                    + "\tfcsPromotionsUsed=" + fcsPromotionsUsed;
        }

        /** Missing or unreadable keys keep their {@link #defaults()} value. */
        public static Settings fromSaveLine(String line) {
            Settings d = defaults();
            java.util.Map<String, String> kv = new java.util.HashMap<>();
            if (line != null) {
                for (String pair : line.split("\t")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) {
                        kv.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                    }
                }
            }
            return new Settings(
                    bool(kv, "careerMode", d.careerMode()),
                    bool(kv, "showPotential", d.showPotential()),
                    bool(kv, "fullGameLog", d.fullGameLog()),
                    bool(kv, "neverRetire", d.neverRetire()),
                    bool(kv, "enableTv", d.enableTv()),
                    bool(kv, "expandedPlayoffs", d.expandedPlayoffs()),
                    bool(kv, "confRealignment", d.confRealignment()),
                    bool(kv, "advancedRealignment", d.advancedRealignment()),
                    bool(kv, "universalProRel", d.universalProRel()),
                    FcsPromotionMode.parse(kv.get("fcsPromotion"), d.fcsPromotionMode()),
                    integer(kv, "fcsPromotionCap", d.fcsPromotionCap()),
                    integer(kv, "fcsPromotionsUsed", d.fcsPromotionsUsed()));
        }

        private static String flag(boolean b) {
            return b ? "1" : "0";
        }

        private static boolean bool(java.util.Map<String, String> kv, String key, boolean fallback) {
            String v = kv.get(key);
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                return true;
            }
            if ("0".equals(v) || "false".equalsIgnoreCase(v)) {
                return false;
            }
            return fallback;
        }

        private static int integer(java.util.Map<String, String> kv, String key, int fallback) {
            try {
                return Integer.parseInt(kv.get(key));
            } catch (RuntimeException e) {
                return fallback;
            }
        }
    }
    // Nested records for structured hierarchy
    public record ConferenceRecord(
        String name,
        List<Integer> oocWeeks,
        List<TeamRecord> teams
    ) {}

    public record TeamRecord(
        String name,
        String abbr,
        int prestige,
        int wins,
        int losses,
        List<Integer> oocWeeks,
        List<String> oocOpponentNames,
        float teamPollScore,
        int rankTeamPollScore,
        StaffRecord headCoach,
        StaffRecord offenseCoach,
        StaffRecord defenseCoach,
        List<PlayerRecord> roster,
        List<TeamHistoryRecord> history,
        List<DataRecord> records,
        String practiceFocus,
        String practicePositionGroup,
        String focusIntensity,
        int nilCollectiveLevel,
        String nickname,
        int prevRankTeamPollScore,
        String rivalName,
        String rivalryTrophyName,
        int rivalryWins,
        boolean holdsRivalryTrophy,
        int teamStadium,
        SeasonBaseline seasonBaseline
    ) {
        /** Pre-baseline call shape: saves and callers without a {@link SeasonBaseline}. */
        public TeamRecord(String name, String abbr, int prestige, int wins, int losses,
                          List<Integer> oocWeeks, List<String> oocOpponentNames,
                          float teamPollScore, int rankTeamPollScore,
                          StaffRecord headCoach, StaffRecord offenseCoach, StaffRecord defenseCoach,
                          List<PlayerRecord> roster, List<TeamHistoryRecord> history, List<DataRecord> records,
                          String practiceFocus, String practicePositionGroup, String focusIntensity,
                          int nilCollectiveLevel, String nickname, int prevRankTeamPollScore,
                          String rivalName, String rivalryTrophyName, int rivalryWins,
                          boolean holdsRivalryTrophy, int teamStadium) {
            this(name, abbr, prestige, wins, losses, oocWeeks, oocOpponentNames, teamPollScore,
                    rankTeamPollScore, headCoach, offenseCoach, defenseCoach, roster, history, records,
                    practiceFocus, practicePositionGroup, focusIntensity, nilCollectiveLevel, nickname,
                    prevRankTeamPollScore, rivalName, rivalryTrophyName, rivalryWins, holdsRivalryTrophy,
                    teamStadium, null);
        }

        public TeamRecord {
            if (practiceFocus == null) {
                practiceFocus = "";
            }
            if (practicePositionGroup == null) {
                practicePositionGroup = "";
            }
            if (focusIntensity == null) {
                focusIntensity = "";
            }
            if (nilCollectiveLevel < 0) {
                nilCollectiveLevel = 0;
            }
            if (nickname == null) {
                nickname = "";
            }
            if (prevRankTeamPollScore < 0) {
                prevRankTeamPollScore = 0;
            }
            if (rivalName == null) {
                rivalName = "";
            }
            if (rivalryTrophyName == null) {
                rivalryTrophyName = "";
            }
            if (rivalryWins < 0) {
                rivalryWins = 0;
            }
            if (teamStadium < 0) {
                teamStadium = 0;
            }
        }
    }

    /**
     * What a team was measured against this season: starting prestige/rank,
     * the preseason projection, and starting talent. The season-end prestige
     * update ({@code Team.calcSeasonPrestige}) and staff evaluation
     * ({@code Team.advanceHC}) grade against these, so they must survive a
     * mid-season save/load. {@code null} on a {@link TeamRecord} means the save
     * predates them; {@code League} recomputes them on load.
     */
    public record SeasonBaseline(
            int prestigeStart,
            int rankPrestigeStart,
            int projectedWins,
            int projectedPollRank,
            float startOffTal,
            float startDefTal
    ) {}

    /**
     * One row per unique {@link Game} (deduped by identity across team schedules).
     * {@link #slot()} is the restore order (append order for {@link Team#addGameToSchedule(Game)}).
     */
    public record GameRecord(
            int slot,
            String homeName,
            String awayName,
            String gameName,
            int week,
            boolean played,
            int homeScore,
            int awayScore,
            boolean seniorDay,
            boolean homecomingGame,
            boolean rivalryGame
    ) {
        private static String tabSafe(String s) {
            return s == null ? "" : s.replace("\t", " ");
        }

        public String toSaveLine() {
            return slot + "\t" + tabSafe(homeName) + "\t" + tabSafe(awayName) + "\t" + tabSafe(gameName) + "\t"
                    + week + "\t" + (played ? 1 : 0) + "\t" + homeScore + "\t" + awayScore
                    + "\t" + (seniorDay ? 1 : 0) + "\t" + (homecomingGame ? 1 : 0) + "\t" + (rivalryGame ? 1 : 0);
        }

        public static GameRecord fromSaveLine(String line) {
            String[] p = line.split("\t", -1);
            if (p.length < 8) {
                throw new IllegalArgumentException("Bad GM line (expected 8 tab fields): " + line);
            }
            // Marquee flags (fields 9-11) are optional; saves from before they
            // were persisted load with all flags false.
            return new GameRecord(
                    Integer.parseInt(p[0]),
                    p[1], p[2], p[3],
                    Integer.parseInt(p[4]),
                    "1".equals(p[5]),
                    Integer.parseInt(p[6]),
                    Integer.parseInt(p[7]),
                    p.length > 8 && "1".equals(p[8]),
                    p.length > 9 && "1".equals(p[9]),
                    p.length > 10 && "1".equals(p[10])
            );
        }

        public static GameRecord fromGame(Game g, int slot) {
            String gn = g.gameName == null ? "" : g.gameName.replace("\t", " ");
            return new GameRecord(
                    slot,
                    Team.normalizeRecordText(g.homeTeam.getName()),
                    Team.normalizeRecordText(g.awayTeam.getName()),
                    gn,
                    g.week,
                    g.hasPlayed,
                    g.homeScore,
                    g.awayScore,
                    g.seniorDay,
                    g.homecomingGame,
                    g.rivalryGame
            );
        }
    }
}
