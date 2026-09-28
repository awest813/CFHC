package desktop;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsive card grid for the Home dashboard.
 *
 * <p>The dashboard used a fixed {@code GridLayout(0, 4)} squeezed into whatever
 * height was left under the Command Center hero. At the default 1200x850 window
 * that left ~95px rows and ~230px columns: nearly every card painted its rows on
 * top of each other (schedule, standings, roster spotlight, head coach, program
 * health). This grid instead
 * <ul>
 *   <li>picks 2-4 columns so a card is never narrower than {@link #MIN_CARD_WIDTH},</li>
 *   <li>never lets a row shrink below the tallest card's preferred height
 *       (clamped to {@link #MIN_ROW_HEIGHT}..{@link #MAX_ROW_HEIGHT}) — the
 *       enclosing scroll pane scrolls instead,</li>
 *   <li>stretches the cards of a partial last row across the full width (16
 *       cards in 3 columns would otherwise leave one card and two holes), and</li>
 *   <li>still stretches rows to fill the viewport on tall windows.</li>
 * </ul>
 * Mount it as the view of a vertical {@link javax.swing.JScrollPane}.
 */
final class DashboardCardGrid extends JPanel implements Scrollable {

    static final int GAP = 12;
    static final int MIN_CARD_WIDTH = 280;
    static final int MIN_ROW_HEIGHT = 178;
    /** Cap so a card wrapping a long scrollable list can't balloon every row. */
    static final int MAX_ROW_HEIGHT = 250;
    private static final int MAX_COLUMNS = 4;
    private static final int MIN_COLUMNS = 2;

    DashboardCardGrid() {
        setLayout(new CardGridLayout());
        setOpaque(false);
    }

    /** Column count that keeps every card at least {@link #MIN_CARD_WIDTH} wide. */
    static int columnsFor(int width) {
        int cols = (width + GAP) / (MIN_CARD_WIDTH + GAP);
        return Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, cols));
    }

    private int availableWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport && viewport.getWidth() > 0) {
            return viewport.getWidth();
        }
        return getWidth() > 0 ? getWidth() : MAX_COLUMNS * (MIN_CARD_WIDTH + GAP);
    }

    private List<Component> visibleCards() {
        List<Component> cards = new ArrayList<>();
        for (Component c : getComponents()) {
            if (c.isVisible()) {
                cards.add(c);
            }
        }
        return cards;
    }

    /** Uniform row height: the tallest card's preferred height, clamped. */
    int rowHeight() {
        int tallest = 0;
        for (Component card : visibleCards()) {
            tallest = Math.max(tallest, card.getPreferredSize().height);
        }
        return Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, tallest));
    }

    private static int rowsFor(int cardCount, int cols) {
        return Math.max(1, (cardCount + cols - 1) / cols);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = availableWidth();
        int rows = rowsFor(visibleCards().size(), columnsFor(width));
        return new Dimension(width, rows * rowHeight() + (rows - 1) * GAP);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(24, visibleRect.height - 48);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // Fill the viewport when it is taller than the minimum grid; scroll otherwise.
        Container parent = getParent();
        return parent instanceof JViewport viewport
                && viewport.getHeight() > getPreferredSize().height;
    }

    /** Uniform rows; a partial last row shares the full width. */
    private final class CardGridLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {}

        @Override
        public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return getPreferredSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(MIN_COLUMNS * MIN_CARD_WIDTH, MIN_ROW_HEIGHT);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int width = parent.getWidth() - in.left - in.right;
            int height = parent.getHeight() - in.top - in.bottom;
            List<Component> cards = visibleCards();
            if (cards.isEmpty() || width <= 0) {
                return;
            }
            int cols = columnsFor(width);
            int rows = rowsFor(cards.size(), cols);
            int rowH = Math.max(rowHeight(), (height - (rows - 1) * GAP) / rows);
            for (int r = 0; r < rows; r++) {
                int first = r * cols;
                int count = Math.min(cols, cards.size() - first);
                int y = in.top + r * (rowH + GAP);
                for (int i = 0; i < count; i++) {
                    // Integer-exact column edges so the right edge never drifts.
                    int x0 = i * (width + GAP) / count;
                    int x1 = (i + 1) * (width + GAP) / count - GAP;
                    cards.get(first + i).setBounds(in.left + x0, y, x1 - x0, rowH);
                }
            }
        }
    }
}
