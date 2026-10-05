package desktop;

import simulation.Team;
import simulation.TeamColors;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * A shared table cell renderer that provides alternating row colors for better readability.
 * Supports optional team-color gradient tinting via {@link #installWithTeamColors(JTable, Map, int)}.
 * Part of the 'Industrial Glass' UI design system.
 */
public class StripedRowRenderer extends DefaultTableCellRenderer {

    private final Map<String, Team> teamMap;
    private final int nameColumn;
    private final String numberPattern; // e.g. "%.1f" for float columns; null = default toString
    private boolean currentSelected;
    private Color teamAccent;
    private Color emphasisFill;
    private Font monoFor;
    private Font monoCache;

    /** Client property: {@code Set<Integer>} of model columns rendered as numbers. */
    private static final String NUMERIC_COLUMNS_KEY = "cfhc.striped.numericColumns";
    /** Client property: {@link IntPredicate} over model rows that get the highlight treatment. */
    private static final String ROW_EMPHASIS_KEY = "cfhc.striped.rowEmphasis";

    public StripedRowRenderer() {
        this(null, -1, null);
    }

    public StripedRowRenderer(Map<String, Team> teamMap, int nameColumn) {
        this(teamMap, nameColumn, null);
    }

    public StripedRowRenderer(Map<String, Team> teamMap, int nameColumn, String numberPattern) {
        this.teamMap = teamMap;
        this.nameColumn = nameColumn;
        this.numberPattern = numberPattern;
    }

    public static void install(JTable table) {
        StripedRowRenderer r = new StripedRowRenderer();
        register(table, r);
    }

    public static void installWithTeamColors(JTable table, Map<String, Team> teamMap, int nameColumn) {
        StripedRowRenderer r = new StripedRowRenderer(teamMap, nameColumn);
        register(table, r);
    }

    /**
     * Like {@link #installWithTeamColors}, but Float/Double cells render via
     * {@code numberPattern} (e.g. "%.1f") so raw double precision such as
     * "86.09153" never reaches the UI.
     */
    public static void installWithTeamColors(JTable table, Map<String, Team> teamMap,
                                             int nameColumn, String numberPattern) {
        StripedRowRenderer r = new StripedRowRenderer(teamMap, nameColumn, numberPattern);
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

    /**
     * Marks model columns whose values are numeric even though they are stored as
     * strings (e.g. "7-0", "1927.53", "1st"). They render right-aligned in the
     * monospaced numeric face, like {@link Number}-typed columns already do.
     */
    public static void setNumericColumns(JTable table, int... modelColumns) {
        Set<Integer> cols = new HashSet<>();
        for (int c : modelColumns) {
            cols.add(c);
        }
        table.putClientProperty(NUMERIC_COLUMNS_KEY, cols);
    }

    /**
     * Highlights model rows matching {@code modelRowEmphasised} (typically the
     * user's team or game): bold text, the theme's user-team tint and an accent
     * bar on the first column.
     */
    public static void setRowEmphasis(JTable table, IntPredicate modelRowEmphasised) {
        table.putClientProperty(ROW_EMPHASIS_KEY, modelRowEmphasised);
    }

    /**
     * Pads plain decimal strings in one model column to a common number of
     * decimals (max 2) so right-aligned monospaced values line up on the point,
     * e.g. "1756" / "1788.07" become "1756.00" / "1788.07". Columns containing
     * any non-decimal value (e.g. "$1,200", "85%") are left untouched.
     */
    public static void alignDecimals(javax.swing.table.DefaultTableModel model, int modelColumn) {
        int rows = model.getRowCount();
        int decimals = 0;
        for (int r = 0; r < rows; r++) {
            Object v = model.getValueAt(r, modelColumn);
            String text = v == null ? "" : v.toString().trim();
            if (!text.matches("-?\\d+(\\.\\d+)?")) {
                return;
            }
            int dot = text.indexOf('.');
            if (dot >= 0) {
                decimals = Math.max(decimals, Math.min(2, text.length() - dot - 1));
            }
        }
        if (decimals == 0) {
            return;
        }
        String pattern = "%." + decimals + "f";
        for (int r = 0; r < rows; r++) {
            double d = Double.parseDouble(model.getValueAt(r, modelColumn).toString().trim());
            model.setValueAt(String.format(java.util.Locale.ROOT, pattern, d), r, modelColumn);
        }
    }

    /** Monospaced numeric face matching {@code base}'s size and style. */
    public static Font numericFont(Font base) {
        int size = base != null ? base.getSize() : 12;
        int style = base != null ? base.getStyle() : Font.PLAIN;
        return new Font(Font.MONOSPACED, style, size);
    }

    /**
     * Opaque background for an emphasised row: the theme's (possibly translucent)
     * user-team tint composited over {@code base}, so opaque cells never smear.
     */
    public static Color emphasisBackground(Color base) {
        Color tint = DesktopTheme.userTeamRowTint();
        if (tint == null) {
            return base;
        }
        if (base == null || tint.getAlpha() == 255) {
            return tint;
        }
        float a = tint.getAlpha() / 255f;
        return new Color(
                Math.round(tint.getRed() * a + base.getRed() * (1 - a)),
                Math.round(tint.getGreen() * a + base.getGreen() * (1 - a)),
                Math.round(tint.getBlue() * a + base.getBlue() * (1 - a)));
    }

    @SuppressWarnings("unchecked")
    private static boolean isNumericColumn(JTable table, int viewColumn) {
        Class<?> colClass = table.getColumnClass(viewColumn);
        if (colClass != null && Number.class.isAssignableFrom(colClass)) {
            return true;
        }
        Object prop = table.getClientProperty(NUMERIC_COLUMNS_KEY);
        return prop instanceof Set<?> set
                && ((Set<Integer>) set).contains(table.convertColumnIndexToModel(viewColumn));
    }

    private static boolean isEmphasisedRow(JTable table, int viewRow) {
        Object prop = table.getClientProperty(ROW_EMPHASIS_KEY);
        if (!(prop instanceof IntPredicate pred) || viewRow < 0) {
            return false;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow >= 0 && pred.test(modelRow);
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

        boolean emphasised = isEmphasisedRow(table, row);
        boolean useTeamColors = teamMap != null && nameColumn >= 0;
        this.emphasisFill = null;

        if (c instanceof javax.swing.JLabel jl) {
            jl.setOpaque(!useTeamColors);
            if (isNumericColumn(table, column)) {
                jl.setHorizontalAlignment(SwingConstants.RIGHT);
                Font base = table.getFont();
                if (monoFor != base) {
                    monoFor = base;
                    monoCache = numericFont(base);
                }
                jl.setFont(monoCache);
            } else {
                jl.setHorizontalAlignment(SwingConstants.LEFT);
            }
            if (emphasised) {
                jl.setFont(jl.getFont().deriveFont(Font.BOLD));
            }
            if (emphasised && column == 0) {
                jl.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 3, 0, 0, DesktopTheme.successGreen()),
                        javax.swing.BorderFactory.createEmptyBorder(0, 7, 0, 10)));
            } else {
                jl.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
            }
        }

        if (isSelected) {
            c.setBackground(DesktopTheme.selectionAccent());
            c.setForeground(Color.WHITE);
        } else if (isHovered) {
            c.setBackground(DesktopTheme.tableHoverTint());
            c.setForeground(DesktopTheme.textPrimary());
        } else {
            Color stripe = row % 2 == 0 ? DesktopTheme.tableBase() : DesktopTheme.tableStripe();
            if (emphasised) {
                stripe = emphasisBackground(stripe);
                if (useTeamColors) {
                    emphasisFill = stripe;
                }
            }
            c.setBackground(stripe);
            c.setForeground(DesktopTheme.textPrimary());
        }

        return c;
    }

    @Override
    protected void setValue(Object value) {
        if (numberPattern != null && (value instanceof Float || value instanceof Double)) {
            setText(String.format(numberPattern, ((Number) value).doubleValue()));
        } else {
            super.setValue(value);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (emphasisFill != null) {
            g.setColor(emphasisFill);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (teamAccent != null) {
            DesktopTheme.paintTableRowGradient(g, getWidth(), getHeight(), teamAccent, currentSelected);
        }
        super.paintComponent(g);
    }
}
