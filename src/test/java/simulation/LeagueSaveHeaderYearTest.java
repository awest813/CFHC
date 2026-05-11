package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression tests for {@link League#getSeasonStartFromSaveHeader(String)} used when
 * resolving portable saves and legacy headers.
 */
public class LeagueSaveHeaderYearTest {

    private League league;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        DesktopResourceProvider resources = new DesktopResourceProvider(projectRoot);
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
    }

    @Test
    public void nullHeader_returnsLegacyYear() {
        assertEquals(League.LEGACY_SAVE_YEAR, league.getSeasonStartFromSaveHeader(null));
    }

    @Test
    public void blankHeader_returnsLegacyYear() {
        assertEquals(League.LEGACY_SAVE_YEAR, league.getSeasonStartFromSaveHeader(""));
    }

    @Test
    public void headerWithoutColon_returnsLegacyYear() {
        assertEquals(League.LEGACY_SAVE_YEAR, league.getSeasonStartFromSaveHeader("2040"));
    }

    @Test
    public void invalidYearToken_returnsLegacyYear() {
        assertEquals(League.LEGACY_SAVE_YEAR, league.getSeasonStartFromSaveHeader("foo:rest"));
    }

    @Test
    public void standardHeader_withEmptyHistory_returnsAbsoluteYear() {
        assertEquals(2040, league.getSeasonStartFromSaveHeader("2040:payload"));
    }

    @Test
    public void recruitingHeader_withEmptyHistory_offsetsByOne() {
        assertEquals(2041, league.getSeasonStartFromSaveHeader("2040-R:payload"));
    }

    @Test
    public void standardHeader_subtractsHistoryDepth() {
        league.leagueHistory.add(new String[]{"row"});
        assertEquals(2039, league.getSeasonStartFromSaveHeader("2040:payload"));
    }
}
