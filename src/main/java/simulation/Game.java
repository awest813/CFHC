package simulation;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import comparator.CompGamePlayerPicker;
import comparator.CompPlayerPosDepth;
import comparator.CompPlayerPosition;
import comparator.CompPlayerReturners;
import comparator.CompPlayerSTSpeed;
import positions.Archetypes;
import positions.Player;
import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerK;
import positions.PlayerLB;
import positions.PlayerOL;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerReturner;
import positions.PlayerS;
import positions.PlayerST;
import positions.PlayerTE;
import positions.PlayerWR;
import staff.HeadCoach;
import staff.OC;
import staff.DC;


public class Game implements Serializable {

    private final GameNewsService newsService;
    private final GameStatRecorder statRecorder;
    private final DecimalFormat df2 = new DecimalFormat("#.##");
    private static final DecimalFormat DF2_DOT = new DecimalFormat(".##");
    private GameBoxScore boxScore;

    private GameBoxScore getBoxScore() {
        if (boxScore == null) boxScore = new GameBoxScore(this);
        return boxScore;
    }

    public final Team homeTeam;
    public final Team awayTeam;

    public boolean hasPlayed;
    private boolean QT1;
    private boolean QT2;
    private boolean QT3;

    public String gameName;
    public int week;

    /** Canonical name for bye placeholders on a team's schedule. */
    public static final String BYE_WEEK_NAME = "BYE WEEK";

    /** True when this schedule slot is a bye (no real opponent). */
    public boolean isByeWeek() {
        return BYE_WEEK_NAME.equals(gameName);
    }

    /**
     * Conference / division / OOC / bye slots — everything that is not a bowl or playoff game.
     * Used when counting postseason appearances for prestige.
     */
    public boolean isRegularSeasonSlot() {
        return "Conference".equals(gameName)
                || "Division".equals(gameName)
                || "OOC".equals(gameName)
                || isByeWeek();
    }

    public int homeScore;
    public final int[] homeQScore;
    public int awayScore;
    public final int[] awayQScore;
    int homeYards;
    int awayYards;

    public int numOT;
    int homeTOs;
    int awayTOs;

    PlayerReturner homeKickReturner;
    PlayerReturner awayKickReturner;
    private ArrayList<PlayerST> teamST;
    private PlayerST playerST;

    ArrayList<String> homePassingStats;
    ArrayList<String> homeRushingStats;
    ArrayList<String> homeReceivingStats;
    ArrayList<String> homeDefenseStats;
    ArrayList<String> homeKickingStats;
    ArrayList<String> awayPassingStats;
    ArrayList<String> awayRushingStats;
    ArrayList<String> awayReceivingStats;
    ArrayList<String> awayDefenseStats;
    ArrayList<String> awayKickingStats;

    ArrayList<PlayerOL> teamOLs;
    private ArrayList<PlayerDL> teamDLs;

    int homePassYards;
    int awayPassYards;
    int homeRushYards;
    int awayRushYards;

    final StringBuilder gameEventLog = new StringBuilder(4096);
    String tdInfo;

    //private variables used when simming games
    int gameTime;
    boolean gamePoss;
    int gameYardLine;
    private int gameYardLinePlay;
    int gameDown;
    int gameYardsNeed;
    boolean playingOT;
    private boolean bottomOT;

    final int timePerPlay = 18;
    private final int intValue = 135; //higher less ints
    private final int sackValue = 200; //higher less sacks
    private final int escapeValue = 150;
    private final int compValue = 250; //higher more completions
    private final int fatigueDropSuper = 13;
    private final int fatigueDropHigh = 9;
    private final int fatigueDropMed = 6;
    private final int fatigueDropLow = 3;
    private final int fatigueGain = 3;
    private int snapCount = 0;
    private final int touchback = 25;

    double hkReturnAvg = 0, akReturnAvg = 0, hpReturnAvg = 0, apReturnAvg = 0;

    private int returnYards;

    private int homeOffense, homeDefense, awayOffense, awayDefense;
    private int tacticalCoach = 83;

    //budget sales
    private int tickets = 10;
    private int merch = 2;
    private int winSales = 150;

    //Injury
    private double injuryFreq = 0.95;
    private int injuryChance = 150;

    //GAME SETUP

    public Game(Team home, Team away, String name) {
        homeTeam = home;
        awayTeam = away;

        gameName = name;

        homeScore = 0;
        homeQScore = new int[10];
        awayScore = 0;
        awayQScore = new int[10];
        numOT = 0;

        homeTOs = 0;
        awayTOs = 0;

        hasPlayed = false;

        if (gameName == null) {
            gameName = "Game";
        }

        newsService = new GameNewsService(this);
        statRecorder = new GameStatRecorder(this);
    }

    public Game(Team home, Team away) {
        homeTeam = home;
        awayTeam = away;
        numOT = 0;
        homeTOs = 0;
        awayTOs = 0;

        gameName = "";

        homeScore = 0;
        homeQScore = new int[10];
        awayScore = 0;
        awayQScore = new int[10];

        hasPlayed = false;

        newsService = new GameNewsService(this);
        statRecorder = new GameStatRecorder(this);
    }

    public Game(Team home, Team away, boolean hp,  int hscore, int ascore) {
        homeTeam = home;
        awayTeam = away;
        gameName = "";
        homeScore = hscore;
        awayScore = ascore;
        hasPlayed = hp;
        homeQScore = new int[10];
        awayQScore = new int[10];
        newsService = new GameNewsService(this);
        statRecorder = new GameStatRecorder(this);
    }


    private int getHFadv() {
        //home field advantage
        int footIQadv = (int) ((homeTeam.getCompositeFootIQ() - awayTeam.getCompositeFootIQ()) / 5);
        if (footIQadv > 3) footIQadv = 3;
        if (footIQadv < -3) footIQadv = -3;
        if (gameName.contains("Bowl") || gameName.contains("NC") || gameName.contains("SF"))
            return 0;
        if (gamePoss) {
            return 3 + footIQadv;
        } else {
            return -footIQadv;
        }
    }

    private int getCoachAdv() {
        coachingStrategyAdjustments();
        HeadCoach homeHC = homeTeam.getHeadCoach();
        HeadCoach awayHC = awayTeam.getHeadCoach();
        OC homeOC = homeTeam.getOC();
        DC homeDC = homeTeam.getDC();
        OC awayOC = awayTeam.getOC();
        DC awayDC = awayTeam.getDC();
        int adv = 0;

        if (gamePoss) {

            int HTstrat = 0;
            if (homeOC != null && homeHC != null) {
                if(homeTeam.getPlaybookOffNum() != homeOC.offStrat && homeTeam.getPlaybookOffNum() != homeHC.offStrat) HTstrat = -2;
                else if(homeTeam.getPlaybookOffNum() == homeOC.offStrat && homeTeam.getPlaybookOffNum() != homeHC.offStrat) HTstrat = 0;
                else if(homeTeam.getPlaybookOffNum() != homeOC.offStrat && homeTeam.getPlaybookOffNum() == homeHC.offStrat) HTstrat = -1;
            }

            int awayDCdef = awayDC != null ? awayDC.defStrat : 0;
            int ATstrat = Math.abs(awayTeam.getPlaybookDefNum() - awayDCdef);
            if (awayDC != null && awayHC != null) {
                if(awayTeam.getPlaybookDefNum() != awayDC.defStrat && awayTeam.getPlaybookDefNum() != awayHC.defStrat) ATstrat = -2;
                else if(awayTeam.getPlaybookDefNum() == awayDC.defStrat && awayTeam.getPlaybookDefNum() != awayHC.defStrat) ATstrat = 0;
                else if(awayTeam.getPlaybookDefNum() != awayDC.defStrat && awayTeam.getPlaybookDefNum() == awayHC.defStrat) ATstrat = -1;
            }

            adv = (int)Math.round(((homeHC != null ? homeHC.ratOff : 0) + 2*(homeOC != null ? homeOC.ratOff : 0) + HTstrat - ATstrat - 2*(awayDC != null ? awayDC.ratDef : 0) - (awayHC != null ? awayHC.ratDef : 0)) / 5.0);

        } else {

            int HTstrat = 0;
            if (homeDC != null && homeHC != null) {
                if(homeTeam.getPlaybookDefNum() != homeDC.defStrat && homeTeam.getPlaybookDefNum() != homeHC.defStrat) HTstrat = -2;
                else if(homeTeam.getPlaybookDefNum() == homeDC.defStrat && homeTeam.getPlaybookDefNum() != homeHC.defStrat) HTstrat = 0;
                else if(homeTeam.getPlaybookDefNum() != homeDC.defStrat && homeTeam.getPlaybookDefNum() == homeHC.defStrat) HTstrat = -1;
            }

            int awayOCoff = awayOC != null ? awayOC.offStrat : 0;
            int ATstrat = Math.abs(awayTeam.getPlaybookOffNum() - awayOCoff);
            if (awayOC != null && awayHC != null) {
                if(awayTeam.getPlaybookOffNum() != awayOC.offStrat && awayTeam.getPlaybookOffNum() != awayHC.offStrat) ATstrat = -2;
                else if(awayTeam.getPlaybookOffNum() == awayOC.offStrat && awayTeam.getPlaybookOffNum() != awayHC.offStrat) ATstrat = 0;
                else if(awayTeam.getPlaybookOffNum() != awayOC.offStrat && awayTeam.getPlaybookOffNum() == awayHC.offStrat) ATstrat = -1;
            }

            adv = (int)Math.round(((awayHC != null ? awayHC.ratOff : 0) + 2*(awayOC != null ? awayOC.ratOff : 0) + ATstrat - HTstrat - 2*(homeDC != null ? homeDC.ratDef : 0) - (homeHC != null ? homeHC.ratDef : 0)) / 5.0);
        }
        adv += getTeamChemistryAdv();
        if (gamePoss) {
            if (homeTeam.getHeadCoach() != null) {
                adv += homeTeam.getHeadCoach().gamePrepBonus();
            }
        } else {
            if (awayTeam.getHeadCoach() != null) {
                adv += awayTeam.getHeadCoach().gamePrepBonus();
            }
        }
        if (adv > 4) adv = 4;
        if (adv < -4) adv = -4;
        return adv;
    }

    private int getTeamChemistryAdv() {
        int adv = 0;

        if (gamePoss) {
            adv = (int) (Math.round((homeTeam.getTeamChemistry() + homeTeam.getStaffDiscipline() - awayTeam.getStaffDiscipline() - awayTeam.getTeamChemistry()) / 5));
        } else {
            adv = (int) (Math.round((awayTeam.getTeamChemistry() + awayTeam.getStaffDiscipline() - homeTeam.getStaffDiscipline() - homeTeam.getTeamChemistry()) / 5));
        }
        if (adv > 3) adv = 3;
        if (adv < -3) adv = -3;
        return adv;
    }

    private void coachingStrategyAdjustments() {
        //Set Default Team Playbooks to memory
        homeOffense = homeTeam.getPlaybookOffNum();
        homeDefense = homeTeam.getPlaybookDefNum();
        awayOffense = awayTeam.getPlaybookOffNum();
        awayDefense = awayTeam.getPlaybookDefNum();
        HeadCoach homeHC = homeTeam.getHeadCoach();
        HeadCoach awayHC = awayTeam.getHeadCoach();

        if (awayTeam.isUserControlled() || homeTeam.isUserControlled()) {

            //Counter Strategies - performed in snake order.
            if (!homeTeam.isUserControlled() && homeHC != null && homeHC.ratDef > tacticalCoach && Math.random() < 0.35) {
                if (awayTeam.getPlaybookOffNum() == 0) homeTeam.setPlaybookDefNum(0);
                if (awayTeam.getPlaybookOffNum() == 1) homeTeam.setPlaybookDefNum(1);
                if (awayTeam.getPlaybookOffNum() == 2) {
                    if (Math.random() > 0.50) homeTeam.setPlaybookDefNum(2);
                    else homeTeam.setPlaybookDefNum(3);
                }
                if (awayTeam.getPlaybookOffNum() == 3) homeTeam.setPlaybookDefNum(4);
                if (awayTeam.getPlaybookOffNum() == 4) homeTeam.setPlaybookDefNum(1);
                if (awayTeam.getPlaybookOffNum() == 5) homeTeam.setPlaybookDefNum(3);
            }

            if (!awayTeam.isUserControlled() && awayHC != null && awayHC.ratDef > tacticalCoach && Math.random() < 0.35) {
                if (homeTeam.getPlaybookOffNum() == 0) awayTeam.setPlaybookDefNum(0);
                if (homeTeam.getPlaybookOffNum() == 1) awayTeam.setPlaybookDefNum(1);
                if (homeTeam.getPlaybookOffNum() == 2) {
                    if (Math.random() > 0.50) awayTeam.setPlaybookDefNum(2);
                    else awayTeam.setPlaybookDefNum(3);
                }
                if (homeTeam.getPlaybookOffNum() == 3) awayTeam.setPlaybookDefNum(4);
                if (homeTeam.getPlaybookOffNum() == 4) awayTeam.setPlaybookDefNum(1);
                if (homeTeam.getPlaybookOffNum() == 5) awayTeam.setPlaybookDefNum(3);
            }

            if (!awayTeam.isUserControlled() && awayHC != null && awayHC.ratOff > tacticalCoach && Math.random() < 0.35) {
                if (awayTeam.getPlaybookOffNum() == 1 && homeTeam.getPlaybookDefNum() == 1 || awayTeam.getPlaybookOffNum() == 4 && homeTeam.getPlaybookDefNum() == 1) {
                    awayTeam.setPlaybookOffNum((int) (Math.random() * 3) + 1);
                    if (awayTeam.getPlaybookOffNum() == 1) awayTeam.setPlaybookOffNum(0);
                }

                if (awayTeam.getPlaybookOffNum() == 3 && homeTeam.getPlaybookDefNum() == 4) {
                    awayTeam.setPlaybookOffNum((int) (Math.random() * 3));
                }
            }

            if (!homeTeam.isUserControlled() && homeHC != null && homeHC.ratOff > tacticalCoach && Math.random() < 0.35) {
                if (homeTeam.getPlaybookOffNum() == 1 && awayTeam.getPlaybookDefNum() == 1 || homeTeam.getPlaybookOffNum() == 4 && awayTeam.getPlaybookDefNum() == 1) {
                    homeTeam.setPlaybookOffNum((int) (Math.random() * 3) + 1);
                    if (homeTeam.getPlaybookOffNum() == 1) homeTeam.setPlaybookOffNum(0);
                }

                if (homeTeam.getPlaybookOffNum() == 3 && awayTeam.getPlaybookDefNum() == 4) {
                    homeTeam.setPlaybookOffNum((int) (Math.random() * 3));
                }
            }

        } else {

            //Counter Strategies - performed in snake order.
            if (homeHC != null && homeHC.ratDef > tacticalCoach && Math.random() < 0.45) {
                if (awayTeam.getPlaybookOffNum() == 0) homeTeam.setPlaybookDefNum(0);
                if (awayTeam.getPlaybookOffNum() == 1) homeTeam.setPlaybookDefNum(1);
                if (awayTeam.getPlaybookOffNum() == 2) {
                    if (Math.random() > 0.50) homeTeam.setPlaybookDefNum(2);
                    else homeTeam.setPlaybookDefNum(3);
                }
                if (awayTeam.getPlaybookOffNum() == 3) homeTeam.setPlaybookDefNum(4);
                if (awayTeam.getPlaybookOffNum() == 4) homeTeam.setPlaybookDefNum(1);
                if (awayTeam.getPlaybookOffNum() == 5) homeTeam.setPlaybookDefNum(3);
            }

            if (awayHC != null && awayHC.ratDef > tacticalCoach && Math.random() < 0.45) {
                if (homeTeam.getPlaybookOffNum() == 0) awayTeam.setPlaybookDefNum(0);
                if (homeTeam.getPlaybookOffNum() == 1) awayTeam.setPlaybookDefNum(1);
                if (homeTeam.getPlaybookOffNum() == 2) {
                    if (Math.random() > 0.50) awayTeam.setPlaybookDefNum(2);
                    else awayTeam.setPlaybookDefNum(3);
                }
                if (homeTeam.getPlaybookOffNum() == 3) awayTeam.setPlaybookDefNum(4);
                if (homeTeam.getPlaybookOffNum() == 4) awayTeam.setPlaybookDefNum(1);
                if (homeTeam.getPlaybookOffNum() == 5) awayTeam.setPlaybookDefNum(3);
            }

            if (awayHC != null && awayHC.ratOff > tacticalCoach && Math.random() < 0.45) {
                if (awayTeam.getPlaybookOffNum() == 1 && homeTeam.getPlaybookDefNum() == 1 || awayTeam.getPlaybookOffNum() == 4 && homeTeam.getPlaybookDefNum() == 1) {
                    awayTeam.setPlaybookOffNum((int) (Math.random() * 3) + 1);
                    if (awayTeam.getPlaybookOffNum() == 1) awayTeam.setPlaybookOffNum(0);
                }

                if (awayTeam.getPlaybookOffNum() == 3 && homeTeam.getPlaybookDefNum() == 4) {
                    awayTeam.setPlaybookOffNum((int) (Math.random() * 3));
                }
            }

            if (homeHC != null && homeHC.ratOff > tacticalCoach && Math.random() < 0.45) {
                if (homeTeam.getPlaybookOffNum() == 1 && awayTeam.getPlaybookDefNum() == 1 || homeTeam.getPlaybookOffNum() == 4 && awayTeam.getPlaybookDefNum() == 1) {
                    homeTeam.setPlaybookOffNum((int) (Math.random() * 3) + 1);
                    if (homeTeam.getPlaybookOffNum() == 1) homeTeam.setPlaybookOffNum(0);
                }

                if (homeTeam.getPlaybookOffNum() == 3 && awayTeam.getPlaybookDefNum() == 4) {
                    homeTeam.setPlaybookOffNum((int) (Math.random() * 3));
                }
            }
        }
    }

    private void getReturner(Team t) {
        ArrayList<PlayerReturner> teamReturner = new ArrayList<>();

        double starterPenalty = 0.85;
        //Choose Kickoff Returners — bound by actual roster size so depleted teams don't NPE
        int wrN = t.getTeamWRs().size();
        int rbN = t.getTeamRBs().size();
        int cbN = t.getTeamCBs().size();
        for (int i = 0; i < Math.min(t.startersWR, wrN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamWRs().get(i).name, "WR", t.getTeamWRs().get(i).getRatSpeed(), (float) (starterPenalty * t.getTeamWRs().get(i).getRatSpeed() * Math.random())));
        }
        for (int i = 0; i < Math.min(t.startersRB, rbN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamRBs().get(i).name, "RB", t.getTeamRBs().get(i).getRatSpeed(), (float) (starterPenalty * t.getTeamRBs().get(i).getRatSpeed() * Math.random())));
        }
        for (int i = 0; i < Math.min(t.startersCB, cbN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamCBs().get(i).name, "CB", t.getTeamCBs().get(i).getRatSpeed(), (float) (starterPenalty * t.getTeamCBs().get(i).getRatSpeed() * Math.random())));
        }

        for (int i = t.startersWR; i < Math.min(t.startersWR + t.subWR, wrN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamWRs().get(i).name, "WR", t.getTeamWRs().get(i).getRatSpeed(), (float) (t.getTeamWRs().get(i).getRatSpeed() * Math.random())));
        }
        for (int i = t.startersRB; i < Math.min(t.startersRB + t.subRB, rbN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamRBs().get(i).name, "RB", t.getTeamRBs().get(i).getRatSpeed(), (float) (t.getTeamRBs().get(i).getRatSpeed() * Math.random())));
        }
        for (int i = t.startersCB; i < Math.min(t.startersCB + t.subCB, cbN); i++) {
            teamReturner.add(new PlayerReturner(t.getAbbr(), t.getTeamCBs().get(i).name, "CB", t.getTeamCBs().get(i).getRatSpeed(), (float) (t.getTeamCBs().get(i).getRatSpeed() * Math.random())));
        }

        if (teamReturner.isEmpty()) {
            // Extremely depleted roster — synthesize a returner so kickoffs never NPE.
            List<? extends Player> roster = t.getAllPlayers();
            if (!roster.isEmpty()) {
                Player p = roster.get(0);
                int speed = Math.max(40, p.ratOvr);
                teamReturner.add(new PlayerReturner(t.getAbbr(), p.name, p.position, speed, (float) speed));
            } else {
                teamReturner.add(new PlayerReturner(t.getAbbr(), "Emergency Returner", "WR", 50, 50f));
            }
        }

        Collections.sort(teamReturner, new CompPlayerReturners());

        PlayerReturner chosen = teamReturner.get(0);
        if (teamReturner.size() >= 2) {
            teamReturner.get(0).gameSpeed = (float) (teamReturner.get(0).ratSpeed * Math.random());
            teamReturner.get(1).gameSpeed = (float) (teamReturner.get(1).ratSpeed * Math.random());
            if (teamReturner.get(1).gameSpeed > teamReturner.get(0).gameSpeed) {
                chosen = teamReturner.get(1);
            }
        }

        chosen.kYards = 0;
        chosen.kReturns = 0;
        chosen.pYards = 0;
        chosen.pReturns = 0;
        chosen.kTD = 0;
        chosen.pTD = 0;
        chosen.startPos = 0;

        if (awayTeam == t) {
            awayKickReturner = chosen;
        } else {
            homeKickReturner = chosen;
        }
    }
    
    private int getSpecialTeamsD(Team specialTeams) {
        int ST = 0;
        teamST = new ArrayList<>();

        int lbN = specialTeams.getTeamLBs().size();
        int cbN = specialTeams.getTeamCBs().size();
        int sN = specialTeams.getTeamSs().size();

        for (int i = specialTeams.startersLB; i < Math.min(specialTeams.startersLB + specialTeams.subLB, lbN); i++) {
            PlayerLB lb = specialTeams.getLB(i);
            if (lb == null) continue;
            ST += lb.getRatSpeed();
            teamST.add(new PlayerST(specialTeams.getAbbr(), lb.name, "LB", lb.getRatSpeed(), lb.getRatTackle()));
        }
        for (int i = specialTeams.startersCB; i < Math.min(specialTeams.startersCB + specialTeams.subCB, cbN); i++) {
            PlayerCB cb = specialTeams.getCB(i);
            if (cb == null) continue;
            ST += cb.getRatSpeed();
            teamST.add(new PlayerST(specialTeams.getAbbr(), cb.name, "CB", cb.getRatSpeed(), cb.getRatTackle()));
        }
        for (int i = specialTeams.startersS; i < Math.min(specialTeams.startersS + specialTeams.subS, sN); i++) {
            PlayerS s = specialTeams.getS(i);
            if (s == null) continue;
            ST += s.getRatSpeed();
            teamST.add(new PlayerST(specialTeams.getAbbr(), s.name, "S", s.getRatSpeed(), s.getRatTackle()));
        }

        if (teamST.isEmpty()) {
            // Fall back to any available defender so ST coverage never crashes.
            List<? extends Player> roster = specialTeams.getAllPlayers();
            for (Player p : roster) {
                if (p instanceof PlayerLB || p instanceof PlayerCB || p instanceof PlayerS) {
                    int speed = Math.max(40, p.ratOvr);
                    teamST.add(new PlayerST(specialTeams.getAbbr(), p.name, p.position, speed, speed));
                    ST += speed;
                    break;
                }
            }
            if (teamST.isEmpty()) {
                teamST.add(new PlayerST(specialTeams.getAbbr(), "Emergency Cover", "LB", 50, 50));
                ST = 50;
            }
        }

        Collections.sort(teamST, new CompPlayerSTSpeed());
        playerST = teamST.get(0);

        int stDenom = Math.max(1, teamST.size());
        ST = ST / stDenom;

        return ST;
    }

    //GAME SIMULATION

    public void playGame() {
        if (gameName.equals(BYE_WEEK_NAME) && !hasPlayed) {
            hasPlayed = true;
            homeTeam.addToGameWLSchedule("BYE");
            awayTeam.addToGameWLSchedule("BYE");
            homeTeam.healInjury(1);
            awayTeam.healInjury(1);
        }

        if (!hasPlayed) {
            gameEventLog.append("LOG: #").append(awayTeam.getRankTeamPollScore()).append(" ").append(awayTeam.getAbbr()).append(" (").append(awayTeam.getWins()).append("-").append(awayTeam.getLosses()).append(") @ #")
                .append(homeTeam.getRankTeamPollScore()).append(" ").append(homeTeam.getAbbr()).append(" (").append(homeTeam.getWins()).append("-").append(homeTeam.getLosses()).append(")").append("\n")
                .append("---------------------------------------------------------\n\n")
                .append(awayTeam.getAbbr()).append(" Off Strategy: ").append(awayTeam.getPlaybookOffense().getStratName()).append("\n")
                .append(awayTeam.getAbbr()).append(" Def Strategy: ").append(awayTeam.getPlaybookDefense().getStratName()).append("\n")
                .append(homeTeam.getAbbr()).append(" Off Strategy: ").append(homeTeam.getPlaybookOffense().getStratName()).append("\n")
                .append(homeTeam.getAbbr()).append(" Def Strategy: ").append(homeTeam.getPlaybookDefense().getStratName()).append("\n")
                .append("\n\n-- 1st QUARTER --");
            //probably establish some home field advantage before playing
            gameTime = 3600;
            gameDown = 1;
            gamePoss = true;

            //Reset Game Stats & Fatigue
            List<Player> allHomePlayers = homeTeam.getAllPlayers();
            List<Player> allAwayPlayers = awayTeam.getAllPlayers();
            for (int i = 0; i < allHomePlayers.size(); ++i) {
                allHomePlayers.get(i).resetGameSimData();
            }
            for (int i = 0; i < allAwayPlayers.size(); ++i) {
                allAwayPlayers.get(i).resetGameSimData();
            }

            getReturner(awayTeam);
            getReturner(homeTeam);

            awayTeam.addGamesStartedPlayers();
            homeTeam.addGamesStartedPlayers();

            kickOff(homeTeam, awayTeam);

            // Regulation
            while (gameTime > 0) {
                //play ball!
                if (gamePoss) runPlay(homeTeam, awayTeam);
                else runPlay(awayTeam, homeTeam);
            }

            // Add last play
            if (homeScore != awayScore) {
                gameEventLog.append(getEventLogScore()).append("\nTime has expired! The game is over.");
            } else {
                gameEventLog.append(getEventLogScore()).append("\nOVERTIME!\nTie game at 0:00, overtime begins!");
            }

            //Overtime (if needed)
            if (gameTime <= 0 && homeScore == awayScore) {
                playingOT = true;
                gamePoss = false;
                gameYardLine = 75;
                numOT++;
                gameTime = -1;
                gameDown = 1;
                gameYardsNeed = 10;

                while (playingOT) {
                    if (gamePoss) runPlay(homeTeam, awayTeam);
                    else runPlay(awayTeam, homeTeam);
                }
            }

            //game over, add wins
            if (homeScore > awayScore) {
                homeTeam.incrementWins();
                homeTeam.addToGameWLSchedule("W");
                awayTeam.incrementLosses();
                awayTeam.addToGameWLSchedule("L");
                homeTeam.addGameWinAgainst(awayTeam);
                awayTeam.addGameLossAgainst(homeTeam);
                homeTeam.getWinStreak().addWin(homeTeam.league.getYear());
                homeTeam.league.checkLongestWinStreak(homeTeam.getWinStreak());
                awayTeam.getWinStreak().resetStreak(awayTeam.league.getYear());
                if (homeTeam.getHeadCoach() != null) homeTeam.getHeadCoach().recordWins(1);
                if (awayTeam.getHeadCoach() != null) awayTeam.getHeadCoach().recordLosses(1);
            } else {
                homeTeam.incrementLosses();
                homeTeam.addToGameWLSchedule("L");
                awayTeam.incrementWins();
                awayTeam.addToGameWLSchedule("W");
                awayTeam.addGameWinAgainst(homeTeam);
                homeTeam.addGameLossAgainst(awayTeam);
                awayTeam.getWinStreak().addWin(awayTeam.league.getYear());
                awayTeam.league.checkLongestWinStreak(awayTeam.getWinStreak());
                homeTeam.getWinStreak().resetStreak(homeTeam.league.getYear());
                if (awayTeam.getHeadCoach() != null) awayTeam.getHeadCoach().recordWins(1);
                if (homeTeam.getHeadCoach() != null) homeTeam.getHeadCoach().recordLosses(1);
            }

            // Add points/opp points
            homeTeam.addGamePlayedPlayers();
            awayTeam.addGamePlayedPlayers();

            homeTeam.setTeamPoints(homeTeam.getTeamPoints() + homeScore);
            awayTeam.setTeamPoints(awayTeam.getTeamPoints() + awayScore);

            homeTeam.setTeamOppPoints(homeTeam.getTeamOppPoints() + awayScore);
            awayTeam.setTeamOppPoints(awayTeam.getTeamOppPoints() + homeScore);

            homeYards = getPassYards(false) + getRushYards(false);
            awayYards = getPassYards(true) + getRushYards(true);

            homeTeam.setTeamYards(homeTeam.getTeamYards() + homeYards);
            awayTeam.setTeamYards(awayTeam.getTeamYards() + awayYards);

            homeTeam.setTeamOppYards(homeTeam.getTeamOppYards() + awayYards);
            awayTeam.setTeamOppYards(awayTeam.getTeamOppYards() + homeYards);

            homeTeam.setTeamOppPassYards(homeTeam.getTeamOppPassYards() + getPassYards(true));
            awayTeam.setTeamOppPassYards(awayTeam.getTeamOppPassYards() + getPassYards(false));
            homeTeam.setTeamOppRushYards(homeTeam.getTeamOppRushYards() + getRushYards(true));
            awayTeam.setTeamOppRushYards(awayTeam.getTeamOppRushYards() + getRushYards(false));
            homePassYards = getPassYards(false);
            awayPassYards = getPassYards(true);
            homeRushYards = getRushYards(false);
            awayRushYards = getRushYards(true);

            homeTeam.setTeamTODiff(homeTeam.getTeamTODiff() + awayTOs - homeTOs);
            awayTeam.setTeamTODiff(awayTeam.getTeamTODiff() + homeTOs - awayTOs);

            gameStatistics();

            //Reset Strategies

            homeOffense = homeTeam.getPlaybookOffNum();
            homeDefense = homeTeam.getPlaybookDefNum();
            awayOffense = awayTeam.getPlaybookOffNum();
            awayDefense = awayTeam.getPlaybookDefNum();

            hasPlayed = true;

            addNewsStory();

            //homeTeam.checkForInjury();
            //awayTeam.checkForInjury();
            homeTeam.healInjury(1);
            awayTeam.healInjury(1);

            if (!gameName.equals("Conference") && !gameName.equals("OOC")) {
                int attendance = ((homeTeam.getTeamPrestige() * 2 + awayTeam.getTeamPrestige()) / 3);
                int homeAdd = (int) (tickets * .75 * attendance) + (int) (merch * Math.random() * homeTeam.getTeamPrestige());
                homeAdd = (int) (homeAdd * homeTeam.getHomeGameRevenueMultiplier());
                homeTeam.setTeamBudget(homeTeam.getTeamBudget() + homeAdd);
                awayTeam.setTeamBudget(awayTeam.getTeamBudget() + (int) (tickets * .25 * attendance));
            } else {
                int attendance = ((homeTeam.getTeamPrestige() * 2 + awayTeam.getTeamPrestige()) / 3);
                if(gameName.contains("CCG")) attendance = (int) (attendance * 1.5);
                else if(gameName.contains("NCG")) attendance = (int) (attendance * 4);
                else if(gameName.contains("Semis")) attendance = (int) (attendance * 2.5);
                else attendance = (int) (attendance * 2);
                int homeAdd = (int) (1.25 * tickets * .50 * attendance) + (int) (2 * merch * Math.random() * homeTeam.getTeamPrestige());
                homeAdd = (int) (homeAdd * homeTeam.getHomeGameRevenueMultiplier());
                homeTeam.setTeamBudget(homeTeam.getTeamBudget() + homeAdd);
                awayTeam.setTeamBudget(awayTeam.getTeamBudget() + (int) (1.25 * tickets * .50 * attendance) + (int) (2 * merch * Math.random() * awayTeam.getTeamPrestige()));
            }

            if (homeScore > awayScore) homeTeam.setTeamBudget(homeTeam.getTeamBudget() + winSales);
            if (awayScore > homeScore) awayTeam.setTeamBudget(awayTeam.getTeamBudget() + winSales);


        }
    }

    // PRE-SNAP DECISIONS

    private void runPlay(Team offense, Team defense) {
        quarterCheck();
        recoup(false, 0);
        snapCount++;
        gameYardLinePlay = gameYardLine;

        if (gameDown > 4) {
            if (!playingOT) {
                //Log the turnover on downs, reset down and distance, give possession to the defense, exit this runPlay()
                gameEventLog.append(getEventLog()).append("TURNOVER ON DOWNS!\n").append(offense.getAbbr()).append(" failed to convert on ").append(gameDown - 1).append("th down. ").append(defense.getAbbr()).append(" takes over possession on downs.");

                //Turn over on downs, change possession, set to first down and 10 yards to go
                gamePoss = !gamePoss;
                gameDown = 1;
                gameYardsNeed = 10;
                //and flip which direction the ball is moving in
                gameYardLine = 100 - gameYardLine;

            } else {
                //OT is over for the offense, log the turnover on downs, run resetForOT().
                gameEventLog.append(getEventLog()).append("TURNOVER ON DOWNS!\n").append(offense.getAbbr()).append(" failed to convert on ").append(gameDown - 1).append("th down in OT and their possession is over.");
                resetForOT();

            }
        } else {
            double preferPass = (offense.getPassProf() - defense.getPassDef()) / 100 + Math.random() * offense.getPlaybookOffense().getPassPref();       //STRATEGIES
            double preferRush = (offense.getRushProf() - defense.getRushDef()) / 90 + Math.random() * offense.getPlaybookOffense().getRunPref();

            // Scheme matchup: heavier defensive fronts vs run-first books tighten rushing looks;
            // pass-favored shells vs pass-heavy offenses chip away at obvious pass advantages.
            int offRunBook = offense.getPlaybookOffense().getRunPref();
            int offPassBook = offense.getPlaybookOffense().getPassPref();
            int defRunBook = defense.getPlaybookDefense().getRunPref();
            int defPassBook = defense.getPlaybookDefense().getPassPref();
            preferRush -= (defRunBook - offRunBook) * 0.04;
            preferPass -= (defPassBook - offPassBook) * 0.03;

            boolean offenseIsHome = offense == homeTeam;
            boolean offenseLeading = offenseIsHome ? homeScore > awayScore : awayScore > homeScore;
            boolean offenseTrailing = offenseIsHome ? homeScore < awayScore : awayScore < homeScore;
            boolean hurryUpPass = !playingOT && gameTime <= 120 && gameTime > 20 && offenseTrailing && gameDown < 4;
            // Fourth quarter: lean run when protecting a lead (outside the final-snap FG / hail mary window below).
            if (!playingOT && gameTime <= 480 && gameTime > 20 && offenseLeading && gameDown < 4) {
                preferRush += 0.35 + 0.15 * Math.random();
                preferPass -= 0.12;
            }
            if (hurryUpPass) {
                preferPass += 0.45 + 0.15 * Math.random();
                preferRush -= 0.15;
            }

            // If it's 1st and Goal to go, adjust yards needed to reflect distance for a TD so that play selection reflects actual yards to go
            // If we don't do this, gameYardsNeed may be higher than the actually distance for a TD and suboptimal plays may be chosen
            if (gameDown == 1 && gameYardLine >= 91) gameYardsNeed = 100 - gameYardLine;

            //Under 20 seconds to play: winning team kneels, trailing team goes for it
            if (gameTime <= 20 && !playingOT) {
                if ((gamePoss && (homeScore > awayScore)) || (!gamePoss && (awayScore > homeScore))) {
                    gameTime -= timePerPlay * Math.random();
                    gameDown++;
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" kneels to run out the clock.");
                    return;
                }
                //Down by 3 or less, or tied, and you have the ball
                if (((gamePoss && (awayScore - homeScore) <= 3) || (!gamePoss && (homeScore - awayScore) <= 3)) && gameYardLine > 60) {
                    //last second FGA
                    fieldGoalAtt(offense, defense);
                } else {
                    //hail mary
                    passingPlay(offense, defense);
                }
            } else if (gameDown >= 4) {
                if (((gamePoss && (awayScore - homeScore) > 3) || (!gamePoss && (homeScore - awayScore) > 3)) && gameTime < 300) {
                    //go for it since we need 7 to win -- This also forces going for it if down by a TD in BOT OT
                    if (gameYardsNeed < 3 && preferRush * 3 > preferPass) {
                        rushingPlay(offense, defense);
                    } else {
                        passingPlay(offense, defense);
                    }
                } else {
                    //4th down
                    if (gameYardsNeed < 3) {
                        if (gameYardLine > 65) {
                            //fga
                            fieldGoalAtt(offense, defense);
                        } else if (gameYardLine > 55) {
                            // run play, go for it!
                            rushingPlay(offense, defense);
                        } else {
                            //punt
                            puntPlay(offense, defense);
                        }
                    } else if (gameYardLine > 60) {
                        //fga
                        fieldGoalAtt(offense, defense);
                    } else {
                        //punt
                        puntPlay(offense, defense);
                    }
                }
            } else if (gameDown == 3 && gameYardsNeed <= 2 && (!hurryUpPass || gameYardsNeed == 1)) {
                // Short-yardage: prefer power run unless late hurry-up on third-and-two (still allow run on third-and-one).
                rushingPlay(offense, defense);
            } else if ((gameDown == 3 && gameYardsNeed > 4) || ((gameDown == 1 || gameDown == 2) && (preferPass >= preferRush))) {
                // pass play
                passingPlay(offense, defense);
            } else {
                //run play
                rushingPlay(offense, defense);
            }
        }


    }

    private void passingPlay(Team offense, Team defense) {
        int x = 0;
        if (gameTime < 900 && gamePoss && (homeScore - awayScore) >= 20 + gameTime / 60) {
            x = 1;
        } else if (gameTime < 900 && !gamePoss && (awayScore - homeScore) >= 20 + gameTime / 60) {
            x = 1;
        }

        PlayerQB selQB;
        PlayerRB selRB;
        PlayerWR selWR;
        PlayerWR selWR2;
        PlayerTE selTE;
        PlayerDL selDL;
        PlayerLB selLB;
        PlayerLB selLB2;
        PlayerCB selCB;
        PlayerS selS;
        PlayerS selS2;


        ArrayList<Player> receiver = new ArrayList<>();
        ArrayList<PlayerRB> RunningBack = new ArrayList<>();
        ArrayList<PlayerWR> WideReceiver = new ArrayList<>();
        ArrayList<PlayerTE> TightEnd = new ArrayList<>();
        ArrayList<PlayerDL> DLineman = new ArrayList<>();
        ArrayList<PlayerLB> Linebacker = new ArrayList<>();
        ArrayList<PlayerS> Safety = new ArrayList<>();

        //Receiver Options
        for (int i = 0 + x; i < offense.startersWR + x; ++i) {
            if (offense.getWR(i).gameFatigue > 0) {
                offense.getWR(i).gameSim = Math.pow(offense.getWR(i).ratOvr, 1) * Math.random();
                offense.getWR(i).posDepth = i;
                offense.getWR(i).gameSnaps++;
                receiver.add(offense.getWR(i));
                WideReceiver.add(offense.getWR(i));
            } else {
                offense.getWR(offense.startersWR).gameSim = Math.pow(offense.getWR(offense.startersWR).ratOvr, 1) * Math.random();
                offense.getWR(offense.startersWR).posDepth = i;
                offense.getWR(offense.startersWR).gameSnaps++;
                receiver.add(offense.getWR(offense.startersWR));
                WideReceiver.add(offense.getWR(offense.startersWR));
            }
        }

        double TEBonus;
        TEBonus = offense.getPlaybookOffense().getPassUsage() * 0.15;
        for (int i = 0 + x; i < offense.startersTE + x; ++i) {
            if (offense.getTE(i).gameFatigue > 0) {
                if (gameYardLine > 80) {
                    offense.getTE(i).gameSim = Math.pow(((offense.getTE(i).getRatCatch() + offense.getTE(0).getRatSpeed()) / 2), 1) * Math.random() * 1.25;
                } else {
                    offense.getTE(i).gameSim = Math.pow(((offense.getTE(i).getRatCatch() + offense.getTE(i).getRatSpeed()) / 2), 1) * Math.random() * (.70 + TEBonus);
                }
                offense.getTE(i).gameSnaps++;
                receiver.add(offense.getTE(i));
                TightEnd.add(offense.getTE(i));
            } else {
                if (gameYardLine > 80) {
                    offense.getTE(offense.startersTE).gameSim = Math.pow(((offense.getTE(offense.startersTE).getRatCatch() + offense.getTE(offense.startersTE).getRatSpeed()) / 2), 1) * Math.random() * 1.25;
                } else {
                    offense.getTE(offense.startersTE).gameSim = Math.pow(((offense.getTE(offense.startersTE).getRatCatch() + offense.getTE(offense.startersTE).getRatSpeed()) / 2), 1) * Math.random() * (.70 + TEBonus);
                }
                offense.getTE(offense.startersTE).gameSnaps++;
                receiver.add(offense.getTE(offense.startersTE));
                TightEnd.add(offense.getTE(offense.startersTE));
            }
        }
        double RBBonus;
        RBBonus = offense.getPlaybookOffense().getPassUsage() * 0.10;
        for (int i = 0 + x; i < offense.startersRB + x; ++i) {
            if (offense.getRB(i).gameFatigue > 0) {
                offense.getRB(i).gameSim = Math.pow(((offense.getRB(0).getRatCatch() + offense.getRB(i).getRatSpeed()) / 2), 1) * Math.random() * (.70 + RBBonus);
                offense.getRB(i).gameSnaps++;
                receiver.add(offense.getRB(i));
                RunningBack.add(offense.getRB(i));
            } else {
                offense.getRB(offense.startersRB).gameSim = Math.pow(((offense.getRB(offense.startersRB).getRatCatch() + offense.getRB(offense.startersRB).getRatSpeed()) / 2), 1) * Math.random() * (.70 + RBBonus);
                offense.getRB(offense.startersRB).gameSnaps++;
                receiver.add(offense.getRB(offense.startersRB));
                RunningBack.add(offense.getRB(offense.startersRB));
            }
        }

        int z = 0; //o-line counter
        teamOLs = new ArrayList<>();
        for (int i = 0 + x; i < offense.startersOL + x; ++i) {
            if (offense.getOL(i).gameFatigue > 0) {
                offense.getOL(i).gameSnaps++;
                teamOLs.add(offense.getOL(i));
            } else {
                z++;
                offense.getOL(offense.startersOL + z).gameSnaps++;
                teamOLs.add(offense.getOL(offense.startersOL + z));
            }
        }

        for (int i = 0 + x; i < defense.startersLB + x; ++i) {
            if (defense.getLB(i).gameFatigue > 0) {
                defense.getLB(i).gameSim = Math.pow(defense.getLB(i).getRatCoverage(), 1) * Math.random();
                defense.getLB(i).gameSnaps++;
                Linebacker.add(defense.getLB(i));
            } else {
                defense.getLB(defense.startersLB).gameSim = Math.pow(defense.getLB(defense.startersLB).getRatCoverage(), 1) * Math.random();
                defense.getLB(defense.startersLB + 1).gameSim = Math.pow(defense.getLB(defense.startersLB + 1).getRatCoverage(), 1) * Math.random();
                defense.getLB(defense.startersLB).gameSnaps++;
                defense.getLB(defense.startersLB + 1).gameSnaps++;
                Linebacker.add(defense.getLB(defense.startersLB));
                Linebacker.add(defense.getLB(defense.startersLB + 1));
            }
        }

        z = 0; //o-line counter
        teamDLs = new ArrayList<>();
        for (int i = 0 + x; i < defense.startersDL + x; ++i) {
            if (defense.getDL(i).gameFatigue > 0) {
                defense.getDL(i).gameSim = Math.pow(defense.getDL(i).getRatPassRush(), 1) * Math.random();
                defense.getDL(i).gameSnaps++;
                DLineman.add(defense.getDL(i));
                teamDLs.add(defense.getDL(i));
            } else {
                z++;
                defense.getDL(defense.startersDL + z).gameSim = Math.pow(defense.getDL(defense.startersDL + z).getRatPassRush(), 1) * Math.random();
                defense.getDL(defense.startersDL + 1 + z).gameSim = Math.pow(defense.getDL(defense.startersDL + 1 + z).getRatPassRush(), 1) * Math.random();
                defense.getDL(defense.startersDL + z).gameSnaps++;
                defense.getDL(defense.startersDL + 1 + z).gameSnaps++;
                DLineman.add(defense.getDL(defense.startersDL + z));
                DLineman.add(defense.getDL(defense.startersDL + 1 + z));
                teamDLs.add(defense.getDL(defense.startersDL + z));
            }
        }

        for (int i = 0 + x; i < defense.startersS + x; ++i) {
            if (defense.getS(i).gameFatigue > 0) {
                defense.getS(i).gameSim = Math.pow(defense.getS(i).getRatCoverage(), 1) * Math.random();
                defense.getS(i).gameSnaps++;
                Safety.add(defense.getS(i));
            } else {
                defense.getS(defense.startersS).gameSim = Math.pow(defense.getS(defense.startersS).getRatCoverage(), 1) * Math.random();
                defense.getS(defense.startersS).gameSnaps++;
                Safety.add(defense.getS(defense.startersS));
            }
        }
        //Rank Players
        Collections.sort(WideReceiver, new CompGamePlayerPicker());
        Collections.sort(TightEnd, new CompGamePlayerPicker());
        Collections.sort(RunningBack, new CompGamePlayerPicker());
        Collections.sort(receiver, new CompGamePlayerPicker());
        Collections.sort(Linebacker, new CompGamePlayerPicker());
        Collections.sort(DLineman, new CompGamePlayerPicker());
        Collections.sort(Safety, new CompGamePlayerPicker());


        //Choose Action Players
        selQB = offense.getQB(0 + x);
        selRB = RunningBack.get(0);
        selTE = TightEnd.get(0);
        selWR = WideReceiver.get(0);
        selWR2 = WideReceiver.size() > 1 ? WideReceiver.get(1) : WideReceiver.get(0);
        selDL = DLineman.get(0);
        selLB = Linebacker.get(0);
        selLB2 = Linebacker.size() > 1 ? Linebacker.get(1) : Linebacker.get(0);
        selCB = defense.getCB(WideReceiver.get(0).posDepth);
        selS = Safety.get(0);
        selS2 = Safety.size() > 1 ? Safety.get(1) : Safety.get(0);

        if (selCB.gameFatigue <= 0) selCB = defense.getCB(defense.startersCB);

        selQB.gameSnaps++;


        //Fatigue selected Action Players
        selRB.gameFatigue -= Math.round((100 - selRB.ratDurability) / 10);
        selWR.gameFatigue -= fatigueDropHigh + Math.round((100 - selWR.ratDurability) / 10);
        selWR2.gameFatigue -= fatigueDropMed + Math.round((100 - selWR.ratDurability) / 10);
        selTE.gameFatigue -= fatigueDropHigh + Math.round((100 - selTE.ratDurability) / 10);
        selDL.gameFatigue -= fatigueDropHigh + Math.round((100 - selDL.ratDurability) / 10);
        selLB.gameFatigue -= fatigueDropSuper + Math.round((100 - selLB.ratDurability) / 10);
        selLB2.gameFatigue -= fatigueDropSuper + Math.round((100 - selLB2.ratDurability) / 10);
        selCB.gameFatigue -= fatigueDropMed + Math.round((100 - selCB.ratDurability) / 10);
        selS.gameFatigue -= fatigueDropMed + Math.round((100 - selS.ratDurability) / 10);
        selS2.gameFatigue -= Math.round((100 - selS2.ratDurability) / 10);

        String pos = receiver.get(0).position;

        passingPlay(offense, defense, selQB, selRB, selWR, selTE, selDL, selLB, selLB2, selCB, selS, selS2, pos);

        if (Math.random() > injuryFreq) checkInjury(selQB, offense);
        if (Math.random() > injuryFreq) checkInjury(selRB, offense);
        if (Math.random() > injuryFreq) checkInjury(selWR, offense);
        if (Math.random() > injuryFreq) checkInjury(selTE, offense);
        if (Math.random() > injuryFreq) checkInjury(selDL, defense);
        if (Math.random() > injuryFreq) checkInjury(selLB, defense);
        if (Math.random() > injuryFreq) checkInjury(selLB2, defense);
        if (Math.random() > injuryFreq) checkInjury(selCB, defense);
        if (Math.random() > injuryFreq) checkInjury(selS, defense);
        if (Math.random() > injuryFreq) checkInjury(selS2, defense);

    }

    private void rushingPlay(Team offense, Team defense) {
        int x = 0;
        if (gameTime < 900 && gamePoss && (homeScore - awayScore) >= 20 + gameTime / 60) {
            x = 1;
        } else if (gameTime < 900 && !gamePoss && (awayScore - homeScore) >= 20 + gameTime / 60) {
            x = 1;
        }

        PlayerQB selQB;
        PlayerRB selRB;
        PlayerTE selTE;
        PlayerDL selDL;
        PlayerLB selLB;
        PlayerCB selCB;
        PlayerS selS;
        PlayerS selS2;

        ArrayList<Player> rusher = new ArrayList<>();
        ArrayList<PlayerRB> RunningBack = new ArrayList<>();
        ArrayList<PlayerTE> TightEnd = new ArrayList<>();
        ArrayList<PlayerDL> DLineman = new ArrayList<>();
        ArrayList<PlayerLB> Linebacker = new ArrayList<>();
        ArrayList<PlayerCB> Cornerback = new ArrayList<>();
        ArrayList<PlayerS> Safety = new ArrayList<>();

        //Action Players

        for (int i = 0 + x; i < offense.startersRB + x; ++i) {
            if (offense.getRB(i).gameFatigue > 0) {
                offense.getRB(i).gameSim = Math.pow(offense.getRB(i).ratOvr, 1.5) * Math.random();
                offense.getRB(i).gameSnaps++;
                rusher.add(offense.getRB(i));
                RunningBack.add(offense.getRB(i));
            } else {
                offense.getRB(offense.startersRB).gameSim = Math.pow(offense.getRB(offense.startersRB).ratOvr, 1.5) * Math.random();
                offense.getRB(offense.startersRB).gameSnaps++;
                rusher.add(offense.getRB(offense.startersRB));
                RunningBack.add(offense.getRB(offense.startersRB));
            }
        }

        if (offense.getPlaybookOffNum() == 4 || offense.getPlaybookOffNum() == 5)
            offense.getQB(0 + x).gameSim = Math.pow(offense.getQB(0 + x).getRatSpeed(), 1.485) * Math.random();
        else
            offense.getQB(0 + x).gameSim = 0.25 * Math.pow(offense.getQB(0 + x).getRatSpeed(), 1.485) * Math.random();
        rusher.add(offense.getQB(0 + x));

        int z = 0; //o-line counter
        teamOLs = new ArrayList<>();
        for (int i = 0 + x; i < offense.startersOL + x; ++i) {
            if (offense.getOL(i).gameFatigue > 0) {
                offense.getOL(i).gameSnaps++;
                teamOLs.add(offense.getOL(i));
            } else {
                z++;
                offense.getOL(offense.startersOL + z).gameSnaps++;
                teamOLs.add(offense.getOL(offense.startersOL + z));
            }
        }

        z = 0; //d-line counter
        teamDLs = new ArrayList<>();
        for (int i = 0 + x; i < defense.startersDL + x; ++i) {
            if (defense.getDL(i).gameFatigue > 0) {
                defense.getDL(i).gameSim = Math.pow(defense.getDL(i).getRatRunStop(), 1) * Math.random();
                defense.getDL(i).gameSnaps++;
                DLineman.add(defense.getDL(i));
                teamDLs.add(defense.getDL(i));
            } else {
                defense.getDL(defense.startersDL + z).gameSim = Math.pow(defense.getDL(defense.startersDL + z).getRatRunStop(), 1) * Math.random();
                defense.getDL(defense.startersDL + 1 + z).gameSim = Math.pow(defense.getDL(defense.startersDL + 1 + z).getRatRunStop(), 1) * Math.random();
                defense.getDL(defense.startersDL + z).gameSnaps++;
                defense.getDL(defense.startersDL + 1 + z).gameSnaps++;
                DLineman.add(defense.getDL(defense.startersDL + z));
                DLineman.add(defense.getDL(defense.startersDL + 1 + z));
                teamDLs.add(defense.getDL(defense.startersDL + z));
            }
        }

        for (int i = 0 + x; i < defense.startersLB + x; ++i) {
            if (defense.getLB(i).gameFatigue > 0) {
                defense.getLB(i).gameSim = Math.pow(defense.getLB(i).getRatRunStop(), 1) * Math.random();
                defense.getLB(i).gameSnaps++;
                Linebacker.add(defense.getLB(i));
            } else {
                defense.getLB(defense.startersLB).gameSim = Math.pow(defense.getLB(defense.startersLB).getRatRunStop(), 1) * Math.random();
                defense.getLB(defense.startersLB + 1).gameSim = Math.pow(defense.getLB(defense.startersLB + 1).getRatRunStop(), 1) * Math.random();
                defense.getLB(defense.startersLB).gameSnaps++;
                defense.getLB(defense.startersLB + 1).gameSnaps++;
                Linebacker.add(defense.getLB(defense.startersLB));
                Linebacker.add(defense.getLB(defense.startersLB + 1));
            }
        }

        for (int i = 0 + x; i < defense.startersCB + x; ++i) {
            if (defense.getCB(i).gameFatigue > 0) {
                defense.getCB(i).gameSim = Math.pow(defense.getCB(i).getRatTackle(), 1) * Math.random();
                defense.getCB(i).gameSnaps++;
                Cornerback.add(defense.getCB(i));
            } else {
                defense.getCB(defense.startersCB).gameSim = Math.pow(defense.getCB(defense.startersCB).getRatTackle(), 1) * Math.random();
                defense.getCB(defense.startersCB).gameSnaps++;
                Cornerback.add(defense.getCB(defense.startersCB));
            }
        }

        for (int i = 0 + x; i < defense.startersS + x; ++i) {
            if (defense.getS(i).gameFatigue > 0) {
                defense.getS(i).gameSim = Math.pow(defense.getS(i).getRatRunStop(), 1) * Math.random();
                defense.getS(i).gameSnaps++;
                Safety.add(defense.getS(i));
            } else {
                defense.getS(defense.startersS).gameSim = Math.pow(defense.getS(defense.startersS).getRatRunStop(), 1) * Math.random();
                defense.getS(defense.startersS).gameSnaps++;
                Safety.add(defense.getS(defense.startersS));
            }
        }

        //Rank Players
        Collections.sort(TightEnd, new CompGamePlayerPicker());
        Collections.sort(RunningBack, new CompGamePlayerPicker());
        Collections.sort(rusher, new CompGamePlayerPicker());
        Collections.sort(Linebacker, new CompGamePlayerPicker());
        Collections.sort(DLineman, new CompGamePlayerPicker());
        Collections.sort(Safety, new CompGamePlayerPicker());


        //Choose Action Players
        selQB = offense.getQB(0 + x);
        selRB = RunningBack.get(0);
        selTE = offense.getTE(0 + x);
        selDL = DLineman.get(0);
        selLB = Linebacker.get(0);
        selCB = Cornerback.get(0);
        selS = Safety.get(0);
        selS2 = Safety.size() > 1 ? Safety.get(1) : Safety.get(0);

        if (selTE.gameFatigue <= 0) selTE = offense.getTE(offense.startersTE);
        if (selS.gameFatigue <= 0) selS = defense.getS(defense.startersS);

        selQB.gameSnaps++;
        selTE.gameSnaps++;

        //Fatigue
        selRB.gameFatigue -= fatigueDropMed + Math.round((100 - selRB.ratDurability) / 10);
        selTE.gameFatigue -= fatigueDropMed + Math.round((100 - selTE.ratDurability) / 10);
        selDL.gameFatigue -= fatigueDropHigh + Math.round((100 - selDL.ratDurability) / 10);
        selLB.gameFatigue -= fatigueDropSuper + Math.round((100 - selLB.ratDurability) / 10);
        selCB.gameFatigue -= Math.round((100 - selCB.ratDurability) / 10);
        selS.gameFatigue -= fatigueDropMed + Math.round((100 - selS.ratDurability) / 10);
        selS2.gameFatigue -= Math.round((100 - selS2.ratDurability) / 10);

        rushPlay(offense, defense, selQB, selRB, selTE, selDL, selLB, selCB, selS, selS2);

        if (Math.random() > injuryFreq) checkInjury(selQB, offense);
        if (Math.random() > injuryFreq) checkInjury(selRB, offense);
        if (Math.random() > injuryFreq) checkInjury(selTE, offense);
        if (Math.random() > injuryFreq) checkInjury(selDL, defense);
        if (Math.random() > injuryFreq) checkInjury(selLB, defense);
        if (Math.random() > injuryFreq) checkInjury(selCB, defense);
        if (Math.random() > injuryFreq) checkInjury(selS, defense);
        if (Math.random() > injuryFreq) checkInjury(selS2, defense);

    }

    //PASSING PLAY - POST-SNAP

    private void passingPlay(Team offense, Team defense, PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerDL selDL, PlayerLB selLB, PlayerLB selLB2, PlayerCB selCB, PlayerS selS, PlayerS selS2, String pos) {
        int yardsGain = 0;
        boolean gotTD = false;
        boolean gotFumble = false;

        int offProtection = getOffPassProtection(offense, selTE);
        int defPressure = getDefPassPressure(defense, selLB2);

        //get how much pressure there is on qb, check if sack
        int pressureOnQB = 2 * defPressure - offProtection + getHFadv() + getCoachAdv();
        // SACK OUTCOME
        if (Math.random() * sackValue < pressureOnQB / 8) {

            if (Math.random() * escapeValue < pressureOnQB / 8 && selQB.getRatSpeed() > selDL.getRatPassRush()) {
                //ESCAPE SACK
                selQB.gameSim = 1;
                selRB.gameSim = 0;
                rushPlay(offense, defense, selQB, selRB, selTE, selDL, selLB, selCB, selS2, selS);
            } else {
                //sacked!
                selDL.gameSim = selDL.getRatTackle() * Math.random() * 100;
                selLB2.gameSim = selLB2.getRatTackle() * Math.random() * 60;
                selS2.gameSim = selS2.getRatTackle() * Math.random() * 25;

                recordSack(offense, defense, selQB, selDL, selLB2, selCB, selS2);

                return;
            }

        } else {

            //Throw  Ball
            recordPassAttempt(selQB, selRB, selWR, selTE, selLB, selCB, pos);

            //check for int
            if (!pos.equals("RB")) {
                double intChance = (pressureOnQB + defense.getS(0).ratOvr - (2 * selQB.getRatPassAcc() + selQB.ratIntelligence + 100) / 4.0) / 18.0
                        - offense.getPlaybookOffense().getPassProtection() + defense.getPlaybookDefense().getPassRush();
                intChance += getArchetypeIntBonus(defense.getS(0), intChance);
                if (intChance < 0.015) intChance = 0.015;
                if (intValue * Math.random() < intChance) {
                    //Interception
                    if (pos.equals("WR")) {
                        selDL.gameSim = selDL.getRatPassRush() * Math.random() * 15;
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 100;
                        selS.gameSim = selS.getRatCoverage() * Math.random() * 50;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 30;
                    } else if (pos.equals("TE")) {
                        selDL.gameSim = selDL.getRatPassRush() * Math.random() * 15;
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 50;
                        selS2.gameSim = selS2.getRatCoverage() * Math.random() * 45;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 65;
                    } else {
                        selDL.gameSim = selDL.getRatPassRush() * Math.random() * 15;
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 80;
                        selS.gameSim = selS.getRatCoverage() * Math.random() * 50;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 65;
                    }

                    recordInterception(offense, selQB, selDL, selLB, selCB, selS, pos);

                    return;
                }
            }

            //Check for completion
            double completion, coverage;

            if (pos.equals("WR")) {
                completion = getHFadv() + getCoachAdv() + 2 * offense.getPlaybookOffense().getPassProtection() + 4 * offense.getPlaybookOffense().getPassPref() +
                        1.5 * (selQB.getRatPassAcc()) + (selWR.getRatCatch()) + getArchetypeCompletionBonus(selQB, pressureOnQB);

                coverage = 2 * defense.getPlaybookDefense().getPassRush() + 4 * defense.getPlaybookDefense().getPassCoverage() + (selCB.getRatCoverage()) + pressureOnQB
                        + getArchetypeDeflectionBonus(selCB) + getArchetypePressBonus(selCB) + getArchetypeDeepRecoveryBonus(selCB);

            } else if (pos.equals("TE")) {
                completion = getHFadv() + getCoachAdv() + 2 * offense.getPlaybookOffense().getPassProtection() + 4 * offense.getPlaybookOffense().getPassPref() +
                        1.5 * (selQB.getRatPassAcc()) + (selTE.getRatCatch()) + getArchetypeCompletionBonus(selQB, pressureOnQB);

                coverage = 2 * defense.getPlaybookDefense().getPassRush() + 4 * defense.getPlaybookDefense().getPassCoverage() + (selLB.getRatCoverage()) + pressureOnQB
                        + getArchetypeCoverageBonus(selLB);

            } else {
                completion = getHFadv() + getCoachAdv() + 2 * offense.getPlaybookOffense().getPassProtection() + 4 * offense.getPlaybookOffense().getPassPref() +
                        1.5 * (selQB.getRatPassAcc()) + (selRB.getRatCatch()) + getArchetypeCompletionBonus(selQB, pressureOnQB);

                coverage = 2 * defense.getPlaybookDefense().getPassRush() + 4 * defense.getPlaybookDefense().getPassCoverage() + (selLB2.getRatCoverage()) + pressureOnQB
                        + getArchetypeCoverageBonus(selLB2);
            }

            if (coverage * Math.random() > completion * Math.random()) {
                if (pos.equals("WR")) {
                    if (100 * Math.random() < (100 - selWR.getRatCatch() - getArchetypeDropReduction(selWR)) / 3) {
                        //drop
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" WR ").append(selWR.name).append(" dropped the catch.");

                        gameDown++;
                        recordDrop(selRB, selTE, selWR, selCB, selLB, pos);
                        //Drop ball = inc pass, so run time for the play, stop clock until next play, move on (aka return;)

                        gameTime -= timePerPlay * Math.random();
                        return;
                    }
                }
                if (pos.equals("TE")) {
                    if (100 * Math.random() < (100 - selTE.getRatCatch() - getArchetypeDropReduction(selTE)) / 3) {
                        //drop
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append("TE ").append(selTE.name).append(" dropped the catch.");

                        gameDown++;
                        recordDrop(selRB, selTE, selWR, selCB, selLB, pos);
                        //Drop ball = inc pass, so run time for the play, stop clock until next play, move on (aka return;)

                        gameTime -= timePerPlay * Math.random();
                        return;
                    }
                }
                if (pos.equals("RB")) {
                    if (100 * Math.random() < (100 - selRB.getRatCatch() - getArchetypeDropReduction(selRB)) / 3) {
                        //drop
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" RB ").append(selRB.name).append(" dropped the catch.");

                        gameDown++;
                        recordDrop(selRB, selTE, selWR, selCB, selLB, pos);
                        //Drop ball = inc pass, so run time for the play, stop clock until next play, move on (aka return;)

                        gameTime -= timePerPlay * Math.random();
                        return;
                    }
                }


                //no completion, advance downs
                if (homeTeam.league.fullGameLog)
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" QB ").append(selQB.name).append(" threw an incomplete pass to the ").append(pos).append(".");
                gameDown++;
                //Incomplete pass stops the clock, so just run time for how long the play took, then move on


                gameTime -= timePerPlay * Math.random();
                if (pos.equals("WR")) {
                    recordDefendedCB(selWR, selCB);
                } else if (pos.equals("TE")) {
                    recordDefendedLB(selTE, selLB2);
                } else if (pos.equals("RB")) {
                    recordDefendedLB2(selRB, selLB2);
                }
                return;


            } else {

                //COMPLETED PASS
                double escapeChance;

                if (pos.equals("WR")) {

                    yardsGain = (int) (((selQB.getRatPassPow()) + (selWR.getRatSpeed()) - (selCB.getRatSpeed())) * Math.random() / 4.8 //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage());
                    //see if receiver can get yards after catch
                    int wrYacBonus = getArchetypeYacBonus(selWR) + (selWR.hasArchetype(Archetypes.WR_DEEP_THREAT) ? 10 : 0);
                    escapeChance = ((selWR.getRatEvasion()) * 3 - selCB.getRatTackle() - selS.getRatTackle()) * Math.random()   //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage()
                            + wrYacBonus - getArchetypeDeepRecoveryBonus(selCB);
                } else if (pos.equals("TE")) {

                    yardsGain = (int) (((selQB.getRatPassPow()) + (selTE.getRatSpeed()) - (selLB.getRatSpeed())) * Math.random() / 4.8 //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage());
                    //see if receiver can get yards after catch
                    escapeChance = ((selTE.getRatEvasion()) * 3 - selLB.getRatTackle() - defense.getS(0).ratOvr) * Math.random()  //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage()
                            + getArchetypeYacBonus(selTE);
                } else {

                    yardsGain = (int) (((selQB.getRatPassPow()) + (selRB.getRatSpeed()) - (selLB.getRatSpeed())) * Math.random() / 4.8 //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage()) - 2;  //subtract 2 for screen pass behind line of scrimmage
                    //see if receiver can get yards after catch
                    escapeChance = ((selRB.getRatEvasion()) * 3 - selLB2.getRatTackle() - defense.getS(0).ratOvr) * Math.random()  //STRATEGIES
                            + offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage()
                            + getArchetypeYacBonus(selRB);
                }

                //BIG GAIN
                if (escapeChance > 92 || Math.random() > 0.95) {
                    if (pos.equals("WR")) {
                        yardsGain += 3 + (selWR.getRatSpeed() * Math.random() / 4);
                    } else if (pos.equals("TE")) {
                        yardsGain += 3 + (selTE.getRatSpeed() * Math.random() / 4);
                    } else {
                        yardsGain += 4 + (selRB.getRatSpeed() * Math.random() / 4);
                    }
                }

                //BREAK AWAY FOR TD
                if (escapeChance > 80 && Math.random() < (0.1 + (offense.getPlaybookOffense().getPassPotential() - defense.getPlaybookDefense().getPassCoverage()) / 200)) {
                    yardsGain += 100;
                }

                //add yardage
                gameYardLine += yardsGain;
                if (gameYardLine >= 100) { //TD!
                    yardsGain -= gameYardLine - 100;
                    gameYardLine = 100 - yardsGain;
                    addPointsQuarter(6);
                    recordPassingTD(offense, selQB, selRB, selWR, selTE, yardsGain, pos);
                    gotTD = true;
                } else {
                    //check for fumble
                    double fumChance = (selS.getRatTackle() + selCB.getRatTackle() + selLB.getRatTackle()) / 3;
                    if (100 * Math.random() < fumChance / 50) {
                        //Fumble!
                        gotFumble = true;
                    }
                }

                if (!gotTD && !gotFumble) {
                    //check downs if there wasnt fumble or TD
                    if (homeTeam.league.fullGameLog) {
                        if (pos.equals("WR")) {
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" WR ").append(selWR.name).append(" caught the pass for a gain of ").append(yardsGain).append(" yards.");
                        } else if (pos.equals("RB")) {
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" RB ").append(selRB.name).append(" caught the pass for a gain of ").append(yardsGain).append(" yards.");
                        } else if (pos.equals("TE")) {
                            gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" TE ").append(selTE.name).append(" caught the pass for a gain of ").append(yardsGain).append(" yards.");
                        }
                    }

                    gameYardsNeed -= yardsGain;


                    if (pos.equals("WR")) {
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 100;
                        selS.gameSim = selS.getRatCoverage() * Math.random() * 60;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 40;
                    } else if (pos.equals("TE")) {
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 40;
                        selS.gameSim = selS.getRatCoverage() * Math.random() * 60;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 80;
                    } else {
                        selCB.gameSim = selCB.getRatCoverage() * Math.random() * 30;
                        selS.gameSim = selS.getRatCoverage() * Math.random() * 40;
                        selLB.gameSim = selLB.getRatCoverage() * Math.random() * 60;
                    }

                    if (gameYardsNeed <= 0) {
                        // Only set new down and distance if there wasn't a TD
                        gameDown = 1;
                        gameYardsNeed = 10;
                        if (homeTeam.league.fullGameLog) gameEventLog.append("\nFIRST DOWN!");

                    } else gameDown++;
                }

                //stats management
                recordPassCompletion(offense, selQB, selRB, selWR, selTE, selLB, selCB, selS, yardsGain, pos, gotTD);
            }

        }


        if (gotFumble) {
            String defender;
            if (pos.equals("WR")) {
                selDL.gameSim = selDL.getRatTackle() * Math.random() * 15;
                selCB.gameSim = selCB.getRatTackle() * Math.random() * 100;
                selS.gameSim = selS.getRatTackle() * Math.random() * 60;
                selLB.gameSim = selLB.getRatTackle() * Math.random() * 40;
            } else if (pos.equals("TE")) {
                selDL.gameSim = selDL.getRatTackle() * Math.random() * 15;
                selCB.gameSim = selCB.getRatTackle() * Math.random() * 50;
                selS.gameSim = selS.getRatTackle() * Math.random() * 55;
                selLB.gameSim = selLB.getRatTackle() * Math.random() * 80;
            } else {
                selDL.gameSim = selDL.getRatTackle() * Math.random() * 40;
                selCB.gameSim = selCB.getRatTackle() * Math.random() * 30;
                selS.gameSim = selS.getRatTackle() * Math.random() * 35;
                selLB.gameSim = selLB.getRatTackle() * Math.random() * 65;
            }
            recordRecFumble(offense, selRB, selWR, selTE, selDL, selLB, selCB, selS, pos);

            if (gamePoss) { // home possession
                homeTOs++;
            } else {
                awayTOs++;
            }
            if (!playingOT) {
                gameDown = 1;
                gameYardsNeed = 10;
                gamePoss = !gamePoss;
                gameYardLine = 100 - gameYardLine;
                gameTime -= timePerPlay * Math.random();
                return;
            } else {
                resetForOT();
                return;
            }
        }

        if (gotTD) {
            gameTime -= timePerPlay * Math.random();
            kickXP(offense, defense);
            if (!playingOT) kickOff(offense, defense);
            else resetForOT();
            return;
        }

        gameTime -= timePerPlay + timePerPlay * Math.random();

    }

    //RUSHING PLAYS POST-SNAP **

    private void rushPlay(Team offense, Team defense, PlayerQB selQB, PlayerRB selRB, PlayerTE selTE, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, Player selS2) {
        boolean gotTD;
        gotTD = false;
        int yardsGain;
        int blockAdv = getOffRunProtection(offense, selTE) - getDefRunStop(defense, selLB, selS) + (offense.getPlaybookOffense().getRunProtection() - defense.getPlaybookDefense().getRunStop());

        //Start Rush Play
        if (selRB.gameSim >= selQB.gameSim) {
            yardsGain = (int) ((selRB.getRatSpeed() + blockAdv + getHFadv() + (int) (Math.random() * getCoachAdv())) * Math.random() / 10 + (double) offense.getPlaybookOffense().getRunPotential() / 2 - (double) defense.getPlaybookDefense().getRunCoverage() / 2)
                    + getArchetypeRushBonus(selRB);
        } else {
            yardsGain = (int) ((selQB.getRatSpeed() + blockAdv + getHFadv() + (int) (Math.random() * getCoachAdv())) * Math.random() / 10 + (double) offense.getPlaybookOffense().getRunPotential() / 2 - (double) defense.getPlaybookDefense().getRunCoverage() / 2)
                    + getArchetypeScrambleBonus(selQB);
        }

        //Break past neutral zone
        if (selRB.gameSim >= selQB.gameSim) {
            if (yardsGain < 2) {
                yardsGain += selRB.getRatRushPower() / 20 - 3 - (double) defense.getPlaybookDefense().getRunCoverage() / 2;
            } else {
                //break free from tackles
                if (Math.random() < (0.28 + (offense.getPlaybookOffense().getRunPotential() - (double) defense.getPlaybookDefense().getRunCoverage() / 2) / 50)) {
                    yardsGain += (selRB.getRatEvasion() - blockAdv) / 5 * Math.random()
                            + getArchetypeBrokenTackleBonus(selRB);
                }
            }
        } else {
            if (yardsGain < 2) {
                yardsGain += selQB.getRatEvasion()/ 20 - 3 - (double) defense.getPlaybookDefense().getRunCoverage() / 2;
            } else {
                //break free from tackles
                if (Math.random() < (0.20 + (offense.getPlaybookOffense().getRunPotential() - (double) defense.getPlaybookDefense().getRunCoverage() / 2) / 50)) {
                    yardsGain += (selQB.getRatEvasion()- blockAdv) / 5 * Math.random();
                }
            }
        }

        //add yardage
        gameYardLine += yardsGain;

        if (gameYardLine >= 100) { //TD!
            addPointsQuarter(6);
            yardsGain -= gameYardLine - 100;
            gameYardLine = 100 - yardsGain;
            gotTD = true;
        }

        //stats management
        recordRushAttempt(offense, selQB, selRB, selDL, selLB, selCB, selS, yardsGain, gotTD);

        //check downs if there wasn't TD
        if (!gotTD) {
            //check downs
            gameYardsNeed -= yardsGain;
            if (gameYardsNeed <= 0) {
                // Only set new down and distance if there wasn't a TD
                gameDown = 1;
                gameYardsNeed = 10;
                if (homeTeam.league.fullGameLog) gameEventLog.append("\nFIRST DOWN!");
            } else gameDown++;
        }


        if (gotTD) {
            gameTime -= 5 + timePerPlay * Math.random(); // Clock stops for the TD, just burn time for the play
            kickXP(offense, defense);
            if (!playingOT) kickOff(offense, defense);
            else resetForOT();
        } else {
            gameTime -= timePerPlay + timePerPlay * Math.random();
            //check for fumble
            double fumChance = ((defense.getS(0).getRatTackle() + selLB.getRatTackle()) / 2 + defense.getCompositeDLRush() - getHFadv()) / 2 + offense.getPlaybookOffense().getRunProtection();  //STRATEGIES
            if (100 * Math.random() < fumChance / 50) {
                //Fumble!

                if (yardsGain < 5) {
                    selDL.gameSim = selDL.getRatTackle() * Math.random() * 80;
                    selCB.gameSim = selCB.getRatTackle() * Math.random() * 20;
                    selS.gameSim = selS.getRatTackle() * Math.random() * 20;
                    selLB.gameSim = selLB.getRatTackle() * Math.random() * 60;
                } else {
                    selDL.gameSim = selDL.getRatTackle() * Math.random() * 20;
                    selCB.gameSim = selCB.getRatTackle() * Math.random() * 25;
                    selS.gameSim = selS.getRatTackle() * Math.random() * 50;
                    selLB.gameSim = selLB.getRatTackle() * Math.random() * 75;
                }

                recordRushFumble(offense, selQB, selRB, selDL, selLB, selCB, selS);

                if (!playingOT) {
                    gameDown = 1;
                    gameYardsNeed = 10;
                    gamePoss = !gamePoss;
                    gameYardLine = 100 - gameYardLine;
                } else resetForOT();
            }
        }
    }


    //ARCHETYPE GAMEPLAY BONUSES

    private int getArchetypeCompletionBonus(PlayerQB qb, int pressureOnQB) {
        if (qb == null) return 0;
        if (qb.hasArchetype(Archetypes.QB_POCKET) && pressureOnQB < 20) return 10;
        return 0;
    }

    private int getArchetypeScrambleBonus(PlayerQB qb) {
        if (qb == null) return 0;
        if (qb.hasArchetype(Archetypes.QB_SCRAMBLER)) return 1;
        return 0;
    }

    private int getArchetypeRushBonus(PlayerRB rb) {
        if (rb == null) return 0;
        if (rb.hasArchetype(Archetypes.RB_SPEED)) return 1;
        return 0;
    }

    private int getArchetypeDropReduction(Player p) {
        if (p == null || p.archetypeTag == null) return 0;
        if (p.hasArchetype(Archetypes.RB_RECEIVING)) return 10;
        if (p.hasArchetype(Archetypes.TE_RECEIVING)) return 10;
        if (p.hasArchetype(Archetypes.WR_ROUTE_RUNNER)) return 10;
        return 0;
    }

    private int getArchetypeYacBonus(Player p) {
        if (p == null || p.archetypeTag == null) return 0;
        if (p.hasArchetype(Archetypes.WR_SLOT)) return 5;
        return 0;
    }

    private int getArchetypeDeflectionBonus(PlayerCB cb) {
        if (cb == null) return 0;
        if (cb.hasArchetype(Archetypes.CB_SHUTDOWN)) return 10;
        return 0;
    }

    private double getArchetypeIntBonus(PlayerS s, double intChance) {
        if (s == null) return 0;
        if (s.hasArchetype(Archetypes.S_BALL_HAWK)) return intChance * 0.20;
        return 0;
    }

    private int getArchetypePassRushBonus(PlayerDL dl) {
        if (dl == null) return 0;
        if (dl.hasArchetype(Archetypes.DL_PASS_RUSHER)) return 3;
        return 0;
    }

    private int getArchetypeRunStopBonus(PlayerDL dl) {
        if (dl == null) return 0;
        if (dl.hasArchetype(Archetypes.DL_RUN_STOPPER)) return 3;
        return 0;
    }

    private int getArchetypeRunStopBonus(PlayerS s) {
        if (s == null) return 0;
        if (s.hasArchetype(Archetypes.S_RUN_SUPPORT)) return 3;
        return 0;
    }

    private int getArchetypePassProtectBonus(PlayerOL ol) {
        if (ol == null) return 0;
        if (ol.hasArchetype(Archetypes.OL_PASS_PROTECTOR)) return 3;
        return 0;
    }

    private int getArchetypeRunBlockBonus(PlayerOL ol) {
        if (ol == null) return 0;
        if (ol.hasArchetype(Archetypes.OL_RUN_BLOCKER)) return 3;
        if (ol.hasArchetype(Archetypes.OL_MAULER)) return 2;
        return 0;
    }

    private int getArchetypeCoverageBonus(PlayerLB lb) {
        if (lb == null) return 0;
        if (lb.hasArchetype(Archetypes.LB_COVERAGE)) return 3;
        return 0;
    }

    private int getArchetypeBlitzBonus(PlayerLB lb) {
        if (lb == null) return 0;
        if (lb.hasArchetype(Archetypes.LB_BLITZER)) return 3;
        return 0;
    }

    private int getArchetypeDeepRecoveryBonus(PlayerCB cb) {
        if (cb == null) return 0;
        if (cb.hasArchetype(Archetypes.CB_SPEED)) return 5;
        return 0;
    }

    private int getArchetypeBrokenTackleBonus(PlayerRB rb) {
        if (rb == null) return 0;
        if (rb.hasArchetype(Archetypes.RB_POWER)) return 3;
        return 0;
    }

    private int getArchetypePressBonus(PlayerCB cb) {
        if (cb == null) return 0;
        if (cb.hasArchetype(Archetypes.CB_PHYSICAL)) return 3;
        return 0;
    }

    private int getArchetypeFgRangeBonus(PlayerK k) {
        if (k == null) return 0;
        if (k.hasArchetype(Archetypes.K_POWER)) return 5;
        return 0;
    }

    private int getArchetypeFgAccBonus(PlayerK k, int yardLine) {
        if (k == null) return 0;
        int fgDist = 110 - yardLine;
        if (k.hasArchetype(Archetypes.K_ACCURATE) && fgDist >= 40) return 10;
        return 0;
    }

    //PLAY CHARACTERISTICS

    //PASS PROTECTION
    private int getOffPassProtection(Team off, PlayerTE TE) {
        int OP = 0;
        if (off.getPlaybookOffense().getPassUsage() > 0) OP = getCompositeOLPassTE(TE);
        else OP = getCompositeOLPass();

        return OP;
    }

    private int getCompositeOLPass() {
        int compositeOL = 0;
        for (int i = 0; i < Math.min(5, teamOLs.size()); ++i) {
            compositeOL += (teamOLs.get(i).getRatStrength() * 2 + teamOLs.get(i).getRatPassBlock() * 2 + teamOLs.get(i).getRatVision()) / 5;
            compositeOL += getArchetypePassProtectBonus(teamOLs.get(i));
        }
        compositeOL = compositeOL / 5;

        return (compositeOL);
    }

    private int getCompositeOLPassTE(PlayerTE selTE) {
        int compositeOL = 0;
        for (int i = 0; i < Math.min(5, teamOLs.size()); ++i) {
            compositeOL += (teamOLs.get(i).getRatStrength() * 2 + teamOLs.get(i).getRatPassBlock() * 2 + teamOLs.get(i).getRatVision()) / 5;
            compositeOL += getArchetypePassProtectBonus(teamOLs.get(i));
        }
        compositeOL += selTE.getRatRunBlock();
        compositeOL = (int) (compositeOL / 5.5);

        return (compositeOL);
    }

    //RUN BLOCKING
    private int getOffRunProtection(Team off, PlayerTE TE) {
        int OP = 0;
        if (off.getPlaybookOffense().getRunUsage() == 0) OP = getCompositeOLRush();
        else OP = getCompositeOLRushTE(TE);

        return OP;
    }

    private int getCompositeOLRush() {
        int compositeOL = 0;
        for (int i = 0; i < Math.min(5, teamOLs.size()); ++i) {
            compositeOL += (teamOLs.get(i).getRatStrength() * 2 + teamOLs.get(i).getRatRunBlock() * 2 + teamOLs.get(i).getRatVision()) / 5;
            compositeOL += getArchetypeRunBlockBonus(teamOLs.get(i));
        }
        compositeOL = compositeOL / 5;

        return compositeOL;
    }

    private int getCompositeOLRushTE(PlayerTE selTE) {
        int compositeOL = 0;
        for (int i = 0; i < Math.min(5, teamOLs.size()); ++i) {
            compositeOL += (teamOLs.get(i).getRatStrength() * 2 + teamOLs.get(i).getRatRunBlock() * 2 + teamOLs.get(i).getRatVision()) / 5;
            compositeOL += getArchetypeRunBlockBonus(teamOLs.get(i));
        }
        compositeOL += selTE.getRatRunBlock();
        compositeOL = (int) (compositeOL / 5.5); //TE bonus

        return compositeOL;
    }

    //PASS PRESSURE
    private int getDefPassPressure(Team def, PlayerLB LB) {
        int DP = 0;
        if (def.getPlaybookDefense().getPassSpy() == 0) DP = getCompositeDLPass();
        else DP = getCompositeDLPassLB(LB);

        return DP;
    }

    private int getCompositeDLPass() {
        int compositeDL = 0;
        for (int i = 0; i < 4; ++i) {
            compositeDL += (teamDLs.get(i).getRatStrength() + teamDLs.get(i).getRatPassRush()) / 2;
            compositeDL += getArchetypePassRushBonus(teamDLs.get(i));
        }
        compositeDL = compositeDL / 4;

        return compositeDL;
    }

    private int getCompositeDLPassLB(PlayerLB selLB) {
        int compositeDL = 0;
        for (int i = 0; i < 4; ++i) {
            compositeDL += (teamDLs.get(i).getRatStrength() + teamDLs.get(i).getRatPassRush()) / 2;
            compositeDL += getArchetypePassRushBonus(teamDLs.get(i));
        }
        compositeDL += (selLB.getRatSpeed() + selLB.getRatTackle()) / 2;
        compositeDL += getArchetypeBlitzBonus(selLB);
        compositeDL = (int) (compositeDL / 4.58);  // 4.58 is equal to 5.5 for OL + TE

        return compositeDL;
    }

    //DEFENDING THE RUN
    private int getDefRunStop(Team def, PlayerLB LB, PlayerS S) {
        int DP = 0;
        if (def.getPlaybookDefense().getRunSpy() == 0) DP = getCompositeDLRush(LB);
        else DP = getCompositeDLRush(LB, S);

        return DP;
    }

    private int getCompositeDLRush(PlayerLB selLB) {
        int compositeDL = 0;
        for (int i = 0; i < 4; ++i) {
            compositeDL += (teamDLs.get(i).getRatStrength() + teamDLs.get(i).getRatRunStop()) / 2;
            compositeDL += getArchetypeRunStopBonus(teamDLs.get(i));
        }
        compositeDL += selLB.getRatRunStop();
        compositeDL = compositeDL / 5;

        return compositeDL;
    }

    private int getCompositeDLRush(PlayerLB selLB, PlayerS selS) {
        int compositeDL = 0;
        for (int i = 0; i < 4; ++i) {
            compositeDL += (teamDLs.get(i).getRatStrength() + teamDLs.get(i).getRatRunStop()) / 2;
            compositeDL += getArchetypeRunStopBonus(teamDLs.get(i));
        }
        compositeDL += selLB.getRatRunStop() + selS.getRatRunStop();
        compositeDL += getArchetypeRunStopBonus(selS);
        compositeDL = (int) (compositeDL / 5.5);

        return compositeDL;
    }


    //KICKING PLAYS

    private void fieldGoalAtt(Team offense, Team defense) {
        PlayerK selK = offense.getK(0);
        if (selK == null) {
            // No kicker available — turn the ball over at the attempt spot.
            gameEventLog.append(getEventLog()).append(offense.getAbbr())
                    .append(" has no kicker available — field goal attempt aborted.");
            if (!playingOT) {
                gameYardLine = Math.max(100 - gameYardLine, 20);
                gameDown = 1;
                gameYardsNeed = 10;
                gamePoss = !gamePoss;
            } else {
                resetForOT();
            }
            return;
        }
        selK.gameSnaps++;
        gameYardLine -= 7;


        double fgDistRatio = Math.pow((110 - gameYardLine) / 50, 2);
        double fgAccRatio = Math.pow((110 - gameYardLine) / 50, 1.25);
        double fgDistChance = (getHFadv() + selK.getRatKickPow() - fgDistRatio * 80) + getArchetypeFgRangeBonus(selK);
        double fgAccChance = (getHFadv() + selK.getRatKickAcc() - fgAccRatio * 80) + getArchetypeFgAccBonus(selK, gameYardLine);

        if (gameTime > 120 && !playingOT) {
            if (fgDistChance > 20 && fgAccChance * Math.random() > 15) {
                // made the fg
                if (gamePoss) { // home possession
                    homeScore += 3;
                } else {
                    awayScore += 3;
                }
                gameEventLog.append(getEventLogScoring()).append(offense.getAbbr()).append(" K ").append(selK.name).append(" made the ").append(110 - gameYardLine).append(" yard FG.");
                addPointsQuarter(3);

                selK.recordFGAtt(1);
                selK.recordFGMade(1);
                selK.gameFGMade++;
                selK.gameFGAttempts++;

                if (!playingOT) { kickOff(offense, defense); return; }
                else { resetForOT(); return; }

            } else {
                //miss
                gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(selK.name).append(" missed the ").append(110 - gameYardLine).append(" yard FG.");
                selK.recordFGAtt(1);
                selK.gameFGAttempts++;
                if (!playingOT) {
                    gameYardLine = Math.max(100 - gameYardLine, 20);
                    gameDown = 1;
                    gameYardsNeed = 10;
                    gamePoss = !gamePoss;
                } else resetForOT();
            }
        } else {
            if (fgDistChance > 20 && fgAccChance * Math.random() > 15 && selK.getRatKickPressure() > Math.random() * 95) {
                // made the fg
                if (gamePoss) { // home possession
                    homeScore += 3;
                } else {
                    awayScore += 3;
                }
                gameEventLog.append(getEventLogScoring()).append(offense.getAbbr()).append(" K ").append(selK.name).append(" made the ").append(110 - gameYardLine).append(" yard FG.");
                addPointsQuarter(3);
                selK.recordFGMade(1);
                selK.recordFGAtt(1);
                selK.gameFGMade++;
                selK.gameFGAttempts++;

                if (!playingOT) { kickOff(offense, defense); return; }
                else { resetForOT(); return; }

            } else {
                //miss
                gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(selK.name).append(" missed the ").append(110 - gameYardLine).append(" yard FG.");
                selK.recordFGAtt(1);
                if (!playingOT) {
                    gameYardLine = Math.max(100 - gameYardLine, 20); //Misses inside the 20 = defense takes over on the 20
                    gameDown = 1;
                    gameYardsNeed = 10;
                    selK.gameFGAttempts++;
                    gamePoss = !gamePoss;
                } else resetForOT();
            }
        }

        gameTime -= 20;

    }

    private void kickXP(Team offense, Team defense) {
        PlayerK selK = offense.getK(0);
        if (selK != null) {
            selK.gameSnaps++;
        }

        // No XP/2pt try if the TD puts the bottom OT offense ahead (aka wins the game)
        if (playingOT && bottomOT && (((numOT % 2 == 0) && awayScore > homeScore) || ((numOT % 2 != 0) && homeScore > awayScore))) {
            gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append("\n").append(offense.getAbbr()).append(" wins on a walk-off touchdown!");
        }
        // If a TD is scored as time expires, there is no XP/2pt if the score difference is greater than 2
        else if (!playingOT && gameTime <= 0 && ((homeScore - awayScore > 2) || (awayScore - homeScore > 2))) {
            //Walkoff TD!
            if ((Math.abs(homeScore - awayScore) < 7) && ((gamePoss && homeScore > awayScore) || (!gamePoss && awayScore > homeScore)))
                gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append("\n").append(offense.getAbbr()).append(" wins on a walk-off touchdown!");
                //Just rubbing in the win or saving some pride
            else gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo);
        } else {
            if ((numOT >= 3) || (((gamePoss && (awayScore - homeScore) == 2) || (!gamePoss && (homeScore - awayScore) == 2)) && gameTime < 300)) {
                //go for 2
                boolean successConversion = false;
                PlayerRB rushBack = offense.getRB(0);
                PlayerQB qb = offense.getQB(0);
                PlayerWR wr = offense.getWR(0);
                PlayerCB cb = defense.getCB(0);
                if (Math.random() <= 0.50) {
                    //rushing
                    if (rushBack == null) {
                        gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo)
                                .append(" 2pt conversion failed — no available rusher.");
                    } else {
                        int blockAdv = (int) offense.getCompositeOLRush() - (int) defense.getCompositeDLRush();
                        int yardsGain = (int) ((rushBack.getRatSpeed() + blockAdv) * Math.random() / 6);
                        if (yardsGain > 5) {
                            successConversion = true;
                            if (gamePoss) { // home possession
                                homeScore += 2;
                            } else {
                                awayScore += 2;
                            }
                            addPointsQuarter(2);
                            gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(rushBack.name).append(" rushed for the 2pt conversion.");
                        } else {
                            gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(rushBack.name).append(" stopped at the line of scrimmage, failed the 2pt conversion.");
                        }
                    }
                } else if (qb == null || wr == null || cb == null) {
                    gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo)
                            .append(" 2pt conversion failed — depleted skill-position depth.");
                } else {
                    int pressureOnQB = (int) defense.getCompositeDLPass() * 2 - (int) offense.getCompositeOLPass();
                    double completion = ((qb.getRatPassAcc()) + wr.getRatCatch() - cb.getRatCoverage()) / 2 + 25 - pressureOnQB / 20;
                    if (100 * Math.random() < completion) {
                        successConversion = true;
                        if (gamePoss) { // home possession
                            homeScore += 2;
                        } else {
                            awayScore += 2;
                        }
                        addPointsQuarter(2);
                        gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(qb.name).append(" completed the pass to ").append(wr.name).append(" for the 2pt conversion.");
                    } else {
                        gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(qb.name).append("'s pass incomplete to ").append(wr.name).append(" for the failed 2pt conversion.");
                    }
                }

            } else {
                //kick XP
                if (selK == null) {
                    gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo)
                            .append(" Extra point skipped — no kicker available.");
                } else if (Math.random() * 100 < 23 + selK.getRatKickAcc() && Math.random() > 0.01) {
                    //made XP
                    if (gamePoss) { // home possession
                        homeScore += 1;
                    } else {
                        awayScore += 1;
                    }
                    gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(selK.name).append(" made the XP.");
                    addPointsQuarter(1);
                    selK.recordXPMade(1);
                    selK.gameXPMade++;
                    selK.recordXPAtt(1);
                    selK.gameXPAttempts++;
                } else {
                    gameEventLog.append(getEventLogScoring()).append("TOUCHDOWN!\n").append(tdInfo).append(" ").append(selK.name).append(" missed the XP.");
                    selK.recordXPAtt(1);
                    selK.gameXPAttempts++;
                }
            }
        }
    }

    private void kickOff(Team offense, Team defense) {
        PlayerReturner returner = selectReturner();
        int specialTeams = getSpecialTeamsD(offense);
        PlayerK kicker = offense.getK(0);
        gameYardLine = 65;

        if (gameTime <= 0) return;
        else {
            //Decide whether to onside kick. Only if losing but within 8 points with < 3 min to go
            if (kicker != null && gameTime < 180 && ((gamePoss && (awayScore - homeScore) <= 8 && (awayScore - homeScore) > 0)
                    || (!gamePoss && (homeScore - awayScore) <= 8 && (homeScore - awayScore) > 0))) {
                // Yes, do onside
                if (kicker.getRatKickFum() * Math.random() > 60 || Math.random() < 0.1) {
                    //Success!
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(kicker.name).append(" successfully executes onside kick! ").append(offense.getAbbr()).append(" has possession!");
                } else {
                    // Fail
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(kicker.name).append(" failed the onside kick and lost possession.");
                    gamePoss = !gamePoss;
                }
                gameYardLine = (gameYardLine - 10) - (int) (10 * Math.random());
                gameDown = 1;
                gameYardsNeed = 10;

                gameTime -= 4 + 5 * Math.random(); //Onside kicks are very fast, unless there's a weird fight for the ball. Chance to burn a lot of time, odds are you'll burn a little time.
            } else {
                // Just regular kick off

                gameYardLine = returnPlay(gameYardLine, kicker, returner, specialTeams, true);

                gameDown = 1;
                gameYardsNeed = 10;
                gamePoss = !gamePoss;

                //Touchdown...
                if (gameYardLine >= 100) {
                    addPointsQuarter(6);
                    if (gamePoss) { // home possession
                        homeScore += 6;
                    } else {
                        awayScore += 6;
                    }
                    tdInfo = returner.team + " " + returner.position + " " + returner.name + " returns the kick " + returnYards + " yards for a TOUCHDOWN!";
                    returner.kTD++;
                    kickXP(defense, offense);
                    if (!playingOT) kickOff(defense, offense);
                    else resetForOT();
                } else {
                    if (gameYardLine <= 0) {
                        gameYardLine = touchback;
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append("\n\nKick-off!\n").append(returner.team).append(" ").append(returner.name).append(" lets it go for a touchback.");
                    } else {
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append("\n\nKick-off!\n").append(returner.team).append(" ").append(returner.name).append(" returns the kickoff to the ").append(gameYardLine).append(" yard line.");
                    }
                }
            }

            gameTime -= timePerPlay * Math.random();
        }
    }

    void freeKick(Team offense, Team defense) {
        if (gameTime <= 0) return;
        else {
            PlayerReturner returner = selectReturner();
            int specialTeams = getSpecialTeamsD(offense);
            PlayerK kicker = offense.getK(0);

            //Decide whether to onside kick. Only if losing but within 8 points with < 3 min to go
            if (kicker != null && gameTime < 180 && ((gamePoss && (awayScore - homeScore) <= 8 && (awayScore - homeScore) > 0)
                    || (!gamePoss && (homeScore - awayScore) <= 8 && (homeScore - awayScore) > 0))) {
                // Yes, do onside
                if (kicker.getRatKickFum() * Math.random() > 60 || Math.random() < 0.1) {
                    //Success!
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(kicker.name).append(" successfully executes onside kick! ").append(offense.getAbbr()).append(" has possession!");
                    gameYardLine = 35;
                    gameDown = 1;
                    gameYardsNeed = 10;
                } else {
                    // Fail
                    gameEventLog.append(getEventLog()).append(offense.getAbbr()).append(" K ").append(kicker.name).append(" failed the onside kick and lost possession.");
                    gamePoss = !gamePoss;
                    gameYardLine = 65;
                    gameDown = 1;
                    gameYardsNeed = 10;
                }

                gameTime -= 4 + 4 * Math.random(); //Onside kicks are very fast, unless there's a weird fight for the ball. Chance to burn a lot of time, odds are you'll burn a little time.
            } else {
                gameYardLine = 80;
                gameYardLine = returnPlay(gameYardLine, kicker, returner, specialTeams, true);

                gameDown = 1;
                gameYardsNeed = 10;
                gamePoss = !gamePoss;

                //Touchdown...
                if (gameYardLine >= 100) {
                    addPointsQuarter(6);
                    if (gamePoss) { // home possession
                        homeScore += 6;
                    } else {
                        awayScore += 6;
                    }
                    tdInfo = returner.team + " " + returner.position + " " + returner.name + " returns the kick " + returnYards + " yards for a TOUCHDOWN!";
                    returner.kTD++;
                    kickXP(defense, offense);
                    if (!playingOT) kickOff(defense, offense);
                    else resetForOT();
                } else {
                    if (gameYardLine <= 0) {
                        gameYardLine = touchback;
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append("\n\nFree-Kick!\n").append(returner.team).append(" ").append(returner.name).append(" lets it go for a touchback.");
                    } else {
                        if (homeTeam.league.fullGameLog)
                            gameEventLog.append("\n\nFree-Kick!\n").append(returner.team).append(" ").append(returner.name).append(" returns the free-kick to the ").append(gameYardLine).append(" yard line.");
                    }
                }

                gameTime -= timePerPlay * Math.random();

            }
        }
    }

    private void puntPlay(Team offense, Team defense) {
        PlayerReturner returner = selectReturner();
        int specialTeams = getSpecialTeamsD(offense);

        gameYardLine = returnPlay(gameYardLine, offense.getK(0), returner, specialTeams, false);
        gamePoss = !gamePoss;

        //Touchdown...
        if (gameYardLine >= 100) {
            addPointsQuarter(6);
            if (gamePoss) { // home possession
                homeScore += 6;
            } else {
                awayScore += 6;
            }
            tdInfo = returner.team + " " + returner.position + " " + returner.name + " returns the punt " + returnYards + " yards for a TOUCHDOWN!";
            returner.pTD++;
            kickXP(defense, offense);
            if (!playingOT) kickOff(defense, offense);
            else resetForOT();
        } else {
            if (gameYardLine <= 0) {
                gameYardLine = touchback;
                if (homeTeam.league.fullGameLog)
                    gameEventLog.append("\n\nPunt!\n").append(returner.team).append(" ").append(returner.name).append(" lets it go for a touchback.");
            } else {
                if (homeTeam.league.fullGameLog)
                    gameEventLog.append("\n\nPunt!\n").append(returner.team).append(" ").append(returner.name).append(" returns the punt to the ").append(gameYardLine).append(" yard line.");
            }
        }

        gameDown = 1;
        gameYardsNeed = 10;
        gameTime -= timePerPlay + timePerPlay * Math.random();
    }

    private PlayerReturner selectReturner() {
        if (gamePoss) return awayKickReturner;
        else return homeKickReturner;
    }

    private int returnPlay(int startYards, PlayerK kicker, PlayerReturner returner, int ST, boolean kickoff) {
        int yards;
        returnYards = 0;

        // Missing kicker — treat as a touchback rather than crashing.
        if (kicker == null) {
            return -4;
        }

        //Kicker kicks the ball
        if (kickoff) yards = startYards - (kicker.getRatKickPow() / 2) - (int) (25 * Math.random());
        else yards = startYards - (kicker.getRatKickPow() - (25 + (int) (20 * Math.random())));

        if (yards < -3) {
            //touchback
            return yards;
        } else if (returner == null) {
            // Depleted roster without a returner — treat as a touchback rather than crashing.
            return -4;
        } else {
            //Returner receives ball and runs at defense

            int ret = (int) (returner.ratSpeed * Math.random());
            int def = (int) (ST * Math.random());

            //Returner tackled by playerST?
            if (def >= ret) returnYards = (int) (Math.random() * 10) + 1;
            else if (ret > def + 80) returnYards += 100 - yards;
            else if (ret > def + 50) returnYards = (int) (Math.random() * 40) + 30;
            else if (ret > def + 35) returnYards = (int) (Math.random() * 20) + 20;
            else returnYards = ret - def;

            if (kickoff) {
                returner.kYards += returnYards;
                returner.kReturns++;
            } else {
                returner.pYards += returnYards;
                returner.pReturns++;
            }
            yards += returnYards;
            return yards;
        }
    }

    //STATISTICS MANAGEMENT

    private void recordRushAttempt(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, int yardsGain, boolean gotTD) {
        statRecorder.recordRushAttempt(offense, selQB, selRB, selDL, selLB, selCB, selS, yardsGain, gotTD);
    }

    private void recordRushFumble(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS) {
        statRecorder.recordRushFumble(offense, selQB, selRB, selDL, selLB, selCB, selS);
    }

    private void recordPassingTD(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, int yardsGain, String pos) {
        statRecorder.recordPassingTD(offense, selQB, selRB, selWR, selTE, yardsGain, pos);
    }

    private void recordPassCompletion(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerLB selLB, PlayerCB selCB, PlayerS selS, int yardsGain, String pos, boolean gotTD) {
        statRecorder.recordPassCompletion(offense, selQB, selRB, selWR, selTE, selLB, selCB, selS, yardsGain, pos, gotTD);
    }

    private void recordPassAttempt(PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerLB selLB, PlayerCB selCB, String pos) {
        statRecorder.recordPassAttempt(selQB, selRB, selWR, selTE, selLB, selCB, pos);
    }

    private void recordDrop(PlayerRB selRB, PlayerTE selTE, PlayerWR selWR, PlayerCB selCB, PlayerLB selLB, String pos) {
        statRecorder.recordDrop(selRB, selTE, selWR, selCB, selLB, pos);
    }

    private void recordDefendedCB(PlayerWR selWR, PlayerCB selCB) {
        statRecorder.recordDefendedCB(selWR, selCB);
    }

    private void recordDefendedLB(PlayerTE selTE, PlayerLB selLB) {
        statRecorder.recordDefendedLB(selTE, selLB);
    }

    private void recordDefendedLB2(PlayerRB selRB, PlayerLB selLB) {
        statRecorder.recordDefendedLB2(selRB, selLB);
    }

    private void recordInterception(Team offense, PlayerQB selQB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, String position) {
        statRecorder.recordInterception(offense, selQB, selDL, selLB, selCB, selS, position);
    }

    private void recordSack(Team offense, Team defense, PlayerQB selQB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS) {
        statRecorder.recordSack(offense, defense, selQB, selDL, selLB, selCB, selS);
    }

    private void recordRecFumble(Team offense, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, String pos) {
        statRecorder.recordRecFumble(offense, selRB, selWR, selTE, selDL, selLB, selCB, selS, pos);
    }

    private void recordSafety(String defender) {
        statRecorder.recordSafety(defender);
    }

    void recordReturnStats() {
        statRecorder.recordReturnStats();
    }

    //CLOCK AND HEALTH MANAGEMENT

    private void addPointsQuarter(int points) {
        if (gamePoss) {
            //home poss
            if (gameTime > 2700) {
                homeQScore[0] += points;
            } else if (gameTime > 1800) {
                homeQScore[1] += points;
            } else if (gameTime > 900) {
                homeQScore[2] += points;
            } else if (numOT == 0) {
                homeQScore[3] += points;
            } else {
                if (3 + numOT < 10) homeQScore[3 + numOT] += points;
                else homeQScore[9] += points;
            }
        } else {
            //away
            if (gameTime > 2700) {
                awayQScore[0] += points;
            } else if (gameTime > 1800) {
                awayQScore[1] += points;
            } else if (gameTime > 900) {
                awayQScore[2] += points;
            } else if (numOT == 0) {
                awayQScore[3] += points;
            } else {
                if (3 + numOT < 10) awayQScore[3 + numOT] += points;
                else awayQScore[9] += points;
            }
        }
    }

    void resetForOT() {
        if (bottomOT && homeScore == awayScore) {
            //Add some gameFatigue points
            List<Player> allHomePlayers = homeTeam.getAllPlayers();
            List<Player> allAwayPlayers = awayTeam.getAllPlayers();
            for (int i = 0; i < allHomePlayers.size(); ++i) {
                allHomePlayers.get(i).gameFatigue += 50;
                if (allHomePlayers.get(i).gameFatigue > 100)
                    allHomePlayers.get(i).gameFatigue = 100;
            }
            for (int i = 0; i < allAwayPlayers.size(); ++i) {
                allAwayPlayers.get(i).gameFatigue += 50;
                if (allAwayPlayers.get(i).gameFatigue > 100)
                    allAwayPlayers.get(i).gameFatigue = 100;
            }

            gameYardLine = 75;
            gameYardsNeed = 10;
            gameDown = 1;
            numOT++;
            gamePoss = (numOT % 2) == 0;
            gameTime = -1;
            bottomOT = false;
            //runPlay( awayTeam, homeTeam );
        } else if (!bottomOT) {
            gamePoss = !gamePoss;
            gameYardLine = 75;
            gameYardsNeed = 10;
            gameDown = 1;
            gameTime = -1;
            bottomOT = true;
            //runPlay( homeTeam, awayTeam );
        } else {
            // game is not tied after both teams had their chance
            playingOT = false;
        }
    }

    private String convGameTime() {
        if (!playingOT) {
            int qNum = (3600 - gameTime) / 900 + 1;
            int minTime;
            int secTime;
            String secStr;
            if (gameTime <= 0 && numOT <= 0) { // Prevent Q5 1X:XX from displaying in the game log
                return "0:00 Q4";
            } else {
                minTime = (gameTime - 900 * (4 - qNum)) / 60;
                secTime = (gameTime - 900 * (4 - qNum)) - 60 * minTime;
                if (secTime < 10) secStr = "0" + secTime;
                else secStr = "" + secTime;
                return minTime + ":" + secStr + " Q" + qNum;
            }
        } else {
            if (!bottomOT) {
                return "TOP OT" + numOT;
            } else {
                return "BOT OT" + numOT;
            }
        }
    }

    private void quarterCheck() {
        if (gameTime < 2700 && !QT1) {
            QT1 = true;
            //Set Player Fatigue +50
            recoup(true, 1);
            gameTime = 2700;
            gameEventLog.append("\n\n-- 2nd QUARTER --");

        } else if (gameTime < 1800 && !QT2) {
            QT2 = true;
            //Set Player Fatigue to 100
            recoup(true, 2);
            gameTime = 1800;
            gameEventLog.append("\n\n-- 3rd QUARTER --");
            gamePoss = false;
            kickOff(awayTeam, homeTeam);

        } else if (gameTime < 900 && !QT3) {
            QT3 = true;
            //Set Player Fatigue +50
            recoup(true, 3);
            gameTime = 900;
            gameEventLog.append("\n\n-- 4th QUARTER --");
        }

    }

    private void recoup(boolean endQT, int qt) {
        int gain = fatigueGain;
        if (endQT && qt != 2) gain = (int)(Math.random() * 35) + 15;
        if (endQT && qt == 2) gain = 50;
        //recoup v2.0
        for (int i = 0; i < homeTeam.startersRB; ++i) {
            homeTeam.getRB(i).gameFatigue += gain;
            if (homeTeam.getRB(i).gameFatigue > 100) homeTeam.getRB(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersWR; ++i) {
            homeTeam.getWR(i).gameFatigue += gain;
            if (homeTeam.getWR(i).gameFatigue > 100) homeTeam.getWR(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersTE; ++i) {
            homeTeam.getTE(i).gameFatigue += gain;
            if (homeTeam.getTE(i).gameFatigue > 100) homeTeam.getTE(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersOL; ++i) {
            homeTeam.getOL(i).gameFatigue += gain;
            if (homeTeam.getOL(i).gameFatigue > 100) homeTeam.getOL(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersDL; ++i) {
            homeTeam.getDL(i).gameFatigue += gain;
            if (homeTeam.getDL(i).gameFatigue > 100) homeTeam.getDL(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersLB; ++i) {
            homeTeam.getLB(i).gameFatigue += gain;
            if (homeTeam.getLB(i).gameFatigue > 100) homeTeam.getLB(i).gameFatigue = 100;
        }
        for (int i = 0; i < homeTeam.startersS; ++i) {
            homeTeam.getS(i).gameFatigue += gain;
            if (homeTeam.getS(i).gameFatigue > 100) homeTeam.getS(i).gameFatigue = 100;
        }

        //recoup v2.0
        for (int i = 0; i < awayTeam.startersRB; ++i) {
            awayTeam.getRB(i).gameFatigue += gain;
            if (awayTeam.getRB(i).gameFatigue > 100) awayTeam.getRB(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersWR; ++i) {
            awayTeam.getWR(i).gameFatigue += gain;
            if (awayTeam.getWR(i).gameFatigue > 100) awayTeam.getWR(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersTE; ++i) {
            awayTeam.getTE(i).gameFatigue += gain;
            if (awayTeam.getTE(i).gameFatigue > 100) awayTeam.getTE(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersOL; ++i) {
            awayTeam.getOL(i).gameFatigue += gain;
            if (awayTeam.getOL(i).gameFatigue > 100) awayTeam.getOL(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersDL; ++i) {
            awayTeam.getDL(i).gameFatigue += gain;
            if (awayTeam.getDL(i).gameFatigue > 100) awayTeam.getDL(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersLB; ++i) {
            awayTeam.getLB(i).gameFatigue += gain;
            if (awayTeam.getLB(i).gameFatigue > 100) awayTeam.getLB(i).gameFatigue = 100;
        }
        for (int i = 0; i < awayTeam.startersS; ++i) {
            awayTeam.getS(i).gameFatigue += gain;
            if (awayTeam.getS(i).gameFatigue > 100) awayTeam.getS(i).gameFatigue = 100;
        }

    }

    private void checkInjury(Player p, Team t) {

        if (p.ratDurability / injuryChance < Math.random()) {

            if (.5 < Math.random()) {
                if(p.gameFatigue < 50) {
                    p.gameFatigue = -(int) (Math.random() * 100);
                    if (homeTeam.league.fullGameLog)
                        gameEventLog.append(getEventLog()).append("Minor Injury!\n").append(t.getAbbr()).append(" ").append(p.position).append(" ").append(p.name).append(" will miss a few snaps with a minor injury.");
                }
            } else {

                int numInjured = 0;
                int numStarters = 0;
                java.util.List<? extends Player> players = new ArrayList<>();

                if (p.position.equals("QB")) {
                    players = new ArrayList<>(t.getTeamQBs());
                    numStarters = t.startersQB + t.subQB;
                } else if (p.position.equals("RB")) {
                    players = new ArrayList<>(t.getTeamRBs());
                    numStarters = t.startersRB + t.subRB;
                } else if (p.position.equals("WR")) {
                    players = new ArrayList<>(t.getTeamWRs());
                    numStarters = t.startersWR + t.subWR;
                } else if (p.position.equals("TE")) {
                    players = new ArrayList<>(t.getTeamTEs());
                    numStarters = t.startersTE + t.subTE;
                } else if (p.position.equals("OL")) {
                    players = new ArrayList<>(t.getTeamOLs());
                    numStarters = t.startersOL + t.subOL;
                } else if (p.position.equals("K")) {
                    players = new ArrayList<>(t.getTeamKs());
                    numStarters = t.startersK + t.subK;
                } else if (p.position.equals("DL")) {
                    players = new ArrayList<>(t.getTeamDLs());
                    numStarters = t.startersDL + t.subDL;
                } else if (p.position.equals("LB")) {
                    players = new ArrayList<>(t.getTeamLBs());
                    numStarters = t.startersLB + t.subLB;
                } else if (p.position.equals("CB")) {
                    players = new ArrayList<>(t.getTeamCBs());
                    numStarters = t.startersCB + t.subCB;
                } else if (p.position.equals("S")) {
                    players = new ArrayList<>(t.getTeamSs());
                    numStarters = t.startersS + t.subS;
                }


                for (Player z : players) {
                    if (z.injury != null && !z.isSuspended && !z.isTransfer) {
                        numInjured++;
                    }
                }

                // Only injure if there are people left to injure
                if (numInjured < numStarters) {
                    if (Math.random() < Math.pow(1 - (double) p.ratDurability / 125, 3) && numInjured < numStarters) {
                        // injury!
                        p.injury = new Injury(p);
                        t.addPlayerInjured(p);
                        //Collections.sort(players, new CompPlayer());
                        Collections.sort(players, new CompPlayerPosDepth());
                        gameEventLog.append(getEventLog()).append("MAJOR INJURY!\n").append(t.getAbbr()).append(" ").append(p.position).append(" ").append(p.name).append(" is out of the game with an injury.");

                        if(awayKickReturner != null && awayKickReturner.name.equals(p.name)) {
                            getReturner(awayTeam);
                        } else if(homeKickReturner != null && homeKickReturner.name.equals(p.name)) {
                            getReturner(homeTeam);
                        }
                    }
                }
            }
        }
    }


    //PREVIEW & BOXSCORE

    private void gameStatistics() {
        getBoxScore().buildStatistics();
    }


    public String[] getGameSummaryStrV2() {
        return getBoxScore().getGameSummaryStrV2();
    }

    public String[] getGameSummaryStr() {
        return getBoxScore().getGameSummaryStr();
    }


    /**
     * Returns the full play-by-play event log for this game.
     * The log is built incrementally during simulation and contains
     * scoring plays, turnovers, injuries, and other key events.
     *
     * @return formatted play-by-play text, or a placeholder if the game
     *         has not been played yet
     */
    public String getPlayByPlayLog() {
        return getBoxScore().getPlayByPlayLog();
    }

    public String[] getGameScoutStr() {
        return getBoxScore().getGameScoutStr();
    }

    private void addNewsStory() {
        newsService.addNewsStory();
    }

    String getEventLogScoring() {
        return "\n\n[ " + homeTeam.getAbbr() + " " + homeScore + " - " + awayScore + " " + awayTeam.getAbbr() + " ]\n\t" + convGameTime() + " ";
    }

    String getEventLog() {
        String possStr;
        if (gamePoss) possStr = homeTeam.getAbbr();
        else possStr = awayTeam.getAbbr();
        String yardsNeedAdj = "" + gameYardsNeed;
        if (gameYardLine + gameYardsNeed >= 100) yardsNeedAdj = "Goal";
        int gameDownAdj;
        if (gameDown > 4) {
            gameDownAdj = 4;
        } else {
            gameDownAdj = gameDown;
        }
        return "\n\n" + convGameTime() + " " + possStr + " " + gameDownAdj + " and " + yardsNeedAdj + " at " + gameYardLinePlay + " yard line." + "\n";
    }

    private String getEventLogScore() {
        String possStr;
        return "\n\n" + homeTeam.getAbbr() + " " + homeScore + " - " + awayScore + " " + awayTeam.getAbbr();
    }

    private int getPassYards(boolean ha) {
        //ha = home/away, false for home, true for away
        int yards = 0;
        if (!ha) {
            for (int i = 0; i < homeTeam.getTeamQBs().size(); ++i) {
                yards += homeTeam.getQB(i).gamePassYards;
            }
            return yards;
        } else {
            for (int i = 0; i < awayTeam.getTeamQBs().size(); ++i) {
                yards += awayTeam.getQB(i).gamePassYards;
            }
            return yards;
        }
    }

    private int getRushYards(boolean ha) {
        //ha = home/away, false for home, true for away
        int yards = 0;
        if (!ha) {
            for (int i = 0; i < homeTeam.getTeamRBs().size(); ++i) {
                yards += homeTeam.getRB(i).gameRushYards;
            }
            for (int i = 0; i < homeTeam.getTeamQBs().size(); ++i) {
                yards += homeTeam.getQB(i).gameRushYards;
            }
            return yards;
        } else {
            for (int i = 0; i < awayTeam.getTeamRBs().size(); ++i) {
                yards += awayTeam.getRB(i).gameRushYards;
            }
            for (int i = 0; i < awayTeam.getTeamQBs().size(); ++i) {
                yards += awayTeam.getQB(i).gameRushYards;
            }
            return yards;
        }
    }

    public void addUpcomingGames(Team name) {
        newsService.addUpcomingGames(name);
    }

    public void addNewSeasonGames(Team name) {
        newsService.addNewSeasonGames(name);
    }


    public Game(Team t, String saveData) {
        String[] save = saveData.split(java.util.regex.Pattern.quote("$$"));
        hasPlayed = Boolean.parseBoolean(save[0]);
        homeTeam = GameSerializer.resolveTeamFromSave(t.league, save.length > 1 ? save[1] : null);
        awayTeam = GameSerializer.resolveTeamFromSave(t.league, save.length > 2 ? save[2] : null);
        gameName = save[3];
        gameEventLog.setLength(0);
        gameEventLog.append(save[4]);

        String[] x = save[5].split(",");
        homeScore = Integer.parseInt(x[0]);
        awayScore = Integer.parseInt(x[1]);
        homeYards = Integer.parseInt(x[2]);
        awayYards = Integer.parseInt(x[3]);
        homePassYards = Integer.parseInt(x[4]);
        awayPassYards = Integer.parseInt(x[5]);
        homeRushYards = Integer.parseInt(x[6]);
        awayRushYards = Integer.parseInt(x[7]);
        homeTOs = Integer.parseInt(x[8]);
        awayTOs = Integer.parseInt(x[9]);
        numOT = Integer.parseInt(x[10]);

        homeQScore = new int[10];
        x = save[6].split(",");
        for (int i = 0; i < homeQScore.length; i++) {
            homeQScore[i] = Integer.parseInt(x[i]);
        }
        awayQScore = new int[10];
        x = save[7].split(",");
        for (int i = 0; i < awayQScore.length; i++) {
            awayQScore[i] = Integer.parseInt(x[i]);
        }

        homePassingStats = new ArrayList<>();
        x = save[8].split("%");
        for (int i = 0; i < x.length; i++) {
            homePassingStats.add(x[i]);
        }

        awayPassingStats = new ArrayList<>();
        x = save[9].split("%");
        for (int i = 0; i < x.length; i++) {
            awayPassingStats.add(x[i]);
        }

        homeRushingStats = new ArrayList<>();
        x = save[10].split("%");
        for (int i = 0; i < x.length; i++) {
            homeRushingStats.add(x[i]);
        }

        awayRushingStats = new ArrayList<>();
        x = save[11].split("%");
        for (int i = 0; i < x.length; i++) {
            awayRushingStats.add(x[i]);
        }

        homeReceivingStats = new ArrayList<>();
        x = save[12].split("%");
        for (int i = 0; i < x.length; i++) {
            homeReceivingStats.add(x[i]);
        }

        awayReceivingStats = new ArrayList<>();
        x = save[13].split("%");
        for (int i = 0; i < x.length; i++) {
            awayReceivingStats.add(x[i]);
        }

        homeDefenseStats = new ArrayList<>();
        x = save[14].split("%");
        for (int i = 0; i < x.length; i++) {
            homeDefenseStats.add(x[i]);
        }

        awayDefenseStats = new ArrayList<>();
        x = save[15].split("%");
        for (int i = 0; i < x.length; i++) {
            awayDefenseStats.add(x[i]);
        }

        homeKickingStats = new ArrayList<>();
        x = save[16].split("%");
        for (int i = 0; i < x.length; i++) {
            homeKickingStats.add(x[i]);
        }


        awayKickingStats = new ArrayList<>();
        x = save[17].split("%");
        for (int i = 0; i < x.length; i++) {
            awayKickingStats.add(x[i]);
        }

        if (gameName == null) {
            gameName = "Game";
        }
        newsService = new GameNewsService(this);
        statRecorder = new GameStatRecorder(this);
    }
    

    public ArrayList<String> saveGameData() {
        return GameSerializer.saveGameData(this);
    }

    
}
