package antdroid.cfbcoach;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Spinner dropdowns and snackbars in both themes (they live in popup windows /
 * transient views the page walkers never open). Writes screenshots for review.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class PopupThemeTest {

    @Test
    public void dropdownsAndSnackbar_dark() throws Exception {
        capture(0, "dark");
    }

    @Test
    public void dropdownsAndSnackbar_light() throws Exception {
        capture(1, "light");
    }

    private void capture(int theme, String tag) throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), theme))) {
            scenario.onActivity(activity -> {
                try {
                    Spinner spinner = activity.findViewById(R.id.examineTeamSpinner);
                    assertNotNull(spinner);
                    SpinnerAdapter adapter = spinner.getAdapter();
                    assertTrue(adapter != null && adapter.getCount() > 0);
                    // The dropdown as the popup draws it: its rows on its own background.
                    LinearLayout list = new LinearLayout(activity);
                    list.setOrientation(LinearLayout.VERTICAL);
                    if (spinner.getPopupBackground() != null) list.setBackground(spinner.getPopupBackground());
                    for (int i = 0; i < Math.min(6, adapter.getCount()); i++) {
                        list.addView(adapter.getDropDownView(i, null, list));
                    }
                    int w = View.MeasureSpec.makeMeasureSpec(spinner.getWidth() > 0 ? spinner.getWidth() : 900,
                            View.MeasureSpec.EXACTLY);
                    int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    list.measure(w, h);
                    list.layout(0, 0, list.getMeasuredWidth(), list.getMeasuredHeight());
                    Screenshots.capture(list, tag + "_popup_spinner_dropdown");

                    PlatformUiHelper.snack(activity, "Saved to slot 1");
                    ShadowLooper.idleMainLooper();
                    View root = activity.getWindow().getDecorView();
                    Screenshots.capture(root, tag + "_popup_snackbar");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
