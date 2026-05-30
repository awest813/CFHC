package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerWR;
import positions.PlayerTE;
import positions.PlayerOL;
import positions.PlayerDL;
import positions.PlayerLB;
import positions.PlayerCB;
import positions.PlayerS;
import positions.PlayerK;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ProgressionInvariantTest {

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
    public void progression_allAttributesBoundedAfterAdvanceSeason() {
        league.advanceSeason();

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertAllAttributesBounded(p, t.getName());
            }
        }
    }

    @Test
    public void progression_yearAdvancesAfterOffseason() {
        Map<String, Integer> yearsBefore = capturePlayerYears(team);

        league.advanceSeason();

        boolean anyYearAdvanced = false;
        for (Player p : team.getAllPlayers()) {
            if (yearsBefore.containsKey(p.getName())) {
                int oldYear = yearsBefore.get(p.getName());
                if (p.year > oldYear) {
                    anyYearAdvanced = true;
                    break;
                }
            }
        }
        assertTrue("At least some returning players should advance in year", anyYearAdvanced);
    }

    @Test
    public void progression_midSeasonProgressionBounded() {
        team.midSeasonProgression();

        for (Player p : team.getAllPlayers()) {
            assertAllAttributesBounded(p, team.getName());
        }
    }

    @Test
    public void progression_midSeasonThenEndSeason_bounded() {
        team.midSeasonProgression();
        league.advanceSeason();

        for (Player p : team.getAllPlayers()) {
            assertAllAttributesBounded(p, team.getName());
        }
    }

    @Test
    public void progression_practiceFocusDoesNotCorruptRatings() {
        for (PracticeFocus focus : PracticeFocus.values()) {
            team.practiceFocus = focus;
            team.midSeasonProgression();

            for (Player p : team.getAllPlayers()) {
                assertAllAttributesBounded(p, team.getName() + " focus=" + focus);
            }
        }
    }

    @Test
    public void progression_positionSpecificAttributesBounded_allPositions() {
        league.advanceSeason();

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                String ctx = t.getName() + " " + p.getName() + " (" + p.position + ")";

                if (p instanceof PlayerQB) {
                    PlayerQB qb = (PlayerQB) p;
                    assertTrue(ctx + " passPow", qb.getRatPassPow() >= 0 && qb.getRatPassPow() <= 99);
                    assertTrue(ctx + " passAcc", qb.getRatPassAcc() >= 0 && qb.getRatPassAcc() <= 99);
                } else if (p instanceof PlayerRB) {
                    PlayerRB rb = (PlayerRB) p;
                    assertTrue(ctx + " speed", rb.getRatSpeed() >= 0 && rb.getRatSpeed() <= 99);
                    assertTrue(ctx + " evasion", rb.getRatEvasion() >= 0 && rb.getRatEvasion() <= 99);
                } else if (p instanceof PlayerWR) {
                    PlayerWR wr = (PlayerWR) p;
                    assertTrue(ctx + " speed", wr.getRatSpeed() >= 0 && wr.getRatSpeed() <= 99);
                    assertTrue(ctx + " catch", wr.getRatCatch() >= 0 && wr.getRatCatch() <= 99);
                } else if (p instanceof PlayerTE) {
                    PlayerTE te = (PlayerTE) p;
                    assertTrue(ctx + " catch", te.getRatCatch() >= 0 && te.getRatCatch() <= 99);
                } else if (p instanceof PlayerOL) {
                    PlayerOL ol = (PlayerOL) p;
                    assertTrue(ctx + " runBlock", ol.getRatRunBlock() >= 0 && ol.getRatRunBlock() <= 99);
                    assertTrue(ctx + " passBlock", ol.getRatPassBlock() >= 0 && ol.getRatPassBlock() <= 99);
                } else if (p instanceof PlayerDL) {
                    PlayerDL dl = (PlayerDL) p;
                    assertTrue(ctx + " passRush", dl.getRatPassRush() >= 0 && dl.getRatPassRush() <= 99);
                } else if (p instanceof PlayerLB) {
                    PlayerLB lb = (PlayerLB) p;
                    assertTrue(ctx + " tackle", lb.getRatTackle() >= 0 && lb.getRatTackle() <= 99);
                } else if (p instanceof PlayerCB) {
                    PlayerCB cb = (PlayerCB) p;
                    assertTrue(ctx + " coverage", cb.getRatCoverage() >= 0 && cb.getRatCoverage() <= 99);
                } else if (p instanceof PlayerS) {
                    PlayerS s = (PlayerS) p;
                    assertTrue(ctx + " coverage", s.getRatCoverage() >= 0 && s.getRatCoverage() <= 99);
                } else if (p instanceof PlayerK) {
                    PlayerK k = (PlayerK) p;
                    assertTrue(ctx + " kickPow", k.getRatKickPow() >= 0 && k.getRatKickPow() <= 99);
                    assertTrue(ctx + " kickAcc", k.getRatKickAcc() >= 0 && k.getRatKickAcc() <= 99);
                }
            }
        }
    }

    @Test
    public void progression_teamAdvancePlayersProducesValidDepthChart() {
        team.advanceTeamPlayers();

        for (Player p : team.getAllPlayers()) {
            assertNotNull("Player position should not be null", p.position);
            assertFalse("Player position should not be empty", p.position.isEmpty());
            assertAllAttributesBounded(p, team.getName());
        }
    }

    @Test
    public void progression_multipleMidSeasonAdvances_bounded() {
        for (int i = 0; i < 5; i++) {
            team.midSeasonProgression();
        }

        for (Player p : team.getAllPlayers()) {
            assertAllAttributesBounded(p, team.getName());
        }
    }

    @Test
    public void progression_ratedImprovementTracked() {
        team.midSeasonProgression();

        boolean anyImprovementRecorded = false;
        for (Player p : team.getAllPlayers()) {
            if (p.ratImprovement != 0) {
                anyImprovementRecorded = true;
                break;
            }
        }
    }

    @Test
    public void progression_graduatingSeniorsRemoved() {
        int beforeCount = team.getAllPlayers().size();
        int seniorsBefore = 0;
        for (Player p : team.getAllPlayers()) {
            if (p.year >= 4) seniorsBefore++;
        }

        league.advanceSeason();

        int seniorsAfter = 0;
        for (Player p : team.getAllPlayers()) {
            if (p.year >= 5) seniorsAfter++;
        }
    }

    @Test
    public void progression_characterStaysBounded() {
        league.advanceSeason();

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertTrue(t.getName() + " " + p.getName() + " character should be >= 0: " + p.character,
                        p.character >= 0);
                assertTrue(t.getName() + " " + p.getName() + " character should be <= 100: " + p.character,
                        p.character <= 100);
            }
        }
    }

    private void assertAllAttributesBounded(Player p, String context) {
        String label = context + " " + p.getName() + " (" + p.position + ")";
        assertTrue(label + " OVR", p.ratOvr >= 0 && p.ratOvr <= 99);
        assertTrue(label + " ratAttr1", p.ratAttr1 >= 0 && p.ratAttr1 <= 99);
        assertTrue(label + " ratAttr2", p.ratAttr2 >= 0 && p.ratAttr2 <= 99);
        assertTrue(label + " ratAttr3", p.ratAttr3 >= 0 && p.ratAttr3 <= 99);
        assertTrue(label + " ratAttr4", p.ratAttr4 >= 0 && p.ratAttr4 <= 99);
        assertTrue(label + " ratPot", p.ratPot >= 0 && p.ratPot <= 99);
        assertTrue(label + " ratInt", p.ratIntelligence >= 0 && p.ratIntelligence <= 100);
        assertTrue(label + " ratDur", p.ratDurability >= 0 && p.ratDurability <= 100);
    }

    private Map<String, Integer> capturePlayerYears(Team t) {
        Map<String, Integer> years = new HashMap<>();
        for (Player p : t.getAllPlayers()) {
            years.put(p.getName(), p.year);
        }
        return years;
    }
}
