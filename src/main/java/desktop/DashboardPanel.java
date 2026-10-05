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
import javax.swing.JButton;
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
                    String text = DesktopRecruitingBudget.format(now);
                    healthBudget.setText(text);
                    ((JPanel) healthBudget.getParent()).setToolTipText("Recruiting Budget: " + text);
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
        return SeasonPresentation.getPlayWeekLabel(league);
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
        CustomCardPanel health = new CustomCardPanel("Program Health");
        // 2x2 HUD stat tiles. "Current Period" / "Next Action" were dropped —
        // they duplicated the Coach Command Center hero directly above.
        JPanel cards = new JPanel(new GridLayout(2, 2, 8, 8));
        cards.setOpaque(false);

        Team user = league.userTeam;
        if (user != null) {
            JPanel budgetCard = makeStatCard("Recruiting Budget", buildRecruitingBudgetLabel(user), DesktopTheme.successGreen());
            healthBudgetLabel = (JLabel) budgetCard.getComponent(1);
            cards.add(budgetCard);
            cards.add(makeStatCard("NIL Collective", "Tier " + user.getNilCollectiveLevel() + " / " + simulation.League.NIL_MAX_TIER, DesktopTheme.warningText()));
            cards.add(makeStatCard("Skill Progress", buildCoachSkillLabel(user), DesktopTheme.textPrimary()));
            cards.add(makeStatCard("Roster Health", buildRosterHealthLabel(user), DesktopTheme.textPrimary()));
        } else {
            healthBudgetLabel = null;
            cards.add(makeStatCard("Recruiting Budget", "-", DesktopTheme.textPrimary()));
            cards.add(makeStatCard("NIL Collective", "-", DesktopTheme.textPrimary()));
            cards.add(makeStatCard("Skill Progress", "-", DesktopTheme.textPrimary()));
            cards.add(makeStatCard("Roster Health", "-", DesktopTheme.textPrimary()));
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
        return hc.coachSkillXp + " XP \u2022 " + totalRanks + " RK";
    }

    private String buildRosterHealthLabel(Team user) {
        if (user == null) return "-";
        int roster = user.getAllPlayers().size();
        if (roster >= RosterRules.MIN_DEPTH_PLAYERS) return roster + " ready";
        return roster + " / " + RosterRules.MIN_DEPTH_PLAYERS;
    }

    /** Rounded inset tile (stripe fill + 1px subtle border) used for HUD stats and list rows. */
    private static JPanel hudTile(java.awt.LayoutManager layout) {
        JPanel tile = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.tableStripe());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tile.setOpaque(false);
        return tile;
    }

    /** HUD stat tile: small muted uppercase label over a monospaced value. */
    private JPanel makeStatCard(String label, String value, Color valueColor) {
        JPanel card = hudTile(new GridLayout(2, 1, 0, 2));
        card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        card.setToolTipText(label + ": " + value);
        JLabel caption = new JLabel(label.toUpperCase(Locale.ROOT));
        caption.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        caption.setForeground(DesktopTheme.textSecondary());
        JLabel val = new JLabel(value);
        val.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        val.setForeground(valueColor);
        card.add(caption);
        card.add(val);
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
        List<String> headlines = new ArrayList<>();
        if (league.getNewsHeadlines() != null) {
            league.getNewsHeadlines().stream().limit(8).map(DashboardPanel::headlineTitle).forEach(headlines::add);
        }

        // Word-wrapped rows in a panel that tracks the viewport width, so each
        // headline wraps to the card instead of being clipped at the right edge.
        WidthTrackingPanel list = new WidthTrackingPanel();
        list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
        list.setOpaque(false);
        if (headlines.isEmpty()) {
            list.add(mutedCaption("No headlines yet. Advance the week to generate league news."));
        }
        for (int i = 0; i < headlines.size(); i++) {
            JTextArea row = new JTextArea(headlines.get(i));
            row.setEditable(false);
            row.setFocusable(false);
            row.setLineWrap(true);
            row.setWrapStyleWord(true);
            row.setOpaque(false);
            row.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            row.setForeground(DesktopTheme.textPrimary());
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, i < headlines.size() - 1 ? 1 : 0, 0,
                            DesktopTheme.borderSubtle()),
                    BorderFactory.createEmptyBorder(4, 2, 4, 2)));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(row);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        news.getContentArea().add(scroll, BorderLayout.CENTER);
        return news;
    }

    /** Vertical list container that always matches the scroll viewport width (enables word-wrap). */
    private static final class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable {
        @Override public java.awt.Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) { return 12; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) { return Math.max(12, r.height - 12); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static JLabel mutedCaption(String text) {
        JLabel l = new JLabel("<html><div style='text-align:center'>"
                + DesktopTheme.escapeForHtml(text) + "</div></html>", JLabel.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        l.setForeground(DesktopTheme.textSecondary());
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /** Engine news entries may be "headline>story"; the card shows the headline. */
    private static String headlineTitle(String entry) {
        if (entry == null) return "";
        int gt = entry.indexOf('>');
        return gt > 0 ? entry.substring(0, gt).trim() : entry.trim();
    }

    private JPanel buildPollLeadersPanel() {
        CustomCardPanel card = new CustomCardPanel("Poll Leaders");
        JPanel rows = new JPanel(new GridLayout(5, 1, 0, 3));
        rows.setOpaque(false);
        league.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(5)
                .forEach(t -> {
                    // Three aligned columns: gold rank | team | mono record (right).
                    JPanel r = hudTile(new BorderLayout(8, 0));
                    r.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    boolean isUser = t == league.userTeam;
                    JLabel rank = new JLabel("#" + t.getRankTeamPollScore());
                    rank.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                    rank.setForeground(DesktopTheme.warningText());
                    rank.setPreferredSize(new java.awt.Dimension(32, 16));
                    JLabel name = new JLabel(t.getName());
                    name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                    name.setForeground(isUser ? DesktopTheme.successGreen() : DesktopTheme.textPrimary());
                    JLabel rec = new JLabel(t.getWins() + "-" + t.getLosses(), JLabel.RIGHT);
                    rec.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                    rec.setForeground(DesktopTheme.textSecondary());
                    r.add(rank, BorderLayout.WEST);
                    r.add(name, BorderLayout.CENTER);
                    r.add(rec, BorderLayout.EAST);
                    rows.add(r);
                });
        card.getContentArea().add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAwardsPanel() {
        CustomCardPanel card = new CustomCardPanel("Awards Race");
        String awardsText = league.getHeismanWinnerStrFull();
        boolean empty = awardsText == null || awardsText.trim().isEmpty()
                || "No winner decided yet.".equals(awardsText.trim());
        if (empty) {
            String caption = awardsText == null || awardsText.trim().isEmpty()
                    ? "Awards tracking appears once the season has enough statistics."
                    : awardsText.trim();
            card.getContentArea().add(mutedCaption(caption), BorderLayout.CENTER);
            return card;
        }
        JTextArea awardsArea = new JTextArea(awardsText);
        awardsArea.setEditable(false);
        awardsArea.setLineWrap(true);
        awardsArea.setWrapStyleWord(true);
        awardsArea.setOpaque(false);
        awardsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        awardsArea.setForeground(DesktopTheme.textPrimary());
        awardsArea.setCaretPosition(0);
        JScrollPane awardsScroll = new JScrollPane(awardsArea);
        awardsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        awardsScroll.setBorder(BorderFactory.createEmptyBorder());
        awardsScroll.setOpaque(false);
        awardsScroll.getViewport().setOpaque(false);
        card.getContentArea().add(awardsScroll, BorderLayout.CENTER);
        return card;
    }

    private JButton mkNavButton(String tabTitle, Runnable action, boolean isAccent) {
        JButton b = new JButton(tabTitle);
        b.setToolTipText("Open " + tabTitle);
        b.addActionListener(e -> action.run());
        DesktopTheme.styleHudQuickButton(b, isAccent);
        return b;
    }
}
