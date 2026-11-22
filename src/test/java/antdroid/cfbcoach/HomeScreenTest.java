package antdroid.cfbcoach;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class HomeScreenTest {

    @Test
    public void homeAndTutorial_renderInBothThemes() throws Exception {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        for (int theme : new int[]{0, 1}) {
            String tag = theme == 1 ? "light" : "dark";
            try (ActivityScenario<Home> scenario = ActivityScenario.launch(
                    GameNavigation.createHomeIntent(context, theme))) {
                scenario.onActivity(activity -> capture(activity.getWindow().getDecorView(), "home_" + tag));
            }
            try (ActivityScenario<TutorialActivity> scenario = ActivityScenario.launch(
                    GameNavigation.createTutorialIntent(context, theme))) {
                scenario.onActivity(activity -> capture(activity.getWindow().getDecorView(), "tutorial_" + tag));
            }
        }
    }

    private static void capture(View root, String name) {
        try {
            Screenshots.capture(root, name);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void homeInflates_andRendersBothThemes() throws Exception {
        try (ActivityScenario<Home> scenario = ActivityScenario.launch(Home.class)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                assertNotNull(activity.findViewById(R.id.buttonNewGame));
                assertNotNull(activity.findViewById(R.id.buttonLoadGame));
                try {
                    Screenshots.capture(root, "home");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
