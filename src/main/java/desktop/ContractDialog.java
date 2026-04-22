package desktop;

import simulation.League;
import simulation.Team;
import staff.HeadCoach;
import staff.Staff;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Interactive contract-review dialog shown during the offseason.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class ContractDialog extends JDialog {

    private static final Color BG_COLOR = new Color(15, 20, 28);
    private static final Color SURFACE_COLOR = new Color(25, 32, 45);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color DANGER_RED = new Color(231, 76, 60);
    private static final Color TEXT_SECONDARY = new Color(171, 178, 191);

    private static final int RETIREMENT_AGE = 65;

    private final League league;
    private boolean retired = false;

    public ContractDialog(JFrame owner, League league) {
        super(owner, "OFF-SEASON REVIEW — " + league.getYear(), true);
        this.league = league;
        setSize(700, 500);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        Team userTeam = league.userTeam;
        HeadCoach hc = userTeam != null ? userTeam.getHeadCoach() : null;

        // Handle retirement check
        if (league.isCareerMode() && hc != null && hc.age >= RETIREMENT_AGE && !league.neverRetire) {
            buildRetirementPanel(hc);
        } else {
            buildContractPanel(userTeam, hc);
        }
    }

    private void buildRetirementPanel(HeadCoach hc) {
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel header = new JLabel("CAREER FINALITY DECISION");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setForeground(Color.WHITE);
        content.add(header, BorderLayout.NORTH);

        String statsHtml = String.format(
            "<html><body style='width: 500px; color:#ABB2BF; font-family:SansSerif; font-size:11pt;'>" +
            "<p>COACH <b style='color:white'>%s</b> (AGE %d)</p><br/>" +
            "<p>You have reached the standard retirement threshold. You may choose to conclude your prestigious career today, or persevere for another campaign.</p><br/>" +
            "<table border='0' cellpadding='6' style='margin-left: 20px;'>" +
            "<tr><td>CAREER RECORD:</td><td style='color:white'>%d-%d</td></tr>" +
            "<tr><td>NATIONAL TITLES:</td><td style='color:#F1C40F'>%d</td></tr>" +
            "<tr><td>CONFERENCE TITLES:</td><td style='color:white'>%d</td></tr>" +
            "<tr><td>BOWL VICTORIES:</td><td style='color:white'>%d</td></tr>" +
            "<tr><td>WIN RATE:</td><td style='color:white'>%.1f%%</td></tr>" +
            "</table>" +
            "</body></html>",
            hc.name.toUpperCase(), hc.age, hc.stats[0], hc.stats[1], hc.stats[6], hc.stats[2], hc.stats[4], hc.getWinPCT() * 100
        );
        
        JLabel statsLabel = new JLabel(statsHtml);
        content.add(statsLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttons.setOpaque(false);
        
        JButton continueBtn = createGlassButton("CONTINUE CAREER", SUCCESS_GREEN);
        continueBtn.addActionListener(e -> {
            retired = false;
            dispose();
        });
        
        JButton retireBtn = createGlassButton("RETIRE IMMEDIATELY", DANGER_RED);
        retireBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to retire?\nThis will conclude your career progress.",
                    "Confirm Final Retirement", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                hc.retirement = true;
                retired = true;
                dispose();
            }
        });
        
        buttons.add(continueBtn);
        buttons.add(retireBtn);
        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private void buildContractPanel(Team userTeam, HeadCoach hc) {
        JPanel content = new JPanel(new BorderLayout(30, 30));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel header = new JLabel("CONTRACT EVALUATION — " + league.getYear());
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setForeground(Color.WHITE);
        content.add(header, BorderLayout.NORTH);

        JPanel mainGrid = new JPanel(new java.awt.GridLayout(1, 2, 30, 0));
        mainGrid.setOpaque(false);

        // Left Card: Contract Details
        JPanel leftCard = createInfoCard("CURRENT AGREEMENT");
        String contractHtml = (userTeam != null) ? userTeam.getContractString().toUpperCase().replace("\n", "<br/>") : "NO ACTIVE CONTRACT DATA.";
        JLabel contractLabel = new JLabel("<html><body style='color:#ABB2BF; font-family:SansSerif; font-size:10pt; line-height: 1.4;'>" + contractHtml + "</body></html>");
        contractLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        leftCard.add(contractLabel);
        
        // Right Card: Coach Status
        JPanel rightCard = createInfoCard("CAREER PROFILE");
        if (hc != null) {
            String profileHtml = String.format(
                "<html><body style='color:#ABB2BF; font-family:SansSerif; font-size:10pt; line-height: 1.4;'>" +
                "NAME: <b style='color:white'>%s</b><br/>" +
                "AGE: <b style='color:white'>%d</b><br/>" +
                "RATING: <b style='color:#3498DB'>%d OVR</b><br/>" +
                "STATUS: %s<br/>" +
                "SEASON GRADE: <b style='color:white'>%s</b><br/><br/>" +
                "TENURE: YEAR %d OF %d<br/>" +
                "CAREER: %d-%d<br/>" +
                "WIN RATE: %.1f%%" +
                "</body></html>",
                hc.name.toUpperCase(), hc.age, hc.ratOvr, hc.coachStatus().toUpperCase(), hc.getSeasonGrade(),
                hc.contractYear, hc.contractLength, hc.stats[0], hc.stats[1], hc.getWinPCT() * 100
            );
            JLabel profileLabel = new JLabel(profileHtml);
            profileLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            rightCard.add(profileLabel);
        }
        
        mainGrid.add(leftCard);
        mainGrid.add(rightCard);
        content.add(mainGrid, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        JButton okBtn = createGlassButton("PROCEED TO OFF-SEASON", ACCENT_BLUE);
        okBtn.addActionListener(e -> dispose());
        buttons.add(okBtn);
        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createInfoCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.setBackground(SURFACE_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 10), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(ACCENT_BLUE);
        card.add(titleLabel);
        
        return card;
    }

    private JButton createGlassButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        return btn;
    }

    public boolean didRetire() {
        return retired;
    }

    public static boolean show(JFrame owner, League league) {
        ContractDialog dlg = new ContractDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.didRetire();
    }
}
