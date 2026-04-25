package desktop;

import simulation.League;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.BorderLayout;

/**
 * Modal recruiting dialog; delegates UI to {@link RecruitingPanel}. Prefer the
 * docked Recruiting tab in {@link LeagueHomeView} for normal desktop play.
 */
public class RecruitingView extends JDialog {

    private boolean finished;
    private String recruitsSaveData;

    public RecruitingView(JFrame owner, League league) {
        super(owner, "Recruiting — " + league.userTeam.getName(), true);
        setLayout(new BorderLayout());
        JPanel root = (JPanel) getContentPane();
        root.setOpaque(true);
        root.setBackground(DesktopTheme.windowBackground());
        RecruitingPanel panel = new RecruitingPanel(league, data -> {
            this.recruitsSaveData = data;
            this.finished = true;
            dispose();
        });
        add(panel, BorderLayout.CENTER);
        setSize(1100, 700);
        setLocationRelativeTo(owner);
    }

    public boolean isFinished() {
        return finished;
    }

    public String getRecruitsSaveData() {
        return recruitsSaveData;
    }

    /**
     * Legacy modal entry point (headless tests or tools).
     *
     * @return serialized recruit data, or {@code null} if cancelled
     */
    public static String showRecruiting(JFrame owner, League league) {
        RecruitingView view = new RecruitingView(owner, league);
        view.setVisible(true);

        if (view.isFinished()) {
            return view.getRecruitsSaveData();
        }
        return null;
    }
}
