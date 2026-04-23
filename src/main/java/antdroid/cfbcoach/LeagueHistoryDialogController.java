package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import simulation.League;
import simulation.PlayerRecord;
import simulation.Team;
import staff.Staff;
import ui.CoachDatabase;
import ui.HallofFameList;
import ui.LeagueHistoryList;
import ui.LeagueRecordsList;

final class LeagueHistoryDialogController {
    private LeagueHistoryDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("League History / Records")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "League Archive", "Move between history, records, hall of fame entries, and coach data without leaving the same league center.");

        String[] historySelection = {"League History", "League Records", "League Stats", "Hall of Fame", "Head Coach Database"};
        Spinner leagueHistorySpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(leagueHistorySpinner);
        ArrayAdapter<String> leagueHistorySpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, historySelection);
        leagueHistorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leagueHistorySpinner.setAdapter(leagueHistorySpinnerAdapter);

        final ListView leagueHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        final PlayerRecord[] hofPlayers = new PlayerRecord[simLeague.getLeagueHoF().size()];
        for (int i = 0; i < simLeague.getLeagueHoF().size(); ++i) {
            hofPlayers[i] = simLeague.getLeagueHoF().get(i);
        }

        leagueHistorySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 1) {
                            final LeagueRecordsList leagueRecordsAdapter =
                                    new LeagueRecordsList(activity, simLeague.getLeagueRecordsStr().split("\n"), userTeam.getAbbr(), userTeam.getName());
                            leagueHistoryList.setAdapter(leagueRecordsAdapter);
                        } else if (position == 2) {
                            showLeagueHistoryStats(activity, simLeague, userTeam);
                        } else if (position == 3) {
                            HallofFameList hofAdapter = new HallofFameList(activity, hofPlayers, userTeam.getName(), false, activity);
                            leagueHistoryList.setAdapter(hofAdapter);
                        } else if (position == 4) {
                            showCoachDatabase(activity, simLeague, userTeam);
                        } else {
                            final LeagueHistoryList leagueHistoryAdapter =
                                    new LeagueHistoryList(activity, simLeague.getLeagueHistoryStr().split("%"), userTeam.getAbbr());
                            leagueHistoryList.setAdapter(leagueHistoryAdapter);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    static void showLeagueHistoryStats(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("League Stats")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "League Stat Leaders", "Compare long-term program achievements across titles, bowls, wins, and hall-of-fame production.");

        ArrayList<String> rankings = new ArrayList<>();
        String[] rankingsSelection =
                {"National Championships", "Conference Championships", "Bowl Victories", "Total Wins", "Hall of Famers"};
        Spinner teamRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamRankingsSpinner);
        ArrayAdapter<String> teamRankingsSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rankingsSelection);
        teamRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankingsSpinner.setAdapter(teamRankingsSpinnerAdapter);

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
        final ui.TeamRankingsList teamRankingsAdapter =
                new ui.TeamRankingsList(activity, rankings, userTeam.getName());
        teamRankingsList.setAdapter(teamRankingsAdapter);

        teamRankingsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getLeagueHistoryStats(position);
                        teamRankingsAdapter.setUserTeamStrRep(userTeam.getName());
                        teamRankingsAdapter.clear();
                        teamRankingsAdapter.addAll(rankings);
                        teamRankingsAdapter.notifyDataSetChanged();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    static void showCoachDatabase(final MainActivity activity, final League simLeague, final Team userTeam) {
        ArrayList<String> userNames = simLeague.getUserNames();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Head Coach Database")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Head Coach Database", "Track the best careers in the universe by wins, trophies, awards, and accumulated prestige.");

        ArrayList<String> rankings = new ArrayList<>();
        String[] rankingsSelection =
                {"National Championships", "Conference Championships", "Bowl Victories", "Total Wins", "Winning PCT", "Head Coach of the Year", "Conf Head Coach of Year", "All-Americans", "All-Conference", "Head Coach Career Score", "Head Coach Accumulated Prestige"};
        Spinner teamRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamRankingsSpinner);
        ArrayAdapter<String> teamRankingsSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rankingsSelection);
        teamRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankingsSpinner.setAdapter(teamRankingsSpinnerAdapter);

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
        final CoachDatabase coachDatabase =
                new CoachDatabase(activity, rankings, userTeam.getName(), activity, userNames);
        teamRankingsList.setAdapter(coachDatabase);

        teamRankingsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getCoachDatabase(position);
                        coachDatabase.setupUserHC(userTeam.getHeadCoach().getName() + " (" + userTeam.getAbbr() + ")");
                        coachDatabase.clear();
                        coachDatabase.addAll(rankings);
                        coachDatabase.notifyDataSetChanged();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
