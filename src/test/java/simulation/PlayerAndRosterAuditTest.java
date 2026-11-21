package simulation;

import org.junit.Before;
import org.junit.Test;

import positions.Player;
import positions.PlayerQB;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingSessionData;

import static org.junit.Assert.*;

/**
 * Player development, signing day and coach-score defects found in the
 * recruiting / development / staff audit.
 */
public class PlayerAndRosterAuditTest {

    private League league;
    private SeasonController controller;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(20261121L);
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(r);
        Team user = league.getTeamList().get(0);
        user.setupUserCoach("Audit Coach");
        league.userTeam = user;
        controller = new SeasonController(league, GameUiBridge.NO_OP);
    }

    private void playToRecruitingGate() {
        for (int g = 0; g < 60 && !SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks); g++) {
            controller.advanceWeek();
        }
    }

    /**
     * A redshirt season's games (up to four, or before a medical redshirt) stayed in
     * the season stats and were counted again as part of the next season; and the
     * season label was only set when a player was created, so every later season
     * read "0" on the stats pages.
     */
    @Test
    public void newSeason_startsWithCleanLabelledStats_andCareerTotalsKeepRedshirtGames() {
        java.util.IdentityHashMap<Player, Integer> careerGamesBefore = new java.util.IdentityHashMap<>();
        playToRecruitingGate();
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) careerGamesBefore.put(p, p.getCareerGames());
        }
        controller.autoCompleteRecruiting();
        assertEquals(0, league.currentWeek);
        int checked = 0;
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertEquals(p.position + " " + p.name + " carried games into the new season", 0, p.getGames());
                assertEquals("season label", league.getYear(), p.getSeason());
                Integer before = careerGamesBefore.get(p);
                if (before != null) {
                    checked++;
                    assertEquals(p.name + " career games", (int) before, p.getCareerGames());
                }
            }
        }
        assertTrue(checked > 1000);
    }

    @Test
    public void seasonsPlayed_namesTheLastSeasonPlayed() {
        Player p = league.userTeam.getTeamQBs().get(0);
        p.careerStats.get(1)[0] = 2026;
        p.careerStats.get(1)[1] = 12;
        p.careerStats.get(2)[0] = 2027;
        p.careerStats.get(2)[1] = 11;
        assertEquals("2026 to 2027", p.getSeasonsPlayed());
    }

    /** "A medical redshirt has been granted" — seniors with one graduated anyway. */
    @Test
    public void medicalRedshirtSenior_staysForAnotherSeason() {
        Team user = league.userTeam;
        PlayerQB senior = user.getTeamQBs().get(user.getTeamQBs().size() - 1);
        senior.year = 4;
        senior.wasRedshirt = false;
        senior.isRedshirt = false;
        senior.isTransfer = false;
        senior.isMedicalRS = true;
        user.advanceSeasonPlayers();
        assertTrue("medical redshirt senior left", user.getTeamQBs().contains(senior));
        assertEquals(4, senior.year);
        assertTrue(senior.wasRedshirt);
        assertFalse(senior.isMedicalRS);
    }

    /**
     * Walk-ons that replace redshirts and sitting-out transfers used to be added
     * on top of a full roster (user rosters are never trimmed), pushing it to 76-78.
     * A position short of its minimum still always gets walk-ons.
     */
    @Test
    public void walkOns_replaceRedshirtsOnlyUnderTheCap_butAlwaysFillMinimums() {
        Team user = league.userTeam;
        while (user.getAllPlayers().size() < RosterRules.MAX_PLAYERS) {
            user.addPlayerWR(new positions.PlayerWR(league.getRandName(), 1, 3, user));
        }
        for (Player p : user.getTeamQBs()) p.isRedshirt = true;
        int shortfall = 0;
        for (String pos : new String[]{"QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"}) {
            int have = 0;
            for (Player p : user.getAllPlayers()) if (p.position.equals(pos)) have++;
            shortfall += Math.max(0, RosterRules.minForPosition(pos) - have);
        }
        user.recruitWalkOns();
        assertEquals("walk-ons for redshirts pushed a full roster past the cap",
                RosterRules.MAX_PLAYERS + shortfall, user.getAllPlayers().size());

        for (Player p : user.getTeamQBs()) p.isRedshirt = false;
        while (user.getTeamOLs().size() > 1) user.removePlayer(user.getTeamOLs().get(0));
        user.recruitWalkOns();
        assertEquals("a position below its minimum is always filled",
                RosterRules.MIN_OLS, user.getTeamOLs().size());
    }

    /**
     * The board keeps the spots signing day's walk-ons will need: with the last
     * open spots needed for a position below its minimum, only that position signs.
     */
    @Test
    public void recruitingBoard_reservesSpotsForPositionsBelowTheirMinimum() {
        playToRecruitingGate();
        league.recruitPlayers();
        RecruitingSessionData s = SimulationFacade.prepareRecruitingSession(league.userTeam);
        s.recruitingBudget = 1_000_000;
        // Fill the board with kickers until only the spots other positions need are left.
        RecruitingPlayerRecord kicker = null;
        for (int guard = 0; guard < 200; guard++) {
            kicker = null;
            for (RecruitingPlayerRecord rec : s.availKs) {
                if (s.signingBlockReason(rec) == null) { kicker = rec; break; }
            }
            if (kicker == null) break;
            s.recruitPlayer(kicker, false, 0.0);
        }
        RecruitingPlayerRecord anotherKicker = s.availKs.isEmpty() ? null : s.availKs.get(0);
        int open = RosterRules.MAX_PLAYERS - s.projectedRosterSize();
        if (anotherKicker != null && open > 0) {
            assertNotNull("a kicker took a spot a short position needs", s.signingBlockReason(anotherKicker));
            try {
                s.recruitPlayer(anotherKicker, false, 0.0);
                fail("recruitPlayer must refuse it too");
            } catch (IllegalStateException expected) {
                // reason shown by the UI
            }
        }
        // Whatever was signed, signing day stays within the cap.
        league.finishRecruitingSeason(s.buildRecruitsSaveData());
        assertTrue("roster " + league.userTeam.getAllPlayers().size(),
                league.userTeam.getAllPlayers().size() <= RosterRules.MAX_PLAYERS);
    }

    /**
     * From the second bowl week to the season summary the coach score dropped its
     * prestige part: a fixed "week &lt; 15" stopped the projection one round into a
     * 14-week calendar's postseason, before the change was applied (at the summary).
     */
    @Test
    public void coachScore_projectsPrestigeUntilTheOffseasonBooksIt() {
        int r = league.regSeasonWeeks;
        for (int g = 0; g < 60 && league.currentWeek < SeasonFlowOrder.nationalChampionshipWeek(r); g++) {
            controller.advanceWeek();
        }
        Team user = league.userTeam;
        int projected = user.calcSeasonPrestige()[0] - user.getTeamPrestige();
        int expected = projected * 10 + (user.getTeamStrengthOfWins() / 20) + 3 * user.getWins()
                - user.getLosses() + user.getConfPrestige();
        assertEquals(expected, user.getHeadCoach().getCoachScore());
        // After the summary books the season, the applied change is used instead.
        while (league.currentWeek <= SeasonFlowOrder.seasonSummaryWeek(r)) controller.advanceWeek();
        int applied = user.getTeamPrestige() - user.getTeamPrestigeStart();
        int expectedAfter = applied * 10 + (user.getTeamStrengthOfWins() / 20) + 3 * user.getWins()
                - user.getLosses() + user.getConfPrestige();
        assertEquals(expectedAfter, user.getHeadCoach().getCoachScore());
    }
}
