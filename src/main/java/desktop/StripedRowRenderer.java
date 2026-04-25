package desktop;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * A shared table cell renderer that provides alternating row colors for better readability.
 * Part of the 'Industrial Glass' UI design system.
 */
public class StripedRowRenderer extends DefaultTableCellRenderer {

    /**
     * Installs this renderer for {@link Object} and common numeric column classes so
     * {@link DefaultTableModel#getColumnClass(int)} rows (e.g. {@link Float}, {@link Integer}) stay striped.
     */
    public static void install(JTable table) {
        StripedRowRenderer r = new StripedRowRenderer();
        table.setDefaultRenderer(Object.class, r);
        table.setDefaultRenderer(String.class, r);
        table.setDefaultRenderer(Integer.class, r);
        table.setDefaultRenderer(Long.class, r);
        table.setDefaultRenderer(Short.class, r);
        table.setDefaultRenderer(Byte.class, r);
        table.setDefaultRenderer(Float.class, r);
        table.setDefaultRenderer(Double.class, r);
        table.setDefaultRenderer(java.math.BigDecimal.class, r);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                  boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (c instanceof javax.swing.JLabel jl) {
            jl.setOpaque(true);
            Class<?> colClass = table.getColumnClass(column);
            if (colClass != null && Number.class.isAssignableFrom(colClass)) {
                jl.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                jl.setHorizontalAlignment(SwingConstants.LEFT);
            }
            jl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        if (isSelected) {
            c.setBackground(DesktopTheme.selectionAccent());
            c.setForeground(Color.WHITE);
        } else {
            c.setBackground(row % 2 == 0 ? DesktopTheme.tableBase() : DesktopTheme.tableStripe());
            c.setForeground(DesktopTheme.textPrimary());
        }

        return c;
    }
}
