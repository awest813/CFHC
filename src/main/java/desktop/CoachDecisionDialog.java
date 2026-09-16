package desktop;

import simulation.Game;
import simulation.GameCoachListener;
import simulation.GameCoachPlan;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;

/**
 * Keyboard-driven coaching decision dialog for interactive gameday (Phase 5
 * part 3). One modal dialog per {@link GameCoachListener.Checkpoint}; shown
 * from the desktop's synchronous coach listener, so the simulation waits for
 * the coach. "Let CPU Decide" (or Escape) defers with a {@code null} plan.
 */
final class CoachDecisionDialog {

    private CoachDecisionDialog() {}

    static GameCoachPlan show(javax.swing.JFrame owner, Game game, GameCoachListener.Checkpoint checkpoint) {
        GameCoachPlan plan = new GameCoachPlan();
        JDialog dialog = new JDialog(owner, titleFor(checkpoint), true);
        dialog.setLayout(new BorderLayout(0, 8));

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 6));
        body.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));
        body.setOpaque(false);

        JComboBox<String> offBox = null;
        JComboBox<String> defBox = null;
        JSpinner aggression = null;
        JComboBox<String> focusBox = null;
        JSpinner timeouts = null;
        JComboBox<String> intentBox = null;

        switch (checkpoint) {
            case PREGAME:
                offBox = schemeCombo(planOffNames(game));
                defBox = schemeCombo(planDefNames(game));
                aggression = new JSpinner(new SpinnerNumberModel(0, -2, 2, 1));
                body.add(new JLabel("Offense"));
                body.add(offBox);
                body.add(new JLabel("Defense"));
                body.add(defBox);
                body.add(new JLabel("Fourth-down aggression (punt-happy \u2190 \u2192 gambler)"));
                body.add(aggression);
                break;

            case HALFTIME:
                offBox = schemeCombo(planOffNames(game));
                defBox = schemeCombo(planDefNames(game));
                focusBox = new JComboBox<>(new DefaultComboBoxModel<>(
                        new String[]{"Balanced", "Attack \u2014 aggressive second half", "Conservative \u2014 protect the lead"}));
                body.add(new JLabel("Locker room"));
                body.add(focusBox);
                body.add(new JLabel("Offense (optional switch)"));
                body.add(offBox);
                body.add(new JLabel("Defense (optional switch)"));
                body.add(defBox);
                break;

            case CRUNCH_TIME:
            default:
                int timeoutsLeft = timeoutsLeft(game);
                timeouts = new JSpinner(new SpinnerNumberModel(0, 0, Math.max(0, timeoutsLeft), 1));
                intentBox = new JComboBox<>(new DefaultComboBoxModel<>(
                        new String[]{"Auto (coordinator's call)", "Go for it on fourth down",
                                "Kick field goals", "Punt"}));
                body.add(new JLabel("Timeouts to burn now (" + timeoutsLeft + " left)"));
                body.add(timeouts);
                body.add(new JLabel("Fourth-down calls from here on"));
                body.add(intentBox);
                break;
        }

        JRadioButton cpu = new JRadioButton("Let the CPU decide");
        cpu.setMnemonic(KeyEvent.VK_C);
        JRadioButton coach = new JRadioButton(checkpoint == GameCoachListener.Checkpoint.PREGAME
                ? "Coach this game" : "Apply");
        coach.setMnemonic(KeyEvent.VK_O);
        coach.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(cpu);
        group.add(coach);
        JPanel choice = new JPanel(new GridLayout(1, 2, 8, 0));
        choice.setOpaque(false);
        choice.add(cpu);
        choice.add(coach);
        body.add(choice);

        dialog.add(body, BorderLayout.CENTER);

        final JComboBox<String> fOff = offBox;
        final JComboBox<String> fDef = defBox;
        final JSpinner fAgg = aggression;
        final JComboBox<String> fFocus = focusBox;
        final JSpinner fTo = timeouts;
        final JComboBox<String> fIntent = intentBox;

        Runnable applyAndClose = () -> {
            if (cpu.isSelected()) {
                dialog.dispose();
                return; // null plan = defer to CPU
            }
            if (fOff != null && fOff.getSelectedIndex() > 0) {
                plan.offScheme = fOff.getSelectedIndex() - 1; // index 0 = "keep current"
            }
            if (fDef != null && fDef.getSelectedIndex() > 0) {
                plan.defScheme = fDef.getSelectedIndex() - 1;
            }
            if (fAgg != null) {
                plan.fourthDownBias = (Integer) fAgg.getValue();
            }
            if (fFocus != null) {
                switch (fFocus.getSelectedIndex()) {
                    case 1 -> plan.halftimeFocus = GameCoachPlan.HalftimeFocus.AGGRESSIVE;
                    case 2 -> plan.halftimeFocus = GameCoachPlan.HalftimeFocus.CONSERVATIVE;
                    default -> plan.halftimeFocus = GameCoachPlan.HalftimeFocus.BALANCED;
                }
            }
            if (fTo != null) {
                plan.timeoutsToBurn = (Integer) fTo.getValue();
            }
            if (fIntent != null) {
                switch (fIntent.getSelectedIndex()) {
                    case 1 -> plan.crunchFourthDownCall = GameCoachPlan.FourthDownCall.GO_FOR_IT;
                    case 2 -> plan.crunchFourthDownCall = GameCoachPlan.FourthDownCall.FIELD_GOAL;
                    case 3 -> plan.crunchFourthDownCall = GameCoachPlan.FourthDownCall.PUNT;
                    default -> plan.crunchFourthDownCall = GameCoachPlan.FourthDownCall.AUTO;
                }
            }
            dialog.dispose();
        };

        javax.swing.JButton ok = new javax.swing.JButton("OK");
        ok.setDefaultCapable(true);
        ok.addActionListener(e -> applyAndClose.run());
        dialog.getRootPane().setDefaultButton(ok);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 6));
        right.setOpaque(false);
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        dialog.getRootPane().getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getPreferredSize().height));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        return cpu.isSelected() ? null : plan;
    }

    private static String titleFor(GameCoachListener.Checkpoint checkpoint) {
        switch (checkpoint) {
            case PREGAME: return "Gameplan";
            case HALFTIME: return "Halftime";
            default: return "Crunch Time";
        }
    }

    private static int timeoutsLeft(Game game) {
        return game.homeTeam.isUserControlled() ? game.getHomeTimeouts() : game.getAwayTimeouts();
    }

    private static String[] planOffNames(Game game) {
        boolean userHome = game.homeTeam.isUserControlled();
        int current = userHome ? game.homeTeam.getPlaybookOffNum() : game.awayTeam.getPlaybookOffNum();
        String[] names = new String[7];
        names[0] = "Keep current (" + new simulation.PlaybookOffense(current).getStratName() + ")";
        for (int i = 0; i < 6; i++) {
            names[i + 1] = new simulation.PlaybookOffense(i).getStratName();
        }
        return names;
    }

    private static String[] planDefNames(Game game) {
        boolean userHome = game.homeTeam.isUserControlled();
        int current = userHome ? game.homeTeam.getPlaybookDefNum() : game.awayTeam.getPlaybookDefNum();
        String[] names = new String[6];
        names[0] = "Keep current (" + new simulation.PlaybookDefense(current).getStratName() + ")";
        for (int i = 0; i < 5; i++) {
            names[i + 1] = new simulation.PlaybookDefense(i).getStratName();
        }
        return names;
    }

    private static JComboBox<String> schemeCombo(String[] names) {
        return new JComboBox<>(new DefaultComboBoxModel<>(names));
    }
}
