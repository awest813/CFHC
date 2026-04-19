package antdroid.cfbcoach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static void importCoaches(InputStream inputStream, League league) throws IOException {
        if (inputStream == null) {
            throw new IOException("Unable to open custom coach import stream");
        }
        try (InputStream stream = inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null && !line.equals("END_COACHES")) {
                String[] fileSplit = line.split(",");
                if (fileSplit.length <= 1 || fileSplit[1].split(" ").length <= 1) {
                    continue;
                }

                for (Team team : league.getTeamList()) {
                    if (!fileSplit[0].equals(team.getName())) {
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
                        team.getHeadCoach().name = fileSplit[1];
                    }
                }
            }
        }

        league.resetPlaybooks();
    }

    public static void importRoster(InputStream inputStream, League league) throws IOException {
        if (inputStream == null) {
            throw new IOException("Unable to open custom roster import stream");
        }
        Map<Team, TeamRosterImportBuffer> rosterBuffers = createRosterBuffers(league);
        try (InputStream stream = inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null && !line.equals("END_ROSTER")) {
                String[] fileSplit = line.split(",");
                if (fileSplit.length <= 4 || fileSplit[1].split(" ").length <= 1) {
                    continue;
                }

                for (Team team : league.getTeamList()) {
                    if (!fileSplit[0].equals(team.getName())) {
                        continue;
                    }

                    addRosterPlayer(rosterBuffers.get(team), team, fileSplit, false);
                }
            }
        }

        applyRosterBuffers(rosterBuffers);
        refillMinimumRosters(league);
        league.updateTeamTalentRatings();
    }

    private static Map<Team, TeamRosterImportBuffer> createRosterBuffers(League league) {
        Map<Team, TeamRosterImportBuffer> buffers = new LinkedHashMap<>();
        for (Team team : league.getTeamList()) {
            buffers.put(team, new TeamRosterImportBuffer());
        }
        return buffers;
    }

    private static void applyRosterBuffers(Map<Team, TeamRosterImportBuffer> rosterBuffers) {
        for (Map.Entry<Team, TeamRosterImportBuffer> entry : rosterBuffers.entrySet()) {
            Team team = entry.getKey();
            TeamRosterImportBuffer buffer = entry.getValue();

            team.clearAllRosters();

            team.getTeamQBs().addAll(buffer.qbs);
            team.getTeamRBs().addAll(buffer.rbs);
            team.getTeamWRs().addAll(buffer.wrs);
            team.getTeamTEs().addAll(buffer.tes);
            team.getTeamOLs().addAll(buffer.ols);
            team.getTeamKs().addAll(buffer.ks);
            team.getTeamDLs().addAll(buffer.dls);
            team.getTeamLBs().addAll(buffer.lbs);
            team.getTeamCBs().addAll(buffer.cbs);
            team.getTeamSs().addAll(buffer.ss);
        }
    }

    private static void refillMinimumRosters(League league) {
        for (Team team : league.getTeamList()) {
            if (team.getAllPlayers().isEmpty()) {
                team.newRoster(team.minQBs, team.minRBs, team.minWRs, team.minTEs, team.minOLs, team.minKs, team.minDLs, team.minLBs, team.minCBs, team.minSs, true);
            } else {
                team.newRoster(team.minQBs - team.getTeamQBs().size(), team.minRBs - team.getTeamRBs().size(), team.minWRs - team.getTeamWRs().size(),
                        team.minTEs - team.getTeamTEs().size(), team.minOLs - team.getTeamOLs().size(), team.minKs - team.getTeamKs().size(),
                        team.minDLs - team.getTeamDLs().size(), team.minLBs - team.getTeamLBs().size(), team.minCBs - team.getTeamCBs().size(), team.minSs - team.getTeamSs().size(), false);
            }
        }
    }

    private static void addRosterPlayer(TeamRosterImportBuffer buffer, Team team, String[] fileSplit, boolean custom) {
        if (fileSplit.length <= 4) {
            return;
        }
        String playerName = fileSplit[1];
        int rating = Integer.parseInt(fileSplit[3]);
        int year = Integer.parseInt(fileSplit[4]);
        String position = fileSplit[2];

        if (position.equals("QB")) {
            buffer.qbs.add(new PlayerQB(playerName, rating, year, team, custom));
        } else if (position.equals("RB")) {
            buffer.rbs.add(new PlayerRB(playerName, rating, year, team, custom));
        } else if (position.equals("WR")) {
            buffer.wrs.add(new PlayerWR(playerName, rating, year, team, custom));
        } else if (position.equals("TE")) {
            buffer.tes.add(new PlayerTE(playerName, rating, year, team, custom));
        } else if (position.equals("OL")) {
            buffer.ols.add(new PlayerOL(playerName, rating, year, team, custom));
        } else if (position.equals("DL")) {
            buffer.dls.add(new PlayerDL(playerName, rating, year, team, custom));
        } else if (position.equals("LB")) {
            buffer.lbs.add(new PlayerLB(playerName, rating, year, team, custom));
        } else if (position.equals("CB")) {
            buffer.cbs.add(new PlayerCB(playerName, rating, year, team, custom));
        } else if (position.equals("S")) {
            buffer.ss.add(new PlayerS(playerName, rating, year, team, custom));
        } else if (position.equals("K")) {
            buffer.ks.add(new PlayerK(playerName, rating, year, team, custom));
        }
    }

    private static final class TeamRosterImportBuffer {
        private final ArrayList<PlayerQB> qbs = new ArrayList<>();
        private final ArrayList<PlayerRB> rbs = new ArrayList<>();
        private final ArrayList<PlayerWR> wrs = new ArrayList<>();
        private final ArrayList<PlayerTE> tes = new ArrayList<>();
        private final ArrayList<PlayerOL> ols = new ArrayList<>();
        private final ArrayList<PlayerK> ks = new ArrayList<>();
        private final ArrayList<PlayerDL> dls = new ArrayList<>();
        private final ArrayList<PlayerLB> lbs = new ArrayList<>();
        private final ArrayList<PlayerCB> cbs = new ArrayList<>();
        private final ArrayList<PlayerS> ss = new ArrayList<>();
    }
}
