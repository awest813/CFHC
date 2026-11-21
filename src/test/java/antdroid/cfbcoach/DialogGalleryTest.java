package antdroid.cfbcoach;

import android.app.Dialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import simulation.Game;
import simulation.Team;

import static org.junit.Assert.*;

/**
 * Screenshot gallery of the dialogs a season raises on its own (week digest,
 * midseason report, season summary, contracts, job offers, transfers, …) plus
 * the box score and scouting dialogs, the preseason redshirt dialog and the
 * new-game setup flow. Each distinct dialog is captured once (first time its
 * title is seen) under app/build/screenshots/gallery_*.png.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class DialogGalleryTest {

    @Test
    public void seasonDialogs_dark() throws Exception {
        playSeasonCapturing(0, "dark");
    }

    @Test
    public void seasonDialogs_light() throws Exception {
        playSeasonCapturing(1, "light");
    }

    private void playSeasonCapturing(int theme, String tag) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), theme))) {
            scenario.onActivity(activity -> {
                try {
                    Set<String> seen = new HashSet<>();
                    captureAndDismiss(tag, seen, 0);
                    NavigationView nav = activity.findViewById(R.id.nav_view);

                    // Preseason: the redshirt dialog (window is week 0 only).
                    openFromNav(activity, nav, R.id.nav_redshirts);
                    captureAndDismiss(tag + "_redshirt", seen, 0);

                    Method sim = MainActivity.class.getDeclaredMethod("simulateWeek");
                    sim.setAccessible(true);
                    for (int step = 0; step < 40; step++) {
                        if (activity.isFinishing() || activity.simLeague.recruitingPhaseActive) break;
                        sim.invoke(activity);
                        int week = activity.simLeague.currentWeek;
                        captureAndDismiss(tag, seen, week);
                        if (week == 6) {
                            captureGameDialogs(activity, tag, seen);
                            openFromNav(activity, nav, R.id.nav_postseason);
                            captureAndDismiss(tag + "_nav", seen, week);
                        }
                        if (week == activity.simLeague.regSeasonWeeks + 4) {
                            openFromNav(activity, nav, R.id.nav_offseason_hub);
                            captureAndDismiss(tag + "_nav", seen, week);
                            openFromNav(activity, nav, R.id.nav_transfers);
                            captureAndDismiss(tag + "_nav", seen, week);
                        }
                    }
                    assertTrue("captured some season dialogs", seen.size() > 5);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    public void newGameSetup_dark() throws Exception {
        try (ActivityScenario<Home> home = ActivityScenario.launch(Home.class)) {
            home.onActivity(activity -> {
                try {
                    Set<String> seen = new HashSet<>();
                    captureAndDismiss("home", seen, 0); // first-run welcome
                    activity.findViewById(R.id.buttonNewGame).performClick();
                    ShadowLooper.idleMainLooper();
                    captureAndDismiss("home", seen, 0);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(GameNavigation.createMainIntent(
                ApplicationProvider.getApplicationContext(),
                simulation.LeagueLaunchCoordinator.LaunchRequest.newLeague(
                        simulation.LeagueLaunchCoordinator.LaunchRequest.PrestigeMode.DEFAULT), 0))) {
            scenario.onActivity(activity -> {
                try {
                    captureAndDismiss("newgame", new HashSet<>(), 0);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    /** A played game's box score and the next unplayed game's scouting report. */
    private static void captureGameDialogs(MainActivity activity, String tag, Set<String> seen) throws Exception {
        Team user = activity.simLeague.userTeam;
        Field f = Team.class.getDeclaredField("gameSchedule");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Game> games = (List<Game>) f.get(user);
        Game played = null;
        Game next = null;
        for (Game g : games) {
            if (g.hasPlayed && played == null) played = g;
            if (!g.hasPlayed && next == null) next = g;
        }
        if (played != null) {
            GameDialogController.show(activity, played, user);
            captureAndDismiss(tag + "_boxscore", seen, 6);
        }
        if (next != null) {
            GameDialogController.show(activity, next, user);
            captureAndDismiss(tag + "_scout", seen, 6);
        }
    }

    private static void openFromNav(MainActivity activity, NavigationView nav, int id) {
        MenuItem item = nav.getMenu().findItem(id);
        if (item != null) activity.onNavigationItemSelected(item);
        ShadowLooper.idleMainLooper();
    }

    /** Captures every showing dialog (top first) whose title is new, then dismisses it. */
    private static void captureAndDismiss(String tag, Set<String> seen, int week) throws Exception {
        ShadowLooper.idleMainLooper();
        for (int i = 0; i < 30; i++) {
            Dialog d = ShadowDialog.getLatestDialog();
            if (d == null || !d.isShowing()) break;
            View root = d.getWindow().getDecorView();
            layoutDialog(root);
            ShadowLooper.idleMainLooper();
            layoutDialog(root);
            String key = tag + "_" + slug(titleOf(root));
            if (seen.add(key)) {
                Screenshots.capture(root, "gallery_" + key + "_w" + week);
            }
            d.dismiss();
            ShadowLooper.idleMainLooper();
        }
    }

    private static String titleOf(View root) {
        int[] ids = {androidx.appcompat.R.id.alertTitle, R.id.digestTitle, R.id.textSimpleDialogShellTitle,
                R.id.textSimpleDialogShellLabel};
        for (int id : ids) {
            View v = root.findViewById(id);
            if (v instanceof TextView && v.getVisibility() == View.VISIBLE) {
                CharSequence t = ((TextView) v).getText();
                if (t != null && t.length() > 0) return t.toString();
            }
        }
        View msg = root.findViewById(android.R.id.message);
        if (msg instanceof TextView) {
            String t = ((TextView) msg).getText().toString();
            return t.length() > 24 ? t.substring(0, 24) : t;
        }
        return "untitled";
    }

    private static String slug(String s) {
        String out = s.toLowerCase(Locale.US).replaceAll("[0-9]", "").replaceAll("[^a-z]+", "_");
        out = out.replaceAll("^_+|_+$", "");
        if (out.length() > 40) out = out.substring(0, 40);
        return out.isEmpty() ? "untitled" : out;
    }

    private static void layoutDialog(View root) {
        int w = root.getWidth() > 0 ? root.getWidth() : 1100;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }
}
