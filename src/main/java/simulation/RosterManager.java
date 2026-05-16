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

/**
 * Manages team roster operations: player lists, sorting, injuries,
 * redshirts, and transfers. Decoupled from Team via the Team reference
 * passed at construction.
 */
public class RosterManager {

    private final Team team;

    RosterManager(Team team) {
        this.team = team;
    }

    public ArrayList<Player> getAllPlayers() {
        ArrayList<Player> allPlayersList = new ArrayList<>();
        allPlayersList.addAll(team.teamQBs);
        allPlayersList.addAll(team.teamRBs);
        allPlayersList.addAll(team.teamWRs);
        allPlayersList.addAll(team.teamTEs);
        allPlayersList.addAll(team.teamOLs);
        allPlayersList.addAll(team.teamKs);
        allPlayersList.addAll(team.teamDLs);
        allPlayersList.addAll(team.teamLBs);
        allPlayersList.addAll(team.teamCBs);
        allPlayersList.addAll(team.teamSs);
        return allPlayersList;
    }

    public void sortPlayers() {
        Collections.sort(team.teamQBs, new CompPlayer());
        Collections.sort(team.teamRBs, new CompPlayer());
        Collections.sort(team.teamWRs, new CompPlayer());
        Collections.sort(team.teamTEs, new CompPlayer());
        Collections.sort(team.teamKs, new CompPlayer());
        Collections.sort(team.teamOLs, new CompPlayer());
        Collections.sort(team.teamDLs, new CompPlayer());
        Collections.sort(team.teamLBs, new CompPlayer());
        Collections.sort(team.teamCBs, new CompPlayer());
        Collections.sort(team.teamSs, new CompPlayer());
    }

    public void healInjury(int weeks) {
        ArrayList<Player> teamPlayers = getAllPlayers();
        for (Player z : teamPlayers) {
            if (z.injury != null && !z.isSuspended && !z.isTransfer) {
                z.injury.advanceGame(weeks);
                if (z.injury == null || !z.isInjured) {
                    team.playersInjured.remove(z);
                    sortByPosition(z);
                }
            }
        }
    }

    public void curePlayers() {
        curePlayersPosition(team.teamQBs);
        curePlayersPosition(team.teamRBs);
        curePlayersPosition(team.teamWRs);
        curePlayersPosition(team.teamTEs);
        curePlayersPosition(team.teamOLs);
        curePlayersPosition(team.teamKs);
        curePlayersPosition(team.teamDLs);
        curePlayersPosition(team.teamLBs);
        curePlayersPosition(team.teamCBs);
        curePlayersPosition(team.teamSs);
        team.playersInjured.clear();
        sortPlayers();
    }

    public void curePlayersPosition(ArrayList<? extends Player> players) {
        for (Player p : players) {
            p.injury = null;
            p.isInjured = false;
        }
    }

    public java.util.List<Player> getPlayersInjured() {
        return java.util.Collections.unmodifiableList(team.playersInjured);
    }

    public java.util.List<Player> getPlayersLeaving() {
        return java.util.Collections.unmodifiableList(team.playersLeaving);
    }

    public ArrayList<Player> getPlayersTransferring() {
        return team.playersTransferring;
    }

    public java.util.List<String> getRedshirtList() {
        return java.util.Collections.unmodifiableList(team.redshirtList);
    }

    public int getTeamSize() {
        int size = team.teamQBs.size() + team.teamRBs.size() + team.teamWRs.size() + team.teamTEs.size() + team.teamOLs.size() + team.teamKs.size() + team.teamDLs.size() + team.teamLBs.size() + team.teamCBs.size() + team.teamSs.size();
        return size;
    }

    public int getActivePlayers(int position) {
        int numPlayers = 0;
        switch (position) {
            case 0:
                for (Player p : team.teamQBs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamQBs.size() - numPlayers;
            case 1:
                for (Player p : team.teamRBs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamRBs.size() - numPlayers;
            case 2:
                for (Player p : team.teamWRs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamWRs.size() - numPlayers;
            case 3:
                for (Player p : team.teamTEs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamTEs.size() - numPlayers;
            case 4:
                for (Player p : team.teamOLs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamOLs.size() - numPlayers;
            case 5:
                for (Player p : team.teamKs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamKs.size() - numPlayers;
            case 6:
                for (Player p : team.teamDLs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamDLs.size() - numPlayers;
            case 7:
                for (Player p : team.teamLBs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamLBs.size() - numPlayers;
            case 8:
                for (Player p : team.teamCBs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamCBs.size() - numPlayers;
            case 9:
                for (Player p : team.teamSs) {
                    if (p.isRedshirt) numPlayers++;
                }
                return team.teamSs.size() - numPlayers;
        }
        return numPlayers;
    }

    public int countRedshirts() {
        int count = 0;
        java.util.List<Player> all = getAllPlayers();
        for (int i = 0; i < all.size(); ++i) {
            if (all.get(i).isRedshirt && !all.get(i).isTransfer) count++;
        }
        return count;
    }

    public void sortByPosition(Player p) {
        ArrayList<? extends Player> players;

        if (p.position.equals("QB")) {
            players = team.teamQBs;
        } else if (p.position.equals("RB")) {
            players = team.teamRBs;
        } else if (p.position.equals("WR")) {
            players = team.teamWRs;
        } else if (p.position.equals("TE")) {
            players = team.teamTEs;
        } else if (p.position.equals("OL")) {
            players = team.teamOLs;
        } else if (p.position.equals("K")) {
            players = team.teamKs;
        } else if (p.position.equals("DL")) {
            players = team.teamDLs;
        } else if (p.position.equals("LB")) {
            players = team.teamLBs;
        } else if (p.position.equals("CB")) {
            players = team.teamCBs;
        } else if (p.position.equals("S")) {
            players = team.teamSs;
        } else {
            return;
        }

        Collections.sort(players, new CompPlayer());
    }

    public void addPlayer(Player p) {
        if (p.position.equals("QB")) team.teamQBs.add(new PlayerQB(p, team));
        else if (p.position.equals("RB")) team.teamRBs.add(new PlayerRB(p, team));
        else if (p.position.equals("WR")) team.teamWRs.add(new PlayerWR(p, team));
        else if (p.position.equals("TE")) team.teamTEs.add(new PlayerTE(p, team));
        else if (p.position.equals("OL")) team.teamOLs.add(new PlayerOL(p, team));
        else if (p.position.equals("K")) team.teamKs.add(new PlayerK(p, team));
        else if (p.position.equals("DL")) team.teamDLs.add(new PlayerDL(p, team));
        else if (p.position.equals("LB")) team.teamLBs.add(new PlayerLB(p, team));
        else if (p.position.equals("CB")) team.teamCBs.add(new PlayerCB(p, team));
        else if (p.position.equals("S")) team.teamSs.add(new PlayerS(p, team));
    }
}
