package simulation;

import java.util.ArrayList;

import positions.Player;

public class StatsTracker {

    private final Team team;

    StatsTracker(Team team) {
        this.team = team;
    }

    // Section 1: Team Proficiency / Talent

    public void updateTalentRatings() {
        team.teamOffTalent = this.getOffTalent();
        team.teamDefTalent = this.getDefTalent();
        team.teamPollScore = team.teamPrestige + team.teamOffTalent + team.teamDefTalent;
        team.teamChemistry = this.getTeamChemistry();
    }

    public float getOffTalent() {
        if (team.league.currentWeek > team.league.regSeasonWeeks + 4) {
            int rat = 0;
            int count = 0;
            ArrayList<Player> p = team.getAllPlayers();
            for (int i = 0; i < p.size(); i++) {
                if (p.get(i).offensePos.contains(p.get(i).position) || p.get(i).olPos.contains(p.get(i).position)) {
                    rat += p.get(i).ratOvr;
                    count++;
                }
            }
            return (float) rat / count;
        } else {
            int wr0 = team.teamWRs.size() > 0 ? team.teamWRs.get(0).ratOvr : 0;
            int wr1 = team.teamWRs.size() > 1 ? team.teamWRs.get(1).ratOvr : 0;
            int wr2 = team.teamWRs.size() > 2 ? team.teamWRs.get(2).ratOvr : 0;
            int rb0 = team.teamRBs.size() > 0 ? team.teamRBs.get(0).ratOvr : 0;
            int rb1 = team.teamRBs.size() > 1 ? team.teamRBs.get(1).ratOvr : 0;
            int te0 = team.teamTEs.size() > 0 ? team.teamTEs.get(0).ratOvr : 0;
            int qb0 = team.teamQBs.size() > 0 ? team.teamQBs.get(0).ratOvr : 0;
            return (qb0 * 5 +
                    wr0 + wr1 + wr2 +
                    rb0 + rb1 + te0 +
                    this.getCompositeOLPass() + this.getCompositeOLRush() + this.getOffSubTalent()) / 14;
        }
    }

    public float getOffSubTalent() {
        return ((team.teamQBs.size() > 1 ? team.teamQBs.get(1).ratOvr : 0) +
                (team.teamRBs.size() > 2 ? team.teamRBs.get(2).ratOvr : 0) +
                (team.teamWRs.size() > 3 ? team.teamWRs.get(3).ratOvr : 0) +
                (team.teamWRs.size() > 4 ? team.teamWRs.get(4).ratOvr : 0) +
                (team.teamTEs.size() > 1 ? team.teamTEs.get(1).ratOvr : 0) +
                (team.teamOLs.size() > 5 ? team.teamOLs.get(5).ratOvr : 0) +
                (team.teamOLs.size() > 6 ? team.teamOLs.get(6).ratOvr : 0)) / 7f;
    }

    public float getDefTalent() {
        if (team.league.currentWeek > team.league.regSeasonWeeks + 4) {
            int rat = 0;
            int count = 0;
            ArrayList<Player> p = team.getAllPlayers();
            for (int i = 0; i < p.size(); i++) {
                if (p.get(i).defensePos.contains(p.get(i).position)) {
                    rat += p.get(i).ratOvr;
                    count++;
                }
            }
            return count == 0 ? 0f : (float) rat / count;
        } else {
            return (this.getRushDef() + this.getPassDef()) / 2;
        }
    }

    public float getCompositeFootIQ() {
        float comp = 0;
        comp += iq(team.getQB(0)) * 5;
        comp += iq(team.getRB(0)) + iq(team.getRB(1));
        comp += iq(team.getWR(0)) + iq(team.getWR(1)) + iq(team.getWR(2));
        comp += iq(team.getTE(0));
        for (int i = 0; i < 5; ++i) {
            comp += iq(team.getOL(i));
        }
        comp += iq(team.getS(0)) * 4 + iq(team.getS(1)) * 4;
        comp += iq(team.getCB(0)) + iq(team.getCB(1)) + iq(team.getCB(2));
        for (int i = 0; i < 4; ++i) {
            comp += iq(team.getDL(i));
        }
        for (int i = 0; i < 3; ++i) {
            comp += iq(team.getLB(i));
        }
        if (team.HC != null) {
            comp += team.HC.ratDef * 4 + team.HC.ratOff * 4;
        }
        comp += (iq(team.getRB(2)) + iq(team.getWR(3)) + iq(team.getWR(4)) + iq(team.getTE(1))
                + iq(team.getOL(5)) + iq(team.getOL(6))
                + iq(team.getDL(4)) + iq(team.getDL(5)) + iq(team.getLB(3))
                + iq(team.getCB(4)) + 2 * iq(team.getS(2))) / 12f;
        return comp / 43;
    }

    public double teamPowerRating() {
        double rating = 0;
        this.updateTalentRatings();
        rating = (team.teamPrestige + 2 * team.teamOffTalent + 2 * team.teamDefTalent) / 5;
        return rating;
    }

    // Section 2: Offensive/Defensive Ratings

    public int offenseRating() {
        int offRating = 0;
        offRating = (team.league.countTeam - team.rankTeamPoints) + (team.league.countTeam - team.rankTeamYards);
        return offRating;
    }

    public int defenseRating() {
        int defRating = 0;
        defRating = (team.league.countTeam - team.rankTeamOppPoints) + (team.league.countTeam - team.rankTeamOppYards);
        return defRating;
    }

    public float getPassProf() {
        float wr0 = team.teamWRs.size() > 0 ? team.teamWRs.get(0).ratOvr : 0;
        float wr1 = team.teamWRs.size() > 1 ? team.teamWRs.get(1).ratOvr : 0;
        float wr2 = team.teamWRs.size() > 2 ? team.teamWRs.get(2).ratOvr : 0;
        float te0c = team.teamTEs.size() > 0 ? team.teamTEs.get(0).getRatCatch() : 0;
        float avgWRs = (wr0 + wr1 + wr2 + te0c) / 4;
        float avgSubs = (2 * catchRating(team.getWR(3)) + catchRating(team.getTE(1))
                + catchRating(team.getRB(0)) + catchRating(team.getRB(1)) + catchRating(team.getRB(2))) / 6;
        float qb = ovr(team.getQB(0));
        float hcOff = team.HC != null ? team.HC.ratOff : 0;
        return (2 * this.getCompositeOLPass() + qb * 5 + avgWRs * 4 + hcOff * 2 + avgSubs) / 14;
    }

    public float getRushProf() {
        float rb0 = team.teamRBs.size() > 0 ? team.teamRBs.get(0).ratOvr : 0;
        float rb1 = team.teamRBs.size() > 1 ? team.teamRBs.get(1).ratOvr : 0;
        float avgRBs = (rb0 + rb1) / 2;
        float QB = team.teamQBs.size() > 0 ? team.teamQBs.get(0).getRatSpeed() : 0;
        float avgSub = ovr(team.getRB(2));
        float hcOff = team.HC != null ? team.HC.ratOff : 0;
        return (3 * this.getCompositeOLRush() + 4 * avgRBs + QB + 2 * hcOff + avgSub) / 11;
    }

    public float getPassDef() {
        float cb0 = team.teamCBs.size() > 0 ? team.teamCBs.get(0).ratOvr : 0;
        float cb1 = team.teamCBs.size() > 1 ? team.teamCBs.get(1).ratOvr : 0;
        float cb2 = team.teamCBs.size() > 2 ? team.teamCBs.get(2).ratOvr : 0;
        float avgCBs = (cb0 + cb1 + cb2) / 3;
        float lb0c = team.teamLBs.size() > 0 ? team.teamLBs.get(0).getRatCoverage() : 0;
        float lb1c = team.teamLBs.size() > 1 ? team.teamLBs.get(1).getRatCoverage() : 0;
        float lb2c = team.teamLBs.size() > 2 ? team.teamLBs.get(2).getRatCoverage() : 0;
        float avgLBs = (lb0c + lb1c + lb2c) / 3;
        float s0c = team.teamSs.size() > 0 ? team.teamSs.get(0).getRatCoverage() : 0;
        float s1c = team.teamSs.size() > 1 ? team.teamSs.get(1).getRatCoverage() : 0;
        float S = (s0c + s1c) / 2;
        float def = (3 * avgCBs + avgLBs + S) / 5;
        float avgSub = (coverage(team.getLB(3)) + ovr(team.getCB(3)) * 2 + coverage(team.getS(2))) / 4;
        float ss0r = team.teamSs.size() > 0 ? team.teamSs.get(0).ratOvr : 0;
        float hcDef = team.HC != null ? team.HC.ratDef : 0;
        return (def * 4 + ss0r + this.getCompositeDLPass() * 2 + 2 * hcDef + avgSub) / 10;
    }

    public float getRushDef() {
        return this.getCompositeDLRush();
    }

    public float getCompositeOLPass() {
        float compositeOL = 0;
        int n = Math.min(5, team.teamOLs.size());
        for (int i = 0; i < n; ++i) {
            compositeOL += (team.teamOLs.get(i).getRatStrength() * 2 + team.teamOLs.get(i).getRatPassBlock() * 2 + team.teamOLs.get(i).getRatVision()) / 5f;
        }
        if (n > 0) {
            compositeOL = compositeOL / n;
        }
        float avgSub = (ovr(team.getOL(5)) + ovr(team.getOL(6))) / 2;
        float hcOff = team.HC != null ? team.HC.ratOff : 0;
        return (9 * compositeOL + avgSub + 3 * hcOff) / 13;
    }

    public float getCompositeOLRush() {
        float compositeOL = 0;
        int n = Math.min(5, team.teamOLs.size());
        for (int i = 0; i < n; ++i) {
            compositeOL += (team.teamOLs.get(i).getRatStrength() * 2 + team.teamOLs.get(i).getRatRunBlock() * 2 + team.teamOLs.get(i).getRatVision()) / 5f;
        }
        if (n > 0) {
            compositeOL = compositeOL / n;
        }
        float compositeTE = team.teamTEs.size() > 0 ? team.teamTEs.get(0).getRatRunBlock() : 0;
        float avgSub = (2 * ovr(team.getOL(5)) + 2 * ovr(team.getOL(6)) + runBlock(team.getTE(1))) / 5;
        float hcOff = team.HC != null ? team.HC.ratOff : 0;
        return (9 * compositeOL + 2 * compositeTE + 3 * hcOff + avgSub) / 15;
    }

    public float getCompositeDLPass() {
        float compositeDL = 0;
        int n = Math.min(4, team.teamDLs.size());
        for (int i = 0; i < n; ++i) {
            compositeDL += (team.teamDLs.get(i).getRatStrength() + team.teamDLs.get(i).getRatPassRush()) / 2f;
        }
        if (n > 0) {
            compositeDL = compositeDL / n;
        }
        float avgSub = ovr(team.getDL(4)) + ovr(team.getDL(5));
        float hcDef = team.HC != null ? team.HC.ratDef : 0;
        return (5 * compositeDL + 2 * hcDef + avgSub) / 8;
    }

    public float getCompositeDLRush() {
        float compositeDL = 0;
        float compositeLB = 0;
        float compositeS = 0;
        int dlN = Math.min(4, team.teamDLs.size());
        for (int i = 0; i < dlN; ++i) {
            compositeDL += (team.teamDLs.get(i).getRatStrength() + team.teamDLs.get(i).getRatRunStop()) / 2f;
        }
        if (dlN > 0) {
            compositeDL = compositeDL / dlN;
        }
        int lbN = Math.min(3, team.teamLBs.size());
        for (int i = 0; i < lbN; ++i) {
            compositeLB += team.teamLBs.get(i).getRatRunStop();
        }
        if (lbN > 0) {
            compositeLB = compositeLB / lbN;
        }
        float ss0rs = team.teamSs.size() > 0 ? team.teamSs.get(0).getRatRunStop() : 0;
        float ss1rs = team.teamSs.size() > 1 ? team.teamSs.get(1).getRatRunStop() : 0;
        compositeS += ss0rs + ss1rs;
        compositeS = compositeS / 2;
        float avgSub = (2 * ovr(team.getDL(4)) + 2 * ovr(team.getDL(5)) + runStop(team.getLB(3)) + runStop(team.getS(2))) / 6;
        float hcDef = team.HC != null ? team.HC.ratDef : 0;
        return (4 * compositeDL + 2 * compositeLB + 2 * compositeS + 2 * hcDef + avgSub) / 11;
    }

    // Section 3: SOS / RPI / Poll

    public void updateSOS() {
        for (int i = 0; i < team.gameSchedule.size(); ++i) {
            Game g = team.gameSchedule.get(i);
            if (g.homeTeam == team) {
                team.teamSOS += team.league.getTeamList().size() - g.awayTeam.rankTeamPollScore;
            } else {
                team.teamSOS += team.league.getTeamList().size() - g.homeTeam.rankTeamPollScore;
            }
        }
    }

    public double getSOS() {
        if (team.league.currentWeek < 2) return 0;
        float sos = 0;
        float oppWP = 0;
        float oppoppWP = 0;
        int teamOPWP = 0;
        float rpi = 0;
        float teamWP = 0;
        if (team.wins + team.losses > 0) teamWP = (float) team.wins / (team.wins + team.losses);
        for (Team t : team.gameWinsAgainst) {
            if (t.wins + t.losses > 0) oppWP += (float) t.wins / (t.wins + t.losses);
            for (int i = 0; i < t.getGameWinsAgainst().size(); i++) {
                for (int j = 0; j < t.getGameWinsAgainst().get(i).getGameWinsAgainst().size(); j++) {
                    Team teamX = t.getGameWinsAgainst().get(i).getGameWinsAgainst().get(j);
                    if (teamX.wins + teamX.losses > 0) oppoppWP += (float) teamX.wins / (teamX.wins + teamX.losses);
                    teamOPWP++;
                }
            }
        }
        for (Team t : team.gameLossesAgainst) {
            if (t.wins + t.losses > 0) oppWP += (float) t.wins / (t.wins + t.losses);
            for (int i = 0; i < t.getGameLossesAgainst().size(); i++) {
                for (int j = 0; j < t.getGameLossesAgainst().get(i).getGameLossesAgainst().size(); j++) {
                    Team teamX = t.getGameLossesAgainst().get(i).getGameLossesAgainst().get(j);
                    if (teamX.wins + teamX.losses > 0) oppoppWP += (float) teamX.wins / (teamX.wins + teamX.losses);
                    teamOPWP++;
                }
            }
        }
        if (team.gameWinsAgainst.size() + team.gameLossesAgainst.size() > 0) {
            oppWP = (float) oppWP / (team.gameWinsAgainst.size() + team.gameLossesAgainst.size());
        }
        if (teamOPWP > 0) {
            oppoppWP = (float) oppoppWP / teamOPWP;
        }
        sos = (float) (0.67 * oppWP + 0.33 * oppoppWP);
        return sos;
    }

    public void updateStrengthOfWins() {
        team.teamStrengthOfWins = 0;
        for (int i = 0; i < team.gameWinsAgainst.size(); ++i) {
            team.teamStrengthOfWins += 5 + (team.league.countTeam - team.gameWinsAgainst.get(i).rankTeamPollScore);
        }
    }

    public void updateLossStrength() {
        team.teamStrengthOfLosses = 0;
        for (int i = 0; i < team.gameLossesAgainst.size(); ++i) {
            team.teamStrengthOfLosses += team.gameLossesAgainst.get(i).rankTeamPollScore;
        }
    }

    public float getSOSPollScore() {
        float teamWP = 0;
        for (Game g : team.gameSchedule) {
            if (g.gameName.equals("BYE")) continue;
            if (!g.gameName.equals("Conference") && !g.gameName.equals("OOC")) {
                if (g.homeTeam == team && g.homeScore > g.awayScore) teamWP += 0.6 * (team.league.countTeam - g.awayTeam.rankTeamPollScore);
                else if (g.homeTeam == team && g.homeScore < g.awayScore) teamWP -= 1.4 * (team.league.countTeam - g.awayTeam.rankTeamPollScore);
                else if (g.awayTeam == team && g.awayScore > g.homeScore) teamWP += 1.4 * (team.league.countTeam - g.homeTeam.rankTeamPollScore);
                else if (g.awayTeam == team && g.awayScore < g.homeScore) teamWP -= 0.6 * (team.league.countTeam - g.homeTeam.rankTeamPollScore);
            } else {
                if (g.homeTeam == team && g.homeScore > g.awayScore) teamWP += 1 * (team.league.countTeam - g.awayTeam.rankTeamPollScore);
                else if (g.homeTeam == team && g.homeScore < g.awayScore) teamWP -= 1 * (team.league.countTeam - g.awayTeam.rankTeamPollScore);
                else if (g.awayTeam == team && g.awayScore > g.homeScore) teamWP += 1 * (team.league.countTeam - g.homeTeam.rankTeamPollScore);
                else if (g.awayTeam == team && g.awayScore < g.homeScore) teamWP -= 1 * (team.league.countTeam - g.homeTeam.rankTeamPollScore);
            }
        }
        return teamWP;
    }

    public void calcRPI() {
        team.teamRPI = (float) this.getRPI();
    }

    public float getRPI() {
        if (team.league.currentWeek < 7) return 0;
        if (team.wins + team.losses <= 0) return 0;
        float rpi = 0;
        float teamWP = 0;
        for (Game g : team.gameSchedule) {
            if (g.gameName.equals("BYE")) continue;
            if (!g.gameName.equals("Conference") && !g.gameName.equals("OOC")) {
                if (g.homeTeam == team && g.homeScore > g.awayScore) teamWP += 0.6;
                else if (g.homeTeam == team && g.homeScore < g.awayScore) teamWP -= 1.4;
                else if (g.awayTeam == team && g.awayScore > g.homeScore) teamWP += 1.4;
                else if (g.awayTeam == team && g.awayScore < g.homeScore) teamWP -= 0.6;
            } else {
                if (g.homeTeam == team && g.homeScore > g.awayScore) teamWP += 1;
                else if (g.homeTeam == team && g.homeScore < g.awayScore) teamWP -= 1;
                else if (g.awayTeam == team && g.awayScore > g.homeScore) teamWP += 1;
                else if (g.awayTeam == team && g.awayScore < g.homeScore) teamWP -= 1;
            }
        }
        teamWP = teamWP / (team.wins + team.losses);
        float oppWP = 0;
        float oppoppWP = 0;
        int teamOPWP = 0;
        for (Team t : team.gameWinsAgainst) {
            if (t.wins + t.losses > 0) oppWP += (float) t.wins / (t.wins + t.losses);
            for (int i = 0; i < t.getGameWinsAgainst().size(); i++) {
                for (int j = 0; j < t.getGameWinsAgainst().get(i).getGameWinsAgainst().size(); j++) {
                    Team teamX = t.getGameWinsAgainst().get(i).getGameWinsAgainst().get(j);
                    if (teamX.wins + teamX.losses > 0) oppoppWP += (float) teamX.wins / (teamX.wins + teamX.losses);
                    teamOPWP++;
                }
            }
        }
        for (Team t : team.gameLossesAgainst) {
            if (t.wins + t.losses > 0) oppWP += (float) t.wins / (t.wins + t.losses);
            for (int i = 0; i < t.getGameLossesAgainst().size(); i++) {
                for (int j = 0; j < t.getGameLossesAgainst().get(i).getGameLossesAgainst().size(); j++) {
                    Team teamX = t.getGameLossesAgainst().get(i).getGameLossesAgainst().get(j);
                    if (teamX.wins + teamX.losses > 0) oppoppWP += (float) teamX.wins / (teamX.wins + teamX.losses);
                    teamOPWP++;
                }
            }
        }
        if (team.gameWinsAgainst.size() + team.gameLossesAgainst.size() > 0) {
            oppWP = (float) oppWP / (team.gameWinsAgainst.size() + team.gameLossesAgainst.size());
        }
        if (teamOPWP > 0) {
            oppoppWP = (float) oppoppWP / teamOPWP;
        }
        rpi = (float) (0.25 * teamWP + 0.5 * oppWP + 0.25 * oppoppWP);
        return rpi;
    }

    public void updatePollScore() {
        this.updateStrengthOfWins();
        this.updateLossStrength();
        int offRating = this.offenseRating();
        int defRating = this.defenseRating();
        team.teamOffTalent = this.getOffTalent();
        team.teamDefTalent = this.getDefTalent();

        int univProRelBonus = 0;
        if (team.league.enableUnivProRel) {
            univProRelBonus = 20 * (10 - team.league.getConfNumber(team.conference));
        }

        double preseasonBias = 15 - (team.wins + team.losses);
        if (preseasonBias < 3) preseasonBias = 3;
        preseasonBias = preseasonBias / 15;
        team.teamPollScore =
                (float) (preseasonBias * this.getPreseasonBiasScore()) +
                        (float) (offRating + defRating + team.teamStrengthOfWins - team.teamStrengthOfLosses + 500 + univProRelBonus);

        if (team.league.currentWeek == 0) {
            team.teamPollScore = this.getPreseasonBiasScore();
        }

        if ("CC".equals(team.confChampion)) {
            team.teamPollScore += 20;
        }
        if ("NCW".equals(team.natChampWL)) {
            team.teamPollScore += 1000;
        }
        if ("NCL".equals(team.natChampWL)) {
            team.teamPollScore += 500;
        }
        if ("SFW".equals(team.semiFinalWL)) {
            team.teamPollScore += 200;
        }
        if ("SFL".equals(team.semiFinalWL)) {
            team.teamPollScore += 150;
        }
        if ("QTW".equals(team.qtFinalWL)) {
            team.teamPollScore += 150;
        }
        if ("QTL".equals(team.qtFinalWL)) {
            team.teamPollScore += 50;
        }
        if ("S16W".equals(team.sweet16) || "FRW".equals(team.sweet16)) {
            team.teamPollScore += 50;
        }
        if ("S16L".equals(team.sweet16) || "FRL".equals(team.sweet16)) {
            team.teamPollScore += 25;
        }
    }

    public float getPreseasonBiasScore() {
        float score = 0;
        if (team.league.currentWeek > 0) {
            score += team.league.getTeamList().size() - team.rankTeamOffTalent;
            score += team.league.getTeamList().size() - team.rankTeamDefTalent;
            score += 1.5 * (team.league.getTeamList().size() - team.rankTeamPrestige);
            if (team.league.getConferences().get(team.league.getConfNumber(team.conference)).confTeams.size() < team.league.getConferences().get(team.league.getConfNumber(team.conference)).minConfTeams) {
                score += team.teamPrestige / 1.2;
            } else {
                score += team.confPrestige;
            }
        } else {
            score += 1 * this.getOffTalent();
            score += 1 * this.getDefTalent();
            score += 3 * team.teamPrestige;
            if (team.league.getConferences().get(team.league.getConfNumber(team.conference)).confTeams.size() < team.league.getConferences().get(team.league.getConfNumber(team.conference)).minConfTeams) {
                score += team.teamPrestige / 1.2;
            } else {
                score += team.confPrestige;
            }
        }
        return score;
    }

    // Section 4: Win/Loss Stats

    public void incrementWins() { team.wins++; }
    public void incrementLosses() { team.losses++; }
    public void incrementTotalWins() { team.totalWins++; }
    public void incrementTotalLosses() { team.totalLosses++; }
    public void incrementTotalCCs() { team.totalCCs++; }
    public void incrementTotalCCLosses() { team.totalCCLosses++; }
    public void incrementTotalNCs() { team.totalNCs++; }
    public void incrementTotalNCLosses() { team.totalNCLosses++; }
    public void incrementTotalBowls() { team.totalBowls++; }
    public void incrementTotalBowlLosses() { team.totalBowlLosses++; }

    public void addTeamPoints(int pts) { team.teamPoints += pts; }
    public void addTeamOppPoints(int pts) { team.teamOppPoints += pts; }
    public void addTeamYards(int y) { team.teamYards += y; }
    public void addTeamOppYards(int y) { team.teamOppYards += y; }
    public void addTeamPassYards(int y) { team.teamPassYards += y; }
    public void addTeamRushYards(int y) { team.teamRushYards += y; }
    public void addTeamOppPassYards(int y) { team.teamOppPassYards += y; }
    public void addTeamOppRushYards(int y) { team.teamOppRushYards += y; }
    public void addTeamTODiff(int v) { team.teamTODiff += v; }

    // Section 5: Chemistry & Discipline

    public int getTeamDiscipline() {
        int rating = 0;
        ArrayList<Player> roster = team.getAllPlayers();
        for (int i = 0; i < roster.size(); ++i) {
            rating += roster.get(i).character;
        }
        return rating / roster.size();
    }

    public double getTeamChemistry() {
        double rating = 0;
        ArrayList<Player> roster = team.getAllPlayers();
        for (int i = 0; i < roster.size(); ++i) {
            rating += roster.get(i).character;
            rating += roster.get(i).ratIntelligence;
        }
        return rating / (2 * roster.size());
    }

    public int getStaffDiscipline() {
        int staff;
        staff = (3 * team.HC.ratDiscipline + team.OC.ratDiscipline + team.DC.ratDiscipline) / 5;
        return staff;
    }

    // Section 6: Team Info

    public String getTeamHomeInfo() {
        StringBuilder data = new StringBuilder();

        data.append(team.name + "&");
        data.append(team.getRankStr(team.rankTeamPollScore) + " Ranked&");
        data.append(team.wins + " wins and " + team.losses + " losses&");
        data.append("Offense: " + team.df2.format(this.getOffTalent()) + " (" + team.getRankStr(team.rankTeamOffTalent) + ") | Defense: " + team.df2.format(this.getDefTalent()) + " (" + team.getRankStr(team.rankTeamDefTalent) + ")\n");
        data.append("Prestige: " + team.getRankStr(team.rankTeamPrestige) + " | Discipline: " + team.teamDisciplineScore + "% | Facilities: L" + team.teamFacilities + "&");

        data.append("\n");

        for (Player p : team.getAllPlayers()) {
            if (p.injury == null) {
                p.isInjured = false;
                team.removePlayerInjured(p);
            }
        }

        if (team.getPlayersInjured().isEmpty()) {
            data.append("No Injuries\n");
        } else {
            for (Player p : team.getPlayersInjured()) {
                if (p.isInjured) data.append(p.position + " " + p.name + " [" + p.ratOvr + "] " + p.injury.duration + "wk\n");
            }
        }

        data.append("&\n");
        int i = 0;
        for (Player p : team.getAllPlayers()) {
            if (p.isSuspended) {
                data.append(p.position + " " + p.name + " [" + p.ratOvr + "] " + p.weeksSuspended + "wk\n");
                i++;
            }
        }
        if (i == 0) data.append("No Suspensions\n");

        data.append("&\n");

        int gamesplayed = 0;
        for (Game g : team.gameSchedule) {
            if (!g.hasPlayed) {
                if (g.gameName.equals("BYE WEEK")) {
                    data.append("Bye\n&");
                    break;
                } else {
                    data.append(g.gameName + " Game\n");
                    if (g.awayTeam.name != team.name) {
                        data.append("vs #" + g.awayTeam.rankTeamPollScore + " " + g.awayTeam.name + "\n" + "Rec: (" + g.awayTeam.wins + " - " + g.awayTeam.losses + ")\nOff: " + team.df2.format(g.awayTeam.getOffTalent()) + " | Def: " + team.df2.format(g.awayTeam.getDefTalent()) + "\n");
                        data.append(g.awayTeam.playbookOff.getStratName() + " | " + g.awayTeam.playbookDef.getStratName() + "\n&");
                    } else {
                        data.append("at #" + g.homeTeam.rankTeamPollScore + " " + g.homeTeam.name + "\n" + "Rec: (" + g.homeTeam.wins + " - " + g.homeTeam.losses + ")\nOff: " + team.df2.format(g.homeTeam.getOffTalent()) + " | Def: " + team.df2.format(g.homeTeam.getDefTalent()) + "\n");
                        data.append(g.homeTeam.playbookOff.getStratName() + " | " + g.homeTeam.playbookDef.getStratName() + "\n&");
                    }
                    break;
                }
            }
            gamesplayed++;
        }
        if (gamesplayed >= team.gameSchedule.size()) {
            data.append("End of Season\n\n\n&");
        }

        for (int n = 0; n < team.league.getNewsHeadlines().size(); n++) {
            data.append("+ " + team.league.getNewsHeadlines().get(n) + "\n\n");
        }

        data.append("&");

        data.append("\n");
        if (team.league.currentWeek < 1) data.append("No Game\n&");
        else {
            int played = 0;
            for (i = 0; i < team.gameSchedule.size(); i++) {
                if (!team.gameSchedule.get(i).hasPlayed) {
                    Game g = team.gameSchedule.get(i - 1);
                    if (team.gameSchedule.get(i - 1).gameName.equals("BYE WEEK")) {
                        data.append("Bye\n&");
                        break;
                    } else {
                        data.append(g.gameName + "\n");
                        data.append(g.awayTeam.name + " " + g.awayScore + "\n");
                        data.append(g.homeTeam.name + " " + g.homeScore + "\n&");
                        break;
                    }
                }
                played++;
            }
            if (played >= team.gameSchedule.size()) {
                Game g = team.gameSchedule.get(team.gameSchedule.size() - 1);
                data.append(g.gameName + "\n");
                data.append(g.awayTeam.name + " " + g.awayScore + "\n");
                data.append(g.homeTeam.name + " " + g.homeScore + "\n&");
            }
        }

        return data.toString();
    }

    private static int iq(Player p) {
        return p == null ? 0 : p.ratIntelligence;
    }

    private static float ovr(Player p) {
        return p == null ? 0 : p.ratOvr;
    }

    private static float catchRating(positions.PlayerWR p) {
        return p == null ? 0 : p.getRatCatch();
    }

    private static float catchRating(positions.PlayerTE p) {
        return p == null ? 0 : p.getRatCatch();
    }

    private static float catchRating(positions.PlayerRB p) {
        return p == null ? 0 : p.getRatCatch();
    }

    private static float coverage(positions.PlayerLB p) {
        return p == null ? 0 : p.getRatCoverage();
    }

    private static float coverage(positions.PlayerS p) {
        return p == null ? 0 : p.getRatCoverage();
    }

    private static float runBlock(positions.PlayerTE p) {
        return p == null ? 0 : p.getRatRunBlock();
    }

    private static float runStop(positions.PlayerLB p) {
        return p == null ? 0 : p.getRatRunStop();
    }

    private static float runStop(positions.PlayerS p) {
        return p == null ? 0 : p.getRatRunStop();
    }
}

