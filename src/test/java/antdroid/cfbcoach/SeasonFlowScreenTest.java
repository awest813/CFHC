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
 * Plays a season through MainActivity (the Play Week path, dialogs dismissed
 * as they appear) and redraws Team Home every week. Engine tests cover the
 * simulation; this covers the UI code each week runs (Team Home text, page
 * adapters, postseason dialogs), which is where the week-1 crash hid.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class SeasonFlowScreenTest {

    private static final int[] PAGES = {
            R.id.nav_home, R.id.nav_roster, R.id.nav_teamplayerstats, R.id.nav_teamstats,
            R.id.nav_schedule, R.id.nav_standings, R.id.nav_rankings};

    @Test
    public void playsASeasonThroughTheUi() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), 0))) {
            scenario.onActivity(activity -> {
                try {
                    Method sim = MainActivity.class.getDeclaredMethod("simulateWeek");
                    sim.setAccessible(true);
                    NavigationView nav = activity.findViewById(R.id.nav_view);
                    int maxWeek = 0;
                    for (int step = 0; step < 40; step++) {
                        dismissDialogs();
                        if (activity.isFinishing() || activity.simLeague.recruitingPhaseActive) break;
                        int before = activity.simLeague.currentWeek;
                        sim.invoke(activity);
                        dismissDialogs();
                        int week = activity.simLeague.currentWeek;
                        maxWeek = Math.max(maxWeek, week);
                        // Redraw Team Home every week: its text builder runs on each state.
                        activity.onNavigationItemSelected(nav.getMenu().findItem(R.id.nav_home));
                        settle(activity);
                        if (week == 6 || week == activity.simLeague.regSeasonWeeks + 4) {
                            for (int id : PAGES) {
                                MenuItem item = nav.getMenu().findItem(id);
                                activity.onNavigationItemSelected(item);
                                settle(activity);
                                Screenshots.capture(activity.getWindow().getDecorView(), "season_w" + week + "_"
                                        + activity.getResources().getResourceEntryName(id));
                            }
                        }
                        if (week == before && !activity.simLeague.recruitingPhaseActive) {
                            fail("Play Week did not advance from week " + before);
                        }
                    }
                    assertTrue("should reach the postseason, got week " + maxWeek,
                            maxWeek > activity.simLeague.regSeasonWeeks);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static void settle(MainActivity activity) {
        ShadowLooper.idleMainLooper();
        View root = activity.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(1233, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2673, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    private static void dismissDialogs() {
        ShadowLooper.idleMainLooper();
        for (int i = 0; i < 20; i++) {
            Dialog d = ShadowDialog.getLatestDialog();
            if (d == null || !d.isShowing()) break;
            d.dismiss();
            ShadowLooper.idleMainLooper();
        }
    }
}
