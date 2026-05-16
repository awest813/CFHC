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
        for (Team team : league.getTeamList()) {
            if (team.getName().equals(teamName)) {
                for (Player p : team.getAllPlayers()) {
                    if (p.name.equals(name)) return p;
                }
            }
        }
        return null;
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
}