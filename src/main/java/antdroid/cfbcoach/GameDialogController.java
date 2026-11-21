package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import simulation.Game;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

final class GameDialogController {
    private GameDialogController() {
    }

    static void show(final MainActivity activity, final Game g, final Team userTeam) {
        if (g.hasPlayed) {
            showSummary(activity, g);
        } else {
            showScout(activity, g, userTeam);
        }
    }

    private static void showSummary(final MainActivity activity, final Game g) {
        final String[] gameStr = g.getGameSummaryStr();

        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle(g.awayTeam.getName() + " @ " + g.homeTeam.getName() + ": " + g.gameName)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.game_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        // Game score
        final TextView gameAwayScore = dialog.findViewById(R.id.gameDialogScoreAway);
        final TextView gameHomeScore = dialog.findViewById(R.id.gameDialogScoreHome);
        final TextView gameAwayScoreName = dialog.findViewById(R.id.gameDialogScoreAwayName);
        final TextView gameHomeScoreName = dialog.findViewById(R.id.gameDialogScoreHomeName);
        gameAwayScore.setText(g.awayScore + "");
        gameHomeScore.setText(g.homeScore + "");
        gameAwayScoreName.setText(g.awayTeam.getStrAbbrWL_2Lines());
        gameHomeScoreName.setText(g.homeTeam.getStrAbbrWL_2Lines());

        final TextView awayTeam = dialog.findViewById(R.id.teamAway);
        final TextView awayQT1 = dialog.findViewById(R.id.awayQT1);
        final TextView awayQT2 = dialog.findViewById(R.id.awayQT2);
        final TextView awayQT3 = dialog.findViewById(R.id.awayQT3);
        final TextView awayQT4 = dialog.findViewById(R.id.awayQT4);
        final TextView awayOT = dialog.findViewById(R.id.awayOT);
        final TextView homeTeam = dialog.findViewById(R.id.teamHome);
        final TextView homeQT1 = dialog.findViewById(R.id.homeQT1);
        final TextView homeQT2 = dialog.findViewById(R.id.homeQT2);
        final TextView homeQT3 = dialog.findViewById(R.id.homeQT3);
        final TextView homeQT4 = dialog.findViewById(R.id.homeQT4);
        final TextView homeOT = dialog.findViewById(R.id.homeOT);
        final TextView scoreOT = dialog.findViewById(R.id.scoreOT);

        awayTeam.setText(g.awayTeam.getAbbr());
        awayQT1.setText(Integer.toString(scoreAt(g.awayQScore, 0)));
        awayQT2.setText(Integer.toString(scoreAt(g.awayQScore, 1)));
        awayQT3.setText(Integer.toString(scoreAt(g.awayQScore, 2)));
        awayQT4.setText(Integer.toString(scoreAt(g.awayQScore, 3)));

        homeTeam.setText(g.homeTeam.getAbbr());
        homeQT1.setText(Integer.toString(scoreAt(g.homeQScore, 0)));
        homeQT2.setText(Integer.toString(scoreAt(g.homeQScore, 1)));
        homeQT3.setText(Integer.toString(scoreAt(g.homeQScore, 2)));
        homeQT4.setText(Integer.toString(scoreAt(g.homeQScore, 3)));

        if (g.numOT > 0) {
            int awayOTscore = g.awayScore - (scoreAt(g.awayQScore, 0) + scoreAt(g.awayQScore, 1) + scoreAt(g.awayQScore, 2) + scoreAt(g.awayQScore, 3));
            int homeOTscore = g.homeScore - (scoreAt(g.homeQScore, 0) + scoreAt(g.homeQScore, 1) + scoreAt(g.homeQScore, 2) + scoreAt(g.homeQScore, 3));
            awayOT.setText(Integer.toString(awayOTscore));
            homeOT.setText(Integer.toString(homeOTscore));
        } else {
            awayOT.setText("");
            homeOT.setText("");
            scoreOT.setText("");
        }

        final TextView gameDialogScoreDashName = dialog.findViewById(R.id.gameDialogScoreDashName);
        if (g.numOT > 0) {
            gameDialogScoreDashName.setText(g.numOT + "OT");
        } else gameDialogScoreDashName.setText("@");

        final TextView gameL = dialog.findViewById(R.id.gameDialogLeft);
        gameL.setText(PlatformUiHelper.valueAt(gameStr, 0));
        final TextView gameC = dialog.findViewById(R.id.gameDialogCenter);
        gameC.setText(PlatformUiHelper.valueAt(gameStr, 1));
        final TextView gameR = dialog.findViewById(R.id.gameDialogRight);
        gameR.setText(PlatformUiHelper.valueAt(gameStr, 2));
        alignColumns(gameL, gameC, gameR);

        final View ql = dialog.findViewById(R.id.gameDialogQBLeft);
        final View qc = dialog.findViewById(R.id.gameDialogQBCenter);
        final View qr = dialog.findViewById(R.id.gameDialogQBRight);
        final View rl = dialog.findViewById(R.id.gameDialogRushLeft);
        final View rc = dialog.findViewById(R.id.gameDialogRushCenter);
        final View rr = dialog.findViewById(R.id.gameDialogRushRight);
        final View wl = dialog.findViewById(R.id.gameDialogRecLeft);
        final View wc = dialog.findViewById(R.id.gameDialogRecCenter);
        final View wr = dialog.findViewById(R.id.gameDialogRecRight);
        final View dl = dialog.findViewById(R.id.gameDialogDefLeft);
        final View dc = dialog.findViewById(R.id.gameDialogDefCenter);
        final View dr = dialog.findViewById(R.id.gameDialogDefRight);
        final View kl = dialog.findViewById(R.id.gameDialogKickLeft);
        final View kc = dialog.findViewById(R.id.gameDialogKickCenter);
        final View kr = dialog.findViewById(R.id.gameDialogKickRight);
        final View bottom = dialog.findViewById(R.id.gameDialogBottom);

        ((TextView)ql).setText(PlatformUiHelper.valueAt(gameStr, 3));
        ((TextView)qc).setText(PlatformUiHelper.valueAt(gameStr, 4));
        ((TextView)qr).setText(PlatformUiHelper.valueAt(gameStr, 5));
        ((TextView)rl).setText(PlatformUiHelper.valueAt(gameStr, 6));
        ((TextView)rc).setText(PlatformUiHelper.valueAt(gameStr, 7));
        ((TextView)rr).setText(PlatformUiHelper.valueAt(gameStr, 8));
        ((TextView)wl).setText(PlatformUiHelper.valueAt(gameStr, 9));
        ((TextView)wc).setText(PlatformUiHelper.valueAt(gameStr, 10));
        ((TextView)wr).setText(PlatformUiHelper.valueAt(gameStr, 11));
        ((TextView)dl).setText(PlatformUiHelper.valueAt(gameStr, 12));
        ((TextView)dc).setText(PlatformUiHelper.valueAt(gameStr, 13));
        ((TextView)dr).setText(PlatformUiHelper.valueAt(gameStr, 14));
        ((TextView)kl).setText(PlatformUiHelper.valueAt(gameStr, 15));
        ((TextView)kc).setText(PlatformUiHelper.valueAt(gameStr, 16));
        ((TextView)kr).setText(PlatformUiHelper.valueAt(gameStr, 17));
        ((TextView)bottom).setText(PlatformUiHelper.valueAt(gameStr, 18) + "\n\n");
        // Engine columns are line-aligned (labels | away | home); lay them out row by row.
        final View qTable = alignColumns((TextView) ql, (TextView) qc, (TextView) qr);
        final View rTable = alignColumns((TextView) rl, (TextView) rc, (TextView) rr);
        final View wTable = alignColumns((TextView) wl, (TextView) wc, (TextView) wr);
        final View dTable = alignColumns((TextView) dl, (TextView) dc, (TextView) dr);
        final View kTable = alignColumns((TextView) kl, (TextView) kc, (TextView) kr);

        String[] selection = activity.getResources().getStringArray(R.array.boxscore_views);
        Spinner potySpinner = dialog.findViewById(R.id.boxscoreMenu);
        PlatformUiHelper.avoidSpinnerDropdownFocus(potySpinner);
        ArrayAdapter<String> boxMenu = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, selection);
        boxMenu.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        potySpinner.setAdapter(boxMenu);

        potySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                qTable.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                rTable.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                wTable.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                dTable.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
                kTable.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                bottom.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void showScout(final MainActivity activity, final Game g, final Team userTeam) {
        final String[] gameStr = g.getGameScoutStr();

        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle(g.awayTeam.getName() + " @ " + g.homeTeam.getName() + ": " + g.gameName)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.game_scout_dialog, null, false));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        ((TextView)dialog.findViewById(R.id.gameScoutDialogLeft)).setText(PlatformUiHelper.valueAt(gameStr, 0));
        ((TextView)dialog.findViewById(R.id.gameScoutDialogCenter)).setText(PlatformUiHelper.valueAt(gameStr, 1));
        ((TextView)dialog.findViewById(R.id.gameScoutDialogRight)).setText(PlatformUiHelper.valueAt(gameStr, 2));
        alignColumns(dialog.findViewById(R.id.gameScoutDialogLeft),
                dialog.findViewById(R.id.gameScoutDialogCenter), dialog.findViewById(R.id.gameScoutDialogRight));
        ((TextView)dialog.findViewById(R.id.gameScoutDialogBottom)).setText(PlatformUiHelper.valueAt(gameStr, 3));

        if (g.awayTeam == userTeam || g.homeTeam == userTeam) {
            ((TextView)dialog.findViewById(R.id.textScoutOffenseStrategy)).setText(
                    activity.getString(R.string.scout_offense_scheme_format, userTeam.getAbbr()));
            ((TextView)dialog.findViewById(R.id.textScoutDefenseStrategy)).setText(
                    activity.getString(R.string.scout_defense_scheme_format, userTeam.getAbbr()));

            final PlaybookOffense[] tsOff = userTeam.getPlaybookOff();
            final PlaybookDefense[] tsDef = userTeam.getPlaybookDef();
            int offStratNum = userTeam.getPlaybookOffNum(), defStratNum = userTeam.getPlaybookDefNum();

            String[] stratOffSelection = new String[tsOff.length];
            for (int i = 0; i < tsOff.length; ++i) {
                stratOffSelection[i] = tsOff[i].getStratName();
            }

            String[] stratDefSelection = new String[tsDef.length];
            for (int i = 0; i < tsDef.length; ++i) {
                stratDefSelection[i] = tsDef[i].getStratName();
            }

            Spinner offSpinner = dialog.findViewById(R.id.spinnerScoutOffenseStrategy);
            PlatformUiHelper.avoidSpinnerDropdownFocus(offSpinner);
            ArrayAdapter<String> offAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, stratOffSelection);
            offAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            offSpinner.setAdapter(offAdapter);
            offSpinner.setSelection(offStratNum);
            offSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    userTeam.setPlaybookOffNum(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Spinner defSpinner = dialog.findViewById(R.id.spinnerScoutDefenseStrategy);
            PlatformUiHelper.avoidSpinnerDropdownFocus(defSpinner);
            ArrayAdapter<String> defAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, stratDefSelection);
            defAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            defSpinner.setAdapter(defAdapter);
            defSpinner.setSelection(defStratNum);
            defSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    userTeam.setPlaybookDefNum(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        } else {
            dialog.findViewById(R.id.spinnerScoutOffenseStrategy).setVisibility(View.GONE);
            dialog.findViewById(R.id.spinnerScoutDefenseStrategy).setVisibility(View.GONE);
            dialog.findViewById(R.id.textScoutOffenseStrategy).setVisibility(View.GONE);
            dialog.findViewById(R.id.textScoutDefenseStrategy).setVisibility(View.GONE);
        }
    }

    /**
     * The engine sends a box-score section as three newline-aligned columns: labels
     * (shown above the values in the old layout, so nothing lined up), away values and
     * home values. Rebuild it as one row per line, "away | label | home", in place of the
     * label view, and hide the original views. Returns the table so callers can toggle it.
     */
    static View alignColumns(TextView labels, TextView away, TextView home) {
        android.content.Context ctx = labels.getContext();
        String[] l = labels.getText().toString().split("\n", -1);
        String[] a = away.getText().toString().split("\n", -1);
        String[] h = home.getText().toString().split("\n", -1);
        int n = Math.max(l.length, Math.max(a.length, h.length));
        while (n > 0 && cell(l, n - 1).isEmpty() && cell(a, n - 1).isEmpty() && cell(h, n - 1).isEmpty()) n--;

        LinearLayout table = new LinearLayout(ctx);
        table.setOrientation(LinearLayout.VERTICAL);
        int pad = ctx.getResources().getDimensionPixelSize(R.dimen.cf_space_xs);
        for (int i = 0; i < n; i++) {
            String label = cell(l, i);
            String av = cell(a, i);
            String hv = cell(h, i);
            boolean section = label.startsWith("[") && label.endsWith("]");
            if (section) label = label.substring(1, label.length() - 1);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, pad / 2, 0, pad / 2);
            if (label.isEmpty() && av.isEmpty() && hv.isEmpty()) {
                row.setMinimumHeight(pad * 2); // blank line in the engine text = group gap
            }
            boolean header = label.isEmpty() && !(av.isEmpty() && hv.isEmpty());
            row.addView(column(ctx, av, Gravity.START, header ? R.color.cf_text_primary : R.color.cf_text_secondary,
                    header, 1f));
            TextView mid = column(ctx, label, Gravity.CENTER_HORIZONTAL,
                    section ? R.color.cf_gold : R.color.cf_text_muted, section, 0.9f);
            row.addView(mid);
            row.addView(column(ctx, hv, Gravity.END, header ? R.color.cf_text_primary : R.color.cf_text_secondary,
                    header, 1f));
            table.addView(row);
        }

        ViewGroup parent = (ViewGroup) labels.getParent();
        int index = parent.indexOfChild(labels);
        table.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(table, index);
        table.setVisibility(labels.getVisibility());
        labels.setVisibility(View.GONE);
        View valuesRow = (View) away.getParent();
        if (valuesRow != parent) {
            valuesRow.setVisibility(View.GONE);
        } else {
            away.setVisibility(View.GONE);
            home.setVisibility(View.GONE);
        }
        return table;
    }

    private static String cell(String[] col, int i) {
        return i < col.length ? col[i].trim() : "";
    }

    private static TextView column(android.content.Context ctx, String text, int gravity, int color,
                                   boolean bold, float weight) {
        TextView tv = new TextView(ctx);
        TextViewCompat.setTextAppearance(tv, R.style.TextAppearance_Cfhc_Cf_Stat);
        tv.setTextColor(ContextCompat.getColor(ctx, color));
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(gravity);
        tv.setText(text);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    private static int scoreAt(int[] scores, int index) {
        return scores != null && index < scores.length ? scores[index] : 0;
    }

}
