package desktop;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card component for PROGRAM FINANCES.
 * Displays Budget ($34.2M), Current Balance ($5.8M), and Weekly Spend ($642K) metrics.
 */
public class ProgramFinancesCard extends CustomCardPanel {

    public ProgramFinancesCard() {
        super("Program Finances");
        JPanel content = getContentArea();

        JPanel list = new JPanel(new GridLayout(3, 1, 0, 4));
        list.setOpaque(false);

        list.add(buildFinRow("Budget", "$34.2M", Color.WHITE));
        list.add(buildFinRow("Current Balance", "$5.8M", DesktopTheme.successGreen()));
        list.add(buildFinRow("Weekly Spend", "$642K", Color.WHITE));

        content.add(list, BorderLayout.CENTER);
    }

    private JPanel buildFinRow(String label, String value, Color valColor) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(DesktopTheme.textSecondary());

        JLabel v = new JLabel(value);
        v.setFont(new Font("Monospaced", Font.BOLD, 12));
        v.setForeground(valColor);

        r.add(l, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }
}
