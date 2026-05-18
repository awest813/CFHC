package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.function.Consumer;

import simulation.League;
import simulation.Team;
import staff.HeadCoach;
import staff.Staff;
import ui.TeamHistoryList;

public final class CareerDialogController {
    private CareerDialogController() {}

    public static void showContractDialog(MainActivity activity, String message, String title,
                                           Runnable onSeasonGoals, Runnable onShowSeasonGoals,
                                           Runnable onShowNews) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNeutralButton("Season Goals", (dialog, which) -> {
                    if (onSeasonGoals != null) onSeasonGoals.run();
                    else if (onShowSeasonGoals != null) onShowSeasonGoals.run();
                })
                .setNegativeButton("View Coaching News", (dialog, which) -> {
                    if (onShowNews != null) onShowNews.run();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        setDialogMessageTextSize(dialog);
    }

    public static void showRetirementQuestion(MainActivity activity, Runnable onContinue,
                                               Runnable onReincarnate, Runnable onRetire) {
        String msg = "You have reached that time in your life when you need to decide to hang it up and retire or continue on. " +
                "At this point, if you choose to continue, your ability to increase skill ratings will be much more challenging. " +
                "You may also retire and end your career. " +
                "Finally, you can choose to reincarnate yourself as a fresh new head coach in his 30s in this same universe!";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg)
                .setTitle("Retirement Age")
                .setPositiveButton("Continue Career", (dialog, which) -> {
                    if (onContinue != null) onContinue.run();
                    dialog.dismiss();
                })
                .setNeutralButton("Reincarnate", (dialog, which) -> {
                    if (onReincarnate != null) onReincarnate.run();
                    dialog.dismiss();
                })
                .setNegativeButton("Retire", (dialog, which) -> {
                    if (onRetire != null) onRetire.run();
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        setDialogMessageTextSize(dialog);
    }

    public static void showRetireSummary(MainActivity activity, Team currentTeam, Runnable onExit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("A brief look back...")
                .setPositiveButton("EXIT GAME", (dialog, which) -> {
                    if (onExit != null) onExit.run();
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        setDialogMessageTextSize(dialog);

        String[] selection = {"Team History"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        teamHistSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                if (position == 0) {
                    if (currentTeam.getHeadCoach() == null) return;
                    TeamHistoryList teamHistoryAdapter =
                            new TeamHistoryList(activity, currentTeam.getHeadCoach().getCoachHistory());
                    teamHistoryList.setAdapter(teamHistoryAdapter);
                }
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    public static void showReincarnationDialog(MainActivity activity, Team userTeam, Team currentTeam,
                                                Runnable onSameTeam, Runnable onPickNewTeam) {
        HeadCoach currentCoach = currentTeam.getHeadCoach();
        String currentCoachName = currentCoach != null ? currentCoach.name : "Head Coach";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Head Coach History: " + currentCoachName)
                .setPositiveButton("Use Same Team", (dialog, which) -> {
                    if (onSameTeam != null) onSameTeam.run();
                    dialog.dismiss();
                })
                .setNeutralButton("Pick New Team", (dialog, which) -> {
                    if (onPickNewTeam != null) onPickNewTeam.run();
                    dialog.dismiss();
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);

        String[] selection = {"Team History"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        teamHistSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                if (position == 0) {
                    if (currentTeam.getHeadCoach() == null) return;
                    TeamHistoryList teamHistoryAdapter =
                            new TeamHistoryList(activity, currentTeam.getHeadCoach().getCoachHistory());
                    teamHistoryList.setAdapter(teamHistoryAdapter);
                }
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    public static void showSeasonGoalsDialog(MainActivity activity, String goals, Runnable onSave) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(goals + "\nNote: You can always review your season goals in the Pre-Season News.")
                .setTitle(activity.simLeague.getYear() + " Season Goals")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNegativeButton("SAVE PROGRESS", (dialog, which) -> {
                    if (onSave != null) onSave.run();
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        setDialogMessageTextSize(dialog);
    }

    public static void showJobOffersDialog(MainActivity activity, String title, String message,
                                            String[] teams, Consumer<Integer> onSelect) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false);
        builder.setItems(teams, (dialog, item) -> {
            if (onSelect != null) onSelect.accept(item);
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        activity.showImmersive(alert);
    }

    public static void showJobOffersEmpty(MainActivity activity, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {});
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        activity.showImmersive(alert);
    }

    public static void showPromotionsDialog(MainActivity activity, String title, String message,
                                             String[] teams, Consumer<Integer> onSelect,
                                             Runnable onDecline) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Decline Offers", (dialog, which) -> {
                    if (onDecline != null) onDecline.run();
                });
        builder.setCancelable(false);
        builder.setItems(teams, (dialog, item) -> {
            if (onSelect != null) onSelect.accept(item);
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        activity.showImmersive(alert);
    }

    public static void showSelectNewTeamDialog(MainActivity activity, String[] teams,
                                                Consumer<Integer> onSelect) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose your new team:");
        builder.setItems(teams, (dialog, item) -> {
            if (onSelect != null) onSelect.accept(item);
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        activity.showImmersive(alert);
    }

    public static void showTeamReviewDialog(MainActivity activity, Team team, String rosterSummary,
                                             Runnable onBack, Runnable onAccept) {
        String[] teamRoster = team.getTeamRosterString();
        StringBuilder sb = new StringBuilder();
        sb.append(rosterSummary);
        sb.append("Projected roster\n");
        for (String s : teamRoster) {
            if (s != null) sb.append(s).append("\n");
        }

        AlertDialog.Builder roster = new AlertDialog.Builder(activity);
        roster.setTitle(team.getName() + " Program Review" +
                        "\nPrestige #" + team.getRankTeamPrestige() + " | Off "
                        + team.getTeamOffTalent() + " | Def " + team.getTeamDefTalent());
        roster.setNeutralButton("Back to Offers", (dialog, which) -> {
            if (onBack != null) onBack.run();
        });
        roster.setPositiveButton("Accept", (dialog, which) -> {
            if (onAccept != null) onAccept.run();
        });
        roster.setMessage(sb.toString());
        roster.setCancelable(false);
        AlertDialog teamWindow = roster.create();
        teamWindow.setCancelable(false);
        activity.showImmersive(teamWindow);
    }

    public static void showHireCoordinatorDialog(MainActivity activity, boolean isOffense,
                                                  boolean isNewTeam,
                                                  java.util.ArrayList<Staff> list,
                                                  Team userTeam, League simLeague,
                                                  Runnable onComplete) {
        String[] items = new String[list.size()];
        int num = 0;

        if (isOffense) {
            final simulation.PlaybookOffense[] playbook = userTeam.getPlaybookOff();
            if (userTeam.OC != null) {
                num = 1;
                items[0] = userTeam.OC.getName() + " [current]\nAge: " + userTeam.OC.age
                        + "  Off: " + userTeam.OC.ratOff + "  Tal: " + userTeam.OC.ratTalent
                        + "  " + playbook[userTeam.OC.offStrat].getStratName() + "\n";
            }
            for (int i = num; i < list.size(); i++) {
                items[i] = list.get(i).name + "\nAge: " + list.get(i).age
                        + "  Off: " + list.get(i).ratOff + "  Tal: " + list.get(i).ratTalent
                        + "  " + playbook[list.get(i).offStrat].getStratName() + "\n";
            }
        } else {
            final simulation.PlaybookDefense[] playbook = userTeam.getPlaybookDef();
            if (userTeam.DC != null) {
                num = 1;
                items[0] = userTeam.DC.getName() + " [current]\nAge: " + userTeam.DC.age
                        + "  Def: " + userTeam.DC.ratDef + "  Tal: " + userTeam.DC.ratTalent
                        + "  " + playbook[userTeam.DC.defStrat].getStratName() + "\n";
            }
            for (int i = num; i < list.size(); i++) {
                items[i] = list.get(i).name + "\nAge: " + list.get(i).age
                        + "  Def: " + list.get(i).ratDef + "  Tal: " + list.get(i).ratTalent
                        + "  " + playbook[list.get(i).defStrat].getStratName() + "\n";
            }
        }

        String title = isOffense ? "Off Coordinators Available:" : "Def Coordinators Available:";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setItems(items, (dialog, item) -> {
            if (isOffense) {
                if (item == 0 && userTeam.OC != null) {
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    userTeam.OC.baselinePrestige = 0;
                } else {
                    userTeam.OC = new staff.OC(list.get(item), userTeam);
                    simLeague.getNewsHeadlines().add(userTeam.getName() + " adds new Off Coord " + userTeam.OC.getName());
                    simLeague.getNewsStories().get(simLeague.currentWeek).add("Off Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, " + userTeam.getName()
                            + " has hired " + userTeam.OC.getName() + " to lead the offense.");
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    simLeague.getCoachFreeAgents().remove(list.get(item));

                    if (isNewTeam) {
                        dialog.dismiss();
                        CareerDialogController.showHireCoordinatorDialog(activity, false, true,
                                simLeague.getDCList(userTeam.getHeadCoach()), userTeam, simLeague, onComplete);
                        return;
                    } else if (userTeam.DC == null || userTeam.DC.contractYear >= userTeam.DC.contractLength) {
                        CareerDialogController.showHireCoordinatorDialog(activity, false, false,
                                simLeague.getDCList(userTeam.getHeadCoach()), userTeam, simLeague, onComplete);
                        return;
                    } else {
                        simLeague.coordinatorCarousel();
                    }
                }
            } else {
                if (item == 0 && userTeam.DC != null) {
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    userTeam.DC.baselinePrestige = 0;
                } else {
                    userTeam.DC = new staff.DC(list.get(item), userTeam);
                    simLeague.getNewsHeadlines().add(userTeam.getName() + " adds new Def Coord " + userTeam.DC.getName());
                    simLeague.getNewsStories().get(simLeague.currentWeek).add("Def Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, " + userTeam.getName()
                            + " has hired " + userTeam.DC.getName() + " to lead the defense.");
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    simLeague.getCoachFreeAgents().remove(list.get(item));
                    simLeague.coordinatorCarousel();
                }
            }
            if (onComplete != null) onComplete.run();
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        activity.showImmersive(alert);
    }

    public static void showSimpleMessage(MainActivity activity, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        setDialogMessageTextSize(dialog);
    }

    public static String[] buildJobTeamList(ArrayList<Team> teams, HeadCoach userHC, DecimalFormat df2) {
        String[] temp = new String[teams.size()];
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            int prestigeDelta = team.getTeamPrestige() - userHC.baselinePrestige;
            String direction = prestigeDelta > 0 ? "+" : "";
            temp[i] = team.getName()
                    + "\nPrestige #" + team.getRankTeamPrestige()
                    + "  |  Talent " + df2.format(team.getTeamOffTalent() + team.getTeamDefTalent())
                    + "  |  Fit Req " + team.getMinCoachHireReq()
                    + "\nProgram swing: " + direction + prestigeDelta + " prestige vs your current baseline";
        }
        return temp;
    }

    public static String buildJobOpportunitySummary(Team team, HeadCoach userHC, DecimalFormat df2) {
        int prestigeDelta = team.getTeamPrestige() - userHC.baselinePrestige;
        String direction = prestigeDelta > 0 ? "+" : "";
        StringBuilder summary = new StringBuilder();
        summary.append("Program snapshot\n");
        summary.append("Prestige: #").append(team.getRankTeamPrestige())
                .append(" (").append(team.getTeamPrestige()).append(")  |  Conference: ").append(team.getConfPrestige()).append("\n");
        summary.append("Roster talent: Off ").append(df2.format(team.getTeamOffTalent()))
                .append("  |  Def ").append(df2.format(team.getTeamDefTalent()))
                .append("  |  Combined ").append(df2.format(team.getTeamOffTalent() + team.getTeamDefTalent())).append("\n");
        summary.append("Coach fit threshold: ").append(team.getMinCoachHireReq())
                .append("  |  Career swing: ").append(direction).append(prestigeDelta).append(" prestige\n");
        summary.append("If you accept, your head coach contract resets to a fresh 6-year deal and your AD expectations will be recalibrated to this job.\n\n");
        return summary.toString();
    }

    static void setDialogMessageTextSize(AlertDialog dialog) {
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }
}
