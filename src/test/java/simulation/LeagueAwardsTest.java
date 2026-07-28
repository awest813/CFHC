package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.Player;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards LeagueAwards candidate-builder extraction from League.
 */
public class LeagueAwardsTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
    }

    @Test
    public void heismanAndDefPoty_candidatesNonEmptyAndDelegated() {
        ArrayList<Player> heisman = LeagueAwards.getHeismanCandidates(league);
        ArrayList<Player> def = LeagueAwards.getDefensivePotyCandidates(league);
        assertFalse(heisman.isEmpty());
        assertFalse(def.isEmpty());
        assertTrue(league.getHeisman() == heisman
                || league.getHeisman().size() == heisman.size());
        assertTrue(league.getDefPOTY().size() == def.size());
    }
}
