package desktop;

import simulation.Conference;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Swing dashboard card component for CONFERENCE STANDINGS.
 * Displays user's conference standings table (up to 7 teams),
 * highlighting the user's team with an emerald green tint, and
 * provides a clickable "Full Standings" link to open the Standings screen.
 */
public class ConferenceStandingsCard extends CustomCardPanel {

    public ConferenceStandingsCard(Team userTeam, Runnable onViewAll) {
        this(userTeam, onViewAll, null);
    }

    public ConferenceStandingsCard(Team userTeam, Runnable onViewAll, Consumer<Team> onSelectTeam) {
        super(userTeam != null && userTeam.conference != null
                ? userTeam.conference.toUpperCase() + " STANDINGS"
                : "CONFERENCE STANDINGS");
        JPanel content = getContentArea();

        // Right header action: "FULL STANDINGS ▸"
        if (onViewAll != null) {
            JPanel headerRight = new JPanel();
            headerRight.setOpaque(false);
            JLabel viewAll = new JLabel("FULL STANDINGS \u25B8");
            viewAll.setFont(new Font("SansSerif", Font.BOLD, 9));
            viewAll.setForeground(DesktopTheme.successGreen());
            viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            viewAll.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onViewAll.run();
                }
            });
            headerRight.add(viewAll);
            getHeaderBar().add(headerRight, BorderLayout.EAST);
        }

        // Find conference
        Conference conf = null;
        if (userTeam != null && userTeam.league != null) {
            for (Conference c : userTeam.league.getConferences()) {
                if (c.confName != null && c.confName.equals(userTeam.conference)) {
                    conf = c;
                    break;
                }
            }
        }

        List<Team> teams = new ArrayList<>();
        if (conf != null) {
            teams.addAll(conf.getTeams());
            teams.sort((a, b) -> {
                int cmp = Integer.compare(b.getConfWins(), a.getConfWins());
                if (cmp != 0) return cmp;
                cmp = Integer.compare(a.getConfLosses(), b.getConfLosses());
                if (cmp != 0) return cmp;
                return Integer.compare(b.getWins(), a.getWins());
            });
        }

        int maxRows = Math.min(7, teams.size());
        JPanel list = new JPanel(new GridLayout(Math.max(1, maxRows + 1), 1, 0, 2));
        list.setOpaque(false);

        // Header Row: RANK | TEAM | CONF | OVR
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        JLabel hRank = new JLabel("#");
        hRank.setPreferredSize(new Dimension(20, 14));
        hRank.setFont(new Font("SansSerif", Font.BOLD, 9));
        hRank.setForeground(DesktopTheme.textSecondary());

        JLabel hTeam = new JLabel("TEAM");
        hTeam.setFont(new Font("SansSerif", Font.BOLD, 9));
        hTeam.setForeground(DesktopTheme.textSecondary());

        JPanel hRight = new JPanel(new GridLayout(1, 2, 6, 0));
        hRight.setOpaque(false);
        JLabel hConf = new JLabel("CONF", JLabel.RIGHT);
        hConf.setFont(new Font("SansSerif", Font.BOLD, 9));
        hConf.setForeground(DesktopTheme.textSecondary());
        hConf.setPreferredSize(new Dimension(38, 14));
        JLabel hOvr = new JLabel("OVR", JLabel.RIGHT);
        hOvr.setFont(new Font("SansSerif", Font.BOLD, 9));
        hOvr.setForeground(DesktopTheme.textSecondary());
        hOvr.setPreferredSize(new Dimension(38, 14));
        hRight.add(hConf);
        hRight.add(hOvr);

        hdr.add(hRank, BorderLayout.WEST);
        hdr.add(hTeam, BorderLayout.CENTER);
        hdr.add(hRight, BorderLayout.EAST);
        list.add(hdr);

        if (teams.isEmpty()) {
            JLabel empty = new JLabel("No conference standings available", JLabel.CENTER);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 11));
            empty.setForeground(DesktopTheme.textSecondary());
            list.add(empty);
        } else {
            for (int i = 0; i < maxRows; i++) {
                Team t = teams.get(i);
                boolean isUser = (userTeam != null && t.getName().equals(userTeam.getName()));
                list.add(buildTeamRow(i + 1, t, isUser, onViewAll, onSelectTeam));
            }
        }

        content.add(list, BorderLayout.CENTER);
    }

    private JPanel buildTeamRow(int rank, Team t, boolean isUser, Runnable onViewAll, Consumer<Team> onSelectTeam) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(true);
        if (isUser) {
            r.setBackground(new Color(0, 230, 118, 35));
            r.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 230, 118, 120), 1),
                    BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        } else {
            r.setBackground(rank % 2 == 0 ? new Color(17, 28, 46) : new Color(13, 23, 38));
            r.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        if (onSelectTeam != null) {
            r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            r.setToolTipText("Click to view " + t.getName() + " details");
            r.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelectTeam.accept(t);
                }
            });
        } else if (onViewAll != null) {
            r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            r.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onViewAll.run();
                }
            });
        }

        JLabel rankLbl = new JLabel(String.valueOf(rank));
        rankLbl.setPreferredSize(new Dimension(20, 16));
        rankLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        rankLbl.setForeground(isUser ? DesktopTheme.successGreen() : DesktopTheme.textSecondary());

        // Prefix rank in poll if top 25
        String prefix = (t.getRankTeamPollScore() > 0 && t.getRankTeamPollScore() <= 25)
                ? "#" + t.getRankTeamPollScore() + " " : "";
        JLabel nameLbl = new JLabel(prefix + t.getName());
        nameLbl.setFont(new Font("SansSerif", isUser ? Font.BOLD : Font.PLAIN, 11));
        nameLbl.setForeground(isUser ? DesktopTheme.successGreen() : Color.WHITE);

        JPanel rightBox = new JPanel(new GridLayout(1, 2, 6, 0));
        rightBox.setOpaque(false);

        JLabel confRec = new JLabel(t.getConfWins() + "-" + t.getConfLosses(), JLabel.RIGHT);
        confRec.setFont(new Font("SansSerif", Font.BOLD, 10));
        confRec.setForeground(isUser ? DesktopTheme.successGreen() : Color.WHITE);
        confRec.setPreferredSize(new Dimension(38, 16));

        JLabel ovrRec = new JLabel(t.getWins() + "-" + t.getLosses(), JLabel.RIGHT);
        ovrRec.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ovrRec.setForeground(DesktopTheme.textSecondary());
        ovrRec.setPreferredSize(new Dimension(38, 16));

        rightBox.add(confRec);
        rightBox.add(ovrRec);

        r.add(rankLbl, BorderLayout.WEST);
        r.add(nameLbl, BorderLayout.CENTER);
        r.add(rightBox, BorderLayout.EAST);
        return r;
    }
}
