package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CoachSkillsTest {
    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void getRank_returnsZeroForDefaultPacked() {
        assertEquals(0, CoachSkills.getRank(0, CoachSkills.RECRUITING));
        assertEquals(0, CoachSkills.getRank(0, CoachSkills.DEVELOPMENT));
        assertEquals(0, CoachSkills.getRank(0, CoachSkills.GAME_PREP));
        assertEquals(0, CoachSkills.getRank(0, CoachSkills.DISCIPLINE_CULTURE));
        assertEquals(0, CoachSkills.getRank(0, CoachSkills.NIL_MARKETING));
    }

    @Test
    public void getRank_extractsPackedBits() {
        int packed = CoachSkills.withRank(0, CoachSkills.RECRUITING, 3);
        packed = CoachSkills.withRank(packed, CoachSkills.DEVELOPMENT, 1);
        packed = CoachSkills.withRank(packed, CoachSkills.NIL_MARKETING, 2);

        assertEquals(3, CoachSkills.getRank(packed, CoachSkills.RECRUITING));
        assertEquals(1, CoachSkills.getRank(packed, CoachSkills.DEVELOPMENT));
        assertEquals(0, CoachSkills.getRank(packed, CoachSkills.GAME_PREP));
        assertEquals(0, CoachSkills.getRank(packed, CoachSkills.DISCIPLINE_CULTURE));
        assertEquals(2, CoachSkills.getRank(packed, CoachSkills.NIL_MARKETING));
    }

    @Test
    public void withRank_clampsRankToMax() {
        int packed = CoachSkills.withRank(0, CoachSkills.RECRUITING, 99);
        assertEquals(3, CoachSkills.getRank(packed, CoachSkills.RECRUITING));
    }

    @Test
    public void withRank_clampsRankToMin() {
        int packed = CoachSkills.withRank(0, CoachSkills.DEVELOPMENT, -5);
        assertEquals(0, CoachSkills.getRank(packed, CoachSkills.DEVELOPMENT));
    }

    @Test
    public void withRank_ignoresInvalidBranch() {
        int packed = CoachSkills.withRank(42, 99, 2);
        assertEquals(42, packed);
    }

    @Test
    public void getRank_returnsZeroForInvalidBranch() {
        assertEquals(0, CoachSkills.getRank(42, 99));
    }

    @Test
    public void getRank_branchesAreIndependent() {
        int packed = 0;
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            packed = CoachSkills.withRank(packed, b, b % 4);
        }
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            assertEquals(b % 4, CoachSkills.getRank(packed, b));
        }
    }

    @Test
    public void costForNextRank_returnsFiniteValuesBelowMax() {
        assertEquals(40, CoachSkills.costForNextRank(0));
        assertEquals(78, CoachSkills.costForNextRank(1));
        assertEquals(116, CoachSkills.costForNextRank(2));
    }

    @Test
    public void costForNextRank_returnsMaxAtMaxRank() {
        assertEquals(Integer.MAX_VALUE, CoachSkills.costForNextRank(3));
        assertEquals(Integer.MAX_VALUE, CoachSkills.costForNextRank(4));
    }

    @Test
    public void branchTitle_returnsExpectedStrings() {
        assertEquals("Recruiting pitch", CoachSkills.branchTitle(CoachSkills.RECRUITING));
        assertEquals("Player development", CoachSkills.branchTitle(CoachSkills.DEVELOPMENT));
        assertEquals("Game prep", CoachSkills.branchTitle(CoachSkills.GAME_PREP));
        assertEquals("Discipline & culture", CoachSkills.branchTitle(CoachSkills.DISCIPLINE_CULTURE));
        assertEquals("NIL & boosters", CoachSkills.branchTitle(CoachSkills.NIL_MARKETING));
        assertEquals("Skill", CoachSkills.branchTitle(99));
    }

    @Test
    public void branchBlurb_returnsExpectedStrings() {
        assertTrue(CoachSkills.branchBlurb(CoachSkills.RECRUITING).contains("recruiting"));
        assertTrue(CoachSkills.branchBlurb(CoachSkills.DEVELOPMENT).contains("attribute"));
        assertTrue(CoachSkills.branchBlurb(CoachSkills.GAME_PREP).contains("coaching"));
        assertTrue(CoachSkills.branchBlurb(CoachSkills.DISCIPLINE_CULTURE).contains("discipline"));
        assertTrue(CoachSkills.branchBlurb(CoachSkills.NIL_MARKETING).contains("collective"));
        assertEquals("", CoachSkills.branchBlurb(99));
    }

    @Test
    public void buildProgramSummary_containsAllBranches() {
        Team t = newTeam();
        t.teamFacilities = 3;
        t.nilCollectiveLevel = 2;
        staff.HeadCoach hc = new staff.HeadCoach("Test Coach", t);
        hc.coachSkillRanksBits = CoachSkills.withRank(0, CoachSkills.RECRUITING, 3);
        hc.coachSkillRanksBits = CoachSkills.withRank(hc.coachSkillRanksBits, CoachSkills.NIL_MARKETING, 1);

        String summary = CoachSkills.buildProgramSummary(t, hc);
        assertTrue(summary.contains("Training facilities: L3"));
        assertTrue(summary.contains("Tier 2"));
        assertTrue(summary.contains("Recruiting pitch"));
        assertTrue(summary.contains("Player development"));
        assertTrue(summary.contains("Game prep"));
        assertTrue(summary.contains("Discipline & culture"));
        assertTrue(summary.contains("NIL & boosters"));
        assertTrue(summary.contains("rank 3/3"));
        assertTrue(summary.contains("rank 1/3"));
    }

    @Test
    public void branchPickerLabel_containsTitleAndRank() {
        int packed = CoachSkills.withRank(0, CoachSkills.GAME_PREP, 2);
        String label = CoachSkills.branchPickerLabel(packed, CoachSkills.GAME_PREP);
        assertTrue(label.contains("Game prep"));
        assertTrue(label.contains("Rank 2/3"));
    }

    @Test
    public void branchPickerLabel_showsMaxForMaxRank() {
        int packed = CoachSkills.withRank(0, CoachSkills.DISCIPLINE_CULTURE, 3);
        String label = CoachSkills.branchPickerLabel(packed, CoachSkills.DISCIPLINE_CULTURE);
        assertTrue(label.contains("Rank 3/3"));
    }

    @Test
    public void buildProgramSummary_includesXpCost() {
        Team t = newTeam();
        staff.HeadCoach hc = new staff.HeadCoach("Test Coach", t);
        hc.coachSkillRanksBits = 0;

        String summary = CoachSkills.buildProgramSummary(t, hc);
        assertTrue(summary.contains("next: 40 XP"));
    }

    private Team newTeam() {
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        return league.getTeamList().get(0);
    }
}
