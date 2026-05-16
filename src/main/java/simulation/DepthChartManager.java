package simulation;

import java.util.ArrayList;
import java.util.Collections;

import comparator.CompPlayer;
import positions.Player;
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
import staff.HeadCoach;

public class DepthChartManager {

    private final Team team;

    DepthChartManager(Team team) {
        this.team = team;
    }

    public java.util.List<? extends Player> getPositionList(String pos) {
        return switch (pos) {
            case "QB" -> team.teamQBs;
            case "RB" -> team.teamRBs;
            case "WR" -> team.teamWRs;
            case "TE" -> team.teamTEs;
            case "OL" -> team.teamOLs;
            case "K"  -> team.teamKs;
            case "DL" -> team.teamDLs;
            case "LB" -> team.teamLBs;
            case "CB" -> team.teamCBs;
            case "S"  -> team.teamSs;
            default   -> null;
        };
    }

    public boolean swapDepthChartOrder(String pos, int idxA, int idxB) {
        java.util.List<? extends Player> list = getPositionList(pos);
        if (list == null || idxA < 0 || idxB < 0 || idxA >= list.size() || idxB >= list.size()) {
            return false;
        }
        java.util.Collections.swap(list, idxA, idxB);
        return true;
    }

    public HeadCoach getHC() {
        return team.HC;
    }

    public PlayerQB getQB(int depth) {
        if (team.teamQBs.isEmpty()) return null;
        if (depth >= team.teamQBs.size()) depth = 0;
        return team.teamQBs.get(depth);
    }

    public PlayerRB getRB(int depth) {
        if (team.teamRBs.isEmpty()) return null;
        if (depth >= team.teamRBs.size()) depth = 0;
        return team.teamRBs.get(depth);
    }

    public PlayerWR getWR(int depth) {
        if (team.teamWRs.isEmpty()) return null;
        if (depth >= team.teamWRs.size()) depth = 0;
        return team.teamWRs.get(depth);
    }

    public PlayerTE getTE(int depth) {
        if (team.teamTEs.isEmpty()) return null;
        if (depth >= team.teamTEs.size()) depth = 0;
        return team.teamTEs.get(depth);
    }

    public PlayerK getK(int depth) {
        if (team.teamKs.isEmpty()) return null;
        if (depth >= team.teamKs.size()) depth = 0;
        return team.teamKs.get(depth);
    }

    public PlayerOL getOL(int depth) {
        if (team.teamOLs.isEmpty()) return null;
        if (depth >= team.teamOLs.size()) depth = 0;
        return team.teamOLs.get(depth);
    }

    public PlayerDL getDL(int depth) {
        if (team.teamDLs.isEmpty()) return null;
        if (depth >= team.teamDLs.size()) depth = 0;
        return team.teamDLs.get(depth);
    }

    public PlayerLB getLB(int depth) {
        if (team.teamLBs.isEmpty()) return null;
        if (depth >= team.teamLBs.size()) depth = 0;
        return team.teamLBs.get(depth);
    }

    public PlayerCB getCB(int depth) {
        if (team.teamCBs.isEmpty()) return null;
        if (depth >= team.teamCBs.size()) depth = 0;
        return team.teamCBs.get(depth);
    }

    public PlayerS getS(int depth) {
        if (team.teamSs.isEmpty()) return null;
        if (depth >= team.teamSs.size()) depth = 0;
        return team.teamSs.get(depth);
    }

    public void setStarters(ArrayList<Player> starters, int position) {
        switch (position) {
            case 0:
                ArrayList<PlayerQB> oldQBs = new ArrayList<>();
                oldQBs.addAll(team.teamQBs);
                team.teamQBs.clear();
                for (Player p : starters) {
                    team.teamQBs.add((PlayerQB) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamQBs, new CompPlayer());
                for (PlayerQB p : oldQBs) {
                    if (!team.teamQBs.contains(p)) {
                        team.teamQBs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 1:
                ArrayList<PlayerRB> oldRBs = new ArrayList<>();
                oldRBs.addAll(team.teamRBs);
                team.teamRBs.clear();
                for (Player p : starters) {
                    team.teamRBs.add((PlayerRB) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamRBs, new CompPlayer());
                for (PlayerRB p : oldRBs) {
                    if (!team.teamRBs.contains(p)) {
                        team.teamRBs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 2:
                ArrayList<PlayerWR> oldWRs = new ArrayList<>();
                oldWRs.addAll(team.teamWRs);
                team.teamWRs.clear();
                for (Player p : starters) {
                    team.teamWRs.add((PlayerWR) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamWRs, new CompPlayer());
                for (PlayerWR p : oldWRs) {
                    if (!team.teamWRs.contains(p)) {
                        team.teamWRs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 3:
                ArrayList<PlayerTE> oldTEs = new ArrayList<>();
                oldTEs.addAll(team.teamTEs);
                team.teamTEs.clear();
                for (Player p : starters) {
                    team.teamTEs.add((PlayerTE) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamTEs, new CompPlayer());
                for (PlayerTE p : oldTEs) {
                    if (!team.teamTEs.contains(p)) {
                        team.teamTEs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 4:
                ArrayList<PlayerOL> oldOLs = new ArrayList<>();
                oldOLs.addAll(team.teamOLs);
                team.teamOLs.clear();
                for (Player p : starters) {
                    team.teamOLs.add((PlayerOL) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamOLs, new CompPlayer());
                for (PlayerOL p : oldOLs) {
                    if (!team.teamOLs.contains(p)) {
                        team.teamOLs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 5:
                ArrayList<PlayerK> oldKs = new ArrayList<>();
                oldKs.addAll(team.teamKs);
                team.teamKs.clear();
                for (Player p : starters) {
                    team.teamKs.add((PlayerK) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamKs, new CompPlayer());
                for (PlayerK p : oldKs) {
                    if (!team.teamKs.contains(p)) {
                        team.teamKs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 6:
                ArrayList<PlayerDL> oldDLs = new ArrayList<>();
                oldDLs.addAll(team.teamDLs);
                team.teamDLs.clear();
                for (Player p : starters) {
                    team.teamDLs.add((PlayerDL) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamDLs, new CompPlayer());
                for (PlayerDL p : oldDLs) {
                    if (!team.teamDLs.contains(p)) {
                        team.teamDLs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 7:
                ArrayList<PlayerLB> oldLBs = new ArrayList<>();
                oldLBs.addAll(team.teamLBs);
                team.teamLBs.clear();
                for (Player p : starters) {
                    team.teamLBs.add((PlayerLB) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamLBs, new CompPlayer());
                for (PlayerLB p : oldLBs) {
                    if (!team.teamLBs.contains(p)) {
                        team.teamLBs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 8:
                ArrayList<PlayerCB> oldCBs = new ArrayList<>();
                oldCBs.addAll(team.teamCBs);
                team.teamCBs.clear();
                for (Player p : starters) {
                    team.teamCBs.add((PlayerCB) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamCBs, new CompPlayer());
                for (PlayerCB p : oldCBs) {
                    if (!team.teamCBs.contains(p)) {
                        team.teamCBs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
            case 9:
                ArrayList<PlayerS> oldSs = new ArrayList<>();
                oldSs.addAll(team.teamSs);
                team.teamSs.clear();
                for (Player p : starters) {
                    team.teamSs.add((PlayerS) p);
                    p.posDepth = 1;
                }
                Collections.sort(team.teamSs, new CompPlayer());
                for (PlayerS p : oldSs) {
                    if (!team.teamSs.contains(p)) {
                        team.teamSs.add(p);
                        p.posDepth = 2;
                    }
                }
                break;
        }
    }

    public void addGamePlayedPlayers() {
        java.util.List<Player> allPlayers = team.getAllPlayers();
        for (int i = 0; i < allPlayers.size(); ++i) {
            if (allPlayers.get(i).gameSnaps > 0) allPlayers.get(i).recordGame(1);
        }
    }

    public void addGamesStartedPlayers() {
        addGamePlayedList(team.teamQBs, team.startersQB);
        addGamePlayedList(team.teamRBs, team.startersRB);
        addGamePlayedList(team.teamWRs, team.startersWR);
        addGamePlayedList(team.teamTEs, team.startersTE);
        addGamePlayedList(team.teamOLs, team.startersOL);
        addGamePlayedList(team.teamKs, team.startersK);
        addGamePlayedList(team.teamDLs, team.startersDL);
        addGamePlayedList(team.teamLBs, team.startersLB);
        addGamePlayedList(team.teamCBs, team.startersCB);
        addGamePlayedList(team.teamSs, team.startersS);
    }

    public void addGamePlayedList(ArrayList<? extends Player> playerList, int starters) {
        for (int i = 0; i < starters; ++i) {
            playerList.get(i).recordGameStarted(1);
        }
    }

    public void setRosterDepth3() {
        for (Player p : team.getAllPlayers()) {
            if (p.isTransfer || p.isRedshirt || p.isMedicalRS) p.posDepth = 3;
        }
    }
}
