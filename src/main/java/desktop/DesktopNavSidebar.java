package desktop;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Broadcast-style league navigation. Replaces the plain list: section labels,
 * a neon active bar, optional count badges, and a program seal in the footer.
 * Selection changes from code do not fire {@code onSelect}; only a click does.
 */
public final class DesktopNavSidebar extends JPanel {

    private static final int ROW_H = 36;
    private static final int SECTION_H = 22;
    private static final int TOP_PAD = 8;
    private static final int SIDEBAR_WIDTH = 228;

    private final String[] titles;
    private final UiIcons.Glyph[] icons;
    private final Consumer<String> onSelect;
    private final Map<String, String> badges = new HashMap<>();
    private final NavCanvas canvas;

    private int selected;
    private String footerMono = "CF";
    private String footerTitle = "League Office";
    private String footerMeta = "";

    public DesktopNavSidebar(String[] titles, UiIcons.Glyph[] icons, Consumer<String> onSelect) {
        super(new BorderLayout());
        if (titles.length != icons.length) {
            throw new IllegalArgumentException("titles and icons must be the same length");
        }
        this.titles = titles.clone();
        this.icons = icons.clone();
        this.onSelect = onSelect;
        setOpaque(false);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        getAccessibleContext().setAccessibleName("League navigation");

        canvas = new NavCanvas();
        JScrollPane scroll = new JScrollPane(canvas);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_H);
        add(scroll, BorderLayout.CENTER);
        add(new SealFooter(), BorderLayout.SOUTH);
    }

    public String selectedTitle() {
        return titles.length == 0 ? "" : titles[selected];
    }

    /** Moves the highlight. Does not notify {@code onSelect}. */
    public void setSelected(String title) {
        int idx = indexOf(title);
        if (idx < 0) {
            return;
        }
        selected = idx;
        canvas.scrollRectToVisible(canvas.rowBounds(idx));
        canvas.repaint();
    }

    public void setBadge(String title, String text) {
        if (text == null || text.isBlank()) {
            badges.remove(title);
        } else {
            badges.put(title, text);
        }
        canvas.repaint();
    }

    public String badge(String title) {
        return badges.get(title);
    }

    public void setFooter(String mono, String title, String meta) {
        footerMono = mono == null || mono.isBlank() ? "CF" : mono;
        footerTitle = title == null || title.isBlank() ? "League Office" : title;
        footerMeta = meta == null ? "" : meta;
        repaint();
    }

    public void focusNav() {
        canvas.requestFocusInWindow();
    }

    /** User activation (click). Package-visible for tests. */
    void activate(String title) {
        int idx = indexOf(title);
        if (idx < 0) {
            return;
        }
        selected = idx;
        canvas.repaint();
        if (onSelect != null) {
            onSelect.accept(title);
        }
    }

    private int indexOf(String title) {
        for (int i = 0; i < titles.length; i++) {
            if (titles[i].equals(title)) {
                return i;
            }
        }
        return -1;
    }

    /** Section heading drawn above this row, or null to continue the current group. */
    private String sectionStart(int index) {
        if (index < 0 || index >= titles.length) {
            return null;
        }
        String title = titles[index];
        if (index == 0 && "Home".equals(title)) {
            return "PROGRAM";
        }
        if ("Poll Rankings".equals(title)) {
            return "RANKINGS";
        }
        if ("League History".equals(title)) {
            return "LEAGUE";
        }
        return null;
    }

    private int contentHeight() {
        int y = TOP_PAD;
        for (int i = 0; i < titles.length; i++) {
            if (sectionStart(i) != null) {
                y += SECTION_H;
            }
            y += ROW_H;
        }
        return y + 8;
    }

    /** Y of row {@code index}, using the same walk as painting and hit testing. */
    private int rowTop(int index) {
        int y = TOP_PAD;
        for (int i = 0; i <= index && i < titles.length; i++) {
            if (sectionStart(i) != null) {
                y += SECTION_H;
            }
            if (i == index) {
                return y;
            }
            y += ROW_H;
        }
        return y;
    }

    private final class NavCanvas extends JPanel {
        NavCanvas() {
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int idx = indexAt(e.getY());
                    if (idx >= 0) {
                        activate(titles[idx]);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = indexAt(e.getY());
                    if (idx != hover) {
                        hover = idx;
                        setCursor(Cursor.getPredefinedCursor(
                                idx >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        setToolTipText(idx >= 0 ? shortcutTip(idx) : null);
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    setToolTipText(null);
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private int hover = -1;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIDEBAR_WIDTH - 2, contentHeight());
        }

        java.awt.Rectangle rowBounds(int index) {
            return new java.awt.Rectangle(0, rowTop(index), getWidth(), ROW_H);
        }

        private int indexAt(int y) {
            for (int i = 0; i < titles.length; i++) {
                int top = rowTop(i);
                if (y >= top && y < top + ROW_H) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = 0; i < titles.length; i++) {
                String section = sectionStart(i);
                if (section != null) {
                    paintSection(g2, section, rowTop(i) - SECTION_H);
                }
                paintRow(g2, i, rowTop(i));
            }
            g2.dispose();
        }

        private void paintSection(Graphics2D g2, String label, int y) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            g2.setColor(DesktopTheme.textSecondary());
            g2.drawString(label, 16, y + 16);
        }

        private void paintRow(Graphics2D g2, int index, int y) {
            boolean active = index == selected;
            boolean hot = index == hover && !active;
            int w = getWidth();
            if (active) {
                Color emerald = DesktopTheme.emerald();
                int alpha = DesktopTheme.isDark() ? 46 : 70;
                g2.setPaint(new java.awt.GradientPaint(
                        0, y, new Color(emerald.getRed(), emerald.getGreen(), emerald.getBlue(), alpha),
                        Math.max(1, w), y, new Color(emerald.getRed(), emerald.getGreen(), emerald.getBlue(), 0)));
                g2.fillRect(0, y, w, ROW_H);
                g2.setColor(emerald);
                g2.fillRect(0, y + 4, 4, ROW_H - 8);
            } else if (hot) {
                g2.setColor(DesktopTheme.isDark()
                        ? new Color(255, 255, 255, 16)
                        : new Color(0, 0, 0, 14));
                g2.fillRect(0, y, w, ROW_H);
            }

            Color fg;
            if (active) {
                if (DesktopTheme.isHighContrast()) {
                    fg = DesktopTheme.isDark() ? Color.WHITE : Color.BLACK;
                } else {
                    fg = DesktopTheme.isDark() ? Color.WHITE : new Color(6, 64, 36);
                }
            } else {
                fg = DesktopTheme.sidebarText();
            }
            UiIcons.of(icons[index], 16, fg).paintIcon(this, g2, 16, y + (ROW_H - 16) / 2);

            String label = titles[index].toUpperCase(java.util.Locale.ROOT);
            g2.setFont(new Font(Font.SANS_SERIF, active ? Font.BOLD : Font.PLAIN, 11));
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            String badge = badges.get(titles[index]);
            int badgeW = badge == null ? 0 : Math.max(18, fm.stringWidth(badge) + 12);
            int textMax = w - 44 - (badgeW > 0 ? badgeW + 10 : 8);
            String drawn = label;
            if (fm.stringWidth(drawn) > textMax && textMax > 12) {
                while (drawn.length() > 1 && fm.stringWidth(drawn + "…") > textMax) {
                    drawn = drawn.substring(0, drawn.length() - 1);
                }
                drawn = drawn + "…";
            }
            int textY = y + (ROW_H + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(drawn, 40, textY);

            if (badge != null) {
                int bx = w - badgeW - 10;
                int by = y + (ROW_H - 16) / 2;
                g2.setColor(DesktopTheme.isHighContrast() && !DesktopTheme.isDark()
                        ? Color.BLACK : new Color(245, 158, 11));
                g2.fillRoundRect(bx, by, badgeW, 16, 10, 10);
                g2.setColor(DesktopTheme.isHighContrast() && !DesktopTheme.isDark()
                        ? Color.WHITE : Color.BLACK);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                FontMetrics bf = g2.getFontMetrics();
                g2.drawString(badge, bx + (badgeW - bf.stringWidth(badge)) / 2,
                        by + (16 + bf.getAscent() - bf.getDescent()) / 2);
            }
        }

        private String shortcutTip(int index) {
            String key;
            if (index < 9) {
                key = "Key " + (index + 1);
            } else if (index < 18) {
                key = "Alt+" + (index - 8);
            } else {
                key = "";
            }
            return "Open " + titles[index] + (key.isEmpty() ? "" : "  (" + key + ")");
        }
    }

    private final class SealFooter extends JPanel {
        SealFooter() {
            setOpaque(false);
            setPreferredSize(new Dimension(SIDEBAR_WIDTH, 92));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(DesktopTheme.borderSubtle());
            g2.drawLine(12, 0, getWidth() - 12, 0);

            int cx = getWidth() / 2;
            int seal = 36;
            int sx = cx - seal / 2;
            int sy = 10;
            g2.setColor(DesktopTheme.isDark() ? new Color(15, 41, 30) : new Color(226, 242, 232));
            g2.fillOval(sx, sy, seal, seal);
            g2.setColor(DesktopTheme.emerald());
            g2.setStroke(new java.awt.BasicStroke(1.6f));
            g2.drawOval(sx, sy, seal, seal);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g2.setColor(DesktopTheme.isDark() ? Color.WHITE : new Color(6, 64, 36));
            FontMetrics fm = g2.getFontMetrics();
            String mono = footerMono.length() > 3 ? footerMono.substring(0, 3) : footerMono;
            g2.drawString(mono, cx - fm.stringWidth(mono) / 2,
                    sy + (seal + fm.getAscent() - fm.getDescent()) / 2);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g2.setColor(DesktopTheme.sidebarText());
            fm = g2.getFontMetrics();
            String title = ellipsize(g2, footerTitle.toUpperCase(java.util.Locale.ROOT), getWidth() - 20);
            g2.drawString(title, cx - fm.stringWidth(title) / 2, sy + seal + 16);

            if (!footerMeta.isEmpty()) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                g2.setColor(DesktopTheme.textSecondary());
                fm = g2.getFontMetrics();
                String meta = ellipsize(g2, footerMeta, getWidth() - 20);
                g2.drawString(meta, cx - fm.stringWidth(meta) / 2, sy + seal + 30);
            }
            g2.dispose();
        }

        private String ellipsize(Graphics2D g2, String text, int max) {
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(text) <= max) {
                return text;
            }
            String trimmed = text;
            while (trimmed.length() > 1 && fm.stringWidth(trimmed + "…") > max) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed + "…";
        }
    }
}
