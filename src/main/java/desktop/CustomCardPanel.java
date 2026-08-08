package desktop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Custom dark slate card container subclass of {@link JPanel} for the sports broadcast HUD layout.
 * Features 8px rounded corners, 1px dark slate border (#1E293B), card title header, and elevation styling.
 */
public class CustomCardPanel extends JPanel {

    private final String cardTitle;
    private JPanel headerBar;
    private JLabel titleLabel;
    private JPanel contentArea;

    public CustomCardPanel(String cardTitle) {
        super(new BorderLayout(0, 8));
        this.cardTitle = cardTitle;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        if (cardTitle != null && !cardTitle.trim().isEmpty()) {
            headerBar = new JPanel(new BorderLayout());
            headerBar.setOpaque(false);
            titleLabel = new JLabel(cardTitle.toUpperCase());
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            titleLabel.setForeground(DesktopTheme.textSecondary());
            headerBar.add(titleLabel, BorderLayout.WEST);
            add(headerBar, BorderLayout.NORTH);
        }

        contentArea = new JPanel(new BorderLayout());
        contentArea.setOpaque(false);
        add(contentArea, BorderLayout.CENTER);
    }

    public JPanel getContentArea() {
        return contentArea;
    }

    public JPanel getHeaderBar() {
        return headerBar;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Card Slate Background (#0D1726)
        g2.setColor(new Color(13, 23, 38));
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

        // 1px Dark Slate Border (#1E293B)
        g2.setColor(DesktopTheme.borderSubtle());
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        g2.dispose();
        super.paintComponent(g);
    }
}
