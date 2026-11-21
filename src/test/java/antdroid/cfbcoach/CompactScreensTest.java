package antdroid.cfbcoach;

import android.app.Dialog;
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

import static org.junit.Assert.*;

/**
 * The main pages and a few text-heavy dialogs on a small phone (360x640dp) at a
 * large font scale (1.3), mid-season, so clipped or overlapping text shows up in
 * the compact_*.png screenshots.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
public class CompactScreensTest {

    private static final int[] PAGES = {
            R.id.nav_home, R.id.nav_roster, R.id.nav_teamplayerstats, R.id.nav_teamstats,
            R.id.nav_schedule, R.id.nav_standings, R.id.nav_rankings};
    private static final int[] DIALOGS = {
            R.id.nav_scores, R.id.nav_news, R.id.nav_strategy, R.id.nav_settings, R.id.nav_my_coach,
            R.id.nav_coach_program, R.id.nav_transfers, R.id.nav_depth_chart};

    @Test
    public void smallPhoneLargeFont_dark() throws Exception {
        walk(0, "dark");
    }

    @Test
    public void smallPhoneLargeFont_light() throws Exception {
        walk(1, "light");
    }

    private void walk(int theme, String tag) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntentAtWeek(ApplicationProvider.getApplicationContext(), theme, 5, true))) {
            scenario.onActivity(activity -> {
                try {
                    assertEquals(1.3f, activity.getResources().getConfiguration().fontScale, 0.01f);
                    dismissDialogs();
                    NavigationView nav = activity.findViewById(R.id.nav_view);
                    for (int id : PAGES) {
                        activity.onNavigationItemSelected(nav.getMenu().findItem(id));
                        settle(activity);
                        Screenshots.capture(activity.getWindow().getDecorView(),
                                "compact_" + tag + "_page_" + activity.getResources().getResourceEntryName(id));
                    }
                    for (int id : DIALOGS) {
                        activity.onNavigationItemSelected(nav.getMenu().findItem(id));
                        ShadowLooper.idleMainLooper();
                        Dialog d = ShadowDialog.getLatestDialog();
                        assertNotNull(d);
                        View root = d.getWindow().getDecorView();
                        layoutDialog(activity, root);
                        ShadowLooper.idleMainLooper();
                        layoutDialog(activity, root);
                        Screenshots.capture(root, "compact_" + tag + "_dialog_"
                                + activity.getResources().getResourceEntryName(id));
                        dismissDialogs();
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static void settle(MainActivity activity) {
        ShadowLooper.idleMainLooper();
        View root = activity.getWindow().getDecorView();
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    private static void layoutDialog(MainActivity activity, View root) {
        int w = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f);
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(activity.getResources().getDisplayMetrics().heightPixels,
                        View.MeasureSpec.AT_MOST));
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
