package simulation;

import comparator.CompPlayerHeisman;
import positions.Player;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Award candidate ranking helpers extracted from {@link League}.
 *
 * <p>Ceremony / news side-effects still live on League for now; this class owns
 * the pure candidate-collection + sort steps for major individual awards.
 */
public final class LeagueAwards {

    private LeagueAwards() {}

    /**
     * Ranked offensive Heisman / Offensive POTY candidates (QB/RB/WR/TE).
     */
    public static ArrayList<Player> getHeismanCandidates(League league) {
        ArrayList<Player> candidates = new ArrayList<>();
        for (int i = 0; i < league.teamList.size(); ++i) {
            Team team = league.teamList.get(i);
            for (int qb = 0; qb < team.getTeamQBs().size(); ++qb) {
                candidates.add(team.getTeamQBs().get(qb));
            }
            for (int rb = 0; rb < team.getTeamRBs().size(); ++rb) {
                candidates.add(team.getTeamRBs().get(rb));
            }
            for (int wr = 0; wr < team.getTeamWRs().size(); ++wr) {
                candidates.add(team.getTeamWRs().get(wr));
            }
            for (int te = 0; te < team.getTeamTEs().size(); ++te) {
                candidates.add(team.getTeamTEs().get(te));
            }
        }
        Collections.sort(candidates, new CompPlayerHeisman());
        return candidates;
    }

    /**
     * Ranked defensive POTY candidates (DL/LB/CB/S).
     */
    public static ArrayList<Player> getDefensivePotyCandidates(League league) {
        ArrayList<Player> candidates = new ArrayList<>();
        for (int i = 0; i < league.teamList.size(); ++i) {
            Team team = league.teamList.get(i);
            for (int dl = 0; dl < team.getTeamDLs().size(); ++dl) {
                candidates.add(team.getTeamDLs().get(dl));
            }
            for (int lb = 0; lb < team.getTeamLBs().size(); ++lb) {
                candidates.add(team.getTeamLBs().get(lb));
            }
            for (int cb = 0; cb < team.getTeamCBs().size(); ++cb) {
                candidates.add(team.getTeamCBs().get(cb));
            }
            for (int s = 0; s < team.getTeamSs().size(); ++s) {
                candidates.add(team.getTeamSs().get(s));
            }
        }
        Collections.sort(candidates, new CompPlayerHeisman());
        return candidates;
    }
}
