package desktop;

import org.junit.Test;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopThemeAccessibilityTest {

    @Test
    public void styleDataTableInScroll_setsAccessibleName() {
        DesktopTheme.load();
        JTable table = new JTable(new DefaultTableModel(new Object[]{"A"}, 0));
        JScrollPane scroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(scroll, table, "Standings");
        assertEquals("Standings", table.getAccessibleContext().getAccessibleName());
        assertFalse(table.getShowVerticalLines());
        assertTrue(table.getRowHeight() >= 26);
        Component header = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, "Team", false, false, -1, 0);
        assertTrue(header instanceof JLabel);
        assertEquals("TEAM", ((JLabel) header).getText());
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
