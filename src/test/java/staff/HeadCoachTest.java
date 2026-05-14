package staff;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class HeadCoachTest {

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
    public void constructor_newCoach_setsPositionAndStats() {
        HeadCoach coach = new HeadCoach("John Smith", 5, 35, team);
        assertEquals("HC", coach.position);
        assertEquals("John Smith", coach.name);
        assertEquals(35, coach.age);
        assertNotNull(coach.stats);
        assertEquals(10, coach.stats.length);
        assertNotNull(coach.awards);
        assertEquals(7, coach.awards.length);
        assertNotNull(coach.history);
    }

    @Test
    public void constructor_fromCSV_parsesFields() {
        String csv = "HC,John Smith,45,3,78,65,80,70,2,1,2,5,50,false,72,5,false,120,6,&10,5,3,2,1,0,0,0,15,8&1,2,1,0,0,1,1";
        HeadCoach coach = new HeadCoach(team, csv);
        assertEquals("HC", coach.position);
        assertEquals("John Smith", coach.name);
        assertEquals(45, coach.age);
        assertEquals(3, coach.year);
        assertEquals(78, coach.ratOff);
        assertEquals(65, coach.ratDef);
        assertEquals(80, coach.ratTalent);
        assertEquals(70, coach.ratDiscipline);
        assertEquals(2, coach.offStrat);
        assertEquals(1, coach.defStrat);
        assertEquals(2, coach.contractYear);
        assertEquals(5, coach.contractLength);
        assertEquals(50, coach.baselinePrestige);
        assertFalse(coach.retired);
        assertEquals(120, coach.coachSkillXp);
        assertEquals(6, coach.coachSkillRanksBits);
        assertEquals(10, coach.getWins());
        assertEquals(5, coach.getLosses());
        assertEquals(1, coach.getCOTY());
    }

    @Test
    public void getHCHiring_returnsPositiveForActive() {
        HeadCoach coach = new HeadCoach("Jane Doe", 5, 35, team);
        float hiring = coach.getHCHiring(wt);
        assertTrue("Active coach hiring value should be positive", hiring > 0);
    }

    @Test
    public void getHCHiring_returnsZeroForRetired() {
        HeadCoach coach = new HeadCoach("Jane Doe", 5, 35, team);
        coach.retired = true;
        float hiring = coach.getHCHiring(wt);
        assertEquals(0f, hiring, 0.001f);
    }

    @Test
    public void saveStaffData_roundTrips() {
        HeadCoach original = new HeadCoach("Save Test", 5, 40, team);
        original.ratOff = 85;
        original.ratDef = 70;
        original.ratTalent = 82;
        original.ratDiscipline = 65;
        original.coachSkillXp = 200;
        original.coachSkillRanksBits = 10;

        String saved = original.saveStaffData();
        HeadCoach loaded = new HeadCoach(team, saved);

        assertEquals(original.position, loaded.position);
        assertEquals(original.name, loaded.name);
        assertEquals(original.age, loaded.age);
        assertEquals(original.year, loaded.year);
        assertEquals(original.ratOff, loaded.ratOff);
        assertEquals(original.ratDef, loaded.ratDef);
        assertEquals(original.ratTalent, loaded.ratTalent);
        assertEquals(original.ratDiscipline, loaded.ratDiscipline);
        assertEquals(original.offStrat, loaded.offStrat);
        assertEquals(original.defStrat, loaded.defStrat);
        assertEquals(original.contractYear, loaded.contractYear);
        assertEquals(original.contractLength, loaded.contractLength);
        assertEquals(original.baselinePrestige, loaded.baselinePrestige);
        assertEquals(original.retired, loaded.retired);
        assertEquals(original.coachSkillXp, loaded.coachSkillXp);
        assertEquals(original.coachSkillRanksBits, loaded.coachSkillRanksBits);
    }

    @Test
    public void advanceSeason_incrementsAge() {
        HeadCoach coach = new HeadCoach("Age Test", 5, 35, team);
        int oldAge = coach.age;
        coach.advanceSeason(0, 0);
        assertTrue("Age should increase after advanceSeason", coach.age > oldAge);
    }
}
