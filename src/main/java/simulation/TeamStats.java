package simulation;

import positions.Player;
import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerLB;
import positions.PlayerOL;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerS;
import positions.PlayerTE;
import positions.PlayerWR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TeamStats {

    private TeamStats() {
    }

    public static float getOffTalent(Team team) {
        if (team.league.currentWeek > team.league.regSeasonWeeks + 4) {
            int rat = 0;
            int count = 0;
            ArrayList<Player> p = team.getAllPlayers();
            for (int i = 0; i < p.size(); i++) {
                if (Arrays.asList(p.get(i).offensePos).contains(p.get(i).position) || Arrays.asList(p.get(i).olPos).contains(p.get(i).position)) {
                    rat += p.get(i).ratOvr;
                    count++;
                }
            }
            return (float) rat / count;
        } else {
            List<PlayerWR> wrs = team.getTeamWRs();
            List<PlayerRB> rbs = team.getTeamRBs();
            List<PlayerTE> tes = team.getTeamTEs();
            return (team.getQB(0).ratOvr * 5 +
                    wrs.get(0).ratOvr + wrs.get(1).ratOvr + wrs.get(2).ratOvr +
                    rbs.get(0).ratOvr + rbs.get(1).ratOvr + tes.get(0).ratOvr +
                    getCompositeOLPass(team) + getCompositeOLRush(team) + getOffSubTalent(team)) / 14;
        }
    }

    public static float getOffSubTalent(Team team) {
        return ((team.getQB(1).ratOvr + team.getRB(2).ratOvr + team.getWR(3).ratOvr + team.getWR(4).ratOvr + team.getTE(1).ratOvr + team.getOL(5).ratOvr + team.getOL(6).ratOvr) / 7);
    }

    public static float getDefTalent(Team team) {
        if (team.league.currentWeek > team.league.regSeasonWeeks + 4) {
            int rat = 0;
            int count = 0;
            ArrayList<Player> p = team.getAllPlayers();
            for (int i = 0; i < p.size(); i++) {
                if (Arrays.asList(p.get(i).defensePos).contains(p.get(i).position)) {
                    rat += p.get(i).ratOvr;
                    count++;
                }
            }
            return (float) rat / count;
        } else {
            return (getRushDef(team) + getPassDef(team)) / 2;
        }
    }

    public static float getCompositeFootIQ(Team team) {
        float comp = 0;
        comp += team.getQB(0).ratIntelligence * 5;
        comp += team.getRB(0).ratIntelligence + team.getRB(1).ratIntelligence;
        comp += team.getWR(0).ratIntelligence + team.getWR(1).ratIntelligence + team.getWR(2).ratIntelligence;
        comp += team.getTE(0).ratIntelligence;
        for (int i = 0; i < 5; ++i) {
            comp += team.getOL(i).ratIntelligence;
        }
        comp += team.getS(0).ratIntelligence * 4 + team.getS(1).ratIntelligence * 4;
        comp += team.getCB(0).ratIntelligence + team.getCB(1).ratIntelligence + team.getCB(2).ratIntelligence;
        for (int i = 0; i < 4; ++i) {
            comp += team.getDL(i).ratIntelligence;
        }
        for (int i = 0; i < 3; ++i) {
            comp += team.getLB(i).ratIntelligence;
        }
        comp += team.HC.ratDef * 4 + team.HC.ratOff * 4;
        comp += (team.getRB(2).ratIntelligence + team.getWR(3).ratIntelligence + team.getWR(4).ratIntelligence + team.getTE(1).ratIntelligence + team.getOL(5).ratIntelligence + team.getOL(6).ratIntelligence +
                team.getDL(4).ratIntelligence + team.getDL(5).ratIntelligence + team.getLB(3).ratIntelligence + team.getCB(4).ratIntelligence + 2 * team.getS(2).ratIntelligence) / 12;
        return comp / 43;
    }

    public static int getTeamDiscipline(Team team) {
        int rating = 0;
        ArrayList<Player> roster = team.getAllPlayers();
        for (int i = 0; i < roster.size(); ++i) {
            rating += roster.get(i).character;
        }
        return rating / roster.size();
    }

    public static double getTeamChemistry(Team team) {
        double rating = 0;
        ArrayList<Player> roster = team.getAllPlayers();
        for (int i = 0; i < roster.size(); ++i) {
            rating += roster.get(i).character;
            rating += roster.get(i).ratIntelligence;
        }
        return rating / (2 * roster.size());
    }

    public static int getStaffDiscipline(Team team) {
        return (3 * team.HC.ratDiscipline + team.OC.ratDiscipline + team.DC.ratDiscipline) / 5;
    }

    public static float getPassProf(Team team) {
        List<PlayerWR> wrs = team.getTeamWRs();
        List<PlayerTE> tes = team.getTeamTEs();
        float avgWRs = (wrs.get(0).ratOvr + wrs.get(1).ratOvr + wrs.get(2).ratOvr + tes.get(0).getRatCatch()) / 4;
        float avgSubs = (2 * team.getWR(3).getRatCatch() + team.getTE(1).getRatCatch() + team.getRB(0).getRatCatch() + team.getRB(1).getRatCatch() + team.getRB(2).getRatCatch()) / 6;
        return (2 * getCompositeOLPass(team) + team.getQB(0).ratOvr * 5 + avgWRs * 4 + team.HC.ratOff * 2 + avgSubs) / 14;
    }

    public static float getRushProf(Team team) {
        List<PlayerRB> rbs = team.getTeamRBs();
        List<PlayerQB> qbs = team.getTeamQBs();
        float avgRBs = (rbs.get(0).ratOvr + rbs.get(1).ratOvr) / 2;
        float QB = qbs.get(0).getRatSpeed();
        float avgSub = team.getRB(2).ratOvr;
        return (3 * getCompositeOLRush(team) + 4 * avgRBs + QB + 2 * team.HC.ratOff + avgSub) / 11;
    }

    public static float getPassDef(Team team) {
        List<PlayerCB> cbs = team.getTeamCBs();
        List<PlayerLB> lbs = team.getTeamLBs();
        List<PlayerS> ss = team.getTeamSs();
        float avgCBs = (cbs.get(0).ratOvr + cbs.get(1).ratOvr + cbs.get(2).ratOvr) / 3;
        float avgLBs = (lbs.get(0).getRatCoverage() + lbs.get(1).getRatCoverage() + lbs.get(2).getRatCoverage()) / 3;
        float S = (ss.get(0).getRatCoverage() + ss.get(1).getRatCoverage()) / 2;
        float def = (3 * avgCBs + avgLBs + S) / 5;
        float avgSub = (team.getLB(3).getRatCoverage() + team.getCB(3).ratOvr * 2 + team.getS(2).getRatCoverage()) / 4;
        return (def * 4 + ss.get(0).ratOvr + getCompositeDLPass(team) * 2 + 2 * team.HC.ratDef + avgSub) / 10;
    }

    public static float getRushDef(Team team) {
        return getCompositeDLRush(team);
    }

    public static float getCompositeOLPass(Team team) {
        List<PlayerOL> ols = team.getTeamOLs();
        float compositeOL = 0;
        for (int i = 0; i < 5; ++i) {
            compositeOL += (ols.get(i).getRatStrength() * 2 + ols.get(i).getRatPassBlock() * 2 + ols.get(i).getRatVision()) / 5;
        }
        compositeOL = compositeOL / 5;
        float avgSub = (team.getOL(5).ratOvr + team.getOL(6).ratOvr) / 2;
        return (9 * compositeOL + avgSub + 3 * team.HC.ratOff) / 13;
    }

    public static float getCompositeOLRush(Team team) {
        List<PlayerOL> ols = team.getTeamOLs();
        List<PlayerTE> tes = team.getTeamTEs();
        float compositeOL = 0;
        for (int i = 0; i < 5; ++i) {
            compositeOL += (ols.get(i).getRatStrength() * 2 + ols.get(i).getRatRunBlock() * 2 + ols.get(i).getRatVision()) / 5;
        }
        compositeOL = compositeOL / 5;
        float compositeTE = tes.get(0).getRatRunBlock();
        float avgSub = (2 * team.getOL(5).ratOvr + 2 * team.getOL(6).ratOvr + team.getTE(1).getRatRunBlock()) / 5;
        return (9 * compositeOL + 2 * compositeTE + 3 * team.HC.ratOff + avgSub) / 15;
    }

    public static float getCompositeDLPass(Team team) {
        List<PlayerDL> dls = team.getTeamDLs();
        float compositeDL = 0;
        for (int i = 0; i < 4; ++i) {
            compositeDL += (dls.get(i).getRatStrength() + dls.get(i).getRatPassRush()) / 2;
        }
        compositeDL = compositeDL / 4;
        float avgSub = team.getDL(4).ratOvr + team.getDL(5).ratOvr;
        return (5 * compositeDL + 2 * team.HC.ratDef + avgSub) / 8;
    }

    public static float getCompositeDLRush(Team team) {
        List<PlayerDL> dls = team.getTeamDLs();
        List<PlayerLB> lbs = team.getTeamLBs();
        List<PlayerS> ss = team.getTeamSs();
        float compositeDL = 0;
        float compositeLB = 0;
        float compositeS = 0;

        for (int i = 0; i < 4; ++i) {
            compositeDL += (dls.get(i).getRatStrength() + dls.get(i).getRatRunStop()) / 2;
        }
        compositeDL = compositeDL / 4;

        for (int i = 0; i < 3; ++i) {
            compositeLB += lbs.get(i).getRatRunStop();
        }
        compositeLB = compositeLB / 3;

        compositeS += ss.get(0).getRatRunStop() + ss.get(1).getRatRunStop();
        compositeS = compositeS / 2;

        float avgSub = (2 * team.getDL(4).ratOvr + 2 * team.getDL(5).ratOvr + team.getLB(3).getRatRunStop() + team.getS(2).getRatRunStop()) / 6;
        return (4 * compositeDL + 2 * compositeLB + 2 * compositeS + 2 * team.HC.ratDef + avgSub) / 11;
    }
}
