package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import positions.Player;
import positions.PlayerK;
import positions.PlayerLB;
import positions.PlayerQB;
import positions.PlayerS;
import positions.PlayerTE;
import simulation.Conference;

import simulation.CustomUniverseParser;
import simulation.Game;
import simulation.GameUiBridge;
import simulation.League;
import simulation.LeagueExportController;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueSaveStorage;
import simulation.PlatformResourceProvider;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;
import staff.DC;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;
import ui.CoachDatabase;
import ui.DepthChart;
import ui.GameScheduleList;
import ui.HallofFameList;
import ui.IndividualStats;
import ui.LeagueHistoryList;
import ui.LeagueRecordsList;
import ui.MainRankings;
import ui.MockDraft;
import ui.NewsStories;
import ui.PlayerProfile;
import ui.PlayerRankingsList;
import ui.RedshirtAdapter;
import ui.SaveFilesList;
import ui.SeasonAwardsList;
import ui.TeamHistoryList;
import ui.TeamHome;
import ui.TeamRankingsList;
import ui.TeamRoster;
import ui.TeamStatsList;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, GameUiBridge, LeagueImportFlowController.Host {
    private HeadCoach userHC;
    private int season;
    public League simLeague;
    private simulation.SeasonController seasonController;

    private Conference currentConference;
    private Team currentTeam;
    private Team userTeam;
    private File saveLeagueFile;
    private String username;
    private LeagueImportFlowController.ImportType pendingImportType;
    private String goals;
    private simulation.SaveLoadService saveLoadService;


    private List<String> teamList;
    private List<String> confList;

    public int currPage = 0;
    private String userTeamStr;
    private Spinner examineTeamSpinner;
    private ArrayAdapter<String> dataAdapterTeam;
    private Spinner examineConfSpinner;
    private ArrayAdapter<String> dataAdapterConf;
    private ListView mainList;

    private ArrayList<Team> jobList;
    private int jobType;
    private boolean jobListSet;

    private boolean wantUpdateConf;
    private boolean redshirtComplete;
    private boolean newGame;
    private boolean skipRetirementQ;
    private boolean reincarnate;

    //Universe Settings
    private final int seasonStart = 2026;
    private final int retireAge = 67;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private final DecimalFormat df2 = new DecimalFormat("#.##");

    public int theme;

    private boolean loadedLeague = false;
    private final ActivityResultLauncher<String[]> importDocumentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportDocumentSelection);

    private simulation.GameFlowManager flowManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = GameNavigation.getTheme(getIntent(), 1);
        flowManager = new AndroidGameFlowManager(this, theme);
        saveLoadService = new simulation.SaveLoadService(getFilesDir(), "5.0"); // Fixed version for now, should come from simLeague eventually
        if(theme == 1) setTheme(R.style.AppThemeLight);

        else setTheme(R.style.AppTheme);

        hideSystemUI();


        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer =  findViewById(R.id.drawer_layout);
        final NavigationView navigationView =  findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                View headerView = navigationView.getHeaderView(0);
                TextView navTeam = headerView.findViewById(R.id.navTextTeam);
                navTeam.setText("#" + currentTeam.rankTeamPollScore +
                        " " + currentTeam.name + " (" + currentTeam.wins + "-" + currentTeam.losses + ") " +
                        currentTeam.confChampion + " " + currentTeam.semiFinalWL + currentTeam.natChampWL);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        //Set up list
        mainList = findViewById(R.id.mainList);
        jobList = new ArrayList<>();

        //Load Data
        loadGame();


        wantUpdateConf = true; // 0 and 1, don't update, 2 update

        try {
            if (!loadedLeague) {
                // Set it to 1st team until one selected
                userTeam = simLeague.teamList.get(0);
                simLeague.userTeam = userTeam;
                userTeam.userControlled = true;
                userTeamStr = userTeam.name;
                currentTeam = simLeague.teamList.get(0);
                currentConference = simLeague.conferences.get(0);

                LeagueLaunchCoordinator.LaunchRequest launchRequest = getLaunchRequest();
                if (launchRequest != null && launchRequest.isCustomUniverse()) importDataPrompt();
                else careerModeOptions();
            }

        } catch (Exception ex) {
            System.out.println(
                    "Error reading file");
            ex.printStackTrace();
            crash();
            return;
        }

        // Set toolbar text
        updateHeaderBar();


        //Set up spinner for examining team.
        examineConfSpinner = findViewById(R.id.examineConfSpinner);
        PlatformUiHelper.avoidSpinnerDropdownFocus(examineConfSpinner);

        confList = new ArrayList<>();
        for (int i = 0; i < simLeague.conferences.size(); i++) {
            if(simLeague.conferences.get(i).confTeams.size() > 0) confList.add(simLeague.conferences.get(i).confName);
        }
        dataAdapterConf = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, confList);
        dataAdapterConf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        examineConfSpinner.setAdapter(dataAdapterConf);
        examineConfSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        currentConference = simLeague.findConference(parent.getItemAtPosition(position).toString());
                        updateCurrConference();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        //heh
                    }
                });

        examineTeamSpinner = findViewById(R.id.examineTeamSpinner);
        PlatformUiHelper.avoidSpinnerDropdownFocus(examineTeamSpinner);
        teamList = new ArrayList<>();
        for (int i = 0; i < simLeague.teamList.size(); i++) {
            teamList.add(simLeague.teamList.get(i).strRep());
        }

        dataAdapterTeam = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, teamList);
        dataAdapterTeam.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        examineTeamSpinner.setAdapter(dataAdapterTeam);
        examineTeamSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        currentTeam = simLeague.findTeam(parent.getItemAtPosition(position).toString());
                        updateCurrTeam();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        //Team Depth Chart Button
        Button depthchartButton = findViewById(R.id.buttonDepthChart);
        if (!redshirtComplete) {
            depthchartButton.setText("REDSHIRT");
            depthchartButton.setBackgroundColor(Color.RED);
        }

        depthchartButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                currentTeam = userTeam;
                if (!redshirtComplete) redshirtDialog();
                else depthChartDialog();
            }
        });

        //Strategy/Playbook
        final Button strategyButton = findViewById(R.id.buttonStrategy);
        strategyButton.setBackgroundColor(0XFF607D8B);
        strategyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                currentTeam = userTeam;
                showTeamStrategyDialog();
            }
        });

        //Simulate Week
        final Button simGameButton = findViewById(R.id.simGameButton);
        simGameButton.setText("START SEASON");
        simGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                simulateWeek();
            }
        });


        if (loadedLeague) {
            // Set rankings so that not everyone is rank #0
            simLeague.setTeamRanks();
            examineTeam(userTeam.name);
        }

        if (simLeague.getYear() != seasonStart) {
            // Only show recruiting classes if not season 1
            showRecruitingClassDialog();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            currentTeam = userTeam;
            examineTeam(currentTeam.name);
            showHome();
            currPage = 0;
        } else if (id == R.id.nav_roster) {
            currPage = 1;
            viewRoster();
        } else if (id == R.id.nav_teamplayerstats) {
            currPage = 2;
                showTeamPlayerStats();
        } else if (id == R.id.nav_teamstats) {
            currPage = 3;
            updateTeamStats();
        } else if (id == R.id.nav_schedule) {
            currPage = 4;
            updateSchedule();
        } else if (id == R.id.nav_news) {
            currPage = 5;
            showNewsStoriesDialog();
        } else if (id == R.id.nav_scores) {
            currPage = 5;
            showWeeklyScores();
        } else if (id == R.id.nav_standings) {
            currPage = 5;
            updateStandings();
        } else if (id == R.id.nav_rankings) {
            currPage = 5;
            updateRankings();
        } else if (id == R.id.nav_leagueteamstats) {
            currPage = 5;
            showTeamRankingsDialog();
        } else if (id == R.id.nav_leagueplayerstats) {
            currPage = 5;
            showPlayerRankingsDialog();
        } else if (id == R.id.nav_awards) {
            currPage = 5;
            showLeagueAwards();
        } else if (id == R.id.nav_postseason) {
            currPage = 5;
            showBowlCCGDialog();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openHomeView(View view) {
        currentTeam = userTeam;
        examineTeam(currentTeam.name);
        showHome();
        currPage = 0;
    }

    public void openRosterView(View view) {
        currPage = 1;
        viewRoster();
    }

    public void openTeamStatsView(View view) {
        currPage = 3;
        updateTeamStats();
    }

    public void openScheduleView(View view) {
        currPage = 4;
        updateSchedule();
    }

    public void openLeagueDrawer(View view) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
    }

    private void defaultScreen() {
        showHome();
    }

    private void loadGame() {
        try {
            PlatformResourceProvider resProvider = new AndroidResourceProvider(this);
            LeagueLaunchCoordinator.LaunchRequest launchRequest = getLaunchRequest();
            LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                    launchRequest,
                    getFilesDir(),
                    this,
                    seasonStart,
                    resProvider.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resProvider.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    resProvider.getString(PlatformResourceProvider.KEY_CONFERENCES),
                    resProvider.getString(PlatformResourceProvider.KEY_TEAMS),
                    resProvider.getString(PlatformResourceProvider.KEY_BOWLS),
                    this::customLeague,
                    this::openDocumentStream
            );

            simLeague = result.league;
            simLeague.setPlatformResourceProvider(resProvider);
            season = result.season;
            seasonController = new simulation.SeasonController(simLeague, this, flowManager);

            loadedLeague = result.loadedLeague;
            newGame = result.newGame;

            if (result.userTeam != null) {
                userTeam = result.userTeam;
                userTeamStr = userTeam.name;
            }
            if (result.currentTeam != null) {
                currentTeam = result.currentTeam;
            }
            if (result.showSeasonGoals) {
                seasonGoals();
            }
        } catch (Exception ex) {
            System.out.println("Error reading file");
            ex.printStackTrace();
            crash();
        }
    }

    //Update Header Bar
    private void updateHeaderBar() {
        getSupportActionBar().setTitle(currentTeam.name);
        SeasonPresentationController.update(this, currentTeam, simLeague, season);
    }

    public String getSeasonBadgeText() {
        return SeasonPresentationController.getSeasonBadgeText(season);
    }

    public String getSeasonTitleText() {
        return SeasonPresentationController.getSeasonTitleText(currentTeam, season);
    }

    public String getSeasonSubtitleText() {
        return SeasonPresentationController.getSeasonSubtitleText();
    }

    public String getSeasonYearChipText() {
        return SeasonPresentationController.getSeasonYearChipText(season);
    }

    public String getSeasonWeekChipText() {
        return SeasonPresentationController.getSeasonWeekChipText(simLeague);
    }

    public String getSeasonPhaseChipText() {
        return SeasonPresentationController.getSeasonPhaseChipText(simLeague);
    }

    private void selectTeam() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Your Program");
        builder.setMessage("Pick the school where your " + seasonStart + " head coaching run begins.");
        final String[] teams = simLeague.getTeamListStr();
        builder.setItems(teams, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                simLeague.teamList.get(item).HC.team = null;
                simLeague.coachFreeAgents.add(simLeague.teamList.get(item).HC);
                userTeam.userControlled = false;
                userTeam = simLeague.teamList.get(item);
                simLeague.userTeam = userTeam;
                userTeam.userControlled = true;
                userTeamStr = userTeam.name;
                currentTeam = userTeam;
                userNameDialog();
                userTeam.setupUserCoach(username);
                // set rankings so that not everyone is rank #0
                simLeague.setTeamRanks();
                simLeague.setTeamBenchMarks();
                simLeague.updateTeamTalentRatings();
                userHC = userTeam.HC;
                // Set toolbar text to '2017 Season' etc
                updateHeaderBar();
                examineTeam(currentTeam.name);
            }
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        showImmersive(alert);
    }

    private void userNameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enter the full name that will represent your head coach across the league, records, and news stories.");
        builder.setTitle("Name Your Head Coach")
                .setView(getLayoutInflater().inflate(R.layout.username_dialog, null));
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        showImmersive(dialog);

        final EditText changeHCEditText = dialog.findViewById(R.id.editTextChangeHC);
        changeHCEditText.setText(simLeague.getRandName());   //change Head HeadCoach Name

        final TextView invalidHCText = dialog.findViewById(R.id.textViewChangeHC);

        changeHCEditText.addTextChangedListener(new TextWatcher() {
            String newHC;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                newHC = s.toString().trim();
                if (!simLeague.isNameValid(newHC)) {
                    invalidHCText.setText("Name already in use or has illegal characters!");
                } else {
                    invalidHCText.setText("");
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newHC = s.toString().trim();
                if (!simLeague.isNameValid(newHC)) {
                    invalidHCText.setText("Name already in use or has illegal characters!");
                } else {
                    invalidHCText.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                newHC = s.toString().trim();
                if (!simLeague.isNameValid(newHC)) {
                    invalidHCText.setText("Name already in use or has illegal characters!");
                } else {
                    invalidHCText.setText("");
                }
            }

        });

        Button okChangeNameButton = dialog.findViewById(R.id.buttonOkChangeName);


        okChangeNameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                String newHC = changeHCEditText.getText().toString().trim();
                if (isNameValid((newHC))) {
                    userTeam.HC.name = newHC;
                    examineTeam(currentTeam.name);
                    dialog.dismiss();
                    setupCoachStyle();
                } else {
                    Toast.makeText(MainActivity.this, "Invalid name/abbr! Name not changed.",
                                Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupCoachStyle() {

        String[] coachChoice = {"Balanced Leader", "Defensive Architect", "Offensive Innovator", "Graduate Assistant (Hard Mode)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle("Choose Your Coaching Identity")
                .setSingleChoiceItems(coachChoice, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i == 0) {
                            setupCoachBal();
                        }
                        if(i == 1) {
                            setupCoachDef();
                        }
                        if(i == 2) {
                            setupCoachOff();
                        }
                        if(i == 3) {
                            setupCoachHard();
                        }
                        dialogInterface.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create(); 
        dialog.setCancelable(false);
        builder.setCancelable(false);
        showImmersive(dialog);

    }

    private void setupCoachOff() {
        userTeam.HC.ratOff = simLeague.getAvgCoachOff()+5;
        userTeam.HC.ratDef = simLeague.getAvgCoachDef()-5;
        userTeam.HC.ratOvr = userTeam.HC.getStaffOverall(userTeam.HC.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachDef() {
        userTeam.HC.ratOff = simLeague.getAvgCoachOff()-5;
        userTeam.HC.ratDef = simLeague.getAvgCoachDef()+5;
        userTeam.HC.ratOvr = userTeam.HC.getStaffOverall(userTeam.HC.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachBal() {
        userTeam.HC.ratOff = simLeague.getAvgCoachOff();
        userTeam.HC.ratDef = simLeague.getAvgCoachDef();
        userTeam.HC.ratOvr = userTeam.HC.getStaffOverall(userTeam.HC.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachHard() {
        userTeam.HC.ratOff = 50;
        userTeam.HC.ratDef = 50;
        userTeam.HC.ratDiscipline = 60;
        userTeam.HC.ratTalent = 50;
        userTeam.HC.ratOvr = userTeam.HC.getStaffOverall(userTeam.HC.overallWt);
        setupPlaybookOff();
    }


    private void setupPlaybookOff() {
        final PlaybookOffense[] pbOff = currentTeam.getPlaybookOff();

        String[] coachChoice = new String[pbOff.length];
        for(int i = 0; i < pbOff.length; i++) {
            coachChoice[i] = pbOff[i].getStratName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle("Choose Your Base Offense")
                .setSingleChoiceItems(coachChoice, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        userTeam.HC.offStrat = i;
                        if(userTeam.OC != null) userTeam.OC.offStrat = i;
                        if(userTeam.DC != null) userTeam.DC.offStrat = i;
                        userTeam.playbookOffNum = i;
                        userTeam.playbookOff = userTeam.getPlaybookOff()[i];
                        dialogInterface.dismiss();
                        setupPlaybooksDef();
                    }
                });

        final AlertDialog dialog = builder.create(); 
        dialog.setCancelable(false);
        builder.setCancelable(false);
        showImmersive(dialog);
    }

    private void setupPlaybooksDef() {
        final PlaybookDefense[] pbOff = currentTeam.getPlaybookDef();

        String[] coachChoice = new String[pbOff.length];
        for(int i = 0; i < pbOff.length; i++) {
            coachChoice[i] = pbOff[i].getStratName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder
                .setTitle("Choose Your Base Defense")
                .setSingleChoiceItems(coachChoice, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        userTeam.HC.defStrat = i;
                        if(userTeam.OC != null) userTeam.OC.offStrat = i;
                        if(userTeam.DC != null) userTeam.DC.offStrat = i;
                        userTeam.playbookDefNum = i;
                        userTeam.playbookDef = userTeam.getPlaybookDef()[i];
                        dialogInterface.dismiss();
                        if(simLeague.currentWeek == 0) seasonGoals();
                        defaultScreen();
                    }
                });

        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        builder.setCancelable(false);
        showImmersive(dialog);
    }




    public void resetTeamUI() {
        currentTeam = userTeam;
        examineTeam(userTeam.name);
        showHome();
    }

    public void resetUI() {
        if (currPage == 4) {
            currPage = 4;
            updateSchedule();
        } else if (currPage == 3) {
            currPage = 3;
            updateTeamStats();
        } else if (currPage == 2) {
            currPage = 2;
            showTeamPlayerStats();
        } else if (currPage == 1) {
            currPage = 1;
            viewRoster();
        }else {
            currPage = 0;
            showHome();
        }
    }

    @Override
    public void updateSpinners() {
        confList.clear();
        for (int i = 0; i < simLeague.conferences.size(); i++) {
            if(simLeague.conferences.get(i).confTeams.size() > 0) confList.add(simLeague.conferences.get(i).confName);
        }

        dataAdapterConf.notifyDataSetChanged();

        teamList = new ArrayList<>();
        dataAdapterTeam.clear();
        for (int i = 0; i < currentConference.confTeams.size() ; i++) {
            teamList.add(currentConference.confTeams.get(i).strRep());
            dataAdapterTeam.add(teamList.get(i));
        }
        dataAdapterTeam.notifyDataSetChanged();

        resetTeamUI();
    }

    @Override
    public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {
        Button simGameButton = findViewById(R.id.simGameButton);
        if (simGameButton != null) {
            simGameButton.setText(buttonText);
            // statusText could be used for a separate label if we had one
        }
        updateHeaderBar();
    }

    @Override
    public void showNotification(String title, String message) {
        PlatformUiHelper.showNotification(this, title, message);
    }

    @Override
    public void refreshCurrentPage() {
        resetUI();
    }

    @Override
    public void showAwardsSummary(String summaryText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Awards ceremony")
                .setMessage(summaryText)
                .setPositiveButton("OK", null);
        showImmersive(builder.create());
    }

    @Override
    public void showMidseasonSummary() {
        midseasonSummary();
    }

    @Override
    public void showSeasonSummary() {
        seasonSummary();
    }

    @Override
    public void showContractDialog() {
        if (simLeague.isCareerMode()) contractDialog();
    }

    @Override
    public void showJobOffersDialog() {
        if (simLeague.isCareerMode()) jobOffers(userHC);
    }

    @Override
    public void showPromotionsDialog() {
        if (simLeague.isCareerMode()) promotions(userHC);
    }

    @Override
    public void showRedshirtList() {
        showRedshirtListFix();
    }

    @Override
    public void showTransferList() {
        transfers();
    }

    @Override
    public void showRealignmentSummary() {
        // realignment summary usually shown via news headlines or specific dialogs
        // placeholder for now
    }

    @Override
    public void startRecruitingFlow() {
        beginRecruiting();
    }




    public void examineTeam(String teamName) {
        wantUpdateConf = false;
        // Find team
        Team tempT = simLeague.teamList.get(0);
        for (Team t : simLeague.teamList) {
            if (t.name.equals(teamName)) {
                currentTeam = t;
                tempT = t;
                break;
            }
        }
        // Find conference
        for (int i = 0; i < simLeague.conferences.size(); ++i) {
            Conference c = simLeague.conferences.get(i);
            if (c.confName.equals(currentTeam.conference)) {
                if (c == currentConference) wantUpdateConf = true;
                currentConference = c;
                examineConfSpinner.setSelection(i);
                break;
            }
        }

        teamList = new ArrayList<>();
        dataAdapterTeam.clear();
        for (int i = 0; i < currentConference.confTeams.size(); i++) {
            teamList.add(currentConference.confTeams.get(i).strRep());
            dataAdapterTeam.add(teamList.get(i));
        }
        dataAdapterTeam.notifyDataSetChanged();

        for (int i = 0; i < currentConference.confTeams.size(); ++i) {
            String[] spinnerSplit = dataAdapterTeam.getItem(i).split(" ");
            if (spinnerSplit.length == 2 && spinnerSplit[1].equals(tempT.name)) {
                examineTeamSpinner.setSelection(i);
                currentTeam = tempT;
                break;
            } else if (spinnerSplit.length == 3 && (spinnerSplit[1] + " " + spinnerSplit[2]).equals(tempT.name)) {
                examineTeamSpinner.setSelection(i);
                currentTeam = tempT;
                break;
            } else if (spinnerSplit.length == 4 && (spinnerSplit[1] + " " + spinnerSplit[2] + " " + spinnerSplit[3]).equals(tempT.name)) {
                examineTeamSpinner.setSelection(i);
                currentTeam = tempT;
                break;
            }
        }

    }

    private void updateCurrTeam() {
        teamList = new ArrayList<>();
        dataAdapterTeam.clear();
        for (int i = 0; i < currentConference.confTeams.size() ; i++) {
            teamList.add(currentConference.confTeams.get(i).strRep());
            dataAdapterTeam.add(teamList.get(i));
        }
        dataAdapterTeam.notifyDataSetChanged();
        updateHeaderBar();

        resetUI();

    }

    private void updateCurrConference() {
        confList.clear();
        for (int i = 0; i < simLeague.conferences.size(); i++) {
            if(simLeague.conferences.get(i).confTeams.size() > 0) confList.add(simLeague.conferences.get(i).confName);
        }
        dataAdapterConf.notifyDataSetChanged();

        if (wantUpdateConf) {
            teamList = new ArrayList<>();
            dataAdapterTeam.clear();
            for (int i = 0; i < currentConference.confTeams.size() ; i++) {
                teamList.add(currentConference.confTeams.get(i).strRep());
                dataAdapterTeam.add(teamList.get(i));
            }
            dataAdapterTeam.notifyDataSetChanged();
            examineTeamSpinner.setSelection(0);
            currentTeam = currentConference.confTeams.get(0);
            updateCurrTeam();
        } else {
            wantUpdateConf = true;
        }
    }

    private void scrollToLatestGame() {
        if (simLeague.currentWeek > 2) {
            mainList.setSelection(currentTeam.numGames() - 3);
        }

    }

    //MAIN SCREEN BUTTONS

    //Team Stats
    private void showHome() {
        currPage = 0;
        String[] teamStatsStr = currentTeam.getTeamHomeInfo().split("!!");

        Game[] games = new Game[currentTeam.gameSchedule.size()];
        for (int i = 0; i < games.length; ++i) {
            games[i] = currentTeam.gameSchedule.get(i);
        }
        int week = simLeague.currentWeek;

        mainList.setAdapter(new TeamHome(this, teamStatsStr, this, games, week));
    }

    //News Display
    public void showNewsStoriesDialog() {
        NewsDialogController.show(this, simLeague);
    }

    //Team Stats
    public void updateTeamStats() {
        String[] teamStatsStr = currentTeam.getTeamStatsStrCSV().split("%\n");
        mainList.setAdapter(new TeamStatsList(this, teamStatsStr));
    }

    //Player Stats
    private void showTeamPlayerStats() {
        ArrayList<String> players;
        players = currentTeam.getRosterStats();

        final IndividualStats playersStats = new IndividualStats(this, players, this);
        mainList.setAdapter(playersStats);
    }

    //Roster 2.0
    public void viewRoster() {
        ArrayList<String> roster;
        roster = currentTeam.getRoster();

        final TeamRoster teamRoster = new TeamRoster(this, roster, this, simLeague.currentWeek);
        mainList.setAdapter(teamRoster);
    }

    //Open Player Profile
    public void examinePlayer(String player) {
        Player p = currentTeam.findTeamPlayer(player);
        if (p == null) {
            //Do nothing
        } else {
            openPlayerProfile(p);
        }
    }

    public void examinePlayerandTeam(String player, String teamAbbr) {
        Team tempTeam = simLeague.findTeamAbbr(teamAbbr);
        Player p = tempTeam.findTeamPlayer(player);
        if (p == null) {
            //Do nothing
        } else {
            openPlayerProfile(p);
        }
    }

    public void openPlayerProfile(final Player p) {
        PlayerProfileSnapshot snapshot = PlayerProfileSnapshot.fromPlayer(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Player Profile")
                .setView(getLayoutInflater().inflate(R.layout.player_profile, null))
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });

        if(p.team == userTeam) {
            builder.setNeutralButton("Cut", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //do nothing
                    cutPlayerDialog(p);
                }
            });
        }

        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        bindPlayerProfile(dialog, p.name, snapshot);
    }

    private void bindPlayerProfile(AlertDialog dialog, String playerName, PlayerProfileSnapshot snapshot) {
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

        ppPosition.setText(a[0]);
        ppClass.setText(a[1]);
        ppTeam.setText(a[2]);
        ppHome.setText(a[3]);
        ppStars.setText(a[4]);
        ppHeight.setText(a[5]);
        ppWeight.setText(a[6]);
        ppOverall.setText(a[7]);
        ppCharacter.setText(a[8]);
        ppAwareness.setText(a[9]);
        ppStatus.setText(a[10]);
        ppDurability.setText(a[11]);

        String[] b = snapshot.ratings;

        if(b.length > 7) {
            ppAttr1Name.setText(b[0]);
            ppAttr1.setText(b[1]);
            ppAttr2Name.setText(b[2]);
            ppAttr2.setText(b[3]);
            ppAttr3Name.setText(b[4]);
            ppAttr3.setText(b[5]);
            ppAttr4Name.setText(b[6]);
            ppAttr4.setText(b[7]);
        }

        String[] teamStat = snapshot.statColumns;

        ppYear.setText(teamStat[0]);
        ppStat0.setText(teamStat[1]);
        ppStat1.setText(teamStat[2]);
        ppStat2.setText(teamStat[3]);
        ppStat3.setText(teamStat[4]);
        ppStat4.setText(teamStat[5]);
        ppStat5.setText(teamStat[6]);
        ppStat6.setText(teamStat[7]);
        ppStat7.setText(teamStat[8]);

        String[] c = snapshot.featuredStats;

        ppFeatStat1Name.setText(c[0]);
        ppFeatStat1.setText(c[1]);
        ppFeatStat2Name.setText(c[2]);
        ppFeatStat2.setText(c[3]);
        ppFeatStat3Name.setText(c[4]);
        ppFeatStat3.setText(c[5]);
        ppFeatStat4Name.setText(c[6]);
        ppFeatStat4.setText(c[7]);
    }

    public void cutPlayerDialog(final Player p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Player Cut")
                .setMessage("Are you sure you want to cut " + p.position + " " + p.name + "?\n\nIf this is cut occurs during season, he may be replaced with a walk-on to fill roster spots.")
                .setPositiveButton("Cut Player", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                        userTeam.cutPlayer(p);
                        resetUI();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        showImmersive(dialog);
    }

    //Open HeadCoach Profile from Database
    public void examineCoachDB(String player) {
        final Staff p = findCoachProfile(player);
        if (p == null) {
            //Do Nothing
        } else {
            openCoachProfile(p);
        }
    }

    public Staff findCoachProfile(String name) {
        Staff p = null;
        String[] nameSplit = name.split(" ");
        String nameHC = nameSplit[0] + " " + nameSplit[1];
        for(int i = 0; i < simLeague.teamList.size(); i++) {
            if(simLeague.teamList.get(i).HC != null && simLeague.teamList.get(i).HC.name.equals(nameHC)) return simLeague.teamList.get(i).HC;
            if(simLeague.teamList.get(i).OC != null && simLeague.teamList.get(i).OC.name.equals(nameHC)) return simLeague.teamList.get(i).OC;
            if(simLeague.teamList.get(i).DC != null && simLeague.teamList.get(i).DC.name.equals(nameHC)) return simLeague.teamList.get(i).DC;

        }

        for(int i = 0; i < simLeague.coachDatabase.size(); i++) {
            if(simLeague.coachDatabase.get(i).name.equals(nameHC)) return simLeague.coachDatabase.get(i);
        }
        return p;
    }

    public void openCoachProfile(final Staff p) {
        CoachProfileSnapshot snapshot = CoachProfileSnapshot.fromStaff(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(p.name)
                .setView(getLayoutInflater().inflate(R.layout.coach_profile, null))
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setNeutralButton("Coach History", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showCoachHistoryDialog(p);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        showImmersive(dialog);
        bindCoachProfile(dialog, snapshot);
    }

    private void bindCoachProfile(AlertDialog dialog, CoachProfileSnapshot snapshot) {
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
        cpPosition.setText(a[0]);
        cpClass.setText(a[1]);
        cpTeam.setText(a[2]);
        cpOverall.setText(a[3]);
        cpWins.setText(a[4]);
        cpLosses.setText(a[5]);
        cpStatus.setText(a[6]);
        cpContract.setText(a[7]);

        String[] b = snapshot.ratings;
        cpAttr1Name.setText(b[0]);
        cpAttr1.setText(b[1]);
        cpAttr2Name.setText(b[2]);
        cpAttr2.setText(b[3]);
        cpAttr3Name.setText(b[4]);
        cpAttr3.setText(b[5]);
        cpAttr4Name.setText(b[6]);
        cpAttr4.setText(b[7]);

        String[] c = snapshot.featuredStats;
        cpFeatStat1Name.setText(c[0]);
        cpFeatStat1.setText(c[1]);
        cpFeatStat2Name.setText(c[2]);
        cpFeatStat2.setText(c[3]);
        cpFeatStat3Name.setText(c[4]);
        cpFeatStat3.setText(c[5]);
        cpFeatStat4Name.setText(c[6]);
        cpFeatStat4.setText(c[7]);
    }




    //Player Awards for Bio
    public int checkAwardPlayer(String player) {
        Player p = currentTeam.findTeamPlayer(player);
        if (p == null) return 0;
        if (p.wonHeisman) return 3;
        if (p.wonAllAmerican) return 2;
        if (p.wonAllConference) return 1;
        return 0;
    }

    //Schedule
    public void updateSchedule() {
        Game[] games = new Game[currentTeam.gameSchedule.size()];
        for (int i = 0; i < games.length; ++i) {
            games[i] = currentTeam.gameSchedule.get(i);
        }
        mainList.setAdapter(new GameScheduleList(this, this, currentTeam, games));
        mainList.setSelection(currentTeam.numGames() - 3);
    }

    //Game Summary
    public void showGameDialog(Game g) {
        GameDialogController.show(this, g, userTeam);
    }

    //Weekly Scoreboard
    private void showWeeklyScores() {
        WeeklyScoresDialogController.show(this, simLeague);
    }

    // Shows Conference Standings
    public void updateStandings() {
        ArrayList<String> standings;
        standings = simLeague.getConfStandings();

        final MainRankings teamRankings = new MainRankings(this, standings, userTeam.name, this);
        mainList.setAdapter(teamRankings);
    }

    // Shows AP Polls
    public void updateRankings() {
        ArrayList<String> standings;
        standings = simLeague.getTeamRankings();

        final MainRankings teamRankings = new MainRankings(this, standings, userTeam.name, this);
        mainList.setAdapter(teamRankings);
    }

    //Depth Chart
    private void depthChartDialog() {
        DepthChartDialogController.showDepthChart(this, userTeam);
    }

    //Depth Chart
    private void redshirtDialog() {
        DepthChartDialogController.showRedshirt(this, userTeam);
    }


    //Team Stategy/Playbook
    private void showTeamStrategyDialog() {
        TeamStrategyDialogController.show(this, userTeam);
    }

    //Simulate Week
    private void simulateWeek() {
        if (seasonController != null) {
            seasonController.advanceWeek();
        }
    }


        if(userTeam.disciplineAction) disciplineSetup();

        resetUI();

    }





    //GAME MENU

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
         if (id == R.id.action_current_team_history) {

             //Current selected team history
            showCurrTeamHistoryDialog();
        } else if (id == R.id.action_league_history) {

             //Clicked League History in drop down menu
            showLeagueHistoryDialog();
        } else if (id == R.id.action_top_25_history) {

             //Clicked Top 25 History
            showTop25History();
        } else if (id == R.id.action_coach_DB) {

              //Clicked User Team History in drop down menu
            showCoachDatabase();
        } else if (id == R.id.action_save_league) {

              //Clicked Save League in drop down menu
            if (simLeague.currentWeek < 1 || simLeague.currentWeek == 99) {
                saveLeague();
            } else if (simLeague.currentWeek > 1) {
                Toast.makeText(MainActivity.this, "Save Function Disabled. Save only available in pre-season or before recruiting.",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Save Function disabled during initial season.",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_export_league) {

              //Clicked Save League in drop down menu

            exportData();

        } else if (id == R.id.action_return_main_menu) {

              //Let user confirm that they actually do want to go to main menu
            exitMainActivity();
        } else if (id == R.id.action_change_team_name) {

              //Let user change their team name and abbr
            changeSettingsDialog();
        } /*else if (id == R.id.action_show_FreeAgents) {

             //Let user change their team name and abbr
             showFreeAgents();
         }*/

        return super.onOptionsItemSelected(item);
    }
//User Settings
    private void changeSettingsDialog() {
        SettingsDialogController.show(this, simLeague);
    }

    //League History
    private void showLeagueHistoryDialog() {
        LeagueHistoryDialogController.show(this, simLeague, userTeam);
    }

    private void showLeagueHistoryStats() {
        LeagueHistoryDialogController.showLeagueHistoryStats(this, simLeague, userTeam);
    }

    private void showCoachDatabase() {
        LeagueHistoryDialogController.showCoachDatabase(this, simLeague, userTeam);
    }



    //AP Poll History
    private void showTop25History() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("AP Poll History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.bowl_ccg_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindArchiveDialogShell(dialog, "AP Poll Archive", "Step through each completed season to review how the national rankings evolved over time.");
        if (season == seasonStart) {
            String[] selection = {"No History to Display"};
            Spinner top25hisSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
            PlatformUiHelper.avoidSpinnerDropdownFocus(top25hisSpinner);
            final ArrayAdapter<String> top25Adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, selection);
            top25Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            top25hisSpinner.setAdapter(top25Adapter);
        } else {
            String[] selection = new String[simLeague.leagueHistory.size()];
            for (int i = 0; i < simLeague.leagueHistory.size(); ++i) {
                selection[i] = Integer.toString(seasonStart + i);
            }
            Spinner top25hisSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
            PlatformUiHelper.avoidSpinnerDropdownFocus(top25hisSpinner);
            final ArrayAdapter<String> top25Adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, selection);
            top25Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            top25hisSpinner.setAdapter(top25Adapter);

            final TextView top25his = dialog.findViewById(R.id.textViewBowlCCGDialog);

            top25hisSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(
                                AdapterView<?> parent, View view, int position, long id) {
                            top25his.setText(simLeague.getLeagueTop25History(position));
                        }

                        public void onNothingSelected(AdapterView<?> parent) {
                            // do nothing
                        }
                    });
        }
    }

    //Team History
    private void showCurrTeamHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentTeam.name + " History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, currentTeam.name + " Archive", "Review your program history, records, and hall-of-fame legacy from one unified team archive.");

        String[] selection = {"Team History", "Team Records", "Hall of Fame", "Graph View: Prestige", "Graph View: Rankings"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);
        final PlayerRecord[] hofPlayers = currentTeam.hallOfFame.toArray(new PlayerRecord[0]);

        teamHistSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            TeamHistoryRecord[] histArray = currentTeam.teamHistory.toArray(new TeamHistoryRecord[0]);
                            TeamHistoryList teamHistoryAdapter =
                                    new TeamHistoryList(MainActivity.this, histArray);
                            teamHistoryList.setAdapter(teamHistoryAdapter);
                        } else if (position == 1) {
                            LeagueRecordsList leagueRecordsAdapter =
                                    new LeagueRecordsList(MainActivity.this, currentTeam.teamRecords.getRecordsStr().split("\n"), "---", "---");
                            teamHistoryList.setAdapter(leagueRecordsAdapter);
                        } else if (position == 2) {
                            HallofFameList hofAdapter = new HallofFameList(MainActivity.this, hofPlayers, userTeam.name, true, MainActivity.this);
                            teamHistoryList.setAdapter(hofAdapter);
                        } else if (position == 3) {
                            dialog.dismiss();
                            teamPrestigeGraphView();
                        } else if (position == 4) {
                            dialog.dismiss();
                            teamRankingGraphView();
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    //Graph View

    private void teamPrestigeGraphView() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentTeam.name + ": Prestige History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.graphview, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindGraphDialogShell(dialog, currentTeam.name + " Prestige Trend", "See how program prestige has risen and fallen across your team's historical arc.");
        GraphView graph = dialog.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        String[] yearLabels = new String[currentTeam.teamHistory.size()];
        for (int i = 0; i < currentTeam.teamHistory.size(); i++) {
            series.appendData(new DataPoint(currentTeam.teamHistory.get(i).year, currentTeam.teamHistory.get(i).prestige), true, i + 1, false);
            yearLabels[i] = Integer.toString(currentTeam.teamHistory.get(i).year);
        }
        graph.addSeries(series);

        if (yearLabels.length > 1) {
            StaticLabelsFormatter years = new StaticLabelsFormatter(graph);
            years.setHorizontalLabels(yearLabels);
            graph.getGridLabelRenderer().setLabelFormatter(years);
            graph.getGridLabelRenderer().setNumHorizontalLabels(5);
            graph.getGridLabelRenderer().setNumVerticalLabels(6);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        simLeague.sortTeamList();
        graph.getViewport().setMaxY(simLeague.teamList.get(0).teamPrestige + 10);
        graph.getViewport().setMinY(0);
    }

    //Graph View

    private void teamRankingGraphView() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(currentTeam.name + ": Rankings History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.graphview, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindGraphDialogShell(dialog, currentTeam.name + " Ranking Trend", "Track where your program has landed in the national pecking order over time.");
        GraphView graph = dialog.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        String[] yearLabels = new String[currentTeam.teamHistory.size()];

        for (int i = 0; i < currentTeam.teamHistory.size(); i++) {
            series.appendData(new DataPoint(currentTeam.teamHistory.get(i).year, simLeague.teamList.size() - currentTeam.teamHistory.get(i).rank), true, i + 1, false);
            yearLabels[i] = Integer.toString(i + seasonStart);
        }
        graph.addSeries(series);

        String[] rankLabels = new String[simLeague.teamList.size()+1];
        for (int i = simLeague.teamList.size(); i >= 0; i--) {
            rankLabels[simLeague.teamList.size() - i] = Integer.toString(i);
        }

        if (yearLabels.length > 1) {
            StaticLabelsFormatter years = new StaticLabelsFormatter(graph);
            years.setHorizontalLabels(yearLabels);
            years.setVerticalLabels(rankLabels);
            graph.getGridLabelRenderer().setLabelFormatter(years);
            graph.getGridLabelRenderer().setNumHorizontalLabels(5);
            graph.getGridLabelRenderer().setNumVerticalLabels(6);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(simLeague.teamList.size());
        graph.getViewport().setMinY(0);
    }

    //HeadCoach History
    private void showCoachHistoryDialog(Staff p) {
        final Staff hc = p;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Head Coach History: " + hc.name)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Head Coach History", "Review one coach's stops and jump into prestige or ranking trend views from the same archive flow.");

        String[] selection = {"Team History", "Graph View: Prestige", "Graph View: Rankings"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);

        teamHistSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            TeamHistoryRecord[] histArray = hc.getCoachHistory().toArray(new TeamHistoryRecord[0]);
                            TeamHistoryList teamHistoryAdapter =
                                    new TeamHistoryList(MainActivity.this, histArray);
                            teamHistoryList.setAdapter(teamHistoryAdapter);
                        } else if (position == 1) {
                            dialog.dismiss();
                            coachGraphView(hc);
                        } else if (position == 2) {
                            dialog.dismiss();
                            coachGraphViewRank(hc);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    private void coachGraphView(Staff hc) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(hc.name + ": Prestige History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.graphview, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindGraphDialogShell(dialog, hc.name + " Prestige Trend", "Follow how this coach changed program prestige across each stop in his career.");

        DataPoint[] data = new DataPoint[hc.history.size()];
        GraphView graph = dialog.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        String[] yearLabels = new String[hc.history.size()];
        for (int i = 0; i < hc.history.size(); i++) {
            if (!hc.history.get(i).equals("")) {
                series.appendData(new DataPoint(Integer.parseInt(hc.history.get(i).split(": ")[0]), Integer.parseInt(hc.history.get(i).split("Prs: ")[1].split(" ")[0])), true, i + 1, false);
                yearLabels[i] = hc.history.get(i).split(":")[0];
            }
        }
        graph.addSeries(series);

        if (yearLabels.length > 1) {
            StaticLabelsFormatter years = new StaticLabelsFormatter(graph);
            years.setHorizontalLabels(yearLabels);
            graph.getGridLabelRenderer().setLabelFormatter(years);
            graph.getGridLabelRenderer().setNumHorizontalLabels(4);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        simLeague.sortTeamList();
        graph.getViewport().setMaxY(simLeague.teamList.get(0).teamPrestige + 10);
        graph.getViewport().setMinY(0);
    }

    private void coachGraphViewRank(Staff hc) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(hc.name + ": Rankings History")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.graphview, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindGraphDialogShell(dialog, hc.name + " Ranking Trend", "Follow how this coach's teams climbed or slid in the national rankings over time.");
        DataPoint[] data = new DataPoint[hc.history.size()];
        GraphView graph = dialog.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        String[] yearLabels = new String[hc.history.size()];
        for (int i = 0; i < hc.history.size(); i++) {
            if (!hc.history.get(i).equals("")) {
                series.appendData(new DataPoint(Integer.parseInt(hc.history.get(i).split(": ")[0]), simLeague.teamList.size() - Integer.parseInt(hc.history.get(i).split("#")[1].split(" ")[0])), true, i + 1, false);
                yearLabels[i] = hc.history.get(i).split(":")[0];
            }
        }
        graph.addSeries(series);

        String[] rankLabels = new String[simLeague.teamList.size()+1];
        for (int i = simLeague.teamList.size(); i >= 0; i--) {
            rankLabels[simLeague.teamList.size() - i] = Integer.toString(i);
        }

        if (yearLabels.length > 1) {
            StaticLabelsFormatter years = new StaticLabelsFormatter(graph);
            years.setHorizontalLabels(yearLabels);
            years.setVerticalLabels(rankLabels);
            graph.getGridLabelRenderer().setLabelFormatter(years);
            graph.getGridLabelRenderer().setNumHorizontalLabels(4);
            graph.getGridLabelRenderer().setNumVerticalLabels(6);
        }
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(simLeague.teamList.size());
        graph.getViewport().setMinY(0);
    }

    //Open Hall of Fame Profile from Database
    public void examineHOF(String player) {
        if (player == null) {

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            String[] pStatsArray = player.split("&");
            PlayerProfile pStatsAdapter = new PlayerProfile(this, pStatsArray);
            builder.setAdapter(pStatsAdapter, null)
                    .setTitle("Player Card")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing
                        }
                    });

            AlertDialog dialog = builder.create(); dialog.setCancelable(false);
            showImmersive(dialog);
        }
    }

    //Team Stats Rankings
    private void showTeamRankingsDialog() {
        TeamRankingsDialogController.show(this, simLeague, userTeam);
    }

    //Player Stats Rankings
    private void showPlayerRankingsDialog() {
        PlayerRankingsDialogController.show(this, simLeague, userTeam);
    }

    //Bowl Games Schedule
    private void showBowlCCGDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Post-Season Games")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.bowl_ccg_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindArchiveDialogShell(dialog, "Post-Season Archive", "Switch between conference title games and postseason slates in one shared archive shell.");

        String[] selection = {"Conf Championships", "Post-Season"};
        Spinner bowlCCGSpinner = dialog.findViewById(R.id.spinnerBowlCCG);
        PlatformUiHelper.avoidSpinnerDropdownFocus(bowlCCGSpinner);
        ArrayAdapter<String> bowlCCGadapter = new ArrayAdapter<>(this,
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

    //Awards Nav Menu
    private void showLeagueAwards() {

        if (simLeague.currentWeek < simLeague.regSeasonWeeks) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Player of the Year Watch")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing?
                        }
                    })
                    .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
            AlertDialog dialog = builder.create(); dialog.setCancelable(false);
            showImmersive(dialog);
            ArrayList<String> rankings = new ArrayList<>();
            String[] rankingsSelection =
                    {"Head Coach - Overall", "QB - Overall", "RB - Overall", "WR - Overall", "TE - Overall", "OL - Overall", "K - Overall", "DL - Overall", "LB - Overall", "CB - Overall", "S - Overall"};
            Spinner teamRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
            PlatformUiHelper.avoidSpinnerDropdownFocus(teamRankingsSpinner);
            ArrayAdapter<String> teamRankingsSpinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, rankingsSelection);
            teamRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            teamRankingsSpinner.setAdapter(teamRankingsSpinnerAdapter);

            final ListView teamRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
            final TeamRankingsList teamRankingsAdapter =
                    new TeamRankingsList(this, rankings, userTeam.abbr);
            teamRankingsList.setAdapter(teamRankingsAdapter);

            teamRankingsSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(
                                AdapterView<?> parent, View view, int position, long id) {
                            ArrayList<String> rankings = simLeague.getAwardsWatch(position);
                            if (position == 12) {
                                teamRankingsAdapter.setUserTeamStrRep(userTeam.abbr);
                            } else {
                                teamRankingsAdapter.setUserTeamStrRep(userTeam.abbr);
                            }
                            teamRankingsAdapter.clear();
                            teamRankingsAdapter.addAll(rankings);
                            teamRankingsAdapter.notifyDataSetChanged();
                        }

                        public void onNothingSelected(AdapterView<?> parent) {
                            // do nothing
                        }
                    });

        } else {
            heismanCeremony();
        }
    }

    //Awards
    private void heismanCeremony() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Post Season Awards")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing?
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);

        String[] selection;
        if (simLeague.currentWeek < simLeague.regSeasonWeeks) {
            selection = new String[1];
            selection[0] = "Offensive Player of the Year";
        } else {
            int confNum = 0;
            for (int i = 0; i < simLeague.conferences.size(); ++i) {
                if(simLeague.conferences.get(i).confTeams.size() >= simLeague.conferences.get(i).minConfTeams) confNum++;
            }
            selection = new String[6 + confNum];
            selection[0] = "Offensive Player of the Year";
            selection[1] = "Defensive Player of the Year";
            selection[2] = "Head Coach of the Year";
            selection[3] = "Freshman of the Year";
            selection[4] = "All-American Team";
            selection[5] = "All-Freshman Team";

            confNum = 0;
            for (int i = 0; i < simLeague.conferences.size(); ++i) {
                if(simLeague.conferences.get(i).confTeams.size() >= simLeague.conferences.get(i).minConfTeams) {
                    selection[confNum + 6] = simLeague.conferences.get(i).confName + " All-Conf Team";
                    confNum++;
                }
            }
        }

        Spinner potySpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(potySpinner);
        ArrayAdapter<String> potyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, selection);
        potyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        potySpinner.setAdapter(potyAdapter);

        final ListView potyList = dialog.findViewById(R.id.listViewTeamRankings);

        // Get all american and all conf
        final String[] coachAwardList = simLeague.getCoachAwardStr().split(">");
        final String[] defAwardList = simLeague.getDefensePOTYStr().split(">");
        final String[] freshmanAwardList = simLeague.getFreshmanCeremonyStr().split(">");
        final String[] allAmericans = simLeague.getAllAmericanStr().split(">");
        final String[] allFreshman = simLeague.getAllFreshmanStr().split(">");
        final String[][] allConference = new String[simLeague.conferences.size()][];

        int confNum = 0;
        for (int i = 0; i < simLeague.conferences.size(); ++i) {
            if(simLeague.conferences.get(i).confTeams.size() >= simLeague.conferences.get(i).minConfTeams) {
                allConference[confNum] = simLeague.getAllConfStr(i).split(">");
                confNum++;
            }
        }


        potySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, simLeague.getHeismanCeremonyStr().split(">"), userTeam.abbr));
                        } else if (position == 1) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, defAwardList, userTeam.abbr));
                        } else if (position == 2) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, coachAwardList, userTeam.abbr));
                        } else if (position == 3) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, freshmanAwardList, userTeam.abbr));
                        } else if (position == 4) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, allAmericans, userTeam.abbr));
                        } else if (position == 5) {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, allFreshman, userTeam.abbr));
                        } else {
                            potyList.setAdapter(new SeasonAwardsList(MainActivity.this, allConference[position - 6], userTeam.abbr));
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    private void importDataPrompt() {
        LeagueImportFlowController.showImportPrompt(this, newGame);
    }

    @Override
    public MainActivity getDialogContext() {
        return this;
    }

    @Override
    public void showDialog(AlertDialog dialog) {
        dialog.setCancelable(false);
        showImmersive(dialog);
    }

    @Override
    public void requestImportDocument(LeagueImportFlowController.ImportType type) {
        pendingImportType = type;
        isExternalStorageReadable();
        importDocumentPicker.launch(new String[]{"*/*"});
    }

    @Override
    public void finishImportFlowForNewGame() {
        if (newGame) {
            newGame = false;
            careerModeOptions();
        }
    }

    //Save File Dialog
    private void saveLeague() {
        AlertDialog.Builder save = new AlertDialog.Builder(this);
        save.setTitle("Choose Save File to Overwrite:");
        final String[] fileInfos = saveLoadService.getSaveFileSummaries();
        SaveFilesList saveFilesAdapter = new SaveFilesList(this, fileInfos);
        save.setAdapter(saveFilesAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                final int itemy = item;
                // Do something with the selection
                if (saveLoadService.isSlotEmpty(itemy)) {
                    // Empty file, don't show dialog confirmation
                    saveLoadService.saveToSlot(simLeague, itemy);
                    Toast.makeText(MainActivity.this, "Saved league!",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    // Ask for confirmation to overwrite file
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to overwrite this save file?\n\n" + fileInfos[itemy])
                            .setPositiveButton("Yes, Overwrite", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Actually go back to main menu
                                    saveLoadService.saveToSlot(simLeague, itemy);
                                    Toast.makeText(MainActivity.this, "Saved league!",
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing
                                    dialog.dismiss();
                                }
                            });
                    AlertDialog dialog2 = builder.create();
                    dialog2.setCancelable(false);
                    dialog2.show();
                    TextView textView = dialog2.findViewById(android.R.id.message);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                }
            }
        });

        save.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });
        AlertDialog popup = save.create();
        popup.show();
    }

    //Get Save Files from Storage
    private String[] getSaveFileInfos() {
        return saveLoadService.getSaveFileSummaries();
    }


    //Export Save File

    private void exportData() {
        //WORK IN PROGRESS
        if(simLeague.currentWeek < 1) exportSave();
        else {
            Toast.makeText(MainActivity.this, "Export Function Disabled. Export is only allowed during Pre-Season.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    //Exit Current Game
    public void exitMainActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to return to main menu? Any progress from the beginning of the season will be lost.")
                .setPositiveButton("Yes, Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Actually go back to main menu
                        finish();
                        flowManager.returnToMainHub();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
    }


    //IN-GAME DISPLAYS

    //New Game Options
    private void careerModeOptions() {
        SettingsDialogController.showCareerSetup(this, simLeague);
    }

    private void universalProRelAction() {

        // Perform action on click
        simLeague.enableUnivProRel = true;
        simLeague.confRealignment = false;
        simLeague.convertUnivProRel();
        updateCurrConference();
        updateCurrTeam();
        examineTeam(userTeam.name);

    }

    //Pre-Season Goals
    private void seasonGoals() {
        simLeague.updateTeamTalentRatings();
        simLeague.setTeamBenchMarks();

        goals = "";
        int confPos = 0;

        for (int i = 0; i < simLeague.conferences.size(); ++i) {
            Conference c = simLeague.conferences.get(i);
            if (c.confName.equals(userTeam.conference)) {
                for (int x = 0; x < c.confTeams.size(); x++) {
                    if (c.confTeams.get(x).name.equals(userTeam.name)) {
                        confPos = x + 1;
                        break;
                    }
                }
            }
        }

        goals = "Welcome to the " + simLeague.getYear() + " College Football season, Coach " + userTeam.HC.name + "!\n\n";
        if (simLeague.isCareerMode()) {
            goals += "Your head coaching career begins at " + userTeam.name + ". Job security, performance swings, and future opportunities will all respond to the seasons you build here.\n\n";
        }
        if (simLeague.expPlayoffs) {
            goals += "This universe is using the 12-team playoff, so a strong finish can still open a path to the national title even if your team starts outside the top four.\n\n";
        } else {
            goals += "This universe is using the classic four-team playoff, so every loss near the top of the rankings carries extra weight.\n\n";
        }
        goals += "This season your team is projected to finish ranked #" + userTeam.projectedPollRank + "!\n\n";

        int num = (int)(simLeague.teamList.size()*.875);
        if (userTeam.projectedPollRank > num) {
            goals += "Despite being projected at #" + userTeam.projectedPollRank + ", your goal is to finish in the Top " + num + ".\n\n";
        }

        goals += "In conference play, your team is expected to finish " + userTeam.getRankStr(confPos) + " in the " + userTeam.conference + " conference.\n\n";

        int games = 0;
        for(Game g : userTeam.gameSchedule) {
            if (g.gameName.equals("OOC") || g.gameName.equals("Conference") || g.gameName.equals("Division")) {
                games++;
            }
        }

        goals += "Based on your schedule, your team is projected to finish with a record of " + userTeam.projectedWins + " - " + (games - userTeam.projectedWins) + ".\n\n";

        if (simLeague.isCareerMode()) {
            int yearsLeft = userTeam.HC.contractLength - userTeam.HC.contractYear;
            if (yearsLeft < 0) yearsLeft = 0;
            goals += "Contract outlook: " + yearsLeft + " year(s) remain on your deal, and your current AD pressure is " + userTeam.HC.coachStatus() + ".\n\n";
        }

        if (simLeague.getYear() > seasonStart) {
            if (userTeam.bowlBan) {
                goals += "Your team was penalized heavily for off-season issues by the College Athletic Administration and will lose Prestige and suffer a post-season bowl ban this year.\n\n";
            }
            if (userTeam.penalized) {
                goals += "Your team had a minor infraction over the off-season and lost some Prestige.\n\n";
            }
        }

        if (simLeague.getYear() > seasonStart) {
            if (userTeam.facilityUpgrade) {
                goals += "Your team upgraded the training facilities this off-season to Level " + userTeam.teamFacilities + " which added an additional " + userTeam.teamFacilities + " prestige points!\n\n";
            }
        }

        simLeague.newsStories.get(simLeague.currentWeek).add("Season Goals>" + goals);
        showSeasonGoalsDialog();

    }

    private void showSeasonGoalsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(goals + "\nNote: You can always review your season goals in the Pre-Season News.")
                .setTitle(simLeague.getYear() + " Season Goals")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("SAVE PROGRESS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveLeague();
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    //Pre-Season Options
    //Redshirts, Set Budgets, etc.
    private void preseasonOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("This will let you redshirt and set budgets in the future")
                .setTitle(simLeague.getYear() + " Pre-Season")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("SAVE PROGRESS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveLeague();
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    public void showSuspensions() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(userTeam.suspensionNews)
                .setTitle("DISCIPLINARY ACTION")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        userTeam.suspension = false;
    }

    //mid-season summary
    private void midseasonSummary() {
        String string = "";
        simLeague.midSeasonProgression();
        string = userTeam.midseasonUserProgression();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(string)
                .setTitle("Mid-Season Progress Report")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    //End of Season Summary
    private void seasonSummary() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(simLeague.seasonSummaryStr() + "\n\nNote: You can always review your season summary in the Off-Season News.")
                .setTitle(simLeague.getYear() + " Season Summary")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("All Prestige Changes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showPrestigeChange();
                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        simLeague.newsStories.get(simLeague.currentWeek + 1).add("Season Summary>" + simLeague.seasonSummaryStr());
        simLeague.newsHeadlines.add("That wraps up the " + simLeague.getYear() + " Season");
    }

    //Show Prestige Change
    private void showPrestigeChange() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Prestige Rankings")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Prestige Movement", "See which programs are rising and falling across the current college football landscape.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(1), userTeam.name);
        teamRankingsList.setAdapter(teamRankingsAdapter);
    }

    //Contract Status Dialog
    private void contractDialog() {
        if (simLeague.isCareerMode()) {
            if (userHC.age > retireAge) {
                userHC.retirement = true;
            }
        }

        if (userHC.retirement && !skipRetirementQ && !simLeague.neverRetire) {
            retirementQuestion();
            skipRetirementQ = true;

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(userTeam.contractString)
                    .setTitle(simLeague.getYear() + " Contract Status")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setNeutralButton("Season Goals", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (goals == null || goals.isEmpty()) {
                                seasonGoals();
                            } else {
                                showSeasonGoalsDialog();
                            }
                        }
                    })
                    .setNegativeButton("View Coaching News", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showNewsStoriesDialog();
                        }
                    });
            AlertDialog dialog = builder.create(); dialog.setCancelable(false);
            showImmersive(dialog);
            TextView textView = dialog.findViewById(android.R.id.message);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    //Job Offers Dialog when fired or resignation from previous team
    private void jobOffers(HeadCoach headCoach) {
        jobType  = 1;
        jobListSet = true;
        jobList.clear();

        userHC = headCoach;
        int ratOvr = userHC.getStaffOverall(userHC.overallWt);
        if (ratOvr < 40) ratOvr = 40;
        String oldTeam = "NO TEAM";
        if(userHC.team != null) oldTeam = userHC.team.name;
        updateHeaderBar();
        //get user team from list dialog
        jobList = simLeague.getCoachListFired(ratOvr, oldTeam);
        String[] teams = setJobTeamList(jobList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (jobList.isEmpty()) {
            builder.setTitle("Head Coach Opportunities")
                    .setMessage("No immediate head coach openings are available right now. You will remain on the market until a program makes a move.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.setCancelable(false);
            showImmersive(alert);
        } else {
            builder.setTitle("Head Coach Opportunities")
                    .setMessage("Review each opening before you decide. Every move resets your contract clock and ties your new expectations to that program's prestige.")
                    .setCancelable(false);
            builder.setItems(teams, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    viewTeam(jobList, item);
                }
            });
            AlertDialog alert = builder.create();
            alert.setCancelable(false);
            showImmersive(alert);
        }
    }

    //Job offers from other teams
    private void promotions(HeadCoach headCoach) {
        jobType  = 2;
        jobListSet = true;
        jobList.clear();

        userHC = headCoach;
        if (userHC.promotionCandidate) {

            int ratOvr = userHC.getStaffOverall(userTeam.HC.overallWt);
            if (ratOvr < 40) ratOvr = 40;
            double offers = 2;
            String oldTeam = "NO TEAM";
            if(userHC.team != null) oldTeam = userHC.team.name;
            updateHeaderBar();
            //get user team from list dialog
            jobList = simLeague.getCoachPromotionList(ratOvr, offers, oldTeam);

            String[] teams = setJobTeamList(jobList);

            if (jobList.isEmpty()) {
                userHC.promotionCandidate = false;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Head Coach Opportunities")
                        .setMessage("No job offers came through this cycle. Build on this season and stronger offers should return if you keep climbing.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog alert = builder.create();
                alert.setCancelable(false);
                showImmersive(alert);

            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Career Advancement Opportunities")
                        .setMessage("Programs are interested in your work. Review the roster foundation, prestige, and expectations before making your move.")
                        .setPositiveButton("Decline Offers", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                userHC.promotionCandidate = false;
                            }
                        });
                builder.setCancelable(false);
                builder.setItems(teams, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // Do something with the selection
                        viewTeam(jobList, item);
                    }
                });
                AlertDialog alert = builder.create();
                alert.setCancelable(false);
                showImmersive(alert);
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Career Advancement Opportunities")
                    .setMessage("No job offers are available this offseason. Your program did not build enough momentum to put you on other schools' short lists.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.setCancelable(false);
            showImmersive(alert);
        }
    }

    //Choose ANY team (manually change from Options Menu)
    private void selectNewTeam(HeadCoach headCoach) {
        jobType  = 0;
        jobListSet = true;
        jobList.clear();

        userHC = headCoach;
        updateHeaderBar();
        //get user team from list dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your new team:");
        jobList = simLeague.teamList;

        final String[] teams = simLeague.getTeamListStr();
        builder.setItems(teams, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                //changeTeams(coachList, item);
                //updateHeaderBar();
                //examineTeam(currentTeam.name);
                viewTeam(jobList, item);
            }
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create(); alert.setCancelable(false);
        showImmersive(alert);
    }

    //Make a list of Team Names for the Team Selection Window
    private String[] setJobTeamList(ArrayList<Team> jobListTemp) {
        String[] temp = new String[jobListTemp.size()];

        for(int i=0; i < jobListTemp.size(); i++) {
            Team team = jobListTemp.get(i);
            int prestigeDelta = team.teamPrestige - userHC.baselinePrestige;
            String direction = prestigeDelta > 0 ? "+" : "";
            temp[i] = team.name
                    + "\nPrestige #" + team.rankTeamPrestige
                    + "  |  Talent " + df2.format(team.teamOffTalent + team.teamDefTalent)
                    + "  |  Fit Req " + team.getMinCoachHireReq()
                    + "\nProgram swing: " + direction + prestigeDelta + " prestige vs your current baseline";
        }

        return temp;
    }

    private String buildJobOpportunitySummary(Team team) {
        int prestigeDelta = team.teamPrestige - userHC.baselinePrestige;
        String direction = prestigeDelta > 0 ? "+" : "";
        StringBuilder summary = new StringBuilder();
        summary.append("Program snapshot\n");
        summary.append("Prestige: #").append(team.rankTeamPrestige)
                .append(" (").append(team.teamPrestige).append(")  |  Conference: ").append(team.confPrestige).append("\n");
        summary.append("Roster talent: Off ").append(df2.format(team.teamOffTalent))
                .append("  |  Def ").append(df2.format(team.teamDefTalent))
                .append("  |  Combined ").append(df2.format(team.teamOffTalent + team.teamDefTalent)).append("\n");
        summary.append("Coach fit threshold: ").append(team.getMinCoachHireReq())
                .append("  |  Career swing: ").append(direction).append(prestigeDelta).append(" prestige\n");
        summary.append("If you accept, your head coach contract resets to a fresh 6-year deal and your AD expectations will be recalibrated to this job.\n\n");
        return summary.toString();
    }

    //View Team prior to choosing
    private void viewTeam(final ArrayList<Team> teamList, final int item) {
        String[] teamRoster = teamList.get(item).getTeamRosterString();
        final Team selectedTeam = teamList.get(item);

        AlertDialog.Builder roster = new AlertDialog.Builder(this);
        roster.setTitle(selectedTeam.name + " Program Review" +
                        "\nPrestige #" + selectedTeam.rankTeamPrestige + " | Off " + df2.format(selectedTeam.teamOffTalent) + " | Def " + df2.format(selectedTeam.teamDefTalent));
        roster.setNeutralButton("Back to Offers", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(jobType == 2)  promotions(userHC);
                else if (jobType == 1) jobOffers(userHC);
                else selectNewTeam(userHC);

            }
        });
        roster.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userHC.promotionCandidate = false;
                changeTeams(jobList, item);
                if(jobType == 2) simLeague.coachCarousel();
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append(buildJobOpportunitySummary(selectedTeam));
        sb.append("Projected roster\n");
        for(String s : teamRoster) {
            if(s != null) sb.append(s).append("\n");
        }
        roster.setMessage(sb);


        roster.setCancelable(false);
        AlertDialog teamWindow = roster.create();
        teamWindow.setCancelable(false);
        showImmersive(teamWindow);
    }


    //Method to actually switch teams
    private void changeTeams(ArrayList<Team> teamList, int item) {
        userTeam.newCoachTeamChanges();
        userTeam.userControlled = false;
        userTeam.HC = null;
        simLeague.coachHiringSingleTeam(userTeam);
        simLeague.newJobtransfer(teamList.get(item).name);
        userTeam = simLeague.userTeam;
        userTeamStr = userTeam.name;
        currentTeam = userTeam;
        userTeam.HC = null;

        if(reincarnate) {
            userTeam.setupUserCoach(userHC.name);
            userHC = userTeam.HC;
            reincarnate = false;
            userNameDialog();
        } else {
            userTeam.HC = userHC;
        }

        userHC.team = userTeam;
        userTeam.fired = false;
        userHC.contractYear = 0;
        userHC.contractLength = 6;
        userHC.baselinePrestige = userTeam.teamPrestige;
        userHC.promotionCandidate = false;
        simLeague.newsStories.get(simLeague.currentWeek + 1).add("Coaching Hire: " + currentTeam.name + ">After an extensive search for a new head coach, " + currentTeam.name + " has hired " + userHC.name +
                " to lead the team.");
        updateHeaderBar();
        examineTeam(currentTeam.name);
        hireOCNewTeam();
    }

    public void hireAssistants() {

        if(userTeam.OC == null || userTeam.OC.contractYear >=  userTeam.OC.contractLength) hireOC();
        else if(userTeam.DC == null || userTeam.DC.contractYear >= userTeam.DC.contractLength) hireDC();
        else simLeague.coordinatorCarousel();
        resetUI();
    }

    public void hireAssistantsFix() {

        if(userTeam.OC == null) hireOC();
        else if(userTeam.DC == null) hireDC();
        else simLeague.coordinatorCarousel();
        resetUI();
    }
    public void hireOC() {
        final ArrayList<Staff> list = simLeague.getOCList(userTeam.HC);
        String[] oc = new String[list.size()];
        final PlaybookOffense[] playbook = userTeam.getPlaybookOff();
        int num = 0;

        if(userTeam.OC != null) {
            num = 1;
            oc[0] = userTeam.OC.name + " [current]\nAge: " + userTeam.OC.age + "  Off: " + userTeam.OC.ratOff + "  Tal: " + userTeam.OC.ratTalent + "  " +  playbook[userTeam.OC.offStrat].getStratName() + "\n";
        }

        for(int i = num; i < list.size(); i++) {
            oc[i] = list.get(i).name + "\nAge: " + list.get(i).age + "  Off: " + list.get(i).ratOff + "  Tal: " + list.get(i).ratTalent + "  " +  playbook[list.get(i).offStrat].getStratName() + "\n";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Off Coordinators Available:");
        builder.setCancelable(false);
        builder.setItems(oc, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0 && userTeam.OC != null) {
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    userTeam.OC.baselinePrestige = 0;
                } else {
                    userTeam.OC = new OC(list.get(item), userTeam);
                    simLeague.newsHeadlines.add(userTeam.name + " adds new Off Coord " + userTeam.OC.name);
                    simLeague.newsStories.get(simLeague.currentWeek).add("Off Coord Change: " + userTeam.name + ">After an extensive search for a new coordinator, " + userTeam.name + " has hired " + userTeam.OC.name +
                            " to lead the offense.");
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    simLeague.coachFreeAgents.remove(list.get(item));

                    if (userTeam.DC == null || userTeam.DC.contractYear >= userTeam.DC.contractLength) hireDC();
                    else simLeague.coordinatorCarousel();
                }
                resetUI();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        showImmersive(alert);

    }

    public void hireDC() {
        final ArrayList<Staff> list = simLeague.getDCList(userTeam.HC);
        String[] dc = new String[list.size()];
        final PlaybookDefense[] playbook = userTeam.getPlaybookDef();
        int num = 0;

        if(userTeam.DC != null) {
            num = 1;
            dc[0] = userTeam.DC.name + " [current]\nAge: " + userTeam.DC.age + "  Def: " + userTeam.DC.ratDef + "  Tal: " + userTeam.DC.ratTalent + "  " + playbook[userTeam.DC.defStrat].getStratName() + "\n";
        }

        for(int i = num; i < list.size(); i++) {
            dc[i] = list.get(i).name + "\nAge: " + list.get(i).age + "  Def: " + list.get(i).ratDef + " Tal: " + list.get(i).ratTalent + "  " +  playbook[list.get(i).defStrat].getStratName() + "\n";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Def Coordinators Available:");
        builder.setCancelable(false);
        builder.setItems(dc, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0 && userTeam.DC != null) {
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    userTeam.DC.baselinePrestige = 0;
                } else {
                    userTeam.DC = new DC(list.get(item), userTeam);
                    simLeague.newsHeadlines.add(userTeam.name + " adds new Def Coord " + userTeam.DC.name);
                    simLeague.newsStories.get(simLeague.currentWeek).add("Def Coord Change: " + userTeam.name + ">After an extensive search for a new coordinator, " + userTeam.name + " has hired " + userTeam.DC.name +
                            " to lead the defense.");
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    simLeague.coachFreeAgents.remove(list.get(item));
                    simLeague.coordinatorCarousel();
                }
                resetUI();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        showImmersive(alert);

    }

    public void hireOCNewTeam() {
        final ArrayList<Staff> list = simLeague.getOCList(userTeam.HC);
        String[] oc = new String[list.size()];
        final PlaybookOffense[] playbook = userTeam.getPlaybookOff();
        int num = 0;

        if(userTeam.OC != null) {
            num = 1;
            oc[0] = userTeam.OC.name + " [current]\nAge: " + userTeam.OC.age + "  Off: " + userTeam.OC.ratOff + "  Tal: " + userTeam.OC.ratTalent + "  " +  playbook[userTeam.OC.offStrat].getStratName() + "\n";
        }

        for(int i = num; i < list.size(); i++) {
            oc[i] = list.get(i).name + "\nAge: " + list.get(i).age + "  Off: " + list.get(i).ratOff + "  Tal: " + list.get(i).ratTalent + "  " +  playbook[list.get(i).offStrat].getStratName() + "\n";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Off Coordinators Available:");
        builder.setCancelable(false);
        builder.setItems(oc, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0 && userTeam.OC != null) {
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    userTeam.OC.baselinePrestige = 0;
                } else {
                    userTeam.OC = new OC(list.get(item), userTeam);
                    simLeague.newsHeadlines.add(userTeam.name + " adds new Off Coord " + userTeam.OC.name);
                    simLeague.newsStories.get(simLeague.currentWeek).add("Off Coord Change: " + userTeam.name + ">After an extensive search for a new coordinator, " + userTeam.name + " has hired " + userTeam.OC.name +
                            " to lead the offense.");
                    userTeam.OC.contractLength = 3;
                    userTeam.OC.contractYear = 0;
                    simLeague.coachFreeAgents.remove(list.get(item));
                    dialog.dismiss();
                    hireDCNewTeam();
                }
                resetUI();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        showImmersive(alert);

    }

    public void hireDCNewTeam() {
        final ArrayList<Staff> list = simLeague.getDCList(userTeam.HC);
        String[] dc = new String[list.size()];
        final PlaybookDefense[] playbook = userTeam.getPlaybookDef();
        int num = 0;

        if(userTeam.DC != null) {
            num = 1;
            dc[0] = userTeam.DC.name + " [current]\nAge: " + userTeam.DC.age + "  Def: " + userTeam.DC.ratDef + "  Tal: " + userTeam.DC.ratTalent + "  " + playbook[userTeam.DC.defStrat].getStratName() + "\n";
        }

        for(int i = num; i < list.size(); i++) {
            dc[i] = list.get(i).name + "\nAge: " + list.get(i).age + "  Def: " + list.get(i).ratDef + " Tal: " + list.get(i).ratTalent + "  " +  playbook[list.get(i).defStrat].getStratName() + "\n";
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Def Coordinators Available:");
        builder.setCancelable(false);
        builder.setItems(dc, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0 && userTeam.DC != null) {
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    userTeam.DC.baselinePrestige = 0;
                } else {
                    userTeam.DC = new DC(list.get(item), userTeam);
                    simLeague.newsHeadlines.add(userTeam.name + " adds new Def Coord " + userTeam.DC.name);
                    simLeague.newsStories.get(simLeague.currentWeek).add("Def Coord Change: " + userTeam.name + ">After an extensive search for a new coordinator, " + userTeam.name + " has hired " + userTeam.DC.name +
                            " to lead the defense.");
                    userTeam.DC.contractLength = 3;
                    userTeam.DC.contractYear = 0;
                    simLeague.coachFreeAgents.remove(list.get(item));
                    simLeague.coordinatorCarousel();
                }
                resetUI();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        showImmersive(alert);

    }




    //Conference Realignment Update
    private void conferenceRealignment() {
        if (simLeague.confRealignment) {
            simLeague.conferenceRealignmentV2(this);
            if (simLeague.countRealignment > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(simLeague.newsRealignment)
                        .setTitle(simLeague.getYear() + " Conference Realignment News")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                AlertDialog dialog = builder.create(); dialog.setCancelable(false);
                showImmersive(dialog);
                TextView textView = dialog.findViewById(android.R.id.message);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                resetUI();
            }
        }
    }

    //Promotions & Relegations Update
    private void universalProRel() {
        if (simLeague.enableUnivProRel) {
            simLeague.universalProRel();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(simLeague.newsRealignment)
                    .setTitle(simLeague.getYear() + " Promotion/Relegation Update")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog dialog = builder.create(); dialog.setCancelable(false);
            showImmersive(dialog);
            TextView textView = dialog.findViewById(android.R.id.message);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            resetUI();
        }
    }

    //Television Contract News
    private void showRedshirtList() {
        StringBuilder update = new StringBuilder();
        update.append("The following is the list of players that were redshirted this season. Some players automatically received redshirts if they did not play in at least 4 games.\n\n");
        for (int i = 0; i < userTeam.redshirtList.size(); ++i) {
            update.append(userTeam.redshirtList.get(i) + "\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(update)
                .setTitle(simLeague.getYear() + " Redshirts")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }


    //Television Contract News
    private void newsTV() {
        StringBuilder update = new StringBuilder();
        for (int i = 0; i < simLeague.newsTV.size(); ++i) {
            update.append(simLeague.newsTV.get(i) + "\n\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(update)
                .setTitle(simLeague.getYear() + " Network Contract Updates")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("Budgets", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showBudget();
                    }
                });
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    //Show Prestige Change
    private void showBudget() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Budget Rankings")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Budget Landscape", "Compare which programs are operating with the strongest financial footing this cycle.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(19), userTeam.name);
        teamRankingsList.setAdapter(teamRankingsAdapter);
    }

    //Transfers Dialog
    private void transfers() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(simLeague.userTransfers)
                .setTitle(simLeague.getYear() + " Transfers")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("View All Transfers", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                        builder1.setMessage(simLeague.sumTransfers)
                                .setTitle(simLeague.getYear() + " Transfers")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                        AlertDialog dialog1 = builder1.create();
                        dialog1.show();
                        TextView textView1 = dialog1.findViewById(android.R.id.message);
                        textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    }
                });
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

    }

    //Recruiting Begins
    public void beginRecruiting() {
        simLeague.recruitPlayers();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(userTeam.abbr + " Players Leaving")
                .setPositiveButton("Recruiting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        simLeague.currentWeek = 0;
                        saveLoadService.saveForRecruiting(simLeague);


                        //Get String of user team's players and such
                        StringBuilder sb = new StringBuilder();
                        userTeam.sortPlayers();
                        sb.append(userTeam.conference + "," + userTeam.name + "," + userTeam.abbr + "," + userTeam.getUserRecruitBudget() + "," + userTeam.HC.ratTalent + "%\n");
                        sb.append(userTeam.getPlayerInfoSaveFile());
                        sb.append("END_TEAM_INFO%\n");
                        sb.append(userTeam.getRecruitsInfoSaveFile());

                        //Start Recruiting Activity
                        finish();
                        flowManager.startRecruiting(sb.toString());
                        finish();
                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        simLeague.currentWeek = 99;
                        dialog.dismiss();
                        saveLeague();
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Recruiting Launch", "Check who is leaving your roster or headed to the draft before you enter recruiting season.");
        String[] spinnerSelection = {"Players Leaving", "Pro Mock Draft"};
        Spinner beginRecruitingSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(beginRecruitingSpinner);
        ArrayAdapter<String> beginRecruitingSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerSelection);
        beginRecruitingSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        beginRecruitingSpinner.setAdapter(beginRecruitingSpinnerAdapter);

        final ListView playerList = dialog.findViewById(R.id.listViewTeamRankings);
        final PlayerProfile playerStatsAdapter =
                new PlayerProfile(this, userTeam.getGradPlayersList());
        final MockDraft mockDraftAdapter =
                new MockDraft(this, simLeague.getMockDraftPlayersList(), userTeam.name);
        playerList.setAdapter(playerStatsAdapter);

        beginRecruitingSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            // Players Leaving
                            playerList.setAdapter(playerStatsAdapter);
                        } else {
                            // Mock Draft
                            playerList.setAdapter(mockDraftAdapter);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });

    }

    @Override
    public void startRecruiting(File saveFile, Team userTeam)  throws InterruptedException, IOException {
        saveLoadService.copyToRecruitingSlot(saveFile);




        //Get String of user team's players and such
        StringBuilder sb = new StringBuilder();
        userTeam.sortPlayers();
        sb.append(userTeam.conference + "," + userTeam.name + "," + userTeam.abbr + "," + userTeam.getUserRecruitBudget() + "," + userTeam.HC.ratTalent + "%\n");
        sb.append(userTeam.getPlayerInfoSaveFile());
        sb.append("END_TEAM_INFO%\n");
        sb.append(userTeam.getRecruitsInfoSaveFile());

        //Start Recruiting Activity
        finish();
        flowManager.startRecruiting(sb.toString());
        finish();
    }

    //Recruiting Score
    private void showRecruitingClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Recruiting Class Rankings")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        seasonGoals();
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Recruiting Class Rankings", "Measure your incoming class against the rest of the country before you move on to season goals.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(17), userTeam.name + "\n" + userTeam.getTopRecruit());
        teamRankingsList.setAdapter(teamRankingsAdapter);
    }

    //Retirement vs Eternal
    private void retirementQuestion() {
        String string = "";
        string = "You have reached that time in your life when you need to decide to hang it up and retire or continue on. " +
                "At this point, if you choose to continue, your ability to increase skill ratings will be much more challenging. " +
                "You may also retire and end your career. " +
                "Finally, you can choose to reincarnate yourself as a fresh new head coach in his 30s in this same universe!";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(string)
                .setTitle("Retirement Age")
                .setPositiveButton("Continue Career", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (skipRetirementQ) contractDialog();
                        dialog.dismiss();

                    }
                })
                .setNeutralButton("Reincarnate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        reincarnation();
                        dialog.dismiss();

                    }
                })
                .setNegativeButton("Retire", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        retire();
                        dialog.dismiss();

                    }
                });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }

    private void retire() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("A brief look back...")
                .setPositiveButton("EXIT GAME", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitMainActivity();
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);

        String[] selection = {"Team History"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);

        teamHistSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            TeamHistoryList teamHistoryAdapter =
                                    new TeamHistoryList(MainActivity.this, currentTeam.HC.getCoachHistory());
                            teamHistoryList.setAdapter(teamHistoryAdapter);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }

    private void reincarnation() {
        userTeam.teamPrestige = (int)(userTeam.teamPrestige* Team.knockdownRet);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Head Coach History: " + currentTeam.HC.name)
                .setPositiveButton("Use Same Team", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userTeam.newCoachTeamChanges();
                        userHC.retired = true;
                        userHC.team = null;
                        simLeague.coachFreeAgents.add(new HeadCoach(userHC, userTeam));
                        userTeam.setupUserCoach(userHC.name);
                        newGame = true;
                        userNameDialog();
                        dialog.dismiss();

                    }
                })
                .setNeutralButton("Pick New Team", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userTeam.newCoachTeamChanges();
                        reincarnate = true;
                        userHC.retired = true;
                        userHC.team = null;
                        simLeague.coachFreeAgents.add(new HeadCoach(userHC, userTeam));

                        jobOffers(userHC);

                        newGame = true;

                        dialog.dismiss();

                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);

        String[] selection = {"Team History"};
        Spinner teamHistSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamHistSpinner);
        final ArrayAdapter<String> teamHistAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, selection);
        teamHistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamHistSpinner.setAdapter(teamHistAdapter);

        final ListView teamHistoryList = dialog.findViewById(R.id.listViewTeamRankings);

        teamHistSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            TeamHistoryList teamHistoryAdapter =
                                    new TeamHistoryList(MainActivity.this, currentTeam.HC.getCoachHistory());
                            teamHistoryList.setAdapter(teamHistoryAdapter);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });

    }

    //CUSTOM DATA


    private LeagueLaunchCoordinator.CustomUniverseFiles customLeague(String uriString) throws IOException {
        try {
            File conferences = new File(getFilesDir(), "conferences.txt");
            File teams = new File(getFilesDir(), "teams.txt");
            File bowls = new File(getFilesDir(), "bowls.txt");
            try (InputStream inputStream = openDocumentStream(uriString)) {
                return CustomUniverseParser.parse(inputStream, conferences, teams, bowls);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error! Bad URL or unable to read file.", Toast.LENGTH_SHORT).show();
            throw new IOException("Unable to import custom universe", e);
        }
    }

    private InputStream openDocumentStream(String uriString) throws IOException {
        if (uriString == null || uriString.isEmpty()) {
            throw new IOException("Missing document uri");
        }
        InputStream inputStream = getContentResolver().openInputStream(android.net.Uri.parse(uriString));
        if (inputStream == null) {
            throw new IOException("Unable to open document stream");
        }
        return inputStream;
    }

    private void importCustomData(String uriString) throws IOException {
        if (pendingImportType == null) {
            return;
        }
        try (InputStream stream = openDocumentStream(uriString)) {
            if (pendingImportType == LeagueImportFlowController.ImportType.COACH) {
                LeagueCustomDataImporter.importCoaches(stream, simLeague);
            } else if (pendingImportType == LeagueImportFlowController.ImportType.ROSTER) {
                LeagueCustomDataImporter.importRoster(stream, simLeague);
            }
        } finally {
            pendingImportType = null;
        }
        defaultScreen();
    }

    // Checks if external storage is available for read and write *//*
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    //* Checks if external storage is available to at least read *//*
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    //* Creates external Save directory *//*

    private void handleImportDocumentSelection(android.net.Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            importCustomData(uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //EXPORT DATA

    private void exportSave() {
        // Empty file, don't show dialog confirmation
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = LeagueExportController.exportPrimarySave(getExportSaveDir(), simLeague);
        Toast.makeText(MainActivity.this, "Exported Save to Storage", Toast.LENGTH_SHORT).show();
    }

    //Export Save File
    private void exportTeams() {
        // Empty file, don't show dialog confirmation
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = LeagueExportController.exportTeams(getExportSaveDir(), simLeague);
        Toast.makeText(MainActivity.this, "Saved league!", Toast.LENGTH_SHORT).show();
    }

    //Export Save File
    private void exportBowlNames() {
        // Empty file, don't show dialog confirmation
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = LeagueExportController.exportBowls(getExportSaveDir(), simLeague);
        Toast.makeText(MainActivity.this, "Saved league!", Toast.LENGTH_SHORT).show();
    }

    //Export Save File
    private void exportPlayers() {
        // Empty file, don't show dialog confirmation
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = LeagueExportController.exportPlayers(getExportSaveDir(), simLeague);
        Toast.makeText(MainActivity.this, "Saved league!", Toast.LENGTH_SHORT).show();
    }

    //Export Save File
    private void exportConferences() {
        // Empty file, don't show dialog confirmation
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = LeagueExportController.exportConferences(getExportSaveDir(), simLeague);
        Toast.makeText(MainActivity.this, "Saved league!", Toast.LENGTH_SHORT).show();
    }

    private File getExportSaveDir() {
        try {
            return LeagueSaveStorage.getExportDir(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CFBCOACH");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare export directory", ex);
        }
    }

    private LeagueLaunchCoordinator.LaunchRequest getLaunchRequest() {
        return GameNavigation.getLaunchRequest(getIntent());
    }



    //MISC STUFF

    public boolean isNameValid(String name) {
        if (name.split(" ").length < 2) {
            return false;
        }
        return !(name.contains(",") || name.contains(">") || name.contains("%") || name.contains("\\"));
    }

    @Override
    public void onBackPressed() {
        if(currPage == 0) exitMainActivity();
        else showHome();
    }


    //GAME EDITOR V2

    public void gameEditorV2() {
        LeagueEditorDialogController.show(this, simLeague);
    }


    private void fixBowlNames() {
        String[] bowls = simLeague.bowlNamesText.split(",");
        simLeague.bowlNames = new String[bowls.length];
        for(int i = 0; i < bowls.length; i++) {
            simLeague.bowlNames[i] = bowls[i];
        }
    }

    //allow the ability to enable editor to edit player names, positions, attributes, etc.
    private void playerEditor() {

    }

    public void userHallofFame() {
        //Retirement Hall of Fame

    }

    private void disciplineSetup() {
        userTeam.suspendPlayerSetup(this);
    }

    @Override
    public void disciplineAction(final Player player, final String issue, final int gamesA, final int gamesB) {
        DisciplineDialogController.showDisciplineAction(this, player, issue, gamesA, gamesB, userTeam);
    }

    @Override
    public void transferPlayer(final Player p) {
        PlayerProfileSnapshot snapshot = PlayerProfileSnapshot.fromPlayer(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accept/Decline " + p.getTransferStatusMessage() + " Request\n")
                .setView(getLayoutInflater().inflate(R.layout.player_profile, null));


        builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform action on click
                simLeague.userTransfers = simLeague.userTransfers + p.position + " " + p.name + " " + p.getYrStr() + " Ovr: " + p.ratOvr + " (" + p.team.name + ")\n";
                simLeague.sumTransfers = simLeague.sumTransfers + p.ratOvr + " " + p.position + " " + p.name + " [" + p.getTransferStatus() + "] " + userTeam.name + " (" + p.team.abbr + ")";
                p.team = userTeam;
                userTeam.addPlayer(p);
                //refresh homepage
                resetUI();
            }
        });
        builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform action on click
                //refresh homepage
                p.isTransfer = false;
                p.team.addPlayer(p);
                resetUI();
            }
        });


        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        bindPlayerProfile(dialog, p.name, snapshot);
    }

    @Override
    public void crash() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("The DATABASE is invalid or corrupt. Please check for formatting or spelling errors.")
                .setPositiveButton("Exit to Main Screen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Actually go back to main menu
                        finish();
                        flowManager.returnToMainHub();
                        finish();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
    }

    private void openGameWindow() {
        AlertDialog.Builder GameViewer = new AlertDialog.Builder(this);
        GameViewer.setTitle("Game Viewer")
                .setView(getLayoutInflater().inflate(R.layout.playwindow, null));
        final AlertDialog dialog = GameViewer.create();

        GameViewer.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform action on click

            }
        });

        TextView teamScores = findViewById(R.id.playScore);
        TextView playbyplay = findViewById(R.id.playPBP);
        showImmersive(dialog);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public void showImmersive(AlertDialog alert) {
        PlatformUiHelper.showImmersive(alert);
    }

    //DEBUG
    private void showFreeAgents() {
        String msg = simLeague.getFreeAgentCoachList();
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(msg)
                .setTitle("Coach Free Agent List")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        TextView textView = dialog.findViewById(android.R.id.message);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    }
}
