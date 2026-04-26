package comparator;

import java.util.Comparator;

import positions.Player;

/**
 * Created by ahngu on 11/13/2017.
 */
//League

public class CompPlayerOVR implements Comparator<Player> {
    @Override
    public int compare(Player a, Player b) {
        // Use ratOvr (game's stored overall), not getOverall() from attributes — they can diverge
        // and callers/tests often adjust ratOvr directly.
        return Integer.compare(b.ratOvr, a.ratOvr);
    }
}