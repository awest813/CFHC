package desktop;

import simulation.League;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Tabbed dialog for end-of-season awards.
 *
 * <p>Each tab shows a distinct award category by calling the league's individual
 * award-string methods, giving a much cleaner layout than dumping everything
 * into a single scrollable text area.
 */
public class SeasonAwardsDialog extends JDialog {

    private static final Font MONO = new Font("Monospaced", Font.PLAIN, 13);

    public SeasonAwardsDialog(JFrame owner, League league, String fallbackSummary) {
        super(owner, "End-of-Season Awards", true);
        setSize(700, 540);
        setLayout(new BorderLayout());
        JPanel root = (JPanel) getContentPane();
        root.setOpaque(true);
        root.setBackground(DesktopTheme.windowBackground());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(true);
        tabs.setBackground(DesktopTheme.windowBackground());
        tabs.setForeground(DesktopTheme.textPrimary());

        tabs.addTab("Heisman",        buildTextTab(emptyIfNull(league.getHeismanCeremonyStr())));
        tabs.addTab("Def. POTY",      buildTextTab(emptyIfNull(league.getDefensePOTYStr())));
        tabs.addTab("Coach Award",    buildTextTab(emptyIfNull(league.getCoachAwardStr())));
        tabs.addTab("All-Americans",  buildTextTab(emptyIfNull(league.getAllAmericanStr())));
        tabs.addTab("Freshman Team",  buildTextTab(emptyIfNull(league.getFreshmanCeremonyStr())));

        // Full summary tab as a fallback / catch-all
        String summary = (fallbackSummary != null && !fallbackSummary.isEmpty())
                ? fallbackSummary : "(No summary available yet.)";
        tabs.addTab("Full Summary", buildTextTab(summary));

        add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(true);
        bottom.setBackground(DesktopTheme.windowBackground());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private JScrollPane buildTextTab(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(MONO);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        DesktopTheme.styleTextContent(area);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new Dimension(660, 440));
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));
        return scroll;
    }

    private static String emptyIfNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "(No data available yet — play through the end of the season.)";
        }
        return s;
    }

    /**
     * Shows the season awards dialog and blocks until the user closes it.
     *
     * @param owner           parent frame
     * @param league          the active league
     * @param fallbackSummary pre-formatted summary string from the engine (used as the
     *                        "Full Summary" tab)
     */
    public static void show(JFrame owner, League league, String fallbackSummary) {
        SeasonAwardsDialog dlg = new SeasonAwardsDialog(owner, league, fallbackSummary);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
