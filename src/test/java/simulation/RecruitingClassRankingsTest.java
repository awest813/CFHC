package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression tests for signing-class strength index and rankings list CSV shape. */
public class RecruitingClassRankingsTest {

    private League league;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
        league.currentWeek = 0;
    }

    @Test
    public void recruitingClassRankings_eachRowHasExactlyThreeCommaDelimitedFields() {
        ArrayList<String> rows = league.getTeamRankingsStr(17);
        assertFalse(rows.isEmpty());
        for (String row : rows) {
            String[] parts = row.split(",", 3);
            assertEquals("Expected rank,name,detail for row: " + row, 3, parts.length);
        }
    }

    @Test
    public void recruitingClassRating_isNonNegativeForAnyTeam() {
        for (Team t : league.getTeamList()) {
            float r = t.getRecruitingClassRat();
            assertTrue(r >= 0f);
            assertTrue(Float.isFinite(r));
        }
    }
}
