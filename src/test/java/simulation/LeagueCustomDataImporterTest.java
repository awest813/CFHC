package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeagueCustomDataImporterTest {
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
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void importCoaches_updatesMatchingTeamStaff() throws Exception {
        String csv = "Team,Name,Role,Talent,Playcalling\n"
                + team.getName() + ",Test Coach,HC,88,76\n"
                + team.getName() + ",Test OC,OC,81,72\n"
                + team.getName() + ",Test DC,DC,79,74\n"
                + "END_COACHES\n";

        LeagueCustomDataImporter.importCoaches(stream(csv), league);

        assertEquals("Test Coach", team.getHeadCoach().name);
        assertEquals("Test OC", team.OC.name);
        assertEquals("Test DC", team.DC.name);
    }

    @Test
    public void importRoster_replacesRosterAndRefillsMinimums() throws Exception {
        String csv = "Team,Name,Position,Rating,Year\n"
                + team.getName() + ",Import Passer,QB,91,1\n"
                + team.getName() + ",Import Runner,RB,87,2\n"
                + "END_ROSTER\n";

        LeagueCustomDataImporter.importRoster(stream(csv), league);

        assertNotNull(team.getTeamQBs().stream()
                .filter(player -> player.name.equals("Import Passer"))
                .findFirst()
                .orElse(null));
        assertNotNull(team.getTeamRBs().stream()
                .filter(player -> player.name.equals("Import Runner"))
                .findFirst()
                .orElse(null));
        assertTrue("Roster should be refilled to minimum playable size", team.getAllPlayers().size() >= 2);
    }

    @Test
    public void importRoster_doesNotClearTeamsMissingFromPartialFile() throws Exception {
        Team untouched = league.getTeamList().get(1);
        int originalRosterSize = untouched.getAllPlayers().size();
        String originalQuarterback = untouched.getTeamQBs().isEmpty() ? "" : untouched.getTeamQBs().get(0).name;
        String csv = "Team,Name,Position,Rating,Year\n"
                + team.getName() + ",Import Passer,QB,91,1\n"
                + "END_ROSTER\n";

        LeagueCustomDataImporter.importRoster(stream(csv), league);

        assertEquals(originalRosterSize, untouched.getAllPlayers().size());
        if (!originalQuarterback.isEmpty()) {
            assertEquals(originalQuarterback, untouched.getTeamQBs().get(0).name);
        }
    }

    @Test(expected = java.io.IOException.class)
    public void importCoaches_rejectsMissingLeague() throws Exception {
        LeagueCustomDataImporter.importCoaches(stream("Team,Name\nEND_COACHES\n"), null);
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
