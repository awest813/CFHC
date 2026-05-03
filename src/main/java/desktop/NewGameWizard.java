package desktop;

import simulation.Conference;
import simulation.League;
import simulation.LeagueLaunchCoordinator.LaunchRequest.PrestigeMode;
import simulation.LeagueSettingsOptions;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
        setSize(760, 660);
        setMinimumSize(new Dimension(720, 600));
        setLayout(new BorderLayout());
        getContentPane().setBackground(DesktopTheme.windowBackground());
        showPrestigeModePage();
    }

    // -------------------------------------------------------------------------
    // Page 1 — Prestige Mode
    // -------------------------------------------------------------------------

    private void showPrestigeModePage() {
        getContentPane().removeAll();
        getContentPane().setBackground(DesktopTheme.windowBackground());

        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(true);
        page.setBackground(DesktopTheme.windowBackground());
        page.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("New Game Settings");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        JLabel subtitle = new JLabel("Choose your universe rules first, then pick a program.");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(DesktopTheme.textSecondary());
        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        page.add(titleBlock, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 6, 8));
        optionsPanel.setOpaque(true);
        optionsPanel.setBackground(DesktopTheme.windowBackground());
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel prestigeHeader = new JLabel("Prestige Mode");
        prestigeHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        prestigeHeader.setForeground(DesktopTheme.textPrimary());
        optionsPanel.add(prestigeHeader);

        JRadioButton defaultBtn = new JRadioButton("Default — historical team prestige");
        defaultBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        defaultBtn.setOpaque(false);
        defaultBtn.setForeground(DesktopTheme.textPrimary());
        defaultBtn.setSelected(true);

        JRadioButton randomBtn = new JRadioButton("Randomize — shuffle prestige across all teams");
        randomBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        randomBtn.setOpaque(false);
        randomBtn.setForeground(DesktopTheme.textPrimary());

        JRadioButton equalBtn = new JRadioButton("Equalize — all teams start with the same prestige");
        equalBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        equalBtn.setOpaque(false);
        equalBtn.setForeground(DesktopTheme.textPrimary());

        ButtonGroup group = new ButtonGroup();
        group.add(defaultBtn);
        group.add(randomBtn);
        group.add(equalBtn);

        optionsPanel.add(defaultBtn);
        optionsPanel.add(randomBtn);
        optionsPanel.add(equalBtn);

        JLabel playoffHeader = new JLabel("Playoff Format");
        playoffHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        playoffHeader.setForeground(DesktopTheme.textPrimary());
        playoffHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        optionsPanel.add(playoffHeader);

        JRadioButton standardPlayoff = new JRadioButton("Standard 4-Team Playoff");
        standardPlayoff.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        standardPlayoff.setOpaque(false);
        standardPlayoff.setForeground(DesktopTheme.textPrimary());
        standardPlayoff.setSelected(true);

        JRadioButton expandedPlayoff = new JRadioButton("Expanded Playoff (12-team format)");
        expandedPlayoff.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        expandedPlayoff.setOpaque(false);
        expandedPlayoff.setForeground(DesktopTheme.textPrimary());

        ButtonGroup playoffGroup = new ButtonGroup();
        playoffGroup.add(standardPlayoff);
        playoffGroup.add(expandedPlayoff);

        optionsPanel.add(standardPlayoff);
        optionsPanel.add(expandedPlayoff);

        JLabel universeHeader = new JLabel("Universe Options");
        universeHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        universeHeader.setForeground(DesktopTheme.textPrimary());
        universeHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        optionsPanel.add(universeHeader);

        JCheckBox showPotential = createOptionCheckBox("Show player potential", false);
        JCheckBox fullGameLog = createOptionCheckBox("Enable detailed game logs", false);
        JCheckBox careerMode = createOptionCheckBox("Coach career movement", true);
        JCheckBox neverRetire = createOptionCheckBox("Never force retirement", false);
        JCheckBox enableTv = createOptionCheckBox("Enable TV contracts", true);
        JCheckBox confRealignment = createOptionCheckBox("Conference realignment", true);
        JCheckBox advancedRealignment = createOptionCheckBox("Advanced transfers and realignment", false);
        JCheckBox universalProRel = createOptionCheckBox("Universal promotion/relegation", false);

        wireMutuallyExclusiveLeagueModes(confRealignment, advancedRealignment, universalProRel);
        optionsPanel.add(showPotential);
        optionsPanel.add(fullGameLog);
        optionsPanel.add(careerMode);
        optionsPanel.add(neverRetire);
        optionsPanel.add(enableTv);
        optionsPanel.add(confRealignment);
        optionsPanel.add(advancedRealignment);
        optionsPanel.add(universalProRel);

        JScrollPane optScroll = new JScrollPane(optionsPanel);
        optScroll.setBorder(null);
        optScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        optScroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(optScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(true);
        buttons.setBackground(DesktopTheme.windowBackground());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton nextBtn = new JButton("Next: Select Team \u25B6");
        nextBtn.setToolTipText("Create the league with these options and choose your school.");
        nextBtn.addActionListener(e -> {
            NewGameOptions options = new NewGameOptions();
            if (randomBtn.isSelected()) options.mode = PrestigeMode.RANDOMIZE;
            else if (equalBtn.isSelected()) options.mode = PrestigeMode.EQUALIZE;
            else options.mode = PrestigeMode.DEFAULT;
            options.expandedPlayoffs = expandedPlayoff.isSelected();
            options.showPotential = showPotential.isSelected();
            options.fullGameLog = fullGameLog.isSelected();
            options.careerMode = careerMode.isSelected();
            options.neverRetire = neverRetire.isSelected();
            options.enableTv = enableTv.isSelected();
            options.conferenceRealignment = confRealignment.isSelected();
            options.advancedRealignment = advancedRealignment.isSelected();
            options.universalProRel = universalProRel.isSelected();
            createLeagueAndShowTeamPicker(options);
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

    private void createLeagueAndShowTeamPicker(NewGameOptions options) {
        try {
            boolean randomize = options.mode == PrestigeMode.RANDOMIZE;
            boolean equalize = options.mode == PrestigeMode.EQUALIZE;

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
            LeagueSettingsOptions leagueOptions = LeagueSettingsOptions.fromLeague(resultLeague);
            leagueOptions.showPotential = options.showPotential;
            leagueOptions.fullGameLog = options.fullGameLog;
            leagueOptions.careerMode = options.careerMode;
            leagueOptions.neverRetire = options.neverRetire;
            leagueOptions.enableTv = options.enableTv;
            leagueOptions.expandedPlayoffs = options.expandedPlayoffs;
            leagueOptions.conferenceRealignment = options.conferenceRealignment;
            leagueOptions.advancedRealignment = options.advancedRealignment;
            leagueOptions.universalProRel = options.universalProRel;
            leagueOptions.applyTo(resultLeague, true, true, true);
            showTeamPickerPage();
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error creating league", ex);
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to create league:\n" + ex.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JCheckBox createOptionCheckBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        box.setOpaque(false);
        box.setForeground(DesktopTheme.textPrimary());
        return box;
    }

    private void wireMutuallyExclusiveLeagueModes(JCheckBox confRealignment,
                                                  JCheckBox advancedRealignment,
                                                  JCheckBox universalProRel) {
        advancedRealignment.addActionListener(e -> {
            if (advancedRealignment.isSelected()) {
                confRealignment.setSelected(true);
                universalProRel.setSelected(false);
            }
        });
        confRealignment.addActionListener(e -> {
            if (confRealignment.isSelected()) {
                universalProRel.setSelected(false);
            }
        });
        universalProRel.addActionListener(e -> {
            if (universalProRel.isSelected()) {
                confRealignment.setSelected(false);
                advancedRealignment.setSelected(false);
            }
        });
    }

    private static final class NewGameOptions {
        PrestigeMode mode = PrestigeMode.DEFAULT;
        boolean expandedPlayoffs;
        boolean showPotential;
        boolean fullGameLog;
        boolean careerMode = true;
        boolean neverRetire;
        boolean enableTv = true;
        boolean conferenceRealignment = true;
        boolean advancedRealignment;
        boolean universalProRel;
    }

    private void showTeamPickerPage() {
        getContentPane().removeAll();
        getContentPane().setBackground(DesktopTheme.windowBackground());

        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(true);
        page.setBackground(DesktopTheme.windowBackground());
        page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Select Your Team");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        page.add(title, BorderLayout.NORTH);

        List<Conference> conferences = resultLeague.getConferences();
        DefaultListModel<Conference> confModel = new DefaultListModel<>();
        for (Conference c : conferences) {
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
                JLabel l = (JLabel) super.getListCellRendererComponent(list, c.confName, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
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
                Team t = (Team) value;
                String label = String.format("%-22s  Prestige %d", t.getName(), t.getTeamPrestige());
                JLabel l = (JLabel) super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });

        JLabel teamInfo = new JLabel("Select a conference, then pick your team.");
        teamInfo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        teamInfo.setForeground(DesktopTheme.textPrimary());
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
                teamInfo.setText(String.format("<html><body style='color:%s;'><b>%s</b> (%s)  &bull;  %s  &bull;  Prestige %d  &bull;  HC: %s (OVR %d)</body></html>",
                        DesktopTheme.cssRgb(DesktopTheme.textPrimary()),
                        DesktopTheme.escapeForHtml(sel.getName()),
                        DesktopTheme.escapeForHtml(sel.getAbbr()),
                        DesktopTheme.escapeForHtml(sel.getConference()),
                        sel.getTeamPrestige(),
                        DesktopTheme.escapeForHtml(sel.getHeadCoach().name),
                        sel.getHeadCoach().ratOvr));
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
        JButton backBtn = new JButton("\u25C0 Back");
        backBtn.addActionListener(e -> {
            resultLeague = null;
            showPrestigeModePage();
        });
        JButton startBtn = new JButton("Start Dynasty \u25B6");
        startBtn.addActionListener(e -> {
            Team sel = teamList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog("Please select a team."),
                        "No Team Selected",
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
