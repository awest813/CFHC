package simulation;

import java.util.ArrayList;
import java.util.Collections;

import comparator.CompTeamConfWins;
import comparator.CompTeamPoll;

/**
 * Manages bowl game scheduling, playoff expansion, and bowl game execution.
 * Decoupled from League via the League reference passed at construction.
 */
public class BowlManager {

    private final League league;

    BowlManager(League league) {
        this.league = league;
    }

    public String getBowlGameWatchStr() {
        if (!league.hasScheduledBowls) {
            if (league.expPlayoffs) {
                getExpPlayoffTeams();
                return league.postseason;
            } else {
                league.setTeamRanks();
                for (int i = 0; i < league.teamList.size(); ++i) {
                    league.teamList.get(i).updatePollScore();
                    if (league.teamList.get(i).isBowlBan())
                        league.teamList.get(i).setTeamPollScore(0);
                }
                Collections.sort(league.teamList, new CompTeamPoll());

                StringBuilder sb = new StringBuilder();
                Team t1;
                Team t2;

                sb.append("Semifinal 1v4:\n\t\t");
                t1 = league.teamList.get(0);
                t2 = league.teamList.get(3);
                sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

                sb.append("Semifinal 2v3:\n\t\t");
                t1 = league.teamList.get(1);
                t2 = league.teamList.get(2);
                sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");

                int t = 4;
                for (int g = 0; g < league.bowlNames.length; g++) {
                    sb.append(league.bowlNames[g] + ":\n\t\t");
                    t1 = league.teamList.get(t);
                    t2 = league.teamList.get(t + 4);
                    sb.append(t1.strRep() + " vs " + t2.strRep() + "\n\n");
                    t++;
                    if (t % 8 == 0) t = t + 8;
                }
                return sb.toString();
            }
        } else {
            if (league.expPlayoffs) {
                return league.postseason;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Semifinal 1v4:\n");
                sb.append(getGameSummaryBowl(league.semiG14));
                sb.append("\n\nSemifinal 2v3:\n");
                sb.append(getGameSummaryBowl(league.semiG23));
                for (int i = 0; i < league.bowlGames.length; ++i) {
                    if (league.bowlGames[i] != null) {
                        sb.append("\n\n" + league.bowlNames[i] + ":\n");
                        sb.append(getGameSummaryBowl(league.bowlGames[i]));
                    }
                }
                return sb.toString();
            }
        }
    }

    public void getExpPlayoffTeams() {
        league.playoffTeams.clear();

        ArrayList<Team> qualifiedTeams = getQualifiedTeams();
        ArrayList<Team> autoBids = getExpandedPlayoffAutoBids(qualifiedTeams);
        league.playoffTeams.addAll(autoBids);
        for (Team qualifiedTeam : qualifiedTeams) {
            if (!league.playoffTeams.contains(qualifiedTeam)) {
                league.playoffTeams.add(qualifiedTeam);
                if (league.playoffTeams.size() >= League.EXPANDED_PLAYOFF_TEAM_COUNT) break;
            }
        }

        if (league.playoffTeams.size() < League.EXPANDED_PLAYOFF_TEAM_COUNT) {
            Collections.sort(league.teamList, new CompTeamPoll());
            for (Team team : league.teamList) {
                if (!league.playoffTeams.contains(team)) {
                    league.playoffTeams.add(team);
                    if (league.playoffTeams.size() >= League.EXPANDED_PLAYOFF_TEAM_COUNT) break;
                }
            }
        }

        Collections.sort(league.playoffTeams, new CompTeamPoll());

        StringBuilder sb = new StringBuilder();
        sb.append("The following teams are expected to make it to the Football Playoffs!\n\n");
        int i = 1;
        for (Team t : league.playoffTeams) {
            sb.append(i + ". " + t.strRankTeamRecord() + "   [" + t.getConference() + "]\n");
            i++;
        }
        league.postseason = sb.toString();

        for (int x = 0; x < league.playoffTeams.size(); x++)
            qualifiedTeams.remove(league.playoffTeams.get(x));

        if (league.hasScheduledBowls) bowlScheduleLogic(qualifiedTeams);
    }

    public ArrayList<Team> getExpandedPlayoffAutoBids(ArrayList<Team> qualifiedTeams) {
        ArrayList<Team> autoBids = new ArrayList<>();
        ArrayList<Team> conferenceLeaders = new ArrayList<>();

        if (league.currentWeek > league.regSeasonWeeks) {
            for (Team qt : qualifiedTeams) {
                if ("CC".equals(qt.getConfChampion())
                        && !qt.getConference().equals("Independent")
                        && !qt.getConference().equals("FCS Division")) {
                    conferenceLeaders.add(qt);
                }
            }
        } else {
            for (Conference c : league.conferences) {
                if (!c.confName.equals("Independent") && !c.confName.equals("FCS Division") && c.confTeams.size() > 0) {
                    Collections.sort(c.confTeams, new CompTeamConfWins());
                    Team projectedChampion = c.confTeams.get(0);
                    if (qualifiedTeams.contains(projectedChampion))
                        conferenceLeaders.add(projectedChampion);
                }
            }
        }

        Collections.sort(conferenceLeaders, new CompTeamPoll());
        for (int i = 0; i < conferenceLeaders.size() && i < 5; i++)
            autoBids.add(conferenceLeaders.get(i));

        return autoBids;
    }

    public void scheduleExpPlayoff() {
        league.hasScheduledBowls = true;
        league.playoffWeek = 1;
        getExpPlayoffTeams();

        league.cfpGames[0] = new Game(league.playoffTeams.get(4), league.playoffTeams.get(11), "First Round");
        league.cfpGames[1] = new Game(league.playoffTeams.get(7), league.playoffTeams.get(8), "First Round");
        league.cfpGames[2] = new Game(league.playoffTeams.get(5), league.playoffTeams.get(10), "First Round");
        league.cfpGames[3] = new Game(league.playoffTeams.get(6), league.playoffTeams.get(9), "First Round");

        for (int i = 0; i < 4; i++) {
            league.cfpGames[i].homeTeam.addGameToSchedule(league.cfpGames[i]);
            league.cfpGames[i].awayTeam.addGameToSchedule(league.cfpGames[i]);
            league.newsStories.get(league.currentWeek + 1).add("Upcoming First Round Playoff Game!>#"
                    + league.cfpGames[i].awayTeam.getRankTeamPollScore() + " " + league.cfpGames[i].awayTeam.getStrAbbrWL()
                    + " will battle with #" + league.cfpGames[i].homeTeam.getRankTeamPollScore() + " "
                    + league.cfpGames[i].homeTeam.getStrAbbrWL() + " in the " + league.getYear()
                    + " College Football Playoff first round!");
            league.weeklyScores.get(league.currentWeek + 2).add(league.cfpGames[i].gameName + ">"
                    + league.cfpGames[i].awayTeam.strRankTeamRecord() + "\n"
                    + league.cfpGames[i].homeTeam.strRankTeamRecord());
        }

        for (int i = 0; i < league.teamList.size(); i++)
            league.teamList.get(i).healInjury(1);
    }

    public void expPlayoffSchdQT() {
        league.cfpGames[4] = new Game(league.playoffTeams.get(3), getWinner(league.cfpGames[0]), "Quarterfinal");
        league.cfpGames[5] = new Game(league.playoffTeams.get(0), getWinner(league.cfpGames[1]), "Quarterfinal");
        league.cfpGames[6] = new Game(league.playoffTeams.get(2), getWinner(league.cfpGames[2]), "Quarterfinal");
        league.cfpGames[7] = new Game(league.playoffTeams.get(1), getWinner(league.cfpGames[3]), "Quarterfinal");

        for (int i = 4; i < 8; i++) {
            league.cfpGames[i].homeTeam.addGameToSchedule(league.cfpGames[i]);
            league.cfpGames[i].awayTeam.addGameToSchedule(league.cfpGames[i]);
            league.newsStories.get(league.currentWeek + 1).add("Quarterfinal Match-up Announced!>"
                    + league.cfpGames[i].awayTeam.getStrAbbrWL() + " will take on "
                    + league.cfpGames[i].homeTeam.getStrAbbrWL() + " in the "
                    + league.getYear() + " College Football Playoff quarterfinals!");
            league.weeklyScores.get(league.currentWeek + 2).add(league.cfpGames[i].gameName + ">#"
                    + league.cfpGames[i].awayTeam.getRankTeamPollScore() + " " + league.cfpGames[i].awayTeam.getName()
                    + "\n#" + league.cfpGames[i].homeTeam.getRankTeamPollScore() + " " + league.cfpGames[i].homeTeam.getName());
        }
    }

    public void expPlayoffSchdSemi() {
        league.cfpGames[8] = new Game(getWinner(league.cfpGames[4]), getWinner(league.cfpGames[5]), "Semifinal");
        league.cfpGames[9] = new Game(getWinner(league.cfpGames[6]), getWinner(league.cfpGames[7]), "Semifinal");

        for (int i = 8; i < 10; i++) {
            league.cfpGames[i].homeTeam.addGameToSchedule(league.cfpGames[i]);
            league.cfpGames[i].awayTeam.addGameToSchedule(league.cfpGames[i]);
            league.newsStories.get(league.currentWeek + 1).add("Semifinal Match-up Announced!>"
                    + league.cfpGames[i].awayTeam.getStrAbbrWL() + " and "
                    + league.cfpGames[i].homeTeam.getStrAbbrWL() + " will play each other in the "
                    + league.getYear() + " national semifinals!");
            league.weeklyScores.get(league.currentWeek + 2).add(league.cfpGames[i].gameName + ">"
                    + league.cfpGames[i].awayTeam.strRankTeamRecord() + "\n"
                    + league.cfpGames[i].homeTeam.strRankTeamRecord());
        }
    }

    public void expPlayoffSchFinals() {
        Team titleTeamA = getWinner(league.cfpGames[8]);
        Team titleTeamB = getWinner(league.cfpGames[9]);

        league.ncg = new Game(titleTeamA, titleTeamB, "Championship");
        titleTeamA.addGameToSchedule(league.ncg);
        titleTeamB.addGameToSchedule(league.ncg);
        league.newsStories.get(league.currentWeek + 1).add("The Upcoming National Title Game!>"
                + titleTeamA.getStrAbbrWL() + " and " + titleTeamB.getStrAbbrWL()
                + " are the last two teams left in the " + league.getYear()
                + " College Football Playoffs. These teams will compete next weekend for the National Title!");
        league.weeklyScores.get(league.currentWeek + 2).add(league.ncg.gameName + ">"
                + league.ncg.awayTeam.strRankTeamRecord() + "\n" + league.ncg.homeTeam.strRankTeamRecord());
        league.newsHeadlines.add(titleTeamA.getStrAbbrWL() + " and " + titleTeamB.getStrAbbrWL()
                + " to meet in the " + league.getYear() + " Championship.");
    }

    public void playExpandedPlayoffFirstRound() {
        playBowlWeek1();
        league.playoffWeek = 1;
        for (int i = 0; i < 4; i++) playPlayoff(league.cfpGames[i]);
        expPlayoffSchdQT();
    }

    public void playExpandedPlayoffQuarterfinals() {
        playBowlWeek2();
        league.playoffWeek = 2;
        for (int i = 4; i < 8; i++) playPlayoff(league.cfpGames[i]);
        expPlayoffSchdSemi();
    }

    public void playExpandedPlayoffSemifinals() {
        playBowlWeek3();
        league.playoffWeek = 3;
        for (int i = 8; i < 10; i++) playPlayoff(league.cfpGames[i]);
        expPlayoffSchFinals();
    }

    public Team getWinner(Game game) {
        return game.homeScore > game.awayScore ? game.homeTeam : game.awayTeam;
    }

    public void playPlayoff(Game g) {
        g.playGame();

        if (league.playoffWeek == 1) {
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSweet16("FRW");
                g.awayTeam.setSweet16("FRL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + ".");
                league.newsHeadlines.add(g.homeTeam.getName() + " wins first-round playoff game!");
            } else {
                g.homeTeam.setSweet16("FRL");
                g.awayTeam.setSweet16("FRW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + ".");
                league.newsHeadlines.add(g.awayTeam.getName() + " advances in the playoffs!");
            }
        }

        if (league.playoffWeek == 2) {
            g.homeTeam.setSweet16("");
            g.awayTeam.setSweet16("");
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setQtFinalWL("QTW");
                g.awayTeam.setQtFinalWL("QTL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + ".");
                league.newsHeadlines.add(g.homeTeam.getName() + " advances to the semifinals!");
            } else {
                g.homeTeam.setQtFinalWL("QTL");
                g.awayTeam.setQtFinalWL("QTW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + ".");
                league.newsHeadlines.add(g.awayTeam.getName() + " advances to the semifinals!");
            }
        }

        if (league.playoffWeek == 3) {
            g.homeTeam.setQtFinalWL("");
            g.awayTeam.setQtFinalWL("");
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSemiFinalWL("SFW");
                g.awayTeam.setSemiFinalWL("SFL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + ".");
                league.newsHeadlines.add(g.homeTeam.getName() + " reaches the national title game!");
            } else {
                g.homeTeam.setSemiFinalWL("SFL");
                g.awayTeam.setSemiFinalWL("SFW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + ".");
                league.newsHeadlines.add(g.awayTeam.getName() + " reaches the national title game!");
            }
        }
    }

    public ArrayList<Team> getQualifiedTeams() {
        ArrayList<Team> bowlTeams = new ArrayList<>();
        league.setTeamRanks();
        Collections.sort(league.teamList, new CompTeamPoll());

        for (int i = 0; i < league.teamList.size(); ++i) {
            if (!league.teamList.get(i).isBowlBan() && league.teamList.get(i).getWins() >= 6)
                bowlTeams.add(league.teamList.get(i));
        }

        Collections.sort(bowlTeams, new CompTeamPoll());
        return bowlTeams;
    }

    public void scheduleNormalCFP() {
        ArrayList<Team> bowlTeams = getQualifiedTeams();

        league.semiG14 = new Game(bowlTeams.get(0), bowlTeams.get(3), "Semis, 1v4");
        bowlTeams.get(0).addGameToSchedule(league.semiG14);
        bowlTeams.get(3).addGameToSchedule(league.semiG14);

        league.semiG23 = new Game(bowlTeams.get(1), bowlTeams.get(2), "Semis, 2v3");
        bowlTeams.get(1).addGameToSchedule(league.semiG23);
        bowlTeams.get(2).addGameToSchedule(league.semiG23);

        league.newsStories.get(league.currentWeek + 1).add("Playoff Teams Announced!>"
                + "#" + bowlTeams.get(0).getRankTeamPollScore() + bowlTeams.get(0).getStrAbbrWL()
                + " will play #" + bowlTeams.get(3).getRankTeamPollScore() + bowlTeams.get(3).getStrAbbrWL()
                + " , while " + "#" + bowlTeams.get(1).getRankTeamPollScore() + bowlTeams.get(1).getStrAbbrWL()
                + " will play #" + bowlTeams.get(2).getRankTeamPollScore() + bowlTeams.get(2).getStrAbbrWL()
                + " in next week's College Football Playoff semi-final round. The winners will compete for this year's National Title!");

        league.weeklyScores.get(league.currentWeek + 4).add(league.semiG14.gameName + ">"
                + league.semiG14.awayTeam.strRankTeamRecord() + "\n" + league.semiG14.homeTeam.strRankTeamRecord());
        league.weeklyScores.get(league.currentWeek + 4).add(league.semiG23.gameName + ">"
                + league.semiG23.awayTeam.strRankTeamRecord() + "\n" + league.semiG23.homeTeam.strRankTeamRecord());

        for (int i = 0; i < 4; i++)
            bowlTeams.get(i).healInjury(3);

        bowlTeams.remove(league.semiG23.awayTeam);
        bowlTeams.remove(league.semiG23.homeTeam);
        bowlTeams.remove(league.semiG14.awayTeam);
        bowlTeams.remove(league.semiG14.homeTeam);
        bowlScheduleLogic(bowlTeams);
    }

    public void bowlScheduleLogic(ArrayList<Team> bowlTeams) {
        int bowlCount = (bowlTeams.size()) / 2;
        if (bowlCount > league.bowlNames.length) bowlCount = league.bowlNames.length;

        int g = 0;
        int r = 1;
        int t = 0;

        while (bowlCount / 4 >= r) {
            for (int i = t; i < t + 4; i++) {
                league.bowlGames[g] = new Game(bowlTeams.get(i), bowlTeams.get(i + 4), league.bowlNames[g]);
                bowlTeams.get(i).addGameToSchedule(league.bowlGames[g]);
                bowlTeams.get(i + 4).addGameToSchedule(league.bowlGames[g]);
                league.newsStories.get(league.currentWeek + 1).add(league.bowlGames[g].gameName + " Announced!>#"
                        + bowlTeams.get(i).getRankTeamPollScore() + " " + bowlTeams.get(i).getStrAbbrWL()
                        + " will compete with #" + bowlTeams.get(i + 4).getRankTeamPollScore() + " "
                        + bowlTeams.get(i + 4).getStrAbbrWL() + " in the " + league.getYear() + " "
                        + league.bowlGames[g].gameName + "!");
                if (g < 6)
                    league.weeklyScores.get(league.currentWeek + 4).add(league.bowlGames[g].gameName + ">"
                            + league.bowlGames[g].awayTeam.strRankTeamRecord() + "\n"
                            + league.bowlGames[g].homeTeam.strRankTeamRecord());
                else if (g < 16)
                    league.weeklyScores.get(league.currentWeek + 3).add(league.bowlGames[g].gameName + ">"
                            + league.bowlGames[g].awayTeam.strRankTeamRecord() + "\n"
                            + league.bowlGames[g].homeTeam.strRankTeamRecord());
                else
                    league.weeklyScores.get(league.currentWeek + 2).add(league.bowlGames[g].gameName + ">"
                            + league.bowlGames[g].awayTeam.strRankTeamRecord() + "\n"
                            + league.bowlGames[g].homeTeam.strRankTeamRecord());
                g++;
            }
            t += 8;
            r++;
        }

        league.hasScheduledBowls = true;

        int tmCount = bowlTeams.size();
        if (tmCount > 32) {
            for (int i = 0; i < 12; i++) bowlTeams.get(i).healInjury(3);
            for (int i = 12; i < 32; i++) bowlTeams.get(i).healInjury(2);
            for (int i = 32; i < bowlTeams.size(); i++) bowlTeams.get(i).healInjury(1);
        } else if (tmCount > 12) {
            for (int i = 0; i < 12; i++) bowlTeams.get(i).healInjury(3);
            for (int i = 12; i < tmCount; i++) bowlTeams.get(i).healInjury(2);
        } else {
            for (int i = 0; i < tmCount; i++) bowlTeams.get(i).healInjury(3);
        }
    }

    public void playBowlWeek1() {
        for (int g = 16; g < league.bowlGames.length; g++)
            if (league.bowlGames[g] != null) playBowl(league.bowlGames[g]);
    }

    public void playBowlWeek2() {
        int end = Math.min(16, league.bowlGames.length);
        for (int g = 6; g < end; g++)
            if (league.bowlGames[g] != null) playBowl(league.bowlGames[g]);
    }

    public void playBowlWeek3() {
        int end = Math.min(6, league.bowlGames.length);
        for (int g = 0; g < end; g++)
            if (league.bowlGames[g] != null) playBowl(league.bowlGames[g]);

        if (!league.expPlayoffs) {
            league.semiG14.playGame();
            league.semiG23.playGame();
            Team semi14winner, semi23winner;

            if (league.semiG14.homeScore > league.semiG14.awayScore) {
                league.semiG14.homeTeam.setSemiFinalWL("SFW");
                league.semiG14.awayTeam.setSemiFinalWL("SFL");
                league.semiG14.awayTeam.incrementTotalBowlLosses();
                league.semiG14.homeTeam.incrementTotalBowls();
                league.semiG14.homeTeam.getHeadCoach().recordBowlWins(1);
                league.semiG14.awayTeam.getHeadCoach().recordBowlLosses(1);
                semi14winner = league.semiG14.homeTeam;
                league.newsStories.get(league.currentWeek + 1).add(
                        league.semiG14.homeTeam.getName() + " wins the " + league.semiG14.gameName + "!>"
                                + league.semiG14.homeTeam.strRep() + " defeats " + league.semiG14.awayTeam.strRep()
                                + " in the semifinals, winning " + league.semiG14.homeScore + " to "
                                + league.semiG14.awayScore + ". " + league.semiG14.homeTeam.getName()
                                + " advances to the National Championship!");
                league.newsHeadlines.add(league.semiG14.homeTeam.strRep() + " defeats "
                        + league.semiG14.awayTeam.strRep() + " in the semifinals, winning "
                        + league.semiG14.homeScore + " to " + league.semiG14.awayScore + ". "
                        + league.semiG14.homeTeam.getName() + " advances to the National Championship!");
            } else {
                league.semiG14.homeTeam.setSemiFinalWL("SFL");
                league.semiG14.awayTeam.setSemiFinalWL("SFW");
                league.semiG14.homeTeam.incrementTotalBowlLosses();
                league.semiG14.awayTeam.incrementTotalBowls();
                league.semiG14.awayTeam.getHeadCoach().recordBowlWins(1);
                league.semiG14.homeTeam.getHeadCoach().recordBowlLosses(1);
                semi14winner = league.semiG14.awayTeam;
                league.newsStories.get(league.currentWeek + 1).add(
                        league.semiG14.awayTeam.getName() + " wins the " + league.semiG14.gameName + "!>"
                                + league.semiG14.awayTeam.strRep() + " defeats " + league.semiG14.homeTeam.strRep()
                                + " in the semifinals, winning " + league.semiG14.awayScore + " to "
                                + league.semiG14.homeScore + ". " + league.semiG14.awayTeam.getName()
                                + " advances to the National Championship!");
                league.newsHeadlines.add(league.semiG14.awayTeam.strRep() + " defeats "
                        + league.semiG14.homeTeam.strRep() + " in the semifinals, winning "
                        + league.semiG14.awayScore + " to " + league.semiG14.homeScore + ". "
                        + league.semiG14.awayTeam.getName() + " advances to the National Championship!");
            }

            if (league.semiG23.homeScore > league.semiG23.awayScore) {
                league.semiG23.homeTeam.setSemiFinalWL("SFW");
                league.semiG23.awayTeam.setSemiFinalWL("SFL");
                league.semiG23.homeTeam.incrementTotalBowls();
                league.semiG23.awayTeam.incrementTotalBowlLosses();
                league.semiG23.homeTeam.getHeadCoach().recordBowlWins(1);
                league.semiG23.awayTeam.getHeadCoach().recordBowlLosses(1);
                semi23winner = league.semiG23.homeTeam;
                league.newsStories.get(league.currentWeek + 1).add(
                        league.semiG23.homeTeam.getName() + " wins the " + league.semiG23.gameName + "!>"
                                + league.semiG23.homeTeam.strRep() + " defeats " + league.semiG23.awayTeam.strRep()
                                + " in the semifinals, winning " + league.semiG23.homeScore + " to "
                                + league.semiG23.awayScore + ". " + league.semiG23.homeTeam.getName()
                                + " advances to the National Championship!");
                league.newsHeadlines.add(league.semiG23.homeTeam.strRep() + " defeats "
                        + league.semiG23.awayTeam.strRep() + " in the semifinals, winning "
                        + league.semiG23.homeScore + " to " + league.semiG23.awayScore + ". "
                        + league.semiG23.homeTeam.getName() + " advances to the National Championship!");
            } else {
                league.semiG23.homeTeam.setSemiFinalWL("SFL");
                league.semiG23.awayTeam.setSemiFinalWL("SFW");
                league.semiG23.awayTeam.incrementTotalBowls();
                league.semiG23.homeTeam.incrementTotalBowlLosses();
                league.semiG23.awayTeam.getHeadCoach().recordBowlWins(1);
                league.semiG23.homeTeam.getHeadCoach().recordBowlLosses(1);
                semi23winner = league.semiG23.awayTeam;
                league.newsStories.get(league.currentWeek + 1).add(
                        league.semiG23.awayTeam.getName() + " wins the " + league.semiG23.gameName + "!>"
                                + league.semiG23.awayTeam.strRep() + " defeats " + league.semiG23.homeTeam.strRep()
                                + " in the semifinals, winning " + league.semiG23.awayScore + " to "
                                + league.semiG23.homeScore + ". " + league.semiG23.awayTeam.getName()
                                + " advances to the National Championship!");
                league.newsHeadlines.add(league.semiG23.awayTeam.strRep() + " defeats "
                        + league.semiG23.homeTeam.strRep() + " in the semifinals, winning "
                        + league.semiG23.awayScore + " to " + league.semiG23.homeScore + ". "
                        + league.semiG23.awayTeam.getName() + " advances to the National Championship!");
            }

            league.ncg = new Game(semi14winner, semi23winner, "NCG");
            semi14winner.addGameToSchedule(league.ncg);
            semi23winner.addGameToSchedule(league.ncg);
            league.newsStories.get(league.currentWeek + 1).add("Upcoming National Title Game!>"
                    + semi14winner.getStrAbbrWL() + " will compete with " + semi23winner.getStrAbbrWL()
                    + " for the " + league.getYear() + " College Football National Title!");
            league.newsHeadlines.add(semi14winner.getStrAbbrWL() + " will compete with "
                    + semi23winner.getStrAbbrWL() + " for the " + league.getYear()
                    + " College Football National Title!");
            league.weeklyScores.get(league.currentWeek + 2).add(league.ncg.gameName + ">"
                    + league.ncg.awayTeam.strRankTeamRecord() + "\n"
                    + league.ncg.homeTeam.strRankTeamRecord());
        }
    }

    public void playBowl(Game g) {
        if (!g.hasPlayed) {
            g.playGame();
            if (g.homeScore > g.awayScore) {
                g.homeTeam.setSemiFinalWL("BW");
                g.awayTeam.setSemiFinalWL("BL");
                g.homeTeam.incrementTotalBowls();
                g.awayTeam.incrementTotalBowlLosses();
                g.homeTeam.getHeadCoach().recordBowlWins(1);
                g.awayTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.homeTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.homeTeam.strRep() + " defeats " + g.awayTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.homeScore + " to " + g.awayScore + ".");
                league.newsHeadlines.add(g.homeTeam.getName() + " wins the " + g.gameName + "!");
            } else {
                g.homeTeam.setSemiFinalWL("BL");
                g.awayTeam.setSemiFinalWL("BW");
                g.homeTeam.incrementTotalBowlLosses();
                g.awayTeam.incrementTotalBowls();
                g.awayTeam.getHeadCoach().recordBowlWins(1);
                g.homeTeam.getHeadCoach().recordBowlLosses(1);
                league.newsStories.get(league.currentWeek + 1).add(
                        g.awayTeam.getName() + " wins the " + g.gameName + "!>"
                                + g.awayTeam.strRep() + " defeats " + g.homeTeam.strRep()
                                + " in the " + g.gameName + ", winning " + g.awayScore + " to " + g.homeScore + ".");
                league.newsHeadlines.add(g.awayTeam.getName() + " wins the " + g.gameName + "!");
            }
        }
    }

    public String getGameSummaryBowl(Game g) {
        StringBuilder sb = new StringBuilder();
        if (!g.hasPlayed) {
            return g.homeTeam.strRep() + " vs " + g.awayTeam.strRep();
        }
        if (g.homeScore > g.awayScore) {
            sb.append(g.homeTeam.strRep() + " W ");
            sb.append(g.homeScore + "-" + g.awayScore + " ");
            sb.append("vs " + g.awayTeam.strRep());
        } else {
            sb.append(g.awayTeam.strRep() + " W ");
            sb.append(g.awayScore + "-" + g.homeScore + " ");
            sb.append("@ " + g.homeTeam.strRep());
        }
        return sb.toString();
    }

    public String ncgSummaryStr() {
        if (league.ncg.homeScore > league.ncg.awayScore) {
            return league.ncg.homeTeam.getName() + " (" + league.ncg.homeTeam.getWins() + "-" + league.ncg.homeTeam.getLosses()
                    + ") won the National Championship, winning against " + league.ncg.awayTeam.getName() + " ("
                    + league.ncg.awayTeam.getWins() + "-" + league.ncg.awayTeam.getLosses() + ") in the NCG "
                    + league.ncg.homeScore + "-" + league.ncg.awayScore + ".";
        } else {
            return league.ncg.awayTeam.getName() + " (" + league.ncg.awayTeam.getWins() + "-" + league.ncg.awayTeam.getLosses()
                    + ") won the National Championship, winning against " + league.ncg.homeTeam.getName() + " ("
                    + league.ncg.homeTeam.getWins() + "-" + league.ncg.homeTeam.getLosses() + ") in the NCG "
                    + league.ncg.awayScore + "-" + league.ncg.homeScore + ".";
        }
    }
}
