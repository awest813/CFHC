package desktop;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * A shared table cell renderer that provides alternating row colors for better readability.
 * Part of the 'Industrial Glass' UI design system.
 */
public class StripedRowRenderer extends DefaultTableCellRenderer {
    private static final Color STRIPE_COLOR = new Color(245, 247, 250);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                  boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? Color.WHITE : STRIPE_COLOR);
        }

        // Add consistent horizontal padding
        if (c instanceof javax.swing.JLabel) {
            ((javax.swing.JLabel) c).setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        return c;
    }
}
