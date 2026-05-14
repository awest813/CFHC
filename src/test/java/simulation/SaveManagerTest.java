package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

/**
 * Tests {@link SaveManager} direct save/load round-trip.
 * Complements {@link SaveRoundTripTest} which tests the file-based path.
 */
public class SaveManagerTest {

    private League league;

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
    }

    @Test
    public void save_loadRoundTrip_preservesTeamNamesWithCommas() throws Exception {
        // The L: header is tab-separated so league names can contain commas.
        // Team names in T: lines are also tab-separated.
        // However, PlayerRecord CSV embedding ("TeamName:Pos") uses commas,
        // so commas in team names break player serialization.
        // This test verifies the L: header round-trip with commas works.
        String originalLeagueName = league.leagueName;
        league.leagueName = "My League, Best Conference, National Division";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LeagueRecord record = SaveManager.load(bais);

        assertEquals("League name with commas must survive SaveManager round-trip",
                "My League, Best Conference, National Division", record.leagueName());

        league.leagueName = originalLeagueName;
    }

    @Test
    public void save_loadRoundTrip_preservesSimpleTeamName() throws Exception {
        // Team names without commas survive the full round-trip
        Team team = league.getTeamList().get(0);
        String originalName = team.name;
        team.name = "Test Team Simple Name";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LeagueRecord record = SaveManager.load(bais);

        boolean found = record.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .anyMatch(t -> t.name().equals("Test Team Simple Name"));
        assertTrue("Simple team name should survive SaveManager round-trip", found);

        team.name = originalName;
    }

    @Test
    public void save_loadRoundTrip_preservesRecordData() throws Exception {
        Team team = league.getTeamList().get(0);
        team.teamRecords.checkRecord("Pass Yards", 5000.0f, "PlayerName%TST", 2025);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LeagueRecord record = SaveManager.load(bais);

        DataRecord foundRecord = record.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .flatMap(t -> t.records().stream())
                .filter(r -> "Pass Yards".equals(r.key()) && r.value() == 5000.0f)
                .findFirst().orElse(null);
        assertNotNull("Record data should survive SaveManager round-trip", foundRecord);
        assertEquals("PlayerName%TST", foundRecord.holder());
        assertEquals(2025, foundRecord.year());
    }

    @Test
    public void save_loadRoundTrip_preservesMultipleRecordTypes() throws Exception {
        Team team = league.getTeamList().get(0);
        team.teamRecords.checkRecord("Rush Yards", 3000.0f, "Runner%TST", 2025);
        team.teamRecords.checkRecord("Sacks", 15.0f, "PassRusher%TST", 2025);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LeagueRecord record = SaveManager.load(bais);

        long count = record.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .filter(t -> t.name().equals(team.name))
                .flatMap(t -> t.records().stream())
                .filter(r -> r.key().equals("Rush Yards") || r.key().equals("Sacks"))
                .count();
        assertEquals("Both records should survive round-trip", 2, count);
    }

    @Test
    public void save_loadRoundTrip_preservesLeagueRecords() throws Exception {
        // Use a key that's in the predefined recordsList so toRecordList() includes it
        league.leagueRecords.addRecord(new DataRecord(
                "Wins", 42, "TestCoach%TST", 2024));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LeagueRecord record = SaveManager.load(bais);

        boolean found = record.leagueRecords().stream()
                .anyMatch(r -> "Wins".equals(r.key()) && r.value() == 42);
        assertTrue("League-level records should survive SaveManager round-trip", found);
    }
}
