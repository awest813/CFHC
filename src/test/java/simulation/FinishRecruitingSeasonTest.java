package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FinishRecruitingSeasonTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
        league.currentWeek = league.regSeasonWeeks + 13;
        league.recruitingPhaseActive = true;
    }

    @Test
    public void finishRecruitingSeason_resetsWeekAndRecruitingFlag() {
        int yearBefore = league.getYear();

        league.finishRecruitingSeason("");

        assertEquals(0, league.currentWeek);
        assertFalse(league.recruitingPhaseActive);
        assertEquals(yearBefore, league.getYear());
        assertFalse(league.userTeam.getGameSchedule().isEmpty());
    }
}
