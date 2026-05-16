package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import simulation.League;

final class PostseasonDialogController {

    private PostseasonDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Postseason Games")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.bowl_ccg_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindArchiveDialogShell(dialog, "Postseason Archive", "Switch between conference title games and postseason slates in one shared archive shell.");

        String[] selection = {"Conf Championships", "Postseason"};
        Spinner bowlCCGSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
        PlatformUiHelper.avoidSpinnerDropdownFocus(bowlCCGSpinner);
        ArrayAdapter<String> bowlCCGadapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, selection);
        bowlCCGadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bowlCCGSpinner.setAdapter(bowlCCGadapter);

        final TextView bowlCCGscores = dialog.findViewById(R.id.textViewBowlCCGDialog);

        bowlCCGSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            bowlCCGscores.setText(simLeague.getCCGsStr());
                        } else {
                            bowlCCGscores.setText(simLeague.getBowlGameWatchStr());
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
