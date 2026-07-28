package desktop;

import org.junit.Test;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import static org.junit.Assert.assertNotNull;

public class DesktopThemeFileChooserTest {

    @Test
    public void styleFileChooser_doesNotThrowInLightOrDark() {
        DesktopTheme.load();
        boolean original = DesktopTheme.isDark();
        try {
            DesktopTheme.setDark(false);
            JFileChooser light = new JFileChooser();
            DesktopTheme.styleFileChooser(light);
            assertNotNull(light.getBackground());

            DesktopTheme.setDark(true);
            JFileChooser dark = new JFileChooser();
            DesktopTheme.styleFileChooser(dark);
            assertNotNull(dark.getBackground());
            assertNotNull(UIManager.getColor("FileChooser.background"));
        } finally {
            DesktopTheme.setDark(original);
        }
    }
}
