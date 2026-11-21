package antdroid.cfbcoach;

import android.app.Dialog;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.navigation.NavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import simulation.Conference;
import simulation.League;
import simulation.SeasonFlowOrder;
import simulation.SimRandom;
import simulation.Team;

import static org.junit.Assert.*;

/**
 * Opening a page or dialog (and flipping through its spinners) must not change
 * the league: no random draws, no re-sorted team lists, nothing a save would
 * carry. A screen that mutates the league makes a reloaded career play out
 * differently from an unbroken one, and merely looking at a screen changes the
 * season. Checked at the preseason, mid-season, the postseason and each
 * offseason step (where staff posts can be vacant), on a classic (four-team)
 * playoff league so Bowl Watch's projection path runs too.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class ScreenReadsLeaveLeagueUntouchedTest {

    /** Read-only destinations: no sim, save or exit items. */
    private static final int[] SCREENS = {
            R.id.nav_home, R.id.nav_roster, R.id.nav_teamplayerstats, R.id.nav_teamstats,
            R.id.nav_schedule, R.id.nav_standings, R.id.nav_rankings,
            R.id.nav_scores, R.id.nav_news, R.id.nav_leagueteamstats, R.id.nav_leagueplayerstats,
            R.id.nav_player_search, R.id.nav_transfers, R.id.nav_postseason, R.id.nav_mock_draft,
            R.id.nav_awards, R.id.nav_history_records, R.id.nav_coach_db, R.id.nav_my_coach,
            R.id.nav_depth_chart, R.id.nav_strategy, R.id.nav_gameday_plan, R.id.nav_coach_program};

    @Test
    public void midSeasonAndPostseason() throws Exception {
        int r = League.STANDARD_REG_SEASON_WEEKS;
        check(6);
        check(SeasonFlowOrder.bowlWeek2(r));
    }

    @Test
    public void offseasonSteps() throws Exception {
        int r = League.STANDARD_REG_SEASON_WEEKS;
        check(SeasonFlowOrder.contractsWeek(r));
        check(SeasonFlowOrder.coordinatorHiringWeek(r));
        check(SeasonFlowOrder.transferListWeek(r));
    }

    private void check(int week) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntentAtWeek(ApplicationProvider.getApplicationContext(), 0, week, false))) {
            scenario.onActivity(activity -> {
                try {
                    dismissDialogs();
                    assertEquals(week, activity.simLeague.currentWeek);
                    String before = fingerprint(activity.simLeague);
                    NavigationView nav = activity.findViewById(R.id.nav_view);
                    for (int id : SCREENS) {
                        MenuItem item = nav.getMenu().findItem(id);
                        if (item == null || !item.isEnabled() || !item.isVisible()) continue;
                        String name = activity.getResources().getResourceEntryName(id);
                        try {
                            activity.onNavigationItemSelected(item);
                            ShadowLooper.idleMainLooper();
                            Dialog d = ShadowDialog.getLatestDialog();
                            if (d != null && d.isShowing()) {
                                flipSpinners(d.getWindow().getDecorView());
                            } else {
                                flipSpinners(activity.getWindow().getDecorView());
                            }
                        } catch (RuntimeException e) {
                            throw new AssertionError(name + " crashed at week " + week, e);
                        }
                        dismissDialogs();
                        String after = fingerprint(activity.simLeague);
                        assertEquals("opening " + name + " changed the league at week " + week, before, after);
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    /** Selects every entry of every spinner in the tree, then puts each back. */
    private static void flipSpinners(View root) {
        List<Spinner> spinners = new ArrayList<>();
        collect(root, spinners);
        for (Spinner s : spinners) {
            if (s.getAdapter() == null) continue;
            int orig = s.getSelectedItemPosition();
            for (int i = 0; i < s.getAdapter().getCount(); i++) {
                s.setSelection(i);
                ShadowLooper.idleMainLooper();
            }
            if (orig >= 0) {
                s.setSelection(orig);
                ShadowLooper.idleMainLooper();
            }
        }
    }

    private static void collect(View v, List<Spinner> out) {
        if (v instanceof Spinner) out.add((Spinner) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out);
        }
    }

    /** RNG position, team-list and conference order, and a hash of the save the league would write. */
    static String fingerprint(League l) throws Exception {
        StringBuilder order = new StringBuilder();
        for (Team t : l.getTeamList()) order.append(t.getName()).append('|');
        for (Conference c : l.getConferences()) {
            for (Team t : c.confTeams) order.append(t.getName()).append(';');
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(l.serializeSave());
        md.update(order.toString().getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
        return SimRandom.exportState() + " " + hex;
    }

    private static void dismissDialogs() {
        ShadowLooper.idleMainLooper();
        for (int i = 0; i < 10; i++) {
            Dialog d = ShadowDialog.getLatestDialog();
            if (d == null || !d.isShowing()) break;
            d.dismiss();
            ShadowLooper.idleMainLooper();
        }
    }
}
