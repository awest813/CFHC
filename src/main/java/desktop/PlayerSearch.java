package desktop;

import java.util.Map;
import simulation.League;
import simulation.Team;
import positions.Player;

/**
 * Shared utility for finding players by name within a team or across the league.
 * Consolidates duplicate implementations previously scattered across TeamDetailView,
 * PlayerSearchPanel, LeagueScreenContext, and LeagueHomeView.
 */
public final class PlayerSearch {

    private PlayerSearch() {}

    /**
     * Find a player by name within a single team.
     */
    public static Player findByName(Team team, String name) {
        if (team == null || name == null) return null;
        for (Player p : team.getAllPlayers()) {
            if (name.equals(p.name)) return p;
        }
        return null;
    }

    /**
     * Find a player by name and team name across the league.
     * First tries the named team, then falls back to all teams (player may have transferred).
     */
    public static Player findInLeague(League league, String name, String teamName) {
        if (name == null) return null;
        // Try the specific team first
        for (Team t : league.getTeamList()) {
            if (t.getName().equals(teamName)) {
                Player p = findByName(t, name);
                if (p != null) return p;
            }
        }
        // Fall back to searching all teams
        for (Team t : league.getTeamList()) {
            Player p = findByName(t, name);
            if (p != null) return p;
        }
        return null;
    }

    /**
     * Find a player by name and team name using a live team map.
     */
    public static Player findInLeague(Map<String, Team> liveTeamMap, String name, String teamName) {
        if (name == null) return null;
        Team t = liveTeamMap.get(teamName);
        if (t != null) {
            Player p = findByName(t, name);
            if (p != null) return p;
        }
        for (Team team : liveTeamMap.values()) {
            Player p = findByName(team, name);
            if (p != null) return p;
        }
        return null;
    }
}
