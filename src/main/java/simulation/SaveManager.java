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
    private static final String END_TOKEN = "END";

    public static void save(LeagueRecord league, OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        
        // Save League Base (tab-separated so names may contain commas)
        writer.write(LEAGUE_PREFIX + sanitizeInlineValue(league.leagueName()) + "\t" + league.year() + "\t"
                + league.currentWeek() + "\t" + sanitizeInlineValue(league.heismanWinnerName()) + "\t"
                + sanitizeInlineValue(league.nationalChampName()) + "\n");

        // Global Hall of Fame
        for (PlayerRecord p : league.leagueHoF()) {
            writer.write("HOF:" + Persistence.toCsv(p) + "\n");
        }

        // Global Records
        for (DataRecord r : league.leagueRecords()) {
            writer.write("LR:" + r.toCsv(new java.text.DecimalFormat("#.##")) + "\n");
        }

        for (LeagueRecord.ConferenceRecord c : league.conferences()) {
            String confLine = CONF_PREFIX + sanitizeInlineValue(c.name());
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
            for (LeagueRecord.TeamRecord t : c.teams()) {
                writer.write(TEAM_PREFIX + sanitizeInlineValue(t.name()) + "," + sanitizeInlineValue(t.abbr()) + ","
                        + t.prestige() + "," + t.wins() + "," + t.losses() + ","
                        + t.teamPollScore() + "," + t.rankTeamPollScore() + "\n");
                
                // Coaches
                writer.write(COACH_PREFIX + "HC," + Persistence.toCsv(t.headCoach()) + "\n");
                writer.write(COACH_PREFIX + "OC," + Persistence.toCsv(t.offenseCoach()) + "\n");
                writer.write(COACH_PREFIX + "DC," + Persistence.toCsv(t.defenseCoach()) + "\n");

                // Roster
                for (PlayerRecord p : t.roster()) {
                    writer.write(PLAYER_PREFIX + Persistence.toCsv(p) + "\n");
                }

                // History
                for (TeamHistoryRecord h : t.history()) {
                    writer.write(HISTORY_PREFIX + h.summary() + "\n");
                }

                // Records
                for (DataRecord r : t.records()) {
                    writer.write(RECORD_PREFIX + r.toCsv(new java.text.DecimalFormat("#.##")) + "\n");
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
                            to.append(sanitizeInlineValue(t.oocOpponentNames().get(i)).replace(";", " "));
                        }
                    }
                    writer.write(to + "\n");
                }
                writer.write(END_TOKEN + "_TEAM\n");
            }
            writer.write(END_TOKEN + "_CONF\n");
        }

        if (league.scheduledGames() != null) {
            for (LeagueRecord.GameRecord g : league.scheduledGames()) {
                writer.write(GAME_PREFIX + g.toSaveLine() + "\n");
            }
        }
        writer.flush();
    }

    private static String sanitizeInlineValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replaceAll("\\s+", " ").trim();
    }

    public static LeagueRecord load(InputStream in) throws IOException {
        String leagueName = "";
        int year = 0, week = 0;
        String heisman = "", champ = "";
        List<PlayerRecord> hof = new ArrayList<>();
        List<DataRecord> lRecords = new ArrayList<>();
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
        List<LeagueRecord.GameRecord> gameRecords = new ArrayList<>();

        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
            if (line.startsWith(LEAGUE_PREFIX)) {
                String body = line.substring(LEAGUE_PREFIX.length());
                ParsedLeagueHeader h = body.indexOf('\t') >= 0
                        ? parseLeagueHeaderTabSeparated(body)
                        : parseLeagueHeaderCommaSeparated(body);
                leagueName = h.leagueName();
                year = h.year();
                week = h.week();
                heisman = h.heisman();
                champ = h.champ();
            } else if (line.startsWith("HOF:")) {
                hof.add(PlayerRecord.fromCsv(line.substring(4)));
            } else if (line.startsWith("LR:")) {
                lRecords.add(DataRecord.fromCsv(line.substring(3)));
            } else if (line.startsWith(CONF_PREFIX)) {
                confTeams = new ArrayList<>();
                String raw = line.substring(2);
                String cname;
                List<Integer> confOoc = new ArrayList<>();
                int pipe = raw.indexOf('|');
                if (pipe >= 0) {
                    cname = raw.substring(0, pipe);
                    String rest = raw.substring(pipe + 1);
                    if (!rest.isEmpty()) {
                        for (String s : rest.split(",")) {
                            if (!s.isEmpty()) {
                                confOoc.add(Integer.parseInt(s.trim()));
                            }
                        }
                    }
                } else {
                    cname = raw;
                }
                currentConf = new LeagueRecord.ConferenceRecord(cname, List.copyOf(confOoc), confTeams);
                conferences.add(currentConf);
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
                String[] p = line.substring(2).split(",");
                teamName = p[0];
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
                roster = new ArrayList<>();
                history = new ArrayList<>();
                tRecords = new ArrayList<>();
            } else if (line.startsWith(COACH_PREFIX)) {
                String[] p = line.substring(2).split(",", 2);
                StaffRecord s = StaffRecord.fromCsv(p[1]);
                if (p[0].equals("HC")) hc = s;
                else if (p[0].equals("OC")) oc = s;
                else if (p[0].equals("DC")) dc = s;
            } else if (line.startsWith(PLAYER_PREFIX)) {
                roster.add(PlayerRecord.fromCsv(line.substring(2)));
            } else if (line.startsWith(HISTORY_PREFIX)) {
                history.add(TeamHistoryRecord.fromCsv(line.substring(2)));
            } else if (line.startsWith(RECORD_PREFIX)) {
                tRecords.add(DataRecord.fromCsv(line.substring(2)));
            } else if (line.startsWith(TEAM_OOC_PREFIX)) {
                teamOocWeeksBuf.clear();
                teamOocNamesBuf.clear();
                String body = line.substring(TEAM_OOC_PREFIX.length());
                String[] sides = body.split("\\|", -1);
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
                            teamOocNamesBuf.add(n.trim());
                        }
                    }
                }
            } else if (line.equals(END_TOKEN + "_TEAM")) {
                confTeams.add(new LeagueRecord.TeamRecord(teamName, teamAbbr, prestige, teamWins, teamLosses,
                        List.copyOf(teamOocWeeksBuf), List.copyOf(teamOocNamesBuf),
                        teamPollSnap, teamRankSnap,
                        hc, oc, dc, roster, history, tRecords));
            } else if (line.startsWith(GAME_PREFIX)) {
                gameRecords.add(LeagueRecord.GameRecord.fromSaveLine(line.substring(GAME_PREFIX.length())));
            }
        }
        }

        return new LeagueRecord(leagueName, year, week, conferences, hof, lRecords, heisman, champ,
                List.copyOf(gameRecords));
    }

    /**
     * Parsed {@code L:} header: league name, season year, current week, heisman string, national champ string.
     */
    private record ParsedLeagueHeader(String leagueName, int year, int week, String heisman, String champ) {}

    private static ParsedLeagueHeader parseLeagueHeaderTabSeparated(String body) throws IOException {
        String[] p = body.split("\t", -1);
        if (p.length < 5) {
            throw new IOException("L: tab line expected 5 fields, got " + p.length + ": " + body);
        }
        try {
            return new ParsedLeagueHeader(
                    p[0],
                    Integer.parseInt(p[1].trim()),
                    Integer.parseInt(p[2].trim()),
                    p[3],
                    p[4]);
        } catch (NumberFormatException e) {
            throw new IOException("L: tab line bad year/week: " + body, e);
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
            return new ParsedLeagueHeader(leagueName, year, week, heisman, champ);
        } catch (NumberFormatException e) {
            throw new IOException("L: line has non-numeric year or week: " + body, e);
        }
    }
}
