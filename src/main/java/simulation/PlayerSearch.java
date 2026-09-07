package simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import positions.Player;

/**
 * Shared utility for finding and querying players by name or criteria within a team or across the league.
 * Used by desktop and mobile UI components (PlayerSearchPanel, PlayerSearchDialogController, TeamDetailView).
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
        if (name == null || league == null) return null;
        // Try the specific team first (by name or abbreviation)
        if (teamName != null && !teamName.isEmpty()) {
            for (Team t : league.getTeamList()) {
                if (t.getName().equalsIgnoreCase(teamName)
                        || (t.getAbbr() != null && t.getAbbr().equalsIgnoreCase(teamName))) {
                    Player p = findByName(t, name);
                    if (p != null) return p;
                }
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
        if (name == null || liveTeamMap == null) return null;
        if (teamName != null) {
            Team t = liveTeamMap.get(teamName);
            if (t == null) {
                for (Team candidate : liveTeamMap.values()) {
                    if (candidate.getAbbr() != null && candidate.getAbbr().equalsIgnoreCase(teamName)) {
                        t = candidate;
                        break;
                    }
                }
            }
            if (t != null) {
                Player p = findByName(t, name);
                if (p != null) return p;
            }
        }
        for (Team team : liveTeamMap.values()) {
            Player p = findByName(team, name);
            if (p != null) return p;
        }
        return null;
    }

    /**
     * Search players across the league by query string, position, year/class, team, and minimum overall rating.
     *
     * @param league active league
     * @param query player name substring (case-insensitive)
     * @param posFilter position filter ("ALL" or "QB", "RB", etc.)
     * @param yearFilter year filter (1=FR, 2=SO, 3=JR, 4=SR, or <= 0 for ALL)
     * @param teamFilter team filter ("ALL" or team name/abbr)
     * @param minOvr minimum overall rating (e.g. 0, 70, 75, 80, 85, 90)
     * @return list of matching players sorted by rating descending
     */
    public static List<Player> search(League league, String query, String posFilter, int yearFilter, String teamFilter, int minOvr) {
        List<Player> results = new ArrayList<>();
        if (league == null) return results;

        String normQuery = query != null ? query.toLowerCase(Locale.ROOT).trim() : "";
        boolean filterPos = posFilter != null && !posFilter.equalsIgnoreCase("ALL") && !posFilter.equalsIgnoreCase("All Positions");
        boolean filterTeam = teamFilter != null && !teamFilter.equalsIgnoreCase("ALL") && !teamFilter.equalsIgnoreCase("All Teams");

        for (Team t : league.getTeamList()) {
            if (filterTeam) {
                if (!t.getName().equalsIgnoreCase(teamFilter) && (t.getAbbr() == null || !t.getAbbr().equalsIgnoreCase(teamFilter))) {
                    continue;
                }
            }
            for (Player p : t.getAllPlayers()) {
                if (!normQuery.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(normQuery)) {
                    continue;
                }
                if (filterPos && !p.position.equalsIgnoreCase(posFilter)) {
                    continue;
                }
                if (yearFilter > 0 && p.year != yearFilter) {
                    continue;
                }
                if (minOvr > 0 && p.ratOvr < minOvr) {
                    continue;
                }
                results.add(p);
            }
        }

        // Sort by overall rating descending, then name ascending
        results.sort((a, b) -> {
            int cmp = Integer.compare(b.ratOvr, a.ratOvr);
            if (cmp != 0) return cmp;
            return a.name.compareToIgnoreCase(b.name);
        });

        return results;
    }
}
