package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Team name/logo data integrity (audit follow-up): every universe team must
 * have a dedicated color pair (no gray fallbacks on the field), abbreviations
 * must be unique, and the FCS filler pool must use consistent spellings so a
 * school can never exist under two names.
 */
public class TeamNamingAndColorsTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0x0710L);
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
    }

    @Test
    public void everyTeam_hasDedicatedColors() {
        List<String> gray = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            // has()==false means the documented gray(100,100,100) fallback pair.
            if (!TeamColors.has(t.getAbbr())) {
                gray.add(t.getName() + " (" + t.getAbbr() + ")");
            }
        }
        assertTrue("Teams rendering with fallback gray colors: " + gray, gray.isEmpty());
    }

    @Test
    public void abbreviations_areUnique() {
        Set<String> seen = new HashSet<>();
        List<String> dupes = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            if (!seen.add(t.getAbbr())) {
                dupes.add(t.getAbbr());
            }
        }
        assertTrue("Duplicate abbreviations: " + dupes, dupes.isEmpty());
    }

    @Test
    public void teamNames_areUnique() {
        Set<String> seen = new HashSet<>();
        List<String> dupes = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            if (!seen.add(t.getName())) {
                dupes.add(t.getName());
            }
        }
        assertTrue("Duplicate team names: " + dupes, dupes.isEmpty());
    }

    @Test
    public void fcsPool_spellingsConsistent_andDistinctFromUniverse() {
        String[] pool = league.teamsFCS;
        Set<String> seen = new HashSet<>();
        for (String name : pool) {
            assertTrue("Pool entry must be non-empty", name != null && !name.isEmpty());
            assertFalse("Pool spelling should use the full 'State' word, found: " + name,
                    name.endsWith(" St"));
            assertTrue("Pool contains a duplicate school: " + name, seen.add(name));
            for (Team t : league.getTeamList()) {
                assertNotEquals("FCS pool name collides with a universe team: " + name,
                        name, t.getName());
            }
        }
    }
}
