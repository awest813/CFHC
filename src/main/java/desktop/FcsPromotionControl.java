package desktop;

import simulation.FcsPromotionMode;
import simulation.LeagueSettingsOptions;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * "FCS promotions [mode] Cap [n] schools" row, shared by the New Game wizard and the
 * Settings dialog. The cap spinner is only enabled in {@link FcsPromotionMode#CAPPED}.
 */
final class FcsPromotionControl extends JPanel {

    static final String TOOLTIP = "<html>Realignment can promote lower-division (FCS) schools into the league,<br>"
            + "as new Independents or as members of a new conference.<br>"
            + "<b>No promotions</b> keeps the league at its current size; <b>Capped</b> allows a set<br>"
            + "number over the whole career; <b>Unlimited</b> is the original behavior.</html>";

    private static final int GAP = 8;

    private final JComboBox<FcsPromotionMode> mode;
    private final JSpinner cap;
    private final JLabel capLabel;
    private final JLabel capSuffix;

    /**
     * @param alreadyPromoted FCS schools promoted so far (shown next to the cap); 0 hides it
     */
    FcsPromotionControl(FcsPromotionMode initialMode, int initialCap, int alreadyPromoted, Font font) {
        // No outer gap so the label lines up with the checkboxes above it.
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setAlignmentX(LEFT_ALIGNMENT);
        setToolTipText(TOOLTIP);

        JLabel label = new JLabel("FCS promotions");
        label.setFont(font);
        label.setForeground(DesktopTheme.textPrimary());
        label.setToolTipText(TOOLTIP);

        mode = new JComboBox<>(FcsPromotionMode.values());
        mode.setSelectedItem(initialMode != null ? initialMode : FcsPromotionMode.UNLIMITED);
        mode.setFont(font.deriveFont(Font.PLAIN));
        mode.setToolTipText(TOOLTIP);
        DesktopTheme.styleFormControl(mode);

        int max = LeagueSettingsOptions.MAX_FCS_PROMOTION_CAP;
        cap = new JSpinner(new SpinnerNumberModel(Math.max(1, Math.min(max, initialCap)), 1, max, 1));
        cap.setFont(font.deriveFont(Font.PLAIN));
        cap.setToolTipText("Most FCS schools that can move up over the whole career.");

        capLabel = new JLabel("Cap");
        capLabel.setFont(font.deriveFont(Font.PLAIN));
        capLabel.setForeground(DesktopTheme.textPrimary());

        capSuffix = new JLabel(alreadyPromoted > 0 ? "schools, " + alreadyPromoted + " used" : "schools");
        capSuffix.setFont(font.deriveFont(Font.PLAIN));
        capSuffix.setForeground(DesktopTheme.textSecondary());

        add(label);
        add(Box.createHorizontalStrut(GAP));
        add(mode);
        add(Box.createHorizontalStrut(GAP));
        add(capLabel);
        add(Box.createHorizontalStrut(GAP));
        add(cap);
        add(Box.createHorizontalStrut(GAP));
        add(capSuffix);

        mode.addActionListener(e -> syncCapEnabled());
        syncCapEnabled();
    }

    private void syncCapEnabled() {
        boolean capped = selectedMode() == FcsPromotionMode.CAPPED;
        cap.setEnabled(capped && isEnabled());
        capLabel.setEnabled(capped && isEnabled());
        capSuffix.setEnabled(capped && isEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mode.setEnabled(enabled);
        syncCapEnabled();
    }

    FcsPromotionMode selectedMode() {
        Object sel = mode.getSelectedItem();
        return sel instanceof FcsPromotionMode m ? m : FcsPromotionMode.UNLIMITED;
    }

    int selectedCap() {
        return ((Number) cap.getValue()).intValue();
    }

    /** Test hook: the mode picker. */
    JComboBox<FcsPromotionMode> modeCombo() {
        return mode;
    }

    /** Test hook: the cap spinner. */
    JSpinner capSpinner() {
        return cap;
    }

    @Override
    public Dimension getMaximumSize() {
        // Stay one row tall inside the Settings dialog's vertical BoxLayout.
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
