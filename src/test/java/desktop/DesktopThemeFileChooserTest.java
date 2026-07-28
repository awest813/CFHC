package desktop;

import org.junit.Test;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void highContrast_togglesAndRestores() {
        DesktopTheme.load();
        boolean originalDark = DesktopTheme.isDark();
        boolean originalHc = DesktopTheme.isHighContrast();
        try {
            DesktopTheme.setDark(false);
            DesktopTheme.setHighContrast(true);
            assertTrue(DesktopTheme.isHighContrast());
            assertNotNull(DesktopTheme.textPrimary());
            DesktopTheme.setHighContrast(false);
            assertTrue(!DesktopTheme.isHighContrast());
        } finally {
            DesktopTheme.setHighContrast(originalHc);
            DesktopTheme.setDark(originalDark);
        }
    }

    @Test
    public void installLookAndFeel_prefersFlatLafWhenPresent() {
        DesktopTheme.load();
        boolean original = DesktopTheme.isDark();
        try {
            DesktopTheme.setDark(false);
            assertTrue("FlatLaf should be on the desktop test classpath",
                    DesktopTheme.installLookAndFeel());
            String lightLaf = UIManager.getLookAndFeel().getClass().getName().toLowerCase();
            assertTrue(lightLaf.contains("flat"));

            DesktopTheme.setDark(true);
            assertTrue(DesktopTheme.installLookAndFeel());
            String darkLaf = UIManager.getLookAndFeel().getClass().getName().toLowerCase();
            assertTrue(darkLaf.contains("flat"));
        } finally {
            DesktopTheme.setDark(original);
        }
    }
}
