package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.TextView;

import positions.Player;
import simulation.Team;

public final class PlayerProfileDialogController {
    private PlayerProfileDialogController() {}

    public static void showProfile(MainActivity activity, Player p, Team userTeam) {
        PlayerProfileSnapshot snapshot = PlayerProfileSnapshot.fromPlayer(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Player Profile")
                .setView(activity.getLayoutInflater().inflate(R.layout.player_profile, null, false))
                .setPositiveButton("Close", (dialog, which) -> {});

        if (p.team == userTeam) {
            builder.setNeutralButton("Cut", (dialog, which) -> showCutDialog(activity, p, userTeam));
        }

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        bindProfile(dialog, p.getName(), snapshot);
    }

    static void showCutDialog(MainActivity activity, Player p, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Player Cut")
                .setMessage("Are you sure you want to cut " + p.position + " " + p.getName()
                        + "?\n\nIf this cut occurs during season, he may be replaced with a walk-on to fill roster spots.")
                .setPositiveButton("Cut Player", (dialog, which) -> {
                    userTeam.cutPlayer(p);
                    activity.resetUI();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
    }

    static void bindProfile(AlertDialog dialog, String playerName, PlayerProfileSnapshot snapshot) {
        final TextView ppPlayerName = dialog.findViewById(R.id.ppPlayerName);
        final TextView ppPosition = dialog.findViewById(R.id.ppPosition);
        final TextView ppClass = dialog.findViewById(R.id.ppClass);
        final TextView ppTeam = dialog.findViewById(R.id.ppTeam);
        final TextView ppStars = dialog.findViewById(R.id.ppStars);
        final TextView ppHome = dialog.findViewById(R.id.ppHome);
        final TextView ppHeight = dialog.findViewById(R.id.ppHeight);
        final TextView ppWeight = dialog.findViewById(R.id.ppWeight);
        final TextView ppOverall = dialog.findViewById(R.id.ppOverall);

        final TextView ppAwarenessName = dialog.findViewById(R.id.ppAwarenessName);
        final TextView ppCharacterName = dialog.findViewById(R.id.ppCharacterName);
        final TextView ppDurabilityName = dialog.findViewById(R.id.ppDurabilityName);
        final TextView ppStatusName = dialog.findViewById(R.id.ppStatusName);
        final TextView ppAwareness = dialog.findViewById(R.id.ppAwarness);
        final TextView ppCharacter = dialog.findViewById(R.id.ppCharacter);
        final TextView ppDurability = dialog.findViewById(R.id.ppDurability);
        final TextView ppStatus = dialog.findViewById(R.id.ppStatus);

        final TextView ppAttr1Name = dialog.findViewById(R.id.ppAttr1Name);
        final TextView ppAttr1 = dialog.findViewById(R.id.ppAttr1);
        final TextView ppAttr2Name = dialog.findViewById(R.id.ppAttr2Name);
        final TextView ppAttr2 = dialog.findViewById(R.id.ppAttr2);
        final TextView ppAttr3Name = dialog.findViewById(R.id.ppAttr3Name);
        final TextView ppAttr3 = dialog.findViewById(R.id.ppAttr3);
        final TextView ppAttr4Name = dialog.findViewById(R.id.ppAttr4Name);
        final TextView ppAttr4 = dialog.findViewById(R.id.ppAttr4);

        final TextView ppYear = dialog.findViewById(R.id.ppYear);
        final TextView ppStat0 = dialog.findViewById(R.id.ppStat0);
        final TextView ppStat1 = dialog.findViewById(R.id.ppStat1);
        final TextView ppStat2 = dialog.findViewById(R.id.ppStat2);
        final TextView ppStat3 = dialog.findViewById(R.id.ppStat3);
        final TextView ppStat4 = dialog.findViewById(R.id.ppStat4);
        final TextView ppStat5 = dialog.findViewById(R.id.ppStat5);
        final TextView ppStat6 = dialog.findViewById(R.id.ppStat6);
        final TextView ppStat7 = dialog.findViewById(R.id.ppStat7);

        final TextView ppFeatStat1Name = dialog.findViewById(R.id.ppFeatStat1Name);
        final TextView ppFeatStat1 = dialog.findViewById(R.id.ppFeatStat1);
        final TextView ppFeatStat2Name = dialog.findViewById(R.id.ppFeatStat2Name);
        final TextView ppFeatStat2 = dialog.findViewById(R.id.ppFeatStat2);
        final TextView ppFeatStat3Name = dialog.findViewById(R.id.ppFeatStat3Name);
        final TextView ppFeatStat3 = dialog.findViewById(R.id.ppFeatStat3);
        final TextView ppFeatStat4Name = dialog.findViewById(R.id.ppFeatStat4Name);
        final TextView ppFeatStat4 = dialog.findViewById(R.id.ppFeatStat4);

        ppPlayerName.setText(playerName);
        String[] a = snapshot.basics;
        ppPosition.setText(valueAt(a, 0));
        ppClass.setText(valueAt(a, 1));
        ppTeam.setText(valueAt(a, 2));
        ppHome.setText(valueAt(a, 3));
        ppStars.setText(valueAt(a, 4));
        ppHeight.setText(valueAt(a, 5));
        ppWeight.setText(valueAt(a, 6));
        ppOverall.setText(valueAt(a, 7));
        ppCharacter.setText(valueAt(a, 8));
        ppAwareness.setText(valueAt(a, 9));
        ppStatus.setText(valueAt(a, 10));
        ppDurability.setText(valueAt(a, 11));

        String[] b = snapshot.ratings;
        ppAttr1Name.setText(valueAt(b, 0));
        ppAttr1.setText(valueAt(b, 1));
        ppAttr2Name.setText(valueAt(b, 2));
        ppAttr2.setText(valueAt(b, 3));
        ppAttr3Name.setText(valueAt(b, 4));
        ppAttr3.setText(valueAt(b, 5));
        ppAttr4Name.setText(valueAt(b, 6));
        ppAttr4.setText(valueAt(b, 7));

        String[] teamStat = snapshot.statColumns;
        ppYear.setText(valueAt(teamStat, 0));
        ppStat0.setText(valueAt(teamStat, 1));
        ppStat1.setText(valueAt(teamStat, 2));
        ppStat2.setText(valueAt(teamStat, 3));
        ppStat3.setText(valueAt(teamStat, 4));
        ppStat4.setText(valueAt(teamStat, 5));
        ppStat5.setText(valueAt(teamStat, 6));
        ppStat6.setText(valueAt(teamStat, 7));
        ppStat7.setText(valueAt(teamStat, 8));

        String[] c = snapshot.featuredStats;
        ppFeatStat1Name.setText(valueAt(c, 0));
        ppFeatStat1.setText(valueAt(c, 1));
        ppFeatStat2Name.setText(valueAt(c, 2));
        ppFeatStat2.setText(valueAt(c, 3));
        ppFeatStat3Name.setText(valueAt(c, 4));
        ppFeatStat3.setText(valueAt(c, 5));
        ppFeatStat4Name.setText(valueAt(c, 6));
        ppFeatStat4.setText(valueAt(c, 7));
    }

    public static int checkAward(Team currentTeam, String player) {
        Player p = currentTeam.findTeamPlayer(player);
        if (p == null) return 0;
        if (p.wonHeisman) return 3;
        if (p.wonAllAmerican) return 2;
        if (p.wonAllConference) return 1;
        return 0;
    }

    static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
