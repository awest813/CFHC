package desktop;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Interface for a desktop league screen panel.
 * Each screen is a standalone panel that LeagueHomeView hosts in its CardLayout.
 */
public interface LeagueScreen {

    /** Title shown in the navigation sidebar. */
    String title();

    /** Build the full panel. Called once when the screen is first shown. */
    JPanel build(LeagueScreenContext ctx);

    /** Update data after league state changes. Default rebuilds the panel. */
    default void refresh(LeagueScreenContext ctx) {
    }

    /**
     * Component that receives focus when the user presses Ctrl+F.
     * Return null if this screen has no search target (falls back to Player Search tab).
     */
    default JComponent searchTarget() {
        return null;
    }
}