package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.*;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ArchetypeGameplayBonusTest {

    private Game game;
    private League league;
    private Team homeTeam;
    private Team awayTeam;

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
        homeTeam = league.getTeamList().get(0);
        awayTeam = league.getTeamList().get(1);
        game = new Game(homeTeam, awayTeam, "Arch Bonus Test");
    }

    private Object invokeBonus(String methodName, Object... args) throws Exception {
        for (Method m : Game.class.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] paramTypes = m.getParameterTypes();
            if (paramTypes.length != args.length) continue;
            boolean match = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) continue;
                Class<?> c = args[i].getClass();
                if (paramTypes[i] == int.class) {
                    if (c != Integer.class) { match = false; break; }
                } else if (paramTypes[i] == double.class) {
                    if (c != Double.class) { match = false; break; }
                } else if (paramTypes[i] == boolean.class) {
                    if (c != Boolean.class) { match = false; break; }
                } else if (!paramTypes[i].isAssignableFrom(c)) {
                    match = false; break;
                }
            }
            if (!match) continue;
            m.setAccessible(true);
            return m.invoke(game, args);
        }
        throw new NoSuchMethodException(methodName + " with given params not found in Game");
    }

    private int intBonus(String methodName, Object... args) throws Exception {
        return (int) invokeBonus(methodName, args);
    }

    private double doubleBonus(String methodName, Object... args) throws Exception {
        return (double) invokeBonus(methodName, args);
    }

    @Test
    public void bonus_pocketPasserCompletion() throws Exception {
        PlayerQB qb = homeTeam.getQB(0);
        qb.archetypeTag = Archetypes.QB_POCKET;
        assertEquals(10, intBonus("getArchetypeCompletionBonus", qb, 10));
        assertEquals(0, intBonus("getArchetypeCompletionBonus", qb, 25));
    }

    @Test
    public void bonus_scramblerRush() throws Exception {
        PlayerQB qb = homeTeam.getQB(0);
        qb.archetypeTag = Archetypes.QB_SCRAMBLER;
        assertEquals(1, intBonus("getArchetypeScrambleBonus", qb));
        PlayerQB noArch = new PlayerQB("Test", 1, 3, homeTeam);
        noArch.archetypeTag = "";
        assertEquals(0, intBonus("getArchetypeScrambleBonus", noArch));
    }

    @Test
    public void bonus_speedBackRush() throws Exception {
        PlayerRB rb = homeTeam.getRB(0);
        rb.archetypeTag = Archetypes.RB_SPEED;
        assertEquals(1, intBonus("getArchetypeRushBonus", rb));
        PlayerRB noArch = new PlayerRB("Test", 1, 3, homeTeam);
        noArch.archetypeTag = "";
        assertEquals(0, intBonus("getArchetypeRushBonus", noArch));
    }

    @Test
    public void bonus_powerBackBrokenTackle() throws Exception {
        PlayerRB rb = homeTeam.getRB(0);
        rb.archetypeTag = Archetypes.RB_POWER;
        assertEquals(3, intBonus("getArchetypeBrokenTackleBonus", rb));
        PlayerRB noArch = new PlayerRB("Test", 1, 3, homeTeam);
        noArch.archetypeTag = "";
        assertEquals(0, intBonus("getArchetypeBrokenTackleBonus", noArch));
    }

    @Test
    public void bonus_receivingBackDropReduction() throws Exception {
        PlayerRB rb = homeTeam.getRB(0);
        rb.archetypeTag = Archetypes.RB_RECEIVING;
        assertEquals(10, intBonus("getArchetypeDropReduction", rb));
    }

    @Test
    public void bonus_receivingTeDropReduction() throws Exception {
        PlayerTE te = homeTeam.getTE(0);
        te.archetypeTag = Archetypes.TE_RECEIVING;
        assertEquals(10, intBonus("getArchetypeDropReduction", te));
    }

    @Test
    public void bonus_routeRunnerDropReduction() throws Exception {
        PlayerWR wr = homeTeam.getWR(0);
        wr.archetypeTag = Archetypes.WR_ROUTE_RUNNER;
        assertEquals(10, intBonus("getArchetypeDropReduction", wr));
    }

    @Test
    public void bonus_slotReceiverYac() throws Exception {
        PlayerWR wr = homeTeam.getWR(0);
        wr.archetypeTag = Archetypes.WR_SLOT;
        assertEquals(5, intBonus("getArchetypeYacBonus", wr));
        wr.archetypeTag = Archetypes.WR_DEEP_THREAT;
        assertEquals(0, intBonus("getArchetypeYacBonus", wr));
    }

    @Test
    public void bonus_shutdownCornerDeflection() throws Exception {
        PlayerCB cb = homeTeam.getCB(0);
        cb.archetypeTag = Archetypes.CB_SHUTDOWN;
        assertEquals(10, intBonus("getArchetypeDeflectionBonus", cb));
    }

    @Test
    public void bonus_speedCbDeepRecovery() throws Exception {
        PlayerCB cb = homeTeam.getCB(0);
        cb.archetypeTag = Archetypes.CB_SPEED;
        assertEquals(5, intBonus("getArchetypeDeepRecoveryBonus", cb));
    }

    @Test
    public void bonus_physicalCbPress() throws Exception {
        PlayerCB cb = homeTeam.getCB(0);
        cb.archetypeTag = Archetypes.CB_PHYSICAL;
        assertEquals(3, intBonus("getArchetypePressBonus", cb));
    }

    @Test
    public void bonus_ballHawkInt() throws Exception {
        PlayerS s = homeTeam.getS(0);
        s.archetypeTag = Archetypes.S_BALL_HAWK;
        double base = 0.05;
        assertEquals(base * 0.20, doubleBonus("getArchetypeIntBonus", s, base), 1e-9);
        PlayerS noArch = new PlayerS("Test", 1, 3, homeTeam);
        noArch.archetypeTag = "";
        assertEquals(0.0, doubleBonus("getArchetypeIntBonus", noArch, base), 1e-9);
    }

    @Test
    public void bonus_runSupportSafety() throws Exception {
        PlayerS s = homeTeam.getS(0);
        s.archetypeTag = Archetypes.S_RUN_SUPPORT;
        assertEquals(3, intBonus("getArchetypeRunStopBonus", s));
    }

    @Test
    public void bonus_passRusherDl() throws Exception {
        PlayerDL dl = homeTeam.getDL(0);
        dl.archetypeTag = Archetypes.DL_PASS_RUSHER;
        assertEquals(3, intBonus("getArchetypePassRushBonus", dl));
    }

    @Test
    public void bonus_runStopperDl() throws Exception {
        PlayerDL dl = homeTeam.getDL(0);
        dl.archetypeTag = Archetypes.DL_RUN_STOPPER;
        assertEquals(3, intBonus("getArchetypeRunStopBonus", dl));
    }

    @Test
    public void bonus_coverageLb() throws Exception {
        PlayerLB lb = homeTeam.getLB(0);
        lb.archetypeTag = Archetypes.LB_COVERAGE;
        assertEquals(3, intBonus("getArchetypeCoverageBonus", lb));
    }

    @Test
    public void bonus_blitzerLb() throws Exception {
        PlayerLB lb = homeTeam.getLB(0);
        lb.archetypeTag = Archetypes.LB_BLITZER;
        assertEquals(3, intBonus("getArchetypeBlitzBonus", lb));
    }

    @Test
    public void bonus_passProtectorOl() throws Exception {
        PlayerOL ol = homeTeam.getOL(0);
        ol.archetypeTag = Archetypes.OL_PASS_PROTECTOR;
        assertEquals(3, intBonus("getArchetypePassProtectBonus", ol));
    }

    @Test
    public void bonus_runBlockerOl() throws Exception {
        PlayerOL ol = homeTeam.getOL(0);
        ol.archetypeTag = Archetypes.OL_RUN_BLOCKER;
        assertEquals(3, intBonus("getArchetypeRunBlockBonus", ol));
    }

    @Test
    public void bonus_maulerOl() throws Exception {
        PlayerOL ol = homeTeam.getOL(0);
        ol.archetypeTag = Archetypes.OL_MAULER;
        assertEquals(2, intBonus("getArchetypeRunBlockBonus", ol));
    }

    @Test
    public void bonus_powerKicker() throws Exception {
        PlayerK k = homeTeam.getK(0);
        k.archetypeTag = Archetypes.K_POWER;
        assertEquals(5, intBonus("getArchetypeFgRangeBonus", k));
    }

    @Test
    public void bonus_accurateKicker() throws Exception {
        PlayerK k = homeTeam.getK(0);
        k.archetypeTag = Archetypes.K_ACCURATE;
        assertEquals(10, intBonus("getArchetypeFgAccBonus", k, 60));
        assertEquals(0, intBonus("getArchetypeFgAccBonus", k, 72));
    }

    @Test
    public void bonus_nullPlayerReturnsZero() throws Exception {
        PlayerRB rb = new PlayerRB("Test", 1, 3, homeTeam);
        rb.archetypeTag = "";
        assertEquals(0, intBonus("getArchetypeDropReduction", rb));
    }
}
