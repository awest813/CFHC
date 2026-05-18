package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import positions.Player;
import simulation.Conference;

import simulation.CustomUniverseParser;
import simulation.AudioEvent;
import simulation.Game;
import simulation.GameUiBridge;
import simulation.League;
import simulation.LeagueCustomDataImporter;
import simulation.LeagueExportController;
import simulation.LeagueImportWorkflow;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueSaveStorage;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.SimulationFacade;
import simulation.Team;
import staff.HeadCoach;
import staff.Staff;
import ui.MockDraft;
import ui.PlayerProfile;
import ui.SaveFilesList;
import ui.TeamRankingsList;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, GameUiBridge, LeagueImportFlowController.Host {
    private HeadCoach userHC;
    private int season;
    public League simLeague;
    private simulation.SeasonController seasonController;

    private Conference currentConference;
    private Team currentTeam;
    Team userTeam;
    private File saveLeagueFile;
    private String username;
    private LeagueImportWorkflow.ImportType pendingImportType;
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
    boolean redshirtComplete;
    private boolean newGame;
    private boolean skipRetirementQ;
    private boolean reincarnate;

    //Universe Settings
    private final int seasonStart = 2026;
    private final int retireAge = 67;

    private final DecimalFormat df2 = new DecimalFormat("#.##");

    public int theme;

    private boolean loadedLeague = false;
    private final ActivityResultLauncher<String[]> importDocumentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportDocumentSelection);

    private simulation.GameFlowManager flowManager;
    private AndroidAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = GameNavigation.getTheme(getIntent(), 1);
        flowManager = new AndroidGameFlowManager(this, theme);
        audioManager = new AndroidAudioManager(this);
        saveLoadService = new simulation.SaveLoadService(getFilesDir());
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
                navTeam.setText("#" + currentTeam.getRankTeamPollScore() +
                        " " + currentTeam.getName() + " (" + currentTeam.getWins() + "-" + currentTeam.getLosses() + ") " +
                        currentTeam.getConfChampion() + " " + currentTeam.semiFinalWL + currentTeam.natChampWL);
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
                userTeam = simLeague.getTeamList().get(0);
                simLeague.userTeam = userTeam;
                userTeam.setUserControlled(true);
                userTeamStr = userTeam.getName();
                currentTeam = simLeague.getTeamList().get(0);
                currentConference = simLeague.getConferences().get(0);

                LeagueLaunchCoordinator.LaunchRequest launchRequest = getLaunchRequest();
                if (launchRequest != null && launchRequest.isCustomUniverse()) importDataPrompt();
                else careerModeOptions();
            }

        } catch (Exception ex) {
            PlatformLog.e("MainActivity", "Error reading file", ex);
            crash();
            return;
        }

        // Set toolbar text
        updateHeaderBar();


        //Set up spinner for examining team.
        examineConfSpinner = findViewById(R.id.examineConfSpinner);
        PlatformUiHelper.avoidSpinnerDropdownFocus(examineConfSpinner);

        confList = new ArrayList<>();
        for (int i = 0; i < simLeague.getConferences().size(); i++) {
            if(simLeague.getConferences().get(i).confTeams.size() > 0) confList.add(simLeague.getConferences().get(i).confName);
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
        for (int i = 0; i < simLeague.getTeamList().size(); i++) {
            teamList.add(simLeague.getTeamList().get(i).strRep());
        }

        dataAdapterTeam = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, teamList);
        dataAdapterTeam.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        examineTeamSpinner.setAdapter(dataAdapterTeam);
        examineTeamSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Team picked = simLeague.findTeam(parent.getItemAtPosition(position).toString());
                        if (picked != null) {
                            currentTeam = picked;
                            updateCurrTeam();
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        //Team Depth Chart Button
        Button depthchartButton = findViewById(R.id.buttonDepthChart);
        if (!redshirtComplete) {
            depthchartButton.setText("REDSHIRT");
            depthchartButton.setBackgroundResource(R.drawable.bg_action_danger);
            depthchartButton.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
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
        strategyButton.setBackgroundResource(R.drawable.bg_action_secondary);
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
            examineTeam(userTeam.getName());
        }

        if (simLeague.getYear() != seasonStart) {
            // Only show recruiting classes if not season 1
            showRecruitingClassDialog();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                    return;
                }
                if (currPage == 0) {
                    exitMainActivity();
                } else {
                    showHome();
                }
            }
        });
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        audioManager.play(AudioEvent.UI_CLICK);
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            currentTeam = userTeam;
            examineTeam(currentTeam.getName());
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
            currPage = 6;
            showWeeklyScores();
        } else if (id == R.id.nav_standings) {
            currPage = 7;
            updateStandings();
        } else if (id == R.id.nav_rankings) {
            currPage = 8;
            updateRankings();
        } else if (id == R.id.nav_leagueteamstats) {
            currPage = 9;
            showTeamRankingsDialog();
        } else if (id == R.id.nav_leagueplayerstats) {
            currPage = 10;
            showPlayerRankingsDialog();
        } else if (id == R.id.nav_awards) {
            currPage = 11;
            showLeagueAwards();
        } else if (id == R.id.nav_postseason) {
            currPage = 12;
            showBowlCCGDialog();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openHomeView(View view) {
        currentTeam = userTeam;
        examineTeam(currentTeam.getName());
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
            seasonController = new simulation.SeasonController(simLeague, this);

            loadedLeague = result.loadedLeague;
            newGame = result.newGame;

            if (result.userTeam != null) {
                userTeam = result.userTeam;
                userTeamStr = userTeam.getName();
            }
            if (result.currentTeam != null) {
                currentTeam = result.currentTeam;
            }
            if (result.showSeasonGoals) {
                seasonGoals();
            }
        } catch (Exception ex) {
            PlatformLog.e("MainActivity", "Error reading file", ex);
            crash();
        }
    }

    //Update Header Bar
    void updateHeaderBar() {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(currentTeam.getName());
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

    void selectTeam() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Your Program");
        builder.setMessage("Pick the school where your " + seasonStart + " head coaching run begins.");
        final String[] teams = simLeague.getTeamListStr();
        builder.setItems(teams, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                if (simLeague.getTeamList().get(item).getHeadCoach() != null) {
                    simLeague.getTeamList().get(item).getHeadCoach().team = null;
                    simLeague.getCoachFreeAgents().add(simLeague.getTeamList().get(item).getHeadCoach());
                }
                userTeam.setUserControlled(false);
                userTeam = simLeague.getTeamList().get(item);
                simLeague.userTeam = userTeam;
                userTeam.setUserControlled(true);
                userTeamStr = userTeam.getName();
                currentTeam = userTeam;
                userNameDialog();
                // set rankings so that not everyone is rank #0
                simLeague.setTeamRanks();
                simLeague.setTeamBenchMarks();
                simLeague.updateTeamTalentRatings();
                userHC = userTeam.getHeadCoach();
                // Set toolbar text to '2017 Season' etc
                updateHeaderBar();
                examineTeam(currentTeam.getName());
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
                .setView(getLayoutInflater().inflate(R.layout.username_dialog, null, false));
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
                    username = newHC;
                    userTeam.setupUserCoach(username);
                    HeadCoach coach = ensureUserHeadCoach();
                    if (coach != null) {
                        coach.name = newHC;
                    }
                    examineTeam(currentTeam.getName());
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
        HeadCoach coach = ensureUserHeadCoach();
        if (coach == null) return;
        coach.ratOff = simLeague.getAvgCoachOff()+5;
        coach.ratDef = simLeague.getAvgCoachDef()-5;
        coach.ratOvr = coach.getStaffOverall(coach.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachDef() {
        HeadCoach coach = ensureUserHeadCoach();
        if (coach == null) return;
        coach.ratOff = simLeague.getAvgCoachOff()-5;
        coach.ratDef = simLeague.getAvgCoachDef()+5;
        coach.ratOvr = coach.getStaffOverall(coach.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachBal() {
        HeadCoach coach = ensureUserHeadCoach();
        if (coach == null) return;
        coach.ratOff = simLeague.getAvgCoachOff();
        coach.ratDef = simLeague.getAvgCoachDef();
        coach.ratOvr = coach.getStaffOverall(coach.overallWt);
        setupPlaybookOff();
    }

    private void setupCoachHard() {
        HeadCoach coach = ensureUserHeadCoach();
        if (coach == null) return;
        coach.ratOff = 50;
        coach.ratDef = 50;
        coach.ratDiscipline = 60;
        coach.ratTalent = 50;
        coach.ratOvr = coach.getStaffOverall(coach.overallWt);
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
                        HeadCoach coach = ensureUserHeadCoach();
                        if (coach == null) return;
                        coach.offStrat = i;
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
                        HeadCoach coach = ensureUserHeadCoach();
                        if (coach == null) return;
                        coach.defStrat = i;
                        if(userTeam.OC != null) userTeam.OC.defStrat = i;
                        if(userTeam.DC != null) userTeam.DC.defStrat = i;
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
        examineTeam(userTeam.getName());
        showHome();
    }

    private HeadCoach ensureUserHeadCoach() {
        if (userTeam == null) {
            return null;
        }
        HeadCoach coach = userTeam.getHeadCoach();
        if (coach == null) {
            String coachName = username != null && !username.trim().isEmpty()
                    ? username.trim()
                    : simLeague.getRandName();
            userTeam.setupUserCoach(coachName);
            coach = userTeam.getHeadCoach();
        }
        userHC = coach;
        return coach;
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
        } else if (currPage == 7) {
            currPage = 7;
            updateStandings();
        } else if (currPage == 8) {
            currPage = 8;
            updateRankings();
        } else {
            currPage = 0;
            showHome();
        }
    }

    private void rebuildTeamSpinner() {
        teamList = new ArrayList<>();
        dataAdapterTeam.clear();
        for (int i = 0; i < currentConference.confTeams.size(); i++) {
            teamList.add(currentConference.confTeams.get(i).strRep());
            dataAdapterTeam.add(teamList.get(i));
        }
        dataAdapterTeam.notifyDataSetChanged();
    }

    private void rebuildConfSpinner() {
        confList.clear();
        for (int i = 0; i < simLeague.getConferences().size(); i++) {
            if (simLeague.getConferences().get(i).confTeams.size() > 0) {
                confList.add(simLeague.getConferences().get(i).confName);
            }
        }
        dataAdapterConf.notifyDataSetChanged();
    }

    @Override
    public void updateSpinners() {
        rebuildConfSpinner();
        rebuildTeamSpinner();
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
        Team tempT = simLeague.getTeamList().get(0);
        for (Team t : simLeague.getTeamList()) {
            if (t.getName().equals(teamName)) {
                currentTeam = t;
                tempT = t;
                break;
            }
        }
        // Find conference
        for (int i = 0; i < simLeague.getConferences().size(); ++i) {
            Conference c = simLeague.getConferences().get(i);
            if (c.confName.equals(currentTeam.getConference())) {
                if (c == currentConference) wantUpdateConf = true;
                currentConference = c;
                examineConfSpinner.setSelection(i);
                break;
            }
        }

        rebuildTeamSpinner();

        for (int i = 0; i < currentConference.confTeams.size(); ++i) {
            String[] spinnerSplit = dataAdapterTeam.getItem(i).split(" ");
            if (spinnerSplit.length >= 2) {
                String teamNameFromSpinner = String.join(" ",
                        java.util.Arrays.copyOfRange(spinnerSplit, 1, spinnerSplit.length));
                if (teamNameFromSpinner.equals(tempT.getName())) {
                    examineTeamSpinner.setSelection(i);
                    currentTeam = tempT;
                    break;
                }
            }
        }

    }

    void updateCurrTeam() {
        rebuildTeamSpinner();
        updateHeaderBar();
        resetUI();
    }

    void updateCurrConference() {
        rebuildConfSpinner();

        if (wantUpdateConf) {
            rebuildTeamSpinner();
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
        HomeScreenController.show(this, mainList, currentTeam, simLeague);
    }

    //News Display
    public void showNewsStoriesDialog() {
        NewsDialogController.show(this, simLeague);
    }

    //Team Stats
    public void updateTeamStats() {
        TeamStatsScreenController.show(this, mainList, currentTeam);
    }

    //Player Stats
    private void showTeamPlayerStats() {
        PlayerStatsScreenController.show(this, mainList, currentTeam);
    }

    //Roster 2.0
    public void viewRoster() {
        RosterScreenController.show(this, mainList, currentTeam, simLeague.currentWeek);
    }

    //Open Player Profile
    public void examinePlayer(String player) {
        Player p = currentTeam.findTeamPlayer(player);
        if (p != null) {
            PlayerProfileDialogController.showProfile(this, p, userTeam);
        }
    }

    public void examinePlayerandTeam(String player, String teamAbbr) {
        Team tempTeam = simLeague.findTeamAbbr(teamAbbr);
        if (tempTeam == null) {
            return;
        }
        Player p = tempTeam.findTeamPlayer(player);
        if (p != null) {
            PlayerProfileDialogController.showProfile(this, p, userTeam);
        }
    }

    public void openPlayerProfile(final Player p) {
        PlayerProfileDialogController.showProfile(this, p, userTeam);
    }

    //Open Head Coach Profile from Database
    public void examineCoachDB(String player) {
        final Staff p = findCoachProfile(player);
        if (p != null) {
            CoachProfileDialogController.showProfile(this, p, () -> showCoachHistoryDialog(p));
        }
    }

    public Staff findCoachProfile(String name) {
        Staff p = null;
        String[] nameSplit = name.split(" ");
        if (nameSplit.length < 2) return p;
        String nameHC = nameSplit[0] + " " + nameSplit[1];
        for(int i = 0; i < simLeague.getTeamList().size(); i++) {
            if(simLeague.getTeamList().get(i).getHeadCoach() != null && simLeague.getTeamList().get(i).getHeadCoach().name.equals(nameHC)) return simLeague.getTeamList().get(i).getHeadCoach();
            if(simLeague.getTeamList().get(i).OC != null && simLeague.getTeamList().get(i).OC.getName().equals(nameHC)) return simLeague.getTeamList().get(i).OC;
            if(simLeague.getTeamList().get(i).DC != null && simLeague.getTeamList().get(i).DC.getName().equals(nameHC)) return simLeague.getTeamList().get(i).DC;

        }

        for(int i = 0; i < simLeague.getCoachDatabase().size(); i++) {
            if(simLeague.getCoachDatabase().get(i).name.equals(nameHC)) return simLeague.getCoachDatabase().get(i);
        }
        return p;
    }

    public void openCoachProfile(final Staff p) {
        CoachProfileDialogController.showProfile(this, p, () -> showCoachHistoryDialog(p));
    }






    //Player Awards for Bio
    public int checkAwardPlayer(String player) {
        return PlayerProfileDialogController.checkAward(currentTeam, player);
    }

    //Schedule
    public void updateSchedule() {
        ScheduleScreenController.show(this, mainList, currentTeam);
    }

    //Game Summary
    public void showGameDialog(Game g) {
        playGameSound(g);
        GameDialogController.show(this, g, userTeam);
    }

    private void playGameSound(Game g) {
        if (g == null || !g.hasPlayed) return;
        boolean isUserGame = userTeam != null && (g.awayTeam == userTeam || g.homeTeam == userTeam);
        if (!isUserGame) return;
        int userScore = g.homeTeam == userTeam ? g.homeScore : g.awayScore;
        int oppScore = g.homeTeam == userTeam ? g.awayScore : g.homeScore;
        if (userScore > oppScore) {
            audioManager.play(AudioEvent.WIN);
        } else {
            audioManager.play(AudioEvent.LOSS);
        }
    }

    //Weekly Scoreboard
    private void showWeeklyScores() {
        WeeklyScoresDialogController.show(this, simLeague);
    }

    // Shows Conference Standings
    public void updateStandings() {
        StandingsScreenController.showStandings(this, mainList, simLeague, userTeam);
    }

    // Shows AP Polls
    public void updateRankings() {
        StandingsScreenController.showRankings(this, mainList, simLeague, userTeam);
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
        audioManager.play(AudioEvent.PLAY_SELECT);
        TeamStrategyDialogController.show(this, userTeam);
    }

    //Simulate Week
    private void simulateWeek() {
        audioManager.play(AudioEvent.ADVANCE);
        if (seasonController != null) {
            seasonController.advanceWeek();
        }

        if (userTeam != null && userTeam.disciplineAction) {
            disciplineSetup();
        }
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
        if (id == R.id.action_coach_program) {
            CoachProgramDialogController.show(this, userTeam);
        } else if (id == R.id.action_current_team_history) {

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
        HistoryDialogController.showTop25History(this, simLeague, seasonStart);
    }

    private void showCurrTeamHistoryDialog() {
        HistoryDialogController.showCurrTeamHistoryDialog(this, currentTeam, userTeam, simLeague, seasonStart);
    }

    private void showCoachHistoryDialog(Staff p) {
        HistoryDialogController.showCoachHistoryDialog(this, p, simLeague, seasonStart);
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
        PostseasonDialogController.show(this, simLeague);
    }

    //Awards Nav Menu
    private void showLeagueAwards() {
        AwardsDialogController.show(this, simLeague, userTeam);
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

    private void setDialogMessageTextSize(AlertDialog dialog) {
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    @Override
    public void requestImportDocument(LeagueImportWorkflow.ImportType type) {
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
                    boolean saved = saveLoadService.saveToSlot(simLeague, itemy);
                    if (saved) audioManager.play(AudioEvent.CONFIRM);
                    Toast.makeText(MainActivity.this,
                            saved ? "Saved league!" : "Error: Failed to save league!",
                            saved ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    // Ask for confirmation to overwrite file
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Are you sure you want to overwrite this save file?\n\n" + fileInfos[itemy])
                            .setPositiveButton("Yes, Overwrite", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    boolean saved = saveLoadService.saveToSlot(simLeague, itemy);
                                    if (saved) audioManager.play(AudioEvent.CONFIRM);
                                    Toast.makeText(MainActivity.this,
                                            saved ? "Saved league!" : "Error: Failed to save league!",
                                            saved ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
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
                    setDialogMessageTextSize(dialog2);
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

    void universalProRelAction() {

        // Perform action on click
        simLeague.enableUnivProRel = true;
        simLeague.confRealignment = false;
        simLeague.convertUnivProRel();
        updateCurrConference();
        updateCurrTeam();
        examineTeam(userTeam.getName());

    }

    //Pre-Season Goals
    private void seasonGoals() {
        simLeague.updateTeamTalentRatings();
        simLeague.setTeamBenchMarks();

        goals = "";
        int confPos = 0;

        for (int i = 0; i < simLeague.getConferences().size(); ++i) {
            Conference c = simLeague.getConferences().get(i);
            if (c.confName.equals(userTeam.getConference())) {
                for (int x = 0; x < c.confTeams.size(); x++) {
                    if (c.confTeams.get(x).name.equals(userTeam.getName())) {
                        confPos = x + 1;
                        break;
                    }
                }
            }
        }

        HeadCoach coach = ensureUserHeadCoach();
        String coachName = coach != null ? coach.name : "Coach";
        goals = "Welcome to the " + simLeague.getYear() + " College Football season, Coach " + coachName + "!\n\n";
        if (simLeague.isCareerMode()) {
            goals += "Your head coaching career begins at " + userTeam.getName() + ". Job security, performance swings, and future opportunities will all respond to the seasons you build here.\n\n";
        }
        if (simLeague.expPlayoffs) {
            goals += "This universe is using the 12-team playoff, so a strong finish can still open a path to the national title even if your team starts outside the top four.\n\n";
        } else {
            goals += "This universe is using the classic four-team playoff, so every loss near the top of the rankings carries extra weight.\n\n";
        }
        goals += "This season your team is projected to finish ranked #" + userTeam.projectedPollRank + "!\n\n";

        int num = (int)(simLeague.getTeamList().size()*.875);
        if (userTeam.projectedPollRank > num) {
            goals += "Despite being projected at #" + userTeam.projectedPollRank + ", your goal is to finish in the Top " + num + ".\n\n";
        }

        goals += "In conference play, your team is expected to finish " + userTeam.getRankStr(confPos) + " in the " + userTeam.getConference() + " conference.\n\n";

        int games = 0;
        for(Game g : userTeam.getGameSchedule()) {
            if (g.gameName.equals("OOC") || g.gameName.equals("Conference") || g.gameName.equals("Division")) {
                games++;
            }
        }

        goals += "Based on your schedule, your team is projected to finish with a record of " + userTeam.projectedWins + " - " + (games - userTeam.projectedWins) + ".\n\n";

        if (simLeague.isCareerMode() && coach != null) {
            int yearsLeft = coach.contractLength - coach.contractYear;
            if (yearsLeft < 0) yearsLeft = 0;
            goals += "Contract outlook: " + yearsLeft + " year(s) remain on your deal, and your current AD pressure is " + coach.coachStatus() + ".\n\n";
        }

        if (simLeague.getYear() > seasonStart) {
            if (userTeam.isBowlBan()) {
                goals += "Your team was penalized heavily for off-season issues by the College Athletic Administration and will lose Prestige and suffer a post-season bowl ban this year.\n\n";
            }
            if (userTeam.penalized) {
                goals += "Your team had a minor infraction over the off-season and lost some Prestige.\n\n";
            }
        }

        if (simLeague.getYear() > seasonStart) {
            if (userTeam.facilityUpgrade) {
                goals += "Your team upgraded the training facilities this off-season to Level " + userTeam.getTeamFacilities() + " which added an additional " + userTeam.getTeamFacilities() + " prestige points!\n\n";
            }
        }

        simLeague.getNewsStories().get(simLeague.currentWeek).add("Season Goals>" + goals);
        showSeasonGoalsDialog();

    }

    private void showSeasonGoalsDialog() {
        CareerDialogController.showSeasonGoalsDialog(this, goals, this::saveLeague);
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
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        setDialogMessageTextSize(dialog);
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
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        setDialogMessageTextSize(dialog);
        userTeam.suspension = false;
    }

    //mid-season summary
    private void midseasonSummary() {
        simLeague.midSeasonProgression();
        String string = userTeam.midseasonUserProgression();
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
        setDialogMessageTextSize(dialog);
    }

    //End of Season Summary
    private void seasonSummary() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(simLeague.seasonSummaryStr() + "\n\nNote: You can always review your season summary in the Offseason News.")
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
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        setDialogMessageTextSize(dialog);

        simLeague.getNewsStories().get(simLeague.currentWeek + 1).add("Season Summary>" + simLeague.seasonSummaryStr());
        simLeague.getNewsHeadlines().add("That wraps up the " + simLeague.getYear() + " Season");
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
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Prestige Movement", "See which programs are rising and falling across the current college football landscape.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(1), userTeam.getName());
        teamRankingsList.setAdapter(teamRankingsAdapter);
    }

    //Contract Status Dialog
    private void contractDialog() {
        if (userHC == null) return;
        if (simLeague.isCareerMode()) {
            if (userHC.age > retireAge) {
                userHC.retirement = true;
            }
        }

        if (userHC.retirement && !skipRetirementQ && !simLeague.neverRetire) {
            retirementQuestion();
            skipRetirementQ = true;

        } else {
            CareerDialogController.showContractDialog(this,
                    userTeam.getContractString(), simLeague.getYear() + " Contract Status",
                    () -> { if (goals == null || goals.isEmpty()) seasonGoals(); },
                    this::showSeasonGoalsDialog,
                    this::showNewsStoriesDialog);
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
        if(userHC.team != null) oldTeam = userHC.team.getName();
        updateHeaderBar();
        jobList = simLeague.getCoachListFired(ratOvr, oldTeam);
        String[] teams = CareerDialogController.buildJobTeamList(jobList, userHC, df2);

        if (jobList.isEmpty()) {
            CareerDialogController.showJobOffersEmpty(this, "Head Coach Opportunities",
                    "No immediate head coach openings are available right now. You will remain on the market until a program makes a move.");
        } else {
            CareerDialogController.showJobOffersDialog(this, "Head Coach Opportunities",
                    "Review each opening before you decide. Every move resets your contract clock and ties your new expectations to that program's prestige.",
                    teams, item -> viewTeam(jobList, item));
        }
    }

    //Job offers from other teams
    private void promotions(HeadCoach headCoach) {
        jobType  = 2;
        jobListSet = true;
        jobList.clear();

        userHC = headCoach;
        if (userHC.promotionCandidate) {
            HeadCoach currentCoach = userTeam != null ? userTeam.getHeadCoach() : null;
            int[] overallWeights = currentCoach != null ? currentCoach.overallWt : userHC.overallWt;
            int ratOvr = userHC.getStaffOverall(overallWeights);
            if (ratOvr < 40) ratOvr = 40;
            double offers = 2;
            String oldTeam = "NO TEAM";
            if(userHC.team != null) oldTeam = userHC.team.getName();
            updateHeaderBar();
            jobList = simLeague.getCoachPromotionList(ratOvr, offers, oldTeam);
            String[] teams = CareerDialogController.buildJobTeamList(jobList, userHC, df2);

            if (jobList.isEmpty()) {
                userHC.promotionCandidate = false;
                CareerDialogController.showJobOffersEmpty(this, "Head Coach Opportunities",
                        "No job offers came through this cycle. Build on this season and stronger offers should return if you keep climbing.");
            } else {
                CareerDialogController.showPromotionsDialog(this,
                        "Career Advancement Opportunities",
                        "Programs are interested in your work. Review the roster foundation, prestige, and expectations before making your move.",
                        teams, item -> viewTeam(jobList, item),
                        () -> userHC.promotionCandidate = false);
            }
        } else {
            CareerDialogController.showJobOffersEmpty(this, "Career Advancement Opportunities",
                    "No job offers are available this offseason. Your program did not build enough momentum to put you on other schools' short lists.");
        }
    }

    //Choose ANY team (manually change from Options Menu)
    private void selectNewTeam(HeadCoach headCoach) {
        jobType  = 0;
        jobListSet = true;
        jobList.clear();

        userHC = headCoach;
        updateHeaderBar();
        jobList = new ArrayList<>(simLeague.getTeamList());
        CareerDialogController.showSelectNewTeamDialog(this, simLeague.getTeamListStr(),
                item -> viewTeam(jobList, item));
    }

    //View Team prior to choosing
    private void viewTeam(final ArrayList<Team> teamList, final int item) {
        final Team selectedTeam = teamList.get(item);
        String summary = CareerDialogController.buildJobOpportunitySummary(selectedTeam, userHC, df2);

        CareerDialogController.showTeamReviewDialog(this, selectedTeam, summary,
                () -> {
                    if (jobType == 2) promotions(userHC);
                    else if (jobType == 1) jobOffers(userHC);
                    else selectNewTeam(userHC);
                },
                () -> {
                    userHC.promotionCandidate = false;
                    changeTeams(jobList, item);
                    if (jobType == 2) simLeague.coachCarousel();
                });
    }


    //Method to actually switch teams
    private void changeTeams(ArrayList<Team> teamList, int item) {
        userTeam.newCoachTeamChanges();
        userTeam.setUserControlled(false);
        userTeam.setHeadCoach(null);
        simLeague.coachHiringSingleTeam(userTeam);
        simLeague.newJobtransfer(teamList.get(item).name);
        userTeam = simLeague.userTeam;
        userTeamStr = userTeam.getName();
        currentTeam = userTeam;
        userTeam.setHeadCoach(null);

        if(reincarnate) {
            userTeam.setupUserCoach(userHC.getName());
            userHC = userTeam.getHeadCoach();
            reincarnate = false;
            userNameDialog();
        } else {
            userTeam.setHeadCoach(userHC);
        }

        userHC.team = userTeam;
        userTeam.fired = false;
        userHC.contractYear = 0;
        userHC.contractLength = 6;
        userHC.baselinePrestige = userTeam.getTeamPrestige();
        userHC.promotionCandidate = false;
        simLeague.getNewsStories().get(simLeague.currentWeek + 1).add("Coaching Hire: " + currentTeam.getName() + ">After an extensive search for a new head coach, " + currentTeam.getName() + " has hired " + userHC.getName() +
                " to lead the team.");
        updateHeaderBar();
        examineTeam(currentTeam.getName());
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
    private void showHireCoordinatorDialog(
            final boolean isOffense,
            final boolean isNewTeam) {
        final ArrayList<Staff> list = isOffense
                ? simLeague.getOCList(userTeam.getHeadCoach())
                : simLeague.getDCList(userTeam.getHeadCoach());
        CareerDialogController.showHireCoordinatorDialog(this, isOffense, isNewTeam,
                list, userTeam, simLeague, this::resetUI);
    }

    public void hireOC() {
        showHireCoordinatorDialog(true, false);
    }

    public void hireDC() {
        showHireCoordinatorDialog(false, false);
    }

    public void hireOCNewTeam() {
        showHireCoordinatorDialog(true, true);
    }

    public void hireDCNewTeam() {
        showHireCoordinatorDialog(false, true);
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
                setDialogMessageTextSize(dialog);
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
            setDialogMessageTextSize(dialog);
            resetUI();
        }
    }

    //Television Contract News
    private void showRedshirtListFix() {
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
        setDialogMessageTextSize(dialog);
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
        setDialogMessageTextSize(dialog);
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
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Budget Landscape", "Compare which programs are operating with the strongest financial footing this cycle.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(19), userTeam.getName());
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
        setDialogMessageTextSize(dialog);

    }

    //Recruiting Begins
    public void beginRecruiting() {
        simLeague.recruitPlayers();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(userTeam.getAbbr() + " Players Leaving")
                .setPositiveButton("Recruiting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        simLeague.currentWeek = 0;
                        saveLoadService.saveForRecruiting(simLeague);


                        //Get String of user team's players and such
                        StringBuilder sb = new StringBuilder();
                        userTeam.sortPlayers();
                        HeadCoach coach = ensureUserHeadCoach();
                        int recruitingRating = coach != null ? coach.ratTalent : simLeague.getAvgCoachTal();
                        sb.append(userTeam.getConference() + "," + userTeam.getName() + "," + userTeam.getAbbr() + "," + userTeam.getUserRecruitBudget() + "," + recruitingRating + "," + userTeam.nilCollectiveLevel + "," + userTeam.teamFacilities + "%\n");
                        sb.append(userTeam.getPlayerInfoSaveFile());
                        sb.append("END_TEAM_INFO%\n");
                        sb.append(userTeam.getRecruitsInfoSaveFile());

                        //Start Recruiting Activity
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
                .setView(getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
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
                new MockDraft(this, simLeague.getMockDraftPlayersList(), userTeam.getName());
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




        //Start Recruiting Activity
        finish();
        flowManager.startRecruiting(SimulationFacade.buildRecruitingPayload(userTeam));
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
                .setView(getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Recruiting Class Rankings", "Measure your incoming class against the rest of the country before you move on to season goals.");

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(this, simLeague.getTeamRankingsStr(17), userTeam.getName());
        teamRankingsList.setAdapter(teamRankingsAdapter);
    }

    //Retirement vs Eternal
    private void retirementQuestion() {
        CareerDialogController.showRetirementQuestion(this,
                () -> { if (skipRetirementQ) contractDialog(); },
                this::reincarnation,
                this::retire);
    }

    private void retire() {
        CareerDialogController.showRetireSummary(this, currentTeam, this::exitMainActivity);
    }

    private void reincarnation() {
        userTeam.setTeamPrestige((int)(userTeam.getTeamPrestige() * Team.knockdownRet));
        CareerDialogController.showReincarnationDialog(this, userTeam, currentTeam,
                () -> {
                    userTeam.newCoachTeamChanges();
                    userHC.retired = true;
                    userHC.team = null;
                    simLeague.getCoachFreeAgents().add(new HeadCoach(userHC, userTeam));
                    userTeam.setupUserCoach(userHC.getName());
                    newGame = true;
                    userNameDialog();
                },
                () -> {
                    userTeam.newCoachTeamChanges();
                    reincarnate = true;
                    userHC.retired = true;
                    userHC.team = null;
                    simLeague.getCoachFreeAgents().add(new HeadCoach(userHC, userTeam));
                    jobOffers(userHC);
                    newGame = true;
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
            if (pendingImportType == LeagueImportWorkflow.ImportType.COACH) {
                LeagueCustomDataImporter.importCoaches(stream, simLeague);
            } else if (pendingImportType == LeagueImportWorkflow.ImportType.ROSTER) {
                LeagueCustomDataImporter.importRoster(stream, simLeague);
            }
        } finally {
            pendingImportType = null;
        }
        defaultScreen();
    }

    // Checks if external storage is available for read and write.
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Checks if external storage is available to at least read.
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    // Creates external save directory when exporting (legacy paths).

    private void handleImportDocumentSelection(android.net.Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            importCustomData(uri.toString());
        } catch (IOException e) {
            PlatformLog.e("MainActivity", "Import from document failed", e);
            Toast.makeText(this, "Unable to import file.", Toast.LENGTH_SHORT).show();
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
    private void exportData(java.util.function.Function<League, File> exporter) {
        isExternalStorageReadable();
        isExternalStorageWritable();
        saveLeagueFile = exporter.apply(simLeague);
        Toast.makeText(MainActivity.this, "Saved league!", Toast.LENGTH_SHORT).show();
    }

    private void exportTeams() {
        exportData(league -> LeagueExportController.exportTeams(getExportSaveDir(), league));
    }

    private void exportBowlNames() {
        exportData(league -> LeagueExportController.exportBowls(getExportSaveDir(), league));
    }

    private void exportPlayers() {
        exportData(league -> LeagueExportController.exportPlayers(getExportSaveDir(), league));
    }

    private void exportConferences() {
        exportData(league -> LeagueExportController.exportConferences(getExportSaveDir(), league));
    }

    private File getExportSaveDir() {
        try {
            return LeagueSaveStorage.getExportDir(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CFHC");
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

    //GAME EDITOR V2

    public void gameEditorV2() {
        LeagueEditorDialogController.show(this, simLeague);
    }


    void fixBowlNames() {
        simLeague.bowlNames = simLeague.bowlNamesText.split(",");
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
                .setView(getLayoutInflater().inflate(R.layout.player_profile, null, false));


        builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform action on click
                simLeague.userTransfers = simLeague.userTransfers + p.position + " " + p.getName() + " " + p.getYrStr() + " Ovr: " + p.ratOvr + " (" + p.team.getName() + ")\n";
                simLeague.sumTransfers = simLeague.sumTransfers + p.ratOvr + " " + p.position + " " + p.getName() + " [" + p.getTransferStatus() + "] " + userTeam.getName() + " (" + p.team.getAbbr() + ")";
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
        PlayerProfileDialogController.bindProfile(dialog, p.getName(), snapshot);
    }

    @Override
    public void crash() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("The DATABASE is invalid or corrupt. Please check for formatting or spelling errors.")
                .setPositiveButton("Exit to Main Screen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Actually go back to main menu
                        flowManager.returnToMainHub();
                        finish();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create(); dialog.setCancelable(false);
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
        setDialogMessageTextSize(dialog);
    }

    @Override
    protected void onDestroy() {
        if (audioManager != null) {
            audioManager.dispose();
        }
        super.onDestroy();
    }
}
