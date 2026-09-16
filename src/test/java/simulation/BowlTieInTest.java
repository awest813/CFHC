package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 3 bowl tie-ins: the marquee (first-tier) bowls must be seeded with
 * conference champion pairings from the highest-prestige conferences before
 * the at-large snake fill runs.
 */
public class BowlTieInTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0xB07B1L);
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
    public void marqueeBowls_honorConferenceTieIns() {
        // Play the full regular season through conference-championship week;
        // bowl/playoff selection runs at the end of that week.
        league.currentWeek = 1;
        for (int w = 1; w <= league.regSeasonWeeks - 1; w++) {
            league.playWeek();
        }
        assertTrue("Bowls should be scheduled after the CCG week", league.hasScheduledBowls);

        List<Game> tierOne = new ArrayList<>();
        for (int g = 0; g < BowlManager.TIER1_CUTOFF && g < league.bowlGames.length; g++) {
            if (league.bowlGames[g] != null) tierOne.add(league.bowlGames[g]);
        }
        assertTrue("Marquee bowl slots should be filled", tierOne.size() >= BowlManager.TIER1_CUTOFF);

        int championPairings = 0;
        for (Game g : tierOne) {
            if ("CC".equals(g.homeTeam.getConfChampion()) && "CC".equals(g.awayTeam.getConfChampion())) {
                championPairings++;
            }
        }
        // Champions are only tie-in eligible when bowl-qualified (6+ wins); the
        // rest of the marquee slots go to those conferences' best eligible teams.
        assertTrue("At least one marquee bowl should pair conference champions (got "
                + championPairings + " of " + tierOne.size() + ")",
                championPairings >= 1);

        boolean bidNews = false;
        for (int w = 0; w < league.newsStories.size() && !bidNews; w++) {
            for (String story : league.newsStories.get(w)) {
                if (story.startsWith("Bowl Bids>")) {
                    bidNews = true;
                    break;
                }
            }
        }
        assertTrue("Tie-in teams should accept their bids in the news", bidNews);
    }
}
