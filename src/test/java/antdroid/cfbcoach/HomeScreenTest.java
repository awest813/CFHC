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
