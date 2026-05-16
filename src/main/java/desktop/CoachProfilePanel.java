package desktop;

import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class CoachProfilePanel implements LeagueScreen {

    @Override
    public String title() {
        return "My Coach";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        if (ctx.league().userTeam == null) {
            JPanel empty = new JPanel(new BorderLayout());
            DesktopTheme.styleTabRoot(empty);
            JLabel msg = new JLabel("<html><div style='text-align:center;width:400px'><b>No program selected</b><br><br>"
                    + "Start or load a career with a team to view your coach profile here.</div></html>");
            msg.setFont(new Font("SansSerif", Font.PLAIN, 14));
            msg.setForeground(DesktopTheme.textSecondary());
            msg.setHorizontalAlignment(JLabel.CENTER);
            empty.add(msg, BorderLayout.CENTER);
            return empty;
        }
        Team ut = ctx.league().userTeam;
        HeadCoach hc = ut.getHeadCoach();

        JPanel panel = new JPanel(new BorderLayout(16, 16));
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new GridLayout(0, 2, 10, 6));
        header.setOpaque(true);
        header.setBackground(DesktopTheme.windowBackground());
        header.setBorder(DesktopTheme.titledBorder("Coach Career"));
        header.add(new JLabel("Name:")); header.add(new JLabel(hc.name, JLabel.LEFT) {{ setFont(new Font("SansSerif", Font.BOLD, 14)); }});
        header.add(new JLabel("Current Team:")); header.add(new JLabel(ut.getName()));
        header.add(new JLabel("Experience:")); header.add(new JLabel(hc.year + " years"));
        header.add(new JLabel("Career Record:")); header.add(new JLabel(hc.getWins() + "-" + hc.getLosses()));
        header.add(new JLabel("Championships:")); header.add(new JLabel(String.valueOf(hc.getNCWins())));
        DesktopTheme.styleLabelsDeep(header, DesktopTheme.textPrimary());

        JPanel attrs = new JPanel(new GridLayout(0, 2, 10, 6));
        attrs.setOpaque(true);
        attrs.setBackground(DesktopTheme.windowBackground());
        attrs.setBorder(DesktopTheme.titledBorder("Coach Attributes"));
        attrs.add(new JLabel("Overall:")); attrs.add(new JLabel(String.valueOf(hc.ratOvr)));
        attrs.add(new JLabel("Offense:")); attrs.add(new JLabel(String.valueOf(hc.ratOff)));
        attrs.add(new JLabel("Defense:")); attrs.add(new JLabel(String.valueOf(hc.ratDef)));
        attrs.add(new JLabel("Recruiting:")); attrs.add(new JLabel(String.valueOf(hc.ratTalent)));
        attrs.add(new JLabel("Discipline:")); attrs.add(new JLabel(String.valueOf(hc.ratDiscipline)));
        DesktopTheme.styleLabelsDeep(attrs, DesktopTheme.textPrimary());

        panel.add(header, BorderLayout.NORTH);
        panel.add(attrs, BorderLayout.CENTER);

        if (!hc.history.isEmpty()) {
            JTextArea hist = new JTextArea("History:\n\n");
            hist.setFont(new Font("SansSerif", Font.PLAIN, 13));
            for (String s : hc.history) hist.append("  \u2022 " + s + "\n");
            DesktopTheme.styleTextContent(hist);
            JScrollPane histScroll = new JScrollPane(hist);
            histScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
            histScroll.setOpaque(true);
            panel.add(histScroll, BorderLayout.SOUTH);
        }

        return panel;
    }
}