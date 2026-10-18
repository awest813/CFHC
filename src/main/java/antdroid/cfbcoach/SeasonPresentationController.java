package antdroid.cfbcoach;

import android.view.View;
import android.widget.TextView;

import java.util.Locale;

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
        TextView seasonNickname = activity.findViewById(R.id.mainSeasonNickname);
        TextView seasonYearChip = activity.findViewById(R.id.mainSeasonYearChip);
        TextView seasonWeekChip = activity.findViewById(R.id.mainSeasonWeekChip);
        TextView seasonPhaseChip = activity.findViewById(R.id.mainSeasonPhaseChip);
        TextView seasonMono = activity.findViewById(R.id.mainSeasonMono);
        TextView seasonRecord = activity.findViewById(R.id.mainSeasonRecord);

        if (seasonBadge != null) {
            seasonBadge.setText(status.badge);
        }
        if (seasonTitle != null) {
            // School name only. The year already lives on the year chip, matching
            // the desktop broadcast header.
            String name = currentTeam.getName();
            seasonTitle.setText(name == null ? "" : name.toUpperCase(Locale.ROOT));
        }
        if (seasonMono != null) {
            String mono = monogram(currentTeam);
            seasonMono.setText(mono);
            seasonMono.setContentDescription(activity.getString(R.string.hub_a11y_mono) + " " + mono);
        }
        if (seasonRecord != null) {
            seasonRecord.setText(recordLine(currentTeam));
        }
        if (seasonSubtitle != null) {
            // Compact broadcast header: the line only takes space when the
            // engine actually has something to say.
            boolean hasSubtitle = status.subtitle != null && !status.subtitle.trim().isEmpty();
            seasonSubtitle.setText(hasSubtitle ? status.subtitle : "");
            seasonSubtitle.setVisibility(hasSubtitle ? View.VISIBLE : View.GONE);
        }
        if (seasonNickname != null) {
            // Caveat script accent (style_guide §2: nickname only); hidden when
            // the team file carries no nickname.
            String nickname = currentTeam.nickname;
            boolean hasNickname = nickname != null && !nickname.trim().isEmpty();
            seasonNickname.setText(hasNickname ? nickname.trim() : "");
            seasonNickname.setVisibility(hasNickname ? View.VISIBLE : View.GONE);
        }
        if (seasonYearChip != null) {
            seasonYearChip.setText(status.yearChip);
        }
        if (seasonWeekChip != null) {
            seasonWeekChip.setText(status.weekChip);
        }
        if (seasonPhaseChip != null) {
            String phase = status.phaseChip == null ? "" : status.phaseChip.replaceFirst("^Phase\\s+", "");
            seasonPhaseChip.setText(phase);
        }
    }

    static String monogram(Team team) {
        if (team == null || team.getAbbr() == null || team.getAbbr().length() < 2) {
            return "CF";
        }
        String abbr = team.getAbbr();
        return abbr.substring(0, Math.min(3, abbr.length())).toUpperCase(Locale.ROOT);
    }

    /** Poll prefix only inside the top 25. Unranked teams otherwise show the full league size. */
    static String pollPrefix(Team team) {
        if (team == null) {
            return "";
        }
        int rank = team.getRankTeamPollScore();
        return rank >= 1 && rank <= 25 ? "#" + rank : "";
    }

    /** Record and conference for the header, e.g. {@code #12  ·  5-2  ·  ACC}. */
    static String recordLine(Team team) {
        if (team == null) {
            return "";
        }
        String conf = team.conference == null ? "" : team.conference.trim();
        String record = team.getWins() + "-" + team.getLosses();
        String poll = pollPrefix(team);
        String head = poll.isEmpty() ? record : poll + "  ·  " + record;
        return conf.isEmpty() ? head : head + "  ·  " + conf;
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

