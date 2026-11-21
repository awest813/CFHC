package comparator;

import java.util.Comparator;

import positions.Player;

/**
 * Created by ahngu on 11/13/2017.
 */
//League

public class CompPlayerHeisman implements Comparator<Player> {
    @Override
    public int compare(Player a, Player b) {
        // Each score is computed once per side; same result as calling it per test.
        int sa = a.getHeismanScore();
        int sb = b.getHeismanScore();
        return sa > sb ? -1 : sa == sb ? 0 : 1;
    }
}