package simulation;

import org.junit.Before;
import org.junit.Test;

import staff.HeadCoach;
import staff.OC;
import staff.DC;
import staff.Staff;

import static org.junit.Assert.*;

public class StaffHiringFiringTest {

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
    public void coordinatorCarousel_fillsNullOCSlots() {
        Team cpuTeam = findCpuTeam();
        cpuTeam.setOC(null);

        league.coordinatorCarousel();

        assertNotNull(cpuTeam.getName() + " should have an OC after carousel", cpuTeam.getOC());
        assertTrue(cpuTeam.getOC().ratOvr >= 0 && cpuTeam.getOC().ratOvr <= 99);
    }

    @Test
    public void coordinatorCarousel_fillsNullDCSlots() {
        Team cpuTeam = findCpuTeam();
        cpuTeam.setDC(null);

        league.coordinatorCarousel();

        assertNotNull(cpuTeam.getName() + " should have a DC after carousel", cpuTeam.getDC());
        assertTrue(cpuTeam.getDC().ratOvr >= 0 && cpuTeam.getDC().ratOvr <= 99);
    }

    @Test
    public void coordinatorCarousel_fillsBothCoordSlots() {
        Team cpuTeam = findCpuTeam();
        cpuTeam.setOC(null);
        cpuTeam.setDC(null);

        league.coordinatorCarousel();

        assertNotNull(cpuTeam.getName() + " should have OC", cpuTeam.getOC());
        assertNotNull(cpuTeam.getName() + " should have DC", cpuTeam.getDC());
    }

    @Test
    public void hireMissingCoaches_fillsNullHCSlots() {
        Team cpuTeam = findCpuTeam();
        String oldName = cpuTeam.getHeadCoach().name;
        league.addCoach(new HeadCoach(cpuTeam.getHeadCoach(), cpuTeam));
        cpuTeam.setHeadCoach(null);
        cpuTeam.setOC(null);
        cpuTeam.setDC(null);

        league.hireMissingCoaches();

        assertNotNull(cpuTeam.getName() + " should have a HC after hiring", cpuTeam.getHeadCoach());
    }

    @Test
    public void advanceHC_ratingsStayBounded() {
        Team cpuTeam = findCpuTeam();

        for (int i = 0; i < 3; i++) {
            league.advanceSeason();
        }

        HeadCoach hc = cpuTeam.getHeadCoach();
        assertNotNull(cpuTeam.getName() + " should still have HC", hc);
        assertTrue("HC ratOff should be 0-99: " + hc.ratOff, hc.ratOff >= 0 && hc.ratOff <= 99);
        assertTrue("HC ratDef should be 0-99: " + hc.ratDef, hc.ratDef >= 0 && hc.ratDef <= 99);
        assertTrue("HC ratTalent should be 0-99: " + hc.ratTalent, hc.ratTalent >= 0 && hc.ratTalent <= 99);
    }

    @Test
    public void advanceCoordinator_ratingsStayBounded() {
        Team cpuTeam = findCpuTeam();

        league.advanceSeason();

        Staff oc = cpuTeam.getOC();
        Staff dc = cpuTeam.getDC();
        if (oc != null) {
            assertTrue("OC ratOff bounded: " + oc.ratOff, oc.ratOff >= 0 && oc.ratOff <= 99);
            assertTrue("OC ratDef bounded: " + oc.ratDef, oc.ratDef >= 0 && oc.ratDef <= 99);
        }
        if (dc != null) {
            assertTrue("DC ratOff bounded: " + dc.ratOff, dc.ratOff >= 0 && dc.ratOff <= 99);
            assertTrue("DC ratDef bounded: " + dc.ratDef, dc.ratDef >= 0 && dc.ratDef <= 99);
        }
    }

    @Test
    public void midSeasonFiring_promotesReplacement() {
        Team cpuTeam = findCpuTeam();
        String firedName = cpuTeam.getHeadCoach().name;

        cpuTeam.midSeasonFiring();

        HeadCoach newHC = cpuTeam.getHeadCoach();
        assertNotNull(cpuTeam.getName() + " should have HC after firing", newHC);
        assertNotEquals("New HC should differ from fired HC", firedName, newHC.name);
        assertEquals("Replacement contract length should be 1", 1, newHC.contractLength);
    }

    @Test
    public void midSeasonFiring_clonesFiredCoachToPool() {
        Team cpuTeam = findCpuTeam();
        String firedName = cpuTeam.getHeadCoach().name;
        int poolBefore = league.getCoachList().size();

        cpuTeam.midSeasonFiring();

        boolean found = false;
        for (Staff s : league.getCoachList()) {
            if (s.name.equals(firedName)) {
                found = true;
                break;
            }
        }
        assertTrue("Fired coach should appear in coach pool", found);
    }

    @Test
    public void getOCList_returnsCandidates() {
        HeadCoach hc = userTeam.getHeadCoach();
        java.util.ArrayList<Staff> candidates = league.getOCList(hc);

        assertNotNull("OC list should not be null", candidates);
        assertTrue("OC list should have candidates", candidates.size() > 0);
    }

    @Test
    public void getDCList_returnsCandidates() {
        HeadCoach hc = userTeam.getHeadCoach();
        java.util.ArrayList<Staff> candidates = league.getDCList(hc);

        assertNotNull("DC list should not be null", candidates);
        assertTrue("DC list should have candidates", candidates.size() > 0);
    }

    @Test
    public void promoteCoach_fromCoordinators() {
        Team cpuTeam = findCpuTeam();
        String ocName = cpuTeam.getOC().name;
        String dcName = cpuTeam.getDC().name;

        cpuTeam.promoteCoach();

        HeadCoach promoted = cpuTeam.getHeadCoach();
        assertNotNull("Team should have HC after promotion", promoted);
        assertTrue("Promoted HC should be OC or DC",
                promoted.name.equals(ocName) || promoted.name.equals(dcName));
    }

    @Test
    public void staffAdvanceSeason_incrementsAgeAndYear() {
        Team cpuTeam = findCpuTeam();
        HeadCoach hc = cpuTeam.getHeadCoach();
        int ageBefore = hc.age;
        int yearBefore = hc.year;

        cpuTeam.advanceHC(league.leagueRecords, cpuTeam.teamRecords);

        HeadCoach hcAfter = cpuTeam.getHeadCoach();
        assertNotNull("Team should still have HC", hcAfter);
        assertEquals("Age should increment", ageBefore + 1, hcAfter.age);
        assertEquals("Year should increment", yearBefore + 1, hcAfter.year);
    }

    @Test
    public void newCoachDecisions_mayReplaceCoordinators() {
        Team cpuTeam = findCpuTeam();
        league.advanceSeason();

        assertNotNull(cpuTeam.getName() + " should have OC", cpuTeam.getOC());
        assertNotNull(cpuTeam.getName() + " should have DC", cpuTeam.getDC());
    }

    @Test
    public void multipleSeasons_staffingRemainsValid() {
        for (int season = 0; season < 5; season++) {
            league.advanceSeason();
            league.startNextSeason();
        }

        for (Team t : league.getTeamList()) {
            assertNotNull(t.getName() + " should have HC after 5 seasons", t.getHeadCoach());
            assertStaffBounded(t.getHeadCoach());
        }
    }

    private Team findCpuTeam() {
        for (Team t : league.getTeamList()) {
            if (!t.isUserControlled()) return t;
        }
        return league.getTeamList().get(1);
    }

    private void assertStaffBounded(Staff s) {
        assertTrue("ratOff non-negative: " + s.ratOff, s.ratOff >= 0);
        assertTrue("ratDef non-negative: " + s.ratDef, s.ratDef >= 0);
        assertTrue("ratTalent non-negative: " + s.ratTalent, s.ratTalent >= 0);
        assertTrue("ratDiscipline non-negative: " + s.ratDiscipline, s.ratDiscipline >= 0);
    }
}
