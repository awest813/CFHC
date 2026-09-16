package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Phase 3 pageantry (see docs/game-flow-audit-and-premium-plan.md): rivalry
 * generation + trophies, the rivalry schedule guarantee, senior day and
 * homecoming tagging, and persistence of the rivalry bookkeeping.
 */
public class PageantryTest {

    private League league;
    private Team userTeam;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0x3A6E7L);
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
    }

    private boolean pageantryTeam(Team t) {
        return !t.getConference().contains("FCS") && !t.getConference().equals("BYE");
    }

    @Test
    public void rivalries_generatedForEveryTeam_andMutual() {
        int checked = 0;
        int unpaired = 0;
        for (Team t : league.getTeamList()) {
            if (!pageantryTeam(t)) continue;
            checked++;
            if (t.getRivalName().isEmpty()) {
                // An odd number of teams leaves at most one without a rival.
                unpaired++;
                continue;
            }
            Team rival = league.findTeam(t.getRivalName());
            assertNotNull("Rival " + t.getRivalName() + " should exist in the league", rival);
            assertEquals("Rivalry must be mutual", t.getName(), rival.getRivalName());
            assertFalse("Trophy name should be set", t.getRivalryTrophyName().isEmpty());
            assertEquals("Both sides share the trophy name",
                    t.getRivalryTrophyName(), rival.getRivalryTrophyName());
        }
        assertTrue("Sanity: expected a full league of teams", checked > 50);
        assertTrue("At most one team may sit out rivalry season (odd count), got " + unpaired,
                unpaired <= 1);
    }

    @Test
    public void seniorDayAndHomecoming_taggedOnceEach() {
        for (Team t : league.getTeamList()) {
            if (!pageantryTeam(t)) continue;
            int seniorDays = 0;
            int homecomings = 0;
            int seniorDayIndex = -1;
            int lastHomeIndex = -1;
            for (int i = 0; i < t.getGameSchedule().size(); i++) {
                Game g = t.getGameSchedule().get(i);
                if (g.isByeWeek()) continue;
                if (g.homeTeam == t) {
                    if (g.seniorDay) {
                        seniorDays++;
                        seniorDayIndex = i;
                    }
                    if (g.homecomingGame) homecomings++;
                    lastHomeIndex = i;
                }
            }
            assertEquals("Team " + t.getName() + " should have exactly one senior day", 1, seniorDays);
            assertEquals("Team " + t.getName() + " should have exactly one homecoming", 1, homecomings);
            assertTrue("Senior day must be the final home game of " + t.getName(),
                    seniorDayIndex == lastHomeIndex);
        }
    }

    @Test
    public void rivalryGames_scheduledAndTrophyProcessed() {
        // Count guaranteed rivalry matchups across the league.
        Set<Game> rivalryGames = new HashSet<>();
        for (Team t : league.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g != null && g.rivalryGame) rivalryGames.add(g);
            }
        }
        assertTrue("Expected rivalry matchups on the schedule, got " + rivalryGames.size(),
                rivalryGames.size() >= 20);

        // Play one rivalry game and verify trophy bookkeeping + news.
        Game rivalry = rivalryGames.iterator().next();
        Team home = rivalry.homeTeam;
        Team away = rivalry.awayTeam;
        rivalry.playGame();

        boolean rivalryNews = false;
        for (int w = 0; w < league.newsStories.size() && !rivalryNews; w++) {
            for (String story : league.newsStories.get(w)) {
                if (story.startsWith("RIVALRY WEEK>")) {
                    rivalryNews = true;
                    break;
                }
            }
        }
        assertTrue("Rivalry result should become league news", rivalryNews);

        if (rivalry.homeScore != rivalry.awayScore) {
            Team winner = rivalry.homeScore > rivalry.awayScore ? home : away;
            Team loser = winner == home ? away : home;
            assertTrue("Winner should hold the trophy", winner.holdsRivalryTrophy());
            assertFalse("Loser should not hold the trophy", loser.holdsRivalryTrophy());
            assertEquals("Winner's rivalry win count", 1, winner.getRivalryWins());
        }
    }

    @Test
    public void rivalryFields_surviveSaveLoadRoundTrip() throws Exception {
        userTeam.setRivalName("Test State");
        userTeam.setRivalryTrophyName("The Test Axe (TST-ABC)");
        userTeam.setRivalryWins(7);
        userTeam.setHoldsRivalryTrophy(true);

        File tmp = File.createTempFile("cfhc-rivalry-roundtrip", ".cfb");
        try {
            assertTrue(league.saveLeague(tmp));
            League loaded = new League(tmp,
                    new FileSystemResourceProvider(System.getProperty("user.dir"))
                            .getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    new FileSystemResourceProvider(System.getProperty("user.dir"))
                            .getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP,
                    true);
            loaded.rebuildScheduleIfNeeded();
            Team loadedUser = loaded.getTeamList().get(0);
            assertEquals("Test State", loadedUser.getRivalName());
            assertEquals("The Test Axe (TST-ABC)", loadedUser.getRivalryTrophyName());
            assertEquals(7, loadedUser.getRivalryWins());
            assertTrue(loadedUser.holdsRivalryTrophy());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }
}
