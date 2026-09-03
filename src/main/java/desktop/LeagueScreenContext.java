package desktop;

import positions.Player;
import simulation.AudioManager;
import simulation.DataRecord;
import simulation.League;
import simulation.LeagueRecord;
import simulation.Team;

import javax.swing.JFrame;
import java.util.Map;

/**
 * Context bundle passed to desktop league screens so they can access
 * the live engine model without depending on LeagueHomeView internals.
 */
public class LeagueScreenContext {

    private final League league;
    private LeagueRecord record;
    private final Map<String, Team> teamMap;
    private final AudioManager audio;
    private final DesktopUiBridge bridge;
    private final JFrame parentFrame;

    public interface Navigation {
        void openTeamDetail(Team team);
        void openUserTeamDetail();
        void selectScreen(String title);
    }

    private final Navigation nav;

    public LeagueScreenContext(League league, LeagueRecord record, Map<String, Team> teamMap,
                               AudioManager audio, DesktopUiBridge bridge, JFrame parentFrame, Navigation nav) {
        this.league = league;
        this.record = record;
        this.teamMap = teamMap;
        this.audio = audio;
        this.bridge = bridge;
        this.parentFrame = parentFrame;
        this.nav = nav;
    }

    public League league() { return league; }
    public LeagueRecord record() { return record; }
    public Map<String, Team> teamMap() { return teamMap; }
    public AudioManager audio() { return audio; }
    public DesktopUiBridge bridge() { return bridge; }
    public JFrame parent() { return parentFrame; }
    public Navigation nav() { return nav; }

    void updateRecord(LeagueRecord newRecord) { this.record = newRecord; }

    /** Find a live player by name and team across the league. */
    public Player findPlayerInLeague(String name, String teamName) {
        return PlayerSearch.findInLeague(league, name, teamName);
    }

    /** Format a DataRecord value as a display string. */
    public static String formatValue(float value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    /** Format a DataRecord holder field. */
    public static String formatHolder(String raw) {
        if (raw == null || raw.isEmpty()) return "-";
        String[] parts = raw.split("\\(");
        return parts[0].trim();
    }

    /**
     * True when every leaderboard row's value parses to zero — the engine
     * seeds all-time leaderboards at 0 before any season completes, and a
     * ranked table of all zeros reads as a bug rather than an empty state.
     * Rows are "rank,name,value" strings (value may carry a % suffix).
     */
    public static boolean isLeaderboardAllZero(java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) return false;
        int parsed = 0;
        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length < 3) continue;
            try {
                if (Float.parseFloat(parts[2].trim().replace("%", "")) != 0f) {
                    return false;
                }
                parsed++;
            } catch (NumberFormatException ignored) {
                // Non-numeric value column — treat as meaningful data.
                return false;
            }
        }
        return parsed > 0;
    }
}