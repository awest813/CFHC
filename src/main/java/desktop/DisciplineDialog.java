package desktop;

import positions.Player;
import simulation.Team;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Desktop parity for Android {@code DisciplineDialogController}:
 * user chooses short suspension, long suspension, or ignore.
 */
final class DisciplineDialog {
    private DisciplineDialog() {
    }

    /**
     * @return true if a choice was applied
     */
    static boolean show(JFrame owner, Team userTeam, Player player, String issue, int gamesA, int gamesB) {
        if (userTeam == null || player == null) {
            return false;
        }
        String message = player.position + " " + player.getName() + " (" + player.ratOvr
                + ") violated a team policy related to " + issue + ".\n\n"
                + "Team discipline rating: " + userTeam.getTeamDisciplineScore() + "%\n\n"
                + "How do you want to proceed?";
        String[] options = {
                "Suspend " + gamesA + " Games",
                "Suspend " + gamesB + " Games",
                "Ignore"
        };
        int choice = JOptionPane.showOptionDialog(
                owner,
                DesktopTheme.messageForDialog(message),
                "Discipline Action Required",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        return applyChoice(userTeam, player, issue, gamesA, gamesB, choice);
    }

    /**
     * Maps dialog choice indices to {@link Team#disciplineAction(Player, String, int, int)}.
     * Choice {@code 0}/{@code 1} suspend; {@code 2} or any other value (cancel) ignores.
     *
     * @return true if a discipline outcome was applied
     */
    static boolean applyChoice(Team userTeam, Player player, String issue,
                               int gamesA, int gamesB, int choice) {
        if (userTeam == null || player == null) {
            return false;
        }
        userTeam.disciplineAction = false;
        if (choice == 0) {
            userTeam.disciplineAction(player, issue, gamesA, 2);
            return true;
        }
        if (choice == 1) {
            userTeam.disciplineAction(player, issue, gamesB, 1);
            return true;
        }
        // Explicit Ignore (2) or dialog closed without a choice — clear the sticky flag.
        userTeam.disciplineAction(player, issue, gamesA, 3);
        return true;
    }
}
