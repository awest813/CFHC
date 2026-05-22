package simulation;

import java.util.ArrayList;

class GameSerializer {

    static Team resolveTeamFromSave(League league, String rawName) {
        Team found = league.findTeam(rawName);
        if (found != null) {
            return found;
        }
        String nm = rawName == null ? "" : rawName.trim();
        if (nm.isEmpty()) {
            nm = "Unknown";
        }
        return new Team(nm, "FCS", "FCS Division", (int) (Math.random() * 40), "FCS1", 0, league, false);
    }

    static ArrayList<String> saveGameData(Game game) {
        ArrayList<String> gameData = new ArrayList<>();
        gameData.add(Boolean.toString(game.hasPlayed));
        gameData.add(game.homeTeam.getName());
        gameData.add(game.awayTeam.getName());
        gameData.add(game.gameName);
        gameData.add(game.gameEventLog.toString());
        gameData.add(game.homeScore + "," + game.awayScore + "," + game.homeYards + "," + game.awayYards + "," + game.homePassYards + "," + game.awayPassYards+ "," + game.homeRushYards + "," + game.awayRushYards+ "," + game.homeTOs + "," + game.awayTOs + "," + game.numOT);

        StringBuilder sb = new StringBuilder();
        for(int x = 0; x < game.homeQScore.length; x++) {
            sb.append(game.homeQScore[x]+ ",");
        }
        gameData.add(sb.toString());

        sb = new StringBuilder();
        for(int x = 0; x < game.awayQScore.length; x++) {
            sb.append(game.awayQScore[x]+ ",");
        }
        gameData.add(sb.toString());


        for(String x : game.homePassingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.awayPassingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.homeRushingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.awayRushingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.homeReceivingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.awayReceivingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.homeKickingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.awayKickingStats) {
            gameData.add(x + "%");
        }

        for(String x : game.homeDefenseStats) {
            gameData.add(x + "%");
        }

        for(String x : game.awayDefenseStats) {
            gameData.add(x + "%");
        }

       return gameData;
    }
}
