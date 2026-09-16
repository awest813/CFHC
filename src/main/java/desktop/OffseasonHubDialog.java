package desktop;

import simulation.League;
import simulation.SeasonFlowOrder;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

/**
 * Offseason hub (Phase 5 part 3): one screen for the ten offseason steps
 * (weeks R+4..R+13) instead of an unlabeled advance chain. "Advance One Step"
 * drives the same week-advance callback the shell's Play Week uses — decision
 * steps pop their own dialogs on top, exactly as before.
 */
final class OffseasonHubDialog {

    private OffseasonHubDialog() {}

    static void show(Frame owner, League league, Runnable advanceOneStep) {
        JDialog dialog = new JDialog(owner, "Offseason Hub \u2014 " + league.getYear() + " Season", true);
        dialog.setLayout(new BorderLayout(0, 8));

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> steps = new JList<>(model);
        steps.setEnabled(false);
        steps.setFont(new Font("SansSerif", Font.PLAIN, 13));
        steps.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JScrollPane scroll = new JScrollPane(steps);
        scroll.setPreferredSize(new Dimension(360, 240));

        Runnable refreshSteps = () -> {
            model.clear();
            int index = SeasonFlowOrder.offseasonStepIndex(league.currentWeek, league.regSeasonWeeks);
            String[] names = SeasonFlowOrder.offseasonSteps();
            for (int i = 0; i < names.length; i++) {
                if (i < index) {
                    model.add(model.size(), "\u2713  " + names[i]);
                } else if (i == index) {
                    model.add(model.size(), "\u25B6  " + names[i] + "   \u2014 next up");
                } else {
                    model.add(model.size(), "    " + names[i]);
                }
            }
            if (index < 0) {
                model.add(model.size(), "The offseason is complete \u2014 recruiting is underway.");
            }
            steps.repaint();
        };
        refreshSteps.run();

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        JButton advance = new JButton("Advance One Step");
        advance.setMnemonic(java.awt.event.KeyEvent.VK_A);
        Runnable refresh = () -> {
            refreshSteps.run();
            advance.setEnabled(
                    SeasonFlowOrder.offseasonStepIndex(league.currentWeek, league.regSeasonWeeks) >= 0);
        };
        advance.addActionListener(e -> {
            advanceOneStep.run();
            refresh.run();
        });
        refresh.run();
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        buttons.add(advance);
        buttons.add(close);
        dialog.getRootPane().setDefaultButton(advance);
        dialog.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(content);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, dialog.getPreferredSize().height));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
