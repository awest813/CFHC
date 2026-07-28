package simulation;

import org.junit.Before;
import org.junit.Test;

import staff.HeadCoach;
import staff.OC;
import staff.DC;
import staff.Staff;

import static org.junit.Assert.*;

public class CoachCreationTest {

    private League league;
    private Team userTeam;
    private GameUiBridge noOpBridge;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        userTeam = league.getTeamList().get(0);
        userTeam.setUserControlled(true);
        league.userTeam = userTeam;
        noOpBridge = new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        };
    }

    @Test
    public void cpuCoach_createdByDefault_hasValidProfile() {
        Team cpuTeam = league.getTeamList().get(1);
        HeadCoach hc = cpuTeam.getHeadCoach();
        assertNotNull("CPU team should have a head coach", hc);
        assertNotNull("HC should have a name", hc.name);
        assertFalse("HC name should not be empty", hc.name.isEmpty());
        assertRatingBounded(hc);
    }

    @Test
    public void cpuCoach_hasValidContract() {
        Team cpuTeam = league.getTeamList().get(1);
        HeadCoach hc = cpuTeam.getHeadCoach();
        assertTrue("Contract length should be positive: " + hc.contractLength,
                hc.contractLength > 0);
        assertTrue("Contract year should be non-negative: " + hc.contractYear,
                hc.contractYear >= 0);
        assertTrue("Contract year should not exceed length: " + hc.contractYear + " > " + hc.contractLength,
                hc.contractYear <= hc.contractLength);
    }

    @Test
    public void userCoach_setupHasValidProfile() {
        userTeam.setupUserCoach("Test User Coach");
        HeadCoach hc = userTeam.getHeadCoach();

        assertNotNull("User team should have a head coach", hc);
        assertEquals("Coach name should match", "Test User Coach", hc.name);
        assertEquals("Position should be HC", "HC", hc.position);
        assertTrue("User flag should be set", hc.user);
        assertTrue(userTeam.isUserControlled());
        assertRatingBounded(hc);
    }

    @Test
    public void userCoach_hasLeagueAverageRatings() {
        userTeam.setupUserCoach("Average Coach");
        HeadCoach hc = userTeam.getHeadCoach();

        assertTrue("HC offensive rating should be reasonable: " + hc.ratOff,
                hc.ratOff >= 30 && hc.ratOff <= 99);
        assertTrue("HC defensive rating should be reasonable: " + hc.ratDef,
                hc.ratDef >= 30 && hc.ratDef <= 99);
        assertTrue("HC talent rating should be reasonable: " + hc.ratTalent,
                hc.ratTalent >= 30 && hc.ratTalent <= 99);
        assertTrue("HC discipline rating should be reasonable: " + hc.ratDiscipline,
                hc.ratDiscipline >= 30 && hc.ratDiscipline <= 99);
    }

    @Test
    public void userCoach_hasValidContract() {
        userTeam.setupUserCoach("Contract Coach");
        HeadCoach hc = userTeam.getHeadCoach();

        assertTrue("Contract length should be positive", hc.contractLength > 0);
        assertEquals("Contract year should start at 0", 0, hc.contractYear);
        assertTrue("Age should be reasonable: " + hc.age,
                hc.age >= 25 && hc.age <= 50);
    }

    @Test
    public void allTeamsHaveCoachesAtStartup() {
        for (Team t : league.getTeamList()) {
            assertNotNull(t.getName() + " should have a head coach", t.getHeadCoach());
            assertNotNull(t.getName() + " should have an OC", t.getOC());
            assertNotNull(t.getName() + " should have a DC", t.getDC());
        }
    }

    @Test
    public void allCoachesHaveValidRatings() {
        for (Team t : league.getTeamList()) {
            assertRatingBounded(t.getHeadCoach());
            assertCoordRatingBounded(t.getOC(), "OC", t.getName());
            assertCoordRatingBounded(t.getDC(), "DC", t.getName());
        }
    }

    @Test
    public void allCoachesHaveSchemes() {
        for (Team t : league.getTeamList()) {
            HeadCoach hc = t.getHeadCoach();
            assertTrue(t.getName() + " HC should have offStrat >= 0: " + hc.offStrat,
                    hc.offStrat >= 0);
            assertTrue(t.getName() + " HC should have defStrat >= 0: " + hc.defStrat,
                    hc.defStrat >= 0);

            Staff oc = t.getOC();
            assertNotNull(t.getName() + " OC should not be null", oc);
            Staff dc = t.getDC();
            assertNotNull(t.getName() + " DC should not be null", dc);
        }
    }

    @Test
    public void coachCreation_newOC_hasValidProfile() {
        Team cpuTeam = league.getTeamList().get(1);
        OC newOC = new OC("Test OC", 5);

        assertEquals("OC position", "OC", newOC.position);
        assertEquals("OC name", "Test OC", newOC.name);
        assertTrue("OC off rating should be reasonable: " + newOC.ratOff,
                newOC.ratOff >= 20 && newOC.ratOff <= 95);
    }

    @Test
    public void coachCreation_newDC_hasValidProfile() {
        DC newDC = new DC("Test DC", 5);

        assertEquals("DC position", "DC", newDC.position);
        assertEquals("DC name", "Test DC", newDC.name);
        assertTrue("DC def rating should be reasonable: " + newDC.ratDef,
                newDC.ratDef >= 20 && newDC.ratDef <= 95);
    }

    @Test
    public void coachCreation_promotedFromOC_hasRatings() {
        Staff oc = userTeam.getOC();
        assertNotNull("Team should have OC", oc);
        int ocOff = oc.ratOff;
        int ocDef = oc.ratDef;

        HeadCoach promoted = new HeadCoach(oc, userTeam);
        assertEquals("Promoted HC should keep name", oc.name, promoted.name);
        assertEquals("Promoted HC should keep ratOff", ocOff, promoted.ratOff);
        assertEquals("Promoted HC should keep ratDef", ocDef, promoted.ratDef);
        assertEquals("HC position", "HC", promoted.position);
    }

    @Test
    public void coachCreation_hiringScore_nonNegative() {
        HeadCoach hc = userTeam.getHeadCoach();
        int[] wt = {1, 1, 1, 1};
        float score = hc.getHCHiring(wt);
        assertTrue("HC hiring score should be >= 0: " + score, score >= 0);
    }

    @Test
    public void coachCreation_saveLoadRoundTrip_preservesCoach() {
        HeadCoach hcBefore = userTeam.getHeadCoach();
        String nameBefore = hcBefore.name;
        int ovrBefore = hcBefore.ratOvr;

        java.io.File tmpFile = new java.io.File(System.getProperty("user.dir"), "build/tmp/coach-test-save.cfb");
        tmpFile.getParentFile().mkdirs();
        assertTrue(league.saveLeague(tmpFile));

        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League loaded = new League(tmpFile,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                noOpBridge, true);
        loaded.setPlatformResourceProvider(resources);

        Team loadedTeam = loaded.getTeamList().get(0);
        HeadCoach hcAfter = loadedTeam.getHeadCoach();
        assertNotNull("Loaded team should have HC", hcAfter);
        assertEquals("HC name should be preserved", nameBefore, hcAfter.name);
        assertEquals("HC OVR should be preserved", ovrBefore, hcAfter.ratOvr);
        tmpFile.delete();
    }

    private void assertRatingBounded(HeadCoach hc) {
        assertTrue("HC OVR should be >= 0: " + hc.ratOvr, hc.ratOvr >= 0);
        assertTrue("HC ratOff should be >= 0: " + hc.ratOff, hc.ratOff >= 0);
        assertTrue("HC ratDef should be >= 0: " + hc.ratDef, hc.ratDef >= 0);
        assertTrue("HC ratTalent should be >= 0: " + hc.ratTalent, hc.ratTalent >= 0);
        assertTrue("HC ratDiscipline should be >= 0: " + hc.ratDiscipline, hc.ratDiscipline >= 0);
    }

    private void assertCoordRatingBounded(Staff coord, String role, String teamName) {
        assertNotNull(teamName + " should have " + role, coord);
        assertTrue(teamName + " " + role + " ratOff should be >= 0: " + coord.ratOff, coord.ratOff >= 0);
        assertTrue(teamName + " " + role + " ratDef should be >= 0: " + coord.ratDef, coord.ratDef >= 0);
    }
}
