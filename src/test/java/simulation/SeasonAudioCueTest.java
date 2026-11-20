package simulation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The week's result cue must match what actually happened to the user's team:
 * one cue per advance, LOSS for losses, never a celebration after a loss, and
 * no postseason crowd noise for a team that isn't playing.
 */
public class SeasonAudioCueTest {

    private static League newLeague(int userIndex) {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(userIndex);
        league.userTeam.setUserControlled(true);
        return league;
    }

    private static List<Game> playedUserGames(League league) {
        List<Game> out = new ArrayList<>();
        for (Game g : league.userTeam.getGameSchedule()) {
            if (g != null && g.hasPlayed && !g.isByeWeek()) out.add(g);
        }
        return out;
    }

    private static void checkSeason(League league) {
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        int natty = SeasonFlowOrder.nationalChampionshipWeek(league.regSeasonWeeks);
        int cues = 0;
        while (league.currentWeek <= natty) {
            int week = league.currentWeek;
            List<Game> before = playedUserGames(league);
            int playedBefore = before.size();
            SeasonAdvanceResult r = controller.advanceWeek();
            List<Game> played = playedUserGames(league);
            AudioEvent cue = r.getAudioEvent();
            if (week == 0) {
                assertEquals(AudioEvent.WHISTLE, cue);
                continue;
            }
            if (played.size() == playedBefore) {
                // No user game this week (bye, eliminated, not selected).
                if (week == natty) {
                    assertEquals(AudioEvent.STADIUM_ORGAN, cue);
                } else {
                    assertNull("week " + week + " had no user game but cued " + cue, cue);
                }
                continue;
            }
            assertEquals(playedBefore + 1, played.size());
            played.removeAll(before);
            Game g = played.get(0);
            cues++;
            boolean home = g.homeTeam == league.userTeam;
            int us = home ? g.homeScore : g.awayScore;
            int them = home ? g.awayScore : g.homeScore;
            if (us < them) {
                assertEquals("week " + week + " loss", AudioEvent.LOSS, cue);
            } else {
                assertNotEquals("week " + week + " win cued as loss", AudioEvent.LOSS, cue);
                assertTrue(cue != null);
            }
        }
        // 12 slots; a bye slot can leave a team with 11 real games.
        assertTrue("expected at least 11 user-game cues, got " + cues, cues >= 11);
    }

    @Test
    public void cuesMatchUserResults_strongTeam() {
        checkSeason(newLeague(0));
    }

    @Test
    public void cuesMatchUserResults_weakTeam() {
        League league = newLeague(0);
        // Pick the lowest-prestige team so losses and a missed postseason occur.
        Team weakest = league.getTeamList().get(0);
        for (Team t : league.getTeamList()) {
            if (t.getTeamPrestige() < weakest.getTeamPrestige()) weakest = t;
        }
        league.userTeam.setUserControlled(false);
        league.userTeam = weakest;
        weakest.setUserControlled(true);
        checkSeason(league);
    }
}
