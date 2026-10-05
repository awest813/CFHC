package antdroid.cfbcoach;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import simulation.Conference;
import simulation.League;
import simulation.Team;

/**
 * First-run "Choose Your Program" picker. Replaces the flat 100+ entry
 * {@code setItems} list with a conference spinner over a card list of teams
 * (name, nickname, conference, prestige pill).
 */
public final class TeamPickerDialogController {

    /** Receives the index of the chosen team in {@link League#getTeamList()}. */
    public interface OnTeamPicked {
        void onTeamPicked(int leagueIndex);
    }

    private TeamPickerDialogController() {
    }

    public static void show(final MainActivity activity, final League league, final OnTeamPicked callback) {
        View content = LayoutInflater.from(activity).inflate(R.layout.team_picker_dialog, null, false);
        final Spinner spinner = content.findViewById(R.id.pickerConferenceSpinner);
        final ListView list = content.findViewById(R.id.pickerTeamList);
        final TextView count = content.findViewById(R.id.pickerCount);

        final List<Conference> conferences = new ArrayList<>();
        final List<String> conferenceNames = new ArrayList<>();
        conferenceNames.add(activity.getString(R.string.picker_all_conferences));
        for (Conference c : league.getConferences()) {
            if (c.confTeams != null && !c.confTeams.isEmpty()) {
                conferences.add(c);
                conferenceNames.add(c.confName);
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, conferenceNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinner);

        final TeamRowAdapter rows = new TeamRowAdapter(activity);
        list.setAdapter(rows);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setCancelable(false)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<Team> teams = new ArrayList<>();
                if (position == 0) {
                    teams.addAll(league.getTeamList());
                } else {
                    teams.addAll(conferences.get(position - 1).confTeams);
                }
                teams.sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));
                rows.replace(teams);
                count.setText(activity.getString(R.string.picker_count, teams.size()));
                list.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            Team picked = rows.getItem(position);
            if (picked == null) return;
            int index = league.getTeamList().indexOf(picked);
            if (index < 0) return;
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            dialog.dismiss();
            callback.onTeamPicked(index);
        });

        activity.showImmersive(dialog);
    }

    /** Card rows for the team list; parsing-free, binds Team getters directly. */
    static final class TeamRowAdapter extends ArrayAdapter<Team> {
        TeamRowAdapter(Context context) {
            super(context, R.layout.team_picker_row, new ArrayList<>());
        }

        void replace(List<Team> teams) {
            setNotifyOnChange(false);
            clear();
            addAll(teams);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView != null ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.team_picker_row, parent, false);
            Team team = getItem(position);
            TextView name = row.findViewById(R.id.pickerRowName);
            TextView meta = row.findViewById(R.id.pickerRowMeta);
            TextView prestige = row.findViewById(R.id.pickerRowPrestige);
            if (team == null) return row;
            name.setText(team.getName());
            String nickname = team.nickname != null ? team.nickname.trim() : "";
            meta.setText(nickname.isEmpty()
                    ? team.getConference()
                    : nickname + "  ·  " + team.getConference());
            prestige.setText(String.valueOf(team.getTeamPrestige()));
            return row;
        }
    }
}
