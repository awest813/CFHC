package desktop;

import simulation.League;
import simulation.Team;
import simulation.TeamColors;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Career / dynasty dashboard: the player's head-coach profile, bound entirely
 * to real {@link HeadCoach} / {@link Team} data. Replaces the old flat label
 * grid + raw history text dump with a three-section layout:
 *
 *   A. Career summary header band (record, titles, years, awards, contract).
 *   B. Coach attributes with color-coded rating badges + scheme info.
 *   C. Career history timeline parsed from the coach's history strings.
 */
public class CoachProfilePanel implements LeagueScreen {

    @Override
    public String title() {
        return "My Coach";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        League league = ctx.league();
        if (league.userTeam == null) {
            return emptyState("No program selected",
                    "Start or load a career with a team to view your coach profile here.");
        }
        Team ut = league.userTeam;
        HeadCoach hc = ut.getHeadCoach();
        if (hc == null) {
            return emptyState("No head coach found",
                    "This team does not have a head coach assigned.");
        }

        JPanel panel = new JPanel(new BorderLayout(16, 16));
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(DesktopTheme.buildScreenHeader("My Coach",
                "Your head coach profile, ratings, and career history."), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        body.add(buildCareerSummary(ut, hc));
        body.add(Box.createVerticalStrut(12));
        body.add(buildAttributes(hc));
        body.add(Box.createVerticalStrut(12));
        body.add(buildHistoryTimeline(hc));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ---- Section A: Career summary header band ---------------------------

    private JPanel buildCareerSummary(Team ut, HeadCoach hc) {
        JPanel section = new JPanel(new BorderLayout(12, 8));
        section.setOpaque(true);
        section.setBackground(DesktopTheme.windowBackground());
        section.setBorder(DesktopTheme.titledBorder("Coach Career"));

        Color teamPrimary = TeamColors.primary(ut.getAbbr());

        // Name + team with team-color accent.
        JPanel nameBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        nameBlock.setOpaque(false);
        JLabel name = new JLabel(hc.name);
        name.setFont(new Font("SansSerif", Font.BOLD, 20));
        name.setForeground(DesktopTheme.textPrimary());
        JLabel teamTag = new JLabel(ut.getName());
        teamTag.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teamTag.setForeground(teamPrimary != null ? teamPrimary : DesktopTheme.accentBlue());
        nameBlock.add(name);
        nameBlock.add(teamTag);

        // Job-security chip (same logic as DesktopHeaderBar).
        JPanel securityChip = buildSecurityChip(ut, hc);
        nameBlock.add(securityChip);

        // Contract line.
        String contractTxt = hc.contractLength > 0
                ? "Yr " + (hc.contractYear + 1) + " of " + hc.contractLength
                : "Contract: \u2014";
        JLabel contract = new JLabel(contractTxt);
        contract.setFont(new Font("SansSerif", Font.PLAIN, 11));
        contract.setForeground(DesktopTheme.textSecondary());
        nameBlock.add(contract);

        // Headline stat tiles.
        int wins = hc.getWins();
        int losses = hc.getLosses();
        int games = wins + losses;
        String winPct = games > 0
                ? String.format("%.3f", (double) wins / games)
                : "\u2014";

        JPanel statsRow = new JPanel(new GridLayout(2, 4, 10, 6));
        statsRow.setOpaque(false);
        statsRow.add(statTile("Record", wins + "-" + losses));
        statsRow.add(statTile("Win %", winPct));
        statsRow.add(statTile("Nat'l Titles", String.valueOf(hc.getNCWins())));
        statsRow.add(statTile("Conf Titles", String.valueOf(hc.getConfWins())));
        statsRow.add(statTile("Years", String.valueOf(hc.year)));
        statsRow.add(statTile("COTY", String.valueOf(hc.getCOTY())));
        statsRow.add(statTile("Conf COTY", String.valueOf(hc.getConfCOTY())));
        statsRow.add(statTile("Prestige", String.valueOf(hc.getCumulativePrestige())));

        section.add(nameBlock, BorderLayout.NORTH);
        section.add(statsRow, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildSecurityChip(Team ut, HeadCoach hc) {
        Color color;
        String label;
        if (ut.fired) {
            color = DesktopTheme.dangerRed();
            label = "HOT SEAT";
        } else if (hc.contractLength - hc.contractYear <= 2) {
            color = DesktopTheme.warningText();
            label = "ON WATCH";
        } else {
            color = DesktopTheme.successGreen();
            label = "SECURE";
        }
        final Color pillColor = color;
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(17, 28, 46));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(pillColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        JLabel t = new JLabel(label);
        t.setFont(new Font("SansSerif", Font.BOLD, 9));
        t.setForeground(pillColor);
        chip.add(t);
        return chip;
    }

    private JLabel statTile(String label, String value) {
        JPanel tile = new JPanel(new GridLayout(0, 1, 0, 1));
        tile.setOpaque(false);
        JLabel v = new JLabel(value, JLabel.CENTER);
        v.setFont(new Font("SansSerif", Font.BOLD, 16));
        v.setForeground(DesktopTheme.textPrimary());
        JLabel l = new JLabel(label, JLabel.CENTER);
        l.setFont(new Font("SansSerif", Font.PLAIN, 9));
        l.setForeground(DesktopTheme.textSecondary());
        tile.add(v);
        tile.add(l);
        // Wrap in a label-style container isn't possible; return a label-styled
        // panel by abusing JLabel is messy, so callers get a JPanel via add.
        // To keep GridLayout happy we return a lightweight host.
        JLabel host = new JLabel();
        host.setLayout(new BorderLayout());
        host.add(tile, BorderLayout.CENTER);
        host.setOpaque(false);
        return host;
    }

    // ---- Section B: Coach attributes with color-coded badges ------------

    private JPanel buildAttributes(HeadCoach hc) {
        JPanel section = new JPanel(new BorderLayout(10, 8));
        section.setOpaque(true);
        section.setBackground(DesktopTheme.windowBackground());
        section.setBorder(DesktopTheme.titledBorder("Coach Attributes"));

        JPanel ratings = new JPanel(new GridLayout(0, 5, 10, 6));
        ratings.setOpaque(false);
        ratings.add(ratingBadge("OVR", hc.ratOvr));
        ratings.add(ratingBadge("OFF", hc.ratOff));
        ratings.add(ratingBadge("DEF", hc.ratDef));
        ratings.add(ratingBadge("REC", hc.ratTalent));
        ratings.add(ratingBadge("DIS", hc.ratDiscipline));

        // Scheme info.
        JPanel scheme = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        scheme.setOpaque(false);
        String offScheme = withinBounds(hc.offStrat, hc.offPlaybook) ? hc.offPlaybook[hc.offStrat] : "\u2014";
        String defScheme = withinBounds(hc.defStrat, hc.defPlaybook) ? hc.defPlaybook[hc.defStrat] : "\u2014";
        scheme.add(schemeLabel("Offense:", offScheme));
        scheme.add(schemeLabel("Defense:", defScheme));

        section.add(ratings, BorderLayout.CENTER);
        section.add(scheme, BorderLayout.SOUTH);
        return section;
    }

    private boolean withinBounds(int idx, String[] arr) {
        return arr != null && idx >= 0 && idx < arr.length;
    }

    private JLabel schemeLabel(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(DesktopTheme.textSecondary());
        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 11));
        v.setForeground(DesktopTheme.textPrimary());
        p.add(l);
        p.add(v);
        JLabel host = new JLabel();
        host.setLayout(new BorderLayout());
        host.add(p, BorderLayout.CENTER);
        host.setOpaque(false);
        return host;
    }

    private JLabel ratingBadge(String label, int value) {
        Color color;
        if (value >= 85) color = DesktopTheme.successGreen();
        else if (value >= 70) color = DesktopTheme.warningText();
        else color = DesktopTheme.textSecondary();

        JPanel tile = new JPanel(new GridLayout(0, 1, 0, 1));
        tile.setOpaque(false);
        JLabel v = new JLabel(String.valueOf(value), JLabel.CENTER);
        v.setFont(new Font("SansSerif", Font.BOLD, 22));
        v.setForeground(color);
        JLabel l = new JLabel(label, JLabel.CENTER);
        l.setFont(new Font("SansSerif", Font.PLAIN, 9));
        l.setForeground(DesktopTheme.textSecondary());
        tile.add(v);
        tile.add(l);

        JLabel host = new JLabel();
        host.setLayout(new BorderLayout());
        host.add(tile, BorderLayout.CENTER);
        host.setOpaque(false);
        return host;
    }

    // ---- Section C: Career history timeline ------------------------------

    private JPanel buildHistoryTimeline(HeadCoach hc) {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(true);
        section.setBackground(DesktopTheme.windowBackground());
        section.setBorder(DesktopTheme.titledBorder("Career History"));

        List<String> entries = new ArrayList<>();
        if (hc.history != null) {
            for (String s : hc.history) {
                if (s != null && !s.trim().isEmpty()) {
                    entries.add(s);
                }
            }
        }

        if (entries.isEmpty()) {
            JLabel empty = new JLabel("No history yet \u2014 your career starts this season.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(DesktopTheme.textSecondary());
            empty.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            section.add(empty, BorderLayout.CENTER);
            return section;
        }

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        // Show most-recent first.
        for (int i = entries.size() - 1; i >= 0; i--) {
            list.add(buildHistoryRow(entries.get(i)));
            list.add(Box.createVerticalStrut(2));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    /**
     * Parse one history entry of the form
     *   "{year}: #{rank} {team} ({wins}-{losses}) {confChamp} {semi}{natc} Prs: {prestige} (+{delta})"
     * into a color-coded row. Falls back to the raw string if the format is
     * unexpected (some legacy entries may differ).
     */
    private JPanel buildHistoryRow(String entry) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        JLabel main = new JLabel(entry);
        main.setFont(new Font("SansSerif", Font.PLAIN, 12));
        main.setForeground(DesktopTheme.textPrimary());

        // Color-code the result tag if recognizable.
        Color tag = null;
        if (entry.contains("NCW")) tag = DesktopTheme.successGreen();
        else if (entry.contains("NCL") || entry.contains("SFL")) tag = DesktopTheme.warningText();
        else if (entry.contains("CC")) tag = DesktopTheme.accentBlue();

        if (tag != null) {
            JLabel badge = new JLabel(resultTag(entry));
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            badge.setForeground(tag);
            badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            row.add(badge, BorderLayout.EAST);
        }

        row.add(main, BorderLayout.CENTER);
        return row;
    }

    private String resultTag(String entry) {
        if (entry.contains("NCW")) return "NAT'L CHAMP";
        if (entry.contains("NCL")) return "NCG RUNNER-UP";
        if (entry.contains("SFL")) return "PLAYOFFS";
        if (entry.contains("CC")) return "CONF CHAMP";
        return "";
    }

    // ---- Helpers ---------------------------------------------------------

    private JPanel emptyState(String title, String message) {
        JPanel empty = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(empty);
        JLabel msg = new JLabel("<html><div style='text-align:center;width:400px'><b>" + title
                + "</b><br><br>" + message + "</div></html>");
        msg.setFont(new Font("SansSerif", Font.PLAIN, 14));
        msg.setForeground(DesktopTheme.textSecondary());
        msg.setHorizontalAlignment(JLabel.CENTER);
        empty.add(msg, BorderLayout.CENTER);
        return empty;
    }
}
