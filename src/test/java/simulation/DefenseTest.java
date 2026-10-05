package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.Player;
import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerLB;
import positions.PlayerS;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Defense: who gets credit, what the box score shows, interception returns and the award score. */
public class DefenseTest {

    private final FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    private League league;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(8675309L);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
    }

    @Test
    public void coverageMakesPicksMoreLikely() {
        int p = Game.LEAGUE_PRESSURE;
        assertTrue(Game.interceptionChance(84, p, 78, 95, 0) > Game.interceptionChance(84, p, 78, 65, 0));
        assertEquals(Game.INT_BASE_CHANCE, Game.interceptionChance(Game.INT_QB_PIVOT, p, Game.INT_SAFETY_PIVOT, Game.INT_COVERAGE_PIVOT, 0), 1e-9);
    }

    @Test
    public void interceptionReturnsAreShortWithTheOccasionalBreakaway() {
        int longest = 0;
        long total = 0;
        for (int i = 0; i < 4000; i++) {
            int y = Game.interceptionReturnYards(90);
            assertTrue(y >= 0);
            total += y;
            longest = Math.max(longest, y);
        }
        double avg = total / 4000.0;
        assertTrue("average return " + avg, avg > 8 && avg < 20);
        assertTrue("a fast DB can take one a long way: " + longest, longest >= 60);
    }

    @Test
    public void everyDefensivePlayIsCreditedAndShown() {
        List<Team> teams = league.getTeamList();
        int picks = 0, safetyPicks = 0, pickSixes = 0, safetyBreakups = 0, sacks = 0, cornerSacks = 0;
        for (int i = 0; i < 150; i++) {
            Team home = teams.get((i * 11) % teams.size());
            Team away = teams.get((i * 11 + 5) % teams.size());
            Game g = new Game(home, away, "Defense");
            g.playGame();
            String log = g.gameEventLog.toString();
            pickSixes += count(log, "returned the interception");

            for (Team t : new Team[]{home, away}) {
                List<String> box = t == home ? g.homeDefenseStats : g.awayDefenseStats;
                for (Player p : t.getAllPlayers()) {
                    if (!Player.defensePos.contains(p.position)) continue;
                    boolean played = p.gameTackles > 0 || p.gameSacks > 0 || p.gameInterceptions > 0
                            || p.gameFumbles > 0 || p.gameDefended > 0;
                    if (played) {
                        // Only tacklers used to be listed: a pick with no tackle went missing.
                        assertTrue(p.position + " " + p.name + " is in the box score", listed(box, p));
                    }
                    picks += p.gameInterceptions;
                    sacks += p.gameSacks;
                    if (p instanceof PlayerCB) cornerSacks += p.gameSacks;
                    if (p instanceof PlayerS) {
                        safetyBreakups += p.gameDefended;
                        safetyPicks += p.gameInterceptions;
                    }
                }
            }
        }
        assertTrue("picks happen (" + picks + ")", picks > 100);
        assertTrue("some are returned for touchdowns (" + pickSixes + ")", pickSixes > 0);
        // Safeties made ~17% of picks (FBS ~35%).
        assertTrue("safeties make " + safetyPicks + " of " + picks + " picks", safetyPicks > picks * 0.29);
        assertTrue("safeties break up passes (" + safetyBreakups + ")", safetyBreakups > 0);
        // A stale roll used to hand corners ~8% of sacks.
        assertTrue("corners rarely get sacks: " + cornerSacks + " of " + sacks, cornerSacks < sacks * 0.05);
    }

    private static boolean listed(List<String> box, Player p) {
        String prefix = p.getInitialName() + "," + p.team.getName() + "," + p.position + ",";
        for (String row : box) if (row.startsWith(prefix)) return true;
        return false;
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    @Test
    public void everyDefenderIsScoredTheSameWay() {
        Team team = league.getTeamList().get(0);
        PlayerDL dl = new PlayerDL("Award DL", 3, 4, team);
        PlayerLB lb = new PlayerLB("Award LB", 3, 4, team);
        PlayerS s = new PlayerS("Award S", 3, 4, team);
        PlayerCB cb = new PlayerCB("Award CB", 3, 4, team);
        for (Player p : new Player[]{dl, lb, s, cb}) {
            p.ratOvr = 85;
            p.recordTackles(60);
            p.recordSacks(4);
            p.recordInterceptions(3);
            p.recordFumblesRec(1);
            p.recordDefended(6);
        }
        // A lineman's sack was worth 425 against 25-35 a tackle, so linemen won
        // Defensive Player of the Year every season.
        assertEquals(dl.getHeismanScore(), lb.getHeismanScore());
        assertEquals(dl.getHeismanScore(), s.getHeismanScore());
        assertEquals(dl.getHeismanScore(), cb.getHeismanScore());

        int before = cb.getHeismanScore();
        cb.recordKOYards(600);
        assertEquals("kick returns are not defense", before, cb.getHeismanScore());

        int beforeSack = dl.getHeismanScore();
        dl.recordSacks(1);
        assertEquals(Player.DEF_AWARD_SACK, dl.getHeismanScore() - beforeSack);
    }
}
