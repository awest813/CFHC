package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Custom universe and CSV import input that used to crash: malformed team rows
 * threw unchecked exceptions out of the league constructor (hosts only catch
 * IOException), a universe of fewer than 25 teams crashed at season end, and a
 * non-numeric roster/coach cell aborted the whole import.
 */
public class CustomUniverseRobustnessTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));

    /** Three conferences of 5, 7 and 9 teams (21 in all), divisions alternating. */
    private static String smallUniverse() {
        StringBuilder u = new StringBuilder("[START_CONFERENCES],,,\n");
        u.append("Alpha,A1,A2,,\nBeta,B1,B2,,\nGamma,G1,G2,,\n[END_CONFERENCES],,\n[START_TEAMS],,\n");
        String[] confs = {"Alpha", "Beta", "Gamma"};
        int[] sizes = {5, 7, 9};
        int n = 0;
        for (int c = 0; c < confs.length; c++) {
            for (int i = 0; i < sizes[c]; i++) {
                n++;
                u.append("Team").append(n).append(",T").append(n).append(',').append(confs[c]).append(',')
                        .append(40 + n).append(',').append(confs[c].charAt(0)).append(i % 2 == 0 ? "1" : "2")
                        .append(',').append(n % 5).append(",,\n");
            }
            u.append("[END_CONF],,\n");
        }
        u.append("[END_TEAMS],,\nBowl A, Bowl B, Bowl C\n");
        return u.toString();
    }

    private LeagueLaunchCoordinator.CustomUniverseFiles parse(String text) throws IOException {
        return CustomUniverseParser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                new File(tmp.getRoot(), "c.txt"), new File(tmp.getRoot(), "t.txt"), new File(tmp.getRoot(), "b.txt"));
    }

    @Test
    public void smallOddSizedUniverse_playsTwoFullSeasons() throws Exception {
        LeagueLaunchCoordinator.CustomUniverseFiles f = parse(smallUniverse());
        SimRandom.pinNextSeed(5L);
        League l = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES), f.conferences, f.teams, f.bowls, false, false);
        l.setPlatformResourceProvider(r);
        assertEquals(21, l.getTeamList().size());
        l.userTeam = l.getTeamList().get(0);
        l.userTeam.setupUserCoach("Small");
        SeasonController c = new SeasonController(l, GameUiBridge.NO_OP);
        int startYear = l.getYear();
        for (int s = 0; s < 2; s++) {
            for (int g = 0; g < 60 && !SeasonFlowOrder.isRecruitingGate(l.currentWeek, l.regSeasonWeeks); g++) {
                c.advanceWeek();
                UiReadsAreSideEffectFreeTest.readEverything(l);
            }
            assertTrue(c.autoCompleteRecruiting());
        }
        assertEquals(startYear + 2, l.getYear());
        assertTrue(l.getLeagueTop25History(0).contains("-"));
    }

    @Test
    public void malformedTeamRows_areReportedAsIoErrors() {
        String good = smallUniverse();
        String[] bad = {
                good.replace("Team3,T3,Alpha,43", "Team3,T3,Alpha,abc"),     // prestige not a number
                good.replace("Team4,T4,Alpha,44,A2,4,,", "Team4,T4"),        // short row
                good.replace("Team2,T2,Alpha,42,A2,2,,\n", "Team2,T2,Alpha,42,A2,2,,\n,,,,,,\n"), // blank row
                "[START_CONFERENCES]\nAlpha,A1,A2\n[END_CONFERENCES]\n[START_TEAMS]\n[END_TEAMS]\nBowl A\n", // no teams
        };
        for (String text : bad) {
            try {
                parse(text);
                fail("accepted:\n" + text);
            } catch (IOException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    public void bundledUniverse_withWindowsLineEndings_stillParses() throws Exception {
        String bundled = new String(java.nio.file.Files.readAllBytes(
                new File(System.getProperty("user.dir"), "Universe/2021-UniverseV1.csv").toPath()), StandardCharsets.UTF_8);
        LeagueLaunchCoordinator.CustomUniverseFiles f = parse(bundled.replace("\n", "\r\n"));
        League l = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES), f.conferences, f.teams, f.bowls, false, false);
        assertEquals(130, l.getTeamList().size());
    }

    @Test
    public void csvImports_skipBadRows_andReadPaddedOrWindowsRows() throws Exception {
        SimRandom.pinNextSeed(9L);
        League l = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES), r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS), r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        l.setPlatformResourceProvider(r);
        Team t = l.getTeamList().get(0);
        String roster = "TEAM,NAME,POSITION,YEAR,STARS\r\n"
                + t.getName() + ",Good Passer,QB,2,7\r\n"
                + t.getName() + ",Bad Row,QB,two,7\r\n"
                + t.getName() + ", Padded Back , RB , 3 , 6 \r\n"
                + t.getName() + ",Old Timer,WR,9,6\r\n"
                + "END_ROSTER\r\n";
        LeagueCustomDataImporter.importRoster(new ByteArrayInputStream(roster.getBytes(StandardCharsets.UTF_8)), l);
        positions.Player passer = find(t, "Good Passer");
        assertNotNull(passer);
        assertEquals(2, passer.year);
        assertNull(find(t, "Bad Row"));
        assertNotNull("padded cells and CRLF rows still import", find(t, "Padded Back"));
        assertEquals("class year clamps to senior", 4, find(t, "Old Timer").year);

        String coaches = "TEAM,NAME,POSITION,STARS,AGE\r\n"
                + t.getName() + ",Bad Coach,HC,eight,50\r\n"
                + t.getName() + ",Good Coord,OC,8,43\r\n"
                + "END_COACHES\r\n";
        LeagueCustomDataImporter.importCoaches(new ByteArrayInputStream(coaches.getBytes(StandardCharsets.UTF_8)), l);
        assertEquals("Good Coord", t.getOC().name);
        assertNotEquals("Bad Coach", t.getHeadCoach().name);
    }

    private static positions.Player find(Team t, String name) {
        for (positions.Player p : t.getAllPlayers()) if (p.name.equals(name)) return p;
        return null;
    }
}
