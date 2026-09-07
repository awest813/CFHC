package desktop;

import java.util.List;
import java.util.Map;
import simulation.League;
import simulation.Team;
import positions.Player;

/**
 * Shared utility for finding players by name within a team or across the league.
 * Delegates to {@link simulation.PlayerSearch} for shared engine and Android parity.
 */
public final class PlayerSearch {

    private PlayerSearch() {}

    /**
     * Find a player by name within a single team.
     */
    public static Player findByName(Team team, String name) {
        return simulation.PlayerSearch.findByName(team, name);
    }

    /**
     * Find a player by name and team name across the league.
     */
    public static Player findInLeague(League league, String name, String teamName) {
        return simulation.PlayerSearch.findInLeague(league, name, teamName);
    }

    /**
     * Find a player by name and team name using a live team map.
     */
    public static Player findInLeague(Map<String, Team> liveTeamMap, String name, String teamName) {
        return simulation.PlayerSearch.findInLeague(liveTeamMap, name, teamName);
    }

    /**
     * Search players across the league with multi-criteria filters.
     */
    public static List<Player> search(League league, String query, String posFilter, int yearFilter, String teamFilter, int minOvr) {
        return simulation.PlayerSearch.search(league, query, posFilter, yearFilter, teamFilter, minOvr);
    }
}
