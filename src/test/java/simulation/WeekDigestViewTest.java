package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WeekDigestViewTest {

    private static final String SAMPLE = "Week 4 In Review\n\n"
            + "Your Result: W 31-17 vs Rival U  (Rivalry Game)\n\n"
            + "Top 10\n1. Alpha (4-0)\n2. Beta (4-0) +1\n\n"
            + "Headlines\n- Alpha stays perfect\n- Beta upsets Gamma\n\n"
            + "Injury Report\n- QB J. Doe (2 wks)\n\n"
            + "Next Up: at Delta State";

    @Test
    public void parsesTitleResultSectionsAndNextUp() {
        WeekDigestView v = WeekDigestView.parse(SAMPLE);
        assertEquals("Week 4 In Review", v.title);
        assertEquals("W 31-17 vs Rival U  (Rivalry Game)", v.result);
        assertEquals("W", v.outcome);
        assertEquals("at Delta State", v.nextUp);
        assertEquals(3, v.sections.size());
        assertEquals("Top 10", v.sections.get(0).heading);
        assertEquals("2. Beta (4-0) +1", v.sections.get(0).lines.get(1));
        assertEquals("Headlines", v.sections.get(1).heading);
        assertEquals("Alpha stays perfect", v.sections.get(1).lines.get(0));
        assertEquals("QB J. Doe (2 wks)", v.sections.get(2).lines.get(0));
    }

    @Test
    public void byeWeekAndLossOutcomes() {
        assertEquals("BYE", WeekDigestView.parse("Week 2 In Review\n\nYour Result: BYE WEEK").outcome);
        assertEquals("L", WeekDigestView.parse("Week 2 In Review\n\nYour Result: L 3-24 at X  (Game)").outcome);
        assertEquals("T", WeekDigestView.parse("Week 2 In Review\n\nYour Result: T 7-7 vs Y  (Game)").outcome);
    }

    @Test
    public void emptyAndNullAreSafe() {
        assertTrue(WeekDigestView.parse(null).sections.isEmpty());
        assertEquals("", WeekDigestView.parse("").title);
    }

    @Test
    public void realDigestRoundTrips() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        SeasonController controller = new SeasonController(league, new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
            @Override public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {}
            @Override public void showNotification(String title, String message) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String summaryText) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        });
        // Advance until a week with a digest is produced (preseason steps carry none).
        String digest = "";
        for (int i = 0; i < 4 && digest.isEmpty(); i++) {
            digest = controller.advanceWeek().getWeekDigest();
        }
        WeekDigestView v = WeekDigestView.parse(digest);
        assertTrue(digest, v.title.startsWith("Week "));
        assertTrue("user result parsed from: " + digest, !v.result.isEmpty());
        assertTrue("outcome from: " + v.result, !v.outcome.isEmpty());
        assertTrue("has sections: " + digest, !v.sections.isEmpty());
    }
}
