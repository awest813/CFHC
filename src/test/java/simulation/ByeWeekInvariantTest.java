package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * BYE-week schedule / prestige invariants. Covers the cleanup-audit season-loop
 * edge case and the {@code "BYE"} vs {@code "BYE WEEK"} prestige counting bug.
 */
public class ByeWeekInvariantTest {

    private League league;

    @Before
    public void setUp() {
        DesktopResourceProvider resources =
                new DesktopResourceProvider(System.getProperty("user.dir"));
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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void byeWeek_gameHelperRecognizesCanonicalName() {
        Team home = league.getTeamList().get(0);
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
        Game g = new Game(home, bye, Game.BYE_WEEK_NAME);
        assertTrue(g.isByeWeek());
        assertTrue(g.isRegularSeasonSlot());

        Game conf = new Game(home, league.getTeamList().get(1), "Conference");
        assertFalse(conf.isByeWeek());
        assertTrue(conf.isRegularSeasonSlot());

        Game bowl = new Game(home, league.getTeamList().get(1), "Rose Bowl");
        assertFalse(bowl.isByeWeek());
        assertFalse(bowl.isRegularSeasonSlot());
    }

    @Test
    public void schedules_mayContainByeWeeks_withoutCountingAsWins() {
        List<Team> withByes = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g.isByeWeek()) {
                    withByes.add(t);
                    break;
                }
            }
        }
        // Odd-sized conferences / padding can introduce byes; if none exist in the
        // default universe, still assert the play path is safe via a synthetic bye.
        if (withByes.isEmpty()) {
            Team t = league.userTeam;
            Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
            t.addGameToSchedule(new Game(t, bye, Game.BYE_WEEK_NAME));
            withByes.add(t);
        }

        for (Team t : withByes) {
            int winsBefore = t.wins;
            int lossesBefore = t.losses;
            for (Game g : t.getGameSchedule()) {
                if (g.isByeWeek() && !g.hasPlayed) {
                    g.playGame();
                }
            }
            assertEquals("BYE WEEK must not add a win for " + t.name, winsBefore, t.wins);
            assertEquals("BYE WEEK must not add a loss for " + t.name, lossesBefore, t.losses);
        }
    }

    @Test
    public void calcSeasonPrestige_byeWeekDoesNotCountAsPostseasonSoftener() {
        Team t = league.userTeam;
        t.gameSchedule.clear();
        Team opp = league.getTeamList().get(1);
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);

        // Regular season only + a bye. Under the old bug ("BYE" vs "BYE WEEK"),
        // the bye was counted as a postseason game and softened a negative delta.
        t.addGameToSchedule(new Game(t, opp, "Conference"));
        t.addGameToSchedule(new Game(t, bye, Game.BYE_WEEK_NAME));

        t.bowlBan = false;
        t.penalized = false;
        t.disciplinePts = 0;
        t.natChampWL = "";
        t.semiFinalWL = "";
        t.confChampion = "";
        t.teamPrestige = 70;
        t.projectedPollRank = 20;
        t.rankTeamPollScore = 80; // badly missed projection → negative prestigeChange
        t.wins = 4;
        t.projectedWins = 8;
        t.rankTeamPrestige = 40;

        int[] withBye = t.calcSeasonPrestige();

        t.gameSchedule.clear();
        t.addGameToSchedule(new Game(t, opp, "Conference"));
        int[] withoutBye = t.calcSeasonPrestige();

        assertEquals(
                "BYE WEEK must not change prestige vs conference-only schedule",
                withoutBye[1], withBye[1]);
    }

    @Test
    public void getSosAndRpi_skipByeWeekSlots() {
        Team t = league.userTeam;
        t.gameSchedule.clear();
        Team opp = league.getTeamList().get(1);
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);

        Game played = new Game(t, opp, "Conference");
        played.hasPlayed = true;
        played.homeScore = 21;
        played.awayScore = 14;
        // Ensure t is home
        assertSame(t, played.homeTeam);
        t.addGameToSchedule(played);
        t.addGameToSchedule(new Game(t, bye, Game.BYE_WEEK_NAME));
        t.wins = 1;
        t.losses = 0;

        league.currentWeek = 8;
        float sos = t.statsTracker.getSOSPollScore();
        float rpi = t.statsTracker.getRPI();
        // Should be finite and not throw; bye must not be treated as a scored game.
        assertFalse(Float.isNaN(sos));
        assertFalse(Float.isNaN(rpi));
    }
}
