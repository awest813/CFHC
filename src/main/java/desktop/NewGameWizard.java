package desktop;

import simulation.Conference;
import simulation.League;
import simulation.LeagueLaunchCoordinator.LaunchRequest.PrestigeMode;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * New-game wizard dialog.  Lets the user pick a prestige mode and then
 * select a team to control from the generated league.
 *
 * <p>Flow:
 * <ol>
 *   <li>Page 1 — choose prestige mode (Default / Randomize / Equalize).</li>
 *   <li>A temporary league is created in the background.</li>
 *   <li>Page 2 — pick a team from the conference/team list.</li>
 *   <li>The selected team is marked as user-controlled and the league is
 *       returned via {@link #getResult()}.</li>
 * </ol>
 */
public class NewGameWizard extends JDialog {

    private static final String TAG = "NewGameWizard";

    private final DesktopResourceProvider resources;
    private League resultLeague;
    private Team resultTeam;
    private boolean confirmed = false;

    public NewGameWizard(JFrame owner, DesktopResourceProvider resources) {
        super(owner, "New Game", true);
        this.resources = resources;
        setSize(700, 520);
        setLayout(new BorderLayout());
        showPrestigeModePage();
    }

    // -------------------------------------------------------------------------
    // Page 1 — Prestige Mode
    // -------------------------------------------------------------------------

    private void showPrestigeModePage() {
        getContentPane().removeAll();

        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("New Game Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        page.add(title, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 6, 8));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Prestige mode section
        JLabel prestigeHeader = new JLabel("Prestige Mode");
        prestigeHeader.setFont(new Font("SansSerif", Font.BOLD, 15));
        optionsPanel.add(prestigeHeader);

        JRadioButton defaultBtn = new JRadioButton("Default — historical team prestige");
        defaultBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        defaultBtn.setSelected(true);

        JRadioButton randomBtn = new JRadioButton("Randomize — shuffle prestige across all teams");
        randomBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JRadioButton equalBtn = new JRadioButton("Equalize — all teams start with the same prestige");
        equalBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));

        ButtonGroup group = new ButtonGroup();
        group.add(defaultBtn);
        group.add(randomBtn);
        group.add(equalBtn);

        optionsPanel.add(defaultBtn);
        optionsPanel.add(randomBtn);
        optionsPanel.add(equalBtn);

        // Playoff format section
        JLabel playoffHeader = new JLabel("Playoff Format");
        playoffHeader.setFont(new Font("SansSerif", Font.BOLD, 15));
        playoffHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        optionsPanel.add(playoffHeader);

        JRadioButton standardPlayoff = new JRadioButton("Standard 4-Team Playoff");
        standardPlayoff.setFont(new Font("SansSerif", Font.PLAIN, 14));
        standardPlayoff.setSelected(true);

        JRadioButton expandedPlayoff = new JRadioButton("Expanded Playoff (12-team format)");
        expandedPlayoff.setFont(new Font("SansSerif", Font.PLAIN, 14));

        ButtonGroup playoffGroup = new ButtonGroup();
        playoffGroup.add(standardPlayoff);
        playoffGroup.add(expandedPlayoff);

        optionsPanel.add(standardPlayoff);
        optionsPanel.add(expandedPlayoff);

        page.add(new JScrollPane(optionsPanel), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton nextBtn = new JButton("Next \u25B6");
        nextBtn.addActionListener(e -> {
            PrestigeMode mode;
            if (randomBtn.isSelected()) mode = PrestigeMode.RANDOMIZE;
            else if (equalBtn.isSelected()) mode = PrestigeMode.EQUALIZE;
            else mode = PrestigeMode.DEFAULT;
            boolean useExpandedPlayoff = expandedPlayoff.isSelected();
            createLeagueAndShowTeamPicker(mode, useExpandedPlayoff);
        });
        buttons.add(cancelBtn);
        buttons.add(nextBtn);
        page.add(buttons, BorderLayout.SOUTH);

        add(page, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Create League + Page 2 — Team Selection
    // -------------------------------------------------------------------------

    private void createLeagueAndShowTeamPicker(PrestigeMode mode, boolean useExpandedPlayoff) {
        try {
            boolean randomize = mode == PrestigeMode.RANDOMIZE;
            boolean equalize = mode == PrestigeMode.EQUALIZE;

            resultLeague = new League(
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                    resources.getString(PlatformResourceProvider.KEY_TEAMS),
                    resources.getString(PlatformResourceProvider.KEY_BOWLS),
                    randomize,
                    equalize
            );
            resultLeague.setPlatformResourceProvider(resources);
            resultLeague.expPlayoffs = useExpandedPlayoff;
            showTeamPickerPage();
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error creating league", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to create league:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTeamPickerPage() {
        getContentPane().removeAll();

        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Select Your Team");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        page.add(title, BorderLayout.NORTH);

        // Left: conference list
        List<Conference> conferences = resultLeague.getConferences();
        DefaultListModel<Conference> confModel = new DefaultListModel<>();
        for (Conference c : conferences) {
            confModel.addElement(c);
        }

        JList<Conference> confList = new JList<>(confModel);
        confList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        confList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        confList.setFixedCellHeight(26);
        confList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Conference c = (Conference) value;
                JLabel l = (JLabel) super.getListCellRendererComponent(list, c.confName, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    l.setBackground(new Color(50, 100, 180));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(index % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
                }
                return l;
            }
        });

        // Right: team list for selected conference
        DefaultListModel<Team> teamModel = new DefaultListModel<>();
        JList<Team> teamList = new JList<>(teamModel);
        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teamList.setFixedCellHeight(26);
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Team t = (Team) value;
                String label = String.format("%-22s  Prestige %d", t.getName(), t.getTeamPrestige());
                JLabel l = (JLabel) super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    l.setBackground(new Color(50, 100, 180));
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(index % 2 == 0 ? Color.WHITE : new Color(245, 247, 250));
                }
                return l;
            }
        });

        // Team info panel
        JLabel teamInfo = new JLabel("Select a conference, then pick your team.");
        teamInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teamInfo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        confList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Conference selected = confList.getSelectedValue();
            if (selected == null) return;
            teamModel.clear();
            List<Team> sorted = new ArrayList<>(selected.getTeams());
            sorted.sort(Comparator.comparingInt(Team::getTeamPrestige).reversed());
            for (Team t : sorted) {
                teamModel.addElement(t);
            }
        });

        teamList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Team sel = teamList.getSelectedValue();
            if (sel != null) {
                teamInfo.setText(String.format("<html><b>%s</b> (%s)  •  %s  •  Prestige %d  •  HC: %s (OVR %d)</html>",
                        sel.getName(), sel.getAbbr(), sel.getConference(),
                        sel.getTeamPrestige(), sel.getHeadCoach().name, sel.getHeadCoach().ratOvr));
            }
        });

        JScrollPane confScroll = new JScrollPane(confList);
        confScroll.setBorder(BorderFactory.createTitledBorder("Conference"));
        confScroll.setPreferredSize(new Dimension(200, 0));

        JScrollPane teamScroll = new JScrollPane(teamList);
        teamScroll.setBorder(BorderFactory.createTitledBorder("Teams"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, confScroll, teamScroll);
        split.setDividerLocation(220);
        page.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(teamInfo, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("\u25C0 Back");
        backBtn.addActionListener(e -> {
            resultLeague = null;
            showPrestigeModePage();
        });
        JButton startBtn = new JButton("Start Dynasty \u25B6");
        startBtn.addActionListener(e -> {
            Team sel = teamList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Please select a team.", "No Team Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            resultTeam = sel;
            resultTeam.setUserControlled(true);
            resultTeam.getHeadCoach().user = true;
            resultLeague.userTeam = resultTeam;
            confirmed = true;
            dispose();
        });

        buttons.add(backBtn);
        buttons.add(startBtn);
        bottom.add(buttons, BorderLayout.SOUTH);
        page.add(bottom, BorderLayout.SOUTH);

        add(page, BorderLayout.CENTER);
        revalidate();
        repaint();

        // Auto-select first conference
        if (!confModel.isEmpty()) {
            confList.setSelectedIndex(0);
        }
    }

    // -------------------------------------------------------------------------
    // Result
    // -------------------------------------------------------------------------

    /** True if the user completed the wizard (not cancelled). */
    public boolean isConfirmed() {
        return confirmed;
    }

    /** The league that was created, or null if cancelled. */
    public League getLeague() {
        return confirmed ? resultLeague : null;
    }

    /** The team the user chose, or null if cancelled. */
    public Team getTeam() {
        return confirmed ? resultTeam : null;
    }

    /**
     * Shows the wizard and blocks until the user finishes or cancels.
     * @return the created League with userTeam set, or null if cancelled.
     */
    public static League showWizard(JFrame owner, DesktopResourceProvider resources) {
        NewGameWizard wizard = new NewGameWizard(owner, resources);
        wizard.setLocationRelativeTo(owner);
        wizard.setVisible(true);
        return wizard.getLeague();
    }
}
