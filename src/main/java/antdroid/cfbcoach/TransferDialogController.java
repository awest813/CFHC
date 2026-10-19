package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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
import simulation.TransferSummaryLine;

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
        final TextView textCount = dialogView.findViewById(R.id.textTransferCount);
        final TextView textHint = dialogView.findViewById(R.id.textTransferTapHint);
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
            textCount.setText(activity.getString(R.string.dlg_portal_count, displayedPlayers.size()));
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
                    if (textHint != null) textHint.setVisibility(View.VISIBLE);
                    layoutRegistry.setVisibility(View.VISIBLE);
                    scrollSummary.setVisibility(View.GONE);
                    listPlayers.setOnItemClickListener((parent1, itemView, index, itemId) -> {
                        activity.uiSounds().play(AudioEvent.UI_CLICK);
                        if (index >= 0 && index < displayedPlayers.size()) {
                            Player p = displayedPlayers.get(index);
                            PlayerProfileDialogController.showProfile(activity, p, userTeam);
                        }
                    });
                    listPlayers.setAdapter(playerAdapter);
                    updateRegistryList.run();
                } else if (position == 1) {
                    showSummary(activity, simLeague.userTransfers,
                            "No transfers involving your program this cycle.",
                            spinnerPos, layoutStatus, textHint, layoutRegistry, scrollSummary,
                            listPlayers, textCount, textEmpty);
                } else {
                    showSummary(activity, simLeague.sumTransfers,
                            "No league-wide transfer summary available yet.",
                            spinnerPos, layoutStatus, textHint, layoutRegistry, scrollSummary,
                            listPlayers, textCount, textEmpty);
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

    private static void showSummary(MainActivity activity, String raw, String emptyCopy,
                                     Spinner spinnerPos, View layoutStatus, TextView textHint,
                                     View layoutRegistry, ScrollView scrollSummary, ListView listPlayers,
                                     TextView textCount, TextView textEmpty) {
        spinnerPos.setVisibility(View.GONE);
        layoutStatus.setVisibility(View.VISIBLE);
        if (textHint != null) textHint.setVisibility(View.GONE);
        scrollSummary.setVisibility(View.GONE);
        layoutRegistry.setVisibility(View.VISIBLE);
        List<TransferSummaryLine> rows = TransferSummaryLine.parse(raw);
        int players = TransferSummaryLine.playerCount(rows);
        textCount.setText(activity.getString(R.string.dlg_portal_summary_count, players));
        listPlayers.setOnItemClickListener(null);
        if (players == 0) {
            textEmpty.setText(emptyCopy);
            textEmpty.setVisibility(View.VISIBLE);
            listPlayers.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            listPlayers.setVisibility(View.VISIBLE);
            listPlayers.setAdapter(new TransferSummaryAdapter(activity, rows));
        }
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

    private static class TransferSummaryAdapter extends ArrayAdapter<TransferSummaryLine> {
        private final Context context;
        private final List<TransferSummaryLine> rows;

        TransferSummaryAdapter(Context context, List<TransferSummaryLine> rows) {
            super(context, R.layout.transfer_summary_row, rows);
            this.context = context;
            this.rows = rows;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(context).inflate(R.layout.transfer_summary_row, parent, false);
            }
            TransferSummaryLine line = rows.get(position);
            TextView section = row.findViewById(R.id.transferSection);
            View card = row.findViewById(R.id.transferSummaryCard);
            if (line.section) {
                section.setVisibility(View.VISIBLE);
                section.setText(line.sectionTitle);
                card.setVisibility(View.GONE);
                return row;
            }
            section.setVisibility(View.GONE);
            card.setVisibility(View.VISIBLE);
            ((TextView) row.findViewById(R.id.transferSummaryPos)).setText(line.position);
            ((TextView) row.findViewById(R.id.transferSummaryName)).setText(line.name);
            String meta = line.year.isEmpty() ? line.destination
                    : line.destination.isEmpty() ? line.year
                    : line.year + "  ·  " + line.destination;
            TextView dest = row.findViewById(R.id.transferSummaryDest);
            dest.setText(meta);
            dest.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
            TextView overall = row.findViewById(R.id.transferSummaryOvr);
            overall.setText(line.overall);
            overall.setVisibility(line.overall.isEmpty() ? View.GONE : View.VISIBLE);
            overall.setTextColor(ContextCompat.getColor(context, overallColor(line.overall)));
            return row;
        }

        private static int overallColor(String overall) {
            try {
                int rating = Integer.parseInt(overall);
                if (rating > 90) return R.color.cf_emerald;
                if (rating > 80) return R.color.cf_positive;
            } catch (NumberFormatException ignored) {
            }
            return R.color.cf_text_primary;
        }
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
