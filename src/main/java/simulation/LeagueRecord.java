package simulation;

import java.util.List;

/**
 * A complete, portable snapshot of a League's state.
 * This can be serialized to any format (JSON, XML, Custom)
 * and is entirely platform-agnostic.
 *
 * <p>{@code recruitingStarted}: whether this offseason's recruiting gate already
 * ran (CPU teams signed their classes). Null when the save predates it; League
 * then infers it from the CPU rosters.
 *
 * <p>{@code coachFreeAgents}: coaches without a job, unemployed or retired (the
 * hiring pool and the coach database's former coaches). Empty for saves that
 * predate it.
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
    Settings settings,
    Boolean recruitingStarted,
    List<StaffRecord> coachFreeAgents,
    /** Weeks before the postseason; 0 = not stored (pre-fix save, treated as legacy 13). */
    int regSeasonWeeks,
    /** History, bowl names and postseason bookkeeping; null for saves that predate it. */
    LeagueState state
) {
    public LeagueRecord {
        if (rngSeed < 0) {
            rngSeed = 0;
        }
        if (regSeasonWeeks < 0) {
            regSeasonWeeks = 0;
        }
        coachFreeAgents = coachFreeAgents == null ? List.of() : List.copyOf(coachFreeAgents);
    }

    /** Pre-{@link LeagueState} call shape. */
    public LeagueRecord(String leagueName, int year, int currentWeek, List<ConferenceRecord> conferences,
                        List<PlayerRecord> leagueHoF, List<DataRecord> leagueRecords,
                        String heismanWinnerName, String nationalChampName,
                        List<GameRecord> scheduledGames, long rngSeed, Settings settings,
                        Boolean recruitingStarted, List<StaffRecord> coachFreeAgents, int regSeasonWeeks) {
        this(leagueName, year, currentWeek, conferences, leagueHoF, leagueRecords, heismanWinnerName,
                nationalChampName, scheduledGames, rngSeed, settings, recruitingStarted, coachFreeAgents,
                regSeasonWeeks, null);
    }

    /** Pre-settings call shape: saves and callers without {@link Settings}. */
    public LeagueRecord(String leagueName, int year, int currentWeek, List<ConferenceRecord> conferences,
                        List<PlayerRecord> leagueHoF, List<DataRecord> leagueRecords,
                        String heismanWinnerName, String nationalChampName,
                        List<GameRecord> scheduledGames, long rngSeed) {
        this(leagueName, year, currentWeek, conferences, leagueHoF, leagueRecords, heismanWinnerName,
                nationalChampName, scheduledGames, rngSeed, null, null, null, 0, null);
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
    /**
     * {@code state}: TV deal and division names ({@link StateBag} keys {@code tv},
     * {@code tvYears}, {@code tvBonus}, {@code tvName}, {@code divA}, {@code divB});
     * empty for saves that predate it.
     */
    public record ConferenceRecord(
        String name,
        List<Integer> oocWeeks,
        List<TeamRecord> teams,
        StateBag state
    ) {
        public ConferenceRecord {
            if (state == null) {
                state = StateBag.EMPTY;
            }
        }

        /** Pre-state call shape. */
        public ConferenceRecord(String name, List<Integer> oocWeeks, List<TeamRecord> teams) {
            this(name, oocWeeks, teams, StateBag.EMPTY);
        }
    }

    /**
     * Ordered {@code key=value} pairs written as one tab-separated save line. Readers
     * ignore keys they do not know and fall back to a default for missing or
     * unreadable ones, so fields can be added without breaking older saves.
     */
    public record StateBag(java.util.Map<String, String> values) {
        public static final StateBag EMPTY = new StateBag(java.util.Map.of());

        public StateBag {
            values = values == null ? java.util.Map.of()
                    : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(values));
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        public boolean has(String key) {
            return values.containsKey(key);
        }

        public String str(String key, String fallback) {
            String v = values.get(key);
            return v != null ? v : fallback;
        }

        public int integer(String key, int fallback) {
            try {
                return Integer.parseInt(values.get(key).trim());
            } catch (RuntimeException e) {
                return fallback;
            }
        }

        public double dbl(String key, double fallback) {
            try {
                return Double.parseDouble(values.get(key).trim());
            } catch (RuntimeException e) {
                return fallback;
            }
        }

        public boolean bool(String key, boolean fallback) {
            String v = values.get(key);
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                return true;
            }
            if ("0".equals(v) || "false".equalsIgnoreCase(v)) {
                return false;
            }
            return fallback;
        }

        /** Tab-separated {@code key=value} pairs; tabs and line breaks in values become spaces. */
        public String toSaveLine() {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : values.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('\t');
                }
                String v = e.getValue() == null ? "" : e.getValue();
                sb.append(e.getKey()).append('=')
                        .append(v.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
            }
            return sb.toString();
        }

        public static StateBag fromSaveLine(String line) {
            java.util.LinkedHashMap<String, String> kv = new java.util.LinkedHashMap<>();
            if (line != null) {
                for (String pair : line.split("\t")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) {
                        kv.put(pair.substring(0, eq).trim(), pair.substring(eq + 1));
                    }
                }
            }
            return new StateBag(kv);
        }

        /** Insertion-ordered builder; values are stringified. */
        public static final class Builder {
            private final java.util.LinkedHashMap<String, String> kv = new java.util.LinkedHashMap<>();

            public Builder put(String key, Object value) {
                kv.put(key, value == null ? "" : String.valueOf(value));
                return this;
            }

            public Builder flag(String key, boolean value) {
                kv.put(key, value ? "1" : "0");
                return this;
            }

            public StateBag build() {
                return new StateBag(kv);
            }
        }
    }

    /**
     * League-wide state beyond the header: the season-by-season history (it sets the
     * season year and realignment eligibility), the bowl names, and the postseason
     * bookkeeping a save made during the postseason needs to resume.
     *
     * <p>{@code leagueHistory} rows are a season's entries joined with {@code %};
     * {@code playoffTeams} is the playoff field in seed order. {@code scalars} holds
     * {@code bowlsScheduled}, {@code playoffWeek}, {@code userTeam},
     * {@code longestStreak}, {@code yearStartStreak} and {@code postseason}.
     */
    public record LeagueState(
            List<String> leagueHistory,
            List<String> heismanHistory,
            List<String> bowlNames,
            List<String> playoffTeams,
            StateBag scalars,
            /**
             * Since v1.5b: named lists ({@code LL:} lines) of news, awards, offseason
             * pools and other league collections (see {@link SaveStateCodec}). Empty
             * for older saves.
             */
            java.util.Map<String, List<String>> lists,
            /** Since v1.5b: players referenced by the lists but on no saved roster. */
            List<DetachedPlayer> detachedPlayers,
            /** Since v1.5b: coaches referenced by the lists but on no staff or pool. */
            List<DetachedStaff> detachedStaff,
            /** Since v1.5b: this season's FCS opponents, in first-scheduled order. */
            List<TeamRecord> fcsTeams
    ) {
        public LeagueState {
            leagueHistory = leagueHistory == null ? List.of() : List.copyOf(leagueHistory);
            heismanHistory = heismanHistory == null ? List.of() : List.copyOf(heismanHistory);
            bowlNames = bowlNames == null ? List.of() : List.copyOf(bowlNames);
            playoffTeams = playoffTeams == null ? List.of() : List.copyOf(playoffTeams);
            if (scalars == null) {
                scalars = StateBag.EMPTY;
            }
            lists = lists == null ? java.util.Map.of()
                    : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(lists));
            detachedPlayers = detachedPlayers == null ? List.of() : List.copyOf(detachedPlayers);
            detachedStaff = detachedStaff == null ? List.of() : List.copyOf(detachedStaff);
            fcsTeams = fcsTeams == null ? List.of() : List.copyOf(fcsTeams);
        }

        /** Pre-v1.5b call shape. */
        public LeagueState(List<String> leagueHistory, List<String> heismanHistory, List<String> bowlNames,
                           List<String> playoffTeams, StateBag scalars) {
            this(leagueHistory, heismanHistory, bowlNames, playoffTeams, scalars, null, null, null, null);
        }

        /** True when the save carries the full v1.5b state (lists, ids, FCS opponents). */
        public boolean hasFullState() {
            return lists.containsKey(SaveStateCodec.MARKER);
        }
    }

    /** A player on no saved roster (a departed or pooled player), for the v1.5b id table. */
    public record DetachedPlayer(String teamName, PlayerRecord player, StateBag state) {
        public DetachedPlayer {
            if (state == null) {
                state = StateBag.EMPTY;
            }
        }
    }

    /** A coach outside every staff and the free-agent pool, for the v1.5b id table. */
    public record DetachedStaff(String teamName, StaffRecord staff) {}

    /**
     * {@code playbookOffense} / {@code playbookDefense}: the team's scheme numbers
     * (0-based), or -1 when the save predates them; League then gives the team the
     * books its coordinators run.
     */
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
        SeasonBaseline seasonBaseline,
        int playbookOffense,
        int playbookDefense,
        /**
         * Program and season state not covered above (conference, division, location,
         * budget, discipline, facilities, sanctions, career totals, win streak,
         * postseason flags, season stat totals, contract status). Empty for saves
         * that predate it; {@code Team#toRecord} lists the keys.
         */
        StateBag state,
        /** Since v1.5b: named team lists ({@code TL:} lines); empty for older saves. */
        java.util.Map<String, List<String>> lists,
        /** Since v1.5b: per-player extra state ({@code PX:} lines), aligned with {@code roster}. */
        List<StateBag> rosterState
    ) {
        /** Pre-v1.5b call shape. */
        public TeamRecord(String name, String abbr, int prestige, int wins, int losses,
                          List<Integer> oocWeeks, List<String> oocOpponentNames,
                          float teamPollScore, int rankTeamPollScore,
                          StaffRecord headCoach, StaffRecord offenseCoach, StaffRecord defenseCoach,
                          List<PlayerRecord> roster, List<TeamHistoryRecord> history, List<DataRecord> records,
                          String practiceFocus, String practicePositionGroup, String focusIntensity,
                          int nilCollectiveLevel, String nickname, int prevRankTeamPollScore,
                          String rivalName, String rivalryTrophyName, int rivalryWins,
                          boolean holdsRivalryTrophy, int teamStadium, SeasonBaseline seasonBaseline,
                          int playbookOffense, int playbookDefense, StateBag state) {
            this(name, abbr, prestige, wins, losses, oocWeeks, oocOpponentNames, teamPollScore,
                    rankTeamPollScore, headCoach, offenseCoach, defenseCoach, roster, history, records,
                    practiceFocus, practicePositionGroup, focusIntensity, nilCollectiveLevel, nickname,
                    prevRankTeamPollScore, rivalName, rivalryTrophyName, rivalryWins, holdsRivalryTrophy,
                    teamStadium, seasonBaseline, playbookOffense, playbookDefense, state, null, null);
        }

        /** The same record with v1.5b team lists and per-player state attached. */
        public TeamRecord withExtras(java.util.Map<String, List<String>> extraLists, List<StateBag> extraRosterState) {
            return new TeamRecord(name, abbr, prestige, wins, losses, oocWeeks, oocOpponentNames, teamPollScore,
                    rankTeamPollScore, headCoach, offenseCoach, defenseCoach, roster, history, records,
                    practiceFocus, practicePositionGroup, focusIntensity, nilCollectiveLevel, nickname,
                    prevRankTeamPollScore, rivalName, rivalryTrophyName, rivalryWins, holdsRivalryTrophy,
                    teamStadium, seasonBaseline, playbookOffense, playbookDefense, state, extraLists,
                    extraRosterState);
        }

        /** Pre-state call shape. */
        public TeamRecord(String name, String abbr, int prestige, int wins, int losses,
                          List<Integer> oocWeeks, List<String> oocOpponentNames,
                          float teamPollScore, int rankTeamPollScore,
                          StaffRecord headCoach, StaffRecord offenseCoach, StaffRecord defenseCoach,
                          List<PlayerRecord> roster, List<TeamHistoryRecord> history, List<DataRecord> records,
                          String practiceFocus, String practicePositionGroup, String focusIntensity,
                          int nilCollectiveLevel, String nickname, int prevRankTeamPollScore,
                          String rivalName, String rivalryTrophyName, int rivalryWins,
                          boolean holdsRivalryTrophy, int teamStadium, SeasonBaseline seasonBaseline,
                          int playbookOffense, int playbookDefense) {
            this(name, abbr, prestige, wins, losses, oocWeeks, oocOpponentNames, teamPollScore,
                    rankTeamPollScore, headCoach, offenseCoach, defenseCoach, roster, history, records,
                    practiceFocus, practicePositionGroup, focusIntensity, nilCollectiveLevel, nickname,
                    prevRankTeamPollScore, rivalName, rivalryTrophyName, rivalryWins, holdsRivalryTrophy,
                    teamStadium, seasonBaseline, playbookOffense, playbookDefense, StateBag.EMPTY);
        }

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
                    teamStadium, null, -1, -1, StateBag.EMPTY);
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
            if (playbookOffense < 0 || playbookOffense >= Team.OFFENSE_PLAYBOOKS) {
                playbookOffense = -1;
            }
            if (playbookDefense < 0 || playbookDefense >= Team.DEFENSE_PLAYBOOKS) {
                playbookDefense = -1;
            }
            if (state == null) {
                state = StateBag.EMPTY;
            }
            lists = lists == null ? java.util.Map.of()
                    : java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(lists));
            rosterState = rosterState == null ? List.of() : java.util.Collections.unmodifiableList(
                    new java.util.ArrayList<>(rosterState));
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

        /**
         * The same record with {@code week} carrying both teams' schedule slots:
         * {@code homeSlot + 1000 * awaySlot}, each 1-based (0 = unknown). The two
         * teams can hold a game at different indexes, so a load needs both to rebuild
         * every schedule exactly. Game.week itself is never assigned in play, and
         * older saves wrote 0 here.
         */
        public GameRecord withScheduleSlots(int homeSlot1Based, int awaySlot1Based) {
            return new GameRecord(slot, homeName, awayName, gameName, homeSlot1Based + 1000 * awaySlot1Based,
                    played, homeScore, awayScore, seniorDay, homecomingGame, rivalryGame);
        }

        /** 0-based home schedule index, or -1 when the save did not record it. */
        public int homeScheduleIndex() {
            return (week % 1000) - 1;
        }

        /** 0-based away schedule index, or -1 when the save did not record it. */
        public int awayScheduleIndex() {
            return (week / 1000) - 1;
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
