package antdroid.cfbcoach;

import java.util.ArrayList;

import positions.Player;

public final class PlayerProfileSnapshot {
    public final String[] basics;
    public final String[] ratings;
    public final String[] statColumns;
    public final String[] featuredStats;

    private PlayerProfileSnapshot(String[] basics, String[] ratings, String[] statColumns, String[] featuredStats) {
        this.basics = basics;
        this.ratings = ratings;
        this.statColumns = statColumns;
        this.featuredStats = featuredStats;
    }

    public static PlayerProfileSnapshot fromPlayer(Player player) {
        String[] basics = player.getProfileBasics().split(",");
        String[] ratings = player.getPlayerRatings().split(",");
        ArrayList<String> stats = player.getPlayerStats();
        String[] statColumns = new String[9];

        for (int i = 0; i < statColumns.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (String statRow : stats) {
                String[] splitRow = statRow.split(",");
                if (i < splitRow.length) {
                    sb.append(splitRow[i]).append("\n");
                }
            }
            statColumns[i] = sb.toString();
        }

        ArrayList<String> feature = player.getPlayerFeaturedStats();
        String[] featuredStats = new String[8];
        for (int i = 0; i < featuredStats.length; i++) {
            featuredStats[i] = feature.get(i);
        }

        return new PlayerProfileSnapshot(basics, ratings, statColumns, featuredStats);
    }
}
