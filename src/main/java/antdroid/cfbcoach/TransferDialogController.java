package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import positions.Player;
import simulation.AudioEvent;
import simulation.League;
import simulation.Team;

/**
 * Modern controller for the Transfer Portal and Redshirt Registry on Android.
 * Features an interactive transfer browser matching desktop functionality with tap-to-inspect profiles.
 */
public final class TransferDialogController {

    private static final String[] TRANSFER_MODES = {
            "Portal Registry",
            "Your Program Transfers",
            "League Transfer Summary"
    };

    private static final String[] POS_FILTERS = {
            "All Positions", "QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"
    };

    private TransferDialogController() {}

    public static void showTransfers(final MainActivity activity, final League simLeague, final Team userTeam) {
        if (activity == null || simLeague == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.transfer_portal_dialog, null, false);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        activity.showImmersive(dialog);

        final Spinner spinnerMode = dialogView.findViewById(R.id.spinnerTransferMode);
        final Spinner spinnerPos = dialogView.findViewById(R.id.spinnerTransferPos);
        final View layoutStatus = dialogView.findViewById(R.id.layoutTransferStatus);
        final View layoutRegistry = dialogView.findViewById(R.id.layoutTransferRegistry);
        final ScrollView scrollSummary = dialogView.findViewById(R.id.scrollTransferSummary);
        final TextView textSummary = dialogView.findViewById(R.id.textTransferSummary);
        final TextView textCount = dialogView.findViewById(R.id.textTransferCount);
        final TextView textEmpty = dialogView.findViewById(R.id.textTransferEmpty);
        final ListView listPlayers = dialogView.findViewById(R.id.listTransferPlayers);
        final Button buttonClose = dialogView.findViewById(R.id.buttonCloseTransfer);

        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerMode);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinnerPos);

        // Populate Mode Adapter
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, TRANSFER_MODES);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);

        // Populate Position Filter Adapter
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, POS_FILTERS);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPos.setAdapter(posAdapter);

        // Collect all transfer players
        final List<Player> allTransferPlayers = collectTransferPlayers(simLeague);
        final List<Player> displayedPlayers = new ArrayList<>(allTransferPlayers);
        final TransferPlayerAdapter playerAdapter = new TransferPlayerAdapter(activity, displayedPlayers);
        listPlayers.setAdapter(playerAdapter);

        final Runnable updateRegistryList = () -> {
            String selectedPos = (String) spinnerPos.getSelectedItem();
            displayedPlayers.clear();
            for (Player p : allTransferPlayers) {
                if (selectedPos == null || selectedPos.equalsIgnoreCase("All Positions") || p.position.equalsIgnoreCase(selectedPos)) {
                    displayedPlayers.add(p);
                }
            }
            playerAdapter.notifyDataSetChanged();
            textCount.setText(String.format(Locale.ROOT, "%d transfers in portal", displayedPlayers.size()));
            if (displayedPlayers.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                listPlayers.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                listPlayers.setVisibility(View.VISIBLE);
            }
        };

        spinnerPos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRegistryList.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activity.uiSounds().play(AudioEvent.UI_TOGGLE);
                if (position == 0) {
                    // Portal Registry
                    spinnerPos.setVisibility(View.VISIBLE);
                    layoutStatus.setVisibility(View.VISIBLE);
                    layoutRegistry.setVisibility(View.VISIBLE);
                    scrollSummary.setVisibility(View.GONE);
                    updateRegistryList.run();
                } else if (position == 1) {
                    // Your Program Transfers
                    spinnerPos.setVisibility(View.GONE);
                    layoutStatus.setVisibility(View.GONE);
                    layoutRegistry.setVisibility(View.GONE);
                    scrollSummary.setVisibility(View.VISIBLE);
                    String userText = simLeague.userTransfers;
                    if (userText == null || userText.trim().isEmpty()) {
                        userText = "No transfers involving your program this cycle.";
                    }
                    textSummary.setText(userText.trim());
                } else {
                    // League Transfer Summary
                    spinnerPos.setVisibility(View.GONE);
                    layoutStatus.setVisibility(View.GONE);
                    layoutRegistry.setVisibility(View.GONE);
                    scrollSummary.setVisibility(View.VISIBLE);
                    String sumText = simLeague.sumTransfers;
                    if (sumText == null || sumText.trim().isEmpty()) {
                        sumText = "No league-wide transfer summary available yet.";
                    }
                    textSummary.setText(sumText.trim());
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        listPlayers.setOnItemClickListener((parent, view, position, id) -> {
            activity.uiSounds().play(AudioEvent.UI_CLICK);
            if (position >= 0 && position < displayedPlayers.size()) {
                Player p = displayedPlayers.get(position);
                PlayerProfileDialogController.showProfile(activity, p, userTeam);
            }
        });

        buttonClose.setOnClickListener(v -> {
            activity.uiSounds().play(AudioEvent.UI_BACK);
            dialog.dismiss();
        });

        // Initialize view
        updateRegistryList.run();
    }

    public static void showRedshirtList(final MainActivity activity, final League simLeague, final Team userTeam) {
        if (activity == null || userTeam == null) return;

        List<Player> redshirtPlayers = new ArrayList<>();
        for (Player p : userTeam.getAllPlayers()) {
            if (p.isRedshirt) {
                redshirtPlayers.add(p);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.player_search_dialog, null, false);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        activity.showImmersive(dialog);

        // Customize player search shell for redshirt review
        final TextView titleView = dialogView.findViewById(R.id.editPlayerSearchName);
        titleView.setVisibility(View.GONE);
        final View spinnerRow = dialogView.findViewById(R.id.spinnerSearchPos).getRootView();
        dialogView.findViewById(R.id.spinnerSearchPos).setVisibility(View.GONE);
        dialogView.findViewById(R.id.spinnerSearchClass).setVisibility(View.GONE);
        dialogView.findViewById(R.id.spinnerSearchOvr).setVisibility(View.GONE);
        dialogView.findViewById(R.id.spinnerSearchTeam).setVisibility(View.GONE);

        final TextView textCount = dialogView.findViewById(R.id.textSearchResultCount);
        final TextView textEmpty = dialogView.findViewById(R.id.textSearchEmpty);
        final ListView listResults = dialogView.findViewById(R.id.listSearchResults);
        final Button buttonClose = dialogView.findViewById(R.id.buttonCloseSearch);

        textCount.setText(String.format(Locale.ROOT, "%d Active Redshirts", redshirtPlayers.size()));

        if (redshirtPlayers.isEmpty()) {
            textEmpty.setText("No players are currently redshirted for this season.");
            textEmpty.setVisibility(View.VISIBLE);
            listResults.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            listResults.setVisibility(View.VISIBLE);
            final TransferPlayerAdapter adapter = new TransferPlayerAdapter(activity, redshirtPlayers);
            listResults.setAdapter(adapter);
            listResults.setOnItemClickListener((parent, view, position, id) -> {
                activity.uiSounds().play(AudioEvent.UI_CLICK);
                if (position >= 0 && position < redshirtPlayers.size()) {
                    Player p = redshirtPlayers.get(position);
                    PlayerProfileDialogController.showProfile(activity, p, userTeam);
                }
            });
        }

        buttonClose.setOnClickListener(v -> {
            activity.uiSounds().play(AudioEvent.UI_BACK);
            dialog.dismiss();
        });
    }

    private static List<Player> collectTransferPlayers(League league) {
        List<Player> list = new ArrayList<>();
        if (league == null) return list;
        addAll(list, league.getTransferQBs());
        addAll(list, league.getTransferRBs());
        addAll(list, league.getTransferWRs());
        addAll(list, league.getTransferTEs());
        addAll(list, league.getTransferOLs());
        addAll(list, league.getTransferKs());
        addAll(list, league.getTransferDLs());
        addAll(list, league.getTransferLBs());
        addAll(list, league.getTransferCBs());
        addAll(list, league.getTransferSs());

        list.sort((a, b) -> Integer.compare(b.ratOvr, a.ratOvr));
        return list;
    }

    private static <T extends Player> void addAll(List<Player> dest, List<T> src) {
        if (src != null) dest.addAll(src);
    }

    private static class TransferPlayerAdapter extends ArrayAdapter<Player> {
        private final Context context;
        private final List<Player> players;

        TransferPlayerAdapter(Context context, List<Player> players) {
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
            textTeam.setText(p.team != null ? p.team.getName() : "Transfer Portal");
            textOvr.setText(p.ratOvr + " OVR");
            textName.setText(p.name);

            if (p.isRedshirt) {
                textStatus.setText("Redshirt");
            } else {
                String arch = p.getArchetypeDisplayName();
                textStatus.setText(arch != null && !arch.isEmpty() ? arch : "Transfer Portal");
            }

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
