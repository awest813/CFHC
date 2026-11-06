package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The pre-kickoff gameday plan (Android's interactive coaching): it answers
 * each checkpoint from choices made before the game, branching on whether the
 * user's team leads or trails.
 */
public class GamedayPlanTest {

    private League league;
    private Team userTeam;
    private Game userHomeGame;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0x6A3EL);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false);
        league.setPlatformResourceProvider(resources);
        userTeam = league.getTeamList().get(0);
        league.userTeam = userTeam;
        userTeam.setUserControlled(true);
        for (Game g : userTeam.getGameSchedule()) {
            if (!g.isByeWeek() && g.homeTeam == userTeam) {
                userHomeGame = g;
                break;
            }
        }
        assertNotNull(userHomeGame);
    }

    private static GamedayPlan fullPlan() {
        GamedayPlan p = new GamedayPlan();
        p.fourthDownBias = 1;
        p.halftimeIfTrailing = GameCoachPlan.HalftimeFocus.AGGRESSIVE;
        p.halftimeIfLeading = GameCoachPlan.HalftimeFocus.CONSERVATIVE;
        p.crunchIfTrailing = GameCoachPlan.FourthDownCall.GO_FOR_IT;
        p.crunchIfLeading = GameCoachPlan.FourthDownCall.PUNT;
        return p;
    }

    @Test
    public void halftime_branchesOnUserMargin() {
        GamedayPlan plan = fullPlan();

        userHomeGame.homeScore = 7;
        userHomeGame.awayScore = 14;
        assertEquals(GameCoachPlan.HalftimeFocus.AGGRESSIVE,
                plan.decide(userHomeGame, GameCoachListener.Checkpoint.HALFTIME).halftimeFocus);

        userHomeGame.homeScore = 21;
        assertEquals(GameCoachPlan.HalftimeFocus.CONSERVATIVE,
                plan.decide(userHomeGame, GameCoachListener.Checkpoint.HALFTIME).halftimeFocus);

        userHomeGame.homeScore = 14;
        assertNull("a tie defers to the staff",
                plan.decide(userHomeGame, GameCoachListener.Checkpoint.HALFTIME));
    }

    @Test
    public void margin_isFromTheUsersSide_whenUserIsAway() {
        for (Game g : userTeam.getGameSchedule()) {
            if (!g.isByeWeek() && g.awayTeam == userTeam) {
                g.homeScore = 3;
                g.awayScore = 10; // user leads on the road
                assertEquals(GameCoachPlan.FourthDownCall.PUNT,
                        fullPlan().decide(g, GameCoachListener.Checkpoint.CRUNCH_TIME).crunchFourthDownCall);
                return;
            }
        }
        fail("expected an away game on the schedule");
    }

    @Test
    public void pregame_carriesClampedBias_andEmptyPlanDefers() {
        GamedayPlan plan = new GamedayPlan();
        assertTrue(plan.isEmpty());
        assertNull(plan.decide(userHomeGame, GameCoachListener.Checkpoint.PREGAME));

        plan.fourthDownBias = 9;
        assertEquals(2, plan.decide(userHomeGame, GameCoachListener.Checkpoint.PREGAME).fourthDownBias);
    }

    @Test
    public void encodeDecode_roundTrips_andRejectsGarbage() {
        GamedayPlan plan = fullPlan();
        GamedayPlan back = GamedayPlan.decode(plan.encode());
        assertEquals(plan.encode(), back.encode());

        assertTrue(GamedayPlan.decode(null).isEmpty());
        assertTrue(GamedayPlan.decode("1,NOPE,NONE,AUTO,AUTO").isEmpty());
        assertTrue(GamedayPlan.decode("x").isEmpty());
    }

    @Test
    public void installedPlan_coachesARealGame() {
        league.setGameCoachListener(fullPlan());
        try {
            userHomeGame.playGame();
            String pbp = userHomeGame.getPlayByPlayLog();
            assertTrue("pregame aggression should be narrated",
                    pbp.contains("Fourth-down aggression dialed up"));
        } finally {
            league.setGameCoachListener(null);
        }
    }
}
