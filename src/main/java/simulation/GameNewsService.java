package simulation;

public class GameNewsService {

    private final Game game;

    public GameNewsService(Game game) {
        this.game = game;
    }

    public void addNewsStory() {

        if (game.gameName.equals("Conference"))
            game.homeTeam.league.addWeeklyScore(game.homeTeam.league.currentWeek + 1, game.homeTeam.getConference() + " " + game.gameName + ">#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " " + game.awayScore + "\n" + "#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " " + game.homeScore);
        else
            game.homeTeam.league.addWeeklyScore(game.homeTeam.league.currentWeek + 1, game.gameName + ">#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " " + game.awayScore + "\n" + "#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " " + game.homeScore);


        if (game.numOT >= 3) {
            Team winner, loser;
            int winScore, loseScore;
            if (game.awayScore > game.homeScore) {
                winner = game.awayTeam;
                loser = game.homeTeam;
                winScore = game.awayScore;
                loseScore = game.homeScore;
            } else {
                winner = game.homeTeam;
                loser = game.awayTeam;
                winScore = game.homeScore;
                loseScore = game.awayScore;
            }

            game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1,
                    game.numOT + "OT Thriller!>" + winner.getStrAbbrWL() + " and " + loser.getStrAbbrWL() + " played an absolutely thrilling game " +
                            "that went to " + game.numOT + " overtimes, with " + winner.getName() + " finally emerging victorious " + winScore + " to " + loseScore + ".");
        } else if (game.homeScore > game.awayScore && game.awayTeam.getLosses() == 1 && game.awayTeam.league.currentWeek > 5 && game.awayTeam.getRankTeamPollScore() < game.awayTeam.league.countTeam) {
            game.awayTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1,
                    "Undefeated no more! " + game.awayTeam.getName() + " suffers first loss!" +
                            ">" + game.homeTeam.getStrAbbrWL() + " hands " + game.awayTeam.getStrAbbrWL() +
                            " their first loss of the season, winning " + game.homeScore + " to " + game.awayScore + ".");
            game.awayTeam.league.addNewsHeadline("Undefeated no more! " + game.awayTeam.getName() + " suffers first loss!");

        } else if (game.awayScore > game.homeScore && game.homeTeam.getLosses() == 1 && game.homeTeam.league.currentWeek > 5 && game.homeTeam.getRankTeamPollScore() < game.homeTeam.league.countTeam) {
            game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1,
                    "Undefeated no more! " + game.homeTeam.getName() + " suffers first loss!" +
                            ">" + game.awayTeam.getStrAbbrWL() + " hands " + game.homeTeam.getStrAbbrWL() +
                            " their first loss of the season, winning " + game.awayScore + " to " + game.homeScore + ".");
            game.homeTeam.league.addNewsHeadline("Undefeated no more! " + game.homeTeam.getName() + " suffers first loss!");

        } else if (game.awayScore > game.homeScore && game.homeTeam.getRankTeamPollScore() < 20 &&
                (game.awayTeam.getRankTeamPollScore() - game.homeTeam.getRankTeamPollScore()) > 20 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
            game.awayTeam.league.addNewsStory(game.awayTeam.league.currentWeek + 1,
                    "Upset! " + game.awayTeam.getStrAbbrWL() + " beats " + game.homeTeam.getStrAbbrWL() +
                            ">#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " was able to pull off the upset on the road against #" +
                            game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ", winning " + game.awayScore + " to " + game.homeScore + ".");
            game.awayTeam.league.addNewsHeadline("Upset! " + game.awayTeam.getStrAbbrWL() + " beats " + game.homeTeam.getStrAbbrWL());

        } else if (game.homeScore > game.awayScore && game.awayTeam.getRankTeamPollScore() < 20 &&
                (game.homeTeam.getRankTeamPollScore() - game.awayTeam.getRankTeamPollScore()) > 20 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
            game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1,
                    "Upset! " + game.homeTeam.getStrAbbrWL() + " beats " + game.awayTeam.getStrAbbrWL() +
                            ">#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " was able to pull off the upset at home against #" +
                            game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + ", winning " + game.homeScore + " to " + game.awayScore + ".");
            game.homeTeam.league.addNewsHeadline("Upset! " + game.homeTeam.getStrAbbrWL() + " beats " + game.awayTeam.getStrAbbrWL());

        } else if (game.awayScore > game.homeScore && game.homeTeam.getRankTeamPollScore() < 40 && game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
            game.awayTeam.league.addNewsStory(game.awayTeam.league.currentWeek + 1,
                    "Upset! " + game.awayTeam.getStrAbbrWL() + " beats " + game.homeTeam.getStrAbbrWL() +
                            ">#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " was able to pull off the upset on the road against #" +
                            game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ", winning " + game.awayScore + " to " + game.homeScore + ".");
            game.awayTeam.league.addNewsHeadline("Upset! " + game.awayTeam.getStrAbbrWL() + " beats " + game.homeTeam.getStrAbbrWL());

        } else if (game.homeScore > game.awayScore && game.awayTeam.getRankTeamPollScore() < 20 && game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
            game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1,
                    "Upset! " + game.homeTeam.getStrAbbrWL() + " beats " + game.awayTeam.getStrAbbrWL() +
                            ">#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " was able to pull off the upset at home against #" +
                            game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + ", winning " + game.homeScore + " to " + game.awayScore + ".");
            game.homeTeam.league.addNewsHeadline("Upset! " + game.homeTeam.getStrAbbrWL() + " beats " + game.awayTeam.getStrAbbrWL());
        }


        if (game.homeTeam.league.currentWeek < 12) {
            if (game.awayTeam.getRankTeamPollScore() < 11 && game.homeTeam.getRankTeamPollScore() < 11) {
                if (game.awayScore > game.homeScore) {
                    game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " defeats #" +
                            game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">" + game.awayTeam.getStrAbbrWL() + " went on the road and beat " + game.homeTeam.getStrAbbrWL() + " today, " + game.awayScore + " - " + game.homeScore + ", in the Game of the Week.");
                    game.homeTeam.league.addNewsHeadline("#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " defeats #" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName());

                } else {
                    game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " defeats #" +
                            game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + ">" + game.homeTeam.getStrAbbrWL() + " defeated " + game.awayTeam.getStrAbbrWL() + " at home today, " + game.homeScore + " - " + game.awayScore + ", in an important game of the season between two Top 10 schools.");
                    game.awayTeam.league.addNewsHeadline("#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " defeats #" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName());

                }
            } else if (game.awayTeam.getRankTeamPollScore() < 26 && game.homeTeam.getRankTeamPollScore() < 26) {
                if (game.awayScore > game.homeScore) {
                    game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " defeats #" +
                            game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">" + game.awayTeam.getStrAbbrWL() + " defeated " + game.homeTeam.getStrAbbrWL() + " today, " + game.awayScore + " - " + game.homeScore + ", in a battle of two Top 25 ranked teams.");
                } else {
                    game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + " defeats #" +
                            game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + ">" + game.homeTeam.getStrAbbrWL() + " defeated " + game.awayTeam.getStrAbbrWL() + " at home today, " + game.homeScore + " - " + game.awayScore + ", in one of the big match-ups of the week.");
                }
            }
        }
    }

    public void addUpcomingGames(Team name) {
        if (name == game.awayTeam) {
            if (game.awayTeam.getRankTeamPollScore() < 11 && game.homeTeam.getRankTeamPollScore() < 11 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
                game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "Upcoming Game: #" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " vs #" +
                        game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">The premier game of the week has " + game.awayTeam.getStrAbbrWL() + " visiting " + game.homeTeam.getStrAbbrWL() + ", as these two Top Ten teams fight for a crucial playoff spot. " +
                        game.awayTeam.getName() + " plays a " + game.awayTeam.getPlaybookOffense().getStratName() + " offense, which is averaging " + (game.awayTeam.getTeamYards() / Math.max(1, game.awayTeam.numGames())) + " yards per game. " + game.homeTeam.getName() + " plays a " +
                        game.homeTeam.getPlaybookOffense().getStratName() + " offense, averaging " + (game.homeTeam.getTeamYards() / Math.max(1, game.homeTeam.numGames())) + " yards per game.");
            } else if (game.awayTeam.getRankTeamPollScore() < 26 && game.homeTeam.getRankTeamPollScore() < 26 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
                game.homeTeam.league.addNewsStory(game.homeTeam.league.currentWeek + 1, "Upcoming Game: #" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " vs #" +
                        game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">Next week, " + game.awayTeam.getStrAbbrWL() + " visits " + game.homeTeam.getStrAbbrWL() + " in a battle of two ranked schools. " +
                        game.awayTeam.getName() + " plays a " + game.awayTeam.getPlaybookOffense().getStratName() + " offense, which is averaging " + (game.awayTeam.getTeamYards() / Math.max(1, game.awayTeam.numGames())) + " yards per game. " + game.homeTeam.getName() + " plays a " +
                        game.homeTeam.getPlaybookOffense().getStratName() + " offense, averaging " + (game.homeTeam.getTeamYards() / Math.max(1, game.homeTeam.numGames())) + " yards per game.");
            }
            if (game.awayTeam.league.currentWeek + 2 < game.awayTeam.league.regSeasonWeeks + 5)
                game.awayTeam.league.addWeeklyScore(game.homeTeam.league.currentWeek + 2, game.gameName + ">" + game.awayTeam.strRankTeamRecord() + "\n" + game.homeTeam.strRankTeamRecord());
        }
    }

    public void addNewSeasonGames(Team name) {
        if (name == game.awayTeam) {
            if (game.awayTeam.getRankTeamPollScore() < 11 && game.homeTeam.getRankTeamPollScore() < 11 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
                game.homeTeam.league.addNewsStory(0, "Kick-Off: #" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " vs #" +
                        game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">The season kicks off with an exciting game between vistors " + game.awayTeam.getName() + " and home team, " + game.homeTeam.getName() + ". " +
                        "These two teams are in the pre-season Top Ten, and both are expected to have big seasons this year. " + game.awayTeam.getName() + " plays a " + game.awayTeam.getPlaybookOffense().getStratName() + " offense, while " + game.homeTeam.getName() + " plays a " +
                        game.homeTeam.getPlaybookOffense().getStratName() + " offense.");

            } else if (game.awayTeam.getRankTeamPollScore() < 26 && game.homeTeam.getRankTeamPollScore() < 26 && !game.awayTeam.getName().contains("FCS") && !game.homeTeam.getName().contains("FCS")) {
                game.homeTeam.league.addNewsStory(0, "Kick-Off: #" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + " vs #" +
                        game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + ">The " + game.homeTeam.league.getYear() + " season starts off with " + game.awayTeam.getName() + " visiting " + game.homeTeam.getName()
                        + " in one of the interesting early season games pitting two ranked teams. " + game.awayTeam.getName() + " plays a " + game.awayTeam.getPlaybookOffense().getStratName() + " offense, while " + game.homeTeam.getName() + " plays a " +
                        game.homeTeam.getPlaybookOffense().getStratName() + " offense.");
            }
        }
        game.awayTeam.league.addWeeklyScore(1, game.gameName + ">" + game.awayTeam.strRankTeamRecord() + "\n" + game.homeTeam.strRankTeamRecord());

    }
}
