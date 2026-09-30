package desktop;

import simulation.CoachSkills;
import simulation.League;
import simulation.RosterRules;
import simulation.SeasonFlowOrder;
import simulation.SeasonPresentation;
import simulation.Team;
import simulation.TeamColors;
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
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardPanel implements LeagueScreen {

    public record Callbacks(
        Runnable playWeek,
        Runnable advanceFullYear,
        Runnable selectRecruitingTab,
        Runnable openUserTeamDetail,
        Runnable saveLeague,
        Runnable showPlaybookDialog,
        Runnable showBowlWatch,
        Runnable selectScreenScoreboard,
        Runnable selectScreenNews,
        Runnable selectScreenPoll,
        Runnable selectScreenPlayerStats,
        Runnable selectScreenRecruiting,
        Runnable selectScreenStandings
    ) {}

    private final Callbacks cb;
    private final DesktopUiBridge bridge;
    private final League league;
    /** Resolved per build in {@link #buildPanel}; see {@link DesktopRecruitingBudget}. */
    private int recruitingBudget = -1;
    /** Program Health's budget tile, captured per build for in-place updates. */
    private JLabel healthBudgetLabel;

    public DashboardPanel(League league, DesktopUiBridge bridge, Callbacks cb) {
        this.league = league;
        this.bridge = bridge;
        this.cb = cb;
    }

    @Override
    public String title() {
        return "Home";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        return buildPanel(ctx);
    }

    private JPanel buildPanel(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(buildCommandCenterHero(), BorderLayout.NORTH);

        // Responsive card grid (2-4 columns, minimum row height) in a vertical
        // scroll pane: a fixed 4x4 grid crushed every card at the default
        // 1200x850 window size. See DashboardCardGrid.
        DashboardCardGrid grid = new DashboardCardGrid();

        grid.add(new TeamOverallCard(league.userTeam, () -> {
            if (ctx != null) ctx.nav().openUserTeamDetail();
            else cb.openUserTeamDetail().run();
        }));
        grid.add(new NextGameMatchupCard(league.userTeam, opp -> {
            if (ctx != null && opp != null) ctx.nav().openTeamDetail(opp);
        }));
        grid.add(new TopNewsCarouselCard(league, () -> {
            if (ctx != null) ctx.nav().selectScreen("News");
            else cb.selectScreenNews().run();
        }));
        grid.add(new ConferenceStandingsCard(league.userTeam, () -> {
            if (ctx != null) ctx.nav().selectScreen("Standings");
            else cb.selectScreenStandings().run();
        }, opp -> {
            if (ctx != null && opp != null) ctx.nav().openTeamDetail(opp);
        }));

        grid.add(new WeeklyScheduleCard(league, league.userTeam, opp -> {
            if (ctx != null && opp != null) ctx.nav().openTeamDetail(opp);
        }));
        grid.add(new RecruitingPipelineCard(league.userTeam, () -> {
            if (ctx != null) ctx.nav().selectScreen("Recruiting");
            else cb.selectRecruitingTab().run();
        }));
        // One value for Program Finances and Program Health so they can't
        // disagree; the host window supplies the live board's remaining budget.
        recruitingBudget = DesktopRecruitingBudget.forTeam(league.userTeam,
                ctx != null ? ctx.parent() : null);
        ProgramFinancesCard finances = new ProgramFinancesCard(league.userTeam, recruitingBudget);
        grid.add(finances);
        grid.add(new ProgramPrestigeCard(league.userTeam));

        grid.add(new TeamMoraleCard(league.userTeam));
        grid.add(new RosterSpotlightCard(league.userTeam, player -> {
            if (ctx != null && ctx.parent() != null && player != null) {
                PlayerDetailView.show(ctx.parent(), player);
            }
        }));
        grid.add(new UpcomingGamesCard(league.userTeam, opp -> {
            if (ctx != null && opp != null) ctx.nav().openTeamDetail(opp);
        }));
        grid.add(new HeadCoachCard(league.userTeam, () -> {
            if (ctx != null) ctx.nav().selectScreen("My Coach");
        }));

        // Phase 5 part 3: wire the previously built-but-unmounted dashboard panels.
        grid.add(buildProgramHealthPanel());
        grid.add(buildPollLeadersPanel());
        grid.add(buildAwardsPanel());
        grid.add(buildLatestHeadlinesPanel());

        JScrollPane gridScroll = new JScrollPane(grid,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        gridScroll.setOpaque(false);
        gridScroll.getViewport().setOpaque(false);
        gridScroll.getVerticalScrollBar().setUnitIncrement(24);
        panel.add(gridScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setOpaque(false);
        quick.add(mkNavButton("Play Week", cb.playWeek(), true));
        quick.add(mkNavButton("Standings", cb.selectScreenStandings(), false));
        quick.add(mkNavButton("Scoreboard", cb.selectScreenScoreboard(), false));
        quick.add(mkNavButton("Poll Rankings", cb.selectScreenPoll(), false));
        quick.add(mkNavButton("Player Stats", cb.selectScreenPlayerStats(), false));
        quick.add(mkNavButton("News", cb.selectScreenNews(), false));
        quick.add(mkNavButton("Recruiting", cb.selectScreenRecruiting(), false));
        quick.add(mkNavButton("My Coach", () -> {
            if (ctx != null) ctx.nav().selectScreen("My Coach");
        }, false));
        if (league.userTeam != null) {
            JButton my = new JButton("\u2605 My Program");
            my.setToolTipText("Roster, depth chart, and team tools (Ctrl+U)");
            my.addActionListener(e -> cb.openUserTeamDetail().run());
            DesktopTheme.styleHudQuickButton(my, false);
            quick.add(my);
        }
        bottom.add(quick, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.SOUTH);

        // Sidebar navigation only flips cards; it doesn't rebuild this screen.
        // Re-read the budget whenever Home is shown so money spent on the
        // recruiting board shows up immediately, not after the next week.
        JLabel healthBudget = healthBudgetLabel;
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                int now = DesktopRecruitingBudget.forTeam(league.userTeam,
                        ctx != null ? ctx.parent() : null);
                finances.setRecruitingBudget(now);
                if (healthBudget != null) {
                    setStatCardText(healthBudget, "Recruiting Budget", DesktopRecruitingBudget.format(now),
                            DesktopTheme.textPrimary());
                }
            }
        });

        return panel;
    }

    private JPanel buildCommandCenterHero() {
        JPanel hero = new JPanel(new BorderLayout(16, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                DesktopTheme.paintCardGradient(g, getWidth(), getHeight(),
                    league.userTeam != null
                        ? TeamColors.primary(league.userTeam.getAbbr())
                        : null);
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        // Top row: eyebrow + season timeline. Keeping the 6-chip timeline out
        // of the EAST column stops it from starving the action text of width
        // ("Upcoming: vs Cincinnati (4-" was clipped at 1200px).
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);
        JLabel eyebrow = new JLabel("COACH COMMAND CENTER");
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        eyebrow.setForeground(DesktopTheme.textSecondary());
        topRow.add(eyebrow, BorderLayout.WEST);
        topRow.add(buildSeasonTimelinePanel(), BorderLayout.EAST);
        hero.add(topRow, BorderLayout.NORTH);

        JPanel actionBlock = new JPanel(new BorderLayout(0, 4));
        actionBlock.setOpaque(false);
        JLabel nextAction = new JLabel(playWeekLabel());
        nextAction.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        nextAction.setForeground(DesktopTheme.textPrimary());
        JLabel context = new JLabel("<html><body style='width:400px;'>"
                + "<span style='font-size:14px; font-weight:bold; color:" + DesktopTheme.cssRgb(DesktopTheme.textPrimary()) + "'>"
                + DesktopTheme.escapeForHtml(buildUpcomingMatchupText()) + "</span><br>"
                + "<span style='font-size:12px; color:" + DesktopTheme.cssRgb(DesktopTheme.textSecondary()) + "'>"
                + DesktopTheme.escapeForHtml(buildNextActionContext()) + "</span></body></html>");
        actionBlock.add(nextAction, BorderLayout.NORTH);
        actionBlock.add(context, BorderLayout.CENTER);
        hero.add(actionBlock, BorderLayout.CENTER);

        hero.add(buildLastResultPanel(), BorderLayout.EAST);
        return hero;
    }

    private String playWeekLabel() {
        return SeasonPresentation.getPlayWeekLabel(league.currentWeek, league.regSeasonWeeks);
    }

    private String buildNextActionContext() {
        if (bridge != null && bridge.isAwaitingDockedRecruiting()) {
            return "Finish recruiting before rolling into the next year.";
        }
        return SeasonPresentation.getNextActionHint(league);
    }

    private JPanel buildSeasonTimelinePanel() {
        JPanel timeline = new JPanel(new GridLayout(1, SeasonFlowOrder.CYCLE_ORDER.length + 1, 4, 0));
        timeline.setOpaque(false);
        String active = decodeSeasonPeriod();
        String[] phases = new String[SeasonFlowOrder.CYCLE_ORDER.length + 1];
        System.arraycopy(SeasonFlowOrder.CYCLE_ORDER, 0, phases, 0, SeasonFlowOrder.CYCLE_ORDER.length);
        phases[phases.length - 1] = "Next Year";
        for (String phase : phases) {
            JLabel label = new JLabel(phase, JLabel.CENTER);
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            label.setOpaque(true);
            boolean isActive = phase.equals(active)
                    || ("Recruiting".equals(phase) && SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks));
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
        result.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JLabel label = new JLabel("RECENT OUTCOME");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setForeground(DesktopTheme.textSecondary());
        JLabel value = new JLabel("<html><body style='width:240px;'>"
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
        simulation.Game last = DesktopWeekResult.findMostRecentPlayed(user);
        if (last == null) return "Waiting for next game result.";
        String opponentName = DesktopWeekResult.opponentName(last, user);
        int score = DesktopWeekResult.userScore(last, user);
        int oppScore = DesktopWeekResult.opponentScore(last, user);
        String result = score > oppScore ? "Win" : (score < oppScore ? "Loss" : "Tie");
        return result + " " + score + "-" + oppScore + " vs " + opponentName;
    }

    private String buildUpcomingMatchupText() {
        Team user = league.userTeam;
        if (user == null) return "No user team selected.";
        simulation.Game upcoming = DesktopWeekResult.findUpcomingGame(user);
        if (upcoming == null) return "No upcoming games scheduled.";
        String opponentName = DesktopWeekResult.opponentName(upcoming, user);
        String location = DesktopWeekResult.userIsHome(upcoming, user) ? "vs" : "@";
        Team oppTeam = upcoming.homeTeam == user ? upcoming.awayTeam : upcoming.homeTeam;
        String oppRecord = oppTeam != null ? " (" + oppTeam.getWins() + "-" + oppTeam.getLosses() + ")" : "";
        return "Upcoming: " + location + " " + opponentName + oppRecord;
    }

    private JPanel buildProgramHealthPanel() {
        // HUD card like the 12 above it (was a TitledBorder panel, a second
        // visual language on the same screen).
        CustomCardPanel health = new CustomCardPanel("Program Health");
        // 2 columns: three across a ~325px card left ~100px per stat card,
        // which truncated every label ("Recruiting Bu...").
        JPanel cards = new JPanel(new GridLayout(0, 2, 8, 8));
        cards.setOpaque(false);

        Color cardBg = DesktopTheme.tableStripe();
        Color cardFg = DesktopTheme.textPrimary();
        // 2x2 cards: "Current Period" / "Next Action" were dropped — they
        // duplicated the Coach Command Center hero directly above, and 6
        // cards could not fit the panel's height (rows crushed to ~28px,
        // overlapping label and value).
        Team user = league.userTeam;
        if (user != null) {
            JPanel budgetCard = makeStatCard("Recruiting Budget", buildRecruitingBudgetLabel(user), cardBg, cardFg);
            healthBudgetLabel = (JLabel) budgetCard.getComponent(0);
            cards.add(budgetCard);
            cards.add(makeStatCard("NIL Collective", "Tier " + user.getNilCollectiveLevel(), cardBg, cardFg));
            cards.add(makeStatCard("Skill Progress", buildCoachSkillLabel(user), cardBg, cardFg));
            cards.add(makeStatCard("Roster Health", buildRosterHealthLabel(user), cardBg, cardFg));
        } else {
            healthBudgetLabel = null;
            cards.add(makeStatCard("Recruiting Budget", "-", cardBg, cardFg));
            cards.add(makeStatCard("NIL Collective", "-", cardBg, cardFg));
            cards.add(makeStatCard("Skill Progress", "-", cardBg, cardFg));
            cards.add(makeStatCard("Roster Health", "-", cardBg, cardFg));
        }
        health.getContentArea().add(cards, BorderLayout.CENTER);
        return health;
    }

    private String buildRecruitingBudgetLabel(Team user) {
        return user == null ? "-" : DesktopRecruitingBudget.format(recruitingBudget);
    }

    private String buildCoachSkillLabel(Team user) {
        HeadCoach hc = user != null ? user.getHeadCoach() : null;
        if (hc == null) return "-";
        int totalRanks = 0;
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            totalRanks += CoachSkills.getRank(hc.coachSkillRanksBits, b);
        }
        return hc.coachSkillXp + " XP \u2022 " + totalRanks + (totalRanks == 1 ? " rank" : " ranks");
    }

    private String buildRosterHealthLabel(Team user) {
        if (user == null) return "-";
        int roster = user.getAllPlayers().size();
        if (roster >= RosterRules.MIN_DEPTH_PLAYERS) return roster + " ready";
        return roster + " / " + RosterRules.MIN_DEPTH_PLAYERS;
    }

    private JPanel makeStatCard(String label, String value, Color bg, Color fg) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        // Single HTML label (small caption over bold value): these cards live
        // in ~34px grid rows — two BorderLayout regions get crushed into each
        // other there, one label can't.
        JLabel combined = new JLabel();
        setStatCardText(combined, label, value, fg);
        card.add(combined, BorderLayout.CENTER);
        return card;
    }

    private static void setStatCardText(JLabel target, String label, String value, Color fg) {
        target.setText("<html><span style=\"font-size:9px;color:"
                + DesktopTheme.cssRgb(DesktopTheme.textSecondary()) + "\">"
                + DesktopTheme.escapeForHtml(label.toUpperCase(Locale.ROOT))
                + "</span><br><b style=\"font-size:12px;color:" + DesktopTheme.cssRgb(fg)
                + "\">" + DesktopTheme.escapeForHtml(value) + "</b></html>");
        target.setToolTipText(label + ": " + value);
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
            moves.add(new DashboardMove("Finish recruiting", "Open the signing board.", cb.selectRecruitingTab()));
            moves.add(new DashboardMove("Review roster", "Open your program detail.", cb.openUserTeamDetail()));
            moves.add(new DashboardMove("Save league", "Save before rolling into next year.", cb.saveLeague()));
            return moves;
        }
        if (week >= reg + 13) {
            moves.add(new DashboardMove("Open signing day", "Load final recruiting into the Recruiting tab.", cb.playWeek()));
            moves.add(new DashboardMove("Review recruiting", "Open current recruiting board.", cb.selectRecruitingTab()));
            moves.add(new DashboardMove("Save league", "Save before signing day.", cb.saveLeague()));
        } else if (week >= reg + 4) {
            moves.add(new DashboardMove("Advance offseason", "Continue contracts, jobs, transfers, and recruiting setup.", cb.advanceFullYear()));
            if (league.userTeam != null) {
                moves.add(new DashboardMove("Review roster", "Open your program detail.", cb.openUserTeamDetail()));
            } else {
                moves.add(new DashboardMove("Choose program", "Pick a user-controlled team for program tools.", cb.openUserTeamDetail()));
            }
            moves.add(new DashboardMove("Save league", "Save current offseason state.", cb.saveLeague()));
        } else if (week >= reg) {
            moves.add(new DashboardMove("Play postseason", "Advance the next postseason game window.", cb.playWeek()));
            moves.add(new DashboardMove("Bowl watch", "Review playoff and bowl picture.", cb.showBowlWatch()));
            moves.add(new DashboardMove("Scoreboard", "Review completed postseason games.", cb.selectScreenScoreboard()));
        } else if (week <= 0) {
            moves.add(new DashboardMove("Begin season", "Simulate preseason setup and start the schedule.", cb.playWeek()));
            moves.add(new DashboardMove("Set schemes", "Tune offensive and defensive gameplans.", cb.showPlaybookDialog()));
            moves.add(new DashboardMove("Review roster", "Open your program detail.", cb.openUserTeamDetail()));
        } else {
            moves.add(new DashboardMove("Play next week", "Simulate the next week.", cb.playWeek()));
            moves.add(new DashboardMove("Review scoreboard", "Check this week and prior results.", cb.selectScreenScoreboard()));
            moves.add(new DashboardMove("Adjust schemes", "Tune offensive and defensive gameplans.", cb.showPlaybookDialog()));
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
        CustomCardPanel news = new CustomCardPanel("Latest Headlines");
        DefaultListModel<String> newsModel = new DefaultListModel<>();
        if (league.getNewsHeadlines() != null) {
            league.getNewsHeadlines().stream().limit(8).forEach(newsModel::addElement);
        }
        if (newsModel.isEmpty()) {
            newsModel.addElement("No headlines yet. Advance the week to generate league news.");
        }
        // Track the viewport width: a plain JList sizes itself to its widest
        // (unwrapped) row, so the width-based wrap below never kicked in.
        JList<String> newsList = new JList<>(newsModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        newsList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        newsList.setVisibleRowCount(4);
        DesktopTheme.styleListShell(newsList);
        newsList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                String fg = isSelected ? "rgb(255,255,255)" : DesktopTheme.cssRgb(DesktopTheme.textPrimary());
                // Wrap to the list's real width (a fixed 230px clipped headlines
                // mid-word whenever the card was narrower than the HTML body).
                int wrap = DesktopTheme.htmlWrapWidth(Math.max(120, list.getWidth() - 24));
                l.setText("<html><body style='width:" + wrap + "px;color:" + fg + ";'>"
                        + DesktopTheme.escapeForHtml(headlineTitle(value.toString())) + "</body></html>");
                l.setToolTipText(null);
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });
        // JList caches row heights; re-measure the wrapped rows when the card resizes.
        newsList.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                newsList.setFixedCellHeight(10);
                newsList.setFixedCellHeight(-1);
            }
        });
        JScrollPane dashNewsScroll = new JScrollPane(newsList);
        dashNewsScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dashNewsScroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));
        dashNewsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        dashNewsScroll.setOpaque(true);
        news.getContentArea().add(dashNewsScroll, BorderLayout.CENTER);
        return news;
    }

    /** Engine news entries may be "headline>story"; the list shows the headline. */
    private static String headlineTitle(String entry) {
        if (entry == null) return "";
        int gt = entry.indexOf('>');
        return gt > 0 ? entry.substring(0, gt).trim() : entry.trim();
    }

    private JPanel buildPollLeadersPanel() {
        CustomCardPanel card = new CustomCardPanel("Poll Leaders");
        JPanel top5 = new JPanel(new GridLayout(5, 1, 0, 3));
        top5.setOpaque(false);
        league.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(5)
                .forEach(t -> top5.add(buildPollLeaderRow(t)));
        card.getContentArea().add(top5, BorderLayout.CENTER);
        return card;
    }

    /** Rank | team | record as real columns (space padding misaligned in a proportional font). */
    private JPanel buildPollLeaderRow(Team t) {
        boolean isUser = t == league.userTeam;
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(true);
        row.setBackground(isUser ? DesktopTheme.userTeamRowTint() : DesktopTheme.tableStripe());
        row.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel rank = new JLabel("#" + t.getRankTeamPollScore());
        rank.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        rank.setForeground(DesktopTheme.warningText());
        rank.setPreferredSize(new java.awt.Dimension(32, 18));

        JLabel name = new JLabel(t.getName());
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        name.setForeground(DesktopTheme.textPrimary());

        JLabel record = new JLabel(t.getWins() + "-" + t.getLosses());
        record.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        record.setForeground(DesktopTheme.textSecondary());

        row.add(rank, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(record, BorderLayout.EAST);
        return row;
    }

    private JPanel buildAwardsPanel() {
        CustomCardPanel awards = new CustomCardPanel("Awards Race");
        JTextArea awardsArea = new JTextArea();
        awardsArea.setEditable(false);
        awardsArea.setLineWrap(true);
        awardsArea.setWrapStyleWord(true);
        awardsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        awardsArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        String awardsText = league.getHeismanWinnerStrFull();
        if (awardsText == null || awardsText.trim().isEmpty()) {
            awardsText = "Awards tracking appears once the season has enough statistics.";
        }
        awardsArea.setText(awardsText);
        DesktopTheme.styleTextContent(awardsArea);
        awardsArea.setCaretPosition(0);
        JScrollPane awardsScroll = new JScrollPane(awardsArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        awardsScroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));
        awardsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        awards.getContentArea().add(awardsScroll, BorderLayout.CENTER);
        return awards;
    }

    private JButton mkNavButton(String tabTitle, Runnable action, boolean isAccent) {
        JButton b = new JButton(tabTitle);
        b.setToolTipText("Open " + tabTitle);
        b.addActionListener(e -> action.run());
        DesktopTheme.styleHudQuickButton(b, isAccent);
        return b;
    }
}
