package antdroid.cfbcoach.recruiting;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import antdroid.cfbcoach.GameNavigation;
import antdroid.cfbcoach.PlatformUiHelper;
import antdroid.cfbcoach.R;
import recruiting.RecruitingController;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingPresentation;
import recruiting.RecruitingSessionData;
import simulation.GameFlowManager;
import simulation.RosterRules;

public class RecruitingActivity extends AppCompatActivity {

    private int theme;
    private RecruitingController controller;
    private GameFlowManager flowManager;
    private RecruitingSessionData sessionData;
    // Variables use during recruiting
    private boolean showPopUp;
    private boolean autoFilter;

    // Keep track of which position is selected in spinner
    private String currentPosition;

    // Android Components to keep track of
    private TextView budgetText;
    private Spinner positionSpinner;
    private ExpandableListView recruitList;
    private ArrayList<String> positions;
    private ArrayAdapter<String> dataAdapterPosition;
    private ExpandableListAdapterRecruiting expListAdapter;
    public Map<String, List<String>> playersInfo;
    public List<RecruitingPlayerRecord> players;


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

        // Get User Team's player info and team info for recruiting
        String userTeamStr = GameNavigation.getUserTeamInfo(getIntent());
        sessionData = RecruitingSessionData.fromUserTeamInfo(userTeamStr);
        flowManager = new antdroid.cfbcoach.AndroidGameFlowManager(this, theme);
        controller = new RecruitingController(sessionData, flowManager);

        getSupportActionBar().setTitle(sessionData.teamName + " | Recruiting");

        showPopUp = true;
        autoFilter = true;
        sessionData.applyBudgetBonuses(RosterRules.MIN_PLAYERS);

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
        PlatformUiHelper.avoidSpinnerDropdownFocus(positionSpinner);
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
                RecruitingDialogController.showRosterDialog(
                        RecruitingActivity.this,
                        sessionData.teamName,
                        RecruitingPresentation.buildRosterText(sessionData, buildPositionNeeds()),
                        sessionData.teamPlayers.size() + sessionData.playersRecruited.size()
                );
            }
        });

        /*
          Set up expandable list view
         */
        recruitList = findViewById(R.id.recruitExpandList);
        currentPosition = positions.isEmpty() ? "Top 50 Recruits" : positions.get(0);
        setPlayerList(0, currentPosition);
        expListAdapter = new ExpandableListAdapterRecruiting(this);
        recruitList.setAdapter(expListAdapter);

        /*
          Set up "Expand All / Collapse All" button
         */
        final Button buttonExpandAll = findViewById(R.id.buttonRecruitExpandCollapse);
        buttonExpandAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RecruitingDialogController.showDisplayOptions(RecruitingActivity.this, autoFilter, players, recruitList, expListAdapter);
            }
        });

    }

    private void updateRecruitingOverview() {
        TextView programName = findViewById(R.id.textRecruitProgramName);
        if (programName != null) {
            programName.setText(sessionData.teamName + " Recruiting");
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



    //FILTERS & SORTINGS


    //UPDATE DATA AFTER CHOOSING FILTER
    private void updateForNewPosition(int position) {
        setPlayerList(position, currentPosition);
        expListAdapter.notifyDataSetChanged();
    }

    private void setPlayerList(int position, String positionLabel) {
        players = new ArrayList<>(controller.getPlayersForPosition(position, positionLabel));
        playersInfo = controller.buildPlayersInfoMap(players, position, positionLabel);
    }

    //Update Position Spinner & Team Needs
    private void updatePositionNeeds() {
        RecruitingSessionData.PositionNeeds needs = buildPositionNeeds();

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
    public void sortByGrade() {
        controller.sortByGrade();
        refreshCurrentBoard();
    }

    //SORT - COST
    public void sortByCost() {
        controller.sortByCost();
        refreshCurrentBoard();
    }

    public void toggleAutoFilter() {
        autoFilter = !autoFilter;
        final Switch filterSwitch = findViewById(R.id.filterSwitch);
        if (filterSwitch != null) filterSwitch.setChecked(autoFilter);
    }




    public void recruitPlayer(RecruitingPlayerRecord recruit) {
        controller.recruitPlayer(recruit, autoFilter);

        Toast.makeText(this, "Recruited " + recruit.position() + " " + recruit.name(),
                Toast.LENGTH_SHORT).show();

        updatePositionNeeds();
        updateBudgetText();
        refreshCurrentBoard();
    }

    private boolean scoutPlayer(RecruitingPlayerRecord recruit) {
        if (controller.scoutPlayer(recruit)) {
            Toast.makeText(this, "Scouted " + recruit.position() + " " + recruit.name(), Toast.LENGTH_SHORT).show();

            expListAdapter.notifyDataSetChanged();
            updateRecruitingOverview();
            updateBudgetText();
            refreshCurrentBoard();
            return true;
        } else {
            Toast.makeText(this, "Not enough budget to scout this player.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void updateBudgetText() {
        if (budgetText != null) {
            budgetText.setText("Budget: $" + sessionData.recruitingBudget);
        }
        updateRecruitingOverview();
    }

    private void refreshCurrentBoard() {
        int position = positionSpinner != null ? positionSpinner.getSelectedItemPosition() : 0;
        if (position < 0) {
            position = 0;
        }
        String label = currentPosition;
        if (positionSpinner != null && positionSpinner.getSelectedItem() != null) {
            label = positionSpinner.getSelectedItem().toString();
        } else if (positions != null && position < positions.size()) {
            label = positions.get(position);
        }
        if (label == null || label.isEmpty()) {
            label = "Top 50 Recruits";
        }
        currentPosition = label;
        setPlayerList(position, label);
        if (expListAdapter != null) {
            expListAdapter.notifyDataSetChanged();
        }
    }

    public int getRecruitCost(RecruitingPlayerRecord p) {
        return controller.getSessionData().getRecruitCost(p);
    }


    /**
     * Exit the recruiting activity. Called when the "Done" button is pressed or when user presses back button.
     */
    private void exitRecruiting() {
        RecruitingDialogController.showExitConfirmDialog(this, positions, controller);
    }

    private RecruitingSessionData.PositionNeeds buildPositionNeeds() {
        return sessionData.calculateNeeds(RosterRules.MIN_QBS, RosterRules.MIN_RBS, RosterRules.MIN_WRS, RosterRules.MIN_TES, RosterRules.MIN_OLS, RosterRules.MIN_KS, RosterRules.MIN_DLS, RosterRules.MIN_LBS, RosterRules.MIN_CBS, RosterRules.MIN_SS);
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        exitRecruiting();
    }

    public void setShowPopUp(boolean tf) {
        showPopUp = tf;
    }


    //MAIN UI FOR PLAYER DATA IN RECRUITING SCREEN
    public class ExpandableListAdapterRecruiting extends BaseExpandableListAdapter {

        private final Activity context;

        ExpandableListAdapterRecruiting(Activity context) {
            this.context = context;
        }

        public String getChild(int groupPosition, int childPosition) {
            RecruitingPlayerRecord recruit = players.get(groupPosition);
            return playersInfo.get(recruit.listKey()).get(childPosition);
        }


        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }


        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final String playerDetail = getChild(groupPosition, childPosition);
            final RecruitingPlayerRecord recruit = getGroup(groupPosition);
            LayoutInflater inflater = context.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.child_recruit, null);
            }

            // Set up Text for player details
            final TextView details = convertView.findViewById(R.id.textRecruitDetails);
            final TextView potential = convertView.findViewById(R.id.textRecruitPotential);

            details.setText(playerDetail);
            potential.setText(RecruitingPresentation.buildPotentialDetails(recruit));

            Button scoutPlayerButton = convertView.findViewById(R.id.buttonScoutPlayer);
            scoutPlayerButton.setText("Scout");
            scoutPlayerButton.setEnabled(!recruit.raw().endsWith(",T"));
            scoutPlayerButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    scoutPlayer(recruit);
                }
            });

            Button recruitPlayerButton = convertView.findViewById(R.id.buttonRecruitPlayer);

            if (sessionData.teamPlayers.size() + sessionData.playersRecruited.size() < RosterRules.MAX_PLAYERS) {
                recruitPlayerButton.setText("Recruit: $" + recruit.cost());
            } else {
                recruitPlayerButton.setText("ROSTER FULL");
                //recruitPlayerButton.setVisibility(View.INVISIBLE);
            }

            // Set up button for recruiting player
            recruitPlayerButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Save who is currently expanded
                    if (sessionData.teamPlayers.size() + sessionData.playersRecruited.size() < RosterRules.MAX_PLAYERS) {
                        RecruitingDialogController.showRecruitConfirmDialog(
                                RecruitingActivity.this,
                                sessionData,
                                recruit,
                                groupPosition,
                                players.size(),
                                recruitList,
                                expListAdapter,
                                showPopUp
                        );
                    }
                }
            });

            return convertView;
        }


        public int getChildrenCount(int groupPosition) {
            RecruitingPlayerRecord recruit = players.get(groupPosition);
            return playersInfo.get(recruit.listKey()).size();
        }


        public RecruitingPlayerRecord getGroup(int groupPosition) {
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
