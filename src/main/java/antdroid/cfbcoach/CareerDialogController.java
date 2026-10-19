package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
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
        builder.setTitle("A Brief Look Back")
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

    public static void showSeasonGoalsDialog(MainActivity activity, String goals, int projectedRank,
                                             String projectedRecord, String conferenceFinish,
                                             Runnable onSave) {
        android.view.View content = android.view.LayoutInflater.from(activity)
                .inflate(R.layout.season_goals_dialog, null, false);
        ((android.widget.TextView) content.findViewById(R.id.goalsPill))
                .setText(activity.getString(R.string.goals_pill, activity.simLeague.getYear()));
        ((android.widget.TextView) content.findViewById(R.id.goalsRank))
                .setText(projectedRank > 0 ? "#" + projectedRank : "\u2014");
        ((android.widget.TextView) content.findViewById(R.id.goalsRecord))
                .setText(projectedRecord == null || projectedRecord.isEmpty() ? "\u2014" : projectedRecord);
        ((android.widget.TextView) content.findViewById(R.id.goalsConference))
                .setText(conferenceFinish == null ? "" : conferenceFinish);
        ((android.widget.TextView) content.findViewById(R.id.goalsBody))
                .setText(goals == null ? "" : goals.trim());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(content)
                .setPositiveButton(R.string.goals_continue, (dialog, which) -> {})
                .setNegativeButton(R.string.goals_save, (dialog, which) -> {
                    if (onSave != null) onSave.run();
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
    }

    public static void showJobOffersDialog(MainActivity activity, String title, String message,
                                            String[] teams, Consumer<Integer> onSelect) {
        java.util.ArrayList<ChoiceCardDialog.Choice> choices = new java.util.ArrayList<>();
        if (teams != null) {
            for (String team : teams) {
                choices.add(jobChoice(team));
            }
        }
        showChoiceList(activity, activity.getString(R.string.dlg_pill_carousel), title, message,
                null, null, choices, onSelect);
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
        java.util.ArrayList<ChoiceCardDialog.Choice> choices = new java.util.ArrayList<>();
        if (teams != null) {
            for (String team : teams) {
                choices.add(jobChoice(team));
            }
        }
        showChoiceList(activity, activity.getString(R.string.dlg_pill_carousel), title, message,
                activity.getString(R.string.dlg_carousel_decline), onDecline, choices, onSelect);
    }

    public static void showSelectNewTeamDialog(MainActivity activity, String[] teams,
                                                Consumer<Integer> onSelect) {
        java.util.ArrayList<ChoiceCardDialog.Choice> choices = new java.util.ArrayList<>();
        if (teams != null) {
            for (String team : teams) {
                choices.add(teamListChoice(team));
            }
        }
        showChoiceList(activity, activity.getString(R.string.dlg_pill_carousel),
                "Choose Your New Team",
                "Every program is open. Prestige is the number on the right.",
                null, null, choices, onSelect);
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
        roster.setTitle(team.getName() + " Program Review");
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
        // Row 0 renews the current coordinator when there is one; the candidates
        // follow it. (They used to share indexes with it, hiding the top candidate.)
        final int num = (isOffense ? userTeam.OC : userTeam.DC) != null ? 1 : 0;
        String[] items = new String[list.size() + num];

        if (isOffense) {
            final simulation.PlaybookOffense[] playbook = userTeam.getPlaybookOff();
            if (userTeam.OC != null) {
                items[0] = userTeam.OC.getName() + " [current]\nAge: " + userTeam.OC.age
                        + "  Off: " + userTeam.OC.ratOff + "  Tal: " + userTeam.OC.ratTalent
                        + "  " + playbook[userTeam.OC.offStrat].getStratName() + "\n";
            }
            for (int i = 0; i < list.size(); i++) {
                items[i + num] = list.get(i).name + "\nAge: " + list.get(i).age
                        + "  Off: " + list.get(i).ratOff + "  Tal: " + list.get(i).ratTalent
                        + "  " + playbook[list.get(i).offStrat].getStratName() + "\n";
            }
        } else {
            final simulation.PlaybookDefense[] playbook = userTeam.getPlaybookDef();
            if (userTeam.DC != null) {
                items[0] = userTeam.DC.getName() + " [current]\nAge: " + userTeam.DC.age
                        + "  Def: " + userTeam.DC.ratDef + "  Tal: " + userTeam.DC.ratTalent
                        + "  " + playbook[userTeam.DC.defStrat].getStratName() + "\n";
            }
            for (int i = 0; i < list.size(); i++) {
                items[i + num] = list.get(i).name + "\nAge: " + list.get(i).age
                        + "  Def: " + list.get(i).ratDef + "  Tal: " + list.get(i).ratTalent
                        + "  " + playbook[list.get(i).defStrat].getStratName() + "\n";
            }
        }

        String title = isOffense ? "Offensive coordinators" : "Defensive coordinators";
        java.util.ArrayList<ChoiceCardDialog.Choice> choices = new java.util.ArrayList<>();
        for (String item : items) {
            choices.add(coordinatorChoice(item, isOffense, activity.getString(R.string.dlg_carousel_current)));
        }
        showChoiceList(activity, activity.getString(R.string.dlg_pill_carousel), title,
                "Age, side rating, recruiting talent, and scheme. The current coach can be renewed.",
                null, null, choices, item -> {
            if (isOffense) {
                if (item == 0 && userTeam.OC != null) {
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    userTeam.OC.baselinePrestige = 0;
                } else {
                    final Staff hired = list.get(item - num);
                    simLeague.releaseCoordinator(userTeam.OC, userTeam);
                    userTeam.OC = new staff.OC(hired, userTeam);
                    simLeague.addNewsHeadline(userTeam.getName() + " adds new Off Coord " + userTeam.OC.getName());
                    simLeague.addNewsStory(simLeague.currentWeek, "Off Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, " + userTeam.getName()
                            + " has hired " + userTeam.OC.getName() + " to lead the offense.");
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    simLeague.removeCoachFreeAgent(hired);

                    if (isNewTeam) {
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
                    final Staff hired = list.get(item - num);
                    simLeague.releaseCoordinator(userTeam.DC, userTeam);
                    userTeam.DC = new staff.DC(hired, userTeam);
                    simLeague.addNewsHeadline(userTeam.getName() + " adds new Def Coord " + userTeam.DC.getName());
                    simLeague.addNewsStory(simLeague.currentWeek, "Def Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, " + userTeam.getName()
                            + " has hired " + userTeam.DC.getName() + " to lead the defense.");
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    simLeague.removeCoachFreeAgent(hired);
                    simLeague.coordinatorCarousel();
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }

    /** Broadcast card list for carousel openings. A tap selects that row. */
    private static void showChoiceList(MainActivity activity, String pill, String title, String subtitle,
                                        String negative, Runnable onNegative,
                                        java.util.List<ChoiceCardDialog.Choice> choices,
                                        Consumer<Integer> onSelect) {
        android.view.View content = activity.getLayoutInflater()
                .inflate(R.layout.simple_list_dialog, null, false);
        TextView pillView = content.findViewById(R.id.textSimpleDialogShellLabel);
        if (pillView != null && pill != null && !pill.isEmpty()) {
            pillView.setText(pill);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(content)
                .setCancelable(false);
        if (negative != null) {
            builder.setNegativeButton(negative, (dialog, which) -> {
                if (onNegative != null) onNegative.run();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, title, subtitle);
        android.widget.ListView list = dialog.findViewById(R.id.listViewDialog);
        list.setAdapter(new android.widget.ArrayAdapter<ChoiceCardDialog.Choice>(
                activity, R.layout.choice_card_row, choices) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                                              android.view.ViewGroup parent) {
                android.view.View row = convertView != null ? convertView
                        : android.view.LayoutInflater.from(getContext())
                        .inflate(R.layout.choice_card_row, parent, false);
                ChoiceCardDialog.Choice choice = getItem(position);
                TextView rowTitle = row.findViewById(R.id.choiceRowTitle);
                TextView badge = row.findViewById(R.id.choiceRowBadge);
                TextView body = row.findViewById(R.id.choiceRowBody);
                rowTitle.setText(choice != null ? choice.title : "");
                bindChoiceText(badge, choice != null ? choice.badge : null);
                bindChoiceText(body, choice != null ? choice.body : null);
                return row;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (onSelect != null) onSelect.accept(position);
        });
    }

    private static void bindChoiceText(TextView view, String text) {
        if (view == null) return;
        if (text == null || text.trim().isEmpty()) {
            view.setVisibility(android.view.View.GONE);
            view.setText("");
        } else {
            view.setVisibility(android.view.View.VISIBLE);
            view.setText(text.trim());
        }
    }

    /** Job-offer block: name, prestige line, swing line. */
    static ChoiceCardDialog.Choice jobChoice(String block) {
        String[] lines = block == null ? new String[0] : block.split("\n");
        String name = lines.length > 0 ? lines[0].trim() : "";
        String body = lines.length > 1 ? lines[1].replace("  |  ", "  ·  ").trim() : "";
        String badge = "";
        if (lines.length > 2) {
            String swing = lines[2];
            int mark = swing.indexOf(':');
            String rest = mark >= 0 ? swing.substring(mark + 1).trim() : swing.trim();
            int space = rest.indexOf(' ');
            badge = space > 0 ? rest.substring(0, space) : rest;
        }
        return new ChoiceCardDialog.Choice(name, badge, body);
    }

    /** {@code Conference:  Name  [prestige]}. */
    static ChoiceCardDialog.Choice teamListChoice(String line) {
        String raw = line == null ? "" : line.trim();
        int colon = raw.indexOf(':');
        String conference = colon >= 0 ? raw.substring(0, colon).trim() : "";
        String rest = colon >= 0 ? raw.substring(colon + 1).trim() : raw;
        String badge = "";
        int bracket = rest.lastIndexOf('[');
        String name = rest;
        if (bracket >= 0 && rest.endsWith("]")) {
            badge = rest.substring(bracket + 1, rest.length() - 1).trim();
            name = rest.substring(0, bracket).trim();
        }
        return new ChoiceCardDialog.Choice(name, badge, conference);
    }

    static ChoiceCardDialog.Choice coordinatorChoice(String block, boolean offense, String currentLabel) {
        String raw = block == null ? "" : block.trim();
        String[] lines = raw.split("\n");
        String title = lines.length > 0 ? lines[0].replace(" [current]", "").trim() : "";
        String body = lines.length > 1 ? lines[1].trim() : "";
        String badge = lines.length > 0 && lines[0].contains("[current]") ? currentLabel : "";
        if (badge.isEmpty()) {
            badge = ratingToken(body, offense ? "Off:" : "Def:");
        }
        return new ChoiceCardDialog.Choice(title, badge, body);
    }

    private static String ratingToken(String body, String label) {
        int at = body.indexOf(label);
        if (at < 0) return "";
        String rest = body.substring(at + label.length()).trim();
        int space = rest.indexOf(' ');
        return space > 0 ? rest.substring(0, space) : rest;
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
