package simulation.rows;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import simulation.FileSystemResourceProvider;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

/**
 * Protocol tests: the row strings the engine produces for the Android list
 * pages must parse into the fields and row kinds the adapters expect.
 */
public class RowModelTest {

    private static League league;
    private static Team team;

    @BeforeClass
    public static void setUp() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        league.setPlatformResourceProvider(r);
        team = league.getTeamList().get(0);
        league.currentWeek = 1;
        league.playWeek();
    }

    @Test
    public void roster_hasSectionsAndRatedEntries() {
        int sections = 0, entries = 0;
        for (String line : team.getRoster()) {
            RosterRow row = RosterRow.parse(line);
            if (row.kind() == RowKind.SECTION) sections++;
            if (row.kind() == RowKind.ENTRY) {
                entries++;
                assertFalse("entry needs a name: " + line, row.name().isEmpty());
                assertTrue("rating should be numeric: " + line, row.rating().matches("\\d+"));
            }
        }
        assertTrue(sections >= 5);
        assertTrue(entries >= 40);
    }

    @Test
    public void rankings_entriesNameRealTeams() {
        Set<String> names = new HashSet<>();
        for (Team t : league.getTeamList()) names.add(t.getName());
        int entries = 0;
        for (String line : league.getTeamRankings()) {
            RankingRow row = RankingRow.parse(line);
            if (row.kind() != RowKind.ENTRY) continue;
            entries++;
            assertTrue("unknown team: " + line, names.contains(row.team()));
            assertTrue("record like W-L: " + line, row.record().matches("\\(?\\d+-\\d+.*"));
        }
        assertEquals(league.getTeamList().size(), entries);
    }

    @Test
    public void confStandings_haveConferenceHeaders() {
        int sections = 0, entries = 0;
        for (String line : league.getConfStandings()) {
            RankingRow row = RankingRow.parse(line);
            if (row.kind() == RowKind.SECTION) {
                sections++;
                assertTrue(row.team().endsWith("Conference"));
            }
            if (row.kind() == RowKind.ENTRY) entries++;
        }
        assertTrue(sections >= 5);
        assertTrue(entries >= league.getTeamList().size() - 20); // independents may sit outside conferences
    }

    @Test
    public void rosterStats_categoriesAreSections() {
        List<String> lines = team.getRosterStats();
        Set<String> categories = new HashSet<>();
        for (String line : lines) {
            StatLine row = StatLine.parse(line);
            if (row.kind() == RowKind.SECTION) categories.add(row.name());
            if (row.kind() == RowKind.ENTRY) assertFalse(row.name().isEmpty());
        }
        assertTrue(categories.containsAll(Set.of("Passing", "Rushing", "Receiving")));
    }

    @Test
    public void teamStats_haveValueLabelRank() {
        for (String line : team.getTeamStatsStrCSV().split("%\n")) {
            if (line.trim().isEmpty()) continue;
            TeamStatRow row = TeamStatRow.parse(line);
            assertFalse("label: " + line, row.label().isEmpty());
            assertFalse("value: " + line, row.value().isEmpty());
        }
    }

    @Test
    public void malformedLinesDegradeToBlanks() {
        assertEquals(RowKind.SPACER, RosterRow.parse(null).kind());
        assertEquals(RowKind.SPACER, RankingRow.parse("").kind());
        assertEquals("", StatLine.parse("Only").value(5));
        assertEquals("", TeamStatRow.parse("1").rank());
    }
}
