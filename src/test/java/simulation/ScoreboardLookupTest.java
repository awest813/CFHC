package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** Tapping a scoreboard line must open the box score of exactly that game. */
public class ScoreboardLookupTest {

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
                false);
        league.setPlatformResourceProvider(resources);
    }

    @Test
    public void everyPlayedLineResolvesToTheGameItDescribes() {
        league.currentWeek = 1; // in-season step; playWeek() advances currentWeek
        league.playWeek();
        league.playWeek();

        int resolved = 0;
        List<List<String>> weeks = league.getWeeklyScores();
        for (int w = 0; w < weeks.size(); w++) {
            for (String line : weeks.get(w)) {
                if (!line.contains("\n")) continue;
                Game g = ScoreboardLookup.findPlayedGame(league, w, line);
                if (g == null) continue; // scheduled (unplayed) line
                assertTrue(g.hasPlayed);
                String body = line.substring(line.indexOf('>') + 1);
                String[] sides = body.split("\n");
                assertEquals(g.awayTeam.getName(), ScoreboardLookup.teamName(sides[0]));
                assertEquals(g.homeTeam.getName(), ScoreboardLookup.teamName(sides[1]));
                assertTrue("away score in line", sides[0].trim().endsWith(" " + g.awayScore));
                assertTrue("home score in line", sides[1].trim().endsWith(" " + g.homeScore));
                resolved++;
            }
        }
        assertTrue("expected played games on the scoreboard", resolved > 20);
    }

    @Test
    public void teamName_stripsRankAndScore() {
        assertEquals("Texas A&M", ScoreboardLookup.teamName("#12 Texas A&M 35"));
        assertEquals("Ohio State", ScoreboardLookup.teamName(" #1 Ohio State 0 "));
    }

    @Test
    public void nonGameLinesResolveToNull() {
        assertNull(ScoreboardLookup.findPlayedGame(league, 0, "Scores:>No games this week."));
        assertNull(ScoreboardLookup.findPlayedGame(league, 1, null));
        assertNull(ScoreboardLookup.findPlayedGame(league, 1, "Bowl>#1 Nowhere U 3\n#2 Elsewhere 7"));
    }
}
