package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import simulation.League;
import simulation.PlayerRecord;
import simulation.Team;
import simulation.TeamHistoryRecord;
import staff.Staff;
import ui.HallofFameList;
import ui.LeagueRecordsList;
import ui.TeamHistoryList;

public final class HistoryDialogController {
    private HistoryDialogController() {}

    public static void showTop25History(MainActivity activity, League simLeague, int seasonStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("AP Poll History")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.bowl_ccg_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindArchiveDialogShell(dialog, "AP Poll Archive", "Step through each completed season to review how the national rankings evolved over time.");

        if (simLeague.getYear() == seasonStart) {
            String[] selection = {"No History to Display"};
            Spinner top25hisSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
            PlatformUiHelper.avoidSpinnerDropdownFocus(top25hisSpinner);
            final ArrayAdapter<String> top25Adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, selection);
            top25Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            top25hisSpinner.setAdapter(top25Adapter);
        } else {
            String[] selection = new String[simLeague.getLeagueHistory().size()];
            for (int i = 0; i < simLeague.getLeagueHistory().size(); ++i) {
                selection[i] = Integer.toString(seasonStart + i);
            }
            Spinner top25hisSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
            PlatformUiHelper.avoidSpinnerDropdownFocus(top25hisSpinner);
            final ArrayAdapter<String> top25Adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, selection);
            top25Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            top25hisSpinner.setAdapter(top25Adapter);

            final TextView top25his = dialog.findViewById(R.id.textViewBowlCCGDialog);
            top25hisSpinner.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                                   int position, long id) {
                            top25his.setText(simLeague.getLeagueTop25History(position));
                        }
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
        }
    }

    public static void showCurrTeamHistoryDialog(MainActivity activity, Team currentTeam,
                                                  Team userTeam, League simLeague,
                                                  int seasonStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(currentTeam.getName() + " History")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, currentTeam.getName() + " Archive",
                "Review your program history, records, and hall-of-fame legacy from one unified team archive.");

        String[] selection = {"Team History", "Team Records", "Hall of Fame", "Graph View: Prestige", "Graph View: Rankings"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        final PlayerRecord[] hofPlayers = currentTeam.getHallOfFame().toArray(new PlayerRecord[0]);

        teamHistSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                               int position, long id) {
                        if (position == 0) {
                            TeamHistoryRecord[] histArray = currentTeam.getTeamHistory().toArray(new TeamHistoryRecord[0]);
                            teamHistoryList.setAdapter(new TeamHistoryList(activity, histArray));
                        } else if (position == 1) {
                            teamHistoryList.setAdapter(new LeagueRecordsList(activity,
                                    currentTeam.teamRecords.getRecordsStr().split("\n"), "---", "---"));
                        } else if (position == 2) {
                            teamHistoryList.setAdapter(new HallofFameList(activity, hofPlayers,
                                    userTeam.getName(), true, activity));
                        } else if (position == 3) {
                            dialog.dismiss();
                            teamPrestigeGraphView(activity, currentTeam, simLeague);
                        } else if (position == 4) {
                            dialog.dismiss();
                            teamRankingGraphView(activity, currentTeam, simLeague, seasonStart);
                        }
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    public static void showCoachHistoryDialog(MainActivity activity, final Staff hc,
                                               League simLeague, int seasonStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Head Coach History: " + hc.getName())
                .setPositiveButton("OK", (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Head Coach History",
                "Review one coach's stops and jump into prestige or ranking trend views from the same archive flow.");

        String[] selection = {"Team History", "Graph View: Prestige", "Graph View: Rankings"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        teamHistSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                               int position, long id) {
                        if (position == 0) {
                            teamHistoryList.setAdapter(new TeamHistoryList(activity, hc.getCoachHistory()));
                        } else if (position == 1) {
                            dialog.dismiss();
                            coachGraphView(activity, hc, simLeague);
                        } else if (position == 2) {
                            dialog.dismiss();
                            coachGraphViewRank(activity, hc, simLeague);
                        }
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    private static void showGraphDialog(MainActivity activity, String title, String shellTitle,
                                         String shellDesc, GraphDataProvider provider,
                                         int numHorizontalLabels, Double maxY, Double minY,
                                         String[] verticalLabels) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setPositiveButton("OK", (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.graphview, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindGraphDialogShell(dialog, shellTitle, shellDesc);

        GraphView graph = dialog.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        String[] yearLabels = new String[provider.getDataCount()];
        for (int i = 0; i < provider.getDataCount(); i++) {
            series.appendData(provider.createDataPoint(i), true, i + 1, false);
            yearLabels[i] = provider.createLabel(i);
        }
        graph.addSeries(series);

        if (yearLabels.length > 1) {
            StaticLabelsFormatter years = new StaticLabelsFormatter(graph);
            years.setHorizontalLabels(yearLabels);
            if (verticalLabels != null) years.setVerticalLabels(verticalLabels);
            graph.getGridLabelRenderer().setLabelFormatter(years);
            graph.getGridLabelRenderer().setNumHorizontalLabels(numHorizontalLabels);
            if (verticalLabels != null) graph.getGridLabelRenderer().setNumVerticalLabels(6);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        if (maxY != null) graph.getViewport().setMaxY(maxY);
        if (minY != null) graph.getViewport().setMinY(minY);
    }

    private static void teamPrestigeGraphView(MainActivity activity, Team currentTeam, League simLeague) {
        simLeague.sortTeamList();
        showGraphDialog(activity,
                currentTeam.getName() + ": Prestige History",
                currentTeam.getName() + " Prestige Trend",
                "See how program prestige has risen and fallen across your team's historical arc.",
                new GraphDataProvider() {
                    public DataPoint createDataPoint(int i) {
                        return new DataPoint(currentTeam.getTeamHistory().get(i).year, currentTeam.getTeamHistory().get(i).prestige);
                    }
                    public String createLabel(int i) { return Integer.toString(currentTeam.getTeamHistory().get(i).year); }
                    public int getDataCount() { return currentTeam.getTeamHistory().size(); }
                },
                5, simLeague.getTeamList().get(0).teamPrestige + 10.0, 0.0, null);
    }

    private static void teamRankingGraphView(MainActivity activity, Team currentTeam,
                                              League simLeague, int seasonStart) {
        String[] rankLabels = new String[simLeague.getTeamList().size() + 1];
        for (int i = simLeague.getTeamList().size(); i >= 0; i--) {
            rankLabels[simLeague.getTeamList().size() - i] = Integer.toString(i);
        }
        showGraphDialog(activity,
                currentTeam.getName() + ": Rankings History",
                currentTeam.getName() + " Ranking Trend",
                "Track where your program has landed in the national pecking order over time.",
                new GraphDataProvider() {
                    public DataPoint createDataPoint(int i) {
                        return new DataPoint(currentTeam.getTeamHistory().get(i).year,
                                simLeague.getTeamList().size() - currentTeam.getTeamHistory().get(i).rank);
                    }
                    public String createLabel(int i) { return Integer.toString(i + seasonStart); }
                    public int getDataCount() { return currentTeam.getTeamHistory().size(); }
                },
                5, (double) simLeague.getTeamList().size(), 0.0, rankLabels);
    }

    private static void coachGraphView(MainActivity activity, final Staff hc, League simLeague) {
        simLeague.sortTeamList();
        showGraphDialog(activity,
                hc.getName() + ": Prestige History",
                hc.getName() + " Prestige Trend",
                "Follow how this coach changed program prestige across each stop in his career.",
                new GraphDataProvider() {
                    public DataPoint createDataPoint(int i) {
                        if (!hc.history.get(i).equals("")) {
                            String[] prsPart = hc.history.get(i).split("Prs: ");
                            if (prsPart.length >= 2) {
                                String[] prsVal = prsPart[1].split(" ");
                                if (prsVal.length >= 1) {
                                    try {
                                        return new DataPoint(Integer.parseInt(hc.history.get(i).split(": ")[0]), Integer.parseInt(prsVal[0]));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                        return new DataPoint(i, 0);
                    }
                    public String createLabel(int i) { return hc.history.get(i).split(":")[0]; }
                    public int getDataCount() { return hc.history.size(); }
                },
                4, simLeague.getTeamList().get(0).teamPrestige + 10.0, 0.0, null);
    }

    private static void coachGraphViewRank(MainActivity activity, final Staff hc, League simLeague) {
        String[] rankLabels = new String[simLeague.getTeamList().size() + 1];
        for (int i = simLeague.getTeamList().size(); i >= 0; i--) {
            rankLabels[simLeague.getTeamList().size() - i] = Integer.toString(i);
        }
        showGraphDialog(activity,
                hc.getName() + ": Rankings History",
                hc.getName() + " Ranking Trend",
                "Follow how this coach's teams climbed or slid in the national rankings over time.",
                new GraphDataProvider() {
                    public DataPoint createDataPoint(int i) {
                        if (!hc.history.get(i).equals("")) {
                            try {
                                return new DataPoint(Integer.parseInt(hc.history.get(i).split(": ")[0]),
                                        simLeague.getTeamList().size() - Integer.parseInt(hc.history.get(i).split("#")[1].split(" ")[0]));
                            } catch (NumberFormatException ignored) {}
                        }
                        return new DataPoint(i, 0);
                    }
                    public String createLabel(int i) { return hc.history.get(i).split(":")[0]; }
                    public int getDataCount() { return hc.history.size(); }
                },
                4, (double) simLeague.getTeamList().size(), 0.0, rankLabels);
    }

    private interface GraphDataProvider {
        DataPoint createDataPoint(int index);
        String createLabel(int index);
        int getDataCount();
    }
}
