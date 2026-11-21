package simulation;

import org.junit.Test;

import staff.HeadCoach;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import comparator.CompCoachScore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Screens read rankings, award watches and coach scores whenever they are opened.
 * Those reads must not draw random numbers (a reload would no longer match an
 * unbroken run, and merely opening a screen would change the season) and must
 * return the same value every call (sorting by a value that changes between
 * comparisons throws "Comparison method violates its general contract").
 */
public class UiReadsAreSideEffectFreeTest {

    private static League newLeague(long seed, boolean expandedPlayoffs) {
        SimRandom.pinNextSeed(seed);
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(r);
        league.expPlayoffs = expandedPlayoffs;
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        return league;
    }

    @Test
    public void rankingAndAwardReads_doNotTouchTheRng() {
        League league = newLeague(424242L, true);
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

    /**
     * Every engine read the Android and desktop screens make, at the preseason,
     * mid-season, each postseason round and each offseason step, on both playoff
     * formats: the RNG position, the team list and conference order, and the save
     * the league would write must all be unchanged. (Rankings sorted the league's
     * own team list, Bowl Watch re-ranked the league and zeroed banned teams' poll
     * scores, the Mock Draft shuffled with the season's stream, the coach database
     * rewrote a saved list, and building the recruiting board sorted the roster
     * and drew a prospect class from the season's stream.)
     */
    @Test
    public void everyScreenRead_leavesTheLeagueUnchanged_throughASeason() throws Exception {
        for (boolean expanded : new boolean[]{true, false}) {
            League league = newLeague(777L + (expanded ? 1 : 0), expanded);
            SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
            int r = league.regSeasonWeeks;
            int last = SeasonFlowOrder.recruitingWeek(r);
            for (int guard = 0; guard < 60 && league.currentWeek <= last; guard++) {
                int week = league.currentWeek;
                String before = fingerprint(league);
                readEverything(league);
                assertEquals("screen reads changed the league at week " + week
                        + (expanded ? "" : " (classic playoff)"), before, fingerprint(league));
                if (week == last) break;
                controller.advanceWeek();
            }
        }
    }

    /** The mock draft and the recruiting board come out the same however often they are built. */
    @Test
    public void mockDraftAndRecruitingBoard_areStableAcrossReads() {
        League league = newLeague(31337L, true);
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        while (league.currentWeek < SeasonFlowOrder.graduationWeek(league.regSeasonWeeks) + 1) controller.advanceWeek();
        assertArrayEquals(league.getMockDraftPlayersList(), league.getMockDraftPlayersList());
        assertEquals(SimulationFacade.buildRecruitingPayload(league.userTeam),
                SimulationFacade.buildRecruitingPayload(league.userTeam));
    }

    static void readEverything(League l) {
        for (int i = 0; i <= 22; i++) l.getTeamRankingsStr(i);
        for (int i = 0; i <= 4; i++) l.getLeagueHistoryStats(i);
        for (int i = 0; i <= 10; i++) l.getCoachDatabase(i);
        for (int i = 0; i <= 21; i++) l.getPlayerRankStr(i);
        for (int i = 0; i <= 12; i++) l.getAwardsWatch(i);
        l.getCoachDatabase();
        l.getTeamRankings();
        l.getConfStandings();
        l.getTeamListStr();
        l.getMockDraftPlayersList();
        l.getBowlGameWatchStr();
        l.getCCGsStr();
        l.getHeismanCeremonyStr();
        l.getDefensePOTYStr();
        l.getFreshmanCeremonyStr();
        l.getCoachAwardStr();
        l.getAllAmericanStr();
        l.getAllFreshmanStr();
        for (int c = 0; c < l.getConferences().size(); c++) l.getAllConfStr(c);
        l.getLeagueHistoryStr();
        l.getLeagueRecordsStr();
        l.getCoachListFired(80, "");
        l.getCoachPromotionList(80, 1.0, "");
        l.getRedshirts();
        SeasonPresentation.getPlayWeekLabel(l);
        SeasonPresentation.getSeasonWeekChipText(l);
        SeasonPresentation.getSeasonPhaseChipText(l);
        SeasonPresentation.getNextActionHint(l);
        Team user = l.userTeam;
        SimulationFacade.previewRecruitingBudget(user);
        SimulationFacade.buildRecruitingPayload(user);
        for (Team t : Arrays.asList(user, l.getTeamList().get(l.getTeamList().size() - 1))) {
            t.getTeamStatsStrCSV();
            t.getTeamRosterString();
            t.getTeamMoraleSnapshot();
            t.getGradPlayersList();
            t.getRecruitingClassRat();
            t.getTeamHistoryList();
            if (t.getHeadCoach() != null) t.getHeadCoach().getCoachScore();
        }
    }

    /** RNG position, team-list and conference order, and a hash of the save. */
    static String fingerprint(League l) throws Exception {
        StringBuilder order = new StringBuilder();
        for (Team t : l.getTeamList()) order.append(t.getName()).append('|');
        for (Conference c : l.getConferences()) {
            for (Team t : c.confTeams) order.append(t.getName()).append(';');
        }
        for (positions.Player p : l.userTeam.getAllPlayers()) order.append(p.name).append(',');
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(l.serializeSave());
        md.update(order.toString().getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
        return SimRandom.exportState() + " " + hex;
    }
}
