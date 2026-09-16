package desktop;

import org.junit.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.Assert.assertEquals;

public class DesktopThemeAccessibilityTest {

    @Test
    public void styleDataTableInScroll_setsAccessibleName() {
        DesktopTheme.load();
        JTable table = new JTable(new DefaultTableModel(new Object[]{"A"}, 0));
        JScrollPane scroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(scroll, table, "Standings");
        assertEquals("Standings", table.getAccessibleContext().getAccessibleName());
    }

    @Test
    public void stylePickerTable_setsAccessibleName() {
        DesktopTheme.load();
        JTable table = DesktopTheme.stylePickerTable(
                new DefaultTableModel(new Object[]{"Name"}, 0), 30, 11, "Candidates");
        assertEquals("Candidates", table.getAccessibleContext().getAccessibleName());
    }

    @Test
    public void applyWindowIcon_loadsWindowIconAsset() {
        try (java.io.InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("assets/cfhc_icon.png")) {
            org.junit.Assert.assertNotNull("assets/cfhc_icon.png must be accessible on desktop classpath", stream);
        } catch (Exception e) {
            org.junit.Assert.fail("Failed to read assets/cfhc_icon.png: " + e.getMessage());
        }
    }
}
