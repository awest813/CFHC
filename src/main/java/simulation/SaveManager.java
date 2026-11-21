package simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages saving and loading of the LeagueRecord hierarchy.
 * This replaces the legacy String-based save/load logic with a structured,
 * scalable format.
 *
 * <p>Files begin with a {@code V:<schema>} line (see {@link SaveSchema}), then the
 * {@code L:} league header. Pre-version {@code L:}-only files still load as
 * {@link League#CURRENT_SAVE_VERSION}.
 */
public class SaveManager {

    private static final String LEAGUE_PREFIX = "L:";
    private static final String CONF_PREFIX = "C:";
    private static final String TEAM_PREFIX = "T:";
    private static final String COACH_PREFIX = "H:";
    private static final String PLAYER_PREFIX = "P:";
    private static final String HISTORY_PREFIX = "S:";
    private static final String RECORD_PREFIX = "R:";
    private static final String GAME_PREFIX = "GM:";
    private static final String TEAM_OOC_PREFIX = "TO:";
    /** League options ({@link LeagueRecord.Settings}); optional, older readers skip it. */
    static final String SETTINGS_PREFIX = "OPT:";
    /** A coach without a job (free agent or retired); older builds skip the line. */
    static final String FREE_AGENT_COACH_PREFIX = "CFA:";
    /** Team state ({@link LeagueRecord.TeamRecord#state()}), after T:; key=value pairs. */
    static final String TEAM_STATE_PREFIX = "TX:";
    /** Conference state ({@link LeagueRecord.ConferenceRecord#state()}), after C:. */
    static final String CONF_STATE_PREFIX = "CX:";
    /** League scalars ({@link LeagueRecord.LeagueState#scalars()}). */
    static final String LEAGUE_STATE_PREFIX = "LS:";
    /** One league-history season row (entries joined with %). */
    static final String LEAGUE_HISTORY_PREFIX = "LH:";
    /** One season's player-of-the-year history line. */
    static final String HEISMAN_HISTORY_PREFIX = "HH:";
    /** Bowl names, tab-separated, in bowl-slot order. */
    static final String BOWL_NAMES_PREFIX = "BN:";
    /** The playoff field in seed order, tab-separated team names. */
    static final String PLAYOFF_TEAMS_PREFIX = "PO:";
    /** v1.5b: a named league list, {@code LL:key<TAB>item<TAB>item...}; items escaped. */
    static final String LEAGUE_LIST_PREFIX = "LL:";
    /** v1.5b: a named team list inside a team block; same layout as {@code LL:}. */
    static final String TEAM_LIST_PREFIX = "TL:";
    /** v1.5b: extra state of the player on the preceding {@code P:} or {@code XP:} line. */
    static final String PLAYER_STATE_PREFIX = "PX:";
    /** v1.5b: a player on no saved roster: {@code XP:team<TAB>player csv}. */
    static final String DETACHED_PLAYER_PREFIX = "XP:";
    /** v1.5b: a coach on no staff or pool: {@code XS:team<TAB>staff csv}. */
    static final String DETACHED_STAFF_PREFIX = "XS:";
    /** v1.5b: starts the block of this season's FCS opponents (team blocks up to END_CONF). */
    static final String FCS_BLOCK_PREFIX = "XC:";
    private static final String END_TOKEN = "END";
    /**
     * v1.5d: the last line, {@code END_SAVE<TAB><lines before it>}. A save of that
     * schema without it (a crash or full disk mid-write, a truncated copy) is
     * refused instead of loading as a smaller league. Older readers skip it.
     */
    static final String END_SAVE_PREFIX = "END_SAVE\t";

    /**
     * Checks a save's {@code END_SAVE} trailer while its lines are read: feed every
     * line to {@link #line}, then {@link #complete()} says whether the file ended
     * with a trailer counting exactly the lines before it.
     */
    static final class TrailerCheck {
        private long lines;
        private long trailerAt = -1;
        private boolean afterTrailer;

        void line(String line) {
            if (trailerAt >= 0) {
                if (!line.isEmpty()) {
                    afterTrailer = true;
                }
                return;
            }
            if (line.startsWith(END_SAVE_PREFIX)) {
                try {
                    long n = Long.parseLong(line.substring(END_SAVE_PREFIX.length()).trim());
                    if (n == lines) {
                        trailerAt = lines;
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // not a valid trailer: the file stays incomplete
                }
            }
            lines++;
        }

        boolean complete() {
            return trailerAt >= 0 && !afterTrailer;
        }
    }

    /** Counts the lines written, for the {@code END_SAVE} trailer. */
    private static final class LineCountingWriter extends java.io.FilterWriter {
        long lines;

        LineCountingWriter(java.io.Writer out) {
            super(out);
        }

        @Override
        public void write(int c) throws IOException {
            if (c == '\n') lines++;
            out.write(c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                if (cbuf[i] == '\n') lines++;
            }
            out.write(cbuf, off, len);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                if (str.charAt(i) == '\n') lines++;
            }
            out.write(str, off, len);
        }
    }

    /**
     * Records written with enough digits that a float reads back identically
     * (the old two-decimal format made a reload compare new marks against a
     * rounded record). US symbols: a device locale with a decimal comma broke the CSV.
     */
    private static java.text.DecimalFormat recordFormat() {
        return new java.text.DecimalFormat("#.#########",
                java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
    }

    /** Escapes a list item so it fits on one tab-separated line ({@link #unescapeItem}). */
    static String escapeItem(String v) {
        if (v == null) {
            return "";
        }
        // Returns v itself when nothing needs escaping (the usual case);
        // otherwise fills a char array once (every escape is one extra char).
        int n = v.length();
        int extra = 0;
        for (int i = 0; i < n; i++) {
            char c = v.charAt(i);
            if (c == '\\' || c == '\t' || c == '\n' || c == '\r') {
                extra++;
            }
        }
        if (extra == 0) {
            return v;
        }
        char[] in = v.toCharArray();
        char[] out = new char[n + extra];
        int o = 0;
        for (int i = 0; i < n; i++) {
            char c = in[i];
            switch (c) {
                case '\\': out[o++] = '\\'; out[o++] = '\\'; break;
                case '\t': out[o++] = '\\'; out[o++] = 't'; break;
                case '\n': out[o++] = '\\'; out[o++] = 'n'; break;
                case '\r': out[o++] = '\\'; out[o++] = 'r'; break;
                default: out[o++] = c;
            }
        }
        return new String(out);
    }

    static String unescapeItem(String v) {
        if (v.indexOf('\\') < 0) {
            return v;
        }
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '\\' && i + 1 < v.length()) {
                char n = v.charAt(++i);
                switch (n) {
                    case 't': sb.append('\t'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Writes {@code prefix key<TAB>items...} as one line, straight to the writer (items can be megabytes). */
    private static void writeListLine(java.io.Writer writer, String prefix, String key, List<String> items)
            throws IOException {
        writer.write(prefix);
        writer.write(escapeItem(key));
        for (String item : items) {
            writer.write('\t');
            writer.write(escapeItem(item));
        }
        writer.write('\n');
    }

    /** {@code key<TAB>items...} body of an {@code LL:}/{@code TL:} line, unescaped. */
    private static java.util.Map.Entry<String, List<String>> parseListLine(String body) {
        String[] p = body.split("\t", -1);
        List<String> items = new ArrayList<>(Math.max(0, p.length - 1));
        for (int i = 1; i < p.length; i++) {
            items.add(unescapeItem(p[i]));
        }
        return new java.util.AbstractMap.SimpleImmutableEntry<>(unescapeItem(p[0]), items);
    }

    /** Why a save of a schema with the {@code END_SAVE} trailer was refused. */
    static final String INCOMPLETE_SAVE =
            "Save file is incomplete (it was cut off or damaged while being written)";

    /** Result of loading a new-format save, including the resolved schema version. */
    public record LoadResult(LeagueRecord record, String schemaVersion) {}

    /**
     * Six trailing T: fields (17..22) for {@link LeagueRecord.SeasonBaseline}; empty when
     * absent. Older readers index the T: fields they know and ignore extras.
     */
    static String seasonBaselineFields(LeagueRecord.SeasonBaseline b) {
        if (b == null) {
            return "";
        }
        return "\t" + b.prestigeStart() + "\t" + b.rankPrestigeStart() + "\t" + b.projectedWins()
                + "\t" + b.projectedPollRank() + "\t" + b.startOffTal() + "\t" + b.startDefTal();
    }

    /**
     * T: fields 24-25: the offensive and defensive scheme numbers. Written after the
     * six baseline fields, which are left empty when there is no baseline so the
     * scheme fields keep their place.
     */
    static String playbookFields(LeagueRecord.TeamRecord t) {
        if (t.playbookOffense() < 0 && t.playbookDefense() < 0) {
            return "";
        }
        String baselinePlaceholder = t.seasonBaseline() == null ? "\t\t\t\t\t\t" : "";
        return baselinePlaceholder + "\t" + t.playbookOffense() + "\t" + t.playbookDefense();
    }

    private static int parsePlaybook(String[] p, int at) {
        if (p.length <= at) {
            return -1;
        }
        try {
            return Integer.parseInt(p[at].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** L: field 7: 1/0 once the recruiting gate state is known; empty when unknown. */
    private static String recruitingStartedField(Boolean started) {
        return started == null ? "" : (started ? "1" : "0");
    }

    /** @return the baseline at {@code p[from..from+5]}, or null when unreadable (then recomputed on load) */
    static LeagueRecord.SeasonBaseline parseSeasonBaseline(String[] p, int from) {
        try {
            return new LeagueRecord.SeasonBaseline(
                    Integer.parseInt(p[from].trim()),
                    Integer.parseInt(p[from + 1].trim()),
                    Integer.parseInt(p[from + 2].trim()),
                    Integer.parseInt(p[from + 3].trim()),
                    Float.parseFloat(p[from + 4].trim()),
                    Float.parseFloat(p[from + 5].trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void save(LeagueRecord league, OutputStream out) throws IOException {
        LineCountingWriter writer = new LineCountingWriter(
                new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        try {
        writer.write(SaveSchema.VERSION_PREFIX + SaveSchema.current() + "\n");

        // Save League Base (tab-separated so names may contain commas).
        // Fields 6 (rng seed), 7 (recruiting started) and 8 (regular-season
        // weeks) are optional on load; older builds simply ignore them.
        writer.write(LEAGUE_PREFIX + sanitizeInlineValue(league.leagueName()) + "\t" + league.year() + "\t"
                + league.currentWeek() + "\t" + sanitizeInlineValue(league.heismanWinnerName()) + "\t"
                + sanitizeInlineValue(league.nationalChampName()) + "\t"
                + league.rngSeed() + "\t"
                + recruitingStartedField(league.recruitingStarted()) + "\t"
                + league.regSeasonWeeks() + "\n");
        if (league.settings() != null) {
            writer.write(SETTINGS_PREFIX + league.settings().toSaveLine() + "\n");
        }
        LeagueRecord.LeagueState state = league.state();
        if (state != null) {
            writer.write(LEAGUE_STATE_PREFIX + state.scalars().toSaveLine() + "\n");
            for (String row : state.leagueHistory()) {
                writer.write(LEAGUE_HISTORY_PREFIX + singleLine(row) + "\n");
            }
            for (String row : state.heismanHistory()) {
                writer.write(HEISMAN_HISTORY_PREFIX + singleLine(row) + "\n");
            }
            if (!state.bowlNames().isEmpty()) {
                writer.write(BOWL_NAMES_PREFIX + joinTabs(state.bowlNames()) + "\n");
            }
            if (!state.playoffTeams().isEmpty()) {
                writer.write(PLAYOFF_TEAMS_PREFIX + joinTabs(state.playoffTeams()) + "\n");
            }
            for (java.util.Map.Entry<String, List<String>> e : state.lists().entrySet()) {
                writeListLine(writer, LEAGUE_LIST_PREFIX, e.getKey(), e.getValue());
            }
            for (LeagueRecord.DetachedPlayer d : state.detachedPlayers()) {
                writer.write(DETACHED_PLAYER_PREFIX + escapeItem(d.teamName()) + "\t"
                        + Persistence.toCsv(d.player()) + "\n");
                if (!d.state().isEmpty()) {
                    writer.write(PLAYER_STATE_PREFIX + d.state().toSaveLine() + "\n");
                }
            }
            for (LeagueRecord.DetachedStaff d : state.detachedStaff()) {
                writer.write(DETACHED_STAFF_PREFIX + escapeItem(d.teamName()) + "\t"
                        + Persistence.toCsv(d.staff()) + "\n");
            }
        }

        // Global Hall of Fame
        for (PlayerRecord p : league.leagueHoF()) {
            writer.write("HOF:" + Persistence.toCsv(p) + "\n");
        }

        // Global Records
        for (DataRecord r : league.leagueRecords()) {
            writer.write("LR:" + r.toCsv(recordFormat()) + "\n");
        }

        for (StaffRecord coach : league.coachFreeAgents()) {
            writer.write(FREE_AGENT_COACH_PREFIX + Persistence.toCsv(coach) + "\n");
        }

        for (LeagueRecord.ConferenceRecord c : league.conferences()) {
            String confLine = CONF_PREFIX + nameField(c.name());
            if (c.oocWeeks() != null && !c.oocWeeks().isEmpty()) {
                StringBuilder cw = new StringBuilder();
                for (int i = 0; i < c.oocWeeks().size(); i++) {
                    if (i > 0) {
                        cw.append(",");
                    }
                    cw.append(c.oocWeeks().get(i));
                }
                confLine += "|" + cw;
            }
            writer.write(confLine + "\n");
            if (!c.state().isEmpty()) {
                writer.write(CONF_STATE_PREFIX + c.state().toSaveLine() + "\n");
            }
            for (LeagueRecord.TeamRecord t : c.teams()) {
                writeTeam(writer, t);
            }
            writer.write(END_TOKEN + "_CONF\n");
        }
        if (state != null && !state.fcsTeams().isEmpty()) {
            writer.write(FCS_BLOCK_PREFIX + "\n");
            for (LeagueRecord.TeamRecord t : state.fcsTeams()) {
                writeTeam(writer, t);
            }
            writer.write(END_TOKEN + "_CONF\n");
        }

        if (league.scheduledGames() != null) {
            for (LeagueRecord.GameRecord g : league.scheduledGames()) {
                writer.write(GAME_PREFIX + g.toSaveLine() + "\n");
            }
        }
        writer.write(END_SAVE_PREFIX + writer.lines + "\n");
        writer.flush();
        } finally {
            writer.close();
        }
    }

    private static void writeTeam(java.io.Writer writer, LeagueRecord.TeamRecord t) throws IOException {
        writer.write(TEAM_PREFIX + nameField(t.name()) + "\t" + sanitizeInlineValue(t.abbr()) + "\t"
                + t.prestige() + "\t" + t.wins() + "\t" + t.losses() + "\t"
                + t.teamPollScore() + "\t" + t.rankTeamPollScore() + "\t"
                + sanitizeInlineValue(t.practiceFocus()) + "\t"
                + t.nilCollectiveLevel() + "\t"
                + sanitizeInlineValue(t.practicePositionGroup()) + "\t"
                + sanitizeInlineValue(t.focusIntensity()) + "\t"
                + t.prevRankTeamPollScore() + "\t"
                + nameField(t.rivalName()) + "\t"
                + sanitizeInlineValue(t.rivalryTrophyName()) + "\t"
                + t.rivalryWins() + "\t"
                + (t.holdsRivalryTrophy() ? 1 : 0) + "\t"
                + t.teamStadium()
                + seasonBaselineFields(t.seasonBaseline())
                + playbookFields(t) + "\n");
        if (!t.state().isEmpty()) {
            writer.write(TEAM_STATE_PREFIX + t.state().toSaveLine() + "\n");
        }

        // Coaches (a post lies vacant between firing and the coaching carousel)
        if (t.headCoach() != null) {
            writer.write(COACH_PREFIX + "HC," + Persistence.toCsv(t.headCoach()) + "\n");
        }
        if (t.offenseCoach() != null) {
            writer.write(COACH_PREFIX + "OC," + Persistence.toCsv(t.offenseCoach()) + "\n");
        }
        if (t.defenseCoach() != null) {
            writer.write(COACH_PREFIX + "DC," + Persistence.toCsv(t.defenseCoach()) + "\n");
        }

        // Roster
        for (int i = 0; i < t.roster().size(); i++) {
            writer.write(PLAYER_PREFIX + Persistence.toCsv(t.roster().get(i)) + "\n");
            LeagueRecord.StateBag px = i < t.rosterState().size() ? t.rosterState().get(i) : null;
            if (px != null && !px.isEmpty()) {
                writer.write(PLAYER_STATE_PREFIX + px.toSaveLine() + "\n");
            }
        }

        // History
        for (TeamHistoryRecord h : t.history()) {
            writer.write(HISTORY_PREFIX + h.summary() + "\n");
        }

        // Records
        for (DataRecord r : t.records()) {
            writer.write(RECORD_PREFIX + r.toCsv(recordFormat()) + "\n");
        }
        if ((t.oocWeeks() != null && !t.oocWeeks().isEmpty())
                || (t.oocOpponentNames() != null && !t.oocOpponentNames().isEmpty())) {
            StringBuilder to = new StringBuilder(TEAM_OOC_PREFIX);
            if (t.oocWeeks() != null) {
                for (int i = 0; i < t.oocWeeks().size(); i++) {
                    if (i > 0) {
                        to.append(",");
                    }
                    to.append(t.oocWeeks().get(i));
                }
            }
            to.append("|");
            if (t.oocOpponentNames() != null) {
                for (int i = 0; i < t.oocOpponentNames().size(); i++) {
                    if (i > 0) {
                        to.append(";");
                    }
                    to.append(nameField(t.oocOpponentNames().get(i)));
                }
            }
            writer.write(to + "\n");
        }
        for (java.util.Map.Entry<String, List<String>> e : t.lists().entrySet()) {
            writeListLine(writer, TEAM_LIST_PREFIX, e.getKey(), e.getValue());
        }
        writer.write(END_TOKEN + "_TEAM\n");
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static String joinTabs(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append('\t');
            }
            String v = values.get(i);
            sb.append(v == null ? "" : singleLine(v).replace('\t', ' '));
        }
        return sb.toString();
    }

    private static List<String> splitTabs(String body) {
        List<String> out = new ArrayList<>();
        for (String s : body.split("\t", -1)) {
            out.add(s);
        }
        return out;
    }

    private static String sanitizeInlineValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replaceAll("\\s+", " ").trim();
    }

    /**
     * Characters escaped in team and conference names on {@code C:}, {@code T:} and
     * {@code TO:} lines: {@code |} ends a conference name and splits the {@code TO:}
     * weeks from the opponents, {@code ;} separates opponents ({@link Persistence#encodeField}).
     */
    private static final String NAME_FIELD_CHARS = "%;|";

    /** A team or conference name: whitespace normalized as before, then delimiters escaped. */
    static String nameField(String value) {
        return Persistence.encodeField(sanitizeInlineValue(value), NAME_FIELD_CHARS);
    }

    /** Reads {@link #nameField}; older saves wrote names raw and read back unchanged. */
    static String readNameField(String value) {
        return Persistence.decodeField(value);
    }

    /** Whether {@code s} is a {@code C:} line's OOC week list: integers (a week can be negative) and commas only. */
    private static boolean isWeekList(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= '0' && c <= '9') && c != ',' && c != ' ' && c != '-') {
                return false;
            }
        }
        return true;
    }

    public static LeagueRecord load(InputStream in) throws IOException {
        return loadWithVersion(in).record();
    }

    public static LoadResult loadWithVersion(InputStream in) throws IOException {
        String leagueName = "";
        int year = 0, week = 0;
        String heisman = "", champ = "";
        long rngSeed = 0;
        int seasonWeeks = 0;
        Boolean recruitingStarted = null;
        List<PlayerRecord> hof = new ArrayList<>();
        List<DataRecord> lRecords = new ArrayList<>();
        List<StaffRecord> freeAgentCoaches = new ArrayList<>();
        List<LeagueRecord.ConferenceRecord> conferences = new ArrayList<>();

        LeagueRecord.ConferenceRecord currentConf = null;
        List<LeagueRecord.TeamRecord> confTeams = null;
        
        LeagueRecord.TeamRecord currentTeam = null;
        String teamName = "", teamAbbr = "";
        int prestige = 0;
        StaffRecord hc = null, oc = null, dc = null;
        List<PlayerRecord> roster = null;
        List<TeamHistoryRecord> history = null;
        List<DataRecord> tRecords = null;
        int teamWins = 0;
        int teamLosses = 0;
        float teamPollSnap = 0f;
        int teamRankSnap = 0;
        List<Integer> teamOocWeeksBuf = new ArrayList<>();
        List<String> teamOocNamesBuf = new ArrayList<>();
        String teamPracticeFocus = "";
        String teamPracticePositionGroup = "";
        String teamFocusIntensity = "";
        int teamNilCollectiveLevel = 0;
        int teamPrevRankTeamPollScore = 0;
        String teamRivalName = "";
        String teamRivalryTrophyName = "";
        int teamRivalryWins = 0;
        boolean teamHoldsRivalryTrophy = false;
        int teamStadiumLevel = 1;
        LeagueRecord.SeasonBaseline teamSeasonBaseline = null;
        int teamPlaybookOffense = -1;
        int teamPlaybookDefense = -1;
        LeagueRecord.StateBag teamState = LeagueRecord.StateBag.EMPTY;
        List<LeagueRecord.StateBag> confStates = new ArrayList<>();
        LeagueRecord.StateBag leagueScalars = null;
        List<String> leagueHistory = new ArrayList<>();
        List<String> heismanHistory = new ArrayList<>();
        List<String> bowlNames = new ArrayList<>();
        List<String> playoffTeams = new ArrayList<>();
        List<LeagueRecord.GameRecord> gameRecords = new ArrayList<>();
        java.util.LinkedHashMap<String, List<String>> leagueLists = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, List<String>> teamLists = new java.util.LinkedHashMap<>();
        List<LeagueRecord.StateBag> rosterState = null;
        List<LeagueRecord.DetachedPlayer> detachedPlayers = new ArrayList<>();
        List<LeagueRecord.DetachedStaff> detachedStaff = new ArrayList<>();
        List<LeagueRecord.TeamRecord> fcsTeams = new ArrayList<>();
        // What the last P:/XP: line was, so a PX: line attaches to it.
        int lastPlayerLine = 0; // 0 none, 1 roster, 2 detached
        LeagueRecord.Settings settings = null;
        String schemaVersion = null;
        boolean sawLeagueHeader = false;
        TrailerCheck trailer = new TrailerCheck();

        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
            trailer.line(line);
            if (line.startsWith(SaveSchema.VERSION_PREFIX)) {
                schemaVersion = SaveSchema.requireSupported(
                        SaveSchema.parseVersionToken(line.substring(SaveSchema.VERSION_PREFIX.length())));
            } else if (line.startsWith(LEAGUE_PREFIX)) {
                sawLeagueHeader = true;
                String body = line.substring(LEAGUE_PREFIX.length());
                ParsedLeagueHeader h = body.indexOf('\t') >= 0
                        ? parseLeagueHeaderTabSeparated(body)
                        : parseLeagueHeaderCommaSeparated(body);
                leagueName = h.leagueName();
                year = h.year();
                week = h.week();
                heisman = h.heisman();
                champ = h.champ();
                rngSeed = h.rngSeed();
                seasonWeeks = h.regSeasonWeeks();
                recruitingStarted = h.recruitingStarted();
            } else if (line.startsWith(SETTINGS_PREFIX)) {
                settings = LeagueRecord.Settings.fromSaveLine(line.substring(SETTINGS_PREFIX.length()));
            } else if (line.startsWith(LEAGUE_STATE_PREFIX)) {
                leagueScalars = LeagueRecord.StateBag.fromSaveLine(line.substring(LEAGUE_STATE_PREFIX.length()));
            } else if (line.startsWith(LEAGUE_HISTORY_PREFIX)) {
                leagueHistory.add(line.substring(LEAGUE_HISTORY_PREFIX.length()));
            } else if (line.startsWith(HEISMAN_HISTORY_PREFIX)) {
                heismanHistory.add(line.substring(HEISMAN_HISTORY_PREFIX.length()));
            } else if (line.startsWith(LEAGUE_LIST_PREFIX)) {
                java.util.Map.Entry<String, List<String>> e = parseListLine(line.substring(LEAGUE_LIST_PREFIX.length()));
                leagueLists.put(e.getKey(), e.getValue());
            } else if (line.startsWith(TEAM_LIST_PREFIX)) {
                java.util.Map.Entry<String, List<String>> e = parseListLine(line.substring(TEAM_LIST_PREFIX.length()));
                teamLists.put(e.getKey(), e.getValue());
            } else if (line.startsWith(PLAYER_STATE_PREFIX)) {
                LeagueRecord.StateBag px = LeagueRecord.StateBag.fromSaveLine(line.substring(PLAYER_STATE_PREFIX.length()));
                if (lastPlayerLine == 1 && rosterState != null && !rosterState.isEmpty()) {
                    rosterState.set(rosterState.size() - 1, px);
                } else if (lastPlayerLine == 2 && !detachedPlayers.isEmpty()) {
                    LeagueRecord.DetachedPlayer d = detachedPlayers.get(detachedPlayers.size() - 1);
                    detachedPlayers.set(detachedPlayers.size() - 1,
                            new LeagueRecord.DetachedPlayer(d.teamName(), d.player(), px));
                }
            } else if (line.startsWith(DETACHED_PLAYER_PREFIX)) {
                String[] p = line.substring(DETACHED_PLAYER_PREFIX.length()).split("\t", 2);
                detachedPlayers.add(new LeagueRecord.DetachedPlayer(unescapeItem(p[0]),
                        PlayerRecord.fromCsv(p[1]), null));
                lastPlayerLine = 2;
            } else if (line.startsWith(DETACHED_STAFF_PREFIX)) {
                String[] p = line.substring(DETACHED_STAFF_PREFIX.length()).split("\t", 2);
                detachedStaff.add(new LeagueRecord.DetachedStaff(unescapeItem(p[0]), StaffRecord.fromCsv(p[1])));
            } else if (line.startsWith(FCS_BLOCK_PREFIX)) {
                confTeams = fcsTeams;
                currentConf = null;
            } else if (line.startsWith(BOWL_NAMES_PREFIX)) {
                bowlNames = splitTabs(line.substring(BOWL_NAMES_PREFIX.length()));
            } else if (line.startsWith(PLAYOFF_TEAMS_PREFIX)) {
                playoffTeams = splitTabs(line.substring(PLAYOFF_TEAMS_PREFIX.length()));
            } else if (line.startsWith(CONF_STATE_PREFIX)) {
                if (!confStates.isEmpty()) {
                    confStates.set(confStates.size() - 1,
                            LeagueRecord.StateBag.fromSaveLine(line.substring(CONF_STATE_PREFIX.length())));
                }
            } else if (line.startsWith(TEAM_STATE_PREFIX)) {
                teamState = LeagueRecord.StateBag.fromSaveLine(line.substring(TEAM_STATE_PREFIX.length()));
            } else if (line.startsWith("HOF:")) {
                hof.add(PlayerRecord.fromCsv(line.substring(4)));
            } else if (line.startsWith("LR:")) {
                DataRecord dr = DataRecord.fromCsv(line.substring(3));
                if (dr != null) lRecords.add(dr);
            } else if (line.startsWith(FREE_AGENT_COACH_PREFIX)) {
                freeAgentCoaches.add(StaffRecord.fromCsv(line.substring(FREE_AGENT_COACH_PREFIX.length())));
            } else if (line.startsWith(CONF_PREFIX)) {
                confTeams = new ArrayList<>();
                String raw = line.substring(2);
                String cname;
                List<Integer> confOoc = new ArrayList<>();
                // The week list follows the last '|' (older saves wrote a '|' in
                // the name raw; a tail that is not a week list is part of the name).
                int pipe = raw.lastIndexOf('|');
                if (pipe >= 0 && !isWeekList(raw.substring(pipe + 1))) {
                    pipe = -1;
                }
                if (pipe >= 0) {
                    cname = readNameField(raw.substring(0, pipe));
                    String rest = raw.substring(pipe + 1);
                    if (!rest.isEmpty()) {
                        for (String s : rest.split(",")) {
                            if (!s.isEmpty()) {
                                confOoc.add(Integer.parseInt(s.trim()));
                            }
                        }
                    }
                } else {
                    cname = readNameField(raw);
                }
                currentConf = new LeagueRecord.ConferenceRecord(cname, List.copyOf(confOoc), confTeams);
                conferences.add(currentConf);
                confStates.add(LeagueRecord.StateBag.EMPTY);
            } else if (line.startsWith(TEAM_PREFIX)) {
                hc = null;
                oc = null;
                dc = null;
                teamWins = 0;
                teamLosses = 0;
                teamPollSnap = 0f;
                teamRankSnap = 0;
                teamOocWeeksBuf.clear();
                teamOocNamesBuf.clear();
                teamPracticeFocus = "";
                teamPracticePositionGroup = "";
                teamFocusIntensity = "";
                teamNilCollectiveLevel = 0;
                // Optional fields must not carry over from the previous team.
                teamPrevRankTeamPollScore = 0;
                teamRivalName = "";
                teamRivalryTrophyName = "";
                teamRivalryWins = 0;
                teamHoldsRivalryTrophy = false;
                teamStadiumLevel = 1;
                teamState = LeagueRecord.StateBag.EMPTY;
                teamSeasonBaseline = null;
                teamPlaybookOffense = -1;
                teamPlaybookDefense = -1;
                String[] p = line.substring(2).split("\t", -1);
                teamName = readNameField(p[0]);
                teamAbbr = p[1];
                prestige = Integer.parseInt(p[2]);
                if (p.length >= 5) {
                    teamWins = Integer.parseInt(p[3]);
                    teamLosses = Integer.parseInt(p[4]);
                }
                if (p.length >= 7) {
                    teamPollSnap = Float.parseFloat(p[5]);
                    teamRankSnap = Integer.parseInt(p[6]);
                }
                if (p.length >= 8) {
                    teamPracticeFocus = p[7];
                }
                if (p.length >= 9) {
                    try {
                        teamNilCollectiveLevel = Integer.parseInt(p[8].trim());
                    } catch (NumberFormatException e) {
                        teamNilCollectiveLevel = 0;
                    }
                }
                if (p.length >= 10) {
                    teamPracticePositionGroup = p[9];
                }
                if (p.length >= 11) {
                    teamFocusIntensity = p[10];
                }
                if (p.length >= 12 && !p[11].trim().isEmpty()) {
                    teamPrevRankTeamPollScore = Integer.parseInt(p[11].trim());
                }
                if (p.length >= 13) {
                    teamRivalName = readNameField(p[12]);
                }
                if (p.length >= 14) {
                    teamRivalryTrophyName = p[13];
                }
                if (p.length >= 15 && !p[14].trim().isEmpty()) {
                    teamRivalryWins = Integer.parseInt(p[14].trim());
                }
                if (p.length >= 16 && !p[15].trim().isEmpty()) {
                    teamHoldsRivalryTrophy = "1".equals(p[15].trim());
                }
                if (p.length >= 17 && !p[16].trim().isEmpty()) {
                    teamStadiumLevel = Integer.parseInt(p[16].trim());
                }
                if (p.length >= 23) {
                    teamSeasonBaseline = parseSeasonBaseline(p, 17);
                }
                teamPlaybookOffense = parsePlaybook(p, 23);
                teamPlaybookDefense = parsePlaybook(p, 24);
                roster = new ArrayList<>();
                history = new ArrayList<>();
                tRecords = new ArrayList<>();
                teamLists = new java.util.LinkedHashMap<>();
                rosterState = new ArrayList<>();
                lastPlayerLine = 0;
            } else if (line.startsWith(COACH_PREFIX)) {
                String[] p = line.substring(2).split(",", 2);
                StaffRecord s = StaffRecord.fromCsv(p[1]);
                if (p[0].equals("HC")) hc = s;
                else if (p[0].equals("OC")) oc = s;
                else if (p[0].equals("DC")) dc = s;
            } else if (line.startsWith(PLAYER_PREFIX)) {
                roster.add(PlayerRecord.fromCsv(line.substring(2)));
                rosterState.add(LeagueRecord.StateBag.EMPTY);
                lastPlayerLine = 1;
            } else if (line.startsWith(HISTORY_PREFIX)) {
                history.add(TeamHistoryRecord.fromCsv(line.substring(2)));
            } else if (line.startsWith(RECORD_PREFIX)) {
                DataRecord dr = DataRecord.fromCsv(line.substring(2));
                if (dr != null) tRecords.add(dr);
            } else if (line.startsWith(TEAM_OOC_PREFIX)) {
                teamOocWeeksBuf.clear();
                teamOocNamesBuf.clear();
                String body = line.substring(TEAM_OOC_PREFIX.length());
                // Weeks never hold a '|', so the first one ends them (older saves
                // wrote opponent names raw, and a '|' in one split them apart).
                int bar = body.indexOf('|');
                String[] sides = bar < 0 ? new String[]{body}
                        : new String[]{body.substring(0, bar), body.substring(bar + 1)};
                if (sides.length >= 1 && !sides[0].isEmpty()) {
                    for (String w : sides[0].split(",")) {
                        if (!w.isEmpty()) {
                            teamOocWeeksBuf.add(Integer.parseInt(w.trim()));
                        }
                    }
                }
                if (sides.length >= 2 && !sides[1].isEmpty()) {
                    for (String n : sides[1].split(";")) {
                        if (!n.isEmpty()) {
                            teamOocNamesBuf.add(readNameField(n.trim()));
                        }
                    }
                }
            } else if (line.equals(END_TOKEN + "_TEAM")) {
                confTeams.add(new LeagueRecord.TeamRecord(teamName, teamAbbr, prestige, teamWins, teamLosses,
                        List.copyOf(teamOocWeeksBuf), List.copyOf(teamOocNamesBuf),
                        teamPollSnap, teamRankSnap,
                        hc, oc, dc, roster, history, tRecords, teamPracticeFocus,
                        teamPracticePositionGroup, teamFocusIntensity,
                        teamNilCollectiveLevel, "", teamPrevRankTeamPollScore,
                        teamRivalName, teamRivalryTrophyName, teamRivalryWins, teamHoldsRivalryTrophy,
                        teamStadiumLevel, teamSeasonBaseline, teamPlaybookOffense, teamPlaybookDefense,
                        teamState, teamLists, rosterState));
            } else if (line.startsWith(GAME_PREFIX)) {
                gameRecords.add(LeagueRecord.GameRecord.fromSaveLine(line.substring(GAME_PREFIX.length())));
            }
        }
        }

        if (!sawLeagueHeader) {
            throw new IOException("Save file is missing the L: league header");
        }
        if (SaveSchema.hasEndTrailer(schemaVersion) && !trailer.complete()) {
            // Checked before the unversioned default: a file without a V: line predates the trailer.
            throw new IOException(INCOMPLETE_SAVE);
        }
        if (schemaVersion == null) {
            schemaVersion = SaveSchema.unversionedNewFormatDefault();
        }
        List<LeagueRecord.ConferenceRecord> confs = new ArrayList<>(conferences.size());
        for (int i = 0; i < conferences.size(); i++) {
            LeagueRecord.ConferenceRecord c = conferences.get(i);
            confs.add(new LeagueRecord.ConferenceRecord(c.name(), c.oocWeeks(), c.teams(), confStates.get(i)));
        }
        LeagueRecord.LeagueState leagueState = leagueScalars == null ? null
                : new LeagueRecord.LeagueState(leagueHistory, heismanHistory, bowlNames, playoffTeams, leagueScalars,
                        leagueLists, detachedPlayers, detachedStaff, fcsTeams);
        LeagueRecord raw = new LeagueRecord(leagueName, year, week, confs, hof, lRecords, heisman, champ,
                List.copyOf(gameRecords), rngSeed, settings, recruitingStarted, freeAgentCoaches, seasonWeeks,
                leagueState);
        LeagueRecord migrated = SaveSchema.migrate(schemaVersion, raw);
        return new LoadResult(migrated, schemaVersion);
    }

    /**
     * Parsed {@code L:} header: league name, season year, current week, heisman string,
     * national champ string, (from field 6) the RNG seed — 0 when absent (legacy saves) — and
     * (field 7) whether the recruiting gate already ran, null when absent, and (field 8) the
     * regular-season length in weeks, 0 when absent (legacy 13-week calendar).
     */
    private record ParsedLeagueHeader(String leagueName, int year, int week, String heisman, String champ, long rngSeed,
                                      Boolean recruitingStarted, int regSeasonWeeks) {}

    private static ParsedLeagueHeader parseLeagueHeaderTabSeparated(String body) throws IOException {
        String[] p = body.split("\t", -1);
        if (p.length < 5) {
            throw new IOException("L: tab line expected at least 5 fields, got " + p.length + ": " + body);
        }
        try {
            long seed = 0;
            if (p.length >= 6 && !p[5].trim().isEmpty()) {
                seed = Long.parseLong(p[5].trim());
            }
            Boolean recruitingStarted = null;
            int seasonWeeks = 0;
            if (p.length >= 7 && !p[6].trim().isEmpty()) {
                String f7 = p[6].trim();
                if (p.length == 7 && f7.length() > 1) {
                    // Short-lived development format put the season length in field 7.
                    seasonWeeks = Integer.parseInt(f7);
                } else {
                    recruitingStarted = "1".equals(f7);
                }
            }
            if (p.length >= 8 && !p[7].trim().isEmpty()) {
                seasonWeeks = Integer.parseInt(p[7].trim());
            }
            return new ParsedLeagueHeader(
                    p[0],
                    Integer.parseInt(p[1].trim()),
                    Integer.parseInt(p[2].trim()),
                    p[3],
                    p[4],
                    seed,
                    recruitingStarted,
                    seasonWeeks);
        } catch (NumberFormatException e) {
            throw new IOException("L: tab line bad year/week/seed: " + body, e);
        }
    }

    /**
     * Legacy comma {@code L:} line: only {@code leagueName} may safely contain commas; other fields should not.
     */
    private static ParsedLeagueHeader parseLeagueHeaderCommaSeparated(String body) throws IOException {
        if (body == null || body.isEmpty()) {
            throw new IOException("Empty L: league header");
        }
        String s = body;
        try {
            int i = s.lastIndexOf(',');
            if (i < 0) {
                throw new IOException("L: line has no field separators");
            }
            String champ = s.substring(i + 1);
            s = s.substring(0, i);

            i = s.lastIndexOf(',');
            if (i < 0) {
                throw new IOException("L: line truncated (champ only)");
            }
            String heisman = s.substring(i + 1);
            s = s.substring(0, i);

            i = s.lastIndexOf(',');
            if (i < 0) {
                throw new IOException("L: line truncated (heisman)");
            }
            int week = Integer.parseInt(s.substring(i + 1).trim());
            s = s.substring(0, i);

            i = s.lastIndexOf(',');
            if (i < 0) {
                throw new IOException("L: line truncated (week)");
            }
            int year = Integer.parseInt(s.substring(i + 1).trim());
            String leagueName = s.substring(0, i);
            return new ParsedLeagueHeader(leagueName, year, week, heisman, champ, 0, null, 0);
        } catch (NumberFormatException e) {
            throw new IOException("L: line has non-numeric year or week: " + body, e);
        }
    }
}
