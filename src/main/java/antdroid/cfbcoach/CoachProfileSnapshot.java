package antdroid.cfbcoach;

import java.util.ArrayList;

import staff.Staff;

public final class CoachProfileSnapshot {
    public final String[] basics;
    public final String[] ratings;
    public final String[] featuredStats;

    private CoachProfileSnapshot(String[] basics, String[] ratings, String[] featuredStats) {
        this.basics = basics;
        this.ratings = ratings;
        this.featuredStats = featuredStats;
    }

    public static CoachProfileSnapshot fromStaff(Staff staff) {
        String[] basics = staff.getHCProfileBasics().split(",");
        String[] ratings = staff.getHCRatings().split(",");
        ArrayList<String> feature = staff.getHCFeaturedStats();
        String[] featuredStats = new String[8];
        for (int i = 0; i < featuredStats.length; i++) {
            featuredStats[i] = feature.get(i);
        }
        return new CoachProfileSnapshot(basics, ratings, featuredStats);
    }
}
