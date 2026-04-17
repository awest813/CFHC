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

        simulation.SeasonPresentation.SeasonStatus status = simulation.SeasonPresentation.getStatus(currentTeam, simLeague, season);

        TextView seasonBadge = activity.findViewById(R.id.mainSeasonBadge);
        TextView seasonTitle = activity.findViewById(R.id.mainSeasonTitle);
        TextView seasonSubtitle = activity.findViewById(R.id.mainSeasonSubtitle);
        TextView seasonYearChip = activity.findViewById(R.id.mainSeasonYearChip);
        TextView seasonWeekChip = activity.findViewById(R.id.mainSeasonWeekChip);
        TextView seasonPhaseChip = activity.findViewById(R.id.mainSeasonPhaseChip);

        if (seasonBadge != null) {
            seasonBadge.setText(status.badge);
        }
        if (seasonTitle != null) {
            seasonTitle.setText(status.title);
        }
        if (seasonSubtitle != null) {
            seasonSubtitle.setText(status.subtitle);
        }
        if (seasonYearChip != null) {
            seasonYearChip.setText(status.yearChip);
        }
        if (seasonWeekChip != null) {
            seasonWeekChip.setText(status.weekChip);
        }
        if (seasonPhaseChip != null) {
            seasonPhaseChip.setText(status.phaseChip);
        }
    }

    public static String getSeasonBadgeText(int season) {
        return simulation.SeasonPresentation.getSeasonBadgeText(season);
    }

    public static String getSeasonTitleText(Team currentTeam, int season) {
        return simulation.SeasonPresentation.getSeasonTitleText(currentTeam, season);
    }

    public static String getSeasonSubtitleText() {
        return simulation.SeasonPresentation.getSeasonSubtitleText();
    }

    public static String getSeasonYearChipText(int season) {
        return simulation.SeasonPresentation.getSeasonYearChipText(season);
    }

    public static String getSeasonWeekChipText(League simLeague) {
        return simulation.SeasonPresentation.getSeasonWeekChipText(simLeague);
    }

    public static String getSeasonPhaseChipText(League simLeague) {
        return simulation.SeasonPresentation.getSeasonPhaseChipText(simLeague);
    }
}

