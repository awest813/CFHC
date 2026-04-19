package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import simulation.Game;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;

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

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(g.awayTeam.getName() + " @ " + g.homeTeam.getName() + ": " + g.gameName)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.game_dialog, null));
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
        awayQT1.setText(Integer.toString(g.awayQScore[0]));
        awayQT2.setText(Integer.toString(g.awayQScore[1]));
        awayQT3.setText(Integer.toString(g.awayQScore[2]));
        awayQT4.setText(Integer.toString(g.awayQScore[3]));

        homeTeam.setText(g.homeTeam.getAbbr());
        homeQT1.setText(Integer.toString(g.homeQScore[0]));
        homeQT2.setText(Integer.toString(g.homeQScore[1]));
        homeQT3.setText(Integer.toString(g.homeQScore[2]));
        homeQT4.setText(Integer.toString(g.homeQScore[3]));

        if (g.numOT > 0) {
            int awayOTscore = g.awayScore - (g.awayQScore[0] + g.awayQScore[1] + g.awayQScore[2] + g.awayQScore[3]);
            int homeOTscore = g.homeScore - (g.homeQScore[0] + g.homeQScore[1] + g.homeQScore[2] + g.homeQScore[3]);
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
        gameL.setText(gameStr[0]);
        final TextView gameC = dialog.findViewById(R.id.gameDialogCenter);
        gameC.setText(gameStr[1]);
        final TextView gameR = dialog.findViewById(R.id.gameDialogRight);
        gameR.setText(gameStr[2]);

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

        ((TextView)ql).setText(gameStr[3]);
        ((TextView)qc).setText(gameStr[4]);
        ((TextView)qr).setText(gameStr[5]);
        ((TextView)rl).setText(gameStr[6]);
        ((TextView)rc).setText(gameStr[7]);
        ((TextView)rr).setText(gameStr[8]);
        ((TextView)wl).setText(gameStr[9]);
        ((TextView)wc).setText(gameStr[10]);
        ((TextView)wr).setText(gameStr[11]);
        ((TextView)dl).setText(gameStr[12]);
        ((TextView)dc).setText(gameStr[13]);
        ((TextView)dr).setText(gameStr[14]);
        ((TextView)kl).setText(gameStr[15]);
        ((TextView)kc).setText(gameStr[16]);
        ((TextView)kr).setText(gameStr[17]);
        ((TextView)bottom).setText(gameStr[18] + "\n\n");

        String[] selection = {"menu >> Game Summary", "menu >> Offense Stats", "menu >> Defense Stats", "menu >> Special Teams Stats", "menu >> Game Play Log"};
        Spinner potySpinner = dialog.findViewById(R.id.boxscoreMenu);
        PlatformUiHelper.avoidSpinnerDropdownFocus(potySpinner);
        ArrayAdapter<String> boxMenu = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, selection);
        boxMenu.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        potySpinner.setAdapter(boxMenu);

        potySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ql.setVisibility(View.GONE); qc.setVisibility(View.GONE); qr.setVisibility(View.GONE);
                rl.setVisibility(View.GONE); rc.setVisibility(View.GONE); rr.setVisibility(View.GONE);
                wl.setVisibility(View.GONE); wc.setVisibility(View.GONE); wr.setVisibility(View.GONE);
                dl.setVisibility(View.GONE); dc.setVisibility(View.GONE); dr.setVisibility(View.GONE);
                kl.setVisibility(View.GONE); kc.setVisibility(View.GONE); kr.setVisibility(View.GONE);
                bottom.setVisibility(View.GONE);

                if (position == 0) {
                    // Summary only
                } else if (position == 1) {
                    ql.setVisibility(View.VISIBLE); qc.setVisibility(View.VISIBLE); qr.setVisibility(View.VISIBLE);
                    rl.setVisibility(View.VISIBLE); rc.setVisibility(View.VISIBLE); rr.setVisibility(View.VISIBLE);
                    wl.setVisibility(View.VISIBLE); wc.setVisibility(View.VISIBLE); wr.setVisibility(View.VISIBLE);
                } else if (position == 2) {
                    dl.setVisibility(View.VISIBLE); dc.setVisibility(View.VISIBLE); dr.setVisibility(View.VISIBLE);
                } else if (position == 3) {
                    kl.setVisibility(View.VISIBLE); kc.setVisibility(View.VISIBLE); kr.setVisibility(View.VISIBLE);
                } else if (position == 4) {
                    bottom.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private static void showScout(final MainActivity activity, final Game g, final Team userTeam) {
        final String[] gameStr = g.getGameScoutStr();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(g.awayTeam.getName() + " @ " + g.homeTeam.getName() + ": " + g.gameName)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.game_scout_dialog, null));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        ((TextView)dialog.findViewById(R.id.gameScoutDialogLeft)).setText(gameStr[0]);
        ((TextView)dialog.findViewById(R.id.gameScoutDialogCenter)).setText(gameStr[1]);
        ((TextView)dialog.findViewById(R.id.gameScoutDialogRight)).setText(gameStr[2]);
        ((TextView)dialog.findViewById(R.id.gameScoutDialogBottom)).setText(gameStr[3]);

        if (g.awayTeam == userTeam || g.homeTeam == userTeam) {
            ((TextView)dialog.findViewById(R.id.textScoutOffenseStrategy)).setText(userTeam.getAbbr() + " Off Strategy:");
            ((TextView)dialog.findViewById(R.id.textScoutDefenseStrategy)).setText(userTeam.getAbbr() + " Def Strategy:");

            final PlaybookOffense[] tsOff = userTeam.getPlaybookOff();
            final PlaybookDefense[] tsDef = userTeam.getPlaybookDef();
            int offStratNum = 0, defStratNum = 0;

            String[] stratOffSelection = new String[tsOff.length];
            for (int i = 0; i < tsOff.length; ++i) {
                stratOffSelection[i] = tsOff[i].getStratName();
                if (stratOffSelection[i].equals(userTeam.playbookOff.getStratName())) offStratNum = i;
            }

            String[] stratDefSelection = new String[tsDef.length];
            for (int i = 0; i < tsDef.length; ++i) {
                stratDefSelection[i] = tsDef[i].getStratName();
                if (stratDefSelection[i].equals(userTeam.playbookDef.getStratName())) defStratNum = i;
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
                    userTeam.playbookOff = tsOff[position];
                    userTeam.playbookOffNum = position;
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
                    userTeam.playbookDef = tsDef[position];
                    userTeam.playbookDefNum = position;
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
}
