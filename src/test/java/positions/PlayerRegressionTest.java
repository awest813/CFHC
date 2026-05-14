package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerRegressionTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void ratIntelligence_canIncreaseFromSeasonProgression() {
        PlayerQB player = new PlayerQB("Test QB IQ", 1, 3, team);
        int initial = player.ratIntelligence;
        player.ratOvr = 60;
        player.ratPot = 85;
        player.progression = 80;

        boolean increased = false;
        for (int i = 0; i < 50; i++) {
            player.ratIntelligence = initial;
            player.genericAdvanceSeason();
            if (player.ratIntelligence > initial) {
                increased = true;
                break;
            }
        }
        assertTrue("ratIntelligence should be able to increase after advanceSeason", increased);
    }

    @Test
    public void ratAttr4_canIncreaseFromSeasonProgression() {
        PlayerQB player = new PlayerQB("Test QB Attr4", 1, 3, team);
        int initial = player.ratAttr4;
        player.ratOvr = 60;
        player.ratPot = 85;
        player.progression = 80;

        boolean increased = false;
        for (int i = 0; i < 50; i++) {
            player.ratAttr4 = initial;
            player.genericAdvanceSeason();
            if (player.ratAttr4 > initial) {
                increased = true;
                break;
            }
        }
        assertTrue("ratAttr4 should be able to increase after advanceSeason", increased);
    }

    @Test
    public void getInfoLineupInjury_handlesNullHeadCoach() {
        PlayerQB player = new PlayerQB("Test QB HC Null", 2, 4, team);
        team.HC = null;
        String info = player.getInfoLineupInjury();
        assertNotNull("getInfoLineupInjury should not crash with null HC", info);
        assertFalse("getInfoLineupInjury should return non-empty string", info.isEmpty());
        assertTrue("Should include Ovr rating", info.contains("Ovr:"));
    }

    @Test
    public void getInfoLineupInjury_worksWithValidHeadCoach() {
        PlayerQB player = new PlayerQB("Test QB Valid HC", 2, 4, team);
        assertNotNull(team.HC);
        String info = player.getInfoLineupInjury();
        assertNotNull(info);
        assertTrue(info.contains("Ovr:"));
        assertTrue(info.contains("Pot:"));
    }
}
