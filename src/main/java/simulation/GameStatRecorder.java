package simulation;

import java.util.ArrayList;
import java.util.Collections;
import comparator.CompGamePlayerPicker;
import positions.Player;
import positions.PlayerCB;
import positions.PlayerDL;
import positions.PlayerLB;
import positions.PlayerOL;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerReturner;
import positions.PlayerS;
import positions.PlayerTE;
import positions.PlayerWR;

class GameStatRecorder {

    private final Game game;

    GameStatRecorder(Game game) {
        this.game = game;
    }

    void recordRushAttempt(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, int yardsGain, boolean gotTD) {
        String defender = "";
        if (selRB.gameSim >= selQB.gameSim) {
            selRB.recordRushAtt(1);
            selRB.recordRushYards(yardsGain);
            selRB.gameRushAttempts++;
            selRB.gameRushYards += yardsGain;
        } else {
            selQB.recordRushAtt(1);
            selQB.recordRushYards(yardsGain);
            selQB.gameRushAttempts++;
            selQB.gameRushYards += yardsGain;
        }

        for (int i = 0; i < Math.min(5, game.teamOLs.size()); i++) {
            game.teamOLs.get(i).recordOLRunYards(yardsGain);
            game.teamOLs.get(i).recordRunSnaps(1);
        }

        if (yardsGain < 2 && !gotTD) {
            selDL.gameTackles++;
            selDL.recordTackles(1);
            defender = "DL " + selDL.name;
        } else if (yardsGain >= 2 && yardsGain < 12 && !gotTD) {
            selLB.gameTackles++;
            selLB.recordTackles(1);
            defender = "LB " + selLB.name;
        } else if (yardsGain >= 12 && !gotTD) {
            if (selCB.getRatTackle() * Math.random() * 50 >= selS.getRatTackle() * Math.random() * 100) {
                selCB.gameTackles++;
                selCB.recordTackles(1);
                defender = "CB " + selCB.name;
            } else {
                selS.gameTackles++;
                selS.recordTackles(1);
                defender = "S " + selS.name;
            }
        }

        if (game.gamePoss) {
            game.homeTeam.addTeamRushYards(yardsGain);
        } else {
            game.awayTeam.addTeamRushYards(yardsGain);
        }

        if (gotTD) {
            if (selRB.gameSim >= selQB.gameSim) {
                selRB.gameRushTDs++;
                selRB.recordRushTDs(1);
                game.tdInfo = offense.getAbbr() + " RB " + selRB.name + " rushed " + yardsGain + " yards for a TD.";
            } else {
                selQB.gameRushTDs++;
                selQB.recordRushTDs(1);
                game.tdInfo = offense.getAbbr() + " QB " + selQB.name + " rushed " + yardsGain + " yards for a TD.";
            }

            if (game.gamePoss) {
                game.homeScore += 6;
            } else {
                game.awayScore += 6;
            }
        } else {
            if (game.homeTeam.league.fullGameLog)
                if (selRB.gameSim >= selQB.gameSim) {
                    game.gameEventLog.append(game.getEventLog()).append(offense.getAbbr()).append(" RB ").append(selRB.name).append(" rushed for ").append(yardsGain).append(" yards, and was tackled by ").append(defender).append(".");
                } else {
                    game.gameEventLog.append(game.getEventLog()).append(offense.getAbbr()).append(" QB ").append(selQB.name).append(" scrambled for ").append(yardsGain).append(" yards, and was tackled by ").append(defender).append(".");
                }

        }
    }

    void recordRushFumble(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS) {
        String defender;
        ArrayList<Player> def = new ArrayList<>();
        def.add(selDL);
        def.add(selCB);
        def.add(selLB);
        def.add(selS);
        Collections.sort(def, new CompGamePlayerPicker());
        String pos = def.get(0).position;

        if (selRB.gameSim >= selQB.gameSim) {
            selRB.gameFumbles++;
            selRB.recordFumbles(1);
        } else {
            selQB.gameFumbles++;
            selQB.recordFumbles(1);
        }

        if (game.gamePoss) {
            game.homeTOs++;
        } else {
            game.awayTOs++;
        }

        if (pos.equals("DL")) {
            selDL.gameTackles++;
            selDL.recordTackles(1);
            selDL.gameFumbles++;
            selDL.recordFumblesRec(1);
            defender = ("DL " + selDL.name);
        } else if (pos.equals("CB")) {
            selCB.gameTackles++;
            selCB.recordTackles(1);
            selCB.gameFumbles++;
            selCB.recordFumblesRec(1);
            defender = ("CB " + selCB.name);
        } else if (pos.equals("S")) {
            selS.gameTackles++;
            selS.recordTackles(1);
            selS.gameFumbles++;
            selS.recordFumblesRec(1);
            defender = ("S " + selS.name);
        } else {
            selLB.gameTackles++;
            selLB.recordTackles(1);
            selLB.gameFumbles++;
            selLB.recordFumblesRec(1);
            defender = ("LB " + selLB.name);
        }

        if (selRB.gameSim >= selQB.gameSim) {
            game.gameEventLog.append(game.getEventLog()).append("FUMBLE!\n").append(offense.getAbbr()).append(" RB ").append(selRB.name).append(" fumbled the ball while rushing and recovered by ").append(defender).append(".");
        } else {
            game.gameEventLog.append(game.getEventLog()).append("FUMBLE!\n").append(offense.getAbbr()).append(" QB ").append(selQB.name).append(" fumbled the ball while rushing and recovered by ").append(defender).append(".");
        }

    }

    void recordPassingTD(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, int yardsGain, String pos) {
        if (game.gamePoss) {
            game.homeScore += 6;
        } else {
            game.awayScore += 6;
        }

        selQB.gamePassTDs++;
        selQB.recordPassTD(1);
        selQB.recordPassComp(1);
        selQB.recordPassYards(yardsGain);
        selQB.gamePassComplete++;
        selQB.gamePassYards += yardsGain;

        if (pos.equals("WR")) {
            selWR.gameRecTDs++;
            selWR.recordRecTDs(1);
            game.tdInfo = offense.getAbbr() + " QB " + selQB.name + " threw a " + yardsGain + " yard TD to WR " + selWR.name + ".";
        } else if (pos.equals("TE")) {
            selTE.gameRecTDs++;
            selTE.recordRecTDs(1);
            game.tdInfo = offense.getAbbr() + " QB " + selQB.name + " threw a " + yardsGain + " yard TD to TE " + selTE.name + ".";
        } else {
            selRB.gameRecTDs++;
            selRB.recordRecTDs(1);
            game.tdInfo = offense.getAbbr() + " QB " + selQB.name + " threw a " + yardsGain + " yard TD to RB " + selRB.name + ".";
        }


    }

    void recordPassCompletion(Team offense, PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerLB selLB, PlayerCB selCB, PlayerS selS, int yardsGain, String pos, boolean gotTD) {
        String defender;
        ArrayList<Player> def = new ArrayList<>();
        def.add(selCB);
        def.add(selLB);
        def.add(selS);
        Collections.sort(def, new CompGamePlayerPicker());
        String tackler = def.get(0).position;

        selQB.recordPassComp(1);
        selQB.recordPassYards(yardsGain);
        selQB.gamePassComplete++;
        selQB.gamePassYards += yardsGain;

        for (int i = 0; i < Math.min(5, game.teamOLs.size()); i++) {
            game.teamOLs.get(i).recordOLPassYards(yardsGain);
        }

        if (pos.equals("WR")) {
            selWR.recordReceptions(1);
            selWR.recordRecYards(yardsGain);
            selWR.gameReceptions++;
            selWR.gameRecYards += yardsGain;
        }
        if (pos.equals("TE")) {
            selTE.recordReceptions(1);
            selTE.recordRecYards(yardsGain);
            selTE.gameReceptions++;
            selTE.gameRecYards += yardsGain;
        }
        if (pos.equals("RB")) {
            selRB.recordReceptions(1);
            selRB.recordRecYards(yardsGain);
            selRB.gameReceptions++;
            selRB.gameRecYards += yardsGain;
        }

        if (!gotTD) {
            if (tackler.equals("CB")) {
                selCB.gameTackles++;
                selCB.recordTackles(1);

            } else if (tackler.equals("S")) {
                selS.gameTackles++;
                selS.recordTackles(1);

            } else {
                selLB.gameTackles++;
                selLB.recordTackles(1);

            }
        }

        offense.addTeamPassYards(yardsGain);
    }

    void recordPassAttempt(PlayerQB selQB, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerLB selLB, PlayerCB selCB, String pos) {
        selQB.recordPassAtt(1);
        selQB.gamePassAtempts++;

        for (int i = 0; i < Math.min(5, game.teamOLs.size()); i++) {
            game.teamOLs.get(i).recordPassSnaps(1);
        }

        if (pos.equals("WR")) {
            selWR.recordTargets(1);
            selWR.gameTargets++;
            selCB.recordTargeted(1);
            selCB.gameTargets++;
        }
        if (pos.equals("TE")) {
            selTE.recordTargets(1);
            selTE.gameTargets++;
            selLB.recordTargeted(1);
            selLB.gameTargets++;
        }
        if (pos.equals("RB")) {
            selRB.recordTargets(1);
            selRB.gameTargets++;
        }
    }

    void recordDrop(PlayerRB selRB, PlayerTE selTE, PlayerWR selWR, PlayerCB selCB, PlayerLB selLB, String pos) {
        if (pos.equals("WR")) {
            selWR.gameDrops++;
            selWR.recordDrops(1);
            selCB.recordDefIncompleted(1);
            selCB.gameIncomplete++;
        }
        if (pos.equals("TE")) {
            selTE.gameDrops++;
            selTE.recordDrops(1);
            selLB.recordDefIncompleted(1);
            selLB.gameIncomplete++;
        }
        if (pos.equals("RB")) {
            selRB.gameDrops++;
            selRB.recordDrops(1);
        }
    }

    void recordDefendedCB(PlayerWR selWR, PlayerCB selCB) {

        if ((selCB.getRatJump() * Math.random() + selCB.getRatCoverage() * Math.random()) > (selWR.getRatJump() * Math.random() + selWR.getRatCatch() * Math.random()) * 2) {
            selCB.recordDefended(1);
            selCB.gameDefended++;
        }
        selCB.recordDefIncompleted(1);
        selCB.gameIncomplete++;
    }

    void recordDefendedLB(PlayerTE selTE, PlayerLB selLB) {

        if ((selLB.getRatSpeed() * Math.random() + selLB.getRatCoverage() * Math.random()) > (selTE.getRatSpeed() * Math.random() + selTE.getRatCatch() * Math.random()) * 2) {
            selLB.recordDefended(1);
            selLB.gameDefended++;
        }
        selLB.recordDefIncompleted(1);
        selLB.gameIncomplete++;
    }

    void recordDefendedLB2(PlayerRB selRB, PlayerLB selLB) {

        if ((selLB.getRatSpeed() * Math.random() + selLB.getRatCoverage() * Math.random()) > (selRB.getRatSpeed() * Math.random() + selRB.getRatCatch() * Math.random()) * 2) {
            selLB.recordDefended(1);
            selLB.gameDefended++;
        }
    }

    void recordInterception(Team offense, PlayerQB selQB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, String position) {
        String defender;
        ArrayList<Player> def = new ArrayList<>();
        def.add(selDL);
        def.add(selCB);
        def.add(selLB);
        def.add(selS);
        Collections.sort(def, new CompGamePlayerPicker());
        String pos = def.get(0).position;

        if(position.equals("RB")) {
            selLB.recordDefIncompleted(1);
            selLB.gameIncomplete++;
        } else if(position.equals("WR")) {
            selCB.recordDefIncompleted(1);
            selCB.gameIncomplete++;
        } else {
            selLB.recordDefIncompleted(1);
            selLB.gameIncomplete++;
        }

        if (pos.equals("DL")) {
            selDL.gameInterceptions++;
            selDL.recordInterceptions(1);
            defender = ("DL " + selDL.name);
        } else if (pos.equals("CB")) {
            selCB.gameInterceptions++;
            selCB.recordInterceptions(1);
            defender = ("CB " + selCB.name);
        } else if (pos.equals("S")) {
            selS.gameInterceptions++;
            selS.recordInterceptions(1);
            defender = ("S " + selS.name);
        } else {
            selLB.gameInterceptions++;
            selLB.recordInterceptions(1);
            defender = ("LB " + selLB.name);
        }

        if (game.gamePoss) {
            game.homeTOs++;
        } else {
            game.awayTOs++;
        }

        selQB.recordPassInt(1);
        selQB.gamePassInts++;

        game.gameEventLog.append(game.getEventLog()).append("INTERCEPTED!\n").append(offense.getAbbr()).append(" QB ").append(offense.getQB(0).name).append(" was intercepted by ").append(defender).append(".");
        game.gameTime -= game.timePerPlay * Math.random();
        if (!game.playingOT) {
            game.gameDown = 1;
            game.gameYardsNeed = 10;
            game.gamePoss = !game.gamePoss;
            game.gameYardLine = 100 - game.gameYardLine;
        } else game.resetForOT();

    }

    void recordSack(Team offense, Team defense, PlayerQB selQB, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS) {
        String defender = "";
        int sackloss = (3 + (int) (Math.random() * (((int) defense.getCompositeDLPass()) - ((int) offense.getCompositeOLPass())) / 2));
        if (sackloss < 2) sackloss = 2;

        ArrayList<Player> def = new ArrayList<>();
        def.add(selDL);
        def.add(selCB);
        def.add(selLB);
        def.add(selS);
        Collections.sort(def, new CompGamePlayerPicker());
        String pos = def.get(0).position;

        selQB.recordSacked(1);
        selQB.gameSacks++;
        selQB.recordRushYards(-sackloss);
        selQB.gameRushYards -= sackloss;


        if (pos.equals("DL")) {
            selDL.gameTackles++;
            selDL.gameSacks++;
            selDL.recordTackles(1);
            selDL.recordSacks(1);
            defender = ("DL " + selDL.name);
            for (int i = 0; i < Math.min(5, game.teamOLs.size()); i++) {
                game.teamOLs.get(i).recordOLSacksAllowed(1);
                game.teamOLs.get(i).recordPassSnaps(1);
            }
        } else if (pos.equals("LB")) {
            selLB.gameTackles++;
            selLB.gameSacks++;
            selLB.recordTackles(1);
            selLB.recordSacks(1);
            defender = ("LB " + selLB.name);
        } else if (pos.equals("CB")) {
            selCB.gameTackles++;
            selCB.gameSacks++;
            selCB.recordTackles(1);
            selCB.recordSacks(1);
            defender = ("CB " + selCB.name);
        } else if (pos.equals("S")) {
            selS.gameTackles++;
            selS.gameSacks++;
            selS.recordTackles(1);
            selS.recordSacks(1);
            defender = ("S " + selS.name);
        }

        if (game.homeTeam.league.fullGameLog)
            game.gameEventLog.append(game.getEventLog()).append("SACK!\n").append(" QB ").append(offense.getQB(0).name).append(
                    " was sacked for a loss of ").append(sackloss).append(" by ").append(defender).append(".");

        game.gameDown++;
        game.gameYardsNeed += sackloss;
        game.gameYardLine -= sackloss;

        if (game.gameYardLine < 0) {
            game.gameTime -= 10 * Math.random();
            recordSafety(defender);
            return;
        }

        game.gameTime -= game.timePerPlay + game.timePerPlay * Math.random();
    }

    void recordRecFumble(Team offense, PlayerRB selRB, PlayerWR selWR, PlayerTE selTE, PlayerDL selDL, PlayerLB selLB, PlayerCB selCB, PlayerS selS, String pos) {
        String defender;
        String fumblerName;
        ArrayList<Player> def = new ArrayList<>();
        def.add(selDL);
        def.add(selCB);
        def.add(selLB);
        def.add(selS);
        Collections.sort(def, new CompGamePlayerPicker());
        String player = def.get(0).position;


        if (pos.equals("WR")) {
            selWR.gameFumbles++;
            selWR.recordFumbles(1);
            fumblerName = selWR.name;
        } else if (pos.equals("TE")) {
            selTE.gameFumbles++;
            selTE.recordFumbles(1);
            fumblerName = selTE.name;
        } else if (pos.equals("RB")) {
            selRB.gameFumbles++;
            selRB.recordFumbles(1);
            fumblerName = selRB.name;
        } else {
            offense.getQB(0).gameFumbles++;
            offense.getQB(0).recordFumbles(1);
            fumblerName = offense.getQB(0).name;
        }

        if (player.equals("DL")) {
            selDL.gameTackles++;
            selDL.recordTackles(1);
            selDL.gameFumbles++;
            selDL.recordFumblesRec(1);
            defender = ("DL " + selDL.name);
        } else if (player.equals("CB")) {
            selCB.gameTackles++;
            selCB.recordTackles(1);
            selCB.gameFumbles++;
            selCB.recordFumblesRec(1);
            defender = ("CB " + selCB.name);
        } else if (player.equals("S")) {
            selS.gameTackles++;
            selS.recordTackles(1);
            selS.gameFumbles++;
            selS.recordFumblesRec(1);
            defender = ("S " + selS.name);
        } else {
            selLB.gameTackles++;
            selLB.recordTackles(1);
            selLB.gameFumbles++;
            selLB.recordFumblesRec(1);
            defender = ("LB " + selLB.name);
        }

        game.gameEventLog.append(game.getEventLog()).append("FUMBLE!\n").append(offense.getAbbr()).append(" receiver ").append(fumblerName).append(" fumbled the ball after a catch. It was recovered by ").append(defender).append(".");
    }

    void recordSafety(String defender) {
        if (game.gamePoss) {
            game.awayScore += 2;
            game.gameEventLog.append(game.getEventLogScoring()).append("SAFETY!\n").append(game.homeTeam.getAbbr()).append(" QB ").append(game.homeTeam.getQB(0).name).append(
                    " was tackled in the endzone by ").append(defender).append("! Result is a Safety and ").append(game.awayTeam.getAbbr()).append(" will get possession.");
            game.freeKick(game.homeTeam, game.awayTeam);
        } else {
            game.homeScore += 2;
            game.gameEventLog.append(game.getEventLogScoring()).append("SAFETY!\n").append(game.awayTeam.getAbbr()).append(" QB ").append(game.awayTeam.getQB(0).name)
                    .append(" was tackled in the endzone by ").append(defender).append("! Result is a Safety and ").append(game.homeTeam.getAbbr()).append(" will get possession.");
            game.freeKick(game.awayTeam, game.homeTeam);
        }
    }

    void recordReturnStats() {
        if (game.homeKickReturner != null) {
            recordSideReturnStats(game.homeTeam, game.homeKickReturner);
        }
        if (game.awayKickReturner != null) {
            recordSideReturnStats(game.awayTeam, game.awayKickReturner);
        }
    }

    private void recordSideReturnStats(Team team, PlayerReturner ret) {
        if (team == null || ret == null || ret.position == null) {
            return;
        }
        if ("RB".equals(ret.position)) {
            int limit = Math.min(team.startersRB + team.subRB, team.getTeamRBs().size());
            for (int i = 0; i < limit; i++) {
                if (team.getRB(i).name.equals(ret.name)) {
                    team.getRB(i).recordKORets(ret.kReturns);
                    team.getRB(i).recordKOYards(ret.kYards);
                    team.getRB(i).recordKOTDs(ret.kTD);
                    team.getRB(i).recordPuntRets(ret.pReturns);
                    team.getRB(i).recordPuntYards(ret.pYards);
                    team.getRB(i).recordPuntTDs(ret.pTD);
                }
            }
        } else if ("WR".equals(ret.position)) {
            int limit = Math.min(team.startersWR + team.subWR, team.getTeamWRs().size());
            for (int i = 0; i < limit; i++) {
                if (team.getWR(i).name.equals(ret.name)) {
                    team.getWR(i).recordKORets(ret.kReturns);
                    team.getWR(i).recordKOYards(ret.kYards);
                    team.getWR(i).recordKOTDs(ret.kTD);
                    team.getWR(i).recordPuntRets(ret.pReturns);
                    team.getWR(i).recordPuntYards(ret.pYards);
                    team.getWR(i).recordPuntTDs(ret.pTD);
                }
            }
        } else {
            int limit = Math.min(team.startersCB + team.subCB, team.getTeamCBs().size());
            for (int i = 0; i < limit; i++) {
                if (team.getCB(i).name.equals(ret.name)) {
                    team.getCB(i).recordKORets(ret.kReturns);
                    team.getCB(i).recordKOYards(ret.kYards);
                    team.getCB(i).recordKOTDs(ret.kTD);
                    team.getCB(i).recordPuntRets(ret.pReturns);
                    team.getCB(i).recordPuntYards(ret.pYards);
                    team.getCB(i).recordPuntTDs(ret.pTD);
                }
            }
        }
    }
}
