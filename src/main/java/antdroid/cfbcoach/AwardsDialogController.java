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
import simulation.Team;
import ui.SeasonAwardsList;
import ui.TeamRankingsList;

final class AwardsDialogController {

    private AwardsDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague, final Team userTeam) {
        if (simLeague.currentWeek < simLeague.regSeasonWeeks) {
            showWatchList(activity, simLeague, userTeam);
        } else {
            showCeremony(activity, simLeague, userTeam);
        }
    }

    private static void showWatchList(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Player of the Year Watch")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Player of the Year Watch", "Browse preseason award watch lists for every position group.");
        ArrayList<String> rankings = new ArrayList<>();
        String[] rankingsSelection =
                {"Head Coach - Overall", "QB - Overall", "RB - Overall", "WR - Overall", "TE - Overall", "OL - Overall", "K - Overall", "DL - Overall", "LB - Overall", "CB - Overall", "S - Overall"};
        Spinner teamRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamRankingsSpinner);
        ArrayAdapter<String> teamRankingsSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rankingsSelection);
        teamRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankingsSpinner.setAdapter(teamRankingsSpinnerAdapter);

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(activity, rankings, userTeam.getAbbr());
        teamRankingsList.setAdapter(teamRankingsAdapter);

        teamRankingsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getAwardsWatch(position);
                        teamRankingsAdapter.setUserTeamStrRep(userTeam.getAbbr());
                        teamRankingsAdapter.clear();
                        teamRankingsAdapter.addAll(rankings);
                        teamRankingsAdapter.notifyDataSetChanged();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    private static void showCeremony(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Postseason Awards")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Postseason Awards", "Review the complete awards slate — Heisman, All-American, player of the year, and all-conference teams.");

        String[] selection;
        int confNum = 0;
        for (int i = 0; i < simLeague.getConferences().size(); ++i) {
            if (simLeague.getConferences().get(i).confTeams.size() >= simLeague.getConferences().get(i).minConfTeams)
                confNum++;
        }
        selection = new String[6 + confNum];
        selection[0] = "Offensive Player of the Year";
        selection[1] = "Defensive Player of the Year";
        selection[2] = "Head Coach of the Year";
        selection[3] = "Freshman of the Year";
        selection[4] = "All-American Team";
        selection[5] = "All-Freshman Team";

        confNum = 0;
        for (int i = 0; i < simLeague.getConferences().size(); ++i) {
            if (simLeague.getConferences().get(i).confTeams.size() >= simLeague.getConferences().get(i).minConfTeams) {
                selection[confNum + 6] = simLeague.getConferences().get(i).confName + " All-Conf Team";
                confNum++;
            }
        }

        Spinner potySpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(potySpinner);
        ArrayAdapter<String> potyAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        potyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        potySpinner.setAdapter(potyAdapter);

        final ListView potyList = dialog.findViewById(R.id.listViewTeamRankings);

        final String[] coachAwardList = simLeague.getCoachAwardStr().split(">");
        final String[] defAwardList = simLeague.getDefensePOTYStr().split(">");
        final String[] freshmanAwardList = simLeague.getFreshmanCeremonyStr().split(">");
        final String[] allAmericans = simLeague.getAllAmericanStr().split(">");
        final String[] allFreshman = simLeague.getAllFreshmanStr().split(">");
        final String[][] allConference = new String[simLeague.getConferences().size()][];

        confNum = 0;
        for (int i = 0; i < simLeague.getConferences().size(); ++i) {
            if (simLeague.getConferences().get(i).confTeams.size() >= simLeague.getConferences().get(i).minConfTeams) {
                allConference[confNum] = simLeague.getAllConfStr(i).split(">");
                confNum++;
            }
        }

        potySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            potyList.setAdapter(new SeasonAwardsList(activity, simLeague.getHeismanCeremonyStr().split(">"), userTeam.getAbbr()));
                        } else if (position == 1) {
                            potyList.setAdapter(new SeasonAwardsList(activity, defAwardList, userTeam.getAbbr()));
                        } else if (position == 2) {
                            potyList.setAdapter(new SeasonAwardsList(activity, coachAwardList, userTeam.getAbbr()));
                        } else if (position == 3) {
                            potyList.setAdapter(new SeasonAwardsList(activity, freshmanAwardList, userTeam.getAbbr()));
                        } else if (position == 4) {
                            potyList.setAdapter(new SeasonAwardsList(activity, allAmericans, userTeam.getAbbr()));
                        } else if (position == 5) {
                            potyList.setAdapter(new SeasonAwardsList(activity, allFreshman, userTeam.getAbbr()));
                        } else {
                            potyList.setAdapter(new SeasonAwardsList(activity, allConference[position - 6], userTeam.getAbbr()));
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
