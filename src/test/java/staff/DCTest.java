package staff;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class DCTest {

    private Team team;
    private int[] wt = {1, 1, 1, 1};

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void constructor_newCoach_setsPositionDC() {
        DC coach = new DC("Bob Coach", 5, 35, team);
        assertEquals("DC", coach.position);
        assertEquals("Bob Coach", coach.name);
        assertEquals(35, coach.age);
        assertNotNull(coach.stats);
        assertEquals(10, coach.stats.length);
        assertNotNull(coach.awards);
        assertNotNull(coach.history);
    }

    @Test
    public void constructor_fromCSV_parsesFields() {
        String csv = "DC,Bob Coach,42,2,60,85,78,65,2,3,1,3,0,false,68,2,false,40,1,&7,5,3,2,0,1,0,0,8,6&1,0,1,0,0,0,1";
        DC coach = new DC(team, csv);
        assertEquals("DC", coach.position);
        assertEquals("Bob Coach", coach.name);
        assertEquals(42, coach.age);
        assertEquals(60, coach.ratOff);
        assertEquals(85, coach.ratDef);
        assertEquals(78, coach.ratTalent);
        assertEquals(65, coach.ratDiscipline);
    }

    @Test
    public void getHCHiring_returnsValue() {
        DC coach = new DC("DC Hire", 5, 35, team);
        float hiring = coach.getHCHiring(wt);
        assertTrue("DC getHCHiring should return positive for active coach", hiring > 0);
    }

    @Test
    public void overallWt_favorsDefAndTalent() {
        DC coach = new DC("Weights Test", 5, 35, team);
        assertArrayEquals(new int[]{0, 4, 3, 1}, coach.overallWt);
        int expectedOvr = (0 * coach.ratOff + 4 * coach.ratDef + 3 * coach.ratTalent + 1 * coach.ratDiscipline) / 8;
        assertEquals(expectedOvr, coach.ratOvr);
    }

    @Test
    public void saveStaffData_roundTrips() {
        DC original = new DC("Round Trip", 5, 40, team);
        original.ratOff = 55;
        original.ratDef = 90;
        original.ratTalent = 82;

        String saved = original.saveStaffData();
        DC loaded = new DC(team, saved);

        assertEquals(original.position, loaded.position);
        assertEquals(original.name, loaded.name);
        assertEquals(original.age, loaded.age);
        assertEquals(original.year, loaded.year);
        assertEquals(original.ratOff, loaded.ratOff);
        assertEquals(original.ratDef, loaded.ratDef);
        assertEquals(original.ratTalent, loaded.ratTalent);
        assertEquals(original.ratDiscipline, loaded.ratDiscipline);
        assertEquals(original.offStrat, loaded.offStrat);
    }

    @Test
    public void advanceSeason_incrementsAgeAndRatings() {
        DC coach = new DC("Advance DC", 5, 35, team);
        int oldAge = coach.age;
        int oldRatDef = coach.ratDef;
        int oldRatOff = coach.ratOff;

        coach.advanceSeason(0, 10);

        assertEquals(oldAge + 1, coach.age);
        assertTrue("DC ratDef should increase with defpts", coach.ratDef > oldRatDef);
        assertEquals("DC ratOff should not change from defpts", oldRatOff, coach.ratOff);
    }
}
