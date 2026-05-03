package desktop;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.prefs.Preferences;

/**
 * Light / dark appearance for the Swing desktop shell. Preference is stored in
 * {@link Preferences} so it survives restarts.
 */
public final class DesktopTheme {

    private static final String PREF_NODE = "cfhc/desktop";
    private static final String KEY_DARK = "dark_mode";

    private static boolean dark;
    private static boolean loaded;

    private DesktopTheme() {}

    /**
     * Reads stored preference and applies {@link UIManager} hints. Call once after
     * the look-and-feel is installed (see {@link Main#main}).
     */
    public static void load() {
        if (!loaded) {
            loaded = true;
            Preferences p = Preferences.userRoot().node(PREF_NODE);
            dark = p.getBoolean(KEY_DARK, false);
        }
        applyGlobalHints();
    }

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean value) {
        if (dark == value) {
            return;
        }
        dark = value;
        Preferences.userRoot().node(PREF_NODE).putBoolean(KEY_DARK, value);
        applyGlobalHints();
    }

    /**
     * Best-effort hints so system LAF dialogs ( JOptionPane, file chooser panels )
     * stay readable in dark mode.
     */
    private static void applyGlobalHints() {
        if (dark) {
            Color bg = windowBackground();
            Color fg = textPrimary();
            UIManager.put("OptionPane.background", bg);
            UIManager.put("Panel.background", bg);
            UIManager.put("Label.foreground", fg);
            UIManager.put("TextField.background", new Color(48, 50, 56));
            UIManager.put("TextField.foreground", fg);
            UIManager.put("TextArea.background", new Color(48, 50, 56));
            UIManager.put("TextArea.foreground", fg);
            UIManager.put("List.background", new Color(42, 44, 50));
            UIManager.put("List.foreground", fg);
            UIManager.put("ComboBox.background", new Color(48, 50, 56));
            UIManager.put("ComboBox.foreground", fg);
            UIManager.put("CheckBox.foreground", fg);
            UIManager.put("RadioButton.foreground", fg);
            UIManager.put("TitledBorder.titleColor", fg);
            UIManager.put("Menu.foreground", fg);
            UIManager.put("MenuItem.foreground", fg);
            UIManager.put("PopupMenu.background", bg);
            UIManager.put("PopupMenu.foreground", fg);
            UIManager.put("OptionPane.messageForeground", fg);
            UIManager.put("Button.background", new Color(58, 65, 78));
            UIManager.put("Button.foreground", fg);
            UIManager.put("ScrollPane.background", bg);
            UIManager.put("Viewport.background", bg);
            UIManager.put("FileChooser.background", bg);
            UIManager.put("FileChooser.listViewBackground", new Color(42, 44, 50));
            UIManager.put("FileChooser.foreground", fg);
            // Extra keys used by some LAFs (Metal / cross-platform fallbacks)
            UIManager.put("FileChooser.previewBackground", new Color(42, 44, 50));
            UIManager.put("FileChooser.detailsViewBackground", new Color(42, 44, 50));
            UIManager.put("Separator.foreground", new Color(88, 92, 100));
        } else {
            Object[] keys = {
                    "OptionPane.background", "Panel.background", "Label.foreground",
                    "TextField.background", "TextField.foreground",
                    "TextArea.background", "TextArea.foreground",
                    "List.background", "List.foreground",
                    "ComboBox.background", "ComboBox.foreground",
                    "CheckBox.foreground", "RadioButton.foreground",
                    "TitledBorder.titleColor",
                    "Menu.foreground", "MenuItem.foreground",
                    "PopupMenu.background", "PopupMenu.foreground",
                    "OptionPane.messageForeground",
                    "Button.background", "Button.foreground",
                    "ScrollPane.background", "Viewport.background",
                    "FileChooser.background", "FileChooser.listViewBackground",
                    "FileChooser.foreground", "FileChooser.previewBackground",
                    "FileChooser.detailsViewBackground", "Separator.foreground"
            };
            for (Object k : keys) {
                UIManager.put(k, null);
            }
        }
    }

    public static Color windowBackground() {
        return dark ? new Color(34, 36, 40) : Color.WHITE;
    }

    public static Color textPrimary() {
        return dark ? new Color(232, 232, 238) : Color.BLACK;
    }

    public static Color textSecondary() {
        return dark ? new Color(160, 165, 175) : new Color(80, 80, 80);
    }

    public static Color warningText() {
        return dark ? new Color(255, 205, 110) : new Color(150, 90, 0);
    }

    public static Color headerBackground() {
        return dark ? new Color(20, 22, 26) : new Color(33, 37, 41);
    }

    public static Color conferenceHeaderBackground() {
        return dark ? new Color(38, 42, 50) : new Color(52, 58, 64);
    }

    public static Color statusBackground() {
        return dark ? new Color(46, 48, 54) : new Color(240, 240, 240);
    }

    public static Color sidebarBackground() {
        return dark ? new Color(28, 31, 36) : new Color(246, 248, 251);
    }

    public static Color sidebarText() {
        return dark ? new Color(210, 216, 224) : new Color(35, 42, 50);
    }

    public static Color sidebarSelectionBackground() {
        return dark ? new Color(58, 96, 150) : selectionAccent();
    }

    public static Color borderSubtle() {
        return dark ? new Color(72, 76, 84) : Color.GRAY;
    }

    public static Color userTeamRowTint() {
        return dark ? new Color(32, 52, 82) : new Color(220, 235, 255);
    }

    public static Color nliBannerBackground() {
        return dark ? new Color(62, 52, 28) : new Color(255, 248, 220);
    }

    public static Color nliBannerBorder() {
        return new Color(200, 160, 60);
    }

    public static Color pollLeaderCard() {
        return dark ? new Color(48, 52, 60) : new Color(245, 245, 250);
    }

    public static Color tableBase() {
        return dark ? new Color(34, 36, 40) : Color.WHITE;
    }

    public static Color tableStripe() {
        return dark ? new Color(42, 45, 52) : new Color(245, 247, 250);
    }

    public static Color tableHeaderBackground() {
        return dark ? new Color(48, 52, 60) : new Color(240, 242, 245);
    }

    public static Color menuBarBackground() {
        if (dark) {
            return new Color(40, 42, 48);
        }
        Color def = UIManager.getColor("MenuBar.background");
        return def != null ? def : new Color(240, 240, 240);
    }

    public static Color launcherMainPanel() {
        return dark ? new Color(28, 30, 34) : Color.WHITE;
    }

    public static Color launcherFooter() {
        return dark ? new Color(120, 125, 135) : Color.LIGHT_GRAY;
    }

    /** Primary actions on the desktop launcher hub (contrasts in light vs dark). */
    public static void styleLauncherHubButton(JButton btn) {
        if (btn == null) {
            return;
        }
        btn.setBackground(dark ? new Color(72, 124, 204) : new Color(50, 100, 180));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }

    public static Color textAreaEditorBackground() {
        return dark ? new Color(42, 44, 50) : Color.WHITE;
    }

    /** Apply table + viewport colors after the table is inside a {@link javax.swing.JScrollPane}. */
    public static void styleDataTable(JTable table) {
        table.setOpaque(true);
        table.setBackground(tableBase());
        table.setForeground(textPrimary());
        table.setGridColor(borderSubtle());
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBackground(tableHeaderBackground());
        table.getTableHeader().setForeground(textPrimary());
        Container p = table.getParent();
        if (p instanceof JViewport vp) {
            vp.setBackground(tableBase());
        }
    }

    /**
     * Styles a data table after it is the viewport view of {@code scroll}. Prefer this over
     * calling {@link #styleDataTable(JTable)} before the table is mounted in a scroll pane.
     */
    public static void styleDataTableInScroll(JScrollPane scroll, JTable table) {
        if (scroll == null || table == null) {
            return;
        }
        styleDataTable(table);
        scroll.getViewport().setBackground(tableBase());
        scroll.setOpaque(true);
        scroll.setBackground(windowBackground());
    }

    /** League tab / analytics page root so the content pane never shows default white in dark mode. */
    public static void styleTabRoot(JPanel panel) {
        if (panel == null) {
            return;
        }
        panel.setOpaque(true);
        panel.setBackground(windowBackground());
    }

    /** Filter / nav row: background plus label and compact control colors. */
    public static void styleToolbar(JPanel row) {
        if (row == null) {
            return;
        }
        row.setOpaque(true);
        row.setBackground(windowBackground());
        for (Component ch : row.getComponents()) {
            if (ch instanceof JLabel lb) {
                lb.setForeground(textPrimary());
            }
            styleFormControl(ch);
        }
    }

    /** Text fields and combo boxes on tab toolbars (dark mode only; light leaves LAF defaults). */
    public static void styleFormControl(Component c) {
        if (!dark || c == null) {
            return;
        }
        if (c instanceof JTextField tf) {
            tf.setBackground(new Color(48, 50, 56));
            tf.setForeground(textPrimary());
            tf.setCaretColor(textPrimary());
        } else if (c instanceof JComboBox<?> cb) {
            cb.setBackground(new Color(48, 50, 56));
            cb.setForeground(textPrimary());
        }
    }

    /** Titled border with theme line + title color (works in light and dark). */
    public static javax.swing.border.Border titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(borderSubtle()), title);
        tb.setTitleColor(textPrimary());
        return tb;
    }

    /** Non-table lists inside league tabs (poll sidebar, news headlines, etc.). */
    public static void styleListShell(JList<?> list) {
        if (list == null) {
            return;
        }
        list.setOpaque(true);
        list.setBackground(textAreaEditorBackground());
        list.setForeground(textPrimary());
        list.setSelectionBackground(selectionAccent());
        list.setSelectionForeground(Color.WHITE);
    }

    /** Settings tab checkboxes and plain labels. */
    public static void styleLeagueSettingsPanel(JPanel panel) {
        styleTabRoot(panel);
        applyLeagueSettingsTheme(panel);
    }

    private static void applyLeagueSettingsTheme(Container root) {
        if (root == null) {
            return;
        }
        for (Component ch : root.getComponents()) {
            if (ch instanceof JCheckBox cb) {
                cb.setOpaque(false);
                cb.setForeground(textPrimary());
            } else if (ch instanceof JLabel jl) {
                jl.setForeground(textPrimary());
            }
            if (ch instanceof Container inner) {
                applyLeagueSettingsTheme(inner);
            }
        }
    }

    /**
     * List row appearance after {@link javax.swing.DefaultListCellRenderer} super call.
     * Use {@code unselectedOverrideBg != null} only when the row is not selected (e.g. user-team tint).
     */
    public static void decorateListCellLabel(javax.swing.JLabel label, int index, boolean isSelected,
                                            Color unselectedOverrideBg) {
        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(selectionAccent());
            label.setForeground(Color.WHITE);
            return;
        }
        if (unselectedOverrideBg != null) {
            label.setBackground(unselectedOverrideBg);
        } else {
            label.setBackground(index % 2 == 0 ? tableBase() : tableStripe());
        }
        label.setForeground(textPrimary());
    }

    /** Sets every {@link JLabel} under {@code root} to {@code fg} (e.g. coach / form tabs). */
    public static void styleLabelsDeep(Container root, Color fg) {
        if (root == null) {
            return;
        }
        for (Component ch : root.getComponents()) {
            if (ch instanceof JLabel jl) {
                jl.setForeground(fg);
            }
            if (ch instanceof Container inner) {
                styleLabelsDeep(inner, fg);
            }
        }
    }

    public static void styleTextContent(javax.swing.JTextArea area) {
        area.setOpaque(true);
        area.setBackground(textAreaEditorBackground());
        area.setForeground(textPrimary());
        area.setCaretColor(textPrimary());
    }

    /**
     * Wraps plain-string JOptionPane messages so dark mode never shows black-on-white
     * blocks. Non-strings (existing components, scroll panes) pass through unchanged.
     */
    public static Object messageForDialog(Object message) {
        if (!dark || message == null) {
            return message;
        }
        if (message instanceof String s) {
            JTextArea a = new JTextArea(s);
            a.setEditable(false);
            a.setWrapStyleWord(true);
            a.setLineWrap(true);
            a.setColumns(42);
            a.setRows(0);
            styleTextContent(a);
            return a;
        }
        return message;
    }

    /** Strong list / table selection color (same in light and dark). */
    public static Color selectionAccent() {
        return new Color(50, 100, 180);
    }

    /** CSS {@code rgb(r,g,b)} for inline HTML in {@link javax.swing.JLabel}. */
    public static String cssRgb(Color c) {
        if (c == null) {
            return "inherit";
        }
        return "rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
    }

    /**
     * Escapes text embedded in minimal {@code <html>} fragments so engine copy cannot
     * break markup (e.g. accidental {@code <b>} or stray ampersands).
     */
    public static String escapeForHtml(String s) {
        if (s == null || s.isEmpty()) {
            return s == null ? "" : s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Walks a {@link JFileChooser} hierarchy so dark mode does not flash white panels
     * on Windows / system LAF (many {@code FileChooser.*} UI defaults are ignored).
     * Safe to call in light mode (no-op).
     */
    public static void styleFileChooser(JFileChooser fc) {
        if (fc == null || !dark) {
            return;
        }
        Color shell = windowBackground();
        Color fg = textPrimary();
        Color fieldBg = new Color(48, 50, 56);
        Color listBg = new Color(42, 44, 50);
        fc.setOpaque(true);
        fc.setBackground(shell);
        fc.setForeground(fg);
        applyFileChooserSubtreeColors(fc, shell, fg, fieldBg, listBg);
    }

    private static void applyFileChooserSubtreeColors(Component c, Color shell, Color fg,
                                                      Color fieldBg, Color listBg) {
        if (c instanceof JComponent jc) {
            if (c instanceof JList<?> list) {
                list.setOpaque(true);
                list.setBackground(listBg);
                list.setForeground(fg);
            } else if (c instanceof JTree tree) {
                tree.setOpaque(true);
                tree.setBackground(listBg);
                tree.setForeground(fg);
            } else if (c instanceof JTextField tf) {
                tf.setBackground(fieldBg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
            } else if (c instanceof JScrollPane sp) {
                sp.setOpaque(true);
                sp.setBackground(shell);
                JViewport vp = sp.getViewport();
                if (vp != null) {
                    vp.setBackground(listBg);
                    Component v = vp.getView();
                    if (v instanceof JList<?> || v instanceof JTree) {
                        vp.setBackground(listBg);
                    } else if (v instanceof JTextField tf) {
                        vp.setBackground(fieldBg);
                        tf.setBackground(fieldBg);
                        tf.setForeground(fg);
                    } else {
                        vp.setBackground(shell);
                    }
                }
            } else if (c instanceof JLabel lb) {
                lb.setForeground(fg);
            } else if (c instanceof JComboBox<?> cb) {
                cb.setBackground(fieldBg);
                cb.setForeground(fg);
            } else if (c instanceof JTable tbl) {
                tbl.setOpaque(true);
                tbl.setBackground(listBg);
                tbl.setForeground(fg);
                tbl.setGridColor(borderSubtle());
            } else if (c instanceof JPanel || c instanceof javax.swing.JLayeredPane) {
                jc.setOpaque(true);
                jc.setBackground(shell);
            }
        }
        if (c instanceof Container box) {
            for (Component child : box.getComponents()) {
                applyFileChooserSubtreeColors(child, shell, fg, fieldBg, listBg);
            }
        }
    }
}
