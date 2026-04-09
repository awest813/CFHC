package recruiting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import antdroid.cfbcoach.GameNavigation;
import antdroid.cfbcoach.LeagueLaunchCoordinator;
import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;
import simulation.RosterRules;

public class RecruitingActivity extends AppCompatActivity {

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private final DecimalFormat df2 = new DecimalFormat("#.##", symbols);
    private int theme;
    private RecruitingSessionData sessionData;
    // Variables use during recruiting
    private String teamName;
    public int recruitingBudget;
    private final Random rand = new Random();
    private int HCtalent;

    public ArrayList<String> playersRecruited;
    private ArrayList<String> playersGraduating;
    private ArrayList<String> teamQBs;
    private ArrayList<String> teamRBs;
    private ArrayList<String> teamWRs;
    private ArrayList<String> teamTEs;
    private ArrayList<String> teamOLs;
    private ArrayList<String> teamKs;
    private ArrayList<String> teamDLs;
    private ArrayList<String> teamLBs;
    private ArrayList<String> teamCBs;
    private ArrayList<String> teamSs;

    public ArrayList<String> teamPlayers; //all players

    private ArrayList<String> availQBs;
    private ArrayList<String> availRBs;
    private ArrayList<String> availWRs;
    private ArrayList<String> availTEs;
    private ArrayList<String> availOLs;
    private ArrayList<String> availKs;
    private ArrayList<String> availDLs;
    private ArrayList<String> availLBs;
    private ArrayList<String> availCBs;
    private ArrayList<String> availSs;
    private ArrayList<String> availAll;
    private ArrayList<String> avail50;

    private ArrayList<String> west;
    private ArrayList<String> midwest;
    private ArrayList<String> central;
    private ArrayList<String> east;
    private ArrayList<String> south;

    private int needQBs;
    private int needRBs;
    private int needWRs;
    private int needTEs;
    private int needOLs;
    private int needKs;
    private int needDLs;
    private int needLBs;
    private int needCBs;
    private int needSs;

    private final int minPlayers = RosterRules.MIN_PLAYERS;
    private final int minQBs = RosterRules.MIN_QBS;
    private final int minRBs = RosterRules.MIN_RBS;
    private final int minWRs = RosterRules.MIN_WRS;
    private final int minTEs = RosterRules.MIN_TES;
    private final int minOLs = RosterRules.MIN_OLS;
    private final int minKs = RosterRules.MIN_KS;
    private final int minDLs = RosterRules.MIN_DLS;
    private final int minLBs = RosterRules.MIN_LBS;
    private final int minCBs = RosterRules.MIN_CBS;
    private final int minSs = RosterRules.MIN_SS;

    public final int maxPlayers = RosterRules.MAX_PLAYERS;
    private final double recruitOffBoard = 0.935;

    private final int five = 84;
    private final int four = 78;
    private final int three = 68;
    private final int two = 58;

    int height;
    int weight;

    // Whether to show pop ups every recruit
    private boolean showPopUp;
    private boolean autoFilter;

    // Keep track of which position is selected in spinner
    private String currentPosition;

    // Android Components to keep track of
    private TextView budgetText;
    private Spinner positionSpinner;
    private ExpandableListView recruitList;
    private ArrayList<String> positions;
    private ArrayAdapter dataAdapterPosition;
    private ExpandableListAdapterRecruiting expListAdapter;
    public Map<String, List<String>> playersInfo;
    public List<String> players;

    public final String[] states = {"AS","AZ","CA","HI","ID","MT","NV","OR","UT","WA","CO","KS","MO","NE","NM","ND","OK","SD","TX","WY","IL","IN","IA","KY","MD","MI","MN","OH","TN","WI","CT","DE","ME","MA","NH","NJ","NY","PA","RI","VT","AL","AK","FL","GA","LA","MS","NC","SC","VA","WV"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = GameNavigation.getTheme(getIntent(), theme);
        if(theme == 1) setTheme(R.style.AppThemeLight);
        else setTheme(R.style.AppTheme);

        setContentView(R.layout.activity_recruiting);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Init all the ArrayLists
        playersRecruited = new ArrayList<>();
        playersGraduating = new ArrayList<>();
        teamQBs = new ArrayList<>();
        teamRBs = new ArrayList<>();
        teamWRs = new ArrayList<>();
        teamTEs = new ArrayList<>();
        teamOLs = new ArrayList<>();
        teamKs = new ArrayList<>();
        teamDLs = new ArrayList<>();
        teamLBs = new ArrayList<>();
        teamCBs = new ArrayList<>();
        teamSs = new ArrayList<>();
        teamPlayers = new ArrayList<>();
        availQBs = new ArrayList<>();
        availRBs = new ArrayList<>();
        availWRs = new ArrayList<>();
        availTEs = new ArrayList<>();
        availOLs = new ArrayList<>();
        availKs = new ArrayList<>();
        availDLs = new ArrayList<>();
        availLBs = new ArrayList<>();
        availCBs = new ArrayList<>();
        availSs = new ArrayList<>();

        avail50 = new ArrayList<>();
        availAll = new ArrayList<>();
        west = new ArrayList<>();
        midwest = new ArrayList<>();
        central = new ArrayList<>();
        east = new ArrayList<>();
        south = new ArrayList<>();

        // Get User Team's player info and team info for recruiting
        String userTeamStr = GameNavigation.getUserTeamInfo(getIntent());
        sessionData = RecruitingSessionData.fromUserTeamInfo(userTeamStr);
        bindSessionData();
        getSupportActionBar().setTitle(teamName + " | Recruiting");

        showPopUp = true;
        autoFilter = true;
        sessionData.applyBudgetBonuses(minPlayers);
        recruitingBudget = sessionData.recruitingBudget;

        // Get needs for each position
        updatePositionNeeds();

        /*
          Assign components to private variables for easier access later
         */
        budgetText = findViewById(R.id.textRecBudget);
        updateRecruitingOverview();

        /*
          Set up spinner for examining choosing position to recruit
         */
        positionSpinner = findViewById(R.id.spinnerRec);
        MainActivity.avoidSpinnerDropdownFocus(positionSpinner);
        positions = sessionData.buildPositionLabels(buildPositionNeeds());

        dataAdapterPosition = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, positions);
        dataAdapterPosition.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(dataAdapterPosition);
        positionSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        currentPosition = parent.getItemAtPosition(position).toString();
                        updateForNewPosition(position);
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        /*
          Set up the "Done" button for returning back to MainActivity
         */
        Button doneRecrutingButton = findViewById(R.id.buttonDoneRecruiting);
        doneRecrutingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                exitRecruiting();
            }
        });

        final Switch filterSwitch = findViewById(R.id.filterSwitch);
        filterSwitch.setText(getString(R.string.recruiting_filter_label));
        filterSwitch.setChecked(autoFilter);
        filterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoFilter = isChecked;
            }
        });

        /*
          Set up the "Roster" button for displaying dialog of all players in roster
         */
        Button viewRosterButton = findViewById(R.id.buttonRecRoster);
        viewRosterButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Make dialog
                makeRosterDialog();
            }
        });

        /*
          Set up expandable list view
         */
        recruitList = findViewById(R.id.recruitExpandList);
        setPlayerList("QB");
        setPlayerInfoMap("QB");
        expListAdapter = new ExpandableListAdapterRecruiting(this);
        recruitList.setAdapter(expListAdapter);

        /*
          Set up "Expand All / Collapse All" button
         */
        final Button buttonExpandAll = findViewById(R.id.buttonRecruitExpandCollapse);
        buttonExpandAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RecruitingActivity.this);
                builder.setTitle("DISPLAY OPTIONS");
                String filter = "Enable Auto-Remove Unaffordable Players";
                if (autoFilter) filter = "Disable Auto-Remove Unaffordable Players";
                final String[] sels = {"Expand All", "Collapse All", "Sort by Grade", "Sort by Cost", filter};
                builder.setItems(sels, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // Do something with the selection
                        if (item == 0) {
                            // Expand everyone
                            for (int i = 0; i < players.size(); ++i) {
                                recruitList.expandGroup(i, false);
                            }
                        } else if (item == 1) {
                            // Collapse everyone
                            for (int i = 0; i < players.size(); ++i) {
                                recruitList.collapseGroup(i);
                            }
                        } else if (item == 2) {
                            sortByGrade();
                            expListAdapter.notifyDataSetChanged();
                        } else if (item == 3) {
                            sortByCost();
                            expListAdapter.notifyDataSetChanged();
                        } else if (item == 4) {
                            autoFilter = !autoFilter;
                            filterSwitch.setChecked(autoFilter);
                        }

                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

    }

    private void updateRecruitingOverview() {
        if (budgetText != null) {
            budgetText.setText("Budget: $" + recruitingBudget);
        }

        TextView programName = findViewById(R.id.textRecruitProgramName);
        if (programName != null) {
            programName.setText(teamName + " Recruiting");
        }

        TextView summaryText = findViewById(R.id.textRecruitSummary);
        if (summaryText != null) {
            summaryText.setText(RecruitingPresentation.buildOverviewSummary(sessionData));
        }

        TextView boardStatus = findViewById(R.id.textRecruitBoardStatus);
        if (boardStatus != null) {
            boardStatus.setText(RecruitingPresentation.buildBoardStatus(sessionData));
        }
    }

    //Create Roster Screen
    private void makeRosterDialog() {
        String rosterStr = RecruitingPresentation.buildRosterText(sessionData, buildPositionNeeds());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(rosterStr)
                .setTitle(teamName + " Roster | Team Size: " + (teamPlayers.size() + playersRecruited.size()))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Dismiss dialog
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    //FILTERS & SORTINGS


    //UPDATE DATA AFTER CHOOSING FILTER
    private void updateForNewPosition(int position) {
        if (position > 1 && position < 12) {
            String[] splitty = currentPosition.split(" ");
            setPlayerList(splitty[0]);
            setPlayerInfoMap(splitty[0]);
            expListAdapter.notifyDataSetChanged();
        } else {
            // See top 100 recruits
            if (position == 0) {
                players = avail50;
            } else if (position == 12) {
                players = west;
            } else if (position == 13) {
                players = midwest;
            } else if (position == 14) {
                players = central;
            } else if (position == 15) {
                players = east;
            } else if (position == 16) {
                players = south;
            } else {
                players = availAll;
            }

            playersInfo = new LinkedHashMap<>();
            for (String p : players) {
                ArrayList<String> pInfoList = new ArrayList<>();
                RecruitingPlayerRecord record = RecruitingPlayerRecord.fromCsv(p);
                pInfoList.add(RecruitingPresentation.buildRecruitBoardDetails(record, record.position()));
                playersInfo.put(record.listKey(), pInfoList);
            }
            expListAdapter.notifyDataSetChanged();
        }
    }

    //Get Players
    private void setPlayerList(String pos) {
        if (pos.equals("QB")) {
            players = availQBs;
        } else if (pos.equals("RB")) {
            players = availRBs;
        } else if (pos.equals("WR")) {
            players = availWRs;
        } else if (pos.equals("TE")) {
            players = availTEs;
        } else if (pos.equals("OL")) {
            players = availOLs;
        } else if (pos.equals("K")) {
            players = availKs;
        } else if (pos.equals("DL")) {
            players = availDLs;
        } else if (pos.equals("LB")) {
            players = availLBs;
        } else if (pos.equals("CB")) {
            players = availCBs;
        } else if (pos.equals("S")) {
            players = availSs;
        }
    }

    //Player General Display
    private void setPlayerInfoMap(String pos) {
        playersInfo = new LinkedHashMap<>();
        for (String p : players) {
            ArrayList<String> pInfoList = new ArrayList<>();
            RecruitingPlayerRecord record = RecruitingPlayerRecord.fromCsv(p);
            pInfoList.add(RecruitingPresentation.buildRecruitBoardDetails(record, pos));
            playersInfo.put(record.listKey(), pInfoList);
        }
    }

    //FILTER OUT UNAFFORDABLE PLAYERS
    private void removeUnaffordableRecruits() {
        sessionData.removeUnaffordableRecruits();
        dataAdapterPosition.notifyDataSetChanged();
    }

    private void removeUnaffordable(List<String> list) {
        sessionData.removeUnaffordableRecruits();
    }

    //REMOVE RECRUITS OFF THE BOARD
    private void removeRecruits() {
        sessionData.removeRandomRecruits(recruitOffBoard, rand);
        dataAdapterPosition.notifyDataSetChanged();
    }

    private void removeRecruits(List<String> list) {
        sessionData.removeRandomRecruits(recruitOffBoard, rand);
    }

    private void removeRecruitBoard(List<String> list) {
        dataAdapterPosition.notifyDataSetChanged();
    }


    //Update Position Spinner & Team Needs
    private void updatePositionNeeds() {
        RecruitingSessionData.PositionNeeds needs = buildPositionNeeds();
        needQBs = needs.qbs;
        needRBs = needs.rbs;
        needWRs = needs.wrs;
        needTEs = needs.tes;
        needOLs = needs.ols;
        needKs = needs.ks;
        needDLs = needs.dls;
        needLBs = needs.lbs;
        needCBs = needs.cbs;
        needSs = needs.ss;

        if (dataAdapterPosition != null) {
            positions = sessionData.buildPositionLabels(needs);

            dataAdapterPosition.clear();
            for (String p : positions) {
                dataAdapterPosition.add(p);
            }
            dataAdapterPosition.notifyDataSetChanged();
        }
        updateRecruitingOverview();
    }


    //SORT - GRADE(Default)
    private void sortByGrade() {
        sessionData.sortBoardsByGrade();
    }

    //SORT - COST
    private void sortByCost() {
        sessionData.sortBoardsByCost();
    }


    //RECRUIT PLAYER
    public void recruitPlayerDialog(String p, int pos, List<Integer> groupsExp) {
        final String player = p;
        final int groupPosition = pos;
        final List<Integer> groupsExpanded = groupsExp;
        int moneyNeeded = getRecruitCost(player);
        if (recruitingBudget >= moneyNeeded) {

            if (showPopUp) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Confirm Recruiting");
                builder.setMessage(RecruitingPresentation.buildRecruitConfirmMessage(sessionData, maxPlayers, player));
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                recruitList.collapseGroup(groupPosition);
                                for (int i = groupPosition + 1; i < players.size(); ++i) {
                                    if (recruitList.isGroupExpanded(i)) {
                                        groupsExpanded.add(i);
                                    }
                                    recruitList.collapseGroup(i);
                                }

                                recruitPlayer(player);

                                expListAdapter.notifyDataSetChanged();

                                dialog.dismiss();
                            }
                        });

                builder.setNeutralButton("Yes, Don't Show",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                recruitList.collapseGroup(groupPosition);
                                for (int i = groupPosition + 1; i < players.size(); ++i) {
                                    if (recruitList.isGroupExpanded(i)) {
                                        groupsExpanded.add(i);
                                    }
                                    recruitList.collapseGroup(i);
                                }

                                recruitPlayer(player);
                                setShowPopUp(false);

                                expListAdapter.notifyDataSetChanged();

                                dialog.dismiss();
                            }
                        });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // not successful
                                recruitList.collapseGroup(groupPosition);
                                for (int i = groupPosition + 1; i < players.size(); ++i) {
                                    if (recruitList.isGroupExpanded(i)) {
                                        groupsExpanded.add(i);
                                    }
                                    recruitList.collapseGroup(i);
                                }

                                recruitList.expandGroup(groupPosition);
                                expListAdapter.notifyDataSetChanged();
/*                                for (int group : groupsExpanded) {
                                    recruitList.expandGroup(group);
                                }*/
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();

                TextView msgTxt = dialog.findViewById(android.R.id.message);
                msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            } else {
                // Don't show pop up dialog
                recruitList.collapseGroup(groupPosition);
                for (int i = groupPosition + 1; i < players.size(); ++i) {
                    if (recruitList.isGroupExpanded(i)) {
                        groupsExpanded.add(i);
                    }
                    recruitList.collapseGroup(i);
                }

                recruitPlayer(player);

                expListAdapter.notifyDataSetChanged();
/*                for (int group : groupsExpanded) {
                    recruitList.expandGroup(group - 1);
                }*/
            }

        } else {
            recruitList.collapseGroup(groupPosition);
            for (int i = groupPosition + 1; i < players.size(); ++i) {
                if (recruitList.isGroupExpanded(i)) {
                    groupsExpanded.add(i);
                }
                recruitList.collapseGroup(i);
            }
            Toast.makeText(this, "Not enough money!",
                    Toast.LENGTH_SHORT).show();
            recruitList.expandGroup(groupPosition);
            expListAdapter.notifyDataSetChanged();
/*            for (int group : groupsExpanded) {
                recruitList.expandGroup(group);
            }*/
        }
    }

    private void recruitPlayer(String player) {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(player);
        sessionData.recruitPlayer(player, autoFilter, recruitOffBoard, rand);
        recruitingBudget = sessionData.recruitingBudget;

        Toast.makeText(this, "Recruited " + recruit.position() + " " + recruit.name(),
                Toast.LENGTH_SHORT).show();

        updatePositionNeeds();
    }
    //SCOUT PLAYER - created by Achi Jones - never used
    private boolean scoutPlayer(String player) {
        if (sessionData.scoutPlayer(player)) {
            recruitingBudget = sessionData.recruitingBudget;
            RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(player);
            Toast.makeText(this, "Scouted " + recruit.position() + " " + recruit.name(), Toast.LENGTH_SHORT).show();

            expListAdapter.notifyDataSetChanged();
            updateRecruitingOverview();

            return true;

        } else {
            Toast.makeText(this, "Not enough money!",
                    Toast.LENGTH_SHORT).show();

            return false;
        }
    }

    private void sortTeam() {
        sessionData.sortTeamByOverall();
    }

    //PLAYER DISPLAY INFO
    private String getReadablePlayerInfo(String p) {
        return sessionData.getReadablePlayerInfo(p);
    }

    /**
     * Converts player string into '$500 QB A. Name, Overall: 89' or similar
     */
    /**
     * Used for parsing through string to get cost
     */
    public int getRecruitCost(String p) {
        return sessionData.getRecruitCost(p);
    }

    /**
     * Exit the recruiting activity. Called when the "Done" button is pressed or when user presses back button.
     */
    private void exitRecruiting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(RecruitingActivity.this);
        builder.setMessage(RecruitingPresentation.buildExitConfirmMessage(positions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Send info about what recruits were selected back
                        RecruitingActivity.this.startActivity(GameNavigation.createMainIntent(
                                RecruitingActivity.this,
                                LeagueLaunchCoordinator.LaunchRequest.doneRecruiting(getRecruitsStr()),
                                theme
                        ));
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Gets all the recruits in a string to send back to MainActivity to be added to user team
     */
    private String getRecruitsStr() {
        return sessionData.buildRecruitsSaveData();
    }

    private RecruitingSessionData.PositionNeeds buildPositionNeeds() {
        return sessionData.calculateNeeds(minQBs, minRBs, minWRs, minTEs, minOLs, minKs, minDLs, minLBs, minCBs, minSs);
    }

    private void bindSessionData() {
        teamName = sessionData.teamName;
        recruitingBudget = sessionData.recruitingBudget;
        HCtalent = sessionData.coachTalent;
        playersRecruited = sessionData.playersRecruited;
        playersGraduating = sessionData.playersGraduating;
        teamPlayers = sessionData.teamPlayers;
        teamQBs = sessionData.teamQBs;
        teamRBs = sessionData.teamRBs;
        teamWRs = sessionData.teamWRs;
        teamTEs = sessionData.teamTEs;
        teamOLs = sessionData.teamOLs;
        teamKs = sessionData.teamKs;
        teamDLs = sessionData.teamDLs;
        teamLBs = sessionData.teamLBs;
        teamCBs = sessionData.teamCBs;
        teamSs = sessionData.teamSs;
        availQBs = sessionData.availQBs;
        availRBs = sessionData.availRBs;
        availWRs = sessionData.availWRs;
        availTEs = sessionData.availTEs;
        availOLs = sessionData.availOLs;
        availKs = sessionData.availKs;
        availDLs = sessionData.availDLs;
        availLBs = sessionData.availLBs;
        availCBs = sessionData.availCBs;
        availSs = sessionData.availSs;
        availAll = sessionData.availAll;
        avail50 = sessionData.avail50;
        west = sessionData.west;
        midwest = sessionData.midwest;
        central = sessionData.central;
        east = sessionData.east;
        south = sessionData.south;
    }

    @Override
    public void onBackPressed() {
        exitRecruiting();
    }


    private void setShowPopUp(boolean tf) {
        showPopUp = tf;
    }


    //MAIN UI FOR PLAYER DATA IN RECRUITING SCREEN
    class ExpandableListAdapterRecruiting extends BaseExpandableListAdapter {

        private final Activity context;

        ExpandableListAdapterRecruiting(Activity context) {
            this.context = context;
        }

        public String getChild(int groupPosition, int childPosition) {
            RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(players.get(groupPosition));
            return playersInfo.get(recruit.listKey()).get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }


        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final String playerDetail = getChild(groupPosition, childPosition);
            final String playerCSV = getGroup(groupPosition);
            LayoutInflater inflater = context.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.child_recruit, null);
            }

            // Set up Text for player details
            final TextView details = convertView.findViewById(R.id.textRecruitDetails);
            final TextView potential = convertView.findViewById(R.id.textRecruitPotential);
            RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCSV);

            details.setText(playerDetail);
            potential.setText(RecruitingPresentation.buildPotentialDetails(recruit));

            // Set up Recruit and Redshirt buttons to display the right price
            Button recruitPlayerButton = convertView.findViewById(R.id.buttonRecruitPlayer);

            if (teamPlayers.size() + playersRecruited.size() < maxPlayers) {
                recruitPlayerButton.setText("Recruit: $" + getRecruitCost(playerCSV));
            } else {
                recruitPlayerButton.setText("ROSTER FULL");
                //recruitPlayerButton.setVisibility(View.INVISIBLE);
            }

            // Set up button for recruiting player
            recruitPlayerButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Save who is currently expanded
                    if (teamPlayers.size() + playersRecruited.size() < maxPlayers) {
                        List<Integer> groupsExpanded = new ArrayList<>();
                        recruitPlayerDialog(playerCSV, groupPosition, groupsExpanded);
                    }
                }
            });

            return convertView;
        }

        public int getChildrenCount(int groupPosition) {
            RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(players.get(groupPosition));
            return playersInfo.get(recruit.listKey()).size();
        }

        public String getGroup(int groupPosition) {
            return players.get(groupPosition);
        }

        public int getGroupCount() {
            return players.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            String playerLeft = RecruitingPresentation.getPlayerListLeftLabel(getGroup(groupPosition));
            String playerRight = RecruitingPresentation.getPlayerListRightLabel(getGroup(groupPosition));
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.group_recruit,
                        null);
            }
            TextView itemL = convertView.findViewById(R.id.textRecruitLeft);
            itemL.setTypeface(null, Typeface.BOLD);
            itemL.setText(playerLeft);
            TextView itemR = convertView.findViewById(R.id.textRecruitRight);
            itemR.setTypeface(null, Typeface.BOLD);
            itemR.setText(playerRight);
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

}
