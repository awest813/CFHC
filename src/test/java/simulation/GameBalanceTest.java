package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 2 balance spot-check: with weather, penalties, momentum, and the rest
 * of the game-day environment enabled, full seeded seasons must stay in a
 * plausible scoring band and keep penalty rates realistic.
 */
public class GameBalanceTest {

    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    }

    private League newSeededLeague(long seed) {
        SimRandom.pinNextSeed(seed);
        League league = new League(
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
        return league;
    }

    private double[] playRegularSeason(League league) {
        league.currentWeek = 1;
        for (int w = 1; w < league.regSeasonWeeks; w++) {
            league.playWeek();
            league.currentWeek = Math.min(league.currentWeek + 1, league.regSeasonWeeks - 1);
        }

        long totalPoints = 0;
        long totalGames = 0;
        long totalFlags = 0;
        List<Game> seen = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g == null || !g.hasPlayed || g.isByeWeek() || seen.contains(g)) {
                    continue;
                }
                seen.add(g);
                totalPoints += g.homeScore + g.awayScore;
                totalFlags += g.homePenalties + g.awayPenalties;
                totalGames++;
            }
        }
        assertTrue("Season should produce games", totalGames > 100);
        return new double[]{
                (double) totalPoints / totalGames,   // combined scoring per game
                (double) totalFlags / totalGames     // combined flags per game
        };
    }

    @Test
    public void fullSeasons_stayInPlausibleBands() {
        double[][] runs = new double[][]{playRegularSeason(newSeededLeague(0xB41A1L)),
                playRegularSeason(newSeededLeague(0xB41A2L))};

        for (double[] stats : runs) {
            double pointsPerGame = stats[0];
            double flagsPerGame = stats[1];
            assertTrue("Combined scoring out of band: " + pointsPerGame + " pts/game",
                    pointsPerGame >= 30 && pointsPerGame <= 85);
            assertTrue("Penalty rate out of band: " + flagsPerGame + " flags/game",
                    flagsPerGame >= 1 && flagsPerGame <= 25);
        }
        // Two seeds must land in the same ballpark (no runaway behavior change).
        assertTrue("Scoring drift across seeds too wide: "
                        + runs[0][0] + " vs " + runs[1][0],
                Math.abs(runs[0][0] - runs[1][0]) <= runs[0][0] * 0.25);
    }
}
