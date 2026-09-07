package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import positions.Player;
import simulation.AudioEvent;
import simulation.League;
import simulation.PlayerSearch;
import simulation.Team;

/**
 * Controller for the League Player Search dialog on Android.
 * Provides multi-criteria real-time search with interactive player profile inspection.
 */
public final class PlayerSearchDialogController {

    private static final String[] POSITIONS = {
            "All Positions", "QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"
    };

    private static final String[] CLASSES = {
            "All Classes", "FR", "SO", "JR", "SR"
    };

    private static final String[] MIN_OVRS = {
            "All OVR", "70+ OVR", "75+ OVR", "80+ OVR", "85+ OVR", "90+ OVR"
    };

    private PlayerSearchDialogController() {}

    public static void show(final MainActivity activity, final League league, final Team userTeam) {
        if (activity == null || league == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.player_search_dialog, null, false);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        activity.showImmersive(dialog);

        final EditText editSearch = dialogView.findViewById(R.id.editPlayerSearchName);
        final Spinner spinnerPos = dialogView.findViewById(R.id.spinnerSearchPos);
        final Spinner spinnerClass = dialogView.findViewById(R.id.spinnerSearchClass);
        final Spinner spinnerOvr = dialogView.findViewById(R.id.spinnerSearchOvr);
        final Spinner spinnerTeam = dialogView.findViewById(R.id.spinnerSearchTeam);
        final TextView textCount = dialogView.findViewById(R.id.textSearchResultCount);
        final TextView textEmpty = dialogView.findViewById(R.id.textSearchEmpty);
        final ListView listResults = dialogView.findViewById(R.id.listSearchResults);
        final Button buttonClose = dialogView.findViewById(R.id.buttonCloseSearch);

        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerPos);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerClass);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerOvr);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerTeam);

        // Populate Position adapter
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, POSITIONS);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPos.setAdapter(posAdapter);

        // Populate Class adapter
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, CLASSES);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClass.setAdapter(classAdapter);

        // Populate Min OVR adapter
        ArrayAdapter<String> ovrAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, MIN_OVRS);
        ovrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOvr.setAdapter(ovrAdapter);

        // Populate Team filter adapter
        List<String> teamOptions = new ArrayList<>();
        teamOptions.add("All Programs");
        for (Team t : league.getTeamList()) {
            teamOptions.add(t.getName());
        }
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, teamOptions);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeam.setAdapter(teamAdapter);

        final List<Player> searchResults = new ArrayList<>();
        final SearchResultsAdapter adapter = new SearchResultsAdapter(activity, searchResults);
        listResults.setAdapter(adapter);

        final Runnable runFilter = () -> {
            String query = editSearch.getText().toString().trim();
            String pos = (String) spinnerPos.getSelectedItem();
            int classIndex = spinnerClass.getSelectedItemPosition(); // 0=All, 1=FR, 2=SO, 3=JR, 4=SR
            int ovrIndex = spinnerOvr.getSelectedItemPosition();
            int minOvr = 0;
            if (ovrIndex == 1) minOvr = 70;
            else if (ovrIndex == 2) minOvr = 75;
            else if (ovrIndex == 3) minOvr = 80;
            else if (ovrIndex == 4) minOvr = 85;
            else if (ovrIndex == 5) minOvr = 90;

            String teamName = (String) spinnerTeam.getSelectedItem();
            if ("All Programs".equalsIgnoreCase(teamName)) {
                teamName = "ALL";
            }

            List<Player> found = PlayerSearch.search(league, query, pos, classIndex, teamName, minOvr);
            searchResults.clear();
            searchResults.addAll(found);
            adapter.notifyDataSetChanged();

            textCount.setText(String.format(Locale.ROOT, "%d players found", found.size()));
            if (found.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                listResults.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                listResults.setVisibility(View.VISIBLE);
            }
        };

        AdapterView.OnItemSelectedListener spinListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                runFilter.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerPos.setOnItemSelectedListener(spinListener);
        spinnerClass.setOnItemSelectedListener(spinListener);
        spinnerOvr.setOnItemSelectedListener(spinListener);
        spinnerTeam.setOnItemSelectedListener(spinListener);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                runFilter.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listResults.setOnItemClickListener((parent, view, position, id) -> {
            activity.uiSounds().play(AudioEvent.UI_CLICK);
            if (position >= 0 && position < searchResults.size()) {
                Player p = searchResults.get(position);
                PlayerProfileDialogController.showProfile(activity, p, userTeam);
            }
        });

        buttonClose.setOnClickListener(v -> {
            activity.uiSounds().play(AudioEvent.UI_BACK);
            dialog.dismiss();
        });

        // Initial search run
        runFilter.run();
    }

    private static class SearchResultsAdapter extends ArrayAdapter<Player> {
        private final Context context;
        private final List<Player> players;

        SearchResultsAdapter(Context context, List<Player> players) {
            super(context, R.layout.player_search_row, players);
            this.context = context;
            this.players = players;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(R.layout.player_search_row, parent, false);
            }

            Player p = players.get(position);
            TextView textPos = row.findViewById(R.id.textSearchRowPos);
            TextView textClass = row.findViewById(R.id.textSearchRowClass);
            TextView textTeam = row.findViewById(R.id.textSearchRowTeam);
            TextView textOvr = row.findViewById(R.id.textSearchRowOvr);
            TextView textName = row.findViewById(R.id.textSearchRowName);
            TextView textStatus = row.findViewById(R.id.textSearchRowStatus);

            textPos.setText(p.position);
            textClass.setText(yearToAbbr(p.year));
            textTeam.setText(p.team != null ? p.team.getName() : "Free Agent");
            textOvr.setText(p.ratOvr + " OVR");
            textName.setText(p.name);

            // Status details (archetype or status)
            StringBuilder sb = new StringBuilder();
            if (p.isRedshirt) sb.append("[RS] ");
            if (p.isTransfer) sb.append("[TR] ");
            if (p.isInjured) sb.append("[INJ] ");
            if (p.isSuspended) sb.append("[SUSP] ");
            String arch = p.getArchetypeDisplayName();
            if (arch != null && !arch.isEmpty()) {
                sb.append(arch);
            } else {
                sb.append(p.getStatus());
            }
            textStatus.setText(sb.toString().trim());

            return row;
        }

        private static String yearToAbbr(int yr) {
            switch (yr) {
                case 1: return "FR";
                case 2: return "SO";
                case 3: return "JR";
                case 4: return "SR";
                default: return "GR";
            }
        }
    }
}
