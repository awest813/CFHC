package antdroid.cfbcoach;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerK;
import positions.PlayerLB;
import positions.PlayerOL;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerS;
import positions.PlayerTE;
import positions.PlayerWR;
import simulation.League;
import simulation.Team;

public final class LeagueCustomDataImporter {
    private LeagueCustomDataImporter() {
    }

    public static void importCoaches(ContentResolver contentResolver, Uri uri, League league) throws IOException {
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null && !line.equals("END_COACHES")) {
                String[] fileSplit = line.split(",");
                if (fileSplit.length <= 1 || fileSplit[1].split(" ").length <= 1) {
                    continue;
                }

                for (Team team : league.teamList) {
                    if (!fileSplit[0].equals(team.name)) {
                        continue;
                    }

                    if (fileSplit.length > 4) {
                        if (fileSplit[2].equals("HC")) {
                            team.newCustomHeadCoach(fileSplit[1], Integer.parseInt(fileSplit[3]), Integer.parseInt(fileSplit[4]));
                        } else if (fileSplit[2].equals("OC")) {
                            team.newCustomOC(fileSplit[1], Integer.parseInt(fileSplit[3]), Integer.parseInt(fileSplit[4]));
                        } else if (fileSplit[2].equals("DC")) {
                            team.newCustomDC(fileSplit[1], Integer.parseInt(fileSplit[3]), Integer.parseInt(fileSplit[4]));
                        }
                    } else if (fileSplit.length > 3) {
                        if (fileSplit[2].equals("HC")) {
                            team.newCustomHeadCoach(fileSplit[1], Integer.parseInt(fileSplit[3]), 0);
                        } else if (fileSplit[2].equals("OC")) {
                            team.newCustomOC(fileSplit[1], Integer.parseInt(fileSplit[3]), 0);
                        } else if (fileSplit[2].equals("DC")) {
                            team.newCustomDC(fileSplit[1], Integer.parseInt(fileSplit[3]), 0);
                        }
                    } else if (fileSplit.length > 2) {
                        team.newCustomHeadCoach(fileSplit[1], Integer.parseInt(fileSplit[2]), 0);
                    } else {
                        team.HC.name = fileSplit[1];
                    }
                }
            }
        }

        league.resetPlaybooks();
    }

    public static void importRoster(ContentResolver contentResolver, Uri uri, League league) throws IOException {
        clearRosters(league);

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null && !line.equals("END_ROSTER")) {
                String[] fileSplit = line.split(",");
                if (fileSplit.length <= 1 || fileSplit[1].split(" ").length <= 1) {
                    continue;
                }

                for (Team team : league.teamList) {
                    if (!fileSplit[0].equals(team.name)) {
                        continue;
                    }

                    addRosterPlayer(team, fileSplit, false);
                }
            }
        }

        refillMinimumRosters(league);
        league.updateTeamTalentRatings();
    }

    private static void clearRosters(League league) {
        for (Team team : league.teamList) {
            team.teamQBs.clear();
            team.teamRBs.clear();
            team.teamWRs.clear();
            team.teamTEs.clear();
            team.teamOLs.clear();
            team.teamKs.clear();
            team.teamDLs.clear();
            team.teamLBs.clear();
            team.teamCBs.clear();
            team.teamSs.clear();
        }
    }

    private static void refillMinimumRosters(League league) {
        for (Team team : league.teamList) {
            if (team.getAllPlayers().isEmpty()) {
                team.newRoster(team.minQBs, team.minRBs, team.minWRs, team.minTEs, team.minOLs, team.minKs, team.minDLs, team.minLBs, team.minCBs, team.minSs, true);
            } else {
                team.newRoster(team.minQBs - team.teamQBs.size(), team.minRBs - team.teamRBs.size(), team.minWRs - team.teamWRs.size(),
                        team.minTEs - team.teamTEs.size(), team.minOLs - team.teamOLs.size(), team.minKs - team.teamKs.size(),
                        team.minDLs - team.teamDLs.size(), team.minLBs - team.teamLBs.size(), team.minCBs - team.teamCBs.size(), team.minSs - team.teamSs.size(), false);
            }
        }
    }

    private static void addRosterPlayer(Team team, String[] fileSplit, boolean custom) {
        String playerName = fileSplit[1];
        int rating = Integer.parseInt(fileSplit[3]);
        int year = Integer.parseInt(fileSplit[4]);
        String position = fileSplit[2];

        if (position.equals("QB")) {
            team.teamQBs.add(new PlayerQB(playerName, rating, year, team, custom));
        } else if (position.equals("RB")) {
            team.teamRBs.add(new PlayerRB(playerName, rating, year, team, custom));
        } else if (position.equals("WR")) {
            team.teamWRs.add(new PlayerWR(playerName, rating, year, team, custom));
        } else if (position.equals("TE")) {
            team.teamTEs.add(new PlayerTE(playerName, rating, year, team, custom));
        } else if (position.equals("OL")) {
            team.teamOLs.add(new PlayerOL(playerName, rating, year, team, custom));
        } else if (position.equals("DL")) {
            team.teamDLs.add(new PlayerDL(playerName, rating, year, team, custom));
        } else if (position.equals("LB")) {
            team.teamLBs.add(new PlayerLB(playerName, rating, year, team, custom));
        } else if (position.equals("CB")) {
            team.teamCBs.add(new PlayerCB(playerName, rating, year, team, custom));
        } else if (position.equals("S")) {
            team.teamSs.add(new PlayerS(playerName, rating, year, team, custom));
        } else if (position.equals("K")) {
            team.teamKs.add(new PlayerK(playerName, rating, year, team, custom));
        }
    }
}
