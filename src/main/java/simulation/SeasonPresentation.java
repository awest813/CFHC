package simulation;

/**
 * Platform-agnostic presentation logic for season-level information.
 */
public final class SeasonPresentation {
    private SeasonPresentation() {
    }

    public static final class SeasonStatus {
        public final String badge;
        public final String title;
        public final String subtitle;
        public final String yearChip;
        public final String weekChip;
        public final String phaseChip;

        public SeasonStatus(String badge, String title, String subtitle, String yearChip, String weekChip, String phaseChip) {
            this.badge = badge;
            this.title = title;
            this.subtitle = subtitle;
            this.yearChip = yearChip;
            this.weekChip = weekChip;
            this.phaseChip = phaseChip;
        }
    }

    public static SeasonStatus getStatus(Team currentTeam, League simLeague, int season) {
        return new SeasonStatus(
                getSeasonBadgeText(season),
                getSeasonTitleText(currentTeam, season),
                getSeasonSubtitleText(),
                getSeasonYearChipText(season),
                getSeasonWeekChipText(simLeague),
                getSeasonPhaseChipText(simLeague)
        );
    }

    public static String getSeasonBadgeText(int season) {
        return season + " Season";
    }

    public static String getSeasonTitleText(Team currentTeam, int season) {
        return currentTeam.getName() + " " + season;
    }

    public static String getSeasonSubtitleText() {
        return "Track the campaign at a glance with your current week, season phase, and team command center in one place.";
    }

    public static String getSeasonYearChipText(int season) {
        return "Year " + season;
    }

    public static String getSeasonWeekChipText(League simLeague) {
        if (simLeague.currentWeek <= 0) {
            return "Week 0  Preseason";
        } else if (simLeague.currentWeek <= simLeague.regSeasonWeeks) {
            return "Week " + simLeague.currentWeek;
        } else if (simLeague.currentWeek == simLeague.regSeasonWeeks + 1) {
            return "Week " + simLeague.currentWeek + "  CCG";
        } else if (simLeague.currentWeek <= simLeague.regSeasonWeeks + 4) {
            return "Week " + simLeague.currentWeek + "  Bowls";
        }
        return "Week " + simLeague.currentWeek + "  Offseason";
    }

    public static String getSeasonPhaseChipText(League simLeague) {
        if (simLeague.currentWeek <= 0) {
            return "Phase  Preseason";
        } else if (simLeague.currentWeek < simLeague.regSeasonWeeks) {
            return "Phase  Regular Season";
        } else if (simLeague.currentWeek == simLeague.regSeasonWeeks) {
            return "Phase  Rivalry Stretch";
        } else if (simLeague.currentWeek == simLeague.regSeasonWeeks + 1) {
            return "Phase  Championship Week";
        } else if (simLeague.currentWeek <= simLeague.regSeasonWeeks + 4) {
            return "Phase  Postseason";
        }
        return "Phase  Transition";
    }
}
