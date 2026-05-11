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
        int week = simLeague.currentWeek;
        int regWeeks = simLeague.regSeasonWeeks;
        if (week <= 0) {
            return "Week 0  Preseason";
        } else if (week < regWeeks - 1) {
            return "Week " + week;
        } else if (week == regWeeks - 1) {
            return "Week " + week + "  CCG";
        } else if (week <= regWeeks + 2) {
            return "Week " + week + "  Bowls";
        } else if (week == regWeeks + 3) {
            return "Week " + week + "  NCG";
        }
        return "Week " + week + "  Offseason";
    }

    public static String getSeasonPhaseChipText(League simLeague) {
        int week = simLeague.currentWeek;
        int regWeeks = simLeague.regSeasonWeeks;
        if (week <= 0) {
            return "Phase  Preseason";
        } else if (week < regWeeks - 1) {
            return "Phase  Regular Season";
        } else if (week == regWeeks - 1) {
            return "Phase  Championship Week";
        } else if (week <= regWeeks + 2) {
            return "Phase  Postseason";
        } else if (week == regWeeks + 3) {
            return "Phase  National Championship";
        }
        return "Phase  Offseason";
    }

    /**
     * Coarse season cycle label for timeline/status UI:
     * Pre-Season -> Regular Season -> Postseason -> Offseason -> Recruiting.
     */
    public static String getSeasonCycleLabel(League simLeague) {
        int week = simLeague.currentWeek;
        int regWeeks = simLeague.regSeasonWeeks;
        if (week >= regWeeks + 13) return "Recruiting";
        if (week <= 0) return "Pre-Season";
        if (week <= regWeeks - 1) return "Regular Season";
        if (week <= regWeeks + 3) return "Postseason";
        return "Offseason";
    }
}
