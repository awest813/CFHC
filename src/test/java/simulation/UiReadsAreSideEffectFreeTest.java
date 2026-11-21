package simulation;

import org.junit.Test;

import staff.HeadCoach;

import java.util.ArrayList;
import java.util.Collections;

import comparator.CompCoachScore;

import static org.junit.Assert.assertEquals;

/**
 * Screens read rankings, award watches and coach scores whenever they are opened.
 * Those reads must not draw random numbers (a reload would no longer match an
 * unbroken run, and merely opening a screen would change the season) and must
 * return the same value every call (sorting by a value that changes between
 * comparisons throws "Comparison method violates its general contract").
 */
public class UiReadsAreSideEffectFreeTest {

    @Test
    public void rankingAndAwardReads_doNotTouchTheRng() {
        SimRandom.pinNextSeed(424242L);
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(r);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        int[] checkWeeks = {6, SeasonFlowOrder.bowlWeek1(league.regSeasonWeeks)};
        for (int target : checkWeeks) {
            while (league.currentWeek < target) controller.advanceWeek();
            String before = SimRandom.exportState();
            for (int i = 0; i <= 12; i++) league.getAwardsWatch(i);
            for (int i = 0; i <= 21; i++) league.getTeamRankingsStr(i);
            ArrayList<HeadCoach> coaches = new ArrayList<>();
            for (Team t : league.getTeamList()) {
                if (t.getHeadCoach() == null) continue;
                coaches.add(t.getHeadCoach());
                int score = t.getHeadCoach().getCoachScore();
                assertEquals("coach score must be stable", score, t.getHeadCoach().getCoachScore());
            }
            Collections.sort(coaches, new CompCoachScore()); // must not throw
            assertEquals("UI reads drew random numbers at week " + target,
                    before, SimRandom.exportState());
        }
    }
}
