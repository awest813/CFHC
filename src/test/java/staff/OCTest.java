package staff;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class OCTest {

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
    public void constructor_newCoach_setsPositionOC() {
        OC coach = new OC("Jane Coach", 5, 35, team);
        assertEquals("OC", coach.position);
        assertEquals("Jane Coach", coach.name);
        assertEquals(35, coach.age);
        assertNotNull(coach.stats);
        assertEquals(10, coach.stats.length);
        assertNotNull(coach.awards);
        assertNotNull(coach.history);
    }

    @Test
    public void constructor_fromCSV_parsesFields() {
        String csv = "OC,Jane Coach,40,2,82,60,75,68,3,2,1,3,0,false,65,3,false,50,2,&8,4,2,1,0,0,0,0,5,10&0,1,0,0,0,0,0";
        OC coach = new OC(team, csv);
        assertEquals("OC", coach.position);
        assertEquals("Jane Coach", coach.name);
        assertEquals(40, coach.age);
        assertEquals(82, coach.ratOff);
        assertEquals(60, coach.ratDef);
        assertEquals(75, coach.ratTalent);
        assertEquals(68, coach.ratDiscipline);
    }

    @Test
    public void getHCHiring_returnsValue() {
        OC coach = new OC("OC Hire", 5, 35, team);
        float hiring = coach.getHCHiring(wt);
        assertTrue("OC getHCHiring should return positive for active coach", hiring > 0);
    }

    @Test
    public void overallWt_favorsOffAndTalent() {
        OC coach = new OC("Weights Test", 5, 35, team);
        assertArrayEquals(new int[]{0, 4, 3, 1}, coach.overallWt);
        int expectedOvr = (0 * coach.ratOff + 4 * coach.ratDef + 3 * coach.ratTalent + 1 * coach.ratDiscipline) / 8;
        assertEquals(expectedOvr, coach.ratOvr);
    }

    @Test
    public void saveStaffData_roundTrips() {
        OC original = new OC("Round Trip", 5, 40, team);
        original.ratOff = 88;
        original.ratDef = 55;
        original.ratTalent = 80;

        String saved = original.saveStaffData();
        OC loaded = new OC(team, saved);

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
        OC coach = new OC("Advance OC", 5, 35, team);
        int oldAge = coach.age;
        int oldRatOff = coach.ratOff;
        int oldRatDef = coach.ratDef;

        coach.advanceSeason(10, 0);

        assertEquals(oldAge + 1, coach.age);
        assertTrue("OC ratOff should increase with offpts", coach.ratOff > oldRatOff);
        assertEquals("OC ratDef should not change from offpts", oldRatDef, coach.ratDef);
    }
}
