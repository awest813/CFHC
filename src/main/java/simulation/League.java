package simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import comparator.CompCoachAllAmericans;
import comparator.CompCoachAllConference;
import comparator.CompCoachBowlWins;
import comparator.CompCoachCC;
import comparator.CompCoachCOTY;
import comparator.CompCoachCareer;
import comparator.CompCoachCareerPrestige;
import comparator.CompCoachConfCOTY;
import comparator.CompCoachDef;
import comparator.CompCoachNC;
import comparator.CompCoachOff;
import comparator.CompCoachOvr;
import comparator.CompCoachScore;
import comparator.CompCoachWinPCT;
import comparator.CompCoachWins;
import comparator.CompConfPrestige;
import comparator.CompKickRetTD;
import comparator.CompKickRetYards;
import comparator.CompNFLTalent;
import comparator.CompPlayer;
import comparator.CompPlayerFGMade;
import comparator.CompPlayerFGpct;
import comparator.CompPlayerFumblesRec;
import comparator.CompPlayerHeisman;
import comparator.CompPlayerInterceptions;
import comparator.CompPlayerPassInts;
import comparator.CompPlayerPassPCT;
import comparator.CompPlayerPassRating;
import comparator.CompPlayerPassTDs;
import comparator.CompPlayerPassYards;
import comparator.CompPlayerRecTDs;
import comparator.CompPlayerRecYards;
import comparator.CompPlayerReceptions;
import comparator.CompPlayerRushTDs;
import comparator.CompPlayerRushYards;
import comparator.CompPlayerSacks;
import comparator.CompPlayerTackles;
import comparator.CompPuntRetTDs;
import comparator.CompPuntRetYards;
import comparator.CompTeamBowls;
import comparator.CompTeamBudget;
import comparator.CompTeamCC;
import comparator.CompTeamChemistry;
import comparator.CompTeamConfWins;
import comparator.CompTeamDefTalent;
import comparator.CompTeamDisciplineScore;
import comparator.CompTeamFacilities;
import comparator.CompTeamHoFCount;
import comparator.CompTeamNC;
import comparator.CompTeamOPPG;
import comparator.CompTeamOPYPG;
import comparator.CompTeamORYPG;
import comparator.CompTeamOYPG;
import comparator.CompTeamOffTalent;
import comparator.CompTeamPPG;
import comparator.CompTeamPYPG;
import comparator.CompTeamPoll;
import comparator.CompTeamPrestige;
import comparator.CompTeamProjPoll;
import comparator.CompTeamRPI;
import comparator.CompTeamRYPG;
import comparator.CompTeamRecruitClass;
import comparator.CompTeamSoS;
import comparator.CompTeamSoW;
import comparator.CompTeamTODiff;
import comparator.CompTeamWins;
import comparator.CompTeamYPG;
import positions.Player;
import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerDefense;
import positions.PlayerK;
import positions.PlayerLB;
import positions.PlayerOL;
import positions.PlayerOffense;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerReturner;
import positions.PlayerS;
import positions.PlayerTE;
import positions.PlayerWR;
import staff.DC;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;

public class League {
    public static final String CURRENT_SAVE_VERSION = "v1.4e";

    private static GameUiBridge bridgeOrNoOp(GameUiBridge bridge) {
        return bridge == null ? GameUiBridge.NO_OP : bridge;
    }

    public String leagueName = "Custom League";
    public String saveVer = CURRENT_SAVE_VERSION;

    public PlatformResourceProvider resProvider;

    public void setPlatformResourceProvider(PlatformResourceProvider resProvider) {
        this.resProvider = resProvider;
    }

    private ArrayList<String[]> leagueHistory;
    private ArrayList<String> heismanHistory;
    private ArrayList<PlayerRecord> leagueHoF;
    private ArrayList<Conference> conferences;
    private ArrayList<Team> teamList;
    private ArrayList<Staff> coachList;
    private ArrayList<Staff> coachStarList;
    private ArrayList<Staff> coachFreeAgents;
    private ArrayList<Staff> coachDatabase;
    private ArrayList<String> nameList;
    private ArrayList<String> lastNameList;
    private ArrayList<ArrayList<String>> newsStories;
    private ArrayList<String> newsHeadlines;
    private ArrayList<ArrayList<String>> weeklyScores;
    private ArrayList<String> teamDiscipline;
    private double disciplineChance = 0.085;
    private double disciplineScrutiny = 0.035;

    public LeagueRecords leagueRecords;
    private TeamStreak longestWinStreak;
    private TeamStreak yearStartLongestWinStreak;
    private TeamStreak longestActiveWinStreak;

    // News Story Variables

    //League Stats
    public int leagueOffTal;
    public int leagueDefTal;
    public int confAvg;
    public double leagueChemistry;

    //Current week, 1-14
    public int currentWeek;

    //Bowl Games
    private boolean hasScheduledBowls;
    private Game semiG14;
    private Game semiG23;
    private Game ncg;
    private Game[] bowlGames;
    private Game[] cfpGames;
    public int playoffWeek;

    //User Team
    public Team userTeam;

    //Freshman Team
    private ArrayList<PlayerQB> fQBs;
    private ArrayList<PlayerRB> fRBs;
    private ArrayList<PlayerWR> fWRs;
    private ArrayList<PlayerTE> fTEs;
    private ArrayList<PlayerK> fKs;
    private ArrayList<PlayerOL> fOLs;
    private ArrayList<PlayerDL> fDLs;
    private ArrayList<PlayerLB> fLBs;
    private ArrayList<PlayerCB> fCBs;
    private ArrayList<PlayerS> fSs;
    //Transfer List
    private ArrayList<PlayerQB> transferQBs;
    private ArrayList<PlayerRB> transferRBs;
    private ArrayList<PlayerWR> transferWRs;
    private ArrayList<PlayerTE> transferTEs;
    private ArrayList<PlayerK> transferKs;
    private ArrayList<PlayerOL> transferOLs;
    private ArrayList<PlayerDL> transferDLs;
    private ArrayList<PlayerLB> transferLBs;
    private ArrayList<PlayerCB> transferCBs;
    private ArrayList<PlayerS> transferSs;
    public String userTransfers;
    public String sumTransfers;
    private ArrayList<String> transfersList;
    private ArrayList<Player> userTransferList;
    private ArrayList<Player> freshmen;
    private ArrayList<Player> redshirts;
    public String[] bowlNames;

    //Game Options
    public boolean fullGameLog;
    public boolean showPotential;
    public boolean confRealignment;
    public boolean enableUnivProRel;
    public boolean enableTV;
    public boolean neverRetire;
    public boolean expPlayoffs;
    public boolean advancedRealignment;
    public int countRealignment;
    public String newsRealignment;
    public boolean updateTV;
    private ArrayList<String> newsTV;
    private ArrayList<Team> playoffTeams;
    public String postseason;

    private static final int DEFAULT_NEW_SAVE_YEAR = 2026;
    private static final int LEGACY_SAVE_YEAR = 2021;
    private static final int EXPANDED_PLAYOFF_TEAM_COUNT = 12;
    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private final DecimalFormat df2 = new DecimalFormat("#.##", symbols);
    private final DecimalFormat df3 = new DecimalFormat("#.####", symbols);
    private int seasonStart = DEFAULT_NEW_SAVE_YEAR;
    int countTeam = 130; //default roster automatically calculates this number when using custom data or loaded saves
    private final int seasonWeeks = 30;
    public int regSeasonWeeks = 13; //original = 13 will change dynamically based on team/conference structure
    private final double confRealignmentChance = .25; //chance of event .25
    private final double realignmentChance = .25; //chance of invite .33
    private boolean heismanDecided;
    private Player heisman;
    private Player defPOTY;
    private HeadCoach coachWinner;
    private Player freshman;
    private ArrayList<Player> heismanCandidates;
    private ArrayList<Player> defPOTYCandidates;
    private ArrayList<Player> freshmanCandidates;
    private ArrayList<Player> allAmericans;
    private ArrayList<Player> allAmericans2;
    private ArrayList<Player> allFreshman;
    private String heismanWinnerStrFull;
    private String defPOTYWinnerStrFull;
    private String freshmanWinnerStrFull;
    private String coachWinnerStrFull;
    public boolean careerMode;



    private ArrayList<String> teamsFCSList;

    private final String[] proTeams = {"New England", "Buffalo", "New Jersey", "Miami", "Pittsburgh", "Baltimore", "Cincinnati", "Cleveland", "Jacksonville", "Indianapolis", "Houston", "Tennessee", "Kansas City", "Oakland", "Anaheim", "Denver",
            "New York", "Philadelphia", "Dallas", "Washington", "Minnesota", "Chicago", "Green Bay", "Detroit", "New Orleans", "Carolina", "Tampa Bay", "Atlanta", "Seattle", "Los Angeles", "San Francisco", "Arizona"};

    public final String[] states = {"AS", "AZ", "CA", "HI", "ID", "MT", "NV", "OR", "UT", "WA", "CO", "KS", "MO", "NE", "NM", "ND", "OK", "SD", "TX", "WY", "IL", "IN", "IA", "KY", "MD", "MI", "MN", "OH", "TN", "WI", "CT", "DE", "ME", "MA", "NH", "NJ", "NY", "PA", "RI", "VT", "AL", "AK", "FL", "GA", "LA", "MS", "NC", "SC", "VA", "WV"};

    public final String bowlNamesText = "Carnation Bowl, Mandarin Bowl, Honey Bowl, Fiesta Bowl, Nectarine Bowl, Polyester Bowl, Twister Bowl, Gator Bowl, Desert Bowl, Fort Bowl, Vacation Bowl, Star Bowl, Bell Bowl, Freedom Bowl, Casino Bowl, American Bowl, Island Bowl, Charity Bowl, Steak Bowl, Camping Bowl, Spud Bowl, Music Bowl, New Orleans Bowl, Cowboy Bowl, Santa Fe Bowl, Burrito Bowl, Mexico Bowl, Chick Bowl, Empire Bowl, Rainbow Bowl, Mushroom Bowl, Coffee Bowl, Cascade Bowl, Great Lakes Bowl, Cowboy Bowl, Alliance Bowl, Appalachian Bowl, Bayou Bowl, Nexus Bowl, Space Bowl, Everest Bowl, Cloud Bowl, Healthcare Bowl, More Chicken Bowl, Avocado Bowl, Realtors Bowl, Search Engine Bowl, Instant Photo Bowl, Social Faces Bowl, Grape Bowl, Tesla Bowl, Earthquake Bowl, Rainforest Bowl";

    public String[] teamsFCS = {"Alabama State", "Albany", "Cal-Poly", "Central Arkansas", "Chattanooga", "Columbia", "Dayton", "Delaware", "Eastern Wash", "Eastern Tenn", "Spokane", "Harvard", "Yale", "Princeton", "Grambling", "Georgetown", "Idaho", "Idaho State", "James Madison", "Maine", "Miss Valley", "Montana", "Montana State", "New Hampshire", "North Dakota", "North Dakota St", "South Dakota", "South Dakota St", "Northern Arizona", "Northern Colorado", "Portland", "Rhode Island", "Sacramento", "Southern", "Southern TX", "Western llinois", "Youngstown"};

    public String[] confNamesNew = {"Antdroid", "Big 8", "National", "Constitution", "Colonial", "Continental"};


    /**
     * Creates League, sets up Conferences, reads team names and conferences from file.
     * Also schedules games for every team.
     */
    public League(String namesCSV, String lastNamesCSV, String confText, String teamText, String bowlText, boolean randomize, boolean equalize) {
        careerMode = true;
        showPotential = false;
        confRealignment = true;
        enableTV = true;
        enableUnivProRel = false;
        neverRetire = false;
        setupCommonInitalizers();

        //set up names database from xml
        setupNamesDB(namesCSV, lastNamesCSV);

        //Set up bowls from XML
        bowlNames = new String[bowlText.split(", ").length];
        for (int b = 0; b < bowlText.split(", ").length; ++b) {
            bowlNames[b] = bowlText.split(", ")[b];
        }

        //Set up conferences from XML
        String[] confSplit = confText.split("%");
        int confDiv = 0;
        for (String n : confSplit) {
            //conferences.add(new Conference(n.split(",")[0], this, false, 0, 0));
            conferences.add(new Conference(n, this, false, 0, 0));
            //conferences.get(confDiv).divisions.add(new Division(n.split(",")[1], this));
            //conferences.get(confDiv).divisions.add(new Division(n.split(",")[2], this));
            confDiv++;
        }


        //Set up teams from XML
        int x = 0;
        int c = 0;
        if (!randomize && !equalize) {
            for (int t = 0; t < teamText.split("%").length; t++) {
                if (teamText.split("%")[t].contains("[END_CONF]")) {
                    x = 0;
                    c++;
                } else {
                    String[] teamID = teamText.split("%")[t].split(",");
                    conferences.get(c).confTeams.add(new Team(
                            normalizeSeedText(teamID[0]),
                            normalizeSeedText(teamID[1]),
                            normalizeSeedText(teamID[2]),
                            Integer.parseInt(normalizeSeedText(teamID[3])),
                            normalizeSeedText(teamID[4]),
                            Integer.parseInt(normalizeSeedText(teamID[5])),
                            this
                    ));
                    x++;
                }
            }
        } else if (randomize) {
            for (int t = 0; t < teamText.split("%").length; t++) {
                int tmPres = 0;
                if (c < 5) tmPres = (int) (Math.random() * 35) + 60;
                else tmPres = (int) (Math.random() * 35) + 25;
                if (teamText.split("%")[t].contains("[END_CONF]")) {
                    x = 0;
                    c++;
                } else {
                    String[] teamID = teamText.split("%")[t].split(",");
                    conferences.get(c).confTeams.add(new Team(
                            normalizeSeedText(teamID[0]),
                            normalizeSeedText(teamID[1]),
                            normalizeSeedText(teamID[2]),
                            tmPres,
                            normalizeSeedText(teamID[4]),
                            Integer.parseInt(normalizeSeedText(teamID[5])),
                            this
                    ));
                    x++;
                }
            }
        } else {
            for (int t = 0; t < teamText.split("%").length; t++) {
                int tmPres = 60;
                if (teamText.split("%")[t].contains("[END_CONF]")) {
                    x = 0;
                    c++;
                } else {
                    String[] teamID = teamText.split("%")[t].split(",");
                    conferences.get(c).confTeams.add(new Team(
                            normalizeSeedText(teamID[0]),
                            normalizeSeedText(teamID[1]),
                            normalizeSeedText(teamID[2]),
                            tmPres,
                            normalizeSeedText(teamID[4]),
                            Integer.parseInt(normalizeSeedText(teamID[5])),
                            this
                    ));
                    x++;
                }
            }
        }

        //set teamList
        for (int i = 0; i < conferences.size(); ++i) {
            for (int j = 0; j < conferences.get(i).confTeams.size(); ++j) {
                teamList.add(conferences.get(i).confTeams.get(j));
                teamList.get(i).setPlaybookOffNum(teamList.get(i).getCPUOffense());
                teamList.get(i).setPlaybookDefNum(teamList.get(i).getCPUDefense());
                teamList.get(i).setPlaybookOffense(teamList.get(i).getPlaybookOff()[teamList.get(i).getPlaybookOffNum()]);
                teamList.get(i).setPlaybookDefense(teamList.get(i).getPlaybookDef()[teamList.get(i).getPlaybookDefNum()]);
            }
        }

        checkIndyConfExists();

        setupSeason();
    }

    private static String normalizeSeedText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replace('\uFEFF', ' ').trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a CUSTOM League Universe
     */
    public League(String namesCSV, String lastNamesCSV, File customConf, File customTeams, File customBowl, boolean randomize, boolean equalize) {
        this(namesCSV, lastNamesCSV, customConf, customTeams, customBowl, randomize, equalize, GameUiBridge.NO_OP);
    }

    public League(String namesCSV, String lastNamesCSV, File customConf, File customTeams, File customBowl, boolean randomize, boolean equalize, GameUiBridge main) {
        GameUiBridge bridge = bridgeOrNoOp(main);
        careerMode = true;
        showPotential = false;
        confRealignment = true;
        enableTV = true;
        enableUnivProRel = false;
        neverRetire = false;

        setupCommonInitalizers();
        setupNamesDB(namesCSV, lastNamesCSV);

        String line = null;

        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(customConf));

            //First ignore the save file info
            line = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null && !line.equals("[END_CONFERENCES]")) {
                conferences.add(new Conference(line, this, false, 0, 0));
            }
        } catch (FileNotFoundException ex) {
            PlatformLog.e("League", "Unable to open file", ex);
        } catch (Exception ex) {
            PlatformLog.e("League", "Error reading file", ex);
            bridge.crash();
            return;
        }


        //Set up conference teams
        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(customTeams));

            //First ignore the save file info
            line = bufferedReader.readLine();
            countTeam = 0;
            while ((line = bufferedReader.readLine()) != null && !line.contains("[END_TEAMS]")) {
                for (int c = 0; c < conferences.size(); ++c) {
                    while ((line = bufferedReader.readLine()) != null && !line.contains("[END_CONF]")) {
                        String[] filesSplit = line.split(", ");
                        if (filesSplit.length > 1) {
                            line.replace("\"", "\\\"");

                            String tmName = filesSplit[0];
                            String tmAbbr = filesSplit[1];
                            String tmConf = filesSplit[2];
                            int tmPres = Integer.parseInt(filesSplit[3]);
                            if (randomize) {
                                if (c < 5) tmPres = (int) (Math.random() * 35) + 60;
                                else tmPres = (int) (Math.random() * 35) + 25;
                            }
                            if (equalize) {
                                tmPres = 60;
                            }
                            String tmRival = filesSplit[4];
                            int tmLoc = Integer.parseInt(filesSplit[5]);
                            conferences.get(c).confTeams.add(new Team(tmName, tmAbbr, tmConf, tmPres, tmRival, tmLoc, this));
                        } else {
                            filesSplit = line.split(",");
                            String tmName = filesSplit[0];
                            String tmAbbr = filesSplit[1];
                            String tmConf = filesSplit[2];
                            int tmPres = Integer.parseInt(filesSplit[3]);
                            if (randomize) {
                                if (c < 5) tmPres = (int) (Math.random() * 35) + 60;
                                else tmPres = (int) (Math.random() * 35) + 25;
                            }
                            if (equalize) {
                                tmPres = 60;
                            }
                            String tmRival = filesSplit[4];
                            int tmLoc = Integer.parseInt(filesSplit[5]);
                            conferences.get(c).confTeams.add(new Team(tmName, tmAbbr, tmConf, tmPres, tmRival, tmLoc, this));
                        }
                        countTeam++;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            PlatformLog.e("League", "Unable to open file", ex);
        } catch (IOException ex) {
            PlatformLog.e("League", "Error reading file", ex);
            bridge.crash();
        }

        //set teamList
        for (int i = 0; i < conferences.size(); ++i) {
            for (int j = 0; j < conferences.get(i).confTeams.size(); ++j) {
                teamList.add(conferences.get(i).confTeams.get(j));
                teamList.get(i).setPlaybookOffNum(teamList.get(i).getCPUOffense());
                teamList.get(i).setPlaybookDefNum(teamList.get(i).getCPUDefense());
                teamList.get(i).setPlaybookOffense(teamList.get(i).getPlaybookOff()[teamList.get(i).getPlaybookOffNum()]);
                teamList.get(i).setPlaybookDefense(teamList.get(i).getPlaybookDef()[teamList.get(i).getPlaybookDefNum()]);
            }
        }

        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(customBowl));

            //First ignore the save file info
            line = bufferedReader.readLine();
            String[] filesSplit = line.split(", ");
            if (filesSplit.length > 1) {
                bowlNames = new String[filesSplit.length];
                line.replaceAll("\"", "\\\"");
                for (int b = 0; b < filesSplit.length; ++b) {
                    bowlNames[b] = filesSplit[b];
                }
            } else {
                filesSplit = line.split(",");
                bowlNames = new String[filesSplit.length];
                for (int b = 0; b < filesSplit.length; ++b) {
                    bowlNames[b] = filesSplit[b];
                }
            }

        } catch (FileNotFoundException ex) {
            PlatformLog.e("League", "Unable to open file", ex);
            bridge.crash();
        } catch (IOException ex) {
            PlatformLog.e("League", "Error reading file", ex);
            bridge.crash();
        }

        if (bowlNames.length != bowlNamesText.split(",").length) {

            String[] bowlTemp = bowlNames.clone();

            bowlNames = new String[bowlNamesText.split(",").length];

            for (int b = 0; b < bowlTemp.length; b++) {
                bowlNames[b] = bowlTemp[b];
            }

            for (int b = bowlTemp.length; b < bowlNamesText.split(",").length; b++) {
                bowlNames[b] = bowlNamesText.split(",")[b];
            }
        }

        //Rename conference to Independent if too small
        for (int c = 0; c < conferences.size(); c++) {
            if (conferences.get(c).confTeams.size() < conferences.get(c).minConfTeams) {
                conferences.get(c).confName = "Independent";
                for (int i = 0; i < conferences.get(c).confTeams.size(); i++) {
                    conferences.get(c).confTeams.get(i).setConference("Independent");
                }
            }
        }

        checkIndyConfExists();

        setupSeason();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * LOAD A SAVE FILE
     * Create League from saved file.
     *
     * @param saveFile file that league is saved in
     */
    public League(File saveFile, String namesCSV, String lastNamesCSV, boolean recruitingChk) {
        this(saveFile, namesCSV, lastNamesCSV, GameUiBridge.NO_OP, recruitingChk);
    }

    public League(File saveFile, String namesCSV, String lastNamesCSV, GameUiBridge mainAct, boolean recruitingChk) {
        GameUiBridge bridge = bridgeOrNoOp(mainAct);

        setupCommonInitalizers();
        setupNamesDB(namesCSV, lastNamesCSV);

        String line = null;
        String saveHeader = null;

        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(new FileReader(saveFile));

            //First ignore the save file info
            line = bufferedReader.readLine();
            if (line != null && line.startsWith("L:")) {
                // NEW FORMAT DETECTION
                bufferedReader.close();
                try (FileInputStream fis = new FileInputStream(saveFile)) {
                    applyLeagueRecord(SaveManager.load(fis));
                    return;
                }
            }
            saveHeader = line;

            // Game Mode
            //careerMode = line.substring(line.length() - 4, line.length()).equals("[C]%");

            //Team Count
            if (line.split(">").length > 1) {
                if (!line.split(">")[1].contains("["))
                    countTeam = Integer.parseInt(line.split(">")[1]);
            }


            //Next get league history
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_HIST")) {
                leagueHistory.add(line.split("%"));
            }
            seasonStart = getSeasonStartFromSaveHeader(saveHeader);

            //Next get heismans
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_HEISMAN_HIST")) {
                heismanHistory.add(line);
            }

            //Next make all the conferences & teams
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CONFERENCES")) {
                conferences.add(new Conference(line, this));
            }

            for (int i = 0; i < countTeam; ++i) { //Do for every team
                StringBuilder sbTeam = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null && !line.equals("END_PLAYERS")) {
                    sbTeam.append(line);
                }
                Team t = new Team(sbTeam.toString(), this);
                conferences.get(getConfNumber(t.getConference())).confTeams.add(t);
                teamList.add(t);
                teamList.get(i).setPlaybookOffNum(teamList.get(i).getCPUOffense());
                teamList.get(i).setPlaybookDefNum(teamList.get(i).getCPUDefense());
                teamList.get(i).setPlaybookOffense(teamList.get(i).getPlaybookOff()[teamList.get(i).getPlaybookOffNum()]);
                teamList.get(i).setPlaybookDefense(teamList.get(i).getPlaybookDef()[teamList.get(i).getPlaybookDefNum()]);
            }

            //Set up user team
            if ((line = bufferedReader.readLine()) != null) {
                for (Team t : teamList) {
                    if (t.getName().equals(line)) {
                        userTeam = t;
                        userTeam.setUserControlled(true);
                        break;
                    }
                }
            }
            //Team History
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM_HISTORY")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM")) {
                        teamList.get(i).addTeamHistory(TeamHistoryRecord.fromCsv(line));

                    }
                }
            }
            //HeadCoach History
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH_HISTORY")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getHeadCoach().history.add(line);
                    }
                }
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getOC().history.add(line);
                    }
                }
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getDC().history.add(line);
                    }
                }
            }


            while ((line = bufferedReader.readLine()) != null && !line.equals("END_BOWL_NAMES")) {
                String[] filesSplit = line.split(",");
                bowlNames = new String[filesSplit.length];
                for (int b = 0; b < filesSplit.length; ++b) {
                    bowlNames[b] = filesSplit[b];
                }
            }

            //fix bowl names
            if (bowlNames.length != bowlNamesText.split(",").length) {

                String[] bowlTemp = bowlNames.clone();

                bowlNames = new String[bowlNamesText.split(",").length];

                for (int b = 0; b < bowlTemp.length; b++) {
                    bowlNames[b] = bowlTemp[b];
                }

                for (int b = bowlTemp.length; b < bowlNamesText.split(",").length; b++) {
                    bowlNames[b] = bowlNamesText.split(",")[b];
                }
            }

            //fix bowl null
            if (bowlNames.length > 0 && bowlNames[0] == null) {

                bowlNames = new String[bowlNamesText.split(",").length];

                for (int b = 0; b < bowlNamesText.split(",").length; b++) {
                    bowlNames[b] = bowlNamesText.split(",")[b];
                }
            }

            String[] record;
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_RECORDS")) {
                record = line.split(",");
                if (!record[1].equals("-1"))
                    leagueRecords.checkRecord(record[0], Float.parseFloat(record[1]), record[2], Integer.parseInt(record[3]));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_WIN_STREAK")) {
                record = line.split(",");
                longestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
                yearStartLongestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM_RECORDS")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM")) {
                        record = line.split(",");
                        if (!record[1].equals("-1"))
                            teamList.get(i).getTeamRecords().checkRecord(record[0], Float.parseFloat(record[1]), record[2], Integer.parseInt(record[3]));
                    }
                }
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_HALL_OF_FAME")) {
                PlayerRecord pr = PlayerRecord.fromCsv(line);
                leagueHoF.add(pr);

                for (Team t : teamList) {
                    if (t.getName().equals(pr.teamName())) {
                        t.addToHallOfFame(pr);


                        t.incrementHoFCount();
                    }
                }
            }


            int coachFA = 0;
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACHES")) {
                coachFreeAgents.add(new HeadCoach(line));

                while ((line = bufferedReader.readLine()) != null && !line.equals("END_FREE_AGENT")) {
                    coachFreeAgents.get(coachFA).history.add(line);
                }

                coachFA++;
            }


            while ((line = bufferedReader.readLine()) != null && !line.equals("END_GAME_LOG")) {
                fullGameLog = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_HIDE_POTENTIAL")) {
                showPotential = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CONF_REALIGNMENT")) {
                confRealignment = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_ENABLE_TV")) {
                enableTV = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_PRO_REL")) {
                enableUnivProRel = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_NEVER_RETIRE")) {
                neverRetire = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CAREER_MODE")) {
                careerMode = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_EXP_PLAYOFFS")) {
                expPlayoffs = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_ADV_CONF_REALIGNMENT")) {
                advancedRealignment = Boolean.parseBoolean(line);
            }

            if(recruitingChk) {
                while ((line = bufferedReader.readLine()) != null && !line.equals("END_RECRUITING")) {
                    if (line.contains("RECRUITING")) {
                        currentWeek = 99;
                        setTeamRanks();
                        bridge.startRecruiting(saveFile, userTeam);
                    }
                }
            }

            if (enableUnivProRel) {
                confRealignment = false;
                advancedRealignment = false;
            }

            // Always close files.
            bufferedReader.close();


        } catch (FileNotFoundException ex) {
            PlatformLog.e("League", "Unable to open file", ex);
        } catch (Exception ex) {
            PlatformLog.e("League", "Error reading file", ex);
        }

        //Get longest active win streak
        updateLongestActiveWinStreak();

        checkIndyConfExists();

        setupSeason();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * IMPORT A SAVE FILE
     * Create League from saved file.
     **/
    public League(InputStream inputStream, String namesCSV, String lastNamesCSV) {
        this(inputStream, namesCSV, lastNamesCSV, GameUiBridge.NO_OP);
    }

    public League(InputStream inputStream, String namesCSV, String lastNamesCSV, GameUiBridge main) {
        GameUiBridge bridge = bridgeOrNoOp(main);
        setupCommonInitalizers();
        setupNamesDB(namesCSV, lastNamesCSV);

        String line = null;
        String saveHeader = null;

        try {
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            //First ignore the save file info
            line = bufferedReader.readLine();
            saveHeader = line;
            //Team Count
            if (line.split(">").length > 1) {
                countTeam = Integer.parseInt(line.split(">")[1]);
            }


            //Next get league history

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_HIST")) {
                leagueHistory.add(line.split("%"));
            }
            seasonStart = getSeasonStartFromSaveHeader(saveHeader);

            //Next get heismans
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_HEISMAN_HIST")) {
                heismanHistory.add(line);
            }

            //Next make all the conferences & teams
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CONFERENCES")) {
                conferences.add(new Conference(line, this));
            }

            for (int i = 0; i < countTeam; ++i) { //Do for every team
                StringBuilder sbTeam = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null && !line.equals("END_PLAYERS")) {
                    sbTeam.append(line);
                }
                Team t = new Team(sbTeam.toString(), this);
                conferences.get(getConfNumber(t.getConference())).confTeams.add(t);
                teamList.add(t);
                teamList.get(i).setPlaybookOffNum(teamList.get(i).getCPUOffense());
                teamList.get(i).setPlaybookDefNum(teamList.get(i).getCPUDefense());
                teamList.get(i).setPlaybookOffense(teamList.get(i).getPlaybookOff()[teamList.get(i).getPlaybookOffNum()]);
                teamList.get(i).setPlaybookDefense(teamList.get(i).getPlaybookDef()[teamList.get(i).getPlaybookDefNum()]);
            }

            //Set up user team
            if ((line = bufferedReader.readLine()) != null) {
                for (Team t : teamList) {
                    if (t.getName().equals(line)) {
                        userTeam = t;
                        userTeam.setUserControlled(true);
                    }
                }
            }
            //Team History
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM_HISTORY")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM")) {
                        teamList.get(i).addTeamHistory(TeamHistoryRecord.fromCsv(line));

                    }
                }
            }
            //HeadCoach History
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH_HISTORY")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getHeadCoach().history.add(line);
                    }
                }
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getOC().history.add(line);
                    }
                }
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACH")) {
                        teamList.get(i).getDC().history.add(line);
                    }
                }
            }


            while ((line = bufferedReader.readLine()) != null && !line.equals("END_BOWL_NAMES")) {
                String[] filesSplit = line.split(",");
                bowlNames = new String[filesSplit.length];

                for (int b = 0; b < filesSplit.length; ++b) {
                    bowlNames[b] = filesSplit[b];
                }
            }

            //fix bowl names
            if (bowlNames.length != bowlNamesText.split(",").length) {

                String[] bowlTemp = bowlNames.clone();

                bowlNames = new String[bowlNamesText.split(",").length];

                for (int b = 0; b < bowlTemp.length; b++) {
                    bowlNames[b] = bowlTemp[b];
                }

                for (int b = bowlTemp.length; b < bowlNamesText.split(",").length; b++) {
                    bowlNames[b] = bowlNamesText.split(",")[b];
                }
            }


            String[] record;
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_RECORDS")) {
                record = line.split(",");
                if (!record[1].equals("-1")) {
                    if (record.length > 4) {
                        leagueRecords.checkRecord(record[0], Float.parseFloat(record[1] + "." + record[2]), record[3], Integer.parseInt(record[4]));
                    } else {
                        leagueRecords.checkRecord(record[0], Float.parseFloat(record[1]), record[2], Integer.parseInt(record[3]));
                    }
                }
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_WIN_STREAK")) {
                record = line.split(",");
                longestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
                yearStartLongestWinStreak = new TeamStreak(
                        Integer.parseInt(record[2]), Integer.parseInt(record[3]), Integer.parseInt(record[0]), record[1]);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM_RECORDS")) {
                for (int i = 0; i < teamList.size(); ++i) { //Do for every team
                    while ((line = bufferedReader.readLine()) != null && !line.equals("END_TEAM")) {
                        record = line.split(",");
                        if (!record[1].equals("-1")) {
                            if (record.length > 4) {
                                leagueRecords.checkRecord(record[0], Float.parseFloat(record[1] + "." + record[2]), record[3], Integer.parseInt(record[4]));
                            } else {
                                leagueRecords.checkRecord(record[0], Float.parseFloat(record[1]), record[2], Integer.parseInt(record[3]));
                            }
                        }
                    }
                }
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_LEAGUE_HALL_OF_FAME")) {
                leagueHoF.add(PlayerRecord.fromCsv(line));

                String[] fileSplit = line.split(":");
                for (int i = 0; i < teamList.size(); ++i) {
                    if (teamList.get(i).getName().equals(fileSplit[0])) {
                        teamList.get(i).addToHallOfFame(PlayerRecord.fromCsv(line));

                        teamList.get(i).incrementHoFCount();
                    }
                }
            }

            int coachFA = 0;
            while ((line = bufferedReader.readLine()) != null && !line.equals("END_COACHES")) {

                coachFreeAgents.add(new HeadCoach(line));

                while ((line = bufferedReader.readLine()) != null && !line.equals("END_FREE_AGENT")) {
                    coachFreeAgents.get(coachFA).history.add(line);
                }
                coachFA++;
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_GAME_LOG")) {
                fullGameLog = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_HIDE_POTENTIAL")) {
                showPotential = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CONF_REALIGNMENT")) {
                confRealignment = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_ENABLE_TV")) {
                enableTV = Boolean.parseBoolean(line);
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_PRO_REL")) {
                enableUnivProRel = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_NEVER_RETIRE")) {
                neverRetire = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_CAREER_MODE")) {
                careerMode = Boolean.parseBoolean((line));
            }

            while ((line = bufferedReader.readLine()) != null && !line.equals("END_EXP_PLAYOFFS")) {
                expPlayoffs = Boolean.parseBoolean((line));
            }

            if (enableUnivProRel) {
                confRealignment = false;
            }


            // Always close files.
            bufferedReader.close();


        } catch (FileNotFoundException ex) {
            PlatformLog.e("League", "Unable to open file", ex);
        } catch (Exception ex) {
            PlatformLog.e("League", "Error reading file", ex);
            bridge.crash();
        }

        //fix team divions
        for (int c = 0; c < conferences.size(); c++) {
            for (int i = 0; i < conferences.get(c).confTeams.size(); i++) {
                conferences.get(c).divisions.add(new Division("A", this));
                conferences.get(c).divisions.add(new Division("B", this));
                if (i % 2 == 0) {
                    conferences.get(c).confTeams.get(i).setDivision(conferences.get(c).divisions.get(1).divName);
                    conferences.get(c).divisions.get(0).divTeams.add(conferences.get(c).confTeams.get(i));
                } else {
                    conferences.get(c).confTeams.get(i).setDivision(conferences.get(c).divisions.get(2).divName);
                    conferences.get(c).divisions.get(1).divTeams.add(conferences.get(c).confTeams.get(i));
                }
            }
        }

        //Get longest active win streak
        updateLongestActiveWinStreak();

        checkIndyConfExists();

        setupSeason();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    //Initialize all common variables for each game type
    private void setupCommonInitalizers() {

        nameList = new ArrayList<>();
        lastNameList = new ArrayList<>();
        heismanDecided = false;
        hasScheduledBowls = false;
        bowlGames = new Game[bowlNamesText.split(",").length];
        bowlNames = new String[bowlNamesText.split(",").length];
        cfpGames = new Game[15];
        playoffTeams = new ArrayList<>();
        leagueHistory = new ArrayList<>();
        heismanHistory = new ArrayList<>();
        leagueHoF = new ArrayList<>();
        coachList = new ArrayList<>();
        coachStarList = new ArrayList<>();
        coachFreeAgents = new ArrayList<>();
        coachDatabase = new ArrayList<>();

        conferences = new ArrayList<>();
        teamList = new ArrayList<>();

        allAmericans = new ArrayList<>();
        allFreshman = new ArrayList<>();
        allAmericans2 = new ArrayList<>();
        transferQBs = new ArrayList<>();
        transferRBs = new ArrayList<>();
        transferWRs = new ArrayList<>();
        transferTEs = new ArrayList<>();
        transferKs = new ArrayList<>();
        transferOLs = new ArrayList<>();
        transferDLs = new ArrayList<>();
        transferLBs = new ArrayList<>();
        transferCBs = new ArrayList<>();
        transferSs = new ArrayList<>();

        freshmen = new ArrayList<>();
        redshirts = new ArrayList<>();

        leagueRecords = new LeagueRecords();
        longestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        yearStartLongestWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");
        longestActiveWinStreak = new TeamStreak(getYear(), getYear(), 0, "XXX");

        newsHeadlines = new ArrayList<>();
        expPlayoffs = true;
    }

    private int getSeasonStartFromSaveHeader(String saveHeader) {
        if (saveHeader == null || !saveHeader.contains(":")) {
            return LEGACY_SAVE_YEAR;
        }

        try {
            String yearToken = saveHeader.split(":")[0].trim();
            boolean recruitingSave = yearToken.endsWith("-R");
            if (recruitingSave) {
                yearToken = yearToken.substring(0, yearToken.length() - 2);
            }

            int absoluteYear = Integer.parseInt(yearToken);
            if (recruitingSave) {
                return absoluteYear - leagueHistory.size() + 1;
            }
            return absoluteYear - leagueHistory.size();
        } catch (Exception ex) {
            return LEGACY_SAVE_YEAR;
        }
    }

    public LeagueRecord toRecord() {
        ArrayList<LeagueRecord.ConferenceRecord> confRecords = new ArrayList<>();
        for (Conference c : conferences) {
            confRecords.add(c.toRecord());
        }

        return new LeagueRecord(
                leagueName, getYear(), currentWeek,

                confRecords,
                new ArrayList<>(leagueHoF),
                leagueRecords.toRecordList(),
                heismanWinnerStrFull != null ? heismanWinnerStrFull : "",
                "Unknown" // Placeholder for national champ name
        );
    }

    public void applyLeagueRecord(LeagueRecord record) {
        this.leagueName = record.leagueName();
        this.seasonStart = record.year() - (leagueHistory != null ? leagueHistory.size() : 0);

        this.currentWeek = record.currentWeek();
        this.heismanWinnerStrFull = record.heismanWinnerName();
        
        this.conferences = new ArrayList<>();
        this.teamList = new ArrayList<>();
        
        for (LeagueRecord.ConferenceRecord cr : record.conferences()) {
            this.conferences.add(new Conference(cr, this));
        }
        
        this.leagueHoF = new ArrayList<>(record.leagueHoF());
        
        for (DataRecord dr : record.leagueRecords()) {
            this.leagueRecords.addRecord(dr);
        }

        
        // Finalize setup
        for (Team t : teamList) {
            t.setPlaybookOffNum(t.getCPUOffense());
            t.setPlaybookDefNum(t.getCPUDefense());
        }
    }



    private void checkIndyConfExists() {
        boolean indyExists = false;

        for(int c = 0; c < conferences.size(); c++) {
            if(conferences.get(c).confName.equals("Independent")) {
                indyExists = true;
            }
        }
        if(!indyExists) {
            conferences.add(new Conference("Independent", this, false, 0, 0));
        }
    }

    public void hireMissingCoaches() {
        for(Team t : teamList) {
            if(t.getHeadCoach() == null) coachHiringSingleTeam(t);
        }
        coordinatorCarousel();
    }

    //Set Up Season variables
    private void setupSeason() {

        //hireMissingCoaches();

        for(int c = 0; c < conferences.size(); c++) {
            conferences.get(c).updateConfPrestige();
        }

        int numOddConf = 0;
        int largeOddConf = 0;
        for (int i = 0; i < conferences.size(); i++) {
            if(conferences.get(i).confTeams.size() % 2 != 0) {
                numOddConf++;
                advancedRealignment = true;
/*                if(conferences.get(i).confTeams.size() >= 13) {
                    largeOddConf++;
                }*/
            }
        }
        //if (numOddConf > 0) regSeasonWeeks++;
        //if (largeOddConf > 0) regSeasonWeeks++;

        //set up schedule
        for (int i = 0; i < conferences.size(); ++i) {
            conferences.get(i).setUpSchedule();
        }

        //decide OOC schedule
        for (int r = 0; r < regSeasonWeeks; r++) {
            int j = 0;
            int k = 0;

            for (int c = 0; c < conferences.size(); c++) {
                if (r < conferences.get(c).oocGames && conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                    boolean scheduled = false;
                    k = k + (int) (Math.random() * 4);
                    while (!scheduled) {
                        int week = (j + r + k) % (regSeasonWeeks - 1);
                        if (!conferences.get(c).oocWeeks.contains(week)) {
                            conferences.get(c).oocWeeks.add(week);
                            for (int t = 0; t < conferences.get(c).confTeams.size(); t++) {
                                conferences.get(c).confTeams.get(t).addOocWeek(week);
                            }
                            scheduled = true;
                        } else {
                            k = k + 2;
                        }
                    }
                    j++;
                } else if (conferences.get(c).confTeams.size() < conferences.get(c).minConfTeams && r < conferences.get(c).oocGames) {
                    for (int t = 0; t < conferences.get(c).confTeams.size(); t++) {
                        conferences.get(c).confTeams.get(t).addOocWeek(r);
                    }
                }
            }
        }

        //setup FCS Team Database

        //get list of team names
        ArrayList<String> leagueTeams = new ArrayList<>();
        for(int i = 0; i < teamList.size(); i++) {
            leagueTeams.add(teamList.get(i).getName());
        }

        teamsFCSList = new ArrayList<>();
        for(int i = 0; i < teamsFCS.length; i++) {
            if(!leagueTeams.contains(teamsFCS[i])) teamsFCSList.add(teamsFCS[i]);
        }

        //Setup OOC v3 Scheduling
        if (!enableUnivProRel) {
            for (int week = 0; week < (regSeasonWeeks-1); week++) {

                ArrayList<Team> availTeams = new ArrayList<>();
                for (int t = 0; t < teamList.size(); t++) {
                    if (teamList.get(t).getOocWeeks().contains(week) && teamList.get(t).getGameSchedule().size() >= week) {
                        availTeams.add(teamList.get(t));
                    }
                }

                while (availTeams.size() > 0) {
                    int selTeamA = (int) (availTeams.size() * Math.random());
                    Team a = availTeams.get(selTeamA);

                    ArrayList<Team> availTeamsB = new ArrayList<>();
                    for (int k = 0; k < availTeams.size(); k++) {
                        if (!availTeams.get(k).getConference().equals(a.getConference()) && !a.getOocTeams().contains(availTeams.get(k))) {
                            availTeamsB.add(availTeams.get(k));
                        }
                    }
                    Team b;

                    if (availTeamsB.isEmpty()) {
                        if(teamsFCSList.isEmpty())
                            b = new Team("Antdroid Tech", "FCS", "FCS Division", (int) (Math.random() * 40), "FCS1", 0, this, false);
                        else
                            b = new Team(teamsFCSList.get((int) (teamsFCSList.size() * Math.random())), "FCS", "FCS Division", (int) (Math.random() * 40), "FCS1", 0, this, false);
                    } else {
                        int selTeamB = (int) (availTeamsB.size() * Math.random());
                        b = availTeamsB.get(selTeamB);
                    }

                    Game gm;
                    gm = new Game(a, b, "OOC");

                    if(a.getGameSchedule().size() != b.getGameSchedule().size())
                    PlatformLog.d("league", "setupSeason: week " + week + " " + a.getName() + " size" + a.getGameSchedule().size() + " vs " + b.getName() + " size" + b.getGameSchedule().size());

                    if (!a.getConference().contains("Independent") && !a.getConference().contains("FCS")) {
                        a.addGameToSchedule(week, gm);
                    }
                    if (!b.getConference().contains("Independent") && !b.getConference().contains("FCS"))  {
                        b.addGameToSchedule(week, gm);
                    }

                    if (a.getConference().contains("Independent")) {
                        a.addGameToSchedule(gm);
                    }
                    if (b.getConference().contains("Independent")) {
                        b.addGameToSchedule(gm);
                    }

                    a.addOocTeam(b);
                    b.addOocTeam(a);

                    availTeams.remove(a);
                    availTeams.remove(b);
                }

            }


            if(numOddConf > 0) {
                for (int c = 0; c < conferences.size(); c++) {
                    if (conferences.get(c).confTeams.size() < conferences.get(c).minConfTeams) {
                        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, this);
                        bye.setRankTeamPollScore(teamList.size());
                        for (int g = 0; g < conferences.get(c).confTeams.size(); ++g) {
                            Team a = conferences.get(c).confTeams.get(g);
                            a.addGameToSchedule(new Game(a, bye, "BYE WEEK"));
                        }
                    }
                }
            }

            if(largeOddConf > 0) {
                for (int c = 0; c < conferences.size(); c++) {
                    if (conferences.get(c).confTeams.size() < conferences.get(c).minConfTeams) {
                        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, this);
                        bye.setRankTeamPollScore(teamList.size());
                        for (int g = 0; g < conferences.get(c).confTeams.size(); ++g) {
                            Team a = conferences.get(c).confTeams.get(g);
                            a.addGameToSchedule(new Game(a, bye, "BYE WEEK"));
                        }
                    }
                }
            }

        }

        confAvg = getAverageConfPrestige();

        // Initialize new stories lists
        newsStories = new ArrayList<>();
        weeklyScores = new ArrayList<>();
        for (int i = 0; i < seasonWeeks; ++i) {
            newsStories.add(new ArrayList<String>());
            weeklyScores.add(new ArrayList<String>());
        }
        newsStories.get(0).add("New Season!>Ready for the new season, coach? Whether the National Championship is " +
                "on your mind, or just a winning season, good luck!");
        weeklyScores.get(0).add("Scores:>No games this week.");

        newsHeadlines.add("New Season Begins!");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conferences.size(); i++) {
            if (conferences.get(i).confTeams.size() >= conferences.get(i).minConfTeams) {
                sb.append(conferences.get(i).confName + ":  " + conferences.get(i).confPrestige + "\n");
            }
        }

        newsStories.get(0).add("Conference Prestige>The latest surveys are in. The " + getYear() + " prestige ratings for each conference are:\n\n" + sb);

        penalizeTeams();

        sb = new StringBuilder();
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).isBowlBan()) {
                sb.append(teamList.get(i).getName() + "\n");
            }
        }

        if (sb.length() > 0) {
            newsStories.get(0).add("Post-Season Ban!>These teams have seen numerous violations pile up and have lost the patiences of the College Football Administration. These teams will see reduced scholarships (loss of prestige), and post-season bans!\n\n" + sb);
            newsHeadlines.add("Post-Season Bans handed down!");
        }

        sb = new StringBuilder();
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).isPenalized()) {
                sb.append(teamList.get(i).getName() + "\n");
            }
        }

        if (sb.length() > 0) {
            newsStories.get(0).add("Minor Infractions!>The following teams have been fined by the College Football Administration for a minor infractions related to discplinary concerns surrounding the school:\n\n" + sb);
            newsHeadlines.add("College Administration warnings sent!");
        }

        upgradeFacilities();

        sb = new StringBuilder();
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).isFacilityUpgrade()) {
                sb.append(teamList.get(i).getName() + " : Level " + teamList.get(i).getTeamFacilities() + "\n");
            }
        }

        if (sb.length() > 0) {
            newsStories.get(0).add("Upgraded Facilities!>The following teams upgraded their team training facilities this off-season:\n\n" + sb);
            newsHeadlines.add("Off-Season Facilities Upgrades Boost Prestige!");
        }

    }

    /**
     * Gets whether it is hard mode.
     * Returns true is hard, false if normal.
     *
     * @return difficulty
     */
    public boolean isCareerMode() {
        return careerMode;
    }

    //Set Up Database of Names
    private void setupNamesDB(String namesCSV, String lastNamesCSV) {
        // Read first names from file
        String[] namesSplit = namesCSV.split(",");
        for (String n : namesSplit) {
            if (isNameValid(n.trim()))
                nameList.add(n.trim());
        }

        // Read last names from file
        namesSplit = lastNamesCSV.split(",");
        for (String n : namesSplit) {
            if (isNameValid(n.trim()))
                lastNameList.add(n.trim());
        }
    }

    /**
     * Gets a random player name.
     *
     * @return random name
     */
    public String getRandName() {
        String name;
        int fn = (int) (Math.random() * nameList.size());
        int ln = (int) (Math.random() * lastNameList.size());
        name = nameList.get(fn) + " " + lastNameList.get(ln);
        return name;
    }
    
    //Set Up Team Benchmarks for Goals
    public void setTeamBenchMarks() {
        setTeamRanks();

        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).setupTeamBenchmark();
        }

        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).projectTeamWins();
            teamList.get(i).projectPollRank();
        }

        Collections.sort(teamList, new CompTeamProjPoll());
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).setProjectedPollRank(i + 1);
        }

        leagueOffTal = getAverageOffTalent();
        leagueDefTal = getAverageDefTalent();
        leagueChemistry = getAverageTeamChemistry();
    }

    /**
     * Gets the current year, starting from 2017
     *
     * @return the current year
     */
    public int getYear() {
        return seasonStart + leagueHistory.size();
    }

    public Player getHeismanWinner() {
        return heisman;
    }

    //Return homeState name

    public String getRegion(int region) {
        String location;
        if (region == 0) location = "West";
        else if (region == 1) location = "Midwest";
        else if (region == 2) location = "Central";
        else if (region == 3) location = "East";
        else location = "South";
        return location;
    }

    /**
     * Get list of teams and their prestige, used for selecting when a new game is started
     *
     * @return array of all the teams
     */
    public String[] getTeamListStr() {
        String[] teams = new String[teamList.size()];
        for (int i = 0; i < teamList.size(); ++i) {
            teams[i] = teamList.get(i).getConference() + ":  " + teamList.get(i).getName() + "  [" + teamList.get(i).getTeamPrestige() + "]";
        }
        return teams;
    }

    /**
     * Find team based on a name
     *
     * @param name team name
     * @return reference to the Team object
     */
    public Team findTeam(String name) {
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).strRep().equals(name)) {
                return teamList.get(i);
            }
        }
        return teamList.get(0);
    }

    /**
     * Find team based on a abbr
     *
     * @param abbr team abbr
     * @return reference to the Team object
     */
    public Team findTeamAbbr(String abbr) {
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).getAbbr().equals(abbr)) {
                return teamList.get(i);
            }
        }
        return teamList.get(0);
    }

    /**
     * Find conference based on a name
     *
     * @param name conf name
     * @return reference to the Conference object
     */
    public Conference findConference(String name) {
        for (int i = 0; i < conferences.size(); i++) {
            if (conferences.get(i).confName.equals(name)) {
                return conferences.get(i);
            }
        }
        return conferences.get(0);
    }

    public void updateTeamConf(String newConf, String oldConf, int x) {
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).getConference().equals(oldConf)) {
                teamList.get(i).setConference(newConf);
            }
        }
        conferences.get(x).confName = newConf;
    }

    /**
     * Get conference number from string
     *
     * @param conf conference name
     * @return int of number 0-5
     */
    public int getConfNumber(String conf) {
        boolean complete = false;
        int i = 0;
        while (complete == false) {
            if (conf.equals(conferences.get(i).confName)) {
                return i;
            } else i++;
        }
        return i;
    }

    public void sortTeamList() {
        Collections.sort(teamList, new CompTeamPrestige());
    }

    /**
     * Update all teams off talent, def talent, etc
     */
    public void updateTeamTalentRatings() {
        for (Team t : teamList) {
            t.updateTalentRatings();
        }
        for (Conference c : conferences) {
            c.updateConfRankings();
        }
    }

    //Sort Hall of Fame

    public void sortHallofFame() {

    }

    //Reset Team Playbooks to Head Coach after Loading Custom Data
    public void resetPlaybooks() {
        for(Team t : teamList) {
            t.setPlaybookOffNum(t.getOC().offStrat);
            t.setPlaybookDefNum(t.getDC().defStrat);
            t.setPlaybookOffense(t.getPlaybookOff()[t.getPlaybookOffNum()]);
            t.setPlaybookDefense(t.getPlaybookDef()[t.getPlaybookDefNum()]);
        }
    }

    //Return League Average Off Yards
    public int getAverageYards() {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getTeamYards();
        }
        average = average / teamList.size();
        return average;
    }

    //Return league average offensive talent
    public int getAverageOffTalent() {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getOffTalent();
        }
        average = average / teamList.size();
        return average;
    }

    //Return league average defensive talent
    public int getAverageDefTalent() {
        int average = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            average += teamList.get(i).getDefTalent();
        }
        average = average / teamList.size();
        return average;
    }

    //Determine League Average Conference Prestige
    public int getAverageConfPrestige() {
        int avgPrestige = 0;
        int countC = 0;
        for (int i = 0; i < conferences.size(); ++i) {
            conferences.get(i).updateConfPrestige();
        }
        for (int i = 0; i < conferences.size(); ++i) {
            if(conferences.get(i).confTeams.size() > conferences.get(i).minConfTeams) {
                avgPrestige += conferences.get(i).confPrestige;
                countC++;
            }
        }

        if (countC > 0) return avgPrestige / countC;
        else return 0;
    }

    //get League Avg Chemistry
    public double getAverageTeamChemistry() {
        double avg = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            avg += teamList.get(i).getTeamChemistry();
        }
        return avg / teamList.size();
    }


    //News on opening weekend
    public void preseasonNews() {
        coachingHotSeat();

        topRecruits();

        //Add Big Games of the Week
        for (int i = 0; i < conferences.size(); ++i) {
            conferences.get(i).newsNSMatchups();
        }
        newsHeadlines.add("College Football Season Kick-Off");
    }

    //Get a list of Top Recruits for News
    public void topRecruits() {
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i) != userTeam) {
                teamList.get(i).redshirtCPUPlayers();
            }
            teamList.get(i).getLeagueFreshman();
        }
        Collections.sort(freshmen, new CompPlayer());
        Collections.sort(redshirts, new CompPlayer());

        StringBuilder newsFreshman = new StringBuilder();
        for (int i = 0; i < 25; ++i) {
            newsFreshman.append((i + 1) + ". " + freshmen.get(i).position + " " + freshmen.get(i).name + ", " + freshmen.get(i).team.getName() + " : Ovr: " + freshmen.get(i).ratOvr + "\n\n");
        }
        StringBuilder newsRedshirts = new StringBuilder();
        for (int i = 0; i < 25; ++i) {
            newsRedshirts.append((i + 1) + ". " + redshirts.get(i).position + " " + redshirts.get(i).name + ", " + redshirts.get(i).team.getName() + " : Ovr: " + redshirts.get(i).ratOvr + "\n\n");
        }

        newsStories.get(0).add("Impact Freshmen>This year's top freshmen who are expected to play right away:\n\n" + newsFreshman);
        newsStories.get(0).add("Top Incoming Redshirted Recruits>The following list is this year's top redshirts. Their respective teams decided to sit them out this season, in hopes of progressing their talent further for next year.\n\n" + newsRedshirts);
        newsHeadlines.add("Impact Freshman and Redshirts List Announced");
    }

    public void penalizeTeams() {


        //Infractions v2
        //Based closer to NCAA Football series
        //If Discipline score drops below threshold, penalties ensue

        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).getTeamDisciplineScore() < 25 && !teamList.get(i).isRecentPenalty()) {
                teamList.get(i).setPenalized(true);
                teamList.get(i).setRecentPenalty(true);
                teamList.get(i).setTeamPrestige((int)(teamList.get(i).getTeamPrestige() - teamList.get(i).getTeamPrestige() * 0.12));
                teamList.get(i).setTeamBudget((int)(teamList.get(i).getTeamBudget() - teamList.get(i).getTeamBudget() * 0.12));

                teamList.get(i).getHeadCoach().contractLength = Math.max(1, teamList.get(i).getHeadCoach().contractLength - 2);
                if(!teamList.get(i).isUserControlled() && Math.random() < .15) {
                    teamList.get(i).midSeasonFiring();
                }

            } else if (teamList.get(i).getTeamDisciplineScore() <= 0) {
                teamList.get(i).setBowlBan(true);
                teamList.get(i).setTeamPrestige((int)(teamList.get(i).getTeamPrestige() - teamList.get(i).getTeamPrestige() * 0.25));
                teamList.get(i).setTeamBudget((int)(teamList.get(i).getTeamBudget() - teamList.get(i).getTeamBudget() * 0.25));
                teamList.get(i).setTeamDisciplineScore(60);
                teamList.get(i).setPenalized(false);
                teamList.get(i).setRecentPenalty(false);

                if(!teamList.get(i).isUserControlled()) {
                    teamList.get(i).midSeasonFiring();
                } else {
                    teamList.get(i).getHeadCoach().contractLength = 1;
                }

            }
        }
    }

    public void upgradeFacilities() {


        //Team Facilities Upgrade -- if teams have enough cash, they will spend on this. helps progression of players
        int baselineCost = 17500;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i).getTeamBudget() > baselineCost * (teamList.get(i).getTeamFacilities() + 1)) {
                //spend cash, upgrade facilities
                teamList.get(i).setTeamBudget(teamList.get(i).getTeamBudget() - baselineCost * (teamList.get(i).getTeamFacilities() + 1));
                teamList.get(i).setFacilityUpgrade(true);
                teamList.get(i).setTeamFacilities(teamList.get(i).getTeamFacilities() + 1);
                teamList.get(i).setTeamPrestige(teamList.get(i).getTeamPrestige() + teamList.get(i).getTeamFacilities());
                teamList.get(i).getHeadCoach().baselinePrestige += teamList.get(i).getTeamFacilities();
            }
        }
    }

    //Simulates Each Game Week
    public void playWeek() {
        //focus on the best player at a position this year each week
        playerSpotlight();

        updateSuspensions();

        disciplineAction();

        //Clear "next week" scoreboard blank data
        weeklyScores.get(currentWeek+1).clear();

        if (currentWeek <= regSeasonWeeks-1) {
            for (int i = 0; i < conferences.size(); ++i) {
                System.out.println("DEBUG: Playing conference " + conferences.get(i).confName);
                conferences.get(i).playWeek();
            }
        }

        if (currentWeek == regSeasonWeeks-1) {
            //bowl week
            for (int i = 0; i < teamList.size(); ++i) {
                teamList.get(i).updatePollScore();
            }
            Collections.sort(teamList, new CompTeamPoll());

            if (expPlayoffs) {
                scheduleExpPlayoff();
            } else {
                scheduleNormalCFP();
            }

        } else if (currentWeek == regSeasonWeeks) {
            ArrayList<Player> heismans = getHeisman();
            if (defPOTYCandidates == null || defPOTYCandidates.isEmpty()) {
                defPOTYCandidates = getDefPOTY();
            }
            heismanHistory.add(heismans.get(0).position + " " + heismans.get(0).getInitialName() + " [" + heismans.get(0).getYrStr() + "], "
                    + heismans.get(0).team.getAbbr() + " (" + heismans.get(0).team.getWins() + "-" + heismans.get(0).team.getLosses() + ")>" +
                    defPOTYCandidates.get(0).position + " " + defPOTYCandidates.get(0).getInitialName() + " [" + defPOTYCandidates.get(0).getYrStr() + "], "
                    + defPOTYCandidates.get(0).team.getAbbr() + " (" + defPOTYCandidates.get(0).team.getWins() + "-" + defPOTYCandidates.get(0).team.getLosses() + ")");

            if (expPlayoffs) playExpandedPlayoffFirstRound();
            else playBowlWeek1();

        } else if (currentWeek == regSeasonWeeks+1) {
            if (expPlayoffs) playExpandedPlayoffQuarterfinals();
            else playBowlWeek2();

        } else if (currentWeek == regSeasonWeeks+2) {
            if (expPlayoffs) playExpandedPlayoffSemifinals();
            else playBowlWeek3();

        } else if (currentWeek == regSeasonWeeks+3) {
            ncg.playGame();
            if (ncg.homeScore > ncg.awayScore) {
                ncg.homeTeam.setSemiFinalWL("");
                ncg.awayTeam.setSemiFinalWL("");
                ncg.homeTeam.setNatChampWL("NCW");
                ncg.awayTeam.setNatChampWL("NCL");
                ncg.homeTeam.incrementTotalNCs();
                ncg.awayTeam.incrementTotalNCLosses();
                ncg.homeTeam.getHeadCoach().recordNCWins(1);
                ncg.awayTeam.getHeadCoach().recordNCLosses(1);
                newsStories.get(currentWeek + 1).add(
                        ncg.homeTeam.getName() + " wins the National Championship!>" +
                                ncg.homeTeam.getName() + " defeats " + ncg.awayTeam.getName() +
                                " in the national championship game " + ncg.homeScore + " to " + ncg.awayScore + "." +
                                " Congratulations " + ncg.homeTeam.getName() + "!"
                );
                newsHeadlines.add(ncg.homeTeam.getName() + " wins the National Championship!");

            } else {
                ncg.homeTeam.setSemiFinalWL("");
                ncg.awayTeam.setSemiFinalWL("");
                ncg.awayTeam.setNatChampWL("NCW");
                ncg.homeTeam.setNatChampWL("NCL");
                ncg.awayTeam.incrementTotalNCs();
                ncg.homeTeam.incrementTotalNCLosses();
                ncg.awayTeam.getHeadCoach().recordNCWins(1);
                ncg.homeTeam.getHeadCoach().recordNCLosses(1);
                newsStories.get(currentWeek + 1).add(
                        ncg.awayTeam.getName() + " wins the National Championship!>" +
                                ncg.awayTeam.getName() + " defeats " + ncg.homeTeam.getName() +
                                " in the national championship game " + ncg.awayScore + " to " + ncg.homeScore + "." +
                                " Congratulations " + ncg.awayTeam.getName() + "!"
                );
                newsHeadlines.add(ncg.awayTeam.getName() + " wins the National Championship!");

            }
        }
        //add news regarding CFB Playoff Committee
        cfbPlayoffsNews();

        //add upcoming matches of the week
        for (int i = 0; i < conferences.size(); ++i) {
            conferences.get(i).newsMatchups();
        }


        System.out.println("DEBUG: Entering coachingHotSeat");
        coachingHotSeat();

        System.out.println("DEBUG: Entering setTeamRanks");
        setTeamRanks();
        
        System.out.println("DEBUG: Entering updateWinStreak");
        updateLongestActiveWinStreak();

        currentWeek++;
        System.out.println("DEBUG: Week advanced to " + currentWeek);
    }


    //Coaching Discipline Opportunities
    private void disciplineAction() {
        teamDiscipline = new ArrayList<>();
        String news = "";
        for (int t = 0; t < teamList.size(); ++t) {
            double disChance = disciplineChance;
            if(teamList.get(t).getTeamDisciplineScore() < 50 || teamList.get(t).getRankTeamPrestige() < teamList.size()*.20) disChance += disciplineScrutiny;
            else if(Math.random() < 0.33) disChance += disciplineScrutiny;

            if (Math.random() < disChance) {
                int teamDis = teamList.get(t).getTeamDiscipline();

                if ((int) (Math.random() * (100 - teamDis)) > (int) (Math.random() * teamList.get(t).getHeadCoach().ratDiscipline)) {
                    teamDiscipline.add(teamList.get(t).getName());
                    teamList.get(t).disciplineFailure();
                } else {
                    teamList.get(t).disciplineSuccess();
                }
            }

        }
        for (int i = 0; i < teamDiscipline.size(); ++i) {
            news += "\n" + teamDiscipline.get(i);
        }
        newsStories.get(currentWeek + 1).add("In-Season Disciplinary Action>The following teams have had issues with discipline in the past week:\n" + news);
    }

    private void updateSuspensions() {
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updateSuspensions();
        }
    }

    //Player Spotlight
    private void playerSpotlight() {
        ArrayList<PlayerQB> QB = rankQB();
        ArrayList<PlayerRB> RB = rankRB();
        ArrayList<PlayerWR> WR = rankWR();
        ArrayList<PlayerDL> DL = rankDL();
        ArrayList<PlayerLB> LB = rankLB();
        ArrayList<PlayerCB> CB = rankCB();
        ArrayList<PlayerS> S = rankS();
        if (currentWeek == 5) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + S.get(0).getYrStr() + " safety, " + S.get(0).name + ", has been cleaning up in the back this year helping " + S.get(0).team.getName() +
                    " to a record of " + S.get(0).team.strTeamRecord() + ". The safety has made " + S.get(0).getTackles() + " tackles and sacked the QB " + S.get(0).getSacks() + " times this year. In coverage, he's recovered " +
                    S.get(0).getFumblesRec() + " fumbles and intercepted opposing QBs " + S.get(0).getInterceptions() + " times this year. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + S.get(0).team.getName() + " " + S.get(0).getYrStr() + " Safety, " + S.get(0).name);

        } else if (currentWeek == 6) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + QB.get(0).getYrStr() + " quarterback, " + QB.get(0).name + ", is one of the top players at his position in the nation this year. He has led " + QB.get(0).team.getName() +
                    " to a record of " + QB.get(0).team.strTeamRecord() + ". He has passed for " + QB.get(0).getPassYards() + " yards this season, and thrown " + QB.get(0).getPassTD() + " touchdowns. " +
                    "He's also carried the ball for " + QB.get(0).getRushYards() + " yards this season. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + QB.get(0).team.getName() + " " + QB.get(0).getYrStr() + " QB, " + QB.get(0).name);
            
        } else if (currentWeek == 7) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + WR.get(0).getYrStr() + " wide receiver, " + WR.get(0).name + ", has been flying pass defensive coverages this year helping " + WR.get(0).team.getName() +
                    " to a record of " + WR.get(0).team.strTeamRecord() + ". The receiver has caught " + WR.get(0).getReceptions() + " for " + WR.get(0).getRecYards() + " yards this year. He's found the end zone " + WR.get(0).getRecTDs() +
                    " times. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + WR.get(0).team.getName() + " " + WR.get(0).getYrStr() + " WR, " + WR.get(0).name);
            
        } else if (currentWeek == 8) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + LB.get(0).getYrStr() + " linebacker, " + LB.get(0).name + ", has been blowing up offenses this year helping " + LB.get(0).team.getName() +
                    " to a record of " + LB.get(0).team.strTeamRecord() + ". The linebacker has made " + LB.get(0).getTackles() + " tackles and sacked the QB " + LB.get(0).getSacks() + " times this year. In coverage, he's recovered " +
                    LB.get(0).getFumblesRec() + " fumbles and intercepted opposing QBs " + LB.get(0).getInterceptions() + " times this year. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + LB.get(0).team.getName() + " " + LB.get(0).getYrStr() + " LB, " + LB.get(0).name);

        } else if (currentWeek == 9) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + DL.get(0).getYrStr() + " defensive lineman, " + DL.get(0).name + ", has been disrupting offensive lines this year helping " + DL.get(0).team.getName() +
                    " to a record of " + DL.get(0).team.strTeamRecord() + ". The lineman has made " + DL.get(0).getTackles() + " tackles and sacked the QB " + DL.get(0).getSacks() + " times this year.He's also recovered " +
                    DL.get(0).getFumblesRec() + " fumbles this year. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + DL.get(0).team.getName() + " " + DL.get(0).getYrStr() + " Defensive Lineman, " + DL.get(0).name);

        } else if (currentWeek == 10) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + RB.get(0).getYrStr() + " running back, " + RB.get(0).name + ", has been finding holes in opposing defenses this season for " + RB.get(0).team.getName() +
                    " as they compiled a record of " + RB.get(0).team.strTeamRecord() + ". The running back has rushed for " + RB.get(0).getRushYards() + " yards and scored " + RB.get(0).getRushTDs() + " times this year. " +
                    "In the passing game, he's caught " + RB.get(0).getReceptions() + " for " + RB.get(0).getRecYards() + " and scored " + RB.get(0).getRecTDs() + " touchdowns in the air this year. " +
                    "Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + RB.get(0).team.getName() + " " + RB.get(0).getYrStr() + " RB, " + RB.get(0).name);

        } else if (currentWeek == 11) {
            newsStories.get(currentWeek + 1).add("Player Spotlight>" + CB.get(0).getYrStr() + " cornerback, " + CB.get(0).name + ", has been shutting down opposing receivers this year helping " + CB.get(0).team.getName() +
                    " to a record of " + CB.get(0).team.strTeamRecord() + ". The corner has made " + CB.get(0).getTackles() + " tackles and sacked the QB " + CB.get(0).getSacks() + " times this year. In coverage, he's recovered " +
                    CB.get(0).getFumblesRec() + " fumbles and intercepted opposing QBs " + CB.get(0).getInterceptions() + " times this year. Look for him to be in the year end running for Player of the Year.");
            newsHeadlines.add("Player Spotlight: " + CB.get(0).team.getName() + " " + CB.get(0).getYrStr() + " CB, " + CB.get(0).name);
        }
    }

    //Committee News
    private void cfbPlayoffsNews() {
        setTeamRanks();
        ArrayList<Team> teams = teamList;
        Collections.sort(teams, new CompTeamPoll());

        if (currentWeek == 8) {
            newsStories.get(currentWeek + 1).add("Committee Announces First Playoff Rankings>The College Football Playoffs Committee has set ther initial rankings for this season's playoffs. The first look at the playoffs have " +
                    teams.get(0).getName() + " at the top of the list. The rest of the playoff order looks like this:\n\n" + "1. " + teams.get(0).getStrAbbrWL() + "\n" + "2. " + teams.get(1).getStrAbbrWL() + "\n" + "3. " +
                    teams.get(2).getStrAbbrWL() + "\n" + "4. " + teams.get(3).getStrAbbrWL() + "\n" + "5. " + teams.get(4).getStrAbbrWL() + "\n" + "6. " + teams.get(5).getStrAbbrWL() + "\n" + "7. " +
                    teams.get(6).getStrAbbrWL() + "\n" + "8. " + teams.get(7).getStrAbbrWL() + "\n");
        }
        if (currentWeek > 8 && currentWeek < regSeasonWeeks-1) {
            newsStories.get(currentWeek + 1).add("Committee Updates Rankings>The College Football Playoff Committee has updated their Playoff Rankings. The order looks like this: \n\n" + "1. " + teams.get(0).getStrAbbrWL() +
                    "\n" + "2. " + teams.get(1).getStrAbbrWL() + "\n" + "3. " + teams.get(2).getStrAbbrWL() + "\n" + "4. " + teams.get(3).getStrAbbrWL() + "\n" + "5. " + teams.get(4).getStrAbbrWL() + "\n" + "6. " +
                    teams.get(5).getStrAbbrWL() + "\n" + "7. " + teams.get(6).getStrAbbrWL() + "\n" + "8. " + teams.get(7).getStrAbbrWL() + "\n");
        }
    }


    /**
     * Calculates who wins the Heisman.
     *
     * @return Heisman Winner
     */
    private ArrayList<Player> getHeisman() {
        ArrayList<Player> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            //qb
            for (int qb = 0; qb < teamList.get(i).getTeamQBs().size(); ++qb) {
                heismanCandidates.add(teamList.get(i).getTeamQBs().get(qb));
            }

            //rb
            for (int rb = 0; rb < teamList.get(i).getTeamRBs().size(); ++rb) {
                heismanCandidates.add(teamList.get(i).getTeamRBs().get(rb));
            }

            //wr
            for (int wr = 0; wr < teamList.get(i).getTeamWRs().size(); ++wr) {
                heismanCandidates.add(teamList.get(i).getTeamWRs().get(wr));
            }

            //te
            for (int te = 0; te < teamList.get(i).getTeamTEs().size(); ++te) {
                heismanCandidates.add(teamList.get(i).getTeamTEs().get(te));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());

        return heismanCandidates;
    }

    /**
     * Perform the heisman ceremony. Congratulate winner and give top 5 vote getters.
     *
     * @return string of the heisman ceremony.
     */
    public String getHeismanCeremonyStr() {
        boolean putNewsStory = false;
        if (!heismanDecided) {
            heismanDecided = true;
            heismanCandidates = getHeisman();
            heisman = heismanCandidates.get(0);
            heisman.wonHeisman = true;
            heisman.recordHeismans(1);
            heisman.team.getHeadCoach().recordHeismans(1);
            putNewsStory = true;
            //full results string
            String heismanTop5 = "\n";
            for (int i = 0; i < 5; ++i) {
                Player p = heismanCandidates.get(i);
                heismanTop5 += (i + 1) + ". " + p.getAwardStats();
            }

            String heismanWinnerStr = "Congratulations to the Offensive Player of the Year, " + heisman.getAwardDescription();
            String heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;

            // Add news story
            if (putNewsStory) {
                newsStories.get(currentWeek + 1).add(heisman.name + " is the Offensive Player of the Year!>" + heismanWinnerStr);
                newsHeadlines.add(heisman.team.getName() + " " + " " + heisman.position + " " + heisman.name + " is the Offensive Player of the Year!");
                heismanWinnerStrFull = heismanStats;
            }

            return heismanStats;
        } else {
            return heismanWinnerStrFull;
        }
    }

    /**
     * Calculates who wins the Heisman.
     *
     * @return Heisman Winner
     */
    private ArrayList<Player> getDefPOTY() {
        ArrayList<Player> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            //dl
            for (int dl = 0; dl < teamList.get(i).getTeamDLs().size(); ++dl) {
                heismanCandidates.add(teamList.get(i).getTeamDLs().get(dl));
            }
            //lb
            for (int lb = 0; lb < teamList.get(i).getTeamLBs().size(); ++lb) {
                heismanCandidates.add(teamList.get(i).getTeamLBs().get(lb));
            }

            //cb
            for (int cb = 0; cb < teamList.get(i).getTeamCBs().size(); ++cb) {
                heismanCandidates.add(teamList.get(i).getTeamCBs().get(cb));
            }

            //s
            for (int s = 0; s < teamList.get(i).getTeamSs().size(); ++s) {
                heismanCandidates.add(teamList.get(i).getTeamSs().get(s));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());

        return heismanCandidates;
    }

    /**
     * Perform the heisman ceremony. Congratulate winner and give top 5 vote getters.
     *
     * @return string of the heisman ceremony.
     */
    public String getDefensePOTYStr() {
        boolean putNewsStory = false;
        if (!heismanDecided) {
            defPOTYCandidates = getDefPOTY();
            defPOTY = defPOTYCandidates.get(0);
            defPOTY.wonHeisman = true;
            defPOTY.recordHeismans(1);
            defPOTY.team.getHeadCoach().recordHeismans(1);
            putNewsStory = true;
            //full results string
            String heismanTop5 = "\n";
            for (int i = 0; i < 5; ++i) {
                Player p = defPOTYCandidates.get(i);
                heismanTop5 += (i + 1) + ". " + p.getAwardStats();
            }

            String heismanWinnerStr = "Congratulations to the Defensive Player of the Year, " + defPOTY.getAwardDescription();
            String heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;

            // Add news story
            if (putNewsStory) {
                newsStories.get(currentWeek + 1).add(defPOTY.name + " is the Defensive Player of the Year!>" + heismanWinnerStr);
                newsHeadlines.add(defPOTY.team.getName() + " " + " " + defPOTY.position + " " + defPOTY.name + " is the Defensive Player of the Year!");
                defPOTYWinnerStrFull = heismanStats;
            }

            return heismanStats;
        } else {
            return defPOTYWinnerStrFull;
        }
    }

    private ArrayList<Player> getTopFreshman() {
        freshman = null;
        ArrayList<Player> freshmanCandidates = new ArrayList<>();
        fQBs = new ArrayList<>();
        fRBs = new ArrayList<>();
        fWRs = new ArrayList<>();
        fTEs = new ArrayList<>();
        fKs = new ArrayList<>();
        fOLs = new ArrayList<>();
        fDLs = new ArrayList<>();
        fLBs = new ArrayList<>();
        fCBs = new ArrayList<>();
        fSs = new ArrayList<>();

        for (int i = 0; i < teamList.size(); ++i) {
            //qb
            for (int qb = 0; qb < teamList.get(i).getTeamQBs().size(); ++qb) {
                if (teamList.get(i).getTeamQBs().get(qb).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamQBs().get(qb));
                    fQBs.add(teamList.get(i).getTeamQBs().get(qb));
                }
            }

            //rb
            for (int rb = 0; rb < teamList.get(i).getTeamRBs().size(); ++rb) {
                if (teamList.get(i).getTeamRBs().get(rb).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamRBs().get(rb));
                    fRBs.add(teamList.get(i).getTeamRBs().get(rb));
                }
            }

            //wr
            for (int wr = 0; wr < teamList.get(i).getTeamWRs().size(); ++wr) {
                if (teamList.get(i).getTeamWRs().get(wr).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamWRs().get(wr));
                    fWRs.add(teamList.get(i).getTeamWRs().get(wr));
                }
            }

            //te
            for (int te = 0; te < teamList.get(i).getTeamTEs().size(); ++te) {
                if (teamList.get(i).getTeamTEs().get(te).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamTEs().get(te));
                    fTEs.add(teamList.get(i).getTeamTEs().get(te));
                }
            }

            //ol
            for (int ol = 0; ol < teamList.get(i).getTeamOLs().size(); ++ol) {
                if (teamList.get(i).getTeamOLs().get(ol).year == 1) {
                    //freshmanCandidates.add(teamList.get(i).getTeamOLs().get(ol));
                    fOLs.add(teamList.get(i).getTeamOLs().get(ol));
                }
            }

            //k
            for (int k = 0; k < teamList.get(i).getTeamKs().size(); ++k) {
                if (teamList.get(i).getTeamKs().get(k).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamKs().get(k));
                    fKs.add(teamList.get(i).getTeamKs().get(k));
                }
            }


            //dl
            for (int dl = 0; dl < teamList.get(i).getTeamDLs().size(); ++dl) {
                if (teamList.get(i).getTeamDLs().get(dl).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamDLs().get(dl));
                    fDLs.add(teamList.get(i).getTeamDLs().get(dl));
                }
            }

            //lb
            for (int lb = 0; lb < teamList.get(i).getTeamLBs().size(); ++lb) {
                if (teamList.get(i).getTeamLBs().get(lb).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamLBs().get(lb));
                    fLBs.add(teamList.get(i).getTeamLBs().get(lb));
                }
            }

            //cb
            for (int cb = 0; cb < teamList.get(i).getTeamCBs().size(); ++cb) {
                if (teamList.get(i).getTeamCBs().get(cb).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamCBs().get(cb));
                    fCBs.add(teamList.get(i).getTeamCBs().get(cb));
                }
            }

            //s
            for (int s = 0; s < teamList.get(i).getTeamSs().size(); ++s) {
                if (teamList.get(i).getTeamSs().get(s).year == 1) {
                    freshmanCandidates.add(teamList.get(i).getTeamSs().get(s));
                    fSs.add(teamList.get(i).getTeamSs().get(s));
                }
            }
        }
        Collections.sort(freshmanCandidates, new CompPlayerHeisman());
        Collections.sort(fQBs, new CompPlayerHeisman());
        Collections.sort(fRBs, new CompPlayerHeisman());
        Collections.sort(fWRs, new CompPlayerHeisman());
        Collections.sort(fTEs, new CompPlayerHeisman());
        Collections.sort(fOLs, new CompPlayerHeisman());
        Collections.sort(fKs, new CompPlayerHeisman());
        Collections.sort(fDLs, new CompPlayerHeisman());
        Collections.sort(fLBs, new CompPlayerHeisman());
        Collections.sort(fCBs, new CompPlayerHeisman());
        Collections.sort(fSs, new CompPlayerHeisman());

        return freshmanCandidates;
    }

    /**
     * Gets All Freshman
     *
     * @return string list of all americans
     */
    public String getAllFreshmanStr() {
        if (allFreshman.isEmpty()) {

            if (fQBs.size() > 0) {
                allFreshman.add(fQBs.get(0));
                fQBs.get(0).wonAllFreshman = true;
                fQBs.get(0).team.getHeadCoach().recordAllFreshman(1);
            }

            if (fRBs.size() > 1) {
                allFreshman.add(fRBs.get(0));
                fRBs.get(0).wonAllFreshman = true;
                fRBs.get(0).team.getHeadCoach().recordAllFreshman(1);

                allFreshman.add(fRBs.get(1));
                fRBs.get(1).wonAllFreshman = true;
                fRBs.get(1).team.getHeadCoach().recordAllFreshman(1);

            }

            if (fWRs.size() > 2) {
                for (int i = 0; i < 3; ++i) {
                    allFreshman.add(fWRs.get(i));
                    fWRs.get(i).wonAllFreshman = true;
                    fWRs.get(i).team.getHeadCoach().recordAllFreshman(1);

                }
            }

            if (fTEs.size() > 0) {
                allFreshman.add(fTEs.get(0));
                fTEs.get(0).wonAllFreshman = true;
                fTEs.get(0).team.getHeadCoach().recordAllFreshman(1);

            }

            if (fOLs.size() > 4) {
                for (int i = 0; i < 5; ++i) {
                    allFreshman.add(fOLs.get(i));
                    fOLs.get(i).wonAllFreshman = true;
                    fOLs.get(i).team.getHeadCoach().recordAllFreshman(1);
                }
            }

            if (fKs.size() > 0) {
                allFreshman.add(fKs.get(0));
                fKs.get(0).wonAllFreshman = true;
                fKs.get(0).team.getHeadCoach().recordAllFreshman(1);
            }

            if (fDLs.size() > 3) {
                for (int i = 0; i < 4; ++i) {
                    allFreshman.add(fDLs.get(i));
                    fDLs.get(i).wonAllFreshman = true;
                    fDLs.get(i).team.getHeadCoach().recordAllFreshman(1);
                }
            }

            if (fLBs.size() > 2) {
                for (int i = 0; i < 3; ++i) {
                    allFreshman.add(fLBs.get(i));
                    fLBs.get(i).wonAllFreshman = true;
                    fLBs.get(i).team.getHeadCoach().recordAllFreshman(1);
                }
            }

            if (fCBs.size() > 2) {
                for (int i = 0; i < 3; ++i) {
                    allFreshman.add(fCBs.get(i));
                    fCBs.get(i).wonAllFreshman = true;
                    fCBs.get(i).team.getHeadCoach().recordAllFreshman(1);
                }
            }
            if (fSs.size() > 1) {
                for (int i = 0; i < 2; ++i) {
                    allFreshman.add(fSs.get(i));
                    fSs.get(i).wonAllFreshman = true;
                    fSs.get(i).team.getHeadCoach().recordAllFreshman(1);
                }
            }
        }

        StringBuilder allFreshmanTeam = new StringBuilder();
        for (int i = 0; i < allFreshman.size(); ++i) {
            Player p = allFreshman.get(i);
            allFreshmanTeam.append(p.team.getAbbr() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")" + " - ");
            allFreshmanTeam.append(" " + p.getAllTeamStats());
            allFreshmanTeam.append(" \t\tOverall: " + p.ratOvr + "\n\n>");
        }

        return allFreshmanTeam.toString();
    }

    /**
     * Perform the heisman ceremony. Congratulate winner and give top 5 vote getters.
     *
     * @return string of the heisman ceremony.
     */
    public String getFreshmanCeremonyStr() {
        boolean putNewsStory = false;
        if (!heismanDecided) {
            freshmanCandidates = getTopFreshman();
            freshman = freshmanCandidates.get(0);
            freshman.wonTopFreshman = true;
            freshman.team.getHeadCoach().recordTopFreshman(1);
            putNewsStory = true;
            //full results string
            String heismanTop5 = "\n";
            for (int i = 0; i < 5; ++i) {
                Player p = freshmanCandidates.get(i);
                heismanTop5 += (i + 1) + ". " + p.getAwardStats();
            }

            String heismanWinnerStr = "Congratulations to the Freshman Player of the Year, " + freshman.getAwardDescription();
            String heismanStats = heismanWinnerStr + "\n\nFull Results:" + heismanTop5;

            // Add news story
            if (putNewsStory) {
                newsStories.get(currentWeek + 1).add(freshman.name + " is the Freshman Player of the Year!>" + heismanWinnerStr);
                newsHeadlines.add(freshman.team.getName() + " " + " " + freshman.position + " " + freshman.name + " is the Freshman Player of the Year!");
                freshmanWinnerStrFull = heismanStats;
            }

            return heismanStats;

        } else {
            return freshmanWinnerStrFull;
        }
    }

    public String getCoachAwardStr() {
        if (!heismanDecided) {
            ArrayList<HeadCoach> coachCandidates = rankHC();
            coachWinner = coachCandidates.get(0);
            String coachAwardTopList = "";
            for (int i = 0; i < 5; ++i) {
                HeadCoach p = coachCandidates.get(i);
                HeadCoach hc = (HeadCoach) p;
                coachAwardTopList += (i + 1) + ". " + hc.name + ": " + ((HeadCoach) p).getCoachScore() + " votes\n";
                coachAwardTopList += p.team.getName() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")  Overall: " + hc.ratOvr + "\n\n";
            }
            String coachStats = "";
            String coachWinnerStr = "";
            coachWinnerStr = "Congratulations to the Head Coach of the Year, " + coachWinner.name + "!\n\nHe led " + coachWinner.team.getName() +
                    " to a " + coachWinner.team.getWins() + "-" + coachWinner.team.getLosses() + " record and a #" + coachWinner.team.getRankTeamPollScore() +
                    " poll ranking.";
            coachStats = coachWinnerStr + "\n\nFull Results:\n\n" + coachAwardTopList;

            newsStories.get(currentWeek + 1).add("Head Coach of the Year Announced>This year's top head coach award was given to " + coachWinner.name +
                    " of " + coachWinner.team.getName() + ".");

            newsHeadlines.add(coachWinner.team.getName() + " " + coachWinner.name + " is the Head Coach of the Year!");
            coachCandidates.get(0).recordCOTY(1);
            coachWinnerStrFull = coachStats;
            coachCandidates.get(0).wonTopHC = true;


            return coachStats;
        } else {
            return coachWinnerStrFull;
        }
    }

    /**
     * Gets All Americans, best of all conference teams
     *
     * @return string list of all americans
     */
    public String getAllAmericanStr() {
        if (allAmericans.isEmpty()) {
            ArrayList<PlayerQB> qbs = new ArrayList<>();
            ArrayList<PlayerRB> rbs = new ArrayList<>();
            ArrayList<PlayerWR> wrs = new ArrayList<>();
            ArrayList<PlayerTE> tes = new ArrayList<>();
            ArrayList<PlayerOL> ols = new ArrayList<>();
            ArrayList<PlayerK> ks = new ArrayList<>();
            ArrayList<PlayerDL> dls = new ArrayList<>();
            ArrayList<PlayerLB> lbs = new ArrayList<>();
            ArrayList<PlayerCB> cbs = new ArrayList<>();
            ArrayList<PlayerS> ss = new ArrayList<>();

            for (int t = 0; t < teamList.size(); t++) {
                Team tm = teamList.get(t);
                qbs.addAll(tm.getTeamQBs());
                rbs.addAll(tm.getTeamRBs());
                wrs.addAll(tm.getTeamWRs());
                tes.addAll(tm.getTeamTEs());
                ols.addAll(tm.getTeamOLs());
                ks.addAll(tm.getTeamKs());
                dls.addAll(tm.getTeamDLs());
                lbs.addAll(tm.getTeamLBs());
                cbs.addAll(tm.getTeamCBs());
                ss.addAll(tm.getTeamSs());
            }

            Collections.sort(qbs, new CompPlayerHeisman());
            Collections.sort(rbs, new CompPlayerHeisman());
            Collections.sort(wrs, new CompPlayerHeisman());
            Collections.sort(tes, new CompPlayerHeisman());
            Collections.sort(ols, new CompPlayerHeisman());
            Collections.sort(ks, new CompPlayerHeisman());
            Collections.sort(dls, new CompPlayerHeisman());
            Collections.sort(lbs, new CompPlayerHeisman());
            Collections.sort(cbs, new CompPlayerHeisman());
            Collections.sort(ss, new CompPlayerHeisman());

            allAmericans.add(qbs.get(0));
            qbs.get(0).wonAllAmerican = true;
            qbs.get(0).team.getHeadCoach().recordAllAmericans(1);
            allAmericans.add(rbs.get(0));
            rbs.get(0).wonAllAmerican = true;
            rbs.get(0).team.getHeadCoach().recordAllAmericans(1);
            allAmericans.add(rbs.get(1));
            rbs.get(1).wonAllAmerican = true;
            rbs.get(1).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(wrs.get(i));
                wrs.get(i).wonAllAmerican = true;
                wrs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            allAmericans.add(tes.get(0));
            tes.get(0).wonAllAmerican = true;
            tes.get(0).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 0; i < 5; ++i) {
                allAmericans.add(ols.get(i));
                ols.get(i).wonAllAmerican = true;
                ols.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            allAmericans.add(ks.get(0));
            ks.get(0).wonAllAmerican = true;
            ks.get(0).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 0; i < 4; ++i) {
                allAmericans.add(dls.get(i));
                dls.get(i).wonAllAmerican = true;
                dls.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(lbs.get(i));
                lbs.get(i).wonAllAmerican = true;
                lbs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 0; i < 3; ++i) {
                allAmericans.add(cbs.get(i));
                cbs.get(i).wonAllAmerican = true;
                cbs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 0; i < 2; ++i) {
                allAmericans.add(ss.get(i));
                ss.get(i).wonAllAmerican = true;
                ss.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            
            allAmericans2.add(qbs.get(1));
            qbs.get(1).wonAllAmerican = true;
            qbs.get(1).team.getHeadCoach().recordAllAmericans(1);
            allAmericans2.add(rbs.get(2));
            rbs.get(2).wonAllAmerican = true;
            rbs.get(2).team.getHeadCoach().recordAllAmericans(1);
            allAmericans2.add(rbs.get(3));
            rbs.get(3).wonAllAmerican = true;
            rbs.get(3).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 3; i < 6; ++i) {
                allAmericans2.add(wrs.get(i));
                wrs.get(i).wonAllAmerican = true;
                wrs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            allAmericans2.add(tes.get(1));
            tes.get(1).wonAllAmerican = true;
            tes.get(1).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 5; i < 10; ++i) {
                allAmericans2.add(ols.get(i));
                ols.get(i).wonAllAmerican = true;
                ols.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            allAmericans2.add(ks.get(1));
            ks.get(1).wonAllAmerican = true;
            ks.get(1).team.getHeadCoach().recordAllAmericans(1);
            for (int i = 4; i < 8; ++i) {
                allAmericans2.add(dls.get(i));
                dls.get(i).wonAllAmerican = true;
                dls.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 3; i < 6; ++i) {
                allAmericans2.add(lbs.get(i));
                lbs.get(i).wonAllAmerican = true;
                lbs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 3; i < 6; ++i) {
                allAmericans2.add(cbs.get(i));
                cbs.get(i).wonAllAmerican = true;
                cbs.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
            for (int i = 2; i < 4; ++i) {
                allAmericans2.add(ss.get(i));
                ss.get(i).wonAllAmerican = true;
                ss.get(i).team.getHeadCoach().recordAllAmericans(1);
            }
        }

        StringBuilder allAmerican = new StringBuilder();
        allAmerican.append("[FIRST TEAM ALL-AMERICANS]\n\n>");
        for (int i = 0; i < allAmericans.size(); ++i) {
            Player p = allAmericans.get(i);
            allAmerican.append(p.team.getAbbr() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")" + " - ");
            allAmerican.append(" " + p.getAllTeamStats());
            allAmerican.append(" \t\tOverall: " + p.ratOvr + "\n\n>");
        }
        allAmerican.append("[SECOND TEAM ALL-AMERICANS]\n\n>");
        for (int i = 0; i < allAmericans2.size(); ++i) {
            Player p = allAmericans2.get(i);
            allAmerican.append(p.team.getAbbr() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")" + " - ");
            allAmerican.append(" " + p.getAllTeamStats());
            allAmerican.append(" \t\tOverall: " + p.ratOvr + "\n\n>");
        }
        
        
        // Go through all the all conf players to get the all americans
        return allAmerican.toString();
    }

    /**
     * Get a string list of all conference team of choice
     *
     * @param confNum which conference
     * @return string of the conference team
     */
    public String getAllConfStr(int confNum) {
        ArrayList<Player> allConfPlayers = conferences.get(confNum).getAllConfPlayers();
        ArrayList<HeadCoach> allConfCoaches = conferences.get(confNum).allConfCoach;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < allConfCoaches.size(); ++i) {
            HeadCoach p = allConfCoaches.get(i);
            sb.append(p.team.getAbbr() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")" + " - ");
            if (p instanceof HeadCoach) {
                HeadCoach hc = (HeadCoach) p;
                sb.append(" HC " + hc.name + "\n \t\tAge: " + hc.age + " Season " + hc.year + "\n");
                sb.append(" \t\tOverall: " + ((HeadCoach) p).getStaffOverall(p.overallWt) + "\n\n>");
            }
        }


        for (int i = 0; i < allConfPlayers.size(); ++i) {
            Player p = allConfPlayers.get(i);
            sb.append(p.team.getAbbr() + " (" + p.team.getWins() + "-" + p.team.getLosses() + ")" + " - ");
            sb.append(" " + p.getAllTeamStats());
            sb.append(" \t\tOverall: " + p.ratOvr + "\n\n>");

        }

        return sb.toString();
    }

    /**
     * Get a list of all the CCGs and their teams
     *
     * @return
     */
    public String getCCGsStr() {
        StringBuilder sb = new StringBuilder();
        for (Conference c : conferences) {
            if (c.confTeams.size() >= c.minConfTeams) sb.append(c.getCCGStr() + "\n\n");
        }
        return sb.toString();
    }

    /**
     * Get list of all bowl games and their predicted teams
     *
     * @return string of all the bowls and their predictions
     */
    public String getBowlGameWatchStr() {
        //if bowls arent scheduled yet, give predictions
        if (!hasScheduledBowls) {

            if (expPlayoffs) {
                getExpPlayoffTeams();
                return postseason;
            } else {
                setTeamRanks();
                for (int i = 0; i < teamList.size(); ++i) {
                    teamList.get(i).updatePollScore();
                    if (teamList.get(i).isBowlBan())
                        teamList.get(i).setTeamPollScore(0);
                }
                Collections.sort(teamList, new CompTeamPoll());

                StringBuilder sb = new StringBuilder();
                Team t1;
                Team t2;

                sb.append("Semifinal 1v4:\n\t\t");
                t1 = teamList.get(0);
                t2 = teamList.get(3);
                sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

                sb.append("Semifinal 2v3:\n\t\t");
                t1 = teamList.get(1);
                t2 = teamList.get(2);
                sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

                int t = 4;
                for (int g = 0; g < bowlNames.length; g++) {
                    sb.append(bowlNames[g] + ":\n\t\t");
                    t1 = teamList.get(t);
                    t2 = teamList.get(t + 4);
                    sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");
                    t++;
                    if (t % 8 == 0) t = t + 8;
                }

                return sb.toString();

            }
        } else {

            if (expPlayoffs) {

                return postseason;

            } else {
                // Games have already been scheduled, give actual teams
                StringBuilder sb = new StringBuilder();

                sb.append("Semifinal 1v4:\n");
                sb.append(getGameSummaryBowl(semiG14));

                sb.append("\n\nSemifinal 2v3:\n");
                sb.append(getGameSummaryBowl(semiG23));

                for (int i = 0; i < bowlGames.length; ++i) {
                    if (bowlGames[i] != null) {
                        sb.append("\n\n" + bowlNames[i] + ":\n");
                        sb.append(getGameSummaryBowl(bowlGames[i]));
                    }
                }

                return sb.toString();
            }

        }
    }

    /*
    EXPANDED PLAYOFFS MODE
    */

    public void getExpPlayoffTeams() {
        playoffTeams.clear();
        
        ArrayList<Team> qualifiedTeams = getQualifiedTeams();
        ArrayList<Team> autoBids = getExpandedPlayoffAutoBids(qualifiedTeams);
        playoffTeams.addAll(autoBids);
        for (Team qualifiedTeam : qualifiedTeams) {
            if (!playoffTeams.contains(qualifiedTeam)) {
                playoffTeams.add(qualifiedTeam);
                if (playoffTeams.size() >= EXPANDED_PLAYOFF_TEAM_COUNT) {
                    break;
                }
            }
        }

        if (playoffTeams.size() < EXPANDED_PLAYOFF_TEAM_COUNT) {
            Collections.sort(teamList, new CompTeamPoll());
            for (Team team : teamList) {
                if (!playoffTeams.contains(team)) {
                    playoffTeams.add(team);
                    if (playoffTeams.size() >= EXPANDED_PLAYOFF_TEAM_COUNT) {
                        break;
                    }
                }
            }
        }

        Collections.sort(playoffTeams, new CompTeamPoll());

        StringBuilder sb = new StringBuilder();
        sb.append("The following teams are expected to make it to the Football Playoffs!\n\n");
        int i = 1;
        for (Team t : playoffTeams) {
            sb.append(i + ". " + t.strRankTeamRecord() + "   [" + t.getConference() + "]\n");
            i++;
        }
        postseason = sb.toString();

        for(int x = 0; x < playoffTeams.size(); x++) {
            qualifiedTeams.remove(playoffTeams.get(x));
        }

        if(hasScheduledBowls) bowlScheduleLogic(qualifiedTeams);

    }

    private ArrayList<Team> getExpandedPlayoffAutoBids(ArrayList<Team> qualifiedTeams) {
        ArrayList<Team> autoBids = new ArrayList<>();
        ArrayList<Team> conferenceLeaders = new ArrayList<>();

        if (currentWeek > regSeasonWeeks) {
            for (Team qualifiedTeam : qualifiedTeams) {
                if ("CC".equals(qualifiedTeam.getConfChampion())
                        && !qualifiedTeam.getConference().equals("Independent")
                        && !qualifiedTeam.getConference().equals("FCS Division")) {
                    conferenceLeaders.add(qualifiedTeam);
                }
            }
        } else {
            for (Conference conference : conferences) {
                if (!conference.confName.equals("Independent")
                        && !conference.confName.equals("FCS Division")
                        && conference.confTeams.size() > 0) {
                    Collections.sort(conference.confTeams, new CompTeamConfWins());
                    Team projectedChampion = conference.confTeams.get(0);
                    if (qualifiedTeams.contains(projectedChampion)) {
                        conferenceLeaders.add(projectedChampion);
                    }
                }
            }
        }

        Collections.sort(conferenceLeaders, new CompTeamPoll());
        for (int i = 0; i < conferenceLeaders.size() && i < 5; i++) {
            autoBids.add(conferenceLeaders.get(i));
        }

        return autoBids;
    }

    public void scheduleExpPlayoff() {
        hasScheduledBowls = true;
        playoffWeek = 1;
        getExpPlayoffTeams();

        cfpGames[0] = new Game(playoffTeams.get(4), playoffTeams.get(11), "First Round");
        cfpGames[1] = new Game(playoffTeams.get(7), playoffTeams.get(8), "First Round");
        cfpGames[2] = new Game(playoffTeams.get(5), playoffTeams.get(10), "First Round");
        cfpGames[3] = new Game(playoffTeams.get(6), playoffTeams.get(9), "First Round");

        for (int i = 0; i < 4; i++) {
            cfpGames[i].homeTeam.addGameToSchedule(cfpGames[i]);
            cfpGames[i].awayTeam.addGameToSchedule(cfpGames[i]);
            newsStories.get(currentWeek + 1).add("Upcoming First Round Playoff Game!>#"
                    + cfpGames[i].awayTeam.getRankTeamPollScore() + " " + cfpGames[i].awayTeam.getStrAbbrWL()
                    + " will battle with #"
                    + cfpGames[i].homeTeam.getRankTeamPollScore() + " " + cfpGames[i].homeTeam.getStrAbbrWL()
                    + " in the " + getYear() + " College Football Playoff first round!");
            weeklyScores.get(currentWeek + 2).add(cfpGames[i].gameName + ">"
                    + cfpGames[i].awayTeam.strRankTeamRecord() + "\n" + cfpGames[i].homeTeam.strRankTeamRecord());
        }

        //Heal teams
        for (int i = 0; i < teamList.size(); i++) {
            teamList.get(i).healInjury(1);
        }
    }
    
    public void expPlayoffSchdQT() {
        cfpGames[4] = new Game(playoffTeams.get(3), getWinner(cfpGames[0]), "Quarterfinal");
        cfpGames[5] = new Game(playoffTeams.get(0), getWinner(cfpGames[1]), "Quarterfinal");
        cfpGames[6] = new Game(playoffTeams.get(2), getWinner(cfpGames[2]), "Quarterfinal");
        cfpGames[7] = new Game(playoffTeams.get(1), getWinner(cfpGames[3]), "Quarterfinal");

        for (int i = 4; i < 8; i++) {
            cfpGames[i].homeTeam.addGameToSchedule(cfpGames[i]);
            cfpGames[i].awayTeam.addGameToSchedule(cfpGames[i]);
            newsStories.get(currentWeek + 1).add("Quarterfinal Match-up Announced!>"
                    + cfpGames[i].awayTeam.getStrAbbrWL() + " will take on "
                    + cfpGames[i].homeTeam.getStrAbbrWL() + " in the "
                    + getYear() + " College Football Playoff quarterfinals!");
            weeklyScores.get(currentWeek + 2).add(cfpGames[i].gameName + ">#"
                    + cfpGames[i].awayTeam.getRankTeamPollScore() + " " + cfpGames[i].awayTeam.getName() + "\n#"
                    + cfpGames[i].homeTeam.getRankTeamPollScore() + " " + cfpGames[i].homeTeam.getName());
        }
    }

    public void expPlayoffSchdSemi() {
        cfpGames[8] = new Game(getWinner(cfpGames[4]), getWinner(cfpGames[5]), "Semifinal");
        cfpGames[9] = new Game(getWinner(cfpGames[6]), getWinner(cfpGames[7]), "Semifinal");

        for (int i = 8; i < 10; i++) {
            cfpGames[i].homeTeam.addGameToSchedule(cfpGames[i]);
            cfpGames[i].awayTeam.addGameToSchedule(cfpGames[i]);
            newsStories.get(currentWeek + 1).add("Semifinal Match-up Announced!>"
                    + cfpGames[i].awayTeam.getStrAbbrWL() + " and " + cfpGames[i].homeTeam.getStrAbbrWL()
                    + " will play each other in the " + getYear() + " national semifinals!");
            weeklyScores.get(currentWeek + 2).add(cfpGames[i].gameName + ">"
                    + cfpGames[i].awayTeam.strRankTeamRecord() + "\n" + cfpGames[i].homeTeam.strRankTeamRecord());
        }
    }

    public void expPlayoffSchFinals() {
        Team titleTeamA = getWinner(cfpGames[8]);
        Team titleTeamB = getWinner(cfpGames[9]);

        ncg = new Game(titleTeamA, titleTeamB, "Championship");
        titleTeamA.addGameToSchedule(ncg);
        titleTeamB.addGameToSchedule(ncg);
        newsStories.get(currentWeek + 1).add("The Upcoming National Title Game!>" + titleTeamA.getStrAbbrWL() + " and " + titleTeamB.getStrAbbrWL() +
                " are the last two teams left in the " + getYear() + " College Football Playoffs. These teams will compete next weekend for the National Title!");
        weeklyScores.get(currentWeek + 2).add(ncg.gameName + ">" + ncg.awayTeam.strRankTeamRecord() + "\n" + ncg.homeTeam.strRankTeamRecord());
        newsHeadlines.add(titleTeamA.getStrAbbrWL() + " and " + titleTeamB.getStrAbbrWL() +
                " to meet in the " + getYear() + " Championship.");
    }

    public void playExpandedPlayoffFirstRound() {
        playBowlWeek1();

        playoffWeek = 1;
        for (int i = 0; i < 4; i++) {
            playPlayoff(cfpGames[i]);
        }
        expPlayoffSchdQT();
    }

    public void playExpandedPlayoffQuarterfinals() {
        playBowlWeek2();

        playoffWeek = 2;
        for (int i = 4; i < 8; i++) {
            playPlayoff(cfpGames[i]);
        }
        expPlayoffSchdSemi();
    }

    public void playExpandedPlayoffSemifinals() {
        playBowlWeek3();

        playoffWeek = 3;
        for (int i = 8; i < 10; i++) {
            playPlayoff(cfpGames[i]);
        }
        expPlayoffSchFinals();
    }

    private Team getWinner(Game game) {
        return game.homeScore > game.awayScore ? game.homeTeam : game.awayTeam;
    }

    private void playPlayoff(Game g) {
        g.playGame();

        if (playoffWeek == 1) {
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSweet16("FRW");
                g.awayTeam.setSweet16("FRL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + "."
                );
                newsHeadlines.add(g.homeTeam.getName() + " wins first-round playoff game!");
            } else {
                g.homeTeam.setSweet16("FRL");
                g.awayTeam.setSweet16("FRW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + "."
                );
                newsHeadlines.add(g.awayTeam.getName() + " advances in the playoffs!");
            }
        }

        if (playoffWeek == 2) {
            g.homeTeam.setSweet16("");
            g.awayTeam.setSweet16("");
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setQtFinalWL("QTW");
                g.awayTeam.setQtFinalWL("QTL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + "."
                );
                newsHeadlines.add(g.homeTeam.getName() + " advances to the semifinals!");
            } else {
                g.homeTeam.setQtFinalWL("QTL");
                g.awayTeam.setQtFinalWL("QTW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + "."
                );
                newsHeadlines.add(g.awayTeam.getName() + " advances to the semifinals!");
            }
        }

        if (playoffWeek == 3) {
            g.homeTeam.setQtFinalWL("");
            g.awayTeam.setQtFinalWL("");
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSemiFinalWL("SFW");
                g.awayTeam.setSemiFinalWL("SFL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + "."
                );
                newsHeadlines.add(g.homeTeam.getName() + " reaches the national title game!");
            } else {
                g.homeTeam.setSemiFinalWL("SFL");
                g.awayTeam.setSemiFinalWL("SFW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + "."
                );
                newsHeadlines.add(g.awayTeam.getName() + " reaches the national title game!");
            }
        }

    }

    /////////////////////////

    /* New Bowl Scheduling Logic
    Only teams qualify make it to bowl games. bowl games max out at total bowl length. if not enough teams qualify, bowl size shrinks.
     */


    private ArrayList<Team> getQualifiedTeams() {
        ArrayList<Team> bowlTeams = new ArrayList<>();
        setTeamRanks();
        Collections.sort(teamList, new CompTeamPoll());

        for (int i = 0; i < teamList.size(); ++i) {
            if (!teamList.get(i).isBowlBan() && teamList.get(i).getWins() >= 6)
                bowlTeams.add(teamList.get(i));
        }

        Collections.sort(bowlTeams, new CompTeamPoll());

        return bowlTeams;
    }

    private void scheduleNormalCFP() {
        ArrayList<Team> bowlTeams = getQualifiedTeams();

        //semifinals
        semiG14 = new Game(bowlTeams.get(0), bowlTeams.get(3), "Semis, 1v4");
        bowlTeams.get(0).addGameToSchedule(semiG14);
        bowlTeams.get(3).addGameToSchedule(semiG14);

        semiG23 = new Game(bowlTeams.get(1), bowlTeams.get(2), "Semis, 2v3");
        bowlTeams.get(1).addGameToSchedule(semiG23);
        bowlTeams.get(2).addGameToSchedule(semiG23);

        newsStories.get(currentWeek + 1).add("Playoff Teams Announced!>"  + "#" + bowlTeams.get(0).getRankTeamPollScore() + bowlTeams.get(0).getStrAbbrWL() + " will play #" + bowlTeams.get(3).getRankTeamPollScore() + bowlTeams.get(3).getStrAbbrWL() +
                " , while " + "#" + bowlTeams.get(1).getRankTeamPollScore() + bowlTeams.get(1).getStrAbbrWL() + " will play #" + bowlTeams.get(2).getRankTeamPollScore() + bowlTeams.get(2).getStrAbbrWL() + " in next week's College Football Playoff semi-final round. The winners will compete for this year's National Title!");

        weeklyScores.get(currentWeek + 4).add(semiG14.gameName + ">" + semiG14.awayTeam.strRankTeamRecord() + "\n" + semiG14.homeTeam.strRankTeamRecord());
        weeklyScores.get(currentWeek + 4).add(semiG23.gameName + ">" + semiG23.awayTeam.strRankTeamRecord() + "\n" + semiG23.homeTeam.strRankTeamRecord());

        for (int i = 0; i < 4; i++) {
            bowlTeams.get(i).healInjury(3);
        }
        bowlTeams.remove(semiG23.awayTeam);
        bowlTeams.remove(semiG23.homeTeam);
        bowlTeams.remove(semiG14.awayTeam);
        bowlTeams.remove(semiG14.homeTeam);
        bowlScheduleLogic(bowlTeams);
    }

    private void bowlScheduleLogic(ArrayList<Team> bowlTeams) {
        int bowlCount = (bowlTeams.size()) / 2;
        if (bowlCount > bowlNames.length) bowlCount = bowlNames.length;

        //schedule bowl games teams ranked #5-12
        int g = 0; //game #
        int r = 1; //rounds #
        int t = 0; //team #

        while (bowlCount / 4 >= r) {
            for (int i = t; i < t + 4; i++) {
                bowlGames[g] = new Game(bowlTeams.get(i), bowlTeams.get(i + 4), bowlNames[g]);
                bowlTeams.get(i).addGameToSchedule(bowlGames[g]);
                bowlTeams.get(i + 4).addGameToSchedule(bowlGames[g]);
                newsStories.get(currentWeek + 1).add(bowlGames[g].gameName + " Announced!>" + "#" + bowlTeams.get(i).getRankTeamPollScore() + " " + bowlTeams.get(i).getStrAbbrWL() + " will compete with " + "#" + bowlTeams.get(i + 4).getRankTeamPollScore() + " " + bowlTeams.get(i + 4).getStrAbbrWL() +
                        " in the " + getYear() + " " + bowlGames[g].gameName + "!");
                if(g < 6) weeklyScores.get(currentWeek + 4).add(bowlGames[g].gameName + ">" + bowlGames[g].awayTeam.strRankTeamRecord() + "\n" + bowlGames[g].homeTeam.strRankTeamRecord());
                else if(g < 16) weeklyScores.get(currentWeek + 3).add(bowlGames[g].gameName + ">" + bowlGames[g].awayTeam.strRankTeamRecord() + "\n" + bowlGames[g].homeTeam.strRankTeamRecord());
                else weeklyScores.get(currentWeek + 2).add(bowlGames[g].gameName + ">" + bowlGames[g].awayTeam.strRankTeamRecord() + "\n" + bowlGames[g].homeTeam.strRankTeamRecord());

                g++;
            }
            t = t + 8;
            r++;
        }

        hasScheduledBowls = true;

        //Heal Bowl Team Players
        int tmCount = bowlTeams.size();

        if(tmCount > 32) {
            for (int i = 0; i < 12; i++) {
                bowlTeams.get(i).healInjury(3);
            }
            for (int i = 12; i < 32; i++) {
                bowlTeams.get(i).healInjury(2);
            }
            for (int i = 32; i < bowlTeams.size(); i++) {
                bowlTeams.get(i).healInjury(1);
            }
        } else if(tmCount > 12) {
            for (int i = 0; i < 12; i++) {
                bowlTeams.get(i).healInjury(3);
            }
            for (int i = 12; i < tmCount; i++) {
                bowlTeams.get(i).healInjury(2);
            }
        } else {
            for (int i = 0; i < tmCount; i++) {
                bowlTeams.get(i).healInjury(3);
            }
        }

    }

    /**
     * Actually plays each bowl game.
     */


    private void playBowlWeek1() {
        for (int g = 16; g < bowlGames.length; g++) {
            if(bowlGames[g] != null) playBowl(bowlGames[g]);
        }
    }

    private void playBowlWeek2() {
        for (int g = 6; g < bowlGames.length; g++) {
            if(bowlGames[g] != null) playBowl(bowlGames[g]);
        }
    }

    private void playBowlWeek3() {
        for (int g = 0; g < bowlGames.length; g++) {
            if(bowlGames[g] != null) playBowl(bowlGames[g]);
        }

        if(!expPlayoffs) {
            semiG14.playGame();
            semiG23.playGame();
            Team semi14winner;
            Team semi23winner;
            if (semiG14.homeScore > semiG14.awayScore) {
                semiG14.homeTeam.setSemiFinalWL("SFW");
                semiG14.awayTeam.setSemiFinalWL("SFL");
                semiG14.awayTeam.incrementTotalBowlLosses();
                semiG14.homeTeam.incrementTotalBowls();
                semiG14.homeTeam.getHeadCoach().recordBowlWins(1);
                semiG14.awayTeam.getHeadCoach().recordBowlLosses(1);
                semi14winner = semiG14.homeTeam;
                newsStories.get(currentWeek + 1).add(
                        semiG14.homeTeam.getName() + " wins the " + semiG14.gameName + "!>" +
                                semiG14.homeTeam.strRep() + " defeats " + semiG14.awayTeam.strRep() +
                                " in the semifinals, winning " + semiG14.homeScore + " to " + semiG14.awayScore + ". " +
                                semiG14.homeTeam.getName() + " advances to the National Championship!");
                newsHeadlines.add(semiG14.homeTeam.strRep() + " defeats " + semiG14.awayTeam.strRep() + " in the semifinals, winning " + semiG14.homeScore + " to " + semiG14.awayScore + ". " + semiG14.homeTeam.getName() + " advances to the National Championship!");
            } else {
                semiG14.homeTeam.setSemiFinalWL("SFL");
                semiG14.awayTeam.setSemiFinalWL("SFW");
                semiG14.homeTeam.incrementTotalBowlLosses();
                semiG14.awayTeam.incrementTotalBowls();
                semiG14.awayTeam.getHeadCoach().recordBowlWins(1);
                semiG14.homeTeam.getHeadCoach().recordBowlLosses(1);
                semi14winner = semiG14.awayTeam;
                newsStories.get(currentWeek + 1).add(
                        semiG14.awayTeam.getName() + " wins the " + semiG14.gameName + "!>" +
                                semiG14.awayTeam.strRep() + " defeats " + semiG14.homeTeam.strRep() +
                                " in the semifinals, winning " + semiG14.awayScore + " to " + semiG14.homeScore + ". " +
                                semiG14.awayTeam.getName() + " advances to the National Championship!");
                newsHeadlines.add(semiG14.awayTeam.strRep() + " defeats " + semiG14.homeTeam.strRep() + " in the semifinals, winning " + semiG14.awayScore + " to " + semiG14.homeScore + ". " + semiG14.awayTeam.getName() + " advances to the National Championship!");
            }

            if (semiG23.homeScore > semiG23.awayScore) {
                semiG23.homeTeam.setSemiFinalWL("SFW");
                semiG23.awayTeam.setSemiFinalWL("SFL");
                semiG23.homeTeam.incrementTotalBowls();
                semiG23.awayTeam.incrementTotalBowlLosses();
                semiG23.homeTeam.getHeadCoach().recordBowlWins(1);
                semiG23.awayTeam.getHeadCoach().recordBowlLosses(1);
                semi23winner = semiG23.homeTeam;
                newsStories.get(currentWeek + 1).add(
                        semiG23.homeTeam.getName() + " wins the " + semiG23.gameName + "!>" +
                                semiG23.homeTeam.strRep() + " defeats " + semiG23.awayTeam.strRep() +
                        " in the semifinals, winning " + semiG23.homeScore + " to " + semiG23.awayScore + ". " +
                        semiG23.homeTeam.getName() + " advances to the National Championship!"

                );
                newsHeadlines.add(semiG23.homeTeam.strRep() + " defeats " + semiG23.awayTeam.strRep() + " in the semifinals, winning " + semiG23.homeScore + " to " + semiG23.awayScore + ". " + semiG23.homeTeam.getName() + " advances to the National Championship!");
            } else {
                semiG23.homeTeam.setSemiFinalWL("SFL");
                semiG23.awayTeam.setSemiFinalWL("SFW");
                semiG23.awayTeam.incrementTotalBowls();
                semiG23.homeTeam.incrementTotalBowlLosses();
                semiG23.awayTeam.getHeadCoach().recordBowlWins(1);
                semiG23.homeTeam.getHeadCoach().recordBowlLosses(1);
                semi23winner = semiG23.awayTeam;
                newsStories.get(currentWeek + 1).add(
                        semiG23.awayTeam.getName() + " wins the " + semiG23.gameName + "!>" +
                                semiG23.awayTeam.strRep() + " defeats " + semiG23.homeTeam.strRep() +
                                " in the semifinals, winning " + semiG23.awayScore + " to " + semiG23.homeScore + ". " +
                                semiG23.awayTeam.getName() + " advances to the National Championship!");
                newsHeadlines.add(semiG23.awayTeam.strRep() + " defeats " + semiG23.homeTeam.strRep() + " in the semifinals, winning " + semiG23.awayScore + " to " + semiG23.homeScore + ". " + semiG23.awayTeam.getName() + " advances to the National Championship!");

            }

            //schedule NCG
            ncg = new Game(semi14winner, semi23winner, "NCG");
            semi14winner.addGameToSchedule(ncg);
            semi23winner.addGameToSchedule(ncg);
            newsStories.get(currentWeek + 1).add("Upcoming National Title Game!>" + semi14winner.getStrAbbrWL() + " will compete with " + semi23winner.getStrAbbrWL() +
                    " for the " + getYear() + " College Football National Title!");
            newsHeadlines.add(semi14winner.getStrAbbrWL() + " will compete with " + semi23winner.getStrAbbrWL() + " for the " + getYear() + " College Football National Title!");
            weeklyScores.get(currentWeek + 2).add(ncg.gameName + ">" + ncg.awayTeam.strRankTeamRecord() + "\n" + ncg.homeTeam.strRankTeamRecord());
        }
    }

    /**
     * Plays a particular bowl game
     *
     * @param g bowl game to be played
     */
    private void playBowl(Game g) {
        if(!g.hasPlayed) {
            g.playGame();
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSemiFinalWL("BW");
                g.awayTeam.setSemiFinalWL("BL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + "."
                );
                newsHeadlines.add(g.homeTeam.getName() + " wins the " + g.gameName + "!");
            } else {
                g.homeTeam.setSemiFinalWL("BL");
                g.awayTeam.setSemiFinalWL("BW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                newsStories.get(currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>" +
                                g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep() +
                                " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + "."
                );
                newsHeadlines.add(g.awayTeam.getName() + " wins the " + g.gameName + "!");
            }
        }
    }

    /**
     * Get string of what happened in a particular bowl
     *
     * @param g Bowl game to be examined
     * @return string of its summary, ALA W 24 - 40 @ GEO, etc
     */
    private String getGameSummaryBowl(Game g) {
        StringBuilder sb = new StringBuilder();
        Team winner, loser;
        if (!g.hasPlayed) {
            return g.homeTeam.strRep() + " vs " + g.awayTeam.strRep();
        } else {
            if (g.homeScore > g.awayScore) {
                winner = g.homeTeam;
                loser = g.awayTeam;
                sb.append(winner.strRep() + " W ");
                sb.append(g.homeScore + "-" + g.awayScore + " ");
                sb.append("vs " + loser.strRep());
                return sb.toString();
            } else {
                winner = g.awayTeam;
                loser = g.homeTeam;
                sb.append(winner.strRep() + " W ");
                sb.append(g.awayScore + "-" + g.homeScore + " ");
                sb.append("@ " + loser.strRep());
                return sb.toString();
            }
        }
    }


    /**
     * Get summary of what happened in the NCG
     *
     * @return string of summary
     */
    private String ncgSummaryStr() {
        // Give summary of what happened in the NCG
        if (ncg.homeScore > ncg.awayScore) {
            return ncg.homeTeam.getName() + " (" + ncg.homeTeam.getWins() + "-" + ncg.homeTeam.getLosses() + ") won the National Championship, " +
                    "winning against " + ncg.awayTeam.getName() + " (" + ncg.awayTeam.getWins() + "-" + ncg.awayTeam.getLosses() + ") in the NCG " +
                    ncg.homeScore + "-" + ncg.awayScore + ".";
        } else {
            return ncg.awayTeam.getName() + " (" + ncg.awayTeam.getWins() + "-" + ncg.awayTeam.getLosses() + ") won the National Championship, " +
                    "winning against " + ncg.homeTeam.getName() + " (" + ncg.homeTeam.getWins() + "-" + ncg.homeTeam.getLosses() + ") in the NCG " +
                    ncg.awayScore + "-" + ncg.homeScore + ".";
        }
    }

    /**
     * Get summary of season.
     *
     * @return ncgSummary, userTeam's summary
     */
    public void enterOffseason() {
        for (Team t : teamList) {
            t.enterOffSeason();
        }
    }

    public String seasonSummaryStr() {
        setTeamRanks();
        StringBuilder sb = new StringBuilder();
        sb.append(ncgSummaryStr());
        sb.append("\n\n" + userTeam.seasonSummaryStr());
        if (getYear() > seasonStart) {
            sb.append("\n\nLEAGUE RECORDS BROKEN:\n" + leagueRecords.brokenRecordsStr(getYear(), userTeam.getAbbr()));
            sb.append("\n\nTEAM RECORDS BROKEN:\n" + userTeam.getTeamRecords().brokenRecordsStr(getYear(), userTeam.getAbbr()));
        }
        return sb.toString();
    }


    public void advanceStaff() {
        coachList.clear();
        coachStarList.clear();
        Collections.sort(teamList, new CompTeamPrestige());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).advanceHC(leagueRecords, teamList.get(t).getTeamRecords());
            teamList.get(t).advanceCoordinator();
            teamList.get(t).checkFacilitiesUpgradeBonus();
        }
    }

    public void updateHCHistory() {
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).updateCoachHistory(teamList.get(t).getHeadCoach());
            teamList.get(t).updateCoachHistory(teamList.get(t).getOC());
            teamList.get(t).updateCoachHistory(teamList.get(t).getDC());
        }

    }

    //Get HeadCoach Job Offers List if fired or quit
    public ArrayList<Team> getCoachListFired(int rating, String oldTeam) {
        ArrayList<Team> teamVacancies = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getMinCoachHireReq() < rating && teamList.get(i).getHeadCoach() == null && !teamList.get(i).getName().equals(oldTeam)) {
                teamVacancies.add(teamList.get(i));
            }
        }

        if (teamVacancies.isEmpty()) {
            teamVacancies = getCoachVacancies();
        }
        return teamVacancies;
    }

    //Get HeadCoach Job Offers List for Team Transfer
    public ArrayList<Team> getCoachPromotionList(int rating, double offers, String oldTeam) {
        ArrayList<Team> teamVacancies = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getMinCoachHireReq() < rating && teamList.get(i).getHeadCoach() == null && !teamList.get(i).getName().equals(oldTeam) && offers > 0.50) {
                teamVacancies.add(teamList.get(i));
            }
        }

        return teamVacancies;
    }

    //If there are no vacancies for user head coach...
    public ArrayList<Team> getCoachVacancies() {
        ArrayList<Team> teamVacancies = new ArrayList<>();

        for (int c = 0; c < conferences.size(); c++) {
            if(conferences.get(c).confTeams.size() > 3) {
                teamVacancies.add(conferences.get(c).confTeams.get(conferences.get(c).confTeams.size() - 1));
                teamVacancies.add(conferences.get(c).confTeams.get(conferences.get(c).confTeams.size() - 2));
            }
        }
        return teamVacancies;
    }

    //Transferring Jobs
    public void newJobtransfer(String coachTeam) {
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getName().equals(coachTeam)) {
                teamList.get(i).setUserControlled(true);
                userTeam = teamList.get(i);
            }
        }
    }

    //COACHING CAROUSEL HIRING METHOD
    //THIS METHOD TAKES THE COACH LIST CREATED AFTER FIRING AND PUTS IT INTO A POPULATION FOR TEAMS WITH NO COACHES TO HIRE FROM
    public void coachCarousel() {
        int[] ovr = {1,1,1,1};
        Collections.sort(teamList, new CompTeamPrestige());
        //Rising Star Coaches
        for (int i = 0; i < coachStarList.size(); ++i) {
            Staff coach = coachStarList.get(i);
            if (coach.team == null) continue;
            
            final String tmName = coach.team.getName();
            final String pos = coach.position;
            int tmPres = coach.team.getTeamPrestige();
            int cPres = coach.team.getConfPrestige();

            for (int t = 0; t < teamList.size(); ++t) {
                if (teamList.get(t).getHeadCoach() == null && coachStarList.get(i).getStaffOverall(ovr) >= teamList.get(t).getMinCoachHireReq() && !teamList.get(t).getName().equals(tmName) && Math.random() > 0.66) {
                    if (!coachStarList.get(i).position.equals("HC") || teamList.get(t).getTeamPrestige() > tmPres && teamList.get(t).getConfPrestige() > cPres || teamList.get(t).getTeamPrestige() > tmPres + 5 || teamList.get(t).getConfPrestige() + 10 > cPres) {
                        final Staff hiredHC = coachStarList.get(i);
                        teamList.get(t).setHeadCoach(new HeadCoach(hiredHC, teamList.get(t)));
                        teamList.get(t).getHeadCoach().contractLength = 6;
                        teamList.get(t).getHeadCoach().contractYear = 0;
                        teamList.get(t).getHeadCoach().baselinePrestige = teamList.get(t).getTeamPrestige();
                        teamList.get(t).getHeadCoach().team = teamList.get(t);
                        coachStarList.remove(hiredHC);
                        newsStories.get(currentWeek + 1).add("Rising Star Head Coach Hired: " + teamList.get(t).getName() + ">Coach " + teamList.get(t).getHeadCoach().name + " has announced his departure from " +
                                tmName + " after being selected by " + teamList.get(t).strRankTeamRecord() + " as their new head coach. His previous track record has had him on the top list of many schools.");
                        newsHeadlines.add(teamList.get(t).getHeadCoach().name + " has announced his departure from " + tmName + " after being selected by " + teamList.get(t).strRankTeamRecord());
                        for (int j = 0; j < teamList.size(); ++j) {
                            if (teamList.get(j).getName().equals(tmName)) {
                                if(pos.equals("HC")) {
                                    teamList.get(j).setHeadCoach(null);
                                    if (Math.random() > 0.20) {
                                        teamList.get(j).promoteCoach();
                                        teamList.get(j).getHeadCoach().history.add("");
                                        newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                                " hopes to continue their recent success, despite the recent loss of coach " + teamList.get(t).getHeadCoach().name + ". The team has promoted his Coordinator " + teamList.get(j).getHeadCoach().name + " to the head coaching job at the school.");
                                        newsHeadlines.add(teamList.get(j).getName() + " has promoted coordinator " + teamList.get(t).getHeadCoach().name + " to Head Coach position.");
                                    }
                                } else if (pos.equals("OC")) {
                                    teamList.get(j).setOC(null);
                                    if(!teamList.get(j).isUserControlled()) {
                                        teamList.get(j).setOC(new OC(getRandName(), teamList.get(j).getRankTeamPrestige() / (teamList.size() / 8)));
                                        newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                                " hopes to continue their recent success, despite the recent loss of OC " + teamList.get(t).getHeadCoach().name + ". The team has promoted his assistant coach " + teamList.get(j).getOC().name + " to the Off Coordinator job at the school.");
                                    }
                                } else if (pos.equals("DC")) {
                                    teamList.get(j).setDC(null);
                                    if (!teamList.get(j).isUserControlled()) {
                                        teamList.get(j).setDC(new DC(getRandName(), teamList.get(j).getRankTeamPrestige() / (teamList.size() / 8)));
                                        newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                                " hopes to continue their recent success, despite the recent loss of DC " + teamList.get(t).getHeadCoach().name + ". The team has promoted his assistant coach " + teamList.get(j).getDC().name + " to the Def Coordinator job at the school.");
                                    }
                                }
                            }
                        }
                        teamList.get(t).newCoachDecisions();
                        break;
                    }
                }
            }
        }

        //Coaches who were fired previous years
        Collections.sort(coachFreeAgents, new CompCoachOvr());
        for (int i = 0; i < coachFreeAgents.size(); ++i) {
            final Staff c = coachFreeAgents.get(i);
            for (int t = 0; t < teamList.size(); ++t) {
                if (teamList.get(t).getHeadCoach() == null && coachFreeAgents.get(i).getStaffOverall(ovr) >= teamList.get(t).getMinCoachHireReq() && Math.random() < 0.60 && !coachFreeAgents.get(i).retired) {
                    teamList.get(t).setHeadCoach(new HeadCoach(c, teamList.get(t)));
                    teamList.get(t).getHeadCoach().contractLength = 6;
                    teamList.get(t).getHeadCoach().contractYear = 0;
                    teamList.get(t).getHeadCoach().baselinePrestige = teamList.get(t).getTeamPrestige();
                    teamList.get(t).getHeadCoach().team = teamList.get(t);
                    coachFreeAgents.remove(c);
                    newsStories.get(currentWeek + 1).add("Return to the Sidelines: " + teamList.get(t).getName() + ">After an extensive search for a new head coach, " + teamList.get(t).strRankTeamRecord() + " has hired " + teamList.get(t).getHeadCoach().name +
                            " to lead the team. Head Coach " + teamList.get(t).getHeadCoach().name + " has been out of football, but is returning this season!");
                    newsHeadlines.add(teamList.get(t).strRankTeamRecord() + " has hired unemployed " + teamList.get(t).getHeadCoach().name + " to lead the team.");

                    teamList.get(t).newCoachDecisions();
                    break;
                }
            }
        }

        //Assistants Promoted
        for (int t = 0; t < teamList.size(); ++t) {
            if (teamList.get(t).getHeadCoach() == null && Math.random() > 0.60) {
                teamList.get(t).promoteCoach();
                teamList.get(t).getHeadCoach().history.add("");
                newsStories.get(currentWeek + 1).add("Coaching Promotion: " + teamList.get(t).getName() + ">Following the departure of their previous head coach, " + teamList.get(t).strRankTeamRecord() + " has promoted assistant " + teamList.get(t).getHeadCoach().name +
                        " to lead the team.");
                newsHeadlines.add(teamList.get(t).getName() + " has promoted coordinator " + teamList.get(t).getHeadCoach().name + " to Head Coach position.");
            }
        }

        //Coaches who were fired
        Collections.sort(coachList, new CompCoachOvr());
        for (int i = 0; i < coachList.size(); ++i) {
            final Staff c = coachList.get(i);
            for (int t = 0; t < teamList.size(); ++t) {
                if (teamList.get(t).getHeadCoach() == null && coachList.get(i).getStaffOverall(ovr) >= teamList.get(t).getMinCoachHireReq() && !teamList.get(t).getName().equals(coachList.get(i).team.getName()) && Math.random() > 0.60) {

                    newsStories.get(currentWeek + 1).add("Coaching Switch: " + teamList.get(t).getName() + ">After an extensive search for a new head coach, " + teamList.get(t).strRankTeamRecord() + " has hired " + coachList.get(i).name +
                            " to lead the team. Head Coach " + coachList.get(i).name + " previously coached at " + coachList.get(i).team.getName() + ", before being let go this past season.");
                    newsHeadlines.add(teamList.get(t).strRankTeamRecord() + " has hired recently fired " + coachList.get(i).name + ".");
                    teamList.get(t).setHeadCoach(new HeadCoach(c, teamList.get(t)));
                    teamList.get(t).getHeadCoach().contractLength = 6;
                    teamList.get(t).getHeadCoach().contractYear = 0;
                    teamList.get(t).getHeadCoach().baselinePrestige = teamList.get(t).getTeamPrestige();
                    teamList.get(t).getHeadCoach().team = teamList.get(t);
                    coachList.remove(c);

                    teamList.get(t).newCoachDecisions();
                    break;
                }
            }
        }

        //Assistants Promoted
        for (int t = 0; t < teamList.size(); ++t) {
            if (teamList.get(t).getHeadCoach() == null) {
                teamList.get(t).promoteCoach();
                teamList.get(t).getHeadCoach().history.add("");
                newsStories.get(currentWeek + 1).add("Coaching Promotion: " + teamList.get(t).getName() + ">Following the departure of their previous head coach, " + teamList.get(t).strRankTeamRecord() + " has promoted assistant " + teamList.get(t).getHeadCoach().name +
                        " to lead the team.");
                newsHeadlines.add(teamList.get(t).getName() + " has promoted coordinator " + teamList.get(t).getHeadCoach().name + " to Head Coach position.");
            }
        }


    }

    //Hiring method for teams that get poached
    public void coachHiringSingleTeam(Team school) {
        int[] ovr = {1,1,1,1};
        //Rising Star Coaches
        for (int i = 0; i < coachStarList.size(); ++i) {
            final Staff c = coachStarList.get(i);

            String tmName = "N/A";
            if(coachStarList.get(i).team.getName() != null) tmName = coachStarList.get(i).team.getName();
            final String pos = coachStarList.get(i).position;
            int tmPres = coachStarList.get(i).team.getTeamPrestige();
            int cPres = coachStarList.get(i).team.getConfPrestige();

            if (coachStarList.get(i).getStaffOverall(ovr) >= school.getMinCoachHireReq() && !school.getName().equals(tmName) && Math.random() > 0.60) {
                if (school.getTeamPrestige() > tmPres && school.getConfPrestige() > cPres || school.getTeamPrestige() > tmPres + 5 || school.getConfPrestige() + 10 > cPres) {
                    school.setHeadCoach(new HeadCoach(c, school));
                    school.getHeadCoach().contractLength = 6;
                    school.getHeadCoach().contractYear = 0;
                    school.getHeadCoach().baselinePrestige = school.getTeamPrestige();
                    school.getHeadCoach().team = school;
                    coachStarList.remove(c);
                    newsStories.get(currentWeek + 1).add("Rising Star Head Coach Hired: " + school.getName() + ">Rising star head coach " + school.getHeadCoach().name + " has announced his departure from " +
                            tmName + " after being selected by " + school.getName() + " as their new head coach. His previous track record has had him on the top list of many schools.");
                    newsHeadlines.add(school.getHeadCoach().name + " has announced his departure from " + tmName + " after being selected by " + school.getName());

                    for (int j = 0; j < teamList.size(); ++j) {
                        if (teamList.get(j).getName().equals(tmName)) {
                            if (pos.equals("HC")) {
                                teamList.get(j).setHeadCoach(null);
                                if (Math.random() > 0.25) {
                                    teamList.get(j).promoteCoach();
                                    teamList.get(j).getHeadCoach().history.add("");
                                    newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                            " hopes to continue their recent success, despite the recent loss of coach " + school.getHeadCoach().name + ". The team has promoted his Coordinator " + teamList.get(j).getHeadCoach().name + " to the head coaching job at the school.");
                                    newsHeadlines.add(teamList.get(j).getName() + " has promoted coordinator " + school.getHeadCoach().name + " to Head Coach position.");
                                } else {
                                    coachHiringSingleTeam(teamList.get(j));
                                }
                            } else if (pos.equals("OC")) {
                                teamList.get(j).setOC(null);
                                if(!teamList.get(j).isUserControlled()) {
                                    teamList.get(j).setOC(new OC(getRandName(), teamList.get(j).getRankTeamPrestige() / (teamList.size() / 8), 0, teamList.get(j)));
                                    newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                            " hopes to continue their recent success, despite the recent loss of OC " + school.getHeadCoach().name + ". The team has promoted his assistant coach " + teamList.get(j).getOC().name + " to the Off Coordinator job at the school.");
                                }
                            } else if (pos.equals("DC")) {
                                teamList.get(j).setDC(null);
                                if (!teamList.get(j).isUserControlled()) {
                                    teamList.get(j).setDC(new DC(getRandName(), teamList.get(j).getRankTeamPrestige() / (teamList.size() / 8), 0, teamList.get(j)));
                                    newsStories.get(currentWeek + 1).add("Replacement Promoted: " + teamList.get(j).getName() + ">" + teamList.get(j).strRankTeamRecord() +
                                            " hopes to continue their recent success, despite the recent loss of DC " + school.getHeadCoach().name + ". The team has promoted his assistant coach " + teamList.get(j).getDC().name + " to the Def Coordinator job at the school.");
                                }
                            }
                        }
                    }

                    school.newCoachDecisions();
                    break;
                }
            }
        }

        if (school.getHeadCoach() == null) {
            //Coaches who were fired previous years
            Collections.sort(coachFreeAgents, new CompCoachOvr());
            for (int i = 0; i < coachFreeAgents.size(); ++i) {
                final Staff c = coachFreeAgents.get(i);
                if (school.getHeadCoach() == null && coachFreeAgents.get(i).getStaffOverall(ovr) >= school.getMinCoachHireReq() && Math.random() < 0.65 && !coachFreeAgents.get(i).retired) {
                    school.setHeadCoach(new HeadCoach(c, school));
                    school.getHeadCoach().contractLength = 6;
                    school.getHeadCoach().contractYear = 0;
                    school.getHeadCoach().baselinePrestige = school.getTeamPrestige();
                    school.getHeadCoach().team = school;
                    coachFreeAgents.remove(c);
                    newsStories.get(currentWeek + 1).add("Back to Coaching: " + school.getName() + ">After an extensive search for a new head coach, " + school.getName() + " has hired " + school.getHeadCoach().name +
                            " to lead the team. Head Coach " + school.getHeadCoach().name + " has been out of football, but is returning this upcoming season!");
                    newsHeadlines.add(school.getName() + " has hired unemployed " + school.getHeadCoach().name + " to lead the team.");
                    school.newCoachDecisions();
                    break;
                }
            }
        }

        if (school.getHeadCoach() == null) {
            //Coaches who were fired
            Collections.sort(coachList, new CompCoachOvr());
            for (int i = 0; i < coachList.size(); ++i) {
                final Staff c = coachList.get(i);
                if (school.getHeadCoach() == null && coachList.get(i).getStaffOverall(ovr) + 5 >= school.getMinCoachHireReq() && !school.getName().equals(coachList.get(i).team.getName()) && Math.random() > 0.45) {
                    school.setHeadCoach(new HeadCoach(c, school));
                    school.getHeadCoach().contractLength = 6;
                    school.getHeadCoach().contractYear = 0;
                    school.getHeadCoach().baselinePrestige = school.getTeamPrestige();
                    school.getHeadCoach().team = school;
                    coachList.remove(c);
                    newsStories.get(currentWeek + 1).add("Coaching Change: " + school.getName() + ">After an extensive search for a new head coach, " + school.getName() + " has hired " + school.getHeadCoach().name +
                            " to lead the team. Head Coach " + school.getHeadCoach().name + " previously coached at " + coachList.get(i).team.getName() + ", before being let go this past season.");
                    newsHeadlines.add(school.getName() + " has hired recently unemployed " + school.getHeadCoach().name + ".");

                    school.newCoachDecisions();
                    break;
                }
            }
        }

        if (school.getHeadCoach() == null) {
            school.promoteCoach();
            school.getHeadCoach().history.add("");
            newsStories.get(currentWeek + 1).add("Coaching Promotion: " + school.getName() + ">Following the departure of their previous head coach, " + school.getName() + " has promoted assistant " + school.getHeadCoach().name +
                    " to lead the team.");
        }
    }


    //Coaching Hot Seat News
    private void coachingHotSeat() {
        if (currentWeek == 0) {
            newsHeadlines.add("Coaching Hot Seat: The Names with Something to Prove");
            for (int i = 0; i < teamList.size(); ++i) {
                if (teamList.get(i).getHeadCoach().baselinePrestige < teamList.get(i).getTeamPrestige() && teamList.get(i).getHeadCoach().contractYear == teamList.get(i).getHeadCoach().contractLength) {
                    newsStories.get(0).add("Coaching Hot Seat: " + teamList.get(i).getName() + ">Head Head Coach " + teamList.get(i).getHeadCoach().name + " has struggled over the course of his current contract with " +
                            teamList.get(i).strRankTeamRecord() + " and has failed to raise the team prestige. Because this is his final contract year, the team will be evaluating whether to continue with the coach at the end of " +
                            "this season. He'll remain on the hot seat throughout this year.");
                } else if (teamList.get(i).getTeamPrestige() > (teamList.get(i).getHeadCoach().baselinePrestige + 10) && teamList.get(i).getTeamPrestige() < teamList.get((int) (teamList.size() * 0.35)).getTeamPrestige()) {
                    newsStories.get(0).add("Coaching Rising Star: " + teamList.get(i).getHeadCoach().name + ">" + teamList.get(i).strRankTeamRecord() + " head coach " + teamList.get(i).getHeadCoach().name +
                            " has been building a strong program and if he continues this path, he'll be on the top of the wishlist at a major program in the future.");
                }
            }
        } else if (currentWeek == 7) {
            newsHeadlines.add("Coaching Hot Seat: Who's On the Brink of Being Fired?");
            for (int i = 0; i < teamList.size(); ++i) {
                if (teamList.get(i).getHeadCoach().baselinePrestige < teamList.get(i).getTeamPrestige() && teamList.get(i).getHeadCoach().contractYear == teamList.get(i).getHeadCoach().contractLength && teamList.get(i).getRankTeamPollScore() > (100 - teamList.get(i).getHeadCoach().baselinePrestige)) {
                    newsStories.get(currentWeek + 1).add("Coaching Hot Seat: " + teamList.get(i).getName() + ">Head Head Coach " + teamList.get(i).getHeadCoach().name + " future is in jeopardy at  " +
                            teamList.get(i).strRankTeamRecord() + ". The coach has failed to get out of the hot seat this season with disappointing losses and failing to live up to the school's standards.");
                }
            }
        }
    }

    public ArrayList<Staff> getOCList(HeadCoach hc) {
        ArrayList<Staff> list = new ArrayList<>();
        int num = 0;

        for(Staff c : coachFreeAgents) {
            if(c.ratOff >= c.ratDef && hc.offStrat == c.offStrat && !c.retired) {
                list.add(c);
                num++;
                if (num > 10) break;
            }
        }

        if (list.size() < 5) {
            for(int i = list.size(); i < 6; i++) {
                list.add(new OC(getRandName(), 6));
            }
        }

        Collections.sort(list, new CompCoachOff());
        return list;
    }

    public ArrayList<Staff> getDCList(HeadCoach hc) {
        ArrayList<Staff> list = new ArrayList<>();
        int num = 0;

        for(Staff c : coachFreeAgents) {
            if(c.ratDef > c.ratOff && hc.defStrat == c.defStrat && !c.retired) {
                list.add(c);
                num++;
                if (num > 10) break;
            }
        }

        if (list.size() < 5) {
            for(int i = list.size(); i < 6; i++) {
                list.add(new DC(getRandName(), 6));
            }
        }
        Collections.sort(list, new CompCoachDef());
        return list;
    }

    public void coordinatorCarousel() {
        for(Team t : teamList) {
            if(t.getHeadCoach() == null) coachHiringSingleTeam(t);
        }
        OCCarousel();
    }

    public void OCCarousel() {
        Collections.sort(teamList, new CompTeamPrestige());
        for (Team t : teamList) {
            if (t.getOC() == null) {
                for (Staff c : coachFreeAgents) {
                    if(c.offStrat == t.getHeadCoach().offStrat && c.ratOff >= c.ratDef && !c.retired) {
                        final Staff hire = c;
                        t.setOC(new OC(hire, t));
                        coachFreeAgents.remove(hire);
                        break;
                    }
                }
                if(t.getOC() == null) t.setOC(new OC(getRandName(), 6));
                newsStories.get(currentWeek).add("Off Coord Change: " + t.getName() + ">After an extensive search for a new coordinator, " + t.getName() + " has hired " + t.getOC().name +
                        " to lead the offense.");
                newsHeadlines.add(t.getName() + " adds new Off Coord " + t.getOC().name);
                t.getOC().contractLength = 3;
                t.getOC().contractYear = 0;
            }
        }
        DCCarousel();
    }

    public void DCCarousel() {
        Collections.sort(teamList, new CompTeamPrestige());
        for(Team t : teamList) {
            if (t.getDC() == null) {
                for (Staff c : coachFreeAgents) {
                    if(c.defStrat == t.getHeadCoach().defStrat && c.ratDef >= c.ratOff && !c.retired) {
                        final Staff hire = c;
                        t.setDC(new DC(hire, t));
                        coachFreeAgents.remove(hire);
                        break;
                    }
                }
                if(t.getDC() == null) t.setDC(new DC(getRandName(), 6));
                newsStories.get(currentWeek).add("Def Coord Change: " + t.getName() + ">After an extensive search for a new coordinator, " + t.getName() + " has hired " + t.getDC().name +
                        " to lead the defense.");
                newsHeadlines.add(t.getName() + " adds new Def Coord " + t.getDC().name);
                t.getDC().contractLength = 3;
                t.getDC().contractYear = 0;
            }
        }
    }

    //Transfer players from the available transfer lists
    public void transferPlayers(GameUiBridge mainAct) {
        GameUiBridge bridge = bridgeOrNoOp(mainAct);
        Collections.sort(teamList, new CompTeamPoll());
        int rand;
        Random random = new Random();
        int max = teamList.size() - 1;
        int min = 0;

        //Transfer List Summary Builder
        transfersList = new ArrayList<>();

        //User Transfer Pop Up Dialog Builder
        userTransfers = "";
        StringBuilder tOut = new StringBuilder();
        tOut.append("Transfers Out:\n\n");

        for (int loc = 1; loc < 4; ++loc) {

            for (int i = 0; i < transferQBs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamQBs().size() < 1 || teamList.get(t).getTeamQBs().get(0).ratOvr < transferQBs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferQBs.get(i).getRegion()) < loc) {
                            int qbTransfers = 0;
                            for (int x = 0; x < teamList.get(t).getTeamQBs().size(); ++x) {
                                if (teamList.get(t).getTeamQBs().get(x).isTransfer) qbTransfers++;
                            }
                            if (qbTransfers == 0 && !teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferQBs.get(i).getYrStr() + " QB " + transferQBs.get(i).name + "(" + transferQBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferQBs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferQBs.get(i).getYrStr() + " QB " + transferQBs.get(i).name + " (" + transferQBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");
                                
                                if (transferQBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferQBs.get(i).position + " " + transferQBs.get(i).name + ", " + transferQBs.get(i).getYrStr() + "  Ovr: " + transferQBs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferQBs.get(i).ratOvr + " " + transferQBs.get(i).position + " " + transferQBs.get(i).name + " [" + transferQBs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferQBs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerQB(transferQBs.get(i));
                                transferQBs.remove(i);
                                break;
                            } else if (userTeam.getQbTransferNum() == 0) {
                                userTeam.incrementQbTransferNum();
                                bridge.transferPlayer(transferQBs.get(i));
                                transferQBs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferRBs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamRBs().size() < 2 || teamList.get(t).getTeamRBs().get(0).ratOvr < transferRBs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferRBs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferRBs.get(i).getYrStr() + " RB " + transferRBs.get(i).name + "(" + transferRBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferRBs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferRBs.get(i).getYrStr() + " RB " + transferRBs.get(i).name + " (" + transferRBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferRBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferRBs.get(i).position + " " + transferRBs.get(i).name + ", " + transferRBs.get(i).getYrStr() + "  Ovr: " + transferRBs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferRBs.get(i).ratOvr + " " + transferRBs.get(i).position + " " + transferRBs.get(i).name + " [" + transferRBs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferRBs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerRB(transferRBs.get(i));
                                transferRBs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferRBs.get(i));
                                transferRBs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferWRs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamWRs().size() < 3 || teamList.get(t).getTeamWRs().get(0).ratOvr < transferWRs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferWRs.get(i).getRegion()) < 1) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferWRs.get(i).getYrStr() + " WR " + transferWRs.get(i).name + "(" + transferWRs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferWRs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferWRs.get(i).getYrStr() + " WR " + transferWRs.get(i).name + " (" + transferWRs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferWRs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferWRs.get(i).position + " " + transferWRs.get(i).name + ", " + transferWRs.get(i).getYrStr() + "  Ovr: " + transferWRs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferWRs.get(i).ratOvr + " " + transferWRs.get(i).position + " " + transferWRs.get(i).name + " [" + transferWRs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferWRs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerWR(transferWRs.get(i));
                                transferWRs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferWRs.get(i));
                                transferWRs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferTEs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamTEs().size() < 1 || teamList.get(t).getTeamTEs().get(0).ratOvr < transferTEs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferTEs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferTEs.get(i).getYrStr() + " TE " + transferTEs.get(i).name + "(" + transferTEs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferTEs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferTEs.get(i).getYrStr() + " TE " + transferTEs.get(i).name + " (" + transferTEs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferTEs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferTEs.get(i).position + " " + transferTEs.get(i).name + ", " + transferTEs.get(i).getYrStr() + "  Ovr: " + transferTEs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferTEs.get(i).ratOvr + " " + transferTEs.get(i).position + " " + transferTEs.get(i).name + " [" + transferTEs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferTEs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerTE(transferTEs.get(i));
                                transferTEs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferTEs.get(i));
                                transferTEs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferOLs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamOLs().size() < 5 || teamList.get(t).getTeamOLs().get(0).ratOvr < transferOLs.get(i).ratOvr && Math.abs(teamList.get(t).getLocation() - transferOLs.get(i).getRegion()) < loc) {
                        if (!teamList.get(t).isUserControlled()) {
                            newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferOLs.get(i).getYrStr() + " OL " + transferOLs.get(i).name + "(" + transferOLs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                    transferOLs.get(i).team.getAbbr() + " .");
                            newsHeadlines.add(transferOLs.get(i).getYrStr() + " OL " + transferOLs.get(i).name + " (" + transferOLs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                            if (transferOLs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                tOut.append(transferOLs.get(i).position + " " + transferOLs.get(i).name + ", " + transferOLs.get(i).getYrStr() + "  Ovr: " + transferOLs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                            }
                            transfersList.add(transferOLs.get(i).ratOvr + " " + transferOLs.get(i).position + " " + transferOLs.get(i).name + " [" + transferOLs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferOLs.get(i).team.getAbbr() + ")");
                            teamList.get(t).addPlayerOL(transferOLs.get(i));
                            transferOLs.remove(i);
                            break;
                        } else {
                            bridge.transferPlayer(transferOLs.get(i));
                            transferOLs.remove(i);
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < transferKs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamKs().size() < 1 || teamList.get(t).getTeamKs().get(0).ratOvr < transferKs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferKs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferKs.get(i).getYrStr() + " K " + transferKs.get(i).name + "(" + transferKs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferKs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferKs.get(i).getYrStr() + " K " + transferKs.get(i).name + " (" + transferKs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferKs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferKs.get(i).position + " " + transferKs.get(i).name + ", " + transferKs.get(i).getYrStr() + "  Ovr: " + transferKs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferKs.get(i).ratOvr + " " + transferKs.get(i).position + " " + transferKs.get(i).name + " [" + transferKs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferKs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerK(transferKs.get(i));
                                transferKs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferKs.get(i));
                                transferKs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferDLs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamDLs().size() < 4 || teamList.get(t).getTeamDLs().get(0).ratOvr < transferDLs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferDLs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferDLs.get(i).getYrStr() + " DL " + transferDLs.get(i).name + "(" + transferDLs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferDLs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferDLs.get(i).getYrStr() + " DL " + transferDLs.get(i).name + " (" + transferDLs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferDLs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferDLs.get(i).position + " " + transferDLs.get(i).name + ", " + transferDLs.get(i).getYrStr() + "  Ovr: " + transferDLs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferDLs.get(i).ratOvr + " " + transferDLs.get(i).position + " " + transferDLs.get(i).name + " [" + transferDLs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferDLs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerDL(transferDLs.get(i));
                                transferDLs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferDLs.get(i));
                                transferDLs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferLBs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamLBs().size() < 3 || teamList.get(t).getTeamLBs().get(0).ratOvr < transferLBs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferLBs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferLBs.get(i).getYrStr() + " LB " + transferLBs.get(i).name + "(" + transferLBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferLBs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferLBs.get(i).getYrStr() + " LB " + transferLBs.get(i).name + " (" + transferLBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferLBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferLBs.get(i).position + " " + transferLBs.get(i).name + ", " + transferLBs.get(i).getYrStr() + "  Ovr: " + transferLBs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferLBs.get(i).ratOvr + " " + transferLBs.get(i).position + " " + transferLBs.get(i).name + " [" + transferLBs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferLBs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerLB(transferLBs.get(i));
                                transferLBs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferLBs.get(i));
                                transferLBs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferCBs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamCBs().size() < 3 || teamList.get(t).getTeamCBs().get(0).ratOvr < transferCBs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferCBs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferCBs.get(i).getYrStr() + " CB " + transferCBs.get(i).name + "(" + transferCBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferCBs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferCBs.get(i).getYrStr() + " CB " + transferCBs.get(i).name + " (" + transferCBs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferCBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferCBs.get(i).position + " " + transferCBs.get(i).name + ", " + transferCBs.get(i).getYrStr() + "  Ovr: " + transferCBs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferCBs.get(i).ratOvr + " " + transferCBs.get(i).position + " " + transferCBs.get(i).name + " [" + transferCBs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferCBs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerCB(transferCBs.get(i));
                                transferCBs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferCBs.get(i));
                                transferCBs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < transferSs.size(); ++i) {
                rand = random.nextInt((max - min) + 1) + min;
                for (int t = rand; t < teamList.size() - rand; ++t) {
                    if (teamList.get(t).getTeamSs().size() < 1 || teamList.get(t).getTeamSs().get(0).ratOvr < transferSs.get(i).ratOvr) {
                        if (Math.abs(teamList.get(t).getLocation() - transferSs.get(i).getRegion()) < loc) {
                            if (!teamList.get(t).isUserControlled()) {
                                newsStories.get(currentWeek + 1).add(teamList.get(t).getName() + " Transfer News>" + transferSs.get(i).getYrStr() + " S " + transferSs.get(i).name + "(" + transferSs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ". He was previously enrolled at " +
                                        transferSs.get(i).team.getAbbr() + " .");
                                newsHeadlines.add(transferSs.get(i).getYrStr() + " S " + transferSs.get(i).name + " (" + transferSs.get(i).ratOvr + ") has announced his transfer to " + teamList.get(t).getName() + ".");

                                if (transferSs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                                    tOut.append(transferSs.get(i).position + " " + transferSs.get(i).name + ", " + transferSs.get(i).getYrStr() + "  Ovr: " + transferSs.get(i).ratOvr + " (" + teamList.get(t).getName() + ")\n\n");
                                }
                                transfersList.add(transferSs.get(i).ratOvr + " " + transferSs.get(i).position + " " + transferSs.get(i).name + " [" + transferSs.get(i).getTransferStatus() + "] " + teamList.get(t).getName() + " (" + transferSs.get(i).team.getAbbr() + ")");
                                teamList.get(t).addPlayerS(transferSs.get(i));
                                transferSs.remove(i);
                                break;
                            } else {
                                bridge.transferPlayer(transferSs.get(i));
                                transferSs.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
        }

        //The remaining user players transfer to FCS/Div II Football

        for (int i = 0; i < transferQBs.size(); ++i) {
            if(Math.random() > .50) {
                if (transferQBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                }
            } else {
                transferQBs.get(i).isTransfer = false;
                transferQBs.get(i).team.addPlayerQB(transferQBs.get(i));
            }
        }
        for (int i = 0; i < transferRBs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferRBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferRBs.get(i).position + " " + transferRBs.get(i).name + ", " + transferRBs.get(i).getYrStr() + "  Ovr: " + transferRBs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferRBs.get(i).isTransfer = false;
                transferRBs.get(i).team.addPlayerRB(transferRBs.get(i));
            }
        }
        for (int i = 0; i < transferWRs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferWRs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferWRs.get(i).position + " " + transferWRs.get(i).name + ", " + transferWRs.get(i).getYrStr() + "  Ovr: " + transferWRs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferWRs.get(i).isTransfer = false;
                transferWRs.get(i).team.addPlayerWR(transferWRs.get(i));
            }
        }
        for (int i = 0; i < transferTEs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferTEs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferTEs.get(i).position + " " + transferTEs.get(i).name + ", " + transferTEs.get(i).getYrStr() + "  Ovr: " + transferTEs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferTEs.get(i).isTransfer = false;
                transferTEs.get(i).team.addPlayerTE(transferTEs.get(i));
            }
        }
        for (int i = 0; i < transferOLs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferOLs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferOLs.get(i).position + " " + transferOLs.get(i).name + ", " + transferOLs.get(i).getYrStr() + "  Ovr: " + transferOLs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferOLs.get(i).isTransfer = false;
                transferOLs.get(i).team.addPlayerOL(transferOLs.get(i));
            }
        }
        for (int i = 0; i < transferKs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferKs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferKs.get(i).position + " " + transferKs.get(i).name + ", " + transferKs.get(i).getYrStr() + "  Ovr: " + transferKs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferKs.get(i).isTransfer = false;
                transferKs.get(i).team.addPlayerK(transferKs.get(i));
            }
        }
        for (int i = 0; i < transferDLs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferDLs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferDLs.get(i).position + " " + transferDLs.get(i).name + ", " + transferDLs.get(i).getYrStr() + "  Ovr: " + transferDLs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferDLs.get(i).isTransfer = false;
                transferDLs.get(i).team.addPlayerDL(transferDLs.get(i));
            }
        }
        for (int i = 0; i < transferLBs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferLBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferLBs.get(i).position + " " + transferLBs.get(i).name + ", " + transferLBs.get(i).getYrStr() + "  Ovr: " + transferLBs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferLBs.get(i).isTransfer = false;
                transferLBs.get(i).team.addPlayerLB(transferLBs.get(i));
            }
        }
        for (int i = 0; i < transferCBs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferCBs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferCBs.get(i).position + " " + transferCBs.get(i).name + ", " + transferCBs.get(i).getYrStr() + "  Ovr: " + transferCBs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferCBs.get(i).isTransfer = false;
                transferCBs.get(i).team.addPlayerCB(transferCBs.get(i));
            }
        }
        for (int i = 0; i < transferSs.size(); ++i) {
            if (Math.random() > .50) {
                if (transferSs.get(i).team.getAbbr().equals(userTeam.getAbbr())) {
                    tOut.append(transferSs.get(i).position + " " + transferSs.get(i).name + ", " + transferSs.get(i).getYrStr() + "  Ovr: " + transferSs.get(i).ratOvr + " (FCS)\n\n");
                }
            } else {
                transferSs.get(i).isTransfer = false;
                transferSs.get(i).team.addPlayerS(transferSs.get(i));
            }
        }

        //Sort Teams
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).sortPlayers();
        }
        
        userTransfers = tOut.toString() + "\n\nTransfers In:\n\n";

        Collections.sort(transfersList, Collections.<String>reverseOrder());
        StringBuilder transferSum = new StringBuilder();

        for (int i = 0; i < transfersList.size(); ++i) {
            transferSum.append(transfersList.get(i) + "\n\n");
        }

        newsStories.get(currentWeek + 1).add("Transfers Summary>" + transferSum);
        sumTransfers = "" + transferSum;

    }




    public void removeTransfer(Player p) {

        if (p.position.equals("QB")) transferQBs.remove(p);
        if (p.position.equals("RB")) transferRBs.remove(p);
        if (p.position.equals("WR")) transferWRs.remove(p);
        if (p.position.equals("TE")) transferTEs.remove(p);
        if (p.position.equals("OL")) transferOLs.remove(p);
        if (p.position.equals("K")) transferKs.remove(p);
        if (p.position.equals("DL")) transferDLs.remove(p);
        if (p.position.equals("LB")) transferLBs.remove(p);
        if (p.position.equals("CB")) transferCBs.remove(p);
        if (p.position.equals("S")) transferSs.remove(p);
    }
    
    
    
    

    
    
    
    
    ///////////////////////////////////////////////////////

    /*
Conference Realignment v2:

Every conference creates a bucket of teams that are not meeting minimum prestige threshold and a bucket of exceeding threshold (if lower tierd conference).

Then conferences can see if they want to add them to their list if the teams meet the new conference list... chosen at random.

*/
    public void conferenceRealignmentV2(GameUiBridge main) {
        GameUiBridge bridge = bridgeOrNoOp(main);
        int minConfTeams = 12;
        int maxConfTeams = 16;
        ArrayList<Team> demoteTeamList = new ArrayList<>();
        ArrayList<Team> promoteTeamList = new ArrayList<>();
        newsRealignment = "";
        countRealignment = 0;

        //Independent Home finding...
        if (advancedRealignment && Math.random() < realignmentChance) {
            ArrayList<Conference> confList = conferences;
            promoteTeamList = new ArrayList<>();


            //find the Independents
            for (int c = 0; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() < confList.get(c).minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        promoteTeamList.add(confList.get(c).confTeams.get(i));
                    }
                }
            }

            //Sort Prestige
            Collections.sort(promoteTeamList, new CompTeamPrestige());

            //try to find a conference for the independents
            for (int c = 0; c < conferences.size(); c++) {
                if (conferences.get(c).confTeams.size() < maxConfTeams && conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                    Conference conf = conferences.get(c);
                    for (int i = 0; i < promoteTeamList.size(); i++) {
                        if (promoteTeamList.get(i).getTeamPrestige() > (conf.confRelegateMin * 1.2) && Math.random() < realignmentChance && Math.abs(promoteTeamList.get(i).getLocation() - conf.confTeams.get(0).getLocation()) < 2) {
                            final Team teamA = promoteTeamList.get(i);
                            conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                            teamA.setConference(conf.confName);
                            conf.confTeams.add(teamA);

                            //break the news
                            newsStories.get(currentWeek + 1).add("No More Independence!>The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to be unaffiliated as an Independent.");

                            newsRealignment += ("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to be unaffiliated as an Independent.\n\n");
                            newsHeadlines.add("The " + conf.confName + " conference announced today they will be adding Indpendent " + teamA.getName() + " to their conference next season!");
                            countRealignment++;
                            promoteTeamList.remove(teamA);
                        }
                    }
                }
            }

        }

        //Advanced Realignment Craziness

        if (advancedRealignment && Math.random() < confRealignmentChance && leagueHistory.size() > 4) {

            ArrayList<Conference> confList = conferences;
            promoteTeamList = new ArrayList<>();


            //find the Independents
            for (int c = 0; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() < confList.get(c).minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        promoteTeamList.add(confList.get(c).confTeams.get(i));
                    }
                }
            }

            //Sort Prestige
            Collections.sort(promoteTeamList, new CompTeamPrestige());

            //Smaller Conferences Will Try to Expand Their Empire...
            for (int c = 0; c < conferences.size(); c++) {
                if (conferences.get(c).confTeams.size() < maxConfTeams && conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                    Conference conf = conferences.get(c);
                    for (int i = 0; i < promoteTeamList.size(); i++) {
                        if (promoteTeamList.get(i).getTeamPrestige() > (conf.confRelegateMin * 1.2) && Math.random() < realignmentChance && Math.abs(promoteTeamList.get(i).getLocation() - conf.confTeams.get(0).getLocation()) < 2) {
                            final Team teamA = promoteTeamList.get(i);
                            conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                            teamA.setConference(conf.confName);
                            conf.confTeams.add(teamA);

                            //break the news
                            newsStories.get(currentWeek + 1).add("No More Independence!>The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to be unaffiliated as an Independent.");

                            newsRealignment += ("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to be unaffiliated as an Independent.\n\n");
                            newsHeadlines.add("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season!");
                            countRealignment++;
                            promoteTeamList.remove(teamA);
                        }
                    }
                }
            }

            //find the teams do not meet conference threshold
            Collections.sort(confList, new CompConfPrestige());
            demoteTeamList = new ArrayList<>();
            promoteTeamList = new ArrayList<>();

            for (int c = 0; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() >= confList.get(c).minConfTeams && confList.get(c).confTeams.size() > minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        if (confList.get(c).confTeams.get(i).getTeamPrestige() < confList.get(c).confRelegateMin) {
                            demoteTeamList.add(confList.get(c).confTeams.get(i));
                        }
                    }
                }
            }

            //Sort Prestige
            Collections.sort(demoteTeamList, new CompTeamPrestige());

            //Smaller Conferences Will Try to Expand Their Empire...
            for (int c = 0; c < conferences.size(); c++) {
                if (conferences.get(c).confTeams.size() < maxConfTeams && conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                    Conference conf = conferences.get(c);
                    for (int i = 0; i < demoteTeamList.size(); i++) {
                        if (demoteTeamList.get(i).getTeamPrestige() > conf.confPromoteMin && Math.random() < realignmentChance && Math.abs(demoteTeamList.get(i).getLocation() - conf.confTeams.get(0).getLocation()) < 2 && !demoteTeamList.get(i).getConference().equals(conferences.get(c).confName)) {
                            final Team teamA = demoteTeamList.get(i);
                            final String oldConf = teamA.getConference();
                            if (conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > conferences.get(getConfNumber(teamA.getConference())).minConfTeams && conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > minConfTeams) {
                                conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                                teamA.setConference(conf.confName);
                                conf.confTeams.add(teamA);

                                //break the news
                                newsStories.get(currentWeek + 1).add("Conference Growth!>The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to part of the " + oldConf + " Conference.");

                                newsRealignment += ("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to part of the " + oldConf + " Conference.\n\n");
                                newsHeadlines.add("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season!");
                                countRealignment++;
                                demoteTeamList.remove(teamA);
                            }
                        }
                    }
                }
            }

            //find the teams that are doing too well for their conference level
            for (int c = 0; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() >= confList.get(c).minConfTeams && confList.get(c).confTeams.size() > minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        if (confList.get(c).confTeams.get(i).getTeamPrestige() > confList.get(c).confPromoteMin) {
                            promoteTeamList.add(confList.get(c).confTeams.get(i));
                        }
                    }
                }
            }

            //Sort Prestige
            Collections.sort(promoteTeamList, new CompTeamPrestige());

            //Smaller Conferences Will Try to Expand Their Empire...
            for (int c = 0; c < conferences.size(); c++) {
                if (conferences.get(c).confTeams.size() < maxConfTeams && conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                    Conference conf = conferences.get(c);
                    for (int i = 0; i < promoteTeamList.size(); i++) {
                        if (promoteTeamList.get(i).getTeamPrestige() > conf.confPromoteMin && Math.random() < realignmentChance && Math.abs(promoteTeamList.get(i).getLocation() - conf.confTeams.get(0).getLocation()) < 2 && !promoteTeamList.get(i).getConference().equals(conferences.get(c).confName) && conferences.get(getConfNumber(promoteTeamList.get(i).getConference())).confPrestige < conf.confPrestige) {
                            final Team teamA = promoteTeamList.get(i);
                            final String oldConf = teamA.getConference();
                            if (conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > conferences.get(getConfNumber(teamA.getConference())).minConfTeams && conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > minConfTeams) {
                                conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                                teamA.setConference(conf.confName);
                                conf.confTeams.add(teamA);

                                //break the news
                                newsStories.get(currentWeek + 1).add("Conference Growth!>The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to part of the " + oldConf + " Conference.");

                                newsRealignment += ("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season! " + teamA.getName() + " used to part of the " + oldConf + " Conference.\n\n");
                                newsHeadlines.add("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " to their conference next season!");
                                countRealignment++;
                                promoteTeamList.remove(teamA);
                            }
                        }
                    }
                }
            }

            //Let's kick people out to Independents
            int indConf = 0;
            boolean indSpace = false;
            for (Conference c : conferences) {
                if (c.confName.equals("Independent") && c.confTeams.size() < c.minConfTeams) {
                    indConf = getConfNumber(c.confName);
                    indSpace = true;
                }
            }
            if (indSpace) {
                Conference indy = conferences.get(indConf);
                for (int i = 0; i < demoteTeamList.size(); i++) {
                    if (Math.random() < realignmentChance && indy.confTeams.size() < (indy.minConfTeams - 1) && !demoteTeamList.get(i).getConference().equals("Independent")) {
                        final Team teamA = demoteTeamList.get(i);
                        final String oldConf = teamA.getConference();

                        if (conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > conferences.get(getConfNumber(teamA.getConference())).minConfTeams && conferences.get(getConfNumber(teamA.getConference())).confTeams.size() > minConfTeams) {
                            conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                            teamA.setConference(indy.confName);
                            indy.confTeams.add(teamA);

                            //break the news
                            newsStories.get(currentWeek + 1).add("Conference and Team Part Ways!>The " + oldConf + " conference announced today they will be removing " + teamA.getName() + " from their conference next season! " + teamA.getName() + " will become an Independent school until picked up by a new conference.");

                            newsRealignment += ("The " + oldConf + " conference announced today they will be removing " + teamA.getName() + " from their conference next season! " + teamA.getName() + " will become and Independent school until picked up by a new conference\n\n");
                            newsHeadlines.add("The " + oldConf + " conference announced today they will be removing " + teamA.getName() + " from their conference next season!");
                            countRealignment++;
                            demoteTeamList.remove(teamA);
                        }
                    }
                }
            }
        }


        //Promote FCS School
        if (advancedRealignment && Math.random() < confRealignmentChance && Math.random() < realignmentChance) {
            int matches = 0;
            for (int t = 0; t < teamsFCSList.size(); t++) {
                for (int x = 0; x < teamList.size(); x++) {
                    if (teamList.get(x).getName().equals(teamsFCSList.get(t))) {
                        matches++;
                    }
                }
            }

            int indConf = 0;
            for (Conference c : conferences) {
                if (c.confName.equals("Independent") && c.confTeams.size() < c.minConfTeams) {
                    indConf = getConfNumber(c.confName);
                }
            }

            if (matches < teamsFCSList.size()) {
                Conference indy = conferences.get(indConf);
                if (conferences.get(indConf).confTeams.size() < indy.minConfTeams - 1) {
                    String fcsName = "New Team";
                    Boolean named = false;

                    while (!named) {
                        int nameTest = 0;
                        fcsName = teamsFCSList.get((int) (teamsFCSList.size() * Math.random()));
                        for (int i = 0; i < teamList.size(); i++) {
                            if (teamList.get(i).getName().equals(fcsName)) {
                                nameTest++;
                            }
                        }
                        if (nameTest == 0) {
                            named = true;
                        }
                    }


                    Team FCS = new Team(fcsName, "FCS", "Independent", 35, "A", (int) (Math.random() * 6), this, true);
                    FCS.setAbbr(FCS.getName().substring(0, 3));
                    FCS.setRankTeamPollScore(teamList.size());
                    teamList.add(FCS);
                    indy.confTeams.add(FCS);

                    //break the news
                    newsStories.get(currentWeek + 1).add("Lower Division School Promoted!>Today is a special day at " + FCS.getName() + ". They have been promoted to Division I College Football today, and will be listed as an Independent team!");

                    newsRealignment += (FCS.getName() + " have been promoted to Division I College Football today, and will be listed as an Independent team!\n\n");
                    newsHeadlines.add(FCS.getName() + " have been promoted to Division I College Football today, and will be listed as an Independent team!\n\n");
                    countRealignment++;
                }
            }
        }

        //Create New Conference
        if (advancedRealignment && Math.random() < confRealignmentChance && Math.random() < realignmentChance && Math.random() < 0.35) {
            int matches = 0;
            for (Conference c : conferences) {
                if (c.confName.equals("Antdroid")) {
                    matches++;
                }
            }
            if (matches <= 0) {
                matches = 0;
                for (int t = 0; t < teamsFCSList.size(); t++) {
                    for (int x = 0; x < teamList.size(); x++) {
                        if (teamList.get(x).getName().equals(teamsFCSList.get(t))) {
                            matches++;
                        }
                    }
                }

                if (matches < teamsFCSList.size() - 12) {

                    //Create new Conference
                    Conference antdroid = new Conference("Antdroid", this, false, 0, 0);
                    conferences.add(antdroid);

                    //Find Independent Conf
                    int indConf = 0;
                    for (Conference c : conferences) {
                        if (c.confName.equals("Independent") && c.confTeams.size() < c.minConfTeams) {
                            indConf = getConfNumber(c.confName);
                        }
                    }
                    Conference indy = conferences.get(indConf);

                    //Move Independent Teams to new Conf & remove from independents
                    for (int i = 0; i < indy.confTeams.size(); i++) {
                        indy.confTeams.get(i).setConference("Antdroid");
                        antdroid.confTeams.add(indy.confTeams.get(i));
                    }
                    for (int i = 0; i < antdroid.confTeams.size(); i++) {
                        indy.confTeams.remove(antdroid.confTeams.get(i));
                    }

                    //Make new Teams
                    int count = antdroid.confTeams.size();
                    while (count < 10) {
                        String fcsName = "New Team";
                        Boolean named = false;

                        while (!named) {
                            int nameTest = 0;
                            fcsName = teamsFCSList.get((int) (teamsFCSList.size() * Math.random()));
                            for (int i = 0; i < teamList.size(); i++) {
                                if (teamList.get(i).getName().equals(fcsName)) {
                                    nameTest++;
                                }
                            }
                            if (nameTest == 0) {
                                named = true;
                            }
                        }

                        Team FCS = new Team(fcsName, "FCS", "Antdroid", 35, "A", (int) (Math.random() * 6), this, true);
                        FCS.setAbbr(FCS.getName().substring(0, 3));
                        FCS.setRankTeamPollScore(teamList.size());
                        teamList.add(FCS);
                        antdroid.confTeams.add(FCS);
                        count++;
                    }

                    //break the news
                    newsStories.get(currentWeek + 1).add("NEW CONFERENCE ANNOUNCED!>Today, the League is excited to announce the formation of the Antdroid Conference! This conference will be a mix of Independent teams and newly promoted lower level teams!");

                    newsRealignment += ("NEW CONFERENCE ANNOUNCED! Today, the League is excited to announce the formation of the Antdroid Conference! This conference will be a mix of Independent teams and newly promoted lower level teams!\n\n");
                    newsHeadlines.add("NEW CONFERENCE ANNOUNCED! Today, the League is excited to announce the formation of the Antdroid Conference!");
                    countRealignment++;

                    bridge.updateSpinners();

                }
            }
        }

        //Conference Realignment (Trading Teams between Conferences)
        if (Math.random() < confRealignmentChance && leagueHistory.size() > 4) {
            ArrayList<Conference> confList = conferences;
            Collections.sort(confList, new CompConfPrestige());
            demoteTeamList = new ArrayList<>();
            promoteTeamList = new ArrayList<>();

            //find conferences for independents
            for (int c = 0; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() < confList.get(c).minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        if (confList.get(c).confTeams.get(i).getTeamPrestige() < confList.get(c).confRelegateMin) {
                            demoteTeamList.add(confList.get(c).confTeams.get(i));
                        }
                    }
                }
            }

            //Smaller Conferences Will Try to Expand Their Empire...
            for (int c = 0; c < conferences.size(); c++) {
                if (conferences.get(c).confTeams.size() < 15) {
                    Conference conf = conferences.get(c);
                    ArrayList<Team> qualified = new ArrayList<>();
                    for (int i = 0; i < demoteTeamList.size(); i++) {
                        if (demoteTeamList.get(i).getTeamPrestige() > conf.confRelegateMin && Math.random() < realignmentChance * 2 && Math.abs(demoteTeamList.get(i).getLocation() - conf.confTeams.get(0).getLocation()) < 2) {
                            qualified.add(demoteTeamList.get(i));
                        }
                        if (qualified.size() >= 2) {
                            final Team teamA = qualified.get(0);
                            final Team teamB = qualified.get(1);
                            conferences.get(getConfNumber(teamA.getConference())).confTeams.remove(teamA);
                            conferences.get(getConfNumber(teamB.getConference())).confTeams.remove(teamB);
                            teamA.setConference(conf.confName);
                            teamB.setConference(conf.confName);
                            conf.confTeams.add(teamA);
                            conf.confTeams.add(teamB);

                            //break the news
                            newsStories.get(currentWeek + 1).add("Conference Addition!>The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " and " + teamB.getName() + " from the " + conf.confName + " to their conference next season!");

                            newsRealignment += ("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " and " + teamB.getName() + " from the " + conf.confName + " to their conference next season!\n\n");
                            newsHeadlines.add("The " + conf.confName + " conference announced today they will be adding " + teamA.getName() + " and " + teamB.getName() + " from the " + conf.confName + " to their conference next season!");
                            countRealignment++;
                            demoteTeamList.remove(teamA);
                            demoteTeamList.remove(teamB);
                        }
                    }
                }
            }

            //find the teams that are doing too well for their conference level
            for (int c = confList.size() / 2; c < confList.size(); c++) {
                if (confList.get(c).confTeams.size() >= confList.get(c).minConfTeams) {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        if (confList.get(c).confTeams.get(i).getTeamPrestige() > confList.get(c).confPromoteMin) {
                            promoteTeamList.add(confList.get(c).confTeams.get(i));
                        }
                    }
                } else {
                    for (int i = 0; i < confList.get(c).confTeams.size(); i++) {
                        promoteTeamList.add(confList.get(c).confTeams.get(i));
                    }
                }
            }

            //Sort Prestige
            Collections.sort(promoteTeamList, new CompTeamPrestige());

            //Bigger Conferences will extend Invites
            for (int i = 0; i < promoteTeamList.size(); i++) {
                int randomConf = (int) (Math.random() * (confList.size() / 2));
                if (promoteTeamList.get(i).getTeamPrestige() > confList.get(randomConf).confPromoteMin && confList.get(randomConf).confTeams.size() >= confList.get(randomConf).minConfTeams) {
                    for (int k = confList.get(randomConf).confTeams.size() - 1; k >= 0; k--) {
                        if (confList.get(randomConf).confTeams.get(k).getTeamPrestige() < confList.get(randomConf).confRelegateMin) {
                            if (Math.random() < realignmentChance && Math.abs(confList.get(randomConf).confTeams.get(k).getLocation() - promoteTeamList.get(i).getLocation()) < 2) {

                                final String teamAconf = confList.get(randomConf).confName;
                                final String teamBconf = promoteTeamList.get(i).getConference();
                                final Team teamA = confList.get(randomConf).confTeams.get(k);
                                final Team teamB = promoteTeamList.get(i);

                                //transfer conf data
                                teamB.setConference(teamAconf);
                                teamA.setConference(teamBconf);

                                //remove + transfer teams
                                confList.get(getConfNumber(teamAconf)).confTeams.remove(teamA);
                                confList.get(getConfNumber(teamBconf)).confTeams.remove(teamB);

                                confList.get(getConfNumber(teamAconf)).confTeams.add(teamB);
                                confList.get(getConfNumber(teamBconf)).confTeams.add(teamA);

                                //Remove some prestige from demoted teams
                                teamA.setTeamPrestige(teamA.getTeamPrestige() - (int) Math.random() * 4);

                                //break the news
                                newsStories.get(currentWeek + 1).add("Conference Realignment News>The " + teamAconf + " conference announced today they will be adding " + teamB.getName() + " to their conference next season! The " + teamBconf + " conference has agreed to add " + teamA.getName() + " as part of the realignment.");

                                newsRealignment += ("The " + teamAconf + " announced today they will be adding " + teamB.getName() + " to their conference next season! The " + teamBconf + " conference has agreed to add " + teamA.getName() + " as part of the realignment.\n\n");
                                newsHeadlines.add("The " + teamAconf + " announced today they will be adding " + teamB.getName() + " to their conference next season! The " + teamBconf + " conference has agreed to add " + teamA.getName() + " as part of the realignment.");

                                countRealignment++;
                                promoteTeamList.remove(teamA);
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (Conference c : conferences) {
            if (c.confTeams.size() < c.minConfTeams) {
                c.confPrestige = 0;
            } else {
                c.updateConfPrestige();
            }
        }
        Collections.sort(conferences, new CompConfPrestige());
    }

    //Universal Pro/Rel system - uses EVERY conferences. This is a giant Premier League + Championship + League 1 + League 2 + National League + Feeder Leagues FA-like system.
    public void convertUnivProRel() {

        //Reorder Every Team by Rank
        Collections.sort(teamList, new CompTeamPrestige());

        //check if there is an even number of teams
        if(teamList.size() % 2 != 0) {
            teamList.add(new Team(teamsFCSList.get((int) (teamsFCSList.size() * Math.random())), "FCS", "FCS Division", (int) (Math.random() * 40), "FCS1", (int)(Math.random()*6), this, false));
        }

        //Clear all Conferences
        for (int i = 0; i < conferences.size(); i++) {
            conferences.get(i).confTeams.clear();
            conferences.get(i).confName = getRankStr(i + 1) + " Tier";
            conferences.get(i).TV = conferences.get(i).getTVName();
        }

        //int teamsPerConf = teamList.size() / conferences.size();
        int teamsPerConf = 20;
        //if (teamsPerConf % 2 != 0) teamsPerConf++;  //check if even # of teams per conf

        int c = 0, next = 0;
        for (int t = 0; t < teamList.size(); t++) {
            if (c == conferences.size() - 1) {

            } else if (next == teamsPerConf) {
                next = 0;
                c++;
            }
            teamList.get(t).setConference(conferences.get(c).confName);
            teamList.get(t).setDivision("A");
            teamList.get(t).clearGameSchedule();
            conferences.get(c).confTeams.add(teamList.get(t));
            next++;
        }

        if(currentWeek == 0) setupSeason();
        updateTeamTalentRatings();
        setTeamBenchMarks();
        setTeamRanks();
        Collections.sort(teamList, new CompTeamPrestige());
    }

    //Universal Promotion/Relegation System - just like in Soccer. Can be enabled. Disabled by default.
    public void universalProRel() {

        //Get list of Promoted & Relegated Teams
        ArrayList<Team> promotedTeams = new ArrayList<>();
        ArrayList<Team> relegatedTeams = new ArrayList<>();

        int confSize = 0;
        for (int c = 0; c < conferences.size(); c++) {
            if (conferences.get(c).confTeams.size() >= conferences.get(c).minConfTeams) {
                confSize++;
            }
        }
        for (int i = 1; i < confSize; ++i) {
            for (int t = 0; t < conferences.get(i).confTeams.size(); t++) {
                if (conferences.get(i).confTeams.get(t).getGameSchedule().size() > 12 && conferences.get(i).confTeams.get(t).getGameSchedule().get(12).gameName.contains("CCG"))
                    promotedTeams.add(conferences.get(i).confTeams.get(t));
            }
        }

        for (int i = 0; i < confSize; ++i) {
            int size = conferences.get(i).confTeams.size();
            relegatedTeams.add(conferences.get(i).confTeams.get(size - 1));
            relegatedTeams.add(conferences.get(i).confTeams.get(size - 2));
        }

        //Remove more prestige from teams
        for (int i = 0; i < relegatedTeams.size(); i++) {
            relegatedTeams.get(i).setTeamPrestige(relegatedTeams.get(i).getTeamPrestige() - 3);
        }

        StringBuilder string = new StringBuilder();
        for (int i = 0; i < confSize - 1; ++i) {

            Conference PConf = conferences.get(i);
            Conference RConf = conferences.get(i + 1);
            string.append("[ " + PConf.confName + " || " + RConf.confName + " ]\n");

            //change team conferences
            promotedTeams.get(2 * i).setConference(PConf.confName);
            promotedTeams.get(2 * i + 1).setConference(PConf.confName);
            relegatedTeams.get(2 * i).setConference(RConf.confName);
            relegatedTeams.get(2 * i + 1).setConference(RConf.confName);

            //Remove teams from Conferences
            PConf.confTeams.remove(relegatedTeams.get(2 * i));
            PConf.confTeams.remove(relegatedTeams.get(2 * i + 1));
            RConf.confTeams.remove(promotedTeams.get(2 * i));
            RConf.confTeams.remove(promotedTeams.get(2 * i + 1));

            //Add teams to Conferences
            PConf.confTeams.add(promotedTeams.get(2 * i));
            PConf.confTeams.add(promotedTeams.get(2 * i + 1));
            RConf.confTeams.add(relegatedTeams.get(2 * i));
            RConf.confTeams.add(relegatedTeams.get(2 * i + 1));

            //calculate Conf Prestige
            PConf.updateConfPrestige();
            RConf.updateConfPrestige();

            //break the news
            string.append("Promoted to " + PConf.confName + " Conference (" + PConf.confPrestige + ")\n");
            string.append(" + " + promotedTeams.get(2 * i).getName() + " (" + promotedTeams.get(2 * i).getTeamPrestige() + ")\n" + " + " + promotedTeams.get(2 * i + 1).getName() + " (" + promotedTeams.get(2 * i + 1).getTeamPrestige() + ")\n");
            string.append("Relegated to " + RConf.confName + " Conference\n");
            string.append(" - " + relegatedTeams.get(2 * i).getName() + " (" + relegatedTeams.get(2 * i).getTeamPrestige() + ")\n" + " - " + relegatedTeams.get(2 * i + 1).getName() + " (" + relegatedTeams.get(2 * i + 1).getTeamPrestige() + ")\n");
            string.append("\n");

        }

        //post news in News
        newsRealignment = string.toString();
        newsStories.get(currentWeek + 1).add("Promotion/Relegation Update>" + newsRealignment);
        newsHeadlines.add("Promotion/Relegation Update>" + newsRealignment);
    }

    //Mock Draft String
    public String[] getMockDraftPlayersList() {
        ArrayList<Player> allPlayersLeaving = new ArrayList<>();
        for (Team t : teamList) {
            for (Player p : t.getPlayersLeaving()) {
                allPlayersLeaving.add(p);
            }
        }

        Collections.sort(allPlayersLeaving, new CompNFLTalent());
        ArrayList<Player> NFLPlayers = new ArrayList<>();

        if (allPlayersLeaving.size() > 224) {
            for (int i = 0; i < 224; ++i) {
                NFLPlayers.add(allPlayersLeaving.get(i));
            }
        } else {
            // Get first 3 rounds
            for (int i = 0; i < 96; ++i) {
                NFLPlayers.add(allPlayersLeaving.get(i));
            }
        }

        List<String> nfl = Arrays.asList(proTeams);
        Collections.shuffle(nfl);

        String[] nflPlayers = new String[NFLPlayers.size()];
        int n = 0, r = 1;
        for (int i = 0; i < nflPlayers.length; ++i) {
            nflPlayers[i] = NFLPlayers.get(i).getMockDraftStr(r, n + 1, nfl.get(n));
            n++;
            if (n >= nfl.size()) {
                n = 0;
                r++;
            }
        }

        return nflPlayers;
    }

    //CPU Recruiting - Tells each team to recruit players
    public void recruitPlayers() {
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).CPUrecruiting();
        }
    }

    //Mid-Season - Give players who have been playing games a bonus
    public void midSeasonProgression() {
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).midSeasonProgression();
        }
    }

    //advances team players
    public void advanceSeason() {
        for (int t = 0; t < teamList.size(); ++t) {

            //temp fix.....
            if(teamList.get(t).getOC() == null || teamList.get(t).getDC() == null) coordinatorCarousel();

            teamList.get(t).advanceTeamPlayers();
        }

        advanceSeasonWinStreaks();

        if (enableTV) newsTV = new ArrayList<>();
        for (int c = 0; c < conferences.size(); ++c) {
            if (enableTV) conferences.get(c).reviewConfTVDeal();
        }

    }


    /**
     * Check the longest win streak. If the given streak is longer, replace.
     *
     * @param streak streak to check
     */
    public void checkLongestWinStreak(TeamStreak streak) {
        if (streak.getStreakLength() > longestWinStreak.getStreakLength()) {
            longestWinStreak = new TeamStreak(streak.getStartYear(), streak.getEndYear(), streak.getStreakLength(), streak.getTeam());
        }
    }

    /**
     * Gets the longest active win streak.
     */
    private void updateLongestActiveWinStreak() {
        for (Team t : teamList) {
            if (t.getWinStreak().getStreakLength() > longestActiveWinStreak.getStreakLength()) {
                longestActiveWinStreak = t.getWinStreak();
            }
        }
    }

    /**
     * Advance season for win streaks, so no save-load whackiness.
     */
    private void advanceSeasonWinStreaks() {
        yearStartLongestWinStreak = longestWinStreak;
    }

    /**
     * Change the team abbr of the lognest win streak if the user changed it
     *
     * @param oldAbbr old abbreviation
     * @param newAbbr new abbreviation
     */
    private void changeAbbrWinStreaks(String oldAbbr, String newAbbr) {
        if (longestWinStreak.getTeam().equals(oldAbbr)) {
            longestWinStreak.changeAbbr(newAbbr);
        }
        if (yearStartLongestWinStreak.getTeam().equals(oldAbbr)) {
            yearStartLongestWinStreak.changeAbbr(newAbbr);
        }
    }

    /**
     * Changes all the abbrs to new abbr, in records and histories.
     *
     * @param oldAbbr
     * @param newAbbr
     */
    public void changeAbbrHistoryRecords(String oldAbbr, String newAbbr) {
        // check records and win streaks
        leagueRecords.changeAbbrRecords(userTeam.getAbbr(), newAbbr);
        changeAbbrWinStreaks(userTeam.getAbbr(), newAbbr);
        userTeam.getWinStreak().changeAbbr(newAbbr);

        // check league and POTY history
        for (String[] yr : leagueHistory) {
            for (int i = 0; i < yr.length; ++i) {
                if (yr[i].split(" ")[0].equals(oldAbbr)) {
                    yr[i] = newAbbr + " " + yr[i].split(" ")[1];
                }
            }
        }

        for (int i = 0; i < heismanHistory.size(); ++i) {
            String p = heismanHistory.get(i);
            if (p.split(" ")[4].equals(oldAbbr)) {
                heismanHistory.set(i,
                        p.split(" ")[0] + " " +
                                p.split(" ")[1] + " " +
                                p.split(" ")[2] + " " +
                                p.split(" ")[3] + " " +
                                newAbbr + " " +
                                p.split(" ")[5]);
            }
        }

    }

    /**
     * Checks if any of the league records were broken by teams.
     */
    public void checkLeagueRecords() {
        for (Team t : teamList) {
            t.checkLeagueRecords(leagueRecords);
            t.checkTeamRecords(t.getTeamRecords());
        }
    }

    /**
     * Gets all the league records, including the longest win streak
     *
     * @return string of all the records, csv
     */
    public String getLeagueRecordsStr() {
        String winStreakStr = "Longest Win Streak," + longestWinStreak.getStreakLength() + "," +
                longestWinStreak.getTeam() + "," + longestWinStreak.getStartYear() + "-" + longestWinStreak.getEndYear() + "\n";
        String activeWinStreakStr = "Active Win Streak," + longestActiveWinStreak.getStreakLength() + "," +
                longestActiveWinStreak.getTeam() + "," + longestActiveWinStreak.getStartYear() + "-" + longestActiveWinStreak.getEndYear() + "\n";
        return winStreakStr + activeWinStreakStr + leagueRecords.getRecordsStr();
    }

    /**
     * At the end of the year, record the top 25 teams for the League's History.
     */
    public void updateLeagueHistory() {
        //update league history
        Collections.sort(teamList, new CompTeamPoll());
        String[] yearTop25 = new String[25];
        Team tt;
        for (int i = 0; i < 25; ++i) {
            tt = teamList.get(i);
            yearTop25[i] = tt.getName() + " (" + tt.getWins() + "-" + tt.getLosses() + ")";
        }
        leagueHistory.add(yearTop25);
    }

    public String getLeagueTop25History(int year) {
        String hist = "";
        hist += (seasonStart + year) + " Top 25 Rankings:\n";
        for (int i = 0; i < 25; ++i) {
            if (i < 9) {
                hist += "\t 0" + (i + 1) + ":  " + leagueHistory.get(year)[i] + "\n";
            } else {
                hist += "\t " + (i + 1) + ":  " + leagueHistory.get(year)[i] + "\n";
            }

        }
        return hist;
    }

    /**
     * Get String of the league's history, year by year.
     * Saves the NCG winner and the POTY.
     *
     * @return list of the league's history.
     */
    public String getLeagueHistoryStr() {
        String hist = "";
        for (int i = leagueHistory.size(); i > 0; --i) {
            hist += (seasonStart + i-1) + ":\n";
            hist += "\tChampions: " + leagueHistory.get(i-1)[0] + "\n";
            hist += "\tOff: " + heismanHistory.get(i-1).split(">")[0] + "\n";
            if (heismanHistory.get(i-1).split(">").length > 1)
                hist += "\tDef: " + heismanHistory.get(i-1).split(">")[1] + "\n%";
            else hist += "%";
        }
        return hist;
    }


    /**
     * Updates team history for each team.
     */
    public void updateTeamHistories() {
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updateTeamHistory();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Updates poll scores for each team and updates their ranking.
     */
    public void setTeamRanks() {
        //get team ranks for PPG, YPG, etc
        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updatePollScore();
        }

        Collections.sort(teamList, new CompTeamPoll());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamPollScore(t + 1);
        }

        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).calcRPI();
        }

        Collections.sort(teamList, new CompTeamRPI());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamRPI(t + 1);
        }

        for (int i = 0; i < teamList.size(); ++i) {
            teamList.get(i).updateSOS();
        }

        Collections.sort(teamList, new CompTeamSoS());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamSOS(t + 1);
        }

        Collections.sort(teamList, new CompTeamSoW());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamStrengthOfWins(t + 1);
        }

        Collections.sort(teamList, new CompTeamPPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamPoints(t + 1);
        }

        Collections.sort(teamList, new CompTeamOPPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamOppPoints(t + 1);
        }

        Collections.sort(teamList, new CompTeamYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamOYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamOppYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamPYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamPassYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamRYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamRushYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamOPYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamOppPassYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamORYPG());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamOppRushYards(t + 1);
        }

        Collections.sort(teamList, new CompTeamTODiff());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamTODiff(t + 1);
        }

        Collections.sort(teamList, new CompTeamOffTalent());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamOffTalent(t + 1);
        }

        Collections.sort(teamList, new CompTeamDefTalent());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamDefTalent(t + 1);
        }

        Collections.sort(teamList, new CompTeamPrestige());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamPrestige(t + 1);
        }

        Collections.sort(teamList, new CompTeamDisciplineScore());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamDisciplineScore(t + 1);
        }

        Collections.sort(teamList, new CompTeamBudget());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamBudget(t + 1);
        }

        Collections.sort(teamList, new CompTeamFacilities());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamFacilities(t + 1);
        }

        Collections.sort(teamList, new CompTeamChemistry());
        for (int t = 0; t < teamList.size(); ++t) {
            teamList.get(t).setRankTeamChemistry(t + 1);
        }

        if (currentWeek == 0) {
            Collections.sort(teamList, new CompTeamRecruitClass());
            for (int t = 0; t < teamList.size(); ++t) {
                teamList.get(t).setRankTeamRecruitClass(t + 1);
            }
        }
    }

    /**
     * Get list of all the teams and their rankings based on selection
     *
     * @param selection stat to sort by, 0-13
     * @return list of the teams: ranking,str rep,stat
     */
    public ArrayList<String> getTeamRankingsStr(int selection) {
        /*
         */
        ArrayList<Team> teams = teamList;

        ArrayList<HeadCoach> HC = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getHeadCoach() != null) HC.add(teamList.get(i).getHeadCoach());
        }

        ArrayList<String> rankings = new ArrayList<>();
        Team t;
        switch (selection) {
            case 0:
                Collections.sort(teams, new CompTeamPoll());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format(t.getTeamPollScore()));
                }
                break;
            case 1:
                Collections.sort(teams, new CompTeamPrestige());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    if (currentWeek > 15 && (t.getTeamPrestige() - t.getTeamPrestigeStart()) > 0)
                        rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamPrestige() + "  (+" + (t.getTeamPrestige() - t.getTeamPrestigeStart()) + ")");
                    else if (currentWeek > 15 && (t.getTeamPrestige() - t.getTeamPrestigeStart()) < 0)
                        rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamPrestige() + "  (" + (t.getTeamPrestige() - t.getTeamPrestigeStart()) + ")");
                    else
                        rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamPrestige());
                }
                break;
            case 2:
                Collections.sort(teams, new CompTeamRPI());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df3.format(t.getTeamRPI()));
                }
                break;
            case 3:
                Collections.sort(teams, new CompTeamSoS());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df3.format(t.getTeamSOS()));
                }
                break;
            case 4:
                Collections.sort(teams, new CompTeamSoW());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamStrengthOfWins());
                }
                break;
            case 5:
                Collections.sort(teams, new CompTeamPPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamPoints() / t.numGames()));
                }
                break;
            case 6:
                Collections.sort(teams, new CompTeamOPPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamOppPoints() / t.numGames()));
                }
                break;
            case 7:
                Collections.sort(teams, new CompTeamYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamYards() / t.numGames()));
                }
                break;
            case 8:
                Collections.sort(teams, new CompTeamOYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamOppYards() / t.numGames()));
                }
                break;
            case 9:
                Collections.sort(teams, new CompTeamPYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamPassYards() / t.numGames()));
                }
                break;
            case 10:
                Collections.sort(teams, new CompTeamRYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamRushYards() / t.numGames()));
                }
                break;
            case 11:
                Collections.sort(teams, new CompTeamOPYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamOppPassYards() / t.numGames()));
                }
                break;
            case 12:
                Collections.sort(teams, new CompTeamORYPG());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format((float) t.getTeamOppRushYards() / t.numGames()));
                }
                break;
            case 13:
                Collections.sort(teams, new CompTeamTODiff());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    if (t.getTeamTODiff() > 0)
                        rankings.add(t.getRankStr(i + 1) + "," + t.getName() + ",+" + t.getTeamTODiff());
                    else
                        rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamTODiff());
                }
                break;
            case 14:
                Collections.sort(teams, new CompTeamOffTalent());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format(t.getTeamOffTalent()));
                }
                break;
            case 15:
                Collections.sort(teams, new CompTeamDefTalent());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format(t.getTeamDefTalent()));
                }
                break;
            case 16:
                Collections.sort(teams, new CompTeamChemistry());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + df2.format(t.getTeamChemistry()));
                }
                break;
            case 17:
                Collections.sort(teams, new CompTeamRecruitClass());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "\n" + t.getTopRecruit() + "," + df2.format(t.getRecruitingClassRat()));

                }
                break;
            case 18:
                Collections.sort(teams, new CompTeamDisciplineScore());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + (t.getTeamDisciplineScore()) + "%");
                }
                break;
            case 19:
                Collections.sort(teams, new CompTeamBudget());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + ",$" + t.getTeamBudget());
                }
                break;
            case 20:
                Collections.sort(teams, new CompTeamFacilities());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + ",L" + t.getTeamFacilities());
                }
                break;
            case 21:
                Collections.sort(HC, new CompCoachOvr());
                for (int i = 0; i < HC.size(); ++i) {
                    rankings.add((i + 1) + ". ," + HC.get(i).team.getName() + "," + HC.get(i).getStaffOverall(HC.get(i).overallWt));
                }
                break;
            case 22:
                Collections.sort(HC, new CompCoachScore());
                for (int i = 0; i < HC.size(); ++i) {
                    rankings.add((i + 1) + ". ," + HC.get(i).team.getName() + "," + HC.get(i).getCoachScore());
                }
                break;

            default:
                Collections.sort(teams, new CompTeamPoll());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTeamPollScore());
                }
                break;
        }

        return rankings;
    }

    public ArrayList<String> getLeagueHistoryStats(int selection) {
        /*
         */
        ArrayList<Team> teams = teamList;
        ArrayList<String> rankings = new ArrayList<>();
        Team t;

        ArrayList<HeadCoach> HC = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getHeadCoach() != null) HC.add(teamList.get(i).getHeadCoach());
        }

        switch (selection) {
            case 0:
                Collections.sort(teams, new CompTeamNC());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTotalNCs());
                }
                break;
            case 1:
                Collections.sort(teams, new CompTeamCC());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTotalCCs());
                }
                break;
            case 2:
                Collections.sort(teams, new CompTeamBowls());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getTotalBowls());
                }
                break;
            case 3:
                Collections.sort(teams, new CompTeamWins());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + (t.getTotalWins() + t.getWins()));
                }
                break;
            case 4:
                Collections.sort(teams, new CompTeamHoFCount());
                for (int i = 0; i < teams.size(); ++i) {
                    t = teams.get(i);
                    rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.getHoFCount());
                }
                break;
        }
        return rankings;
    }

    public void createCoachDatabase() {
        coachDatabase.clear();

        for(Staff x : coachDatabase) {
            coachDatabase.add(x);
        }

        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getHeadCoach() != null) coachDatabase.add(teamList.get(i).getHeadCoach());
            if (teamList.get(i).getOC() != null && teamList.get(i).getOC().getWins() > 0) coachDatabase.add(teamList.get(i).getOC());
            if (teamList.get(i).getDC() != null && teamList.get(i).getDC().getWins() > 0) coachDatabase.add(teamList.get(i).getDC());

        }

        for(Staff x : coachList) {
            if(x.getWins() > 0) coachDatabase.add(x);
        }

        for(Staff x : coachFreeAgents) {
            if(x.getWins() > 0) coachDatabase.add(x);
        }
    }

    public ArrayList<String> getCoachDatabase(int selection) {
        /*
         */
        coachDatabase.clear();
        ArrayList<String> rankings = new ArrayList<>();
        Staff c;

        createCoachDatabase();

        switch (selection) {
            case 0:
                Collections.sort(coachDatabase, new CompCoachNC());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getNCWins());
                }
                break;
            case 1:
                Collections.sort(coachDatabase, new CompCoachCC());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getConfWins());
                }
                break;
            case 2:
                Collections.sort(coachDatabase, new CompCoachBowlWins());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getBowlWins());
                }
                break;
            case 3:
                Collections.sort(coachDatabase, new CompCoachWins());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getWins());
                }
                break;
            case 4:
                Collections.sort(coachDatabase, new CompCoachWinPCT());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + df2.format(c.getWinPCT()) + "%");
                }
                break;
            case 5:
                Collections.sort(coachDatabase, new CompCoachCOTY());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getCOTY());
                }
                break;
            case 6:
                Collections.sort(coachDatabase, new CompCoachConfCOTY());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getConfCOTY());
                }
                break;
            case 7:
                Collections.sort(coachDatabase, new CompCoachAllAmericans());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getAllAmericans());
                }
                break;
            case 8:
                Collections.sort(coachDatabase, new CompCoachAllConference());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getAllConference());
                }
                break;
            case 9:
                Collections.sort(coachDatabase, new CompCoachCareer());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getCoachCareerScore());
                }
                break;
            case 10:
                Collections.sort(coachDatabase, new CompCoachCareerPrestige());
                for (int i = 0; i < coachDatabase.size(); ++i) {
                    c = coachDatabase.get(i);
                    rankings.add(getRankStr(i + 1) + "," + c.name + checkCoachStatus(c) + "," + c.getCumulativePrestige());
                }
                break;
        }
        return rankings;
    }

    private String checkCoachStatus(Staff c) {
        String s = "";
        if(c.retired) s = " [R]";
        else if (c.team == null) s = " [U]";
        else s = " (" + c.team.getAbbr() +")";

        return s;
    }


    /**
     * Get conference standings in an list of Strings.
     * Must be CSV form: Rank,Team,Num
     */
    public ArrayList<String> getConfStandings() {
        ArrayList<String> confStandings = new ArrayList<>();
        ArrayList<Team> confTeams = new ArrayList<>();
        for (Conference c : conferences) {
            confTeams.addAll(c.confTeams);
            Collections.sort(confTeams, new CompTeamConfWins());
            confStandings.add(" ," + c.confName + " Conference, , ");
            Team t;
            for (int i = 0; i < confTeams.size(); ++i) {
                t = confTeams.get(i);
                confStandings.add(t.getRankStr(i + 1) + "," + t.strConfStandings() + "," + t.strTeamRecord() + "," + t.getConfWins() + "-" + t.getConfLosses());
            }
            confTeams.clear();
            confStandings.add(" , , , ");
        }
        return confStandings;
    }

    public ArrayList<String> getTeamRankings() {
        ArrayList<Team> teams = teamList;
        ArrayList<String> rankings = new ArrayList<>();
        Team t;
        Collections.sort(teams, new CompTeamPoll());
        for (int i = 0; i < teams.size(); ++i) {
            t = teams.get(i);
            rankings.add(t.getRankStr(i + 1) + "," + t.getName() + "," + t.strTeamRecord() + "," + df2.format(t.getTeamPollScore()));
        }
        return rankings;
    }

    private ArrayList<PlayerQB> rankQB() {
        heisman = null;
        ArrayList<PlayerQB> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int qb = 0; qb < teamList.get(i).getTeamQBs().size(); ++qb) {
                heismanCandidates.add(teamList.get(i).getTeamQBs().get(qb));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerRB> rankRB() {
        heisman = null;
        ArrayList<PlayerRB> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int rb = 0; rb < teamList.get(i).getTeamRBs().size(); ++rb) {
                heismanCandidates.add(teamList.get(i).getTeamRBs().get(rb));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerWR> rankWR() {
        heisman = null;
        ArrayList<PlayerWR> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int wr = 0; wr < teamList.get(i).getTeamWRs().size(); ++wr) {
                heismanCandidates.add(teamList.get(i).getTeamWRs().get(wr));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerTE> rankTE() {
        heisman = null;
        ArrayList<PlayerTE> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int te = 0; te < teamList.get(i).getTeamTEs().size(); ++te) {
                heismanCandidates.add(teamList.get(i).getTeamTEs().get(te));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerOL> rankOL() {
        heisman = null;
        ArrayList<PlayerOL> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int te = 0; te < teamList.get(i).getTeamOLs().size(); ++te) {
                heismanCandidates.add(teamList.get(i).getTeamOLs().get(te));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerK> rankK() {
        heisman = null;
        ArrayList<PlayerK> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int te = 0; te < teamList.get(i).getTeamKs().size(); ++te) {
                heismanCandidates.add(teamList.get(i).getTeamKs().get(te));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerDL> rankDL() {
        heisman = null;
        ArrayList<PlayerDL> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int dl = 0; dl < teamList.get(i).getTeamDLs().size(); ++dl) {
                heismanCandidates.add(teamList.get(i).getTeamDLs().get(dl));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerLB> rankLB() {
        heisman = null;
        ArrayList<PlayerLB> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int lb = 0; lb < teamList.get(i).getTeamLBs().size(); ++lb) {
                heismanCandidates.add(teamList.get(i).getTeamLBs().get(lb));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerCB> rankCB() {
        heisman = null;
        ArrayList<PlayerCB> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int cb = 0; cb < teamList.get(i).getTeamCBs().size(); ++cb) {
                heismanCandidates.add(teamList.get(i).getTeamCBs().get(cb));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<PlayerS> rankS() {
        heisman = null;
        ArrayList<PlayerS> heismanCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int s = 0; s < teamList.get(i).getTeamSs().size(); ++s) {
                heismanCandidates.add(teamList.get(i).getTeamSs().get(s));
            }
        }
        Collections.sort(heismanCandidates, new CompPlayerHeisman());
        return heismanCandidates;
    }

    private ArrayList<HeadCoach> rankHC() {
        heisman = null;
        ArrayList<HeadCoach> coachCandidates = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getHeadCoach() != null) {
                coachCandidates.add(teamList.get(i).getHeadCoach());
            }
        }
        Collections.sort(coachCandidates, new CompCoachScore());
        return coachCandidates;
    }


    //PLAYER RANKINGS STUFF

    public ArrayList<String> getPlayerRankStr(int selection) {
        int rankNum = 0;
        ArrayList<PlayerQB> pQB = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamQBs().size(); ++p) {
                if (teamList.get(i).getTeamQBs().get(p).getPassAtt() >= (10 * currentWeek)) {
                    rankNum++;
                    pQB.add(teamList.get(i).getTeamQBs().get(p));
                }
            }
        }
        ArrayList<PlayerRB> pRB = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamRBs().size(); ++p) {
                pRB.add(teamList.get(i).getTeamRBs().get(p));
            }
        }
        ArrayList<PlayerWR> pWR = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamWRs().size(); ++p) {
                pWR.add(teamList.get(i).getTeamWRs().get(p));
            }
        }
        ArrayList<PlayerTE> pTE = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamTEs().size(); ++p) {
                pTE.add(teamList.get(i).getTeamTEs().get(p));
            }
        }
        ArrayList<PlayerOL> pOL = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamOLs().size(); ++p) {
                pOL.add(teamList.get(i).getTeamOLs().get(p));
            }
        }
        ArrayList<PlayerK> pK = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamKs().size(); ++p) {
                pK.add(teamList.get(i).getTeamKs().get(p));
            }
        }
        ArrayList<PlayerDL> pDL = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamDLs().size(); ++p) {
                pDL.add(teamList.get(i).getTeamDLs().get(p));
            }
        }
        ArrayList<PlayerLB> pLB = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamLBs().size(); ++p) {
                pLB.add(teamList.get(i).getTeamLBs().get(p));
            }
        }
        ArrayList<PlayerCB> pCB = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamCBs().size(); ++p) {
                pCB.add(teamList.get(i).getTeamCBs().get(p));
            }
        }
        ArrayList<PlayerS> pS = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamSs().size(); ++p) {
                pS.add(teamList.get(i).getTeamSs().get(p));
            }
        }
        ArrayList<HeadCoach> HC = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            if (teamList.get(i).getHeadCoach() != null) HC.add(teamList.get(i).getHeadCoach());
        }

        ArrayList<PlayerOffense> off = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamQBs().size(); ++p) {
                off.add(new PlayerOffense(teamList.get(i),
                        teamList.get(i).getTeamQBs().get(p).name,
                        "QB",
                        teamList.get(i).getTeamQBs().get(p).year,
                        teamList.get(i).getTeamQBs().get(p).getRushYards(),
                        teamList.get(i).getTeamQBs().get(p).getRushTDs(),
                        0, 0, 0,
                        teamList.get(i).getTeamQBs().get(p).getFumbles()));
            }
        }
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamRBs().size(); ++p) {
                off.add(new PlayerOffense(teamList.get(i),
                        teamList.get(i).getTeamRBs().get(p).name,
                        "RB",
                        teamList.get(i).getTeamRBs().get(p).year,
                        teamList.get(i).getTeamRBs().get(p).getRushYards(),
                        teamList.get(i).getTeamRBs().get(p).getRushTDs(),
                        teamList.get(i).getTeamRBs().get(p).getReceptions(),
                        teamList.get(i).getTeamRBs().get(p).getRecYards(),
                        teamList.get(i).getTeamRBs().get(p).getRecTDs(),
                        teamList.get(i).getTeamRBs().get(p).getFumbles()));
            }
        }
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamWRs().size(); ++p) {
                off.add(new PlayerOffense(teamList.get(i),
                        teamList.get(i).getTeamWRs().get(p).name,
                        "WR",
                        teamList.get(i).getTeamWRs().get(p).year,
                        0, 0,
                        teamList.get(i).getTeamWRs().get(p).getReceptions(),
                        teamList.get(i).getTeamWRs().get(p).getRecYards(),
                        teamList.get(i).getTeamWRs().get(p).getRecTDs(),
                        teamList.get(i).getTeamWRs().get(p).getFumbles()));
            }
        }
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamTEs().size(); ++p) {
                off.add(new PlayerOffense(teamList.get(i),
                        teamList.get(i).getTeamTEs().get(p).name,
                        "TE",
                        teamList.get(i).getTeamTEs().get(p).year,
                        0, 0,
                        teamList.get(i).getTeamTEs().get(p).getReceptions(),
                        teamList.get(i).getTeamTEs().get(p).getRecYards(),
                        teamList.get(i).getTeamTEs().get(p).getRecTDs(),
                        teamList.get(i).getTeamTEs().get(p).getFumbles()));
            }
        }

        ArrayList<PlayerDefense> def = new ArrayList<>();
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamDLs().size(); ++p) {
                def.add(new PlayerDefense(teamList.get(i),
                        teamList.get(i).getTeamDLs().get(p).name,
                        "DL",
                        teamList.get(i).getTeamDLs().get(p).year,
                        teamList.get(i).getTeamDLs().get(p).getTackles(),
                        teamList.get(i).getTeamDLs().get(p).getSacks(),
                        teamList.get(i).getTeamDLs().get(p).getFumblesRec(),
                        teamList.get(i).getTeamDLs().get(p).getInterceptions()));
            }
            for (int p = 0; p < teamList.get(i).getTeamLBs().size(); ++p) {
                def.add(new PlayerDefense(teamList.get(i),
                        teamList.get(i).getTeamLBs().get(p).name,
                        "LB",
                        teamList.get(i).getTeamLBs().get(p).year,
                        teamList.get(i).getTeamLBs().get(p).getTackles(),
                        teamList.get(i).getTeamLBs().get(p).getSacks(),
                        teamList.get(i).getTeamLBs().get(p).getFumblesRec(),
                        teamList.get(i).getTeamLBs().get(p).getInterceptions()));
            }
            for (int p = 0; p < teamList.get(i).getTeamCBs().size(); ++p) {
                def.add(new PlayerDefense(teamList.get(i),
                        teamList.get(i).getTeamCBs().get(p).name,
                        "CB",
                        teamList.get(i).getTeamCBs().get(p).year,
                        teamList.get(i).getTeamCBs().get(p).getTackles(),
                        teamList.get(i).getTeamCBs().get(p).getSacks(),
                        teamList.get(i).getTeamCBs().get(p).getFumblesRec(),
                        teamList.get(i).getTeamCBs().get(p).getInterceptions()));
            }
            for (int p = 0; p < teamList.get(i).getTeamSs().size(); ++p) {
                def.add(new PlayerDefense(teamList.get(i),
                        teamList.get(i).getTeamSs().get(p).name,
                        "S",
                        teamList.get(i).getTeamSs().get(p).year,
                        teamList.get(i).getTeamSs().get(p).getTackles(),
                        teamList.get(i).getTeamSs().get(p).getSacks(),
                        teamList.get(i).getTeamSs().get(p).getFumblesRec(),
                        teamList.get(i).getTeamSs().get(p).getInterceptions()));
            }
        }

        ArrayList<PlayerReturner> returner = new ArrayList<>();
        int retNum = 0;
        for (int i = 0; i < teamList.size(); ++i) {
            for (int p = 0; p < teamList.get(i).getTeamRBs().size(); ++p) {
                if (teamList.get(i).getTeamRBs().get(p).getKORets() > 0 || teamList.get(i).getTeamRBs().get(p).getPuntRets() > 0) {
                    returner.add(new PlayerReturner(teamList.get(i).getAbbr(),
                            teamList.get(i).getTeamRBs().get(p).name,
                            "RB",
                            teamList.get(i).getTeamRBs().get(p).getKORets(),
                            teamList.get(i).getTeamRBs().get(p).getKOYards(),
                            teamList.get(i).getTeamRBs().get(p).getKOTDs(),
                            teamList.get(i).getTeamRBs().get(p).getPuntRets(),
                            teamList.get(i).getTeamRBs().get(p).getPuntYards(),
                            teamList.get(i).getTeamRBs().get(p).getPuntTDs()));
                    retNum++;
                }
            }
            for (int p = 0; p < teamList.get(i).getTeamWRs().size(); ++p) {
                if (teamList.get(i).getTeamWRs().get(p).getKORets() > 0 || teamList.get(i).getTeamWRs().get(p).getPuntRets() > 0) {
                    returner.add(new PlayerReturner(teamList.get(i).getAbbr(),
                            teamList.get(i).getTeamWRs().get(p).name,
                            "WR",
                            teamList.get(i).getTeamWRs().get(p).getKORets(),
                            teamList.get(i).getTeamWRs().get(p).getKOYards(),
                            teamList.get(i).getTeamWRs().get(p).getKOTDs(),
                            teamList.get(i).getTeamWRs().get(p).getPuntRets(),
                            teamList.get(i).getTeamWRs().get(p).getPuntYards(),
                            teamList.get(i).getTeamWRs().get(p).getPuntTDs()));
                    retNum++;
                }
            }
            for (int p = 0; p < teamList.get(i).getTeamCBs().size(); ++p) {
                if (teamList.get(i).getTeamCBs().get(p).getKORets() > 0 || teamList.get(i).getTeamCBs().get(p).getPuntRets() > 0) {
                    returner.add(new PlayerReturner(teamList.get(i).getAbbr(),
                            teamList.get(i).getTeamCBs().get(p).name,
                            "CB",
                            teamList.get(i).getTeamCBs().get(p).getKORets(),
                            teamList.get(i).getTeamCBs().get(p).getKOYards(),
                            teamList.get(i).getTeamCBs().get(p).getKOTDs(),
                            teamList.get(i).getTeamCBs().get(p).getPuntRets(),
                            teamList.get(i).getTeamCBs().get(p).getPuntYards(),
                            teamList.get(i).getTeamCBs().get(p).getPuntTDs()));
                    retNum++;
                }
            }
        }


        ArrayList<String> rankings = new ArrayList<>();
        switch (selection) {
            case 0:
                Collections.sort(pQB, new CompPlayerPassRating());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + "," + pQB.get(i).team.getAbbr() + "," + df2.format(pQB.get(i).getPasserRating()));
                }
                break;
            case 1:
                Collections.sort(pQB, new CompPlayerPassYards());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + "," + pQB.get(i).team.getAbbr() + "," + pQB.get(i).getPassYards());
                }
                break;
            case 2:
                Collections.sort(pQB, new CompPlayerPassTDs());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + "," + pQB.get(i).team.getAbbr() + "," + pQB.get(i).getPassTD());
                }
                break;
            case 3:
                Collections.sort(pQB, new CompPlayerPassInts());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + "," + pQB.get(i).team.getAbbr() + "," + pQB.get(i).getPassInt());
                }
                break;
            case 4:
                Collections.sort(pQB, new CompPlayerPassPCT());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + "," + pQB.get(i).team.getAbbr() + "," + df2.format(pQB.get(i).getPassPCT()) + "%");
                }
                break;
            case 5:
                Collections.sort(off, new CompPlayerRushYards());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + off.get(i).position + " " + off.get(i).name + "," + off.get(i).team.getAbbr() + "," + off.get(i).rushYards);
                }
                break;
            case 6:
                Collections.sort(off, new CompPlayerRushTDs());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + off.get(i).position + " " + off.get(i).name + "," + off.get(i).team.getAbbr() + "," + off.get(i).rushTDs);
                }
                break;
            case 7:
                Collections.sort(off, new CompPlayerReceptions());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + off.get(i).position + " " + off.get(i).name + "," + off.get(i).team.getAbbr() + "," + off.get(i).receptions);
                }
                break;
            case 8:
                Collections.sort(off, new CompPlayerRecYards());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + off.get(i).position + " " + off.get(i).name + "," + off.get(i).team.getAbbr() + "," + off.get(i).receptionYards);
                }
                break;
            case 9:
                Collections.sort(off, new CompPlayerRecTDs());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + off.get(i).position + " " + off.get(i).name + "," + off.get(i).team.getAbbr() + "," + off.get(i).receptionTDs);
                }
                break;
            case 10:
                Collections.sort(def, new CompPlayerTackles());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + def.get(i).position + " " + def.get(i).name + "," + def.get(i).team.getAbbr() + "," + def.get(i).tackles);
                }
                break;
            case 11:
                Collections.sort(def, new CompPlayerSacks());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + def.get(i).position + " " + def.get(i).name + "," + def.get(i).team.getAbbr() + "," + def.get(i).sacks);
                }
                break;
            case 12:
                Collections.sort(def, new CompPlayerFumblesRec());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + def.get(i).position + " " + def.get(i).name + "," + def.get(i).team.getAbbr() + "," + def.get(i).fumbles);
                }
                break;
            case 13:
                Collections.sort(def, new CompPlayerInterceptions());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + def.get(i).position + " " + def.get(i).name + "," + def.get(i).team.getAbbr() + "," + def.get(i).interceptions);
                }
                break;
            case 14:
                Collections.sort(pK, new CompPlayerFGMade());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pK.get(i).name + "," + pK.get(i).team.getAbbr() + "," + pK.get(i).getFGMade());
                }
                break;
            case 15:
                Collections.sort(pK, new CompPlayerFGpct());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pK.get(i).name + "," + pK.get(i).team.getAbbr() + "," + pK.get(i).getFGpct() + "%");
                }
                break;
            case 16:
                Collections.sort(returner, new CompKickRetYards());
                for (int i = 0; i < retNum; ++i) {
                    rankings.add((i + 1) + ". ," + returner.get(i).name + "," + returner.get(i).team + "," + returner.get(i).kYards);
                }
                break;
            case 17:
                Collections.sort(returner, new CompKickRetTD());
                for (int i = 0; i < retNum; ++i) {
                    rankings.add((i + 1) + ". ," + returner.get(i).name + "," + returner.get(i).team + "," + returner.get(i).kTD);
                }
                break;
            case 18:
                Collections.sort(returner, new CompPuntRetYards());
                for (int i = 0; i < retNum; ++i) {
                    rankings.add((i + 1) + ". ," + returner.get(i).name + "," + returner.get(i).team + "," + returner.get(i).pYards);
                }
                break;
            case 19:
                Collections.sort(returner, new CompPuntRetTDs());
                for (int i = 0; i < retNum; ++i) {
                    rankings.add((i + 1) + ". ," + returner.get(i).name + "," + returner.get(i).team + "," + returner.get(i).pTD);
                }
                break;
            case 20:
                Collections.sort(HC, new CompCoachOvr());
                for (int i = 0; i < HC.size(); ++i) {
                    rankings.add((i + 1) + ". ," + HC.get(i).name + "," + HC.get(i).team.getAbbr() + "," + HC.get(i).getStaffOverall(HC.get(i).overallWt));
                }
                break;
            case 21:
                Collections.sort(HC, new CompCoachScore());
                for (int i = 0; i < HC.size(); ++i) {
                    rankings.add((i + 1) + ". ," + HC.get(i).name + "," + HC.get(i).team.getAbbr() + "," + HC.get(i).getCoachScore());
                }
                break;
        }
        return rankings;
    }

    public ArrayList<String> getAwardsWatch(int selection) {
        int rankNum = 40;
        ArrayList<String> rankings = new ArrayList<>();
        switch (selection) {
            case 0:
                ArrayList<HeadCoach> HC = rankHC();
                Collections.sort(HC, new CompCoachScore());
                for (int i = 0; i < HC.size(); ++i) {
                    rankings.add((i + 1) + ". ," + HC.get(i).name + " (" + HC.get(i).team.getAbbr() + ")," + HC.get(i).getCoachScore());
                }
                break;
            case 1:
                ArrayList<PlayerQB> pQB = rankQB();
                Collections.sort(pQB, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pQB.get(i).name + " (" + pQB.get(i).team.getAbbr() + ")," + pQB.get(i).getHeismanScore());
                }
                break;
            case 2:
                ArrayList<PlayerRB> pRB = rankRB();
                Collections.sort(pRB, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pRB.get(i).name + " (" + pRB.get(i).team.getAbbr() + ")," + pRB.get(i).getHeismanScore());
                }
                break;
            case 3:
                ArrayList<PlayerWR> pWR = rankWR();
                Collections.sort(pWR, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pWR.get(i).name + " (" + pWR.get(i).team.getAbbr() + ")," + pWR.get(i).getHeismanScore());
                }
                break;
            case 4:
                ArrayList<PlayerTE> pTE = rankTE();
                Collections.sort(pTE, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pTE.get(i).name + " (" + pTE.get(i).team.getAbbr() + ")," + pTE.get(i).getHeismanScore());
                }
                break;
            case 5:
                ArrayList<PlayerOL> pOL = rankOL();
                Collections.sort(pOL, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pOL.get(i).name + " (" + pOL.get(i).team.getAbbr() + ")," + pOL.get(i).getHeismanScore());
                }
                break;
            case 6:
                ArrayList<PlayerK> pK = rankK();
                Collections.sort(pK, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pK.get(i).name + " (" + pK.get(i).team.getAbbr() + ")," + pK.get(i).getHeismanScore());
                }
                break;
            case 7:
                ArrayList<PlayerDL> pDL = rankDL();
                Collections.sort(pDL, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pDL.get(i).name + " (" + pDL.get(i).team.getAbbr() + ")," + pDL.get(i).getHeismanScore());
                }
                break;
            case 8:
                ArrayList<PlayerLB> pLB = rankLB();
                Collections.sort(pLB, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pLB.get(i).name + " (" + pLB.get(i).team.getAbbr() + ")," + pLB.get(i).getHeismanScore());
                }
                break;
            case 9:
                ArrayList<PlayerCB> pCB = rankCB();
                Collections.sort(pCB, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pCB.get(i).name + " (" + pCB.get(i).team.getAbbr() + ")," + pCB.get(i).getHeismanScore());
                }
                break;
            case 10:
                ArrayList<PlayerS> pS = rankS();
                Collections.sort(pS, new CompPlayerHeisman());
                for (int i = 0; i < rankNum; ++i) {
                    rankings.add((i + 1) + ". ," + pS.get(i).name + " (" + pS.get(i).team.getAbbr() + ")," + pS.get(i).getHeismanScore());
                }
                break;
        }
        return rankings;
    }


    /**
     * See if team name is in use, or has illegal characters.
     *
     * @param name team name
     * @return true if valid, false if not
     */
    public boolean isNameValid(String name) {
        if (name.length() == 0) {
            return false;
        }
        return !(name.contains(",") || name.contains(">") || name.contains("%") || name.contains("\\"));
    }

    public boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            if(Integer.parseInt(input) > 4 || Integer.parseInt(input) < 0 ) return false;
            else return true;
        }
        catch( Exception e ) {
            return false;
        }
    }

    /**
     * See if team abbr is in use, or has illegal characters, or is not 3 characters
     *
     * @param abbr new abbr
     * @return true if valid, false if not
     */
    public boolean isAbbrValid(String abbr) {
        if (abbr.length() > 4 || abbr.length() == 0) {
            // Only 4 letter abbr allowed
            return false;
        }

        return !(abbr.contains(",") || abbr.contains(">") || abbr.contains("%") || abbr.contains("\\") || abbr.contains(" "));

    }

    public String getRankStr(int num) {
        if (num == 11) {
            return "11th";
        } else if (num == 12) {
            return "12th";
        } else if (num == 13) {
            return "13th";
        } else if (num % 10 == 1) {
            return num + "st";
        } else if (num % 10 == 2) {
            return num + "nd";
        } else if (num % 10 == 3) {
            return num + "rd";
        } else {
            return num + "th";
        }
    }

    public int getAvgCoachTal() {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratTalent;
        }
        return avg / (teamList.size() - 1);
    }

    public int getAvgCoachDis() {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratDiscipline;
        }
        return avg / (teamList.size() - 1);
    }

    public int getAvgCoachOff() {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratOff;
        }
        return avg / (teamList.size() - 1);
    }

    public int getAvgCoachDef() {
        int avg = 0;
        for (int i = 0; i < teamList.size(); i++) {
            if (teamList.get(i) != userTeam && teamList.get(i).getHeadCoach() != null)
                avg += teamList.get(i).getHeadCoach().ratDef;
        }
        return avg / (teamList.size() - 1);
    }

    public ArrayList<String> getUserNames() {
        ArrayList<String> names = new ArrayList<>();

        for(Staff c : coachFreeAgents) {
            if(c.user) names.add(c.name + " [R]");
        }

        return names;
    }


    public String getFreeAgentCoachSave() {
        for (Staff h : coachFreeAgents) {
            if(!h.retired) h.age++;
        }
        //Adding to the Available HeadCoach List Pool
        for (Staff h : coachList) {
            if(!coachFreeAgents.contains((h))) coachFreeAgents.add(h);
        }
        Collections.sort(coachFreeAgents, new CompCoachOvr());
        StringBuilder sb = new StringBuilder();

        for (Staff h : coachFreeAgents) {
            if (h.age < 63) {
                h.ratOff += (int) (Math.random()*3);
                h.ratDef += (int) (Math.random()*3);
                h.ratTalent += (int) (Math.random()*3);
                h.ratDiscipline += (int) (Math.random()*3);
            } else {
                h.retired = true;
            }

            sb.append(h.saveStaffData() + "\n");
            for (String s : h.history) {
                sb.append(s + "\n");
            }
            sb.append("END_FREE_AGENT\n");
        }

        return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Prepares the league for the next season without requiring a save/load
     * cycle.  This is the desktop equivalent of the Android save-then-reload
     * pattern: it resets all current-season state, auto-fills any remaining
     * roster spots via CPU recruiting, and re-runs the full schedule builder.
     *
     * <p>Call this after the offseason has fully completed (i.e. after
     * {@link #recruitPlayers()} has been called and
     * {@code currentWeek >= regSeasonWeeks + 13}).
     */
    public void startNextSeason() {
        // Reset league-level per-season state
        heismanDecided = false;
        hasScheduledBowls = false;
        heisman = null;
        defPOTY = null;
        freshman = null;
        coachWinner = null;
        heismanCandidates = new ArrayList<>();
        defPOTYCandidates = new ArrayList<>();
        freshmanCandidates = new ArrayList<>();
        allAmericans = new ArrayList<>();
        allAmericans2 = new ArrayList<>();
        allFreshman = new ArrayList<>();
        playoffTeams = new ArrayList<>();
        cfpGames = new Game[15];

        // Clear transfer pools (they were consumed during the offseason)
        transferQBs = new ArrayList<>();
        transferRBs = new ArrayList<>();
        transferWRs = new ArrayList<>();
        transferTEs = new ArrayList<>();
        transferKs = new ArrayList<>();
        transferOLs = new ArrayList<>();
        transferDLs = new ArrayList<>();
        transferLBs = new ArrayList<>();
        transferCBs = new ArrayList<>();
        transferSs = new ArrayList<>();
        userTransferList = new ArrayList<>();
        freshmen = new ArrayList<>();
        redshirts = new ArrayList<>();

        // Reset per-team season state and conference schedule bookkeeping
        for (Conference c : conferences) {
            c.oocWeeks.clear();
        }
        for (Team t : teamList) {
            t.resetSeasonStats();
        }

        currentWeek = 0;
        newsHeadlines.clear();

        // Rebuild the schedule for the new season
        setupSeason();
    }

    /**
     * Save League in a file.
     *
     * @param saveFile file to be overwritten
     * @return true if successful
     */
    public boolean saveLeague(File saveFile) {
        File tmp = new File(saveFile.getAbsolutePath() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            SaveManager.save(this.toRecord(), fos);
        } catch (IOException e) {
            PlatformLog.e("League", "Failed to write temp save file " + tmp.getAbsolutePath(), e);
            if (!tmp.delete()) {
                PlatformLog.w("League", "Could not delete temp save file " + tmp.getAbsolutePath());
            }
            return false;
        }
        if (!tmp.renameTo(saveFile)) {
            PlatformLog.e("League", "Failed to rename temp save file to " + saveFile.getAbsolutePath());
            if (!tmp.delete()) {
                PlatformLog.w("League", "Could not delete temp save file " + tmp.getAbsolutePath());
            }
            return false;
        }
        return true;
    }




    /* Rivals.com
    30 5-star
    380 4-star
    1328 3-star
    1859 2-star
     */

    public void generateRecruitingPool () {
        
        final int five = 3;
        final int four = 38;
        final int three = 133;
        final int two = 186;
        final int one = 250;
        
        ArrayList<PlayerQB> qbRecruits = new ArrayList<>();
        ArrayList<PlayerRB> rbRecruits = new ArrayList<>();
        ArrayList<PlayerWR> wrRecruits = new ArrayList<>();
        ArrayList<PlayerTE> teRecruits = new ArrayList<>();
        ArrayList<PlayerOL> olRecruits = new ArrayList<>();
        ArrayList<PlayerK> kRecruits = new ArrayList<>();
        ArrayList<PlayerDL> dlRecruits = new ArrayList<>();
        ArrayList<PlayerLB> lbRecruits = new ArrayList<>();
        ArrayList<PlayerCB> cbRecruits = new ArrayList<>();
        ArrayList<PlayerS> sRecruits = new ArrayList<>();
        
        for(int i = 0; i < five; i++) {
            qbRecruits.add(new PlayerQB(getRandName(), 1, 9, null));
            rbRecruits.add(new PlayerRB(getRandName(), 1, 9, null));
            wrRecruits.add(new PlayerWR(getRandName(), 1, 9, null));
            teRecruits.add(new PlayerTE(getRandName(), 1, 9 , null));
            olRecruits.add(new PlayerOL(getRandName(), 1, 9 , null));
            kRecruits.add(new PlayerK(getRandName(), 1, 9 , null));
            dlRecruits.add(new PlayerDL(getRandName(), 1 , 9 , null));
            lbRecruits.add(new PlayerLB(getRandName(), 1, 9 , null));
            cbRecruits.add(new PlayerCB(getRandName(), 1, 9 , null));
            sRecruits.add(new PlayerS(getRandName(), 1, 9 , null));
        }

        for(int i = 0; i < four; i++) {
            qbRecruits.add(new PlayerQB(getRandName(), 1, 8, null));
            rbRecruits.add(new PlayerRB(getRandName(), 1, 8, null));
            wrRecruits.add(new PlayerWR(getRandName(), 1, 8, null));
            teRecruits.add(new PlayerTE(getRandName(), 1, 8 , null));
            olRecruits.add(new PlayerOL(getRandName(), 1, 8 , null));
            kRecruits.add(new PlayerK(getRandName(), 1, 8 , null));
            dlRecruits.add(new PlayerDL(getRandName(), 1 , 8 , null));
            lbRecruits.add(new PlayerLB(getRandName(), 1, 8 , null));
            cbRecruits.add(new PlayerCB(getRandName(), 1, 8 , null));
            sRecruits.add(new PlayerS(getRandName(), 1, 8 , null));
        }

        for(int i = 0; i < three; i++) {
            qbRecruits.add(new PlayerQB(getRandName(), 1, 7, null));
            rbRecruits.add(new PlayerRB(getRandName(), 1, 7, null));
            wrRecruits.add(new PlayerWR(getRandName(), 1, 7, null));
            teRecruits.add(new PlayerTE(getRandName(), 1, 7 , null));
            olRecruits.add(new PlayerOL(getRandName(), 1, 7 , null));
            kRecruits.add(new PlayerK(getRandName(), 1, 7 , null));
            dlRecruits.add(new PlayerDL(getRandName(), 1 , 7 , null));
            lbRecruits.add(new PlayerLB(getRandName(), 1, 7 , null));
            cbRecruits.add(new PlayerCB(getRandName(), 1, 7 , null));
            sRecruits.add(new PlayerS(getRandName(), 1, 7 , null));
        }

        for(int i = 0; i < two; i++) {
            qbRecruits.add(new PlayerQB(getRandName(), 1, 7, null));
            rbRecruits.add(new PlayerRB(getRandName(), 1, 6, null));
            wrRecruits.add(new PlayerWR(getRandName(), 1, 6, null));
            teRecruits.add(new PlayerTE(getRandName(), 1, 6 , null));
            olRecruits.add(new PlayerOL(getRandName(), 1, 6 , null));
            kRecruits.add(new PlayerK(getRandName(), 1, 6 , null));
            dlRecruits.add(new PlayerDL(getRandName(), 1 , 6 , null));
            lbRecruits.add(new PlayerLB(getRandName(), 1, 6 , null));
            cbRecruits.add(new PlayerCB(getRandName(), 1, 6 , null));
            sRecruits.add(new PlayerS(getRandName(), 1, 6 , null));
        }

        for(int i = 0; i < one; i++) {
            qbRecruits.add(new PlayerQB(getRandName(), 1, 5, null));
            rbRecruits.add(new PlayerRB(getRandName(), 1, 5, null));
            wrRecruits.add(new PlayerWR(getRandName(), 1, 5, null));
            teRecruits.add(new PlayerTE(getRandName(), 1, 5 , null));
            olRecruits.add(new PlayerOL(getRandName(), 1, 5 , null));
            kRecruits.add(new PlayerK(getRandName(), 1, 5 , null));
            dlRecruits.add(new PlayerDL(getRandName(), 1 , 5 , null));
            lbRecruits.add(new PlayerLB(getRandName(), 1, 5 , null));
            cbRecruits.add(new PlayerCB(getRandName(), 1, 5 , null));
            sRecruits.add(new PlayerS(getRandName(), 1, 5 , null));
        }
    }

    public String getFreeAgentCoachList() {

        StringBuilder sb = new StringBuilder();

        sb.append("Free Agents:\n\n");
        int i = 1;
        for (Staff c : coachFreeAgents) {
            sb.append(i + ". " + c.name + "\n");
            i++;
        }

        sb.append("\n\nRecently Fired:\n\n");
        i = 1;
        for (Staff c : coachList) {
            sb.append(i + ". " + c.name + "\n");
            i++;
        }
        sb.append("\n\nReplicated Names:\n\n");
        ArrayList<String> names = new ArrayList<>();
        for(Team t : teamList) {
            if(t.getHeadCoach() != null && names.contains(t.getHeadCoach().name)) sb.append(t.getName() + " " + t.getHeadCoach().position + " " + t.getHeadCoach().name +"\n");
            if(t.getOC() != null && names.contains(t.getOC().name)) sb.append(t.getName() + " " + t.getOC().position + " " + t.getOC().name +"\n");
            if(t.getDC() != null && names.contains(t.getDC().name)) sb.append(t.getName() + " " + t.getDC().position + " " + t.getDC().name +"\n");
            if(t.getHeadCoach() != null) names.add(t.getHeadCoach().name);
            if(t.getOC() != null) names.add(t.getOC().name);
            if(t.getDC() != null) names.add(t.getDC().name);
        }
        for (Staff c : coachFreeAgents) {
            if(names.contains(c.name)) sb.append("Free Agents " + c.position + " " + c.name +"\n");
            names.add(c.name);
        }
        for (Staff c : coachList) {
            if(names.contains(c.name)) sb.append("CoachLists " + c.position + " " + c.name +"\n");
            names.add(c.name);
        }
        return sb.toString();
    }

    // =========================================================================
    // Transfer Pool Accessors
    // =========================================================================

    /**
     * Get an unmodifiable view of the QB transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerQB> getTransferQBs() {
        return java.util.Collections.unmodifiableList(transferQBs);
    }

    /**
     * Get an unmodifiable view of the RB transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerRB> getTransferRBs() {
        return java.util.Collections.unmodifiableList(transferRBs);
    }

    /**
     * Get an unmodifiable view of the WR transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerWR> getTransferWRs() {
        return java.util.Collections.unmodifiableList(transferWRs);
    }

    /**
     * Get an unmodifiable view of the TE transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerTE> getTransferTEs() {
        return java.util.Collections.unmodifiableList(transferTEs);
    }

    /**
     * Get an unmodifiable view of the K transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerK> getTransferKs() {
        return java.util.Collections.unmodifiableList(transferKs);
    }

    /**
     * Get an unmodifiable view of the OL transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerOL> getTransferOLs() {
        return java.util.Collections.unmodifiableList(transferOLs);
    }

    /**
     * Get an unmodifiable view of the DL transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerDL> getTransferDLs() {
        return java.util.Collections.unmodifiableList(transferDLs);
    }

    /**
     * Get an unmodifiable view of the LB transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerLB> getTransferLBs() {
        return java.util.Collections.unmodifiableList(transferLBs);
    }

    /**
     * Get an unmodifiable view of the CB transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerCB> getTransferCBs() {
        return java.util.Collections.unmodifiableList(transferCBs);
    }

    /**
     * Get an unmodifiable view of the S transfer pool.
     * Use {@link #addToTransferPool(Player)} to add players.
     */
    public java.util.List<PlayerS> getTransferSs() {
        return java.util.Collections.unmodifiableList(transferSs);
    }

    /**
     * Add a player to the appropriate transfer pool based on their position.
     * @param player the player to add to the transfer pool
     */
    public void addToTransferPool(Player player) {
        if (player instanceof PlayerQB) {
            transferQBs.add((PlayerQB) player);
        } else if (player instanceof PlayerRB) {
            transferRBs.add((PlayerRB) player);
        } else if (player instanceof PlayerWR) {
            transferWRs.add((PlayerWR) player);
        } else if (player instanceof PlayerTE) {
            transferTEs.add((PlayerTE) player);
        } else if (player instanceof PlayerK) {
            transferKs.add((PlayerK) player);
        } else if (player instanceof PlayerOL) {
            transferOLs.add((PlayerOL) player);
        } else if (player instanceof PlayerDL) {
            transferDLs.add((PlayerDL) player);
        } else if (player instanceof PlayerLB) {
            transferLBs.add((PlayerLB) player);
        } else if (player instanceof PlayerCB) {
            transferCBs.add((PlayerCB) player);
        } else if (player instanceof PlayerS) {
            transferSs.add((PlayerS) player);
        }
    }

    // =========================================================================
    // League Collections Accessors (Phase 2)
    // =========================================================================

    /**
     * Get an unmodifiable view of the league history.
     */
    public java.util.List<String[]> getLeagueHistory() {
        return java.util.Collections.unmodifiableList(leagueHistory);
    }

    /**
     * Get an unmodifiable view of the Hall of Fame players.
     * Use {@link #addToHallOfFame(PlayerRecord)} to add players.
     */
    public java.util.List<PlayerRecord> getLeagueHoF() {
        return java.util.Collections.unmodifiableList(leagueHoF);
    }

    /**
     * Add a player to the Hall of Fame.
     */
    public void addToHallOfFame(PlayerRecord record) {
        if (record != null) {
            leagueHoF.add(record);
        }
    }

    /**
     * Get an unmodifiable view of conferences.
     */
    public java.util.List<Conference> getConferences() {
        return java.util.Collections.unmodifiableList(conferences);
    }

    /**
     * Add a conference to the league.
     */
    public void addConference(Conference conference) {
        if (conference != null) {
            conferences.add(conference);
        }
    }

    /**
     * Get an unmodifiable view of all teams.
     */
    public java.util.List<Team> getTeamList() {
        return java.util.Collections.unmodifiableList(teamList);
    }

    /**
     * Add a team to the league.
     */
    public void addTeam(Team team) {
        if (team != null) {
            teamList.add(team);
        }
    }

    /**
     * Get an unmodifiable view of the coach list.
     */
    public java.util.List<Staff> getCoachList() {
        return java.util.Collections.unmodifiableList(coachList);
    }

    /**
     * Get an unmodifiable view of star coaches.
     */
    public java.util.List<Staff> getCoachStarList() {
        return coachStarList;
    }

    /**
     * Get an unmodifiable view of free agent coaches.
     * Use {@link #addCoachFreeAgent(Staff)} and {@link #removeCoachFreeAgent(Staff)} to modify.
     */
    public java.util.List<Staff> getCoachFreeAgents() {
        return coachFreeAgents;
    }

    /**
     * Add a coach to the unemployed coach list.
     */
    public void addCoach(Staff coach) {
        if (coach != null) {
            coachList.add(coach);
        }
    }

    /**
     * Add a coach to free agency.
     */
    public void addCoachFreeAgent(Staff coach) {
        if (coach != null) {
            coachFreeAgents.add(coach);
        }
    }

    /**
     * Remove a coach from free agency.
     */
    public void removeCoachFreeAgent(Staff coach) {
        coachFreeAgents.remove(coach);
    }

    /**
     * Get an unmodifiable view of the coach database.
     */
    public java.util.List<Staff> getCoachDatabase() {
        return java.util.Collections.unmodifiableList(coachDatabase);
    }

    /**
     * Get an unmodifiable view of news stories for the current week.
     * Use {@link #addNewsStory(int, String)} to add stories.
     */
    public java.util.List<java.util.List<String>> getNewsStories() {
        java.util.List<java.util.List<String>> unmodifiable = new java.util.ArrayList<>();
        for (java.util.List<String> week : newsStories) {
            unmodifiable.add(java.util.Collections.unmodifiableList(week));
        }
        return java.util.Collections.unmodifiableList(unmodifiable);
    }

    /**
     * Get the news stories for a specific week.
     */
    public java.util.List<String> getNewsStoriesForWeek(int week) {
        if (week >= 0 && week < newsStories.size()) {
            return java.util.Collections.unmodifiableList(newsStories.get(week));
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Add a news story for a specific week.
     */
    public void addNewsStory(int week, String story) {
        if (week >= 0 && week < newsStories.size() && story != null) {
            newsStories.get(week).add(story);
        }
    }

    /**
     * Get an unmodifiable view of news headlines.
     * Use {@link #addNewsHeadline(String)} and {@link #clearNewsHeadlines()} to modify.
     */
    public java.util.List<String> getNewsHeadlines() {
        return java.util.Collections.unmodifiableList(newsHeadlines);
    }

    /**
     * Add a news headline.
     */
    public void addNewsHeadline(String headline) {
        if (headline != null) {
            newsHeadlines.add(headline);
        }
    }

    /**
     * Clear all news headlines.
     */
    public void clearNewsHeadlines() {
        newsHeadlines.clear();
    }

    /**
     * Get an unmodifiable view of weekly scores.
     */
    public java.util.List<java.util.List<String>> getWeeklyScores() {
        java.util.List<java.util.List<String>> unmodifiable = new java.util.ArrayList<>();
        for (java.util.List<String> week : weeklyScores) {
            unmodifiable.add(java.util.Collections.unmodifiableList(week));
        }
        return java.util.Collections.unmodifiableList(unmodifiable);
    }

    /**
     * Get the weekly scores for a specific week.
     */
    public java.util.List<String> getWeeklyScoresForWeek(int week) {
        if (week >= 0 && week < weeklyScores.size()) {
            return java.util.Collections.unmodifiableList(weeklyScores.get(week));
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Add a score entry for a specific week.
     */
    public void addWeeklyScore(int week, String score) {
        if (week >= 0 && week < weeklyScores.size() && score != null) {
            weeklyScores.get(week).add(score);
        }
    }

    /**
     * Get an unmodifiable view of the user's transfer list.
     */
    public java.util.List<Player> getUserTransferList() {
        return java.util.Collections.unmodifiableList(userTransferList);
    }

    /**
     * Get an unmodifiable view of freshmen players.
     */
    public java.util.List<Player> getFreshmen() {
        return freshmen;
    }

    /**
     * Add a player to the season's freshman tracking list.
     */
    public void addFreshman(Player p) {
        if (p != null) {
            freshmen.add(p);
        }
    }

    /**
     * Get an unmodifiable view of redshirted players.
     */
    public java.util.List<Player> getRedshirts() {
        return redshirts;
    }

    /**
     * Add a player to the season's redshirt tracking list.
     */
    public void addRedshirt(Player p) {
        if (p != null) {
            redshirts.add(p);
        }
    }

    /**
     * Get an unmodifiable view of TV news entries.
     */
    public java.util.List<String> getNewsTV() {
        return newsTV;
    }

    /**
     * Get an unmodifiable view of playoff teams.
     */
    public java.util.List<Team> getPlayoffTeams() {
        return java.util.Collections.unmodifiableList(playoffTeams);
    }

    /**
     * Get an unmodifiable view of FCS teams list.
     */
    public java.util.List<String> getTeamsFCSList() {
        return java.util.Collections.unmodifiableList(teamsFCSList);
    }


    /**
     * Get the Heisman winner summary string.
     */
    public String getHeismanWinnerStrFull() {
        return heismanWinnerStrFull != null ? heismanWinnerStrFull : "No winner decided yet.";
    }
}
