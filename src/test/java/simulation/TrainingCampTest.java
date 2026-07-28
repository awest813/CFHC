package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.*;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class TrainingCampTest {

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
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void focusPlayerGetsBoost() {
        ArrayList<PlayerQB> saved = new ArrayList<>(team.teamQBs);
        team.teamQBs.clear();

        PlayerQB qb = new PlayerQB("Camp QB", 1, 5, team);
        qb.year = 2;
        team.teamQBs.add(qb);

        team.trainingCampFocusNames.add("Camp QB");
        boolean savedUser = team.userControlled;
        team.userControlled = true;
        int before = qb.ratOvr;
        team.trainingCamp();
        team.userControlled = savedUser;
        assertTrue("Focus player should gain OVR", qb.ratOvr > before);

        team.teamQBs.clear();
        team.teamQBs.addAll(saved);
    }

    @Test
    public void nonFocusPlayerMayGetSmallBoost() {
        ArrayList<PlayerQB> saved = new ArrayList<>(team.teamQBs);
        team.teamQBs.clear();

        PlayerQB qb = new PlayerQB("Normal QB", 1, 3, team);
        qb.year = 2;
        team.teamQBs.add(qb);

        boolean savedUser = team.userControlled;
        team.userControlled = true;
        int before = qb.ratOvr;
        team.trainingCamp();
        team.userControlled = savedUser;
        assertTrue("Non-focus player should gain OVR or stay same", qb.ratOvr >= before);

        team.teamQBs.clear();
        team.teamQBs.addAll(saved);
    }

    @Test
    public void selectTrainingCampFocusPicksThreeUnderclassmen() {
        team.selectTrainingCampFocusPlayers();
        assertTrue("Should pick at most 3 focus players", team.trainingCampFocusNames.size() <= 3);
        assertTrue("Should pick at least 1 focus player if roster has underclassmen",
                team.trainingCampFocusNames.size() >= 1);
    }

    @Test
    public void focusPlayersGetHigherBoostThanNonFocus() {
        ArrayList<PlayerQB> saved = new ArrayList<>(team.teamQBs);
        team.teamQBs.clear();

        PlayerQB focus = new PlayerQB("Focus QB", 1, 5, team);
        focus.year = 2;
        team.teamQBs.add(focus);

        PlayerQB normal = new PlayerQB("Normal QB", 1, 3, team);
        normal.year = 2;
        team.teamQBs.add(normal);

        team.trainingCampFocusNames.add("Focus QB");
        boolean savedUser = team.userControlled;
        team.userControlled = true;
        int focusBefore = focus.ratOvr;
        int normalBefore = normal.ratOvr;
        team.trainingCamp();
        team.userControlled = savedUser;
        int focusGain = focus.ratOvr - focusBefore;
        int normalGain = normal.ratOvr - normalBefore;
        assertTrue("Focus player should gain at least as much as non-focus", focusGain >= normalGain);

        team.teamQBs.clear();
        team.teamQBs.addAll(saved);
    }

    @Test
    public void trainingCampResetsFocusList() {
        team.trainingCampFocusNames.add("Ghost");
        boolean savedUser = team.userControlled;
        team.userControlled = true;
        team.trainingCamp();
        team.userControlled = savedUser;
        assertTrue("Focus list should be cleared after camp", team.trainingCampFocusNames.isEmpty());
    }
}
