package antdroid.cfbcoach;

import android.widget.TextView;

import simulation.League;
import simulation.Team;

public final class SeasonPresentationController {
    private SeasonPresentationController() {
    }

    public static void update(MainActivity activity, Team currentTeam, League simLeague, int season) {
        if (activity == null || currentTeam == null || simLeague == null) {
            return;
        }

        TextView seasonBadge = activity.findViewById(R.id.mainSeasonBadge);
        TextView seasonTitle = activity.findViewById(R.id.mainSeasonTitle);
        TextView seasonSubtitle = activity.findViewById(R.id.mainSeasonSubtitle);
        TextView seasonYearChip = activity.findViewById(R.id.mainSeasonYearChip);
        TextView seasonWeekChip = activity.findViewById(R.id.mainSeasonWeekChip);
        TextView seasonPhaseChip = activity.findViewById(R.id.mainSeasonPhaseChip);

        if (seasonBadge != null) {
            seasonBadge.setText(getSeasonBadgeText(season));
        }
        if (seasonTitle != null) {
            seasonTitle.setText(getSeasonTitleText(currentTeam, season));
        }
        if (seasonSubtitle != null) {
            seasonSubtitle.setText(getSeasonSubtitleText());
        }
        if (seasonYearChip != null) {
            seasonYearChip.setText(getSeasonYearChipText(season));
        }
        if (seasonWeekChip != null) {
            seasonWeekChip.setText(getSeasonWeekChipText(simLeague));
        }
        if (seasonPhaseChip != null) {
            seasonPhaseChip.setText(getSeasonPhaseChipText(simLeague));
        }
    }

    public static String getSeasonBadgeText(int season) {
        return season + " Season";
    }

    public static String getSeasonTitleText(Team currentTeam, int season) {
        return currentTeam.name + " " + season;
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
