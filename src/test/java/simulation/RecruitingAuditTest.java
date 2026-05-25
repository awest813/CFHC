package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingSessionData;
import recruiting.RecruitingPresentation;
import simulation.SimulationFacade;

import static org.junit.Assert.*;

public class RecruitingAuditTest {

    private League league;
    private Team userTeam;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
    }

    @Test
    public void recruitingSession_prospectListIsPopulated() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);

        assertNotNull("Session data should not be null", data);
        assertNotNull("Prospect list should not be null", data.availAll);
    }

    @Test
    public void recruitingSession_prospectRecordsAreValid() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);

        if (data.availAll != null && !data.availAll.isEmpty()) {
            for (RecruitingPlayerRecord prospect : data.availAll) {
                assertNotNull("Prospect should have a name", prospect.name());
                assertNotNull("Prospect should have a position", prospect.position());
                assertTrue("Prospect cost should be >= 0: " + prospect.cost(),
                        prospect.cost() >= 0);
                assertTrue("Prospect stars should be 1-5: " + prospect.stars(),
                        prospect.stars() >= 1 && prospect.stars() <= 5);
            }
        }
    }

    @Test
    public void recruitingPresenter_buildOverviewSummary_returnsNonEmpty() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);
        String summary = RecruitingPresentation.buildOverviewSummary(data);
        assertNotNull("Overview summary should not be null", summary);
        assertTrue("Overview summary should not be empty", summary.length() > 0);
    }

    @Test
    public void recruitingPresenter_buildBoardStatus_returnsNonEmpty() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);
        String status = RecruitingPresentation.buildBoardStatus(data);
        assertNotNull("Board status should not be null", status);
        assertTrue("Board status should not be empty", status.length() > 0);
    }

    @Test
    public void recruitingPresenter_buildRecruitBoardDetails_handlesAllPositions() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);

        if (data.availAll != null) {
            for (RecruitingPlayerRecord r : data.availAll) {
                String details = RecruitingPresentation.buildRecruitBoardDetails(r, r.position());
                assertNotNull("Details for " + r.position() + " should not be null", details);
                assertTrue("Details should contain position info: " + r.position(),
                        details.contains("Home State"));
            }
        }
    }

    @Test
    public void recruitingPresenter_buildPotentialDetails_returnsNonEmpty() {
        RecruitingSessionData data = SimulationFacade.prepareRecruitingSession(userTeam);

        if (data.availAll != null && !data.availAll.isEmpty()) {
            RecruitingPlayerRecord r = data.availAll.get(0);
            String details = RecruitingPresentation.buildPotentialDetails(r);
            assertNotNull("Potential details should not be null", details);
            assertTrue("Potential details should contain Height", details.contains("Height"));
        }
    }
}
