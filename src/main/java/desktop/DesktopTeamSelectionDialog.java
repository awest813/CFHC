package desktop;

import simulation.Conference;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Desktop user-team picker used when a loaded Android/portable save has no
 * active user team marker.
 */
public class DesktopTeamSelectionDialog extends JDialog {
    private final League league;
    private Team selectedTeam;
    private boolean confirmed;

    private DesktopTeamSelectionDialog(JFrame owner, League league, String title) {
        super(owner, title, true);
        this.league = league;
        setSize(700, 520);
        setLayout(new BorderLayout());
        getContentPane().setBackground(DesktopTheme.windowBackground());
        buildUi();
    }

    private void buildUi() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(true);
        page.setBackground(DesktopTheme.windowBackground());
        page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Select Your Team");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        page.add(title, BorderLayout.NORTH);

        DefaultListModel<Conference> confModel = new DefaultListModel<>();
        for (Conference c : league.getConferences()) {
            confModel.addElement(c);
        }

        JList<Conference> confList = new JList<>(confModel);
        confList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        confList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        confList.setFixedCellHeight(26);
        confList.setBackground(DesktopTheme.textAreaEditorBackground());
        confList.setForeground(DesktopTheme.textPrimary());
        confList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Conference c = (Conference) value;
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, c.confName, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                DesktopTheme.decorateListCellLabel(label, index, isSelected, null);
                return label;
            }
        });

        DefaultListModel<Team> teamModel = new DefaultListModel<>();
        JList<Team> teamList = new JList<>(teamModel);
        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        teamList.setFixedCellHeight(26);
        teamList.setBackground(DesktopTheme.textAreaEditorBackground());
        teamList.setForeground(DesktopTheme.textPrimary());
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Team team = (Team) value;
                String labelText = String.format("%-22s  Prestige %d", team.getName(), team.getTeamPrestige());
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, labelText, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                DesktopTheme.decorateListCellLabel(label, index, isSelected, null);
                return label;
            }
        });

        JLabel teamInfo = new JLabel("Select a conference, then pick your team.");
        teamInfo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        teamInfo.setForeground(DesktopTheme.textPrimary());
        teamInfo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        confList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            Conference selected = confList.getSelectedValue();
            if (selected == null) {
                return;
            }
            teamModel.clear();
            List<Team> sorted = new ArrayList<>(selected.getTeams());
            sorted.sort(Comparator.comparingInt(Team::getTeamPrestige).reversed());
            for (Team team : sorted) {
                teamModel.addElement(team);
            }
        });

        teamList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            Team team = teamList.getSelectedValue();
            if (team != null) {
                teamInfo.setText(String.format("<html><body style='color:%s;'><b>%s</b> (%s) | %s | Prestige %d | HC: %s (OVR %d)</body></html>",
                        DesktopTheme.cssRgb(DesktopTheme.textPrimary()),
                        DesktopTheme.escapeForHtml(team.getName()),
                        DesktopTheme.escapeForHtml(team.getAbbr()),
                        DesktopTheme.escapeForHtml(team.getConference()),
                        team.getTeamPrestige(),
                        DesktopTheme.escapeForHtml(team.getHeadCoach().name),
                        team.getHeadCoach().ratOvr));
            }
        });

        JScrollPane confScroll = new JScrollPane(confList);
        confScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Conference"));
        confScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        confScroll.setPreferredSize(new Dimension(200, 0));

        JScrollPane teamScroll = new JScrollPane(teamList);
        teamScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Teams"));
        teamScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, confScroll, teamScroll);
        split.setDividerLocation(220);
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
        page.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(true);
        bottom.setBackground(DesktopTheme.windowBackground());
        bottom.add(teamInfo, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(true);
        buttons.setBackground(DesktopTheme.windowBackground());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        JButton selectBtn = new JButton("Use Team");
        selectBtn.addActionListener(e -> {
            Team team = teamList.getSelectedValue();
            if (team == null) {
                return;
            }
            markUserTeam(league, team);
            selectedTeam = team;
            confirmed = true;
            dispose();
        });
        buttons.add(cancelBtn);
        buttons.add(selectBtn);
        bottom.add(buttons, BorderLayout.SOUTH);
        page.add(bottom, BorderLayout.SOUTH);

        add(page, BorderLayout.CENTER);
        if (!confModel.isEmpty()) {
            confList.setSelectedIndex(0);
        }
    }

    private static void markUserTeam(League league, Team selected) {
        for (Team team : league.getTeamList()) {
            team.setUserControlled(false);
            if (team.getHeadCoach() != null) {
                team.getHeadCoach().user = false;
            }
        }
        selected.setUserControlled(true);
        if (selected.getHeadCoach() != null) {
            selected.getHeadCoach().user = true;
        }
        league.userTeam = selected;
    }

    public static boolean ensureUserTeam(JFrame owner, League league) {
        if (league.userTeam != null) {
            markUserTeam(league, league.userTeam);
            return true;
        }
        for (Team team : league.getTeamList()) {
            if (team.userControlled) {
                markUserTeam(league, team);
                return true;
            }
        }

        DesktopTeamSelectionDialog dialog = new DesktopTeamSelectionDialog(
                owner, league, "Select User Team");
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return dialog.confirmed && dialog.selectedTeam != null;
    }
}
