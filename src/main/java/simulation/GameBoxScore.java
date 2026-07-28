package simulation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import comparator.CompPlayerPosition;
import positions.Player;
import staff.HeadCoach;

class GameBoxScore {

    private final Game game;
    private final DecimalFormat df2 = new DecimalFormat("#.##");
    private static final DecimalFormat DF2_DOT = new DecimalFormat(".##");

    GameBoxScore(Game game) {
        this.game = game;
    }

    void buildStatistics() {
        Player player;
        game.homePassingStats = new ArrayList<>();
        game.awayPassingStats = new ArrayList<>();
        game.homeRushingStats = new ArrayList<>();
        game.awayRushingStats = new ArrayList<>();
        game.homeReceivingStats = new ArrayList<>();
        game.awayReceivingStats = new ArrayList<>();
        game.homeKickingStats = new ArrayList<>();
        game.awayKickingStats = new ArrayList<>();
        game.homeDefenseStats = new ArrayList<>();
        game.awayDefenseStats = new ArrayList<>();

        List<Player> allHomePlayers = game.homeTeam.getAllPlayers();
        List<Player> allAwayPlayers = game.awayTeam.getAllPlayers();

        for (int i = 0; i < allHomePlayers.size(); ++i) {
            if (allHomePlayers.get(i).gamePassAtempts > 0) {
                player = allHomePlayers.get(i);
                game.homePassingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gamePassYards + "," + player.gamePassComplete + "," + player.gamePassAtempts + "," + player.gamePassTDs + "," + player.gamePassInts + "," + player.gameSacks);
            }
        }

        for (int i = 0; i < allAwayPlayers.size(); ++i) {
            if (allAwayPlayers.get(i).gamePassAtempts > 0) {
                player = allAwayPlayers.get(i);
                game.awayPassingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gamePassYards + "," + player.gamePassComplete + "," + player.gamePassAtempts + "," + player.gamePassTDs + "," + player.gamePassInts + "," + player.gameSacks);
            }
        }

        for (int i = 0; i < allHomePlayers.size(); ++i) {
            if (allHomePlayers.get(i).gameRushAttempts > 0) {
                player = allHomePlayers.get(i);
                game.homeRushingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameRushYards + "," + player.gameRushAttempts + "," + player.gameRushTDs + "," + player.gameFumbles);
            }
        }

        for (int i = 0; i < allAwayPlayers.size(); ++i) {
            if (allAwayPlayers.get(i).gameRushAttempts > 0) {
                player = allAwayPlayers.get(i);
                game.awayRushingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameRushYards + "," + player.gameRushAttempts + "," + player.gameRushTDs + "," + player.gameFumbles);
            }
        }

        for (int i = 0; i < allHomePlayers.size(); ++i) {
            if (allHomePlayers.get(i).gameReceptions > 0) {
                player = allHomePlayers.get(i);
                game.homeReceivingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameRecYards + "," + player.gameReceptions + "," + player.gameTargets + "," + player.gameRecTDs + "," + player.gameDrops);
            }
        }

        for (int i = 0; i < allAwayPlayers.size(); ++i) {
            if (allAwayPlayers.get(i).gameReceptions > 0) {
                player = allAwayPlayers.get(i);
                game.awayReceivingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameRecYards + "," + player.gameReceptions + "," + player.gameTargets + "," + player.gameRecTDs + "," + player.gameDrops);
            }
        }

        player = game.homeTeam.getK(0);
        if (player != null) {
            game.homeKickingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameFGMade + "," + player.gameFGAttempts + "," + player.gameXPMade + "," + player.gameXPAttempts);
        }
        player = game.awayTeam.getK(0);
        if (player != null) {
            game.awayKickingStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameFGMade + "," + player.gameFGAttempts + "," + player.gameXPMade + "," + player.gameXPAttempts);
        }

        for (int i = 0; i < allHomePlayers.size(); ++i) {
            if (allHomePlayers.get(i).gameTackles > 0) {
                player = allHomePlayers.get(i);
                game.homeDefenseStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameTackles + "," + player.gameSacks + "," + player.gameFumbles + "," + player.gameInterceptions + "," + player.gameTargets + "," + player.gameDefended);
            }

        }

        for (int i = 0; i < allAwayPlayers.size(); ++i) {
            if (allAwayPlayers.get(i).gameTackles > 0) {
                player = allAwayPlayers.get(i);
                game.awayDefenseStats.add(player.getInitialName() + "," + player.team.getName() + "," + player.position + "," + player.gameTackles + "," + player.gameSacks + "," + player.gameFumbles + "," + player.gameInterceptions + "," + player.gameTargets + "," + player.gameDefended);
            }
        }

        if (game.homeKickReturner != null && game.homeKickReturner.kReturns > 0)
            game.hkReturnAvg = (double) game.homeKickReturner.kYards / game.homeKickReturner.kReturns;
        if (game.awayKickReturner != null && game.awayKickReturner.kReturns > 0)
            game.akReturnAvg = (double) game.awayKickReturner.kYards / game.awayKickReturner.kReturns;

        if (game.homeKickReturner != null && game.homeKickReturner.pReturns > 0)
            game.hpReturnAvg = (double) game.homeKickReturner.pYards / game.homeKickReturner.pReturns;
        if (game.awayKickReturner != null && game.awayKickReturner.pReturns > 0)
            game.apReturnAvg = (double) game.awayKickReturner.pYards / game.awayKickReturner.pReturns;

        game.recordReturnStats();

    }


    public String[] getGameSummaryStrV2() {

        String[] gameSum = new String[19];
        StringBuilder gameL = new StringBuilder();
        StringBuilder gameC = new StringBuilder();
        StringBuilder gameR = new StringBuilder();

        gameL.append("\nPoints\nYards\nPass Yards\nRush Yards\nTOs\n");
        gameC.append("#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getAbbr() + "\n" + game.awayScore + "\n" + game.awayYards + " yds\n" +
                game.awayPassYards + " pyds\n" + game.awayRushYards + " ryds\n" + game.awayTOs + " TOs\n");
        gameR.append("#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getAbbr() + "\n" + game.homeScore + "\n" + game.homeYards + " yds\n" +
                game.homePassYards + " pyds\n" + game.homeRushYards + " ryds\n" + game.homeTOs + " TOs\n");

        StringBuilder gamePL = new StringBuilder();
        StringBuilder gamePC = new StringBuilder();
        StringBuilder gamePR = new StringBuilder();

        gamePL.append("\n");
        gamePC.append("[PASSING]\n");
        gamePR.append("\n");
        gamePL.append("\n");
        gamePC.append("\n");
        gamePR.append("\n");

        if (game.homePassingStats.size() >= game.awayPassingStats.size()) {
            for (int i = 0; i < game.homePassingStats.size(); ++i) {
                gamePL.append("QB:" + "\nYards:" + "\nComp/Att:" + "\nPass TDs:" + "\nPass Ints:" + "\nSacks:" + "\nRating:" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayPassingStats.size(); ++i) {
                gamePL.append("QB:" + "\nYards:" + "\nComp/Att:" + "\nPass TDs:" + "\nPass Ints:" + "\nSacks:" + "\nRating:" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayPassingStats.size(); ++i) {
            String[] stats = game.awayPassingStats.get(i).split(",");
            gamePC.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n" + stats[8] + "\n" + getPasserRating(Integer.parseInt(stats[3]), Integer.parseInt(stats[6]), Integer.parseInt(stats[4]), Integer.parseInt(stats[5]), Integer.parseInt(stats[7])) + "\n\n");
        }
        for (int i = 0; i < game.homePassingStats.size(); ++i) {
            String[] stats = game.homePassingStats.get(i).split(",");
            gamePR.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n" + stats[8] + "\n" + getPasserRating(Integer.parseInt(stats[3]), Integer.parseInt(stats[6]), Integer.parseInt(stats[4]), Integer.parseInt(stats[5]), Integer.parseInt(stats[7])) + "\n\n");
        }


        StringBuilder gameRL = new StringBuilder();
        StringBuilder gameRC = new StringBuilder();
        StringBuilder gameRR = new StringBuilder();

        gameRL.append("\n");
        gameRC.append("[RUSHING]\n");
        gameRR.append("\n");
        gameRL.append("\n");
        gameRC.append("\n");
        gameRR.append("\n");

        if (game.awayRushingStats.size() >= game.homeRushingStats.size()) {
            for (int i = 0; i < game.awayRushingStats.size(); ++i) {
                gameRL.append("Name:" + "\nPosition:" + "\nYards:" + "\nCarries:" + "\nYards/Carry:" + "\nTDs:" + "\nFumbles:" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayRushingStats.size(); ++i) {
                gameRL.append("Name:" + "\nPosition:" + "\nYards:" + "\nCarries:" + "\nYards/Carry:" + "\nTDs:" + "\nFumbles:" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayRushingStats.size(); ++i) {
            String[] stats = game.awayRushingStats.get(i).split(",");
            gameRC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format((Double.parseDouble(stats[3]) / Double.parseDouble(stats[4]))) + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }
        for (int i = 0; i < game.homeRushingStats.size(); ++i) {
            String[] stats = game.homeRushingStats.get(i).split(",");
            gameRR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format((Double.parseDouble(stats[3]) / Double.parseDouble(stats[4]))) + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }


        StringBuilder gameWL = new StringBuilder();
        StringBuilder gameWC = new StringBuilder();
        StringBuilder gameWR = new StringBuilder();

        gameWL.append("\n");
        gameWC.append("[RECEIVING]\n");
        gameWR.append("\n");
        gameWL.append("\n");
        gameWC.append("\n");
        gameWR.append("\n");

        if (game.homeReceivingStats.size() >= game.awayReceivingStats.size()) {
            for (int i = 0; i < game.homeReceivingStats.size(); ++i) {
                gameWL.append("Name:" + "\nPosition:" + "\nYards:" + "\nReceptions:" + "\nYards/Rec:" + "\nRec/Targets:" + "\nTDs:" + "\nDrops:" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayReceivingStats.size(); ++i) {
                gameWL.append("Name:" + "\nPosition:" + "\nYards:" + "\nReceptions:" + "\nYards/Rec:" + "\nRec/Targets:" + "\nTDs:" + "\nDrops:" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayReceivingStats.size(); ++i) {
            String[] stats = game.awayReceivingStats.get(i).split(",");
            gameWC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format(getRecYardsperCatch(Double.parseDouble(stats[3]), Double.parseDouble(stats[4]))) + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n\n");
        }
        for (int i = 0; i < game.homeReceivingStats.size(); ++i) {
            String[] stats = game.homeReceivingStats.get(i).split(",");
            gameWR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format(getRecYardsperCatch(Double.parseDouble(stats[3]), Double.parseDouble(stats[4]))) + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n\n");
        }

        StringBuilder gameDL = new StringBuilder();
        StringBuilder gameDC = new StringBuilder();
        StringBuilder gameDR = new StringBuilder();

        gameDL.append("\n");
        gameDC.append("[DEFENDING]\n");
        gameDR.append("\n");
        gameDL.append("\n");
        gameDC.append("\n");
        gameDR.append("\n");

        if (game.homeDefenseStats.size() >= game.awayDefenseStats.size()) {
            for (int i = 0; i < game.homeDefenseStats.size(); ++i) {
                gameDL.append("Name:" + "\nPosition:" + "\nTackles:" + "\nSacks:" + "\nFumbles:" + "\nInts:" + "\nDefended:" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayDefenseStats.size(); ++i) {
                gameDL.append("Name:" + "\nPosition:" + "\nTackles:" + "\nSacks:" + "\nFumbles:" + "\nInts:" + "\nDefended:" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayDefenseStats.size(); ++i) {
            String[] stats = game.awayDefenseStats.get(i).split(",");
            gameDC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n" + stats[8] + "\n\n");
        }
        for (int i = 0; i < game.homeDefenseStats.size(); ++i) {
            String[] stats = game.homeDefenseStats.get(i).split(",");
            gameDR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n" + stats[8] + "\n\n");
        }

        StringBuilder gameKL = new StringBuilder();
        StringBuilder gameKC = new StringBuilder();
        StringBuilder gameKR = new StringBuilder();

        gameKL.append("\n");
        gameKC.append("[KICKING]\n");
        gameKR.append("\n");
        gameKL.append("\n");
        gameKC.append("\n");
        gameKR.append("\n");


        if (game.homeKickingStats.size() >= game.awayKickingStats.size()) {
            for (int i = 0; i < game.homeKickingStats.size(); ++i) {
                gameKL.append("Name:" + "\nFG Made:" + "\nFG Att:" + "\nXP Made:" + "\nXP Att:" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayKickingStats.size(); ++i) {
                gameKL.append("Name:" + "\nFG Made:" + "\nFG Att:" + "\nXP Made:" + "\nXP Att:" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayKickingStats.size(); ++i) {
            String[] stats = game.awayKickingStats.get(i).split(",");
            gameKC.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }
        for (int i = 0; i < game.homeKickingStats.size(); ++i) {
            String[] stats = game.homeKickingStats.get(i).split(",");
            gameKR.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }

        gameKL.append("\n");
        gameKC.append("[RETURNS]\n");
        gameKR.append("\n");
        gameKL.append("\n");
        gameKC.append("\n");
        gameKR.append("\n");

        gameKL.append("Name:" + "\nKick Rets:" + "\nK Ret Yrds:" + "\nK Yrds/Ret:" + "\nK Ret TDs" + "\nPunt Rets:" + "\nP Ret Yrds:" + "\nP Yrds/Ret:" + "\nP Ret TDs" + "\n\n");
        gameKC.append(game.awayKickReturner.getInitialName() + "\n" + game.awayKickReturner.kReturns + "\n" + game.awayKickReturner.kYards + "\n" + game.akReturnAvg + "\n" + game.awayKickReturner.kTD + "\n" + game.awayKickReturner.pReturns + "\n" + game.awayKickReturner.pYards + "\n" + game.apReturnAvg + "\n" + game.awayKickReturner.pTD + "\n\n");
        gameKR.append(game.homeKickReturner.getInitialName() + "\n" + game.homeKickReturner.kReturns + "\n" + game.homeKickReturner.kYards + "\n" + game.hkReturnAvg + "\n" + game.homeKickReturner.kTD + "\n" + game.homeKickReturner.pReturns + "\n" + game.homeKickReturner.pYards + "\n" + game.hpReturnAvg + "\n" + game.homeKickReturner.pTD + "\n\n");


        gameSum[0] = gameL.toString();
        gameSum[1] = gameC.toString();
        gameSum[2] = gameR.toString();

        gameSum[3] = gamePL.toString();
        gameSum[4] = gamePC.toString();
        gameSum[5] = gamePR.toString();

        gameSum[6] = gameRL.toString();
        gameSum[7] = gameRC.toString();
        gameSum[8] = gameRR.toString();

        gameSum[9] = gameWL.toString();
        gameSum[10] = gameWC.toString();
        gameSum[11] = gameWR.toString();

        gameSum[12] = gameDL.toString();
        gameSum[13] = gameDC.toString();
        gameSum[14] = gameDR.toString();

        gameSum[15] = gameKL.toString();
        gameSum[16] = gameKC.toString();
        gameSum[17] = gameKR.toString();

        gameSum[18] = getPlayByPlayLog();


        return gameSum;

    }

    public String[] getGameSummaryStr() {

        String[] gameSum = new String[19];
        StringBuilder gameL = new StringBuilder();
        StringBuilder gameC = new StringBuilder();
        StringBuilder gameR = new StringBuilder();

        gameL.append("\nPoints\nYards\nPass Yards\nRush Yards\nTOs\n\nOffense\nDefense\n");
        gameC.append("#" + game.awayTeam.getRankTeamPollScore() + " " + game.awayTeam.getName() + "\n" + game.awayScore + "\n" + game.awayYards + " yds\n" +
                game.awayPassYards + " pyds\n" + game.awayRushYards + " ryds\n" + game.awayTOs + " TOs\n\n" + game.awayTeam.getPlaybookOffense().getStratName() + "\n" + game.awayTeam.getPlaybookDefense().getStratName() + " \n");
        gameR.append("#" + game.homeTeam.getRankTeamPollScore() + " " + game.homeTeam.getName() + "\n" + game.homeScore + "\n" + game.homeYards + " yds\n" +
                game.homePassYards + " pyds\n" + game.homeRushYards + " ryds\n" + game.homeTOs + " TOs\n\n" + game.homeTeam.getPlaybookOffense().getStratName() + "\n" + game.homeTeam.getPlaybookDefense().getStratName() + " \n");

        StringBuilder gamePL = new StringBuilder();
        StringBuilder gamePC = new StringBuilder();
        StringBuilder gamePR = new StringBuilder();

        gamePL.append("[PASSING]\n");
        gamePC.append("\n");
        gamePR.append("\n");
        gamePL.append("\n");
        gamePC.append("\n");
        gamePR.append("\n");

        if (game.homePassingStats.size() >= game.awayPassingStats.size()) {
            for (int i = 0; i < game.homePassingStats.size(); ++i) {
                gamePL.append("QB" + "\nYards" + "\nComp/Att" + "\nPass TDs" + "\nPass Ints" + "\nSacks" + "\nRating" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayPassingStats.size(); ++i) {
                gamePL.append("QB" + "\nYards" + "\nComp/Att" + "\nPass TDs" + "\nPass Ints" + "\nSacks" + "\nRating" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayPassingStats.size(); ++i) {
            String[] stats = game.awayPassingStats.get(i).split(",");
            gamePC.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n" + stats[8] + "\n" + getPasserRating(Integer.parseInt(stats[3]), Integer.parseInt(stats[6]), Integer.parseInt(stats[4]), Integer.parseInt(stats[5]), Integer.parseInt(stats[7])) + "\n\n");
        }
        for (int i = 0; i < game.homePassingStats.size(); ++i) {
            String[] stats = game.homePassingStats.get(i).split(",");
            gamePR.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n" + stats[8] + "\n" + getPasserRating(Integer.parseInt(stats[3]), Integer.parseInt(stats[6]), Integer.parseInt(stats[4]), Integer.parseInt(stats[5]), Integer.parseInt(stats[7])) + "\n\n");
        }


        StringBuilder gameRL = new StringBuilder();
        StringBuilder gameRC = new StringBuilder();
        StringBuilder gameRR = new StringBuilder();

        gameRL.append("[RUSHING]\n");
        gameRC.append("\n");
        gameRR.append("\n");
        gameRL.append("\n");
        gameRC.append("\n");
        gameRR.append("\n");

        if (game.homeRushingStats.size() >= game.awayRushingStats.size()) {
            for (int i = 0; i < game.awayRushingStats.size(); ++i) {
                gameRL.append("Name" + "\nPosition" + "\nYards" + "\nCarries" + "\nYards/Carry" + "\nTDs" + "\nFumbles" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayRushingStats.size(); ++i) {
                gameRL.append("Name" + "\nPosition" + "\nYards" + "\nCarries" + "\nYards/Carry" + "\nTDs" + "\nFumbles" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayRushingStats.size(); ++i) {
            String[] stats = game.awayRushingStats.get(i).split(",");
            gameRC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format((Double.parseDouble(stats[3]) / Double.parseDouble(stats[4]))) + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }
        for (int i = 0; i < game.homeRushingStats.size(); ++i) {
            String[] stats = game.homeRushingStats.get(i).split(",");
            gameRR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format((Double.parseDouble(stats[3]) / Double.parseDouble(stats[4]))) + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }


        StringBuilder gameWL = new StringBuilder();
        StringBuilder gameWC = new StringBuilder();
        StringBuilder gameWR = new StringBuilder();

        gameWL.append("[RECEIVING]\n");
        gameWC.append("\n");
        gameWR.append("\n");
        gameWL.append("\n");
        gameWC.append("\n");
        gameWR.append("\n");

        if (game.homeReceivingStats.size() >= game.awayReceivingStats.size()) {
            for (int i = 0; i < game.homeReceivingStats.size(); ++i) {
                gameWL.append("Name" + "\nPosition" + "\nYards" + "\nReceptions" + "\nYards/Rec" + "\nRec/Targets" + "\nTDs" + "\nDrops" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayReceivingStats.size(); ++i) {
                gameWL.append("Name" + "\nPosition" + "\nYards" + "\nReceptions" + "\nYards/Rec" + "\nRec/Targets" + "\nTDs" + "\nDrops" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayReceivingStats.size(); ++i) {
            String[] stats = game.awayReceivingStats.get(i).split(",");
            gameWC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format(getRecYardsperCatch(Double.parseDouble(stats[3]), Double.parseDouble(stats[4]))) + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n\n");
        }
        for (int i = 0; i < game.homeReceivingStats.size(); ++i) {
            String[] stats = game.homeReceivingStats.get(i).split(",");
            gameWR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + DF2_DOT.format(getRecYardsperCatch(Double.parseDouble(stats[3]), Double.parseDouble(stats[4]))) + "\n" + stats[4] + "/" + stats[5] + "\n" + stats[6] + "\n" + stats[7] + "\n\n");
        }

        StringBuilder gameDL = new StringBuilder();
        StringBuilder gameDC = new StringBuilder();
        StringBuilder gameDR = new StringBuilder();

        gameDL.append("[DEFENDING]\n");
        gameDC.append("\n");
        gameDR.append("\n");
        gameDL.append("\n");
        gameDC.append("\n");
        gameDR.append("\n");

        if (game.homeDefenseStats.size() >= game.awayDefenseStats.size()) {
            for (int i = 0; i < game.homeDefenseStats.size(); ++i) {
                gameDL.append("Name" + "\nPosition" + "\nTackles" + "\nSacks" + "\nFumbles" + "\nInts" + "\nDefended" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayDefenseStats.size(); ++i) {
                gameDL.append("Name" + "\nPosition" + "\nTackles" + "\nSacks" + "\nFumbles" + "\nInts" + "\nDefended" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayDefenseStats.size(); ++i) {
            String[] stats = game.awayDefenseStats.get(i).split(",");
            gameDC.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n" + stats[8] + "\n\n");
        }
        for (int i = 0; i < game.homeDefenseStats.size(); ++i) {
            String[] stats = game.homeDefenseStats.get(i).split(",");
            gameDR.append(stats[0] + "\n" + stats[2] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n" + stats[8] + "\n\n");
        }

        StringBuilder gameKL = new StringBuilder();
        StringBuilder gameKC = new StringBuilder();
        StringBuilder gameKR = new StringBuilder();

        gameKL.append("[KICKING]\n");
        gameKC.append("\n");
        gameKR.append("\n");
        gameKL.append("\n");
        gameKC.append("\n");
        gameKR.append("\n");


        if (game.homeKickingStats.size() >= game.awayKickingStats.size()) {
            for (int i = 0; i < game.homeKickingStats.size(); ++i) {
                gameKL.append("Name" + "\nFG Made" + "\nFG Att" + "\nXP Made" + "\nXP Att" + "\n\n");
            }
        } else {
            for (int i = 0; i < game.awayKickingStats.size(); ++i) {
                gameKL.append("Name" + "\nFG Made" + "\nFG Att" + "\nXP Made" + "\nXP Att" + "\n\n");
            }
        }

        for (int i = 0; i < game.awayKickingStats.size(); ++i) {
            String[] stats = game.awayKickingStats.get(i).split(",");
            gameKC.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }
        for (int i = 0; i < game.homeKickingStats.size(); ++i) {
            String[] stats = game.homeKickingStats.get(i).split(",");
            gameKR.append(stats[0] + "\n" + stats[3] + "\n" + stats[4] + "\n" + stats[5] + "\n" + stats[6] + "\n\n");
        }

        gameKL.append("[RETURNS]\n");
        gameKC.append("\n");
        gameKR.append("\n");
        gameKL.append("\n");
        gameKC.append("\n");
        gameKR.append("\n");

        gameKL.append("Name" + "\nKick Rets" + "\nK Ret Yrds" + "\nK Yrds/Ret" + "\nK Ret TDs" + "\nPunt Rets" + "\nP Ret Yrds" + "\nP Yrds/Ret" + "\nP Ret TDs" + "\n\n");
        gameKC.append(game.awayKickReturner.getInitialName() + "\n" + game.awayKickReturner.kReturns + "\n" + game.awayKickReturner.kYards + "\n" + game.akReturnAvg + "\n" + game.awayKickReturner.kTD + "\n" + game.awayKickReturner.pReturns + "\n" + game.awayKickReturner.pYards + "\n" + game.apReturnAvg + "\n" + game.awayKickReturner.pTD + "\n\n");
        gameKR.append(game.homeKickReturner.getInitialName() + "\n" + game.homeKickReturner.kReturns + "\n" + game.homeKickReturner.kYards + "\n" + game.hkReturnAvg + "\n" + game.homeKickReturner.kTD + "\n" + game.homeKickReturner.pReturns + "\n" + game.homeKickReturner.pYards + "\n" + game.hpReturnAvg + "\n" + game.homeKickReturner.pTD + "\n\n");


        gameSum[0] = gameL.toString();
        gameSum[1] = gameC.toString();
        gameSum[2] = gameR.toString();

        gameSum[3] = gamePL.toString();
        gameSum[4] = gamePC.toString();
        gameSum[5] = gamePR.toString();

        gameSum[6] = gameRL.toString();
        gameSum[7] = gameRC.toString();
        gameSum[8] = gameRR.toString();

        gameSum[9] = gameWL.toString();
        gameSum[10] = gameWC.toString();
        gameSum[11] = gameWR.toString();

        gameSum[12] = gameDL.toString();
        gameSum[13] = gameDC.toString();
        gameSum[14] = gameDR.toString();

        gameSum[15] = gameKL.toString();
        gameSum[16] = gameKC.toString();
        gameSum[17] = gameKR.toString();

        gameSum[18] = getPlayByPlayLog();


        return gameSum;

    }

    public String getPlayByPlayLog() {
        if (game.gameEventLog.length() == 0) {
            return "No play-by-play data available for this game.";
        }
        return "GAME PLAY-BY-PLAY LOG\n" + game.gameEventLog.toString();
    }

    public String[] getGameScoutStr() {
        String[] gameSum = new String[4];
        StringBuilder gameL = new StringBuilder();
        StringBuilder gameC = new StringBuilder();
        StringBuilder gameR = new StringBuilder();

        HeadCoach homeHC = game.homeTeam.getHeadCoach();
        HeadCoach awayHC = game.awayTeam.getHeadCoach();
        int homeRating = (int) ((homeHC != null ? homeHC.ratDef + homeHC.ratOff : 0) + 3*game.homeTeam.getTeamOffTalent() + 3*game.homeTeam.getTeamDefTalent() + 3)/8;
        int awayRating = (int) ((awayHC != null ? awayHC.ratDef + awayHC.ratOff : 0) + 3*game.awayTeam.getTeamOffTalent() + 3*game.awayTeam.getTeamDefTalent())/8;

        gameL.append("Ranking\nRecord\nPPG\nOpp PPG\nYPG\nOpp YPG\n" +
                "\nPass YPG\nRush YPG\nOpp PYPG\nOpp RYPG\n\nOff Talent\nDef Talent\nPrestige\n\nHC\nHC Ovr\nHC Off\nOffense\nHC Def\nDefense\n\nFavorite");
        int g = Math.max(1, game.awayTeam.numGames());
        Team t = game.awayTeam;
        gameC.append("#" + t.getRankTeamPollScore() + " " + t.getAbbr() + "\n" + t.getWins() + "-" + t.getLosses() + "\n" +
                t.getTeamPoints() / g + " (" + t.getRankTeamPoints() + ")\n" + t.getTeamOppPoints() / g + " (" + t.getRankTeamOppPoints() + ")\n" +
                t.getTeamYards() / g + " (" + t.getRankTeamYards() + ")\n" + t.getTeamOppYards() / g + " (" + t.getRankTeamOppYards() + ")\n\n" +
                t.getTeamPassYards() / g + " (" + t.getRankTeamPassYards() + ")\n" + t.getTeamRushYards() / g + " (" + t.getRankTeamRushYards() + ")\n" +
                t.getTeamOppPassYards() / g + " (" + t.getRankTeamOppPassYards() + ")\n" + t.getTeamOppRushYards() / g + " (" + t.getRankTeamOppRushYards() + ")\n\n" +
                df2.format(t.getTeamOffTalent()) + " (" + t.getRankTeamOffTalent() + ")\n" + df2.format(t.getTeamDefTalent()) + " (" + t.getRankTeamDefTalent() + ")\n" +
                t.getTeamPrestige() + " (" + t.getRankTeamPrestige() + ")\n\n"
                + (t.getHeadCoach() != null ? t.getHeadCoach().getInitialName() : "None") + "\n" +
                (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratOvr) : "0") + "\n" + (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratOff) : "0") + "\n" + t.getPlaybookOffense().getStratName() + "\n" + (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratDef) : "0") + "\n" + t.getPlaybookDefense().getStratName() +
                "\n\n" + getFavorite(homeRating, awayRating, false));
        g = Math.max(1, game.homeTeam.numGames());
        t = game.homeTeam;
        gameR.append("#" + t.getRankTeamPollScore() + " " + t.getAbbr() + "\n" + t.getWins() + "-" + t.getLosses() + "\n" +
                t.getTeamPoints() / g + " (" + t.getRankTeamPoints() + ")\n" + t.getTeamOppPoints() / g + " (" + t.getRankTeamOppPoints() + ")\n" +
                t.getTeamYards() / g + " (" + t.getRankTeamYards() + ")\n" + t.getTeamOppYards() / g + " (" + t.getRankTeamOppYards() + ")\n\n" +
                t.getTeamPassYards() / g + " (" + t.getRankTeamPassYards() + ")\n" + t.getTeamRushYards() / g + " (" + t.getRankTeamRushYards() + ")\n" +
                t.getTeamOppPassYards() / g + " (" + t.getRankTeamOppPassYards() + ")\n" + t.getTeamOppRushYards() / g + " (" + t.getRankTeamOppRushYards() + ")\n\n" +
                df2.format(t.getTeamOffTalent()) + " (" + t.getRankTeamOffTalent() + ")\n" + df2.format(t.getTeamDefTalent()) + " (" + t.getRankTeamDefTalent() + ")\n" +
                t.getTeamPrestige() + " (" + t.getRankTeamPrestige() + ")\n\n" +
                (t.getHeadCoach() != null ? t.getHeadCoach().getInitialName() : "None") + "\n" +
                (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratOvr) : "0") + "\n" + (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratOff) : "0") + "\n" + t.getPlaybookOffense().getStratName() + "\n" + (t.getHeadCoach() != null ? String.valueOf(t.getHeadCoach().ratDef) : "0") + "\n" + t.getPlaybookDefense().getStratName() +
                "\n\n" + getFavorite(homeRating, awayRating, true));

        gameSum[0] = gameL.toString();
        gameSum[1] = gameC.toString();
        gameSum[2] = gameR.toString();

        StringBuilder gameScout = new StringBuilder();
        if (game.awayTeam.getPlayersInjured() != null && !game.awayTeam.getPlayersInjured().isEmpty()) {
            ArrayList<Player> awayInjured = new ArrayList<>(game.awayTeam.getPlayersInjured());
            Collections.sort(awayInjured, new CompPlayerPosition());
            gameScout.append("\n" + game.awayTeam.getAbbr() + " Injury Report:\n");
            for (Player p : awayInjured) {
                gameScout.append(p.getPosNameYrOvrPot_OneLine() + "\n");
            }
        }
        if (game.homeTeam.getPlayersInjured() != null && !game.homeTeam.getPlayersInjured().isEmpty()) {
            ArrayList<Player> homeInjured = new ArrayList<>(game.homeTeam.getPlayersInjured());
            Collections.sort(homeInjured, new CompPlayerPosition());
            gameScout.append("\n" + game.homeTeam.getAbbr() + " Injury Report:\n");
            for (Player p : homeInjured) {
                gameScout.append(p.getPosNameYrOvrPot_OneLine() + "\n");
            }
        }

        gameSum[3] = gameScout.toString();

        return gameSum;
    }

    private String getFavorite(int home, int away, boolean hometeam) {
        String fav = "";

        if (hometeam) {
            if (home > away) {
                fav = "by " + 2*(home-away);
            }
        } else {
            if (away > home) {
                fav = "by " + 2*(away-home);
            }
        }

        return fav;
    }

    private int getPasserRating(int yards, int td, int comp, int att, int ints) {
        int rating;
        if (att < 1) {
            return 0;
        } else {
            rating = (int) (((8.4 * yards + (300 * td) + (100 * comp) - (200 * ints)) / att));
            return rating;
        }
    }

    private double getRecYardsperCatch(double yards, double rec) {
        double rating;
        if (rec < 1) {
            return 0;
        } else {
            rating = yards / rec;
            return rating;
        }
    }
}
