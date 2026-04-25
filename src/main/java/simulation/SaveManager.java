package simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private static final String END_TOKEN = "END";

    public static void save(LeagueRecord league, OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        
        // Save League Base
        writer.write(LEAGUE_PREFIX + sanitizeInlineValue(league.leagueName()) + "," + league.year() + "," +
                     league.currentWeek() + "," + sanitizeInlineValue(league.heismanWinnerName()) + "," +
                     sanitizeInlineValue(league.nationalChampName()) + "\n");

        // Global Hall of Fame
        for (PlayerRecord p : league.leagueHoF()) {
            writer.write("HOF:" + Persistence.toCsv(p) + "\n");
        }

        // Global Records
        for (DataRecord r : league.leagueRecords()) {
            writer.write("LR:" + r.toCsv(new java.text.DecimalFormat("#.##")) + "\n");
        }

        for (LeagueRecord.ConferenceRecord c : league.conferences()) {
            writer.write(CONF_PREFIX + sanitizeInlineValue(c.name()) + "\n");
            for (LeagueRecord.TeamRecord t : c.teams()) {
                writer.write(TEAM_PREFIX + sanitizeInlineValue(t.name()) + "," + sanitizeInlineValue(t.abbr()) + ","
                        + t.prestige() + "," + t.wins() + "," + t.losses() + "\n");
                
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
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public static LeagueRecord load(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        
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
        List<LeagueRecord.GameRecord> gameRecords = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith(LEAGUE_PREFIX)) {
                String[] p = line.substring(2).split(",");
                leagueName = p[0];
                year = Integer.parseInt(p[1]);
                week = Integer.parseInt(p[2]);
                heisman = p[3];
                champ = p[4];
            } else if (line.startsWith("HOF:")) {
                hof.add(PlayerRecord.fromCsv(line.substring(4)));
            } else if (line.startsWith("LR:")) {
                lRecords.add(DataRecord.fromCsv(line.substring(3)));
            } else if (line.startsWith(CONF_PREFIX)) {
                confTeams = new ArrayList<>();
                currentConf = new LeagueRecord.ConferenceRecord(line.substring(2), confTeams);
                conferences.add(currentConf);
            } else if (line.startsWith(TEAM_PREFIX)) {
                hc = null;
                oc = null;
                dc = null;
                teamWins = 0;
                teamLosses = 0;
                String[] p = line.substring(2).split(",");
                teamName = p[0];
                teamAbbr = p[1];
                prestige = Integer.parseInt(p[2]);
                if (p.length >= 5) {
                    teamWins = Integer.parseInt(p[3]);
                    teamLosses = Integer.parseInt(p[4]);
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
            } else if (line.equals(END_TOKEN + "_TEAM")) {
                confTeams.add(new LeagueRecord.TeamRecord(teamName, teamAbbr, prestige, teamWins, teamLosses,
                        hc, oc, dc, roster, history, tRecords));
            } else if (line.startsWith(GAME_PREFIX)) {
                gameRecords.add(LeagueRecord.GameRecord.fromSaveLine(line.substring(GAME_PREFIX.length())));
            }
        }
        
        return new LeagueRecord(leagueName, year, week, conferences, hof, lRecords, heisman, champ,
                List.copyOf(gameRecords));
    }
}
