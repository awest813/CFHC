package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.Player;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeamDisciplineSafetyTest {

    private League league;
    private Team team;

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
        team = league.getTeamList().get(0);
        team.setUserControlled(true);
        league.userTeam = team;
    }

    @Test
    public void suspendPlayerSetup_emptyCandidatesClearsFlagWithoutThrowing() {
        team.playersDis = new ArrayList<>();
        team.disciplineAction = true;
        // Force empty candidate pools so getLowDisciplinePlayers cannot refill.
        team.teamQBs = new ArrayList<>();
        team.teamRBs = new ArrayList<>();
        team.teamWRs = new ArrayList<>();
        team.teamTEs = new ArrayList<>();
        team.teamOLs = new ArrayList<>();
        team.teamKs = new ArrayList<>();
        team.teamDLs = new ArrayList<>();
        team.teamLBs = new ArrayList<>();
        team.teamCBs = new ArrayList<>();
        team.teamSs = new ArrayList<>();

        team.suspendPlayerSetup(GameUiBridge.NO_OP);
        assertFalse(team.disciplineAction);
    }

    @Test
    public void checkSuspensionPosition_thinRosterDoesNotThrow() {
        ArrayList<Player> thin = new ArrayList<>();
        if (!team.teamQBs.isEmpty()) {
            thin.add(team.teamQBs.get(0));
        }
        team.playersDis = new ArrayList<>();
        team.checkSuspensionPosition(thin, 11, 100);
        assertTrue(team.playersDis.size() <= thin.size());
    }
}
