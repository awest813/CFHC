package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.TextView;

import staff.Staff;

public final class CoachProfileDialogController {
    private CoachProfileDialogController() {}

    public static void showProfile(MainActivity activity, Staff p, Runnable onHistoryClick) {
        CoachProfileSnapshot snapshot = CoachProfileSnapshot.fromStaff(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(p.getName())
                .setView(activity.getLayoutInflater().inflate(R.layout.coach_profile, null, false))
                .setPositiveButton("Close", (dialog, which) -> {})
                .setNeutralButton("Coach History", (dialog, which) -> onHistoryClick.run());

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        bindProfile(dialog, snapshot);
    }

    static void bindProfile(AlertDialog dialog, CoachProfileSnapshot snapshot) {
        final TextView cpPosition = dialog.findViewById(R.id.cpPosition);
        final TextView cpClass = dialog.findViewById(R.id.cpClass);
        final TextView cpTeam = dialog.findViewById(R.id.cpTeam);
        final TextView cpOverall = dialog.findViewById(R.id.cpOverall);

        final TextView cpWins = dialog.findViewById(R.id.cpWins);
        final TextView cpLosses = dialog.findViewById(R.id.cpLosses);
        final TextView cpContract = dialog.findViewById(R.id.cpContract);
        final TextView cpStatus = dialog.findViewById(R.id.cpStatus);

        final TextView cpAttr1Name = dialog.findViewById(R.id.cpAttr1Name);
        final TextView cpAttr1 = dialog.findViewById(R.id.cpAttr1);
        final TextView cpAttr2Name = dialog.findViewById(R.id.cpAttr2Name);
        final TextView cpAttr2 = dialog.findViewById(R.id.cpAttr2);
        final TextView cpAttr3Name = dialog.findViewById(R.id.cpAttr3Name);
        final TextView cpAttr3 = dialog.findViewById(R.id.cpAttr3);
        final TextView cpAttr4Name = dialog.findViewById(R.id.cpAttr4Name);
        final TextView cpAttr4 = dialog.findViewById(R.id.cpAttr4);

        final TextView cpFeatStat1Name = dialog.findViewById(R.id.cpFeatStat1Name);
        final TextView cpFeatStat1 = dialog.findViewById(R.id.cpFeatStat1);
        final TextView cpFeatStat2Name = dialog.findViewById(R.id.cpFeatStat2Name);
        final TextView cpFeatStat2 = dialog.findViewById(R.id.cpFeatStat2);
        final TextView cpFeatStat3Name = dialog.findViewById(R.id.cpFeatStat3Name);
        final TextView cpFeatStat3 = dialog.findViewById(R.id.cpFeatStat3);
        final TextView cpFeatStat4Name = dialog.findViewById(R.id.cpFeatStat4Name);
        final TextView cpFeatStat4 = dialog.findViewById(R.id.cpFeatStat4);

        String[] a = snapshot.basics;
        cpPosition.setText(valueAt(a, 0));
        cpClass.setText(valueAt(a, 1));
        cpTeam.setText(valueAt(a, 2));
        cpOverall.setText(valueAt(a, 3));
        cpWins.setText(valueAt(a, 4));
        cpLosses.setText(valueAt(a, 5));
        cpStatus.setText(valueAt(a, 6));
        cpContract.setText(valueAt(a, 7));

        String[] b = snapshot.ratings;
        cpAttr1Name.setText(valueAt(b, 0));
        cpAttr1.setText(valueAt(b, 1));
        cpAttr2Name.setText(valueAt(b, 2));
        cpAttr2.setText(valueAt(b, 3));
        cpAttr3Name.setText(valueAt(b, 4));
        cpAttr3.setText(valueAt(b, 5));
        cpAttr4Name.setText(valueAt(b, 6));
        cpAttr4.setText(valueAt(b, 7));

        String[] c = snapshot.featuredStats;
        cpFeatStat1Name.setText(valueAt(c, 0));
        cpFeatStat1.setText(valueAt(c, 1));
        cpFeatStat2Name.setText(valueAt(c, 2));
        cpFeatStat2.setText(valueAt(c, 3));
        cpFeatStat3Name.setText(valueAt(c, 4));
        cpFeatStat3.setText(valueAt(c, 5));
        cpFeatStat4Name.setText(valueAt(c, 6));
        cpFeatStat4.setText(valueAt(c, 7));
    }

    static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
