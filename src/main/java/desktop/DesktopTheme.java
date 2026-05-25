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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

    private static Color _windowBg, _textPrimary, _textSecondary, _warningText;
    private static Color _headerBg, _confHeaderBg, _statusBg, _sidebarBg, _sidebarText;
    private static Color _sidebarSelBg, _borderSubtle, _userTeamRow, _nliBannerBg;
    private static Color _pollLeader, _tableBase, _tableStripe, _tableHdrBg, _tableHover;
    private static Color _menuBarBg, _launcherMain, _launcherFooter, _textAreaBg;
    private static Color _selectionAccent, _accentBlue, _successGreen, _dangerRed;
    private static Color _dialogSurface, _inputFieldBg, _inputListBg;

    private DesktopTheme() {}

    private static void recache() {
        if (dark) {
            _windowBg = new Color(34, 36, 40);
            _textPrimary = new Color(232, 232, 238);
            _textSecondary = new Color(160, 165, 175);
            _warningText = new Color(255, 205, 110);
            _headerBg = new Color(20, 22, 26);
            _confHeaderBg = new Color(38, 42, 50);
            _statusBg = new Color(46, 48, 54);
            _sidebarBg = new Color(28, 31, 36);
            _sidebarText = new Color(210, 216, 224);
            _sidebarSelBg = new Color(58, 96, 150);
            _borderSubtle = new Color(72, 76, 84);
            _userTeamRow = new Color(32, 52, 82);
            _nliBannerBg = new Color(62, 52, 28);
            _pollLeader = new Color(48, 52, 60);
            _tableBase = new Color(34, 36, 40);
            _tableStripe = new Color(42, 45, 52);
            _tableHdrBg = new Color(48, 52, 60);
            _tableHover = new Color(52, 56, 68);
            _launcherMain = new Color(28, 30, 34);
            _launcherFooter = new Color(120, 125, 135);
            _textAreaBg = new Color(42, 44, 50);
            _menuBarBg = new Color(40, 42, 48);
        } else {
            _windowBg = Color.WHITE;
            _textPrimary = Color.BLACK;
            _textSecondary = new Color(80, 80, 80);
            _warningText = new Color(150, 90, 0);
            _headerBg = new Color(33, 37, 41);
            _confHeaderBg = new Color(52, 58, 64);
            _statusBg = new Color(240, 240, 240);
            _sidebarBg = new Color(246, 248, 251);
            _sidebarText = new Color(35, 42, 50);
            _sidebarSelBg = new Color(50, 100, 180);
            _borderSubtle = Color.GRAY;
            _userTeamRow = new Color(220, 235, 255);
            _nliBannerBg = new Color(255, 248, 220);
            _pollLeader = new Color(245, 245, 250);
            _tableBase = Color.WHITE;
            _tableStripe = new Color(245, 247, 250);
            _tableHdrBg = new Color(240, 242, 245);
            _tableHover = new Color(235, 238, 244);
            _launcherMain = Color.WHITE;
            _launcherFooter = Color.LIGHT_GRAY;
            _textAreaBg = Color.WHITE;
            Color def = UIManager.getColor("MenuBar.background");
            _menuBarBg = def != null ? def : new Color(240, 240, 240);
        }
        _selectionAccent = new Color(50, 100, 180);
        _accentBlue = new Color(52, 152, 219);
        _successGreen = new Color(46, 204, 113);
        _dangerRed = new Color(231, 76, 60);
        _dialogSurface = dark ? new Color(25, 32, 45) : new Color(246, 248, 251);
        _inputFieldBg = dark ? new Color(48, 50, 56) : Color.WHITE;
        _inputListBg = dark ? new Color(42, 44, 50) : Color.WHITE;
    }

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
        recache();
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
        recache();
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

    public static Color windowBackground() { return _windowBg; }

    public static Color textPrimary() { return _textPrimary; }

    public static Color textSecondary() { return _textSecondary; }

    public static Color warningText() { return _warningText; }

    public static Color headerBackground() { return _headerBg; }

    public static Color conferenceHeaderBackground() { return _confHeaderBg; }

    public static Color statusBackground() { return _statusBg; }

    public static Color sidebarBackground() { return _sidebarBg; }

    public static Color sidebarText() { return _sidebarText; }

    public static Color sidebarSelectionBackground() { return _sidebarSelBg; }

    public static Color borderSubtle() { return _borderSubtle; }

    public static Color userTeamRowTint() { return _userTeamRow; }

    public static Color nliBannerBackground() { return _nliBannerBg; }

    private static final Color _nliBannerBorder = new Color(200, 160, 60);

    public static Color nliBannerBorder() {
        return _nliBannerBorder;
    }

    public static Color pollLeaderCard() { return _pollLeader; }

    public static Color tableBase() { return _tableBase; }

    public static Color tableStripe() { return _tableStripe; }

    public static Color tableHeaderBackground() { return _tableHdrBg; }

    public static Color tableHoverTint() { return _tableHover; }

    public static Color menuBarBackground() { return _menuBarBg; }

    public static Color launcherMainPanel() { return _launcherMain; }

    public static Color launcherFooter() { return _launcherFooter; }

    /** Primary actions on the desktop launcher hub (contrasts in light vs dark). */
    public static void styleLauncherHubButton(JButton btn) {
        if (btn == null) {
            return;
        }
        btn.setBackground(dark ? new Color(72, 124, 204) : _selectionAccent);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Styles a primary action button consistently across all dialogs.
     * Uses the accent blue with white text, rounded appearance.
     */
    public static void stylePrimaryButton(JButton btn) {
        if (btn == null) return;
        btn.setBackground(_selectionAccent);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Styles a secondary / cancel button with a subtle appearance.
     */
    public static void styleSecondaryButton(JButton btn) {
        if (btn == null) return;
        btn.setForeground(textSecondary());
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Applies the window background to a content pane for a dialog.
     */
    public static void styleDialogContentPane(java.awt.Container pane) {
        if (pane == null) return;
        pane.setBackground(windowBackground());
    }

    /**
     * Builds a consistent dialog header panel with title and subtitle.
     */
    public static JPanel buildDialogHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(true);
        header.setBackground(_dialogSurface);
        header.setBorder(BorderFactory.createEmptyBorder(22, 30, 20, 30));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLbl.setForeground(textPrimary());

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(textSecondary());

        header.add(titleLbl, BorderLayout.NORTH);
        header.add(subLbl, BorderLayout.SOUTH);
        return header;
    }

    /**
     * Builds a dialog bottom bar with Apply/Cancel buttons aligned right.
     */
    public static JPanel buildDialogBottomBar(JButton... buttons) {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        bottom.setOpaque(true);
        bottom.setBackground(_dialogSurface);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                isDark() ? new Color(255, 255, 255, 20) : new Color(200, 200, 200)));
        for (JButton btn : buttons) {
            bottom.add(btn);
        }
        return bottom;
    }

    public static Color textAreaEditorBackground() { return _textAreaBg; }

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

    /**
     * Styles a picker/selection table (coordinator hiring, redshirt management, etc.)
     * with a consistent row height, single-selection mode, and styled header.
     */
    public static JTable stylePickerTable(DefaultTableModel model, int rowHeight, int headerFontSize) {
        JTable table = new JTable(model);
        table.setRowHeight(rowHeight);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(windowBackground());
        table.setForeground(textPrimary());
        table.setGridColor(borderSubtle());
        table.setShowVerticalLines(false);
        table.setSelectionBackground(accentBlue());

        table.getTableHeader().setBackground(tableBase());
        table.getTableHeader().setForeground(textSecondary());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, headerFontSize));
        table.getTableHeader().setPreferredSize(new Dimension(0, rowHeight + 5));
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle()));
        return table;
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
    public static Color selectionAccent() { return _selectionAccent; }

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

    public static Color accentBlue() { return _accentBlue; }

    public static Color successGreen() { return _successGreen; }

    public static Color dangerRed() { return _dangerRed; }

    public static Color dialogSurface() { return _dialogSurface; }

    public static void applyWindowIcon(java.awt.Window window) {
        try (java.io.InputStream iconStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("assets/cfhc_icon.png")) {
            if (iconStream != null) {
                java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                if (icon != null) {
                    window.setIconImage(icon);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void showScrollableText(java.awt.Component parent, String title, String text) {
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
        DesktopTheme.styleTextContent(area);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new java.awt.Dimension(650, 450));
        javax.swing.JOptionPane.showMessageDialog(parent, scroll, title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    public static JButton createGlassButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        return btn;
    }

    public static String yearAbbreviation(int year) {
        return switch (year) {
            case 0 -> "RS";
            case 1 -> "FR";
            case 2 -> "SO";
            case 3 -> "JR";
            case 4 -> "SR";
            case 5 -> "5SR";
            default -> String.valueOf(year);
        };
    }

    public static JPanel buildScreenHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setForeground(textPrimary());
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setForeground(textSecondary());
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        return header;
    }

    public static void paintHeaderGradient(java.awt.Graphics g, int width, int height, Color accent) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        Color top = accent != null ? accent : _headerBg;
        Color bot = darkerClamped(top, 0.25f, 30);
        g2.setPaint(new java.awt.GradientPaint(0, 0, top, 0, height, bot));
        g2.fillRect(0, 0, width, height);
        g2.dispose();
    }

    public static void paintSidebarGradient(java.awt.Graphics g, int width, int height) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        Color top = _sidebarBg;
        Color bot = new Color(
            Math.max(0, top.getRed() - 12),
            Math.max(0, top.getGreen() - 12),
            Math.max(0, top.getBlue() - 12));
        g2.setPaint(new java.awt.GradientPaint(0, 0, top, 0, height, bot));
        g2.fillRect(0, 0, width, height);
        g2.dispose();
    }

    public static void paintCardGradient(java.awt.Graphics g, int width, int height, Color accent) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = accent != null ? accent : _selectionAccent;
        Color top = transparent(base, 0.20f);
        Color bot = transparent(darker(base, 0.25f), 0.12f);
        g2.setPaint(new java.awt.GradientPaint(0, 0, top, 0, height, bot));
        g2.fillRoundRect(0, 0, width, height, 12, 12);
        g2.dispose();
    }

    public static void paintTableRowGradient(java.awt.Graphics g, int width, int height, Color accent, boolean selected) {
        if (accent == null) return;
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        Color tint = selected ? accent : transparent(accent, 0.12f);
        g2.setPaint(new java.awt.GradientPaint(0, 0, tint, width, 0, transparent(tint, 0f)));
        g2.fillRect(0, 0, width, height);
        g2.dispose();
    }

    private static Color darker(Color c, float factor) {
        return new Color(
            Math.max(0, (int)(c.getRed() * (1f - factor))),
            Math.max(0, (int)(c.getGreen() * (1f - factor))),
            Math.max(0, (int)(c.getBlue() * (1f - factor))));
    }

    private static Color darkerClamped(Color c, float factor, int minBrightness) {
        return new Color(
            Math.max(minBrightness, (int)(c.getRed() * (1f - factor))),
            Math.max(minBrightness, (int)(c.getGreen() * (1f - factor))),
            Math.max(minBrightness, (int)(c.getBlue() * (1f - factor))));
    }

    private static Color brighter(Color c, float factor) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int i = (int)(1.0f / (1.0f - factor));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;
        return new Color(
            Math.min(255, (int)(r / (1f - factor))),
            Math.min(255, (int)(g / (1f - factor))),
            Math.min(255, (int)(b / (1f - factor))));
    }

    private static Color transparent(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
            Math.min(255, Math.max(0, (int)(alpha * 255f))));
    }

}
