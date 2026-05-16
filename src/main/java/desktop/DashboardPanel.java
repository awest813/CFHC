package desktop;

import recruiting.RecruitingSessionData;
import simulation.CoachSkills;
import simulation.GameUiBridge;
import simulation.League;
import simulation.SimulationFacade;
import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardPanel implements LeagueScreen {

    private final Runnable playWeek;
    private final Runnable advanceFullYear;
    private final Runnable selectRecruitingTab;
    private final Runnable openUserTeamDetail;
    private final Runnable saveLeague;
    private final Runnable showPlaybookDialog;
    private final Runnable showBowlWatch;
    private final Runnable selectScreenScoreboard;
    private final Runnable selectScreenNews;
    private final Runnable selectScreenPoll;
    private final Runnable selectScreenPlayerStats;
    private final Runnable selectScreenRecruiting;
    private final Runnable selectScreenStandings;
    private final DesktopUiBridge bridge;
    private final League league;

    public DashboardPanel(League league, DesktopUiBridge bridge,
                          Runnable playWeek, Runnable advanceFullYear,
                          Runnable selectRecruitingTab, Runnable openUserTeamDetail,
                          Runnable saveLeague, Runnable showPlaybookDialog,
                          Runnable showBowlWatch,
                          Runnable selectScreenScoreboard, Runnable selectScreenNews,
                          Runnable selectScreenPoll, Runnable selectScreenPlayerStats,
                          Runnable selectScreenRecruiting, Runnable selectScreenStandings) {
        this.league = league;
        this.bridge = bridge;
        this.playWeek = playWeek;
        this.advanceFullYear = advanceFullYear;
        this.selectRecruitingTab = selectRecruitingTab;
        this.openUserTeamDetail = openUserTeamDetail;
        this.saveLeague = saveLeague;
        this.showPlaybookDialog = showPlaybookDialog;
        this.showBowlWatch = showBowlWatch;
        this.selectScreenScoreboard = selectScreenScoreboard;
        this.selectScreenNews = selectScreenNews;
        this.selectScreenPoll = selectScreenPoll;
        this.selectScreenPlayerStats = selectScreenPlayerStats;
        this.selectScreenRecruiting = selectScreenRecruiting;
        this.selectScreenStandings = selectScreenStandings;
    }

    @Override
    public String title() {
        return "Home";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        return buildPanel();
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(buildCommandCenterHero(), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(true);
        grid.setBackground(DesktopTheme.windowBackground());

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(true);
        left.setBackground(DesktopTheme.windowBackground());
        JPanel leftTop = new JPanel(new BorderLayout(0, 12));
        leftTop.setOpaque(false);
        leftTop.add(buildProgramHealthPanel(), BorderLayout.NORTH);
        leftTop.add(buildNextMovesPanel(), BorderLayout.CENTER);
        left.add(leftTop, BorderLayout.NORTH);
        left.add(buildLatestHeadlinesPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(true);
        right.setBackground(DesktopTheme.windowBackground());
        right.add(buildPollLeadersPanel(), BorderLayout.NORTH);
        right.add(buildAwardsPanel(), BorderLayout.CENTER);

        grid.add(left);
        grid.add(right);
        panel.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(true);
        bottom.setBackground(DesktopTheme.windowBackground());
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setBorder(DesktopTheme.titledBorder("Quick navigation"));
        quick.add(mkNavButton("Standings", selectScreenStandings));
        quick.add(mkNavButton("Scoreboard", selectScreenScoreboard));
        quick.add(mkNavButton("Poll Rankings", selectScreenPoll));
        quick.add(mkNavButton("Player Stats", selectScreenPlayerStats));
        quick.add(mkNavButton("News", selectScreenNews));
        quick.add(mkNavButton("Recruiting", selectScreenRecruiting));
        if (league.userTeam != null) {
            JButton my = new JButton("\u2605 My Program");
            my.setToolTipText("Roster, depth chart, and team tools (Ctrl+U)");
            my.addActionListener(e -> openUserTeamDetail.run());
            quick.add(my);
        }
        DesktopTheme.styleToolbar(quick);
        bottom.add(quick, BorderLayout.NORTH);

        JLabel hint = new JLabel("Main focus: advance the season, read what changed, then act on the next program need. F1 for shortcuts.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(DesktopTheme.textSecondary());
        bottom.add(hint, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildCommandCenterHero() {
        JPanel hero = new JPanel(new BorderLayout(16, 0));
        hero.setOpaque(true);
        hero.setBackground(DesktopTheme.pollLeaderCard());
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel actionBlock = new JPanel(new BorderLayout(0, 8));
        actionBlock.setOpaque(false);
        JLabel eyebrow = new JLabel("COACH COMMAND CENTER");
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        eyebrow.setForeground(DesktopTheme.textSecondary());
        JLabel nextAction = new JLabel(playWeekLabel());
        nextAction.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        nextAction.setForeground(DesktopTheme.textPrimary());
        JLabel context = new JLabel("<html><body style='width:420px;'>"
                + DesktopTheme.escapeForHtml(buildNextActionContext()) + "</body></html>");
        context.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        context.setForeground(DesktopTheme.textSecondary());
        actionBlock.add(eyebrow, BorderLayout.NORTH);
        actionBlock.add(nextAction, BorderLayout.CENTER);
        actionBlock.add(context, BorderLayout.SOUTH);
        hero.add(actionBlock, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.add(buildSeasonTimelinePanel(), BorderLayout.NORTH);
        right.add(buildLastResultPanel(), BorderLayout.CENTER);
        hero.add(right, BorderLayout.EAST);
        return hero;
    }

    private String playWeekLabel() {
        int week = league.currentWeek;
        int reg = league.regSeasonWeeks;
        if (week >= reg + 13) return "Recruiting\u2026";
        if (week >= reg + 4)  return "Offseason: Step " + (week - reg - 3);
        if (week == reg + 3)  return "Play National Championship";
        if (week == reg + 2)  return "Play Semifinals / Bowl Week 3";
        if (week == reg + 1)  return "Play Quarterfinals / Bowl Week 2";
        if (week == reg)      return "Play First Round / Bowl Week 1";
        if (week == reg - 1)  return "Play Conf. Championships";
        if (week <= 0)        return "Begin Season";
        return "Play Week " + (week + 1);
    }

    private String buildNextActionContext() {
        int week = league.currentWeek;
        int reg = league.regSeasonWeeks;
        if (bridge != null && bridge.isAwaitingDockedRecruiting()) {
            return "Finish recruiting before rolling into the next year.";
        }
        if (week >= reg + 13) return "Signing day is live. Review recruits and finalize your class.";
        if (week >= reg + 4)  return "Offseason progression. Contracts, transfers, and recruiting setup.";
        if (week >= reg)      return "Postseason games. Each week advances the playoff or bowl bracket.";
        if (week <= 0)        return "Pre-season setup. Review your roster and set playbooks before kickoff.";
        return "Week " + week + " of " + reg + ". Simulate the next set of games.";
    }

    private JPanel buildSeasonTimelinePanel() {
        JPanel timeline = new JPanel(new GridLayout(1, 6, 4, 0));
        timeline.setOpaque(false);
        String active = decodeSeasonPeriod();
        String[] phases = {"Pre-Season", "Regular Season", "Postseason", "Offseason", "Recruiting", "Next Year"};
        for (String phase : phases) {
            JLabel label = new JLabel(phase, JLabel.CENTER);
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            label.setOpaque(true);
            boolean isActive = phase.equals(active)
                    || ("Recruiting".equals(phase) && league.currentWeek >= league.regSeasonWeeks + 13);
            label.setBackground(isActive ? DesktopTheme.sidebarSelectionBackground() : DesktopTheme.windowBackground());
            label.setForeground(isActive ? Color.WHITE : DesktopTheme.textSecondary());
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            timeline.add(label);
        }
        return timeline;
    }

    private String decodeSeasonPeriod() {
        return simulation.SeasonPresentation.getSeasonCycleLabel(league);
    }

    private JPanel buildLastResultPanel() {
        JPanel result = new JPanel(new BorderLayout(0, 2));
        result.setOpaque(false);
        JLabel label = new JLabel("Recent Outcome");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setForeground(DesktopTheme.textSecondary());
        JLabel value = new JLabel("<html><body style='width:360px;'>"
                + DesktopTheme.escapeForHtml(buildRecentOutcomeText()) + "</body></html>");
        value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        value.setForeground(DesktopTheme.textPrimary());
        result.add(label, BorderLayout.NORTH);
        result.add(value, BorderLayout.CENTER);
        return result;
    }

    private String buildRecentOutcomeText() {
        Team user = league.userTeam;
        if (user == null) return "No user team selected.";
        List<simulation.Game> schedule = user.getGameSchedule();
        if (schedule.isEmpty()) return "Season hasn't started yet.";
        simulation.Game last = schedule.get(schedule.size() - 1);
        if (!last.hasPlayed) return "Waiting for next game result.";
        String opponentName = last.homeTeam == user
                ? (last.awayTeam != null ? last.awayTeam.getName() : "Opponent")
                : (last.homeTeam != null ? last.homeTeam.getName() : "Opponent");
        int score = last.homeTeam == user ? last.homeScore : last.awayScore;
        int oppScore = last.homeTeam == user ? last.awayScore : last.homeScore;
        String result = score > oppScore ? "Win" : (score < oppScore ? "Loss" : "Tie");
        return result + " " + score + "-" + oppScore + " vs " + opponentName;
    }

    private JPanel buildProgramHealthPanel() {
        JPanel health = new JPanel(new BorderLayout(0, 8));
        health.setOpaque(false);
        health.setBorder(DesktopTheme.titledBorder("Program Health"));
        JPanel cards = new JPanel(new GridLayout(0, 3, 8, 8));
        cards.setOpaque(false);

        Color cardBg = DesktopTheme.pollLeaderCard();
        Color cardFg = DesktopTheme.textPrimary();
        Team user = league.userTeam;
        cards.add(makeStatCard("Current Period", decodeSeasonPeriod(), cardBg, cardFg));
        if (user != null) {
            cards.add(makeStatCard("Next Action", playWeekLabel(), cardBg, cardFg));
            cards.add(makeStatCard("Recruiting Budget", buildRecruitingBudgetLabel(user), cardBg, cardFg));
            cards.add(makeStatCard("NIL Collective", "Tier " + user.getNilCollectiveLevel(), cardBg, cardFg));
            cards.add(makeStatCard("Skill Progress", buildCoachSkillLabel(user), cardBg, cardFg));
            cards.add(makeStatCard("Roster Health", buildRosterHealthLabel(user), cardBg, cardFg));
        } else {
            cards.add(makeStatCard("Next Action", playWeekLabel(), cardBg, cardFg));
            cards.add(makeStatCard("Recruiting Budget", "-", cardBg, cardFg));
            cards.add(makeStatCard("NIL Collective", "-", cardBg, cardFg));
            cards.add(makeStatCard("Skill Progress", "-", cardBg, cardFg));
            cards.add(makeStatCard("Roster Health", "-", cardBg, cardFg));
        }
        health.add(cards, BorderLayout.CENTER);
        return health;
    }

    private String buildRecruitingBudgetLabel(Team user) {
        if (user == null) return "-";
        try {
            RecruitingSessionData session = SimulationFacade.prepareRecruitingSession(user);
            return "$" + session.recruitingBudget;
        } catch (RuntimeException ex) {
            return "$" + user.getUserRecruitBudget();
        }
    }

    private String buildCoachSkillLabel(Team user) {
        HeadCoach hc = user != null ? user.getHeadCoach() : null;
        if (hc == null) return "-";
        int totalRanks = 0;
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            totalRanks += CoachSkills.getRank(hc.coachSkillRanksBits, b);
        }
        return hc.coachSkillXp + " XP / " + totalRanks + " ranks";
    }

    private String buildRosterHealthLabel(Team user) {
        if (user == null) return "-";
        int roster = user.getAllPlayers().size();
        if (roster >= SimulationFacade.MIN_ROSTER_SIZE) return roster + " ready";
        return roster + " / " + SimulationFacade.MIN_ROSTER_SIZE;
    }

    private JPanel makeStatCard(String label, String value, Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout(4, 2));
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lbl.setForeground(DesktopTheme.textSecondary());
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 14));
        val.setForeground(fg);
        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildNextMovesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(DesktopTheme.titledBorder("What to Do Next"));

        JPanel moves = new JPanel(new GridLayout(0, 1, 4, 4));
        moves.setOpaque(false);
        for (DashboardMove move : buildDashboardMoves()) {
            JButton button = new JButton(move.label);
            button.setToolTipText(move.tooltip);
            button.setHorizontalAlignment(JButton.LEFT);
            button.setFont(new Font("SansSerif", Font.PLAIN, 13));
            button.setFocusPainted(false);
            button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            button.addActionListener(e -> move.action.run());
            moves.add(button);
        }
        DesktopTheme.styleToolbar(moves);
        panel.add(moves, BorderLayout.CENTER);
        return panel;
    }

    private List<DashboardMove> buildDashboardMoves() {
        List<DashboardMove> moves = new ArrayList<>();
        int week = league.currentWeek;
        int reg = league.regSeasonWeeks;
        if (bridge != null && bridge.isAwaitingDockedRecruiting()) {
            moves.add(new DashboardMove("Finish recruiting", "Open the signing board.", selectRecruitingTab));
            moves.add(new DashboardMove("Review roster", "Open your program detail.", openUserTeamDetail));
            moves.add(new DashboardMove("Save league", "Save before rolling into next year.", saveLeague));
            return moves;
        }
        if (week >= reg + 13) {
            moves.add(new DashboardMove("Open signing day", "Load final recruiting into the Recruiting tab.", playWeek));
            moves.add(new DashboardMove("Review recruiting", "Open current recruiting board.", selectRecruitingTab));
            moves.add(new DashboardMove("Save league", "Save before signing day.", saveLeague));
        } else if (week >= reg + 4) {
            moves.add(new DashboardMove("Advance offseason", "Continue contracts, jobs, transfers, and recruiting setup.", advanceFullYear));
            if (league.userTeam != null) {
                moves.add(new DashboardMove("Program tools", "Open coach program and NIL tools.", () -> {}));
            } else {
                moves.add(new DashboardMove("Choose program", "Pick a user-controlled team for program tools.", openUserTeamDetail));
            }
            moves.add(new DashboardMove("Save league", "Save current offseason state.", saveLeague));
        } else if (week >= reg) {
            moves.add(new DashboardMove("Play postseason", "Advance the next postseason game window.", playWeek));
            moves.add(new DashboardMove("Bowl watch", "Review playoff and bowl picture.", showBowlWatch));
            moves.add(new DashboardMove("Scoreboard", "Review completed postseason games.", selectScreenScoreboard));
        } else if (week <= 0) {
            moves.add(new DashboardMove("Begin season", "Simulate preseason setup and start the schedule.", playWeek));
            moves.add(new DashboardMove("Set playbooks", "Tune offensive and defensive strategy.", showPlaybookDialog));
            moves.add(new DashboardMove("Review roster", "Open your program detail.", openUserTeamDetail));
        } else {
            moves.add(new DashboardMove("Play next week", "Simulate the next week.", playWeek));
            moves.add(new DashboardMove("Review scoreboard", "Check this week and prior results.", selectScreenScoreboard));
            moves.add(new DashboardMove("Adjust playbooks", "Tune offensive and defensive strategy.", showPlaybookDialog));
        }
        return moves;
    }

    private static final class DashboardMove {
        final String label;
        final String tooltip;
        final Runnable action;
        DashboardMove(String label, String tooltip, Runnable action) {
            this.label = label;
            this.tooltip = tooltip;
            this.action = action;
        }
    }

    private JPanel buildLatestHeadlinesPanel() {
        JPanel news = new JPanel(new BorderLayout());
        news.setOpaque(true);
        news.setBackground(DesktopTheme.windowBackground());
        news.setBorder(DesktopTheme.titledBorder("Latest Headlines"));
        DefaultListModel<String> newsModel = new DefaultListModel<>();
        if (league.getNewsHeadlines() != null) {
            league.getNewsHeadlines().stream().limit(8).forEach(newsModel::addElement);
        }
        if (newsModel.isEmpty()) {
            newsModel.addElement("No headlines yet. Advance the week to generate league news.");
        }
        JList<String> newsList = new JList<>(newsModel);
        newsList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        newsList.setVisibleRowCount(6);
        DesktopTheme.styleListShell(newsList);
        newsList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                String fg = isSelected ? "rgb(255,255,255)" : DesktopTheme.cssRgb(DesktopTheme.textPrimary());
                l.setText("<html><body style='width:250px;color:" + fg + ";'>- "
                        + DesktopTheme.escapeForHtml(value.toString()) + "</body></html>");
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });
        JScrollPane dashNewsScroll = new JScrollPane(newsList);
        dashNewsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        dashNewsScroll.setOpaque(true);
        news.add(dashNewsScroll, BorderLayout.CENTER);
        return news;
    }

    private JPanel buildPollLeadersPanel() {
        JPanel top5 = new JPanel(new GridLayout(0, 1, 4, 2));
        top5.setOpaque(true);
        top5.setBackground(DesktopTheme.windowBackground());
        top5.setBorder(DesktopTheme.titledBorder("Poll Leaders"));
        league.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(5)
                .forEach(t -> {
                    JLabel l = new JLabel(String.format(Locale.ROOT, " #%-2d %-18s  (%d-%d)",
                            t.getRankTeamPollScore(), t.getName(), t.getWins(), t.getLosses()));
                    l.setFont(new Font("SansSerif", Font.BOLD, 12));
                    l.setOpaque(true);
                    l.setBackground(DesktopTheme.pollLeaderCard());
                    l.setForeground(DesktopTheme.textPrimary());
                    l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                    top5.add(l);
                });
        return top5;
    }

    private JPanel buildAwardsPanel() {
        JPanel awards = new JPanel(new BorderLayout());
        awards.setOpaque(true);
        awards.setBackground(DesktopTheme.windowBackground());
        awards.setBorder(DesktopTheme.titledBorder("Awards Race"));
        JTextArea awardsArea = new JTextArea();
        awardsArea.setEditable(false);
        String awardsText = league.getHeismanWinnerStrFull();
        if (awardsText == null || awardsText.trim().isEmpty()) {
            awardsText = "Awards tracking appears once the season has enough statistics.";
        }
        awardsArea.setText(awardsText);
        DesktopTheme.styleTextContent(awardsArea);
        JScrollPane awardsScroll = new JScrollPane(awardsArea);
        awardsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        awards.add(awardsScroll, BorderLayout.CENTER);
        return awards;
    }

    private JButton mkNavButton(String tabTitle, Runnable action) {
        JButton b = new JButton(tabTitle);
        b.setToolTipText("Open " + tabTitle);
        b.addActionListener(e -> action.run());
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        DesktopTheme.stylePrimaryButton(b);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }
}