package desktop;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * A shared table cell renderer that provides alternating row colors for better readability.
 * Part of the 'Industrial Glass' UI design system. Includes optional hover highlighting
 * via {@link #installWithHover(JTable)}.
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

    /**
     * Installs the striped renderer and adds lightweight hover highlighting.
     * Hovered rows get a subtle tint overlay; selection still takes precedence.
     * Safe to call on any JTable. Hover state resets on scroll.
     */
    public static void installWithHover(JTable table) {
        install(table);
        final StripedRowRenderer r = new StripedRowRenderer();
        table.setDefaultRenderer(Object.class, r);
        table.setDefaultRenderer(String.class, r);
        table.setDefaultRenderer(Integer.class, r);
        table.setDefaultRenderer(Long.class, r);
        table.setDefaultRenderer(Float.class, r);
        table.setDefaultRenderer(Double.class, r);

        final int[] hoveredRow = { -1 };
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    table.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow[0]) {
                    hoveredRow[0] = row;
                    table.repaint();
                }
            }
        });
        table.putClientProperty("hoverRenderer", r);
        table.putClientProperty("hoveredRow", hoveredRow);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                  boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        int[] hovered = (int[]) table.getClientProperty("hoveredRow");
        int hoverRow = hovered != null ? hovered[0] : -1;
        boolean isHovered = !isSelected && row == hoverRow && hoverRow >= 0;

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
        } else if (isHovered) {
            c.setBackground(DesktopTheme.tableHoverTint());
            c.setForeground(DesktopTheme.textPrimary());
        } else {
            c.setBackground(row % 2 == 0 ? DesktopTheme.tableBase() : DesktopTheme.tableStripe());
            c.setForeground(DesktopTheme.textPrimary());
        }

        return c;
    }
}
