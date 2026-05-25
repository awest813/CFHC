package desktop;

import simulation.Team;
import simulation.TeamColors;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;

/**
 * A shared table cell renderer that provides alternating row colors for better readability.
 * Supports optional team-color gradient tinting via {@link #installWithTeamColors(JTable, Map, int)}.
 * Part of the 'Industrial Glass' UI design system.
 */
public class StripedRowRenderer extends DefaultTableCellRenderer {

    private final Map<String, Team> teamMap;
    private final int nameColumn;
    private boolean currentSelected;
    private Color teamAccent;

    public StripedRowRenderer() {
        this(null, -1);
    }

    public StripedRowRenderer(Map<String, Team> teamMap, int nameColumn) {
        this.teamMap = teamMap;
        this.nameColumn = nameColumn;
    }

    public static void install(JTable table) {
        StripedRowRenderer r = new StripedRowRenderer();
        register(table, r);
    }

    public static void installWithTeamColors(JTable table, Map<String, Team> teamMap, int nameColumn) {
        StripedRowRenderer r = new StripedRowRenderer(teamMap, nameColumn);
        register(table, r);
    }

    public static void installWithHover(JTable table) {
        final StripedRowRenderer r = new StripedRowRenderer();
        register(table, r);
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
        table.putClientProperty("hoveredRow", hoveredRow);
    }

    private static void register(JTable table, StripedRowRenderer r) {
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
        this.currentSelected = isSelected;
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        this.teamAccent = null;
        if (teamMap != null && nameColumn >= 0) {
            Object nameVal = table.getValueAt(row, nameColumn);
            if (nameVal != null) {
                Team t = teamMap.get(nameVal.toString());
                if (t != null) teamAccent = TeamColors.primary(t.getAbbr());
            }
        }

        int[] hovered = (int[]) table.getClientProperty("hoveredRow");
        int hoverRow = hovered != null ? hovered[0] : -1;
        boolean isHovered = !isSelected && row == hoverRow && hoverRow >= 0;

        if (c instanceof javax.swing.JLabel jl) {
            boolean useTeamColors = teamMap != null && nameColumn >= 0;
            jl.setOpaque(!useTeamColors);
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

    @Override
    protected void paintComponent(Graphics g) {
        if (teamAccent != null) {
            DesktopTheme.paintTableRowGradient(g, getWidth(), getHeight(), teamAccent, currentSelected);
        }
        super.paintComponent(g);
    }
}
