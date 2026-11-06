package antdroid.cfbcoach;

import android.app.Dialog;
import android.view.MenuItem;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.navigation.NavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * MainActivity on a saved dynasty: every list page and the main dialogs open
 * without crashing, in both themes. Screenshots of each go to
 * app/build/screenshots/ for review (layouts, light-theme audit).
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class MainScreensTest {

    private static final int[] PAGES = {
            R.id.nav_home, R.id.nav_roster, R.id.nav_teamplayerstats, R.id.nav_teamstats,
            R.id.nav_schedule, R.id.nav_standings, R.id.nav_rankings};
    private static final int[] DIALOGS = {
            R.id.nav_scores, R.id.nav_news, R.id.nav_mock_draft, R.id.nav_depth_chart,
            R.id.nav_strategy, R.id.nav_history_records, R.id.nav_coach_db, R.id.nav_awards,
            R.id.nav_leagueteamstats, R.id.nav_leagueplayerstats, R.id.nav_transfers,
            R.id.nav_coach_program, R.id.nav_settings, R.id.nav_my_coach};

    @Test
    public void pagesAndDialogs_dark() throws Exception {
        walk(0, "dark");
    }

    @Test
    public void pagesAndDialogs_light() throws Exception {
        walk(1, "light");
    }

    private void walk(int theme, String tag) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), theme))) {
            scenario.onActivity(activity -> {
                try {
                    dismissDialogs();
                    NavigationView nav = activity.findViewById(R.id.nav_view);
                    for (int id : PAGES) {
                        MenuItem item = nav.getMenu().findItem(id);
                        activity.onNavigationItemSelected(item);
                        settle(activity);
                        Screenshots.capture(activity.getWindow().getDecorView(),
                                tag + "_page_" + activity.getResources().getResourceEntryName(id));
                    }
                    for (int id : DIALOGS) {
                        activity.onNavigationItemSelected(nav.getMenu().findItem(id));
                        settle(activity);
                        Dialog d = ShadowDialog.getLatestDialog();
                        assertNotNull("no dialog for " + activity.getResources().getResourceEntryName(id), d);
                        View root = d.getWindow().getDecorView();
                        layoutDialog(root); // lists fill on a posted spinner callback,
                        ShadowLooper.idleMainLooper();
                        layoutDialog(root); // so lay out again after it has run
                        Screenshots.capture(root, tag + "_dialog_" + activity.getResources().getResourceEntryName(id));
                        dismissDialogs();
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void recreation_resumesFromAutosave_notTheLaunchSlot() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), 0))) {
            final int[] weekAfterPlay = new int[1];
            scenario.onActivity(activity -> {
                dismissDialogs();
                try {
                    Method sim = MainActivity.class.getDeclaredMethod("simulateWeek");
                    sim.setAccessible(true);
                    sim.invoke(activity);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                dismissDialogs();
                weekAfterPlay[0] = activity.simLeague.currentWeek;
                assertTrue("a week should have been played", weekAfterPlay[0] > 0);
            });
            scenario.recreate(); // what Android does after process death or a config change
            scenario.onActivity(activity -> {
                assertEquals("recreated activity must resume the played week, not reload the slot",
                        weekAfterPlay[0], activity.simLeague.currentWeek);
                assertEquals(LeagueFixture.userTeamName, activity.simLeague.userTeam.getName());
            });
        }
    }

    private static void settle(MainActivity activity) {
        ShadowLooper.idleMainLooper();
        View root = activity.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth() > 0 ? root.getWidth() : 1233, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(root.getHeight() > 0 ? root.getHeight() : 2673, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    /** Dialog windows are not laid out by the activity pass: size them like a phone dialog. */
    private static void layoutDialog(View root) {
        int w = root.getWidth() > 0 ? root.getWidth() : 1100;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
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
