package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProgramSnapshotTest {

    @Test
    public void trendBands() {
        assertEquals(ProgramSnapshot.Trend.RISING, new ProgramSnapshot(0, 0, 0, 0, 70, 6, 0, 0f).trend());
        assertEquals(ProgramSnapshot.Trend.STABLE, new ProgramSnapshot(0, 0, 0, 0, 70, 5, 0, 0f).trend());
        assertEquals(ProgramSnapshot.Trend.STABLE, new ProgramSnapshot(0, 0, 0, 0, 70, -2, 0, 0f).trend());
        assertEquals(ProgramSnapshot.Trend.DECLINING, new ProgramSnapshot(0, 0, 0, 0, 70, -3, 0, 0f).trend());
    }

    @Test
    public void money() {
        assertEquals("$34.2M", ProgramSnapshot.formatMoney(34_200_000));
        assertEquals("$642K", ProgramSnapshot.formatMoney(642_000));
        assertEquals("$900", ProgramSnapshot.formatMoney(900));
    }

    @Test
    public void ofRealTeam() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        Team t = league.getTeamList().get(0);
        ProgramSnapshot p = ProgramSnapshot.of(t);
        assertEquals(t.getTeamPrestige(), p.prestige());
        assertEquals(t.getTeamPrestige() - t.getTeamPrestigeStart(), p.prestigeDelta());
        assertTrue(p.commits() >= 0);
    }
}
