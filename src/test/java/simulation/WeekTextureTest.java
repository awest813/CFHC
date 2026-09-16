package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 1 texture layer (see docs/game-flow-audit-and-premium-plan.md):
 * weekly poll releases with movement, the week-in-review digest, result audio
 * cues, the real morale backend, and the previously-dead offseason weeks
 * (R+6 job interest, R+11 portal needs).
 */
public class WeekTextureTest {

    private League league;
    private SeasonController controller;
    private Team userTeam;
    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0x7E77E1L);
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
        userTeam = league.getTeamList().get(0);
        league.userTeam = userTeam;
        userTeam.setUserControlled(true);
        controller = new SeasonController(league, GameUiBridge.NO_OP);
    }

    @Test
    public void weeklyPollRelease_firesEveryRegularWeekWithMovement() {
        controller.advanceWeek(); // preseason -> week 1

        for (int week = 1; week <= 4; week++) {
            SeasonAdvanceResult result = controller.advanceWeek();
            assertEquals(week + 1, result.weekAfter);
            boolean pollStory = false;
            boolean pollHeadline = false;
            for (String story : league.newsStories.get(week + 1)) {
                if (story.startsWith("New Top 25 Released>")) {
                    pollStory = true;
                    break;
                }
            }
            for (String headline : league.newsHeadlines) {
                if (headline.contains("new Top 25") || headline.contains("No. 1")) {
                    pollHeadline = true;
                    break;
                }
            }
            assertTrue("Week " + week + " should publish a Top 25 story", pollStory);
            assertTrue("Week " + week + " should publish a Top 25 headline", pollHeadline);
        }

        Team ranked = null;
        for (Team t : league.getTeamList()) {
            if (t.getRankTeamPollScore() >= 1 && t.getRankTeamPollScore() <= 25
                    && t.getPrevRankTeamPollScore() > 0) {
                ranked = t;
                break;
            }
        }
        assertNotNull("At least one team should carry a previous rank by week 4", ranked);
    }

    @Test
    public void weekDigest_containsUserResultAndPoll_andAudioCues() {
        assertEquals("Preseason advance should cue WHISTLE",
                AudioEvent.WHISTLE, controller.advanceWeek().getAudioEvent());

        SeasonAdvanceResult result = controller.advanceWeek(); // week 1 played
        String digest = result.getWeekDigest();
        assertTrue("Digest should exist for a regular week", digest != null && !digest.isEmpty());
        assertTrue("Digest should be titled with the week", digest.contains("Week 1 In Review"));
        assertTrue("Digest should show the user result", digest.contains("Your Result:"));
        assertTrue("Digest should show the poll", digest.contains("Top 10"));
        assertTrue("Digest should name the user team somewhere",
                digest.contains(userTeam.getName()) || digest.contains(userTeam.getAbbr()));

        // Audio: non-postseason weeks may only cue cheer/roar on a user win, or nothing.
        AudioEvent audio = result.getAudioEvent();
        assertTrue("In-season audio must be null or a win cue",
                audio == null || audio == AudioEvent.TOUCHDOWN_CHEER || audio == AudioEvent.CROWD_ROAR);
    }

    @Test
    public void moraleSnapshot_withinBounds_andRespondsToCharacter() {
        TeamMoraleSnapshot before = userTeam.getTeamMoraleSnapshot();
        assertTrue(before.chemistry() >= 0 && before.chemistry() <= 100);
        assertTrue(before.leadership() >= 0 && before.leadership() <= 100);
        assertTrue(before.buyIn() >= 0 && before.buyIn() <= 100);
        assertTrue(before.overall() >= 0 && before.overall() <= 100);

        List<positions.Player> players = new ArrayList<>(userTeam.getAllPlayers());
        players.sort((a, b) -> Integer.compare(b.ratOvr, a.ratOvr));
        int topN = Math.min(11, players.size());
        for (int i = 0; i < topN; i++) {
            players.get(i).character = 95;
        }
        int highLeadership = userTeam.getTeamMoraleSnapshot().leadership();
        for (int i = 0; i < topN; i++) {
            players.get(i).character = 5;
        }
        int lowLeadership = userTeam.getTeamMoraleSnapshot().leadership();

        assertTrue("High-character leaders should out-score low-character leaders ("
                        + highLeadership + " vs " + lowLeadership + ")",
                highLeadership > lowLeadership);
    }

    @Test
    public void deadOffseasonWeeks_nowProduceNews() {
        // Preseason + full regular season + postseason through the NCG: lands at week R+4.
        for (int w = 0; w < league.regSeasonWeeks + 4; w++) {
            controller.advanceWeek();
        }
        // R+4 season summary -> R+5, then R+5 contracts -> R+6.
        controller.advanceWeek();
        controller.advanceWeek();
        league.newsHeadlines.clear();
        controller.advanceWeek(); // R+6 job-interest week
        assertFalse("Job-interest week should publish carousel headlines",
                league.newsHeadlines.isEmpty());

        // Advance R+7 carousel, R+8 coordinator, R+9 graduation, R+10 transfers.
        for (int w = 0; w < 4; w++) {
            controller.advanceWeek();
        }
        league.newsHeadlines.clear();
        controller.advanceWeek(); // R+11 portal-need week
        boolean portalHeadline = false;
        for (String headline : league.newsHeadlines) {
            if (headline.startsWith("Portal Need") || headline.startsWith("Transfer Portal")) {
                portalHeadline = true;
                break;
            }
        }
        assertTrue("Portal-need week should publish portal headlines", portalHeadline);
    }

    @Test
    public void prevRank_survivesSaveLoadRoundTrip() throws Exception {
        controller.advanceWeek(); // preseason
        controller.advanceWeek(); // week 1: ranks + prev ranks populated
        userTeam.setPrevRankTeamPollScore(7);

        File tmp = File.createTempFile("cfhc-prevrank-roundtrip", ".cfb");
        try {
            assertTrue(league.saveLeague(tmp));
            League loaded = new League(tmp,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP,
                    true);
            loaded.rebuildScheduleIfNeeded();
            Team loadedUser = loaded.getTeamList().get(0);
            assertEquals("prevRank must round-trip through the save", 7, loadedUser.getPrevRankTeamPollScore());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }
}
