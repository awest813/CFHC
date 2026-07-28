package positions;

import simulation.FileSystemResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class ArchetypeTest {

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
    public void qb_assignsArchetype() {
        PlayerQB qb = new PlayerQB("Test QB", 1, 3, team);
        assertNotNull(qb.archetypeTag);
        assertFalse(qb.archetypeTag.isEmpty());
        assertTrue(qb.archetypeTag.startsWith("POCKET") || qb.archetypeTag.startsWith("SCRAMBLER")
                || qb.archetypeTag.startsWith("FIELD") || qb.archetypeTag.startsWith("DUAL"));
    }

    @Test
    public void rb_assignsArchetype() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        assertNotNull(rb.archetypeTag);
        assertFalse(rb.archetypeTag.isEmpty());
        assertTrue(rb.archetypeTag.equals(Archetypes.RB_SPEED)
                || rb.archetypeTag.equals(Archetypes.RB_POWER)
                || rb.archetypeTag.equals(Archetypes.RB_RECEIVING));
    }

    @Test
    public void wr_assignsArchetype() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        assertNotNull(wr.archetypeTag);
        assertFalse(wr.archetypeTag.isEmpty());
    }

    @Test
    public void allPositions_assignArchetype() {
        PlayerQB qb = new PlayerQB("Q", 1, 3, team);
        PlayerRB rb = new PlayerRB("R", 1, 3, team);
        PlayerWR wr = new PlayerWR("W", 1, 3, team);
        PlayerTE te = new PlayerTE("T", 1, 3, team);
        PlayerOL ol = new PlayerOL("O", 1, 3, team);
        PlayerDL dl = new PlayerDL("D", 1, 3, team);
        PlayerLB lb = new PlayerLB("L", 1, 3, team);
        PlayerCB cb = new PlayerCB("C", 1, 3, team);
        PlayerS s = new PlayerS("S", 1, 3, team);
        PlayerK k = new PlayerK("K", 1, 3, team);

        assertFalse(qb.archetypeTag.isEmpty());
        assertFalse(rb.archetypeTag.isEmpty());
        assertFalse(wr.archetypeTag.isEmpty());
        assertFalse(te.archetypeTag.isEmpty());
        assertFalse(ol.archetypeTag.isEmpty());
        assertFalse(dl.archetypeTag.isEmpty());
        assertFalse(lb.archetypeTag.isEmpty());
        assertFalse(cb.archetypeTag.isEmpty());
        assertFalse(s.archetypeTag.isEmpty());
        assertFalse(k.archetypeTag.isEmpty());
    }

    @Test
    public void archetypeCaps_enforcedAfterConstruction() {
        int attempts = 0;
        boolean capFound = false;
        while (attempts < 100 && !capFound) {
            PlayerQB qb = new PlayerQB("Cap Test", 1, 3, team);
            int[] caps = Archetypes.getCaps("QB", qb.archetypeTag);
            for (int i = 0; i < 4; i++) {
                int[] attrs = {qb.ratAttr1, qb.ratAttr2, qb.ratAttr3, qb.ratAttr4};
                if (caps[i] < 99 && attrs[i] <= caps[i]) {
                    capFound = true;
                }
            }
            attempts++;
        }
        assertTrue("At least one archetype cap below 99 should be observed", capFound);
    }

    @Test
    public void archetypeCaps_enforcedAfterProgression() {
        PlayerRB rb = new PlayerRB("Prog Cap", 1, 3, team);
        int[] caps = Archetypes.getCaps("RB", rb.archetypeTag);
        rb.ratAttr1 = 99;
        rb.ratAttr2 = 99;
        rb.ratAttr3 = 99;
        rb.ratAttr4 = 99;
        rb.ratOvr = rb.getOverall();
        rb.ratPot = 99;
        rb.genericAdvanceSeason();
        assertTrue(rb.ratAttr1 <= caps[0]);
        assertTrue(rb.ratAttr2 <= caps[1]);
        assertTrue(rb.ratAttr3 <= caps[2]);
        assertTrue(rb.ratAttr4 <= caps[3]);
    }

    @Test
    public void displayName_returnsNonEmpty() {
        String[] allTags = {
                Archetypes.QB_POCKET, Archetypes.QB_SCRAMBLER, Archetypes.QB_FIELD_GENERAL, Archetypes.QB_DUAL_THREAT,
                Archetypes.RB_SPEED, Archetypes.RB_POWER, Archetypes.RB_RECEIVING,
                Archetypes.WR_DEEP_THREAT, Archetypes.WR_ROUTE_RUNNER, Archetypes.WR_SLOT,
                Archetypes.TE_BLOCKING, Archetypes.TE_RECEIVING, Archetypes.TE_HYBRID,
                Archetypes.OL_RUN_BLOCKER, Archetypes.OL_PASS_PROTECTOR, Archetypes.OL_MAULER,
                Archetypes.DL_RUN_STOPPER, Archetypes.DL_PASS_RUSHER, Archetypes.DL_NOSE,
                Archetypes.LB_RUN_STOPPER, Archetypes.LB_COVERAGE, Archetypes.LB_BLITZER,
                Archetypes.CB_SHUTDOWN, Archetypes.CB_SPEED, Archetypes.CB_PHYSICAL,
                Archetypes.S_BALL_HAWK, Archetypes.S_RUN_SUPPORT, Archetypes.S_HYBRID,
                Archetypes.K_POWER, Archetypes.K_ACCURATE
        };
        for (String tag : allTags) {
            String display = Archetypes.displayName(tag);
            assertNotNull(display);
            assertFalse(display.isEmpty());
            assertFalse(display.equals("Balanced"));
        }
    }

    @Test
    public void displayName_emptyTag_returnsBalanced() {
        assertEquals("Balanced", Archetypes.displayName(""));
        assertEquals("Balanced", Archetypes.displayName(null));
    }

    @Test
    public void multipliers_validForAllArchetypes() {
        String[] positions = {"QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"};
        for (String pos : positions) {
            String[] archs = Archetypes.getArchetypesForPosition(pos);
            for (String arch : archs) {
                double[] mults = Archetypes.getMultipliers(pos, arch);
                assertEquals(4, mults.length);
                for (double m : mults) {
                    assertTrue(m > 0);
                }
                int[] caps = Archetypes.getCaps(pos, arch);
                assertEquals(4, caps.length);
                for (int c : caps) {
                    assertTrue(c > 0 && c <= 99);
                }
            }
        }
    }

    @Test
    public void getArchetypeMultipliers_returnsFromPlayer() {
        PlayerWR wr = new PlayerWR("Mult Test", 1, 3, team);
        double[] mults = wr.getArchetypeMultipliers();
        assertNotNull(mults);
        assertEquals(4, mults.length);
        for (double m : mults) {
            assertTrue(m > 0);
        }
    }

    @Test
    public void getArchetypeDisplayName_returnsFromPlayer() {
        PlayerQB qb = new PlayerQB("Disp Test", 1, 3, team);
        String display = qb.getArchetypeDisplayName();
        assertNotNull(display);
        assertFalse(display.isEmpty());
    }

    @Test
    public void saveLoadRoundTrip_preservesArchetype() {
        PlayerQB original = new PlayerQB("Save Test", 1, 3, team);
        String originalTag = original.archetypeTag;

        var record = original.toRecord();
        assertEquals(originalTag, record.archetypeTag());

        PlayerQB loaded = new PlayerQB(team, record);
        assertEquals(originalTag, loaded.archetypeTag);
    }

    @Test
    public void transferPlayer_preservesArchetype() {
        PlayerRB rb = new PlayerRB("Transfer Src", 2, 4, team);
        String tag = rb.archetypeTag;
        PlayerRB dest = new PlayerRB(rb, team);
        assertEquals(tag, dest.archetypeTag);
    }

    @Test
    public void customPlayerConstructor_assignsArchetype() {
        PlayerQB custom = new PlayerQB("Custom QB", 1, 3, team, true);
        assertNotNull(custom.archetypeTag);
        assertFalse(custom.archetypeTag.isEmpty());
    }

    @Test
    public void assignArchetype_deterministicForSameAttributes() {
        String a1 = Archetypes.assignArchetype("QB", 90, 90, 50, 50);
        String a2 = Archetypes.assignArchetype("QB", 90, 90, 50, 50);
        assertEquals(a1, a2);

        String speed = Archetypes.assignArchetype("RB", 90, 80, 60, 50);
        assertEquals(Archetypes.RB_SPEED, speed);

        String power = Archetypes.assignArchetype("RB", 60, 70, 90, 50);
        assertEquals(Archetypes.RB_POWER, power);
    }

    @Test
    public void pocketPasser_hasCorrectCaps() {
        int[] caps = Archetypes.getCaps("QB", Archetypes.QB_POCKET);
        assertEquals(99, caps[0]);
        assertEquals(99, caps[1]);
        assertTrue(caps[2] < 99);
        assertTrue(caps[3] < 99);
    }

    @Test
    public void scrambler_hasCorrectCaps() {
        int[] caps = Archetypes.getCaps("QB", Archetypes.QB_SCRAMBLER);
        assertTrue(caps[0] < 99);
        assertEquals(99, caps[2]);
        assertEquals(99, caps[3]);
    }

    @Test
    public void progression_highMultiplierAttr_growsMoreThanLow() {
        PlayerRB rb = new PlayerRB("Prog Bias", 1, 3, team);
        double[] mults = Archetypes.getMultipliers("RB", rb.archetypeTag);
        int bestIdx = 0;
        int worstIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (mults[i] > mults[bestIdx]) bestIdx = i;
            if (mults[i] < mults[worstIdx]) worstIdx = i;
        }
        if (mults[bestIdx] == mults[worstIdx]) return;

        int bestGrowth = 0;
        int worstGrowth = 0;
        int[] attrs = {rb.ratAttr1, rb.ratAttr2, rb.ratAttr3, rb.ratAttr4};
        int[] caps = Archetypes.getCaps("RB", rb.archetypeTag);

        for (int trial = 0; trial < 200; trial++) {
            int[] startAttrs = {40, 40, 40, 40};
            rb.ratAttr1 = startAttrs[0];
            rb.ratAttr2 = startAttrs[1];
            rb.ratAttr3 = startAttrs[2];
            rb.ratAttr4 = startAttrs[3];
            rb.ratOvr = rb.getOverall();
            rb.ratPot = 90;
            rb.stats[1] = 10;
            rb.stats[2] = 8;
            rb.genericAdvanceSeason();
            int[] endAttrs = {rb.ratAttr1, rb.ratAttr2, rb.ratAttr3, rb.ratAttr4};
            bestGrowth += (endAttrs[bestIdx] - startAttrs[bestIdx]);
            worstGrowth += (endAttrs[worstIdx] - startAttrs[worstIdx]);
        }
        assertTrue("Primary archetype attr should grow more over many seasons (best="
                + bestGrowth + " worst=" + worstGrowth + " archetype=" + rb.archetypeTag + ")",
                bestGrowth > worstGrowth);
    }

    @Test
    public void progression_primaryAttrReachesHigherValueThanCapped() {
        PlayerQB qb = new PlayerQB("Cap Grow", 1, 3, team);
        double[] mults = Archetypes.getMultipliers("QB", qb.archetypeTag);
        int highIdx = 0;
        int lowIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (mults[i] > mults[highIdx]) highIdx = i;
            if (mults[i] < mults[lowIdx]) lowIdx = i;
        }
        if (mults[highIdx] == mults[lowIdx]) return;

        int highMax = 0;
        int lowMax = 0;
        for (int trial = 0; trial < 100; trial++) {
            qb.ratAttr1 = 50; qb.ratAttr2 = 50; qb.ratAttr3 = 50; qb.ratAttr4 = 50;
            qb.ratOvr = qb.getOverall();
            qb.ratPot = 95;
            for (int season = 0; season < 4; season++) {
                qb.stats[1] = 10;
                qb.stats[2] = 8;
                qb.genericAdvanceSeason();
            }
            int[] attrs = {qb.ratAttr1, qb.ratAttr2, qb.ratAttr3, qb.ratAttr4};
            if (attrs[highIdx] > highMax) highMax = attrs[highIdx];
            if (attrs[lowIdx] > lowMax) lowMax = attrs[lowIdx];
        }
        assertTrue("High-multiplier attr should reach higher values than low-multiplier (high="
                + highMax + " low=" + lowMax + " archetype=" + qb.archetypeTag + ")",
                highMax >= lowMax);
    }

    @Test
    public void midSeasonProgression_usesArchetypeMultipliers() {
        PlayerWR wr = new PlayerWR("Mid Prog", 1, 3, team);
        double[] mults = Archetypes.getMultipliers("WR", wr.archetypeTag);
        int highIdx = 0;
        int lowIdx = 0;
        for (int i = 1; i < 4; i++) {
            if (mults[i] > mults[highIdx]) highIdx = i;
            if (mults[i] < mults[lowIdx]) lowIdx = i;
        }
        if (mults[highIdx] == mults[lowIdx]) return;

        int highGrowth = 0;
        int lowGrowth = 0;

        for (int trial = 0; trial < 200; trial++) {
            wr.ratAttr1 = 40; wr.ratAttr2 = 40; wr.ratAttr3 = 40; wr.ratAttr4 = 40;
            wr.ratOvr = wr.getOverall();
            wr.ratPot = 90;
            wr.stats[1] = 10;
            wr.stats[2] = 8;
            wr.midSeasonProgression();
            int[] endAttrs = {wr.ratAttr1, wr.ratAttr2, wr.ratAttr3, wr.ratAttr4};
            highGrowth += (endAttrs[highIdx] - 40);
            lowGrowth += (endAttrs[lowIdx] - 40);
        }
        assertTrue("Mid-season high-multiplier attr should grow more (high="
                + highGrowth + " low=" + lowGrowth + " archetype=" + wr.archetypeTag + ")",
                highGrowth > lowGrowth);
    }

    @Test
    public void balancedArchetype_growsEvenly() {
        double[] balanced = Archetypes.getMultipliers("QB", "");
        assertEquals(1.0, balanced[0], 0.01);
        assertEquals(1.0, balanced[1], 0.01);
        assertEquals(1.0, balanced[2], 0.01);
        assertEquals(1.0, balanced[3], 0.01);
    }
}
