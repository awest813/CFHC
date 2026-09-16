package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 5 part 1 — the interactive gameday engine core (see
 * docs/game-flow-audit-and-premium-plan.md): opt-in coaching checkpoints
 * (pre-game / halftime / crunch time) for the user team's games, with the
 * un-coached path byte-identical to the standard simulation.
 */
public class InteractiveGamedayTest {

    private League league;
    private Team userTeam;
    private Game userHomeGame;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0x0C0ACL);
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
        userTeam = league.getTeamList().get(0);
        league.userTeam = userTeam;
        userTeam.setUserControlled(true);

        // First home game on the user's schedule.
        for (Game g : userTeam.getGameSchedule()) {
            if (!g.isByeWeek() && g.homeTeam == userTeam) {
                userHomeGame = g;
                break;
            }
        }
        assertNotNull("User should have a home game", userHomeGame);
    }

    /** Records every checkpoint and returns scripted plans. */
    private static class ScriptedCoach implements GameCoachListener {
        final List<Checkpoint> calls = new ArrayList<>();
        GameCoachPlan pregame;
        GameCoachPlan halftime;
        GameCoachPlan crunch;

        @Override
        public GameCoachPlan decide(Game game, Checkpoint checkpoint) {
            calls.add(checkpoint);
            switch (checkpoint) {
                case PREGAME: return pregame;
                case HALFTIME: return halftime;
                default: return crunch;
            }
        }

        int count(Checkpoint c) {
            return Collections.frequency(calls, c);
        }
    }

    @Test
    public void noListener_gamesRunClean_andUncoached() {
        userHomeGame.playGame();
        assertTrue(userHomeGame.hasPlayed);
        assertFalse("Un-coached games must not log coaching lines",
                userHomeGame.getPlayByPlayLog().contains("COACHING:"));
    }

    @Test
    public void pregamePlan_appliesScheme_andRestoresSeasonBooks() {
        int seasonOff = userTeam.getPlaybookOffNum();
        int seasonDef = userTeam.getPlaybookDefNum();

        ScriptedCoach coach = new ScriptedCoach();
        coach.pregame = new GameCoachPlan();
        coach.pregame.offScheme = 1;
        coach.pregame.defScheme = 1;
        league.setGameCoachListener(coach);
        try {
            userHomeGame.playGame();

            assertTrue("Pregame checkpoint should fire exactly once", coach.count(GameCoachListener.Checkpoint.PREGAME) == 1);
            assertTrue("Coached games should log the coaching notes",
                    userHomeGame.getPlayByPlayLog().contains("COACHING:"));
            assertEquals("Season offensive playbook must be restored after the game",
                    seasonOff, userTeam.getPlaybookOffNum());
            assertEquals("Season defensive playbook must be restored after the game",
                    seasonDef, userTeam.getPlaybookDefNum());
        } finally {
            league.setGameCoachListener(null);
        }
    }

    @Test
    public void halftimeAndCrunch_checkpointsFireOnceEach() {
        ScriptedCoach coach = new ScriptedCoach();
        coach.pregame = new GameCoachPlan();
        coach.halftime = new GameCoachPlan();
        coach.halftime.halftimeFocus = GameCoachPlan.HalftimeFocus.AGGRESSIVE;
        coach.crunch = new GameCoachPlan();
        coach.crunch.timeoutsToBurn = 2;
        coach.crunch.crunchFourthDownCall = GameCoachPlan.FourthDownCall.GO_FOR_IT;
        league.setGameCoachListener(coach);
        try {
            userHomeGame.playGame();

            assertEquals("Halftime should fire exactly once", 1, coach.count(GameCoachListener.Checkpoint.HALFTIME));
            assertEquals("Crunch time should fire exactly once (start of Q4)", 1, coach.count(GameCoachListener.Checkpoint.CRUNCH_TIME));

            String pbp = userHomeGame.getPlayByPlayLog();
            assertTrue("Aggressive halftime focus should be narrated",
                    pbp.contains("challenge their squad"));
            assertTrue("Timeout burn should be narrated", pbp.contains("burns 2 timeouts"));
            assertTrue("Fourth-down intent should be narrated",
                    pbp.contains("fourth-down intent: go for it"));
            assertTrue("Burned timeouts must be deducted",
                    userTeam == userHomeGame.homeTeam ? userHomeGame.homeTimeouts <= 1 : userHomeGame.awayTimeouts <= 1);
        } finally {
            league.setGameCoachListener(null);
        }
    }

    @Test
    public void cpuGames_neverCoached() {
        ScriptedCoach coach = new ScriptedCoach();
        league.setGameCoachListener(coach);
        try {
            Game cpuGame = null;
            for (Team t : league.getTeamList()) {
                if (t.isUserControlled()) continue;
                for (Game g : t.getGameSchedule()) {
                    if (g == null || g.isByeWeek() || g.hasPlayed) continue;
                    if (!g.homeTeam.isUserControlled() && !g.awayTeam.isUserControlled()) {
                        cpuGame = g;
                        break;
                    }
                }
                if (cpuGame != null) break;
            }
            assertNotNull(cpuGame);
            cpuGame.playGame();
            assertEquals("CPU-vs-CPU games must never hit the coach hook", 0, coach.calls.size());
        } finally {
            league.setGameCoachListener(null);
        }
    }

    @Test
    public void coachedSeason_replaysDeterministically() {
        long seed = 0xC04A1L;
        String seasonA = playCoachedYear(newSeededLeague(seed, true), seed);
        String seasonB = playCoachedYear(newSeededLeague(seed, true), seed);
        assertEquals("Identical seeds + identical coaching must replay identically", seasonA, seasonB);
    }

    private League newSeededLeague(long seed, boolean coached) {
        SimRandom.pinNextSeed(seed);
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        League l = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        l.setPlatformResourceProvider(resources);
        l.userTeam = l.getTeamList().get(0);
        l.userTeam.setUserControlled(true);
        if (coached) {
            // Deterministic "coach": aggressive all game, conservative at the half.
            l.setGameCoachListener((game, checkpoint) -> {
                GameCoachPlan plan = new GameCoachPlan();
                if (checkpoint == GameCoachListener.Checkpoint.PREGAME) {
                    plan.fourthDownBias = 2;
                } else if (checkpoint == GameCoachListener.Checkpoint.HALFTIME) {
                    plan.halftimeFocus = GameCoachPlan.HalftimeFocus.CONSERVATIVE;
                } else {
                    plan.timeoutsToBurn = 1;
                }
                return plan;
            });
        }
        return l;
    }

    private String playCoachedYear(League l, long seed) {
        SeasonController controller = new SeasonController(l, GameUiBridge.NO_OP);
        for (int steps = 0; steps < 40; steps++) {
            controller.advanceWeek();
            if (SeasonFlowOrder.isRecruitingGate(l.currentWeek, l.regSeasonWeeks)
                    && l.recruitingPhaseActive) {
                break;
            }
        }
        StringBuilder digest = new StringBuilder();
        List<String> rows = new ArrayList<>();
        for (Team t : l.getTeamList()) {
            rows.add(String.format("%s|%d-%d|%d", t.getName(), t.getWins(), t.getLosses(), t.getTeamPoints()));
        }
        Collections.sort(rows);
        digest.append(String.join("\n", rows));
        assertTrue(controller.autoCompleteRecruiting());
        assertEquals(seed, l.getRngSeed());
        return digest.toString();
    }
}
