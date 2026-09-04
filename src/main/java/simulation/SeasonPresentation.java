package simulation;

/**
 * Platform-agnostic presentation logic for season-level information.
 * Week/phase boundaries come from {@link SeasonFlowOrder}.
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
        int week = SeasonFlowOrder.clampWeek(simLeague.currentWeek);
        int regWeeks = simLeague.regSeasonWeeks;
        SeasonFlowOrder.Phase phase = SeasonFlowOrder.phaseAt(week, regWeeks);
        switch (phase) {
            case PRESEASON:
                // Phase chip already says "Preseason" — repeating it here
                // rendered as two identical chips on the dashboard.
                return "Week 0";
            case REGULAR_SEASON:
                return "Week " + week;
            case CONFERENCE_CHAMPIONSHIP:
                return "Week " + week + "  CCG";
            case POSTSEASON:
                return "Week " + week + "  Bowls";
            case NATIONAL_CHAMPIONSHIP:
                return "Week " + week + "  NCG";
            case RECRUITING:
                return "Week " + week + "  Recruiting";
            case OFFSEASON:
            default:
                return "Week " + week + "  Offseason";
        }
    }

    public static String getSeasonPhaseChipText(League simLeague) {
        SeasonFlowOrder.Phase phase = SeasonFlowOrder.phaseAt(simLeague);
        switch (phase) {
            case PRESEASON:
                return "Phase  Preseason";
            case REGULAR_SEASON:
                return "Phase  Regular Season";
            case CONFERENCE_CHAMPIONSHIP:
                return "Phase  Championship Week";
            case POSTSEASON:
                return "Phase  Postseason";
            case NATIONAL_CHAMPIONSHIP:
                return "Phase  National Championship";
            case RECRUITING:
                return "Phase  Recruiting";
            case OFFSEASON:
            default:
                return "Phase  Offseason";
        }
    }

    public static String getPlayWeekLabel(int week, int regSeasonWeeks) {
        if (SeasonFlowOrder.isRecruitingGate(week, regSeasonWeeks)) return "Recruiting\u2026";
        if (week >= SeasonFlowOrder.firstOffseasonWeek(regSeasonWeeks)) {
            return "Offseason: Step " + (SeasonFlowOrder.clampWeek(week) - regSeasonWeeks - 3);
        }
        if (week == regSeasonWeeks + 3)  return "Play National Championship";
        if (week == regSeasonWeeks + 2)  return "Play Semifinals / Bowl Week 3";
        if (week == regSeasonWeeks + 1)  return "Play Quarterfinals / Bowl Week 2";
        if (week == regSeasonWeeks)      return "Play First Round / Bowl Week 1";
        if (week == regSeasonWeeks - 1)  return "Play Conf. Championships";
        if (week <= 0)                   return "Begin Season";
        return "Play Week " + (week + 1);
    }

    /**
     * Coarse season cycle label for timeline/status UI:
     * Preseason → Regular Season → Postseason → Offseason → Recruiting.
     */
    public static String getSeasonCycleLabel(League simLeague) {
        return SeasonFlowOrder.cycleLabel(SeasonFlowOrder.phaseAt(simLeague));
    }

    /** Short next-action hint for dashboards (one vocabulary with cycle labels). */
    public static String getNextActionHint(League simLeague) {
        int week = simLeague.currentWeek;
        int regWeeks = simLeague.regSeasonWeeks;
        if (week < 0) week = 0;
        if (week <= 0) {
            return "Preseason setup. Review your roster and set schemes before kickoff.";
        }
        if (week < regWeeks - 1) {
            return "Play the next regular-season game and manage injuries, depth, and practice focus.";
        }
        if (week == regWeeks - 1) {
            return "Conference championship week — prepare your lineup for a title shot.";
        }
        if (week <= regWeeks + 3) {
            return "Postseason in progress — chase bowl and playoff wins.";
        }
        if (week >= regWeeks + 13) {
            return "Recruiting — fill roster needs and finish your class.";
        }
        return "Offseason — handle contracts, staff, and roster decisions.";
    }

    /**
     * Scoreboard week-type badge aligned with CCG / bowls / NCG boundaries
     * used by {@link #getSeasonWeekChipText}.
     */
    public static String getScoreboardWeekType(int week, int regWeeks) {
        if (week < 0) week = 0;
        if (week <= 0) return "Preseason";
        if (week < regWeeks - 1) return "Regular Season";
        if (week == regWeeks - 1) return "Conference Championship";
        if (week <= regWeeks + 2) return "Bowl Season";
        if (week == regWeeks + 3) return "National Championship";
        return "Offseason";
    }
}
