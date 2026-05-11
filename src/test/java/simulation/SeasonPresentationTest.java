package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the week/phase chips track the actual game flow:
 * regSeasonWeeks-1 is the CCG week, regSeasonWeeks..+2 are bowl/playoff
 * weeks, and regSeasonWeeks+3 is the National Championship.
 */
public class SeasonPresentationTest {

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
    }

    @Test
    public void weekChip_preseason() {
        league.currentWeek = 0;
        assertEquals("Week 0  Preseason", SeasonPresentation.getSeasonWeekChipText(league));
    }

    @Test
    public void weekChip_regularSeason() {
        league.currentWeek = 5;
        assertEquals("Week 5", SeasonPresentation.getSeasonWeekChipText(league));
    }

    @Test
    public void weekChip_ccgWeek() {
        league.currentWeek = league.regSeasonWeeks - 1;
        assertTrue(SeasonPresentation.getSeasonWeekChipText(league).endsWith("CCG"));
    }

    @Test
    public void weekChip_postseasonBowls() {
        league.currentWeek = league.regSeasonWeeks;
        assertTrue(SeasonPresentation.getSeasonWeekChipText(league).endsWith("Bowls"));
        league.currentWeek = league.regSeasonWeeks + 2;
        assertTrue(SeasonPresentation.getSeasonWeekChipText(league).endsWith("Bowls"));
    }

    @Test
    public void weekChip_ncg() {
        league.currentWeek = league.regSeasonWeeks + 3;
        assertTrue(SeasonPresentation.getSeasonWeekChipText(league).endsWith("NCG"));
    }

    @Test
    public void weekChip_offseason() {
        league.currentWeek = league.regSeasonWeeks + 5;
        assertTrue(SeasonPresentation.getSeasonWeekChipText(league).endsWith("Offseason"));
    }

    @Test
    public void phaseChip_preseason() {
        league.currentWeek = 0;
        assertEquals("Phase  Preseason", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void phaseChip_regularSeason() {
        league.currentWeek = 5;
        assertEquals("Phase  Regular Season", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void phaseChip_championshipWeek() {
        // Conference championships actually play at currentWeek == regSeasonWeeks-1.
        league.currentWeek = league.regSeasonWeeks - 1;
        assertEquals("Phase  Championship Week", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void phaseChip_postseason() {
        league.currentWeek = league.regSeasonWeeks;
        assertEquals("Phase  Postseason", SeasonPresentation.getSeasonPhaseChipText(league));
        league.currentWeek = league.regSeasonWeeks + 2;
        assertEquals("Phase  Postseason", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void phaseChip_nationalChampionship() {
        league.currentWeek = league.regSeasonWeeks + 3;
        assertEquals("Phase  National Championship", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void phaseChip_offseason() {
        league.currentWeek = league.regSeasonWeeks + 5;
        assertEquals("Phase  Offseason", SeasonPresentation.getSeasonPhaseChipText(league));
    }

    @Test
    public void cycleLabel_transitionsAtExpectedBoundaries() {
        league.currentWeek = 0;
        assertEquals("Pre-Season", SeasonPresentation.getSeasonCycleLabel(league));

        league.currentWeek = league.regSeasonWeeks - 1;
        assertEquals("Regular Season", SeasonPresentation.getSeasonCycleLabel(league));

        league.currentWeek = league.regSeasonWeeks;
        assertEquals("Postseason", SeasonPresentation.getSeasonCycleLabel(league));

        league.currentWeek = league.regSeasonWeeks + 4;
        assertEquals("Offseason", SeasonPresentation.getSeasonCycleLabel(league));

        league.currentWeek = league.regSeasonWeeks + 13;
        assertEquals("Recruiting", SeasonPresentation.getSeasonCycleLabel(league));
    }
}
