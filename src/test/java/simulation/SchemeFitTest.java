package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import positions.*;
import staff.DC;
import staff.OC;

import static org.junit.Assert.*;

public class SchemeFitTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
    public void oc_hasSchemeArchetypesAfterCreation() {
        OC oc = team.getOC();
        if (oc == null) return;
        assertNotNull(oc.schemeArchetypes);
        assertFalse(oc.schemeArchetypes.isEmpty());
    }

    @Test
    public void dc_hasSchemeArchetypesAfterCreation() {
        DC dc = team.getDC();
        if (dc == null) return;
        assertNotNull(dc.schemeArchetypes);
        assertFalse(dc.schemeArchetypes.isEmpty());
    }

    @Test
    public void hasSchemeFit_returnsTrueForMatchingArchetype() {
        OC oc = team.getOC();
        if (oc == null) return;
        oc.schemeArchetypes = Archetypes.QB_POCKET + "," + Archetypes.RB_SPEED;
        assertTrue(oc.hasSchemeFit(Archetypes.QB_POCKET));
        assertTrue(oc.hasSchemeFit(Archetypes.RB_SPEED));
        assertFalse(oc.hasSchemeFit(Archetypes.QB_SCRAMBLER));
    }

    @Test
    public void hasSchemeFit_returnsFalseForEmptyOrNull() {
        OC oc = team.getOC();
        if (oc == null) return;
        assertFalse(oc.hasSchemeFit(null));
        assertFalse(oc.hasSchemeFit(""));
        oc.schemeArchetypes = "";
        assertFalse(oc.hasSchemeFit(Archetypes.QB_POCKET));
    }

    @Test
    public void getSchemeFit_returnsTrueForMatchingPlayer() {
        OC oc = team.getOC();
        if (oc == null) return;
        oc.schemeArchetypes = Archetypes.QB_POCKET;

        PlayerQB qb = new PlayerQB("Scheme QB", 1, 3, team);
        qb.archetypeTag = Archetypes.QB_POCKET;
        assertTrue(qb.getSchemeFit());
    }

    @Test
    public void getSchemeFit_returnsFalseForNonMatchingPlayer() {
        OC oc = team.getOC();
        if (oc == null) return;
        oc.schemeArchetypes = Archetypes.QB_SCRAMBLER;

        PlayerQB qb = new PlayerQB("Scheme QB", 1, 3, team);
        qb.archetypeTag = Archetypes.QB_POCKET;
        assertFalse(qb.getSchemeFit());
    }

    @Test
    public void defensivePlayer_usesDCScheme() {
        DC dc = team.getDC();
        if (dc == null) return;
        dc.schemeArchetypes = Archetypes.LB_COVERAGE;

        PlayerLB lb = new PlayerLB("Scheme LB", 1, 3, team);
        lb.archetypeTag = Archetypes.LB_COVERAGE;
        assertTrue(lb.getSchemeFit());
    }

    @Test
    public void schemeFitBonus_positiveWhenMatch() {
        OC oc = team.getOC();
        if (oc == null) return;
        oc.schemeArchetypes = Archetypes.WR_DEEP_THREAT;

        PlayerWR wr = new PlayerWR("Bonus WR", 1, 3, team);
        wr.archetypeTag = Archetypes.WR_DEEP_THREAT;
        assertEquals(5, wr.getSchemeFitBonus(true));
        assertEquals(0, wr.getSchemeFitBonus(false));
    }

    @Test
    public void progressionOff_higherWithSchemeFit() {
        OC oc = team.getOC();
        if (oc == null) return;

        PlayerQB matchQB = new PlayerQB("Match QB", 1, 3, team);
        PlayerQB missQB = new PlayerQB("Miss QB", 1, 3, team);
        matchQB.ratPot = 80;
        missQB.ratPot = 80;
        matchQB.archetypeTag = Archetypes.QB_POCKET;
        missQB.archetypeTag = Archetypes.QB_SCRAMBLER;

        oc.schemeArchetypes = Archetypes.QB_POCKET;

        int matchTotal = 0;
        int missTotal = 0;
        for (int i = 0; i < 500; i++) {
            matchTotal += matchQB.getProgressionOff();
            missTotal += missQB.getProgressionOff();
        }
        assertTrue("Scheme-fit player should have higher average progression (match="
                + matchTotal + " miss=" + missTotal + ")", matchTotal > missTotal);
    }

    @Test
    public void saveLoadRoundTrip_preservesSchemeArchetypes() {
        OC oc = team.getOC();
        if (oc == null) return;
        String original = oc.schemeArchetypes;

        StaffRecord record = oc.toRecord();
        assertEquals(original, record.schemeArchetypes());

        OC loaded = new OC(team, record);
        assertEquals(original, loaded.schemeArchetypes);
    }

    @Test
    public void dcSaveLoadRoundTrip_preservesSchemeArchetypes() {
        DC dc = team.getDC();
        if (dc == null) return;
        String original = dc.schemeArchetypes;

        StaffRecord record = dc.toRecord();
        assertEquals(original, record.schemeArchetypes());

        DC loaded = new DC(team, record);
        assertEquals(original, loaded.schemeArchetypes);
    }
}
